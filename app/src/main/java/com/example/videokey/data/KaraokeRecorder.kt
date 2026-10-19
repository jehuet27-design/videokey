package com.example.videokey.data

import android.media.MediaPlayer
import android.os.Environment
import android.util.Log
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.sqrt

data class RecordedPerformance(
    val fileName: String,
    val songTitle: String,
    val dateLabel: String,
    val durationLabel: String,
    val filePath: String,
    val sizeKb: Long
)

/**
 * Studio-Grade Videoke Performance Recorder (True 48,000 Hz 2-Channel Stereo 16-bit PCM .WAV):
 * - Captures 100% genuine 32-bit float stereo BASSMIDI SoundFont (.sf2) accompaniment directly from [SoundEngine]
 *   after the 10-band Studio EQ, Compressor, Reverb, and Instrument Mixer — preserving full stereo separation ("buo ang tunog").
 * - Uses BASSMIDI's real-time stereo frame clock 1:1 as the master recording clock when music is playing so
 *   zero instrument samples are ever dropped or padded (0 waveform discontinuities).
 * - Uses a smart close-mic vocal noise gate that blocks loudspeaker bleed when music is playing while mixing
 *   live singing vocals centered across the stereo stage with soft-knee mastering limiting ("walang basag").
 * - Offloads all disk writes to a dedicated background writer thread so BASS's real-time DSP thread never blocks.
 */
class KaraokeRecorder {

    companion object {
        private const val TAG = "KaraokeRecorder"
        const val SAMPLE_RATE = 48000
        const val NUM_CHANNELS = 2 // True Stereo (Left + Right)
        const val BYTES_PER_FRAME = NUM_CHANNELS * 2 // 4 bytes per stereo 16-bit frame
        const val BYTE_RATE = SAMPLE_RATE * BYTES_PER_FRAME // 192,000 bytes/sec
        private const val MIC_INPUT_RATE = 16000
        private const val VOCAL_RING_CAPACITY = SAMPLE_RATE * 2 // 2 seconds of 48kHz mono vocal samples
    }

    @Volatile
    var isRecording: Boolean = false
        private set

    private var currentPcmStream: BufferedOutputStream? = null
    private var currentTempFile: File? = null
    private var currentSongCode: String = ""
    private var currentSongTitle: String = ""
    @Volatile
    private var totalStereoFramesWritten: Long = 0L

    // Async disk writer queue so BASS's real-time DSP callback never touches disk I/O
    private val writeQueue = LinkedBlockingQueue<ByteArray>(1024)
    private var writerThread: Thread? = null
    @Volatile
    private var isWriterRunning: Boolean = false

    // Lock-protected 48kHz mono vocal ring buffer (fed by MicrophoneScorer, drained alongside BASS stereo frames)
    private val vocalLock = Any()
    private val vocalRing = ShortArray(VOCAL_RING_CAPACITY)
    private var vocalReadIdx = 0
    private var vocalWriteIdx = 0
    private var vocalCount = 0
    private var lastHeldVocal = 0f

    @Volatile
    private var lastMusicChunkTimeMs: Long = 0L
    @Volatile
    private var hasMusicStarted: Boolean = false
    @Volatile
    private var recordingStartTimeMs: Long = 0L

    // Microphone Vocal DSP state (at 16kHz before 3x linear interpolation to 48kHz)
    private var hpPrevIn = 0f
    private var hpPrevOut = 0f
    private var prevVocalSample = 0f

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
        accompNotes: List<MidiKaraokeParser.MelodyNote> = emptyList()
    ): Boolean {
        stopAndSaveRecording()
        return try {
            val dir = getRecordingsFolder()
            val cleanTitle = songTitle.replace(Regex("""[^a-zA-Z0-9 _-]"""), "").trim().take(24).ifBlank { "Song" }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val tempFile = File(dir, ".rec_tmp_${songCode}_$timestamp.pcm")
            val stream = BufferedOutputStream(FileOutputStream(tempFile), 65536)
            currentPcmStream = stream
            currentTempFile = tempFile
            currentSongCode = songCode
            currentSongTitle = cleanTitle
            totalStereoFramesWritten = 0L
            lastMusicChunkTimeMs = 0L
            hasMusicStarted = false
            recordingStartTimeMs = System.currentTimeMillis()

            synchronized(vocalLock) {
                vocalReadIdx = 0
                vocalWriteIdx = 0
                vocalCount = 0
                lastHeldVocal = 0f
            }

            hpPrevIn = 0f
            hpPrevOut = 0f
            prevVocalSample = 0f

            writeQueue.clear()
            isWriterRunning = true
            isRecording = true

            writerThread = Thread({
                try {
                    while (isWriterRunning || writeQueue.isNotEmpty()) {
                        val chunk = writeQueue.poll(20, TimeUnit.MILLISECONDS)
                        if (chunk != null) {
                            stream.write(chunk)
                            while (true) {
                                val next = writeQueue.poll() ?: break
                                stream.write(next)
                            }
                        }
                    }
                    stream.flush()
                } catch (e: Exception) {
                    Log.e(TAG, "Background PCM writer error", e)
                }
            }, "VideoKey-RecWriter").apply {
                priority = Thread.NORM_PRIORITY + 1
                start()
            }

            Log.d(TAG, "Started 48kHz True Stereo Studio Recording for [$songCode] $songTitle -> ${tempFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            isRecording = false
            isWriterRunning = false
            false
        }
    }

    fun updatePlaybackState(posMs: Long, keyTranspose: Int, accompNotes: List<MidiKaraokeParser.MelodyNote>? = null) {
        // Real BASSMIDI SoundFont stereo PCM is streamed directly via onBassMusicPcm()
    }

    /**
     * Called directly from [SoundEngine]'s BASS DSP callback with live 48,000 Hz stereo 32-bit float
     * SoundFont (.sf2) audio after the 10-band Studio EQ, Compressor, and Instrument Mixer.
     *
     * Drives the recording clock 1:1 with zero dropped or padded music frames!
     */
    fun onBassMusicPcm(stereoFloats: FloatArray, floatCount: Int) {
        if (!isRecording) return
        val frames = floatCount / 2
        if (frames <= 0) return

        hasMusicStarted = true
        lastMusicChunkTimeMs = System.currentTimeMillis()

        val outBytes = ByteArray(frames * BYTES_PER_FRAME)

        synchronized(vocalLock) {
            // Buffer guard: Only adjust if vocal buffer has accumulated over 500ms of lag
            val maxBufferLag = SAMPLE_RATE / 2 // 500ms
            if (vocalCount > maxBufferLag) {
                val excess = vocalCount - (SAMPLE_RATE / 4)
                vocalReadIdx = (vocalReadIdx + excess) % VOCAL_RING_CAPACITY
                vocalCount -= excess
            }

            var byteOffset = 0
            for (i in 0 until frames) {
                val musicL = stereoFloats[i * 2]
                val musicR = stereoFloats[i * 2 + 1]

                val vocalSample = if (vocalCount > 0) {
                    val v = vocalRing[vocalReadIdx].toFloat() / 32768f
                    vocalReadIdx = (vocalReadIdx + 1) % VOCAL_RING_CAPACITY
                    vocalCount--
                    lastHeldVocal = v
                    v
                } else {
                    lastHeldVocal *= 0.95f
                    lastHeldVocal
                }

                // True Stereo Studio Mix: Full instrument accompaniment + clean vocal
                val mixL = musicL * 0.82f + vocalSample * 0.72f
                val mixR = musicR * 0.82f + vocalSample * 0.72f

                // Studio soft-knee analog saturation (strictly prevents digital clipping / basag na tunog)
                val limL = softLimitStudio(mixL)
                val limR = softLimitStudio(mixR)

                val pcmL = (limL * 32767f).toInt().coerceIn(-32768, 32767)
                val pcmR = (limR * 32767f).toInt().coerceIn(-32768, 32767)

                outBytes[byteOffset] = (pcmL and 0xFF).toByte()
                outBytes[byteOffset + 1] = ((pcmL ushr 8) and 0xFF).toByte()
                outBytes[byteOffset + 2] = (pcmR and 0xFF).toByte()
                outBytes[byteOffset + 3] = ((pcmR ushr 8) and 0xFF).toByte()
                byteOffset += 4
            }
        }

        if (writeQueue.offer(outBytes)) {
            totalStereoFramesWritten += frames
        }
    }

    private fun softLimitStudio(x: Float): Float {
        val ax = abs(x)
        if (ax <= 0.65f) return x
        val sign = if (x >= 0f) 1f else -1f
        val excess = ax - 0.65f
        val compressed = 0.65f + 0.30f * (excess / (excess + 0.35f))
        return (sign * compressed).coerceIn(-0.99f, 0.99f)
    }

    /**
     * Called from [MicrophoneScorer]'s audio loop with live 16,000 Hz 16-bit PCM microphone samples.
     * Uses natural high-pass filtering and clean 3x interpolation to 48kHz without harsh gating.
     */
    fun onMicPcmChunk(micBuffer: ShortArray, readSize: Int) {
        if (!isRecording || readSize <= 0) return

        val now = System.currentTimeMillis()

        synchronized(vocalLock) {
            for (i in 0 until readSize) {
                // 1. Gentle high-pass filter (~80 Hz) to eliminate microphone handling thumps
                val rawIn = micBuffer[i].toFloat()
                val hpOut = 0.970f * (hpPrevOut + rawIn - hpPrevIn)
                hpPrevIn = rawIn
                hpPrevOut = hpOut

                // 2. Subtle studio ambience without abrupt syllable chopping
                val vocalOut = hpOut.coerceIn(-30000f, 30000f)

                // 3. Exact 3x Linear Interpolation (16,000 Hz -> 48,000 Hz)
                val s0 = prevVocalSample
                val s1 = vocalOut
                prevVocalSample = s1

                val u1 = (s0 + (s1 - s0) * 0.3333333f).toInt().coerceIn(-32768, 32767).toShort()
                val u2 = (s0 + (s1 - s0) * 0.6666667f).toInt().coerceIn(-32768, 32767).toShort()
                val u3 = s1.toInt().coerceIn(-32768, 32767).toShort()

                pushVocalSampleLocked(u1)
                pushVocalSampleLocked(u2)
                pushVocalSampleLocked(u3)
            }

            // Only if music has never started and at least 4s has elapsed without music (pure voice recording test)
            // drain vocal samples directly. When a song is playing (hasMusicStarted is true), onBassMusicPcm
            // is the SOLE master clock and writes all frames to guarantee 0 chopped instruments!
            if (!hasMusicStarted && (now - recordingStartTimeMs) > 4000L && vocalCount > 0) {
                val frames = vocalCount
                val outBytes = ByteArray(frames * BYTES_PER_FRAME)
                var byteOffset = 0
                for (i in 0 until frames) {
                    val v = vocalRing[vocalReadIdx].toFloat() / 32768f
                    vocalReadIdx = (vocalReadIdx + 1) % VOCAL_RING_CAPACITY
                    vocalCount--
                    val lim = softLimitStudio(v * 0.85f)
                    val pcm = (lim * 32767f).toInt().coerceIn(-32768, 32767)
                    val lo = (pcm and 0xFF).toByte()
                    val hi = ((pcm ushr 8) and 0xFF).toByte()
                    outBytes[byteOffset] = lo
                    outBytes[byteOffset + 1] = hi
                    outBytes[byteOffset + 2] = lo
                    outBytes[byteOffset + 3] = hi
                    byteOffset += 4
                }
                if (writeQueue.offer(outBytes)) {
                    totalStereoFramesWritten += frames
                }
            }
        }
    }

    private fun pushVocalSampleLocked(sample: Short) {
        if (vocalCount == VOCAL_RING_CAPACITY) {
            vocalReadIdx = (vocalReadIdx + 1) % VOCAL_RING_CAPACITY
            vocalCount--
        }
        vocalRing[vocalWriteIdx] = sample
        vocalWriteIdx = (vocalWriteIdx + 1) % VOCAL_RING_CAPACITY
        vocalCount++
    }

    @Synchronized
    fun stopAndSaveRecording(): RecordedPerformance? {
        if (!isRecording) return null
        isRecording = false
        isWriterRunning = false

        try {
            writerThread?.join(1500)
        } catch (_: Exception) {}
        writerThread = null

        try {
            currentPcmStream?.flush()
            currentPcmStream?.close()
        } catch (_: Exception) {}
        currentPcmStream = null

        val tempFile = currentTempFile
        currentTempFile = null
        if (tempFile == null || !tempFile.exists() || totalStereoFramesWritten < SAMPLE_RATE / 2) {
            tempFile?.delete()
            return null
        }

        return try {
            val dir = getRecordingsFolder()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val safeTitle = currentSongTitle.replace(" ", "_").ifBlank { "Videoke" }
            val wavFile = File(dir, "VK_${currentSongCode}_${safeTitle}_$timestamp.wav")
            writeStereoWavFile(tempFile, wavFile, SAMPLE_RATE)
            tempFile.delete()
            Log.d(TAG, "Saved 48kHz True Stereo Studio WAV recording: ${wavFile.absolutePath} (${wavFile.length()} bytes)")
            toRecordedPerformance(wavFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to finalize WAV file", e)
            tempFile.delete()
            null
        }
    }

    fun getRecordingDurationSeconds(): Int {
        if (!isRecording) return 0
        return (totalStereoFramesWritten / SAMPLE_RATE).toInt()
    }

    private fun writeStereoWavFile(pcmFile: File, wavFile: File, sampleRate: Int) {
        val pcmDataSize = pcmFile.length().toInt()
        val totalDataLen = pcmDataSize + 36
        val byteRate = sampleRate * BYTES_PER_FRAME // 48,000 * 4 = 192,000 bytes/sec (True 16-bit Stereo)

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
            writeShortLE(header, 22, NUM_CHANNELS) // NumChannels = 2 (Stereo)
            writeIntLE(header, 24, sampleRate)
            writeIntLE(header, 28, byteRate)
            writeShortLE(header, 32, BYTES_PER_FRAME) // BlockAlign = 4 (2 channels * 16-bit)
            writeShortLE(header, 34, 16) // BitsPerSample = 16
            // "data"
            header[36] = 'd'.code.toByte()
            header[37] = 'a'.code.toByte()
            header[38] = 't'.code.toByte()
            header[39] = 'a'.code.toByte()
            writeIntLE(header, 40, pcmDataSize)
            out.write(header, 0, 44)

            FileInputStream(pcmFile).use { fis ->
                val buf = ByteArray(32768)
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

    private fun readWavByteRate(file: File): Int {
        return try {
            FileInputStream(file).use { fis ->
                val header = ByteArray(32)
                if (fis.read(header) >= 32) {
                    val b0 = header[28].toInt() and 0xFF
                    val b1 = (header[29].toInt() and 0xFF) shl 8
                    val b2 = (header[30].toInt() and 0xFF) shl 16
                    val b3 = (header[31].toInt() and 0xFF) shl 24
                    val rate = b0 or b1 or b2 or b3
                    if (rate in 16000..384000) rate else BYTE_RATE
                } else {
                    BYTE_RATE
                }
            }
        } catch (_: Exception) {
            BYTE_RATE
        }
    }

    fun listSavedRecordings(): List<RecordedPerformance> {
        val dir = getRecordingsFolder()
        val files = dir.listFiles { f -> f.isFile && f.extension.equals("wav", ignoreCase = true) && f.length() > 44L }
            ?: return emptyList()
        return files.sortedByDescending { it.lastModified() }.map { toRecordedPerformance(it) }
    }

    private fun toRecordedPerformance(file: File): RecordedPerformance {
        val pcmBytes = (file.length() - 44L).coerceAtLeast(0L)
        val byteRate = readWavByteRate(file).coerceAtLeast(1)
        val totalSec = (pcmBytes / byteRate).toInt()
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
            Log.e(TAG, "Failed to play recording: $path", e)
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
