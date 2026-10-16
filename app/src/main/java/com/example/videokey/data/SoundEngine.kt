package com.example.videokey.data

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.os.Environment
import android.util.Log
import java.io.File
import kotlin.math.pow

/**
 * High-performance Videoke Sound Engine:
 * - Real-time MIDI accompaniment playback via Android MediaPlayer (hardware GM wavetable)
 * - Pitch transpose (-6 to +6 semitones)
 * - Tempo scaling (70% to 130%)
 * - Millisecond synchronization for 2-line yellow lyric wipes
 */
class SoundEngine(private val context: Context) {

    data class SoundFontPreset(
        val id: String,
        val displayName: String,
        val fileName: String,
        val description: String
    )

    val availableSoundFonts = listOf(
        SoundFontPreset(
            id = "dreamsam",
            displayName = "Dream SAM2634 v2.1",
            fileName = "Dream SAM2634 (ver. 2.1).sf2",
            description = "Dream hardware synthesizer soundchip"
        ),
        SoundFontPreset(
            id = "mega",
            displayName = "MegaPro Sound",
            fileName = "mega.sf2",
            description = "Megapro karaoke player soundfont"
        )
    )

    private var activeSoundFont: SoundFontPreset = availableSoundFonts.first()
    private var activeSoundFontName: String = activeSoundFont.displayName
    private var mediaPlayer: MediaPlayer? = null
    private var equalizer: android.media.audiofx.Equalizer? = null
    private var bassBoost: android.media.audiofx.BassBoost? = null
    private var virtualizer: android.media.audiofx.Virtualizer? = null
    private var presetReverb: android.media.audiofx.PresetReverb? = null
    private var currentMidiFile: File? = null
    private var keyTranspose: Int = 0 // semitones (-6 to +6)
    private var tempoPercent: Int = 100 // 70% to 130%
    private var micEchoPercent: Int = 50 // 0% to 100%
    private var melodyPercent: Int = 50 // Default 50% (0% to 100%)

    private val midiIndexByCode = java.util.concurrent.ConcurrentHashMap<String, File>()
    private val midiIndexByTitle = java.util.concurrent.ConcurrentHashMap<String, File>()
    @Volatile private var isIndexingStarted = false
    @Volatile private var isIndexReady = false

    init {
        buildMidiIndexAsync()
    }

    fun buildMidiIndexAsync() {
        if (isIndexingStarted) return
        isIndexingStarted = true
        Thread {
            buildMidiIndexSync()
        }.start()
    }

    @Synchronized
    private fun buildMidiIndexSync() {
        try {
            val root = Environment.getExternalStorageDirectory()
            val candidateDirs = listOf(
                File(root, "VideoKey/songs"),
                File("/storage/emulated/0/VideoKey/songs"),
                File(root, "VideoKey"),
                File(root, "Music/songs"),
                File(root, "Music"),
                File(root, "Download"),
                File("/storage/emulated/0/Download")
            )
            val codeRegex = Regex("""^(\d{4,6})""")
            for (dir in candidateDirs) {
                if (dir.exists() && dir.isDirectory) {
                    val files = dir.listFiles() ?: continue
                    for (f in files) {
                        if (!f.isFile) continue
                        val ext = f.extension.lowercase()
                        if (ext != "mid" && ext != "kar") continue

                        val name = f.name
                        val match = codeRegex.find(name)
                        if (match != null) {
                            val code = match.groupValues[1]
                            midiIndexByCode.putIfAbsent(code, f)
                            val unpadded = code.trimStart('0')
                            if (unpadded.isNotBlank()) {
                                midiIndexByCode.putIfAbsent(unpadded, f)
                            }
                        }

                        val cleanTitle = f.nameWithoutExtension.replace(Regex("""[^a-zA-Z0-9]"""), "").lowercase().take(15)
                        if (cleanTitle.length >= 3) {
                            midiIndexByTitle.putIfAbsent(cleanTitle, f)
                        }
                    }
                }
            }
            isIndexReady = true
            Log.d("SoundEngine", "MIDI Index built with ${midiIndexByCode.size} code entries and ${midiIndexByTitle.size} title entries")
        } catch (e: Exception) {
            Log.e("SoundEngine", "Error building MIDI index", e)
        }
    }

    fun indexFolderMidi(folder: File) {
        if (!folder.exists() || !folder.isDirectory) return
        val codeRegex = Regex("""^(\d{4,6})""")
        try {
            val allFiles = try {
                folder.walkTopDown().maxDepth(3).filter { it.isFile && (it.extension.equals("mid", true) || it.extension.equals("kar", true)) }.toList()
            } catch (e: Exception) {
                folder.listFiles()?.filter { it.isFile && (it.extension.equals("mid", true) || it.extension.equals("kar", true)) } ?: emptyList()
            }
            for (f in allFiles) {
                val name = f.name
                val match = codeRegex.find(name)
                if (match != null) {
                    val code = match.groupValues[1]
                    midiIndexByCode[code] = f
                    val unpadded = code.trimStart('0')
                    if (unpadded.isNotBlank()) {
                        midiIndexByCode[unpadded] = f
                    }
                }
                val cleanTitle = f.nameWithoutExtension.replace(Regex("""[^a-zA-Z0-9]"""), "").lowercase().take(15)
                if (cleanTitle.length >= 3) {
                    midiIndexByTitle[cleanTitle] = f
                }
            }
            Log.d("SoundEngine", "Indexed folder ${folder.absolutePath}: index has ${midiIndexByCode.size} code entries")
        } catch (e: Exception) {
            Log.e("SoundEngine", "Error indexing folder MIDI", e)
        }
    }

    fun playMidi(
        filePath: String,
        deleteAfterOpen: Boolean = false,
        resumePositionMs: Long = 0L,
        onCompletion: (() -> Unit)? = null
    ): Boolean {
        val file = File(filePath)
        if (!file.exists() || !file.canRead()) {
            Log.e("SoundEngine", "MIDI file not found or unreadable: $filePath")
            return false
        }

        try {
            stop()
            currentMidiFile = file
            mediaPlayer = MediaPlayer().apply {
                if (deleteAfterOpen) {
                    // Open via file descriptor, then remove the decrypted file right away.
                    // The player keeps reading from the open descriptor; nothing stays on disk.
                    java.io.FileInputStream(file).use { fis -> setDataSource(fis.fd) }
                    file.delete()
                } else {
                    setDataSource(file.absolutePath)
                }
                setOnPreparedListener { mp ->
                    applySoundFontDsp(mp.audioSessionId)
                    applyPlaybackParams()
                    if (resumePositionMs > 0L && resumePositionMs < mp.duration - 200) {
                        mp.seekTo(resumePositionMs.toInt())
                    }
                    mp.start()
                    Log.d("SoundEngine", "MIDI Playback started with SoundFont [$activeSoundFontName]: ${file.name}, duration=${mp.duration}ms, resumePos=${resumePositionMs}ms")
                }
                setOnCompletionListener {
                    Log.d("SoundEngine", "MIDI Playback completed: ${file.name}")
                    onCompletion?.invoke()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("SoundEngine", "MediaPlayer error: what=$what, extra=$extra")
                    true // Consume error so onCompletionListener is not falsely triggered
                }
                prepareAsync()
            }
            return true
        } catch (e: Exception) {
            Log.e("SoundEngine", "Failed to start MIDI playback", e)
            return false
        }
    }

    private fun releaseAudioEffects() {
        try {
            equalizer?.release()
        } catch (_: Exception) {}
        equalizer = null
        try {
            bassBoost?.release()
        } catch (_: Exception) {}
        bassBoost = null
        try {
            virtualizer?.release()
        } catch (_: Exception) {}
        virtualizer = null
        try {
            presetReverb?.release()
        } catch (_: Exception) {}
        presetReverb = null
    }

    private fun applySoundFontDsp(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        releaseAudioEffects()
        val profile = MidiMelodyScaler.resolveProfile(activeSoundFontName)
        try {
            val eq = android.media.audiofx.Equalizer(0, audioSessionId)
            eq.enabled = true
            val numBands = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            val minMb = range[0].toInt()
            val maxMb = range[1].toInt()
            for (b in 0 until numBands) {
                val targetMb = profile.eqBandOffsetsMb.getOrElse(b) { 0 }.coerceIn(minMb, maxMb)
                eq.setBandLevel(b.toShort(), targetMb.toShort())
            }
            equalizer = eq
        } catch (e: Exception) {
            Log.w("SoundEngine", "Equalizer DSP not supported on session $audioSessionId", e)
        }
        try {
            val bb = android.media.audiofx.BassBoost(0, audioSessionId)
            if (bb.strengthSupported) {
                bb.setStrength(profile.bassBoostPerMille.coerceIn(0, 1000).toShort())
            }
            bb.enabled = true
            bassBoost = bb
        } catch (e: Exception) {
            Log.w("SoundEngine", "BassBoost DSP not supported", e)
        }
        try {
            val virt = android.media.audiofx.Virtualizer(0, audioSessionId)
            if (virt.strengthSupported) {
                virt.setStrength(profile.virtualizerPerMille.coerceIn(0, 1000).toShort())
            }
            virt.enabled = true
            virtualizer = virt
        } catch (e: Exception) {
            Log.w("SoundEngine", "Virtualizer DSP not supported", e)
        }
        try {
            val rev = android.media.audiofx.PresetReverb(0, audioSessionId)
            rev.preset = profile.reverbPreset
            rev.enabled = true
            presetReverb = rev
            Log.d("SoundEngine", "Applied PresetReverb preset ${profile.reverbPreset} for ${profile.name}")
        } catch (e: Exception) {
            Log.w("SoundEngine", "PresetReverb DSP not supported", e)
        }
    }

    fun pause() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (e: Exception) {
            Log.e("SoundEngine", "Error pausing playback", e)
        }
    }

    fun resume() {
        try {
            if (mediaPlayer != null && !mediaPlayer!!.isPlaying) {
                mediaPlayer?.start()
            }
        } catch (e: Exception) {
            Log.e("SoundEngine", "Error resuming playback", e)
        }
    }

    fun stop() {
        releaseAudioEffects()
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.reset()
                it.release()
            }
        } catch (e: Exception) {
            Log.e("SoundEngine", "Error stopping playback", e)
        } finally {
            mediaPlayer = null
        }
    }

    fun isPlaying(): Boolean {
        return try {
            mediaPlayer?.isPlaying == true
        } catch (e: Exception) {
            false
        }
    }

    fun getCurrentPositionMs(): Long {
        return try {
            mediaPlayer?.currentPosition?.toLong() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    fun getDurationMs(): Long {
        return try {
            mediaPlayer?.duration?.toLong() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    fun setKeyTranspose(semitones: Int) {
        keyTranspose = semitones.coerceIn(-6, 6)
        applyPlaybackParams()
    }

    fun getKeyTranspose(): Int = keyTranspose

    fun setTempo(percent: Int) {
        tempoPercent = percent.coerceIn(70, 130)
        applyPlaybackParams()
    }

    fun getTempo(): Int = tempoPercent

    private fun applyPlaybackParams() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mediaPlayer != null) {
                // Pitch: 2^(semitones / 12)
                val pitch = 2.0f.pow(keyTranspose / 12.0f)
                val speed = tempoPercent / 100.0f
                val params = PlaybackParams().apply {
                    setPitch(pitch)
                    setSpeed(speed)
                }
                mediaPlayer?.playbackParams = params
                Log.d("SoundEngine", "Applied PlaybackParams: pitch=$pitch ($keyTranspose semitones), speed=$speed")
            }
        } catch (e: Exception) {
            Log.e("SoundEngine", "Error applying PlaybackParams", e)
        }
    }

    fun setMicEcho(percent: Int) {
        micEchoPercent = percent.coerceIn(0, 100)
    }

    fun getMicEcho(): Int = micEchoPercent

    fun setMelodyVolume(percent: Int) {
        melodyPercent = percent.coerceIn(0, 100)
    }

    fun getMelodyVolume(): Int = melodyPercent

    fun getActiveSoundFont(): SoundFontPreset = activeSoundFont

    fun setActiveSoundFont(presetId: String) {
        activeSoundFontName = presetId
        val found = availableSoundFonts.find {
            it.id.equals(presetId, ignoreCase = true) ||
            presetId.lowercase().contains(it.id.lowercase()) ||
            it.displayName.lowercase().contains(presetId.lowercase()) ||
            it.fileName.lowercase().contains(presetId.lowercase())
        }
        if (found != null) {
            activeSoundFont = found
        }
        try {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    applySoundFontDsp(mp.audioSessionId)
                }
            }
        } catch (e: Exception) {
            Log.w("SoundEngine", "Failed to hot-apply SoundFont DSP: ${e.message}")
        }
    }

    /**
     * Resolves the best available MIDI file for playback:
     * 1. Exact file path if it's a valid MIDI file
     * 2. File matching song code in VideoKey/songs or Music
     * 3. Fallback to any available MIDI in VideoKey/songs so music ALWAYS plays!
     */
    /**
     * Resolves the exact MIDI file for playback:
     * 1. Exact direct file path
     * 2. Instant O(1) indexed lookup by song code
     * 3. Instant O(1) indexed lookup by song title
     * NEVER blocks the UI thread with directory walking!
     */
    fun findMidiFileForSong(song: Song): File? {
        // 1. Direct path check
        if (song.filePath.isNotBlank()) {
            val direct = File(song.filePath)
            if (direct.exists() && (direct.extension.equals("mid", true) || direct.extension.equals("kar", true))) {
                return direct
            }
        }

        // 2. Fast indexed lookup by code
        val trimmedCode = song.code.trim()
        midiIndexByCode[trimmedCode]?.let { return it }

        val cleanCode = trimmedCode.trimStart('0')
        if (cleanCode.isNotBlank()) {
            midiIndexByCode[cleanCode]?.let { return it }
        }

        val asInt = trimmedCode.toIntOrNull()
        if (asInt != null) {
            val padded = String.format("%06d", asInt)
            midiIndexByCode[padded]?.let { return it }
        }

        // 3. Fast indexed lookup by clean title
        val cleanTitle = song.title.replace(Regex("""[^a-zA-Z0-9]"""), "").lowercase().take(15)
        if (cleanTitle.length >= 3) {
            midiIndexByTitle[cleanTitle]?.let { return it }
            val partial = midiIndexByTitle.entries.firstOrNull { it.key.contains(cleanTitle) || cleanTitle.contains(it.key) }
            if (partial != null) return partial.value
        }

        // 4. If not yet indexed, index once synchronously (only if initial background index is still completing)
        if (!isIndexReady) {
            buildMidiIndexSync()
            midiIndexByCode[trimmedCode]?.let { return it }
            if (cleanCode.isNotBlank()) midiIndexByCode[cleanCode]?.let { return it }
            if (cleanTitle.length >= 3) {
                midiIndexByTitle[cleanTitle]?.let { return it }
                val partial = midiIndexByTitle.entries.firstOrNull { it.key.contains(cleanTitle) || cleanTitle.contains(it.key) }
                if (partial != null) return partial.value
            }
        }

        // Do NOT use a random fallback so the user never hears the wrong song!
        Log.w("SoundEngine", "No exact MIDI file found for song: ${song.code} - ${song.title}")
        return null
    }

    fun hasMidiForSong(song: Song): Boolean {
        if (song.isPacked) return true
        if (song.sectorOffset > 0L) return true
        if (song.filePath.isNotBlank()) {
            val direct = File(song.filePath)
            if (direct.exists() && (direct.extension.equals("mid", true) || direct.extension.equals("kar", true))) {
                return true
            }
        }
        val trimmedCode = song.code.trim()
        if (midiIndexByCode.containsKey(trimmedCode) || midiIndexByCode.containsKey(trimmedCode.trimStart('0'))) return true
        val asInt = trimmedCode.toIntOrNull()
        if (asInt != null && midiIndexByCode.containsKey(String.format("%06d", asInt))) return true
        val cleanTitle = song.title.replace(Regex("""[^a-zA-Z0-9]"""), "").lowercase().take(15)
        if (cleanTitle.length >= 3 && (midiIndexByTitle.containsKey(cleanTitle) || midiIndexByTitle.entries.any { it.key.contains(cleanTitle) })) return true
        return false
    }

    fun getPlayableCodes(): Set<String> = midiIndexByCode.keys
}

