package com.example.videokey.ui.main

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import android.util.Log
import android.graphics.BitmapFactory
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.videokey.data.Song

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun VideoKeyScreen(
    viewModel: VideoKeyViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? android.app.Activity
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE || state.isTvMode

    // Automatically trigger internal storage scan on app launch
    LaunchedEffect(Unit) {
        viewModel.scanInternalStorage(context)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
    ) {
        if (isLandscape) {
            // Fullscreen TV Mode (Android TV or Landscape Phone)
            VideokeTvScreen(
                state = state,
                isLandscape = true,
                onToggleTvMode = {
                    viewModel.exitLandscapeMode(activity)
                },
                onTriggerScore = { viewModel.triggerScoreModal() },
                onOpenSongbook = {
                    if (state.isSongbookOpen) viewModel.closeSongbook() else viewModel.openSongbook()
                },
                onCloseSongbook = { viewModel.closeSongbook() },
                onToggleReserveQueue = { viewModel.toggleReserveQueueOverlay() },
                onCloseReserveQueue = { viewModel.closeReserveQueueOverlay() },
                onRemoveReservedSong = { viewModel.removeFromReserveQueue(it) },
                onPlayReservedSong = { viewModel.playFromReserveQueue(it) },
                onToggleFavorites = { viewModel.toggleFavoritesOverlay() },
                onCloseFavorites = { viewModel.closeFavoritesOverlay() },
                onToggleFavoriteSong = { viewModel.toggleFavoriteSong(it) },
                onOpenRecordingsDialog = { viewModel.openRecordingsDialog() },
                onToggleSongbookFilter = { viewModel.toggleSongbookFilter(it) },
                onOpenSongFolderLocator = { viewModel.openSongFolderPicker() },
                onQuerySongbook = { viewModel.searchSongbook(it) },
                onSelectSongFromBook = { viewModel.selectSong(it) },
                onReserveSongFromBook = { viewModel.reserveSong(it) },
                onOpenSoundFontDialog = { viewModel.openSoundFontDialog() },
                onOpenBackgroundDialog = { viewModel.openBackgroundDialog() },
                onOpenLyricStyleDialog = { viewModel.openLyricStyleDialog() },
                onOpenAudioMixerDialog = { viewModel.openAudioMixerDialog() },
                onOpenSettingsDialog = { viewModel.openSettingsDialog() },
                onAdvanceBackground = { viewModel.advanceNextBackground() },
                onPlay = { viewModel.onPlay() },
                onNext = { viewModel.onNext() },
                onStop = { viewModel.onStop() },
                onDigit = { viewModel.onDigitPressed(it) },
                onClear = { viewModel.onClearCode() },
                onDelete = { viewModel.onDeleteDigit() },
                onReserve = { viewModel.onReserve() },
                onRButtonPressed = { viewModel.onRButtonPressed() },
                onAdjustKey = { viewModel.adjustKey(it) },
                onAdjustTempo = { viewModel.adjustTempo(it) },
                onAdjustMelody = { viewModel.adjustMelody(it) },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Split-Screen Portrait Mode for Android Phone
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Half: Videoke TV Screen
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    VideokeTvScreen(
                        state = state,
                        isLandscape = false,
                        onToggleTvMode = { viewModel.enterTvLandscapeMode(activity) },
                        onTriggerScore = { viewModel.triggerScoreModal() },
                        onOpenSongbook = {
                            if (state.isSongbookOpen) viewModel.closeSongbook() else viewModel.openSongbook()
                        },
                        onCloseSongbook = { viewModel.closeSongbook() },
                        onToggleReserveQueue = { viewModel.toggleReserveQueueOverlay() },
                        onCloseReserveQueue = { viewModel.closeReserveQueueOverlay() },
                        onRemoveReservedSong = { viewModel.removeFromReserveQueue(it) },
                        onPlayReservedSong = { viewModel.playFromReserveQueue(it) },
                        onToggleFavorites = { viewModel.toggleFavoritesOverlay() },
                        onCloseFavorites = { viewModel.closeFavoritesOverlay() },
                        onToggleFavoriteSong = { viewModel.toggleFavoriteSong(it) },
                        onOpenRecordingsDialog = { viewModel.openRecordingsDialog() },
                        onToggleSongbookFilter = { viewModel.toggleSongbookFilter(it) },
                        onOpenSongFolderLocator = { viewModel.openSongFolderPicker() },
                        onQuerySongbook = { viewModel.searchSongbook(it) },
                        onSelectSongFromBook = { viewModel.selectSong(it) },
                        onReserveSongFromBook = { viewModel.reserveSong(it) },
                        onOpenSoundFontDialog = { viewModel.openSoundFontDialog() },
                        onOpenBackgroundDialog = { viewModel.openBackgroundDialog() },
                        onOpenLyricStyleDialog = { viewModel.openLyricStyleDialog() },
                        onOpenAudioMixerDialog = { viewModel.openAudioMixerDialog() },
                        onOpenSettingsDialog = { viewModel.openSettingsDialog() },
                        onAdvanceBackground = { viewModel.advanceNextBackground() },
                        onPlay = { viewModel.onPlay() },
                        onNext = { viewModel.onNext() },
                        onStop = { viewModel.onStop() },
                        onDigit = { viewModel.onDigitPressed(it) },
                        onClear = { viewModel.onClearCode() },
                        onDelete = { viewModel.onDeleteDigit() },
                        onReserve = { viewModel.onReserve() },
                        onRButtonPressed = { viewModel.onRButtonPressed() },
                        onAdjustKey = { viewModel.adjustKey(it) },
                        onAdjustTempo = { viewModel.adjustTempo(it) },
                        onAdjustMelody = { viewModel.adjustMelody(it) },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Bottom Half: Videoke Machine Remote Console (Clean Keypad + Icon-only Controls)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    VideokeRemoteConsole(
                        state = state,
                        onDigit = { viewModel.onDigitPressed(it) },
                        onClear = { viewModel.onClearCode() },
                        onDelete = { viewModel.onDeleteDigit() },
                        onReserve = { viewModel.onRButtonPressed() },
                        onPlay = { viewModel.onPlay() },
                        onNext = { viewModel.onNext() },
                        onStop = { viewModel.onStop() },
                        onOpenSongbook = {
                            if (state.isSongbookOpen) viewModel.closeSongbook() else viewModel.openSongbook()
                        },
                        onToggleReserveQueue = { viewModel.toggleReserveQueueOverlay() },
                        onToggleFavorites = { viewModel.toggleFavoritesOverlay() },
                        onOpenRecordingsDialog = { viewModel.openRecordingsDialog() },
                        onOpenSoundFontDialog = { viewModel.openSoundFontDialog() },
                        onOpenBackgroundDialog = { viewModel.openBackgroundDialog() },
                        onOpenLyricStyleDialog = { viewModel.openLyricStyleDialog() },
                        onOpenAudioMixerDialog = { viewModel.openAudioMixerDialog() },
                        onToggleTvMode = { viewModel.enterTvLandscapeMode(activity) },
                        onOpenSettingsDialog = { viewModel.openSettingsDialog() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Audio Mixer Modal (Key / Tempo / Melody Guide) - Appears when pressing 🎚️ icon
        if (state.isAudioMixerDialogOpen) {
            AudioMixerDialog(
                state = state,
                isLandscape = isLandscape,
                onAdjustKey = { viewModel.adjustKey(it) },
                onAdjustTempo = { viewModel.adjustTempo(it) },
                onAdjustMelody = { viewModel.adjustMelody(it) },
                onDismiss = { viewModel.closeAudioMixerDialog() }
            )
        }

        // Voice + Music Recordings Dialog (🎙️ / 🔴)
        if (state.isRecordingsDialogOpen) {
            RecordingsDialog(
                state = state,
                isLandscape = isLandscape,
                onToggleRecording = { viewModel.toggleRecording() },
                onPlayRecording = { viewModel.playSavedRecording(it) },
                onDeleteRecording = { viewModel.deleteSavedRecording(it) },
                onDismiss = { viewModel.closeRecordingsDialog() }
            )
        }

        // App Settings Modal (Mic Scoring, Mic Sensitivity, Keypad Voice, QR Remote, etc.)
        if (state.isSettingsDialogOpen) {
            AppSettingsDialog(
                state = state,
                isLandscape = isLandscape,
                onToggleMicScoring = { viewModel.toggleMicScoring(it) },
                onAdjustMicSensitivity = { viewModel.setMicSensitivity(it) },
                onToggleKeypadVoice = { viewModel.toggleKeypadVoice(it) },
                onToggleTvMode = { viewModel.toggleTvMode(activity) },
                onAdjustMelody = { viewModel.adjustMelody(it) },
                onSelectLanguage = { viewModel.setAppLanguage(it) },
                onCheckForUpdates = { viewModel.checkForUpdates() },
                onDismiss = { viewModel.closeSettingsDialog() }
            )
        }

        // Automatic Over-The-Air Force-Update Modal (Works over Wi-Fi, Mobile Data, any network)
        if (state.updateState !is com.example.videokey.update.AppUpdateManager.UpdateState.Idle) {
            ForceUpdateDialog(
                updateState = state.updateState,
                isLandscape = isLandscape,
                onRetry = { viewModel.retryUpdate() },
                onInstall = { file -> viewModel.promptInstallDownloadedApk(file) },
                onDismiss = { viewModel.dismissUpdateDialog() }
            )
        }

        // Lyric Font & Style Customization Modal
        if (state.isLyricStyleDialogOpen) {
            LyricStyleDialog(
                config = state.lyricConfig,
                onSelectFontFamily = { viewModel.setLyricFontFamily(it) },
                onSelectWipeColor = { viewModel.setLyricWipeColor(it) },
                onSelectShadowStyle = { viewModel.setLyricShadowStyle(it) },
                onToggleAllCaps = { viewModel.toggleLyricAllCaps() },
                onToggleItalic = { viewModel.toggleLyricItalic() },
                onAdjustFontScale = { viewModel.setLyricFontScale(it) },
                onSelectLineCount = { viewModel.setLyricLineCount(it) },
                onSelectPosition = { viewModel.setLyricPosition(it) },
                onApplyPreset = { viewModel.applyLyricPreset(it) },
                onDismiss = { viewModel.closeLyricStyleDialog() }
            )
        }

        // Background Video / Image Selection Modal
        if (state.isBackgroundDialogOpen) {
            BackgroundSelectionDialog(
                backgrounds = state.availableBackgrounds,
                selectedBackground = state.selectedBackground,
                changeMode = state.backgroundChangeMode,
                currentBackgroundFolder = state.currentBackgroundFolder,
                isTagalog = state.appLanguage == com.example.videokey.ui.main.AppLanguage.TAGALOG,
                onOpenFolderLocator = { viewModel.openBgFolderPicker() },
                onSelectBackground = { viewModel.selectBackground(it) },
                onChangeMode = { viewModel.setBackgroundChangeMode(it) },
                onRescanMedia = { viewModel.scanBackgroundMedia() },
                onDismiss = { viewModel.closeBackgroundDialog() }
            )
        }

        // SoundFont Selection Modal
        if (state.isSoundFontDialogOpen) {
            SoundFontSelectionDialog(
                soundFonts = state.availableSoundFonts,
                activeSoundFont = state.activeSoundFontName,
                isTagalog = state.appLanguage == com.example.videokey.ui.main.AppLanguage.TAGALOG,
                onSelectSoundFont = { viewModel.selectSoundFont(it) },
                onScanStorage = { viewModel.scanInternalStorage(context) },
                onDismiss = { viewModel.closeSoundFontDialog() }
            )
        }

        // Song Folder Browser Dialog
        if (state.isSongFolderPickerOpen) {
            FolderBrowserDialog(
                title = "📁 PILIIN ANG SONG FOLDER (MGA KANTA)",
                initialPath = state.currentSongFolder,
                onFolderSelected = { folderPath ->
                    viewModel.setSongFolder(folderPath)
                    viewModel.closeSongFolderPicker()
                },
                onDismiss = { viewModel.closeSongFolderPicker() }
            )
        }

        // Background Folder Browser Dialog
        if (state.isBgFolderPickerOpen) {
            FolderBrowserDialog(
                title = "🎬 PILIIN ANG BACKGROUND FOLDER",
                initialPath = state.currentBackgroundFolder,
                onFolderSelected = { folderPath ->
                    viewModel.setBackgroundFolder(folderPath)
                    viewModel.closeBgFolderPicker()
                },
                onDismiss = { viewModel.closeBgFolderPicker() }
            )
        }

        // Score Reveal Modal
        if (state.showScoreModal) {
            ScoreCelebrationDialog(
                score = state.score,
                isTagalog = state.appLanguage == com.example.videokey.ui.main.AppLanguage.TAGALOG,
                onDismiss = { viewModel.closeScoreModal() }
            )
        }
    }
}

/**
 * Authentic Arcade/Videoke Single Countdown Badge.
 * Positioned right above the first letter of the active lyrics.
 * Stays in one single position and cycles numbers (4 -> 3 -> 2 -> 1) with pulse animation.
 */
@Composable
fun SingleCountdownBadge(
    number: Int,
    isLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    if (number <= 0) return

    val infiniteTransition = rememberInfiniteTransition(label = "singPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(260, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "singPulseScale"
    )

    val badgeSize = if (isLandscape) 26.dp else 22.dp
    val badgeFont = if (isLandscape) 15.sp else 12.sp

    Box(
        modifier = modifier
            .scale(pulseScale)
            .size(badgeSize)
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFFF1744), Color(0xFFD50000))
                )
            )
            .border(1.5.dp, Color(0xFFFFEB3B), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$number",
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            fontSize = badgeFont,
            color = Color.White,
            style = TextStyle(
                shadow = Shadow(
                    color = Color(0xFFFF1744),
                    offset = Offset.Zero,
                    blurRadius = 6f
                )
            )
        )
    }
}

/**
 * Authentic Videoke "INSTRUMENTAL" Center Screen Display
 * Shown in the center of the TV when musical intro or interlude is playing.
 * Slightly smaller and dead-centered in Landscape mode as requested!
 */
@Composable
fun InstrumentalDisplay(
    isLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "instPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "instAlpha"
    )

    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(if (isLandscape) 18.dp else 16.dp))
            .border(1.5.dp, Color(0xFF00E5FF).copy(alpha = pulseAlpha), RoundedCornerShape(if (isLandscape) 18.dp else 16.dp))
            .padding(horizontal = if (isLandscape) 16.dp else 12.dp, vertical = if (isLandscape) 6.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 8.dp else 6.dp)
    ) {
        Text(
            text = "🎷",
            fontSize = if (isLandscape) 14.sp else 12.sp
        )
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "★  I N S T R U M E N T A L  ★",
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = if (isLandscape) 2.sp else 1.5.sp,
                fontSize = if (isLandscape) 14.sp else 12.sp,
                style = TextStyle(
                    color = Color.Black,
                    drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 4f,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                    )
                )
            )
            Text(
                text = "★  I N S T R U M E N T A L  ★",
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = if (isLandscape) 2.sp else 1.5.sp,
                fontSize = if (isLandscape) 14.sp else 12.sp,
                color = Color(0xFF00E5FF)
            )
        }
        Text(
            text = "🎸",
            fontSize = if (isLandscape) 14.sp else 12.sp
        )
    }
}

/**
 * Authentic Videoke Song Intro Splash:
 * Centered on the TV screen when a song starts, showcasing the Song Title & Artist in gold/cyan arcade styling.
 */
@Composable
fun SongIntroTitleSplash(
    song: Song,
    isLandscape: Boolean,
    lang: com.example.videokey.ui.main.AppLanguage = com.example.videokey.ui.main.AppLanguage.TAGALOG,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "introGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "titleGlowAlpha"
    )

    // Dynamic auto-adjust font size for title: starts BIG and scales down if overflowing screen
    var titleFitScale by remember(song.title) { mutableStateOf(1f) }
    val baseTitleSize = if (isLandscape) 44.sp else 34.sp
    val titleFontSize = baseTitleSize * titleFitScale

    // Dynamic auto-adjust font size for artist:
    var artistFitScale by remember(song.artist) { mutableStateOf(1f) }
    val baseArtistSize = if (isLandscape) 22.sp else 18.sp
    val artistFontSize = baseArtistSize * artistFitScale

    val isTagalog = lang == com.example.videokey.ui.main.AppLanguage.TAGALOG

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = if (isLandscape) 32.dp else 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .shadow(20.dp, RoundedCornerShape(22.dp))
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xF50A0F1D),
                            Color(0xF5131E33),
                            Color(0xF5070B14)
                        )
                    )
                )
                .border(
                    2.5.dp,
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFFFFD54F).copy(alpha = glowAlpha),
                            Color(0xFF00E5FF).copy(alpha = glowAlpha),
                            Color(0xFFFFD54F).copy(alpha = glowAlpha)
                        )
                    ),
                    RoundedCornerShape(22.dp)
                )
                .padding(
                    horizontal = if (isLandscape) 32.dp else 18.dp,
                    vertical = if (isLandscape) 22.dp else 16.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Song Number Pill
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFD54F))
                ) {
                    Text(
                        text = if (isTagalog) "★ KANTA BLG. ${song.code} ★" else "★ SONG NO. ${song.code} ★",
                        color = Color(0xFFFFD54F),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = if (isLandscape) 14.sp else 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }

                // Big Centered Auto-Adjusting Song Title
                Text(
                    text = song.title.uppercase(),
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = titleFontSize,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    softWrap = true,
                    onTextLayout = { result ->
                        if ((result.didOverflowWidth || result.didOverflowHeight) && titleFitScale > 0.4f) {
                            titleFitScale *= 0.90f
                        }
                    },
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color(0xFF00E5FF).copy(alpha = 0.95f),
                            offset = Offset.Zero,
                            blurRadius = 18f
                        )
                    )
                )

                // Big Centered Auto-Adjusting Artist
                Text(
                    text = song.artist.uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = artistFontSize,
                    color = Color(0xFFFFD54F),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    onTextLayout = { result ->
                        if (result.didOverflowWidth && artistFitScale > 0.5f) {
                            artistFitScale *= 0.92f
                        }
                    },
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color(0xFFFF9100).copy(alpha = 0.85f),
                            offset = Offset(2f, 2f),
                            blurRadius = 6f
                        )
                    )
                )

                Text(
                    text = if (isTagalog) "PHILIPPINE VIDEOKEY • HUMANDA SA PAGKANTA" else "PHILIPPINE VIDEOKEY • GET READY TO SING",
                    fontWeight = FontWeight.Black,
                    fontSize = if (isLandscape) 11.sp else 9.sp,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 1.5.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Clean Bold Header Text for Landscape and Portrait modes without stroke outlines.
 */
@Composable
fun OutlinedHeaderText(
    text: String,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    isLandscape: Boolean,
    fontFamily: FontFamily = FontFamily.Default,
    maxLines: Int = 1,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = color,
        fontWeight = FontWeight.Bold,
        fontFamily = fontFamily,
        fontSize = fontSize,
        maxLines = maxLines,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/**
 * Authentic Videoke TV Display with Background Video/Image support,
 * 4-line lyrics in portrait, and 2-line lower-third in landscape!
 */
@Composable
fun VideokeTvScreen(
    state: VideoKeyUiState,
    isLandscape: Boolean = false,
    onToggleTvMode: () -> Unit,
    onTriggerScore: () -> Unit,
    onOpenSongbook: () -> Unit,
    onCloseSongbook: () -> Unit = {},
    onToggleReserveQueue: () -> Unit = {},
    onCloseReserveQueue: () -> Unit = {},
    onRemoveReservedSong: (Int) -> Unit = {},
    onPlayReservedSong: (Int) -> Unit = {},
    onToggleFavorites: () -> Unit = {},
    onCloseFavorites: () -> Unit = {},
    onToggleFavoriteSong: (Song) -> Unit = {},
    onOpenRecordingsDialog: () -> Unit = {},
    onToggleSongbookFilter: (Boolean) -> Unit = {},
    onOpenSongFolderLocator: () -> Unit = {},
    onQuerySongbook: (String) -> Unit = {},
    onSelectSongFromBook: (Song) -> Unit = {},
    onReserveSongFromBook: (Song) -> Unit = {},
    onOpenSoundFontDialog: () -> Unit = {},
    onOpenBackgroundDialog: () -> Unit = {},
    onOpenLyricStyleDialog: () -> Unit = {},
    onOpenAudioMixerDialog: () -> Unit = {},
    onOpenSettingsDialog: () -> Unit = {},
    onAdvanceBackground: () -> Unit = {},
    onPlay: () -> Unit = {},
    onNext: () -> Unit = {},
    onStop: () -> Unit = {},
    onDigit: (Char) -> Unit = {},
    onClear: () -> Unit = {},
    onDelete: () -> Unit = {},
    onReserve: () -> Unit = {},
    onRButtonPressed: () -> Unit = {},
    onAdjustKey: (Int) -> Unit = {},
    onAdjustTempo: (Int) -> Unit = {},
    onAdjustMelody: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isTagalog = state.appLanguage == com.example.videokey.ui.main.AppLanguage.TAGALOG
    var showTvKeypadModal by remember { mutableStateOf(false) }

    // Auto-hide top-right buttons in Landscape Mode after 3.8s of inactivity
    var showLandscapeControls by remember { mutableStateOf(true) }
    var interactionTick by remember { mutableStateOf(0) }
    val isAnyOverlayOpen = state.isSongbookOpen || state.isReserveQueueOpen || state.isFavoritesOpen ||
        showTvKeypadModal || state.isSettingsDialogOpen || state.isAudioMixerDialogOpen ||
        state.isSoundFontDialogOpen || state.isBackgroundDialogOpen || state.isLyricStyleDialogOpen ||
        state.isRecordingsDialogOpen

    LaunchedEffect(isLandscape, interactionTick, isAnyOverlayOpen) {
        if (isLandscape && !isAnyOverlayOpen) {
            showLandscapeControls = true
            kotlinx.coroutines.delay(3800)
            showLandscapeControls = false
        } else {
            showLandscapeControls = true
        }
    }

    // Alternating ticker between PLAYING: and NEXT:
    var showNextInHeader by remember { mutableStateOf(false) }
    LaunchedEffect(state.reserveQueue.size, state.isPlaying) {
        if (state.isPlaying && state.reserveQueue.isNotEmpty()) {
            while (true) {
                kotlinx.coroutines.delay(3500)
                showNextInHeader = !showNextInHeader
            }
        } else {
            showNextInHeader = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(isLandscape) {
                if (isLandscape) {
                    detectTapGestures {
                        showLandscapeControls = true
                        interactionTick++
                    }
                }
            }
    ) {
        // LAYER 1: Background Video / Image / Stage
        val mediaCount = state.availableBackgrounds.count { it.type == BackgroundType.VIDEO || it.type == BackgroundType.IMAGE }
        VideokeBackgroundLayer(
            background = state.selectedBackground,
            hasMultipleMedia = mediaCount > 1,
            onBackgroundCompleted = onAdvanceBackground,
            modifier = Modifier.fillMaxSize()
        )

        // LAYER 2: Foreground UI (OSD Header, Songbook/Reserve/Favorites Overlay, Lyrics)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .displayCutoutPadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP BANNER (OSD Header & Queue Ticker)
            Column(modifier = Modifier.fillMaxWidth()) {

                // Clean Header Bar (NO BOX, NO BACKGROUND, Transparent in both Portrait & Landscape)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        val hasReserve = state.reserveQueue.isNotEmpty()
                        val currentSongText = "${state.currentSong.title} • ${state.currentSong.artist}".uppercase()
                        val nextSongText = if (hasReserve) {
                            "${state.reserveQueue.first().title} • ${state.reserveQueue.first().artist}".uppercase()
                        } else ""

                        val isShowingNext = hasReserve && showNextInHeader

                        val badgeLabel = when {
                            !state.isPlaying && hasReserve -> if (isTagalog) "SUSUNOD:" else "NEXT:"
                            !state.isPlaying -> "★ VIDEOKEY:"
                            isShowingNext -> if (isTagalog) "SUSUNOD:" else "NEXT:"
                            else -> if (isTagalog) "TUMUTUGTOG:" else "PLAYING:"
                        }

                        val badgeColor = when {
                            badgeLabel.contains("TUMUTUGTOG") || badgeLabel.contains("PLAYING") -> Color(0xFF00FF88)
                            badgeLabel.contains("SUSUNOD") || badgeLabel.contains("NEXT") -> Color(0xFF38BDF8)
                            else -> Color(0xFFFFD54F)
                        }

                        // Pure Text Badge (Bold + Black Outline in Landscape!)
                        OutlinedHeaderText(
                            text = "$badgeLabel ",
                            color = badgeColor,
                            fontSize = if (isLandscape) 13.sp else 11.5.sp,
                            isLandscape = isLandscape,
                            fontFamily = FontFamily.Monospace
                        )

                        val headerMarquee = when {
                            !state.isPlaying && hasReserve -> nextSongText
                            !state.isPlaying -> {
                                val selectText = if (isTagalog) "PUMILI NG KANTA" else "SELECT A SONG"
                                "PHILIPPINE VIDEOKEY SYSTEM • $selectText"
                            }
                            isShowingNext -> nextSongText
                            else -> {
                                if (hasReserve) {
                                    val nextWord = if (isTagalog) "SUSUNOD:" else "NEXT:"
                                    "$currentSongText     ★★★     $nextWord $nextSongText"
                                } else {
                                    currentSongText
                                }
                            }
                        }

                        OutlinedHeaderText(
                            text = headerMarquee,
                            color = Color.White,
                            fontSize = if (isLandscape) 13.sp else 12.sp,
                            isLandscape = isLandscape,
                            modifier = Modifier
                                .weight(1f)
                                .basicMarquee(iterations = Int.MAX_VALUE)
                        )
                    }

                    // Active Recording Badge (if recording in portrait or landscape)
                    if (state.isRecording) {
                        val recMins = state.recordingSeconds / 60
                        val recSecs = state.recordingSeconds % 60
                        val recLabel = String.format("🔴 REC %02d:%02d", recMins, recSecs)
                        Box(
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .clickable { onOpenRecordingsDialog() }
                        ) {
                            OutlinedHeaderText(
                                text = recLabel,
                                color = Color(0xFFFF5252),
                                fontSize = if (isLandscape) 12.sp else 11.sp,
                                isLandscape = isLandscape,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Top-Right Horizontal TV Mode Icon Controls in Landscape Mode (Auto-Hiding!)
                    if (isLandscape) {
                        AnimatedVisibility(visible = showLandscapeControls) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 8.dp, end = 4.dp)
                            ) {
                                TvHeaderIconButton(
                                    icon = "📖",
                                    borderColor = Color(0xFFFFD54F),
                                    isActive = state.isSongbookOpen,
                                    onClick = {
                                        interactionTick++
                                        onOpenSongbook()
                                    }
                                )
                                TvHeaderIconButton(
                                    icon = "R",
                                    borderColor = Color(0xFFFFD54F),
                                    isActive = state.isReserveQueueOpen,
                                    onClick = {
                                        interactionTick++
                                        onToggleReserveQueue()
                                    }
                                )
                                TvHeaderIconButton(
                                    icon = "❤️",
                                    borderColor = Color(0xFFFF4081),
                                    isActive = state.isFavoritesOpen,
                                    onClick = {
                                        interactionTick++
                                        onToggleFavorites()
                                    }
                                )
                                TvHeaderIconButton(
                                    icon = if (state.isRecording) "🔴" else "🎙️",
                                    borderColor = Color(0xFFFF5252),
                                    isActive = state.isRecording || state.isRecordingsDialogOpen,
                                    onClick = {
                                        interactionTick++
                                        onOpenRecordingsDialog()
                                    }
                                )
                                TvHeaderIconButton(
                                    icon = "🎛️",
                                    borderColor = Color(0xFF00E676),
                                    isActive = state.isSoundFontDialogOpen,
                                    onClick = {
                                        interactionTick++
                                        onOpenSoundFontDialog()
                                    }
                                )
                                TvHeaderIconButton(
                                    icon = "🔤",
                                    borderColor = Color(0xFF818CF8),
                                    isActive = state.isLyricStyleDialogOpen,
                                    onClick = {
                                        interactionTick++
                                        onOpenLyricStyleDialog()
                                    }
                                )
                                TvHeaderIconButton(
                                    icon = "🎚️",
                                    borderColor = Color(0xFF00E5FF),
                                    isActive = state.isAudioMixerDialogOpen,
                                    onClick = {
                                        interactionTick++
                                        onOpenAudioMixerDialog()
                                    }
                                )
                                TvHeaderIconButton(
                                    icon = "🎬",
                                    borderColor = Color(0xFFFFB300),
                                    isActive = state.isBackgroundDialogOpen,
                                    onClick = {
                                        interactionTick++
                                        onOpenBackgroundDialog()
                                    }
                                )
                                TvHeaderIconButton(
                                    icon = "📱",
                                    borderColor = Color(0xFF00FF88),
                                    isActive = showTvKeypadModal,
                                    onClick = {
                                        interactionTick++
                                        showTvKeypadModal = true
                                    }
                                )
                                TvHeaderIconButton(
                                    icon = "⚙️",
                                    borderColor = Color(0xFF38BDF8),
                                    isActive = state.isSettingsDialogOpen,
                                    onClick = {
                                        interactionTick++
                                        onOpenSettingsDialog()
                                    }
                                )
                                TvHeaderIconButton(
                                    icon = "◀",
                                    borderColor = Color(0xFF94A3B8),
                                    isActive = false,
                                    onClick = onToggleTvMode
                                )
                            }
                        }

                        // When right buttons auto-hide in Landscape, show Bold Black-Outlined Top-Right Header Info
                        if (!showLandscapeControls) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(start = 10.dp, end = 4.dp)
                                    .clickable {
                                        showLandscapeControls = true
                                        interactionTick++
                                    }
                            ) {
                                OutlinedHeaderText(
                                    text = "R: ${state.reserveQueue.size}  •  ❤️ ${state.favoriteSongs.size}",
                                    color = Color(0xFFFFD54F),
                                    fontSize = 12.sp,
                                    isLandscape = true,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                // Sub-header Ticker: R: (Yellow R without circle on the side, matching reserved codes)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = if (isLandscape) 0.dp else 2.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val reserveCodes = state.reserveQueue.joinToString(separator = "   ") { it.code }
                    OutlinedHeaderText(
                        text = "R: ",
                        color = Color(0xFFFFD54F),
                        fontSize = if (isLandscape) 12.sp else 11.sp,
                        isLandscape = isLandscape,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.clickable { onToggleReserveQueue() }
                    )
                    OutlinedHeaderText(
                        text = reserveCodes,
                        color = Color(0xFFFFD54F),
                        fontSize = if (isLandscape) 12.sp else 11.sp,
                        isLandscape = isLandscape,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .weight(1f)
                            .basicMarquee(iterations = Int.MAX_VALUE)
                            .clickable { onToggleReserveQueue() }
                    )
                }
            }

            // WHEN SONGBOOK, RESERVED QUEUE (R), OR FAVORITES (❤️) OVERLAY IS OPEN:
            // Show the form in the upper area and automatically switch lyrics to 2 lines at the bottom so they are NEVER covered!
            val showUpperOverlay = state.isSongbookOpen || state.isReserveQueueOpen || state.isFavoritesOpen
            if (showUpperOverlay) {
                SongbookSearchOverlay(
                    songs = state.searchResults,
                    reserveQueue = state.reserveQueue,
                    favoriteSongs = state.favoriteSongs,
                    favoriteCodes = state.favoriteCodes,
                    activeTab = when {
                        state.isReserveQueueOpen -> 1
                        state.isFavoritesOpen -> 2
                        else -> 0
                    },
                    currentSongFolder = state.currentSongFolder,
                    scanStatus = state.scanStatus,
                    isScanning = state.isScanning,
                    onlyShowPlayableSongs = state.onlyShowPlayableSongs,
                    playableCodes = state.playableCodes,
                    isTagalog = isTagalog,
                    isLandscape = isLandscape,
                    onSwitchTab = { tab ->
                        when (tab) {
                            0 -> if (!state.isSongbookOpen) onOpenSongbook()
                            1 -> if (!state.isReserveQueueOpen) onToggleReserveQueue()
                            2 -> if (!state.isFavoritesOpen) onToggleFavorites()
                        }
                    },
                    onToggleFilter = onToggleSongbookFilter,
                    onOpenFolderLocator = onOpenSongFolderLocator,
                    onQueryChange = onQuerySongbook,
                    onSelectSong = onSelectSongFromBook,
                    onReserveSong = onReserveSongFromBook,
                    onToggleFavoriteSong = onToggleFavoriteSong,
                    onPlayReservedIndex = onPlayReservedSong,
                    onRemoveReservedIndex = onRemoveReservedSong,
                    onDismiss = {
                        onCloseSongbook()
                        onCloseReserveQueue()
                        onCloseFavorites()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = if (isLandscape) 16.dp else 12.dp, vertical = 2.dp)
                )

                if (state.isPlaying) {
                    // Automatic 2-Line Lyrics moved down below the form so it is never covered!
                    val isTopActive = state.activeLyricPosition == ActiveLyricPosition.TOP
                    val isBottomActive = state.activeLyricPosition == ActiveLyricPosition.BOTTOM
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = if (isLandscape) 20.dp else 10.dp,
                                end = if (isLandscape) 20.dp else 10.dp,
                                top = if (state.countdownDots > 0) 18.dp else 2.dp,
                                bottom = if (isLandscape) 10.dp else 4.dp
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        KaraokeLyricLine(
                            text = state.landscapeLine1,
                            isActive = isTopActive,
                            wipePercent = if (isTopActive) state.wipePercent else 0f,
                            fontSize = if (isLandscape) (25.sp * state.lyricConfig.fontScale) else (21.sp * state.lyricConfig.fontScale),
                            lyricConfig = state.lyricConfig,
                            countdownNumber = state.countdownDots,
                            isLandscape = isLandscape
                        )
                        KaraokeLyricLine(
                            text = state.landscapeLine2,
                            isActive = isBottomActive,
                            wipePercent = if (isBottomActive) state.wipePercent else 0f,
                            fontSize = if (isLandscape) (25.sp * state.lyricConfig.fontScale) else (21.sp * state.lyricConfig.fontScale),
                            lyricConfig = state.lyricConfig,
                            countdownNumber = 0,
                            isLandscape = isLandscape
                        )
                    }
                }
            } else if (!state.isPlaying) {
                // STANDBY / WELCOME SCREEN: SELECT A SONG & Advancing 6-Digit Slots
                WelcomeSongSelectionScreen(
                    state = state,
                    isLandscape = isLandscape,
                    onPlay = onPlay,
                    onReserve = onReserve,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            } else if (state.showSongTitleIntro) {
                // Centered Song Title & Artist Intro Banner on Song Selection
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    SongIntroTitleSplash(
                        song = state.currentSong,
                        isLandscape = isLandscape,
                        lang = state.appLanguage
                    )
                }
            } else if (state.isInstrumental) {
                // INSTRUMENTAL PLAYING (NO MELODY/SINGING) -> DEAD CENTER OF PLAYER SCREEN
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    InstrumentalDisplay(isLandscape = isLandscape)
                }
            } else {
                // PLAYING SCREEN WITH VOCALS / LYRICS
                val topPadding = when (state.lyricConfig.position) {
                    LyricScreenPosition.TOP -> if (isLandscape) 12.dp else 6.dp
                    LyricScreenPosition.CENTER -> 0.dp
                    LyricScreenPosition.BOTTOM -> 0.dp
                }

                val bottomPadding = if (isLandscape) {
                    52.dp // Elevated comfortably above bottom edge
                } else {
                    when (state.lyricConfig.position) {
                        LyricScreenPosition.BOTTOM -> 8.dp
                        else -> 0.dp
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = topPadding, bottom = bottomPadding),
                    contentAlignment = when {
                        isLandscape -> Alignment.BottomCenter
                        state.lyricConfig.position == LyricScreenPosition.TOP -> Alignment.TopCenter
                        state.lyricConfig.position == LyricScreenPosition.CENTER -> Alignment.Center
                        else -> Alignment.BottomCenter
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = if (isLandscape) 24.dp else 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // LYRIC DISPLAY: 2 LINES or 4 LINES (CLEAN, NO ENCLOSING BOX!)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = if (isLandscape) 18.dp else 8.dp,
                                    end = if (isLandscape) 18.dp else 8.dp,
                                    top = if (state.countdownDots > 0) 24.dp else 0.dp
                                )
                        ) {
                            val showFourLines = !isLandscape && state.lyricConfig.lineCount == LyricLineCount.FOUR_LINES
                            if (showFourLines) {
                                // 4 LINES DISPLAY (Portrait only if user selected 4 lines)
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val linesList = listOf(
                                        Pair(state.line1, 1),
                                        Pair(state.line2, 2),
                                        Pair(state.line3, 3),
                                        Pair(state.line4, 4)
                                    )
                                    linesList.forEach { (lineText, lineNum) ->
                                        if (lineText.isNotEmpty()) {
                                            val isActive = state.activeLineNumber == lineNum
                                            val lineCountdown = if (lineNum == 1) state.countdownDots else 0
                                            KaraokeLyricLine(
                                                text = lineText,
                                                isActive = isActive,
                                                wipePercent = if (isActive) state.wipePercent else 0f,
                                                fontSize = if (isLandscape) (20.sp * state.lyricConfig.fontScale) else (22.sp * state.lyricConfig.fontScale),
                                                lyricConfig = state.lyricConfig,
                                                countdownNumber = lineCountdown,
                                                isLandscape = isLandscape
                                            )
                                        }
                                    }
                                }
                            } else {
                                // 2 LINES DISPLAY (DEFAULT)
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val isTopActive = state.activeLyricPosition == ActiveLyricPosition.TOP
                                    val isBottomActive = state.activeLyricPosition == ActiveLyricPosition.BOTTOM
                                    val topCountdown = state.countdownDots
                                    val bottomCountdown = 0

                                    KaraokeLyricLine(
                                        text = state.landscapeLine1,
                                        isActive = isTopActive,
                                        wipePercent = if (isTopActive) state.wipePercent else 0f,
                                        fontSize = if (isLandscape) (32.sp * state.lyricConfig.fontScale) else (28.sp * state.lyricConfig.fontScale),
                                        lyricConfig = state.lyricConfig,
                                        countdownNumber = topCountdown,
                                        isLandscape = isLandscape
                                    )
                                    KaraokeLyricLine(
                                        text = state.landscapeLine2,
                                        isActive = isBottomActive,
                                        wipePercent = if (isBottomActive) state.wipePercent else 0f,
                                        fontSize = if (isLandscape) (32.sp * state.lyricConfig.fontScale) else (28.sp * state.lyricConfig.fontScale),
                                        lyricConfig = state.lyricConfig,
                                        countdownNumber = bottomCountdown,
                                        isLandscape = isLandscape
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // FIXED FULL-WIDTH SONG / RESERVE BOX ABOVE NUMBERING WHILE PLAYING:
        // Aligned with the 12.dp horizontal button padding, never shifts lyrics or resizes when appearing,
        // text auto-centers inside, and tapping R RESERVE automatically reserves the song!
        if (state.isPlaying && state.enteredCode.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .height(46.dp)
                    .shadow(8.dp, RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xF20A1910))
                    .border(1.5.dp, Color(0xFF00FF88), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Entered 6-digit code
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🔢", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = state.enteredCode.padStart(6, '0'),
                            color = Color(0xFF00FF88),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }

                    // Center: Auto-Centered Song Title or Helper Text
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.matchedEnteredSong != null) {
                            Text(
                                text = "${state.matchedEnteredSong.title.uppercase()} • ${state.matchedEnteredSong.artist.uppercase()}",
                                color = Color(0xFFFFD54F),
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .basicMarquee(iterations = Int.MAX_VALUE)
                            )
                        } else if (state.enteredCode.length < 6) {
                            Text(
                                text = if (isTagalog) "Kulang ng ${6 - state.enteredCode.length} digits" else "Need ${6 - state.enteredCode.length} more digits",
                                color = Color(0xFFA5B4FC),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(
                                text = if (isTagalog) "Walang kanta sa code" else "Song code not found",
                                color = Color(0xFFFF8A80),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Right: Clickable Automatic Reserve (R RESERVE) & Play buttons!
                    if (state.matchedEnteredSong != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF332000))
                                    .border(1.5.dp, Color(0xFFFFD54F), RoundedCornerShape(6.dp))
                                    .clickable { onReserve() }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isTagalog) "R IRESERBA" else "R RESERVE",
                                    color = Color(0xFFFFD54F),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.5.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF00381B))
                                    .border(1.dp, Color(0xFF00FF88), RoundedCornerShape(6.dp))
                                    .clickable { onPlay() }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "▶",
                                    color = Color(0xFF00FF88),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // TV Keypad Dialog
        if (showTvKeypadModal) {
            TvKeypadDialog(
                state = state,
                onDigit = onDigit,
                onClear = onClear,
                onDelete = onDelete,
                onReserve = onReserve,
                onPlay = onPlay,
                onDismiss = { showTvKeypadModal = false }
            )
        }
    }
}

@Composable
fun TvHeaderIconButton(
    icon: String,
    borderColor: Color,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = if (isActive) borderColor.copy(alpha = 0.35f) else Color(0xB30F172A),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isActive) 2.dp else 1.2.dp,
            color = borderColor
        ),
        modifier = Modifier
            .size(34.dp)
            .shadow(4.dp, CircleShape)
            .clickable { onClick() }
            .focusable()
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = icon,
                color = if (icon == "R") Color(0xFFFFD54F) else Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

/**
 * Authentic Arcade Welcome Screen: "SELECT A SONG" with Advancing 6-Digit Slots
 * Fixed Full-Box Reserve/Song container above the numbering that is 100% aligned with the keypad buttons
 * and never shifts or resizes when appearing, with auto-centered text inside!
 */
@Composable
fun WelcomeSongSelectionScreen(
    state: VideoKeyUiState,
    isLandscape: Boolean,
    onPlay: () -> Unit = {},
    onReserve: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseBorderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorPulse"
    )

    val isTagalog = state.appLanguage == com.example.videokey.ui.main.AppLanguage.TAGALOG

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = if (isLandscape) 24.dp else 12.dp, vertical = if (isLandscape) 6.dp else 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Upper & Middle Area: SELECT A SONG + 6 Advancing Digit Slots
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Glowing Arcade Header: ★ SELECT A SONG ★
            val titleText = if (isTagalog) "PUMILI NG KANTA" else "SELECT A SONG"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "★",
                        fontWeight = FontWeight.Black,
                        fontSize = if (isLandscape) 28.sp else 20.sp,
                        style = TextStyle(
                            color = Color.Black,
                            drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = if (isLandscape) 5.5f else 4f,
                                join = androidx.compose.ui.graphics.StrokeJoin.Round
                            )
                        )
                    )
                    Text(
                        text = "★",
                        color = Color(0xFFFFD54F),
                        fontSize = if (isLandscape) 28.sp else 20.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Box(contentAlignment = Alignment.Center) {
                    // Crisp Black Outline (NO BLURRY SHADOW)
                    Text(
                        text = titleText,
                        fontWeight = FontWeight.Black,
                        fontSize = if (isLandscape) 32.sp else 22.sp,
                        letterSpacing = 2.sp,
                        style = TextStyle(
                            color = Color.Black,
                            drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = if (isLandscape) 6.5f else 4.8f,
                                join = androidx.compose.ui.graphics.StrokeJoin.Round
                            )
                        )
                    )
                    // Yellow Text Fill
                    Text(
                        text = titleText,
                        color = Color(0xFFFFD54F),
                        fontWeight = FontWeight.Black,
                        fontSize = if (isLandscape) 32.sp else 22.sp,
                        letterSpacing = 2.sp
                    )
                }
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "★",
                        fontWeight = FontWeight.Black,
                        fontSize = if (isLandscape) 28.sp else 20.sp,
                        style = TextStyle(
                            color = Color.Black,
                            drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = if (isLandscape) 5.5f else 4f,
                                join = androidx.compose.ui.graphics.StrokeJoin.Round
                            )
                        )
                    )
                    Text(
                        text = "★",
                        color = Color(0xFFFFD54F),
                        fontSize = if (isLandscape) 28.sp else 20.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Text(
                text = if (isTagalog) "ITIPA ANG 6-DIGIT NA NUMERO SA KEYPAD" else "ENTER 6-DIGIT SONG NUMBER ON KEYPAD",
                color = Color(0xFF81D4FA),
                fontSize = if (isLandscape) 12.sp else 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = if (isLandscape) 10.dp else 6.dp)
            )

            // 6 Advancing Digit Slots: Starts from Right Side [ 0 ] [ 0 ] [ 0 ] [ 0 ] [ 0 ] [ 0 ]
            Row(
                horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 10.dp else 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val code = state.enteredCode
                val paddedCode = if (code.isEmpty()) "000000" else code.padStart(6, '0')
                for (i in 0 until 6) {
                    val distFromRight = 5 - i
                    val isFilled = distFromRight < code.length
                    val isActiveSlot = if (code.isEmpty()) (i == 5) else (distFromRight == code.length)
                    val digitChar = paddedCode[i]

                    val borderColor = when {
                        isActiveSlot -> Color(0xFFFFD54F).copy(alpha = pulseBorderAlpha)
                        isFilled -> Color(0xFF00FF88)
                        else -> Color(0xFF1E293B)
                    }

                    val digitColor = when {
                        isFilled -> Color(0xFF00FF88)
                        isActiveSlot -> Color(0xFFFFD54F).copy(alpha = pulseBorderAlpha * 0.7f + 0.3f)
                        else -> Color(0xFF0A3E24) // Ghost unlit 0 segment
                    }

                    val boxBg = when {
                        isActiveSlot -> Color(0xFF131D2E)
                        isFilled -> Color(0xFF021E11)
                        else -> Color(0xFF060B12)
                    }

                    Box(
                        modifier = Modifier
                            .size(
                                width = if (isLandscape) 52.dp else 40.dp,
                                height = if (isLandscape) 60.dp else 46.dp
                            )
                            .shadow(if (isActiveSlot || isFilled) 6.dp else 2.dp, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .background(boxBg)
                            .border(
                                width = if (isActiveSlot) 2.dp else 1.5.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "$digitChar",
                                color = digitColor,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = if (isLandscape) 30.sp else 22.sp,
                                style = TextStyle(
                                    shadow = if (isFilled || isActiveSlot) {
                                        Shadow(
                                            color = (if (isFilled) Color(0xFF00FF88) else Color(0xFFFFD54F)).copy(alpha = 0.8f),
                                            offset = Offset(0f, 0f),
                                            blurRadius = 8f
                                        )
                                    } else null
                                )
                            )

                            // Active slot pulsing cursor underline
                            if (isActiveSlot) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .width(if (isLandscape) 20.dp else 14.dp)
                                        .height(2.5.dp)
                                        .background(Color(0xFFFFD54F).copy(alpha = pulseBorderAlpha), RoundedCornerShape(1.dp))
                                )
                            }
                        }
                    }
                }
            }
        }

        // FIXED FULL-BOX RESERVE / SONG PREVIEW CONTAINER ABOVE THE NUMBERING KEYPAD:
        // Always full-width (aligned with the 12.dp keypad margin), fixed height so it never shifts layout,
        // and only the text inside auto-centers!
        val boxBorderColor = when {
            state.matchedEnteredSong != null -> Color(0xFF00E676)
            state.alertMessage != null -> Color(0xFFEF4444)
            state.enteredCode.isNotEmpty() -> Color(0xFF6366F1)
            else -> Color(0xFF334155)
        }
        val boxBgColor = when {
            state.matchedEnteredSong != null -> Color(0xEE0A1E14)
            state.alertMessage != null -> Color(0xEE450A0A)
            state.enteredCode.isNotEmpty() -> Color(0xEE131633)
            else -> Color(0xCC0B1120)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth(if (isLandscape) 0.78f else 1f)
                .height(if (isLandscape) 66.dp else 58.dp)
                .shadow(6.dp, RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .background(boxBgColor)
                .border(1.5.dp, boxBorderColor, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            if (state.matchedEnteredSong != null) {
                val song = state.matchedEnteredSong
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "🎵 ${song.code}: ${song.title.uppercase()}",
                        color = Color(0xFFFFD54F),
                        fontWeight = FontWeight.Black,
                        fontSize = if (isLandscape) 16.sp else 13.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee(iterations = Int.MAX_VALUE)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "👤 ${song.artist.uppercase()}",
                        color = Color(0xFFE2E8F0),
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isLandscape) 13.sp else 11.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else if (state.alertMessage != null) {
                Text(
                    text = state.alertMessage,
                    color = Color(0xFFFFCDD2),
                    fontSize = if (isLandscape) 13.sp else 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (state.enteredCode.isNotEmpty()) {
                Text(
                    text = if (state.enteredCode.length == 6) {
                        if (isTagalog) "⚠️ Walang kanta para sa ${state.enteredCode} • Buksan ang 📖 SONGBOOK" else "⚠️ No song found for ${state.enteredCode} • Open 📖 SONGBOOK"
                    } else {
                        if (isTagalog) "🔢 Pindutin ang keypad • Kulang pa ng ${6 - state.enteredCode.length} digits" else "🔢 Press keypad • Need ${6 - state.enteredCode.length} more digits"
                    },
                    color = Color(0xFFA5B4FC),
                    fontSize = if (isLandscape) 13.sp else 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isTagalog)
                            "💡 Pumili ng 6-digit code sa ibaba o buksan ang 📖 SONGBOOK"
                        else
                            "💡 Enter a 6-digit code below or open 📖 SONGBOOK",
                        color = Color(0xFFCBD5E1),
                        fontSize = if (isLandscape) 12.sp else 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isTagalog)
                            "Halimbawa: 000016 • Pindutin ang R para sa Reserved o ❤️ para sa Favorites"
                        else
                            "Example: 000016 • Tap R for Reserved Queue or ❤️ for Favorites",
                        color = Color(0xFF94A3B8),
                        fontSize = if (isLandscape) 11.sp else 9.5.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Authentic Videoke Remote Console with 3D Tactile Keypad, R Reserve Button,
 * and Bottom Icon Bar (Songbook 📖, Reserved R, Favorites ❤️, Record 🎙️, SoundFont 🎛️, Lyrics 🔤, Mixer 🎚️, BG 🎬, TV 📺, Settings ⚙️)
 */
@Composable
fun VideokeRemoteConsole(
    state: VideoKeyUiState,
    onDigit: (Char) -> Unit,
    onClear: () -> Unit,
    onDelete: () -> Unit,
    onReserve: () -> Unit,
    onPlay: () -> Unit,
    onNext: () -> Unit,
    onStop: () -> Unit,
    onOpenSongbook: () -> Unit,
    onToggleReserveQueue: () -> Unit = {},
    onToggleFavorites: () -> Unit = {},
    onOpenRecordingsDialog: () -> Unit = {},
    onOpenSoundFontDialog: () -> Unit = {},
    onOpenBackgroundDialog: () -> Unit = {},
    onOpenLyricStyleDialog: () -> Unit = {},
    onOpenAudioMixerDialog: () -> Unit = {},
    onToggleTvMode: () -> Unit = {},
    onOpenSettingsDialog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F172A),
                        Color(0xFF0B1120),
                        Color(0xFF070B14)
                    )
                )
            )
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Main Area: Numeric 0-9 Keypad Grid + Transport Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Numeric 0-9 Keypad Grid (Cleanly visible on screen)
            Column(
                modifier = Modifier.weight(3.2f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val rows = listOf(
                    listOf('1', '2', '3'),
                    listOf('4', '5', '6'),
                    listOf('7', '8', '9')
                )

                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row.forEach { digit ->
                            RemoteKey(
                                text = "$digit",
                                onClick = { onDigit(digit) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Bottom row: CLR, 0, DEL
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RemoteKey(
                        text = "✕",
                        textColor = Color.White,
                        bgBrush = Brush.verticalGradient(
                            listOf(Color(0xFFEF4444), Color(0xFF991B1B))
                        ),
                        onClick = onClear,
                        modifier = Modifier.weight(1f)
                    )
                    RemoteKey(
                        text = "0",
                        onClick = { onDigit('0') },
                        modifier = Modifier.weight(1f)
                    )
                    RemoteKey(
                        text = "⌫",
                        textColor = Color.White,
                        bgBrush = Brush.verticalGradient(
                            listOf(Color(0xFFF59E0B), Color(0xFFB45309))
                        ),
                        onClick = onDelete,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Right Column: Transport Action Buttons (📖 Songbook, ▶ Play, ⏭ Next, ⏹ Stop)
            // Songbook is now at the top as requested!
            Column(
                modifier = Modifier.weight(1.05f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ActionButton(
                    text = "",
                    icon = "📖",
                    bgBrush = Brush.verticalGradient(
                        listOf(Color(0xFFFFD54F), Color(0xFFFFB300), Color(0xFFE65100))
                    ),
                    textColor = Color.Black,
                    onClick = onOpenSongbook,
                    modifier = Modifier.height(40.dp)
                )
                ActionButton(
                    text = "",
                    icon = "▶",
                    bgBrush = Brush.verticalGradient(
                        listOf(Color(0xFF00FF88), Color(0xFF00E676), Color(0xFF00701A))
                    ),
                    textColor = Color.Black,
                    onClick = onPlay,
                    modifier = Modifier.height(40.dp)
                )
                ActionButton(
                    text = "",
                    icon = "⏭",
                    bgBrush = Brush.verticalGradient(
                        listOf(Color(0xFF448AFF), Color(0xFF2979FF), Color(0xFF1565C0))
                    ),
                    textColor = Color.White,
                    onClick = onNext,
                    modifier = Modifier.height(40.dp)
                )
                ActionButton(
                    text = "",
                    icon = "⏹",
                    bgBrush = Brush.verticalGradient(
                        listOf(Color(0xFFFF5252), Color(0xFFFF1744), Color(0xFFB71C1C))
                    ),
                    textColor = Color.White,
                    onClick = onStop,
                    modifier = Modifier.height(40.dp)
                )
            }
        }

        // Bottom Icon-Only Bar: Forms appear ONLY when an icon is pressed!
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF162032), Color(0xFF0B1120))
                    )
                )
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Reserved Queue / Reserve Icon (R) - now in bottom bar as requested!
            ConsoleIconButton(
                icon = if (state.reserveQueue.isNotEmpty()) "R (${state.reserveQueue.size})" else "R",
                borderColor = Color(0xFFFFD54F),
                bgColor = Color(0xFF2A2008),
                isActive = state.isReserveQueueOpen,
                onClick = onReserve
            )

            // 2. Favorite Songs Icon (❤️)
            ConsoleIconButton(
                icon = "❤️",
                borderColor = Color(0xFFFF4081),
                bgColor = Color(0xFF2D0A1B),
                isActive = state.isFavoritesOpen,
                onClick = onToggleFavorites
            )

            // 3. Voice + Music Recording Icon (🎙️ / 🔴)
            ConsoleIconButton(
                icon = if (state.isRecording) "🔴" else "🎙️",
                borderColor = Color(0xFFFF5252),
                bgColor = if (state.isRecording) Color(0xFF450A0A) else Color(0xFF260E14),
                isActive = state.isRecording || state.isRecordingsDialogOpen,
                onClick = onOpenRecordingsDialog
            )

            // 4. SoundFont SF2 Selector Icon (🎛️)
            ConsoleIconButton(
                icon = "🎛️",
                borderColor = Color(0xFF00FF88),
                bgColor = Color(0xFF062619),
                isActive = state.isSoundFontDialogOpen,
                onClick = onOpenSoundFontDialog
            )

            // 5. Lyric Font & Style Icon (🔤)
            ConsoleIconButton(
                icon = "🔤",
                borderColor = Color(0xFF818CF8),
                bgColor = Color(0xFF1E1B4B),
                isActive = state.isLyricStyleDialogOpen,
                onClick = onOpenLyricStyleDialog
            )

            // 6. Audio Mixer (Key / Tempo / Melody) Icon (🎚️)
            ConsoleIconButton(
                icon = "🎚️",
                borderColor = Color(0xFF00E5FF),
                bgColor = Color(0xFF06283D),
                isActive = state.isAudioMixerDialogOpen,
                onClick = onOpenAudioMixerDialog
            )

            // 7. Backdrop Video/Image Folder Icon (🎬)
            ConsoleIconButton(
                icon = "🎬",
                borderColor = Color(0xFFFFB300),
                bgColor = Color(0xFF281A05),
                isActive = state.isBackgroundDialogOpen,
                onClick = onOpenBackgroundDialog
            )

            // 8. Landscape TV Mode Icon (📺)
            ConsoleIconButton(
                icon = "📺",
                borderColor = Color(0xFF4ADE80),
                bgColor = Color(0xFF0F291E),
                isActive = state.isTvMode,
                onClick = onToggleTvMode
            )

            // 9. Settings Icon (⚙️)
            ConsoleIconButton(
                icon = "⚙️",
                borderColor = Color(0xFF38BDF8),
                bgColor = Color(0xFF0F172A),
                isActive = state.isSettingsDialogOpen,
                onClick = onOpenSettingsDialog
            )
        }
    }
}

@Composable
fun ConsoleIconButton(
    icon: String,
    borderColor: Color,
    bgColor: Color,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .shadow(if (isActive) 8.dp else 3.dp, RoundedCornerShape(9.dp))
            .clip(RoundedCornerShape(9.dp))
            .background(if (isActive) borderColor.copy(alpha = 0.28f) else bgColor)
            .border(
                width = if (isActive) 2.dp else 1.2.dp,
                color = borderColor,
                shape = RoundedCornerShape(9.dp)
            )
            .clickable { onClick() }
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = icon,
            fontSize = 15.sp
        )
    }
}

/**
 * Audio Mixer Popup Modal (Key Transpose, Tempo BPM, Melody Guide Volume)
 * Appears only when pressing the 🎚️ Audio Mixer icon!
 */
@Composable
fun AudioMixerDialog(
    state: VideoKeyUiState,
    isLandscape: Boolean = false,
    onAdjustKey: (Int) -> Unit,
    onAdjustTempo: (Int) -> Unit,
    onAdjustMelody: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val isTagalog = state.appLanguage == com.example.videokey.ui.main.AppLanguage.TAGALOG

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF0F172A),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF00E5FF)),
            modifier = Modifier
                .fillMaxWidth(if (isLandscape) 0.72f else 0.92f)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isTagalog) "🎚️ TIMPLA NG TUNOG (KEY • TEMPO • MELODY)" else "🎚️ AUDIO MIXER (KEY • TEMPO • MELODY)",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = Color(0xFF00E5FF)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Text(text = "✕", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // 1. Key Transpose
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isTagalog) "🎹 TONO (KEY TRANSPOSE)" else "🎹 KEY TRANSPOSE",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (state.keyTranspose == 0) (if (isTagalog) "Normal (0)" else "Standard (0)") else if (state.keyTranspose > 0) "+${state.keyTranspose} Semitones" else "${state.keyTranspose} Semitones",
                            fontSize = 11.sp,
                            color = Color(0xFFFFD54F),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onAdjustKey(-1) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(text = "♭ -1", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                        Text(
                            text = if (state.keyTranspose > 0) "+${state.keyTranspose}" else "${state.keyTranspose}",
                            color = Color(0xFFFFD54F),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Button(
                            onClick = { onAdjustKey(1) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(text = "♯ +1", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                    }
                }

                // 2. Tempo BPM
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isTagalog) "⏱️ BILIS NG TUGTOG (TEMPO)" else "⏱️ SONG TEMPO (BPM)",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                        Text(
                            text = "${state.tempoPercent}% Speed",
                            fontSize = 11.sp,
                            color = Color(0xFF00FF88),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onAdjustTempo(-5) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(text = "-5%", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                        Text(
                            text = "${state.tempoPercent}%",
                            color = Color(0xFF00FF88),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                        Button(
                            onClick = { onAdjustTempo(5) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(text = "+5%", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                    }
                }

                // 3. Melody Guide Volume
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isTagalog) "🎵 GABAY NA MELODY (VOCAL GUIDE)" else "🎵 MELODY GUIDE VOLUME",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (state.melodyPercent == 0) "OFF (Minus-One)" else "${state.melodyPercent}% Volume",
                            fontSize = 11.sp,
                            color = if (state.melodyPercent == 0) Color(0xFFFF5252) else Color(0xFF00E5FF),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onAdjustMelody(-10) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(text = "-10%", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                        Text(
                            text = if (state.melodyPercent == 0) "OFF" else "${state.melodyPercent}%",
                            color = if (state.melodyPercent == 0) Color(0xFFFF5252) else Color(0xFF00E5FF),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                        Button(
                            onClick = { onAdjustMelody(10) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(text = "+10%", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                ) {
                    Text(
                        text = if (isTagalog) "TAPOS NA / ISARA ✓" else "DONE / CLOSE ✓",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Alternating Karaoke Lyric Line with high-contrast drop shadow and yellow wipe
 */
@Composable
fun KaraokeLyricLine(
    text: String,
    isActive: Boolean,
    wipePercent: Float,
    fontSize: androidx.compose.ui.unit.TextUnit = 22.sp,
    lyricConfig: LyricCustomization = LyricCustomization(),
    countdownNumber: Int = 0,
    isLandscape: Boolean = false,
    modifier: Modifier = Modifier
) {
    val wipeClamped = wipePercent.coerceIn(0f, 1f)
    val displayText = if (lyricConfig.isAllCaps) text.uppercase() else text
    val activeColor = Color(lyricConfig.wipeColor.activeColor)
    val inactiveBaseColor = Color(lyricConfig.inactiveColor)

    val composeFontFamily = when (lyricConfig.fontFamily) {
        LyricFontFamily.SAN_SERIF -> FontFamily.SansSerif
        LyricFontFamily.MONOSPACE -> FontFamily.Monospace
        LyricFontFamily.SERIF -> FontFamily.Serif
        LyricFontFamily.CURSIVE -> FontFamily.Cursive
    }

    val shadow = when (lyricConfig.shadowStyle) {
        LyricShadowStyle.HEAVY_OUTLINE -> Shadow.None // real stroke is drawn as a separate layer below
        LyricShadowStyle.NEON_GLOW -> Shadow(
            color = activeColor.copy(alpha = 0.85f),
            offset = Offset(0f, 0f),
            blurRadius = 14f
        )
        LyricShadowStyle.SUBTLE_DROP -> Shadow(
            color = Color.Black.copy(alpha = 0.85f),
            offset = Offset(2f, 2f),
            blurRadius = 4f
        )
        LyricShadowStyle.CLEAN -> Shadow.None
    }

    // Inner text fill brush: wipes from Blue (active) to White (inactive)
    val brush = if (isActive && wipeClamped > 0f) {
        Brush.horizontalGradient(
            0.0f to activeColor,
            wipeClamped to activeColor,
            (wipeClamped + 0.001f).coerceAtMost(1f) to inactiveBaseColor,
            1.0f to inactiveBaseColor
        )
    } else null

    // Outer outline brush: wipes from Crisp White (highlighted portion) to Crisp Black (unhighlighted portion)
    val outlineBrush = if (isActive && wipeClamped > 0f) {
        Brush.horizontalGradient(
            0.0f to Color.White,
            wipeClamped to Color.White,
            (wipeClamped + 0.001f).coerceAtMost(1f) to Color.Black,
            1.0f to Color.Black
        )
    } else null

    val annotatedString = buildAnnotatedString {
        if (brush != null) {
            withStyle(
                SpanStyle(
                    brush = brush,
                    letterSpacing = 0.sp,
                    shadow = shadow
                )
            ) {
                append(displayText)
            }
        } else {
            val textColor = inactiveBaseColor // Solid pure white for crisp readability and high contrast with black outline
            withStyle(
                SpanStyle(
                    color = textColor,
                    letterSpacing = 0.sp,
                    shadow = shadow
                )
            ) {
                append(displayText)
            }
        }
    }

    // Shrink-to-fit: if the line is wider than the screen, reduce the size step by step
    var fitScale by remember(text, fontSize, lyricConfig.fontScale) { mutableStateOf(1f) }
    val scaledFontSize = (fontSize.value * lyricConfig.fontScale * fitScale).sp
    val useOutline = lyricConfig.shadowStyle == LyricShadowStyle.HEAVY_OUTLINE
    val outlineWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) { scaledFontSize.toPx() * 0.095f }

    // Shared unified TextStyle to guarantee 100% identical glyph layout, metrics and spacing:
    val sharedLyricStyle = TextStyle(
        fontSize = scaledFontSize,
        fontWeight = FontWeight.Bold,
        fontStyle = if (lyricConfig.isItalic) FontStyle.Italic else FontStyle.Normal,
        fontFamily = composeFontFamily,
        textAlign = TextAlign.Center,
        letterSpacing = 0.sp
    )

    val outlineStyle = if (outlineBrush != null) {
        sharedLyricStyle.copy(
            brush = outlineBrush,
            drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(
                width = outlineWidthPx,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
    } else {
        sharedLyricStyle.copy(
            color = Color.Black,
            drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(
                width = outlineWidthPx,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
    }

    val fillStyle = sharedLyricStyle.copy(
        drawStyle = androidx.compose.ui.graphics.drawscope.Fill
    )

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Inner container centered in the line that tightly wraps the lyric text
        Box(
            modifier = Modifier.wrapContentSize(),
            contentAlignment = Alignment.Center
        ) {
            if (useOutline) {
                // Crisp 2D normal outline layer (Arial Bold with white outline on highlight)
                Text(
                    text = displayText,
                    maxLines = 1,
                    softWrap = false,
                    style = outlineStyle
                )
            }
            Text(
                text = annotatedString,
                maxLines = 1,
                softWrap = false,
                style = fillStyle,
                onTextLayout = { result ->
                    if (result.didOverflowWidth && fitScale > 0.35f) fitScale *= 0.92f
                }
            )

            // Single countdown number placed right above the first letter of lyrics!
            if (countdownNumber in 1..4) {
                SingleCountdownBadge(
                    number = countdownNumber,
                    isLandscape = isLandscape,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(
                            x = 0.dp,
                            y = if (isLandscape) (-30).dp else (-26).dp
                        )
                )
            }
        }
    }
}

/**
 * Animated Audio Equalizer DSP Bars for Authentic Arcade/Videoke Screen
 */
@Composable
fun AudioEqualizerBars(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "eq")
    val b1 by transition.animateFloat(
        initialValue = 0.2f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(380, easing = LinearEasing), RepeatMode.Reverse), label = "b1"
    )
    val b2 by transition.animateFloat(
        initialValue = 0.85f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(320, easing = LinearEasing), RepeatMode.Reverse), label = "b2"
    )
    val b3 by transition.animateFloat(
        initialValue = 0.4f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse), label = "b3"
    )
    val b4 by transition.animateFloat(
        initialValue = 0.9f, targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(290, easing = LinearEasing), RepeatMode.Reverse), label = "b4"
    )

    Row(
        modifier = modifier.height(18.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(b1, b2, b3, b4).forEach { heightFraction ->
            val barH = if (isPlaying) (heightFraction * 18).coerceIn(3f, 18f).dp else 3.dp
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(barH)
                    .clip(RoundedCornerShape(1.dp))
                    .background(Color(0xFF00E676))
            )
        }
    }
}

@Composable
fun RemoteKey(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    bgBrush: Brush? = null
) {
    val defaultBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF27354A),
            Color(0xFF151D2A)
        )
    )
    Box(
        modifier = modifier
            .height(40.dp)
            .shadow(4.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(bgBrush ?: defaultBrush)
            .border(
                1.dp,
                Brush.verticalGradient(listOf(Color(0xFF475569), Color(0xFF1E293B))),
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontWeight = FontWeight.Black,
            fontSize = if (text.length > 2) 13.sp else 18.sp,
            fontFamily = FontFamily.Monospace,
            style = TextStyle(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.8f),
                    offset = Offset(1f, 1f),
                    blurRadius = 2f
                )
            )
        )
    }
}

@Composable
fun ActionButton(
    text: String,
    bgBrush: Brush,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: String = ""
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(bgBrush)
            .border(
                1.5.dp,
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.4f), Color.Transparent)),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        if (text.isEmpty()) {
            Text(
                text = icon,
                color = textColor,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.4f),
                        offset = Offset(1f, 1f),
                        blurRadius = 2f
                    )
                )
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon.isNotEmpty()) {
                    Text(
                        text = "$icon ",
                        color = textColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                }
                Text(
                    text = text,
                    color = textColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.4f),
                            offset = Offset(1f, 1f),
                            blurRadius = 2f
                        )
                    )
                )
            }
        }
    }
}

@Composable
fun MiniCircleButton(text: String, size: androidx.compose.ui.unit.Dp = 21.dp, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(size)
            .shadow(2.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF2E3E56), Color(0xFF182233))
                )
            )
            .border(1.dp, Color(0xFF475569), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Color.White, fontWeight = FontWeight.Black, fontSize = 10.sp)
    }
}

/**
 * Sleek TV Mode Control Dock for Android TV / Landscape
 * Large tactile buttons for 10-foot viewing and TV remote / air-mouse / touch control.
 */
@Composable
fun TvControlDock(
    state: VideoKeyUiState,
    onOpenSongbook: () -> Unit,
    onOpenKeypad: () -> Unit,
    onPlay: () -> Unit,
    onNext: () -> Unit,
    onStop: () -> Unit,
    onAdjustKey: (Int) -> Unit,
    onAdjustTempo: (Int) -> Unit,
    onAdjustMelody: (Int) -> Unit,
    onOpenBackgroundDialog: () -> Unit,
    onOpenSettingsDialog: () -> Unit,
    onToggleTvMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xD90F172A), Color(0xF2020617))
                )
            )
            .border(1.dp, Color(0xFF334155))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Group: Quick Actions
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Songbook
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF261C05))
                    .border(1.5.dp, Color(0xFFFFC107), RoundedCornerShape(8.dp))
                    .clickable { onOpenSongbook() }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📖 SONGBOOK",
                    color = Color(0xFFFFD54F),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }

            // Keypad Dial Modal
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF002738))
                    .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(8.dp))
                    .clickable { onOpenKeypad() }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🔢 DIAL NO.",
                    color = Color(0xFF00E5FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // Center Group: Playback Controls (Play, Next, Stop)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Play
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF00C853), Color(0xFF009624))))
                    .border(1.dp, Color(0xFF69F0AE), RoundedCornerShape(8.dp))
                    .clickable { onPlay() }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "▶ PLAY", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }

            // Next
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF2979FF), Color(0xFF1565C0))))
                    .border(1.dp, Color(0xFF82B1FF), RoundedCornerShape(8.dp))
                    .clickable { onNext() }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "⏭ NEXT", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }

            // Stop
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFFD50000), Color(0xFFB71C1C))))
                    .border(1.dp, Color(0xFFFF8A80), RoundedCornerShape(8.dp))
                    .clickable { onStop() }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "■ STOP", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        }

        // Right Group: Key, BPM, Melody, Background, Settings, and Phone toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Key Transpose
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "KEY", fontSize = 10.sp, color = Color.LightGray, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(2.dp))
                MiniCircleButton(text = "♭", size = 22.dp) { onAdjustKey(-1) }
                Text(
                    text = if (state.keyTranspose > 0) "+${state.keyTranspose}" else "${state.keyTranspose}",
                    color = Color(0xFFFFD54F),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
                MiniCircleButton(text = "♯", size = 22.dp) { onAdjustKey(1) }
            }

            // Tempo BPM
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "BPM", fontSize = 10.sp, color = Color.LightGray, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(2.dp))
                MiniCircleButton(text = "-", size = 22.dp) { onAdjustTempo(-5) }
                Text(
                    text = "${state.tempoPercent}%",
                    color = Color(0xFF00FF88),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
                MiniCircleButton(text = "+", size = 22.dp) { onAdjustTempo(5) }
            }

            // Melody
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "MEL", fontSize = 10.sp, color = Color.LightGray, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(2.dp))
                MiniCircleButton(text = "-", size = 22.dp) { onAdjustMelody(-10) }
                Text(
                    text = if (state.melodyPercent == 0) "OFF" else "${state.melodyPercent}%",
                    color = if (state.melodyPercent == 0) Color(0xFFFF5252) else Color(0xFF00E5FF),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
                MiniCircleButton(text = "+", size = 22.dp) { onAdjustMelody(10) }
            }

            // Background
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF261C05))
                    .border(1.dp, Color(0xFFFFC107), RoundedCornerShape(6.dp))
                    .clickable { onOpenBackgroundDialog() }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🎬 BG", fontSize = 10.sp, color = Color(0xFFFFD54F), fontWeight = FontWeight.Black)
            }

            // Settings
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color(0xFF475569), RoundedCornerShape(6.dp))
                    .clickable { onOpenSettingsDialog() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "⚙️", fontSize = 13.sp)
            }

            // Phone Split-screen Toggle
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1E293B))
                    .border(1.dp, Color(0xFF64748B), RoundedCornerShape(6.dp))
                    .clickable { onToggleTvMode() }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "📱 PHONE", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * On-Screen Keypad Dialog for TV Mode
 * Allows TV remote / air-mouse / touch users to dial song numbers easily.
 */
@Composable
fun TvKeypadDialog(
    state: VideoKeyUiState,
    onDigit: (Char) -> Unit,
    onClear: () -> Unit,
    onDelete: () -> Unit,
    onReserve: () -> Unit,
    onPlay: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .width(420.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                    )
                )
                .border(2.dp, Color(0xFFFFD54F), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val isTagalog = state.appLanguage == com.example.videokey.ui.main.AppLanguage.TAGALOG
                var showQrRemoteInTv by remember { mutableStateOf(false) }
                val deviceIp = remember { com.example.videokey.remote.VideokeRemoteServer.getLocalIpAddress() }

                // Header with Close & QR Remote button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isTagalog) "🔢 KEYPAD NG TV" else "🔢 TV KEYPAD",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = Color(0xFFFFD54F)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (showQrRemoteInTv) Color(0xFF00E676) else Color(0xFF1E293B),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E676)),
                            modifier = Modifier.clickable { showQrRemoteInTv = !showQrRemoteInTv }
                        ) {
                            Text(
                                text = "📱 QR REMOTE",
                                color = if (showQrRemoteInTv) Color.Black else Color(0xFF00FF88),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Text(text = "✕", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (showQrRemoteInTv) {
                    val qrBitmap = remember(deviceIp) {
                        com.example.videokey.remote.QrCodeGenerator.generateQrBitmap("http://$deviceIp:8080", 300)
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, Color(0xFF00FF88), RoundedCornerShape(10.dp))
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (qrBitmap != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White
                            ) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "QR Remote",
                                    modifier = Modifier.size(140.dp).padding(6.dp)
                                )
                            }
                        }
                        Text(
                            text = "http://$deviceIp:8080",
                            color = Color(0xFFFFD54F),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = if (isTagalog) "I-scan gamit ang cellphone camera para maka-connect" else "Scan with phone camera to connect",
                            color = Color(0xFF94A3B8),
                            fontSize = 9.sp
                        )
                    }
                }

                // 6-digit display
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 6) {
                        val ch = state.enteredCode.getOrNull(i)
                        val isCursor = i == state.enteredCode.length
                        Box(
                            modifier = Modifier
                                .size(40.dp, 48.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black)
                                .border(
                                    if (isCursor) 2.dp else 1.dp,
                                    if (isCursor) Color(0xFFFFD54F) else Color(0xFF334155),
                                    RoundedCornerShape(6.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ch?.toString() ?: if (isCursor) "_" else "",
                                color = Color(0xFF00FF88),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp
                            )
                        }
                    }
                }

                // Matched Song preview if any (Song Title and Artist preserved as original)
                if (state.matchedEnteredSong != null) {
                    Text(
                        text = "★ ${state.matchedEnteredSong.title} • ${state.matchedEnteredSong.artist}".uppercase(),
                        color = Color(0xFFFFD54F),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                // Keypad grid (1-9, CLR, 0, DEL)
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("CLR", "0", "DEL")
                )
                rows.forEach { rowKeys ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowKeys.forEach { key ->
                            val keyDisplay = when (key) {
                                "CLR" -> if (isTagalog) "BURA" else "CLR"
                                "DEL" -> if (isTagalog) "ALIS" else "DEL"
                                else -> key
                            }
                            Button(
                                onClick = {
                                    when (key) {
                                        "CLR" -> onClear()
                                        "DEL" -> onDelete()
                                        else -> onDigit(key[0])
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when (key) {
                                        "CLR" -> Color(0xFFB71C1C)
                                        "DEL" -> Color(0xFFD97706)
                                        else -> Color(0xFF334155)
                                    }
                                )
                            ) {
                                Text(
                                    text = keyDisplay,
                                    fontSize = if (keyDisplay.length > 3) 13.sp else 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Action Row: RESERVE & PLAY
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            onReserve()
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                    ) {
                        Text(
                            text = if (isTagalog) "R IRESERBA" else "R RESERVE",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = Color.Black
                        )
                    }

                    Button(
                        onClick = {
                            onPlay()
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                    ) {
                        Text(
                            text = if (isTagalog) "▶ TUGTUGIN" else "▶ PLAY",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Unified Upper Screen Overlay with 3 Tabs:
 * 1. 📖 SONGBOOK (Search catalog & MIDI filter)
 * 2. R RESERVED QUEUE (List of reserved songs, order, delete, play)
 * 3. ❤️ FAVORITES (User favorite songs list with quick play and reserve)
 * Automatically leaves space at the bottom for the 2-line lyrics so lyrics are NEVER covered!
 */
@Composable
fun SongbookSearchOverlay(
    songs: List<Song>,
    reserveQueue: List<Song> = emptyList(),
    favoriteSongs: List<Song> = emptyList(),
    favoriteCodes: Set<String> = emptySet(),
    activeTab: Int = 0, // 0 = SONGBOOK, 1 = RESERVED (R), 2 = FAVORITES (❤️)
    currentSongFolder: String = "",
    scanStatus: String = "",
    isScanning: Boolean = false,
    onlyShowPlayableSongs: Boolean = false,
    playableCodes: Set<String> = emptySet(),
    isTagalog: Boolean = false,
    isLandscape: Boolean = false,
    onSwitchTab: (Int) -> Unit = {},
    onToggleFilter: (Boolean) -> Unit = {},
    onOpenFolderLocator: () -> Unit = {},
    onQueryChange: (String) -> Unit = {},
    onSelectSong: (Song) -> Unit = {},
    onReserveSong: (Song) -> Unit = {},
    onToggleFavoriteSong: (Song) -> Unit = {},
    onPlayReservedIndex: (Int) -> Unit = {},
    onRemoveReservedIndex: (Int) -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    val reservedCodes = remember { mutableStateMapOf<String, Boolean>() }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xF20F172A),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            when (activeTab) {
                1 -> Color(0xFFFFD54F) // Reserved Queue
                2 -> Color(0xFFFF4081) // Favorites
                else -> Color(0xFFFFC107) // Songbook
            }
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            // Top Tab Navigation Bar (📖 SONGBOOK | R RESERVED | ❤️ FAVORITES) + Close (✕)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // TAB 0: 📖 SONGBOOK
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (activeTab == 0) Color(0xFFFFB300) else Color(0xFF1E293B),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (activeTab == 0) Color(0xFFFFD54F) else Color(0xFF334155)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSwitchTab(0) }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "📖 SONGBOOK",
                                color = if (activeTab == 0) Color.Black else Color(0xFFFFD54F),
                                fontSize = if (isLandscape) 11.sp else 9.5.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // TAB 1: R RESERVED
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (activeTab == 1) Color(0xFFFFD54F) else Color(0xFF1E293B),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (activeTab == 1) Color(0xFFFFE082) else Color(0xFF334155)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSwitchTab(1) }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "R RESERVE (${reserveQueue.size})",
                                color = if (activeTab == 1) Color.Black else Color(0xFFFFD54F),
                                fontSize = if (isLandscape) 11.sp else 9.5.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // TAB 2: ❤️ FAVORITES
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (activeTab == 2) Color(0xFFFF4081) else Color(0xFF1E293B),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (activeTab == 2) Color(0xFFFF80AB) else Color(0xFF334155)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSwitchTab(2) }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "❤️ FAVORITES (${favoriteSongs.size})",
                                color = if (activeTab == 2) Color.White else Color(0xFFFF80AB),
                                fontSize = if (isLandscape) 11.sp else 9.5.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF334155))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "✕", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // CONTENT BY ACTIVE TAB:
            when (activeTab) {
                0 -> {
                    // TAB 0: SONGBOOK SEARCH & MIDI FILTER
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF0284C7),
                                modifier = Modifier.clickable { onOpenFolderLocator() }
                            ) {
                                Text(
                                    text = "📂 Folder",
                                    color = Color.White,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (onlyShowPlayableSongs) Color(0xFF00E676) else Color(0xFF1E293B),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E676)),
                                modifier = Modifier.clickable { onToggleFilter(!onlyShowPlayableSongs) }
                            ) {
                                Text(
                                    text = if (onlyShowPlayableSongs) "🎵 MIDI (${playableCodes.size})" else (if (isTagalog) "📖 Lahat" else "📖 All"),
                                    color = if (onlyShowPlayableSongs) Color.Black else Color.White,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (scanStatus.isNotEmpty()) {
                            Text(
                                text = scanStatus,
                                color = Color(0xFF94A3B8),
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Compact Search Input Bar
                    OutlinedTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            onQueryChange(it)
                        },
                        placeholder = {
                            Text(
                                text = if (isTagalog) "I-type ang titulo, artist, o numero ng kanta..." else "Type song title, artist, or code...",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        },
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .heightIn(min = 40.dp)
                    )

                    // Song Results List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (songs.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isScanning)
                                            (if (isTagalog) "Nag-i-scan ng mga kanta..." else "Scanning songs...")
                                        else
                                            (if (isTagalog) "Walang nahanap na kanta." else "No matching songs found."),
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        items(songs) { song ->
                            val isPlayable = song.isPacked || song.sectorOffset > 0L || playableCodes.contains(song.code) || song.filePath.endsWith(".mid", true) || song.filePath.endsWith(".kar", true)
                            val isFav = favoriteCodes.contains(song.code)
                            val isReserved = reservedCodes[song.code] == true

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1E293B))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onSelectSong(song) }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = song.code,
                                            color = Color(0xFFFFD54F),
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = song.title,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = if (song.artist.isNotBlank()) "🎤 ${song.artist}" else "🎤 OPM / Videoke",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // 1. Favorite Toggle (❤️)
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isFav) Color(0xFF831843) else Color(0xFF0F172A),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isFav) Color(0xFFFF4081) else Color(0xFF475569)),
                                        modifier = Modifier.clickable { onToggleFavoriteSong(song) }
                                    ) {
                                        Text(
                                            text = "❤️",
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                        )
                                    }

                                    // 2. Reserve Button (R)
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isReserved) Color(0xFF064E3B) else Color(0xFF332000),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isReserved) Color(0xFF00FF88) else Color(0xFFFFD54F)),
                                        modifier = Modifier.clickable {
                                            onReserveSong(song)
                                            reservedCodes[song.code] = true
                                        }
                                    ) {
                                        Text(
                                            text = if (isReserved) "✓ R" else "R",
                                            color = if (isReserved) Color(0xFF00FF88) else Color(0xFFFFD54F),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    // 3. Play Button (▶)
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isPlayable) Color(0xFF00E676) else Color(0xFF475569),
                                        modifier = Modifier.clickable { onSelectSong(song) }
                                    ) {
                                        Text(
                                            text = "▶",
                                            color = if (isPlayable) Color.Black else Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: RESERVED SONGS (R) LIST
                    Text(
                        text = if (isTagalog) "MGA NAKA-RESERVE NA KANTA (${reserveQueue.size})" else "RESERVED SONGS QUEUE (${reserveQueue.size})",
                        color = Color(0xFFFFD54F),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (reserveQueue.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isTagalog)
                                            "Walang naka-reserve na kanta.\nMag-reserve sa Songbook o pindutin ang 6-digit code sa keypad + R."
                                        else
                                            "No reserved songs in queue.\nReserve from Songbook or press 6-digit code on keypad + R.",
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        items(reserveQueue.size) { index ->
                            val song = reserveQueue[index]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1E293B))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "#${index + 1}",
                                        color = Color(0xFF38BDF8),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.width(28.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${song.code} • ${song.title}",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = song.artist,
                                            color = Color(0xFF94A3B8),
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Play from reserve
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF00E676),
                                        modifier = Modifier.clickable { onPlayReservedIndex(index) }
                                    ) {
                                        Text(
                                            text = "▶",
                                            color = Color.Black,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    // Remove from reserve
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFEF4444),
                                        modifier = Modifier.clickable { onRemoveReservedIndex(index) }
                                    ) {
                                        Text(
                                            text = "✕",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: FAVORITE SONGS (❤️) LIST
                    Text(
                        text = if (isTagalog) "MGA PABORITONG KANTA (${favoriteSongs.size})" else "FAVORITE SONGS (${favoriteSongs.size})",
                        color = Color(0xFFFF4081),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (favoriteSongs.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isTagalog)
                                            "Walang naka-save na paboritong kanta.\nPindutin ang ❤️ sa alinmang kanta sa Songbook para i-save dito!"
                                        else
                                            "No favorite songs saved yet.\nTap ❤️ on any song in Songbook to save it here!",
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        items(favoriteSongs) { song ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1E293B))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onSelectSong(song) }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = song.code,
                                            color = Color(0xFFFFD54F),
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = song.title,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = song.artist,
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Remove from Favorites (❤️)
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF831843),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF4081)),
                                        modifier = Modifier.clickable { onToggleFavoriteSong(song) }
                                    ) {
                                        Text(
                                            text = "❤️",
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                        )
                                    }

                                    // Reserve Button (R)
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF332000),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD54F)),
                                        modifier = Modifier.clickable { onReserveSong(song) }
                                    ) {
                                        Text(
                                            text = "R",
                                            color = Color(0xFFFFD54F),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    // Play Button (▶)
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF00E676),
                                        modifier = Modifier.clickable { onSelectSong(song) }
                                    ) {
                                        Text(
                                            text = "▶",
                                            color = Color.Black,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SongbookDialog(
    songs: List<Song>,
    currentSongFolder: String,
    scanStatus: String,
    isScanning: Boolean,
    onlyShowPlayableSongs: Boolean = false,
    playableCodes: Set<String> = emptySet(),
    isTagalog: Boolean = false,
    onToggleFilter: (Boolean) -> Unit = {},
    onOpenFolderLocator: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSelectSong: (Song) -> Unit,
    onReserveSong: (Song) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val reservedCodes = remember { mutableStateMapOf<String, Boolean>() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isTagalog) "📖 AKLAT NG MGA KANTA (SONGBOOK)" else "📖 VIDEOKEY SONGBOOK",
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFC107),
                        fontSize = 14.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Text(text = "✕", color = Color.White)
                    }
                }

                // Dedicated Single Song Folder Locator Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                            Text(
                                text = if (isTagalog) "📁 LOKASYON NG MGA KANTA:" else "📁 SONG FOLDER LOCATOR:",
                                fontSize = 9.sp,
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = currentSongFolder,
                                fontSize = 11.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isScanning) (if (isTagalog) "⏳ Nag-i-scan..." else "⏳ Scanning...") else scanStatus,
                                fontSize = 9.sp,
                                color = Color(0xFF94A3B8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Button(
                            onClick = onOpenFolderLocator,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                text = if (isTagalog) "📂 Palitan" else "📂 Change",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Filter Tabs: Playable (with real MIDI) vs All Catalog
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onToggleFilter(true) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (onlyShowPlayableSongs) Color(0xFF00E676) else Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = if (isTagalog) "🎵 May MIDI (${playableCodes.size})" else "🎵 With MIDI (${playableCodes.size})",
                            color = if (onlyShowPlayableSongs) Color.Black else Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { onToggleFilter(false) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!onlyShowPlayableSongs) Color(0xFFFFC107) else Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = if (isTagalog) "📖 Lahat (47k+ Catalog)" else "📖 All (47k+ Catalog)",
                            color = if (!onlyShowPlayableSongs) Color.Black else Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        onQueryChange(it)
                    },
                    placeholder = { Text(if (isTagalog) "Maghanap ng titulo, artist, o code..." else "Search title, artist, or code...", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (songs.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isScanning)
                                        (if (isTagalog) "Nag-i-scan ng internal storage..." else "Scanning internal storage...")
                                    else
                                        (if (isTagalog) "Walang nahanap na kanta.\nPindutin ang 'Palitan' sa itaas." else "No songs found.\nTap 'Change' above."),
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    items(songs) { song ->
                        val isPlayable = song.isPacked || playableCodes.contains(song.code) || song.filePath.endsWith(".mid", true) || song.filePath.endsWith(".kar", true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E293B))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSelectSong(song) }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = song.code,
                                        color = Color(0xFFFFC107),
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    if (isPlayable) {
                                        Text(
                                            text = "🎵 MIDI",
                                            color = Color(0xFF00E676),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier
                                                .background(Color(0xFF00381B), RoundedCornerShape(3.dp))
                                                .border(1.dp, Color(0xFF00E676), RoundedCornerShape(3.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "📖 IDX",
                                            color = Color.LightGray,
                                            fontSize = 9.sp,
                                            modifier = Modifier
                                                .background(Color(0xFF334155), RoundedCornerShape(3.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                // Buong Titulo ng Kanta (Full Song Title - No cut off)
                                Text(
                                    text = song.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 4,
                                    softWrap = true
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                // Buong Artist ng Kanta (Full Artist - No cut off)
                                Text(
                                    text = if (song.artist.isNotBlank()) "🎤 ${song.artist}" else (if (isTagalog) "🎤 Hindi Kilalang Artist" else "🎤 Unknown Artist"),
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    maxLines = 3,
                                    softWrap = true
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val isReserved = reservedCodes[song.code] == true
                                Button(
                                    onClick = {
                                        onReserveSong(song)
                                        reservedCodes[song.code] = true
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isReserved) Color(0xFF1E3A2F) else Color(0xFF2E3D5E)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(
                                        text = if (isReserved) (if (isTagalog) "✓ NAKALISTA" else "✓ QUEUED") else (if (isTagalog) "IRESERBA" else "RESERVE"),
                                        color = if (isReserved) Color(0xFF00FF88) else Color(0xFFFFD54F),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Button(
                                    onClick = { onSelectSong(song) },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isPlayable) Color(0xFF00E676) else Color(0xFF475569)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(
                                        text = if (isTagalog) "TUGTOG" else "PLAY",
                                        color = if (isPlayable) Color.Black else Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScoreCelebrationDialog(
    score: Int,
    isTagalog: Boolean = false,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1A102F),
            border = androidx.compose.foundation.BorderStroke(3.dp, Color(0xFFFFC107)),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isTagalog) "🏆 PAGBATI! 🏆" else "🏆 CONGRATULATIONS! 🏆",
                    color = Color(0xFFFFC107),
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "$score",
                    color = Color(0xFFFFEB3B),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 60.sp
                )
                Text(
                    text = if (isTagalog) "BIRIT KING! ANG GALING MO!" else "GREAT SINGING! AWESOME PERFORMANCE!",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = if (isTagalog) "MAGPATULOY" else "CONTINUE", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * SoundFont Selection Modal Dialog
 * Allows selecting SoundFont (.sf2) bank for genuine videoke audio timbre
 */
@Composable
fun SoundFontSelectionDialog(
    soundFonts: List<String>,
    activeSoundFont: String,
    isTagalog: Boolean = false,
    onSelectSoundFont: (String) -> Unit,
    onScanStorage: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFC107)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isTagalog) "🎛️ PILIIN ANG SOUNDFONT (SF2)" else "🎛️ SELECT SOUNDFONT (SF2)",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = Color(0xFFFFC107)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Text(text = "✕", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = if (isTagalog) "Pumili ng Soundbank para sa tugtog ng VideoKey:" else "Select Soundbank for VideoKey audio timbre:",
                    fontSize = 11.sp,
                    color = Color.LightGray,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (soundFonts.isEmpty()) {
                        item {
                            Text(
                                text = if (isTagalog) "Walang nahanap na .sf2 sa /sdcard/VideoKey/SoundFont" else "No .sf2 files found in /sdcard/VideoKey/SoundFont",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        items(soundFonts) { sf ->
                            val isSelected = sf.equals(activeSoundFont, ignoreCase = true) ||
                                    activeSoundFont.contains(sf, ignoreCase = true) ||
                                    sf.contains(activeSoundFont, ignoreCase = true)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF1E3A8A) else Color(0xFF1E293B))
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) Color(0xFFFFC107) else Color(0xFF334155),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onSelectSoundFont(sf) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = if (isSelected) "📻 " else "🎹 ",
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = sf,
                                        color = if (isSelected) Color(0xFFFFE082) else Color.White,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (isSelected) {
                                    Text(
                                        text = if (isTagalog) "GINAGAMIT ✓" else "ACTIVE ✓",
                                        color = Color(0xFF00E676),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        modifier = Modifier
                                            .background(Color(0xFF064E3B), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onScanStorage,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = if (isTagalog) "🔄 I-SCAN MULI ANG SF2" else "🔄 RESCAN SF2", fontSize = 11.sp, color = Color.White)
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = if (isTagalog) "TAPOS NA" else "DONE", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Background Layer: Renders MP4 video, static image, or animated neon stage
 */
@Composable
fun VideokeBackgroundLayer(
    background: KaraokeBackground?,
    hasMultipleMedia: Boolean = true,
    onBackgroundCompleted: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentOnCompleted by rememberUpdatedState(onBackgroundCompleted)

    Box(modifier = modifier.fillMaxSize()) {
        when {
            background?.type == BackgroundType.VIDEO && background.path.isNotEmpty() && File(background.path).exists() -> {
                KaraokeVideoPlayer(
                    videoPath = background.path,
                    hasMultipleMedia = hasMultipleMedia,
                    onVideoCompleted = currentOnCompleted
                )
            }
            background?.type == BackgroundType.IMAGE && background.path.isNotEmpty() && File(background.path).exists() -> {
                // Auto-advance images every 30 seconds
                LaunchedEffect(background.path) {
                    kotlinx.coroutines.delay(30000L)
                    currentOnCompleted()
                }
                val bitmap = remember(background.path) {
                    try {
                        BitmapFactory.decodeFile(background.path)?.asImageBitmap()
                    } catch (e: Exception) {
                        null
                    }
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LaunchedEffect(background.path) {
                        currentOnCompleted()
                    }
                    AnimatedStageBackground()
                }
            }
            else -> {
                AnimatedStageBackground()
            }
        }

        // Semi-transparent dark vignette scrim for 100% lyric contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.40f),
                            Color.Black.copy(alpha = 0.25f),
                            Color.Black.copy(alpha = 0.65f)
                        )
                    )
                )
        )
    }
}

/**
 * Optimized, Hardware-Accelerated Looping MP4 Video Player for Videoke TV Screen Background.
 * Scales with Center-Crop Matrix so videos fill the screen smoothly without stretching or lag.
 * Seamlessly transitions between video files using active TextureView Surface.
 */
@Composable
fun KaraokeVideoPlayer(
    videoPath: String,
    hasMultipleMedia: Boolean = true,
    onVideoCompleted: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentOnVideoCompleted by rememberUpdatedState(onVideoCompleted)
    var activeSurface by remember { mutableStateOf<Surface?>(null) }
    var textureViewRef by remember { mutableStateOf<TextureView?>(null) }
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, mediaPlayerRef) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            val mp = mediaPlayerRef
            if (mp != null) {
                try {
                    when (event) {
                        androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> {
                            if (mp.isPlaying) mp.pause()
                        }
                        androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                            if (!mp.isPlaying && videoPath.isNotBlank()) mp.start()
                        }
                        else -> {}
                    }
                } catch (e: Exception) {
                    Log.w("VideoPlayer", "Error handling lifecycle event $event", e)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    fun adjustAspectMatrix(view: TextureView, viewW: Int, viewH: Int, vidW: Int, vidH: Int) {
        if (viewW <= 0 || viewH <= 0 || vidW <= 0 || vidH <= 0) return
        val scaleX = viewW.toFloat() / vidW.toFloat()
        val scaleY = viewH.toFloat() / vidH.toFloat()
        val maxScale = maxOf(scaleX, scaleY)
        val scaledW = vidW.toFloat() * maxScale
        val scaledH = vidH.toFloat() * maxScale
        val matrix = android.graphics.Matrix()
        matrix.setScale(scaledW / viewW.toFloat(), scaledH / viewH.toFloat(), viewW / 2f, viewH / 2f)
        view.setTransform(matrix)
    }

    DisposableEffect(videoPath, activeSurface) {
        val surface = activeSurface
        if (surface == null || !surface.isValid || videoPath.isBlank()) {
            return@DisposableEffect onDispose {}
        }
        val file = File(videoPath)
        if (!file.exists() || file.length() == 0L) {
            currentOnVideoCompleted()
            return@DisposableEffect onDispose {}
        }

        val mp = MediaPlayer().apply {
            try {
                setDataSource(videoPath)
                setSurface(surface)
                setVolume(0f, 0f) // Muted so it does not clash with karaoke music
                setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                isLooping = !hasMultipleMedia // Loop if single video, otherwise onCompletion advances

                setOnVideoSizeChangedListener { _, vWidth, vHeight ->
                    textureViewRef?.let { tv ->
                        adjustAspectMatrix(tv, tv.width, tv.height, vWidth, vHeight)
                    }
                }
                setOnCompletionListener {
                    Log.d("VideoPlayer", "Background video ended, auto-advancing: $videoPath")
                    currentOnVideoCompleted()
                }
                setOnErrorListener { _, what, extra ->
                    Log.w("VideoPlayer", "MediaPlayer playback error: $what, extra: $extra")
                    currentOnVideoCompleted()
                    true
                }
                setOnPreparedListener { preparedMp ->
                    textureViewRef?.let { tv ->
                        adjustAspectMatrix(tv, tv.width, tv.height, preparedMp.videoWidth, preparedMp.videoHeight)
                    }
                    preparedMp.start()
                }
                prepareAsync()
            } catch (e: Exception) {
                Log.e("VideoPlayer", "Failed to configure video: $videoPath", e)
                currentOnVideoCompleted()
            }
        }
        mediaPlayerRef = mp

        onDispose {
            mediaPlayerRef = null
            try {
                if (mp.isPlaying) mp.stop()
                mp.reset()
                mp.release()
            } catch (e: Exception) {
                Log.w("VideoPlayer", "Error releasing MediaPlayer", e)
            }
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            TextureView(ctx).apply {
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                        activeSurface = Surface(surface)
                        textureViewRef = this@apply
                    }

                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                        textureViewRef?.let { tv ->
                            // Dynamic resize handled on video prepared/aspect matrix
                        }
                    }

                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        activeSurface?.release()
                        activeSurface = null
                        textureViewRef = null
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                }
                if (isAvailable && surfaceTexture != null) {
                    activeSurface = Surface(surfaceTexture)
                    textureViewRef = this
                }
            }
        },
        update = { view ->
            textureViewRef = view
            if (view.isAvailable && (activeSurface == null || !activeSurface!!.isValid) && view.surfaceTexture != null) {
                activeSurface = Surface(view.surfaceTexture)
            }
        }
    )
}

/**
 * Dynamic Animated Stage Lighting Background
 */
@Composable
fun AnimatedStageBackground(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "stage")
    val c1 by transition.animateColor(
        initialValue = Color(0xFF030712),
        targetValue = Color(0xFF0A1528),
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Reverse),
        label = "c1"
    )
    val c2 by transition.animateColor(
        initialValue = Color(0xFF140C26),
        targetValue = Color(0xFF1B0B2E),
        animationSpec = infiniteRepeatable(tween(3500, easing = LinearEasing), RepeatMode.Reverse),
        label = "c2"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(c1, c2, Color(0xFF05030A))
                )
            )
    )
}

/**
 * Background Video / Image / Stage Selection Modal Dialog
 */
@Composable
fun BackgroundSelectionDialog(
    backgrounds: List<KaraokeBackground>,
    selectedBackground: KaraokeBackground?,
    changeMode: BackgroundChangeMode,
    currentBackgroundFolder: String,
    isTagalog: Boolean = false,
    onOpenFolderLocator: () -> Unit,
    onSelectBackground: (KaraokeBackground) -> Unit,
    onChangeMode: (BackgroundChangeMode) -> Unit,
    onRescanMedia: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: All, 1: Videos, 2: Images, 3: Stage
    val filtered = when (selectedTab) {
        1 -> backgrounds.filter { it.type == BackgroundType.VIDEO }
        2 -> backgrounds.filter { it.type == BackgroundType.IMAGE }
        3 -> backgrounds.filter { it.type == BackgroundType.STAGE }
        else -> backgrounds
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFC107)),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isTagalog) "🎬 BACKGROUND NG VIDEOKEY" else "🎬 VIDEOKEY BACKGROUND",
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFC107),
                        fontSize = 14.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Text(text = "✕", color = Color.White, fontSize = 16.sp)
                    }
                }

                // Dedicated Single Background Folder Locator Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFC107)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                            Text(
                                text = if (isTagalog) "📁 LOKASYON NG BACKGROUND FOLDER:" else "📁 BACKGROUND FOLDER LOCATOR:",
                                fontSize = 9.sp,
                                color = Color(0xFFFFC107),
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = currentBackgroundFolder,
                                fontSize = 11.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isTagalog) "Nahanap: ${backgrounds.size} background media files" else "Found: ${backgrounds.size} background media files",
                                fontSize = 9.sp,
                                color = Color(0xFF94A3B8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Button(
                            onClick = onOpenFolderLocator,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                text = if (isTagalog) "📂 Palitan" else "📂 Change",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }

                // Mode Selector: Auto-Change vs Continuous
                Text(
                    text = if (isTagalog) "PARAAN NG PAGPALIT (CHANGE MODE):" else "BACKGROUND CHANGE MODE:",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Auto-Change Pill
                    val isAuto = changeMode == BackgroundChangeMode.AUTO_CHANGE_PER_SONG
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isAuto) Color(0xFF00E676) else Color(0xFF1E293B))
                            .border(1.dp, if (isAuto) Color(0xFF00FF88) else Color(0xFF334155), RoundedCornerShape(8.dp))
                            .clickable { onChangeMode(BackgroundChangeMode.AUTO_CHANGE_PER_SONG) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isTagalog) "🔀 Auto-Change (Bawat Kanta)" else "🔀 Auto-Change (Per Song)",
                            color = if (isAuto) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }

                    // Continuous Pill
                    val isContinuous = changeMode == BackgroundChangeMode.CONTINUOUS
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isContinuous) Color(0xFF2979FF) else Color(0xFF1E293B))
                            .border(1.dp, if (isContinuous) Color(0xFF448AFF) else Color(0xFF334155), RoundedCornerShape(8.dp))
                            .clickable { onChangeMode(BackgroundChangeMode.CONTINUOUS) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isTagalog) "🔁 Continuous (Tuloy-tuloy)" else "🔁 Continuous (Loop)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Media Filter Tabs: All, Videos, Images, Stage
                val filterTabTitles = if (isTagalog)
                    listOf("Lahat (${backgrounds.size})", "Video", "Larawan", "Stage")
                else
                    listOf("All (${backgrounds.size})", "Videos", "Images", "Stage")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    filterTabTitles.forEachIndexed { idx, tabTitle ->
                        val isSel = selectedTab == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) Color(0xFFFFC107) else Color(0xFF1E293B))
                                .clickable { selectedTab = idx }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tabTitle,
                                color = if (isSel) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Background List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filtered) { bg ->
                        val isSelected = selectedBackground?.id == bg.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFF1E3A2F) else Color(0xFF1E293B))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF00FF88) else Color(0xFF334155),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { onSelectBackground(bg) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = when (bg.type) {
                                        BackgroundType.VIDEO -> "🎬"
                                        BackgroundType.IMAGE -> "🖼️"
                                        BackgroundType.STAGE -> "✨"
                                    },
                                    fontSize = 16.sp
                                )
                                Column {
                                    Text(
                                        text = bg.name,
                                        color = if (isSelected) Color(0xFF00FF88) else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = when (bg.type) {
                                            BackgroundType.VIDEO -> if (isTagalog) "Video (MP4 / Media Loop)" else "Video (MP4 / Media Loop)"
                                            BackgroundType.IMAGE -> if (isTagalog) "Larawan (Wallpaper / Photo)" else "Picture (Wallpaper / Photo)"
                                            BackgroundType.STAGE -> if (isTagalog) "Stage Canvas na may Neon Effect" else "Animated Neon Stage Canvas"
                                        },
                                        color = Color.Gray,
                                        fontSize = 9.sp
                                    )
                                }
                            }

                            if (isSelected) {
                                Text(
                                    text = if (isTagalog) "✓ GINAGAMIT" else "✓ ACTIVE",
                                    color = Color(0xFF00FF88),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Lyric Font Style & Visual Customization Modal Dialog
 */
@Composable
fun LyricStyleDialog(
    config: LyricCustomization,
    onSelectFontFamily: (LyricFontFamily) -> Unit,
    onSelectWipeColor: (LyricWipeColor) -> Unit,
    onSelectShadowStyle: (LyricShadowStyle) -> Unit,
    onToggleAllCaps: () -> Unit,
    onToggleItalic: () -> Unit,
    onAdjustFontScale: (Float) -> Unit,
    onSelectLineCount: (LyricLineCount) -> Unit,
    onSelectPosition: (LyricScreenPosition) -> Unit,
    onApplyPreset: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "lyric_preview")
    val previewWipe by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "preview_wipe"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF818CF8)),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🔤 LYRIC FONT, STYLE & POSITION",
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFA5B4FC),
                            fontSize = 13.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Text(text = "✕", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // LIVE PREVIEW CARD (Simulated TV screen with live wiping!)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF020617), Color(0xFF0B1120), Color(0xFF020617))
                            )
                        )
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "LIVE TV PREVIEW",
                                fontSize = 8.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF312E81)
                            ) {
                                Text(
                                    text = "${config.lineCount.displayName} • ${config.position.displayName}",
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFA5B4FC),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        KaraokeLyricLine(
                            text = "STILL THE ONE",
                            isActive = true,
                            wipePercent = previewWipe,
                            fontSize = 17.sp,
                            lyricConfig = config
                        )
                        KaraokeLyricLine(
                            text = "YOU'RE STILL THE ONE THAT I LOVE",
                            isActive = false,
                            wipePercent = 0f,
                            fontSize = 17.sp,
                            lyricConfig = config
                        )
                        if (config.lineCount == LyricLineCount.FOUR_LINES) {
                            KaraokeLyricLine(
                                text = "THE ONLY ONE I DREAM OF",
                                isActive = false,
                                wipePercent = 0f,
                                fontSize = 15.sp,
                                lyricConfig = config
                            )
                            KaraokeLyricLine(
                                text = "STILL THE ONE I KISS GOODNIGHT",
                                isActive = false,
                                wipePercent = 0f,
                                fontSize = 15.sp,
                                lyricConfig = config
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Customization Options
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // SECTION 0A: LINE COUNT (2 LINES VS 4 LINES)
                    item {
                        Text(
                            text = "📄 BILANG NG LINYA NG LYRICS (LINE COUNT):",
                            fontSize = 11.sp,
                            color = Color(0xFFFFD54F),
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LyricLineCount.values().forEach { lineOption ->
                                val isSelected = config.lineCount == lineOption
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFF312E81) else Color(0xFF1E293B),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF818CF8) else Color(0xFF334155)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onSelectLineCount(lineOption) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isSelected) "✓ ${lineOption.displayName}" else lineOption.displayName,
                                            fontSize = 10.5.sp,
                                            color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // SECTION 0B: POSITION NG LYRICS (LOWER THIRD, CENTER, TOP)
                    item {
                        Text(
                            text = "📍 POSISYON NG LYRICS SA SCREEN (POSITION):",
                            fontSize = 11.sp,
                            color = Color(0xFFFFD54F),
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LyricScreenPosition.values().forEach { posOption ->
                                val isSelected = config.position == posOption
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFF312E81) else Color(0xFF1E293B),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF818CF8) else Color(0xFF334155)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onSelectPosition(posOption) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isSelected) "✓ ${posOption.displayName}" else posOption.displayName,
                                            fontSize = 9.5.sp,
                                            color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // SECTION 1: INSTANT PRESETS
                    item {
                        Text(
                            text = "⚡ QUICK PRESETS (MGA TEMA):",
                            fontSize = 11.sp,
                            color = Color(0xFFA5B4FC),
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val presets = listOf(
                                "live_tv" to "📺 Live TV Blue",
                                "platinum" to "🌟 Platinum",
                                "cyber" to "🪩 Cyber Neon",
                                "pop" to "🎤 Party Pink",
                                "gold" to "👑 Pure Gold",
                                "ballad" to "🍂 Ballad"
                            )
                            presets.forEach { (presetKey, presetLabel) ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1E293B),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onApplyPreset(presetKey) }
                                ) {
                                    Text(
                                        text = presetLabel,
                                        fontSize = 9.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // SECTION 2: FONT FAMILY (TYPEFACE)
                    item {
                        Text(
                            text = "🔤 FONT FAMILY (ESTILO NG TITIK):",
                            fontSize = 11.sp,
                            color = Color(0xFFA5B4FC),
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LyricFontFamily.values().forEach { family ->
                                val isSelected = config.fontFamily == family
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFF312E81) else Color(0xFF1E293B))
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) Color(0xFF818CF8) else Color(0xFF334155),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { onSelectFontFamily(family) }
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val sampleFont = when (family) {
                                        LyricFontFamily.SAN_SERIF -> FontFamily.SansSerif
                                        LyricFontFamily.MONOSPACE -> FontFamily.Monospace
                                        LyricFontFamily.SERIF -> FontFamily.Serif
                                        LyricFontFamily.CURSIVE -> FontFamily.Cursive
                                    }
                                    Text(
                                        text = family.displayName,
                                        fontFamily = sampleFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                                    )
                                    if (isSelected) {
                                        Text(
                                            text = "✓ AKTIBO",
                                            color = Color(0xFF818CF8),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // SECTION 3: WIPE COLOR (KULAY NG KANTA)
                    item {
                        Text(
                            text = "🎨 WIPE COLOR (KULAY NG KINAKANTA):",
                            fontSize = 11.sp,
                            color = Color(0xFFA5B4FC),
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            LyricWipeColor.values().forEach { wipeColor ->
                                val isSelected = config.wipeColor == wipeColor
                                val c = Color(wipeColor.activeColor)
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) Color.White else Color.Black.copy(alpha = 0.5f),
                                            shape = CircleShape
                                        )
                                        .clickable { onSelectWipeColor(wipeColor) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Text(
                                            text = "✓",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // SECTION 4: SHADOW & GLOW EFFECTS
                    item {
                        Text(
                            text = "✨ SHADOW & OUTLINE (EPEKTO / ANINO):",
                            fontSize = 11.sp,
                            color = Color(0xFFA5B4FC),
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val shadows = listOf(
                                LyricShadowStyle.HEAVY_OUTLINE to "Outline",
                                LyricShadowStyle.NEON_GLOW to "Neon Glow",
                                LyricShadowStyle.SUBTLE_DROP to "3D Drop",
                                LyricShadowStyle.CLEAN to "Clean"
                            )
                            shadows.forEach { (shadow, label) ->
                                val isSelected = config.shadowStyle == shadow
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFF312E81) else Color(0xFF1E293B),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF818CF8) else Color(0xFF334155)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onSelectShadowStyle(shadow) }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 7.dp)
                                    )
                                }
                            }
                        }
                    }

                    // SECTION 5: TEXT FORMATTING (ALL CAPS & ITALIC)
                    item {
                        Text(
                            text = "✍️ TEXT CASING AT FORMAT:",
                            fontSize = 11.sp,
                            color = Color(0xFFA5B4FC),
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // ALL CAPS Toggle
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (config.isAllCaps) Color(0xFF312E81) else Color(0xFF1E293B),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (config.isAllCaps) 1.5.dp else 1.dp,
                                    color = if (config.isAllCaps) Color(0xFF818CF8) else Color(0xFF334155)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onToggleAllCaps() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (config.isAllCaps) "🔠 ALL CAPS: ON" else "🔡 Normal Case",
                                        fontSize = 10.sp,
                                        color = if (config.isAllCaps) Color.White else Color(0xFF94A3B8),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Italic Toggle
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (config.isItalic) Color(0xFF312E81) else Color(0xFF1E293B),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (config.isItalic) 1.5.dp else 1.dp,
                                    color = if (config.isItalic) Color(0xFF818CF8) else Color(0xFF334155)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onToggleItalic() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (config.isItalic) "🖋️ Italic: ON" else "🖋️ Normal (Tindig)",
                                        fontSize = 10.sp,
                                        color = if (config.isItalic) Color.White else Color(0xFF94A3B8),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // SECTION 6: FONT SIZE SCALE
                    item {
                        Text(
                            text = "📐 LAKI NG TITIK (FONT SIZE SCALE):",
                            fontSize = 11.sp,
                            color = Color(0xFFA5B4FC),
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val scales = listOf(
                                0.85f to "Maliit (85%)",
                                1.0f to "Normal (100%)",
                                1.15f to "Malaki (115%)",
                                1.30f to "Extra (130%)"
                            )
                            scales.forEach { (scale, label) ->
                                val isSelected = kotlin.math.abs(config.fontScale - scale) < 0.05f
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFF312E81) else Color(0xFF1E293B),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF818CF8) else Color(0xFF334155)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onAdjustFontScale(scale) }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 9.sp,
                                        color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 7.dp, horizontal = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // DONE / CLOSE BUTTON
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                ) {
                    Text(
                        text = "ILAPAT & ISARA (APPLY & CLOSE) ✓",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Settings Modal Dialog:
 * Configures Microphone Scoring, Keypad Number Voice, and TV Fullscreen options.
 */
@Composable
fun AppSettingsDialog(
    state: VideoKeyUiState,
    isLandscape: Boolean = false,
    onToggleMicScoring: (Boolean) -> Unit,
    onAdjustMicSensitivity: (Float) -> Unit,
    onToggleKeypadVoice: (Boolean) -> Unit,
    onToggleTvMode: () -> Unit,
    onAdjustMelody: (Int) -> Unit = {},
    onSelectLanguage: (com.example.videokey.ui.main.AppLanguage) -> Unit = {},
    onCheckForUpdates: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val isTagalog = state.appLanguage == com.example.videokey.ui.main.AppLanguage.TAGALOG

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F172A),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF38BDF8)),
            modifier = Modifier
                .fillMaxWidth(if (isLandscape) 0.85f else 0.95f)
                .fillMaxHeight(if (isLandscape) 0.92f else 0.88f)
                .padding(if (isLandscape) 8.dp else 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isLandscape) 12.dp else 16.dp)
            ) {
                // Header Bar (Fixed)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = if (isLandscape) "📺" else "⚙️", fontSize = 18.sp)
                        Text(
                            text = if (isLandscape) {
                                if (isTagalog) "TV MODE • MGA SETTING NG VIDEOKEY" else "TV MODE • VIDEOKEY SETTINGS"
                            } else {
                                if (isTagalog) "MGA SETTING NG VIDEOKEY" else "VIDEOKEY SETTINGS"
                            },
                            fontWeight = FontWeight.Black,
                            color = if (isLandscape) Color(0xFFFFD54F) else Color(0xFF38BDF8),
                            fontSize = 13.sp
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Text(text = "✕", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFF334155), thickness = 1.dp)

                // Scrollable Content Column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                // OPTION 0: WIKA / LANGUAGE (Tagalog at English Lamang, Default: Tagalog)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isTagalog) "🌐 WIKA / LANGUAGE" else "🌐 LANGUAGE",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                            Text(
                                text = if (isTagalog) "Piliin ang wika (Tagalog o English)" else "Select language (Tagalog or English)",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Tagalog Button (Default)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isTagalog) Color(0xFF0284C7) else Color(0xFF0F172A))
                                .border(
                                    width = if (isTagalog) 2.dp else 1.dp,
                                    color = if (isTagalog) Color(0xFF38BDF8) else Color(0xFF334155),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onSelectLanguage(com.example.videokey.ui.main.AppLanguage.TAGALOG) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🇵🇭 Tagalog (Default)",
                                fontWeight = if (isTagalog) FontWeight.Black else FontWeight.Bold,
                                color = if (isTagalog) Color.White else Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }

                        // English Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!isTagalog) Color(0xFF0284C7) else Color(0xFF0F172A))
                                .border(
                                    width = if (!isTagalog) 2.dp else 1.dp,
                                    color = if (!isTagalog) Color(0xFF38BDF8) else Color(0xFF334155),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onSelectLanguage(com.example.videokey.ui.main.AppLanguage.ENGLISH) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🇺🇸 English",
                                fontWeight = if (!isTagalog) FontWeight.Black else FontWeight.Bold,
                                color = if (!isTagalog) Color.White else Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // OPTION 1: SCORING GAMIT ANG MIKROPONO & SENSITIVITY ADJUSTER
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isTagalog) "🎤 SCORING GAMIT ANG MIKROPONO" else "🎤 MICROPHONE SCORING",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                            Text(
                                text = if (state.isMicScoringEnabled) (if (isTagalog) "BUKAS (Naka-on ang Mic Scoring)" else "ON (Mic Scoring Enabled)") else (if (isTagalog) "SARADO (Naka-off ang Mic Scoring)" else "OFF (Mic Scoring Disabled)"),
                                fontSize = 10.sp,
                                color = if (state.isMicScoringEnabled) Color(0xFF00FF88) else Color(0xFF94A3B8)
                            )
                        }
                        Switch(
                            checked = state.isMicScoringEnabled,
                            onCheckedChange = { onToggleMicScoring(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF00E676),
                                checkedTrackColor = Color(0xFF064E3B)
                            )
                        )
                    }

                    // Microphone Sensitivity Adjuster Controls
                    if (state.isMicScoringEnabled) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isTagalog) "🎚️ LAKAS / SENSITIVITY: ${(state.micSensitivity * 100).toInt()}%" else "🎚️ SENSITIVITY: ${(state.micSensitivity * 100).toInt()}%",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Button(
                                        onClick = {
                                            val newVal = ((state.micSensitivity - 0.2f) * 10).toInt() / 10f
                                            onAdjustMicSensitivity(newVal.coerceAtLeast(0.2f))
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.height(26.dp)
                                    ) {
                                        Text(text = "–", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    Button(
                                        onClick = {
                                            val newVal = ((state.micSensitivity + 0.2f) * 10).toInt() / 10f
                                            onAdjustMicSensitivity(newVal.coerceAtMost(3.0f))
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.height(26.dp)
                                    ) {
                                        Text(text = "+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                            }

                            Slider(
                                value = state.micSensitivity,
                                onValueChange = { onAdjustMicSensitivity(it) },
                                valueRange = 0.2f..3.0f,
                                steps = 13,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF38BDF8),
                                    activeTrackColor = Color(0xFF0284C7),
                                    inactiveTrackColor = Color(0xFF334155)
                                ),
                                modifier = Modifier.fillMaxWidth().height(20.dp)
                            )

                            // Live mic input level indicator
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (state.isSingingDetected) (if (isTagalog) "🎙️ KUMAKANTA:" else "🎙️ SINGING:") else (if (isTagalog) "🎙️ TUNOG NG BOSES:" else "🎙️ VOICE INPUT:"),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.isSingingDetected) Color(0xFF00FF88) else Color(0xFF94A3B8)
                                )
                                LinearProgressIndicator(
                                    progress = { state.micLevel },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = if (state.isSingingDetected) Color(0xFF00E676) else Color(0xFFFFD54F),
                                    trackColor = Color(0xFF334155)
                                )
                                Text(
                                    text = "${(state.micLevel * 100).toInt()}%",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Text(
                        text = if (isTagalog)
                            "I-adjust ang sensitivity ayon sa layo o lakas ng mic. Sinusukat ang boses habang tumutugtog ang kanta para sa final score."
                        else
                            "Adjust sensitivity based on distance or mic loudness. Voice is measured during singing for the final score.",
                        fontSize = 9.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                // OPTION 2: BOSES SA PINDUTAN NG NUMERO (0-9)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isTagalog) "🗣️ BOSES SA MGA NUMERO (0-9)" else "🗣️ KEYPAD VOICE (0-9)",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                            Text(
                                text = if (state.isKeypadVoiceEnabled) (if (isTagalog) "BUKAS (May boses ang keypad)" else "ON (Keypad voice enabled)") else (if (isTagalog) "SARADO (Walang boses ang keypad)" else "OFF (Keypad voice disabled)"),
                                fontSize = 10.sp,
                                color = if (state.isKeypadVoiceEnabled) Color(0xFF00FF88) else Color(0xFF94A3B8)
                            )
                        }
                        Switch(
                            checked = state.isKeypadVoiceEnabled,
                            onCheckedChange = { onToggleKeypadVoice(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF38BDF8),
                                checkedTrackColor = Color(0xFF0C4A6E)
                            )
                        )
                    }
                    Text(
                        text = if (isTagalog)
                            "Binibigkas ang numero (Zero hanggang Nine) kapag pumipindot sa keypad. Ang mga numero lamang ang may boses."
                        else
                            "Pronounces numbers (Zero to Nine) when pressing the keypad. Only numbers have voice prompts.",
                        fontSize = 9.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                // OPTION 3: TV FULLSCREEN MODE
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isTagalog) "📺 TV FULLSCREEN MODE" else "📺 TV FULLSCREEN MODE",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                            Text(
                                text = if (state.isTvMode) (if (isTagalog) "BUKAS (Puno ang TV screen)" else "ON (Full TV Screen)") else (if (isTagalog) "SARADO (May Remote Console)" else "OFF (With Remote Console)"),
                                fontSize = 10.sp,
                                color = if (state.isTvMode) Color(0xFF00FF88) else Color(0xFF94A3B8)
                            )
                        }
                        Switch(
                            checked = state.isTvMode,
                            onCheckedChange = { onToggleTvMode() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFFFB300),
                                checkedTrackColor = Color(0xFF78350F)
                            )
                        )
                    }
                }

                // OPTION 4: MELODY GUIDE TRACK VOLUME
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isTagalog) "🎵 MELODY GUIDE (LAKAS NG GUIDE SYNTH)" else "🎵 MELODY GUIDE (SYNTH GUIDE VOLUME)",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                            Text(
                                text = if (state.melodyPercent == 0) (if (isTagalog) "OFF (Minus-One / Walang Gabay)" else "OFF (Minus-One / Accompaniment Only)") else "${state.melodyPercent}% Volume",
                                fontSize = 10.sp,
                                color = if (state.melodyPercent == 0) Color(0xFFFF5252) else Color(0xFF00E5FF)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(
                                onClick = { onAdjustMelody(-10) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text(text = "–", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Button(
                                onClick = { onAdjustMelody(10) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text(text = "+", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                    Slider(
                        value = state.melodyPercent.toFloat(),
                        onValueChange = { onAdjustMelody(it.toInt() - state.melodyPercent) },
                        valueRange = 0f..100f,
                        steps = 9,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00E5FF),
                            activeTrackColor = Color(0xFF00B0FF),
                            inactiveTrackColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.fillMaxWidth().height(20.dp)
                    )
                }

                // Close Button
                // OPTION 5: WI-FI MOBILE REMOTE & QR BARCODE (FOR ANDROID TV)
                val deviceIp = remember { com.example.videokey.remote.VideokeRemoteServer.getLocalIpAddress() }
                val qrBitmap = remember(deviceIp) {
                    com.example.videokey.remote.QrCodeGenerator.generateQrBitmap("http://$deviceIp:8080", 380)
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.5.dp, Color(0xFF00FF88), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "📱", fontSize = 16.sp)
                        Text(
                            text = if (isTagalog) "WI-FI REMOTE & QR BARCODE" else "WI-FI REMOTE & QR BARCODE",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = Color(0xFF00FF88)
                        )
                    }
                    Text(
                        text = if (isTagalog)
                            "Gamitin ang iyong cellphone bilang remote control ng TV. Siguraduhing pareho ng Wi-Fi ang TV at cellphone, at buksan ang link o i-scan ang QR barcode sa ibaba gamit ang cellphone camera:"
                        else
                            "Use your phone as a TV videoke remote control. Make sure TV and phone are on the same Wi-Fi, then open this link or scan the QR barcode below:",
                        fontSize = 10.sp,
                        color = Color(0xFFCBD5E1)
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "http://$deviceIp:8080",
                            color = Color(0xFFFFD54F),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    if (qrBitmap != null) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "QR Code Remote Barcode",
                                    modifier = Modifier
                                        .size(if (isLandscape) 140.dp else 170.dp)
                                        .padding(8.dp)
                                )
                            }
                            Text(
                                text = if (isTagalog) "📷 I-SCAN ANG QR BARCODE GAMIT ANG CELLPHONE CAMERA" else "📷 SCAN QR BARCODE WITH PHONE CAMERA TO CONNECT",
                                color = Color(0xFF00FF88),
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // OPTION 6: AUTOMATIC OVER-THE-AIR (OTA) FORCE UPDATE
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "🚀", fontSize = 16.sp)
                        Text(
                            text = if (isTagalog) "AUTOMATIC FORCE UPDATE (OTA)" else "AUTOMATIC FORCE UPDATE (OTA)",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = Color(0xFF00E5FF)
                        )
                    }
                    Text(
                        text = if (isTagalog)
                            "Awtomatikong mag-che-check at mag-fo-force update ang VideoKey gamit ang Wi-Fi o mobile data kapag may mas bagong version na inilabas kahit sa ibang network pa nakakonekta."
                        else
                            "VideoKey will automatically check and force update over Wi-Fi or data whenever a newer version is released regardless of network connection.",
                        fontSize = 10.sp,
                        color = Color(0xFFCBD5E1)
                    )
                    Button(
                        onClick = onCheckForUpdates,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                    ) {
                        Text(
                            text = if (isTagalog) "🔄 SURIIN KUNG MAY BAGONG UPDATE" else "🔄 CHECK FOR UPDATES NOW",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(42.dp)
            ) {
                Text(text = if (isTagalog) "TAPOS NA / ISARA ✓" else "DONE / CLOSE ✓", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
}

/**
 * In-App Folder Browser Dialog for selecting exact Song Folder or Background Folder.
 * Allows effortless browsing of device directories and quick bookmarks without external SAF picker issues.
 */
@Composable
fun FolderBrowserDialog(
    title: String,
    initialPath: String,
    onFolderSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val defaultRoot = android.os.Environment.getExternalStorageDirectory()?.absolutePath ?: "/storage/emulated/0"
    var currentPath by remember {
        mutableStateOf(if (initialPath.isNotBlank() && File(initialPath).exists()) initialPath else defaultRoot)
    }

    val currentDir = File(currentPath)
    val subFolders = remember(currentPath) {
        try {
            if (currentDir.exists() && currentDir.isDirectory && currentDir.canRead()) {
                currentDir.listFiles()
                    ?.filter { it.isDirectory && !it.name.startsWith(".") }
                    ?.sortedBy { it.name.lowercase() }
                    ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF0F172A),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF38BDF8)),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF38BDF8),
                        fontSize = 13.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Text(text = "✕", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Current Path Display + Up Button
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                            Text(
                                text = "KASALUKUYANG FOLDER:",
                                fontSize = 8.sp,
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentPath,
                                fontSize = 11.sp,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        val parent = currentDir.parentFile
                        if (parent != null && parent.canRead() && parent.path.length >= 4) {
                            Button(
                                onClick = { currentPath = parent.absolutePath },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(text = "⬆️ Taas", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Quick Shortcut Bookmarks
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val shortcuts = listOf(
                        "📁 Root" to "/storage/emulated/0",
                        "📁 VideoKey" to "/storage/emulated/0/VideoKey",
                        "📁 Download" to "/storage/emulated/0/Download"
                    )
                    shortcuts.forEach { (label, path) ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF1E293B),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val f = File(path)
                                    if (f.exists()) currentPath = f.absolutePath
                                }
                        ) {
                            Text(
                                text = label,
                                fontSize = 9.sp,
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "MGA SUBFOLDER (${subFolders.size}):",
                    fontSize = 9.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )

                // Subfolder LazyColumn
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (subFolders.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Walang karagdagang subfolder dito.\nI-click ang 'Piliin ang Folder na Ito' sa ibaba.",
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(subFolders) { folder ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1E293B))
                                    .clickable { currentPath = folder.absolutePath }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = "📁", fontSize = 16.sp)
                                    Text(
                                        text = folder.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "Buksan ▶",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Select Folder Action Button
                Button(
                    onClick = { onFolderSelected(currentPath) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                ) {
                    Text(
                        text = "✓ PILIIN ANG FOLDER NA ITO",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

/**
 * Voice + Music Performance Recordings Modal Dialog (🎙️ / 🔴)
 * Allows users to record voice with studio echo mixed with MIDI accompaniment into .wav files,
 * preview/playback recordings in-app, and manage saved performances in /storage/emulated/0/VideoKey/recordings.
 */
@Composable
fun RecordingsDialog(
    state: VideoKeyUiState,
    isLandscape: Boolean = false,
    onToggleRecording: () -> Unit,
    onPlayRecording: (String) -> Unit,
    onDeleteRecording: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val isTagalog = state.appLanguage == com.example.videokey.ui.main.AppLanguage.TAGALOG

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF0F172A),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFF5252)),
            modifier = Modifier
                .fillMaxWidth(if (isLandscape) 0.85f else 0.94f)
                .fillMaxHeight(if (isLandscape) 0.92f else 0.86f)
                .padding(6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = if (state.isRecording) "🔴" else "🎙️", fontSize = 18.sp)
                        Text(
                            text = if (isTagalog) "RECORDING NG BOSES AT TUGTOG" else "VOICE & MUSIC RECORDINGS",
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFF5252),
                            fontSize = 13.sp
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Text(text = "✕", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = Color(0xFF334155), thickness = 1.dp)

                // Recording Control Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.isRecording) Color(0xFF450A0A) else Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (state.isRecording) Color(0xFFEF4444) else Color(0xFF38BDF8)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (state.isRecording) {
                            val recMins = state.recordingSeconds / 60
                            val recSecs = state.recordingSeconds % 60
                            Text(
                                text = String.format("🔴 NAGRE-RECORD: %02d:%02d", recMins, recSecs),
                                color = Color(0xFFFF5252),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                            Text(
                                text = if (isTagalog)
                                    "Maririnig ang boses na may warm studio echo kasama ang tugtog ng kanta."
                                else
                                    "Recording singer voice with studio warm echo mixed with MIDI accompaniment.",
                                color = Color(0xFFCBD5E1),
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = onToggleRecording,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth(if (isLandscape) 0.6f else 0.85f)
                                    .height(40.dp)
                            ) {
                                Text(
                                    text = if (isTagalog) "⏹ ITIGIL AT I-SAVE ANG RECORDING" else "⏹ STOP & SAVE RECORDING",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.5.sp,
                                    color = Color.White
                                )
                            }
                        } else {
                            Text(
                                text = if (isTagalog) "🎙️ HANDANG MAG-RECORD" else "🎙️ READY TO RECORD",
                                color = Color(0xFF00FF88),
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                            Text(
                                text = if (isTagalog)
                                    "Pindutin ang pindutan sa ibaba habang kumakanta upang mai-save ang iyong boses at tugtog bilang WAV file."
                                else
                                    "Press the button below while singing to record your vocals and music accompaniment as a WAV file.",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = onToggleRecording,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth(if (isLandscape) 0.6f else 0.85f)
                                    .height(40.dp)
                            ) {
                                Text(
                                    text = if (isTagalog) "🔴 SIMULAN ANG RECORDING" else "🔴 START RECORDING",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.5.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Saved Recordings Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isTagalog) "MGA NAI-SAVE NA RECORDING (${state.savedRecordings.size}):" else "SAVED RECORDINGS (${state.savedRecordings.size}):",
                        color = Color(0xFFFFD54F),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "/VideoKey/recordings",
                        color = Color(0xFF64748B),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Recordings LazyColumn
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (state.savedRecordings.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isTagalog)
                                        "Wala pang nai-save na recording.\nPindutin ang 'SIMULAN ANG RECORDING' habang kumakanta!"
                                    else
                                        "No saved recordings yet.\nTap 'START RECORDING' while singing!",
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(state.savedRecordings) { rec ->
                            val isPlayingThis = state.playingRecordingPath == rec.filePath
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isPlayingThis) Color(0xFF0F3B24) else Color(0xFF1E293B))
                                    .border(1.dp, if (isPlayingThis) Color(0xFF00FF88) else Color(0xFF334155), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                                    Text(
                                        text = rec.songTitle,
                                        color = if (isPlayingThis) Color(0xFF00FF88) else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${rec.dateLabel} • ${rec.durationLabel} • ${rec.sizeKb} KB",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 9.5.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Play / Stop Playback Button
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isPlayingThis) Color(0xFFFF1744) else Color(0xFF00E676),
                                        modifier = Modifier.clickable { onPlayRecording(rec.filePath) }
                                    ) {
                                        Text(
                                            text = if (isPlayingThis) "⏹ ITIGIL" else "▶ TUGTUGIN",
                                            color = if (isPlayingThis) Color.White else Color.Black,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    // Delete Button
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF334155),
                                        modifier = Modifier.clickable { onDeleteRecording(rec.filePath) }
                                    ) {
                                        Text(
                                            text = "🗑️",
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Done Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                ) {
                    Text(
                        text = if (isTagalog) "TAPOS NA / ISARA ✓" else "DONE / CLOSE ✓",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Over-The-Air (OTA) Force-Update Dialog:
 * Un-dismissible when force-updating, streams download progress, and triggers install.
 */
@Composable
fun ForceUpdateDialog(
    updateState: com.example.videokey.update.AppUpdateManager.UpdateState,
    isLandscape: Boolean = false,
    onRetry: () -> Unit,
    onInstall: (java.io.File) -> Unit,
    onDismiss: () -> Unit
) {
    val isDownloading = updateState is com.example.videokey.update.AppUpdateManager.UpdateState.Downloading
    val isForce = when (updateState) {
        is com.example.videokey.update.AppUpdateManager.UpdateState.UpdateAvailable -> updateState.info.isForceUpdate
        is com.example.videokey.update.AppUpdateManager.UpdateState.Downloading -> updateState.info.isForceUpdate
        is com.example.videokey.update.AppUpdateManager.UpdateState.ReadyToInstall -> updateState.info.isForceUpdate
        else -> false
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = {
            if (!isForce && !isDownloading) onDismiss()
        },
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = !isForce && !isDownloading,
            dismissOnClickOutside = !isForce && !isDownloading,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A),
            border = androidx.compose.foundation.BorderStroke(2.5.dp, Color(0xFF00E5FF)),
            modifier = Modifier
                .fillMaxWidth(if (isLandscape) 0.65f else 0.92f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (updateState) {
                    is com.example.videokey.update.AppUpdateManager.UpdateState.Checking -> {
                        Text(text = "🔄", fontSize = 36.sp)
                        Text(
                            text = "SINI-SURI ANG UPDATE...",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = Color(0xFF00E5FF)
                        )
                        Text(
                            text = updateState.message,
                            fontSize = 12.sp,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center
                        )
                        androidx.compose.material3.CircularProgressIndicator(
                            color = Color(0xFF00E5FF),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    is com.example.videokey.update.AppUpdateManager.UpdateState.UpdateAvailable -> {
                        Text(text = "🚀", fontSize = 38.sp)
                        Text(
                            text = "BAGONG UPDATE: v${updateState.info.versionName}",
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            color = Color(0xFFFFD54F)
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E293B),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "MGA BAGONG DAGDAG / PAGBABAGO:",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    color = Color(0xFF00FF88)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = updateState.info.releaseNotes,
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            }
                        }
                        Text(
                            text = "🌐 Awtomatikong ida-download gamit ang Wi-Fi o data.",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                    }

                    is com.example.videokey.update.AppUpdateManager.UpdateState.Downloading -> {
                        Text(text = "⬇️", fontSize = 38.sp)
                        Text(
                            text = "KINAKAILANGANG I-UPDATE ANG VIDEOKEY",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = Color(0xFF00E5FF),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Bersyon ${updateState.info.versionName} • Huwag munang isara ang app habang dina-download ang update.",
                            fontSize = 11.5.sp,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center
                        )

                        // Progress Bar
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { updateState.progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            color = Color(0xFF00FF88),
                            trackColor = Color(0xFF1E293B)
                        )

                        val mbDownloaded = updateState.downloadedBytes / (1024f * 1024f)
                        val mbTotal = updateState.totalBytes / (1024f * 1024f)
                        Text(
                            text = "${updateState.progressPercent}%  (${String.format("%.1f", mbDownloaded)} MB / ${String.format("%.1f", mbTotal)} MB)",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = Color(0xFFFFD54F)
                        )
                    }

                    is com.example.videokey.update.AppUpdateManager.UpdateState.ReadyToInstall -> {
                        Text(text = "🎉", fontSize = 42.sp)
                        Text(
                            text = "TAPOS NA ANG PAG-DOWNLOAD!",
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            color = Color(0xFF00FF88)
                        )
                        Text(
                            text = "Handa nang i-install ang VideoKey v${updateState.info.versionName}. Pindutin ang pindutan sa ibaba para ituloy ang pag-install:",
                            fontSize = 12.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { onInstall(updateState.apkFile) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text(
                                text = "📦 I-INSTALL ANG UPDATE NGAYON",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = Color.Black
                            )
                        }
                        Text(
                            text = "💡 Hindi mawawala ang iyong mga kanta, paborito, at recordings.",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                    }

                    is com.example.videokey.update.AppUpdateManager.UpdateState.Error -> {
                        Text(text = "⚠️", fontSize = 38.sp)
                        Text(
                            text = "HINDI MATAPOS ANG UPDATE",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = Color(0xFFFF5252)
                        )
                        Text(
                            text = updateState.message,
                            fontSize = 12.sp,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onRetry,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                            ) {
                                Text(
                                    text = "🔄 SUBUKANG MULI",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    color = Color.White
                                )
                            }
                            if (!isForce) {
                                Button(
                                    onClick = onDismiss,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier
                                        .weight(0.7f)
                                        .height(42.dp)
                                ) {
                                    Text(
                                        text = "ISARA",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    else -> Unit
                }
            }
        }
    }
}


