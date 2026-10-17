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
 * Studio-Grade Videoke Performance Recorder (48,000 Hz 16-bit PCM .WAV):
 * - Captures 100% genuine 32-bit float BASSMIDI SoundFont (.sf2) accompaniment directly from [SoundEngine]
 *   after the 10-band Studio EQ, Compressor, Reverb, and Instrument Mixer.
 * - Mixes live microphone vocals (with high-pass rumble filter, smart noise gate, and warm karaoke echo)
 *   using a sample-accurate 48,000 Hz dual ring buffer and soft-knee studio limiter ("walang basag").
 */
class KaraokeRecorder {

    companion object {
        private const val TAG = "KaraokeRecorder"
        const val SAMPLE_RATE = 48000
        private const val MIC_INPUT_RATE = 16000
        private const val UPSAMPLE_FACTOR = SAMPLE_RATE / MIC_INPUT_RATE // Exact 3x (16kHz -> 48kHz)
        private const val RING_CAPACITY = SAMPLE_RATE * 3 // 3 seconds at 48kHz (144,000 samples)
    }

    @Volatile
    var isRecording: Boolean = false
        private set

    private var currentPcmStream: BufferedOutputStream? = null
    private var currentTempFile: File? = null
    private var currentSongCode: String = ""
    private var currentSongTitle: String = ""
    private var totalSamplesWritten: Long = 0L

    // Dual 48kHz Ring Buffers for Glitch-Free Sample-Accurate Music + Vocal Mixing
    private val musicRing = ShortArray(RING_CAPACITY)
    private var musicReadIdx = 0
    private var musicWriteIdx = 0
    private var musicCount = 0
    @Volatile
    private var lastMusicChunkTimeMs: Long = 0L

    private val vocalRing = ShortArray(RING_CAPACITY)
    private var vocalReadIdx = 0
    private var vocalWriteIdx = 0
    private var vocalCount = 0

    // Microphone Vocal DSP state (at 16kHz before 3x linear interpolation to 48kHz)
    private val echoDelaySamples = (MIC_INPUT_RATE * 0.175).toInt() // 175ms studio slapback reverb
    private val echoBuffer = ShortArray(echoDelaySamples)
    private var echoWriteIdx = 0
    private var echoLpState = 0f
    private var hpPrevIn = 0f
    private var hpPrevOut = 0f
    private var gateEnvelope = 0f
    private var prevVocalSample = 0f

    // Reusable output byte buffer
    private var mixByteBuffer = ByteArray(16384)

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
            currentPcmStream = BufferedOutputStream(FileOutputStream(tempFile), 32768)
            currentTempFile = tempFile
            currentSongCode = songCode
            currentSongTitle = cleanTitle
            totalSamplesWritten = 0L

            musicReadIdx = 0
            musicWriteIdx = 0
            musicCount = 0
            lastMusicChunkTimeMs = 0L

            vocalReadIdx = 0
            vocalWriteIdx = 0
            vocalCount = 0

            echoBuffer.fill(0)
            echoWriteIdx = 0
            echoLpState = 0f
            hpPrevIn = 0f
            hpPrevOut = 0f
            gateEnvelope = 0f
            prevVocalSample = 0f

            isRecording = true
            Log.d(TAG, "Started 48kHz Studio Recording for [$songCode] $songTitle -> ${tempFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            isRecording = false
            false
        }
    }

    fun updatePlaybackState(posMs: Long, keyTranspose: Int, accompNotes: List<MidiKaraokeParser.MelodyNote>? = null) {
        // Real BASSMIDI SoundFont PCM is streamed directly via onBassMusicPcm()
    }

    /**
     * Called directly from [SoundEngine]'s BASS DSP callback with live 48,000 Hz stereo 32-bit float
     * SoundFont (.sf2) audio after the 10-band Studio EQ, Compressor, and Instrument Mixer.
     */
    @Synchronized
    fun onBassMusicPcm(stereoFloats: FloatArray, floatCount: Int) {
        if (!isRecording) return
        val frames = floatCount / 2
        if (frames <= 0) return

        lastMusicChunkTimeMs = System.currentTimeMillis()

        for (i in 0 until frames) {
            val left = stereoFloats[i * 2]
            val right = stereoFloats[i * 2 + 1]
            // Balanced stereo-to-mono studio folddown with headroom
            val mono = ((left + right) * 0.56f).coerceIn(-1.0f, 1.0f)
            val pcm16 = (mono * 30500f).toInt().coerceIn(-32768, 32767).toShort()

            if (musicCount == RING_CAPACITY) {
                // Drop oldest sample if ring buffer ever fills
                musicReadIdx = (musicReadIdx + 1) % RING_CAPACITY
                musicCount--
            }
            musicRing[musicWriteIdx] = pcm16
            musicWriteIdx = (musicWriteIdx + 1) % RING_CAPACITY
            musicCount++
        }

        drainAndMixLocked()
    }

    /**
     * Called from [MicrophoneScorer]'s audio loop with live 16,000 Hz 16-bit PCM microphone samples.
     * Applies studio high-pass filter, smart noise gate, and warm karaoke echo, then upsamples 3x
     * to 48,000 Hz to mix seamlessly with the BASSMIDI SoundFont audio.
     */
    @Synchronized
    fun onMicPcmChunk(micBuffer: ShortArray, readSize: Int) {
        if (!isRecording || readSize <= 0) return

        // Measure chunk RMS to drive the smooth vocal noise gate
        var sumSq = 0.0
        for (i in 0 until readSize) {
            val s = micBuffer[i].toDouble()
            sumSq += s * s
        }
        val rms = sqrt(sumSq / readSize)
        val targetGate = when {
            rms > 320.0 -> 1.0f
            rms > 140.0 -> ((rms - 140.0) / 180.0).toFloat().coerceIn(0.15f, 1.0f)
            else -> 0.08f // Keep room noise floor ultra-quiet so SoundFont music stays crystal-clear
        }

        for (i in 0 until readSize) {
            gateEnvelope = gateEnvelope * 0.985f + targetGate * 0.015f

            // 1. High-pass filter (~95 Hz) to eliminate mic handling thumps and plosive rumble
            val rawIn = micBuffer[i].toFloat()
            val hpOut = 0.962f * (hpPrevOut + rawIn - hpPrevIn)
            hpPrevIn = rawIn
            hpPrevOut = hpOut

            val gatedVocal = hpOut * gateEnvelope

            // 2. Warm Studio Karaoke Echo (175ms delay with low-pass warmth damping in feedback)
            val delayed = echoBuffer[echoWriteIdx].toFloat()
            echoLpState = echoLpState * 0.45f + delayed * 0.55f
            val vocalOut = (gatedVocal * 1.25f + echoLpState * 0.30f).coerceIn(-32000f, 32000f)

            val nextFeedback = (gatedVocal + echoLpState * 0.26f).coerceIn(-32000f, 32000f).toInt().toShort()
            echoBuffer[echoWriteIdx] = nextFeedback
            echoWriteIdx = (echoWriteIdx + 1) % echoDelaySamples

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

        drainAndMixLocked()
    }

    private fun pushVocalSampleLocked(sample: Short) {
        if (vocalCount == RING_CAPACITY) {
            vocalReadIdx = (vocalReadIdx + 1) % RING_CAPACITY
            vocalCount--
        }
        vocalRing[vocalWriteIdx] = sample
        vocalWriteIdx = (vocalWriteIdx + 1) % RING_CAPACITY
        vocalCount++
    }

    /**
     * Drains matched 48,000 Hz samples from [musicRing] and [vocalRing], mixes them through a
     * soft-knee studio mastering limiter, and writes 16-bit PCM to disk without a single click or gap.
     */
    private fun drainAndMixLocked() {
        val out = currentPcmStream ?: return
        val now = System.currentTimeMillis()
        val isMusicStreaming = (now - lastMusicChunkTimeMs) < 350L

        try {
            if (isMusicStreaming) {
                // Keep vocal buffer tightly aligned with music buffer (discard pre-song vocal backlog > 90ms)
                val maxLeadSamples = SAMPLE_RATE / 11 // ~4,360 samples (~90ms)
                if (vocalCount - musicCount > maxLeadSamples) {
                    val drop = vocalCount - musicCount - (SAMPLE_RATE / 25)
                    if (drop > 0) {
                        vocalReadIdx = (vocalReadIdx + drop) % RING_CAPACITY
                        vocalCount -= drop
                    }
                }

                // If microphone capture ever lags behind BASSMIDI by > 250ms, pad vocal with silence so music never stalls
                if (musicCount - vocalCount > SAMPLE_RATE / 4) {
                    val pad = musicCount - vocalCount
                    for (p in 0 until pad) {
                        pushVocalSampleLocked(0)
                    }
                }

                val framesToWrite = minOf(musicCount, vocalCount)
                if (framesToWrite <= 0) return

                val byteLen = framesToWrite * 2
                if (mixByteBuffer.size < byteLen) {
                    mixByteBuffer = ByteArray(byteLen)
                }

                for (i in 0 until framesToWrite) {
                    val m = musicRing[musicReadIdx].toFloat() / 32768f
                    musicReadIdx = (musicReadIdx + 1) % RING_CAPACITY
                    musicCount--

                    val v = vocalRing[vocalReadIdx].toFloat() / 32768f
                    vocalReadIdx = (vocalReadIdx + 1) % RING_CAPACITY
                    vocalCount--

                    // Soft-knee studio mastering limiter so music + vocals never clip ("walang basag")
                    val sum = m * 0.96f + v * 0.90f
                    val limited = sum / (1.0f + abs(sum) * 0.22f)
                    val pcm = (limited * 32767f).toInt().coerceIn(-32768, 32767)

                    mixByteBuffer[i * 2] = (pcm and 0xFF).toByte()
                    mixByteBuffer[i * 2 + 1] = ((pcm ushr 8) and 0xFF).toByte()
                }

                out.write(mixByteBuffer, 0, byteLen)
                totalSamplesWritten += framesToWrite
            } else {
                // Song is paused or not playing: drain any remaining music + vocals
                val framesToWrite = maxOf(musicCount, vocalCount)
                if (framesToWrite <= 0) return

                val byteLen = framesToWrite * 2
                if (mixByteBuffer.size < byteLen) {
                    mixByteBuffer = ByteArray(byteLen)
                }

                for (i in 0 until framesToWrite) {
                    val m = if (musicCount > 0) {
                        val s = musicRing[musicReadIdx].toFloat() / 32768f
                        musicReadIdx = (musicReadIdx + 1) % RING_CAPACITY
                        musicCount--
                        s
                    } else 0f

                    val v = if (vocalCount > 0) {
                        val s = vocalRing[vocalReadIdx].toFloat() / 32768f
                        vocalReadIdx = (vocalReadIdx + 1) % RING_CAPACITY
                        vocalCount--
                        s
                    } else 0f

                    val sum = m * 0.96f + v * 0.95f
                    val limited = sum / (1.0f + abs(sum) * 0.22f)
                    val pcm = (limited * 32767f).toInt().coerceIn(-32768, 32767)

                    mixByteBuffer[i * 2] = (pcm and 0xFF).toByte()
                    mixByteBuffer[i * 2 + 1] = ((pcm ushr 8) and 0xFF).toByte()
                }

                out.write(mixByteBuffer, 0, byteLen)
                totalSamplesWritten += framesToWrite
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing mixed PCM chunk", e)
        }
    }

    @Synchronized
    fun stopAndSaveRecording(): RecordedPerformance? {
        if (!isRecording) return null
        // Flush any remaining buffered samples before closing
        lastMusicChunkTimeMs = 0L
        drainAndMixLocked()
        isRecording = false
        try {
            currentPcmStream?.flush()
            currentPcmStream?.close()
        } catch (_: Exception) {}
        currentPcmStream = null

        val tempFile = currentTempFile
        currentTempFile = null
        if (tempFile == null || !tempFile.exists() || totalSamplesWritten < SAMPLE_RATE / 2) {
            tempFile?.delete()
            return null
        }

        return try {
            val dir = getRecordingsFolder()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val safeTitle = currentSongTitle.replace(" ", "_").ifBlank { "Videoke" }
            val wavFile = File(dir, "VK_${currentSongCode}_${safeTitle}_$timestamp.wav")
            writeWavFile(tempFile, wavFile, SAMPLE_RATE)
            tempFile.delete()
            Log.d(TAG, "Saved 48kHz Studio WAV recording: ${wavFile.absolutePath} (${wavFile.length()} bytes)")
            toRecordedPerformance(wavFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to finalize WAV file", e)
            tempFile.delete()
            null
        }
    }

    fun getRecordingDurationSeconds(): Int {
        if (!isRecording) return 0
        return (totalSamplesWritten / SAMPLE_RATE).toInt()
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
                val buf = ByteArray(16384)
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
                    if (rate in 16000..384000) rate else (SAMPLE_RATE * 2)
                } else {
                    SAMPLE_RATE * 2
                }
            }
        } catch (_: Exception) {
            SAMPLE_RATE * 2
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
