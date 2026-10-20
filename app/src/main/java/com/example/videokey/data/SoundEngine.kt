package com.example.videokey.data

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.un4seen.bass.BASS
import com.un4seen.bass.BASSMIDI
import com.un4seen.bass.BASS_FX
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.pow

/**
 * Studio-Grade Videoke SoundFont (.sf2) Audio Engine powered by Un4seen BASS + BASSMIDI + BASS_FX:
 * - True hardware-accelerated 32-bit floating-point SoundFont (.sf2) synthesis (256 voices, sinc interpolation)
 * - Preserves 100% of the song's original arranged GM/GS/XG instruments and velocity layers
 * - Real-time Instrument Mixer / Adjuster (Melody Guide, Drums, Bass, Guitar/Piano, Strings/Brass, Reverb)
 * - Studio Mastering 10-band EQ + BASS_BFX_COMPRESSOR2 anti-clipping limiter ("hindi basag kahit malakas")
 * - Zero-latency real-time Key Transpose (-6 to +6 semitones, melodic channels only) & Tempo (70% to 130%)
 */
class SoundEngine(
    private val context: Context,
    private val karaokeRecorder: KaraokeRecorder? = null
) {

    data class SoundFontPreset(
        val id: String,
        val displayName: String,
        val fileName: String,
        val description: String,
        val genreTag: String = "ALL"
    )

    val availableSoundFonts = listOf(
        SoundFontPreset(
            id = "auto_genre",
            displayName = "✨ Awtomatiko (Depende sa Genre)",
            fileName = "auto",
            description = "Matalinong pagpili ng SoundFont batay sa genre ng kanta (Rock, Ballad, Pop, Disco, atbp.)",
            genreTag = "AUTO"
        ),
        SoundFontPreset(
            id = "videokey_pop",
            displayName = "🎵 VideoKey OPM & Pop Studio",
            fileName = "VideoKey_Studio_Minus1.sf2",
            description = "Balanced Studio Minus-One para sa OPM, Pop, at Billboard charts",
            genreTag = "POP"
        ),
        SoundFontPreset(
            id = "videokey_rock",
            displayName = "🎸 VideoKey Rock & Band Studio",
            fileName = "VideoKey_Studio_Minus1.sf2",
            description = "Lakas ng live rock band: malutong na electric guitar, driving bass, at punchy snare",
            genreTag = "ROCK"
        ),
        SoundFontPreset(
            id = "videokey_acoustic",
            displayName = "🎹 VideoKey Acoustic & Ballad Studio",
            fileName = "VideoKey_Studio_Minus1.sf2",
            description = "Concert Grand Piano, Nylon Guitar, at lush warm strings para sa Love Songs",
            genreTag = "BALLAD"
        ),
        SoundFontPreset(
            id = "videokey_disco",
            displayName = "🪩 VideoKey Disco & 80s/90s Synth",
            fileName = "Dream SAM2634 (ver. 2.1).sf2",
            description = "Classic hardware videoke synthesizer soundchip, bouncy dance beat, at synth bass",
            genreTag = "DANCE"
        ),
        SoundFontPreset(
            id = "videokey_chacha",
            displayName = "🎺 VideoKey Cha-Cha & Ballroom",
            fileName = "Dream SAM2634 (ver. 2.1).sf2",
            description = "Crisp Latin percussion, lively brass section, at classic ballroom videoke groove",
            genreTag = "BALLROOM"
        ),
        SoundFontPreset(
            id = "videokey_party",
            displayName = "🥁 VideoKey Party & Mega Beat",
            fileName = "mega.sf2",
            description = "High-energy party sound, dynamic brass section, at punchy sub-bass",
            genreTag = "PARTY"
        ),
        SoundFontPreset(
            id = "yamaha_korg_ketron",
            displayName = "Yamaha x Korg x Ketron Studio",
            fileName = "yamaha x korg x ketron x dream sam (custom made by jc).sf2",
            description = "905MB Multi-Sampled Live Acoustic & Electric Band",
            genreTag = "STUDIO"
        ),
        SoundFontPreset(
            id = "dreamsam",
            displayName = "Dream SAM2634 v2.1",
            fileName = "Dream SAM2634 (ver. 2.1).sf2",
            description = "Dream Hardware Synthesizer Soundchip",
            genreTag = "SYNTH"
        ),
        SoundFontPreset(
            id = "mega",
            displayName = "MegaPro Sound",
            fileName = "mega.sf2",
            description = "MegaPro Karaoke Player SoundFont",
            genreTag = "MEGAPRO"
        )
    )

    companion object {
        private const val TAG = "SoundEngine"
        private const val SAMPLE_RATE = 48000
        private val EQ_FREQUENCIES = floatArrayOf(31f, 62f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)
        @Volatile private var bassInitialized = false
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var activeSoundFont: SoundFontPreset = availableSoundFonts.first()
    private var activeSoundFontName: String = activeSoundFont.displayName

    // Native BASS / BASSMIDI handles
    private var midiStream: Int = 0
    private var primaryFontHandle: Int = 0
    private var primaryFontPath: String = ""
    private var fallbackFontHandle: Int = 0
    private var fallbackFontPath: String = ""
    private val eqFxHandles = IntArray(10)
    private var compressorFxHandle: Int = 0
    private var autoVolumeDspHandle: Int = 0
    private var recordDspHandle: Int = 0

    // Auto-Volume / AGC (Equal Loudness Normalizer):
    @Volatile
    private var isAutoVolumeEnabled: Boolean = true
    @Volatile
    private var isAutoMelodyNormalizeEnabled: Boolean = true
    @Volatile
    private var currentAgcGain: Float = 1.0f
    @Volatile
    private var targetAgcGain: Float = 1.0f
    private val TARGET_RMS = 0.22f // ~ -13.1 dBFS (ideal fullness and clarity)
    private val SILENCE_RMS_FLOOR = 0.015f // -36.5 dBFS (noise gate freeze: does not amplify silence)
    private val MIN_AGC_GAIN = 0.55f // -5.2 dB (attenuation for very loud tracks)
    private val MAX_AGC_GAIN = 2.40f // +7.6 dB (clean boost for quiet tracks)
    private var dspFloatArray = FloatArray(8192)
    private var endSyncHandle: Int = 0

    // Fallback MediaPlayer only if native library is ever unavailable
    private var fallbackPlayer: MediaPlayer? = null

    private var currentMidiFile: File? = null
    private var currentAnalysis: MidiMelodyScaler.MidiAnalysis? = null
    private val baseChannelVolumes = IntArray(16) { 100 }
    private val liveChannelPrograms = IntArray(16) { 0 }
    private val liveDrumChannels = BooleanArray(16) { it == 9 }

    @Volatile private var lastKnownPositionMs: Long = 0L
    @Volatile private var cachedDurationMs: Long = 0L
    private var onCompletionCallback: (() -> Unit)? = null

    // Real-time Playback & Instrument Mixer state
    private var keyTranspose: Int = 0          // -6 to +6 semitones
    private var tempoPercent: Int = 100        // 70% to 130%
    private var micEchoPercent: Int = 50       // 0% to 100%
    private var melodyPercent: Int = 60        // 0% to 100% (Vocal Guide)
    private var drumsPercent: Int = 100        // 0% to 150% (Drums / Percussion)
    private var bassPercent: Int = 100         // 0% to 150% (Bass Guitar)
    private var guitarPianoPercent: Int = 100  // 0% to 150% (Guitar, Piano & Organ)
    private var stringsBrassPercent: Int = 100 // 0% to 150% (Strings, Brass, Sax & Synth)
    private var reverbPercent: Int = 50        // 0% to 100% (Studio Reverb / Ambience)
    private var isAutoMixerEnabled: Boolean = true // Smart Auto-Mixer based on song analysis

    // Real-time BASSMIDI event filter to keep custom instrument mix & drum preset fallbacks locked
    private val midiFilterProc = BASSMIDI.MIDIFILTERPROC { handle, track, event, _, _ ->
        if (event == null) return@MIDIFILTERPROC true
        val ch = event.chan
        if (ch !in 0..15) return@MIDIFILTERPROC true

        when (event.event) {
            BASSMIDI.MIDI_EVENT_DRUMS -> {
                liveDrumChannels[ch] = event.param > 0
            }
            BASSMIDI.MIDI_EVENT_PROGRAM -> {
                val prog = event.param and 0x7F
                liveChannelPrograms[ch] = prog
                if (ch == 9 || liveDrumChannels[ch]) {
                    // Ensure drum kit preset exists in the loaded SoundFont; if not, fallback to Standard Kit (0)
                    val fontToCheck = if (primaryFontHandle != 0) primaryFontHandle else fallbackFontHandle
                    if (fontToCheck != 0) {
                        val has128 = BASSMIDI.BASS_MIDI_FontGetPreset(fontToCheck, prog, 128) != null
                        val has127 = BASSMIDI.BASS_MIDI_FontGetPreset(fontToCheck, prog, 127) != null
                        if (!has128 && !has127) {
                            event.param = 0
                        }
                    }
                }
            }
            BASSMIDI.MIDI_EVENT_VOLUME -> {
                if (track != -1) {
                    val rawVol = event.param.coerceIn(0, 127)
                    baseChannelVolumes[ch] = if (rawVol > 0) rawVol else 100
                    event.param = computeScaledChannelVolume(ch, baseChannelVolumes[ch])
                }
            }
            BASSMIDI.MIDI_EVENT_NOTE -> {
                if (track != -1) {
                    val analysis = currentAnalysis
                    val isMelody = (analysis != null) && (
                        (analysis.numTracks > 1 && analysis.melodyTracks.contains(track)) ||
                        (analysis.exclusiveMelodyChannels.contains(ch)) ||
                        (analysis.numTracks <= 1 && analysis.melodyChannels.contains(ch))
                    )
                    if (isMelody) {
                        val key = event.param and 0xFF
                        val vel = (event.param shr 8) and 0xFF
                        if (vel in 1..127) {
                            if (melodyPercent <= 0) {
                                return@MIDIFILTERPROC false
                            }
                            val effectiveVel = if (isAutoMelodyNormalizeEnabled) {
                                normalizeMelodyVelocity(vel, ch, liveChannelPrograms[ch], analysis)
                            } else {
                                vel
                            }
                            val scaledVel = ((effectiveVel * melodyPercent) / 100).coerceIn(1, 127)
                            event.param = key or (scaledVel shl 8)
                        }
                    }
                }
            }
        }
        true
    }

    private val endSyncProc = BASS.SYNCPROC { _, _, _, _ ->
        mainHandler.post {
            onCompletionCallback?.invoke()
        }
    }

    // Real-time DSP tap to stream mastered 48kHz 32-bit float BASSMIDI SoundFont PCM to KaraokeRecorder
    private val recordDspProc = BASS.DSPPROC { _, _, buffer, length, _ ->
        val recorder = karaokeRecorder ?: return@DSPPROC
        if (!recorder.isRecording || buffer == null || length <= 0) return@DSPPROC
        try {
            buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN)
            buffer.rewind()
            val floatCount = length / 4
            if (floatCount <= 0) return@DSPPROC
            if (dspFloatArray.size < floatCount) {
                dspFloatArray = FloatArray(floatCount)
            }
            buffer.asFloatBuffer().get(dspFloatArray, 0, floatCount)
            recorder.onBassMusicPcm(dspFloatArray, floatCount)
        } catch (e: Exception) {
            Log.e(TAG, "Error in recordDspProc", e)
        }
    }

    /**
     * Real-time Auto-Volume / Automatic Gain Control (AGC) DSP:
     * Continuously analyzes the short-term RMS loudness of the SoundFont audio.
     * Smoothly scales quiet songs up and loud songs down to the standard videoke target
     * with zero pumping, preserving full punch and clarity without digital clipping.
     */
    /**
     * Studio Melody Auto-Normalization:
     * Centers guide melody velocity around nominal target (78 / 127) so the guide vocal
     * is never uncomfortably loud / piercing and never faint / drowned out.
     * Also compensates for SoundFont instrument timbre loudness (e.g. piercing brass vs soft flute).
     */
    private fun normalizeMelodyVelocity(rawVel: Int, channel: Int, program: Int, analysis: MidiMelodyScaler.MidiAnalysis?): Int {
        if (rawVel <= 0) return 0
        val ch = channel.coerceIn(0, 15)
        val songAvgVel = analysis?.channelAvgVelocities?.getOrNull(ch) ?: 80

        // 1. Center around nominal target (78 out of 127)
        val target = 78.0f
        val diff = (rawVel - target)

        // Compress extreme velocity dynamics (30..127) into sweet-spot guide corridor (65..90)
        val compressedVel = if (diff > 0) {
            target + diff * 0.28f // 127 -> ~91.7
        } else {
            target + diff * 0.38f // 40 -> ~63.5
        }

        // 2. Timbre Loudness Compensation (SoundFont instrument perceptual loudness)
        val timbreScale = when (program) {
            in 56..63 -> 0.88f // Brass / Trumpet / Trombone (piercing high harmonics)
            in 80..87 -> 0.88f // Synth Leads (sustained aggressive waveforms)
            in 64..71 -> 0.94f // Sax / Reeds (punchy mid-range)
            in 72..79 -> 1.10f // Flutes / Pipes (soft breathy tone)
            in 24..31 -> 1.05f // Guitars
            else -> 1.00f
        }

        // 3. Song-level baseline adjustment
        val songScale = when {
            songAvgVel > 110 -> 0.92f // Overall loud melody song
            songAvgVel < 60 -> 1.12f // Overall very soft melody song
            else -> 1.00f
        }

        val finalVel = (compressedVel * timbreScale * songScale).toInt()
        return finalVel.coerceIn(40, 102)
    }

    private val autoVolumeDspProc = BASS.DSPPROC { _, _, buffer, length, _ ->
        if (!isAutoVolumeEnabled || buffer == null || length <= 0) return@DSPPROC
        try {
            buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN)
            buffer.rewind()
            val floatBuffer = buffer.asFloatBuffer()
            val floatCount = length / 4
            if (floatCount <= 0) return@DSPPROC

            // 1. Calculate RMS of current chunk
            var sumSquare = 0.0
            for (i in 0 until floatCount) {
                val s = floatBuffer.get(i)
                sumSquare += (s * s)
            }
            val rms = Math.sqrt(sumSquare / floatCount).toFloat()

            // 2. Compute Target Gain with Noise/Silence Gating
            if (rms >= SILENCE_RMS_FLOOR) {
                val desiredGain = (TARGET_RMS / rms).coerceIn(MIN_AGC_GAIN, MAX_AGC_GAIN)
                targetAgcGain = desiredGain
            }

            // 3. Smooth Exponential Moving Average Ramping
            val smoothingRate = if (targetAgcGain < currentAgcGain) 0.035f else 0.008f
            currentAgcGain = currentAgcGain * (1.0f - smoothingRate) + targetAgcGain * smoothingRate
            val gain = currentAgcGain

            // 4. Apply Gain In-Place with Soft-Knee Analog Limiting
            floatBuffer.rewind()
            for (i in 0 until floatCount) {
                val orig = floatBuffer.get(i)
                val amplified = orig * gain
                val limited = if (amplified > 0.85f) {
                    0.85f + 0.14f * (amplified - 0.85f) / (amplified - 0.85f + 0.40f)
                } else if (amplified < -0.85f) {
                    -0.85f - 0.14f * (-amplified - 0.85f) / (-amplified - 0.85f + 0.40f)
                } else {
                    amplified
                }
                floatBuffer.put(i, limited.coerceIn(-0.99f, 0.99f))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in autoVolumeDspProc", e)
        }
    }

    private val midiIndexByCode = ConcurrentHashMap<String, File>()
    private val midiIndexByTitle = ConcurrentHashMap<String, File>()
    @Volatile private var isIndexingStarted = false
    @Volatile private var isIndexReady = false

    init {
        initBassEngine()
        buildMidiIndexAsync()
    }

    @Synchronized
    private fun initBassEngine(): Boolean {
        if (bassInitialized) return true
        return try {
            var ok = BASS.BASS_Init(-1, SAMPLE_RATE, 0)
            if (!ok) {
                val err = BASS.BASS_ErrorGetCode()
                if (err == 14) { // BASS_ERROR_ALREADY
                    ok = true
                } else {
                    Log.e(TAG, "BASS_Init failed with error code: $err")
                }
            }
            if (ok) {
                BASS.BASS_PluginLoad("libbassmidi.so", 0)
                BASS.BASS_PluginLoad("libbass_fx.so", 0)
                BASS.BASS_SetConfig(BASS.BASS_CONFIG_BUFFER, 250)
                BASS.BASS_SetConfig(BASS.BASS_CONFIG_ANDROID_AAUDIO, 1)
                BASS.BASS_SetConfig(BASS.BASS_CONFIG_UPDATEPERIOD, 30)
                BASS.BASS_SetConfig(BASS.BASS_CONFIG_UPDATETHREADS, 2)
                BASS.BASS_SetConfig(BASS.BASS_CONFIG_SRC_SAMPLE, 2) // Sinc interpolation
                BASS.BASS_SetConfig(BASS.BASS_CONFIG_DEV_PERIOD, 20)
                BASS.BASS_SetConfig(BASS.BASS_CONFIG_DEV_BUFFER, 40)
                BASS.BASS_SetConfig(BASSMIDI.BASS_CONFIG_MIDI_VOICES, 256)
                BASS.BASS_SetConfig(BASSMIDI.BASS_CONFIG_MIDI_AUTOFONT, 1)
                BASS.BASS_Start()
                bassInitialized = true
                Log.d(TAG, "BASS + BASSMIDI + BASS_FX initialized at ${SAMPLE_RATE}Hz (256 voices, 32-bit float)")
                loadSoundFontHandles()
            }
            bassInitialized
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to initialize native BASSMIDI engine", t)
            false
        }
    }

    private fun getCandidateSoundFontDirs(): List<File> {
        val root = Environment.getExternalStorageDirectory()
        return listOf(
            File(root, "VideoKey/soundfonts"),
            File("/storage/emulated/0/VideoKey/soundfonts"),
            File("/sdcard/VideoKey/soundfonts"),
            File(root, "VideoKey/SoundFont"),
            File(root, "VideoKey"),
            File(root, "Download"),
            File("/storage/emulated/0/Download"),
            File("/sdcard/Download"),
            context.filesDir
        )
    }

    private fun findSoundFontFile(queryName: String): File? {
        val trimmed = queryName.trim()
        if (trimmed.isNotEmpty()) {
            val direct = File(trimmed)
            if (direct.exists() && direct.isFile && direct.length() > 1024L) return direct
        }

        val allSf2Files = ArrayList<File>()
        val seenPaths = HashSet<String>()
        for (dir in getCandidateSoundFontDirs()) {
            if (dir.exists() && dir.isDirectory) {
                val files = dir.listFiles() ?: continue
                for (f in files) {
                    if (f.isFile && f.extension.equals("sf2", ignoreCase = true) && f.length() > 1024L) {
                        if (seenPaths.add(f.canonicalPath)) {
                            allSf2Files.add(f)
                        }
                    }
                }
            }
        }
        if (allSf2Files.isEmpty()) return null

        val lowerQuery = trimmed.lowercase()
        val preset = availableSoundFonts.find {
            it.id.equals(trimmed, ignoreCase = true) ||
                it.displayName.equals(trimmed, ignoreCase = true) ||
                it.fileName.equals(trimmed, ignoreCase = true)
        }
        val targetFileName = preset?.fileName?.lowercase() ?: lowerQuery

        // 1. Exact file name match
        allSf2Files.firstOrNull { it.name.equals(targetFileName, ignoreCase = true) || it.nameWithoutExtension.equals(targetFileName, ignoreCase = true) }?.let { return it }

        // 2. Partial keyword match
        if (lowerQuery.contains("yamaha") || lowerQuery.contains("korg") || lowerQuery.contains("ketron")) {
            allSf2Files.firstOrNull { it.name.lowercase().contains("yamaha") || it.name.lowercase().contains("korg") }?.let { return it }
        }
        if (lowerQuery.contains("minus") || lowerQuery.contains("studio") || lowerQuery.contains("videokey")) {
            allSf2Files.firstOrNull { it.name.lowercase().contains("minus1") || it.name.lowercase().contains("studio") }?.let { return it }
        }
        if (lowerQuery.contains("dream") || lowerQuery.contains("sam") || lowerQuery.contains("2634")) {
            allSf2Files.firstOrNull { it.name.lowercase().contains("dream") || it.name.lowercase().contains("2634") }?.let { return it }
        }
        if (lowerQuery.contains("mega")) {
            allSf2Files.firstOrNull { it.name.lowercase().contains("mega") }?.let { return it }
        }

        allSf2Files.firstOrNull {
            it.name.lowercase().contains(lowerQuery) || lowerQuery.contains(it.nameWithoutExtension.lowercase())
        }?.let { return it }

        // Default to VideoKey_Studio_Minus1.sf2 or largest studio .sf2 available
        return allSf2Files.firstOrNull { it.name.lowercase().contains("minus1") }
            ?: allSf2Files.maxByOrNull { it.length() }
    }

    @Synchronized
    private fun loadSoundFontHandles() {
        if (!bassInitialized) return
        val lowerActive = activeSoundFontName.lowercase()

        val primaryFile = when {
            lowerActive.contains("acoustic") || lowerActive.contains("ballad") -> {
                findSoundFontFile("yamaha x korg x ketron")
                    ?: findSoundFontFile("VideoKey_Studio_Minus1.sf2")
            }
            lowerActive.contains("disco") || lowerActive.contains("synth") || lowerActive.contains("dance") -> {
                findSoundFontFile("Dream SAM2634 (ver. 2.1).sf2")
                    ?: findSoundFontFile("mega.sf2")
                    ?: findSoundFontFile("VideoKey_Studio_Minus1.sf2")
            }
            lowerActive.contains("chacha") || lowerActive.contains("cha-cha") || lowerActive.contains("ballroom") || lowerActive.contains("latin") -> {
                findSoundFontFile("Dream SAM2634 (ver. 2.1).sf2")
                    ?: findSoundFontFile("mega.sf2")
                    ?: findSoundFontFile("VideoKey_Studio_Minus1.sf2")
            }
            lowerActive.contains("party") -> {
                findSoundFontFile("mega.sf2")
                    ?: findSoundFontFile("VideoKey_Studio_Minus1.sf2")
            }
            lowerActive.contains("rock") -> {
                findSoundFontFile("VideoKey_Studio_Minus1.sf2")
                    ?: findSoundFontFile("mega.sf2")
            }
            lowerActive.contains("yamaha") || lowerActive.contains("korg") -> {
                findSoundFontFile("yamaha x korg x ketron")
                    ?: findSoundFontFile("VideoKey_Studio_Minus1.sf2")
            }
            lowerActive.contains("dream") || lowerActive.contains("sam") -> {
                findSoundFontFile("Dream SAM2634 (ver. 2.1).sf2")
                    ?: findSoundFontFile("VideoKey_Studio_Minus1.sf2")
            }
            lowerActive.contains("mega") -> {
                findSoundFontFile("mega.sf2")
                    ?: findSoundFontFile("VideoKey_Studio_Minus1.sf2")
            }
            else -> {
                // Default Studio / Pop
                findSoundFontFile("VideoKey_Studio_Minus1.sf2")
                    ?: findSoundFontFile(activeSoundFontName)
                    ?: findSoundFontFile(activeSoundFont.fileName)
            }
        }

        if (primaryFile != null && primaryFile.exists()) {
            if (primaryFontPath != primaryFile.absolutePath || primaryFontHandle == 0) {
                if (primaryFontHandle != 0) {
                    BASSMIDI.BASS_MIDI_FontFree(primaryFontHandle)
                    primaryFontHandle = 0
                }
                val flags = BASSMIDI.BASS_MIDI_FONT_MMAP or BASSMIDI.BASS_MIDI_FONT_XGDRUMS
                val handle = BASSMIDI.BASS_MIDI_FontInit(primaryFile.absolutePath, flags)
                if (handle != 0) {
                    primaryFontHandle = handle
                    primaryFontPath = primaryFile.absolutePath
                    val profile = MidiMelodyScaler.resolveProfile(activeSoundFontName)
                    BASSMIDI.BASS_MIDI_FontSetVolume(handle, profile.fontGain)
                    val info = BASSMIDI.BASS_MIDI_FONTINFO()
                    if (BASSMIDI.BASS_MIDI_FontGetInfo(handle, info)) {
                        Log.d(TAG, "Loaded Primary SoundFont: ${primaryFile.name} (internal='${info.name}', presets=${info.presets}, samsize=${info.samsize / (1024 * 1024)}MB, gain=${profile.fontGain})")
                    } else {
                        Log.d(TAG, "Loaded Primary SoundFont: ${primaryFile.name} (handle=$handle)")
                    }
                } else {
                    Log.e(TAG, "BASS_MIDI_FontInit failed for ${primaryFile.absolutePath}, err=${BASS.BASS_ErrorGetCode()}")
                }
            }
        }

        // Load complete GM/Drum fallback SoundFont (VideoKey_Studio_Minus1.sf2 or Dream SAM2634) so no preset is ever missing
        val fallbackCandidate = findSoundFontFile("VideoKey_Studio_Minus1.sf2")
            ?.takeIf { it.absolutePath != primaryFontPath }
            ?: findSoundFontFile("Dream SAM2634")?.takeIf { it.absolutePath != primaryFontPath }

        if (fallbackCandidate != null && fallbackCandidate.exists()) {
            if (fallbackFontPath != fallbackCandidate.absolutePath || fallbackFontHandle == 0) {
                if (fallbackFontHandle != 0) {
                    BASSMIDI.BASS_MIDI_FontFree(fallbackFontHandle)
                    fallbackFontHandle = 0
                }
                val flags = BASSMIDI.BASS_MIDI_FONT_MMAP or BASSMIDI.BASS_MIDI_FONT_XGDRUMS
                val handle = BASSMIDI.BASS_MIDI_FontInit(fallbackCandidate.absolutePath, flags)
                if (handle != 0) {
                    fallbackFontHandle = handle
                    fallbackFontPath = fallbackCandidate.absolutePath
                    BASSMIDI.BASS_MIDI_FontSetVolume(handle, 0.78f)
                    Log.d(TAG, "Loaded Fallback SoundFont: ${fallbackCandidate.name} (handle=$handle)")
                }
            }
        }

        applyFontsToStream(midiStream)
    }

    private fun applyFontsToStream(streamHandle: Int) {
        if (!bassInitialized) return
        val fontList = ArrayList<BASSMIDI.BASS_MIDI_FONT>()

        // In BASSMIDI, preset = -1 and bank = 0 maps ALL presets and ALL banks (0..128, including Drum Bank 128)
        // with 0 bank offset. Never add bank = 128 when preset = -1 (which would shift Piano bank 0 onto Drum bank 128).
        if (primaryFontHandle != 0) {
            fontList.add(BASSMIDI.BASS_MIDI_FONT().apply {
                font = primaryFontHandle
                preset = -1
                bank = 0
            })
        }
        if (fallbackFontHandle != 0 && fallbackFontHandle != primaryFontHandle) {
            fontList.add(BASSMIDI.BASS_MIDI_FONT().apply {
                font = fallbackFontHandle
                preset = -1
                bank = 0
            })
        }

        if (fontList.isNotEmpty()) {
            val arr = fontList.toTypedArray()
            if (streamHandle != 0) {
                BASSMIDI.BASS_MIDI_StreamSetFonts(streamHandle, arr, arr.size)
                BASSMIDI.BASS_MIDI_StreamLoadSamples(streamHandle)
            }
            BASSMIDI.BASS_MIDI_StreamSetFonts(0, arr, arr.size)
        }
    }

    private fun applyMasteringChain(streamHandle: Int) {
        if (streamHandle == 0 || !bassInitialized) return
        val profile = MidiMelodyScaler.resolveProfile(activeSoundFontName)
        if (primaryFontHandle != 0) {
            BASSMIDI.BASS_MIDI_FontSetVolume(primaryFontHandle, profile.fontGain)
        }

        // 1. 10-Band 32-bit Float Studio Mastering Equalizer (Dynamic per Genre)
        val mixStyle = currentAnalysis?.detectedMixStyle ?: MidiMelodyScaler.SongMixStyle.STANDARD_POP
        val targetEqGains = if (isAutoMixerEnabled) {
            MidiMelodyScaler.resolveEqForMixStyle(mixStyle)
        } else {
            profile.eqGainsDb
        }
        for (i in 0 until 10) {
            if (eqFxHandles[i] == 0) {
                eqFxHandles[i] = BASS.BASS_ChannelSetFX(streamHandle, BASS_FX.BASS_FX_BFX_PEAKEQ, 10)
            }
            val fx = eqFxHandles[i]
            if (fx != 0) {
                val eq = BASS_FX.BASS_BFX_PEAKEQ().apply {
                    lBand = i
                    fBandwidth = 1.0f
                    fQ = 0.0f
                    fCenter = EQ_FREQUENCIES[i]
                    fGain = targetEqGains.getOrElse(i) { 0.0f }
                    lChannel = BASS_FX.BASS_BFX_CHANALL
                }
                BASS.BASS_FXSetParameters(fx, eq)
            }
        }

        // 2. Studio Auto-Volume Leveler / AGC (Priority 5: runs after EQ, before Compressor)
        if (autoVolumeDspHandle == 0) {
            autoVolumeDspHandle = BASS.BASS_ChannelSetDSP(streamHandle, autoVolumeDspProc, null, 5)
        }

        // 3. Studio Mastering Compressor / Anti-Clipping Limiter ("hindi basag kahit malakas")
        if (compressorFxHandle == 0) {
            compressorFxHandle = BASS.BASS_ChannelSetFX(streamHandle, BASS_FX.BASS_FX_BFX_COMPRESSOR2, 1)
        }
        if (compressorFxHandle != 0) {
            val comp = BASS_FX.BASS_BFX_COMPRESSOR2().apply {
                fThreshold = -4.5f
                fRatio = 3.5f
                fAttack = 6.0f
                fRelease = 140.0f
                fGain = 0.5f
                lChannel = BASS_FX.BASS_BFX_CHANALL
            }
            BASS.BASS_FXSetParameters(compressorFxHandle, comp)
        }

        // 3. Studio Reverb Level
        applyReverbToStream(streamHandle)

        // 4. Real-time Recording Tap (Priority -50: runs after EQ & Compressor for 100% mastered SoundFont audio)
        if (recordDspHandle == 0) {
            recordDspHandle = BASS.BASS_ChannelSetDSP(streamHandle, recordDspProc, null, -50)
        }
    }

    private fun applyReverbToStream(streamHandle: Int) {
        if (streamHandle == 0 || !bassInitialized) return
        val reverbFloat = ((reverbPercent.coerceIn(0, 100) / 100.0f) * 0.85f).coerceIn(0.0f, 1.0f)
        BASS.BASS_ChannelSetAttribute(streamHandle, BASSMIDI.BASS_ATTRIB_MIDI_REVERB, reverbFloat)
    }

    private fun getPercentForCategory(category: MidiMelodyScaler.InstrumentCategory): Int {
        return when (category) {
            MidiMelodyScaler.InstrumentCategory.MELODY -> melodyPercent.coerceIn(0, 100)
            MidiMelodyScaler.InstrumentCategory.DRUMS -> drumsPercent.coerceIn(0, 150)
            MidiMelodyScaler.InstrumentCategory.BASS -> bassPercent.coerceIn(0, 150)
            MidiMelodyScaler.InstrumentCategory.GUITAR_PIANO -> guitarPianoPercent.coerceIn(0, 150)
            MidiMelodyScaler.InstrumentCategory.STRINGS_BRASS -> stringsBrassPercent.coerceIn(0, 150)
        }
    }

    private fun computeScaledChannelVolume(channel: Int, baseVol: Int): Int {
        val ch = channel.coerceIn(0, 15)
        val analysis = currentAnalysis
        val prog = liveChannelPrograms[ch]
        val category = if (ch == 9 || liveDrumChannels[ch]) {
            MidiMelodyScaler.InstrumentCategory.DRUMS
        } else {
            analysis?.getCategoryForChannel(ch, prog) ?: when (prog) {
                in 32..39 -> MidiMelodyScaler.InstrumentCategory.BASS
                in 0..31 -> MidiMelodyScaler.InstrumentCategory.GUITAR_PIANO
                else -> MidiMelodyScaler.InstrumentCategory.STRINGS_BRASS
            }
        }
        val effectiveBaseVol = if (isAutoMelodyNormalizeEnabled && category == MidiMelodyScaler.InstrumentCategory.MELODY) {
            when {
                baseVol < 75 -> 95 // Soft melody channel boosted to standard
                baseVol > 120 -> 100 // Blasting melody channel toned down to standard
                else -> baseVol
            }
        } else {
            baseVol
        }
        val pct = getPercentForCategory(category)
        if (pct <= 0) return 0
        return ((effectiveBaseVol * pct) / 100).coerceIn(1, 127)
    }

    /**
     * Applies real-time instrument volumes (Melody, Drums, Bass, Guitar/Piano, Strings/Brass)
     * directly to the active BASSMIDI stream with zero latency or interruption.
     */
    fun applyInstrumentMix() {
        val handle = midiStream
        if (handle == 0 || !bassInitialized) return
        val analysis = currentAnalysis

        // 1. Scale dedicated Melody Track(s) via BASS_ATTRIB_MIDI_TRACK_VOL when multi-track
        if (analysis != null && analysis.numTracks > 1 && analysis.melodyTracks.isNotEmpty()) {
            val melodyGain = (melodyPercent.coerceIn(0, 100) / 100.0f)
            for (trk in analysis.melodyTracks) {
                BASS.BASS_ChannelSetAttribute(handle, BASSMIDI.BASS_ATTRIB_MIDI_TRACK_VOL + trk, melodyGain)
            }
        }

        // 2. Scale all 16 MIDI channels according to their instrument category
        for (ch in 0..15) {
            val curProg = BASSMIDI.BASS_MIDI_StreamGetEvent(handle, ch, BASSMIDI.MIDI_EVENT_PROGRAM)
            if (curProg in 0..127) {
                liveChannelPrograms[ch] = curProg
            }
            val isDrum = (ch == 9) || (BASSMIDI.BASS_MIDI_StreamGetEvent(handle, ch, BASSMIDI.MIDI_EVENT_DRUMS) > 0)
            liveDrumChannels[ch] = isDrum

            val category = if (isDrum) {
                MidiMelodyScaler.InstrumentCategory.DRUMS
            } else {
                analysis?.getCategoryForChannel(ch, liveChannelPrograms[ch]) ?: when (liveChannelPrograms[ch]) {
                    in 32..39 -> MidiMelodyScaler.InstrumentCategory.BASS
                    in 0..31 -> MidiMelodyScaler.InstrumentCategory.GUITAR_PIANO
                    else -> MidiMelodyScaler.InstrumentCategory.STRINGS_BRASS
                }
            }
            val pct = getPercentForCategory(category)
            val baseVol = baseChannelVolumes[ch].coerceIn(1, 127)
            val scaledVol = if (pct <= 0) 0 else ((baseVol * pct) / 100).coerceIn(1, 127)
            BASSMIDI.BASS_MIDI_StreamEvent(handle, ch, BASSMIDI.MIDI_EVENT_VOLUME, scaledVol)

            // Also apply BASSMIDI mixlevel boost for sliders set above 100% (101% .. 150%)
            val mixLevel = if (pct <= 0) 0 else if (pct > 100) pct.coerceIn(100, 150) else 100
            BASSMIDI.BASS_MIDI_StreamEvent(handle, ch, BASSMIDI.MIDI_EVENT_MIXLEVEL, mixLevel)
        }

        // 3. Update Reverb
        applyReverbToStream(handle)
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
            Log.d(TAG, "MIDI Index built with ${midiIndexByCode.size} code entries and ${midiIndexByTitle.size} title entries")
        } catch (e: Exception) {
            Log.e(TAG, "Error building MIDI index", e)
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
            Log.d(TAG, "Indexed folder ${folder.absolutePath}: index has ${midiIndexByCode.size} code entries")
        } catch (e: Exception) {
            Log.e(TAG, "Error indexing folder MIDI", e)
        }
    }

    private var currentSongRef: Song? = null

    fun playMidi(
        filePath: String,
        song: Song? = null,
        deleteAfterOpen: Boolean = false,
        resumePositionMs: Long = 0L,
        onCompletion: (() -> Unit)? = null
    ): Boolean {
        val file = File(filePath)
        if (!file.exists() || !file.canRead()) {
            Log.e(TAG, "MIDI file not found or unreadable: $filePath")
            return false
        }

        stop()
        currentMidiFile = file
        onCompletionCallback = onCompletion
        lastKnownPositionMs = resumePositionMs

        // Analyze MIDI structure for accurate real-time instrument & melody mixing
        try {
            val bytes = file.readBytes()
            currentAnalysis = MidiMelodyScaler.analyzeMidi(bytes)
            for (ch in 0..15) {
                liveChannelPrograms[ch] = currentAnalysis?.channelPrograms?.get(ch) ?: 0
                liveDrumChannels[ch] = (ch == 9) || (currentAnalysis?.drumChannels?.contains(ch) == true)
                baseChannelVolumes[ch] = 100
            }
            currentSongRef = song

            // Auto SoundFont Refinement: refine matched SoundFont with fresh MIDI analysis
            if (activeSoundFont.id == "auto_genre") {
                val matched = resolveGenreForSong(song, currentAnalysis)
                activeSoundFontName = matched.displayName
                Log.d(TAG, "Auto SoundFont refined with MIDI analysis: ${matched.displayName} for ${song?.title ?: file.name}")
            }

            if (isAutoMixerEnabled) {
                val autoStyle = currentAnalysis?.detectedMixStyle ?: MidiMelodyScaler.SongMixStyle.STANDARD_POP
                melodyPercent = autoStyle.melody
                drumsPercent = autoStyle.drums
                bassPercent = autoStyle.bass
                guitarPianoPercent = autoStyle.guitarPiano
                stringsBrassPercent = autoStyle.stringsBrass
                reverbPercent = autoStyle.reverb
                Log.d(TAG, "Smart Auto-Mix applied for ${file.name}: ${autoStyle.name} (${autoStyle.labelEnglish}) BPM=${currentAnalysis?.detectedBpm}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not pre-analyze MIDI bytes: ${e.message}")
            currentAnalysis = null
        }

        if (initBassEngine()) {
            try {
                loadSoundFontHandles()
                val streamFlags = BASS.BASS_SAMPLE_FLOAT or BASSMIDI.BASS_MIDI_SINCINTER or BASSMIDI.BASS_MIDI_DECAYEND
                val handle = BASSMIDI.BASS_MIDI_StreamCreateFile(file.absolutePath, 0L, 0L, streamFlags, SAMPLE_RATE)
                if (deleteAfterOpen) {
                    file.delete()
                }
                if (handle != 0) {
                    midiStream = handle
                    BASS.BASS_ChannelSetAttribute(handle, BASSMIDI.BASS_ATTRIB_MIDI_CHANS, 16.0f)
                    BASS.BASS_ChannelSetAttribute(handle, BASSMIDI.BASS_ATTRIB_MIDI_VOL, 0.90f)

                    // Apply .sf2 SoundFont banks (Bank 0 Melodic + Bank 128 Drums + Fallback)
                    applyFontsToStream(handle)

                    // Enable GS/GM drum & instrument system
                    BASSMIDI.BASS_MIDI_StreamEvent(handle, 0, BASSMIDI.MIDI_EVENT_SYSTEM, BASSMIDI.MIDI_SYSTEM_GS)

                    // Read initial channel volumes from stream
                    for (ch in 0..15) {
                        val initVol = BASSMIDI.BASS_MIDI_StreamGetEvent(handle, ch, BASSMIDI.MIDI_EVENT_VOLUME)
                        baseChannelVolumes[ch] = if (initVol in 1..127) initVol else 100
                        val initProg = BASSMIDI.BASS_MIDI_StreamGetEvent(handle, ch, BASSMIDI.MIDI_EVENT_PROGRAM)
                        if (initProg in 0..127) {
                            liveChannelPrograms[ch] = initProg
                        }
                    }

                    // Smart Initial Song Gain: Pre-normalize based on average channel volumes
                    if (isAutoVolumeEnabled) {
                        var sumVol = 0
                        var activeChans = 0
                        for (ch in 0..15) {
                            val v = baseChannelVolumes[ch]
                            if (v in 1..127) {
                                sumVol += v
                                activeChans++
                            }
                        }
                        val avgVol = if (activeChans > 0) sumVol / activeChans else 100
                        currentAgcGain = when {
                            avgVol < 75 -> 1.55f // Noticeably quiet MIDI -> start with boost immediately
                            avgVol < 90 -> 1.25f // Mildly quiet MIDI
                            avgVol > 120 -> 0.85f // Very loud MIDI -> start slightly tamed
                            else -> 1.0f
                        }
                        targetAgcGain = currentAgcGain
                    } else {
                        currentAgcGain = 1.0f
                        targetAgcGain = 1.0f
                    }

                    // Register real-time MIDI event filter
                    BASSMIDI.BASS_MIDI_StreamSetFilter(handle, false, midiFilterProc, null)

                    // Apply Studio Mastering EQ + Anti-Clipping Compressor
                    applyMasteringChain(handle)

                    // Apply Key Transpose, Tempo, and Instrument Mixer levels
                    applyPlaybackParams()
                    applyInstrumentMix()

                    // Compute duration
                    val lenBytes = BASS.BASS_ChannelGetLength(handle, BASS.BASS_POS_BYTE)
                    cachedDurationMs = if (lenBytes > 0) {
                        (BASS.BASS_ChannelBytes2Seconds(handle, lenBytes) * 1000.0).toLong()
                    } else 0L

                    // Seek if resuming
                    if (resumePositionMs > 0L && (cachedDurationMs <= 0L || resumePositionMs < cachedDurationMs - 200L)) {
                        val targetBytes = BASS.BASS_ChannelSeconds2Bytes(handle, resumePositionMs / 1000.0)
                        BASS.BASS_ChannelSetPosition(handle, targetBytes, BASS.BASS_POS_BYTE)
                    }

                    // Register end-of-stream callback
                    endSyncHandle = BASS.BASS_ChannelSetSync(handle, BASS.BASS_SYNC_END, 0L, endSyncProc, null)

                    // Start playback
                    BASS.BASS_ChannelPlay(handle, false)
                    Log.d(
                        TAG,
                        "BASSMIDI Playback started: song=${file.name}, sf2=${File(primaryFontPath).name}, duration=${cachedDurationMs}ms, melodyTracks=${currentAnalysis?.melodyTracks}, melodyChans=${currentAnalysis?.exclusiveMelodyChannels}"
                    )
                    return true
                } else {
                    Log.e(TAG, "BASS_MIDI_StreamCreateFile failed with error: ${BASS.BASS_ErrorGetCode()}")
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Error starting BASSMIDI playback, falling back to MediaPlayer", t)
            }
        }

        // Fallback only if native BASS failed
        return try {
            fallbackPlayer = MediaPlayer().apply {
                if (deleteAfterOpen && file.exists()) {
                    java.io.FileInputStream(file).use { fis -> setDataSource(fis.fd) }
                    file.delete()
                } else {
                    setDataSource(file.absolutePath)
                }
                setOnPreparedListener { mp ->
                    applyPlaybackParams()
                    if (resumePositionMs > 0L && resumePositionMs < mp.duration - 200) {
                        mp.seekTo(resumePositionMs.toInt())
                    }
                    mp.start()
                }
                setOnCompletionListener {
                    onCompletion?.invoke()
                }
                setOnErrorListener { _, _, _ -> true }
                prepareAsync()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Fallback MediaPlayer also failed", e)
            false
        }
    }

    fun pause() {
        try {
            if (midiStream != 0) {
                if (BASS.BASS_ChannelIsActive(midiStream) == BASS.BASS_ACTIVE_PLAYING) {
                    BASS.BASS_ChannelPause(midiStream)
                }
                return
            }
            if (fallbackPlayer?.isPlaying == true) {
                fallbackPlayer?.pause()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing playback", e)
        }
    }

    fun resume() {
        try {
            if (midiStream != 0) {
                if (BASS.BASS_ChannelIsActive(midiStream) != BASS.BASS_ACTIVE_PLAYING) {
                    BASS.BASS_ChannelPlay(midiStream, false)
                }
                return
            }
            if (fallbackPlayer != null && !fallbackPlayer!!.isPlaying) {
                fallbackPlayer?.start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming playback", e)
        }
    }

    fun stop() {
        onCompletionCallback = null
        try {
            if (midiStream != 0) {
                for (i in eqFxHandles.indices) {
                    if (eqFxHandles[i] != 0) {
                        BASS.BASS_ChannelRemoveFX(midiStream, eqFxHandles[i])
                        eqFxHandles[i] = 0
                    }
                }
                if (compressorFxHandle != 0) {
                    BASS.BASS_ChannelRemoveFX(midiStream, compressorFxHandle)
                    compressorFxHandle = 0
                }
                if (autoVolumeDspHandle != 0) {
                    BASS.BASS_ChannelRemoveDSP(midiStream, autoVolumeDspHandle)
                    autoVolumeDspHandle = 0
                }
                if (recordDspHandle != 0) {
                    BASS.BASS_ChannelRemoveDSP(midiStream, recordDspHandle)
                    recordDspHandle = 0
                }
                if (endSyncHandle != 0) {
                    BASS.BASS_ChannelRemoveSync(midiStream, endSyncHandle)
                    endSyncHandle = 0
                }
                BASS.BASS_ChannelStop(midiStream)
                BASS.BASS_StreamFree(midiStream)
                midiStream = 0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping BASSMIDI stream", e)
            midiStream = 0
        }

        try {
            fallbackPlayer?.let {
                if (it.isPlaying) it.stop()
                it.reset()
                it.release()
            }
        } catch (_: Exception) {
        } finally {
            fallbackPlayer = null
        }
    }

    fun isPlaying(): Boolean {
        return try {
            if (midiStream != 0) {
                BASS.BASS_ChannelIsActive(midiStream) == BASS.BASS_ACTIVE_PLAYING
            } else {
                fallbackPlayer?.isPlaying == true
            }
        } catch (e: Exception) {
            false
        }
    }

    fun getCurrentPositionMs(): Long {
        return try {
            if (midiStream != 0) {
                val active = BASS.BASS_ChannelIsActive(midiStream)
                val posBytes = BASS.BASS_ChannelGetPosition(midiStream, BASS.BASS_POS_BYTE)
                if (posBytes >= 0) {
                    val posMs = (BASS.BASS_ChannelBytes2Seconds(midiStream, posBytes) * 1000.0).toLong()
                    if (active == BASS.BASS_ACTIVE_PLAYING || posMs > 0L) {
                        lastKnownPositionMs = posMs
                    }
                    lastKnownPositionMs
                } else {
                    lastKnownPositionMs
                }
            } else {
                fallbackPlayer?.currentPosition?.toLong() ?: 0L
            }
        } catch (e: Exception) {
            lastKnownPositionMs
        }
    }

    fun getDurationMs(): Long {
        return try {
            if (midiStream != 0) {
                if (cachedDurationMs <= 0L) {
                    val lenBytes = BASS.BASS_ChannelGetLength(midiStream, BASS.BASS_POS_BYTE)
                    if (lenBytes > 0) {
                        cachedDurationMs = (BASS.BASS_ChannelBytes2Seconds(midiStream, lenBytes) * 1000.0).toLong()
                    }
                }
                cachedDurationMs
            } else {
                fallbackPlayer?.duration?.toLong() ?: 0L
            }
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
            val handle = midiStream
            if (handle != 0 && bassInitialized) {
                // 1. Key Transpose on non-drum channels only (100 = 0 semitones)
                for (ch in 0..15) {
                    val isDrum = (ch == 9) || liveDrumChannels[ch] ||
                        (BASSMIDI.BASS_MIDI_StreamGetEvent(handle, ch, BASSMIDI.MIDI_EVENT_DRUMS) > 0)
                    val transposeParam = if (isDrum) 100 else (100 + keyTranspose)
                    BASSMIDI.BASS_MIDI_StreamEvent(handle, ch, BASSMIDI.MIDI_EVENT_TRANSPOSE, transposeParam)
                }
                // 2. MIDI Tempo Speed multiplier (1.0f = 100%)
                val speed = tempoPercent / 100.0f
                BASS.BASS_ChannelSetAttribute(handle, BASSMIDI.BASS_ATTRIB_MIDI_SPEED, speed)
                Log.d(TAG, "Applied BASSMIDI params: keyTranspose=$keyTranspose semitones, speed=${tempoPercent}%")
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && fallbackPlayer != null) {
                val pitch = 2.0f.pow(keyTranspose / 12.0f)
                val speed = tempoPercent / 100.0f
                val params = PlaybackParams().apply {
                    setPitch(pitch)
                    setSpeed(speed)
                }
                fallbackPlayer?.playbackParams = params
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying PlaybackParams", e)
        }
    }

    fun setMicEcho(percent: Int) {
        micEchoPercent = percent.coerceIn(0, 100)
    }

    fun getMicEcho(): Int = micEchoPercent

    fun setAutoVolumeEnabled(enabled: Boolean) {
        isAutoVolumeEnabled = enabled
        if (!enabled) {
            currentAgcGain = 1.0f
            targetAgcGain = 1.0f
        }
        Log.d(TAG, "Auto Volume Leveling set to: $enabled")
    }

    fun isAutoVolumeEnabled(): Boolean = isAutoVolumeEnabled

    fun setAutoMelodyNormalizeEnabled(enabled: Boolean) {
        isAutoMelodyNormalizeEnabled = enabled
        Log.d(TAG, "Auto Melody Normalize set to: $enabled")
    }

    fun isAutoMelodyNormalizeEnabled(): Boolean = isAutoMelodyNormalizeEnabled

    fun setMelodyVolume(percent: Int) {
        melodyPercent = percent.coerceIn(0, 100)
        applyInstrumentMix()
    }

    fun getMelodyVolume(): Int = melodyPercent

    fun setDrumsVolume(percent: Int) {
        drumsPercent = percent.coerceIn(0, 150)
        applyInstrumentMix()
    }

    fun getDrumsVolume(): Int = drumsPercent

    fun setBassVolume(percent: Int) {
        bassPercent = percent.coerceIn(0, 150)
        applyInstrumentMix()
    }

    fun getBassVolume(): Int = bassPercent

    fun setGuitarPianoVolume(percent: Int) {
        guitarPianoPercent = percent.coerceIn(0, 150)
        applyInstrumentMix()
    }

    fun getGuitarPianoVolume(): Int = guitarPianoPercent

    fun setStringsBrassVolume(percent: Int) {
        stringsBrassPercent = percent.coerceIn(0, 150)
        applyInstrumentMix()
    }

    fun getStringsBrassVolume(): Int = stringsBrassPercent

    fun setReverbVolume(percent: Int) {
        reverbPercent = percent.coerceIn(0, 100)
        applyReverbToStream(midiStream)
    }

    fun getReverbVolume(): Int = reverbPercent

    fun resetInstrumentMix() {
        melodyPercent = 60
        drumsPercent = 100
        bassPercent = 100
        guitarPianoPercent = 100
        stringsBrassPercent = 100
        reverbPercent = 50
        applyInstrumentMix()
    }

    fun setAutoMixerEnabled(enabled: Boolean) {
        isAutoMixerEnabled = enabled
    }

    fun isAutoMixerEnabled(): Boolean = isAutoMixerEnabled

    fun getDetectedMixStyle(): MidiMelodyScaler.SongMixStyle =
        currentAnalysis?.detectedMixStyle ?: MidiMelodyScaler.SongMixStyle.STANDARD_POP

    fun applyMixStyle(style: MidiMelodyScaler.SongMixStyle) {
        melodyPercent = style.melody
        drumsPercent = style.drums
        bassPercent = style.bass
        guitarPianoPercent = style.guitarPiano
        stringsBrassPercent = style.stringsBrass
        reverbPercent = style.reverb
        applyInstrumentMix()
    }

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
        } else {
            val customFile = findSoundFontFile(presetId)
            if (customFile != null) {
                activeSoundFont = SoundFontPreset(
                    id = customFile.nameWithoutExtension,
                    displayName = customFile.nameWithoutExtension,
                    fileName = customFile.name,
                    description = "Custom Studio SoundFont (${customFile.length() / (1024 * 1024)} MB)"
                )
            }
        }
        try {
            if (initBassEngine()) {
                loadSoundFontHandles()
                if (midiStream != 0) {
                    applyMasteringChain(midiStream)
                    applyInstrumentMix()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to hot-swap SoundFont: ${e.message}")
        }
    }

    fun resolveGenreForSong(song: Song?, analysis: MidiMelodyScaler.MidiAnalysis? = null): SoundFontPreset {
        val cat = song?.category?.uppercase() ?: ""
        val title = song?.title?.uppercase() ?: ""
        val artist = song?.artist?.uppercase() ?: ""
        val fullText = "$title $artist $cat"

        // 1. Cha-Cha / Latin / Ballroom check
        val isChaCha = cat.contains("CHACHA") || cat.contains("CHA-CHA") || cat.contains("CHA CHA") ||
                cat.contains("BALLROOM") || cat.contains("LATIN") || cat.contains("SALSA") ||
                cat.contains("MAMBO") || cat.contains("TANGO") || cat.contains("WALTZ") ||
                cat.contains("RHUMBA") || cat.contains("RUMBA") || cat.contains("BOSSA") ||
                analysis?.detectedMixStyle == MidiMelodyScaler.SongMixStyle.LATIN_CHACHA ||
                fullText.contains("CHACHA") || fullText.contains("CHA CHA") || fullText.contains("CHA-CHA") ||
                fullText.contains("MAMBO") || fullText.contains("TANGO") || fullText.contains("WALTZ") ||
                fullText.contains("RHUMBA") || fullText.contains("CUMBIA")

        if (isChaCha) {
            return availableSoundFonts.find { it.id == "videokey_chacha" }
                ?: availableSoundFonts.find { it.id == "videokey_disco" }
                ?: availableSoundFonts.first()
        }

        // 2. Rock & Live Band check
        val isRock = cat.contains("ROCK") || cat.contains("METAL") || cat.contains("PUNK") || cat.contains("ALTERNATIVE") ||
                analysis?.detectedMixStyle == MidiMelodyScaler.SongMixStyle.ROCK_UPBEAT ||
                fullText.contains("AEGIS") || fullText.contains("KAMIKAZEE") || fullText.contains("PAROKYA") ||
                fullText.contains("ERASERHEADS") || fullText.contains("RIVERMAYA") || fullText.contains("SLAPSHOCK") ||
                fullText.contains("QUEEN") || fullText.contains("BON JOVI") || fullText.contains("SCORPIONS") ||
                fullText.contains("METALLICA") || fullText.contains("GUNS N") || fullText.contains("AC/DC") ||
                fullText.contains("AEROSMITH") || fullText.contains("JOURNEY") || fullText.contains("SURVIVOR") ||
                fullText.contains("EUROPE") || fullText.contains("DEF LEPPARD") || fullText.contains("EAGLES") ||
                fullText.contains("CREED") || fullText.contains("NICKELBACK") || fullText.contains("GREEN DAY") ||
                fullText.contains("BLINK-182") || fullText.contains("LINKIN PARK") || fullText.contains("EVANESCENCE") ||
                fullText.contains("SIAKOL") || fullText.contains("TEETH") || fullText.contains("SPONGE COLA") ||
                fullText.contains("CUESHE") || fullText.contains("CALLALILY") || fullText.contains("6CYCLEMIND") ||
                fullText.contains("MAYONNAISE") || fullText.contains("HALE") || fullText.contains("ORANGE AND LEMONS") ||
                fullText.contains("URBANDUB") || fullText.contains("FRANCO") || fullText.contains("CHICOSCI") ||
                fullText.contains("JUAN DE LA CRUZ") || fullText.contains("SAMPAGUITA") || fullText.contains("MIKE HANOPOL")

        if (isRock) {
            return availableSoundFonts.find { it.id == "videokey_rock" } ?: availableSoundFonts.first()
        }

        // 3. Disco / 80s / Retro / Dance check
        val isDisco = cat.contains("DISCO") || cat.contains("DANCE") || cat.contains("RETRO") ||
                cat.contains("ELECTRONIC") || cat.contains("SYNTH") || cat.contains("80S") || cat.contains("90S") ||
                analysis?.detectedMixStyle == MidiMelodyScaler.SongMixStyle.DANCE_DISCO ||
                (analysis != null && analysis.detectedBpm in 118..138 && !isRock) ||
                fullText.contains("VST") || fullText.contains("HAGIBIS") || fullText.contains("BOYFRIENDS") ||
                fullText.contains("HOTDOG") || fullText.contains("CINDERELLA") || fullText.contains("ABBA") ||
                fullText.contains("BEE GEES") || fullText.contains("BONEY M") || fullText.contains("EARTH WIND") ||
                fullText.contains("VILLAGE PEOPLE") || fullText.contains("RICK ASTLEY") || fullText.contains("MODERN TALKING") ||
                fullText.contains("MICHAEL JACKSON") || fullText.contains("MADONNA") || fullText.contains("PET SHOP")

        if (isDisco) {
            return availableSoundFonts.find { it.id == "videokey_disco" } ?: availableSoundFonts.first()
        }

        // 4. Acoustic / Unplugged & Ballad / Love Songs check
        val isAcousticOrBallad = cat.contains("BALLAD") || cat.contains("ACOUSTIC") || cat.contains("SLOW") ||
                cat.contains("LOVE") || cat.contains("ROMANTIC") || cat.contains("SENTIMENTAL") ||
                analysis?.detectedMixStyle == MidiMelodyScaler.SongMixStyle.ACOUSTIC ||
                analysis?.detectedMixStyle == MidiMelodyScaler.SongMixStyle.BALLAD ||
                analysis?.detectedMixStyle == MidiMelodyScaler.SongMixStyle.RNB_SOUL ||
                (analysis != null && analysis.detectedBpm in 1..88) ||
                fullText.contains("REGINE") || fullText.contains("SHARON") || fullText.contains("GARY V") ||
                fullText.contains("MARTIN NIEVERA") || fullText.contains("OGIE ALCASID") || fullText.contains("JAYA") ||
                fullText.contains("LANI MISALUCHA") || fullText.contains("SARAH GERONIMO") || fullText.contains("CHRISTIAN BAUTISTA") ||
                fullText.contains("ERIK SANTOS") || fullText.contains("JED MADELA") || fullText.contains("MORISSETTE") ||
                fullText.contains("ANGELINE QUINTO") || fullText.contains("KZ TANDINGAN") || fullText.contains("KYLA") ||
                fullText.contains("JAY R") || fullText.contains("NINA") || fullText.contains("FREESTYLE") ||
                fullText.contains("SIDE A") || fullText.contains("SOUTH BORDER") || fullText.contains("TRUE FAITH") ||
                fullText.contains("MYMP") || fullText.contains("MOIRA") || fullText.contains("ARTHUR NERY") ||
                fullText.contains("TJ MONTERDE") || fullText.contains("ADIE") || fullText.contains("ZACK TABUDLO") ||
                fullText.contains("BEN&BEN") || fullText.contains("DECEMBER AVENUE") || fullText.contains("IV OF SPADES") ||
                fullText.contains("AIR SUPPLY") || fullText.contains("WESTLIFE") || fullText.contains("CARPENTERS") ||
                fullText.contains("CELINE DION") || fullText.contains("WHITNEY HOUSTON") || fullText.contains("MARIAH CAREY") ||
                fullText.contains("MICHAEL BOLTON") || fullText.contains("PEABO BRYSON") || fullText.contains("KENNY ROGERS") ||
                fullText.contains("BRYAN ADAMS") || fullText.contains("ELTON JOHN") || fullText.contains("LIONEL RICHIE") ||
                fullText.contains("DAN HILL") || fullText.contains("BREAD") || fullText.contains("CHICAGO") ||
                fullText.contains("BALLAD") || fullText.contains("ACOUSTIC") || fullText.contains("UNPLUGGED")

        if (isAcousticOrBallad) {
            return availableSoundFonts.find { it.id == "videokey_acoustic" }
                ?: availableSoundFonts.find { it.id == "yamaha_korg_ketron" }
                ?: availableSoundFonts.first()
        }

        // 5. Party / Fiesta / Novelty check
        val isParty = cat.contains("PARTY") || cat.contains("NOVELTY") || cat.contains("RAP") ||
                cat.contains("HIPHOP") || cat.contains("HIP-HOP") || cat.contains("FIESTA") ||
                analysis?.detectedMixStyle == MidiMelodyScaler.SongMixStyle.PARTY_FIESTA ||
                fullText.contains("GRIN DEPT") || fullText.contains("ANDREW E") || fullText.contains("YOYOY") ||
                fullText.contains("MAX SURBAN") || fullText.contains("BLAKDYAK") || fullText.contains("BAYANI AGBAYANI") ||
                fullText.contains("SEXBOMB") || fullText.contains("MASCULADOS") || fullText.contains("VHONG NAVARRO") ||
                fullText.contains("GLOC-9") || fullText.contains("FLOW G") || fullText.contains("SKUSTA CLEE") ||
                fullText.contains("EX BATTALION") || fullText.contains("SHANTI DOPE")

        if (isParty) {
            return availableSoundFonts.find { it.id == "videokey_party" } ?: availableSoundFonts.first()
        }

        // 6. Default: Balanced OPM & Pop Studio
        return availableSoundFonts.find { it.id == "videokey_pop" } ?: availableSoundFonts.first()
    }

    fun findMidiFileForSong(song: Song): File? {
        if (song.filePath.isNotBlank()) {
            val direct = File(song.filePath)
            if (direct.exists() && (direct.extension.equals("mid", true) || direct.extension.equals("kar", true))) {
                return direct
            }
        }

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

        val cleanTitle = song.title.replace(Regex("""[^a-zA-Z0-9]"""), "").lowercase().take(15)
        if (cleanTitle.length >= 3) {
            midiIndexByTitle[cleanTitle]?.let { return it }
            val partial = midiIndexByTitle.entries.firstOrNull { it.key.contains(cleanTitle) || cleanTitle.contains(it.key) }
            if (partial != null) return partial.value
        }

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

        Log.w(TAG, "No exact MIDI file found for song: ${song.code} - ${song.title}")
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
