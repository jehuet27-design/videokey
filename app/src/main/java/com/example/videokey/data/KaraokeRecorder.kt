package com.example.videokey.data

import android.media.MediaPlayer
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

data class RecordedPerformance(
    val fileName: String,
    val songTitle: String,
    val dateLabel: String,
    val durationLabel: String,
    val filePath: String,
    val sizeKb: Long
)

/**
 * Videoke Performance Recorder (Voice + Accompaniment Music -> Saved .WAV File):
 * Captures live microphone vocals (boses) with warm studio echo AND mixes in the
 * synchronized song accompaniment (tugtog) so both voice and music are clearly
 * audible in the saved recording under /storage/emulated/0/VideoKey/recordings/.
 */
class KaraokeRecorder {

    @Volatile
    var isRecording: Boolean = false
        private set

    private var currentPcmStream: FileOutputStream? = null
    private var currentTempFile: File? = null
    private var currentSongCode: String = ""
    private var currentSongTitle: String = ""
    private var totalSamplesWritten: Long = 0L
    private val sampleRate = 16000

    // Echo delay line (~180ms slapback karaoke reverb)
    private val echoDelaySamples = (sampleRate * 0.18).toInt()
    private val echoBuffer = ShortArray(echoDelaySamples)
    private var echoWriteIdx = 0

    // Synthesized accompaniment phase accumulators for up to 6 simultaneous MIDI notes
    private var activeAccompNotes: List<MidiKaraokeParser.MelodyNote> = emptyList()
    @Volatile
    private var currentPlaybackPosMs: Long = 0L
    @Volatile
    private var keyTransposeSemitones: Int = 0
    private var synthSampleCounter: Long = 0L

    private var previewPlayer: MediaPlayer? = null
    @Volatile
    var currentlyPlayingRecordingPath: String? = null
        private set

    fun getRecordingsFolder(): File {
        val root = Environment.getExternalStorageDirectory() ?: File("/storage/emulated/0")
        val dir = File(root, "VideoKey/recordings")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    @Synchronized
    fun startRecording(
        songCode: String,
        songTitle: String,
        accompNotes: List<MidiKaraokeParser.MelodyNote>
    ): Boolean {
        stopAndSaveRecording()
        return try {
            val dir = getRecordingsFolder()
            val cleanTitle = songTitle.replace(Regex("""[^a-zA-Z0-9 _-]"""), "").trim().take(24).ifBlank { "Song" }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val tempFile = File(dir, ".rec_tmp_${songCode}_$timestamp.pcm")
            currentPcmStream = FileOutputStream(tempFile)
            currentTempFile = tempFile
            currentSongCode = songCode
            currentSongTitle = cleanTitle
            activeAccompNotes = accompNotes
            totalSamplesWritten = 0L
            synthSampleCounter = 0L
            echoBuffer.fill(0)
            echoWriteIdx = 0
            isRecording = true
            Log.d("KaraokeRecorder", "Started recording for [$songCode] $songTitle -> ${tempFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e("KaraokeRecorder", "Failed to start recording", e)
            isRecording = false
            false
        }
    }

    fun updatePlaybackState(posMs: Long, keyTranspose: Int, accompNotes: List<MidiKaraokeParser.MelodyNote>? = null) {
        currentPlaybackPosMs = posMs
        keyTransposeSemitones = keyTranspose
        if (accompNotes != null && accompNotes.isNotEmpty()) {
            activeAccompNotes = accompNotes
        }
    }

    /**
     * Called from MicrophoneScorer's audio loop with live 16-bit PCM microphone samples.
     * Mixes the singer's voice (with karaoke echo) + backing accompaniment notes and writes to disk.
     */
    @Synchronized
    fun onMicPcmChunk(micBuffer: ShortArray, readSize: Int) {
        if (!isRecording) return
        val out = currentPcmStream ?: return

        try {
            val posMs = currentPlaybackPosMs
            val notesNow = if (activeAccompNotes.isNotEmpty() && posMs > 0L) {
                activeAccompNotes.filter { posMs in it.startMs..it.endMs }.take(5)
            } else {
                emptyList()
            }

            // Precompute angular frequencies for currently active accompaniment notes
            val freqs = DoubleArray(notesNow.size)
            val amps = DoubleArray(notesNow.size)
            for (nIdx in notesNow.indices) {
                val note = notesNow[nIdx]
                val shiftedMidi = (note.midiNote + keyTransposeSemitones).coerceIn(24, 108)
                val hz = 440.0 * 2.0.pow((shiftedMidi - 69) / 12.0)
                freqs[nIdx] = (2.0 * PI * hz) / sampleRate.toDouble()
                amps[nIdx] = (note.velocity.coerceIn(30, 127) / 127.0) * 1800.0
            }

            val outBytes = ByteArray(readSize * 2)
            for (i in 0 until readSize) {
                val rawMic = micBuffer[i].toInt()
                // Apply studio vocal boost + warm karaoke echo
                val delayedSample = echoBuffer[echoWriteIdx].toInt()
                val vocalWithEcho = (rawMic * 1.45 + delayedSample * 0.32).toInt()
                echoBuffer[echoWriteIdx] = (rawMic + (delayedSample * 0.25).toInt()).coerceIn(-32768, 32767).toShort()
                echoWriteIdx = (echoWriteIdx + 1) % echoDelaySamples

                // Synthesize warm harmonic accompaniment backing (tugtog) so music is always audible
                var accompSignal = 0.0
                val sampleIdx = synthSampleCounter++
                for (nIdx in freqs.indices) {
                    val phase = freqs[nIdx] * sampleIdx
                    // Fundamental + soft 2nd harmonic for warm electric piano/synth accompaniment timbre
                    accompSignal += (sin(phase) + 0.35 * sin(phase * 2.0)) * amps[nIdx]
                }

                val mixed = (vocalWithEcho + accompSignal.toInt()).coerceIn(-32768, 32767).toShort().toInt()
                outBytes[i * 2] = (mixed and 0xFF).toByte()
                outBytes[i * 2 + 1] = ((mixed ushr 8) and 0xFF).toByte()
            }

            out.write(outBytes)
            totalSamplesWritten += readSize
        } catch (e: Exception) {
            Log.e("KaraokeRecorder", "Error writing PCM chunk", e)
        }
    }

    @Synchronized
    fun stopAndSaveRecording(): RecordedPerformance? {
        if (!isRecording) return null
        isRecording = false
        try {
            currentPcmStream?.flush()
            currentPcmStream?.close()
        } catch (_: Exception) {}
        currentPcmStream = null

        val tempFile = currentTempFile
        currentTempFile = null
        if (tempFile == null || !tempFile.exists() || totalSamplesWritten < sampleRate / 2) {
            tempFile?.delete()
            return null
        }

        return try {
            val dir = getRecordingsFolder()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val safeTitle = currentSongTitle.replace(" ", "_").ifBlank { "Videoke" }
            val wavFile = File(dir, "VK_${currentSongCode}_${safeTitle}_$timestamp.wav")
            writeWavFile(tempFile, wavFile, sampleRate)
            tempFile.delete()
            Log.d("KaraokeRecorder", "Saved WAV recording: ${wavFile.absolutePath} (${wavFile.length()} bytes)")
            toRecordedPerformance(wavFile)
        } catch (e: Exception) {
            Log.e("KaraokeRecorder", "Failed to finalize WAV file", e)
            tempFile.delete()
            null
        }
    }

    fun getRecordingDurationSeconds(): Int {
        if (!isRecording) return 0
        return (totalSamplesWritten / sampleRate).toInt()
    }

    private fun writeWavFile(pcmFile: File, wavFile: File, sampleRate: Int) {
        val pcmDataSize = pcmFile.length().toInt()
        val totalDataLen = pcmDataSize + 36
        val byteRate = sampleRate * 2 // 16-bit mono = 2 bytes per sample

        FileOutputStream(wavFile).use { out ->
            val header = ByteArray(44)
            // "RIFF"
            header[0] = 'R'.code.toByte()
            header[1] = 'I'.code.toByte()
            header[2] = 'F'.code.toByte()
            header[3] = 'F'.code.toByte()
            writeIntLE(header, 4, totalDataLen)
            // "WAVEfmt "
            header[8] = 'W'.code.toByte()
            header[9] = 'A'.code.toByte()
            header[10] = 'V'.code.toByte()
            header[11] = 'E'.code.toByte()
            header[12] = 'f'.code.toByte()
            header[13] = 'm'.code.toByte()
            header[14] = 't'.code.toByte()
            header[15] = ' '.code.toByte()
            writeIntLE(header, 16, 16) // Subchunk1Size (16 for PCM)
            writeShortLE(header, 20, 1) // AudioFormat = 1 (PCM)
            writeShortLE(header, 22, 1) // NumChannels = 1 (Mono)
            writeIntLE(header, 24, sampleRate)
            writeIntLE(header, 28, byteRate)
            writeShortLE(header, 32, 2) // BlockAlign = 2
            writeShortLE(header, 34, 16) // BitsPerSample = 16
            // "data"
            header[36] = 'd'.code.toByte()
            header[37] = 'a'.code.toByte()
            header[38] = 't'.code.toByte()
            header[39] = 'a'.code.toByte()
            writeIntLE(header, 40, pcmDataSize)
            out.write(header, 0, 44)

            FileInputStream(pcmFile).use { fis ->
                val buf = ByteArray(8192)
                var read: Int
                while (fis.read(buf).also { read = it } != -1) {
                    out.write(buf, 0, read)
                }
            }
        }
    }

    private fun writeIntLE(buf: ByteArray, offset: Int, value: Int) {
        buf[offset] = (value and 0xFF).toByte()
        buf[offset + 1] = ((value ushr 8) and 0xFF).toByte()
        buf[offset + 2] = ((value ushr 16) and 0xFF).toByte()
        buf[offset + 3] = ((value ushr 24) and 0xFF).toByte()
    }

    private fun writeShortLE(buf: ByteArray, offset: Int, value: Int) {
        buf[offset] = (value and 0xFF).toByte()
        buf[offset + 1] = ((value ushr 8) and 0xFF).toByte()
    }

    fun listSavedRecordings(): List<RecordedPerformance> {
        val dir = getRecordingsFolder()
        val files = dir.listFiles { f -> f.isFile && f.extension.equals("wav", ignoreCase = true) && f.length() > 44L }
            ?: return emptyList()
        return files.sortedByDescending { it.lastModified() }.map { toRecordedPerformance(it) }
    }

    private fun toRecordedPerformance(file: File): RecordedPerformance {
        val pcmBytes = (file.length() - 44L).coerceAtLeast(0L)
        val totalSec = (pcmBytes / (sampleRate * 2)).toInt()
        val mins = totalSec / 60
        val secs = totalSec % 60
        val durLabel = String.format(Locale.US, "%02d:%02d", mins, secs)
        val dateLabel = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.US).format(Date(file.lastModified()))

        // Parse friendly song title from VK_<code>_<title>_<timestamp>.wav
        val parts = file.nameWithoutExtension.split("_")
        val parsedTitle = if (parts.size >= 4 && parts[0] == "VK") {
            val code = parts[1]
            val titlePart = parts.subList(2, parts.size - 2).joinToString(" ")
            "[$code] $titlePart"
        } else {
            file.nameWithoutExtension
        }

        return RecordedPerformance(
            fileName = file.name,
            songTitle = parsedTitle,
            dateLabel = dateLabel,
            durationLabel = durLabel,
            filePath = file.absolutePath,
            sizeKb = (file.length() / 1024L).coerceAtLeast(1L)
        )
    }

    fun playRecording(path: String, onFinished: () -> Unit = {}) {
        stopPlayingRecording()
        try {
            val f = File(path)
            if (!f.exists()) return
            currentlyPlayingRecordingPath = path
            previewPlayer = MediaPlayer().apply {
                setDataSource(f.absolutePath)
                setOnCompletionListener {
                    currentlyPlayingRecordingPath = null
                    onFinished()
                }
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("KaraokeRecorder", "Failed to play recording: $path", e)
            currentlyPlayingRecordingPath = null
            onFinished()
        }
    }

    fun stopPlayingRecording() {
        try {
            previewPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (_: Exception) {}
        previewPlayer = null
        currentlyPlayingRecordingPath = null
    }

    fun deleteRecording(path: String): Boolean {
        if (currentlyPlayingRecordingPath == path) {
            stopPlayingRecording()
        }
        return try {
            File(path).delete()
        } catch (_: Exception) {
            false
        }
    }
}
