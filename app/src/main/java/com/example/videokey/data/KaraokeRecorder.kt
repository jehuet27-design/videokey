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

data class RecordedPerformance(
    val fileName: String,
    val songTitle: String,
    val dateLabel: String,
    val durationLabel: String,
    val filePath: String,
    val sizeKb: Long
)

/**
 * Studio-Grade Videoke Performance Recorder (True 48,000 Hz 2-Channel Stereo 16-bit PCM .WAV)
 * with Automatic Output-to-Input Vocal Latency Compensation:
 * - Solves vocal delay ("nahuhuli ang boses sa record") by compensating the BASS playback pre-buffer
 *   (~250ms) and hardware microphone input latency (~30-50ms) using a synchronized studio delay line.
 * - Streams 100% genuine 32-bit float stereo BASSMIDI SoundFont accompaniment after EQ & Mastering.
 * - Vocal sync offset is configurable (default 280ms) for sample-accurate timing on any device.
 * - Offloads all disk writes to a dedicated background writer thread so BASS DSP never blocks.
 */
class KaraokeRecorder {

    companion object {
        private const val TAG = "KaraokeRecorder"
        const val SAMPLE_RATE = 48000
        const val NUM_CHANNELS = 2 // True Stereo (Left + Right)
        const val BYTES_PER_FRAME = NUM_CHANNELS * 2 // 4 bytes per stereo 16-bit frame
        const val BYTE_RATE = SAMPLE_RATE * BYTES_PER_FRAME // 192,000 bytes/sec
        private const val MIC_INPUT_RATE = 16000
        private const val VOCAL_RING_CAPACITY = SAMPLE_RATE * 3 // 3 seconds of 48kHz mono vocal samples
        private const val MUSIC_RING_CAPACITY = SAMPLE_RATE * 2 // 2 seconds of 48kHz stereo music frames
        const val DEFAULT_VOCAL_SYNC_OFFSET_MS = 280 // Compensates BASS pre-buffer (~240ms) + mic latency (~40ms)
    }

    @Volatile
    var isRecording: Boolean = false
        private set

    @Volatile
    var vocalSyncOffsetMs: Int = DEFAULT_VOCAL_SYNC_OFFSET_MS

    private var currentPcmStream: BufferedOutputStream? = null
    private var currentTempFile: File? = null
    private var currentSongCode: String = ""
    private var currentSongTitle: String = ""
    @Volatile
    private var totalStereoFramesWritten: Long = 0L

    // Async disk writer queue
    private val writeQueue = LinkedBlockingQueue<ByteArray>(1024)
    private var writerThread: Thread? = null
    @Volatile
    private var isWriterRunning: Boolean = false

    // Music Delay Ring Buffer (holds stereo float samples: Left, Right)
    private val musicLock = Any()
    private val musicRing = FloatArray(MUSIC_RING_CAPACITY * 2)
    private var musicWriteIdx = 0
    private var musicReadIdx = 0
    private var musicFramesCount = 0

    // Lock-protected 48kHz mono vocal ring buffer
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

    // Microphone Vocal DSP state
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

            synchronized(musicLock) {
                musicWriteIdx = 0
                musicReadIdx = 0
                musicFramesCount = 0
            }

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

            Log.d(TAG, "Started 48kHz True Stereo Studio Recording (Latency Comp: ${vocalSyncOffsetMs}ms) for [$songCode] $songTitle -> ${tempFile.absolutePath}")
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
     * Called directly from [SoundEngine]'s BASS DSP callback with live 48,000 Hz stereo 32-bit float audio.
     * Uses a calibrated music delay line to ensure voice and music are mixed 100% in sync without vocal lag.
     */
    fun onBassMusicPcm(stereoFloats: FloatArray, floatCount: Int) {
        if (!isRecording) return
        val incomingFrames = floatCount / 2
        if (incomingFrames <= 0) return

        if (!hasMusicStarted) {
            hasMusicStarted = true
            // Discard any room noise / mic handling captured before the song actually began
            synchronized(vocalLock) {
                vocalReadIdx = 0
                vocalWriteIdx = 0
                vocalCount = 0
                lastHeldVocal = 0f
            }
        }
        lastMusicChunkTimeMs = System.currentTimeMillis()

        val targetDelayFrames = ((SAMPLE_RATE.toLong() * vocalSyncOffsetMs.coerceIn(0, 1000)) / 1000L).toInt()

        val framesToProcess: Int
        synchronized(musicLock) {
            // Push incoming stereo floats into music delay ring
            for (i in 0 until floatCount) {
                if (musicFramesCount >= MUSIC_RING_CAPACITY) {
                    musicReadIdx = (musicReadIdx + 2) % (MUSIC_RING_CAPACITY * 2)
                    musicFramesCount--
                }
                musicRing[musicWriteIdx] = stereoFloats[i]
                musicWriteIdx = (musicWriteIdx + 1) % (MUSIC_RING_CAPACITY * 2)
            }
            musicFramesCount += incomingFrames

            framesToProcess = (musicFramesCount - targetDelayFrames).coerceAtLeast(0)
        }

        if (framesToProcess <= 0) return

        val outBytes = ByteArray(framesToProcess * BYTES_PER_FRAME)
        var byteOffset = 0

        synchronized(vocalLock) {
            // Prevent vocal buffer backlog: keep vocal buffer within a tight ~40ms window
            val maxVocalLag = SAMPLE_RATE / 25
            if (vocalCount > maxVocalLag + framesToProcess) {
                val drop = vocalCount - (maxVocalLag + framesToProcess)
                vocalReadIdx = (vocalReadIdx + drop) % VOCAL_RING_CAPACITY
                vocalCount -= drop
            }

            synchronized(musicLock) {
                for (f in 0 until framesToProcess) {
                    val musicL = musicRing[musicReadIdx]
                    val musicR = musicRing[musicReadIdx + 1]
                    musicReadIdx = (musicReadIdx + 2) % (MUSIC_RING_CAPACITY * 2)
                    musicFramesCount--

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

                    // Soft-knee analog limiting (strictly prevents digital clipping / basag na tunog)
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
        }

        if (writeQueue.offer(outBytes)) {
            totalStereoFramesWritten += framesToProcess
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
     * Uses natural high-pass filtering (~80 Hz) and clean 3x interpolation to 48kHz.
     */
    fun onMicPcmChunk(micBuffer: ShortArray, readSize: Int) {
        if (!isRecording || readSize <= 0) return

        val now = System.currentTimeMillis()

        synchronized(vocalLock) {
            // If music hasn't started yet and less than 4 seconds elapsed, do not accumulate pre-song background noise
            if (!hasMusicStarted && (now - recordingStartTimeMs) < 4000L) {
                vocalCount = 0
                vocalReadIdx = 0
                vocalWriteIdx = 0
                return
            }

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
            if (!hasMusicStarted && (now - recordingStartTimeMs) >= 4000L && vocalCount > 0) {
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

    private fun flushRemainingFrames() {
        val remainingFrames = synchronized(musicLock) { musicFramesCount }
        if (remainingFrames <= 0) return

        val outBytes = ByteArray(remainingFrames * BYTES_PER_FRAME)
        var byteOffset = 0

        synchronized(vocalLock) {
            synchronized(musicLock) {
                for (f in 0 until remainingFrames) {
                    val musicL = musicRing[musicReadIdx]
                    val musicR = musicRing[musicReadIdx + 1]
                    musicReadIdx = (musicReadIdx + 2) % (MUSIC_RING_CAPACITY * 2)
                    musicFramesCount--

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

                    val mixL = musicL * 0.82f + vocalSample * 0.72f
                    val mixR = musicR * 0.82f + vocalSample * 0.72f

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
        }

        if (writeQueue.offer(outBytes)) {
            totalStereoFramesWritten += remainingFrames
        }
    }

    @Synchronized
    fun stopAndSaveRecording(): RecordedPerformance? {
        if (!isRecording) return null

        // Flush remaining buffered frames so the tail of the song is recorded
        flushRemainingFrames()

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
        // ONLY files inside /sdcard/VideoKey/recordings that were recorded by VideoKey!
        val files = dir.listFiles { f ->
            f.isFile && (
                (f.extension.equals("wav", ignoreCase = true) && f.length() > 44L) ||
                (f.extension.equals("mp4", ignoreCase = true) && f.name.startsWith("VideoKey_Screen_", ignoreCase = true) && f.length() > 1024L)
            )
        } ?: return emptyList()

        return files.sortedByDescending { it.lastModified() }.map { toRecordedPerformance(it) }
    }

    private fun toRecordedPerformance(file: File): RecordedPerformance {
        val isMp4 = file.extension.equals("mp4", ignoreCase = true)
        val durLabel = if (isMp4) {
            // Fast calculation from 6Mbps H264 bit rate (0ms UI latency, no native OOM/ANR)
            val approxSec = (file.length() / 780_000L).coerceAtLeast(1L).toInt()
            String.format(Locale.US, "%02d:%02d", approxSec / 60, approxSec % 60)
        } else {
            val pcmBytes = (file.length() - 44L).coerceAtLeast(0L)
            val byteRate = readWavByteRate(file).coerceAtLeast(1)
            val totalSec = (pcmBytes / byteRate).toInt()
            val mins = totalSec / 60
            val secs = totalSec % 60
            String.format(Locale.US, "%02d:%02d", mins, secs)
        }

        val dateLabel = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.US).format(Date(file.lastModified()))

        val parsedTitle = if (isMp4) {
            val raw = file.nameWithoutExtension.removePrefix("VideoKey_Screen_")
            "Screen Record ($raw)"
        } else {
            // Parse friendly song title from VK_<code>_<title>_<timestamp>.wav
            val parts = file.nameWithoutExtension.split("_")
            if (parts.size >= 4 && parts[0] == "VK") {
                val code = parts[1]
                val titlePart = parts.subList(2, parts.size - 2).joinToString(" ")
                "[$code] $titlePart"
            } else {
                file.nameWithoutExtension
            }
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
