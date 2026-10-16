package com.example.videokey.ui.main

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.videokey.data.KaraokeLine
import com.example.videokey.data.MidiKaraokeParser
import com.example.videokey.data.Song
import com.example.videokey.data.SongRepository
import com.example.videokey.data.SoundEngine
import com.example.videokey.data.ShubBlobExtractor
import com.example.videokey.data.VkPackReader
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ActiveLyricPosition {
    TOP, BOTTOM, NONE
}

enum class BackgroundType {
    VIDEO, IMAGE, STAGE
}

enum class BackgroundChangeMode {
    AUTO_CHANGE_PER_SONG, CONTINUOUS
}

data class KaraokeBackground(
    val id: String,
    val name: String,
    val path: String,
    val type: BackgroundType
)

enum class LyricFontFamily(val displayName: String) {
    SAN_SERIF("Arial Bold (Sans)"),
    MONOSPACE("Retro Arcade (Mono)"),
    SERIF("Elegant Ballad (Serif)"),
    CURSIVE("Artistic Script")
}

enum class LyricWipeColor(val displayName: String, val activeColor: Long) {
    DARK_BLUE("Dark Blue (Videoke)", 0xFF1D4ED8),
    OCEAN_BLUE("Karaoke Blue (Live TV)", 0xFF0088FF),
    SUNSHINE_YELLOW("Sunshine Yellow (Classic)", 0xFFFFEB3B),
    ELECTRIC_CYAN("Electric Cyan (Neon)", 0xFF00E5FF),
    NEON_GREEN("Lime Green (Cyber)", 0xFF00E676),
    HOT_PINK("Hot Pink (Party)", 0xFFFF4081),
    VIBRANT_ORANGE("Sunset Orange (Rock)", 0xFFFF9100),
    PURE_GOLD("Pure Gold (Luxury)", 0xFFFFD54F)
}

enum class LyricShadowStyle(val displayName: String) {
    HEAVY_OUTLINE("Deep Dark Outline"),
    NEON_GLOW("Neon Ambient Glow"),
    SUBTLE_DROP("Subtle 3D Drop"),
    CLEAN("Clean (No Shadow)")
}

enum class LyricLineCount(val displayName: String, val lines: Int) {
    TWO_LINES("2 Lines (Dalawang Linya)", 2),
    FOUR_LINES("4 Lines (Apat na Linya)", 4)
}

enum class LyricScreenPosition(val displayName: String) {
    BOTTOM("⬇️ Ibaba (Lower Third)"),
    CENTER("⏺️ Gitna (Center / Middle)"),
    TOP("⬆️ Itaas (Top / Upper)")
}

data class LyricCustomization(
    val fontFamily: LyricFontFamily = LyricFontFamily.SAN_SERIF,
    val wipeColor: LyricWipeColor = LyricWipeColor.DARK_BLUE,
    val inactiveColor: Long = 0xFFFFFFFF, // Pure White with Black Outline
    val shadowStyle: LyricShadowStyle = LyricShadowStyle.HEAVY_OUTLINE,
    val isAllCaps: Boolean = true,
    val isItalic: Boolean = false,
    val fontScale: Float = 1.0f,
    val lineCount: LyricLineCount = LyricLineCount.TWO_LINES,
    val position: LyricScreenPosition = LyricScreenPosition.CENTER
)

enum class AppLanguage(val code: String, val displayName: String) {
    TAGALOG("tl", "🇵🇭 Tagalog (Default)"),
    ENGLISH("en", "🇺🇸 English")
}

data class VideoKeyUiState(
    val currentSong: Song = Song("000016", "Pasko Na, Pasko Na", "Noel Cabangon", "", "OPM"),
    val enteredCode: String = "",
    val matchedEnteredSong: Song? = null,
    val reserveQueue: List<Song> = emptyList(),
    val isPlaying: Boolean = false,
    val showSongTitleIntro: Boolean = false,
    // 4-Line Lyrics for Portrait Mode:
    val line1: String = "READY TO SING...",
    val line2: String = "PRESS PLAY OR ENTER SONG NUMBER",
    val line3: String = "CHOOSE YOUR FAVORITE SONG",
    val line4: String = "ENJOY PHILIPPINE VIDEOKE",
    val activeLineNumber: Int = 1, // 1, 2, 3, or 4 (which line is actively wiping)
    // 2-Line Lyrics for Landscape Mode (Lower Third):
    val landscapeLine1: String = "READY TO SING...",
    val landscapeLine2: String = "PRESS PLAY OR ENTER SONG NUMBER",
    val activeLyricPosition: ActiveLyricPosition = ActiveLyricPosition.TOP,
    val wipePercent: Float = 0f,
    val countdownDots: Int = 0,
    val isInstrumental: Boolean = false,
    val keyTranspose: Int = 0,
    val tempoPercent: Int = 100,
    val melodyPercent: Int = 70,
    val micEchoPercent: Int = 50,
    val score: Int = 94,
    val showScoreModal: Boolean = false,
    val isMicScoringEnabled: Boolean = true,
    val isScoringSoundEnabled: Boolean = true,
    val isKeypadVoiceEnabled: Boolean = true,
    val isSettingsDialogOpen: Boolean = false,
    val micLevel: Float = 0f,
    val isSingingDetected: Boolean = false,
    val pitchAccuracyPercent: Int = 0,
    val micSensitivity: Float = 1.0f,
    val currentSongFolder: String = "/storage/emulated/0/SongHub",
    val currentBackgroundFolder: String = "/storage/emulated/0/VideoKey/backgrounds",
    val isSongFolderPickerOpen: Boolean = false,
    val isBgFolderPickerOpen: Boolean = false,
    val activeSoundFontName: String = "Platinum Major HD 10",
    val availableSoundFonts: List<String> = listOf(
        "Platinum Major HD 10",
        "Dream SAM2634 v2.1",
        "MegaPro Sound",
        "Yamaha x Korg x Ketron"
    ),
    val isSoundFontDialogOpen: Boolean = false,
    val isSongbookOpen: Boolean = false,
    val isReserveQueueOpen: Boolean = false,
    val isFavoritesOpen: Boolean = false,
    val favoriteCodes: Set<String> = emptySet(),
    val favoriteSongs: List<Song> = emptyList(),
    val searchResults: List<Song> = emptyList(),
    val isTvMode: Boolean = false,
    val scanStatus: String = "Internal Storage: Ready",
    val isScanning: Boolean = false,
    // Recording state:
    val isRecording: Boolean = false,
    val recordingSeconds: Int = 0,
    val isRecordingsDialogOpen: Boolean = false,
    val savedRecordings: List<com.example.videokey.data.RecordedPerformance> = emptyList(),
    val playingRecordingPath: String? = null,
    val recordingBannerMessage: String? = null,
    // Background Video / Image / Stage Settings:
    val availableBackgrounds: List<KaraokeBackground> = listOf(
        KaraokeBackground("stage_neon", "✨ Neon Videoke Stage", "", BackgroundType.STAGE),
        KaraokeBackground("stage_sunset", "🌅 Sunset Arena", "sunset", BackgroundType.STAGE),
        KaraokeBackground("stage_disco", "🪩 Disco Lights", "disco", BackgroundType.STAGE)
    ),
    val selectedBackground: KaraokeBackground? = KaraokeBackground("stage_neon", "✨ Neon Videoke Stage", "", BackgroundType.STAGE),
    val backgroundChangeMode: BackgroundChangeMode = BackgroundChangeMode.CONTINUOUS,
    val isBackgroundDialogOpen: Boolean = false,
    // Lyric Font & Style Customization Settings:
    val lyricConfig: LyricCustomization = LyricCustomization(),
    val isLyricStyleDialogOpen: Boolean = false,
    val isAudioMixerDialogOpen: Boolean = false,
    val alertMessage: String? = null,
    val onlyShowPlayableSongs: Boolean = false,
    val playableCodes: Set<String> = emptySet(),
    val appLanguage: AppLanguage = AppLanguage.TAGALOG,
    val updateState: com.example.videokey.update.AppUpdateManager.UpdateState = com.example.videokey.update.AppUpdateManager.UpdateState.Idle,
    val isDownloadCenterOpen: Boolean = false,
    val downloadCatalog: List<com.example.videokey.data.ContentDownloadManager.DownloadItem> = emptyList(),
    val downloadStatus: com.example.videokey.data.ContentDownloadManager.DownloadStatus = com.example.videokey.data.ContentDownloadManager.DownloadStatus.Idle
)

class VideoKeyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SongRepository()
    private val soundEngine = SoundEngine(application.applicationContext)
    private val voiceManager = com.example.videokey.data.KeypadVoiceManager(application.applicationContext)
    private val karaokeRecorder = com.example.videokey.data.KaraokeRecorder()
    private val micScorer = com.example.videokey.data.MicrophoneScorer(karaokeRecorder)
    val appUpdateManager = com.example.videokey.update.AppUpdateManager(application.applicationContext)
    val contentDownloadManager = com.example.videokey.data.ContentDownloadManager(application.applicationContext)
    private val _uiState = MutableStateFlow(VideoKeyUiState())
    val uiState: StateFlow<VideoKeyUiState> = _uiState.asStateFlow()

    private val prefs = application.getSharedPreferences("videokey_prefs", android.content.Context.MODE_PRIVATE)
    private var playbackJob: Job? = null
    private var melodyReloadJob: Job? = null
    private var parsedKaraoke: MidiKaraokeParser.ParsedKaraoke? = null
    @Volatile
    private var currentRawMidiBytes: ByteArray? = null
    private var songStartTimeMs: Long = 0L

    init {
        // Detect initial song folder
        val savedSongFolder = prefs.getString("song_folder", null)
        val initialSongFolder = when {
            File("/storage/emulated/0/VideoKey").exists() -> "/storage/emulated/0/VideoKey"
            File("/sdcard/VideoKey").exists() -> "/sdcard/VideoKey"
            !savedSongFolder.isNullOrBlank() && File(savedSongFolder).exists() -> savedSongFolder
            File("/storage/emulated/0/SongHub").exists() -> "/storage/emulated/0/SongHub"
            File("/sdcard/SongHub").exists() -> "/sdcard/SongHub"
            else -> "/storage/emulated/0"
        }

        // Detect initial background folder
        val savedBgFolder = prefs.getString("background_folder", null)
        val initialBgFolder = when {
            !savedBgFolder.isNullOrBlank() && File(savedBgFolder).exists() -> savedBgFolder
            File("/storage/emulated/0/VideoKey/backgrounds").exists() -> "/storage/emulated/0/VideoKey/backgrounds"
            File("/sdcard/VideoKey/backgrounds").exists() -> "/sdcard/VideoKey/backgrounds"
            File("/storage/emulated/0/VideoKey/videos").exists() -> "/storage/emulated/0/VideoKey/videos"
            File("/sdcard/VideoKey/videos").exists() -> "/sdcard/VideoKey/videos"
            File("/storage/emulated/0/SongHub/bgv").exists() -> "/storage/emulated/0/SongHub/bgv"
            File("/sdcard/SongHub/bgv").exists() -> "/sdcard/SongHub/bgv"
            else -> "/storage/emulated/0"
        }

        val savedModeName = prefs.getString("background_change_mode", BackgroundChangeMode.CONTINUOUS.name) ?: BackgroundChangeMode.CONTINUOUS.name
        val initialBgMode = try {
            BackgroundChangeMode.valueOf(savedModeName)
        } catch (e: Exception) {
            BackgroundChangeMode.CONTINUOUS
        }

        val initialSensitivity = prefs.getFloat("mic_sensitivity", 1.0f)
        micScorer.micSensitivity = initialSensitivity

        val savedLang = prefs.getString("app_language", "tl") ?: "tl"
        val initialLang = if (savedLang == "en") AppLanguage.ENGLISH else AppLanguage.TAGALOG

        val savedSf = prefs.getString("active_soundfont", "platinum major. hd 10 soundbank sf2") ?: "platinum major. hd 10 soundbank sf2"
        soundEngine.setActiveSoundFont(savedSf)
        val initialMelody = prefs.getInt("melody_percent", 70) // Default 70% as requested!
        soundEngine.setMelodyVolume(initialMelody)

        val savedFavSet = prefs.getStringSet("favorite_song_codes", emptySet())?.toSet() ?: emptySet()
        val initialFavSongs = savedFavSet.mapNotNull { repository.findByCode(it) }

        val savedScoring = prefs.getBoolean("mic_scoring_enabled", true)
        val savedScoringSound = prefs.getBoolean("scoring_sound_enabled", true)

        _uiState.update {
            it.copy(
                searchResults = repository.search(""),
                currentSongFolder = initialSongFolder,
                currentBackgroundFolder = initialBgFolder,
                backgroundChangeMode = initialBgMode,
                micSensitivity = initialSensitivity,
                appLanguage = initialLang,
                activeSoundFontName = savedSf,
                melodyPercent = initialMelody,
                isMicScoringEnabled = savedScoring,
                isScoringSoundEnabled = savedScoringSound,
                favoriteCodes = savedFavSet,
                favoriteSongs = initialFavSongs,
                savedRecordings = karaokeRecorder.listSavedRecordings()
            )
        }

        scanBackgroundMedia(File(initialBgFolder))
        scanSongFolder(File(initialSongFolder))

        viewModelScope.launch {
            micScorer.micLevel.collect { level ->
                _uiState.update { it.copy(micLevel = level) }
            }
        }
        viewModelScope.launch {
            micScorer.isVoiceDetected.collect { isVoice ->
                _uiState.update { it.copy(isSingingDetected = isVoice) }
            }
        }
        viewModelScope.launch {
            micScorer.pitchAccuracyPercent.collect { pct ->
                _uiState.update { it.copy(pitchAccuracyPercent = pct) }
            }
        }
        viewModelScope.launch {
            appUpdateManager.updateState.collect { updState ->
                _uiState.update { it.copy(updateState = updState) }
            }
        }
        viewModelScope.launch {
            contentDownloadManager.downloadStatus.collect { status ->
                _uiState.update { it.copy(downloadStatus = status) }
                if (status is com.example.videokey.data.ContentDownloadManager.DownloadStatus.Completed) {
                    refreshDownloadCatalog()
                    scanSongFolder(File(_uiState.value.currentSongFolder))
                    scanBackgroundMedia(File(_uiState.value.currentBackgroundFolder))
                }
            }
        }
        refreshDownloadCatalog()
        // Background OTA Update Check across any internet connection (Wi-Fi, Data)
        appUpdateManager.checkForUpdates(autoDownloadIfNewer = true, isManual = false)
    }

    @Volatile
    private var isCompletingSong = false

    fun playSong(song: Song) {
        isCompletingSong = false
        playbackJob?.cancel()
        melodyReloadJob?.cancel()
        soundEngine.stop()
        karaokeRecorder.stopPlayingRecording()
        songStartTimeMs = System.currentTimeMillis()

        if (_uiState.value.backgroundChangeMode == BackgroundChangeMode.AUTO_CHANGE_PER_SONG) {
            advanceNextBackground()
        }

        if (_uiState.value.isMicScoringEnabled || _uiState.value.isRecording) {
            micScorer.startScoring()
        }

        _uiState.update {
            it.copy(
                currentSong = song,
                isPlaying = true,
                enteredCode = "",
                matchedEnteredSong = null,
                wipePercent = 0f,
                countdownDots = 0,
                showSongTitleIntro = true,
                isInstrumental = false,
                playingRecordingPath = null,
                line1 = song.title.uppercase(),
                line2 = song.artist.uppercase(),
                line3 = "",
                line4 = "",
                activeLineNumber = 0,
                landscapeLine1 = song.title.uppercase(),
                landscapeLine2 = song.artist.uppercase(),
                activeLyricPosition = ActiveLyricPosition.TOP
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            var rawMidiBytes: ByteArray? = null
            var isTempFile = false
            var midiFile: File? = null
            val currentMelody = _uiState.value.melodyPercent
            val currentSoundFont = _uiState.value.activeSoundFontName
            soundEngine.setActiveSoundFont(currentSoundFont)

            // 1. Encrypted .vkpack song
            if (song.isPacked) {
                rawMidiBytes = VkPackReader.readSongBytes(song)
            }

            // 2. Direct / indexed .mid or .kar file
            if (rawMidiBytes == null && !song.isPacked) {
                val found = soundEngine.findMidiFileForSong(song)
                if (found != null && found.exists()) {
                    try {
                        rawMidiBytes = found.readBytes()
                    } catch (e: Exception) {
                        midiFile = found
                    }
                }
            }

            // 3. SongHub shubblob sector extraction
            if (rawMidiBytes == null && midiFile == null && song.sectorOffset >= 0L) {
                val currentFolder = File(_uiState.value.currentSongFolder)
                val candidateDirs = listOfNotNull(
                    currentFolder,
                    File(currentFolder, "blob"),
                    File(currentFolder, "idx"),
                    if (song.filePath.isNotBlank()) File(song.filePath).parentFile else null,
                    File("/storage/emulated/0/SongHub/blob"),
                    File("/sdcard/SongHub/blob"),
                    File("/storage/emulated/0/VideoKey/idx"),
                    File("/sdcard/VideoKey/idx"),
                    File("/storage/emulated/0/SongHub/idx"),
                    File("/sdcard/SongHub/idx")
                )
                val extractedFile = ShubBlobExtractor.extractMidi(
                    sectorOffset = song.sectorOffset,
                    sectorCount = song.sectorCount,
                    cacheDir = getApplication<Application>().cacheDir,
                    songCode = song.code,
                    customBlobDirs = candidateDirs
                )
                if (extractedFile != null && extractedFile.exists()) {
                    try {
                        rawMidiBytes = extractedFile.readBytes()
                        extractedFile.delete()
                    } catch (e: Exception) {
                        midiFile = extractedFile
                        isTempFile = true
                    }
                }
            }

            currentRawMidiBytes = rawMidiBytes

            // Apply active SoundFont timbre + Melody Guide volume scaling to rawMidiBytes
            if (rawMidiBytes != null) {
                val processedBytes = try {
                    com.example.videokey.data.MidiMelodyScaler.scaleMelody(
                        midiBytes = rawMidiBytes,
                        volumePercent = currentMelody,
                        soundFontName = currentSoundFont
                    )
                } catch (e: Exception) {
                    rawMidiBytes
                }
                midiFile = VkPackReader.extractToPrivateTemp(song, processedBytes, getApplication<Application>().cacheDir)
                isTempFile = midiFile != null
            }

            Log.d("VideoKeyViewModel", "Playing song: ${song.title} (${song.code}), SoundFont: $currentSoundFont, Melody: $currentMelody%, MIDI: ${midiFile?.absolutePath}")

            val parsed = if (rawMidiBytes != null) {
                try {
                    MidiKaraokeParser.parse(rawMidiBytes)
                } catch (e: Exception) {
                    Log.e("VideoKeyViewModel", "Failed to parse raw MIDI lyrics", e)
                    null
                }
            } else if (midiFile != null && midiFile.exists()) {
                try {
                    MidiKaraokeParser.parse(midiFile)
                } catch (e: Exception) {
                    Log.e("VideoKeyViewModel", "Failed to parse MIDI lyrics", e)
                    null
                }
            } else null

            withContext(Dispatchers.Main) {
                parsedKaraoke = parsed
                if (midiFile != null && midiFile.exists()) {
                    if (_uiState.value.isRecording && !karaokeRecorder.isRecording) {
                        karaokeRecorder.startRecording(song.code, song.title, parsed?.accompNotes.orEmpty())
                    } else if (karaokeRecorder.isRecording) {
                        karaokeRecorder.updatePlaybackState(0L, _uiState.value.keyTranspose, parsed?.accompNotes.orEmpty())
                    }
                    soundEngine.playMidi(midiFile.absolutePath, deleteAfterOpen = isTempFile) {
                        onSongCompleted()
                    }
                    startRealtimeLyricLoop()
                } else {
                    Log.w("VideoKeyViewModel", "MIDI file not found for ${song.title} (${song.code})")
                    if (!_uiState.value.isRecording) {
                        micScorer.stopScoring()
                    }
                    _uiState.update {
                        it.copy(
                            isPlaying = false,
                            showSongTitleIntro = false,
                            isInstrumental = false,
                            countdownDots = 0,
                            alertMessage = "⚠️ Walang audio file para sa [${song.code}] \"${song.title}\". Mangyaring maglagay ng \"${song.code}.mid\" sa /sdcard/VideoKey/songs/ folder upang mapatugtog ito.",
                            line1 = "NO MIDI FILE FOUND",
                            line2 = "WALANG FILE PARA SA: ${song.title.take(24).uppercase()}",
                            line3 = "PUMILI NG MAY 🎵 BADGE SA SONGBOOK",
                            line4 = "",
                            activeLineNumber = 1,
                            landscapeLine1 = "NO MIDI FILE FOUND",
                            landscapeLine2 = song.title.take(24).uppercase(),
                            wipePercent = 0f
                        )
                    }
                }
            }
        }
    }

    private fun startRealtimeLyricLoop() {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            while (true) {
                delay(40)
                if (!_uiState.value.isPlaying) continue

                val isAudioPlaying = soundEngine.isPlaying()
                val posMs = soundEngine.getCurrentPositionMs()
                val durMs = soundEngine.getDurationMs()
                val elapsedSinceStart = System.currentTimeMillis() - songStartTimeMs
                val lines = parsedKaraoke?.lines.orEmpty()
                val melodyNotes = parsedKaraoke?.melodyNotes.orEmpty()
                val firstLineStart = lines.firstOrNull()?.startMs ?: 10000L
                val isTitleIntroActive = elapsedSinceStart < 4500L && (firstLineStart - posMs > 3000L)

                // Update recorder & melody pitch target for accurate scoring
                val activeMelodyNote = melodyNotes.firstOrNull { posMs in it.startMs..it.endMs }
                val activeLyricLine = lines.firstOrNull { posMs in it.startMs..it.endMs }
                val targetPitch = if (activeMelodyNote != null) {
                    (activeMelodyNote.midiNote + _uiState.value.keyTranspose).coerceIn(0, 127)
                } else {
                    -1
                }
                micScorer.updateTargetMelody(
                    isVocalActive = activeLyricLine != null || activeMelodyNote != null,
                    midiNote = targetPitch
                )
                if (karaokeRecorder.isRecording) {
                    karaokeRecorder.updatePlaybackState(posMs, _uiState.value.keyTranspose)
                    _uiState.update { it.copy(recordingSeconds = karaokeRecorder.getRecordingDurationSeconds()) }
                }

                // Auto-detect end of song for instant auto-play of reserve queue
                if (elapsedSinceStart > 5000L && ((durMs > 3000L && posMs >= durMs - 250L) || (!isAudioPlaying && posMs > 2000L && melodyReloadJob?.isActive != true))) {
                    withContext(Dispatchers.Main) {
                        onSongCompleted()
                    }
                    break
                }

                if (lines.isNotEmpty() && isAudioPlaying) {
                    val activeIndex = lines.indexOfFirst { posMs in it.startMs..it.endMs }
                    if (activeIndex != -1) {
                        val currentLine = lines[activeIndex]
                        val nextLine = lines.getOrNull(activeIndex + 1)
                        // Synchronize wipe highlight with exact melody notes and syllables
                        val wipe = computeMelodyWipe(currentLine, posMs)

                        // 1. Portrait 4-line calculation (Page of 4 lines)
                        val pageStart = (activeIndex / 4) * 4
                        val p1 = lines.getOrNull(pageStart)?.fullText?.uppercase() ?: ""
                        val p2 = lines.getOrNull(pageStart + 1)?.fullText?.uppercase() ?: ""
                        val p3 = lines.getOrNull(pageStart + 2)?.fullText?.uppercase() ?: ""
                        val p4 = lines.getOrNull(pageStart + 3)?.fullText?.uppercase() ?: ""
                        val lineInPage = (activeIndex % 4) + 1

                        // 2. Landscape 2-line alternating calculation
                        val isEven = activeIndex % 2 == 0
                        val land1 = if (isEven) currentLine.fullText.uppercase() else (nextLine?.fullText?.uppercase() ?: "")
                        val land2 = if (isEven) (nextLine?.fullText?.uppercase() ?: "") else currentLine.fullText.uppercase()
                        val landPos = if (isEven) ActiveLyricPosition.TOP else ActiveLyricPosition.BOTTOM

                        _uiState.update {
                            it.copy(
                                showSongTitleIntro = false,
                                line1 = p1,
                                line2 = p2,
                                line3 = p3,
                                line4 = p4,
                                activeLineNumber = lineInPage,
                                landscapeLine1 = land1,
                                landscapeLine2 = land2,
                                activeLyricPosition = landPos,
                                wipePercent = wipe,
                                countdownDots = 0,
                                isInstrumental = false
                            )
                        }
                    } else {
                        // Between lines or intro: find upcoming line
                        val upcomingIndex = lines.indexOfFirst { it.startMs > posMs }
                        if (upcomingIndex != -1) {
                            val upcoming = lines[upcomingIndex]
                            val nextUpcoming = lines.getOrNull(upcomingIndex + 1)
                            val timeUntilStart = upcoming.startMs - posMs

                            // Long instrumental gap detection:
                            val prevLineEndMs = if (upcomingIndex > 0) lines[upcomingIndex - 1].endMs else 0L
                            val instrumentalGap = upcoming.startMs - prevLineEndMs
                            val isAfterLongInstrumental = instrumentalGap >= 4500L

                            // Countdown: 4 seconds before vocals start ONLY after a long instrumental or song intro!
                            val dots = if (isAfterLongInstrumental) {
                                when {
                                    timeUntilStart in 3001L..4000L -> 4
                                    timeUntilStart in 2001L..3000L -> 3
                                    timeUntilStart in 1001L..2000L -> 2
                                    timeUntilStart in 1L..1000L -> 1
                                    else -> 0
                                }
                            } else {
                                0
                            }

                            val showInstrumental = !isTitleIntroActive && dots == 0 && (timeUntilStart > 3500L) && isAfterLongInstrumental

                            val pageStart = (upcomingIndex / 4) * 4
                            val p1 = lines.getOrNull(pageStart)?.fullText?.uppercase() ?: ""
                            val p2 = lines.getOrNull(pageStart + 1)?.fullText?.uppercase() ?: ""
                            val p3 = lines.getOrNull(pageStart + 2)?.fullText?.uppercase() ?: ""
                            val p4 = lines.getOrNull(pageStart + 3)?.fullText?.uppercase() ?: ""
                            val lineInPage = (upcomingIndex % 4) + 1

                            val isEven = upcomingIndex % 2 == 0
                            val land1 = if (isEven) upcoming.fullText.uppercase() else (nextUpcoming?.fullText?.uppercase() ?: "")
                            val land2 = if (isEven) (nextUpcoming?.fullText?.uppercase() ?: "") else upcoming.fullText.uppercase()
                            val landPos = if (isEven) ActiveLyricPosition.TOP else ActiveLyricPosition.BOTTOM

                            _uiState.update {
                                it.copy(
                                    showSongTitleIntro = isTitleIntroActive,
                                    line1 = p1,
                                    line2 = p2,
                                    line3 = p3,
                                    line4 = p4,
                                    activeLineNumber = lineInPage,
                                    landscapeLine1 = land1,
                                    landscapeLine2 = land2,
                                    activeLyricPosition = landPos,
                                    wipePercent = 0f,
                                    countdownDots = dots,
                                    isInstrumental = showInstrumental
                                )
                            }
                        } else {
                            // Song outro / after last lyric
                            _uiState.update {
                                it.copy(
                                    showSongTitleIntro = false,
                                    wipePercent = 0f,
                                    countdownDots = 0,
                                    isInstrumental = true
                                )
                            }
                        }
                    }
                } else {
                    // Fallback lyric wipe animation if MIDI has no lyrics or audio loading
                    var wipe = _uiState.value.wipePercent + 0.015f
                    if (wipe > 1.0f) wipe = 0f
                    val step = ((wipe * 4).toInt() % 4) + 1
                    val landPos = if (step % 2 == 1) ActiveLyricPosition.TOP else ActiveLyricPosition.BOTTOM
                    val dots = when {
                        elapsedSinceStart in 4500L..5500L -> 4
                        elapsedSinceStart in 5501L..6500L -> 3
                        elapsedSinceStart in 6501L..7500L -> 2
                        elapsedSinceStart in 7501L..8500L -> 1
                        else -> 0
                    }
                    val isInst = !isTitleIntroActive && dots == 0 && elapsedSinceStart > 8500L
                    _uiState.update {
                        it.copy(
                            showSongTitleIntro = isTitleIntroActive,
                            line1 = it.currentSong.title.uppercase(),
                            line2 = it.currentSong.artist.uppercase(),
                            line3 = "MUSIC PLAYING LIVE",
                            line4 = "KARAOKE INSTRUMENTAL",
                            activeLineNumber = step,
                            landscapeLine1 = it.currentSong.title.uppercase(),
                            landscapeLine2 = it.currentSong.artist.uppercase(),
                            activeLyricPosition = landPos,
                            wipePercent = wipe,
                            countdownDots = dots,
                            isInstrumental = isInst
                        )
                    }
                }
            }
        }
    }

    private fun computeMelodyWipe(line: KaraokeLine, posMs: Long): Float {
        val syllables = line.syllables
        if (syllables.isEmpty()) return 0f
        if (posMs < syllables.first().timestampMs) return 0f
        val lastSyl = syllables.last()
        val lineEnd = lastSyl.timestampMs + lastSyl.durationMs
        if (posMs >= lineEnd) return 1f

        val totalChars = syllables.sumOf { it.text.length }.coerceAtLeast(1)
        var charsPassed = 0
        for (syl in syllables) {
            val sylLen = syl.text.length
            val sylStart = syl.timestampMs
            val sylEnd = syl.timestampMs + syl.durationMs
            if (posMs < sylStart) {
                break
            } else if (posMs in sylStart..sylEnd) {
                val dur = syl.durationMs.coerceAtLeast(1L)
                val frac = ((posMs - sylStart).toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                val activeChars = charsPassed + (frac * sylLen)
                return (activeChars / totalChars.toFloat()).coerceIn(0f, 1f)
            } else {
                charsPassed += sylLen
            }
        }
        return (charsPassed.toFloat() / totalChars.toFloat()).coerceIn(0f, 1f)
    }

    private fun onSongCompleted() {
        if (isCompletingSong) return
        isCompletingSong = true

        // If recording was active, save the WAV file automatically!
        var savedBanner: String? = null
        if (_uiState.value.isRecording || karaokeRecorder.isRecording) {
            val saved = karaokeRecorder.stopAndSaveRecording()
            if (saved != null) {
                savedBanner = "✅ Na-save ang Recording: ${saved.fileName}"
            }
        }
        micScorer.stopScoring()

        val showModal = _uiState.value.isMicScoringEnabled
        val finalScore = if (showModal) {
            micScorer.calculateFinalScore()
        } else {
            (92..99).random()
        }

        if (showModal && _uiState.value.isScoringSoundEnabled) {
            com.example.videokey.data.VideokeSoundEffects.playScoringFanfare(finalScore)
        }

        _uiState.update {
            it.copy(
                score = finalScore,
                showScoreModal = showModal,
                showSongTitleIntro = false,
                isInstrumental = false,
                isRecording = false,
                recordingSeconds = 0,
                savedRecordings = karaokeRecorder.listSavedRecordings(),
                recordingBannerMessage = savedBanner
            )
        }

        viewModelScope.launch {
            if (showModal) {
                delay(2800)
                closeScoreModal()
            } else {
                delay(300)
            }
            onNext()
        }
    }

    fun onDigitPressed(digit: Char) {
        if (!_uiState.value.isPlaying && _uiState.value.isKeypadVoiceEnabled && digit in '0'..'9') {
            voiceManager.speakDigit(digit)
        }
        _uiState.update {
            val newCode = if (it.enteredCode.length >= 6) "$digit" else it.enteredCode + digit
            val padded = newCode.padStart(6, '0')
            val matched = repository.findByCode(padded) ?: repository.findByCode(newCode)
            it.copy(
                enteredCode = newCode,
                matchedEnteredSong = matched
            )
        }
    }

    fun onClearCode() {
        _uiState.update {
            it.copy(
                enteredCode = "",
                matchedEnteredSong = null
            )
        }
    }

    fun onDeleteDigit() {
        _uiState.update {
            val newCode = if (it.enteredCode.isNotEmpty()) it.enteredCode.dropLast(1) else ""
            val matched = if (newCode.isNotEmpty()) {
                val padded = newCode.padStart(6, '0')
                repository.findByCode(padded) ?: repository.findByCode(newCode)
            } else null
            it.copy(
                enteredCode = newCode,
                matchedEnteredSong = matched
            )
        }
    }

    fun onReserve() {
        val code = _uiState.value.enteredCode
        val padded = code.padStart(6, '0')
        val matched = _uiState.value.matchedEnteredSong ?: if (code.isNotBlank()) (repository.findByCode(padded) ?: repository.findByCode(code)) else null
        val song = matched ?: if (code.isNotBlank()) Song(padded, "Track $padded", "Videoke Song", "") else null
        if (song != null) {
            _uiState.update {
                it.copy(
                    reserveQueue = it.reserveQueue + song,
                    enteredCode = "",
                    matchedEnteredSong = null
                )
            }
        }
    }

    /**
     * Triggered when pressing the 'R' button on the keypad:
     * - If a song code is currently entered, reserves it automatically.
     * - Otherwise toggles the Reserved Songs ('R') overlay form (same style as Songbook Search form).
     */
    fun onRButtonPressed() {
        if (_uiState.value.enteredCode.isNotBlank()) {
            onReserve()
        } else {
            toggleReserveQueueOverlay()
        }
    }

    fun toggleReserveQueueOverlay() {
        _uiState.update {
            val nextOpen = !it.isReserveQueueOpen
            it.copy(
                isReserveQueueOpen = nextOpen,
                isSongbookOpen = false,
                isFavoritesOpen = false
            )
        }
    }

    fun openReserveQueue() {
        _uiState.update {
            it.copy(
                isReserveQueueOpen = true,
                isSongbookOpen = false,
                isFavoritesOpen = false
            )
        }
    }

    fun closeReserveQueue() {
        _uiState.update { it.copy(isReserveQueueOpen = false) }
    }

    fun closeReserveQueueOverlay() {
        closeReserveQueue()
    }

    fun removeFromReserveQueue(index: Int) {
        _uiState.update {
            if (index in it.reserveQueue.indices) {
                val mutable = it.reserveQueue.toMutableList()
                mutable.removeAt(index)
                it.copy(reserveQueue = mutable)
            } else it
        }
    }

    fun playFromReserveQueue(index: Int) {
        val queue = _uiState.value.reserveQueue
        if (index in queue.indices) {
            val song = queue[index]
            val mutable = queue.toMutableList()
            mutable.removeAt(index)
            _uiState.update {
                it.copy(
                    reserveQueue = mutable,
                    isReserveQueueOpen = false
                )
            }
            playSong(song)
        }
    }

    fun toggleFavoritesOverlay() {
        _uiState.update {
            val nextOpen = !it.isFavoritesOpen
            it.copy(
                isFavoritesOpen = nextOpen,
                isSongbookOpen = false,
                isReserveQueueOpen = false,
                favoriteSongs = it.favoriteCodes.mapNotNull { code -> repository.findByCode(code) }
            )
        }
    }

    fun openFavorites() {
        _uiState.update {
            it.copy(
                isFavoritesOpen = true,
                isSongbookOpen = false,
                isReserveQueueOpen = false,
                favoriteSongs = it.favoriteCodes.mapNotNull { code -> repository.findByCode(code) }
            )
        }
    }

    fun closeFavorites() {
        _uiState.update { it.copy(isFavoritesOpen = false) }
    }

    fun closeFavoritesOverlay() {
        closeFavorites()
    }

    fun toggleFavoriteSong(song: Song) {
        val currentFavs = _uiState.value.favoriteCodes.toMutableSet()
        if (currentFavs.contains(song.code)) {
            currentFavs.remove(song.code)
        } else {
            currentFavs.add(song.code)
        }
        prefs.edit().putStringSet("favorite_song_codes", currentFavs).apply()
        val favSongList = currentFavs.mapNotNull { code -> repository.findByCode(code) }
        _uiState.update {
            it.copy(
                favoriteCodes = currentFavs,
                favoriteSongs = favSongList
            )
        }
    }

    fun toggleRecording() {
        if (_uiState.value.isRecording) {
            stopAndSaveRecording()
        } else {
            startRecordingNow()
        }
    }

    fun startRecordingNow() {
        val song = _uiState.value.currentSong
        val started = karaokeRecorder.startRecording(
            songCode = song.code,
            songTitle = song.title,
            accompNotes = parsedKaraoke?.accompNotes.orEmpty()
        )
        if (started) {
            micScorer.startScoring()
            _uiState.update {
                it.copy(
                    isRecording = true,
                    recordingSeconds = 0,
                    recordingBannerMessage = "🔴 Nagre-record ng Boses at Tugtog: ${song.title}"
                )
            }
        }
    }

    fun stopAndSaveRecording() {
        val saved = karaokeRecorder.stopAndSaveRecording()
        if (!_uiState.value.isPlaying) {
            micScorer.stopScoring()
        }
        val list = karaokeRecorder.listSavedRecordings()
        _uiState.update {
            it.copy(
                isRecording = false,
                recordingSeconds = 0,
                savedRecordings = list,
                recordingBannerMessage = if (saved != null) "✅ Na-save ang Recording: ${saved.fileName}" else null
            )
        }
    }

    fun openRecordingsDialog() {
        _uiState.update {
            it.copy(
                isRecordingsDialogOpen = true,
                savedRecordings = karaokeRecorder.listSavedRecordings()
            )
        }
    }

    fun closeRecordingsDialog() {
        karaokeRecorder.stopPlayingRecording()
        _uiState.update {
            it.copy(
                isRecordingsDialogOpen = false,
                playingRecordingPath = null
            )
        }
    }

    fun playSavedRecording(path: String) {
        if (_uiState.value.playingRecordingPath == path) {
            karaokeRecorder.stopPlayingRecording()
            _uiState.update { it.copy(playingRecordingPath = null) }
        } else {
            if (_uiState.value.isPlaying) {
                soundEngine.pause()
            }
            _uiState.update { it.copy(playingRecordingPath = path) }
            karaokeRecorder.playRecording(path) {
                _uiState.update { it.copy(playingRecordingPath = null) }
            }
        }
    }

    fun deleteSavedRecording(path: String) {
        karaokeRecorder.deleteRecording(path)
        _uiState.update {
            it.copy(
                savedRecordings = karaokeRecorder.listSavedRecordings(),
                playingRecordingPath = karaokeRecorder.currentlyPlayingRecordingPath
            )
        }
    }

    fun onPlay() {
        val code = _uiState.value.enteredCode
        if (code.isNotBlank()) {
            val padded = code.padStart(6, '0')
            val song = _uiState.value.matchedEnteredSong ?: repository.findByCode(padded) ?: repository.findByCode(code) ?: Song(padded, "Track $padded", "Videoke Song", "")
            _uiState.update { it.copy(enteredCode = "", matchedEnteredSong = null) }
            playSong(song)
        } else if (_uiState.value.reserveQueue.isNotEmpty()) {
            val nextSong = _uiState.value.reserveQueue.first()
            _uiState.update { it.copy(reserveQueue = it.reserveQueue.drop(1)) }
            playSong(nextSong)
        } else if (!_uiState.value.isPlaying) {
            playSong(_uiState.value.currentSong)
        } else {
            playSong(_uiState.value.currentSong)
        }
    }

    fun onNext() {
        if (_uiState.value.isRecording || karaokeRecorder.isRecording) {
            stopAndSaveRecording()
        }
        val queue = _uiState.value.reserveQueue
        if (queue.isNotEmpty()) {
            val nextSong = queue.first()
            _uiState.update {
                it.copy(
                    reserveQueue = queue.drop(1),
                    enteredCode = "",
                    matchedEnteredSong = null
                )
            }
            playSong(nextSong)
        } else {
            soundEngine.stop()
            _uiState.update {
                it.copy(
                    isPlaying = false,
                    enteredCode = "",
                    matchedEnteredSong = null,
                    wipePercent = 0f,
                    countdownDots = 0
                )
            }
        }
    }

    fun onStop() {
        if (_uiState.value.isRecording || karaokeRecorder.isRecording) {
            stopAndSaveRecording()
        }
        val queue = _uiState.value.reserveQueue
        if (queue.isNotEmpty()) {
            onNext()
        } else {
            soundEngine.stop()
            micScorer.stopScoring()
            com.example.videokey.data.VideokeSoundEffects.stop()
            playbackJob?.cancel()
            _uiState.update {
                it.copy(
                    isPlaying = false,
                    wipePercent = 0f,
                    countdownDots = 0,
                    showSongTitleIntro = false,
                    isInstrumental = false
                )
            }
        }
    }

    fun adjustMelody(delta: Int) {
        val newVol = (_uiState.value.melodyPercent + delta).coerceIn(0, 100)
        soundEngine.setMelodyVolume(newVol)
        prefs.edit().putInt("melody_percent", newVol).apply()
        _uiState.update {
            it.copy(melodyPercent = newVol)
        }
        // Live hot-reload of scaled MIDI at current position if a song is currently playing!
        val rawBytes = currentRawMidiBytes
        if (_uiState.value.isPlaying && rawBytes != null) {
            melodyReloadJob?.cancel()
            melodyReloadJob = viewModelScope.launch(Dispatchers.IO) {
                delay(150) // Debounce rapid taps/slider drags
                val currentPos = soundEngine.getCurrentPositionMs()
                val song = _uiState.value.currentSong
                val currentSf = _uiState.value.activeSoundFontName
                val scaledBytes = try {
                    com.example.videokey.data.MidiMelodyScaler.scaleMelody(
                        midiBytes = rawBytes,
                        volumePercent = newVol,
                        soundFontName = currentSf
                    )
                } catch (e: Exception) {
                    rawBytes
                }
                val tempFile = VkPackReader.extractToPrivateTemp(song, scaledBytes, getApplication<Application>().cacheDir)
                if (tempFile != null && tempFile.exists()) {
                    withContext(Dispatchers.Main) {
                        if (_uiState.value.isPlaying) {
                            soundEngine.playMidi(
                                filePath = tempFile.absolutePath,
                                deleteAfterOpen = true,
                                resumePositionMs = currentPos
                            ) {
                                onSongCompleted()
                            }
                        }
                    }
                }
            }
        }
    }

    fun adjustKey(delta: Int) {
        soundEngine.setKeyTranspose(_uiState.value.keyTranspose + delta)
        _uiState.update {
            it.copy(keyTranspose = soundEngine.getKeyTranspose())
        }
    }

    fun adjustTempo(delta: Int) {
        soundEngine.setTempo(_uiState.value.tempoPercent + delta)
        _uiState.update {
            it.copy(tempoPercent = soundEngine.getTempo())
        }
    }

    fun setMicEcho(percent: Int) {
        soundEngine.setMicEcho(percent)
        _uiState.update { it.copy(micEchoPercent = percent.coerceIn(0, 100)) }
    }

    fun openSongbook() {
        _uiState.update {
            it.copy(
                isSongbookOpen = true,
                isReserveQueueOpen = false,
                isFavoritesOpen = false
            )
        }
    }

    fun closeSongbook() {
        _uiState.update { it.copy(isSongbookOpen = false) }
    }

    private var searchJob: Job? = null
    fun searchSongbook(query: String, onlyPlayable: Boolean = _uiState.value.onlyShowPlayableSongs) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch(Dispatchers.Default) {
            val playables = soundEngine.getPlayableCodes()
            val results = repository.search(query, onlyPlayable) { playables.contains(it) }
            _uiState.update {
                it.copy(
                    searchResults = results,
                    onlyShowPlayableSongs = onlyPlayable,
                    playableCodes = playables
                )
            }
        }
    }

    fun toggleSongbookFilter(onlyPlayable: Boolean) {
        _uiState.update { it.copy(onlyShowPlayableSongs = onlyPlayable) }
        searchSongbook("", onlyPlayable)
    }

    fun clearAlertMessage() {
        _uiState.update { it.copy(alertMessage = null) }
    }

    fun findSongByCode(code: String): Song? = repository.findByCode(code)

    fun searchSongs(query: String): List<Song> = repository.search(query)

    fun selectSong(song: Song) {
        _uiState.update {
            it.copy(
                enteredCode = "",
                matchedEnteredSong = null,
                isSongbookOpen = false,
                isReserveQueueOpen = false,
                isFavoritesOpen = false,
                alertMessage = null
            )
        }
        playSong(song)
    }

    fun reserveSong(song: Song) {
        _uiState.update {
            it.copy(
                reserveQueue = it.reserveQueue + song
            )
        }
    }

    fun triggerScoreModal() {
        _uiState.update { it.copy(showScoreModal = true, score = (92..99).random()) }
    }

    fun closeScoreModal() {
        com.example.videokey.data.VideokeSoundEffects.stop()
        _uiState.update { it.copy(showScoreModal = false) }
    }

    fun toggleTvMode(activity: android.app.Activity? = null) {
        val nextTv = !_uiState.value.isTvMode
        _uiState.update { it.copy(isTvMode = nextTv) }
        if (nextTv) {
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    fun enterTvLandscapeMode(activity: android.app.Activity? = null) {
        _uiState.update { it.copy(isTvMode = true) }
        activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

    fun exitLandscapeMode(activity: android.app.Activity? = null) {
        _uiState.update { it.copy(isTvMode = false) }
        activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    @Volatile private var wasPlayingBeforePause = false

    fun onAppPause() {
        if (_uiState.value.isPlaying && soundEngine.isPlaying()) {
            wasPlayingBeforePause = true
            soundEngine.pause()
        }
    }

    fun onAppResume() {
        if (wasPlayingBeforePause && _uiState.value.isPlaying) {
            wasPlayingBeforePause = false
            soundEngine.resume()
        }
    }

    fun openSoundFontDialog() {
        _uiState.update { it.copy(isSoundFontDialogOpen = true) }
    }

    fun closeSoundFontDialog() {
        _uiState.update { it.copy(isSoundFontDialogOpen = false) }
    }

    fun selectSoundFont(name: String) {
        prefs.edit().putString("active_soundfont", name).apply()
        soundEngine.setActiveSoundFont(name)
        val resolvedName = soundEngine.getActiveSoundFont().displayName
        val currentlyPlaying = _uiState.value.isPlaying
        val songToRestart = _uiState.value.currentSong
        val isSongValid = songToRestart.code.isNotBlank()
        _uiState.update {
            it.copy(
                activeSoundFontName = resolvedName,
                isSoundFontDialogOpen = false,
                alertMessage = "🎛️ Na-apply ang SoundFont: $resolvedName"
            )
        }
        if (currentlyPlaying && isSongValid) {
            soundEngine.stop()
            playSong(songToRestart)
        }
    }

    fun checkForUpdates() {
        appUpdateManager.checkForUpdates(autoDownloadIfNewer = true, isManual = true)
    }

    fun retryUpdate() {
        appUpdateManager.checkForUpdates(autoDownloadIfNewer = true, isManual = true)
    }

    fun promptInstallDownloadedApk(apkFile: java.io.File) {
        appUpdateManager.promptInstall(apkFile)
    }

    fun dismissUpdateDialog() {
        appUpdateManager.dismiss()
    }

    fun refreshDownloadCatalog() {
        val catalog = contentDownloadManager.getAvailableCatalog()
        _uiState.update { it.copy(downloadCatalog = catalog) }
    }

    fun openDownloadCenter() {
        refreshDownloadCatalog()
        _uiState.update { it.copy(isDownloadCenterOpen = true) }
    }

    fun closeDownloadCenter() {
        _uiState.update { it.copy(isDownloadCenterOpen = false) }
    }

    fun downloadContentItem(item: com.example.videokey.data.ContentDownloadManager.DownloadItem) {
        contentDownloadManager.downloadItem(item) {
            refreshDownloadCatalog()
            scanSongFolder(File(_uiState.value.currentSongFolder))
            scanBackgroundMedia(File(_uiState.value.currentBackgroundFolder))
        }
    }

    fun cancelContentDownload() {
        contentDownloadManager.cancelCurrentDownload()
    }

    fun dismissDownloadStatus() {
        contentDownloadManager.dismissStatus()
    }

    fun scanInternalStorage(context: android.content.Context) {
        scanSongFolder(File(_uiState.value.currentSongFolder))
    }

    fun setSongFolder(path: String) {
        prefs.edit().putString("song_folder", path).apply()
        _uiState.update { it.copy(currentSongFolder = path, isSongFolderPickerOpen = false) }
        scanSongFolder(File(path))
    }

    fun scanSongFolder(folder: File = File(_uiState.value.currentSongFolder)) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _uiState.update { it.copy(isScanning = true, scanStatus = "Scanning ${folder.name}...") }
            soundEngine.indexFolderMidi(folder)
            val result = repository.scanFolder(folder, getApplication<Application>().applicationContext)
            val count = repository.getTotalSongsCount()
            val sfCount = result.soundFonts.size
            val statusMsg = "Folder [${folder.name}]: ${count} songs, ${sfCount} SoundFonts"
            val sfNames = if (result.soundFonts.isNotEmpty()) {
                result.soundFonts.map { it.nameWithoutExtension }
            } else {
                _uiState.value.availableSoundFonts
            }
            val savedSf = prefs.getString("active_soundfont", null)
            val resolvedSf = when {
                !savedSf.isNullOrBlank() && sfNames.any { it.equals(savedSf, ignoreCase = true) } ->
                    sfNames.first { it.equals(savedSf, ignoreCase = true) }
                result.soundFonts.isNotEmpty() -> result.soundFonts.first().nameWithoutExtension
                else -> _uiState.value.activeSoundFontName
            }
            soundEngine.setActiveSoundFont(resolvedSf)
            val playables = soundEngine.getPlayableCodes()
            val favSongs = _uiState.value.favoriteCodes.mapNotNull { code -> repository.findByCode(code) }
            _uiState.update {
                it.copy(
                    isScanning = false,
                    scanStatus = statusMsg,
                    searchResults = repository.search("", it.onlyShowPlayableSongs) { c -> playables.contains(c) },
                    playableCodes = playables,
                    availableSoundFonts = sfNames,
                    activeSoundFontName = resolvedSf,
                    favoriteSongs = favSongs
                )
            }
        }
    }

    fun setBackgroundFolder(path: String) {
        prefs.edit().putString("background_folder", path).apply()
        _uiState.update { it.copy(currentBackgroundFolder = path, isBgFolderPickerOpen = false) }
        scanBackgroundMedia(File(path))
    }

    private val playlistLock = Any()
    private val backgroundPlaylist = mutableListOf<KaraokeBackground>()

    fun scanBackgroundMedia(folder: File = File(_uiState.value.currentBackgroundFolder)) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val backgrounds = mutableListOf<KaraokeBackground>()
            val seenPaths = HashSet<String>()

            // Strictly scan ONLY the user-selected folder for mp4/video or image files!
            if (folder.exists() && folder.isDirectory) {
                val allFiles = try {
                    folder.walkTopDown().maxDepth(6).filter { it.isFile && it.length() > 0L }.toList()
                } catch (e: Exception) {
                    folder.listFiles()?.filter { it.isFile && it.length() > 0L } ?: emptyList()
                }
                for (file in allFiles) {
                    if (!seenPaths.add(file.absolutePath)) continue
                    val ext = file.extension.lowercase()
                    val folderTag = if (file.parentFile != null && file.parentFile != folder) "[${file.parentFile!!.name}] " else ""
                    when (ext) {
                        "mp4", "mkv", "webm", "3gp", "avi", "mov" -> {
                            backgrounds.add(
                                KaraokeBackground(
                                    id = file.absolutePath,
                                    name = "🎬 " + folderTag + file.nameWithoutExtension.take(24),
                                    path = file.absolutePath,
                                    type = BackgroundType.VIDEO
                                )
                            )
                        }
                        "jpg", "jpeg", "png", "webp" -> {
                            backgrounds.add(
                                KaraokeBackground(
                                    id = file.absolutePath,
                                    name = "🖼️ " + folderTag + file.nameWithoutExtension.take(24),
                                    path = file.absolutePath,
                                    type = BackgroundType.IMAGE
                                )
                            )
                        }
                    }
                }
            }

            // Only include stage fallback if selected folder has no mp4 or image files
            if (backgrounds.isEmpty()) {
                backgrounds.add(KaraokeBackground("stage_neon", "✨ Neon Videoke Stage", "", BackgroundType.STAGE))
            }

            val mediaOnly = backgrounds.filter { it.type == BackgroundType.VIDEO || it.type == BackgroundType.IMAGE }
            val savedId = prefs.getString("selected_bg", null)
            val savedBg = backgrounds.firstOrNull { b -> savedId != null && b.id == savedId }
            val newSel = if (savedBg != null && (savedBg.type == BackgroundType.VIDEO || savedBg.type == BackgroundType.IMAGE)) {
                savedBg
            } else {
                mediaOnly.shuffled().firstOrNull() ?: savedBg ?: backgrounds.firstOrNull()
            }

            synchronized(playlistLock) {
                backgroundPlaylist.clear()
                if (mediaOnly.isNotEmpty()) {
                    val shuffled = mediaOnly.shuffled().toMutableList()
                    if (newSel != null && shuffled.size > 1 && shuffled.first().id == newSel.id) {
                        val first = shuffled.removeAt(0)
                        shuffled.add(first)
                    }
                    backgroundPlaylist.addAll(shuffled)
                }
            }

            _uiState.update {
                it.copy(
                    availableBackgrounds = backgrounds,
                    selectedBackground = newSel
                )
            }
        }
    }

    fun openSongFolderPicker() {
        _uiState.update { it.copy(isSongFolderPickerOpen = true) }
    }

    fun closeSongFolderPicker() {
        _uiState.update { it.copy(isSongFolderPickerOpen = false) }
    }

    fun openBgFolderPicker() {
        _uiState.update { it.copy(isBgFolderPickerOpen = true) }
    }

    fun closeBgFolderPicker() {
        _uiState.update { it.copy(isBgFolderPickerOpen = false) }
    }

    fun setMicSensitivity(sensitivity: Float) {
        val clamped = sensitivity.coerceIn(0.2f, 3.0f)
        prefs.edit().putFloat("mic_sensitivity", clamped).apply()
        micScorer.micSensitivity = clamped
        _uiState.update { it.copy(micSensitivity = clamped) }
    }

    fun selectBackground(bg: KaraokeBackground) {
        prefs.edit().putString("selected_bg", bg.id).apply()
        synchronized(playlistLock) {
            backgroundPlaylist.removeAll { it.id == bg.id }
        }
        _uiState.update { it.copy(selectedBackground = bg) }
    }

    fun setBackgroundChangeMode(mode: BackgroundChangeMode) {
        prefs.edit().putString("background_change_mode", mode.name).apply()
        _uiState.update { it.copy(backgroundChangeMode = mode) }
    }

    fun openBackgroundDialog() {
        _uiState.update { it.copy(isBackgroundDialogOpen = true) }
    }

    fun closeBackgroundDialog() {
        _uiState.update { it.copy(isBackgroundDialogOpen = false) }
    }

    fun openAudioMixerDialog() {
        _uiState.update { it.copy(isAudioMixerDialogOpen = true) }
    }

    fun closeAudioMixerDialog() {
        _uiState.update { it.copy(isAudioMixerDialogOpen = false) }
    }

    fun advanceNextBackground() {
        val list = _uiState.value.availableBackgrounds
        val mediaList = list.filter { it.type == BackgroundType.VIDEO || it.type == BackgroundType.IMAGE }.ifEmpty { list }
        if (mediaList.isEmpty()) return

        val nextBg: KaraokeBackground = synchronized(playlistLock) {
            if (backgroundPlaylist.isEmpty()) {
                val shuffled = mediaList.shuffled().toMutableList()
                val currentId = _uiState.value.selectedBackground?.id
                if (shuffled.size > 1 && shuffled.first().id == currentId) {
                    val first = shuffled.removeAt(0)
                    shuffled.add(first)
                }
                backgroundPlaylist.addAll(shuffled)
            }
            if (backgroundPlaylist.isNotEmpty()) {
                backgroundPlaylist.removeAt(0)
            } else {
                mediaList.random()
            }
        }

        _uiState.update { it.copy(selectedBackground = nextBg) }
    }

    fun openLyricStyleDialog() {
        _uiState.update { it.copy(isLyricStyleDialogOpen = true) }
    }

    fun closeLyricStyleDialog() {
        _uiState.update { it.copy(isLyricStyleDialogOpen = false) }
    }

    fun openSettingsDialog() {
        _uiState.update { it.copy(isSettingsDialogOpen = true) }
    }

    fun closeSettingsDialog() {
        _uiState.update { it.copy(isSettingsDialogOpen = false) }
    }

    fun toggleMicScoring(enabled: Boolean) {
        prefs.edit().putBoolean("mic_scoring_enabled", enabled).apply()
        _uiState.update { it.copy(isMicScoringEnabled = enabled) }
        if (!enabled) {
            micScorer.stopScoring()
            com.example.videokey.data.VideokeSoundEffects.stop()
        } else if (_uiState.value.isPlaying) {
            micScorer.startScoring()
        }
    }

    fun toggleScoringSound(enabled: Boolean) {
        prefs.edit().putBoolean("scoring_sound_enabled", enabled).apply()
        _uiState.update { it.copy(isScoringSoundEnabled = enabled) }
        if (!enabled) {
            com.example.videokey.data.VideokeSoundEffects.stop()
        }
    }

    fun toggleKeypadVoice(enabled: Boolean) {
        _uiState.update { it.copy(isKeypadVoiceEnabled = enabled) }
    }

    fun updateLyricConfig(config: LyricCustomization) {
        _uiState.update { it.copy(lyricConfig = config) }
    }

    fun setLyricFontFamily(family: LyricFontFamily) {
        _uiState.update { it.copy(lyricConfig = it.lyricConfig.copy(fontFamily = family)) }
    }

    fun setLyricWipeColor(wipeColor: LyricWipeColor) {
        _uiState.update { it.copy(lyricConfig = it.lyricConfig.copy(wipeColor = wipeColor)) }
    }

    fun setLyricShadowStyle(shadowStyle: LyricShadowStyle) {
        _uiState.update { it.copy(lyricConfig = it.lyricConfig.copy(shadowStyle = shadowStyle)) }
    }

    fun toggleLyricAllCaps() {
        _uiState.update { it.copy(lyricConfig = it.lyricConfig.copy(isAllCaps = !it.lyricConfig.isAllCaps)) }
    }

    fun toggleLyricItalic() {
        _uiState.update { it.copy(lyricConfig = it.lyricConfig.copy(isItalic = !it.lyricConfig.isItalic)) }
    }

    fun setLyricFontScale(scale: Float) {
        val clamped = scale.coerceIn(0.75f, 1.4f)
        _uiState.update { it.copy(lyricConfig = it.lyricConfig.copy(fontScale = clamped)) }
    }

    fun setLyricLineCount(lineCount: LyricLineCount) {
        _uiState.update { it.copy(lyricConfig = it.lyricConfig.copy(lineCount = lineCount)) }
    }

    fun setLyricPosition(position: LyricScreenPosition) {
        _uiState.update { it.copy(lyricConfig = it.lyricConfig.copy(position = position)) }
    }

    fun setAppLanguage(language: AppLanguage) {
        prefs.edit().putString("app_language", language.code).apply()
        _uiState.update { it.copy(appLanguage = language) }
    }

    fun applyLyricPreset(presetName: String) {
        val current = _uiState.value.lyricConfig
        val baseConfig = when (presetName) {
            "live_tv" -> LyricCustomization(
                fontFamily = LyricFontFamily.SAN_SERIF,
                wipeColor = LyricWipeColor.OCEAN_BLUE,
                shadowStyle = LyricShadowStyle.HEAVY_OUTLINE,
                isAllCaps = true,
                isItalic = false,
                fontScale = 1.05f
            )
            "platinum" -> LyricCustomization(
                fontFamily = LyricFontFamily.SAN_SERIF,
                wipeColor = LyricWipeColor.SUNSHINE_YELLOW,
                shadowStyle = LyricShadowStyle.HEAVY_OUTLINE,
                isAllCaps = true,
                isItalic = false,
                fontScale = 1.0f
            )
            "cyber" -> LyricCustomization(
                fontFamily = LyricFontFamily.MONOSPACE,
                wipeColor = LyricWipeColor.ELECTRIC_CYAN,
                shadowStyle = LyricShadowStyle.NEON_GLOW,
                isAllCaps = true,
                isItalic = false,
                fontScale = 1.15f
            )
            "pop" -> LyricCustomization(
                fontFamily = LyricFontFamily.SAN_SERIF,
                wipeColor = LyricWipeColor.HOT_PINK,
                shadowStyle = LyricShadowStyle.HEAVY_OUTLINE,
                isAllCaps = false,
                isItalic = false,
                fontScale = 1.05f
            )
            "gold" -> LyricCustomization(
                fontFamily = LyricFontFamily.SERIF,
                wipeColor = LyricWipeColor.PURE_GOLD,
                shadowStyle = LyricShadowStyle.HEAVY_OUTLINE,
                isAllCaps = true,
                isItalic = false,
                fontScale = 1.0f
            )
            "ballad" -> LyricCustomization(
                fontFamily = LyricFontFamily.SERIF,
                wipeColor = LyricWipeColor.SUNSHINE_YELLOW,
                shadowStyle = LyricShadowStyle.SUBTLE_DROP,
                isAllCaps = false,
                isItalic = true,
                fontScale = 1.0f
            )
            else -> LyricCustomization()
        }
        val newConfig = baseConfig.copy(
            lineCount = current.lineCount,
            position = current.position
        )
        _uiState.update { it.copy(lyricConfig = newConfig) }
    }

    override fun onCleared() {
        super.onCleared()
        soundEngine.stop()
        micScorer.stopScoring()
        com.example.videokey.data.VideokeSoundEffects.stop()
        voiceManager.shutdown()
        playbackJob?.cancel()
    }
}
