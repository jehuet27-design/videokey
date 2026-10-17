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
    SAN_SERIF("Arial Bold"),
    IMPACT_HEAVY("Impact Heavy"),
    CONDENSED_TALL("Bebas Tall"),
    GEOMETRIC_POP("Montserrat Pop"),
    ROUNDED_CLEAN("Verdana Wide"),
    SMALL_CAPS("Oswald Cinema"),
    SERIF("Georgia Serif"),
    SERIF_MONOSPACE("Courier Slab"),
    MONOSPACE("Digital Mono"),
    CASUAL_FUN("Comic Casual"),
    CURSIVE("Dancing Script"),
    LIGHT_MODERN("Futura Sleek")
}

enum class LyricWipeColor(val displayName: String, val activeColor: Long) {
    DARK_BLUE("Dark Blue", 0xFF1D4ED8),
    OCEAN_BLUE("Karaoke Blue", 0xFF0088FF),
    SUNSHINE_YELLOW("Sunshine Yellow", 0xFFFFEB3B),
    ELECTRIC_CYAN("Electric Cyan", 0xFF00E5FF),
    NEON_GREEN("Lime Green", 0xFF00E676),
    HOT_PINK("Hot Pink", 0xFFFF4081),
    VIBRANT_ORANGE("Sunset Orange", 0xFFFF9100),
    PURE_GOLD("Pure Gold", 0xFFFFD54F)
}

enum class LyricShadowStyle(val displayName: String, val tagalogName: String) {
    HEAVY_OUTLINE("Heavy Outline", "Makapal na Outline"),
    NEON_GLOW("Neon Glow", "Neon Glow"),
    SUBTLE_DROP("3D Drop Shadow", "3D Anino"),
    CLEAN("No Shadow", "Walang Anino");

    fun label(isTagalog: Boolean): String = if (isTagalog) tagalogName else displayName
}

enum class LyricLineCount(val displayName: String, val tagalogName: String, val lines: Int) {
    TWO_LINES("2 Lines", "2 Linya (Dalawa)", 2),
    FOUR_LINES("4 Lines", "4 Linya (Apat)", 4);

    fun label(isTagalog: Boolean): String = if (isTagalog) tagalogName else displayName
    fun shortBadge(isTagalog: Boolean): String = if (isTagalog) "${lines} Linya" else "${lines} Lines"
}

enum class LyricScreenPosition(val displayName: String, val tagalogName: String) {
    BOTTOM("⬇️ Bottom", "⬇️ Ibaba"),
    CENTER("⏺️ Center", "⏺️ Gitna"),
    TOP("⬆️ Top", "⬆️ Itaas");

    fun label(isTagalog: Boolean): String = if (isTagalog) tagalogName else displayName
}

enum class LyricOutlineColor(val displayName: String, val tagalogName: String, val colorLong: Long) {
    BLACK("Black", "Itim (Black)", 0xFF000000),
    WHITE("White", "Puti (White)", 0xFFFFFFFF),
    NAVY("Navy Blue", "Navy Blue", 0xFF0A192F),
    GOLD("Gold", "Ginto (Gold)", 0xFFFFD700),
    RED("Crimson Red", "Pula (Red)", 0xFFDC2626),
    CYAN("Cyan Neon", "Cyan Neon", 0xFF06B6D4),
    NONE("No Outline", "Walang Outline", 0x00000000);

    fun label(isTagalog: Boolean): String = if (isTagalog) tagalogName else displayName
}

data class LyricCustomization(
    val fontFamily: LyricFontFamily = LyricFontFamily.SAN_SERIF,
    val wipeColor: LyricWipeColor = LyricWipeColor.DARK_BLUE,
    val inactiveColor: Long = 0xFFFFFFFF, // Pure White with Outline
    val outlineColor: LyricOutlineColor = LyricOutlineColor.BLACK,
    val shadowStyle: LyricShadowStyle = LyricShadowStyle.HEAVY_OUTLINE,
    val isAllCaps: Boolean = true,
    val isItalic: Boolean = false,
    val fontScale: Float = 1.0f,
    val lineCount: LyricLineCount = LyricLineCount.TWO_LINES,
    val position: LyricScreenPosition = LyricScreenPosition.CENTER
)

enum class AppTheme(
    val id: String,
    val displayName: String,
    val tagalogName: String,
    val primaryColor: Long,       // Smooth flat accent
    val secondaryColor: Long,     // Secondary accent
    val surfaceColor: Long,       // Card / Dialog surface
    val backgroundColor: Long,    // Deep base bg
    val buttonBgColor: Long,      // Uniform button background
    val buttonBorderColor: Long,  // Uniform button border
    val textColor: Long,          // Primary text
    val textMutedColor: Long      // Muted / secondary text
) {
    SLATE_DARK(
        "slate_dark",
        "Classic Slate",
        "Klasikong Slate",
        primaryColor = 0xFF38BDF8,      // Flat Sky Blue
        secondaryColor = 0xFF818CF8,    // Soft Indigo
        surfaceColor = 0xFF1E293B,      // Deep Slate Card
        backgroundColor = 0xFF0F172A,   // Smooth Dark Slate
        buttonBgColor = 0xFF1E293B,     // Uniform Flat Slate
        buttonBorderColor = 0xFF334155, // Crisp Subtle Border
        textColor = 0xFFF8FAFC,
        textMutedColor = 0xFF94A3B8
    ),
    EMERALD_STAGE(
        "emerald_stage",
        "Emerald Stage",
        "Emerald Entablado",
        primaryColor = 0xFF10B981,      // Smooth Emerald
        secondaryColor = 0xFF34D399,
        surfaceColor = 0xFF064E3B,
        backgroundColor = 0xFF022C22,
        buttonBgColor = 0xFF064E3B,
        buttonBorderColor = 0xFF047857,
        textColor = 0xFFF0FDF4,
        textMutedColor = 0xFFA7F3D0
    ),
    WARM_AMBER(
        "warm_amber",
        "Warm Gold",
        "Gintong Entablado",
        primaryColor = 0xFFF59E0B,      // Flat Amber
        secondaryColor = 0xFFFBBF24,
        surfaceColor = 0xFF292524,
        backgroundColor = 0xFF1C1917,
        buttonBgColor = 0xFF292524,
        buttonBorderColor = 0xFF44403C,
        textColor = 0xFFFAFAF9,
        textMutedColor = 0xFFD6D3D1
    ),
    ROYAL_INDIGO(
        "royal_indigo",
        "Royal Studio",
        "Royal Studio",
        primaryColor = 0xFF6366F1,      // Smooth Indigo
        secondaryColor = 0xFFA5B4FC,
        surfaceColor = 0xFF1E1B4B,
        backgroundColor = 0xFF0F0E2A,
        buttonBgColor = 0xFF1E1B4B,
        buttonBorderColor = 0xFF312E81,
        textColor = 0xFFF5F3FF,
        textMutedColor = 0xFFC7D2FE
    ),
    CHARCOAL_MINIMAL(
        "charcoal_minimal",
        "Minimal Charcoal",
        "Minimal Charcoal",
        primaryColor = 0xFFE2E8F0,      // Soft Silver/Slate
        secondaryColor = 0xFF94A3B8,
        surfaceColor = 0xFF18181B,      // Zinc 900
        backgroundColor = 0xFF09090B,   // Zinc 950
        buttonBgColor = 0xFF27272A,     // Zinc 800
        buttonBorderColor = 0xFF3F3F46, // Zinc 700
        textColor = 0xFFF4F4F5,
        textMutedColor = 0xFFA1A1AA
    );

    fun label(isTagalog: Boolean): String = if (isTagalog) tagalogName else displayName
}

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
    val melodyPercent: Int = 60,
    val drumsPercent: Int = 100,
    val bassPercent: Int = 100,
    val guitarPianoPercent: Int = 100,
    val stringsBrassPercent: Int = 100,
    val reverbPercent: Int = 50,
    val micEchoPercent: Int = 50,
    val appTheme: AppTheme = AppTheme.SLATE_DARK,
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
    val activeSoundFontName: String = "VideoKey Studio Minus-One",
    val availableSoundFonts: List<String> = listOf(
        "VideoKey Studio Minus-One",
        "Yamaha x Korg x Ketron Studio",
        "Dream SAM2634 v2.1",
        "MegaPro Sound",
        "SongHub Original Sound"
    ),
    val isSoundFontDialogOpen: Boolean = false,
    val isSongbookOpen: Boolean = false,
    val songbookQuery: String = "",
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
    private val karaokeRecorder = com.example.videokey.data.KaraokeRecorder()
    private val soundEngine = SoundEngine(application.applicationContext, karaokeRecorder)
    private val voiceManager = com.example.videokey.data.KeypadVoiceManager(application.applicationContext)
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
    private val playlistLock = Any()
    private val backgroundPlaylist = mutableListOf<KaraokeBackground>()

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

        val rawSavedSf = prefs.getString("active_soundfont", "VideoKey Studio Minus-One") ?: "VideoKey Studio Minus-One"
        val savedSf = when {
            rawSavedSf.contains("yamaha", ignoreCase = true) || rawSavedSf.contains("korg", ignoreCase = true) || rawSavedSf.contains("ketron", ignoreCase = true) -> "Yamaha x Korg x Ketron Studio"
            rawSavedSf.contains("mega", ignoreCase = true) -> "MegaPro Sound"
            rawSavedSf.contains("songhub", ignoreCase = true) -> "SongHub Original Sound"
            rawSavedSf.contains("sam", ignoreCase = true) || rawSavedSf.contains("dream", ignoreCase = true) -> "Dream SAM2634 v2.1"
            else -> "VideoKey Studio Minus-One"
        }
        soundEngine.setActiveSoundFont(savedSf)
        val initialMelody = prefs.getInt("melody_percent", 60)
        val initialDrums = prefs.getInt("drums_percent", 100)
        val initialBass = prefs.getInt("bass_percent", 100)
        val initialGuitarPiano = prefs.getInt("guitar_piano_percent", 100)
        val initialStringsBrass = prefs.getInt("strings_brass_percent", 100)
        val initialReverb = prefs.getInt("reverb_percent", 50)
        soundEngine.setMelodyVolume(initialMelody)
        soundEngine.setDrumsVolume(initialDrums)
        soundEngine.setBassVolume(initialBass)
        soundEngine.setGuitarPianoVolume(initialGuitarPiano)
        soundEngine.setStringsBrassVolume(initialStringsBrass)
        soundEngine.setReverbVolume(initialReverb)

        val savedThemeName = prefs.getString("app_theme", AppTheme.SLATE_DARK.name) ?: AppTheme.SLATE_DARK.name
        val initialTheme = try {
            AppTheme.valueOf(savedThemeName)
        } catch (e: Exception) {
            AppTheme.SLATE_DARK
        }

        val savedFavSet = prefs.getStringSet("favorite_song_codes", emptySet())?.toSet() ?: emptySet()
        val initialFavSongs = savedFavSet.mapNotNull { repository.findByCode(it) }

        val savedScoring = prefs.getBoolean("mic_scoring_enabled", true)
        val savedScoringSound = prefs.getBoolean("scoring_sound_enabled", true)

        val savedOutlineName = prefs.getString("lyric_outline_color", LyricOutlineColor.BLACK.name) ?: LyricOutlineColor.BLACK.name
        val initialOutline = try {
            LyricOutlineColor.valueOf(savedOutlineName)
        } catch (_: Exception) {
            LyricOutlineColor.BLACK
        }

        val savedFontName = prefs.getString("lyric_font_family", LyricFontFamily.SAN_SERIF.name) ?: LyricFontFamily.SAN_SERIF.name
        val initialFontFamily = try {
            LyricFontFamily.valueOf(savedFontName)
        } catch (_: Exception) {
            LyricFontFamily.SAN_SERIF
        }

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
                drumsPercent = initialDrums,
                bassPercent = initialBass,
                guitarPianoPercent = initialGuitarPiano,
                stringsBrassPercent = initialStringsBrass,
                reverbPercent = initialReverb,
                appTheme = initialTheme,
                isMicScoringEnabled = savedScoring,
                isScoringSoundEnabled = savedScoringSound,
                lyricConfig = it.lyricConfig.copy(
                    outlineColor = initialOutline,
                    fontFamily = initialFontFamily
                ),
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
                    val baseDir = contentDownloadManager.getBaseStorageDir()
                    if (status.itemId == "all_backgrounds") {
                        val bgDir = File(baseDir, "backgrounds")
                        if (bgDir.exists()) {
                            prefs.edit().putString("background_folder", bgDir.absolutePath).apply()
                            _uiState.update { it.copy(currentBackgroundFolder = bgDir.absolutePath) }
                            scanBackgroundMedia(bgDir, forcePickVideo = true)
                        } else {
                            scanBackgroundMedia(File(_uiState.value.currentBackgroundFolder), forcePickVideo = true)
                        }
                    } else if (status.itemId == "core_songs") {
                        scanSongFolder(File(_uiState.value.currentSongFolder))
                        // Automatic chain: download soundfonts if missing right after songs finish!
                        if (!contentDownloadManager.isSoundFontInstalled()) {
                            val sfItem = contentDownloadManager.getAvailableCatalog().find { it.id == "all_soundfonts" }
                            if (sfItem != null) {
                                contentDownloadManager.downloadItem(sfItem)
                            }
                        }
                    } else if (status.itemId == "all_soundfonts") {
                        scanSongFolder(File(_uiState.value.currentSongFolder))
                    } else {
                        scanSongFolder(File(_uiState.value.currentSongFolder))
                        scanBackgroundMedia(File(_uiState.value.currentBackgroundFolder))
                    }
                }
            }
        }
        refreshDownloadCatalog()

        // Automated startup download: if no songs are installed, automatically download songs and then soundfonts!
        viewModelScope.launch {
            delay(2500)
            val hasSongs = repository.getTotalSongsCount() > 0 || contentDownloadManager.hasAnySongsInstalled()
            val hasSf = contentDownloadManager.isSoundFontInstalled()
            if (!hasSongs) {
                Log.d("VideoKeyViewModel", "No songs detected on startup. Triggering automatic download...")
                _uiState.update { it.copy(isDownloadCenterOpen = true) }
                val songsItem = contentDownloadManager.getAvailableCatalog().find { it.id == "core_songs" }
                if (songsItem != null) {
                    contentDownloadManager.downloadItem(songsItem)
                }
            } else if (!hasSf) {
                Log.d("VideoKeyViewModel", "SoundFont missing on startup. Triggering automatic download...")
                _uiState.update { it.copy(isDownloadCenterOpen = true) }
                val sfItem = contentDownloadManager.getAvailableCatalog().find { it.id == "all_soundfonts" }
                if (sfItem != null) {
                    contentDownloadManager.downloadItem(sfItem)
                }
            }
        }

        // Background OTA Update Check across any internet connection (Wi-Fi, Data)
        appUpdateManager.checkForUpdates(autoDownloadIfNewer = false, isManual = false)
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

            // Extract raw MIDI bytes directly so BASSMIDI preserves 100% original instruments & mixes in real time
            if (rawMidiBytes != null) {
                midiFile = VkPackReader.extractToPrivateTemp(song, rawMidiBytes, getApplication<Application>().cacheDir)
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
                    songStartTimeMs = System.currentTimeMillis()
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
                        val isTl = it.appLanguage == AppLanguage.TAGALOG
                        it.copy(
                            isPlaying = false,
                            showSongTitleIntro = false,
                            isInstrumental = false,
                            countdownDots = 0,
                            alertMessage = if (isTl) {
                                "⚠️ Walang audio file para sa [${song.code}] \"${song.title}\". Mangyaring maglagay ng \"${song.code}.mid\" sa /sdcard/VideoKey/songs/ folder upang mapatugtog ito."
                            } else {
                                "⚠️ No audio file found for [${song.code}] \"${song.title}\". Please place \"${song.code}.mid\" inside /sdcard/VideoKey/songs/ to play this song."
                            },
                            line1 = "NO MIDI FILE FOUND",
                            line2 = if (isTl) "WALANG FILE PARA SA: ${song.title.take(24).uppercase()}" else "MISSING FILE FOR: ${song.title.take(24).uppercase()}",
                            line3 = if (isTl) "PUMILI NG MAY 🎵 BADGE SA SONGBOOK" else "CHOOSE A SONG WITH 🎵 BADGE IN SONGBOOK",
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
        playbackJob = viewModelScope.launch(Dispatchers.Default) {
            var cachedLineIdx = 0
            var cachedMelodyIdx = 0
            var lastEmittedWipe = -1f
            var lastEmittedActiveIndex = -2
            var lastEmittedDots = -1
            var lastEmittedIntro = false
            var lastEmittedInst = false
            var lastRecSecs = -1

            while (true) {
                delay(33)
                if (!_uiState.value.isPlaying) continue

                val isAudioPlaying = soundEngine.isPlaying()
                val posMs = soundEngine.getCurrentPositionMs()
                val durMs = soundEngine.getDurationMs()
                val elapsedSinceStart = System.currentTimeMillis() - songStartTimeMs
                val lines = parsedKaraoke?.lines.orEmpty()
                val melodyNotes = parsedKaraoke?.melodyNotes.orEmpty()
                val firstLineStart = lines.firstOrNull()?.startMs ?: 10000L
                val isTitleIntroActive = elapsedSinceStart in 0L..3500L && (lines.isEmpty() || posMs < firstLineStart)

                // Fast pointer-based melody note search (O(1) amortized instead of scanning thousands of notes)
                val activeMelodyNote = if (melodyNotes.isNotEmpty()) {
                    if (cachedMelodyIdx >= melodyNotes.size || melodyNotes[cachedMelodyIdx].startMs > posMs) {
                        cachedMelodyIdx = 0
                    }
                    while (cachedMelodyIdx < melodyNotes.size && melodyNotes[cachedMelodyIdx].endMs < posMs) {
                        cachedMelodyIdx++
                    }
                    val cand = melodyNotes.getOrNull(cachedMelodyIdx)
                    if (cand != null && posMs in cand.startMs..cand.endMs) cand else null
                } else null

                // Fast pointer-based line search for active vocal (O(1) amortized)
                val activeLyricLine = if (lines.isNotEmpty()) {
                    if (cachedLineIdx >= lines.size || lines[cachedLineIdx].startMs > posMs) {
                        cachedLineIdx = 0
                    }
                    while (cachedLineIdx < lines.size && lines[cachedLineIdx].endMs < posMs) {
                        cachedLineIdx++
                    }
                    val cand = lines.getOrNull(cachedLineIdx)
                    if (cand != null && posMs in cand.startMs..cand.endMs) cand else null
                } else null

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
                    val recSecs = karaokeRecorder.getRecordingDurationSeconds()
                    if (recSecs != lastRecSecs) {
                        lastRecSecs = recSecs
                        _uiState.update { it.copy(recordingSeconds = recSecs) }
                    }
                }

                // Auto-detect end of song for instant auto-play of reserve queue
                if (elapsedSinceStart > 5000L && ((durMs > 3000L && posMs >= durMs - 250L) || (!isAudioPlaying && posMs > 2000L && melodyReloadJob?.isActive != true))) {
                    withContext(Dispatchers.Main) {
                        onSongCompleted()
                    }
                    break
                }

                if (lines.isNotEmpty() && isAudioPlaying) {
                    // Fast index lookup for active line with holdEnd
                    var activeIndex = -1
                    if (cachedLineIdx >= lines.size || lines[cachedLineIdx].startMs > posMs) {
                        cachedLineIdx = 0
                    }
                    for (i in cachedLineIdx until lines.size) {
                        val cur = lines[i]
                        val nextStart = lines.getOrNull(i + 1)?.startMs ?: (cur.endMs + 500L)
                        val holdEnd = minOf(cur.endMs + 420L, (nextStart - 60L).coerceAtLeast(cur.endMs))
                        if (posMs in cur.startMs..holdEnd) {
                            activeIndex = i
                            cachedLineIdx = i
                            break
                        } else if (cur.startMs > posMs) {
                            break
                        }
                    }

                    if (activeIndex != -1) {
                        val currentLine = lines[activeIndex]
                        val nextLine = lines.getOrNull(activeIndex + 1)
                        val wipe = computeMelodyWipe(currentLine, posMs)

                        val wipeDelta = kotlin.math.abs(wipe - lastEmittedWipe)
                        val lineChanged = activeIndex != lastEmittedActiveIndex
                        val introChanged = isTitleIntroActive != lastEmittedIntro
                        val instChanged = lastEmittedInst

                        if (lineChanged || introChanged || instChanged || wipeDelta >= 0.003f) {
                            lastEmittedWipe = wipe
                            lastEmittedActiveIndex = activeIndex
                            lastEmittedIntro = false
                            lastEmittedInst = false
                            lastEmittedDots = 0

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
                        }
                    } else {
                        // Between lines or intro: find upcoming line
                        var upcomingIndex = -1
                        for (i in cachedLineIdx until lines.size) {
                            if (lines[i].startMs > posMs) {
                                upcomingIndex = i
                                break
                            }
                        }
                        if (upcomingIndex == -1) {
                            for (i in 0 until cachedLineIdx) {
                                if (lines[i].startMs > posMs) {
                                    upcomingIndex = i
                                    break
                                }
                            }
                        }

                        if (upcomingIndex != -1) {
                            val upcoming = lines[upcomingIndex]
                            val nextUpcoming = lines.getOrNull(upcomingIndex + 1)
                            val timeUntilStart = upcoming.startMs - posMs

                            // Long instrumental gap detection:
                            val prevLineEndMs = if (upcomingIndex > 0) lines[upcomingIndex - 1].endMs else 0L
                            val instrumentalGap = upcoming.startMs - prevLineEndMs
                            val isAfterLongInstrumental = instrumentalGap >= 4500L

                            // Countdown: 4 seconds before vocals start ONLY after a long instrumental or song intro (when title intro finishes)!
                            val dots = if (isAfterLongInstrumental && !isTitleIntroActive) {
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

                            val showInstrumental = !isTitleIntroActive && dots == 0 && (timeUntilStart > 3500L) && isAfterLongInstrumental && elapsedSinceStart > 3500L

                            val lineChanged = upcomingIndex != lastEmittedActiveIndex
                            val dotsChanged = dots != lastEmittedDots
                            val introChanged = isTitleIntroActive != lastEmittedIntro
                            val instChanged = showInstrumental != lastEmittedInst
                            val wipeChanged = lastEmittedWipe != 0f

                            if (lineChanged || dotsChanged || introChanged || instChanged || wipeChanged) {
                                lastEmittedWipe = 0f
                                lastEmittedActiveIndex = upcomingIndex
                                lastEmittedDots = dots
                                lastEmittedIntro = isTitleIntroActive
                                lastEmittedInst = showInstrumental

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
                            }
                        } else {
                            // Song outro / after last lyric
                            if (lastEmittedActiveIndex != -3 || !lastEmittedInst) {
                                lastEmittedActiveIndex = -3
                                lastEmittedWipe = 0f
                                lastEmittedDots = 0
                                lastEmittedIntro = false
                                lastEmittedInst = true

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
                    val isInst = !isTitleIntroActive && dots == 0 && elapsedSinceStart > 3500L
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
                val isTl = _uiState.value.appLanguage == AppLanguage.TAGALOG
                savedBanner = if (isTl) "✅ Na-save ang Recording: ${saved.fileName}" else "✅ Recording Saved: ${saved.fileName}"
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
        val current = _uiState.value
        if (current.isSongbookOpen && current.songbookQuery.isNotEmpty() && current.enteredCode.isEmpty()) {
            searchSongbook("", current.onlyShowPlayableSongs)
            return
        }
        _uiState.update {
            it.copy(
                enteredCode = "",
                matchedEnteredSong = null
            )
        }
    }

    fun onDeleteDigit() {
        val current = _uiState.value
        if (current.isSongbookOpen && current.songbookQuery.isNotEmpty() && current.enteredCode.isEmpty()) {
            val nextQuery = current.songbookQuery.dropLast(1)
            searchSongbook(nextQuery, current.onlyShowPlayableSongs)
            return
        }
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
                val isTl = it.appLanguage == AppLanguage.TAGALOG
                it.copy(
                    isRecording = true,
                    recordingSeconds = 0,
                    recordingBannerMessage = if (isTl) "🔴 Nagre-record ng Boses at Tugtog: ${song.title}" else "🔴 Recording Voice & Music: ${song.title}"
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
            val isTl = it.appLanguage == AppLanguage.TAGALOG
            it.copy(
                isRecording = false,
                recordingSeconds = 0,
                savedRecordings = list,
                recordingBannerMessage = if (saved != null) {
                    if (isTl) "✅ Na-save ang Recording: ${saved.fileName}" else "✅ Recording Saved: ${saved.fileName}"
                } else null
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
        setMelodyPercent(_uiState.value.melodyPercent + delta)
    }

    fun setMelodyPercent(percent: Int) {
        val newVol = percent.coerceIn(0, 100)
        soundEngine.setMelodyVolume(newVol)
        prefs.edit().putInt("melody_percent", newVol).apply()
        _uiState.update { it.copy(melodyPercent = newVol) }
    }

    fun adjustDrums(delta: Int) {
        setDrumsPercent(_uiState.value.drumsPercent + delta)
    }

    fun setDrumsPercent(percent: Int) {
        val newVol = percent.coerceIn(0, 150)
        soundEngine.setDrumsVolume(newVol)
        prefs.edit().putInt("drums_percent", newVol).apply()
        _uiState.update { it.copy(drumsPercent = newVol) }
    }

    fun adjustBass(delta: Int) {
        setBassPercent(_uiState.value.bassPercent + delta)
    }

    fun setBassPercent(percent: Int) {
        val newVol = percent.coerceIn(0, 150)
        soundEngine.setBassVolume(newVol)
        prefs.edit().putInt("bass_percent", newVol).apply()
        _uiState.update { it.copy(bassPercent = newVol) }
    }

    fun adjustGuitarPiano(delta: Int) {
        setGuitarPianoPercent(_uiState.value.guitarPianoPercent + delta)
    }

    fun setGuitarPianoPercent(percent: Int) {
        val newVol = percent.coerceIn(0, 150)
        soundEngine.setGuitarPianoVolume(newVol)
        prefs.edit().putInt("guitar_piano_percent", newVol).apply()
        _uiState.update { it.copy(guitarPianoPercent = newVol) }
    }

    fun adjustStringsBrass(delta: Int) {
        setStringsBrassPercent(_uiState.value.stringsBrassPercent + delta)
    }

    fun setStringsBrassPercent(percent: Int) {
        val newVol = percent.coerceIn(0, 150)
        soundEngine.setStringsBrassVolume(newVol)
        prefs.edit().putInt("strings_brass_percent", newVol).apply()
        _uiState.update { it.copy(stringsBrassPercent = newVol) }
    }

    fun adjustReverb(delta: Int) {
        setReverbPercent(_uiState.value.reverbPercent + delta)
    }

    fun setReverbPercent(percent: Int) {
        val newVol = percent.coerceIn(0, 100)
        soundEngine.setReverbVolume(newVol)
        prefs.edit().putInt("reverb_percent", newVol).apply()
        _uiState.update { it.copy(reverbPercent = newVol) }
    }

    fun resetInstrumentMixer() {
        soundEngine.resetInstrumentMix()
        soundEngine.setKeyTranspose(0)
        soundEngine.setTempo(100)
        prefs.edit()
            .putInt("melody_percent", 60)
            .putInt("drums_percent", 100)
            .putInt("bass_percent", 100)
            .putInt("guitar_piano_percent", 100)
            .putInt("strings_brass_percent", 100)
            .putInt("reverb_percent", 50)
            .apply()
        _uiState.update {
            it.copy(
                keyTranspose = 0,
                tempoPercent = 100,
                melodyPercent = 60,
                drumsPercent = 100,
                bassPercent = 100,
                guitarPianoPercent = 100,
                stringsBrassPercent = 100,
                reverbPercent = 50
            )
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
        _uiState.update {
            it.copy(
                songbookQuery = query,
                onlyShowPlayableSongs = onlyPlayable
            )
        }
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
        val currentQuery = _uiState.value.songbookQuery
        _uiState.update { it.copy(onlyShowPlayableSongs = onlyPlayable) }
        searchSongbook(currentQuery, onlyPlayable)
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
        _uiState.update {
            val isTl = it.appLanguage == AppLanguage.TAGALOG
            it.copy(
                activeSoundFontName = resolvedName,
                isSoundFontDialogOpen = false,
                alertMessage = if (isTl) "🎛️ Na-apply ang SoundFont: $resolvedName" else "🎛️ Applied SoundFont: $resolvedName"
            )
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
            val defaultSoundFonts = soundEngine.availableSoundFonts.map { it.displayName }
            val sfNames = if (result.soundFonts.isNotEmpty()) {
                val scanned = result.soundFonts.map { it.nameWithoutExtension }
                (defaultSoundFonts + scanned).distinct()
            } else {
                defaultSoundFonts
            }
            val rawSavedSf = prefs.getString("active_soundfont", "VideoKey Studio Minus-One") ?: "VideoKey Studio Minus-One"
            val resolvedSf = when {
                sfNames.any { it.equals(rawSavedSf, ignoreCase = true) } ->
                    sfNames.first { it.equals(rawSavedSf, ignoreCase = true) }
                rawSavedSf.contains("yamaha", ignoreCase = true) || rawSavedSf.contains("korg", ignoreCase = true) || rawSavedSf.contains("ketron", ignoreCase = true) -> "Yamaha x Korg x Ketron Studio"
                rawSavedSf.contains("minus", ignoreCase = true) || rawSavedSf.contains("videokey", ignoreCase = true) -> "VideoKey Studio Minus-One"
                rawSavedSf.contains("sam", ignoreCase = true) || rawSavedSf.contains("dream", ignoreCase = true) -> "Dream SAM2634 v2.1"
                rawSavedSf.contains("mega", ignoreCase = true) -> "MegaPro Sound"
                rawSavedSf.contains("songhub", ignoreCase = true) -> "SongHub Original Sound"
                else -> "VideoKey Studio Minus-One"
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

    fun scanBackgroundMedia(folder: File = File(_uiState.value.currentBackgroundFolder), forcePickVideo: Boolean = false) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val backgrounds = mutableListOf<KaraokeBackground>()
            val seenPaths = HashSet<String>()

            fun scanDir(dir: File) {
                if (!dir.exists() || !dir.isDirectory) return
                val allFiles = try {
                    dir.walkTopDown().maxDepth(6).filter { it.isFile && it.length() > 0L }.toList()
                } catch (e: Exception) {
                    dir.listFiles()?.filter { it.isFile && it.length() > 0L } ?: emptyList()
                }
                for (file in allFiles) {
                    if (!seenPaths.add(file.absolutePath)) continue
                    val ext = file.extension.lowercase()
                    val folderTag = if (file.parentFile != null && file.parentFile != dir) "[${file.parentFile!!.name}] " else ""
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

            scanDir(folder)

            // If selected folder has no media, also check default VideoKey backgrounds locations
            if (backgrounds.isEmpty()) {
                val base = contentDownloadManager.getBaseStorageDir()
                scanDir(File(base, "backgrounds"))
                scanDir(File("/storage/emulated/0/VideoKey/backgrounds"))
                scanDir(File("/sdcard/VideoKey/backgrounds"))
                scanDir(File("/storage/emulated/0/SongHub/bgv"))
            }

            // Only include stage fallback if no mp4 or image files were found anywhere
            if (backgrounds.isEmpty()) {
                backgrounds.add(KaraokeBackground("stage_neon", "✨ Neon Videoke Stage", "", BackgroundType.STAGE))
            }

            val mediaOnly = backgrounds.filter { it.type == BackgroundType.VIDEO || it.type == BackgroundType.IMAGE }
            val videoOnly = backgrounds.filter { it.type == BackgroundType.VIDEO }
            val savedId = prefs.getString("selected_bg", null)
            val savedBg = backgrounds.firstOrNull { b -> savedId != null && b.id == savedId }

            val newSel = if (forcePickVideo && videoOnly.isNotEmpty()) {
                videoOnly.shuffled().first()
            } else if (savedBg != null && (savedBg.type == BackgroundType.VIDEO || (videoOnly.isEmpty() && savedBg.type == BackgroundType.IMAGE))) {
                savedBg
            } else {
                videoOnly.shuffled().firstOrNull() ?: mediaOnly.shuffled().firstOrNull() ?: savedBg ?: backgrounds.firstOrNull()
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
        prefs.edit().putString("lyric_font_family", family.name).apply()
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

    fun setLyricOutlineColor(outlineColor: LyricOutlineColor) {
        prefs.edit().putString("lyric_outline_color", outlineColor.name).apply()
        _uiState.update { it.copy(lyricConfig = it.lyricConfig.copy(outlineColor = outlineColor)) }
    }

    fun setAppLanguage(language: AppLanguage) {
        prefs.edit().putString("app_language", language.code).apply()
        _uiState.update { it.copy(appLanguage = language) }
    }

    fun setAppTheme(theme: AppTheme) {
        prefs.edit().putString("app_theme", theme.name).apply()
        _uiState.update { it.copy(appTheme = theme) }
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
