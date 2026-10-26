package com.example.videokey.ui.main



import android.app.Activity

import com.example.videokey.data.SupabaseVipManager

import com.example.videokey.data.VipSecureStorage



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

import com.example.videokey.data.VipLicenseManager

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



enum class BackgroundVisualMode(val displayName: String, val tagalogName: String) {

    VIDEOS("Videos", "Mga Video"),

    PHOTOS("Photos", "Mga Larawan"),

    CAMERA("Camera", "Live Camera");



    fun label(isTagalog: Boolean): String = if (isTagalog) tagalogName else displayName

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



enum class ScoreMode(val displayName: String, val tagalogName: String) {

    RANDOM("Random", "Random (Awtomatiko)"),

    OFF("Off", "Naka-off (Walang Score)"),

    MICROPHONE("Microphone", "Mikropono (Real Mic)");



    fun label(isTagalog: Boolean): String = if (isTagalog) tagalogName else displayName

}



enum class ScoringDifficulty(val displayName: String, val tagalogName: String) {

    EASY("Easy", "Madali (Easy)"),

    MEDIUM("Medium", "Katamtaman (Medium)"),

    HARD("Hard", "Mahirap (Hard)");



    fun label(isTagalog: Boolean): String = if (isTagalog) tagalogName else displayName

}



enum class FanfareSoundLevel(val displayName: String, val tagalogName: String, val volume: Float) {

    OFF("Off", "Naka-off", 0.0f),

    LOW("Low", "Mahina (Low)", 0.35f),

    MID("Mid", "Katamtaman (Mid)", 0.70f),

    HIGH("High", "Malakas (High)", 1.0f);



    fun label(isTagalog: Boolean): String = if (isTagalog) tagalogName else displayName

}



enum class LyricFontFamily(val displayName: String) {

    RADIO_CANADA("Radio Canada"),

    SAN_SERIF("Arial Bold"),

    IMPACT_HEAVY("Impact Heavy"),

    CONDENSED_TALL("Bebas Tall"),

    GEOMETRIC_POP("Montserrat Pop (Default)"),

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



enum class LyricCountdownMode(val displayName: String, val tagalogName: String) {

    ARCADE_NUMBER("🔢 4-3-2-1 Number", "🔢 4-3-2-1 Numero"),

    CLASSIC_DOTS("🔴 4 Dots Videoke", "🔴 4 Tuldok Videoke"),

    PHOTO_BADGE("🖼️ Photo Countdown", "🖼️ Photo Countdown"),

    OFF("⛔ Off", "⛔ Patay");



    fun label(isTagalog: Boolean): String = if (isTagalog) tagalogName else displayName

}



data class LyricCustomization(

    val fontFamily: LyricFontFamily = LyricFontFamily.SAN_SERIF,

    val wipeColor: LyricWipeColor = LyricWipeColor.DARK_BLUE,

    val inactiveColor: Long = 0xFFFFFFFF, // Pure White with Outline

    val outlineColor: LyricOutlineColor = LyricOutlineColor.BLACK,

    val shadowStyle: LyricShadowStyle = LyricShadowStyle.CLEAN,

    val outlineThickness: Float = 3.0f,

    val fontSizePortraitSp: Float = 42.0f,

    val fontSizeLandscapeSp: Float = 63.0f,

    val lineSpacingDp: Float = 4.0f,

    val hasDropShadow: Boolean = false,

    val isAllCaps: Boolean = false,

    val isItalic: Boolean = false,

    val fontScale: Float = 1.0f,

    val lineCount: LyricLineCount = LyricLineCount.FOUR_LINES,

    val position: LyricScreenPosition = LyricScreenPosition.BOTTOM,

    val countdownMode: LyricCountdownMode = LyricCountdownMode.ARCADE_NUMBER,

    val timingOffsetMs: Int = 0,

    val isFpsDroppedTo30: Boolean = false,

    val isWipeHighlightEnabled: Boolean = true,

    val customCountdownPhotoUri: String? = null,

    val countdownImg4Uri: String? = null,

    val countdownImg3Uri: String? = null,

    val countdownImg2Uri: String? = null,

    val countdownImg1Uri: String? = null,

    val countdownImgGoUri: String? = null

)



enum class VipButtonTexture(val id: String, val displayName: String, val tagalogName: String) {

    DEFAULT("default", "Classic Flat", "Klasiko"),

    CARBON_FIBER("carbon", "Carbon Fiber", "Carbon Fiber"),

    BRUSHED_METAL("metal", "Brushed Metal", "Brushed Metal"),

    GOLD_GLOW("gold", "Royal Gold Velvet", "Gintong Pelus"),

    CYBER_GRID("cyber", "Cyber Neon Matrix", "Cyber Matrix");



    fun label(isTagalog: Boolean): String = if (isTagalog) tagalogName else displayName

}



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

    CYBER_NEON(

        "cyber_neon",

        "Cyber Neon KTV",

        "Cyber Neon KTV",

        primaryColor = 0xFFEC4899,      // Neon Pink / Magenta

        secondaryColor = 0xFF38BDF8,    // Electric Cyan

        surfaceColor = 0xFF161B2E,      // Deep Obsidian Card

        backgroundColor = 0xFF090D16,   // Midnight Base

        buttonBgColor = 0xFF1E293B,     // Sleek Slate Button

        buttonBorderColor = 0xFF334155, // Subtle Border

        textColor = 0xFFF8FAFC,         // Clean White

        textMutedColor = 0xFF94A3B8     // Muted Blue-Grey

    ),

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

    ),

    CRIMSON_ROCK(

        "crimson_rock",

        "Crimson Rock",

        "Crimson Rock (Pula)",

        primaryColor = 0xFFEF4444,      // Vibrant Ruby Crimson

        secondaryColor = 0xFFF87171,

        surfaceColor = 0xFF2A1215,

        backgroundColor = 0xFF180A0C,

        buttonBgColor = 0xFF35171B,

        buttonBorderColor = 0xFF5C1D24,

        textColor = 0xFFFFF1F2,

        textMutedColor = 0xFFFECDD3

    ),

    OCEAN_AQUA(

        "ocean_aqua",

        "Ocean Aqua",

        "Asul Dagat (Aqua)",

        primaryColor = 0xFF06B6D4,      // Vibrant Cyan/Aqua

        secondaryColor = 0xFF22D3EE,

        surfaceColor = 0xFF082F49,

        backgroundColor = 0xFF041826,

        buttonBgColor = 0xFF0E3F61,

        buttonBorderColor = 0xFF155E75,

        textColor = 0xFFECFEFF,

        textMutedColor = 0xFFA5F3FC

    ),

    SUNSET_ORANGE(

        "sunset_orange",

        "Sunset Groove",

        "Takipsilim (Orange)",

        primaryColor = 0xFFFF6B00,      // Radiant Sunset Orange

        secondaryColor = 0xFFFFA24C,

        surfaceColor = 0xFF2C1508,

        backgroundColor = 0xFF1A0C05,

        buttonBgColor = 0xFF3D1E0C,

        buttonBorderColor = 0xFF6C3415,

        textColor = 0xFFFFF7ED,

        textMutedColor = 0xFFFFEDD5

    ),

    PURPLE_GALAXY(

        "purple_galaxy",

        "Purple Galaxy",

        "Lilang Kalawakan",

        primaryColor = 0xFFA855F7,      // Cosmic Electric Purple

        secondaryColor = 0xFFC084FC,

        surfaceColor = 0xFF241038,

        backgroundColor = 0xFF150821,

        buttonBgColor = 0xFF32174D,

        buttonBorderColor = 0xFF581C87,

        textColor = 0xFFFAF5FF,

        textMutedColor = 0xFFE9D5FF

    );



    fun label(isTagalog: Boolean): String = if (isTagalog) tagalogName else displayName

}



enum class AppLanguage(val code: String, val displayName: String) {

    ENGLISH("en", "🇺🇸 English (Default)"),

    TAGALOG("tl", "🇵🇭 Tagalog")

}



data class VideoKeyUiState(

    val currentSong: Song = Song("000016", "Pasko Na, Pasko Na", "Noel Cabangon", "", "OPM"),

    val enteredCode: String = "",

    val matchedEnteredSong: Song? = null,

    val reserveQueue: List<Song> = emptyList(),

    val isPlaying: Boolean = false,
    val isPictureInPicture: Boolean = false,

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

    val bassBoostPercent: Int = 0,

    val treblePercent: Int = 0,

    val isMelodyMuted: Boolean = false,

    val isScreenRecording: Boolean = false,

    val screenRecordingSeconds: Int = 0,

    val isAutoMixerEnabled: Boolean = true,

    val isAutoVolumeEnabled: Boolean = true,

    val isAutoMelodyNormalizeEnabled: Boolean = true,

    val activeMixStyle: com.example.videokey.data.MidiMelodyScaler.SongMixStyle = com.example.videokey.data.MidiMelodyScaler.SongMixStyle.STANDARD_POP,

    val selectedBottomNavTab: Int = 0, // 0 = Home, 1 = Sing, 2 = Saved, 3 = Profile

    val selectedCategoryFilter: String = "ALL",

    val appTheme: AppTheme = AppTheme.SLATE_DARK,

    val userName: String = "JUAN",

    val isEditNameDialogOpen: Boolean = false,

    val isAdminKeyGeneratorOpen: Boolean = false,

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

    val currentSongFolder: String = "/storage/emulated/0/VideoKey/songs",

    val currentBackgroundFolder: String = "/storage/emulated/0/VideoKey/backgrounds",

    val isSongFolderPickerOpen: Boolean = false,

    val isBgFolderPickerOpen: Boolean = false,

    val activeSoundFontName: String = "Dream SAM2634 v2.1",

    val autoMatchedSoundFontForCurrentSong: String = "Dream SAM2634 v2.1",

    val availableSoundFonts: List<String> = listOf(

        "✨ Awtomatiko (Depende sa Genre)",

        "🎵 VideoKey OPM & Pop Studio",

        "🎸 VideoKey Rock & Band Studio",

        "🎹 VideoKey Acoustic & Ballad Studio",

        "🪩 VideoKey Disco & 80s/90s Synth",

        "🎺 VideoKey Cha-Cha & Ballroom",

        "🥁 VideoKey Party & Mega Beat",

        "Yamaha x Korg x Ketron Studio",

        "Dream SAM2634 v2.1",

        "MegaPro Sound"

    ),

    val isSoundFontDialogOpen: Boolean = false,

    val isSongbookOpen: Boolean = false,

    val songbookQuery: String = "",

    val isReserveQueueOpen: Boolean = false,

    val isFavoritesOpen: Boolean = false,

    val favoriteCodes: Set<String> = emptySet(),

    val favoriteSongs: List<Song> = emptyList(),

    val searchResults: List<Song> = emptyList(),

    val lastPlayedSong: Song? = null,

    val trendingSongs: List<Song> = emptyList(),

    val randomFavoriteSong: Song? = null,

    val isTvMode: Boolean = false,

    val scanStatus: String = "Internal Storage: Ready",

    val isScanning: Boolean = false,

    // Recording state:

    val isRecording: Boolean = false,

    val recordingSeconds: Int = 0,

    val vocalSyncOffsetMs: Int = com.example.videokey.data.KaraokeRecorder.DEFAULT_VOCAL_SYNC_OFFSET_MS,

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

    val vipButtonTexture: VipButtonTexture = VipButtonTexture.DEFAULT,

    val isMp3PlayerDialogOpen: Boolean = false,

    val mp3PlayerSongList: List<Song> = emptyList(),

    val activeMp3Song: Song? = null,

    val isMp3Playing: Boolean = false,

    val mp3CurrentPositionMs: Int = 0,

    val mp3DurationMs: Int = 0,

    val alertMessage: String? = null,

    val onlyShowPlayableSongs: Boolean = false,

    val appLanguage: AppLanguage = AppLanguage.ENGLISH,

    val updateState: com.example.videokey.update.AppUpdateManager.UpdateState = com.example.videokey.update.AppUpdateManager.UpdateState.Idle,

    val isDownloadCenterOpen: Boolean = false,

    val downloadCatalog: List<com.example.videokey.data.ContentDownloadManager.DownloadItem> = emptyList(),

    val downloadStatus: com.example.videokey.data.ContentDownloadManager.DownloadStatus = com.example.videokey.data.ContentDownloadManager.DownloadStatus.Idle,

    val isRemoteConnected: Boolean = false,

    val isStartupGatePassed: Boolean = false,

    val isVideoSong: Boolean = false,

    val isYouTubeDownloaderOpen: Boolean = false,

    val youTubeSearchQuery: String = "",

    val youTubeSearchResults: List<com.example.videokey.data.YouTubeVideoItem> = emptyList(),

    val isYouTubeSearching: Boolean = false,

    val youTubeDownloadProgress: com.example.videokey.data.YouTubeDownloadProgress? = null,

    val activeYouTubeStreamVideoId: String? = null,

    val activeYouTubeStreamTitle: String? = null,

    val isPlayingYouTubeStream: Boolean = false,

    val downloadedMp4Songs: List<com.example.videokey.data.Song> = emptyList(),

    val playableCodes: Set<String> = emptySet(),

    val isVipActivated: Boolean = false,

    val deviceId: String = "",

    val vipActivationError: String? = null,

    val scoreMode: ScoreMode = ScoreMode.MICROPHONE,

    val scoringDifficulty: ScoringDifficulty = ScoringDifficulty.MEDIUM,

    val fanfareSoundLevel: FanfareSoundLevel = FanfareSoundLevel.HIGH,

    val isPlaybackPaused: Boolean = false,

    val bgVisualMode: BackgroundVisualMode = BackgroundVisualMode.VIDEOS,

    val continuousVideoChangePerSong: Boolean = false,

    val isFpsDroppedTo30: Boolean = false,

    val isCheckingVipStatus: Boolean = false,
    val scoringSoundStyle: com.example.videokey.data.ScoringSoundStyle = com.example.videokey.data.ScoringSoundStyle.CLASSIC_ARCADE,
    val customScoringSoundUri: String? = null,
    val isCameraFrontFacing: Boolean = false,

    val isInitiatingGcashPayment: Boolean = false

)



class VideoKeyViewModel(application: Application) : AndroidViewModel(application) {



    internal val repository = SongRepository()

    private val karaokeRecorder = com.example.videokey.data.KaraokeRecorder()

    private val soundEngine = SoundEngine(application.applicationContext, karaokeRecorder)

    private val voiceManager = com.example.videokey.data.KeypadVoiceManager(application.applicationContext)

    private val micScorer = com.example.videokey.data.MicrophoneScorer(karaokeRecorder)

    val appUpdateManager = com.example.videokey.update.AppUpdateManager(application.applicationContext)

    val contentDownloadManager = com.example.videokey.data.ContentDownloadManager(application.applicationContext)

    val youTubeDownloader = com.example.videokey.data.YouTubeKaraokeDownloader(application.applicationContext)

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

    private val categoryCache = java.util.concurrent.ConcurrentHashMap<String, List<Song>>()

    private var categoryJob: Job? = null



    init {

        // Detect initial song folder

        val savedSongFolder = prefs.getString("song_folder", null)

        val initialSongFolder = when {

            !savedSongFolder.isNullOrBlank() && File(savedSongFolder).exists() && savedSongFolder != "/storage/emulated/0/SongHub" -> savedSongFolder

            File("/storage/emulated/0/VideoKey/songs").exists() -> "/storage/emulated/0/VideoKey/songs"

            File("/storage/emulated/0/VideoKey").exists() -> "/storage/emulated/0/VideoKey"

            File("/sdcard/VideoKey/songs").exists() -> "/sdcard/VideoKey/songs"

            File("/sdcard/VideoKey").exists() -> "/sdcard/VideoKey"

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



        val savedLang = prefs.getString("app_language", "en") ?: "en"

        val initialLang = if (savedLang == "tl") AppLanguage.TAGALOG else AppLanguage.ENGLISH

        val isLocalSecureVip = VipSecureStorage.isVipActive(application)

        val isVip = prefs.getBoolean("is_vip_activated", false) || isLocalSecureVip

        val devId = SupabaseVipManager.getHardwareDeviceId(application)



        val defaultSf = if (isVip) "✨ Awtomatiko (Depende sa Genre)" else "Dream SAM2634 v2.1"

        val rawSavedSf = prefs.getString("active_soundfont", defaultSf) ?: defaultSf

        val isRawAuto = rawSavedSf.contains("auto", ignoreCase = true) || rawSavedSf.contains("awtomatiko", ignoreCase = true)

        val savedSf = when {

            isRawAuto && isVip -> "✨ Awtomatiko (Depende sa Genre)"

            isRawAuto && !isVip -> "Dream SAM2634 v2.1"

            rawSavedSf.contains("rock", ignoreCase = true) -> "🎸 VideoKey Rock & Band Studio"

            rawSavedSf.contains("acoustic", ignoreCase = true) || rawSavedSf.contains("ballad", ignoreCase = true) -> "🎹 VideoKey Acoustic & Ballad Studio"

            rawSavedSf.contains("disco", ignoreCase = true) || rawSavedSf.contains("synth", ignoreCase = true) || rawSavedSf.contains("dance", ignoreCase = true) -> "🪩 VideoKey Disco & 80s/90s Synth"

            rawSavedSf.contains("party", ignoreCase = true) -> "🥁 VideoKey Party & Mega Beat"

            rawSavedSf.contains("yamaha", ignoreCase = true) || rawSavedSf.contains("korg", ignoreCase = true) || rawSavedSf.contains("ketron", ignoreCase = true) -> "Yamaha x Korg x Ketron Studio"

            rawSavedSf.contains("mega", ignoreCase = true) -> "MegaPro Sound"

            rawSavedSf.contains("sam", ignoreCase = true) || rawSavedSf.contains("dream", ignoreCase = true) -> "Dream SAM2634 v2.1"

            else -> "Dream SAM2634 v2.1"

        }

        val initialSoundFontToLoad = if (savedSf.contains("auto", ignoreCase = true) || savedSf.contains("awtomatiko", ignoreCase = true)) {

            "videokey_pop"

        } else {

            savedSf

        }

        soundEngine.setActiveSoundFont(initialSoundFontToLoad)

        val savedMelodyMuted = prefs.getBoolean("is_melody_muted", false)

        soundEngine.setMelodyMuted(savedMelodyMuted)

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

        val initialBassBoost = if (isVip) prefs.getInt("bass_boost_percent", 0) else 0

        val initialTreble = if (isVip) prefs.getInt("treble_percent", 0) else 0

        soundEngine.setBassBoost(initialBassBoost)

        soundEngine.setTreble(initialTreble)

        val initialAutoMixer = if (isVip) prefs.getBoolean("auto_mixer_enabled", true) else false

        soundEngine.setAutoMixerEnabled(initialAutoMixer)

        val savedAutoVolume = prefs.getBoolean("auto_volume_enabled", true)

        soundEngine.setAutoVolumeEnabled(savedAutoVolume)

        val savedAutoMelody = prefs.getBoolean("auto_melody_normalize_enabled", true)

        soundEngine.setAutoMelodyNormalizeEnabled(savedAutoMelody)



        val savedThemeName = prefs.getString("app_theme", AppTheme.SLATE_DARK.name) ?: AppTheme.SLATE_DARK.name

        val initialTheme = try {

            AppTheme.valueOf(savedThemeName)

        } catch (e: Exception) {

            AppTheme.SLATE_DARK

        }



        val savedFavSet = prefs.getStringSet("favorite_song_codes", emptySet())?.toSet() ?: emptySet()

        val initialFavSongs = savedFavSet.mapNotNull { repository.findByCode(it) }



        val savedScoreModeName = prefs.getString("score_mode", ScoreMode.MICROPHONE.name) ?: ScoreMode.MICROPHONE.name

        val initialScoreMode = try { ScoreMode.valueOf(savedScoreModeName) } catch (_: Exception) { ScoreMode.MICROPHONE }



        val savedDifficultyName = prefs.getString("scoring_difficulty", ScoringDifficulty.MEDIUM.name) ?: ScoringDifficulty.MEDIUM.name

        val initialDifficulty = try { ScoringDifficulty.valueOf(savedDifficultyName) } catch (_: Exception) { ScoringDifficulty.MEDIUM }



        val savedFanfareName = prefs.getString("fanfare_sound_level", FanfareSoundLevel.HIGH.name) ?: FanfareSoundLevel.HIGH.name

        val initialFanfare = try { FanfareSoundLevel.valueOf(savedFanfareName) } catch (_: Exception) { FanfareSoundLevel.HIGH }



        val savedBgVisualModeName = prefs.getString("bg_visual_mode", BackgroundVisualMode.VIDEOS.name) ?: BackgroundVisualMode.VIDEOS.name

        val initialBgVisualMode = try { BackgroundVisualMode.valueOf(savedBgVisualModeName) } catch (_: Exception) { BackgroundVisualMode.VIDEOS }



        val savedContinuousChange = prefs.getBoolean("continuous_video_change_per_song", false)
        val savedScoringStyleName = prefs.getString("scoring_sound_style", com.example.videokey.data.ScoringSoundStyle.CLASSIC_ARCADE.name) ?: com.example.videokey.data.ScoringSoundStyle.CLASSIC_ARCADE.name
        val initialScoringSoundStyle = try { com.example.videokey.data.ScoringSoundStyle.valueOf(savedScoringStyleName) } catch (_: Exception) { com.example.videokey.data.ScoringSoundStyle.CLASSIC_ARCADE }
        val initialCustomScoringSoundUri = prefs.getString("custom_scoring_sound_uri", null)
        val initialCameraFrontFacing = prefs.getBoolean("camera_is_front_facing", false)



        val hasAppliedTvDefaults = prefs.getBoolean("lyric_tv_defaults_applied_v3", false)

        if (!hasAppliedTvDefaults) {

            prefs.edit()

                .putString("lyric_outline_color", LyricOutlineColor.NAVY.name)

                .putString("lyric_font_family", LyricFontFamily.GEOMETRIC_POP.name)

                .putString("lyric_shadow_style", LyricShadowStyle.CLEAN.name)

                .putString("lyric_wipe_color", LyricWipeColor.DARK_BLUE.name)

                .putFloat("lyric_outline_thickness", 3.0f)

                .putFloat("lyric_font_size_portrait", 36.0f)

                .putFloat("lyric_font_size_landscape", 54.0f)

                .putFloat("lyric_line_spacing", 8.0f)

                .putBoolean("lyric_has_drop_shadow", true)

                .putBoolean("lyric_all_caps", false)

                .putBoolean("lyric_is_italic", false)

                .putFloat("lyric_font_scale", 0.85f)

                .putString("lyric_line_count", LyricLineCount.TWO_LINES.name)

                .putString("lyric_position", LyricScreenPosition.BOTTOM.name)

                .putString("lyric_countdown_mode", LyricCountdownMode.CLASSIC_DOTS.name)

                .putInt("lyric_timing_offset", 0)

                .putBoolean("lyric_tv_defaults_applied_v3", true)

                .apply()

        }



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



        val savedShadowStyleName = prefs.getString("lyric_shadow_style", LyricShadowStyle.CLEAN.name) ?: LyricShadowStyle.CLEAN.name

        val initialShadowStyle = try {

            LyricShadowStyle.valueOf(savedShadowStyleName)

        } catch (_: Exception) {

            LyricShadowStyle.CLEAN

        }



        val savedWipeColorName = prefs.getString("lyric_wipe_color", LyricWipeColor.DARK_BLUE.name) ?: LyricWipeColor.DARK_BLUE.name

        val initialWipeColor = try {

            LyricWipeColor.valueOf(savedWipeColorName)

        } catch (_: Exception) {

            LyricWipeColor.DARK_BLUE

        }



        val savedLineCountName = prefs.getString("lyric_line_count", LyricLineCount.FOUR_LINES.name) ?: LyricLineCount.FOUR_LINES.name

        val initialLineCount = try {

            LyricLineCount.valueOf(savedLineCountName)

        } catch (_: Exception) {

            LyricLineCount.FOUR_LINES

        }



        val savedPositionName = prefs.getString("lyric_position", LyricScreenPosition.BOTTOM.name) ?: LyricScreenPosition.BOTTOM.name

        val initialPosition = try {

            LyricScreenPosition.valueOf(savedPositionName)

        } catch (_: Exception) {

            LyricScreenPosition.BOTTOM

        }



        val savedCountdown4 = prefs.getString("lyric_countdown_img_4_uri", null)
        val savedCountdown3 = prefs.getString("lyric_countdown_img_3_uri", null)
        val savedCountdown2 = prefs.getString("lyric_countdown_img_2_uri", null)
        val savedCountdown1 = prefs.getString("lyric_countdown_img_1_uri", null)
        val savedCountdownGo = prefs.getString("lyric_countdown_img_go_uri", null)
        val savedCountdownPhoto = prefs.getString("lyric_countdown_photo_uri", null)
        val savedCountdownModeName = prefs.getString("lyric_countdown_mode", LyricCountdownMode.ARCADE_NUMBER.name) ?: LyricCountdownMode.ARCADE_NUMBER.name

        val initialCountdownMode = try { LyricCountdownMode.valueOf(savedCountdownModeName) } catch (_: Exception) { LyricCountdownMode.ARCADE_NUMBER }

        val initialTimingOffset = prefs.getInt("lyric_timing_offset", 0)

        val savedTextureName = prefs.getString("vip_button_texture", VipButtonTexture.DEFAULT.name) ?: VipButtonTexture.DEFAULT.name

        val initialTexture = try { VipButtonTexture.valueOf(savedTextureName) } catch (_: Exception) { VipButtonTexture.DEFAULT }



        val savedLastPlayedCode = prefs.getString("last_played_song_code", null)

        val initialLastPlayed = savedLastPlayedCode?.let { repository.findByCode(it) }

        val initialAllSongs = repository.getAllSongs()

        val initialTrending = initialAllSongs



        _uiState.update {

            it.copy(

                searchResults = repository.search(""),

                lastPlayedSong = initialLastPlayed ?: repository.findByCode("000016") ?: initialAllSongs.firstOrNull(),

                trendingSongs = initialTrending,

                currentSongFolder = initialSongFolder,

                currentBackgroundFolder = initialBgFolder,

                backgroundChangeMode = initialBgMode,

                micSensitivity = initialSensitivity,

                appLanguage = initialLang,

                isVipActivated = isVip,

                deviceId = devId,

                activeSoundFontName = savedSf,

                melodyPercent = initialMelody,

                drumsPercent = initialDrums,

                bassPercent = initialBass,

                guitarPianoPercent = initialGuitarPiano,

                stringsBrassPercent = initialStringsBrass,

                reverbPercent = initialReverb,

                bassBoostPercent = initialBassBoost,

                treblePercent = initialTreble,

                isMelodyMuted = savedMelodyMuted,

                isAutoMixerEnabled = initialAutoMixer,

                isAutoVolumeEnabled = savedAutoVolume,

                isAutoMelodyNormalizeEnabled = savedAutoMelody,

                appTheme = initialTheme,

                userName = run {

                    val saved = prefs.getString("user_profile_name", null)

                    if (saved.isNullOrBlank() || saved == "Jerome Huet") {

                        prefs.edit().putString("user_profile_name", "JUAN").apply()

                        "JUAN"

                    } else saved

                },

                isMicScoringEnabled = initialScoreMode != ScoreMode.OFF,

                isScoringSoundEnabled = initialFanfare != FanfareSoundLevel.OFF,

                isKeypadVoiceEnabled = prefs.getBoolean("is_keypad_voice_enabled", true),

                scoreMode = initialScoreMode,

                vipButtonTexture = initialTexture,

                scoringDifficulty = initialDifficulty,

                fanfareSoundLevel = initialFanfare,

                bgVisualMode = initialBgVisualMode,

                continuousVideoChangePerSong = savedContinuousChange,
                scoringSoundStyle = initialScoringSoundStyle,
                customScoringSoundUri = initialCustomScoringSoundUri,
                isCameraFrontFacing = initialCameraFrontFacing,

                lyricConfig = it.lyricConfig.copy(

                    outlineColor = initialOutline,

                    fontFamily = initialFontFamily,

                    shadowStyle = initialShadowStyle,

                    wipeColor = initialWipeColor,

                    outlineThickness = prefs.getFloat("lyric_outline_thickness", 3.0f),

                    fontSizePortraitSp = prefs.getFloat("lyric_font_size_portrait", 42.0f),

                    fontSizeLandscapeSp = prefs.getFloat("lyric_font_size_landscape", 63.0f),

                    lineSpacingDp = prefs.getFloat("lyric_line_spacing", 4.0f),

                    hasDropShadow = prefs.getBoolean("lyric_has_drop_shadow", false),

                    isAllCaps = prefs.getBoolean("lyric_all_caps", false),

                    isItalic = prefs.getBoolean("lyric_is_italic", false),

                    fontScale = run {

                        val saved = prefs.getFloat("lyric_font_scale", 1.0f)

                        if (Math.abs(saved - 0.85f) < 0.01f && !prefs.getBoolean("has_migrated_font_scale_v26", false)) {

                            prefs.edit().putFloat("lyric_font_scale", 1.0f).putBoolean("has_migrated_font_scale_v26", true).apply()

                            1.0f

                        } else saved

                    },

                    lineCount = initialLineCount,

                    position = initialPosition,

                    countdownMode = initialCountdownMode,

                    customCountdownPhotoUri = savedCountdownPhoto,

                    countdownImg4Uri = savedCountdown4,

                    countdownImg3Uri = savedCountdown3,

                    countdownImg2Uri = savedCountdown2,

                    countdownImg1Uri = savedCountdown1,

                    countdownImgGoUri = savedCountdownGo,

                    timingOffsetMs = initialTimingOffset,

                    isWipeHighlightEnabled = prefs.getBoolean("lyric_wipe_highlight_enabled", true)

                ),

                favoriteCodes = savedFavSet,

                favoriteSongs = initialFavSongs,

                savedRecordings = karaokeRecorder.listSavedRecordings(),

                vocalSyncOffsetMs = run {

                    val saved = prefs.getInt("vocal_sync_offset_ms", com.example.videokey.data.KaraokeRecorder.DEFAULT_VOCAL_SYNC_OFFSET_MS)

                    karaokeRecorder.vocalSyncOffsetMs = saved

                    saved

                }

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

                    } else if (status.itemId == "core_songs" || status.itemId == "core_songs_47k" || status.itemId == "vip_songs_54k" || status.itemId == "vip_songs_57k") {

                        if (status.itemId == "vip_songs_54k" || status.itemId == "vip_songs_57k") {

                            contentDownloadManager.deleteOld47kSongPack()

                        }

                        scanSongFolder(File(_uiState.value.currentSongFolder))

                        if (contentDownloadManager.isSoundFontInstalled() && contentDownloadManager.isBackgroundVideoInstalled()) {

                            passStartupGate()

                        }

                    } else if (status.itemId == "all_soundfonts") {

                        scanSongFolder(File(_uiState.value.currentSongFolder))

                        val hasSongs = contentDownloadManager.hasAnySongsInstalled()

                        if (hasSongs && contentDownloadManager.isBackgroundVideoInstalled()) {

                            passStartupGate()

                        }

                    } else if (status.itemId == "all_backgrounds") {

                        scanBackgroundMedia(File(_uiState.value.currentBackgroundFolder), forcePickVideo = true)

                        val hasSongs = contentDownloadManager.hasAnySongsInstalled()

                        val hasSf = contentDownloadManager.isSoundFontInstalled()

                        if (hasSongs && hasSf) {

                            passStartupGate()

                        }

                    } else {

                        scanSongFolder(File(_uiState.value.currentSongFolder))

                        scanBackgroundMedia(File(_uiState.value.currentBackgroundFolder))

                    }

                }

            }

        }

        refreshDownloadCatalog()



        // Startup Gate: auto-detect files on device and auto-download if missing

        checkAndAutoDetectFiles(triggerAutoDownloadIfMissing = true)



        // Background OTA Update Check across any internet connection (Wi-Fi, Data)

        appUpdateManager.checkForUpdates(autoDownloadIfNewer = false, isManual = false)



        // Automatic silent online VIP license verification

        if (!isVip) {

            checkOnlineVipStatus(showMessage = false)

        }

    }



    @Volatile

    private var isCompletingSong = false



    fun playSong(song: Song) {

        isCompletingSong = false

        stopMp3Player()

        playbackJob?.cancel()

        melodyReloadJob?.cancel()

        soundEngine.stop()

        karaokeRecorder.stopPlayingRecording()

        songStartTimeMs = System.currentTimeMillis()



        val shouldChangeBg = _uiState.value.backgroundChangeMode == BackgroundChangeMode.AUTO_CHANGE_PER_SONG ||

                (_uiState.value.backgroundChangeMode == BackgroundChangeMode.CONTINUOUS && _uiState.value.continuousVideoChangePerSong)

        if (shouldChangeBg) {

            advanceNextBackground()

        }



        val isVideo = song.filePath.endsWith(".mp4", ignoreCase = true) || song.filePath.endsWith(".mkv", ignoreCase = true)



        if (!isVideo && (_uiState.value.scoreMode == ScoreMode.MICROPHONE || _uiState.value.isRecording)) {

            micScorer.startScoring()

        }



        prefs.edit().putString("last_played_song_code", song.code).apply()



        _uiState.update {

            it.copy(

                currentSong = song,

                lastPlayedSong = song,

                isPlaying = true,

                isPlaybackPaused = false,

                isVideoSong = isVideo,

                isTvMode = it.isTvMode,

                selectedBottomNavTab = 1,

                enteredCode = "",

                matchedEnteredSong = null,

                wipePercent = 0f,

                countdownDots = 0,

                showSongTitleIntro = true,

                isInstrumental = false,

                playingRecordingPath = null,

                line1 = song.title,

                line2 = song.artist,

                line3 = "",

                line4 = "",

                activeLineNumber = 0,

                landscapeLine1 = song.title,

                landscapeLine2 = song.artist,

                activeLyricPosition = ActiveLyricPosition.TOP

            )

        }



        if (isVideo) {

            // For MP4 Video Karaoke, audio & video are played directly via KaraokeVideoPlayer!

            viewModelScope.launch {

                delay(3000)

                _uiState.update { it.copy(showSongTitleIntro = false) }

            }

            return

        }



        viewModelScope.launch(Dispatchers.IO) {

            var rawMidiBytes: ByteArray? = null

            var isTempFile = false

            var midiFile: File? = null

            val currentMelody = _uiState.value.melodyPercent

            val currentSoundFont = _uiState.value.activeSoundFontName

            val isAutoMode = (currentSoundFont.contains("auto", ignoreCase = true) || currentSoundFont.contains("awtomatiko", ignoreCase = true)) && _uiState.value.isVipActivated

            val effectiveSoundFont = if (isAutoMode) {

                val matched = soundEngine.resolveGenreForSong(song, null)

                _uiState.update { it.copy(autoMatchedSoundFontForCurrentSong = matched.displayName) }

                matched.id

            } else {

                currentSoundFont

            }

            soundEngine.setActiveSoundFont(effectiveSoundFont)



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



            // Fast background analysis for zero-latency start

            val midiAnalysis = if (rawMidiBytes != null) {

                try {

                    com.example.videokey.data.MidiMelodyScaler.analyzeMidi(rawMidiBytes)

                } catch (e: Exception) {

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

                    soundEngine.playMidi(midiFile.absolutePath, song = song, deleteAfterOpen = isTempFile, precomputedAnalysis = midiAnalysis) {

                        onSongCompleted()

                    }

                    val currentSf = soundEngine.getActiveSoundFont()

                    if (isAutoMode) {

                        _uiState.update { it.copy(autoMatchedSoundFontForCurrentSong = currentSf.displayName) }

                    }

                    val autoStyle = soundEngine.getDetectedMixStyle()

                    if (_uiState.value.isAutoMixerEnabled) {

                        _uiState.update {

                            it.copy(

                                activeMixStyle = autoStyle,

                                melodyPercent = soundEngine.getMelodyVolume(),

                                drumsPercent = soundEngine.getDrumsVolume(),

                                bassPercent = soundEngine.getBassVolume(),

                                guitarPianoPercent = soundEngine.getGuitarPianoVolume(),

                                stringsBrassPercent = soundEngine.getStringsBrassVolume(),

                                reverbPercent = soundEngine.getReverbVolume()

                            )

                        }

                    } else {

                        _uiState.update { it.copy(activeMixStyle = autoStyle) }

                    }

                    if (!soundEngine.hasLoadedSoundFont()) {

                        val isTl = _uiState.value.appLanguage == AppLanguage.TAGALOG

                        val msg = if (isTl) "⚠️ Naka-fallback sa basic synth. I-download ang SoundFont sa Download Center para sa live videoke sound!"

                                  else "⚠️ Using basic system synth. Download the SoundFont Bank in Download Center for studio sound!"

                        _uiState.update { it.copy(alertMessage = msg) }

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

            var lastAudioPosMs = -1L

            var lastAudioSystemClockMs = 0L

            var maxWipeForCurrentLine = 0f

            var currentWipeLineIndex = -1

            while (true) {
                // Rock-solid 60 FPS playback loop (strictly 16ms per frame, zero throttle)
                delay(16L)
                if (!_uiState.value.isPlaying || _uiState.value.isPlaybackPaused) {
                    lastAudioPosMs = -1L
                    lastAudioSystemClockMs = 0L
                    continue
                }

                val isAudioPlaying = soundEngine.isPlaying()

                val rawPosMs = soundEngine.getCurrentPositionMs()

                val nowElapsed = android.os.SystemClock.elapsedRealtime()

                val elapsedSinceStart = (System.currentTimeMillis() - songStartTimeMs).coerceAtLeast(0L)

                // Resilient high-precision audio clock:
                // If hardware audio position is advancing, use it directly.
                // If rawPosMs returns 0 or fails to advance while audio is playing (e.g. Android MediaPlayer MIDI bug on other phones/tablets),
                // smoothly fallback to elapsed wall-clock so lyrics highlight NEVER gets stuck at 0%!
                val effectiveAudioPos = if (rawPosMs > 0L) {
                    rawPosMs
                } else if (isAudioPlaying && elapsedSinceStart > 0L) {
                    elapsedSinceStart
                } else {
                    0L
                }

                if (effectiveAudioPos != lastAudioPosMs || !isAudioPlaying) {
                    lastAudioPosMs = effectiveAudioPos
                    lastAudioSystemClockMs = nowElapsed
                }

                // Extrapolate hardware audio clock for jitter-free, linear 60 FPS frame pacing:
                val extrapolatedMs = if (isAudioPlaying && lastAudioSystemClockMs > 0L) {
                    val delta = (nowElapsed - lastAudioSystemClockMs).coerceIn(0L, 80L)
                    lastAudioPosMs + delta
                } else {
                    effectiveAudioPos
                }

                val offset = _uiState.value.lyricConfig.timingOffsetMs

                val posMs = (extrapolatedMs + offset).coerceAtLeast(0L)

                val durMs = soundEngine.getDurationMs()

                val lines = parsedKaraoke?.lines.orEmpty()

                val melodyNotes = parsedKaraoke?.melodyNotes.orEmpty()

                val firstLineStart = lines.firstOrNull()?.startMs ?: 10000L

                val timeUntilFirstLine = firstLineStart - posMs

                val isCountingDownIntro = timeUntilFirstLine in 1L..4000L && firstLineStart >= 1200L

                val isTitleIntroActive = elapsedSinceStart in 0L..3500L && (lines.isEmpty() || (posMs < firstLineStart && !isCountingDownIntro))



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



                if (com.example.videokey.data.ScreenCaptureService.isRunning) {

                    val scrSecs = com.example.videokey.data.ScreenCaptureService.getDurationSeconds()

                    if (scrSecs != _uiState.value.screenRecordingSeconds) {

                        _uiState.update { it.copy(isScreenRecording = true, screenRecordingSeconds = scrSecs) }

                    }

                } else if (_uiState.value.isScreenRecording) {

                    _uiState.update { it.copy(isScreenRecording = false, screenRecordingSeconds = 0) }

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

                    if (cachedLineIdx >= lines.size || (cachedLineIdx > 0 && lines[cachedLineIdx].startMs > posMs)) {
                        cachedLineIdx = 0
                    }

                    for (i in cachedLineIdx until lines.size) {

                        val cur = lines[i]

                        val nextStart = lines.getOrNull(i + 1)?.startMs ?: (cur.endMs + 500L)

                        val holdEnd = if (nextStart > cur.endMs && (nextStart - cur.endMs) <= 300L) {
                            nextStart
                        } else {
                            minOf(cur.endMs + 280L, (nextStart - 40L).coerceAtLeast(cur.endMs))
                        }

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

                        if (activeIndex != currentWipeLineIndex) {
                            currentWipeLineIndex = activeIndex
                            maxWipeForCurrentLine = 0f
                        }
                        val rawWipe = computeMelodyWipe(currentLine, posMs)
                        if (rawWipe > maxWipeForCurrentLine) {
                            maxWipeForCurrentLine = rawWipe
                        }
                        // Strictly monotonic: wipe can NEVER move backwards during an active line!
                        val wipe = maxWipeForCurrentLine

                        val isCountdownEligible = (activeIndex == 0 && currentLine.startMs >= 7000L) ||
                            (activeIndex > 0 && (currentLine.startMs - (lines.getOrNull(activeIndex - 1)?.endMs ?: 0L)) >= 7000L)
                        val showGoBadge = isCountdownEligible && (posMs - currentLine.startMs) in 0L..450L
                        val currentDots = if (showGoBadge) 5 else 0

                        val wipeDelta = kotlin.math.abs(wipe - lastEmittedWipe)

                        val lineChanged = activeIndex != lastEmittedActiveIndex

                        val introChanged = isTitleIntroActive != lastEmittedIntro

                        val instChanged = lastEmittedInst

                        val dotsChanged = currentDots != lastEmittedDots

                        if (lineChanged || introChanged || instChanged || dotsChanged || wipeDelta >= 0.0001f) {

                            lastEmittedWipe = wipe

                            lastEmittedActiveIndex = activeIndex

                            lastEmittedIntro = false

                            lastEmittedInst = false

                            lastEmittedDots = currentDots



                            // 1. Continuous rolling 4-line calculation:

                            // The moment a line's highlight finishes, that slot immediately rolls to the next upcoming line not on screen!

                            val rolling4 = computeRollingLyrics4(lines, activeIndex)

                            val lineInPage = (activeIndex % 4) + 1



                            // 2. Continuous rolling 2-line alternating calculation

                            val (land1, land2) = computeRollingLyrics2(lines, activeIndex)

                            val landPos = if (activeIndex % 2 == 0) ActiveLyricPosition.TOP else ActiveLyricPosition.BOTTOM



                            _uiState.update {

                                it.copy(

                                    showSongTitleIntro = false,

                                    line1 = rolling4[0],

                                    line2 = rolling4[1],

                                    line3 = rolling4[2],

                                    line4 = rolling4[3],

                                    activeLineNumber = lineInPage,

                                    landscapeLine1 = land1,

                                    landscapeLine2 = land2,

                                    activeLyricPosition = landPos,

                                    wipePercent = wipe,

                                    countdownDots = currentDots,

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

                            val timeUntilStart = upcoming.startMs - posMs



                            // Long instrumental gap or song intro detection:

                            val prevLineEndMs = if (upcomingIndex > 0) lines[upcomingIndex - 1].endMs else 0L

                            val instrumentalGap = upcoming.startMs - prevLineEndMs

                            val isCountdownEligible = (upcomingIndex == 0 && upcoming.startMs >= 7000L) || (instrumentalGap >= 7000L)



                            // Countdown: 4 seconds before vocals start for song intro or after instrumental gap!

                            val dots = if (isCountdownEligible && timeUntilStart in 1L..4000L) {

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



                            val showInstrumental = !isTitleIntroActive && dots == 0 && (timeUntilStart > 3500L) && (instrumentalGap >= 4500L) && elapsedSinceStart > 3500L



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



                                val rolling4 = computeRollingLyrics4(lines, upcomingIndex)

                                val lineInPage = (upcomingIndex % 4) + 1



                                val (land1, land2) = computeRollingLyrics2(lines, upcomingIndex)

                                val landPos = if (upcomingIndex % 2 == 0) ActiveLyricPosition.TOP else ActiveLyricPosition.BOTTOM



                                _uiState.update {

                                    it.copy(

                                        showSongTitleIntro = isTitleIntroActive,

                                        line1 = rolling4[0],

                                        line2 = rolling4[1],

                                        line3 = rolling4[2],

                                        line4 = rolling4[3],

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

                            line1 = it.currentSong.title,

                            line2 = it.currentSong.artist,

                            line3 = "MUSIC PLAYING LIVE",

                            line4 = "KARAOKE INSTRUMENTAL",

                            activeLineNumber = step,

                            landscapeLine1 = it.currentSong.title,

                            landscapeLine2 = it.currentSong.artist,

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
        val cleanLineText = line.fullText.trimEnd('-', ' ')
        val totalChars = cleanLineText.length.coerceAtLeast(1)

        val lastSyl = syllables.last()
        val lineEnd = maxOf(lastSyl.timestampMs + lastSyl.durationMs, line.endMs)
        if (posMs >= lineEnd) return 1f

        var charAccumulator = 0
        val sylCharRanges = Array(syllables.size) { i ->
            val startChar = charAccumulator
            val sylLen = syllables[i].text.length
            val endChar = if (i == syllables.lastIndex) {
                totalChars
            } else {
                (charAccumulator + sylLen).coerceAtMost(totalChars)
            }
            charAccumulator = endChar
            Pair(startChar, endChar)
        }

        for (i in syllables.indices) {
            val syl = syllables[i]
            val sylStart = syl.timestampMs
            val rawSylEnd = sylStart + syl.durationMs
            val nextSylStart = syllables.getOrNull(i + 1)?.timestampMs

            // Continuous natural singing bridge:
            // In singing, notes sustain until the next syllable or rest.
            // Bridging gaps up to 1200ms ensures uninterrupted, butter-smooth wipe
            // without freezing or rushing across every song tempo!
            val handoffEnd = if (nextSylStart != null && nextSylStart > sylStart) {
                val gap = nextSylStart - rawSylEnd
                if (gap <= 1200L) {
                    nextSylStart
                } else {
                    minOf(rawSylEnd + 500L, sylStart + ((nextSylStart - sylStart) * 0.7f).toLong())
                }
            } else {
                maxOf(rawSylEnd, line.endMs)
            }

            val (startChar, endChar) = sylCharRanges[i]
            val sylCharCount = (endChar - startChar).coerceAtLeast(1)

            if (posMs < sylStart) {
                break
            } else if (posMs in sylStart..handoffEnd) {
                val dur = (handoffEnd - sylStart).coerceAtLeast(1L)
                val frac = ((posMs - sylStart).toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                val activeChar = startChar + (frac * sylCharCount)
                return (activeChar / totalChars.toFloat()).coerceIn(0f, 1f)
            }
        }

        return (charAccumulator.toFloat() / totalChars.toFloat()).coerceIn(0f, 1f)
    }

    private fun computeRollingLyrics4(lines: List<KaraokeLine>, currIdx: Int): List<String> {

        val activeSlot = currIdx % 4

        return (0..3).map { s ->

            val lineIdx = if (s >= activeSlot) {

                currIdx + (s - activeSlot)

            } else {

                currIdx + (4 - activeSlot) + s

            }

            lines.getOrNull(lineIdx)?.fullText ?: ""

        }

    }



    /**

     * Authentic Videoke 2-Line Alternating Rolling Queue (Top / Bottom):

     * When Top finishes, it immediately becomes the next upcoming line.

     * When Bottom finishes, it immediately becomes the next upcoming line.

     */

    private fun computeRollingLyrics2(lines: List<KaraokeLine>, currIdx: Int): Pair<String, String> {

        val activeSlot = currIdx % 2

        val land1 = lines.getOrNull(if (activeSlot == 0) currIdx else currIdx + 1)?.fullText ?: ""

        val land2 = lines.getOrNull(if (activeSlot == 1) currIdx else currIdx + 1)?.fullText ?: ""

        return Pair(land1, land2)

    }



    fun onSongCompleted() {

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



        val showModal = _uiState.value.scoreMode != ScoreMode.OFF

        val finalScore = when (_uiState.value.scoreMode) {

            ScoreMode.OFF -> 0

            ScoreMode.RANDOM -> {

                when (_uiState.value.scoringDifficulty) {

                    ScoringDifficulty.EASY -> (88..98).random()

                    ScoringDifficulty.MEDIUM -> (78..95).random()

                    ScoringDifficulty.HARD -> (65..90).random()

                }

            }

            ScoreMode.MICROPHONE -> {

                micScorer.calculateFinalScore(_uiState.value.scoringDifficulty.name)

            }

        }



        val fanfareVol = _uiState.value.fanfareSoundLevel.volume

        if (showModal && fanfareVol > 0.01f) {

            playScoringFanfareAudio(finalScore)

            voiceManager.speakScoreFeedback(finalScore, _uiState.value.appLanguage == AppLanguage.TAGALOG)

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
            try {
                if (showModal) {
                    delay(2800)
                    try { closeScoreModal() } catch (_: Throwable) {}
                } else {
                    delay(300)
                }
                onNext()
            } catch (e: Throwable) {
                android.util.Log.e("VideoKeyViewModel", "Error in onSongCompleted coroutine", e)
            }
        }

    }



    private var digitInactivityJob: kotlinx.coroutines.Job? = null



    private fun resetDigitInactivityTimer() {

        digitInactivityJob?.cancel()

        digitInactivityJob = viewModelScope.launch {

            kotlinx.coroutines.delay(5000L)

            _uiState.update {

                it.copy(

                    enteredCode = "",

                    matchedEnteredSong = null

                )

            }

        }

    }



    private fun cancelDigitInactivityTimer() {

        digitInactivityJob?.cancel()

        digitInactivityJob = null

    }



    fun onDigitPressed(digit: Char) {

        if (_uiState.value.isKeypadVoiceEnabled && digit in '0'..'9' && !_uiState.value.isPlaying) {

            voiceManager.speakDigit(digit)

        }

        resetDigitInactivityTimer()

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



    fun speakDigit(digit: Char) {

        if (_uiState.value.isKeypadVoiceEnabled && digit in '0'..'9' && !_uiState.value.isPlaying) {

            voiceManager.speakDigit(digit)

        }

    }



    fun onClearCode() {

        cancelDigitInactivityTimer()

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

            if (newCode.isEmpty()) {

                cancelDigitInactivityTimer()

            } else {

                resetDigitInactivityTimer()

            }

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

        cancelDigitInactivityTimer()

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

        cancelDigitInactivityTimer()

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



    fun setVocalSyncOffsetMs(offsetMs: Int) {

        val clamped = offsetMs.coerceIn(0, 600)

        karaokeRecorder.vocalSyncOffsetMs = clamped

        prefs.edit().putInt("vocal_sync_offset_ms", clamped).apply()

        _uiState.update { it.copy(vocalSyncOffsetMs = clamped) }

    }



    fun adjustVocalSyncOffset(deltaMs: Int) {

        setVocalSyncOffsetMs(_uiState.value.vocalSyncOffsetMs + deltaMs)

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



    fun selectBottomNavTab(tab: Int) {

        // Tuloy-tuloy ang pag-awit: Hindi nag-a-autopause kapag lumipat ng tab,
        // sa halip ay mananatiling live ang Floating Lyrics screen!
        _uiState.update { state ->

            if (tab == 0 && state.songbookQuery.isBlank()) {

                val cat = state.selectedCategoryFilter

                val allSongs = repository.getAllSongs()

                val catSongs = if (cat == "ALL") allSongs else allSongs.filter { it.matchesCategory(cat) }

                state.copy(

                    selectedBottomNavTab = tab,

                    trendingSongs = catSongs

                )

            } else {

                state.copy(selectedBottomNavTab = tab)

            }

        }

    }



    fun selectCategoryFilter(category: String) {

        // 1. Immediately highlight the selected tab on UI for instant response

        _uiState.update { it.copy(selectedCategoryFilter = category) }



        // 2. Compute/retrieve filtered and trending lists asynchronously using memory cache

        categoryJob?.cancel()

        categoryJob = viewModelScope.launch(Dispatchers.Default) {

            val allSongs = repository.getAllSongs()

            val catKey = category.uppercase()

            val catMatching = if (catKey == "ALL") {

                allSongs

            } else {

                categoryCache.getOrPut(catKey) {

                    allSongs.filter { it.matchesCategory(category) }

                }

            }

            val query = _uiState.value.songbookQuery

            val onlyPlayable = _uiState.value.onlyShowPlayableSongs

            val playables = soundEngine.getPlayableCodes()

            val filtered = if (query.isNotBlank() || onlyPlayable) {

                val base = repository.search(query, onlyPlayable) { playables.contains(it) }

                if (catKey == "ALL") base else base.filter { it.matchesCategory(category) }

            } else {

                catMatching

            }

            val shuffledTrending = catMatching.shuffled().take(60)



            _uiState.update { state ->

                if (state.selectedCategoryFilter == category) {

                    state.copy(

                        searchResults = filtered,

                        trendingSongs = shuffledTrending

                    )

                } else state

            }

        }

    }





    fun onPlay() {
        try {
            cancelDigitInactivityTimer()
            val code = _uiState.value.enteredCode
            if (code.isNotBlank()) {
                val padded = code.padStart(6, '0')
                val song = _uiState.value.matchedEnteredSong ?: repository.findByCode(padded) ?: repository.findByCode(code) ?: Song(padded, "Track $padded", "Videoke Song", "")
                _uiState.update { it.copy(enteredCode = "", matchedEnteredSong = null) }
                playSong(song)
            } else if (_uiState.value.isPlaying) {
                // When already playing and no new code is entered, alternate between PAUSE and RESUME on the same button!
                togglePlaybackPause()
            } else if (_uiState.value.reserveQueue.isNotEmpty()) {
                val nextSong = _uiState.value.reserveQueue.first()
                _uiState.update { it.copy(reserveQueue = it.reserveQueue.drop(1)) }
                playSong(nextSong)
            } else {
                playSong(_uiState.value.currentSong)
            }
        } catch (e: Throwable) {
            android.util.Log.e("VideoKeyViewModel", "Error in onPlay()", e)
        }
    }



    fun onNext() {
        try {
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
                try { soundEngine.stop() } catch (_: Throwable) {}
                _uiState.update {
                    it.copy(
                        isPlaying = false,
                        isVideoSong = false,
                        enteredCode = "",
                        matchedEnteredSong = null,
                        wipePercent = 0f,
                        countdownDots = 0
                    )
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("VideoKeyViewModel", "Error in onNext()", e)
        }
    }



    fun onStop() {
        try {
            if (_uiState.value.isRecording || karaokeRecorder.isRecording) {
                stopAndSaveRecording()
            }
            val queue = _uiState.value.reserveQueue
            if (queue.isNotEmpty()) {
                onNext()
            } else {
                try { soundEngine.stop() } catch (e: Throwable) { android.util.Log.e("VideoKeyViewModel", "Error stopping sound engine", e) }
                try { micScorer.stopScoring() } catch (_: Throwable) {}
                try { stopScoringFanfareAudio() } catch (_: Throwable) {}
                playbackJob?.cancel()
                _uiState.update {
                    it.copy(
                        isPlaying = false,
                        isPlaybackPaused = false,
                        isVideoSong = false,
                        isPlayingYouTubeStream = false,
                        activeYouTubeStreamVideoId = null,
                        activeYouTubeStreamTitle = null,
                        wipePercent = 0f,
                        countdownDots = 0,
                        showSongTitleIntro = false,
                        isInstrumental = false
                    )
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("VideoKeyViewModel", "Error in onStop()", e)
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



    fun toggleMelodyMute() {

        val target = !_uiState.value.isMelodyMuted

        setMelodyMuted(target)

    }



    fun setMelodyMuted(muted: Boolean) {

        soundEngine.setMelodyMuted(muted)

        prefs.edit().putBoolean("is_melody_muted", muted).apply()

        val isTl = _uiState.value.appLanguage == AppLanguage.TAGALOG

        _uiState.update {

            it.copy(

                isMelodyMuted = muted,

                alertMessage = if (muted) {

                    if (isTl) "🔇 Naka-OFF ang Gabay na Melody (Mawawala ang tono sa kanta)" else "🔇 Melody Guide turned OFF (Minus-One Mode)"

                } else {

                    if (isTl) "🎵 Naka-ON ang Gabay na Melody" else "🎵 Melody Guide turned ON"

                }

            )

        }

    }



    fun requestStartScreenRecording() {

        com.example.videokey.MainActivity.instance?.requestScreenRecording()

    }



    fun onScreenRecordingStarted() {

        _uiState.update {

            val isTl = it.appLanguage == AppLanguage.TAGALOG

            it.copy(

                isScreenRecording = true,

                screenRecordingSeconds = 0,

                alertMessage = if (isTl) "🔴 Nagsimula na ang Screen Recording kasama ang Sound!" else "🔴 Screen Recording with Sound Started!"

            )

        }

    }



    fun onScreenRecordingFailed() {

        _uiState.update {

            val isTl = it.appLanguage == AppLanguage.TAGALOG

            it.copy(

                isScreenRecording = false,

                alertMessage = if (isTl) "Hindi natuloy ang Screen Recording (Kailangan ang pahintulot)." else "Screen Recording cancelled or permission denied."

            )

        }

    }



    fun stopScreenRecording() {

        com.example.videokey.MainActivity.instance?.stopScreenRecordingService()

        val savedFile = com.example.videokey.data.ScreenCaptureService.lastRecordedFile

        _uiState.update {

            val isTl = it.appLanguage == AppLanguage.TAGALOG

            val name = savedFile?.name ?: "VideoKey_Screen.mp4"

            it.copy(

                isScreenRecording = false,

                screenRecordingSeconds = 0,

                savedRecordings = karaokeRecorder.listSavedRecordings(),

                alertMessage = if (isTl) "🎥 Matagumpay na na-save ang Screen Record ($name) sa VideoKey/recordings!" else "🎥 Screen Recording saved ($name) to VideoKey/recordings!"

            )

        }

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

            .putInt("bass_boost_percent", 0)

            .putInt("treble_percent", 0)

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

                reverbPercent = 50,

                bassBoostPercent = 0,

                treblePercent = 0

            )

        }

    }



    fun setAutoVolumeEnabled(enabled: Boolean) {

        soundEngine.setAutoVolumeEnabled(enabled)

        prefs.edit().putBoolean("auto_volume_enabled", enabled).apply()

        _uiState.update { it.copy(isAutoVolumeEnabled = enabled) }

    }



    fun toggleAutoVolume(enabled: Boolean? = null) {

        val target = enabled ?: !_uiState.value.isAutoVolumeEnabled

        setAutoVolumeEnabled(target)

    }



    fun setAutoMelodyNormalizeEnabled(enabled: Boolean) {

        soundEngine.setAutoMelodyNormalizeEnabled(enabled)

        prefs.edit().putBoolean("auto_melody_normalize_enabled", enabled).apply()

        _uiState.update { it.copy(isAutoMelodyNormalizeEnabled = enabled) }

    }



    fun toggleAutoMelodyNormalize(enabled: Boolean? = null) {

        val target = enabled ?: !_uiState.value.isAutoMelodyNormalizeEnabled

        setAutoMelodyNormalizeEnabled(target)

    }



    fun adjustBassBoost(delta: Int) {

        if (!_uiState.value.isVipActivated) {

            val isTl = _uiState.value.appLanguage == AppLanguage.TAGALOG

            _uiState.update {

                it.copy(alertMessage = if (isTl) "⭐ Eksklusibo sa VIP ang Bass Boost! I-activate ang VIP para ma-unlock." else "⭐ Bass Boost is a VIP exclusive feature! Activate VIP to unlock.")

            }

            return

        }

        setBassBoost(_uiState.value.bassBoostPercent + delta)

    }



    fun setBassBoost(percent: Int) {

        if (!_uiState.value.isVipActivated) {

            val isTl = _uiState.value.appLanguage == AppLanguage.TAGALOG

            _uiState.update {

                it.copy(alertMessage = if (isTl) "⭐ Eksklusibo sa VIP ang Bass Boost! I-activate ang VIP para ma-unlock." else "⭐ Bass Boost is a VIP exclusive feature! Activate VIP to unlock.")

            }

            return

        }

        val clamped = percent.coerceIn(0, 100)

        soundEngine.setBassBoost(clamped)

        prefs.edit().putInt("bass_boost_percent", clamped).apply()

        _uiState.update { it.copy(bassBoostPercent = clamped) }

    }



    fun adjustTreble(delta: Int) {

        if (!_uiState.value.isVipActivated) {

            val isTl = _uiState.value.appLanguage == AppLanguage.TAGALOG

            _uiState.update {

                it.copy(alertMessage = if (isTl) "⭐ Eksklusibo sa VIP ang Treble Boost! I-activate ang VIP para ma-unlock." else "⭐ Treble Boost is a VIP exclusive feature! Activate VIP to unlock.")

            }

            return

        }

        setTreble(_uiState.value.treblePercent + delta)

    }



    fun setTreble(percent: Int) {

        if (!_uiState.value.isVipActivated) {

            val isTl = _uiState.value.appLanguage == AppLanguage.TAGALOG

            _uiState.update {

                it.copy(alertMessage = if (isTl) "⭐ Eksklusibo sa VIP ang Treble Boost! I-activate ang VIP para ma-unlock." else "⭐ Treble Boost is a VIP exclusive feature! Activate VIP to unlock.")

            }

            return

        }

        val clamped = percent.coerceIn(0, 100)

        soundEngine.setTreble(clamped)

        prefs.edit().putInt("treble_percent", clamped).apply()

        _uiState.update { it.copy(treblePercent = clamped) }

    }



    fun toggleAutoMixer(enabled: Boolean) {

        if (!_uiState.value.isVipActivated) {

            val isTl = _uiState.value.appLanguage == AppLanguage.TAGALOG

            _uiState.update {

                it.copy(alertMessage = if (isTl) "⭐ Eksklusibo sa VIP ang Smart Auto-Mix! I-activate ang VIP para ma-unlock." else "⭐ Smart Auto-Mix is a VIP exclusive feature! Activate VIP to unlock.")

            }

            return

        }

        soundEngine.setAutoMixerEnabled(enabled)

        prefs.edit().putBoolean("auto_mixer_enabled", enabled).apply()

        val autoStyle = soundEngine.getDetectedMixStyle()

        if (enabled) {

            soundEngine.applyMixStyle(autoStyle)

            _uiState.update {

                it.copy(

                    isAutoMixerEnabled = true,

                    activeMixStyle = autoStyle,

                    melodyPercent = soundEngine.getMelodyVolume(),

                    drumsPercent = soundEngine.getDrumsVolume(),

                    bassPercent = soundEngine.getBassVolume(),

                    guitarPianoPercent = soundEngine.getGuitarPianoVolume(),

                    stringsBrassPercent = soundEngine.getStringsBrassVolume(),

                    reverbPercent = soundEngine.getReverbVolume()

                )

            }

        } else {

            val manualMelody = prefs.getInt("melody_percent", 60)

            val manualDrums = prefs.getInt("drums_percent", 100)

            val manualBass = prefs.getInt("bass_percent", 100)

            val manualGuitarPiano = prefs.getInt("guitar_piano_percent", 100)

            val manualStringsBrass = prefs.getInt("strings_brass_percent", 100)

            val manualReverb = prefs.getInt("reverb_percent", 50)

            soundEngine.setMelodyVolume(manualMelody)

            soundEngine.setDrumsVolume(manualDrums)

            soundEngine.setBassVolume(manualBass)

            soundEngine.setGuitarPianoVolume(manualGuitarPiano)

            soundEngine.setStringsBrassVolume(manualStringsBrass)

            soundEngine.setReverbVolume(manualReverb)

            _uiState.update {

                it.copy(

                    isAutoMixerEnabled = false,

                    melodyPercent = manualMelody,

                    drumsPercent = manualDrums,

                    bassPercent = manualBass,

                    guitarPianoPercent = manualGuitarPiano,

                    stringsBrassPercent = manualStringsBrass,

                    reverbPercent = manualReverb

                )

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

        _uiState.update {

            it.copy(

                songbookQuery = query,

                onlyShowPlayableSongs = onlyPlayable

            )

        }

        searchJob?.cancel()

        searchJob = viewModelScope.launch(Dispatchers.Default) {

            val playables = soundEngine.getPlayableCodes()

            val baseResults = repository.search(query, onlyPlayable) { playables.contains(it) }

            val cat = _uiState.value.selectedCategoryFilter

            val results = if (cat == "ALL") baseResults else baseResults.filter { it.matchesCategory(cat) }

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

        stopScoringFanfareAudio()

        _uiState.update { it.copy(showScoreModal = false) }

    }



    var isRunningOnTvDevice: Boolean = false

        private set



    fun initAndroidTvMode() {

        isRunningOnTvDevice = true

        _uiState.update { it.copy(isTvMode = true) }

        openSongbook()

        val hasCustomized = prefs.getBoolean("has_customized_tv_lyrics_v4", false)

        if (!hasCustomized) {

            applyTvDefaultLyricSettings()

        } else if (Math.abs(_uiState.value.lyricConfig.fontScale - 0.85f) < 0.01f) {

            setLyricFontScale(1.0f)

        }

    }



    fun applyTvDefaultLyricSettings() {

        val tvConfig = LyricCustomization(

            fontFamily = LyricFontFamily.GEOMETRIC_POP,

            wipeColor = LyricWipeColor.DARK_BLUE,

            inactiveColor = 0xFFFFFFFF,

            outlineColor = LyricOutlineColor.NAVY,

            shadowStyle = LyricShadowStyle.CLEAN,

            outlineThickness = 3.0f,

            fontSizePortraitSp = 36.0f,

            fontSizeLandscapeSp = 54.0f,

            lineSpacingDp = 8.0f,

            hasDropShadow = true,

            isAllCaps = false,

            isItalic = false,

            fontScale = 1.0f, // Normal 100% font scale

            lineCount = LyricLineCount.TWO_LINES,

            position = LyricScreenPosition.BOTTOM,

            countdownMode = LyricCountdownMode.CLASSIC_DOTS,

            timingOffsetMs = 0

        )

        saveFullLyricConfig(tvConfig)

        _uiState.update { it.copy(lyricConfig = tvConfig) }

    }



    fun toggleTvMode(activity: android.app.Activity? = null) {

        val nextTv = !_uiState.value.isTvMode

        _uiState.update { it.copy(isTvMode = nextTv) }

        if (nextTv) {

            openSongbook()

            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

            val hasCustomized = prefs.getBoolean("has_customized_tv_lyrics_v4", false)

            if (!hasCustomized) {

                applyTvDefaultLyricSettings()

            } else if (Math.abs(_uiState.value.lyricConfig.fontScale - 0.85f) < 0.01f) {

                setLyricFontScale(1.0f)

            }

        } else {

            if (!isRunningOnTvDevice) {

                activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

            }

        }

    }



    fun enterTvLandscapeMode(activity: android.app.Activity? = null) {

        _uiState.update { it.copy(isTvMode = true) }

        openSongbook()

        if (!isRunningOnTvDevice) {

            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        }

        val hasCustomized = prefs.getBoolean("has_customized_tv_lyrics_v4", false)

        if (!hasCustomized) {

            applyTvDefaultLyricSettings()

        } else if (Math.abs(_uiState.value.lyricConfig.fontScale - 0.85f) < 0.01f) {

            setLyricFontScale(1.0f)

        }

    }



    fun exitLandscapeMode(activity: android.app.Activity? = null) {

        if (isRunningOnTvDevice) {

            // Android TV is physically landscape only. Do not force portrait.

            return

        }

        _uiState.update { it.copy(isTvMode = false) }

        activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

    }



    @Volatile private var wasPlayingBeforePause = false

    @Volatile private var wasAutoPausedByScreenChange = false



    fun onAppPause() {
        // Tuloy-tuloy ang pag-awit: Hindi nagpo-pause kapag natakpan ang screen,
        // mananatiling tumutugtog nang live sa Picture-in-Picture at background!
    }




    fun setPictureInPictureMode(inPip: Boolean) {
        _uiState.update { it.copy(isPictureInPicture = inPip) }
    }

    fun onAppResume() {

        if (!_uiState.value.isVipActivated) {

            checkOnlineVipStatus(showMessage = false)

        }

        if (wasPlayingBeforePause && _uiState.value.isPlaying) {

            wasPlayingBeforePause = false

            resumePlayback()

        }

    }



    fun pausePlayback() {

        if (_uiState.value.isPlaying && !_uiState.value.isPlaybackPaused) {

            soundEngine.pause()

            _uiState.update { it.copy(isPlaybackPaused = true) }

        }

    }



    fun resumePlayback() {

        if (_uiState.value.isPlaying && _uiState.value.isPlaybackPaused) {

            soundEngine.resume()

            _uiState.update { it.copy(isPlaybackPaused = false) }

        }

    }



    fun togglePlaybackPause() {

        if (_uiState.value.isPlaybackPaused) {

            resumePlayback()

        } else {

            pausePlayback()

        }

    }



    fun openSoundFontDialog() {

        _uiState.update { it.copy(isSoundFontDialogOpen = true) }

    }



    fun closeSoundFontDialog() {

        _uiState.update { it.copy(isSoundFontDialogOpen = false) }

    }



    fun deleteSoundFont(sfName: String) {

        val isTl = _uiState.value.appLanguage == AppLanguage.TAGALOG

        val isAuto = sfName.contains("auto", ignoreCase = true) || sfName.contains("awtomatiko", ignoreCase = true)

        if (sfName.startsWith("[VIP Preset]")) {

            val presetTitle = sfName.removePrefix("[VIP Preset]").trim()

            val savedPresets = prefs.getStringSet("vip_soundfont_presets", emptySet())?.toMutableSet() ?: mutableSetOf()

            savedPresets.removeAll { it.startsWith("$presetTitle::") }

            prefs.edit().putStringSet("vip_soundfont_presets", savedPresets).apply()

            _uiState.update {

                it.copy(

                    availableSoundFonts = it.availableSoundFonts.filterNot { name -> name.equals(sfName, ignoreCase = true) },

                    alertMessage = if (isTl) "🗑️ Na-delete ang VIP Preset: $presetTitle" else "🗑️ Deleted VIP Preset: $presetTitle"

                )

            }

            return

        }

        val isBuiltIn = isAuto ||
                sfName.contains("VideoKey", ignoreCase = true) ||
                sfName.contains("SAM2634", ignoreCase = true) ||
                sfName.contains("Dream", ignoreCase = true) ||
                sfName.contains("MegaPro", ignoreCase = true) ||
                sfName.contains("Yamaha", ignoreCase = true) ||
                sfName.contains("Minus-One", ignoreCase = true)

        if (isBuiltIn) {

            _uiState.update {

                it.copy(alertMessage = if (isTl) "Hindi maaaring i-delete ang built-in na SoundFont." else "Cannot delete built-in SoundFont.")

            }

            return

        }

        val file = soundEngine.getSoundFontFile(sfName)

        if (file != null && file.exists()) {

            try {

                val deleted = file.delete()

                if (deleted) {

                    soundEngine.clearSoundFontCache()

                    val nextActive = if (_uiState.value.isVipActivated) "✨ Awtomatiko (Depende sa Genre)" else "Dream SAM2634 v2.1"

                    if (_uiState.value.activeSoundFontName.equals(sfName, ignoreCase = true)) {

                        selectSoundFont(nextActive)

                    }

                    _uiState.update {

                        it.copy(

                            availableSoundFonts = it.availableSoundFonts.filterNot { name -> name.equals(sfName, ignoreCase = true) },

                            alertMessage = if (isTl) "🗑️ Na-delete ang SoundFont: $sfName" else "🗑️ Deleted SoundFont: $sfName"

                        )

                    }

                } else {

                    _uiState.update {

                        it.copy(alertMessage = if (isTl) "Hindi ma-delete ang SoundFont file." else "Could not delete SoundFont file.")

                    }

                }

            } catch (e: Exception) {

                _uiState.update {

                    it.copy(alertMessage = if (isTl) "Bumagsak ang pag-delete: ${e.message}" else "Delete failed: ${e.message}")

                }

            }

        } else {

            _uiState.update {

                it.copy(

                    availableSoundFonts = it.availableSoundFonts.filterNot { name -> name.equals(sfName, ignoreCase = true) },

                    alertMessage = if (isTl) "🗑️ Inalis sa listahan: $sfName" else "🗑️ Removed from list: $sfName"

                )

            }

        }

    }



    fun selectSoundFont(name: String) {

        val isAuto = name.contains("auto", ignoreCase = true) || name.contains("awtomatiko", ignoreCase = true)

        val isTl = _uiState.value.appLanguage == AppLanguage.TAGALOG



        if (name.startsWith("[VIP Preset]")) {

            val presetTitle = name.removePrefix("[VIP Preset]").trim()

            val savedPresets = prefs.getStringSet("vip_soundfont_presets", emptySet()) ?: emptySet()

            val match = savedPresets.firstOrNull { it.startsWith("$presetTitle::") }

            if (match != null) {

                val parts = match.substringAfter("::").split(",")

                if (parts.size >= 6) {

                    setDrumsPercent(parts[0].toIntOrNull() ?: 100)

                    setBassPercent(parts[1].toIntOrNull() ?: 100)

                    setGuitarPianoPercent(parts[2].toIntOrNull() ?: 100)

                    setStringsBrassPercent(parts[3].toIntOrNull() ?: 100)

                    setMelodyPercent(parts[4].toIntOrNull() ?: 100)

                    setReverbPercent(parts[5].toIntOrNull() ?: 40)

                }

            }

            prefs.edit().putString("active_soundfont", name).apply()

            _uiState.update {

                it.copy(

                    activeSoundFontName = name,

                    isSoundFontDialogOpen = false,

                    alertMessage = if (isTl) "⭐ Na-apply ang VIP SoundFont Preset: $presetTitle" else "⭐ Applied VIP Preset: $presetTitle"

                )

            }

            return

        }

        if (isAuto && !_uiState.value.isVipActivated) {

            _uiState.update {

                it.copy(

                    isSoundFontDialogOpen = false,

                    alertMessage = if (isTl) "👑 Ang Awtomatikong SoundFont ay para lamang sa mga VIP! I-activate ang VIP para ma-unlock."

                                   else "👑 Auto SoundFont is exclusive to VIP members! Activate VIP to unlock."

                )

            }

            openYouTubeDownloader(null)

            return

        }

        prefs.edit().putString("active_soundfont", name).apply()

        if (isAuto) {

            val currSong = _uiState.value.currentSong

            val matched = soundEngine.resolveGenreForSong(currSong, null)

            soundEngine.setActiveSoundFont(matched.id)

            _uiState.update {

                it.copy(

                    activeSoundFontName = name,

                    autoMatchedSoundFontForCurrentSong = matched.displayName,

                    isSoundFontDialogOpen = false,

                    alertMessage = if (isTl) "✨ Naka-on ang Awtomatikong SoundFont (Depende sa Genre)" else "✨ Auto SoundFont enabled (Adapts to song genre)"

                )

            }

        } else {

            soundEngine.setActiveSoundFont(name)

            val resolvedName = soundEngine.getActiveSoundFont().displayName

            _uiState.update {

                it.copy(

                    activeSoundFontName = resolvedName,

                    isSoundFontDialogOpen = false,

                    alertMessage = if (isTl) "🎛️ Na-apply ang SoundFont: $resolvedName" else "🎛️ Applied SoundFont: $resolvedName"

                )

            }

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

        val catalog = contentDownloadManager.getAvailableCatalog(_uiState.value.isVipActivated)

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



    fun isContentItemInstalled(item: com.example.videokey.data.ContentDownloadManager.DownloadItem): Boolean {

        return contentDownloadManager.isItemInstalled(item)

    }



    private var autoDownloadJob: kotlinx.coroutines.Job? = null



    fun checkAndAutoDetectFiles(triggerAutoDownloadIfMissing: Boolean = true) {

        viewModelScope.launch(Dispatchers.IO) {

            // Auto remove legacy or dummy files (e.g. obsolete songhub.sf2, corrupt <50MB packs, stubs)

            contentDownloadManager.cleanLegacyOrDummyFiles()



            // Automatically hide exposed song packs from public external storage into private internal storage

            contentDownloadManager.hideExposedSongPacks()

            if (_uiState.value.isVipActivated && (contentDownloadManager.has57kSongsInstalled() || contentDownloadManager.has54kSongsInstalled())) {

                contentDownloadManager.deleteOld47kSongPack()

            }



            // 1. Resolve storage directories if accessible

            val vkRoot = when {

                File("/storage/emulated/0/VideoKey").exists() -> File("/storage/emulated/0/VideoKey")

                File("/sdcard/VideoKey").exists() -> File("/sdcard/VideoKey")

                else -> null

            }



            if (vkRoot != null) {

                val songsSub = File(vkRoot, "songs")

                val songsPath = if (songsSub.exists()) songsSub.absolutePath else vkRoot.absolutePath

                val bgSub = File(vkRoot, "backgrounds")

                val bgVideosSub = File(vkRoot, "videos")

                val bgPath = when {

                    bgSub.exists() -> bgSub.absolutePath

                    bgVideosSub.exists() -> bgVideosSub.absolutePath

                    else -> vkRoot.absolutePath

                }



                val currentSongF = _uiState.value.currentSongFolder

                if (currentSongF == "/storage/emulated/0" || !File(currentSongF).exists() || currentSongF == vkRoot.absolutePath) {

                    prefs.edit().putString("song_folder", songsPath).apply()

                    _uiState.update { it.copy(currentSongFolder = songsPath) }

                }

                val currentBgF = _uiState.value.currentBackgroundFolder

                if (currentBgF == "/storage/emulated/0" || !File(currentBgF).exists() || currentBgF == vkRoot.absolutePath) {

                    prefs.edit().putString("background_folder", bgPath).apply()

                    _uiState.update { it.copy(currentBackgroundFolder = bgPath) }

                }

            }



            // 2. Perform live check of all required files

            val hasSf = contentDownloadManager.isSoundFontInstalled()

            val hasSongs = contentDownloadManager.hasAnySongsInstalled()

            val hasBg = contentDownloadManager.isBackgroundVideoInstalled()

            val allFilesExist = hasSf && hasSongs && hasBg



            refreshDownloadCatalog()



            if (allFilesExist) {

                // All 3 required components exist! Auto-pass the gate and index library

                prefs.edit().putBoolean("is_initial_setup_completed", true).apply()

                _uiState.update { it.copy(isStartupGatePassed = true) }

                scanSongFolder(File(_uiState.value.currentSongFolder))

                scanBackgroundMedia(File(_uiState.value.currentBackgroundFolder))

            } else {

                // Files are missing! Keep gate locked and trigger autonomous download

                _uiState.update { it.copy(isStartupGatePassed = false) }

                if (triggerAutoDownloadIfMissing && autoDownloadJob?.isActive != true) {

                    startSequentialAutoDownload()

                }

            }

        }

    }



    fun startSequentialAutoDownload() {

        if (autoDownloadJob?.isActive == true) return

        autoDownloadJob = viewModelScope.launch(Dispatchers.IO) {

            kotlinx.coroutines.delay(400)

            val catalog = contentDownloadManager.getAvailableCatalog(_uiState.value.isVipActivated)

            val sfItem = catalog.find { it.id == "all_soundfonts" }

            val songsItem = catalog.find { it.id == "vip_songs_57k" || it.id == "vip_songs_54k" } ?: contentDownloadManager.vipSongs57kItem

            val bgItem = catalog.find { it.id == "all_backgrounds" }



            // 1. Check & download SoundFont if missing

            if (!contentDownloadManager.isSoundFontInstalled() && sfItem != null) {

                val deferred = kotlinx.coroutines.CompletableDeferred<Boolean>()

                contentDownloadManager.downloadItem(sfItem) { success ->

                    deferred.complete(success)

                }

                deferred.await()

                scanSongFolder(File(_uiState.value.currentSongFolder))

                refreshDownloadCatalog()

            }



            // 2. Check & download Songs if missing

            val songsExist = if (_uiState.value.isVipActivated) {

                contentDownloadManager.has57kSongsInstalled()

            } else {

                contentDownloadManager.hasAnySongsInstalled()

            }

            if (!songsExist && songsItem != null) {

                val deferred = kotlinx.coroutines.CompletableDeferred<Boolean>()

                contentDownloadManager.downloadItem(songsItem) { success ->

                    deferred.complete(success)

                }

                deferred.await()

                if (_uiState.value.isVipActivated) {

                    contentDownloadManager.deleteOld47kSongPack()

                }

                scanSongFolder(File(_uiState.value.currentSongFolder))

                refreshDownloadCatalog()

            }



            // 3. Check & download Background Video if missing

            if (!contentDownloadManager.isBackgroundVideoInstalled() && bgItem != null) {

                val deferred = kotlinx.coroutines.CompletableDeferred<Boolean>()

                contentDownloadManager.downloadItem(bgItem) { success ->

                    deferred.complete(success)

                }

                deferred.await()

                scanBackgroundMedia(File(_uiState.value.currentBackgroundFolder))

                refreshDownloadCatalog()

            }



            // Final check: if all 3 are now fully installed, pass startup gate automatically!

            val allReady = contentDownloadManager.isSoundFontInstalled() &&

                    contentDownloadManager.hasAnySongsInstalled() &&

                    contentDownloadManager.isBackgroundVideoInstalled()



            if (allReady) {

                prefs.edit().putBoolean("is_initial_setup_completed", true).apply()

                _uiState.update { it.copy(isStartupGatePassed = true) }

                scanInternalStorage(getApplication())

                scanBackgroundMedia(File(_uiState.value.currentBackgroundFolder))

            }

        }

    }



    fun cancelContentDownload() {

        autoDownloadJob?.cancel()

        contentDownloadManager.cancelCurrentDownload()

    }



    fun passStartupGate() {

        val allReady = contentDownloadManager.isSoundFontInstalled() &&

                contentDownloadManager.hasAnySongsInstalled() &&

                contentDownloadManager.isBackgroundVideoInstalled()

        if (allReady) {

            prefs.edit().putBoolean("is_initial_setup_completed", true).apply()

            _uiState.update { it.copy(isStartupGatePassed = true, isDownloadCenterOpen = false) }

            viewModelScope.launch {

                scanInternalStorage(getApplication())

            }

        } else {

            // Strictly cannot open app if files are missing!

            checkAndAutoDetectFiles(triggerAutoDownloadIfMissing = true)

        }

    }



    fun refreshDownloadedMp4Songs() {

        val songsFolder = youTubeDownloader.getSongsFolder()

        val diskMp4Files = songsFolder.listFiles { f ->

            f.isFile && (f.extension.equals("mp4", true) || f.extension.equals("mkv", true)) &&

            !f.absolutePath.contains("/backgrounds/", ignoreCase = true) &&

            !f.absolutePath.contains("\\backgrounds\\", ignoreCase = true)

        } ?: emptyArray()



        val currentAll = repository.getAllSongs()

        val scanner = com.example.videokey.data.StorageScanner(getApplication())

        diskMp4Files.forEach { file ->

            if (!currentAll.any { it.filePath == file.absolutePath }) {

                val parsed = scanner.parseSongFilename(file)

                repository.addSong(parsed)

            }

        }



        val mp4s = repository.getAllSongs().filter {

            (it.filePath.endsWith(".mp4", ignoreCase = true) || it.filePath.endsWith(".mkv", ignoreCase = true)) &&

            !it.filePath.contains("/backgrounds/", ignoreCase = true) &&

            !it.filePath.contains("\\backgrounds\\", ignoreCase = true)

        }.sortedByDescending { it.code }



        _uiState.update { it.copy(downloadedMp4Songs = mp4s) }

    }



    fun deleteDownloadedMp4Song(song: Song) {

        try {

            val file = File(song.filePath)

            if (file.exists()) {

                file.delete()

            }

        } catch (e: Exception) {

            Log.e("VideoKeyVM", "Failed to delete MP4: ${e.message}")

        }

        repository.removeSong(song)

        val updatedSongs = repository.getAllSongs()

        val mp4s = updatedSongs.filter {

            it.filePath.endsWith(".mp4", ignoreCase = true) || it.filePath.endsWith(".mkv", ignoreCase = true)

        }.sortedByDescending { it.code }



        _uiState.update {

            it.copy(

                searchResults = updatedSongs,

                trendingSongs = updatedSongs,

                downloadedMp4Songs = mp4s

            )

        }

    }



    fun openYouTubeDownloader(prefilledQuery: String? = null) {

        refreshDownloadedMp4Songs()

        val cleanQuery = prefilledQuery?.trim().orEmpty()

        _uiState.update {

            it.copy(

                isYouTubeDownloaderOpen = true,

                youTubeSearchQuery = if (cleanQuery.isNotEmpty()) cleanQuery else it.youTubeSearchQuery

            )

        }

        if (cleanQuery.isNotEmpty()) {

            searchYouTube(cleanQuery)

        }

    }



    fun closeYouTubeDownloader() {

        _uiState.update { it.copy(isYouTubeDownloaderOpen = false, youTubeDownloadProgress = null, vipActivationError = null) }

        youTubeDownloader.cancelDownload()

    }



    fun setVipActivated(active: Boolean) {

        prefs.edit().putBoolean("is_vip_activated", active).apply()

        val devId = _uiState.value.deviceId.ifBlank { SupabaseVipManager.getHardwareDeviceId(getApplication()) }

        if (active) {

            val key = VipSecureStorage.getStoredLicenseKey(getApplication()) ?: VipLicenseManager.generateLicenseKey(devId)

            VipSecureStorage.saveVipStatus(getApplication(), devId, key, true)

            val autoSf = "✨ Awtomatiko (Depende sa Genre)"

            prefs.edit().putString("active_soundfont", autoSf).apply()

            val matched = soundEngine.resolveGenreForSong(_uiState.value.currentSong, null)

            soundEngine.setActiveSoundFont(matched.id)

            _uiState.update {

                it.copy(

                    activeSoundFontName = autoSf,

                    autoMatchedSoundFontForCurrentSong = matched.displayName

                )

            }

        } else {

            VipSecureStorage.clearVip(getApplication())

            VipSecureStorage.clearPendingCheckout(getApplication())

            val defaultNonVipSf = "Dream SAM2634 v2.1"

            prefs.edit().putString("active_soundfont", defaultNonVipSf).apply()

            prefs.edit().putString("vip_button_texture", VipButtonTexture.DEFAULT.name).apply()

            soundEngine.setActiveSoundFont("SAM2634")

            _uiState.update {

                it.copy(

                    activeSoundFontName = defaultNonVipSf,

                    autoMatchedSoundFontForCurrentSong = defaultNonVipSf

                )

            }

            viewModelScope.launch(Dispatchers.IO) {

                try {

                    val deleteUrl = java.net.URL("${SupabaseVipManager.SUPABASE_URL}/rest/v1/vip_licenses?device_id=eq.${android.net.Uri.encode(devId)}")

                    val conn = deleteUrl.openConnection() as java.net.HttpURLConnection

                    conn.requestMethod = "DELETE"

                    val activeKey = if (SupabaseVipManager.SUPABASE_SERVICE_KEY.isNotBlank()) SupabaseVipManager.SUPABASE_SERVICE_KEY else SupabaseVipManager.SUPABASE_ANON_KEY

                    conn.setRequestProperty("apikey", activeKey)

                    conn.setRequestProperty("Authorization", "Bearer $activeKey")

                    conn.responseCode

                } catch (_: Exception) {}

            }

        }

        val savedBass = if (active) prefs.getInt("bass_boost_percent", 0) else 0

        val savedTreb = if (active) prefs.getInt("treble_percent", 0) else 0

        val savedMixer = if (active) prefs.getBoolean("auto_mixer_enabled", true) else false

        soundEngine.setBassBoost(savedBass)

        soundEngine.setTreble(savedTreb)

        soundEngine.setAutoMixerEnabled(savedMixer)

        _uiState.update {

            it.copy(

                isVipActivated = active,

                bassBoostPercent = savedBass,

                treblePercent = savedTreb,

                isAutoMixerEnabled = savedMixer

            )

        }

    }



    fun checkAndTriggerVip54kDownload() {

        if (!_uiState.value.isVipActivated) return

        if (contentDownloadManager.has57kSongsInstalled()) {

            viewModelScope.launch(Dispatchers.IO) {

                contentDownloadManager.deleteOld47kSongPack()

                scanSongFolder(File(_uiState.value.currentSongFolder))

            }

            return

        }



        viewModelScope.launch {

            val isTl = _uiState.value.appLanguage == AppLanguage.TAGALOG

            val vipItem = contentDownloadManager.vipSongs57kItem

            _uiState.update {

                it.copy(

                    alertMessage = if (isTl) "⭐ VIP Activated: Sinisimulan ang kusang pag-download ng 57k VIP Songs Pack..."

                                   else "⭐ VIP Activated: Starting automatic download of 57k VIP Songs Pack..."

                )

            }

            contentDownloadManager.downloadItem(vipItem) { success ->

                if (success) {

                    viewModelScope.launch(Dispatchers.IO) {

                        contentDownloadManager.deleteOld47kSongPack()

                        scanSongFolder(File(_uiState.value.currentSongFolder))

                        val count = repository.getTotalSongsCount()

                        withContext(Dispatchers.Main) {

                            _uiState.update {

                                it.copy(

                                    alertMessage = if (isTl) "⭐ 57k VIP Songs matagumpay na na-install ($count kanta)! Nabura na ang lumang 47k pack."

                                                   else "⭐ 57k VIP Songs successfully installed ($count songs)! Old 47k pack removed."

                                )

                            }

                        }

                    }

                }

            }

        }

    }



    fun activateVip(enteredKey: String): Boolean {

        if (enteredKey.equals("DEACTIVATE", ignoreCase = true) || enteredKey.equals("RESET", ignoreCase = true)) {

            setVipActivated(false)

            _uiState.update { it.copy(vipActivationError = null, alertMessage = "VIP Deactivated") }

            return true

        }

        val devId = _uiState.value.deviceId.ifBlank { VipLicenseManager.getDeviceId(getApplication()) }

        val isValid = VipLicenseManager.verifyLicenseKey(devId, enteredKey)

        if (isValid) {

            setVipActivated(true)

            viewModelScope.launch(Dispatchers.IO) {

                try {

                    SupabaseVipManager.recordVipLicenseInSupabase(devId, enteredKey, "MANUAL-VIP-KEY")

                } catch (_: Exception) {}

            }

            val isTl = _uiState.value.appLanguage == AppLanguage.TAGALOG

            _uiState.update {

                it.copy(

                    vipActivationError = null,

                    alertMessage = if (isTl) "⭐ Matagumpay na Na-activate ang VIP! Na-unlock na ang 57k VIP Songs, Smart Auto-Mix, Bass Boost & Treble, at Auto SoundFont!"

                                   else "⭐ VIP Activated! Unlocked 54k VIP Songs, Smart Auto-Mix, Bass Boost & Treble, and Auto SoundFont!"

                )

            }

            refreshDownloadCatalog()

            checkAndTriggerVip54kDownload()

            return true

        } else {

            val errMsg = if (_uiState.value.appLanguage == AppLanguage.TAGALOG) {

                "Maling Activation Key! Paki-check ang 8-character code."

            } else {

                "Invalid Activation Key! Please check your 8-character code."

            }

            _uiState.update { it.copy(vipActivationError = errMsg) }

            return false

        }

    }



    fun clearVipActivationError() {

        _uiState.update { it.copy(vipActivationError = null) }

    }



    fun searchYouTube(query: String) {

        _uiState.update { it.copy(youTubeSearchQuery = query, isYouTubeSearching = true) }

        viewModelScope.launch {

            val results = youTubeDownloader.search(query)

            _uiState.update { it.copy(youTubeSearchResults = results, isYouTubeSearching = false) }

        }

    }



    fun downloadYouTubeSong(item: com.example.videokey.data.YouTubeVideoItem) {

        val existingMax = repository.getAllSongs().mapNotNull { it.code.toIntOrNull() }.maxOrNull() ?: 900000

        youTubeDownloader.downloadAsSong(

            item = item,

            existingMaxCode = existingMax,

            onProgress = { prog ->

                _uiState.update { it.copy(youTubeDownloadProgress = prog) }

            },

            onFinished = { newSong ->

                if (newSong != null) {

                    repository.addSong(newSong)

                    val updatedSongs = repository.getAllSongs()

                    val mp4s = updatedSongs.filter {

                        it.filePath.endsWith(".mp4", ignoreCase = true) || it.filePath.endsWith(".mkv", ignoreCase = true)

                    }.sortedByDescending { it.code }

                    _uiState.update {

                        it.copy(

                            searchResults = updatedSongs,

                            trendingSongs = updatedSongs,

                            downloadedMp4Songs = mp4s

                        )

                    }

                }

            }

        )

    }



    fun downloadYouTubeBackground(item: com.example.videokey.data.YouTubeVideoItem) {

        youTubeDownloader.downloadAsBackground(

            item = item,

            onProgress = { prog ->

                _uiState.update { it.copy(youTubeDownloadProgress = prog) }

            },

            onFinished = { file ->

                if (file != null) {

                    scanBackgroundMedia(youTubeDownloader.getBackgroundsFolder())

                }

            }

        )

    }



    fun playYouTubeKaraokeDirect(item: com.example.videokey.data.YouTubeVideoItem) {

        soundEngine.stop()

        _uiState.update {

            it.copy(

                activeYouTubeStreamVideoId = item.id,

                activeYouTubeStreamTitle = item.title,

                isPlayingYouTubeStream = true,

                isPlaying = true,

                isTvMode = true,

                selectedBottomNavTab = 1,

                isYouTubeDownloaderOpen = false,

                showSongTitleIntro = false,

                currentSong = Song(

                    code = "YT-DIRECT",

                    title = item.title,

                    artist = item.uploader,

                    filePath = "https://www.youtube.com/watch?v=${item.id}"

                )

            )

        }

    }



    fun stopYouTubeKaraokeDirect(activity: android.app.Activity? = null) {

        _uiState.update {

            it.copy(

                isPlayingYouTubeStream = false,

                activeYouTubeStreamVideoId = null,

                activeYouTubeStreamTitle = null,

                isPlaying = false,

                isTvMode = false

            )

        }

        activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

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

            val vipPresets = (prefs.getStringSet("vip_soundfont_presets", emptySet()) ?: emptySet()).map {

                "[VIP Preset] " + it.substringBefore("::")

            }

            val sfNames = if (result.soundFonts.isNotEmpty()) {

                val scanned = result.soundFonts.map { it.nameWithoutExtension }

                (defaultSoundFonts + vipPresets + scanned).distinct()

            } else {

                (defaultSoundFonts + vipPresets).distinct()

            }

            val isVipNow = _uiState.value.isVipActivated

            val defaultSfForState = if (isVipNow) "✨ Awtomatiko (Depende sa Genre)" else "Dream SAM2634 v2.1"

            val rawSavedSf = prefs.getString("active_soundfont", defaultSfForState) ?: defaultSfForState

            val resolvedSf = when {

                (rawSavedSf.contains("auto", ignoreCase = true) || rawSavedSf.contains("awtomatiko", ignoreCase = true)) && isVipNow ->

                    "✨ Awtomatiko (Depende sa Genre)"

                sfNames.any { it.equals(rawSavedSf, ignoreCase = true) } ->

                    sfNames.first { it.equals(rawSavedSf, ignoreCase = true) }

                rawSavedSf.contains("yamaha", ignoreCase = true) || rawSavedSf.contains("korg", ignoreCase = true) || rawSavedSf.contains("ketron", ignoreCase = true) -> "Yamaha x Korg x Ketron Studio"

                rawSavedSf.contains("sam", ignoreCase = true) || rawSavedSf.contains("dream", ignoreCase = true) -> "Dream SAM2634 v2.1"

                rawSavedSf.contains("mega", ignoreCase = true) -> "MegaPro Sound"

                rawSavedSf.contains("minus", ignoreCase = true) || rawSavedSf.contains("videokey", ignoreCase = true) -> "VideoKey Studio Minus-One"

                else -> defaultSfForState

            }

            soundEngine.setActiveSoundFont(resolvedSf)

            val playables = soundEngine.getPlayableCodes()

            val favSongs = _uiState.value.favoriteCodes.mapNotNull { code -> repository.findByCode(code) }

            val allScanned = repository.getAllSongs()

            categoryCache.clear()

            val cat = _uiState.value.selectedCategoryFilter

            val catMatching = if (cat == "ALL") allScanned else allScanned.filter { it.matchesCategory(cat) }

            categoryCache[cat.uppercase()] = catMatching

            viewModelScope.launch(Dispatchers.Default) {

                listOf("OPM", "ROCK", "POP", "MEDLEY").forEach { c ->

                    if (!categoryCache.containsKey(c)) {

                        categoryCache[c] = allScanned.filter { it.matchesCategory(c) }

                    }

                }

            }

            val currentLast = _uiState.value.lastPlayedSong ?: allScanned.firstOrNull()

            _uiState.update {

                it.copy(

                    isScanning = false,

                    scanStatus = statusMsg,

                    searchResults = repository.search("", it.onlyShowPlayableSongs) { c -> playables.contains(c) },

                    trendingSongs = catMatching,

                    lastPlayedSong = currentLast,

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

        val mediaList = when (_uiState.value.bgVisualMode) {

            BackgroundVisualMode.VIDEOS -> list.filter { it.type == BackgroundType.VIDEO }.ifEmpty { list }

            BackgroundVisualMode.PHOTOS -> list.filter { it.type == BackgroundType.IMAGE }.ifEmpty { list }

            BackgroundVisualMode.CAMERA -> emptyList()

        }

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



    fun setScoreMode(mode: ScoreMode) {

        prefs.edit().putString("score_mode", mode.name).apply()

        _uiState.update { it.copy(scoreMode = mode, isMicScoringEnabled = mode != ScoreMode.OFF) }

        if (mode == ScoreMode.OFF) {

            micScorer.stopScoring()

            stopScoringFanfareAudio()

        } else if (_uiState.value.isPlaying && mode == ScoreMode.MICROPHONE) {

            micScorer.startScoring()

        }

    }



    fun setScoringDifficulty(difficulty: ScoringDifficulty) {

        prefs.edit().putString("scoring_difficulty", difficulty.name).apply()

        _uiState.update { it.copy(scoringDifficulty = difficulty) }

    }



    fun setFanfareSoundLevel(level: FanfareSoundLevel) {

        prefs.edit().putString("fanfare_sound_level", level.name).apply()

        _uiState.update { it.copy(fanfareSoundLevel = level, isScoringSoundEnabled = level != FanfareSoundLevel.OFF) }

    }



    private var customScoringMediaPlayer: android.media.MediaPlayer? = null

    fun playScoringFanfareAudio(score: Int) {
        val fanfareVol = _uiState.value.fanfareSoundLevel.volume
        if (fanfareVol <= 0.01f) return
        val style = _uiState.value.scoringSoundStyle
        val customUri = _uiState.value.customScoringSoundUri

        if (style == com.example.videokey.data.ScoringSoundStyle.CUSTOM_AUDIO && !customUri.isNullOrBlank()) {
            try {
                customScoringMediaPlayer?.release()
                customScoringMediaPlayer = android.media.MediaPlayer().apply {
                    setDataSource(getApplication(), android.net.Uri.parse(customUri))
                    setVolume(fanfareVol, fanfareVol)
                    setOnCompletionListener { mp ->
                        try { mp.release() } catch (_: Exception) {}
                        customScoringMediaPlayer = null
                    }
                    prepare()
                    start()
                }
                return
            } catch (e: Exception) {
                Log.w("VideoKeyViewModel", "Failed to play custom scoring audio, falling back to fanfare: ${e.message}")
            }
        }
        com.example.videokey.data.VideokeSoundEffects.playScoringFanfare(score, fanfareVol, style)
    }

    fun stopScoringFanfareAudio() {
        try {
            customScoringMediaPlayer?.stop()
            customScoringMediaPlayer?.release()
            customScoringMediaPlayer = null
        } catch (_: Throwable) {}
        try {
            com.example.videokey.data.VideokeSoundEffects.stop()
        } catch (_: Throwable) {}
    }

    fun setScoringSoundStyle(style: com.example.videokey.data.ScoringSoundStyle) {
        prefs.edit().putString("scoring_sound_style", style.name).apply()
        _uiState.update { it.copy(scoringSoundStyle = style) }
    }

    fun setCustomScoringSoundUri(uri: String?) {
        prefs.edit().putString("custom_scoring_sound_uri", uri).apply()
        _uiState.update { it.copy(customScoringSoundUri = uri) }
    }

    fun setCameraFrontFacing(isFront: Boolean) {
        prefs.edit().putBoolean("camera_is_front_facing", isFront).apply()
        _uiState.update { it.copy(isCameraFrontFacing = isFront) }
    }

    fun toggleCameraFacing() {
        setCameraFrontFacing(!_uiState.value.isCameraFrontFacing)
    }

    fun testScoringFanfare() {
        playScoringFanfareAudio(95)
    }

    fun setBackgroundVisualMode(mode: BackgroundVisualMode) {

        prefs.edit().putString("bg_visual_mode", mode.name).apply()

        _uiState.update { it.copy(bgVisualMode = mode) }

    }



    fun toggleContinuousVideoChangePerSong(enabled: Boolean) {

        prefs.edit().putBoolean("continuous_video_change_per_song", enabled).apply()

        _uiState.update { it.copy(continuousVideoChangePerSong = enabled) }

    }



    fun setLyricOutlineThickness(thickness: Float) {

        val clamped = thickness.coerceIn(1.0f, 12.0f)

        prefs.edit().putFloat("lyric_outline_thickness", clamped).apply()

        _uiState.update { it.copy(lyricConfig = it.lyricConfig.copy(outlineThickness = clamped)) }

    }



    fun setLyricFontSize(portraitSp: Float, landscapeSp: Float) {

        val portClamped = portraitSp.coerceIn(24f, 65f)

        val landClamped = landscapeSp.coerceIn(36f, 85f)

        prefs.edit()

            .putFloat("lyric_font_size_portrait", portClamped)

            .putFloat("lyric_font_size_landscape", landClamped)

            .apply()

        _uiState.update {

            it.copy(

                lyricConfig = it.lyricConfig.copy(

                    fontSizePortraitSp = portClamped,

                    fontSizeLandscapeSp = landClamped

                )

            )

        }

    }



    fun setLyricLineSpacing(spacingDp: Float) {

        val clamped = spacingDp.coerceIn(4f, 32f)

        prefs.edit().putFloat("lyric_line_spacing", clamped).apply()

        _uiState.update { it.copy(lyricConfig = it.lyricConfig.copy(lineSpacingDp = clamped)) }

    }



    fun toggleLyricDropShadow(enabled: Boolean) {

        prefs.edit().putBoolean("lyric_has_drop_shadow", enabled).apply()

        _uiState.update { it.copy(lyricConfig = it.lyricConfig.copy(hasDropShadow = enabled)) }

    }



    fun setLyricCountdownMode(mode: LyricCountdownMode) {

        prefs.edit().putString("lyric_countdown_mode", mode.name).apply()

        _uiState.update { it.copy(lyricConfig = it.lyricConfig.copy(countdownMode = mode)) }

    }



    fun setLyricTimingOffset(offsetMs: Int) {

        val clamped = offsetMs.coerceIn(-500, 500)

        prefs.edit().putInt("lyric_timing_offset", clamped).apply()

        _uiState.update { it.copy(lyricConfig = it.lyricConfig.copy(timingOffsetMs = clamped)) }

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

        val mode = if (enabled) ScoreMode.MICROPHONE else ScoreMode.OFF

        setScoreMode(mode)

    }



    fun toggleScoringSound(enabled: Boolean) {

        prefs.edit().putBoolean("scoring_sound_enabled", enabled).apply()

        _uiState.update { it.copy(isScoringSoundEnabled = enabled) }

        if (!enabled) {

            stopScoringFanfareAudio()

        }

    }



    fun toggleKeypadVoice(enabled: Boolean) {

        prefs.edit().putBoolean("is_keypad_voice_enabled", enabled).apply()

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

        prefs.edit().putString("lyric_wipe_color", wipeColor.name).apply()

        _uiState.update { it.copy(lyricConfig = it.lyricConfig.copy(wipeColor = wipeColor)) }

    }



    fun setLyricShadowStyle(shadowStyle: LyricShadowStyle) {

        prefs.edit().putString("lyric_shadow_style", shadowStyle.name).apply()

        _uiState.update { it.copy(lyricConfig = it.lyricConfig.copy(shadowStyle = shadowStyle)) }

    }



    fun toggleLyricAllCaps() {

        val newVal = !_uiState.value.lyricConfig.isAllCaps

        prefs.edit().putBoolean("lyric_all_caps", newVal).apply()

        _uiState.update { it.copy(lyricConfig = it.lyricConfig.copy(isAllCaps = newVal)) }

    }



    fun toggleLyricItalic() {

        val newVal = !_uiState.value.lyricConfig.isItalic

        prefs.edit().putBoolean("lyric_is_italic", newVal).apply()

        _uiState.update { it.copy(lyricConfig = it.lyricConfig.copy(isItalic = newVal)) }

    }



    fun setLyricFontScale(scale: Float) {

        val clamped = scale.coerceIn(0.75f, 1.4f)

        prefs.edit().putFloat("lyric_font_scale", clamped).apply()

        _uiState.update { it.copy(lyricConfig = it.lyricConfig.copy(fontScale = clamped)) }

    }



    fun setLyricLineCount(lineCount: LyricLineCount) {

        prefs.edit().putString("lyric_line_count", lineCount.name).apply()

        _uiState.update { it.copy(lyricConfig = it.lyricConfig.copy(lineCount = lineCount)) }

    }



    fun setLyricPosition(position: LyricScreenPosition) {

        prefs.edit().putString("lyric_position", position.name).apply()

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

        _uiState.update {

            it.copy(appTheme = theme)

        }

    }



    fun saveFullLyricConfig(config: LyricCustomization) {

        prefs.edit()

            .putString("lyric_outline_color", config.outlineColor.name)

            .putString("lyric_font_family", config.fontFamily.name)

            .putString("lyric_shadow_style", config.shadowStyle.name)

            .putString("lyric_wipe_color", config.wipeColor.name)

            .putFloat("lyric_outline_thickness", config.outlineThickness)

            .putFloat("lyric_font_size_portrait", config.fontSizePortraitSp)

            .putFloat("lyric_font_size_landscape", config.fontSizeLandscapeSp)

            .putFloat("lyric_line_spacing", config.lineSpacingDp)

            .putBoolean("lyric_has_drop_shadow", config.hasDropShadow)

            .putBoolean("lyric_all_caps", config.isAllCaps)

            .putBoolean("lyric_is_italic", config.isItalic)

            .putFloat("lyric_font_scale", config.fontScale)

            .putString("lyric_line_count", config.lineCount.name)

            .putString("lyric_position", config.position.name)

            .putString("lyric_countdown_mode", config.countdownMode.name)

            .putInt("lyric_timing_offset", config.timingOffsetMs)

            .putBoolean("has_customized_tv_lyrics_v4", true)

            .apply()

    }



    fun applyLyricPreset(presetName: String) {

        val current = _uiState.value.lyricConfig

        val baseConfig = when (presetName) {

            "live_tv" -> LyricCustomization(

                fontFamily = LyricFontFamily.GEOMETRIC_POP,

                wipeColor = LyricWipeColor.DARK_BLUE,

                inactiveColor = 0xFFFFFFFF,

                outlineColor = LyricOutlineColor.NAVY,

                shadowStyle = LyricShadowStyle.CLEAN,

                outlineThickness = 3.0f,

                fontSizePortraitSp = 36.0f,

                fontSizeLandscapeSp = 54.0f,

                lineSpacingDp = 8.0f,

                hasDropShadow = true,

                isAllCaps = false,

                isItalic = false,

                fontScale = 1.0f,

                lineCount = LyricLineCount.TWO_LINES,

                position = LyricScreenPosition.BOTTOM,

                countdownMode = LyricCountdownMode.CLASSIC_DOTS,

                timingOffsetMs = 0

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

        val newConfig = if (presetName == "live_tv") {

            baseConfig

        } else {

            baseConfig.copy(

                lineCount = current.lineCount,

                position = current.position

            )

        }

        saveFullLyricConfig(newConfig)

        _uiState.update { it.copy(lyricConfig = newConfig) }

    }



    fun setRemoteConnected(connected: Boolean) {

        if (_uiState.value.isRemoteConnected != connected) {

            _uiState.update { it.copy(isRemoteConnected = connected) }

        }

    }



    fun openEditNameDialog() {

        _uiState.update { it.copy(isEditNameDialogOpen = true) }

    }



    fun closeEditNameDialog() {

        _uiState.update { it.copy(isEditNameDialogOpen = false) }

    }



    fun openAdminKeyGenerator() {

        _uiState.update { it.copy(isAdminKeyGeneratorOpen = true) }

    }



    fun closeAdminKeyGenerator() {

        _uiState.update { it.copy(isAdminKeyGeneratorOpen = false) }

    }



    fun setUserName(name: String) {

        val clean = name.trim().ifEmpty { "JUAN" }

        prefs.edit().putString("user_profile_name", clean).apply()

        _uiState.update { it.copy(userName = clean, isEditNameDialogOpen = false) }

    }



    // --- VIP & MEDIA MANAGEMENT FEATURES ---

    private var mp3Player: android.media.MediaPlayer? = null

    private var mp3ProgressJob: kotlinx.coroutines.Job? = null



    fun saveMixerAsSoundFontPreset(presetName: String) {

        if (!_uiState.value.isVipActivated) {

            _uiState.update {

                it.copy(

                    isYouTubeDownloaderOpen = true,

                    vipActivationError = if (it.appLanguage == AppLanguage.TAGALOG)

                        "Ang pag-save ng Custom SoundFont Preset ay para lamang sa mga VIP Members!"

                    else

                        "Saving Custom SoundFont Presets is exclusively for VIP Members!"

                )

            }

            return

        }



        val cleanName = presetName.trim().ifBlank { "Acoustic Stage Mix" }

        val s = _uiState.value

        val presetData = "${s.drumsPercent},${s.bassPercent},${s.guitarPianoPercent},${s.stringsBrassPercent},${s.melodyPercent},${s.reverbPercent}"



        val savedPresets = prefs.getStringSet("vip_soundfont_presets", emptySet())?.toMutableSet() ?: mutableSetOf()

        val entry = "$cleanName::$presetData"

        savedPresets.removeAll { it.startsWith("$cleanName::") }

        savedPresets.add(entry)

        prefs.edit().putStringSet("vip_soundfont_presets", savedPresets).apply()



        // Re-inject into availableSoundFonts

        val presetDisplay = "[VIP Preset] $cleanName"

        _uiState.update { current ->

            val updatedList = (current.availableSoundFonts + presetDisplay).distinct()

            current.copy(

                availableSoundFonts = updatedList,

                activeSoundFontName = presetDisplay

            )

        }



        val isTl = s.appLanguage == AppLanguage.TAGALOG

        _uiState.update { it.copy(alertMessage = if (isTl) "⭐ Na-save ang VIP SoundFont Preset: $cleanName!" else "⭐ Saved VIP SoundFont Preset: $cleanName!") }

    }



    fun deleteBackground(bg: KaraokeBackground) {

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {

            try {

                if (bg.path.isNotBlank()) {

                    val f = java.io.File(bg.path)

                    if (f.exists()) f.delete()

                }

                scanBackgroundMedia(java.io.File(_uiState.value.currentBackgroundFolder))

            } catch (e: Exception) {

                android.util.Log.e("VideoKeyViewModel", "Failed to delete background", e)

            }

        }

    }



    fun toggleWipeHighlight(enabled: Boolean? = null) {

        if (!_uiState.value.isVipActivated) {

            _uiState.update {

                it.copy(

                    isYouTubeDownloaderOpen = true,

                    vipActivationError = if (it.appLanguage == AppLanguage.TAGALOG)

                        "Ang Lyric Highlight Wipe Turn ON/OFF ay para lamang sa mga VIP Members!"

                    else

                        "Lyric Highlight Wipe toggle is exclusively for VIP Members!"

                )

            }

            return

        }

        val current = _uiState.value.lyricConfig

        val newVal = enabled ?: !current.isWipeHighlightEnabled

        val updated = current.copy(isWipeHighlightEnabled = newVal)

        prefs.edit().putBoolean("lyric_wipe_highlight_enabled", newVal).apply()

        _uiState.update { it.copy(lyricConfig = updated) }

    }



    fun setVipButtonTexture(texture: VipButtonTexture) {

        if (!_uiState.value.isVipActivated && texture != VipButtonTexture.DEFAULT) {

            val isTl = _uiState.value.appLanguage == AppLanguage.TAGALOG

            _uiState.update {

                it.copy(

                    alertMessage = if (isTl) "👑 Ang Button Texture ay para lamang sa VIP! Mag-activate ng VIP para ma-unlock."

                                   else "👑 Button Textures are exclusive to VIP members! Activate VIP to unlock."

                )

            }

            openYouTubeDownloader(null)

            return

        }

        prefs.edit().putString("vip_button_texture", texture.name).apply()

        _uiState.update { it.copy(vipButtonTexture = texture) }

    }



    fun openVipMp3Player() {

        if (!_uiState.value.isVipActivated) {

            _uiState.update {

                it.copy(

                    isYouTubeDownloaderOpen = true,

                    vipActivationError = if (it.appLanguage == AppLanguage.TAGALOG)

                        "Ang VIP MP3 Player ay para lamang sa mga VIP Members!"

                    else

                        "The VIP MP3 Player is exclusively for VIP Members!"

                )

            }

            return

        }

        scanMp3Songs()

        _uiState.update { it.copy(isMp3PlayerDialogOpen = true) }

    }



    fun closeVipMp3Player() {

        _uiState.update { it.copy(isMp3PlayerDialogOpen = false) }

    }



    fun scanMp3Songs() {

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {

            val list = mutableListOf<Song>()

            val seenPaths = HashSet<String>()



            // 1. Query Android MediaStore for all device MP3s

            try {

                val proj = arrayOf(

                    android.provider.MediaStore.Audio.Media._ID,

                    android.provider.MediaStore.Audio.Media.TITLE,

                    android.provider.MediaStore.Audio.Media.ARTIST,

                    android.provider.MediaStore.Audio.Media.DATA

                )

                val sel = "${android.provider.MediaStore.Audio.Media.IS_MUSIC} != 0"

                getApplication<android.app.Application>().contentResolver.query(

                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,

                    proj, sel, null, null

                )?.use { cursor ->

                    val titleCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.TITLE)

                    val artistCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ARTIST)

                    val dataCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DATA)

                    while (cursor.moveToNext()) {

                        val path = cursor.getString(dataCol) ?: continue

                        if (path.lowercase().endsWith(".mp3") && seenPaths.add(path)) {

                            val f = java.io.File(path)

                            if (f.exists() && f.length() > 0) {

                                val t = cursor.getString(titleCol) ?: f.nameWithoutExtension

                                val a = cursor.getString(artistCol) ?: "Local MP3"

                                val code = String.format("%06d", (100000 + (list.size % 899999)))

                                list.add(Song(code = code, title = t, artist = if (a.contains("<unknown>")) "Local MP3" else a, filePath = path))

                            }

                        }

                    }

                }

            } catch (e: Exception) {

                android.util.Log.w("VideoKeyViewModel", "MediaStore scan skipped: ${e.message}")

            }

            val searchDirs = listOfNotNull(

                java.io.File("/storage/emulated/0/Music"),

                java.io.File("/sdcard/Music"),

                java.io.File("/storage/emulated/0/Download"),

                java.io.File("/sdcard/Download"),

                java.io.File("/storage/emulated/0/VideoKey"),

                java.io.File("/sdcard/VideoKey"),

                android.os.Environment.getExternalStorageDirectory()?.let { java.io.File(it, "Music") },

                android.os.Environment.getExternalStorageDirectory()?.let { java.io.File(it, "Download") },

                getApplication<android.app.Application>().getExternalFilesDir(null)

            ).distinctBy { it.absolutePath }



            for (dir in searchDirs) {

                if (dir.exists() && dir.isDirectory) {

                    dir.walkTopDown().maxDepth(3).filter {

                        it.isFile && it.name.lowercase().endsWith(".mp3")

                    }.forEach { f ->

                        val code = (100000 + (list.size % 899999)).toString()

                        val rawName = f.nameWithoutExtension

                        val parts = rawName.split(" - ", "-", limit = 2)

                        val title = if (parts.size >= 2) parts[1].trim() else rawName

                        val artist = if (parts.size >= 2) parts[0].trim() else "Local MP3"

                        list.add(Song(code = code, title = title, artist = artist, filePath = f.absolutePath))

                    }

                }

            }

            val distinctMp3s = list.distinctBy { it.filePath.lowercase() }
            _uiState.update { it.copy(mp3PlayerSongList = distinctMp3s) }

        }

    }



    fun playMp3Song(song: Song) {

        try {

            if (_uiState.value.isPlaying) {

                onStop()

            }

            mp3Player?.release()

            mp3Player = android.media.MediaPlayer().apply {

                setDataSource(song.filePath)

                prepare()

                start()

                setOnCompletionListener {

                    playNextMp3Song()

                }

            }

            _uiState.update {

                it.copy(

                    activeMp3Song = song,

                    isMp3Playing = true,

                    mp3DurationMs = mp3Player?.duration ?: 0,

                    mp3CurrentPositionMs = 0

                )

            }

            startMp3ProgressTracking()

        } catch (e: Exception) {

            android.util.Log.e("VideoKeyViewModel", "Failed to play MP3", e)

        }

    }



    fun toggleMp3PlayPause() {

        val mp = mp3Player ?: return

        if (mp.isPlaying) {

            mp.pause()

            _uiState.update { it.copy(isMp3Playing = false) }

        } else {

            mp.start()

            _uiState.update { it.copy(isMp3Playing = true) }

        }

    }



    fun seekMp3(posMs: Int) {

        mp3Player?.seekTo(posMs)

        _uiState.update { it.copy(mp3CurrentPositionMs = posMs) }

    }



    fun playNextMp3Song() {

        val currentList = _uiState.value.mp3PlayerSongList

        val currentSong = _uiState.value.activeMp3Song

        if (currentList.isNotEmpty()) {

            val idx = currentList.indexOfFirst { it.filePath == currentSong?.filePath }

            val nextIdx = if (idx >= 0) (idx + 1) % currentList.size else 0

            playMp3Song(currentList[nextIdx])

        }

    }



    fun playPrevMp3Song() {

        val currentList = _uiState.value.mp3PlayerSongList

        val currentSong = _uiState.value.activeMp3Song

        if (currentList.isNotEmpty()) {

            val idx = currentList.indexOfFirst { it.filePath == currentSong?.filePath }

            val prevIdx = if (idx > 0) idx - 1 else currentList.size - 1

            playMp3Song(currentList[prevIdx])

        }

    }



    private fun startMp3ProgressTracking() {

        mp3ProgressJob?.cancel()

        mp3ProgressJob = viewModelScope.launch {

            while (coroutineContext[kotlinx.coroutines.Job]?.isActive == true) {

                val mp = mp3Player

                if (mp != null && mp.isPlaying) {

                    _uiState.update {

                        it.copy(mp3CurrentPositionMs = mp.currentPosition)

                    }

                }

                kotlinx.coroutines.delay(500)

            }

        }

    }





    fun stopMp3Player() {

        try {

            if (mp3Player?.isPlaying == true) {

                mp3Player?.pause()

            }

            mp3Player?.stop()

            mp3Player?.release()

            mp3Player = null

        } catch (e: Exception) {

            android.util.Log.e("VideoKeyViewModel", "Error stopping MP3 player", e)

        }

        _uiState.update { it.copy(isMp3Playing = false) }

    }



    fun refreshRandomFavorite() {

        val all = repository.getAllSongs()

        val candidate = if (all.isNotEmpty()) {

            all.random()

        } else if (_uiState.value.trendingSongs.isNotEmpty()) {

            _uiState.value.trendingSongs.random()

        } else null



        if (candidate != null) {

            _uiState.update { it.copy(randomFavoriteSong = candidate) }

        }

    }



    fun resetSettingsToDefault() {

        try {

            prefs.edit()

                .remove("theme")

                .remove("mic_scoring_mode")

                .remove("fanfare_level")

                .remove("keypad_voice")

                .remove("vip_button_texture")

                .remove("countdown_mode")

                .remove("wipe_highlight_enabled")

                .apply()

        } catch (e: Exception) {}



        val defaultLyric = LyricCustomization()

        _uiState.update {

            it.copy(

                appTheme = AppTheme.SLATE_DARK,

                lyricConfig = defaultLyric,

                backgroundChangeMode = BackgroundChangeMode.AUTO_CHANGE_PER_SONG,

                scoreMode = ScoreMode.MICROPHONE,

                scoringDifficulty = ScoringDifficulty.MEDIUM,

                isKeypadVoiceEnabled = true,

                vipButtonTexture = VipButtonTexture.DEFAULT,

                alertMessage = if (it.appLanguage == AppLanguage.TAGALOG)

                    "✅ Naibalik na sa orihinal na default ang lahat ng settings!"

                else

                    "✅ All settings have been reset to factory default!"

            )

        }

    }



    fun setCountdownPhoto(uri: String) {
        setCountdownAllImages(uri)
    }

    fun setCountdownStepImage(step: Int, uri: String?) {
        val key = when (step) {
            4 -> "lyric_countdown_img_4_uri"
            3 -> "lyric_countdown_img_3_uri"
            2 -> "lyric_countdown_img_2_uri"
            1 -> "lyric_countdown_img_1_uri"
            5 -> "lyric_countdown_img_go_uri"
            else -> null
        }
        if (key != null) {
            if (uri != null) {
                prefs.edit().putString(key, uri).putString("lyric_countdown_mode", LyricCountdownMode.PHOTO_BADGE.name).apply()
            } else {
                prefs.edit().remove(key).apply()
            }
        }
        _uiState.update { state ->
            val cfg = state.lyricConfig
            val updated = when (step) {
                4 -> cfg.copy(countdownImg4Uri = uri, countdownMode = LyricCountdownMode.PHOTO_BADGE)
                3 -> cfg.copy(countdownImg3Uri = uri, countdownMode = LyricCountdownMode.PHOTO_BADGE)
                2 -> cfg.copy(countdownImg2Uri = uri, countdownMode = LyricCountdownMode.PHOTO_BADGE)
                1 -> cfg.copy(countdownImg1Uri = uri, countdownMode = LyricCountdownMode.PHOTO_BADGE)
                5 -> cfg.copy(countdownImgGoUri = uri, countdownMode = LyricCountdownMode.PHOTO_BADGE)
                else -> cfg
            }
            state.copy(lyricConfig = updated)
        }
    }

    fun setCountdownAllImages(uri: String) {
        prefs.edit()
            .putString("lyric_countdown_img_4_uri", uri)
            .putString("lyric_countdown_img_3_uri", uri)
            .putString("lyric_countdown_img_2_uri", uri)
            .putString("lyric_countdown_img_1_uri", uri)
            .putString("lyric_countdown_img_go_uri", uri)
            .putString("lyric_countdown_photo_uri", uri)
            .putString("lyric_countdown_mode", LyricCountdownMode.PHOTO_BADGE.name)
            .apply()
        _uiState.update { state ->
            state.copy(
                lyricConfig = state.lyricConfig.copy(
                    customCountdownPhotoUri = uri,
                    countdownImg4Uri = uri,
                    countdownImg3Uri = uri,
                    countdownImg2Uri = uri,
                    countdownImg1Uri = uri,
                    countdownImgGoUri = uri,
                    countdownMode = LyricCountdownMode.PHOTO_BADGE
                )
            )
        }
    }

    fun clearAllCountdownImages() {
        prefs.edit()
            .remove("lyric_countdown_img_4_uri")
            .remove("lyric_countdown_img_3_uri")
            .remove("lyric_countdown_img_2_uri")
            .remove("lyric_countdown_img_1_uri")
            .remove("lyric_countdown_img_go_uri")
            .remove("lyric_countdown_photo_uri")
            .apply()
        _uiState.update { state ->
            state.copy(
                lyricConfig = state.lyricConfig.copy(
                    customCountdownPhotoUri = null,
                    countdownImg4Uri = null,
                    countdownImg3Uri = null,
                    countdownImg2Uri = null,
                    countdownImg1Uri = null,
                    countdownImgGoUri = null
                )
            )
        }
    }



    fun initiateGcashVipPayment(activity: Activity) {

        val devId = _uiState.value.deviceId.ifBlank { SupabaseVipManager.getHardwareDeviceId(activity) }

        _uiState.update { it.copy(isInitiatingGcashPayment = true) }

        viewModelScope.launch {

            val isTl = _uiState.value.appLanguage == AppLanguage.TAGALOG

            val result = SupabaseVipManager.initiatePayMongoCheckout(activity, devId)

            _uiState.update { it.copy(isInitiatingGcashPayment = false) }

            result.onSuccess {

                _uiState.update {

                    it.copy(

                        alertMessage = if (isTl) "💳 Nagbukas ang GCash Checkout (₱110). Pakikumpleto ang pagbayad at bumalik dito."

                                       else "💳 GCash Checkout (₱110) opened. Please complete payment and return here."

                    )

                }

            }.onFailure { err ->

                _uiState.update {

                    it.copy(

                        alertMessage = if (isTl) "⚠️ Hindi ma-load ang checkout: ${err.message}"

                                       else "⚠️ Failed to load checkout: ${err.message}"

                    )

                }

            }

        }

    }



    fun checkOnlineVipStatus(showMessage: Boolean = true) {

        val context = getApplication<android.app.Application>()

        val devId = _uiState.value.deviceId.ifBlank { SupabaseVipManager.getHardwareDeviceId(context) }

        _uiState.update { it.copy(isCheckingVipStatus = true) }

        viewModelScope.launch {

            val isTl = _uiState.value.appLanguage == AppLanguage.TAGALOG

            val result = SupabaseVipManager.verifyVipStatusOnline(context, devId)

            _uiState.update { it.copy(isCheckingVipStatus = false) }

            if (result.isVip) {

                setVipActivated(true)

                refreshDownloadCatalog()

                checkAndTriggerVip54kDownload()

                if (showMessage) {

                    _uiState.update {

                        it.copy(

                            alertMessage = if (isTl) "💎 Matagumpay na Na-verify ang VIP! (Device: $devId)"

                                           else "💎 VIP Successfully Verified! (Device: $devId)"

                        )

                    }

                }

            } else if (showMessage) {

                _uiState.update {

                    it.copy(

                        alertMessage = if (isTl) "ℹ️ Hindi pa aktibo ang VIP para sa Device: $devId"

                                       else "ℹ️ VIP is not active yet for Device: $devId"

                    )

                }

            }

        }

    }



    fun showPaymentCancelledAlert() {

        val isTl = _uiState.value.appLanguage == AppLanguage.TAGALOG

        _uiState.update {

            it.copy(

                alertMessage = if (isTl) "Kanselado ang pagbayad sa GCash."

                               else "GCash payment was cancelled."

            )

        }

    }



    override fun onCleared() {

        super.onCleared()

        soundEngine.stop()

        micScorer.stopScoring()

        stopScoringFanfareAudio()

        voiceManager.shutdown()

        playbackJob?.cancel()

        mp3ProgressJob?.cancel()

        try {

            mp3Player?.release()

            mp3Player = null

        } catch (_: Exception) {}

    }

}



// ==========================================

// Category Filtering Helpers (Home Tabs)

// ==========================================



private val OPM_KEYWORDS = listOf(

    "eraserheads", "parokya", "rivermaya", "kamikazee", "gary valenciano",

    "regine", "martin nievera", "sarah geronimo", "ben&ben", "ben & ben", "moira",

    "aegis", "rey valera", "freddie aguilar", "side a", "silent sanctuary",

    "yeng constantino", "up dharma down", "bamboo", "sponge cola", "hale",

    "callalily", "cueshe", "cueshé", "gloc 9", "gloc-9", "december avenue", "iv of spades",

    "south border", "orange and lemons", "itchyworms", "noel cabangon",

    "apo hiking", "vhong navarro", "siakol", "grin dept", "chicosci",

    "mayonnaise", "true faith", "freestyle", "brownman revival", "willie revillame",

    "ric segreto", "sharon cuneta", "lea salonga", "ogie alcasid",

    "jaya", "janno gibbs", "sheryn regis", "erik santos", "christian bautista",

    "jed madela", "angeline quinto", "morissette", "jona", "kz tandingan",

    "tj monterde", "arthur nery", "adie", "zack tabudlo", "dionela", "maki",

    "sb19", "bini", "al james", "flow g", "skusta clee", "shanti dope", "ex battalion",

    "april boy", "victor wood", "eddie peregrina", "claire dela fuente", "eva eugenio",

    "didith reyes", "imelda papin", "marco sison", "anthony castelo", "dingdong avanzado",

    "florian", "nonoy zuniga", "roel cortez", "freddie", "vst & co", "boyfriends"

)



private val TAGALOG_WORDS_SET = setOf(

    "ang", "sa", "ng", "mga", "ikaw", "ako", "ko", "mo", "na", "at", "pa", "ba", "lang", "lamang",

    "din", "rin", "naman", "sana", "dahil", "bakit", "hindi", "di", "kailangan", "hanggang",

    "pangarap", "tayo", "natin", "kahit", "huwag", "wag", "gising", "kapag", "wala", "meron", "may",

    "buhay", "mahal", "puso", "pusong", "pag-ibig", "pagibig", "pagsuyo", "pasko", "kung", "sino",

    "akin", "iyo", "amin", "inyo", "kanya", "kanila", "nila", "siya", "kami", "kayo", "dito",

    "diyan", "doon", "nito", "niyan", "niyon", "paano", "kailan", "saan", "kumusta", "salamat",

    "paalam", "halika", "halina", "tara", "nanay", "tatay", "inay", "itay", "ate", "kuya", "bunso",

    "anak", "kapatid", "kaibigan", "kasama", "minamahal", "iniibig", "sinisinta", "giliw", "sinta",

    "mutya", "paraluman", "tala", "araw", "buwan", "bituin", "langit", "lupa", "dagat", "ulan",

    "hangin", "apoy", "luha", "ngiti", "tawa", "yakap", "halik", "kamay", "mata", "labi", "awit",

    "kanta", "tugtog", "himig", "musika", "sayaw", "galaw", "laro", "gabi", "umaga", "tanghali",

    "hapon", "ngayon", "bukas", "kahapon", "sandali", "saglit", "muli", "minsan", "lagi", "tuwina",

    "kailanman", "habangbuhay", "dalangin", "dasal", "panalangin", "bayan", "bansa", "pilipinas",

    "pinoy", "pinay", "tagalog", "dalaga", "binata", "tao", "mukha", "isip", "alaala", "damdamin",

    "lungkot", "saya", "tuwa", "galit", "takot", "hiya", "awa", "pag-asa", "tiwala", "sumpa",

    "pangako", "liham", "sulat", "kuwento", "tinig", "boses", "tawag", "sigaw", "bulong", "dinggin",

    "pakinggan", "tingnan", "masdan", "hawak", "yakapin", "hagkan", "halikan", "ibigin", "mahalin",

    "alagaan", "ingatan", "iwan", "iwanan", "layas", "alis", "balik", "balikan", "uwi", "hanap",

    "hanapin", "tagpo", "tagpuan", "samahan", "sabay", "tulong", "damayan", "abot", "takbo",

    "lipad", "tulog", "gising", "panaginip", "puyat", "pagod", "gutom", "sarap", "tamis", "pait",

    "lamig", "init", "bagyo", "kidlat", "baha", "puti", "itim", "pula", "asul", "dilaw", "berde",

    "noon", "minsan", "kaya", "bato", "kahoy", "dahon", "bulaklak", "paru-paro", "paruparo", "ibon",

    "isda", "tubig", "ilog", "bundok", "kalsada", "daan", "sakay", "dyip", "kotse", "bahay",

    "silid", "bintana", "pinto", "hagdan", "mesa", "silya", "orasan", "salamin", "liwanag",

    "dilim", "anino", "lasa", "tunog", "kutob", "hinala", "totoo", "sinungaling",

    "lihim", "sikreto", "hiwaga", "milagro", "himala", "swerte", "malas", "tadhana", "kapalaran",

    "wakas", "simula", "gitna", "dulo", "hangganan", "habang", "bago", "pagkatapos",

    "habang-buhay", "magpakailanman", "panghabangbuhay", "saanman", "kailanpaman", "sinuman",

    "gayon", "ganoon", "ganito", "ganyan", "siyang", "akong", "ikaw", "inyong",

    "aming", "kanilang", "iyong", "aking", "sintang", "pagmamahal",

    "iniirog", "katulad", "tulad", "parang", "tila", "wari", "animo", "kapara", "kawangis",

    "awitin", "kantahin", "tugtugin", "sayawin", "isayaw", "kantang", "awiting", "himig-pasko",

    "paskong", "bagong", "taon", "simbang", "gabi", "misa", "talisay", "dapit", "mapasagot",

    "oras", "panahon", "yakap", "titig", "tingin", "alapaap", "ligaya", "sinta'y", "ako'y", "ikaw'y",

    "kayang", "lumayo", "lumapit", "bumalik", "nawala", "nakita", "naglaho", "sumikat", "lumubog"

)



private val ROCK_KEYWORDS = listOf(

    "rock", "metal", "punk", "anthem", "band", "eraserheads", "parokya", "rivermaya",

    "kamikazee", "bamboo", "siakol", "wolfgang", "razorback", "slapshock",

    "greyhoundz", "queso", "chicosci", "urbandub", "franco", "mayonnaise",

    "typecast", "sponge cola", "callalily", "cueshe", "sandwich", "6cyclemind",

    "kitchie nadal", "queen", "bon jovi", "guns n", "aerosmith", "metallica",

    "scorpions", "led zeppelin", "ac/dc", "nirvana", "linkin park", "green day",

    "red hot", "journey", "the beatles", "rolling stones", "oasis", "u2",

    "coldplay", "radiohead", "foo fighters", "audioslave", "creed", "nickelback",

    "evanescence", "avril lavigne", "paramore", "blink-182", "my chemical romance",

    "fall out boy", "simple plan", "cranberries", "deep purple", "eagles",

    "kiss", "whitesnake", "air supply", "survivor", "foreigner", "boston",

    "toto", "europe", "def leppard", "the cure", "iron maiden", "black sabbath"

)



private val POP_KEYWORDS = listOf(

    "pop", "ballad", "dance", "michael jackson", "madonna", "taylor swift",

    "ariana grande", "bruno mars", "ed sheeran", "katy perry", "justin bieber",

    "rihanna", "beyonce", "lady gaga", "dua lipa", "selena gomez",

    "britney spears", "backstreet boys", "westlife", "nsync", "boyzone",

    "sarah geronimo", "morissette", "regine", "moira", "ben&ben", "sb19",

    "bini", "sam smith", "adele", "whitney houston", "mariah carey",

    "celine dion", "billie eilish", "shawn mendes", "harry styles", "maroon 5",

    "one direction", "charlie puth", "chainsmokers", "avicii", "calvin harris",

    "olivia rodrigo", "abba", "carpenter", "elton john"

)



private val MEDLEY_KEYWORDS = listOf(

    "medley", "nonstop", "non-stop", "megamix", "mega-mix", "mashup", "mash-up",

    "potpourri", "remix", "continuous", "hits medley", "classic medley", "dance medley"

)



private val TAGALOG_SPLIT_REGEX = Regex("[^a-zA-Z0-9ñÑ'-]+")



fun Song.matchesCategory(cat: String): Boolean {

    if (cat.equals("ALL", ignoreCase = true)) return true

    val titleLower = title.lowercase()

    val artistLower = artist.lowercase()

    val catLower = category.lowercase()



    return when (cat.uppercase()) {

        "OPM" -> {

            // Must strictly be Tagalog / Filipino songs as requested by user

            val words = titleLower.split(TAGALOG_SPLIT_REGEX).filter { it.isNotBlank() }

            words.any { TAGALOG_WORDS_SET.contains(it) } ||

            words.any { w ->

                w.startsWith("mahal") || w.startsWith("puso") || w.startsWith("ibig") ||

                w.startsWith("buhay") || w.startsWith("pasko") || w.startsWith("giliw") ||

                w.startsWith("sinta") || w.startsWith("kanta") || w.startsWith("awit") ||

                w.startsWith("sayaw") || w.startsWith("tayo") || w.startsWith("atin")

            }

        }

        "ROCK" -> {

            catLower.contains("rock") ||

            ROCK_KEYWORDS.any { artistLower.contains(it) || titleLower.contains(it) }

        }

        "POP" -> {

            catLower.contains("pop") ||

            POP_KEYWORDS.any { artistLower.contains(it) || titleLower.contains(it) }

        }

        "MEDLEY" -> {

            catLower.contains("medley") ||

            catLower.contains("nonstop") ||

            MEDLEY_KEYWORDS.any { artistLower.contains(it) || titleLower.contains(it) }

        }

        else -> catLower.contains(cat.lowercase()) || titleLower.contains(cat.lowercase())

    }

}

