package com.example.videokey.ui.main







import android.content.res.Configuration



import androidx.compose.animation.AnimatedVisibility



import androidx.compose.animation.animateColor



import androidx.compose.animation.core.*



import androidx.compose.foundation.background



import androidx.compose.foundation.basicMarquee



import androidx.compose.foundation.BorderStroke



import androidx.compose.foundation.border



import androidx.compose.foundation.clickable



import androidx.compose.foundation.focusable

import androidx.compose.ui.focus.onFocusChanged



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



import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.draw.alpha



import androidx.compose.ui.draw.clip

import androidx.compose.ui.draw.clipToBounds



import androidx.compose.ui.draw.scale



import androidx.compose.ui.draw.shadow



import androidx.compose.ui.graphics.Brush



import androidx.compose.ui.graphics.Color



import androidx.compose.ui.platform.LocalConfiguration



import androidx.compose.ui.geometry.Offset



import androidx.compose.ui.graphics.Shadow



import androidx.compose.ui.graphics.drawscope.Stroke



import androidx.compose.ui.graphics.StrokeJoin



import androidx.compose.ui.text.SpanStyle



import androidx.compose.ui.text.TextStyle



import androidx.compose.ui.text.buildAnnotatedString



import androidx.compose.ui.text.font.FontFamily



import androidx.compose.ui.text.font.FontStyle



import androidx.compose.ui.text.font.Font



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



import androidx.compose.ui.platform.LocalClipboardManager



import androidx.compose.ui.res.painterResource



import androidx.compose.ui.viewinterop.AndroidView



import android.webkit.WebView



import android.webkit.WebViewClient



import android.webkit.WebChromeClient



import kotlinx.coroutines.launch



import kotlinx.coroutines.delay



import java.io.File



import androidx.compose.ui.unit.dp



import androidx.compose.ui.unit.sp



import androidx.compose.ui.window.Dialog



import com.example.videokey.data.Song



import com.example.videokey.R



import androidx.compose.ui.zIndex







import androidx.compose.foundation.gestures.detectTapGestures



import androidx.compose.ui.input.pointer.pointerInput







private val CleanFont = FontFamily(Font(R.font.radio_canada))







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



    val isTagalog = state.appLanguage == AppLanguage.TAGALOG







    // Automatically trigger internal storage scan on app launch



    LaunchedEffect(Unit) {



        viewModel.scanInternalStorage(context)



    }







    var showExitConfirmDialog by remember { mutableStateOf(false) }







    androidx.activity.compose.BackHandler(enabled = true) {



        when {



            !state.isStartupGatePassed -> activity?.finish()



            showExitConfirmDialog -> showExitConfirmDialog = false



            state.isAdminKeyGeneratorOpen -> viewModel.closeAdminKeyGenerator()



            state.isEditNameDialogOpen -> viewModel.closeEditNameDialog()



            state.isYouTubeDownloaderOpen -> viewModel.closeYouTubeDownloader()



            state.isSettingsDialogOpen -> viewModel.closeSettingsDialog()



            state.isAudioMixerDialogOpen -> viewModel.closeAudioMixerDialog()



            state.isSoundFontDialogOpen -> viewModel.closeSoundFontDialog()



            state.isBackgroundDialogOpen -> viewModel.closeBackgroundDialog()



            state.isLyricStyleDialogOpen -> viewModel.closeLyricStyleDialog()



            state.isRecordingsDialogOpen -> viewModel.closeRecordingsDialog()



            state.isSongFolderPickerOpen -> viewModel.closeSongFolderPicker()



            state.showScoreModal -> viewModel.closeScoreModal()



            state.isSongbookOpen -> viewModel.closeSongbook()



            state.isReserveQueueOpen -> viewModel.closeReserveQueueOverlay()



            state.isFavoritesOpen -> viewModel.closeFavoritesOverlay()



            state.isTvMode && !viewModel.isRunningOnTvDevice -> viewModel.exitLandscapeMode(activity)



            state.selectedBottomNavTab != 0 -> viewModel.selectBottomNavTab(0)



            else -> showExitConfirmDialog = true



        }



    }







    if (!state.isStartupGatePassed) {



        val hasSoundFont = viewModel.contentDownloadManager.isSoundFontInstalled()



        val hasSongs = viewModel.contentDownloadManager.hasAnySongsInstalled()



        val hasBackgrounds = viewModel.contentDownloadManager.isBackgroundVideoInstalled()



        StartupDownloadGateScreen(



            state = state,



            hasSoundFont = hasSoundFont,



            hasSongs = hasSongs,



            hasBackgrounds = hasBackgrounds,



            onStartDownload = { viewModel.downloadContentItem(it) },



            onStartSequentialDownload = { viewModel.startSequentialAutoDownload() },



            onCancelDownload = { viewModel.cancelContentDownload() },



            onOpenApp = { viewModel.passStartupGate() },



            onExitApp = { activity?.finish() },



            modifier = modifier.fillMaxSize()



        )



        return



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
                onSongCompleted = { viewModel.onSongCompleted() },
                onTogglePause = { viewModel.togglePlaybackPause() },
                onToggleCameraFacing = { viewModel.toggleCameraFacing() },



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



                onOpenYouTubeDownloader = { query -> viewModel.openYouTubeDownloader(query) },



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



                onStopYouTubeStream = { viewModel.stopYouTubeKaraokeDirect(activity) },



                modifier = Modifier.fillMaxSize()



            )



        } else {



            // SingPulse Modern Multi-Tab Layout for Android Phone (Portrait)



            Column(



                modifier = Modifier.fillMaxSize()



            ) {



                Box(



                    modifier = Modifier



                        .fillMaxWidth()



                        .weight(1f)



                ) {



                    when (state.selectedBottomNavTab) {



                        0 -> {



                            // TAB 0: HOME SCREEN (SingPulse Discovery, Categories, Featured Hit, Trending)



                            SingPulseHomeScreen(



                                state = state,



                                onQueryChange = { viewModel.searchSongbook(it) },



                                onSelectCategory = { viewModel.selectCategoryFilter(it) },



                                onPlaySong = { viewModel.playSong(it) },



                                onReserveSong = { viewModel.reserveSong(it) },



                                onEnterTvMode = { viewModel.enterTvLandscapeMode(activity) },



                                onOpenProfile = { viewModel.selectBottomNavTab(3) },



                                onOpenEditName = { viewModel.openEditNameDialog() },



                                onOpenAdminKeyGenerator = { viewModel.openAdminKeyGenerator() },



                                                                                                onOpenYouTubeDownloader = { query -> viewModel.openYouTubeDownloader(query) },



                                onOpenDownloadCenter = { viewModel.openDownloadCenter() },

                                onSpeakDigit = { viewModel.speakDigit(it) },

                                onRefreshRandomFavorite = { viewModel.refreshRandomFavorite() }

                            )



                        }



                        1 -> {



                            // TAB 1: SING (Split-Screen Player: Top TV lyrics + Bottom Keypad Remote Console)



                            Column(modifier = Modifier.fillMaxSize()) {



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
                onSongCompleted = { viewModel.onSongCompleted() },
                onTogglePause = { viewModel.togglePlaybackPause() },
                onToggleCameraFacing = { viewModel.toggleCameraFacing() },



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



                                        onOpenYouTubeDownloader = { query -> viewModel.openYouTubeDownloader(query) },



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



                                        onStopYouTubeStream = { viewModel.stopYouTubeKaraokeDirect(activity) },



                                        modifier = Modifier.fillMaxSize()



                                    )



                                }







                                if (!state.isPlayingYouTubeStream) {



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



                        }



                        2 -> {



                            // TAB 2: SAVED (Reserved Queue & Favorites)



                            SingPulseSavedScreen(



                                state = state,



                                onPlaySong = { viewModel.playSong(it) },



                                onRemoveReserved = { viewModel.removeFromReserveQueue(it) }



                            )



                        }



                        3 -> {



                            // TAB 3: PROFILE & STUDIO SETTINGS



                            SingPulseProfileScreen(



                                state = state,

                                onPayViaGcash = {

                                    if (activity != null) {

                                        viewModel.initiateGcashVipPayment(activity)

                                    }

                                },

                                onCheckVipStatus = {

                                    viewModel.checkOnlineVipStatus(showMessage = true)

                                },

                                onDeactivateVip = {

                                    viewModel.setVipActivated(false)

                                },



                                onEnterTvMode = { viewModel.enterTvLandscapeMode(activity) },



                                onOpenSoundFontDialog = { viewModel.openSoundFontDialog() },



                                onOpenAudioMixerDialog = { viewModel.openAudioMixerDialog() },



                                onOpenBackgroundDialog = { viewModel.openBackgroundDialog() },



                                onOpenSettingsDialog = { viewModel.openSettingsDialog() },



                                onSelectTheme = { viewModel.setAppTheme(it) },



                                onToggleRecording = { viewModel.toggleRecording() },



                                onSelectLanguage = { viewModel.setAppLanguage(it) },



                                onOpenEditName = { viewModel.openEditNameDialog() },



                                onOpenAdminKeyGenerator = { viewModel.openAdminKeyGenerator() },

                                onOpenVipMp3Player = { viewModel.openVipMp3Player() },

                                onSelectVipButtonTexture = { viewModel.setVipButtonTexture(it) },

                                onToggleWipeHighlight = { viewModel.toggleWipeHighlight() },

                                onResetSettingsToDefault = { viewModel.resetSettingsToDefault() }

                            )



                        }



                    }



                }







                // SingPulse Bottom Navigation Bar (Always available in Portrait mode!)



                // Floating Live Lyrics Mini-Player (Shows when navigating away from player screen while playing)
                if (state.isPlaying && state.selectedBottomNavTab != 1) {
                    FloatingLyricsOverlay(
                        state = state,
                        onExpandToPlayer = { viewModel.selectBottomNavTab(1) },
                        onTogglePause = { viewModel.togglePlaybackPause() },
                        onNext = { viewModel.onNext() }
                    )
                }

                SingPulseBottomNavBar(



                    selectedTab = state.selectedBottomNavTab,



                    isPlaying = state.isPlaying,



                    reserveCount = state.reserveQueue.size,



                    theme = state.appTheme,



                    isTagalog = state.appLanguage == AppLanguage.TAGALOG,



                    onSelectTab = { viewModel.selectBottomNavTab(it) }



                )



            }



        }







        // Studio Instrument & Audio Mixer Modal - Appears when pressing 🎚️ icon



        if (state.isAudioMixerDialogOpen) {



            AudioMixerDialog(



                state = state,



                isLandscape = isLandscape,



                onToggleAutoMixer = { viewModel.toggleAutoMixer(it) },



                onToggleAutoVolume = { viewModel.toggleAutoVolume(it) },



                onToggleAutoMelodyNormalize = { viewModel.toggleAutoMelodyNormalize(it) },



                onAdjustKey = { viewModel.adjustKey(it) },



                onAdjustTempo = { viewModel.adjustTempo(it) },



                onAdjustMelody = { viewModel.adjustMelody(it) },



                onSetMelodyPercent = { viewModel.setMelodyPercent(it) },



                onToggleMelodyMute = { viewModel.toggleMelodyMute() },



                onAdjustDrums = { viewModel.adjustDrums(it) },



                onSetDrumsPercent = { viewModel.setDrumsPercent(it) },



                onAdjustBass = { viewModel.adjustBass(it) },



                onSetBassPercent = { viewModel.setBassPercent(it) },



                onAdjustGuitarPiano = { viewModel.adjustGuitarPiano(it) },



                onSetGuitarPianoPercent = { viewModel.setGuitarPianoPercent(it) },



                onAdjustStringsBrass = { viewModel.adjustStringsBrass(it) },



                onSetStringsBrassPercent = { viewModel.setStringsBrassPercent(it) },



                onAdjustReverb = { viewModel.adjustReverb(it) },



                onSetReverbPercent = { viewModel.setReverbPercent(it) },



                onAdjustBassBoost = { viewModel.adjustBassBoost(it) },



                onSetBassBoostPercent = { viewModel.setBassBoost(it) },



                onAdjustTreble = { viewModel.adjustTreble(it) },



                onSetTreblePercent = { viewModel.setTreble(it) },



                onResetMixer = { viewModel.resetInstrumentMixer() },



                onSaveAsSoundFontPreset = { viewModel.saveMixerAsSoundFontPreset(it) },



                onOpenSoundFontDialog = {



                    viewModel.closeAudioMixerDialog()



                    viewModel.openSoundFontDialog()



                },



                onDismiss = { viewModel.closeAudioMixerDialog() }



            )



        }







        // Voice + Music Recordings Dialog (🎙️ / 🔴)



        if (state.isRecordingsDialogOpen) {



            RecordingsDialog(



                state = state,



                isLandscape = isLandscape,



                onToggleRecording = { viewModel.toggleRecording() },



                onStartScreenRecording = { viewModel.requestStartScreenRecording() },



                onStopScreenRecording = { viewModel.stopScreenRecording() },



                onAdjustVocalSync = { viewModel.adjustVocalSyncOffset(it) },



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



                onSelectScoreMode = { viewModel.setScoreMode(it) },



                onSelectScoringDifficulty = { viewModel.setScoringDifficulty(it) },



                onSelectFanfareSoundLevel = { viewModel.setFanfareSoundLevel(it) },
                onSelectScoringSoundStyle = { viewModel.setScoringSoundStyle(it) },
                onPickCustomScoringAudio = { viewModel.setCustomScoringSoundUri(it) },
                onTestFanfare = { viewModel.testScoringFanfare() },



                onToggleScoringSound = { viewModel.toggleScoringSound(it) },



                onAdjustMicSensitivity = { viewModel.setMicSensitivity(it) },



                onToggleKeypadVoice = { viewModel.toggleKeypadVoice(it) },



                onToggleTvMode = { viewModel.toggleTvMode(activity) },



                onAdjustMelody = { viewModel.adjustMelody(it) },



                onSelectLanguage = { viewModel.setAppLanguage(it) },



                onSelectTheme = { viewModel.setAppTheme(it) },



                onCheckForUpdates = { viewModel.checkForUpdates() },

                onOpenDownloadCenter = { viewModel.openDownloadCenter() },

                onResetSettingsToDefault = { viewModel.resetSettingsToDefault() },



                onDismiss = { viewModel.closeSettingsDialog() }



            )



        }







        // Automatic Over-The-Air Force-Update Modal (Works over Wi-Fi, Mobile Data, any network)



        if (state.updateState !is com.example.videokey.update.AppUpdateManager.UpdateState.Idle) {



            val currentCode = remember {



                try {



                    val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)



                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) pInfo.longVersionCode.toInt() else @Suppress("DEPRECATION") pInfo.versionCode



                } catch (e: Exception) { 1 }



            }



            val targetCode = when (val s = state.updateState) {



                is com.example.videokey.update.AppUpdateManager.UpdateState.ReadyToInstall -> s.info.versionCode



                is com.example.videokey.update.AppUpdateManager.UpdateState.UpdateAvailable -> s.info.versionCode



                is com.example.videokey.update.AppUpdateManager.UpdateState.Downloading -> s.info.versionCode



                else -> 999



            }



            if (targetCode > currentCode || state.updateState is com.example.videokey.update.AppUpdateManager.UpdateState.UpToDate || state.updateState is com.example.videokey.update.AppUpdateManager.UpdateState.Checking || state.updateState is com.example.videokey.update.AppUpdateManager.UpdateState.Error) {



                ForceUpdateDialog(



                    updateState = state.updateState,



                    isLandscape = isLandscape,



                    isTagalog = state.appLanguage == com.example.videokey.ui.main.AppLanguage.TAGALOG,



                    onRetry = { viewModel.retryUpdate() },



                    onInstall = { file -> viewModel.promptInstallDownloadedApk(file) },



                    onDismiss = { viewModel.dismissUpdateDialog() }



                )



            }



        }







        // Download Center Modal (Songs, SoundFonts, and Selective Backgrounds)



        if (state.isDownloadCenterOpen) {



            DownloadCenterDialog(



                state = state,



                isLandscape = isLandscape,



                onDownloadItem = { viewModel.downloadContentItem(it) },



                onCancelDownload = { viewModel.cancelContentDownload() },



                onDismiss = { viewModel.closeDownloadCenter() },



                isItemInstalled = { viewModel.isContentItemInstalled(it) }



            )



        }







        // Lyric Font & Style Customization Modal



        if (state.isLyricStyleDialogOpen) {



            LyricStyleDialog(



                config = state.lyricConfig,



                isLandscape = isLandscape,



                isTagalog = state.appLanguage == com.example.videokey.ui.main.AppLanguage.TAGALOG,



                theme = state.appTheme,



                onSelectFontFamily = { viewModel.setLyricFontFamily(it) },



                onSelectWipeColor = { viewModel.setLyricWipeColor(it) },



                onSelectShadowStyle = { viewModel.setLyricShadowStyle(it) },



                onSelectOutlineColor = { viewModel.setLyricOutlineColor(it) },



                onToggleAllCaps = { viewModel.toggleLyricAllCaps() },



                onToggleItalic = { viewModel.toggleLyricItalic() },



                onAdjustFontScale = { viewModel.setLyricFontScale(it) },



                onAdjustOutlineThickness = { viewModel.setLyricOutlineThickness(it) },



                onAdjustFontSize = { port, land -> viewModel.setLyricFontSize(port, land) },



                onAdjustLineSpacing = { viewModel.setLyricLineSpacing(it) },



                onToggleDropShadow = { viewModel.toggleLyricDropShadow(!state.lyricConfig.hasDropShadow) },



                onSelectLineCount = { viewModel.setLyricLineCount(it) },



                onSelectPosition = { viewModel.setLyricPosition(it) },



                onSelectCountdownMode = { viewModel.setLyricCountdownMode(it) },

                onSetCountdownStepImage = { step, uri -> viewModel.setCountdownStepImage(step, uri) },

                onSetCountdownAllImages = { uri -> viewModel.setCountdownAllImages(uri) },

                onClearAllCountdownImages = { viewModel.clearAllCountdownImages() },



                onAdjustTimingOffset = { viewModel.setLyricTimingOffset(it) },



                onApplyPreset = { viewModel.applyLyricPreset(it) },



                onToggleWipeHighlight = { viewModel.toggleWipeHighlight() },



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



                bgVisualMode = state.bgVisualMode,
                continuousVideoChangePerSong = state.continuousVideoChangePerSong,
                isCameraFrontFacing = state.isCameraFrontFacing,
                onSetCameraFrontFacing = { viewModel.setCameraFrontFacing(it) },



                isTagalog = state.appLanguage == com.example.videokey.ui.main.AppLanguage.TAGALOG,



                theme = state.appTheme,



                onOpenFolderLocator = { viewModel.openBgFolderPicker() },



                onSelectBackground = { viewModel.selectBackground(it) },



                onChangeMode = { viewModel.setBackgroundChangeMode(it) },



                onSelectBgVisualMode = { viewModel.setBackgroundVisualMode(it) },



                onToggleContinuousVideoChangePerSong = { viewModel.toggleContinuousVideoChangePerSong(it) },



                onRescanMedia = { viewModel.scanBackgroundMedia() },



                onDeleteBackground = { viewModel.deleteBackground(it) },



                onDismiss = { viewModel.closeBackgroundDialog() }



            )



        }







        // SoundFont Selection Modal



        if (state.isSoundFontDialogOpen) {



            SoundFontSelectionDialog(

                soundFonts = state.availableSoundFonts,

                activeSoundFont = state.activeSoundFontName,

                autoMatchedSoundFont = state.autoMatchedSoundFontForCurrentSong,

                isVipActivated = state.isVipActivated,

                isTagalog = state.appLanguage == com.example.videokey.ui.main.AppLanguage.TAGALOG,

                theme = state.appTheme,

                onSelectSoundFont = { viewModel.selectSoundFont(it) },

                onScanStorage = { viewModel.scanInternalStorage(context) },

                onDeleteSoundFont = { viewModel.deleteSoundFont(it) },

                onRequireVip = {

                    viewModel.closeSoundFontDialog()

                    viewModel.openYouTubeDownloader(null)

                },

                onDismiss = { viewModel.closeSoundFontDialog() }

            )



        }







        // Song Folder Browser Dialog



        if (state.isSongFolderPickerOpen) {



            val isTl = state.appLanguage == com.example.videokey.ui.main.AppLanguage.TAGALOG



            FolderBrowserDialog(



                title = if (isTl) "📁 PILIIN ANG SONG FOLDER (MGA KANTA)" else "📁 SELECT SONG FOLDER",



                initialPath = state.currentSongFolder,



                isTagalog = isTl,



                onFolderSelected = { folderPath ->



                    viewModel.setSongFolder(folderPath)



                    viewModel.closeSongFolderPicker()



                },



                onDismiss = { viewModel.closeSongFolderPicker() }



            )



        }







        // Background Folder Browser Dialog



        if (state.isBgFolderPickerOpen) {



            val isTl = state.appLanguage == com.example.videokey.ui.main.AppLanguage.TAGALOG



            FolderBrowserDialog(



                title = if (isTl) "🎬 PILIIN ANG BACKGROUND FOLDER" else "🎬 SELECT BACKGROUND FOLDER",



                initialPath = state.currentBackgroundFolder,



                isTagalog = isTl,



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



                theme = state.appTheme,



                onDismiss = { viewModel.closeScoreModal() }



            )



        }







        // VIP MP3 Player Modal

        if (state.isMp3PlayerDialogOpen) {

            VipMp3PlayerDialog(

                state = state,

                isLandscape = isLandscape,

                onPlayMp3 = { viewModel.playMp3Song(it) },

                onTogglePlayPause = { viewModel.toggleMp3PlayPause() },

                onSeek = { viewModel.seekMp3(it) },

                onNext = { viewModel.playNextMp3Song() },

                onPrev = { viewModel.playPrevMp3Song() },

                onRescan = { viewModel.scanMp3Songs() },

                onDismiss = { viewModel.closeVipMp3Player() }

            )

        }



        // YouTube Downloader Modal



        if (state.isYouTubeDownloaderOpen) {



            YouTubeDownloaderDialog(



                state = state,

                isLandscape = isLandscape,



                onSearch = { viewModel.searchYouTube(it) },



                onPlayDirect = { viewModel.playYouTubeKaraokeDirect(it) },



                onDownloadSong = { viewModel.downloadYouTubeSong(it) },



                onDownloadBackground = { viewModel.downloadYouTubeBackground(it) },



                onPlayDownloadedSong = { song ->



                    viewModel.closeYouTubeDownloader()



                    viewModel.playSong(song)



                },



                onDeleteDownloadedSong = { song ->



                    viewModel.deleteDownloadedMp4Song(song)



                },



                onActivateVip = { key ->



                    viewModel.activateVip(key)



                },



                onClearVipError = {



                    viewModel.clearVipActivationError()



                },



                onOpenAdminKeyGenerator = {



                    viewModel.openAdminKeyGenerator()



                },



                onPayViaGcash = {

                    if (activity != null) {

                        viewModel.initiateGcashVipPayment(activity)

                    }

                },



                onCheckVipStatus = {

                    viewModel.checkOnlineVipStatus(showMessage = true)

                },



                onDismiss = { viewModel.closeYouTubeDownloader() }



            )



        }







        // Edit Profile Username Dialog



        if (state.isEditNameDialogOpen) {



            EditProfileNameDialog(



                currentName = state.userName,



                theme = state.appTheme,



                isTagalog = state.appLanguage == AppLanguage.TAGALOG,



                onSaveName = { newName ->



                    viewModel.setUserName(newName)



                },



                onDismiss = { viewModel.closeEditNameDialog() }



            )



        }







        // Admin VIP Key Generator Dialog (Triggered by 10 taps on JUAN or VIP Crown + PIN SXg46k88videokey)



        if (state.isAdminKeyGeneratorOpen) {



            AdminKeyGeneratorDialog(



                theme = state.appTheme,



                isTagalog = state.appLanguage == AppLanguage.TAGALOG,



                onDismiss = { viewModel.closeAdminKeyGenerator() }



            )



        }







        // Minimized Floating Background Download Pill (Dynamic Island style)



        val downloadStatus = state.downloadStatus



        val isDownloadingOrExtracting = downloadStatus is com.example.videokey.data.ContentDownloadManager.DownloadStatus.Downloading ||



                downloadStatus is com.example.videokey.data.ContentDownloadManager.DownloadStatus.Extracting



        val showMinimizedDownload = isDownloadingOrExtracting && !state.isDownloadCenterOpen







        androidx.compose.animation.AnimatedVisibility(



            visible = showMinimizedDownload,



            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically { -it },



            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically { -it },



            modifier = Modifier



                .align(Alignment.TopCenter)



                .padding(top = if (isLandscape) 8.dp else 12.dp)



                .zIndex(99f)



        ) {



            MinimizedDownloadFloatingPill(



                downloadStatus = downloadStatus,



                downloadCatalog = state.downloadCatalog,



                isTagalog = isTagalog,



                onMaximize = { viewModel.openDownloadCenter() },



                onCancel = { viewModel.cancelContentDownload() }



            )



        }







        // Exit Confirmation Dialog



        if (showExitConfirmDialog) {



            ExitConfirmDialog(



                isTagalog = isTagalog,



                primaryColor = state.appTheme.primaryColor,



                onConfirmExit = {



                    showExitConfirmDialog = false



                    activity?.finishAffinity()



                },



                onDismiss = {



                    showExitConfirmDialog = false



                }



            )



        }



    }



}







/**



 * Ad-Free, Hardware-Accelerated Responsive YouTube Karaoke Stream Player.



 * Automatically fits the player screen cleanly, continuously auto-skips any ads,



 * and allows instant singing without waiting for the file to download!



 */



@Composable



fun AdFreeYouTubeStreamPlayer(



    videoId: String,



    title: String,



    theme: AppTheme,



    onStop: () -> Unit,



    onScore: () -> Unit,



    onToggleLandscape: (() -> Unit)? = null,



    isLandscape: Boolean = false,



    modifier: Modifier = Modifier



) {



    val embedHtml = remember(videoId) {



        """



        <!DOCTYPE html>



        <html>



        <head>



            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">



            <style>



                * {



                    box-sizing: border-box;



                    margin: 0;



                    padding: 0;



                }



                html, body {



                    width: 100%;



                    height: 100%;



                    background-color: #000000;



                    overflow: hidden;



                    display: flex;



                    align-items: center;



                    justify-content: center;



                }



                .player-box {



                    position: relative;



                    width: 100%;



                    height: 100%;



                    display: flex;



                    align-items: center;



                    justify-content: center;



                    background-color: #000000;



                }



                iframe {



                    width: 100%;



                    height: 100%;



                    border: 0;



                }



            </style>



        </head>



        <body>



            <div class="player-box">



                <iframe 



                    id="yt-player"



                    src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&mute=0&controls=1&enablejsapi=1&playsinline=1&rel=0&iv_load_policy=3&modestbranding=1"



                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"



                    allowfullscreen>



                </iframe>



            </div>



            <script>



                function triggerPlay() {



                    try {



                        var f = document.getElementById('yt-player');



                        if (f && f.contentWindow) {



                            f.contentWindow.postMessage('{"event":"command","func":"playVideo","args":""}', '*');



                        }



                    } catch(e) {}



                }



                setTimeout(triggerPlay, 600);



                setTimeout(triggerPlay, 1500);



                setTimeout(triggerPlay, 3000);







                // Auto Ad Skip



                setInterval(function() {



                    try {



                        var skipBtn = document.querySelector('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button, button.ytp-ad-overlay-close-button');



                        if (skipBtn) skipBtn.click();



                        var adOverlays = document.querySelectorAll('.video-ads, .ytp-ad-module, .ad-showing, .ad-interrupting');



                        adOverlays.forEach(function(el) { el.style.display = 'none'; });



                    } catch(e) {}



                }, 500);



            </script>



        </body>



        </html>



        """.trimIndent()



    }







    Box(



        modifier = modifier



            .fillMaxSize()



            .background(Color.Black),



        contentAlignment = Alignment.Center



    ) {



        key(videoId) {



            AndroidView(



                factory = { ctx ->



                    WebView(ctx).apply {



                        layoutParams = android.view.ViewGroup.LayoutParams(



                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,



                            android.view.ViewGroup.LayoutParams.MATCH_PARENT



                        )



                        setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)



                        val cm = android.webkit.CookieManager.getInstance()



                        cm.setAcceptCookie(true)



                        cm.setAcceptThirdPartyCookies(this, true)



                        cm.setCookie("https://youtube.com", "CONSENT=YES+cb; Path=/; Domain=.youtube.com")



                        cm.setCookie("https://youtube.com", "SOCS=CAESEwgDEgk0ODE3Nzk3MjQaAmVuIAEaBgiA_LyaBg; Path=/; Domain=.youtube.com")



                        cm.setCookie("https://m.youtube.com", "CONSENT=YES+cb; Path=/; Domain=.youtube.com")



                        cm.setCookie("https://m.youtube.com", "SOCS=CAESEwgDEgk0ODE3Nzk3MjQaAmVuIAEaBgiA_LyaBg; Path=/; Domain=.youtube.com")



                        cm.flush()



                        settings.apply {



                            javaScriptEnabled = true



                            domStorageEnabled = true



                            databaseEnabled = true



                            mediaPlaybackRequiresUserGesture = false



                            useWideViewPort = true



                            loadWithOverviewMode = true



                            allowFileAccess = true



                            allowContentAccess = true



                            javaScriptCanOpenWindowsAutomatically = true



                            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW



                            userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"



                        }



                        webChromeClient = object : WebChromeClient() {



                            override fun onConsoleMessage(cm: android.webkit.ConsoleMessage?): Boolean {



                                Log.i("YouTubePlayer", "Console: ${cm?.message()}")



                                return true



                            }



                        }



                        val ytHeaders = mapOf(



                            "Referer" to "https://www.youtube.com/",



                            "Origin" to "https://www.youtube.com"



                        )



                        webViewClient = object : WebViewClient() {



                            override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {



                                val reqUrl = request?.url?.toString() ?: return false



                                view?.loadUrl(reqUrl, ytHeaders)



                                return true



                            }



                            override fun onPageFinished(view: WebView?, url: String?) {



                                super.onPageFinished(view, url)



                                Log.i("YouTubePlayer", "Page finished: $url")



                                view?.evaluateJavascript(



                                    """



                                    (function() {



                                        var isLandscapeMode = $isLandscape;



                                        function cleanAndCenterPlayer() {



                                            // Auto-dismiss Google/YouTube consent dialog



                                            var consentButtons = document.querySelectorAll('button, form button, input[type="submit"], [role="button"]');



                                            consentButtons.forEach(function(b) {



                                                var txt = (b.innerText || b.value || '').toLowerCase();



                                                if (txt.includes('accept all') || txt.includes('agree') || txt.includes('i agree') || txt.includes('tanggapin') || txt.includes('accept')) {



                                                    b.click();



                                                }



                                            });







                                            var pageText = document.body ? document.body.innerText : '';



                                            if (pageText.indexOf('Error 153') !== -1 || pageText.indexOf('Error 150') !== -1 || document.querySelector('.ytp-error')) {



                                                window.location.replace('https://m.youtube.com/watch?v=' + '$videoId');



                                                return;



                                            }







                                            if (window.location.hostname.indexOf('m.youtube.com') !== -1) {



                                                var styleId = 'videokey-clean-yt';



                                                var existingStyle = document.getElementById(styleId);



                                                if (!existingStyle) {



                                                    var style = document.createElement('style');



                                                    style.id = styleId;



                                                    if (isLandscapeMode) {



                                                        style.innerHTML = `



                                                            #header-bar, ytm-mobile-topbar-renderer, ytm-pivot-bar-renderer, 



                                                            .related-items-container, ytm-item-section-renderer, 



                                                            ytm-single-column-watch-next-results-renderer, #player-control-overlay, 



                                                            .ytm-comments-entry-point-header, #related, .ytm-app-promo,



                                                            .consent-bump-v2, ytm-consent-bump-v2-renderer,



                                                            .watch-below-the-player, ytm-watch-metadata, .slim-video-metadata-header {



                                                                display: none !important;



                                                            }



                                                            body, html {



                                                                background: #000000 !important;



                                                                overflow: hidden !important;



                                                                margin: 0 !important;



                                                                padding: 0 !important;



                                                            }



                                                            .player-container, #player-container-id, .video-stream {



                                                                width: 100vw !important;



                                                                height: 100vh !important;



                                                                position: fixed !important;



                                                                top: 0 !important;



                                                                left: 0 !important;



                                                                z-index: 999999 !important;



                                                            }



                                                        `;



                                                    } else {



                                                        style.innerHTML = `



                                                            #header-bar, ytm-mobile-topbar-renderer, ytm-pivot-bar-renderer, 



                                                            .related-items-container, ytm-item-section-renderer, 



                                                            ytm-single-column-watch-next-results-renderer, #player-control-overlay, 



                                                            .ytm-comments-entry-point-header, #related, .ytm-app-promo,



                                                            .consent-bump-v2, ytm-consent-bump-v2-renderer,



                                                            .watch-below-the-player, ytm-watch-metadata, .slim-video-metadata-header {



                                                                display: none !important;



                                                            }



                                                            body, html {



                                                                background: #000000 !important;



                                                                overflow: hidden !important;



                                                                margin: 0 !important;



                                                                padding: 0 !important;



                                                            }



                                                            .player-container, #player-container-id {



                                                                position: fixed !important;



                                                                top: 50% !important;



                                                                left: 50% !important;



                                                                transform: translate(-50%, -50%) !important;



                                                                width: 100vw !important;



                                                                height: 56.25vw !important;



                                                                max-height: 100vh !important;



                                                                z-index: 999999 !important;



                                                            }



                                                            video {



                                                                width: 100% !important;



                                                                height: 100% !important;



                                                                object-fit: contain !important;



                                                            }



                                                        `;



                                                    }



                                                    document.head.appendChild(style);



                                                }



                                            }







                                            var v = document.querySelector('video');



                                            if (v) {



                                                v.muted = false;



                                                if (v.paused) v.play();



                                            }



                                            var btn = document.querySelector('.ytp-large-play-button, .video-stream');



                                            if (btn) btn.click();



                                            var unmute = document.querySelector('.ytp-unmute, .ytp-volume-panel, button[aria-label*="unmute" i], .ytp-mute-button');



                                            if (unmute) unmute.click();







                                            var skipBtn = document.querySelector('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button, button.ytp-ad-overlay-close-button');



                                            if (skipBtn) skipBtn.click();



                                            var adOverlays = document.querySelectorAll('.video-ads, .ytp-ad-module, .ad-showing, .ad-interrupting');



                                            adOverlays.forEach(function(el) { el.style.display = 'none'; });



                                        }







                                        cleanAndCenterPlayer();



                                        setInterval(cleanAndCenterPlayer, 400);



                                    })();



                                    """.trimIndent(),



                                    null



                                )



                            }



                            override fun onReceivedError(view: WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {



                                super.onReceivedError(view, request, error)



                                Log.e("YouTubePlayer", "Error: ${error?.description}")



                            }



                        }



                        loadUrl("https://www.youtube.com/embed/$videoId?autoplay=1&mute=0&controls=1&enablejsapi=1&playsinline=1&rel=0&iv_load_policy=3&modestbranding=1", ytHeaders)



                    }



                },



                update = { view ->



                    val ytHeaders = mapOf(



                        "Referer" to "https://www.youtube.com/",



                        "Origin" to "https://www.youtube.com"



                    )



                    view.loadUrl("https://www.youtube.com/embed/$videoId?autoplay=1&mute=0&controls=1&enablejsapi=1&playsinline=1&rel=0&iv_load_policy=3&modestbranding=1", ytHeaders)



                },



                modifier = Modifier.fillMaxSize()



            )



        }







        // Top Control Ribbon (Title + Live Badge + Score + Stop)



        Row(



            modifier = Modifier



                .align(Alignment.TopCenter)



                .fillMaxWidth()



                .background(



                    Brush.verticalGradient(



                        listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)



                    )



                )



                .padding(horizontal = 12.dp, vertical = 8.dp),



            verticalAlignment = Alignment.CenterVertically,



            horizontalArrangement = Arrangement.SpaceBetween



        ) {



            Row(



                verticalAlignment = Alignment.CenterVertically,



                horizontalArrangement = Arrangement.spacedBy(6.dp),



                modifier = Modifier.weight(1f)



            ) {



                Surface(



                    shape = RoundedCornerShape(4.dp),



                    color = Color(0xFFE50914)



                ) {



                    Text(



                        text = "LIVE YOUTUBE (NO ADS)",



                        fontSize = 9.sp,



                        fontWeight = FontWeight.Black,



                        color = Color.White,



                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)



                    )



                }



                Text(



                    text = title,



                    color = Color.White,



                    fontSize = 11.sp,



                    fontWeight = FontWeight.Bold,



                    maxLines = 1,



                    overflow = TextOverflow.Ellipsis



                )



            }







            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {



                if (onToggleLandscape != null) {



                    Surface(



                        shape = RoundedCornerShape(6.dp),



                        color = Color(0xFF374151),



                        modifier = Modifier.clickable { onToggleLandscape() }



                    ) {



                        Text(



                            text = if (isLandscape) "📱 PORTRAIT" else "📺 TV / FULL",



                            fontSize = 10.sp,



                            fontWeight = FontWeight.Bold,



                            color = Color.White,



                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)



                        )



                    }



                }







                Surface(



                    shape = RoundedCornerShape(6.dp),



                    color = Color(0xFFFFD700),



                    modifier = Modifier.clickable { onScore() }



                ) {



                    Text(



                        text = "🏆 SCORE",



                        fontSize = 10.sp,



                        fontWeight = FontWeight.Black,



                        color = Color.Black,



                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)



                    )



                }







                Surface(



                    shape = RoundedCornerShape(6.dp),



                    color = Color(0xFFE53935),



                    modifier = Modifier.clickable { onStop() }



                ) {



                    Text(



                        text = "✕ ITIGIL",



                        fontSize = 10.sp,



                        fontWeight = FontWeight.Black,



                        color = Color.White,



                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)



                    )



                }



            }



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
    if (number == 5) {
        ArcadeGoBadge(isLandscape = isLandscape, modifier = modifier)
        return
    }







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



@Composable
fun ArcadeGoBadge(
    isLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "goPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(220, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "goPulseScale"
    )

    val badgeWidth = if (isLandscape) 44.dp else 34.dp
    val badgeHeight = if (isLandscape) 26.dp else 22.dp

    Box(
        modifier = modifier
            .scale(pulseScale)
            .size(width = badgeWidth, height = badgeHeight)
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFFFF1744), Color(0xFFFF9100))
                )
            )
            .border(1.5.dp, Color(0xFFFFEB3B), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "GO!",
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            fontSize = if (isLandscape) 14.sp else 11.sp,
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

@Composable
fun CustomCountdownStepBadge(
    step: Int,
    lyricConfig: LyricCustomization,
    isLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    if (step <= 0) return
    val context = androidx.compose.ui.platform.LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "stepCountdownPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(260, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "stepPulseScale"
    )

    val imgUri = when (step) {
        4 -> lyricConfig.countdownImg4Uri ?: lyricConfig.customCountdownPhotoUri
        3 -> lyricConfig.countdownImg3Uri ?: lyricConfig.customCountdownPhotoUri
        2 -> lyricConfig.countdownImg2Uri ?: lyricConfig.customCountdownPhotoUri
        1 -> lyricConfig.countdownImg1Uri ?: lyricConfig.customCountdownPhotoUri
        5 -> lyricConfig.countdownImgGoUri ?: lyricConfig.customCountdownPhotoUri
        else -> null
    }

    val bitmap = remember(imgUri) {
        if (!imgUri.isNullOrEmpty()) {
            try {
                val uri = android.net.Uri.parse(imgUri)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    android.graphics.BitmapFactory.decodeStream(stream)
                }
            } catch (_: Exception) {
                null
            }
        } else null
    }

    val badgeHeight = if (isLandscape) 52.dp else 44.dp
    val badgeWidth = if (step == 5) (if (isLandscape) 64.dp else 54.dp) else badgeHeight
    val shape = if (step == 5) RoundedCornerShape(10.dp) else CircleShape

    Box(
        modifier = modifier
            .scale(pulseScale)
            .size(width = badgeWidth, height = badgeHeight)
            .clip(shape)
            .background(Color(0xFF0F172A))
            .border(
                width = 2.dp,
                brush = Brush.sweepGradient(
                    if (step == 5) {
                        listOf(Color(0xFFFF1744), Color(0xFFFF9100), Color(0xFFFFD700), Color(0xFFFF1744))
                    } else {
                        listOf(Color(0xFFFFD700), Color(0xFFFF4081), Color(0xFF00E5FF), Color(0xFFFFD700))
                    }
                ),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Countdown Step $step",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.75f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(if (isLandscape) 20.dp else 16.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (step == 5) "GO" else "$step",
                        color = if (step == 5) Color(0xFFFF5252) else Color(0xFFFDE047),
                        fontWeight = FontWeight.Black,
                        fontSize = if (isLandscape) 11.sp else 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        } else {
            if (step == 5) {
                ArcadeGoBadge(isLandscape = isLandscape)
            } else {
                SingleCountdownBadge(number = step, isLandscape = isLandscape)
            }
        }
    }
}

@Composable
fun PhotoCountdownBadge(
    number: Int,
    photoUri: String?,
    isLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    CustomCountdownStepBadge(
        step = number,
        lyricConfig = LyricCustomization(customCountdownPhotoUri = photoUri),
        isLandscape = isLandscape,
        modifier = modifier
    )
}

/**



 * Classic Philippine Videoke 4-Dots Countdown.



 * Starts with 4 dots and disappears one-by-one from right to left (●●●● -> ●●●○ -> ●●○○ -> ●○○○ -> 0).



 */



@Composable



fun VideokeDotsCountdownBadge(



    number: Int,



    isLandscape: Boolean,



    modifier: Modifier = Modifier



) {



    if (number !in 1..4) return



    val dotSize = if (isLandscape) 13.dp else 10.5.dp



    val activeCount = number // 4 dots active at beat 4 -> rightmost disappears at beat 3 -> dot 3 at 2 -> dot 2 at 1 -> lyrics wipe



    Row(



        modifier = modifier



            .background(Color(0xCC000000), RoundedCornerShape(10.dp))



            .border(1.dp, Color(0xFFFFD54F).copy(alpha = 0.7f), RoundedCornerShape(10.dp))



            .padding(horizontal = 6.dp, vertical = 3.dp),



        horizontalArrangement = Arrangement.spacedBy(4.dp),



        verticalAlignment = Alignment.CenterVertically



    ) {



        for (i in 1..4) {



            val isActive = i <= activeCount



            val dotColor = when (i) {



                1 -> Color(0xFFFF1744) // Beat 1 (Final warning right above the first syllable) - Vivid Red



                2 -> Color(0xFFFF9100) // Beat 2 - Bright Orange



                3 -> Color(0xFFFFD600) // Beat 3 - Golden Yellow



                else -> Color(0xFF00E676) // Beat 4 (Rightmost dot that disappears first) - Neon Green



            }



            Box(



                modifier = Modifier



                    .size(dotSize)



                    .clip(CircleShape)



                    .background(if (isActive) dotColor else Color.Transparent)



                    .border(



                        width = if (isActive) 1.dp else 0.dp,



                        color = if (isActive) Color.White else Color.Transparent,



                        shape = CircleShape



                    )



            )



        }



    }



}







/**



 * Authentic Videoke "INSTRUMENTAL" Center Screen Display



 * Shown in the center of the TV when musical intro or interlude is playing.



 * Slightly smaller and dead-centered in Landscape mode as requested!



 */



@Composable



fun InstrumentalDisplay(



    theme: AppTheme = AppTheme.CYBER_NEON,



    isLandscape: Boolean,



    modifier: Modifier = Modifier



) {



    val infiniteTransition = rememberInfiniteTransition(label = "instPulse")



    val pulseAlpha by infiniteTransition.animateFloat(



        initialValue = 0.65f,



        targetValue = 0.90f,



        animationSpec = infiniteRepeatable(



            animation = tween(700, easing = FastOutSlowInEasing),



            repeatMode = RepeatMode.Reverse



        ),



        label = "instAlpha"



    )



    val pulseScale by infiniteTransition.animateFloat(



        initialValue = 0.98f,



        targetValue = 1.02f,



        animationSpec = infiniteRepeatable(



            animation = tween(700, easing = FastOutSlowInEasing),



            repeatMode = RepeatMode.Reverse



        ),



        label = "instScale"



    )







    // Subtle transparent grey styling with slightly smaller compact size



    val transparentGrey = Color(0xFFE2E8F0).copy(alpha = 0.60f * pulseAlpha)



    val fontSize = if (isLandscape) 15.sp else 12.sp



    val iconSize = if (isLandscape) 15.sp else 12.sp



    val letterSpacing = if (isLandscape) 2.sp else 1.5.sp







    Row(



        modifier = modifier.scale(pulseScale),



        verticalAlignment = Alignment.CenterVertically,



        horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 8.dp else 6.dp)



    ) {



        Text(



            text = "🎷",



            fontSize = iconSize,



            modifier = Modifier.alpha(0.65f * pulseAlpha)



        )



        Box(contentAlignment = Alignment.Center) {



            // Subtle soft stroke so transparent grey text is softly readable over video without harshness



            Text(



                text = "★  I N S T R U M E N T A L  ★",



                fontWeight = FontWeight.Bold,



                fontFamily = CleanFont,



                letterSpacing = letterSpacing,



                fontSize = fontSize,



                style = TextStyle(



                    color = Color.Black.copy(alpha = 0.45f * pulseAlpha),



                    drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(



                        width = 3.5f,



                        join = androidx.compose.ui.graphics.StrokeJoin.Round



                    )



                )



            )



            // Transparent grey bold text



            Text(



                text = "★  I N S T R U M E N T A L  ★",



                fontWeight = FontWeight.Bold,



                fontFamily = CleanFont,



                letterSpacing = letterSpacing,



                fontSize = fontSize,



                color = transparentGrey



            )



        }



        Text(



            text = "🎸",



            fontSize = iconSize,



            modifier = Modifier.alpha(0.65f * pulseAlpha)



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



    isTvMode: Boolean = false,



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



    val baseTitleSize = if (isTvMode) 68.sp else if (isLandscape) 44.sp else 34.sp



    val titleFontSize = baseTitleSize * titleFitScale







    // Dynamic auto-adjust font size for artist:



    var artistFitScale by remember(song.artist) { mutableStateOf(1f) }



    val baseArtistSize = if (isTvMode) 32.sp else if (isLandscape) 22.sp else 18.sp



    val artistFontSize = baseArtistSize * artistFitScale







    val isTagalog = lang == com.example.videokey.ui.main.AppLanguage.TAGALOG







    // Curated high-visibility vibrant colors for song titles (Clean, readable, dynamic per song):



    val titleColors = remember {



        listOf(



            Color(0xFFFFEE58), // Vivid Golden Yellow



            Color(0xFF00E5FF), // Electric Cyan



            Color(0xFFFF4081), // Neon Pink



            Color(0xFF00FF88), // Bright Spring Green



            Color(0xFFFF9100), // Vibrant Amber



            Color(0xFFE040FB), // Neon Purple/Violet



            Color(0xFF69F0AE), // Bright Mint



            Color(0xFFFF5252), // Bright Coral Red



            Color(0xFF40C4FF), // Vivid Sky Blue



            Color(0xFFFFD54F)  // Warm Gold



        )



    }



    val songTitleColor = remember(song.code, song.title) {



        val hash = kotlin.math.abs((song.code.hashCode() * 31) xor song.title.hashCode())



        titleColors[hash % titleColors.size]



    }







    Column(



        modifier = modifier



            .fillMaxWidth()



            .padding(horizontal = if (isLandscape) 32.dp else 12.dp),



        horizontalAlignment = Alignment.CenterHorizontally,



        verticalArrangement = Arrangement.Center



    ) {



        Box(
            modifier = Modifier
                .wrapContentSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
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







                val sharedTitleStyle = TextStyle(



                    fontSize = titleFontSize,



                    fontWeight = FontWeight.Black,



                    fontFamily = FontFamily.SansSerif,



                    textAlign = TextAlign.Center,



                    lineHeight = titleFontSize * 1.15f,



                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)



                )



                val outlineTitleStyle = sharedTitleStyle.copy(



                    color = Color.Black,



                    drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(



                        width = if (isLandscape) 4.5f else 3.5f,



                        join = androidx.compose.ui.graphics.StrokeJoin.Round



                    )



                )



                val fillTitleStyle = sharedTitleStyle.copy(



                    color = songTitleColor,



                    drawStyle = androidx.compose.ui.graphics.drawscope.Fill



                )







                // Big Centered Auto-Adjusting Song Title with Thin Black Outline & Clean Font Style



                Box(contentAlignment = Alignment.Center) {



                    Text(



                        text = song.title.uppercase(),



                        maxLines = 2,



                        softWrap = true,



                        style = outlineTitleStyle



                    )



                    Text(



                        text = song.title.uppercase(),



                        maxLines = 2,



                        softWrap = true,



                        style = fillTitleStyle,



                        onTextLayout = { result ->



                            if ((result.didOverflowWidth || result.didOverflowHeight) && titleFitScale > 0.4f) {



                                titleFitScale *= 0.90f



                            }



                        }



                    )



                }







                val sharedArtistStyle = TextStyle(



                    fontSize = artistFontSize,



                    fontWeight = FontWeight.Bold,



                    fontFamily = FontFamily.SansSerif,



                    textAlign = TextAlign.Center,



                    lineHeight = artistFontSize * 1.2f,



                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)



                )



                val outlineArtistStyle = sharedArtistStyle.copy(



                    color = Color.Black,



                    drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(



                        width = if (isLandscape) 3.5f else 2.5f,



                        join = androidx.compose.ui.graphics.StrokeJoin.Round



                    )



                )



                val fillArtistStyle = sharedArtistStyle.copy(



                    color = Color(0xFFFFD54F),



                    drawStyle = androidx.compose.ui.graphics.drawscope.Fill



                )







                // Big Centered Auto-Adjusting Artist with Thin Black Outline



                Box(contentAlignment = Alignment.Center) {



                    Text(



                        text = song.artist.uppercase(),



                        maxLines = 1,



                        softWrap = false,



                        style = outlineArtistStyle



                    )



                    Text(



                        text = song.artist.uppercase(),



                        maxLines = 1,



                        softWrap = false,



                        style = fillArtistStyle,



                        onTextLayout = { result ->



                            if (result.didOverflowWidth && artistFitScale > 0.5f) {



                                artistFitScale *= 0.92f



                            }



                        }



                    )



                }







                Text(



                    text = if (isTagalog) "HUMANDA SA PAGKANTA" else "GET READY TO SING",



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



    fontWeight: FontWeight = FontWeight.Bold,



    outlineColor: Color = Color.Black.copy(alpha = 0.90f),



    outlineWidth: Float = if (isLandscape) 2.2f else 1.8f,



    maxLines: Int = 1,



    modifier: Modifier = Modifier



) {



    Box(modifier = modifier) {



        // Outline layer (behind) - thin/medium crisp stroke, NO shadow



        Text(



            text = text,



            color = outlineColor,



            fontWeight = fontWeight,



            fontFamily = fontFamily,



            fontSize = fontSize,



            maxLines = maxLines,



            softWrap = false,



            overflow = TextOverflow.Ellipsis,



            style = TextStyle(



                drawStyle = Stroke(



                    width = outlineWidth,



                    join = StrokeJoin.Round



                ),



                shadow = null



            )



        )



        // Foreground text (front) - bold, themed color, NO shadow



        Text(



            text = text,



            color = color,



            fontWeight = fontWeight,



            fontFamily = fontFamily,



            fontSize = fontSize,



            maxLines = maxLines,



            softWrap = false,



            overflow = TextOverflow.Ellipsis,



            style = TextStyle(



                shadow = null



            )



        )



    }



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



    onSongCompleted: () -> Unit = {},



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



    onOpenYouTubeDownloader: (String?) -> Unit = {},



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



    onStopYouTubeStream: () -> Unit = {},
    onTogglePause: () -> Unit = {},
    onToggleCameraFacing: () -> Unit = {},



    modifier: Modifier = Modifier



) {



    val isTagalog = state.appLanguage == com.example.videokey.ui.main.AppLanguage.TAGALOG



    val isTvMode = state.isTvMode



    val effectiveLyricConfig = state.lyricConfig



    val shouldShowHeader = remember(state.isTvMode, state.isPlaying, state.reserveQueue.size) {



        if (state.isTvMode) (state.isPlaying || state.reserveQueue.isNotEmpty()) else true



    }



    var showTvKeypadModal by remember { mutableStateOf(false) }
    var showTvQrBarcodeModal by remember { mutableStateOf(false) }







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
                detectTapGestures {
                    if (isLandscape) {
                        showLandscapeControls = true
                        interactionTick++
                    }
                }
            }



    ) {



        // LAYER 1: Background Video / Image / Stage (Isolated to prevent recomposition during lyric wipe)



        val mediaCount = remember(state.availableBackgrounds) {



            state.availableBackgrounds.count { it.type == BackgroundType.VIDEO || it.type == BackgroundType.IMAGE }



        }



        val currentBg = state.selectedBackground



        val videoSongPath = if (state.isVideoSong) state.currentSong.filePath else null



        // NOTE: Do NOT key on state.isPlaying here. Re-keying tears down the TextureView AndroidView

        // in the same frame playback starts, which crashes Compose (LayoutNode.onChildRemoved NPE) in TV mode.

        key(currentBg?.id, currentBg?.path, videoSongPath) {



            VideokeBackgroundLayer(



                background = currentBg,



                hasMultipleMedia = mediaCount > 1,



                videoSongPath = videoSongPath,



                isVideoSong = state.isVideoSong,



                isPlaying = state.isPlaying,



                isPlaybackPaused = state.isPlaybackPaused,



                bgVisualMode = state.bgVisualMode,
                isCameraFrontFacing = state.isCameraFrontFacing,
                onToggleCameraFacing = onToggleCameraFacing,
                onBackgroundCompleted = {



                    if (state.isVideoSong) {



                        onSongCompleted()



                    } else {



                        onAdvanceBackground()



                    }



                },



                modifier = Modifier.fillMaxSize()



            )



        }







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



                        .padding(start = 12.dp, end = 12.dp, top = if (isLandscape) 2.dp else 4.dp, bottom = 0.dp),



                    horizontalArrangement = Arrangement.SpaceBetween,



                    verticalAlignment = Alignment.CenterVertically



                ) {



                    if (shouldShowHeader) {



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



                                state.isPlaying && state.isPlaybackPaused -> if (isTagalog) "⏸️ NAKA-PAUSE:" else "⏸️ PAUSED:"



                                !state.isPlaying && hasReserve -> if (isTagalog) "SUSUNOD:" else "NEXT:"



                                !state.isPlaying -> "VIDEOKEY:"



                                isShowingNext -> if (isTagalog) "SUSUNOD:" else "NEXT:"



                                else -> if (isTagalog) "TUMUTUGTOG:" else "PLAYING:"



                            }







                            val themePrimary = Color(state.appTheme.primaryColor)



                            val themeSecondary = Color(state.appTheme.secondaryColor)



                            val themeTextColor = Color(state.appTheme.textColor)







                            val badgeColor = when {



                                badgeLabel.contains("NAKA-PAUSE") || badgeLabel.contains("PAUSED") -> Color(0xFFF59E0B)



                                badgeLabel.contains("TUMUTUGTOG") || badgeLabel.contains("PLAYING") -> themePrimary



                                badgeLabel.contains("SUSUNOD") || badgeLabel.contains("NEXT") -> themeSecondary



                                else -> themePrimary



                            }







                            // Pure Text Badge: bold, themed, outlined without shadow



                            OutlinedHeaderText(



                                text = "$badgeLabel ",



                                color = badgeColor,



                                fontSize = if (isTvMode) 16.sp else if (isLandscape) 13.5.sp else 12.sp,



                                isLandscape = isLandscape,



                                fontFamily = FontFamily.Monospace,



                                fontWeight = FontWeight.Black



                            )







                            val headerMarquee = when {



                                !state.isPlaying && hasReserve -> if (isTagalog) "DEVELOPED BY JEROME HUET ITO PO AY FREE : $nextSongText" else "DEVELOPED BY JEROME HUET THIS IS FREE : $nextSongText"



                                !state.isPlaying -> if (isTagalog) "DEVELOPED BY JEROME HUET ITO PO AY FREE : PUMILI NG KANTA" else "DEVELOPED BY JEROME HUET THIS IS FREE : SELECT A SONG"



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



                                color = if (isTvMode) themeTextColor else Color.White,



                                fontSize = if (isTvMode) 16.sp else if (isLandscape) 13.5.sp else 12.sp,



                                isLandscape = isLandscape,



                                fontWeight = FontWeight.Bold,



                                modifier = Modifier



                                    .weight(1f)



                                    .basicMarquee(iterations = Int.MAX_VALUE)



                            )



                        }



                    } else {



                        Spacer(modifier = Modifier.weight(1f))



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



                        val uniformBorder = Color(state.appTheme.primaryColor)



                        AnimatedVisibility(visible = showLandscapeControls) {



                            Row(



                                horizontalArrangement = Arrangement.spacedBy(5.dp),



                                verticalAlignment = Alignment.CenterVertically,



                                modifier = Modifier.padding(start = 8.dp, end = 4.dp)



                            ) {



                                TvHeaderIconButton(



                                    icon = "📖",



                                    borderColor = uniformBorder,



                                    isActive = state.isSongbookOpen,



                                    onClick = {



                                        interactionTick++



                                        onOpenSongbook()



                                    }



                                )



                                TvHeaderIconButton(



                                    icon = if (state.isRecording) "🔴" else "🎙️",



                                    borderColor = uniformBorder,



                                    isActive = state.isRecording || state.isRecordingsDialogOpen,



                                    onClick = {



                                        interactionTick++



                                        onOpenRecordingsDialog()



                                    }



                                )



                                TvHeaderIconButton(



                                    icon = "🎛️",



                                    borderColor = uniformBorder,



                                    isActive = state.isSoundFontDialogOpen,



                                    onClick = {



                                        interactionTick++



                                        onOpenSoundFontDialog()



                                    }



                                )



                                TvHeaderIconButton(



                                    icon = "🔤",



                                    borderColor = uniformBorder,



                                    isActive = state.isLyricStyleDialogOpen,



                                    onClick = {



                                        interactionTick++



                                        onOpenLyricStyleDialog()



                                    }



                                )



                                TvHeaderIconButton(



                                    icon = "🎚️",



                                    borderColor = uniformBorder,



                                    isActive = state.isAudioMixerDialogOpen,



                                    onClick = {



                                        interactionTick++



                                        onOpenAudioMixerDialog()



                                    }



                                )



                                TvHeaderIconButton(



                                    icon = "🎬",



                                    borderColor = uniformBorder,



                                    isActive = state.isBackgroundDialogOpen,



                                    onClick = {



                                        interactionTick++



                                        onOpenBackgroundDialog()



                                    }



                                )



                                TvHeaderIconButton(



                                    icon = "📱",



                                    borderColor = uniformBorder,



                                    isActive = showTvKeypadModal,



                                    onClick = {



                                        interactionTick++



                                        showTvKeypadModal = true



                                    }



                                )

                                TvHeaderIconButton(
                                    icon = "📲",
                                    borderColor = uniformBorder,
                                    isActive = showTvQrBarcodeModal,
                                    onClick = {
                                        interactionTick++
                                        showTvQrBarcodeModal = !showTvQrBarcodeModal
                                    }
                                )



                                if (!isTvMode) {



                                    Surface(



                                        onClick = onToggleTvMode,



                                        shape = RoundedCornerShape(6.dp),



                                        color = Color(state.appTheme.primaryColor),



                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))



                                    ) {



                                        Row(



                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),



                                            verticalAlignment = Alignment.CenterVertically,



                                            horizontalArrangement = Arrangement.spacedBy(4.dp)



                                        ) {



                                            Text("📱", fontSize = 12.sp)



                                            Text(



                                                "PORTRAIT",



                                                color = Color.Black,



                                                fontSize = 11.sp,



                                                fontWeight = FontWeight.Bold



                                            )



                                        }



                                    }



                                }



                            }



                        }







                        // When right buttons auto-hide in Landscape, show clean Return to Portrait button



                        if (!showLandscapeControls && !isTvMode) {



                            Row(



                                horizontalArrangement = Arrangement.spacedBy(8.dp),



                                verticalAlignment = Alignment.CenterVertically,



                                modifier = Modifier.padding(start = 10.dp, end = 4.dp)



                            ) {



                                Surface(



                                    onClick = onToggleTvMode,



                                    shape = RoundedCornerShape(6.dp),



                                    color = Color.Black.copy(alpha = 0.7f),



                                    border = BorderStroke(1.dp, uniformBorder)



                                ) {



                                    Row(



                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),



                                        verticalAlignment = Alignment.CenterVertically,



                                        horizontalArrangement = Arrangement.spacedBy(4.dp)



                                    ) {



                                        Text("📱", fontSize = 11.sp)



                                        Text(



                                            "PORTRAIT",



                                            color = Color.White,



                                            fontSize = 10.sp,



                                            fontWeight = FontWeight.Bold



                                        )



                                    }



                                }



                            }



                        }



                    }



                }







                // Sub-header Ticker: R: (Yellow R without circle on the side, matching reserved codes)



                if (shouldShowHeader && state.reserveQueue.isNotEmpty()) {



                    Row(



                        modifier = Modifier



                            .fillMaxWidth()



                            .offset(y = if (isLandscape) (-7).dp else (-2).dp)



                            .padding(horizontal = 12.dp, vertical = 0.dp),



                        horizontalArrangement = Arrangement.Start,



                        verticalAlignment = Alignment.CenterVertically



                    ) {



                        val reserveCodes = state.reserveQueue.joinToString(separator = "   ") { it.code }



                        val reserveThemeColor = Color(state.appTheme.secondaryColor)



                        OutlinedHeaderText(



                            text = "R: ",



                            color = reserveThemeColor,



                            fontSize = if (isTvMode) 14.5.sp else if (isLandscape) 12.5.sp else 11.sp,



                            isLandscape = isLandscape,



                            fontFamily = CleanFont,



                            fontWeight = FontWeight.Black,



                            modifier = Modifier.clickable { onToggleReserveQueue() }



                        )



                        OutlinedHeaderText(



                            text = reserveCodes,



                            color = reserveThemeColor,



                            fontSize = if (isTvMode) 14.5.sp else if (isLandscape) 12.5.sp else 11.sp,



                            isLandscape = isLandscape,



                            fontFamily = CleanFont,



                            fontWeight = FontWeight.Bold,



                            modifier = Modifier



                                .weight(1f)



                                .basicMarquee(iterations = Int.MAX_VALUE)



                                .clickable { onToggleReserveQueue() }



                        )



                    }



                } else if (!isTvMode && state.reserveQueue.isNotEmpty()) {



                    Row(



                        modifier = Modifier



                            .fillMaxWidth()



                            .offset(y = (-2).dp)



                            .padding(horizontal = 12.dp, vertical = 0.dp),



                        horizontalArrangement = Arrangement.Start,



                        verticalAlignment = Alignment.CenterVertically



                    ) {



                        val reserveCodes = state.reserveQueue.joinToString(separator = "   ") { it.code }



                        OutlinedHeaderText(



                            text = "R: ",



                            color = Color(0xFFFFD54F),



                            fontSize = 11.sp,



                            isLandscape = false,



                            fontFamily = CleanFont,



                            modifier = Modifier.clickable { onToggleReserveQueue() }



                        )



                        OutlinedHeaderText(



                            text = reserveCodes,



                            color = Color(0xFFFFD54F),



                            fontSize = 11.sp,



                            isLandscape = false,



                            fontFamily = CleanFont,



                            modifier = Modifier



                                .weight(1f)



                                .basicMarquee(iterations = Int.MAX_VALUE)



                                .clickable { onToggleReserveQueue() }



                        )



                    }



                }



            }







            // WHEN SONGBOOK, RESERVED QUEUE (R), OR FAVORITES (❤️) OVERLAY IS OPEN:



            // Show the form in the upper area and automatically switch lyrics to 2 lines at the bottom so they are NEVER covered!



            // WHEN SONGBOOK, RESERVED QUEUE (R), OR FAVORITES (❤️) OVERLAY IS OPEN:



            // In TV mode while playing: Side-by-side split screen so lyrics are NEVER covered!



            // In phone mode or while idle: Standard layout with lyrics below if playing.



            val showUpperOverlay = state.isSongbookOpen || state.isReserveQueueOpen || state.isFavoritesOpen



            if (showUpperOverlay) {



                if (isLandscape && state.isPlaying) {



                    // LANDSCAPE SIDE-BY-SIDE SPLIT: Left 52% Songbook / Right 48% Dedicated Uncovered Lyrics!



                    Row(



                        modifier = Modifier



                            .fillMaxWidth()



                            .weight(1f)



                            .padding(horizontal = 14.dp, vertical = 2.dp),



                        horizontalArrangement = Arrangement.spacedBy(12.dp),



                        verticalAlignment = Alignment.CenterVertically



                    ) {



                        SongbookSearchOverlay(



                            songs = state.searchResults,



                            searchQuery = state.songbookQuery,



                            reserveQueue = state.reserveQueue,



                            favoriteSongs = state.favoriteSongs,



                            favoriteCodes = state.favoriteCodes,



                            theme = state.appTheme,



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



                            isLandscape = true,



                            onSwitchTab = { tab ->



                                when (tab) {



                                    0 -> if (!state.isSongbookOpen) onOpenSongbook()



                                    1 -> if (!state.isReserveQueueOpen) onToggleReserveQueue()



                                    2 -> if (!state.isFavoritesOpen) onToggleFavorites()



                                }



                            },



                            onToggleFilter = onToggleSongbookFilter,



                            onOpenFolderLocator = onOpenSongFolderLocator,



                            onOpenYouTubeDownloader = onOpenYouTubeDownloader,



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



                            onSpeakDigit = onDigit,



                            modifier = Modifier



                                .widthIn(max = 480.dp)



                                .weight(if (isTvMode) 0.44f else 0.48f)



                                .fillMaxHeight()



                        )







                        // Dedicated TV Lyrics Panel on Right Side



                        Column(



                            modifier = Modifier



                                .weight(1f)



                                .fillMaxHeight()



                                .clip(RoundedCornerShape(12.dp))



                                .background(Color(0xD9050E1A))



                                .border(1.5.dp, Color(state.appTheme.primaryColor), RoundedCornerShape(12.dp))



                                .padding(horizontal = 16.dp, vertical = 12.dp),



                            horizontalAlignment = Alignment.CenterHorizontally,



                            verticalArrangement = Arrangement.Center



                        ) {



                            // Song info badge



                            Row(



                                verticalAlignment = Alignment.CenterVertically,



                                horizontalArrangement = Arrangement.spacedBy(6.dp),



                                modifier = Modifier



                                    .clip(RoundedCornerShape(20.dp))



                                    .background(Color(0xFF1E293B))



                                    .padding(horizontal = 12.dp, vertical = 4.dp)



                            ) {



                                Text("🎵", fontSize = 14.sp)



                                Text(



                                    text = "${state.currentSong.title} • ${state.currentSong.artist}".uppercase(),



                                    color = Color(0xFFFFD54F),



                                    fontSize = 13.sp,



                                    fontWeight = FontWeight.Bold,



                                    maxLines = 1,



                                    overflow = TextOverflow.Ellipsis



                                )



                            }



                            Spacer(modifier = Modifier.height(20.dp))



                            val isTopActive = state.activeLyricPosition == ActiveLyricPosition.TOP



                            val isBottomActive = state.activeLyricPosition == ActiveLyricPosition.BOTTOM



                            KaraokeLyricLine(



                                text = state.landscapeLine1,



                                isActive = isTopActive,



                                wipePercent = if (isTopActive) state.wipePercent else 0f,



                                fontSize = ((if (isTvMode) 32f else 26f) * 0.85f).sp,



                                lyricConfig = effectiveLyricConfig,



                                countdownNumber = state.countdownDots,



                                isLandscape = true



                            )



                            Spacer(modifier = Modifier.height(10.dp))



                            KaraokeLyricLine(



                                text = state.landscapeLine2,



                                isActive = isBottomActive,



                                wipePercent = if (isBottomActive) state.wipePercent else 0f,



                                fontSize = ((if (isTvMode) 32f else 26f) * 0.85f).sp,



                                lyricConfig = effectiveLyricConfig,



                                countdownNumber = 0,



                                isLandscape = true



                            )



                        }



                    }



                } else {



                    // Standard Layout (Vertical Stack for Phone or Idle TV)



                    SongbookSearchOverlay(



                        songs = state.searchResults,



                        searchQuery = state.songbookQuery,



                        reserveQueue = state.reserveQueue,



                        favoriteSongs = state.favoriteSongs,



                        favoriteCodes = state.favoriteCodes,



                        theme = state.appTheme,



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



                        onOpenYouTubeDownloader = onOpenYouTubeDownloader,



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



                        onSpeakDigit = onDigit,



                        modifier = Modifier



                            .then(



                                if (isTvMode) Modifier.widthIn(min = 480.dp, max = 580.dp).fillMaxWidth(0.62f)



                                else if (isLandscape) Modifier.widthIn(max = 580.dp).fillMaxWidth(0.85f)



                                else Modifier.fillMaxWidth()



                            )



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



                                fontSize = if (isLandscape) 24.sp else 20.sp,



                                lyricConfig = effectiveLyricConfig,



                                countdownNumber = state.countdownDots,



                                isLandscape = isLandscape



                            )



                            KaraokeLyricLine(



                                text = state.landscapeLine2,



                                isActive = isBottomActive,



                                wipePercent = if (isBottomActive) state.wipePercent else 0f,



                                fontSize = if (isLandscape) 24.sp else 20.sp,



                                lyricConfig = effectiveLyricConfig,



                                countdownNumber = 0,



                                isLandscape = isLandscape



                            )



                        }



                    }



                }



            } else if (!state.isPlaying) {



                // STANDBY / WELCOME SCREEN: SELECT A SONG & Advancing 6-Digit Slots



                WelcomeSongSelectionScreen(



                    state = state,



                    isLandscape = isLandscape,



                    isTvMode = isTvMode,



                    onPlay = onPlay,



                    onReserve = onReserve,



                    modifier = Modifier



                        .fillMaxWidth()



                        .weight(1f)



                )



            } else if (state.showSongTitleIntro && !state.isVideoSong && !state.isPlayingYouTubeStream) {



                // Song Title & Artist Intro Banner on Song Selection (Elevated and enlarged in TV mode!)



                Box(



                    modifier = Modifier



                        .fillMaxWidth()



                        .weight(1f),



                    contentAlignment = Alignment.Center



                ) {



                    SongIntroTitleSplash(



                        song = state.currentSong,



                        isLandscape = isLandscape,



                        lang = state.appLanguage,



                        isTvMode = isTvMode,



                        modifier = Modifier



                    )



                }



            } else if (state.isPlayingYouTubeStream && !state.activeYouTubeStreamVideoId.isNullOrBlank()) {



                // AD-FREE DIRECT YOUTUBE STREAM PLAYER



                Box(



                    modifier = Modifier



                        .fillMaxWidth()



                        .weight(1f),



                    contentAlignment = Alignment.Center



                ) {



                    AdFreeYouTubeStreamPlayer(



                        videoId = state.activeYouTubeStreamVideoId,



                        title = state.activeYouTubeStreamTitle ?: state.currentSong.title,



                        theme = state.appTheme,



                        onStop = onStopYouTubeStream,



                        onScore = {



                            onStopYouTubeStream()



                            onTriggerScore()



                        },



                        onToggleLandscape = onToggleTvMode,



                        isLandscape = isLandscape,



                        modifier = Modifier.fillMaxSize()



                    )



                }



            } else if (state.isVideoSong) {



                // VIDEO KARAOKE (MP4 with embedded lyrics & audio) - Title box auto-hides quickly!



                var showMp4TitleBox by remember(state.currentSong.code, state.currentSong.filePath) {



                    mutableStateOf(true)



                }



                LaunchedEffect(state.currentSong.code, state.currentSong.filePath) {



                    showMp4TitleBox = true



                    kotlinx.coroutines.delay(3500)



                    showMp4TitleBox = false



                }







                androidx.compose.animation.AnimatedVisibility(



                    visible = showMp4TitleBox,



                    enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300)) + androidx.compose.animation.slideInVertically(androidx.compose.animation.core.tween(300)) { -it },



                    exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(500)) + androidx.compose.animation.slideOutVertically(androidx.compose.animation.core.tween(500)) { -it },



                    modifier = Modifier



                        .fillMaxWidth()



                        .weight(1f)



                        .padding(16.dp)



                ) {



                    Box(contentAlignment = Alignment.TopStart) {



                        Surface(



                            color = Color.Black.copy(alpha = 0.75f),



                            shape = RoundedCornerShape(10.dp),



                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(state.appTheme.primaryColor).copy(alpha = 0.8f)),



                            modifier = Modifier.clickable { showMp4TitleBox = false }



                        ) {



                            Row(



                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),



                                verticalAlignment = Alignment.CenterVertically,



                                horizontalArrangement = Arrangement.spacedBy(8.dp)



                            ) {



                                Text("🎬", fontSize = 16.sp)



                                Column {



                                    Text(



                                        text = state.currentSong.title,



                                        color = Color.White,



                                        fontSize = 13.sp,



                                        fontWeight = FontWeight.Bold,



                                        maxLines = 1



                                    )



                                    Text(



                                        text = state.currentSong.artist,



                                        color = Color(state.appTheme.primaryColor),



                                        fontSize = 11.sp,



                                        fontWeight = FontWeight.Medium,



                                        maxLines = 1



                                    )



                                }



                            }



                        }



                    }



                }



            } else if (state.isInstrumental) {



                // INSTRUMENTAL PLAYING (NO MELODY/SINGING) -> DEAD CENTER OF PLAYER SCREEN



                Box(



                    modifier = Modifier



                        .fillMaxWidth()



                        .weight(1f),



                    contentAlignment = Alignment.Center



                ) {



                    InstrumentalDisplay(



                        theme = state.appTheme,



                        isLandscape = isLandscape,



                        modifier = if (isLandscape) Modifier.offset(y = (-45).dp) else Modifier



                    )



                }



            } else {



                // PLAYING SCREEN WITH VOCALS / LYRICS



                val topPadding = when (state.lyricConfig.position) {



                    LyricScreenPosition.TOP -> if (isLandscape) 12.dp else 6.dp



                    LyricScreenPosition.CENTER -> 0.dp



                    LyricScreenPosition.BOTTOM -> 0.dp



                }







                val bottomPadding = if (isTvMode) {



                    58.dp



                } else if (isLandscape) {



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



                            .padding(horizontal = if (isLandscape) 20.dp else 10.dp),



                        horizontalAlignment = Alignment.CenterHorizontally,



                        verticalArrangement = Arrangement.spacedBy(6.dp)



                    ) {



                        // LYRIC DISPLAY: 2 LINES or 4 LINES (CLEAN, NO ENCLOSING BOX!)



                        Box(



                            modifier = Modifier



                                .fillMaxWidth()



                                .padding(



                                    start = if (isLandscape) 12.dp else 4.dp,



                                    end = if (isLandscape) 12.dp else 4.dp,



                                    top = 0.dp



                                )



                        ) {



                            val screenWidthDp = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.toFloat()



                            val safeWidthDp = (screenWidthDp - (if (isLandscape) 56f else 28f)).coerceAtLeast(260f)



                            val showFourLines = !isLandscape && state.lyricConfig.lineCount == LyricLineCount.FOUR_LINES



                            if (showFourLines) {



                                // 4 LINES DISPLAY (Portrait only if user selected 4 lines)



                                val linesList = listOf(



                                    Pair(state.line1, 1),



                                    Pair(state.line2, 2),



                                    Pair(state.line3, 3),



                                    Pair(state.line4, 4)



                                )



                                // Steady base font size for the song (never jumps back and forth when lines rotate!)



                                val baseSizeSp = if (isLandscape) (effectiveLyricConfig.fontSizeLandscapeSp * 0.65f) else (effectiveLyricConfig.fontSizePortraitSp * 0.65f)







                                Column(



                                    modifier = Modifier.fillMaxWidth(),



                                    horizontalAlignment = Alignment.CenterHorizontally,



                                    verticalArrangement = Arrangement.spacedBy((effectiveLyricConfig.lineSpacingDp * 0.65f).dp)



                                ) {



                                    linesList.forEach { (lineText, lineNum) ->



                                        if (lineText.isNotEmpty()) {



                                            val isActive = state.activeLineNumber == lineNum



                                            val lineCountdown = if (lineNum == state.activeLineNumber) state.countdownDots else 0







                                            // Per-line independent safe fit:



                                            // Normal lines stay 100% steady at baseSizeSp.



                                            // Only an exceptionally long line gently scales down to fit safeWidthDp, without moving or shrinking any other line!



                                            val lineChars = lineText.length.coerceAtLeast(1)



                                            val estLineWidthDp = lineChars * baseSizeSp * effectiveLyricConfig.fontScale * 0.70f + 16f



                                            val lineScale = if (estLineWidthDp > safeWidthDp) (safeWidthDp / estLineWidthDp).coerceIn(0.50f, 1.0f) else 1.0f



                                            val perLineFontSize = (baseSizeSp * lineScale).sp







                                            KaraokeLyricLine(



                                                text = lineText,



                                                isActive = isActive,



                                                wipePercent = if (isActive) state.wipePercent else 0f,



                                                fontSize = perLineFontSize,



                                                lyricConfig = effectiveLyricConfig,



                                                countdownNumber = lineCountdown,



                                                isLandscape = isLandscape



                                            )



                                        }



                                    }



                                }



                            } else {



                                // 2 LINES DISPLAY (DEFAULT) - Scale to small 85% in phone landscape

                                val baseSizeSp = if (isLandscape) (effectiveLyricConfig.fontSizeLandscapeSp * (if (isTvMode) 1.0f else 0.85f)) else effectiveLyricConfig.fontSizePortraitSp







                                val line1Chars = state.landscapeLine1.length.coerceAtLeast(1)



                                val estLine1WidthDp = line1Chars * baseSizeSp * effectiveLyricConfig.fontScale * 0.58f + 16f



                                val line1Scale = if (estLine1WidthDp > safeWidthDp) (safeWidthDp / estLine1WidthDp).coerceIn(0.60f, 1.0f) else 1.0f



                                val line1FontSize = (baseSizeSp * line1Scale).sp







                                val line2Chars = state.landscapeLine2.length.coerceAtLeast(1)



                                val estLine2WidthDp = line2Chars * baseSizeSp * effectiveLyricConfig.fontScale * 0.58f + 16f



                                val line2Scale = if (estLine2WidthDp > safeWidthDp) (safeWidthDp / estLine2WidthDp).coerceIn(0.60f, 1.0f) else 1.0f



                                val line2FontSize = (baseSizeSp * line2Scale).sp







                                Column(



                                    modifier = Modifier.fillMaxWidth(),



                                    horizontalAlignment = Alignment.CenterHorizontally,



                                    verticalArrangement = Arrangement.spacedBy(effectiveLyricConfig.lineSpacingDp.dp)



                                ) {



                                    val isTopActive = state.activeLyricPosition == ActiveLyricPosition.TOP



                                    val isBottomActive = state.activeLyricPosition == ActiveLyricPosition.BOTTOM



                                    val topCountdown = if (isTopActive) state.countdownDots else 0



                                    val bottomCountdown = if (isBottomActive) state.countdownDots else 0







                                    KaraokeLyricLine(



                                        text = state.landscapeLine1,



                                        isActive = isTopActive,



                                        wipePercent = if (isTopActive) state.wipePercent else 0f,



                                        fontSize = line1FontSize,



                                        lyricConfig = effectiveLyricConfig,



                                        countdownNumber = topCountdown,



                                        isLandscape = isLandscape



                                    )



                                    KaraokeLyricLine(



                                        text = state.landscapeLine2,



                                        isActive = isBottomActive,



                                        wipePercent = if (isBottomActive) state.wipePercent else 0f,



                                        fontSize = line2FontSize,



                                        lyricConfig = effectiveLyricConfig,



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



        // In TV mode, elevated to top so it never obstructs singing lyrics!



        // 100% synchronized with active theme colors!



        if (state.isPlaying && state.enteredCode.isNotEmpty()) {



            val theme = state.appTheme



            val boxBorderColor = if (state.matchedEnteredSong != null) Color(theme.primaryColor) else Color(theme.secondaryColor)



            Box(



                modifier = Modifier



                    .align(if (isTvMode) Alignment.TopCenter else Alignment.BottomCenter)



                    .fillMaxWidth()



                    .padding(



                        horizontal = 12.dp,



                        vertical = if (isTvMode) 38.dp else 4.dp



                    )



                    .height(if (isTvMode) 52.dp else 46.dp)



                    .shadow(8.dp, RoundedCornerShape(10.dp))



                    .clip(RoundedCornerShape(10.dp))



                    .background(Color(theme.surfaceColor).copy(alpha = 0.95f))



                    .border(1.5.dp, boxBorderColor, RoundedCornerShape(10.dp))



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



                        Text(text = "🔢", fontSize = if (isTvMode) 16.sp else 13.sp)



                        Spacer(modifier = Modifier.width(6.dp))



                        Text(



                            text = state.enteredCode.padStart(6, '0'),



                            color = Color(theme.primaryColor),



                            fontFamily = FontFamily.Monospace,



                            fontWeight = FontWeight.Black,



                            fontSize = if (isTvMode) 18.sp else 14.sp



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



                                color = Color(theme.secondaryColor),



                                fontWeight = FontWeight.Black,



                                fontSize = if (isTvMode) 15.sp else 12.sp,



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



                                color = Color(theme.textMutedColor),



                                fontSize = if (isTvMode) 13.sp else 11.sp,



                                fontWeight = FontWeight.Bold,



                                textAlign = TextAlign.Center,



                                maxLines = 1,



                                modifier = Modifier.fillMaxWidth()



                            )



                        } else {



                            Text(



                                text = if (isTagalog) "Walang kanta sa code" else "Song code not found",



                                color = Color(0xFFFF8A80),



                                fontSize = if (isTvMode) 13.sp else 11.sp,



                                fontWeight = FontWeight.Bold,



                                textAlign = TextAlign.Center,



                                maxLines = 1,



                                modifier = Modifier.fillMaxWidth()



                            )



                        }



                    }







                    // Right: Clickable Automatic Reserve (R RESERVE) & Play buttons!



                    if (state.matchedEnteredSong != null) {



                        val rBtnText = if (isTagalog) "R IRESERBA" else "R RESERVE"



                        val playTextColor = getContrastingTextColor(theme.primaryColor)



                        Row(



                            horizontalArrangement = Arrangement.spacedBy(6.dp),



                            verticalAlignment = Alignment.CenterVertically



                        ) {



                            Box(



                                modifier = Modifier



                                    .clip(RoundedCornerShape(6.dp))



                                    .background(Color(theme.buttonBgColor))



                                    .border(1.2.dp, Color(theme.buttonBorderColor), RoundedCornerShape(6.dp))



                                    .clickable { onReserve() }



                                    .padding(horizontal = 10.dp, vertical = 6.dp),



                                contentAlignment = Alignment.Center



                            ) {



                                Text(



                                    text = rBtnText,



                                    color = Color(theme.textColor),



                                    fontWeight = FontWeight.Black,



                                    fontSize = if (isTvMode) 12.sp else 10.5.sp



                                )



                            }



                            val isPlayingActive = state.isPlaying && !state.isPlaybackPaused
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isPlayingActive) Color(0xFFF59E0B) else Color(theme.primaryColor))
                                    .border(1.dp, if (isPlayingActive) Color(0xFFD97706) else Color(theme.primaryColor), RoundedCornerShape(6.dp))
                                    .clickable { onPlay() }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isPlayingActive) "⏸" else "▶",
                                    color = if (isPlayingActive) Color.White else playTextColor,
                                    fontWeight = FontWeight.Black,
                                    fontSize = if (isTvMode) 12.sp else 10.5.sp
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







        // TV MODE AUTOMATIC PERSISTENT REMOTE QR BARCODE



        // Automatically visible on TV mode in STANDBY until a phone remote client connects!



        // Never shown during playback or on MP4/YouTube video karaoke!



        if (showTvQrBarcodeModal) {
            val deviceIp = remember { com.example.videokey.remote.VideokeRemoteServer.getLocalIpAddress() }
            val qrBitmap = remember(deviceIp) {
                com.example.videokey.remote.QrCodeGenerator.generateQrBitmap("http://$deviceIp:8080", 300)
            }
            androidx.compose.ui.window.Dialog(onDismissRequest = { showTvQrBarcodeModal = false }) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xF00A111E),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF38BDF8)),
                    modifier = Modifier.widthIn(max = 320.dp).padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(text = "📱", fontSize = 16.sp)
                                Text(
                                    text = if (isTagalog) "PHONE REMOTE" else "PHONE REMOTE",
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                            }
                            IconButton(onClick = { showTvQrBarcodeModal = false }, modifier = Modifier.size(28.dp)) {
                                Text(text = "✕", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (qrBitmap != null) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White
                            ) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "TV Remote QR Barcode",
                                    modifier = Modifier.size(150.dp).padding(6.dp)
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
                            text = if (isTagalog) "I-scan gamit ang cellphone camera para kumonekta ang remote control"
                                   else "Scan with phone camera to connect remote control",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center
                        )

                        Button(
                            onClick = { showTvQrBarcodeModal = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(36.dp)
                        ) {
                            Text(text = if (isTagalog) "ISARA" else "CLOSE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }





    }



}







@Composable



fun SongbookButtonGraphic(



    showLabel: Boolean = true,



    iconWidth: androidx.compose.ui.unit.Dp = 20.dp,



    iconHeight: androidx.compose.ui.unit.Dp = 13.dp



) {



    Box(



        modifier = Modifier.fillMaxSize(),



        contentAlignment = Alignment.Center



    ) {



        Column(



            horizontalAlignment = Alignment.CenterHorizontally,



            verticalArrangement = Arrangement.Center



        ) {



            androidx.compose.foundation.Canvas(



                modifier = Modifier.size(width = iconWidth, height = iconHeight)



            ) {



                val w = size.width



                val h = size.height



                // 1. Bright Gold outer hardcover book base



                drawRoundRect(



                    color = Color(0xFFFFB300),



                    topLeft = Offset(0f, h * 0.08f),



                    size = androidx.compose.ui.geometry.Size(w, h * 0.88f),



                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.14f, w * 0.14f)



                )



                // 2. Crisp Pure White left page



                drawRoundRect(



                    color = Color.White,



                    topLeft = Offset(w * 0.08f, h * 0.02f),



                    size = androidx.compose.ui.geometry.Size(w * 0.40f, h * 0.80f),



                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f, w * 0.08f)



                )



                // 3. Crisp Pure White right page



                drawRoundRect(



                    color = Color.White,



                    topLeft = Offset(w * 0.52f, h * 0.02f),



                    size = androidx.compose.ui.geometry.Size(w * 0.40f, h * 0.80f),



                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f, w * 0.08f)



                )



                // 4. Center book spine in dark slate



                drawLine(



                    color = Color(0xFF0F172A),



                    start = Offset(w * 0.50f, h * 0.02f),



                    end = Offset(w * 0.50f, h * 0.92f),



                    strokeWidth = w * 0.06f



                )



                // 5. High-contrast text lines on left page



                val lineStroke = (h * 0.09f).coerceAtLeast(1.5f)



                drawLine(



                    color = Color(0xFF0284C7),



                    start = Offset(w * 0.15f, h * 0.25f),



                    end = Offset(w * 0.41f, h * 0.25f),



                    strokeWidth = lineStroke



                )



                drawLine(



                    color = Color(0xFF1E293B),



                    start = Offset(w * 0.15f, h * 0.46f),



                    end = Offset(w * 0.41f, h * 0.46f),



                    strokeWidth = lineStroke



                )



                drawLine(



                    color = Color(0xFF1E293B),



                    start = Offset(w * 0.15f, h * 0.66f),



                    end = Offset(w * 0.36f, h * 0.66f),



                    strokeWidth = lineStroke



                )



                // 6. High-contrast text lines on right page



                drawLine(



                    color = Color(0xFF0284C7),



                    start = Offset(w * 0.59f, h * 0.25f),



                    end = Offset(w * 0.85f, h * 0.25f),



                    strokeWidth = lineStroke



                )



                drawLine(



                    color = Color(0xFF1E293B),



                    start = Offset(w * 0.59f, h * 0.46f),



                    end = Offset(w * 0.85f, h * 0.46f),



                    strokeWidth = lineStroke



                )



                drawLine(



                    color = Color(0xFF1E293B),



                    start = Offset(w * 0.59f, h * 0.66f),



                    end = Offset(w * 0.80f, h * 0.66f),



                    strokeWidth = lineStroke



                )



            }



            if (showLabel) {



                Spacer(modifier = Modifier.height(2.dp))



                Text(



                    text = "SONGBOOK",



                    color = Color(0xFFFFD54F),



                    fontSize = 8.sp,



                    lineHeight = 8.5.sp,



                    fontWeight = FontWeight.Black,



                    letterSpacing = 0.3.sp,



                    textAlign = TextAlign.Center,



                    maxLines = 1,



                    style = TextStyle(



                        platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)



                    )



                )



            }



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



            if (icon == "📖") {



                SongbookButtonGraphic(showLabel = false, iconWidth = 19.dp, iconHeight = 14.dp)



            } else {



                Text(



                    text = icon,



                    color = if (icon == "R") Color(0xFFFFD54F) else Color.White,



                    fontSize = 14.sp,



                    fontWeight = FontWeight.Black



                )



            }



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



    isTvMode: Boolean = false,



    onPlay: () -> Unit = {},



    onReserve: () -> Unit = {},



    modifier: Modifier = Modifier



) {



    val theme = state.appTheme



    val primaryColor = Color(theme.primaryColor)



    val secondaryColor = Color(theme.secondaryColor)



    val surfaceColor = Color(theme.surfaceColor)



    val textColor = Color(theme.textColor)



    val textMutedColor = Color(theme.textMutedColor)



    val btnBorderColor = Color(theme.buttonBorderColor)







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



        // In TV mode, elevated to the upper portion of the screen for maximum visibility!



        Column(



            modifier = Modifier



                .fillMaxWidth()



                .weight(1f)



                .padding(top = if (isTvMode || isLandscape) 24.dp else 0.dp),



            horizontalAlignment = Alignment.CenterHorizontally,



            verticalArrangement = if (isTvMode || isLandscape) Arrangement.Top else Arrangement.Center



        ) {



            // Theme-reactive Header: ★ SELECT A SONG ★



            val titleText = if (isTagalog) "PUMILI NG KANTA" else "SELECT A SONG"



            val titleFontSize = if (isTvMode && isLandscape) 46.sp else if (isLandscape) 36.sp else 28.sp



            val starFontSize = if (isTvMode && isLandscape) 40.sp else if (isLandscape) 32.sp else 24.sp



            Row(



                verticalAlignment = Alignment.CenterVertically,



                horizontalArrangement = Arrangement.spacedBy(8.dp)



            ) {



                Text(



                    text = "★",



                    color = secondaryColor,



                    fontSize = starFontSize,



                    fontFamily = CleanFont,



                    fontWeight = FontWeight.Bold



                )



                Text(



                    text = titleText,



                    color = primaryColor,



                    fontWeight = FontWeight.Bold,



                    fontFamily = CleanFont,



                    fontSize = titleFontSize,



                    letterSpacing = 2.sp,



                    maxLines = 1,



                    softWrap = false



                )



                Text(



                    text = "★",



                    color = secondaryColor,



                    fontSize = starFontSize,



                    fontFamily = CleanFont,



                    fontWeight = FontWeight.Bold



                )



            }







            Text(



                text = if (isTagalog) "ITIPA ANG 6-DIGIT NA NUMERO SA KEYPAD" else "ENTER 6-DIGIT SONG NUMBER ON KEYPAD",



                color = secondaryColor,



                fontSize = if (isTvMode && isLandscape) 14.sp else if (isLandscape) 12.sp else 10.sp,



                fontWeight = FontWeight.Bold,



                fontFamily = CleanFont,



                letterSpacing = 1.sp,



                modifier = Modifier.padding(top = if (isTvMode) 8.dp else 2.dp, bottom = if (isTvMode) 18.dp else if (isLandscape) 10.dp else 6.dp)



            )







            // 6 Advancing Digit Slots: Starts from Right Side [ 0 ] [ 0 ] [ 0 ] [ 0 ] [ 0 ] [ 0 ]



            Row(



                horizontalArrangement = Arrangement.spacedBy(if (isTvMode && isLandscape) 14.dp else if (isLandscape) 10.dp else 6.dp),



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



                        isActiveSlot -> primaryColor.copy(alpha = pulseBorderAlpha)



                        isFilled -> secondaryColor



                        else -> btnBorderColor



                    }







                    val digitColor = when {



                        isFilled -> secondaryColor



                        isActiveSlot -> primaryColor.copy(alpha = pulseBorderAlpha * 0.7f + 0.3f)



                        else -> textMutedColor.copy(alpha = 0.35f)



                    }







                    val boxBg = when {



                        isActiveSlot -> primaryColor.copy(alpha = 0.15f)



                        isFilled -> secondaryColor.copy(alpha = 0.12f)



                        else -> surfaceColor.copy(alpha = 0.85f)



                    }







                    Box(



                        modifier = Modifier



                            .size(



                                width = if (isTvMode && isLandscape) 64.dp else if (isLandscape) 52.dp else 40.dp,



                                height = if (isTvMode && isLandscape) 74.dp else if (isLandscape) 60.dp else 46.dp



                            )



                            .clip(RoundedCornerShape(8.dp))



                            .background(boxBg)



                            .border(



                                width = if (isActiveSlot) 2.dp else 1.2.dp,



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



                                fontFamily = CleanFont,



                                fontWeight = FontWeight.ExtraBold,



                                fontSize = if (isTvMode && isLandscape) 40.sp else if (isLandscape) 30.sp else 22.sp



                            )







                            // Active slot pulsing cursor underline



                            if (isActiveSlot) {



                                Box(



                                    modifier = Modifier



                                        .padding(top = 2.dp)



                                        .width(if (isTvMode && isLandscape) 26.dp else if (isLandscape) 20.dp else 14.dp)



                                        .height(2.5.dp)



                                        .background(primaryColor.copy(alpha = pulseBorderAlpha), RoundedCornerShape(1.dp))



                                )



                            }



                        }



                    }



                }



            }



        }







        // FIXED FULL-BOX RESERVE / SONG PREVIEW CONTAINER ABOVE THE NUMBERING KEYPAD:



        // Always full-width, fixed height, synchronized with active theme colors!



        val boxBorderColor = when {



            state.matchedEnteredSong != null -> secondaryColor



            state.alertMessage != null -> Color(0xFFEF4444)



            state.enteredCode.isNotEmpty() -> primaryColor



            else -> btnBorderColor



        }



        val boxBgColor = when {



            state.matchedEnteredSong != null -> surfaceColor



            state.alertMessage != null -> Color(0xEE450A0A)



            state.enteredCode.isNotEmpty() -> surfaceColor



            else -> surfaceColor.copy(alpha = 0.85f)



        }







        Box(



            modifier = Modifier



                .fillMaxWidth(if (isTvMode) 0.88f else if (isLandscape) 0.78f else 1f)



                .height(if (isTvMode) 76.dp else if (isLandscape) 66.dp else 58.dp)



                .clip(RoundedCornerShape(10.dp))



                .background(boxBgColor)



                .border(1.2.dp, boxBorderColor, RoundedCornerShape(10.dp))



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



                        color = primaryColor,



                        fontWeight = FontWeight.Black,



                        fontFamily = CleanFont,



                        fontSize = if (isTvMode) 19.sp else if (isLandscape) 16.sp else 13.sp,



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



                        color = textColor,



                        fontWeight = FontWeight.Bold,



                        fontFamily = CleanFont,



                        fontSize = if (isTvMode) 15.sp else if (isLandscape) 13.sp else 11.sp,



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



                    fontFamily = CleanFont,



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



                    color = secondaryColor,



                    fontSize = if (isLandscape) 13.sp else 11.5.sp,



                    fontWeight = FontWeight.Bold,



                    fontFamily = CleanFont,



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



                        color = textColor,



                        fontSize = if (isLandscape) 12.sp else 10.5.sp,



                        fontWeight = FontWeight.Bold,



                        fontFamily = CleanFont,



                        textAlign = TextAlign.Center,



                        modifier = Modifier.fillMaxWidth()



                    )



                    Spacer(modifier = Modifier.height(2.dp))



                    Text(



                        text = if (isTagalog)



                            "Halimbawa: 000016 • Pindutin ang R para sa Reserved o ❤️ para sa Favorites"



                        else



                            "Example: 000016 • Tap R for Reserved Queue or ❤️ for Favorites",



                        color = textMutedColor,



                        fontSize = if (isLandscape) 11.sp else 9.5.sp,



                        fontWeight = FontWeight.Medium,



                        fontFamily = CleanFont,



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



    val theme = state.appTheme



    val btnTextColor = getContrastingTextColor(theme.buttonBgColor)



    val playTextColor = getContrastingTextColor(theme.primaryColor)







    // VIP Console & Button Texture: distinct, beautiful visible styling for all keypad & console buttons

    val effectiveTexture = if (state.isVipActivated) state.vipButtonTexture else VipButtonTexture.DEFAULT
    val (uniformBtnBrush, uniformBorderBrush, consoleBgBrush) = when (effectiveTexture) {

        VipButtonTexture.CARBON_FIBER -> Triple(

            Brush.verticalGradient(listOf(Color(0xFF2E3846), Color(0xFF1E2530), Color(0xFF131820))),

            Brush.linearGradient(listOf(Color(0xFF94A3B8), Color(0xFF475569), Color(0xFF64748B))),

            Brush.verticalGradient(listOf(Color(0xFF0F172A).copy(alpha = 0.95f), Color(0xFF020617).copy(alpha = 0.98f)))

        )

        VipButtonTexture.BRUSHED_METAL -> Triple(

            Brush.verticalGradient(listOf(Color(0xFF475569), Color(0xFF334155), Color(0xFF475569))),

            Brush.linearGradient(listOf(Color(0xFFF1F5F9), Color(0xFF94A3B8), Color(0xFFE2E8F0))),

            Brush.horizontalGradient(listOf(Color(0xFF111827).copy(alpha = 0.95f), Color(0xFF1F2937).copy(alpha = 0.95f)))

        )

        VipButtonTexture.GOLD_GLOW -> Triple(

            Brush.verticalGradient(listOf(Color(0xFF78350F), Color(0xFF451A03), Color(0xFF291002))),

            Brush.linearGradient(listOf(Color(0xFFFDE047), Color(0xFFCA8A04), Color(0xFFFEF08A))),

            Brush.verticalGradient(listOf(Color(0xFF1A1300).copy(alpha = 0.95f), Color(0xFF2C2000).copy(alpha = 0.95f)))

        )

        VipButtonTexture.CYBER_GRID -> Triple(

            Brush.verticalGradient(listOf(Color(0xFF3B0764), Color(0xFF1E0338), Color(0xFF0F011D))),

            Brush.linearGradient(listOf(Color(0xFF00F0FF), Color(0xFFEC4899), Color(0xFFA855F7))),

            Brush.linearGradient(listOf(Color(0xFF0B001A).copy(alpha = 0.95f), Color(0xFF180036).copy(alpha = 0.95f)))

        )

        VipButtonTexture.DEFAULT -> Triple(

            Brush.verticalGradient(listOf(Color(theme.buttonBgColor).copy(alpha = 0.65f), Color(theme.buttonBgColor).copy(alpha = 0.45f))),

            Brush.verticalGradient(listOf(Color(theme.buttonBorderColor).copy(alpha = 0.85f), Color(theme.buttonBorderColor).copy(alpha = 0.50f))),

            Brush.verticalGradient(listOf(Color(theme.backgroundColor).copy(alpha = 0.90f), Color(theme.backgroundColor)))

        )

    }



    val playBtnBrush = Brush.horizontalGradient(

        listOf(Color(theme.primaryColor).copy(alpha = 0.92f), Color(theme.secondaryColor).copy(alpha = 0.88f))

    )



    val playBorderBrush = Brush.horizontalGradient(

        listOf(Color(theme.primaryColor), Color(theme.secondaryColor))

    )
    val isKeypadPlayingActive = state.isPlaying && !state.isPlaybackPaused
    val pauseBtnBrush = Brush.horizontalGradient(
        listOf(Color(0xFFF59E0B), Color(0xFFD97706))
    )
    val pauseBorderBrush = Brush.horizontalGradient(
        listOf(Color(0xFFFDE68A), Color(0xFFF59E0B))
    )



    Column(

        modifier = modifier

            .background(consoleBgBrush)

            .padding(horizontal = 12.dp, vertical = 6.dp),

        verticalArrangement = Arrangement.spacedBy(8.dp)

    ) {



        // Main Area: Numeric 0-9 Keypad Grid + Transport Action Buttons (Single Uniform Color)



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



                                textColor = btnTextColor,



                                bgBrush = uniformBtnBrush,



                                borderBrush = uniformBorderBrush,



                                onClick = { onDigit(digit) },



                                modifier = Modifier.weight(1f)



                            )



                        }



                    }



                }







                // Bottom row: CLR, 0, DEL (Same uniform color as all buttons)



                Row(



                    modifier = Modifier.fillMaxWidth(),



                    horizontalArrangement = Arrangement.spacedBy(6.dp)



                ) {



                    RemoteKey(



                        text = "✕",



                        textColor = btnTextColor,



                        bgBrush = uniformBtnBrush,



                        borderBrush = uniformBorderBrush,



                        onClick = onClear,



                        modifier = Modifier.weight(1f)



                    )



                    RemoteKey(



                        text = "0",



                        textColor = btnTextColor,



                        bgBrush = uniformBtnBrush,



                        borderBrush = uniformBorderBrush,



                        onClick = { onDigit('0') },



                        modifier = Modifier.weight(1f)



                    )



                    RemoteKey(



                        text = "⌫",



                        textColor = btnTextColor,



                        bgBrush = uniformBtnBrush,



                        borderBrush = uniformBorderBrush,



                        onClick = onDelete,



                        modifier = Modifier.weight(1f)



                    )



                }



            }







            // Right Column: Transport Action Buttons (📖 Songbook, ▶ Play, ⏭ Next, ⏹ Stop)



            // Single uniform button color as requested!



            Column(



                modifier = Modifier.weight(1.05f),



                verticalArrangement = Arrangement.spacedBy(6.dp)



            ) {



                ActionButton(



                    text = "",



                    icon = "📖",



                    bgBrush = uniformBtnBrush,



                    borderBrush = uniformBorderBrush,



                    textColor = btnTextColor,



                    onClick = onOpenSongbook,



                    modifier = Modifier.height(50.dp)



                )



                ActionButton(
                    text = "",
                    icon = if (isKeypadPlayingActive) "⏸" else "▶",
                    bgBrush = if (isKeypadPlayingActive) pauseBtnBrush else playBtnBrush,
                    borderBrush = if (isKeypadPlayingActive) pauseBorderBrush else playBorderBrush,
                    textColor = if (isKeypadPlayingActive) Color.White else playTextColor,
                    onClick = onPlay,
                    modifier = Modifier.height(50.dp)
                )



                ActionButton(



                    text = "",



                    icon = "⏭",



                    bgBrush = uniformBtnBrush,



                    borderBrush = uniformBorderBrush,



                    textColor = btnTextColor,



                    onClick = onNext,



                    modifier = Modifier.height(50.dp)



                )



                ActionButton(



                    text = "",



                    icon = "⏹",



                    bgBrush = uniformBtnBrush,



                    borderBrush = uniformBorderBrush,



                    textColor = btnTextColor,



                    onClick = onStop,



                    modifier = Modifier.height(50.dp)



                )



            }



        }







        // Bottom Icon Bar: Clean, compact spacing without large empty gaps

        Row(

            modifier = Modifier

                .fillMaxWidth()

                .clip(RoundedCornerShape(12.dp))

                .background(Color(state.appTheme.backgroundColor))

                .border(1.dp, Color(state.appTheme.buttonBorderColor), RoundedCornerShape(12.dp))

                .padding(4.dp),

            horizontalArrangement = Arrangement.spacedBy(6.dp),

            verticalAlignment = Alignment.CenterVertically

        ) {

            // 1. Favorite Songs Icon

            ConsoleIconButton(

                icon = "❤️",

                isActive = state.isFavoritesOpen,

                theme = theme,

                modifier = Modifier.weight(1f),

                onClick = onToggleFavorites

            )

            // 2. Voice + Music Recording Icon

            ConsoleIconButton(

                icon = if (state.isRecording) "🔴" else "🎙️",

                isActive = state.isRecording || state.isRecordingsDialogOpen,

                theme = theme,

                modifier = Modifier.weight(1f),

                onClick = onOpenRecordingsDialog

            )

            // 3. SoundFont SF2 Selector Icon

            ConsoleIconButton(

                icon = "🎛️",

                isActive = state.isSoundFontDialogOpen,

                theme = theme,

                modifier = Modifier.weight(1f),

                onClick = onOpenSoundFontDialog

            )

            // 4. Lyric Font & Style Icon

            ConsoleIconButton(

                icon = "🔤",

                isActive = state.isLyricStyleDialogOpen,

                theme = theme,

                modifier = Modifier.weight(1f),

                onClick = onOpenLyricStyleDialog

            )

            // 5. Audio & Instrument Mixer Icon

            ConsoleIconButton(

                icon = "🎚️",

                isActive = state.isAudioMixerDialogOpen,

                theme = theme,

                modifier = Modifier.weight(1f),

                onClick = onOpenAudioMixerDialog

            )

            // 6. Backdrop Video/Image Folder Icon

            ConsoleIconButton(

                icon = "🎬",

                isActive = state.isBackgroundDialogOpen,

                theme = theme,

                modifier = Modifier.weight(1f),

                onClick = onOpenBackgroundDialog

            )

        }



    }



}







@Composable



fun ConsoleIconButton(

    icon: String,

    isActive: Boolean = false,

    theme: com.example.videokey.ui.main.AppTheme = com.example.videokey.ui.main.AppTheme.SLATE_DARK,

    customBorderColor: Color? = null,

    customBgColor: Color? = null,

    modifier: Modifier = Modifier,

    onClick: () -> Unit

) {

    val activeBorder = customBorderColor ?: Color(theme.primaryColor)

    val defaultBorder = Color(theme.buttonBorderColor)

    val activeBg = customBorderColor?.copy(alpha = 0.28f) ?: Color(theme.primaryColor).copy(alpha = 0.25f)

    val defaultBg = customBgColor ?: Color(theme.buttonBgColor)



    Box(

        modifier = modifier

            .height(38.dp)

            .clip(RoundedCornerShape(8.dp))

            .background(if (isActive) activeBg else defaultBg)

            .border(

                width = if (isActive) 1.8.dp else 1.dp,

                color = if (isActive) activeBorder else defaultBorder,

                shape = RoundedCornerShape(8.dp)

            )

            .clickable { onClick() },

        contentAlignment = Alignment.Center

    ) {

        Text(

            text = icon,

            fontSize = 17.sp,

            textAlign = TextAlign.Center

        )

    }

}







/**



 * Studio Instrument & Audio Mixer Modal (🎚️)



 * Real-time BASSMIDI SoundFont synthesizer mixer:



 * - Key Transpose (-12 to +12 semitones)



 * - Tempo Speed (50% to 150%)



 * - Individual Instrument Adjusters (0% to 150%):



 *   1. Melody Guide (Gabay na Melody)



 *   2. Drums & Percussion (Tambol / Drums)



 *   3. Bass Guitar (Baho / Bass)



 *   4. Guitar & Piano (Gitara at Piano)



 *   5. Strings, Brass & Synth (Strings / Brass / Sax)



 *   6. Studio Reverb (Echo ng Tugtog)



 */



@Composable



fun AudioMixerDialog(



    state: VideoKeyUiState,



    isLandscape: Boolean = false,



    onToggleAutoMixer: (Boolean) -> Unit = {},



    onToggleAutoVolume: (Boolean) -> Unit = {},



    onToggleAutoMelodyNormalize: (Boolean) -> Unit = {},



    onAdjustKey: (Int) -> Unit,



    onAdjustTempo: (Int) -> Unit,



    onAdjustMelody: (Int) -> Unit,



    onSetMelodyPercent: (Int) -> Unit = {},



    onToggleMelodyMute: () -> Unit = {},



    onAdjustDrums: (Int) -> Unit = {},



    onSetDrumsPercent: (Int) -> Unit = {},



    onAdjustBass: (Int) -> Unit = {},



    onSetBassPercent: (Int) -> Unit = {},



    onAdjustGuitarPiano: (Int) -> Unit = {},



    onSetGuitarPianoPercent: (Int) -> Unit = {},



    onAdjustStringsBrass: (Int) -> Unit = {},



    onSetStringsBrassPercent: (Int) -> Unit = {},



    onAdjustReverb: (Int) -> Unit = {},



    onSetReverbPercent: (Int) -> Unit = {},



    onAdjustBassBoost: (Int) -> Unit = {},



    onSetBassBoostPercent: (Int) -> Unit = {},



    onAdjustTreble: (Int) -> Unit = {},



    onSetTreblePercent: (Int) -> Unit = {},



    onResetMixer: () -> Unit = {},



    onSaveAsSoundFontPreset: (String) -> Unit = {},



    onOpenSoundFontDialog: () -> Unit = {},



    onDismiss: () -> Unit



) {



    val isTagalog = state.appLanguage == com.example.videokey.ui.main.AppLanguage.TAGALOG



    val uniformBtnBg = Color(state.appTheme.buttonBgColor)



    val uniformBtnBorder = Color(state.appTheme.buttonBorderColor)







    Dialog(



        onDismissRequest = onDismiss,



        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)



    ) {



        Surface(



            shape = RoundedCornerShape(16.dp),



            color = Color(state.appTheme.surfaceColor),



            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(state.appTheme.primaryColor)),



            modifier = Modifier



                .fillMaxWidth(if (isLandscape) 0.85f else 0.96f)



                .fillMaxHeight(if (isLandscape) 0.94f else 0.93f)



                .padding(4.dp)



        ) {



            Column(



                modifier = Modifier



                    .fillMaxSize()



                    .padding(horizontal = 12.dp, vertical = 10.dp),



                verticalArrangement = Arrangement.spacedBy(6.dp)



            ) {



                // Header



                Row(



                    modifier = Modifier.fillMaxWidth(),



                    horizontalArrangement = Arrangement.SpaceBetween,



                    verticalAlignment = Alignment.CenterVertically



                ) {



                    Column(modifier = Modifier.weight(1f)) {



                        Text(



                            text = if (isTagalog) "🎚️ TIMPLA NG MGA INSTRUMENTO AT TUNOG" else "🎚️ STUDIO INSTRUMENT & AUDIO MIXER",



                            fontWeight = FontWeight.Black,



                            fontFamily = CleanFont,



                            fontSize = 13.sp,



                            color = Color(state.appTheme.primaryColor),



                            maxLines = 1,



                            overflow = TextOverflow.Ellipsis



                        )



                        Text(



                            text = if (isTagalog)



                                "BASSMIDI 32-Bit Studio SoundFont • Walang Basag Kahit Malakas"



                            else



                                "BASSMIDI 32-Bit Studio SoundFont • Anti-Clip Studio Mastering",



                            fontSize = 9.5.sp,



                            fontFamily = CleanFont,



                            color = Color(state.appTheme.textMutedColor),



                            maxLines = 1,



                            overflow = TextOverflow.Ellipsis



                        )



                    }



                    IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {



                        Text(text = "✕", color = Color(state.appTheme.textColor), fontFamily = CleanFont, fontSize = 15.sp, fontWeight = FontWeight.Bold)



                    }



                }







                                // Active SoundFont Quick Bar + Reset Button

                Row(

                    modifier = Modifier

                        .fillMaxWidth()

                        .clip(RoundedCornerShape(8.dp))

                        .background(Color(state.appTheme.buttonBgColor))

                        .border(1.dp, uniformBtnBorder, RoundedCornerShape(8.dp))

                        .padding(horizontal = 8.dp, vertical = 5.dp),

                    horizontalArrangement = Arrangement.SpaceBetween,

                    verticalAlignment = Alignment.CenterVertically

                ) {

                    Row(

                        modifier = Modifier.weight(1f).padding(end = 6.dp),

                        verticalAlignment = Alignment.CenterVertically,

                        horizontalArrangement = Arrangement.spacedBy(4.dp)

                    ) {

                        Text(

                            text = "🎛️ SF2:",

                            fontSize = 10.sp,

                            fontFamily = CleanFont,

                            color = Color(state.appTheme.primaryColor),

                            fontWeight = FontWeight.Black

                        )

                        Text(

                            text = state.activeSoundFontName,

                            fontSize = 10.sp,

                            fontFamily = CleanFont,

                            color = Color(state.appTheme.textColor),

                            fontWeight = FontWeight.Bold,

                            maxLines = 1,

                            overflow = TextOverflow.Ellipsis

                        )

                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {

                        Surface(

                            shape = RoundedCornerShape(6.dp),

                            color = uniformBtnBg,

                            border = androidx.compose.foundation.BorderStroke(1.dp, uniformBtnBorder),

                            modifier = Modifier

                                .height(26.dp)

                                .clickable { onOpenSoundFontDialog() }

                        ) {

                            Box(modifier = Modifier.padding(horizontal = 7.dp), contentAlignment = Alignment.Center) {

                                Text(

                                    text = if (isTagalog) "Palitan" else "Change",

                                    color = Color(state.appTheme.textColor),

                                    fontFamily = CleanFont,

                                    fontSize = 9.5.sp,

                                    fontWeight = FontWeight.Bold,

                                    softWrap = false,

                                    maxLines = 1

                                )

                            }

                        }

                        var showSavePresetModal by remember { mutableStateOf(false) }

                        var presetNameInput by remember { mutableStateOf("Acoustic Stage Mix") }



                        if (showSavePresetModal) {

                            AlertDialog(

                                onDismissRequest = { showSavePresetModal = false },

                                title = {

                                    Text(

                                        text = if (isTagalog) "👑 I-Save Bilang VIP SoundFont Preset" else "👑 Save as VIP SoundFont Preset",

                                        fontWeight = FontWeight.Bold,

                                        fontSize = 14.sp,

                                        color = Color(0xFFFDE047)

                                    )

                                },

                                text = {

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                                        Text(

                                            text = if (isTagalog) "Ilagay ang pangalan ng SoundFont preset para sa sinet na mga instrument:"

                                                   else "Enter preset name to save your customized instrument mix:",

                                            fontSize = 11.5.sp,

                                            color = Color.White

                                        )

                                        OutlinedTextField(

                                            value = presetNameInput,

                                            onValueChange = { presetNameInput = it },

                                            singleLine = true,

                                            colors = OutlinedTextFieldDefaults.colors(

                                                focusedTextColor = Color.White,

                                                unfocusedTextColor = Color.White,

                                                focusedBorderColor = Color(0xFFEAB308)

                                            )

                                        )

                                    }

                                },

                                confirmButton = {

                                    Button(

                                        onClick = {

                                            onSaveAsSoundFontPreset(presetNameInput)

                                            showSavePresetModal = false

                                        },

                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAB308))

                                    ) {

                                        Text("💾 SAVE", color = Color.Black, fontWeight = FontWeight.Black)

                                    }

                                },

                                dismissButton = {

                                    TextButton(onClick = { showSavePresetModal = false }) {

                                        Text("CANCEL", color = Color.White)

                                    }

                                }

                            )

                        }



                        Surface(

                            shape = RoundedCornerShape(6.dp),

                            color = if (state.isVipActivated) Color(0xFFEAB308).copy(alpha = 0.2f) else uniformBtnBg.copy(alpha = 0.5f),

                            border = androidx.compose.foundation.BorderStroke(1.dp, if (state.isVipActivated) Color(0xFFEAB308) else uniformBtnBorder),

                            modifier = Modifier

                                .height(26.dp)

                                .clickable {

                                    if (state.isVipActivated) {

                                        showSavePresetModal = true

                                    } else {

                                        onSaveAsSoundFontPreset("")

                                    }

                                }

                        ) {

                            Box(modifier = Modifier.padding(horizontal = 7.dp), contentAlignment = Alignment.Center) {

                                Text(

                                    text = if (state.isVipActivated) "👑 Save VIP Preset" else (if (isTagalog) "🔒 Save Preset (VIP)" else "🔒 Save Preset (VIP)"),

                                    color = if (state.isVipActivated) Color(0xFFFDE047) else Color.Gray,

                                    fontFamily = CleanFont,

                                    fontSize = 9.5.sp,

                                    fontWeight = FontWeight.Bold,

                                    softWrap = false,

                                    maxLines = 1

                                )

                            }

                        }



                        Spacer(modifier = Modifier.width(6.dp))



                        Surface(

                            shape = RoundedCornerShape(6.dp),

                            color = uniformBtnBg,

                            border = androidx.compose.foundation.BorderStroke(1.dp, uniformBtnBorder),

                            modifier = Modifier

                                .height(26.dp)

                                .clickable { onResetMixer() }

                        ) {

                            Box(modifier = Modifier.padding(horizontal = 7.dp), contentAlignment = Alignment.Center) {

                                Text(

                                    text = "🔄 Reset",

                                    color = Color(state.appTheme.textColor),

                                    fontFamily = CleanFont,

                                    fontSize = 9.5.sp,

                                    fontWeight = FontWeight.Bold,

                                    softWrap = false,

                                    maxLines = 1

                                )

                            }

                        }

                    }

                }



                // Smart Auto-Mixer Row (VIP Protected)

                Row(

                    modifier = Modifier

                        .fillMaxWidth()

                        .clip(RoundedCornerShape(8.dp))

                        .background(

                            if (!state.isVipActivated) uniformBtnBg.copy(alpha = 0.5f)

                            else if (state.isAutoMixerEnabled) Color(state.appTheme.primaryColor).copy(alpha = 0.14f)

                            else uniformBtnBg

                        )

                        .border(

                            1.dp,

                            if (!state.isVipActivated) Color(0xFFEAB308).copy(alpha = 0.4f)

                            else if (state.isAutoMixerEnabled) Color(state.appTheme.primaryColor)

                            else uniformBtnBorder,

                            RoundedCornerShape(8.dp)

                        )

                        .clickable(enabled = !state.isVipActivated) {

                            onToggleAutoMixer(true)

                        }

                        .padding(horizontal = 8.dp, vertical = 5.dp),

                    horizontalArrangement = Arrangement.SpaceBetween,

                    verticalAlignment = Alignment.CenterVertically

                ) {

                    Column(modifier = Modifier.weight(1f).padding(end = 4.dp)) {

                        Row(

                            verticalAlignment = Alignment.CenterVertically,

                            horizontalArrangement = Arrangement.spacedBy(6.dp)

                        ) {

                            Text(

                                text = if (state.isVipActivated) "🤖 SMART AUTO-MIX" else "🔒 SMART AUTO-MIX (VIP ONLY)",

                                fontSize = 10.sp,

                                fontFamily = CleanFont,

                                color = if (state.isVipActivated) Color(state.appTheme.primaryColor) else Color(0xFFFDE047),

                                fontWeight = FontWeight.Black,

                                modifier = Modifier.weight(1f, fill = false),

                                maxLines = 1,

                                overflow = TextOverflow.Ellipsis

                            )

                            Surface(

                                shape = RoundedCornerShape(4.dp),

                                color = if (!state.isVipActivated) Color(0xFFEAB308).copy(alpha = 0.2f)

                                       else if (state.isAutoMixerEnabled) Color(state.appTheme.primaryColor).copy(alpha = 0.22f) 

                                       else Color(state.appTheme.buttonBorderColor)

                            ) {

                                Text(

                                    text = if (!state.isVipActivated) {

                                        "👑 VIP ONLY"

                                    } else if (state.isAutoMixerEnabled) {

                                        "${state.activeMixStyle.emoji} " + (if (isTagalog) state.activeMixStyle.labelTagalog else state.activeMixStyle.labelEnglish)

                                    } else {

                                        "MANUAL (OFF)"

                                    },

                                    fontSize = 8.5.sp,

                                    fontFamily = CleanFont,

                                    color = if (!state.isVipActivated) Color(0xFFFDE047)

                                           else if (state.isAutoMixerEnabled) Color(state.appTheme.primaryColor) 

                                           else Color(state.appTheme.textColor),

                                    fontWeight = FontWeight.Black,

                                    softWrap = false,

                                    maxLines = 1,

                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)

                                )

                            }

                        }

                        Text(

                            text = if (!state.isVipActivated) {

                                if (isTagalog) "⭐ Eksklusibo sa VIP ang Smart Auto-Mix! Mag-activate upang magamit."

                                else "⭐ VIP exclusive feature! Activate VIP to enable Smart Auto-Mix."

                            } else if (state.isAutoMixerEnabled) {

                                if (isTagalog) state.activeMixStyle.descriptionTagalog else state.activeMixStyle.descriptionEnglish

                            } else {

                                if (isTagalog) "Naka-manual mix. Kayo ang may buong kontrol sa mga sliders sa ibaba." else "Manual mode. You have full custom control of all sliders below."

                            },

                            fontSize = 8.sp,

                            fontFamily = CleanFont,

                            color = Color(state.appTheme.textMutedColor),

                            maxLines = 1,

                            overflow = TextOverflow.Ellipsis

                        )

                    }



                    Switch(

                        checked = state.isVipActivated && state.isAutoMixerEnabled,

                        onCheckedChange = { 

                            if (state.isVipActivated) {

                                onToggleAutoMixer(it)

                            } else {

                                onToggleAutoMixer(true)

                            }

                        },

                        enabled = state.isVipActivated,

                        colors = SwitchDefaults.colors(

                            checkedThumbColor = Color(state.appTheme.primaryColor),

                            checkedTrackColor = Color(state.appTheme.primaryColor).copy(alpha = 0.4f),

                            uncheckedThumbColor = Color.Gray,

                            uncheckedTrackColor = Color(0xFF1E293B),

                            disabledCheckedThumbColor = Color.Gray,

                            disabledUncheckedThumbColor = Color(0xFF475569),

                            disabledUncheckedTrackColor = Color(0xFF1E293B)

                        ),

                        modifier = Modifier.scale(0.75f)

                    )

                }



                // Auto-Volume Normalization Row

                Row(

                    modifier = Modifier

                        .fillMaxWidth()

                        .clip(RoundedCornerShape(8.dp))

                        .background(

                            if (state.isAutoVolumeEnabled) Color(state.appTheme.primaryColor).copy(alpha = 0.14f)

                            else uniformBtnBg

                        )

                        .border(

                            1.dp,

                            if (state.isAutoVolumeEnabled) Color(state.appTheme.primaryColor)

                            else uniformBtnBorder,

                            RoundedCornerShape(8.dp)

                        )

                        .padding(horizontal = 8.dp, vertical = 5.dp),

                    horizontalArrangement = Arrangement.SpaceBetween,

                    verticalAlignment = Alignment.CenterVertically

                ) {

                    Column(modifier = Modifier.weight(1f).padding(end = 4.dp)) {

                        Row(

                            verticalAlignment = Alignment.CenterVertically,

                            horizontalArrangement = Arrangement.spacedBy(6.dp)

                        ) {

                            Text(

                                text = "🔊 AUTO-VOLUME (AGC)",

                                fontSize = 10.sp,

                                fontFamily = CleanFont,

                                color = Color(state.appTheme.primaryColor),

                                fontWeight = FontWeight.Black,

                                modifier = Modifier.weight(1f, fill = false),

                                maxLines = 1,

                                overflow = TextOverflow.Ellipsis

                            )

                            Surface(

                                shape = RoundedCornerShape(4.dp),

                                color = if (state.isAutoVolumeEnabled) Color(0xFF10B981).copy(alpha = 0.22f) else Color(state.appTheme.buttonBorderColor)

                            ) {

                                Text(

                                    text = if (state.isAutoVolumeEnabled) {

                                        if (isTagalog) "✓ PANTAY" else "✓ BALANCED"

                                    } else {

                                        "OFF"

                                    },

                                    fontSize = 8.5.sp,

                                    fontFamily = CleanFont,

                                    color = if (state.isAutoVolumeEnabled) Color(0xFF10B981) else Color(state.appTheme.textColor),

                                    fontWeight = FontWeight.Black,

                                    softWrap = false,

                                    maxLines = 1,

                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)

                                )

                            }

                        }

                        Text(

                            text = if (state.isAutoVolumeEnabled) {

                                if (isTagalog) "Awtomatikong pinapantay ang bawat kanta para komportable ang tunog."

                                else "Automatically balances volume across songs for consistent playback."

                            } else {

                                if (isTagalog) "Naka-off. Ang lakas ay batay sa orihinal na bulto ng MIDI file."

                                else "Off. Song loudness depends on the original MIDI file mix."

                            },

                            fontSize = 8.sp,

                            fontFamily = CleanFont,

                            color = Color(state.appTheme.textMutedColor),

                            maxLines = 1,

                            overflow = TextOverflow.Ellipsis

                        )

                    }



                    Switch(

                        checked = state.isAutoVolumeEnabled,

                        onCheckedChange = { onToggleAutoVolume(it) },

                        colors = SwitchDefaults.colors(

                            checkedThumbColor = Color(state.appTheme.primaryColor),

                            checkedTrackColor = Color(state.appTheme.primaryColor).copy(alpha = 0.4f),

                            uncheckedThumbColor = Color.Gray,

                            uncheckedTrackColor = Color(0xFF1E293B)

                        ),

                        modifier = Modifier.scale(0.75f)

                    )

                }



                // Auto-Normalize Melody Voice Row (Guide Melody AGC)

                Row(

                    modifier = Modifier

                        .fillMaxWidth()

                        .clip(RoundedCornerShape(8.dp))

                        .background(

                            if (state.isAutoMelodyNormalizeEnabled) Color(state.appTheme.primaryColor).copy(alpha = 0.14f)

                            else uniformBtnBg

                        )

                        .border(

                            1.dp,

                            if (state.isAutoMelodyNormalizeEnabled) Color(state.appTheme.primaryColor)

                            else uniformBtnBorder,

                            RoundedCornerShape(8.dp)

                        )

                        .padding(horizontal = 8.dp, vertical = 5.dp),

                    horizontalArrangement = Arrangement.SpaceBetween,

                    verticalAlignment = Alignment.CenterVertically

                ) {

                    Column(modifier = Modifier.weight(1f).padding(end = 4.dp)) {

                        Row(

                            verticalAlignment = Alignment.CenterVertically,

                            horizontalArrangement = Arrangement.spacedBy(6.dp)

                        ) {

                            Text(

                                text = "🎤 AUTO-MELODY GUIDE",

                                fontSize = 10.sp,

                                fontFamily = CleanFont,

                                color = Color(state.appTheme.primaryColor),

                                fontWeight = FontWeight.Black,

                                modifier = Modifier.weight(1f, fill = false),

                                maxLines = 1,

                                overflow = TextOverflow.Ellipsis

                            )

                            Surface(

                                shape = RoundedCornerShape(4.dp),

                                color = if (state.isAutoMelodyNormalizeEnabled) Color(0xFF10B981).copy(alpha = 0.22f) else Color(state.appTheme.buttonBorderColor)

                            ) {

                                Text(

                                    text = if (state.isAutoMelodyNormalizeEnabled) {

                                        if (isTagalog) "✓ BALANSE" else "✓ BALANCED"

                                    } else {

                                        "OFF"

                                    },

                                    fontSize = 8.5.sp,

                                    fontFamily = CleanFont,

                                    color = if (state.isAutoMelodyNormalizeEnabled) Color(0xFF10B981) else Color(state.appTheme.textColor),

                                    fontWeight = FontWeight.Black,

                                    softWrap = false,

                                    maxLines = 1,

                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)

                                )

                            }

                        }

                        Text(

                            text = if (state.isAutoMelodyNormalizeEnabled) {

                                if (isTagalog) "Binabalanse ang guide melody ng boses upang hindi sobrang lakas o mahina."

                                else "Normalizes guide melody voice to prevent harsh spikes or quiet notes."

                            } else {

                                if (isTagalog) "Naka-off. Ang lakas ng melody ay batay sa orihinal na bilis at lakas ng MIDI."

                                else "Off. Melody volume follows raw MIDI note velocities and mix."

                            },

                            fontSize = 8.sp,

                            fontFamily = CleanFont,

                            color = Color(state.appTheme.textMutedColor),

                            maxLines = 1,

                            overflow = TextOverflow.Ellipsis

                        )

                    }



                    Switch(

                        checked = state.isAutoMelodyNormalizeEnabled,

                        onCheckedChange = { onToggleAutoMelodyNormalize(it) },

                        colors = SwitchDefaults.colors(

                            checkedThumbColor = Color(state.appTheme.primaryColor),

                            checkedTrackColor = Color(state.appTheme.primaryColor).copy(alpha = 0.4f),

                            uncheckedThumbColor = Color.Gray,

                            uncheckedTrackColor = Color(0xFF1E293B)

                        ),

                        modifier = Modifier.scale(0.75f)

                    )

                }



// Key Transpose & Tempo Compact Row



                Row(



                    modifier = Modifier.fillMaxWidth(),



                    horizontalArrangement = Arrangement.spacedBy(6.dp)



                ) {



                    // 1. Key Transpose Card



                    Row(



                        modifier = Modifier



                            .weight(1f)



                            .clip(RoundedCornerShape(8.dp))



                            .background(Color(state.appTheme.buttonBgColor))



                            .border(1.dp, uniformBtnBorder, RoundedCornerShape(8.dp))



                            .padding(horizontal = 8.dp, vertical = 6.dp),



                        horizontalArrangement = Arrangement.SpaceBetween,



                        verticalAlignment = Alignment.CenterVertically



                    ) {



                        Text(



                            text = "🎹 KEY",



                            fontWeight = FontWeight.Black,



                            fontFamily = CleanFont,



                            fontSize = 10.5.sp,



                            color = Color(state.appTheme.textColor)



                        )



                        Row(



                            horizontalArrangement = Arrangement.spacedBy(4.dp),



                            verticalAlignment = Alignment.CenterVertically



                        ) {



                            Box(



                                modifier = Modifier



                                    .size(26.dp)



                                    .clip(RoundedCornerShape(6.dp))



                                    .background(uniformBtnBg)



                                    .border(1.dp, uniformBtnBorder, RoundedCornerShape(6.dp))



                                    .clickable { onAdjustKey(-1) },



                                contentAlignment = Alignment.Center



                            ) {



                                Text(text = "−", color = Color(state.appTheme.textColor), fontFamily = CleanFont, fontWeight = FontWeight.Black, fontSize = 15.sp)



                            }



                            Text(



                                text = if (state.keyTranspose > 0) "+${state.keyTranspose}" else "${state.keyTranspose}",



                                color = Color(state.appTheme.secondaryColor),



                                fontFamily = CleanFont,



                                fontWeight = FontWeight.Black,



                                fontSize = 12.sp,



                                textAlign = TextAlign.Center,



                                modifier = Modifier.width(26.dp)



                            )



                            Box(



                                modifier = Modifier



                                    .size(26.dp)



                                    .clip(RoundedCornerShape(6.dp))



                                    .background(uniformBtnBg)



                                    .border(1.dp, uniformBtnBorder, RoundedCornerShape(6.dp))



                                    .clickable { onAdjustKey(1) },



                                contentAlignment = Alignment.Center



                            ) {



                                Text(text = "+", color = Color(state.appTheme.textColor), fontFamily = CleanFont, fontWeight = FontWeight.Black, fontSize = 15.sp)



                            }



                        }



                    }







                    // 2. Tempo Speed Card



                    Row(



                        modifier = Modifier



                            .weight(1f)



                            .clip(RoundedCornerShape(8.dp))



                            .background(Color(state.appTheme.buttonBgColor))



                            .border(1.dp, uniformBtnBorder, RoundedCornerShape(8.dp))



                            .padding(horizontal = 8.dp, vertical = 6.dp),



                        horizontalArrangement = Arrangement.SpaceBetween,



                        verticalAlignment = Alignment.CenterVertically



                    ) {



                        Text(



                            text = "⏱️ BPM",



                            fontWeight = FontWeight.Black,



                            fontFamily = CleanFont,



                            fontSize = 10.5.sp,



                            color = Color(state.appTheme.textColor)



                        )



                        Row(



                            horizontalArrangement = Arrangement.spacedBy(4.dp),



                            verticalAlignment = Alignment.CenterVertically



                        ) {



                            Box(



                                modifier = Modifier



                                    .size(26.dp)



                                    .clip(RoundedCornerShape(6.dp))



                                    .background(uniformBtnBg)



                                    .border(1.dp, uniformBtnBorder, RoundedCornerShape(6.dp))



                                    .clickable { onAdjustTempo(-5) },



                                contentAlignment = Alignment.Center



                            ) {



                                Text(text = "−", color = Color(state.appTheme.textColor), fontFamily = CleanFont, fontWeight = FontWeight.Black, fontSize = 15.sp)



                            }



                            Text(



                                text = "${state.tempoPercent}%",



                                color = Color(state.appTheme.primaryColor),



                                fontFamily = CleanFont,



                                fontWeight = FontWeight.Black,



                                fontSize = 11.5.sp,



                                textAlign = TextAlign.Center,



                                modifier = Modifier.width(36.dp)



                            )



                            Box(



                                modifier = Modifier



                                    .size(26.dp)



                                    .clip(RoundedCornerShape(6.dp))



                                    .background(uniformBtnBg)



                                    .border(1.dp, uniformBtnBorder, RoundedCornerShape(6.dp))



                                    .clickable { onAdjustTempo(5) },



                                contentAlignment = Alignment.Center



                            ) {



                                Text(text = "+", color = Color(state.appTheme.textColor), fontFamily = CleanFont, fontWeight = FontWeight.Black, fontSize = 15.sp)



                            }



                        }



                    }



                }







                // Scrollable list of 6 Individual Instrument Adjusters



                val mixerListState = androidx.compose.foundation.lazy.rememberLazyListState()



                LazyColumn(



                    state = mixerListState,



                    modifier = Modifier



                        .weight(1f)



                        .fillMaxWidth(),



                    verticalArrangement = Arrangement.spacedBy(5.dp)



                ) {



                    // 3. Drums & Percussion (Tambol)



                    item {



                        InstrumentSliderRow(



                            title = if (isTagalog) "🥁 Drums at Percussion (Tambol)" else "🥁 Drums & Percussion",



                            percent = state.drumsPercent,



                            maxPercent = 150,



                            accentColor = Color(0xFF38BDF8),



                            btnBg = uniformBtnBg,



                            btnBorder = uniformBtnBorder,



                            onStep = onAdjustDrums,



                            onSlide = onSetDrumsPercent



                        )



                    }







                    // 4. Bass Guitar (Baho)



                    item {



                        InstrumentSliderRow(



                            title = if (isTagalog) "🎸 Bass Guitar (Baho)" else "🎸 Bass Guitar",



                            percent = state.bassPercent,



                            maxPercent = 150,



                            accentColor = Color(0xFF00E676),



                            btnBg = uniformBtnBg,



                            btnBorder = uniformBtnBorder,



                            onStep = onAdjustBass,



                            onSlide = onSetBassPercent



                        )



                    }







                    // 5. Guitar & Piano



                    item {



                        InstrumentSliderRow(



                            title = if (isTagalog) "🎹 Gitara, Piano at Organ" else "🎹 Guitar, Piano & Keys",



                            percent = state.guitarPianoPercent,



                            maxPercent = 150,



                            accentColor = Color(0xFFFFD54F),



                            btnBg = uniformBtnBg,



                            btnBorder = uniformBtnBorder,



                            onStep = onAdjustGuitarPiano,



                            onSlide = onSetGuitarPianoPercent



                        )



                    }







                    // 6. Strings, Brass & Sax



                    item {



                        InstrumentSliderRow(



                            title = if (isTagalog) "🎷 Strings, Brass at Saxophone" else "🎷 Strings, Brass & Winds",



                            percent = state.stringsBrassPercent,



                            maxPercent = 150,



                            accentColor = Color(0xFFF472B6),



                            btnBg = uniformBtnBg,



                            btnBorder = uniformBtnBorder,



                            onStep = onAdjustStringsBrass,



                            onSlide = onSetStringsBrassPercent



                        )



                    }







                    // 7. Melody Guide (Gabay sa Kanta) with dedicated ON/OFF Button

                    item {

                        Column(

                            modifier = Modifier

                                .fillMaxWidth()

                                .clip(RoundedCornerShape(8.dp))

                                .background(uniformBtnBg)

                                .border(1.dp, uniformBtnBorder, RoundedCornerShape(8.dp))

                                .padding(horizontal = 8.dp, vertical = 5.dp),

                            verticalArrangement = Arrangement.spacedBy(2.dp)

                        ) {

                            Row(

                                modifier = Modifier.fillMaxWidth(),

                                horizontalArrangement = Arrangement.SpaceBetween,

                                verticalAlignment = Alignment.CenterVertically

                            ) {

                                Row(

                                    verticalAlignment = Alignment.CenterVertically,

                                    horizontalArrangement = Arrangement.spacedBy(6.dp),

                                    modifier = Modifier.weight(1f).padding(end = 4.dp)

                                ) {

                                    Text(

                                        text = if (isTagalog) "🎵 Gabay na Melody (Vocal Guide)" else "🎵 Vocal Melody Guide",

                                        fontWeight = FontWeight.Bold,

                                        fontFamily = CleanFont,

                                        fontSize = 11.sp,

                                        color = Color.White,

                                        maxLines = 1,

                                        overflow = TextOverflow.Ellipsis

                                    )

                                    Surface(

                                        shape = RoundedCornerShape(4.dp),

                                        color = if (state.isMelodyMuted) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFF00E5FF).copy(alpha = 0.2f),

                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (state.isMelodyMuted) Color(0xFFEF4444) else Color(0xFF00E5FF)),

                                        modifier = Modifier.clickable { onToggleMelodyMute() }

                                    ) {

                                        Text(

                                            text = if (state.isMelodyMuted) "🔇 OFF" else "✓ ON",

                                            fontSize = 8.5.sp,

                                            fontFamily = CleanFont,

                                            fontWeight = FontWeight.Black,

                                            color = if (state.isMelodyMuted) Color(0xFFEF4444) else Color(0xFF00E5FF),

                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)

                                        )

                                    }

                                }



                                Row(

                                    horizontalArrangement = Arrangement.spacedBy(4.dp),

                                    verticalAlignment = Alignment.CenterVertically

                                ) {

                                    Box(

                                        modifier = Modifier

                                            .size(24.dp)

                                            .clip(RoundedCornerShape(5.dp))

                                            .background(uniformBtnBg)

                                            .border(1.dp, uniformBtnBorder, RoundedCornerShape(5.dp))

                                            .clickable { onAdjustMelody(-10) },

                                        contentAlignment = Alignment.Center

                                    ) {

                                        Text(text = "−", color = Color.White, fontFamily = CleanFont, fontWeight = FontWeight.Black, fontSize = 14.sp)

                                    }



                                    Text(

                                        text = if (state.isMelodyMuted) "OFF" else "${state.melodyPercent}%",

                                        color = if (state.isMelodyMuted) Color(0xFFEF4444) else Color(0xFF00E5FF),

                                        fontFamily = CleanFont,

                                        fontWeight = FontWeight.Black,

                                        fontSize = 11.5.sp,

                                        textAlign = TextAlign.Center,

                                        modifier = Modifier.width(38.dp)

                                    )



                                    Box(

                                        modifier = Modifier

                                            .size(24.dp)

                                            .clip(RoundedCornerShape(5.dp))

                                            .background(uniformBtnBg)

                                            .border(1.dp, uniformBtnBorder, RoundedCornerShape(5.dp))

                                            .clickable { onAdjustMelody(10) },

                                        contentAlignment = Alignment.Center

                                    ) {

                                        Text(text = "+", color = Color.White, fontFamily = CleanFont, fontWeight = FontWeight.Black, fontSize = 14.sp)

                                    }

                                }

                            }



                            Slider(

                                value = (if (state.isMelodyMuted) 0 else state.melodyPercent).toFloat(),

                                onValueChange = {

                                    if (state.isMelodyMuted) onToggleMelodyMute()

                                    onSetMelodyPercent(it.toInt())

                                },

                                valueRange = 0f..100f,

                                colors = SliderDefaults.colors(

                                    thumbColor = if (state.isMelodyMuted) Color.Gray else Color(0xFF00E5FF),

                                    activeTrackColor = if (state.isMelodyMuted) Color.DarkGray else Color(0xFF00E5FF),

                                    inactiveTrackColor = Color(0xFF0F172A)

                                ),

                                modifier = Modifier

                                    .fillMaxWidth()

                                    .height(18.dp)

                            )

                        }

                    }







                    // 8. Studio Reverb & Ambience



                    item {



                        InstrumentSliderRow(



                            title = if (isTagalog) "🏛️ Studio Reverb (Echo ng Tugtog)" else "🏛️ Studio Reverb & Warmth",



                            percent = state.reverbPercent,



                            maxPercent = 150,



                            accentColor = Color(0xFFA78BFA),



                            btnBg = uniformBtnBg,



                            btnBorder = uniformBtnBorder,



                            onStep = onAdjustReverb,



                            onSlide = onSetReverbPercent



                        )



                    }



                    // 9. Bass Boost (VIP Exclusive)

                    item {

                        InstrumentSliderRow(

                            title = if (isTagalog) "🔊 Bass Boost (Dagdag-Baho / Sub-Bass)" else "🔊 Bass Boost (Sub-Bass)",

                            percent = state.bassBoostPercent,

                            maxPercent = 100,

                            accentColor = Color(0xFFFDE047),

                            btnBg = uniformBtnBg,

                            btnBorder = uniformBtnBorder,

                            onStep = onAdjustBassBoost,

                            onSlide = onSetBassBoostPercent,

                            isVipOnly = true,

                            isVipActivated = state.isVipActivated,

                            onLockedClick = { onAdjustBassBoost(0) }

                        )

                    }



                    // 10. Treble Boost (VIP Exclusive)

                    item {

                        InstrumentSliderRow(

                            title = if (isTagalog) "🎚️ Treble Boost (Kalansing / Hi-Crisp)" else "🎚️ Treble Boost (High Crisp)",

                            percent = state.treblePercent,

                            maxPercent = 100,

                            accentColor = Color(0xFF38BDF8),

                            btnBg = uniformBtnBg,

                            btnBorder = uniformBtnBorder,

                            onStep = onAdjustTreble,

                            onSlide = onSetTreblePercent,

                            isVipOnly = true,

                            isVipActivated = state.isVipActivated,

                            onLockedClick = { onAdjustTreble(0) }

                        )

                    }



                }







                // Close Button



                Button(



                    onClick = onDismiss,



                    colors = ButtonDefaults.buttonColors(containerColor = uniformBtnBg),



                    border = androidx.compose.foundation.BorderStroke(1.dp, uniformBtnBorder),



                    shape = RoundedCornerShape(10.dp),



                    modifier = Modifier



                        .fillMaxWidth()



                        .height(38.dp)



                ) {



                    Text(



                        text = if (isTagalog) "TAPOS NA / ISARA ✓" else "DONE / CLOSE ✓",



                        color = Color.White,



                        fontFamily = CleanFont,



                        fontWeight = FontWeight.Bold,



                        fontSize = 12.sp



                    )



                }



            }



        }



    }



}







@Composable



private fun InstrumentSliderRow(



    title: String,



    percent: Int,



    maxPercent: Int,



    accentColor: Color,



    btnBg: Color,



    btnBorder: Color,



    onStep: (Int) -> Unit,



    onSlide: (Int) -> Unit,



    isVipOnly: Boolean = false,



    isVipActivated: Boolean = true,



    onLockedClick: () -> Unit = {}



) {



    val isLocked = isVipOnly && !isVipActivated



    Column(



        modifier = Modifier



            .fillMaxWidth()



            .clip(RoundedCornerShape(8.dp))



            .background(if (isLocked) btnBg.copy(alpha = 0.5f) else btnBg)



            .border(1.dp, if (isLocked) Color(0xFFEAB308).copy(alpha = 0.4f) else btnBorder, RoundedCornerShape(8.dp))



            .padding(horizontal = 8.dp, vertical = 5.dp),



        verticalArrangement = Arrangement.spacedBy(2.dp)



    ) {



        Row(



            modifier = Modifier.fillMaxWidth(),



            horizontalArrangement = Arrangement.SpaceBetween,



            verticalAlignment = Alignment.CenterVertically



        ) {



            Row(

                modifier = Modifier.weight(1f).padding(end = 4.dp),

                verticalAlignment = Alignment.CenterVertically,

                horizontalArrangement = Arrangement.spacedBy(4.dp)

            ) {

                Text(

                    text = title,

                    fontWeight = FontWeight.Bold,

                    fontFamily = CleanFont,

                    fontSize = 11.sp,

                    color = if (isLocked) Color(0xFF94A3B8) else Color.White,

                    maxLines = 1,

                    overflow = TextOverflow.Ellipsis

                )



            }



            Row(



                horizontalArrangement = Arrangement.spacedBy(4.dp),



                verticalAlignment = Alignment.CenterVertically



            ) {



                Box(



                    modifier = Modifier



                        .size(24.dp)



                        .clip(RoundedCornerShape(5.dp))



                        .background(btnBg)



                        .border(1.dp, btnBorder, RoundedCornerShape(5.dp))



                        .alpha(if (isLocked) 0.35f else 1.0f)

                        .clickable(enabled = !isLocked) { onStep(-10) },



                    contentAlignment = Alignment.Center



                ) {



                    Text(text = "−", color = if (isLocked) Color.Gray else Color.White, fontFamily = CleanFont, fontWeight = FontWeight.Black, fontSize = 14.sp)



                }



                Text(



                    text = if (isLocked) "🔒 VIP" else if (percent == 0) "OFF" else "$percent%",



                    color = if (isLocked) Color(0xFFFDE047) else if (percent == 0) Color(0xFFFF5252) else accentColor,



                    fontFamily = CleanFont,



                    fontWeight = FontWeight.Black,



                    fontSize = 11.sp,



                    textAlign = TextAlign.Center,



                    modifier = Modifier.width(42.dp)



                )



                Box(



                    modifier = Modifier



                        .size(24.dp)



                        .clip(RoundedCornerShape(5.dp))



                        .background(btnBg)



                        .border(1.dp, btnBorder, RoundedCornerShape(5.dp))



                        .alpha(if (isLocked) 0.35f else 1.0f)

                        .clickable(enabled = !isLocked) { onStep(10) },



                    contentAlignment = Alignment.Center



                ) {



                    Text(text = "+", color = if (isLocked) Color.Gray else Color.White, fontFamily = CleanFont, fontWeight = FontWeight.Black, fontSize = 14.sp)



                }



            }



        }



        Slider(



            value = (if (isLocked) 0 else percent).toFloat(),



            onValueChange = { if (!isLocked) onSlide(it.toInt()) },



            valueRange = 0f..maxPercent.toFloat(),



            enabled = !isLocked,



            colors = SliderDefaults.colors(



                thumbColor = if (isLocked) Color(0xFF64748B) else accentColor,



                activeTrackColor = if (isLocked) Color(0xFF475569) else accentColor,



                inactiveTrackColor = Color(0xFF0F172A),



                disabledThumbColor = Color(0xFF64748B),



                disabledActiveTrackColor = Color(0xFF475569),



                disabledInactiveTrackColor = Color(0xFF1E293B)



            ),



            modifier = Modifier



                .fillMaxWidth()



                .height(18.dp)



                .alpha(if (isLocked) 0.35f else 1.0f)



        )



    }



}







fun resolveLyricFontParams(



    family: LyricFontFamily



): Triple<FontFamily, FontWeight, androidx.compose.ui.unit.TextUnit> {



    return when (family) {



        LyricFontFamily.RADIO_CANADA -> Triple(



            FontFamily(Font(R.font.radio_canada, FontWeight.Bold)),



            FontWeight.Bold,



            0.2.sp



        )



        LyricFontFamily.SAN_SERIF -> Triple(



            FontFamily.SansSerif,



            FontWeight.Bold,



            0.sp



        )



        LyricFontFamily.IMPACT_HEAVY -> Triple(



            FontFamily(android.graphics.Typeface.create("sans-serif-black", android.graphics.Typeface.BOLD)),



            FontWeight.Black,



            0.6.sp



        )



        LyricFontFamily.CONDENSED_TALL -> Triple(



            FontFamily(android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD)),



            FontWeight.Bold,



            0.3.sp



        )



        LyricFontFamily.GEOMETRIC_POP -> Triple(



            FontFamily(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)),



            FontWeight.ExtraBold,



            0.8.sp



        )



        LyricFontFamily.ROUNDED_CLEAN -> Triple(



            FontFamily(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)),



            FontWeight.ExtraBold,



            1.2.sp



        )



        LyricFontFamily.SMALL_CAPS -> Triple(



            FontFamily(android.graphics.Typeface.create("sans-serif-smallcaps", android.graphics.Typeface.BOLD)),



            FontWeight.Bold,



            0.5.sp



        )



        LyricFontFamily.SERIF -> Triple(



            FontFamily.Serif,



            FontWeight.Bold,



            0.sp



        )



        LyricFontFamily.SERIF_MONOSPACE -> Triple(



            FontFamily(android.graphics.Typeface.create("serif-monospace", android.graphics.Typeface.BOLD)),



            FontWeight.Bold,



            0.sp



        )



        LyricFontFamily.MONOSPACE -> Triple(



            FontFamily.Monospace,



            FontWeight.Bold,



            0.sp



        )



        LyricFontFamily.CASUAL_FUN -> Triple(



            FontFamily(android.graphics.Typeface.create("casual", android.graphics.Typeface.BOLD)),



            FontWeight.Bold,



            0.sp



        )



        LyricFontFamily.CURSIVE -> Triple(



            FontFamily.Cursive,



            FontWeight.Bold,



            0.4.sp



        )



        LyricFontFamily.LIGHT_MODERN -> Triple(



            FontFamily(android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL)),



            FontWeight.Light,



            0.5.sp



        )



    }



}







fun toKaraokeNormalCase(text: String): String {



    if (text.isBlank()) return text



    return text.split(" ").joinToString(" ") { word ->



        if (word.isEmpty()) ""



        else {



            val firstLetterIdx = word.indexOfFirst { it.isLetter() }



            if (firstLetterIdx == -1) word



            else {



                val prefix = word.substring(0, firstLetterIdx)



                val firstChar = word[firstLetterIdx].uppercaseChar()



                val rest = word.substring(firstLetterIdx + 1).lowercase()



                prefix + firstChar + rest



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
    val cleanText = text.trimEnd('-', ' ')
    val displayText = if (lyricConfig.isAllCaps) cleanText.uppercase() else toKaraokeNormalCase(cleanText)
    val activeColor = Color(lyricConfig.wipeColor.activeColor)
    val inactiveBaseColor = Color(lyricConfig.inactiveColor)

    // Character-level geometry tracking: maps character bounding boxes so each letter is hit one-by-one!
    var textLayoutResult by remember(displayText) { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
    val density = androidx.compose.ui.platform.LocalDensity.current

    val badgeXOffsetDp = remember(textLayoutResult, displayText, lyricConfig.countdownMode, isLandscape) {
        val layout = textLayoutResult
        if (layout != null && displayText.isNotBlank()) {
            val firstCharIdx = displayText.indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0)
            if (firstCharIdx < layout.layoutInput.text.length) {
                try {
                    val box = layout.getBoundingBox(firstCharIdx)
                    val startPadPx = with(density) { 4.dp.toPx() }
                    val letterCenterPx = startPadPx + box.left + (box.width / 2f)
                    when (lyricConfig.countdownMode) {
                        LyricCountdownMode.CLASSIC_DOTS -> {
                            val dotSizeDp = if (isLandscape) 13.dp else 10.5.dp
                            val firstDotCenterPx = with(density) { (6.dp + (dotSizeDp / 2f)).toPx() }
                            val targetXPx = (letterCenterPx - firstDotCenterPx).coerceAtLeast(0f)
                            with(density) { targetXPx.toDp() }
                        }
                        LyricCountdownMode.ARCADE_NUMBER -> {
                            val badgeSizeDp = if (isLandscape) 26.dp else 22.dp
                            val halfBadgePx = with(density) { (badgeSizeDp / 2f).toPx() }
                            val targetXPx = (letterCenterPx - halfBadgePx).coerceAtLeast(0f)
                            with(density) { targetXPx.toDp() }
                        }
                        LyricCountdownMode.PHOTO_BADGE -> {
                            val badgeSizeDp = if (isLandscape) 52.dp else 44.dp
                            val halfBadgePx = with(density) { (badgeSizeDp / 2f).toPx() }
                            val targetXPx = (letterCenterPx - halfBadgePx).coerceAtLeast(0f)
                            with(density) { targetXPx.toDp() }
                        }
                        else -> 0.dp
                    }
                } catch (e: Exception) {
                    0.dp
                }
            } else 0.dp
        } else 0.dp
    }

    val (composeFontFamily, composeFontWeight, composeLetterSpacing) = remember(lyricConfig.fontFamily) {
        resolveLyricFontParams(lyricConfig.fontFamily)
    }

    val shadow = when (lyricConfig.shadowStyle) {
        LyricShadowStyle.HEAVY_OUTLINE -> Shadow.None
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

    val isWipeOn = lyricConfig.isWipeHighlightEnabled
    val unhighlightedOutlineColor = Color(lyricConfig.outlineColor.colorLong)
    val useOutline = lyricConfig.outlineColor != LyricOutlineColor.NONE

    val scaledFontSize = (fontSize.value * lyricConfig.fontScale).sp
    val outlineWidthPx = with(density) { lyricConfig.outlineThickness.dp.toPx() }

    val sharedLyricStyle = TextStyle(
        fontSize = scaledFontSize,
        fontWeight = composeFontWeight,
        fontStyle = if (lyricConfig.isItalic) FontStyle.Italic else FontStyle.Normal,
        fontFamily = composeFontFamily,
        textAlign = TextAlign.Center,
        letterSpacing = composeLetterSpacing
    )

    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
        val availableWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) { (maxWidth - 10.dp).toPx() }

        val measuredWidthPx = remember(displayText, sharedLyricStyle, availableWidthPx) {
            try {
                textMeasurer.measure(
                    text = androidx.compose.ui.text.AnnotatedString(displayText),
                    style = sharedLyricStyle,
                    maxLines = 1,
                    softWrap = false
                ).size.width.toFloat()
            } catch (_: Exception) {
                0f
            }
        }

        val autoFitRatio = remember(measuredWidthPx, availableWidthPx) {
            if (measuredWidthPx > availableWidthPx && availableWidthPx > 0f) {
                (availableWidthPx / measuredWidthPx).coerceIn(0.45f, 1.0f)
            } else {
                1.0f
            }
        }

        val activeFontSize = if (autoFitRatio < 0.99f) (scaledFontSize.value * autoFitRatio).sp else scaledFontSize
        val activeOutlinePx = if (autoFitRatio < 0.99f) outlineWidthPx * autoFitRatio else outlineWidthPx

        val activeSharedStyle = if (autoFitRatio < 0.99f) {
            sharedLyricStyle.copy(fontSize = activeFontSize)
        } else {
            sharedLyricStyle
        }

        val baseOutlineStyle = remember(activeSharedStyle, unhighlightedOutlineColor, activeOutlinePx) {
            activeSharedStyle.copy(
                color = unhighlightedOutlineColor,
                drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = activeOutlinePx,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )
        }

        val baseFillStyle = remember(activeSharedStyle, inactiveBaseColor) {
            activeSharedStyle.copy(
                color = inactiveBaseColor,
                drawStyle = androidx.compose.ui.graphics.drawscope.Fill
            )
        }

        // Inner container centered in the line that tightly wraps the lyric text and places countdown cleanly ABOVE
        Box(
            modifier = Modifier.wrapContentSize(),
            contentAlignment = Alignment.TopStart
        ) {
            // Text container with reserved top space for the countdown badge
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(
                        start = 4.dp,
                        end = 4.dp,
                        top = if (countdownNumber in 1..5 && lyricConfig.countdownMode != LyricCountdownMode.OFF) {
                            if (lyricConfig.countdownMode == LyricCountdownMode.PHOTO_BADGE) {
                                if (isLandscape) 54.dp else 46.dp
                            } else {
                                if (isLandscape) 32.dp else 28.dp
                            }
                        } else 2.dp,
                        bottom = 2.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                // LAYER 1: Deep 3D Drop Shadow layer for supreme legibility on any bright video/photo background
                if (lyricConfig.hasDropShadow || lyricConfig.shadowStyle == LyricShadowStyle.SUBTLE_DROP) {
                    Text(
                        text = displayText,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.offset(x = 2.5.dp, y = 3.5.dp),
                        style = activeSharedStyle.copy(
                            color = Color.Black.copy(alpha = 0.92f),
                            drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = activeOutlinePx * 1.3f,
                                join = androidx.compose.ui.graphics.StrokeJoin.Round
                            )
                        )
                    )
                }

                // LAYER 2: Crisp 2D stroke outline layer (base / unhighlighted)
                if (useOutline) {
                    Text(
                        text = displayText,
                        maxLines = 1,
                        softWrap = false,
                        style = baseOutlineStyle
                    )
                }

                // LAYER 3: Base inactive fill (single CPU layout pass, caches textLayoutResult)
                Text(
                    text = displayText,
                    maxLines = 1,
                    softWrap = false,
                    onTextLayout = { textLayoutResult = it },
                    style = baseFillStyle
                )

                // LAYER 4: GPU-accelerated Active Highlight Overlay (60 FPS letter-by-letter Skia clipRect)
                if (isActive && isWipeOn && wipeClamped > 0f) {
                    val activeOutlineColor = if (lyricConfig.outlineColor == LyricOutlineColor.WHITE) Color.Black else Color.White
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .drawWithContent {
                                val layout = textLayoutResult
                                val targetX = if (layout != null && displayText.isNotEmpty()) {
                                    if (wipeClamped >= 1f) {
                                        size.width + 20f
                                    } else {
                                        try {
                                            val totalChars = displayText.length.coerceAtMost(layout.layoutInput.text.length)
                                            val charProgress = (wipeClamped * totalChars).coerceIn(0f, totalChars.toFloat())
                                            val charIdx = charProgress.toInt().coerceIn(0, (totalChars - 1).coerceAtLeast(0))
                                            val charFrac = (charProgress - charIdx).coerceIn(0f, 1f)
                                            val box = layout.getBoundingBox(charIdx)
                                            box.left + (box.width * charFrac)
                                        } catch (e: Throwable) {
                                            size.width * wipeClamped
                                        }
                                    }
                                } else {
                                    size.width * wipeClamped
                                }
                                clipRect(left = -10f, top = -10f, right = targetX, bottom = size.height + 10f) {
                                    this@drawWithContent.drawContent()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (useOutline) {
                            Text(
                                text = displayText,
                                maxLines = 1,
                                softWrap = false,
                                style = activeSharedStyle.copy(
                                    color = activeOutlineColor,
                                    drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = activeOutlinePx,
                                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                                    )
                                )
                            )
                        }
                        Text(
                            text = displayText,
                            maxLines = 1,
                            softWrap = false,
                            style = activeSharedStyle.copy(
                                color = activeColor,
                                shadow = if (lyricConfig.shadowStyle == LyricShadowStyle.NEON_GLOW) shadow else null,
                                drawStyle = androidx.compose.ui.graphics.drawscope.Fill
                            )
                        )
                    }
                }
            }

            // Countdown indicator placed cleanly ABOVE the first letter of lyrics without blocking!
            if (countdownNumber in 1..5 && lyricConfig.countdownMode != LyricCountdownMode.OFF) {
                Box(
                    modifier = Modifier
                        .zIndex(10f)
                        .align(Alignment.TopStart)
                        .offset(
                            x = badgeXOffsetDp,
                            y = 2.dp
                        )
                ) {
                    when (lyricConfig.countdownMode) {
                        LyricCountdownMode.ARCADE_NUMBER -> {
                            SingleCountdownBadge(
                                number = countdownNumber,
                                isLandscape = isLandscape
                            )
                        }
                        LyricCountdownMode.CLASSIC_DOTS -> {
                            if (countdownNumber == 5) {
                                ArcadeGoBadge(isLandscape = isLandscape)
                            } else {
                                VideokeDotsCountdownBadge(
                                    number = countdownNumber,
                                    isLandscape = isLandscape
                                )
                            }
                        }
                        LyricCountdownMode.PHOTO_BADGE -> {
                            CustomCountdownStepBadge(
                                step = countdownNumber,
                                lyricConfig = lyricConfig,
                                isLandscape = isLandscape
                            )
                        }
                        LyricCountdownMode.OFF -> {}
                    }
                }
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



    bgBrush: Brush? = null,



    borderBrush: Brush? = null



) {



    val defaultBrush = Brush.verticalGradient(



        colors = listOf(



            Color(0xFF1E293B),



            Color(0xFF1E293B)



        )



    )



    val defaultBorder = Brush.verticalGradient(



        colors = listOf(



            Color(0xFF334155),



            Color(0xFF334155)



        )



    )



    Box(



        modifier = modifier



            .height(50.dp)



            .clip(RoundedCornerShape(14.dp))



            .background(bgBrush ?: defaultBrush)



            .border(



                1.dp,



                borderBrush ?: defaultBorder,



                RoundedCornerShape(14.dp)



            )



            .clickable { onClick() }



            .focusable(),



        contentAlignment = Alignment.Center



    ) {



        Text(



            text = text,



            color = textColor,



            fontWeight = FontWeight.Bold,



            fontSize = if (text.length > 2) 14.sp else 22.sp,



            fontFamily = FontFamily.SansSerif



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



    icon: String = "",



    borderBrush: Brush? = null



) {



    val defaultBorder = Brush.verticalGradient(



        listOf(Color(0xFF334155), Color(0xFF334155))



    )



    Box(



        modifier = modifier



            .fillMaxWidth()



            .height(50.dp)



            .clip(RoundedCornerShape(14.dp))



            .background(bgBrush)



            .border(



                1.dp,



                borderBrush ?: defaultBorder,



                RoundedCornerShape(14.dp)



            )



            .clickable { onClick() }



            .focusable(),



        contentAlignment = Alignment.Center



    ) {



        if (text.isEmpty()) {



            if (icon == "📖") {



                SongbookButtonGraphic(showLabel = true, iconWidth = 24.dp, iconHeight = 16.dp)



            } else {



                Text(



                    text = icon,



                    color = textColor,



                    fontWeight = FontWeight.Bold,



                    fontSize = 20.sp



                )



            }



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



            .background(Color(0xFF1E293B))



            .border(1.dp, Color(0xFF334155), CircleShape)



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



    val btnBg = Color(state.appTheme.buttonBgColor)



    val btnBorder = Color(state.appTheme.buttonBorderColor)







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



                    .background(btnBg)



                    .border(1.dp, btnBorder, RoundedCornerShape(8.dp))



                    .clickable { onOpenSongbook() }



                    .padding(horizontal = 10.dp),



                contentAlignment = Alignment.Center



            ) {



                Text(



                    text = "📖 SONGBOOK",



                    color = Color.White,



                    fontSize = 11.sp,



                    fontWeight = FontWeight.Black



                )



            }







            // Keypad Dial Modal



            Box(



                modifier = Modifier



                    .height(32.dp)



                    .clip(RoundedCornerShape(8.dp))



                    .background(btnBg)



                    .border(1.dp, btnBorder, RoundedCornerShape(8.dp))



                    .clickable { onOpenKeypad() }



                    .padding(horizontal = 10.dp),



                contentAlignment = Alignment.Center



            ) {



                Text(



                    text = "🔢 DIAL NO.",



                    color = Color.White,



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



                    .background(if (state.isPlaying && !state.isPlaybackPaused) Color(0xFFF59E0B) else btnBg)



                    .border(1.dp, if (state.isPlaying && !state.isPlaybackPaused) Color(0xFFD97706) else btnBorder, RoundedCornerShape(8.dp))



                    .clickable { onPlay() }



                    .padding(horizontal = 12.dp),



                contentAlignment = Alignment.Center



            ) {



                Text(text = if (state.isPlaying && !state.isPlaybackPaused) "⏸ PAUSE" else "▶ PLAY", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)



            }







            // Next



            Box(



                modifier = Modifier



                    .height(32.dp)



                    .clip(RoundedCornerShape(8.dp))



                    .background(btnBg)



                    .border(1.dp, btnBorder, RoundedCornerShape(8.dp))



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



                    .background(btnBg)



                    .border(1.dp, btnBorder, RoundedCornerShape(8.dp))



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



                    .background(btnBg)



                    .border(1.dp, btnBorder, RoundedCornerShape(6.dp))



                    .clickable { onOpenBackgroundDialog() }



                    .padding(horizontal = 8.dp),



                contentAlignment = Alignment.Center



            ) {



                Text(text = "🎬 BG", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Black)



            }







            // Settings



            Box(



                modifier = Modifier



                    .size(28.dp)



                    .clip(RoundedCornerShape(6.dp))



                    .background(btnBg)



                    .border(1.dp, btnBorder, RoundedCornerShape(6.dp))



                    .clickable { onOpenSettingsDialog() },



                contentAlignment = Alignment.Center



            ) {



                Text(text = "⚙️", fontSize = 13.sp, color = Color.White)



            }







            // Phone Split-screen Toggle



            Box(



                modifier = Modifier



                    .height(28.dp)



                    .clip(RoundedCornerShape(6.dp))



                    .background(btnBg)



                    .border(1.dp, btnBorder, RoundedCornerShape(6.dp))



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



    val btnBg = Color(state.appTheme.buttonBgColor)



    val btnBorder = Color(state.appTheme.buttonBorderColor)







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



                .border(2.dp, btnBorder, RoundedCornerShape(16.dp))



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



                            color = Color.White



                        )



                        Surface(



                            shape = RoundedCornerShape(6.dp),



                            color = btnBg,



                            border = androidx.compose.foundation.BorderStroke(1.dp, btnBorder),



                            modifier = Modifier.clickable { showQrRemoteInTv = !showQrRemoteInTv }



                        ) {



                            Text(



                                text = "📱 QR REMOTE",



                                color = Color.White,



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







                // 6 Advancing Digit Slots: Starts from Right Side [ 0 ] [ 0 ] [ 0 ] [ 0 ] [ 0 ] [ 0 ]



                Row(



                    horizontalArrangement = Arrangement.spacedBy(6.dp),



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



                            isActiveSlot -> Color(0xFFFFD54F)



                            isFilled -> Color(0xFF00FF88)



                            else -> Color(0xFF334155)



                        }







                        val digitColor = when {



                            isFilled -> Color(0xFF00FF88)



                            isActiveSlot -> Color(0xFFFFD54F)



                            else -> Color(0xFF1E293B) // Ghost unlit 0 segment



                        }







                        val boxBg = when {



                            isActiveSlot -> Color(0xFF131D2E)



                            isFilled -> Color(0xFF021E11)



                            else -> Color.Black



                        }







                        Box(



                            modifier = Modifier



                                .size(40.dp, 48.dp)



                                .clip(RoundedCornerShape(6.dp))



                                .background(boxBg)



                                .border(



                                    if (isActiveSlot) 2.dp else 1.dp,



                                    borderColor,



                                    RoundedCornerShape(6.dp)



                                ),



                            contentAlignment = Alignment.Center



                        ) {



                            Text(



                                text = digitChar.toString(),



                                color = digitColor,



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







                // Keypad grid (1-9, CLR, 0, DEL) - Single Uniform Button Color



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



                                border = androidx.compose.foundation.BorderStroke(1.dp, btnBorder),



                                colors = ButtonDefaults.buttonColors(



                                    containerColor = btnBg



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







                // Action Row: RESERVE & PLAY (Single Uniform Color)



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



                        border = androidx.compose.foundation.BorderStroke(1.dp, btnBorder),



                        colors = ButtonDefaults.buttonColors(containerColor = btnBg)



                    ) {



                        Text(



                            text = if (isTagalog) "R IRESERBA" else "R RESERVE",



                            fontWeight = FontWeight.Black,



                            fontSize = 14.sp,



                            color = Color.White



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



                        border = androidx.compose.foundation.BorderStroke(1.dp, btnBorder),



                        colors = ButtonDefaults.buttonColors(containerColor = btnBg)



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



    searchQuery: String = "",



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



    theme: AppTheme = AppTheme.CYBER_NEON,



    onSwitchTab: (Int) -> Unit = {},



    onToggleFilter: (Boolean) -> Unit = {},



    onOpenFolderLocator: () -> Unit = {},



    onOpenYouTubeDownloader: (String?) -> Unit = {},



    onQueryChange: (String) -> Unit = {},



    onSelectSong: (Song) -> Unit = {},



    onReserveSong: (Song) -> Unit = {},



    onToggleFavoriteSong: (Song) -> Unit = {},



    onPlayReservedIndex: (Int) -> Unit = {},



    onRemoveReservedIndex: (Int) -> Unit = {},



    onDismiss: () -> Unit,



    onSpeakDigit: (Char) -> Unit = {},



    modifier: Modifier = Modifier



) {



    val cleanFont = FontFamily(Font(R.font.radio_canada))



    var query by remember { mutableStateOf(searchQuery) }



    LaunchedEffect(searchQuery) {



        if (query != searchQuery) {



            query = searchQuery



        }



    }



    val reservedCodes = remember { mutableStateMapOf<String, Boolean>() }







    Surface(



        shape = RoundedCornerShape(14.dp),



        color = Color(theme.surfaceColor).copy(alpha = 0.95f),



        border = androidx.compose.foundation.BorderStroke(



            1.5.dp,



            Color(theme.primaryColor)



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



                    val isSongbookActive = activeTab == 0



                    Surface(



                        shape = RoundedCornerShape(8.dp),



                        color = if (isSongbookActive) Color(theme.primaryColor) else Color(theme.buttonBgColor),



                        border = androidx.compose.foundation.BorderStroke(



                            1.dp,



                            if (isSongbookActive) Color(theme.primaryColor) else Color(theme.buttonBorderColor)



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



                            val activeTextColor = if (theme.primaryColor == 0xFF00E676L || theme.primaryColor == 0xFFFFD54FL || theme.primaryColor == 0xFFFFC107L) Color.Black else Color.White



                            Text(



                                text = "📖 SONGBOOK",



                                color = if (isSongbookActive) activeTextColor else Color(theme.textColor),



                                fontFamily = cleanFont,



                                fontSize = if (isLandscape) 11.sp else 9.5.sp,



                                fontWeight = FontWeight.Bold,



                                maxLines = 1,



                                textAlign = TextAlign.Center



                            )



                        }



                    }







                    // TAB 1: R RESERVED



                    val isReserveActive = activeTab == 1



                    Surface(



                        shape = RoundedCornerShape(8.dp),



                        color = if (isReserveActive) Color(theme.secondaryColor) else Color(theme.buttonBgColor),



                        border = androidx.compose.foundation.BorderStroke(



                            1.dp,



                            if (isReserveActive) Color(theme.secondaryColor) else Color(theme.buttonBorderColor)



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



                            val activeSecTextColor = if (theme.secondaryColor == 0xFF00E676L || theme.secondaryColor == 0xFFFFD54FL || theme.secondaryColor == 0xFFFFC107L || theme.secondaryColor == 0xFFFF80ABL) Color.Black else Color.White



                            Text(



                                text = "R RESERVE (${reserveQueue.size})",



                                color = if (isReserveActive) activeSecTextColor else Color(theme.textColor),



                                fontFamily = cleanFont,



                                fontSize = if (isLandscape) 11.sp else 9.5.sp,



                                fontWeight = FontWeight.Bold,



                                maxLines = 1,



                                textAlign = TextAlign.Center



                            )



                        }



                    }







                    // TAB 2: ❤️ FAVORITES



                    val isFavActive = activeTab == 2



                    Surface(



                        shape = RoundedCornerShape(8.dp),



                        color = if (isFavActive) Color(0xFFFF4081) else Color(theme.buttonBgColor),



                        border = androidx.compose.foundation.BorderStroke(



                            1.dp,



                            if (isFavActive) Color(0xFFFF80AB) else Color(theme.buttonBorderColor)



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



                                color = if (isFavActive) Color.White else Color(0xFFFF80AB),



                                fontFamily = cleanFont,



                                fontSize = if (isLandscape) 11.sp else 9.5.sp,



                                fontWeight = FontWeight.Bold,



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



                        .background(Color(theme.buttonBgColor))



                        .border(1.dp, Color(theme.buttonBorderColor), CircleShape)



                        .clickable { onDismiss() },



                    contentAlignment = Alignment.Center



                ) {



                    Text(text = "✕", color = Color(theme.textColor), fontSize = 13.sp, fontWeight = FontWeight.Bold)



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



                            if (songs.isNotEmpty()) {



                                Surface(



                                    shape = RoundedCornerShape(6.dp),



                                    color = Color(0xFFE50914),



                                    modifier = Modifier.clickable { onOpenYouTubeDownloader(null) }



                                ) {



                                    Text(



                                        text = "▶ YouTube",



                                        color = Color.White,



                                        fontSize = 9.5.sp,



                                        fontWeight = FontWeight.Bold,



                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)



                                    )



                                }



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







                    // Compact Search Input Bar with inline Backspace (⌫) and Clear (✕) buttons



                    OutlinedTextField(



                        value = query,



                        onValueChange = { newText ->



                            if (newText.length > query.length) {



                                val addedChar = newText.last()



                                if (addedChar in '0'..'9') {



                                    onSpeakDigit(addedChar)



                                }



                            }



                            query = newText



                            onQueryChange(newText)



                        },



                        placeholder = {



                            Text(



                                text = if (isTagalog) "I-type ang titulo, artist, o numero ng kanta..." else "Type song title, artist, or code...",



                                fontFamily = cleanFont,



                                fontSize = 11.5.sp,



                                color = Color(theme.textMutedColor).copy(alpha = 0.7f)



                            )



                        },



                        trailingIcon = {



                            if (query.isNotEmpty()) {



                                Row(



                                    verticalAlignment = Alignment.CenterVertically,



                                    horizontalArrangement = Arrangement.spacedBy(4.dp),



                                    modifier = Modifier.padding(end = 6.dp)



                                ) {



                                    // Delete 1 mistyped character (⌫)



                                    Box(



                                        modifier = Modifier



                                            .height(26.dp)



                                            .clip(RoundedCornerShape(6.dp))



                                            .background(Color(theme.buttonBgColor))



                                            .border(1.dp, Color(theme.secondaryColor), RoundedCornerShape(6.dp))



                                            .clickable {



                                                if (query.isNotEmpty()) {



                                                    val updated = query.dropLast(1)



                                                    query = updated



                                                    onQueryChange(updated)



                                                }



                                            }



                                            .padding(horizontal = 8.dp),



                                        contentAlignment = Alignment.Center



                                    ) {



                                        Text(



                                            text = "⌫",



                                            color = Color(theme.secondaryColor),



                                            fontSize = 12.sp,



                                            fontWeight = FontWeight.Black



                                        )



                                    }



                                    // Clear all text (✕)



                                    Box(



                                        modifier = Modifier



                                            .size(26.dp)



                                            .clip(RoundedCornerShape(6.dp))



                                            .background(Color(theme.buttonBorderColor))



                                            .clickable {



                                                query = ""



                                                onQueryChange("")



                                            },



                                        contentAlignment = Alignment.Center



                                    ) {



                                        Text(



                                            text = "✕",



                                            color = Color(theme.textColor),



                                            fontSize = 11.sp,



                                            fontWeight = FontWeight.Black



                                        )



                                    }



                                }



                            }



                        },



                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(



                            autoCorrect = false,



                            imeAction = androidx.compose.ui.text.input.ImeAction.Search



                        ),



                        textStyle = TextStyle(



                            color = Color(theme.textColor),



                            fontFamily = cleanFont,



                            fontSize = 12.5.sp,



                            fontWeight = FontWeight.SemiBold



                        ),



                        colors = OutlinedTextFieldDefaults.colors(



                            focusedContainerColor = Color(theme.backgroundColor),



                            unfocusedContainerColor = Color(theme.backgroundColor),



                            focusedBorderColor = Color(theme.primaryColor),



                            unfocusedBorderColor = Color(theme.buttonBorderColor),



                            focusedTextColor = Color(theme.textColor),



                            unfocusedTextColor = Color(theme.textColor)



                        ),



                        shape = RoundedCornerShape(10.dp),



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



                                Column(



                                    modifier = Modifier



                                        .fillMaxWidth()



                                        .padding(vertical = 16.dp, horizontal = 10.dp),



                                    horizontalAlignment = Alignment.CenterHorizontally,



                                    verticalArrangement = Arrangement.spacedBy(8.dp)



                                ) {



                                    Text(



                                        text = if (isScanning)



                                            (if (isTagalog) "Nag-i-scan ng mga kanta..." else "Scanning songs...")



                                        else if (searchQuery.isNotBlank())



                                            (if (isTagalog) "Walang nahanap na kanta para sa \"$searchQuery\"." else "No matching songs found for \"$searchQuery\".")



                                        else



                                            (if (isTagalog) "Walang nahanap na kanta." else "No matching songs found."),



                                        color = Color.LightGray,



                                        fontSize = 11.5.sp,



                                        textAlign = TextAlign.Center



                                    )



                                    if (searchQuery.isNotBlank() && !isScanning && songs.isNotEmpty()) {



                                        Surface(



                                            onClick = { onOpenYouTubeDownloader(searchQuery) },



                                            shape = RoundedCornerShape(8.dp),



                                            color = Color(0xFFE53935),



                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))



                                        ) {



                                            Row(



                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),



                                                verticalAlignment = Alignment.CenterVertically,



                                                horizontalArrangement = Arrangement.spacedBy(6.dp)



                                            ) {



                                                Text("▶", fontSize = 11.sp, color = Color.White)



                                                Text(



                                                    text = if (isTagalog) "I-DOWNLOAD SA YOUTUBE / MP4" else "DOWNLOAD VIA YOUTUBE / MP4",



                                                    color = Color.White,



                                                    fontSize = 11.sp,



                                                    fontWeight = FontWeight.Bold



                                                )



                                            }



                                        }



                                    }



                                }



                            }



                        }







                        items(songs) { song ->



                            val isPlayable = song.isPacked || song.sectorOffset > 0L || playableCodes.contains(song.code) || song.filePath.endsWith(".mid", true) || song.filePath.endsWith(".kar", true)



                            val isFav = favoriteCodes.contains(song.code)



                            val isReserved = reservedCodes[song.code] == true







                            Column(



                                modifier = Modifier



                                    .fillMaxWidth()



                                    .clip(RoundedCornerShape(8.dp))



                                    .background(Color(theme.surfaceColor))



                                    .border(1.dp, Color(theme.buttonBorderColor), RoundedCornerShape(8.dp))



                                    .clickable { onSelectSong(song) }



                                    .padding(horizontal = 9.dp, vertical = 6.dp),



                                verticalArrangement = Arrangement.spacedBy(3.dp)



                            ) {



                                // Top Row: Full Song Title across the entire width (clean theme font!)



                                Text(



                                    text = song.title,



                                    color = Color(theme.textColor),



                                    fontFamily = cleanFont,



                                    fontWeight = FontWeight.Bold,



                                    fontSize = 13.sp,



                                    lineHeight = 16.sp,



                                    maxLines = 2,



                                    softWrap = true,



                                    modifier = Modifier.fillMaxWidth()



                                )







                                // Bottom Row: 6-digit Code + Artist on Left, Action Buttons on Right



                                Row(



                                    modifier = Modifier.fillMaxWidth(),



                                    horizontalArrangement = Arrangement.SpaceBetween,



                                    verticalAlignment = Alignment.CenterVertically



                                ) {



                                    Row(



                                        verticalAlignment = Alignment.CenterVertically,



                                        modifier = Modifier.weight(1f).padding(end = 6.dp)



                                    ) {



                                        Box(



                                            modifier = Modifier



                                                .clip(RoundedCornerShape(5.dp))



                                                .background(Color(theme.secondaryColor).copy(alpha = 0.18f))



                                                .border(0.8.dp, Color(theme.secondaryColor).copy(alpha = 0.5f), RoundedCornerShape(5.dp))



                                                .padding(horizontal = 6.dp, vertical = 2.dp)



                                        ) {



                                            Text(



                                                text = song.code,
                                                softWrap = false,
                                                maxLines = 1,



                                                color = Color(theme.secondaryColor),



                                                fontFamily = cleanFont,



                                                fontWeight = FontWeight.Bold,



                                                fontSize = 11.sp,



                                                letterSpacing = 0.5.sp



                                            )



                                        }



                                        Spacer(modifier = Modifier.width(6.dp))



                                        Text(



                                            text = "🎤 ${com.example.videokey.data.SongMetadataResolver.getDisplayArtist(song)}",



                                            color = Color(theme.textMutedColor),



                                            fontFamily = cleanFont,



                                            fontWeight = FontWeight.Normal,



                                            fontSize = 10.5.sp,



                                            maxLines = 1,



                                            overflow = TextOverflow.Ellipsis



                                        )



                                    }







                                    Row(



                                        verticalAlignment = Alignment.CenterVertically,



                                        horizontalArrangement = Arrangement.spacedBy(5.dp)



                                    ) {



                                        // 1. Favorite Toggle (❤️)



                                        Surface(



                                            shape = RoundedCornerShape(7.dp),



                                            color = if (isFav) Color(0xFF831843) else Color(theme.buttonBgColor),



                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isFav) Color(0xFFFF4081) else Color(theme.buttonBorderColor)),



                                            modifier = Modifier



                                                .height(27.dp)



                                                .clickable { onToggleFavoriteSong(song) }



                                        ) {



                                            Box(



                                                modifier = Modifier.padding(horizontal = 7.dp),



                                                contentAlignment = Alignment.Center



                                            ) {



                                                Text(



                                                    text = "❤️",



                                                    fontSize = 10.5.sp



                                                )



                                            }



                                        }







                                        // 2. Reserve Button (+ RES)



                                        Surface(



                                            shape = RoundedCornerShape(7.dp),



                                            color = if (isReserved) Color(theme.primaryColor).copy(alpha = 0.25f) else Color(theme.buttonBgColor),



                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isReserved) Color(theme.primaryColor) else Color(theme.buttonBorderColor)),



                                            modifier = Modifier



                                                .height(27.dp)



                                                .clickable {



                                                    onReserveSong(song)



                                                    reservedCodes[song.code] = true



                                                }



                                        ) {



                                            Box(



                                                modifier = Modifier.padding(horizontal = 8.dp),



                                                contentAlignment = Alignment.Center



                                            ) {



                                                Text(



                                                    text = if (isReserved) "✓ RES" else "+ RES",



                                                    color = if (isReserved) Color(theme.primaryColor) else Color(theme.secondaryColor),



                                                    fontFamily = cleanFont,



                                                    fontSize = 10.sp,



                                                    fontWeight = FontWeight.Black



                                                )



                                            }



                                        }







                                        // 3. Play Button (▶ TUGTOG / ▶ PLAY)



                                        val playBtnBg = if (isPlayable) Color(theme.primaryColor) else Color(theme.buttonBgColor)



                                        val playBtnText = if (isPlayable) getContrastingTextColor(theme.primaryColor) else Color(theme.textMutedColor)



                                        Surface(



                                            shape = RoundedCornerShape(7.dp),



                                            color = playBtnBg,



                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isPlayable) Color(theme.primaryColor) else Color(theme.buttonBorderColor)),



                                            modifier = Modifier



                                                .height(27.dp)



                                                .clickable { onSelectSong(song) }



                                        ) {



                                            Box(



                                                modifier = Modifier.padding(horizontal = 9.dp),



                                                contentAlignment = Alignment.Center



                                            ) {



                                                Text(



                                                    text = if (isTagalog) "▶ TUGTOG" else "▶ PLAY",



                                                    color = playBtnText,



                                                    fontFamily = cleanFont,



                                                    fontSize = 10.sp,



                                                    fontWeight = FontWeight.Black



                                                )



                                            }



                                        }



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



                        color = Color(theme.secondaryColor),



                        fontFamily = cleanFont,



                        fontSize = 12.sp,



                        fontWeight = FontWeight.Bold,



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



                                        color = Color(theme.textMutedColor),



                                        fontFamily = cleanFont,



                                        fontSize = 11.sp,



                                        textAlign = TextAlign.Center



                                    )



                                }



                            }



                        }







                        items(reserveQueue.size) { index ->



                            val song = reserveQueue[index]



                            Column(



                                modifier = Modifier



                                    .fillMaxWidth()



                                    .clip(RoundedCornerShape(8.dp))



                                    .background(Color(theme.surfaceColor))



                                    .border(1.dp, Color(theme.buttonBorderColor), RoundedCornerShape(8.dp))



                                    .padding(horizontal = 9.dp, vertical = 6.dp),



                                verticalArrangement = Arrangement.spacedBy(3.dp)



                            ) {



                                Text(



                                    text = "#${index + 1}  ${song.title}",



                                    color = Color(theme.textColor),



                                    fontFamily = cleanFont,



                                    fontWeight = FontWeight.Bold,



                                    fontSize = 12.5.sp,



                                    lineHeight = 15.sp,



                                    maxLines = 2,



                                    softWrap = true,



                                    modifier = Modifier.fillMaxWidth()



                                )



                                Row(



                                    modifier = Modifier.fillMaxWidth(),



                                    horizontalArrangement = Arrangement.SpaceBetween,



                                    verticalAlignment = Alignment.CenterVertically



                                ) {



                                    Row(



                                        verticalAlignment = Alignment.CenterVertically,



                                        modifier = Modifier.weight(1f).padding(end = 6.dp)



                                    ) {



                                        Box(



                                            modifier = Modifier



                                                .clip(RoundedCornerShape(5.dp))



                                                .background(Color(theme.secondaryColor).copy(alpha = 0.18f))



                                                .border(0.8.dp, Color(theme.secondaryColor).copy(alpha = 0.5f), RoundedCornerShape(5.dp))



                                                .padding(horizontal = 6.dp, vertical = 2.dp)



                                        ) {



                                            Text(



                                                text = song.code,



                                                color = Color(theme.secondaryColor),



                                                fontFamily = cleanFont,



                                                fontWeight = FontWeight.Bold,



                                                fontSize = 11.sp,



                                                letterSpacing = 0.5.sp



                                            )



                                        }



                                        Spacer(modifier = Modifier.width(6.dp))



                                        Text(



                                            text = "🎤 ${com.example.videokey.data.SongMetadataResolver.getDisplayArtist(song)}",



                                            color = Color(theme.textMutedColor),



                                            fontFamily = cleanFont,



                                            fontSize = 10.5.sp,



                                            maxLines = 1,



                                            overflow = TextOverflow.Ellipsis



                                        )



                                    }







                                    Row(



                                        verticalAlignment = Alignment.CenterVertically,



                                        horizontalArrangement = Arrangement.spacedBy(4.dp)



                                    ) {



                                        // Play from reserve



                                        Surface(



                                            shape = RoundedCornerShape(6.dp),



                                            color = Color(theme.primaryColor),



                                            modifier = Modifier.clickable { onPlayReservedIndex(index) }



                                        ) {



                                            val playTextColor = if (theme.primaryColor == 0xFF00E676L || theme.primaryColor == 0xFFFFD54FL || theme.primaryColor == 0xFFFFC107L) Color.Black else Color.White



                                            Text(



                                                text = "▶ PLAY",



                                                color = playTextColor,



                                                fontFamily = cleanFont,



                                                fontSize = 10.5.sp,



                                                fontWeight = FontWeight.Bold,



                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)



                                            )



                                        }







                                        // Remove from reserve



                                        Surface(



                                            shape = RoundedCornerShape(6.dp),



                                            color = Color(0xFFEF4444).copy(alpha = 0.18f),



                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),



                                            modifier = Modifier.clickable { onRemoveReservedIndex(index) }



                                        ) {



                                            Text(



                                                text = "✕",



                                                color = Color(0xFFFF5252),



                                                fontFamily = cleanFont,



                                                fontSize = 11.sp,



                                                fontWeight = FontWeight.Bold,



                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)



                                            )



                                        }



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



                        fontFamily = cleanFont,



                        fontSize = 12.sp,



                        fontWeight = FontWeight.Bold,



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



                                        color = Color(theme.textMutedColor),



                                        fontFamily = cleanFont,



                                        fontSize = 11.sp,



                                        textAlign = TextAlign.Center



                                    )



                                }



                            }



                        }







                        items(favoriteSongs) { song ->



                            Column(



                                modifier = Modifier



                                    .fillMaxWidth()



                                    .clip(RoundedCornerShape(8.dp))



                                    .background(Color(theme.surfaceColor))



                                    .border(1.dp, Color(theme.buttonBorderColor), RoundedCornerShape(8.dp))



                                    .clickable { onSelectSong(song) }



                                    .padding(horizontal = 9.dp, vertical = 6.dp),



                                verticalArrangement = Arrangement.spacedBy(3.dp)



                            ) {



                                Text(



                                    text = song.title,



                                    color = Color(theme.textColor),



                                    fontFamily = cleanFont,



                                    fontWeight = FontWeight.Bold,



                                    fontSize = 12.5.sp,



                                    lineHeight = 15.sp,



                                    maxLines = 2,



                                    softWrap = true,



                                    modifier = Modifier.fillMaxWidth()



                                )



                                Row(



                                    modifier = Modifier.fillMaxWidth(),



                                    horizontalArrangement = Arrangement.SpaceBetween,



                                    verticalAlignment = Alignment.CenterVertically



                                ) {



                                    Row(



                                        verticalAlignment = Alignment.CenterVertically,



                                        modifier = Modifier.weight(1f).padding(end = 6.dp)



                                    ) {



                                        Box(



                                            modifier = Modifier



                                                .clip(RoundedCornerShape(5.dp))



                                                .background(Color(theme.secondaryColor).copy(alpha = 0.18f))



                                                .border(0.8.dp, Color(theme.secondaryColor).copy(alpha = 0.5f), RoundedCornerShape(5.dp))



                                                .padding(horizontal = 6.dp, vertical = 2.dp)



                                        ) {



                                            Text(



                                                text = song.code,



                                                color = Color(theme.secondaryColor),



                                                fontFamily = cleanFont,



                                                fontWeight = FontWeight.Bold,



                                                fontSize = 11.sp,



                                                letterSpacing = 0.5.sp



                                            )



                                        }



                                        Spacer(modifier = Modifier.width(6.dp))



                                        Text(



                                            text = "🎤 ${com.example.videokey.data.SongMetadataResolver.getDisplayArtist(song)}",



                                            color = Color(theme.textMutedColor),



                                            fontFamily = cleanFont,



                                            fontSize = 10.5.sp,



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



                                            color = Color(0xFFFF4081).copy(alpha = 0.2f),



                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF4081).copy(alpha = 0.6f)),



                                            modifier = Modifier.clickable { onToggleFavoriteSong(song) }



                                        ) {



                                            Text(



                                                text = "❤️",



                                                fontSize = 11.sp,



                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)



                                            )



                                        }







                                        // Reserve Button (✓ RES)



                                        Surface(



                                            shape = RoundedCornerShape(6.dp),



                                            color = Color(theme.secondaryColor).copy(alpha = 0.18f),



                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(theme.secondaryColor).copy(alpha = 0.5f)),



                                            modifier = Modifier.clickable { onReserveSong(song) }



                                        ) {



                                            Text(



                                                text = "✓ RES",



                                                color = Color(theme.secondaryColor),



                                                fontFamily = cleanFont,



                                                fontSize = 10.5.sp,



                                                fontWeight = FontWeight.Bold,



                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)



                                            )



                                        }







                                        // Play Button (▶ PLAY)



                                        Surface(



                                            shape = RoundedCornerShape(6.dp),



                                            color = Color(theme.primaryColor),



                                            modifier = Modifier.clickable { onSelectSong(song) }



                                        ) {



                                            val playTextColor = if (theme.primaryColor == 0xFF00E676L || theme.primaryColor == 0xFFFFD54FL || theme.primaryColor == 0xFFFFC107L) Color.Black else Color.White



                                            Text(



                                                text = "▶ PLAY",



                                                color = playTextColor,



                                                fontFamily = cleanFont,



                                                fontSize = 10.5.sp,



                                                fontWeight = FontWeight.Bold,



                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)



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



    theme: AppTheme = AppTheme.CYBER_NEON,



    onSpeakDigit: (Char) -> Unit = {},



    onToggleFilter: (Boolean) -> Unit = {},



    onOpenFolderLocator: () -> Unit,



    onQueryChange: (String) -> Unit,



    onSelectSong: (Song) -> Unit,



    onReserveSong: (Song) -> Unit,



    onDismiss: () -> Unit



) {



    val cleanFont = FontFamily(Font(R.font.radio_canada))



    var query by remember { mutableStateOf("") }



    val reservedCodes = remember { mutableStateMapOf<String, Boolean>() }







    Dialog(onDismissRequest = onDismiss) {



        Surface(



            shape = RoundedCornerShape(20.dp),



            color = Color(theme.surfaceColor),



            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(theme.primaryColor)),



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



                        fontFamily = cleanFont,



                        color = Color(theme.primaryColor),



                        fontSize = 14.sp



                    )



                    IconButton(onClick = onDismiss) {



                        Text(text = "✕", color = Color(theme.textColor), fontFamily = cleanFont, fontWeight = FontWeight.Bold)



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



                        if (it.length > query.length) {



                            val lastChar = it.last()



                            if (lastChar in '0'..'9') {



                                onSpeakDigit(lastChar)



                            }



                        }



                        query = it



                        onQueryChange(it)



                    },



                    placeholder = {



                        Text(



                            text = if (isTagalog) "Maghanap ng titulo, artist, o numero ng kanta..." else "Search title, artist, or song code...",



                            fontFamily = cleanFont,



                            color = Color(theme.textMutedColor).copy(alpha = 0.7f),



                            fontSize = 12.sp



                        )



                    },



                    textStyle = TextStyle(



                        color = Color(theme.textColor),



                        fontFamily = cleanFont,



                        fontSize = 13.sp,



                        fontWeight = FontWeight.SemiBold



                    ),



                    colors = OutlinedTextFieldDefaults.colors(



                        focusedContainerColor = Color(theme.backgroundColor),



                        unfocusedContainerColor = Color(theme.backgroundColor),



                        focusedBorderColor = Color(theme.primaryColor),



                        unfocusedBorderColor = Color(theme.buttonBorderColor),



                        focusedTextColor = Color(theme.textColor),



                        unfocusedTextColor = Color(theme.textColor)



                    ),



                    shape = RoundedCornerShape(12.dp),



                    trailingIcon = {



                        if (query.isNotEmpty()) {



                            Row(



                                verticalAlignment = Alignment.CenterVertically,



                                horizontalArrangement = Arrangement.spacedBy(4.dp),



                                modifier = Modifier.padding(end = 6.dp)



                            ) {



                                Box(



                                    modifier = Modifier



                                        .height(26.dp)



                                        .clip(RoundedCornerShape(6.dp))



                                        .background(Color(theme.buttonBgColor))



                                        .border(1.dp, Color(theme.secondaryColor), RoundedCornerShape(6.dp))



                                        .clickable {



                                            if (query.isNotEmpty()) {



                                                val updated = query.dropLast(1)



                                                query = updated



                                                onQueryChange(updated)



                                            }



                                        }



                                        .padding(horizontal = 8.dp),



                                    contentAlignment = Alignment.Center



                                ) {



                                    Text(



                                        text = "⌫",



                                        color = Color(theme.secondaryColor),



                                        fontSize = 12.sp,



                                        fontWeight = FontWeight.Black



                                    )



                                }



                                Box(



                                    modifier = Modifier



                                        .size(26.dp)



                                        .clip(RoundedCornerShape(6.dp))



                                        .background(Color(theme.buttonBorderColor))



                                        .clickable {



                                            query = ""



                                            onQueryChange("")



                                        },



                                    contentAlignment = Alignment.Center



                                ) {



                                    Text(



                                        text = "✕",



                                        color = Color(theme.textColor),



                                        fontSize = 11.sp,



                                        fontWeight = FontWeight.Black



                                    )



                                }



                            }



                        }



                    },



                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(



                        autoCorrect = false,



                        imeAction = androidx.compose.ui.text.input.ImeAction.Search



                    ),



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



                                    color = Color(theme.textMutedColor),



                                    fontFamily = cleanFont,



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



                                .background(Color(theme.backgroundColor))



                                .border(1.dp, Color(theme.buttonBorderColor), RoundedCornerShape(10.dp))



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



                                    Box(



                                        modifier = Modifier



                                            .clip(RoundedCornerShape(5.dp))



                                            .background(Color(theme.secondaryColor).copy(alpha = 0.18f))



                                            .border(0.8.dp, Color(theme.secondaryColor).copy(alpha = 0.5f), RoundedCornerShape(5.dp))



                                            .padding(horizontal = 6.dp, vertical = 2.dp)



                                    ) {



                                        Text(



                                            text = song.code,



                                            color = Color(theme.secondaryColor),



                                            fontFamily = cleanFont,



                                            fontWeight = FontWeight.Bold,



                                            fontSize = 11.sp,



                                            letterSpacing = 0.5.sp



                                        )



                                    }



                                    Spacer(modifier = Modifier.width(6.dp))



                                    if (isPlayable) {



                                        Text(



                                            text = "🎵 MIDI",



                                            color = Color(0xFF00E676),



                                            fontSize = 9.sp,



                                            fontFamily = cleanFont,



                                            fontWeight = FontWeight.Black,



                                            modifier = Modifier



                                                .background(Color(0xFF00381B), RoundedCornerShape(3.dp))



                                                .border(1.dp, Color(0xFF00E676), RoundedCornerShape(3.dp))



                                                .padding(horizontal = 4.dp, vertical = 1.dp)



                                        )



                                    } else {



                                        Text(



                                            text = "📖 IDX",



                                            color = Color(theme.textMutedColor),



                                            fontSize = 9.sp,



                                            fontFamily = cleanFont,



                                            modifier = Modifier



                                                .background(Color(theme.buttonBgColor), RoundedCornerShape(3.dp))



                                                .padding(horizontal = 4.dp, vertical = 1.dp)



                                        )



                                    }



                                }



                                Spacer(modifier = Modifier.height(3.dp))



                                // Buong Titulo ng Kanta (Full Song Title - clean theme font)



                                Text(



                                    text = song.title,



                                    color = Color(theme.textColor),



                                    fontFamily = cleanFont,



                                    fontWeight = FontWeight.Bold,



                                    fontSize = 13.5.sp,



                                    maxLines = 4,



                                    softWrap = true



                                )



                                Spacer(modifier = Modifier.height(2.dp))



                                // Buong Artist ng Kanta (Full Artist - clean theme font)



                                Text(



                                    text = "🎤 ${com.example.videokey.data.SongMetadataResolver.getDisplayArtist(song)}",



                                    color = Color(theme.textMutedColor),



                                    fontFamily = cleanFont,



                                    fontSize = 11.sp,



                                    maxLines = 3,



                                    softWrap = true



                                )



                            }







                            Row(



                                verticalAlignment = Alignment.CenterVertically,



                                horizontalArrangement = Arrangement.spacedBy(6.dp)



                            ) {



                                val isReserved = reservedCodes[song.code] == true



                                Surface(



                                    shape = RoundedCornerShape(8.dp),



                                    color = if (isReserved) Color(0xFF064E3B) else Color(0xFF1E293B),



                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isReserved) Color(0xFF00FF88) else Color(0xFFFFD54F)),



                                    modifier = Modifier



                                        .height(30.dp)



                                        .clickable {



                                            onReserveSong(song)



                                            reservedCodes[song.code] = true



                                        }



                                ) {



                                    Box(



                                        modifier = Modifier.padding(horizontal = 10.dp),



                                        contentAlignment = Alignment.Center



                                    ) {



                                        Text(



                                            text = if (isReserved) (if (isTagalog) "✓ NAKALISTA" else "✓ QUEUED") else (if (isTagalog) "+ IRESERBA" else "+ RESERVE"),



                                            color = if (isReserved) Color(0xFF00FF88) else Color(0xFFFFD54F),



                                            fontSize = 10.sp,



                                            fontWeight = FontWeight.Bold



                                        )



                                    }



                                }







                                Surface(



                                    shape = RoundedCornerShape(8.dp),



                                    color = if (isPlayable) Color(0xFF00E676) else Color(0xFF334155),



                                    modifier = Modifier



                                        .height(30.dp)



                                        .clickable { onSelectSong(song) }



                                ) {



                                    Box(



                                        modifier = Modifier.padding(horizontal = 12.dp),



                                        contentAlignment = Alignment.Center



                                    ) {



                                        Text(



                                            text = if (isTagalog) "▶ TUGTOG" else "▶ PLAY",



                                            color = if (isPlayable) Color.Black else Color.White,



                                            fontSize = 10.5.sp,



                                            fontWeight = FontWeight.Black



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



fun ScoreCelebrationDialog(



    score: Int,



    isTagalog: Boolean = false,



    theme: com.example.videokey.ui.main.AppTheme = com.example.videokey.ui.main.AppTheme.CYBER_NEON,



    onDismiss: () -> Unit



) {



    val btnTextClr = getContrastingTextColor(theme.primaryColor)



    Dialog(onDismissRequest = onDismiss) {



        Surface(



            shape = RoundedCornerShape(24.dp),



            color = Color(theme.surfaceColor),



            border = androidx.compose.foundation.BorderStroke(3.dp, Color(theme.primaryColor)),



            modifier = Modifier.padding(16.dp)



        ) {



            Column(



                modifier = Modifier.padding(24.dp),



                horizontalAlignment = Alignment.CenterHorizontally



            ) {



                Text(



                    text = if (isTagalog) "🏆 PAGBATI! 🏆" else "🏆 CONGRATULATIONS! 🏆",



                    color = Color(theme.primaryColor),



                    fontWeight = FontWeight.Black,



                    fontFamily = FontFamily.SansSerif,



                    fontSize = 16.sp



                )



                Spacer(modifier = Modifier.height(12.dp))



                Text(



                    text = "$score",



                    color = Color(theme.secondaryColor),



                    fontFamily = FontFamily.SansSerif,



                    fontWeight = FontWeight.Black,



                    fontSize = 60.sp



                )



                Text(



                    text = if (isTagalog) "BIRIT KING! ANG GALING MO!" else "GREAT SINGING! AWESOME PERFORMANCE!",



                    color = Color(theme.textColor),



                    fontFamily = FontFamily.SansSerif,



                    fontWeight = FontWeight.Bold,



                    fontSize = 13.sp



                )



                Spacer(modifier = Modifier.height(16.dp))



                Button(



                    onClick = onDismiss,



                    colors = ButtonDefaults.buttonColors(containerColor = Color(theme.primaryColor)),



                    shape = RoundedCornerShape(12.dp)



                ) {



                    Text(text = if (isTagalog) "MAGPATULOY" else "CONTINUE", color = btnTextClr, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif)



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

    autoMatchedSoundFont: String = "",

    isVipActivated: Boolean = false,

    isTagalog: Boolean = false,

    theme: com.example.videokey.ui.main.AppTheme = com.example.videokey.ui.main.AppTheme.CYBER_NEON,

    onSelectSoundFont: (String) -> Unit,

    onScanStorage: () -> Unit,

    onDeleteSoundFont: ((String) -> Unit)? = null,

    onRequireVip: () -> Unit = {},

    onDismiss: () -> Unit

) {

    val doneBtnTextClr = getContrastingTextColor(theme.primaryColor)

    Dialog(

        onDismissRequest = onDismiss,

        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)

    ) {

        Surface(

            shape = RoundedCornerShape(22.dp),

            color = Color(theme.surfaceColor),

            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(theme.primaryColor)),

            modifier = Modifier

                .fillMaxWidth(0.92f)

                .widthIn(max = 480.dp)

                .wrapContentHeight()

                .padding(vertical = 16.dp)

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

                        fontFamily = CleanFont,

                        fontSize = 15.sp,

                        color = Color(theme.primaryColor)

                    )

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {

                        Text(text = "✕", color = Color(theme.textColor), fontFamily = CleanFont, fontSize = 16.sp, fontWeight = FontWeight.Bold)

                    }

                }



                Text(

                    text = if (isTagalog) "Pumili ng Soundbank o piliin ang Awtomatiko para mag-adjust batay sa genre:" else "Select soundbank or Auto to match song genre:",

                    fontSize = 11.sp,

                    fontFamily = CleanFont,

                    color = Color(theme.textMutedColor),

                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)

                )



                LazyColumn(

                    modifier = Modifier

                        .fillMaxWidth()

                        .heightIn(max = 340.dp),

                    verticalArrangement = Arrangement.spacedBy(8.dp)

                ) {

                    if (soundFonts.isEmpty()) {

                        item {

                            Text(

                                text = if (isTagalog) "Walang nahanap na SoundFont" else "No SoundFonts found",

                                color = Color(theme.textMutedColor),

                                fontSize = 12.sp,

                                fontFamily = CleanFont,

                                modifier = Modifier.padding(16.dp)

                            )

                        }

                    } else {

                        items(soundFonts) { sf ->
                            val isSelected = sf.equals(activeSoundFont, ignoreCase = true) ||
                                    activeSoundFont.contains(sf, ignoreCase = true) ||
                                    sf.contains(activeSoundFont, ignoreCase = true)
                            val isAuto = sf.contains("auto", ignoreCase = true) || sf.contains("awtomatiko", ignoreCase = true)

                            // Built-in / default soundfonts NEVER have a delete button
                            val isDefaultSoundFont = isAuto ||
                                    sf.contains("SAM2634", ignoreCase = true) ||
                                    sf.contains("Dream", ignoreCase = true) ||
                                    sf.contains("MegaPro", ignoreCase = true) ||
                                    sf.contains("Yamaha", ignoreCase = true) ||
                                    sf.contains("Minus-One", ignoreCase = true)

                            // Only VIP preset soundfonts or user imported soundfonts can be deleted
                            // Only user-created presets or imported .sf2 files have a delete button
                            val isUserCreatedPreset = sf.startsWith("[VIP Preset]", ignoreCase = true) || sf.contains("[VIP Preset]", ignoreCase = true)
                            val canDelete = (isUserCreatedPreset || sf.endsWith(".sf2", ignoreCase = true)) && onDeleteSoundFont != null

                            val cleanSfName = when {
                                isAuto -> if (isTagalog) "Awtomatiko" else "Automatic"
                                else -> sf.replace(Regex("^[✨🎵🎸🎹🪩🥁🎛️🎼\\s]+"), "").trim()
                            }

                            val sfIcon = when {
                                isAuto -> "✨"
                                sf.contains("Rock") -> "🎸"
                                sf.contains("Acoustic") || sf.contains("Ballad") -> "🎹"
                                sf.contains("Disco") || sf.contains("Synth") -> "🪩"
                                sf.contains("Party") -> "🥁"
                                sf.contains("Cha-Cha") || sf.contains("Ballroom") -> "🎺"
                                sf.contains("Pop") || sf.contains("Minus") -> "🎵"
                                sf.contains("Yamaha") || sf.contains("Mega") || sf.contains("SAM") -> "🎛️"
                                else -> "🎼"
                            }

                            val sfDesc = when {
                                isAuto -> {
                                    if (isSelected && autoMatchedSoundFont.isNotBlank()) {
                                        val cleanAutoMatched = autoMatchedSoundFont.replace(Regex("^[✨🎵🎸🎹🪩🥁🎛️🎼\\s]+"), "").trim()
                                        if (isTagalog) "▶ Aktibo: $cleanAutoMatched" else "▶ Active: $cleanAutoMatched"
                                    } else {
                                        if (isTagalog) "Kusang nag-a-adjust sa genre ng kanta" else "Auto-adapts to song genre"
                                    }
                                }
                                sf.contains("Pop") -> if (isTagalog) "Balanced studio minus-one para sa OPM at Pop hits" else "Balanced studio minus-one for OPM & Pop"
                                sf.contains("Rock") -> if (isTagalog) "Live rock band: malutong na guitar at driving bass" else "Live rock band: crunchy guitars & driving bass"
                                sf.contains("Acoustic") || sf.contains("Ballad") -> if (isTagalog) "Concert grand piano, acoustic guitar, at strings" else "Concert grand piano, acoustic guitar & strings"
                                sf.contains("Disco") -> if (isTagalog) "Classic videoke synthesizer at dance beat" else "Classic videoke synth & dance beat"
                                sf.contains("Cha-Cha") -> if (isTagalog) "Ballroom brass, latin percussion, at rhythm" else "Ballroom brass, latin percussion & rhythm"
                                sf.contains("Party") -> if (isTagalog) "Dynamic brass section at punchy sub-bass" else "Dynamic brass & punchy sub-bass"
                                sf.contains("Yamaha") -> if (isTagalog) "Multi-Sampled Live Acoustic & Electric Band" else "Multi-Sampled Live Band"
                                sf.contains("SAM2634") -> if (isTagalog) "Dream Hardware Synthesizer Soundchip" else "Dream Hardware Synthesizer Soundchip"
                                sf.contains("MegaPro") -> if (isTagalog) "MegaPro Karaoke Player SoundFont" else "MegaPro Karaoke Player SoundFont"
                                else -> ""
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) Color(theme.primaryColor).copy(alpha = 0.16f) else Color(theme.buttonBgColor))
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) Color(theme.primaryColor) else Color(theme.buttonBorderColor),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        if (isAuto && !isVipActivated) {
                                            onRequireVip()
                                        } else {
                                            onSelectSoundFont(sf)
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(theme.primaryColor).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = sfIcon, fontSize = 15.sp)
                                }

                                Spacer(modifier = Modifier.width(9.dp))

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = cleanSfName,
                                            color = if (isSelected) Color(theme.primaryColor) else Color(theme.textColor),
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                            fontSize = 12.sp,
                                            fontFamily = CleanFont,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (isAuto) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFFEAB308),
                                                modifier = Modifier.wrapContentSize()
                                            ) {
                                                Text(
                                                    text = "👑 VIP",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color(0xFF0F172A),
                                                    softWrap = false,
                                                    maxLines = 1,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (sfDesc.isNotBlank()) {
                                        Text(
                                            text = sfDesc,
                                            color = Color(theme.textMutedColor),
                                            fontSize = 9.5.sp,
                                            fontFamily = CleanFont,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(top = 1.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isSelected) {
                                        Surface(
                                            shape = RoundedCornerShape(5.dp),
                                            color = Color(theme.primaryColor),
                                            modifier = Modifier.wrapContentSize()
                                        ) {
                                            Text(
                                                text = if (isTagalog) "AKTIBO ✓" else "ACTIVE ✓",
                                                color = doneBtnTextClr,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 8.5.sp,
                                                fontFamily = CleanFont,
                                                softWrap = false,
                                                maxLines = 1,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else if (isAuto && !isVipActivated) {
                                        Surface(
                                            shape = RoundedCornerShape(5.dp),
                                            color = Color(0xFFEAB308).copy(alpha = 0.15f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEAB308)),
                                            modifier = Modifier.wrapContentSize()
                                        ) {
                                            Text(
                                                text = if (isTagalog) "🔒 I-UNLOCK" else "🔒 UNLOCK",
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = CleanFont,
                                                color = Color(0xFFEAB308),
                                                softWrap = false,
                                                maxLines = 1,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    if (canDelete) {
                                        var showDeleteConfirm by remember { mutableStateOf(false) }
                                        IconButton(
                                            onClick = { showDeleteConfirm = true },
                                            modifier = Modifier.size(26.dp).focusable()
                                        ) {
                                            Text(text = "🗑️", fontSize = 12.sp)
                                        }
                                        if (showDeleteConfirm) {
                                            AlertDialog(
                                                onDismissRequest = { showDeleteConfirm = false },
                                                title = {
                                                    Text(
                                                        text = if (isTagalog) "I-delete ang SoundFont?" else "Delete SoundFont?",
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(theme.textColor)
                                                    )
                                                },
                                                text = {
                                                    Text(
                                                        text = if (isTagalog) "Sigurado ka bang nais mong tanggalin ang '$cleanSfName' mula sa storage?"
                                                               else "Are you sure you want to delete '$cleanSfName' from storage?",
                                                        color = Color(theme.textMutedColor)
                                                    )
                                                },
                                                confirmButton = {
                                                    Button(
                                                        onClick = {
                                                            showDeleteConfirm = false
                                                            onDeleteSoundFont?.invoke(sf)
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                                                    ) {
                                                        Text(text = if (isTagalog) "I-delete" else "Delete", color = Color.White, fontWeight = FontWeight.Bold)
                                                    }
                                                },
                                                dismissButton = {
                                                    TextButton(onClick = { showDeleteConfirm = false }) {
                                                        Text(text = if (isTagalog) "Kanselahin" else "Cancel", color = Color(theme.textColor))
                                                    }
                                                },
                                                containerColor = Color(theme.surfaceColor),
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }



                Spacer(modifier = Modifier.height(14.dp))



                Row(

                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement = Arrangement.spacedBy(10.dp)

                ) {

                    Button(

                        onClick = onScanStorage,

                        colors = ButtonDefaults.buttonColors(containerColor = Color(theme.buttonBgColor)),

                        shape = RoundedCornerShape(10.dp),

                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(theme.buttonBorderColor)),

                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),

                        modifier = Modifier.weight(1f).height(42.dp)

                    ) {

                        Text(

                            text = if (isTagalog) "🔍 I-scan Storage" else "🔍 Scan Storage",

                            fontSize = 11.5.sp,

                            fontWeight = FontWeight.Bold,

                            fontFamily = CleanFont,

                            maxLines = 1,

                            color = Color(theme.textColor)

                        )

                    }



                    Button(

                        onClick = onDismiss,

                        colors = ButtonDefaults.buttonColors(containerColor = Color(theme.primaryColor)),

                        shape = RoundedCornerShape(10.dp),

                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),

                        modifier = Modifier.weight(1f).height(42.dp)

                    ) {

                        Text(

                            text = if (isTagalog) "TAPOS NA ✓" else "DONE ✓",

                            fontSize = 12.sp,

                            fontWeight = FontWeight.Black,

                            fontFamily = CleanFont,

                            maxLines = 1,

                            color = doneBtnTextClr

                        )

                    }

                }

            }

        }

    }

}







/**



 * Live Camera Background Feed for Authentic KTV Studio Experience:



 * Displays real-time camera preview (front/back camera) behind the lyrics.



 */



@Composable



fun LiveCameraBackground(
    isFrontFacing: Boolean = true,
    onToggleFacing: () -> Unit = {},
    modifier: Modifier = Modifier
) {



    val context = LocalContext.current



    var hasPermission by remember {



        mutableStateOf(



            androidx.core.content.ContextCompat.checkSelfPermission(



                context,



                android.Manifest.permission.CAMERA



            ) == android.content.pm.PackageManager.PERMISSION_GRANTED



        )



    }







    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(



        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()



    ) { granted ->



        hasPermission = granted



    }







    if (!hasPermission) {



        Box(



            modifier = modifier



                .fillMaxSize()



                .background(Color(0xFF0F172A)),



            contentAlignment = Alignment.Center



        ) {



            Column(



                horizontalAlignment = Alignment.CenterHorizontally,



                verticalArrangement = Arrangement.spacedBy(10.dp),



                modifier = Modifier.padding(20.dp)



            ) {



                Text(text = "📷", fontSize = 42.sp)



                Text(



                    text = "Live Camera Background",



                    color = Color.White,



                    fontWeight = FontWeight.Bold,



                    fontSize = 16.sp



                )



                Text(



                    text = "Kailangan ng Camera permission upang makita ang sarili habang kumakanta.",



                    color = Color(0xFF94A3B8),



                    fontSize = 12.sp,



                    textAlign = TextAlign.Center



                )



                Button(



                    onClick = { permissionLauncher.launch(android.Manifest.permission.CAMERA) },



                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),



                    shape = RoundedCornerShape(10.dp)



                ) {



                    Text(text = "📸 Buksan ang Camera", fontWeight = FontWeight.Bold, color = Color.White)



                }



            }



        }



    } else {



        var cameraInstance by remember { mutableStateOf<android.hardware.Camera?>(null) }



        val activity = context as? android.app.Activity







        DisposableEffect(isFrontFacing) {
            onDispose {
                val camToRelease = cameraInstance
                cameraInstance = null
                if (camToRelease != null) {
                    Thread {
                        try {
                            camToRelease.stopPreview()
                            camToRelease.release()
                        } catch (_: Exception) {}
                    }.start()
                }
            }
        }







        Box(modifier = modifier.fillMaxSize()) {



            key(isFrontFacing) {
            AndroidView(



                factory = { ctx ->



                    TextureView(ctx).apply {



                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {



                            var currentCamWidth = 1280



                            var currentCamHeight = 720



                            var isPortraitOrientation = false







                            fun applyNaturalAspectTransform(viewW: Int, viewH: Int) {



                                if (viewW == 0 || viewH == 0 || currentCamWidth == 0 || currentCamHeight == 0) return



                                val matrix = android.graphics.Matrix()







                                val effectiveW = if (isPortraitOrientation) currentCamHeight else currentCamWidth



                                val effectiveH = if (isPortraitOrientation) currentCamWidth else currentCamHeight







                                val viewRatio = viewW.toFloat() / viewH.toFloat()



                                val videoRatio = effectiveW.toFloat() / effectiveH.toFloat()







                                var scaleX = 1.0f



                                var scaleY = 1.0f







                                // Center crop: Preserve natural proportions without stretching or letterboxing



                                if (viewRatio > videoRatio) {



                                    scaleY = (viewW.toFloat() / effectiveW.toFloat()) / (viewH.toFloat() / effectiveH.toFloat())



                                } else {



                                    scaleX = (viewH.toFloat() / effectiveH.toFloat()) / (viewW.toFloat() / effectiveW.toFloat())



                                }







                                val pivotX = viewW / 2.0f



                                val pivotY = viewH / 2.0f



                                matrix.setScale(scaleX, scaleY, pivotX, pivotY)







                                // Mirror horizontally for front-facing camera like a natural mirror



                                if (isFrontFacing) {



                                    matrix.postScale(-1.0f, 1.0f, pivotX, pivotY)



                                }







                                setTransform(matrix)



                            }







                            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {



                                try {



                                    val count = android.hardware.Camera.getNumberOfCameras()



                                    var targetCamId = 0



                                    val camInfo = android.hardware.Camera.CameraInfo()



                                    for (i in 0 until count) {



                                        android.hardware.Camera.getCameraInfo(i, camInfo)



                                        if (isFrontFacing && camInfo.facing == android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT) {



                                            targetCamId = i



                                            break



                                        } else if (!isFrontFacing && camInfo.facing == android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK) {



                                            targetCamId = i



                                            break



                                        }



                                    }







                                    val cam = android.hardware.Camera.open(targetCamId)







                                    // 1. Calculate proper display orientation based on device rotation



                                    val rotation = activity?.windowManager?.defaultDisplay?.rotation ?: android.view.Surface.ROTATION_0



                                    var degrees = 0



                                    when (rotation) {



                                        android.view.Surface.ROTATION_0 -> degrees = 0



                                        android.view.Surface.ROTATION_90 -> degrees = 90



                                        android.view.Surface.ROTATION_180 -> degrees = 180



                                        android.view.Surface.ROTATION_270 -> degrees = 270



                                    }







                                    val displayOrientation = if (camInfo.facing == android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT) {



                                        val result = (camInfo.orientation + degrees) % 360



                                        (360 - result) % 360



                                    } else {



                                        (camInfo.orientation - degrees + 360) % 360



                                    }



                                    cam.setDisplayOrientation(displayOrientation)



                                    isPortraitOrientation = (displayOrientation == 90 || displayOrientation == 270)







                                    // 2. Select best preview size that closely matches view aspect ratio



                                    val params = cam.parameters



                                    val supportedSizes = params.supportedPreviewSizes



                                    if (!supportedSizes.isNullOrEmpty()) {



                                        val targetAspect = if (isPortraitOrientation) {



                                            height.toFloat() / width.toFloat()



                                        } else {



                                            width.toFloat() / height.toFloat()



                                        }







                                        val bestSize = supportedSizes.minByOrNull { size ->



                                            val r = size.width.toFloat() / size.height.toFloat()



                                            val aspectDiff = Math.abs(r - targetAspect)



                                            val prefPenalty = if (size.width in 1280..1920) 0 else 500



                                            aspectDiff * 1000 + prefPenalty



                                        } ?: supportedSizes.first()







                                        params.setPreviewSize(bestSize.width, bestSize.height)



                                        currentCamWidth = bestSize.width



                                        currentCamHeight = bestSize.height



                                    }







                                    // 3. Continuous video focus if available



                                    val focusModes = params.supportedFocusModes



                                    if (focusModes?.contains(android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO) == true) {



                                        params.focusMode = android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO



                                    }







                                    cam.parameters = params



                                    cam.setPreviewTexture(surface)



                                    cam.startPreview()



                                    cameraInstance = cam







                                    // 4. Apply natural non-stretch transform



                                    post {



                                        applyNaturalAspectTransform(width, height)



                                    }



                                } catch (e: Exception) {



                                    Log.w("CameraBg", "Failed to start camera preview: ${e.message}")



                                }



                            }







                            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {



                                applyNaturalAspectTransform(width, height)



                            }







                            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                                val camToRelease = cameraInstance
                                cameraInstance = null
                                if (camToRelease != null) {
                                    Thread {
                                        try {
                                            camToRelease.stopPreview()
                                            camToRelease.release()
                                        } catch (_: Exception) {}
                                    }.start()
                                }
                                return true
                            }







                            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}



                        }



                    }



                },



                modifier = Modifier.fillMaxSize()



            )
            }







            // Camera flip switch (Front / Back camera)



            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .clickable { onToggleFacing() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = "🔄", fontSize = 13.sp)
                Text(
                    text = if (isFrontFacing) "Harap (Front)" else "Likod (Back)",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }



        }



    }



}







/**



 * Background Layer: Renders MP4 video, static image, live camera, or animated neon stage



 */



@Composable



fun VideokeBackgroundLayer(



    background: KaraokeBackground?,



    hasMultipleMedia: Boolean = true,



    videoSongPath: String? = null,



    isVideoSong: Boolean = false,



    isPlaying: Boolean = false,



    isPlaybackPaused: Boolean = false,



    bgVisualMode: BackgroundVisualMode = BackgroundVisualMode.VIDEOS,
    isCameraFrontFacing: Boolean = false,
    onToggleCameraFacing: () -> Unit = {},
    onBackgroundCompleted: () -> Unit = {},



    modifier: Modifier = Modifier



) {



    val currentOnCompleted by rememberUpdatedState(onBackgroundCompleted)







    Box(modifier = modifier.fillMaxSize()) {



        when {



            isPlaying && isVideoSong && !videoSongPath.isNullOrBlank() && File(videoSongPath).exists() -> {



                KaraokeVideoPlayer(



                    videoPath = videoSongPath,



                    hasMultipleMedia = false,



                    isMuted = false,



                    isVideoSong = true,



                    isPlaybackPaused = isPlaybackPaused,



                    onVideoCompleted = currentOnCompleted



                )



            }



            !isVideoSong && bgVisualMode == BackgroundVisualMode.CAMERA -> {
                LiveCameraBackground(
                    isFrontFacing = isCameraFrontFacing,
                    onToggleFacing = onToggleCameraFacing,
                    modifier = Modifier.fillMaxSize()
                )
            }



            background?.type == BackgroundType.VIDEO && background.path.isNotEmpty() && File(background.path).exists() -> {



                KaraokeVideoPlayer(



                    videoPath = background.path,



                    hasMultipleMedia = hasMultipleMedia,



                    isMuted = true,



                    isVideoSong = false,



                    isPlaybackPaused = isPlaybackPaused,



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







// Removed dark vignette scrim to keep background video 100% bright and vibrant



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



    isMuted: Boolean = true,



    isVideoSong: Boolean = false,



    isPlaybackPaused: Boolean = false,



    onVideoCompleted: () -> Unit = {},



    modifier: Modifier = Modifier



) {



    val context = androidx.compose.ui.platform.LocalContext.current



    val currentOnVideoCompleted by rememberUpdatedState(onVideoCompleted)



    var activeSurface by remember { mutableStateOf<Surface?>(null) }



    var textureViewRef by remember { mutableStateOf<TextureView?>(null) }



    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }



    var isPlayerPrepared by remember { mutableStateOf(false) }



    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current



    val coroutineScope = rememberCoroutineScope()







    // Dynamically update audio volume whenever isMuted changes



    // Pause and Resume control

    LaunchedEffect(isPlaybackPaused, isPlayerPrepared) {

        mediaPlayerRef?.let { mp ->

            try {

                if (isPlaybackPaused) {

                    if (isPlayerPrepared && mp.isPlaying) mp.pause()

                } else {

                    if (isPlayerPrepared && !mp.isPlaying && videoPath.isNotBlank() && !isPlaybackPaused) {

                        mp.start()

                    }

                }

            } catch (e: Exception) {

                Log.w("VideoPlayer", "Pause state update warning", e)

            }

        }

    }



    LaunchedEffect(isMuted, isPlayerPrepared) {



        mediaPlayerRef?.let { mp ->



            try {



                if (isMuted) {



                    mp.setVolume(0f, 0f)



                } else {



                    mp.setVolume(1f, 1f)



                }



            } catch (e: Exception) {



                Log.w("VideoPlayer", "Volume update warning", e)



            }



        }



    }







    DisposableEffect(lifecycleOwner, mediaPlayerRef) {



        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->



            val mp = mediaPlayerRef



            if (mp != null) {



                try {



                    when (event) {



                        androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> {



                            if (isPlayerPrepared && mp.isPlaying) mp.pause()



                        }



                        androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {



                            if (isPlayerPrepared && !mp.isPlaying && videoPath.isNotBlank()) {



                                mp.start()



                                if (isMuted) {



                                    mp.setVolume(0.0f, 0.0f)



                                }



                            }



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







    fun adjustAspectMatrix(view: TextureView?, viewW: Int, viewH: Int, vidW: Int, vidH: Int) {



        val tv = view ?: return



        if (viewW <= 0 || viewH <= 0 || vidW <= 0 || vidH <= 0) return



        val scaleX = viewW.toFloat() / vidW.toFloat()



        val scaleY = viewH.toFloat() / vidH.toFloat()



        // In landscape (or on landscape screens viewW > viewH), use maxOf(scaleX, scaleY) so there are NO black bars on sides!

        val isLandscapeScreen = viewW > viewH



        val fillScale = if (isVideoSong) {

            if (isLandscapeScreen) maxOf(scaleX, scaleY) else minOf(scaleX, scaleY)

        } else {

            maxOf(scaleX, scaleY)

        }



        val scaledW = vidW.toFloat() * fillScale



        val scaledH = vidH.toFloat() * fillScale



        val matrix = android.graphics.Matrix()



        matrix.setScale(scaledW / viewW.toFloat(), scaledH / viewH.toFloat(), viewW / 2f, viewH / 2f)



        tv.setTransform(matrix)



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







        // Request AudioFocus for clear, unmuted media playback



        val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager



        if (!isMuted) {



            try {



                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {



                    val focusRequest = android.media.AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN)



                        .setAudioAttributes(



                            android.media.AudioAttributes.Builder()



                                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)



                                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)



                                .build()



                        )



                        .build()



                    audioManager?.requestAudioFocus(focusRequest)



                } else {



                    @Suppress("DEPRECATION")



                    audioManager?.requestAudioFocus(null, android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.AUDIOFOCUS_GAIN)



                }



            } catch (e: Exception) {



                Log.w("VideoPlayer", "Audio focus request warning", e)



            }



        }







        isPlayerPrepared = false



        val mp = MediaPlayer().apply {



            try {



                if (!isMuted) {



                    setAudioAttributes(



                        android.media.AudioAttributes.Builder()



                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)



                            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)



                            .build()



                    )



                }



                setDataSource(file.absolutePath)



                setSurface(surface)



                setVolume(if (isMuted) 0f else 1f, if (isMuted) 0f else 1f)



                isLooping = if (!isMuted) false else !hasMultipleMedia







                setOnVideoSizeChangedListener { _, vWidth, vHeight ->



                    textureViewRef?.let { tv ->



                        adjustAspectMatrix(tv, tv.width, tv.height, vWidth, vHeight)



                    }



                }



                setOnCompletionListener {



                    Log.d("VideoPlayer", "Video playback completed: $videoPath")



                    currentOnVideoCompleted()



                }



                setOnErrorListener { _, what, extra ->



                    Log.w("VideoPlayer", "MediaPlayer playback error: $what, extra: $extra")



                    isPlayerPrepared = false



                    if (what != -38) {



                        currentOnVideoCompleted()



                    }



                    true



                }



                setOnSeekCompleteListener { seekedMp ->



                    if (isMuted) {



                        try { seekedMp.setVolume(0.0f, 0.0f) } catch (ignored: Exception) {}



                    }



                }



                setOnPreparedListener { preparedMp ->



                    isPlayerPrepared = true



                    if (isMuted) {



                        // 1. Programmatically deselect all audio tracks so Android decoders never process or route audio



                        try {



                            val trackInfo = preparedMp.trackInfo



                            for (i in trackInfo.indices) {



                                if (trackInfo[i].trackType == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO) {



                                    try {



                                        preparedMp.deselectTrack(i)



                                        Log.i("VideoPlayer", "Deselected audio track $i on background video: $videoPath")



                                    } catch (e: Exception) {



                                        Log.w("VideoPlayer", "Could not deselect audio track $i: ${e.message}")



                                    }



                                }



                            }



                        } catch (e: Exception) {



                            Log.w("VideoPlayer", "Error getting trackInfo: ${e.message}")



                        }



                        try {



                            preparedMp.setVolume(0.0f, 0.0f)



                        } catch (e: Exception) {



                            Log.w("VideoPlayer", "Volume setting warning", e)



                        }



                    } else {



                        try {



                            preparedMp.setVolume(1.0f, 1.0f)



                        } catch (e: Exception) {



                            Log.w("VideoPlayer", "Volume setting warning", e)



                        }



                    }



                    try {



                        preparedMp.setVideoScalingMode(



                            if (isVideoSong) MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT



                            else MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING



                        )



                    } catch (e: Exception) {



                        Log.w("VideoPlayer", "Scaling mode warning", e)



                    }



                    textureViewRef?.let { tv ->



                        adjustAspectMatrix(tv, tv.width, tv.height, preparedMp.videoWidth, preparedMp.videoHeight)



                    }



                    try {



                        preparedMp.start()



                        if (isMuted) {



                            preparedMp.setVolume(0.0f, 0.0f)



                            // Repeatedly clamp volume to 0 to prevent async hardware AudioSink volume reset



                            coroutineScope.launch {



                                for (d in listOf(30L, 70L, 120L, 250L, 500L, 1000L)) {



                                    kotlinx.coroutines.delay(d)



                                    try {



                                        if (isMuted) {



                                            preparedMp.setVolume(0.0f, 0.0f)



                                        }



                                    } catch (ignored: Exception) {}



                                }



                            }



                        }



                    } catch (e: Exception) {



                        Log.w("VideoPlayer", "Error starting prepared video", e)



                    }



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



                        mediaPlayerRef?.let { mp ->



                            if (isPlayerPrepared && mp.videoWidth > 0 && mp.videoHeight > 0) {



                                adjustAspectMatrix(this@apply, width, height, mp.videoWidth, mp.videoHeight)



                            }



                        }



                    }







                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {



                        textureViewRef = this@apply



                        mediaPlayerRef?.let { mp ->



                            if (isPlayerPrepared && mp.videoWidth > 0 && mp.videoHeight > 0) {



                                adjustAspectMatrix(this@apply, width, height, mp.videoWidth, mp.videoHeight)



                            }



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



                    mediaPlayerRef?.let { mp ->



                        if (isPlayerPrepared && mp.videoWidth > 0 && mp.videoHeight > 0 && width > 0 && height > 0) {



                            adjustAspectMatrix(this, width, height, mp.videoWidth, mp.videoHeight)



                        }



                    }



                }



            }



        },



        update = { tv ->



            mediaPlayerRef?.let { mp ->



                if (isPlayerPrepared && mp.videoWidth > 0 && mp.videoHeight > 0 && tv.width > 0 && tv.height > 0) {



                    adjustAspectMatrix(tv, tv.width, tv.height, mp.videoWidth, mp.videoHeight)



                }



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



    bgVisualMode: BackgroundVisualMode = BackgroundVisualMode.VIDEOS,



    continuousVideoChangePerSong: Boolean = false,



    isTagalog: Boolean = false,



    theme: com.example.videokey.ui.main.AppTheme = com.example.videokey.ui.main.AppTheme.CYBER_NEON,



    onOpenFolderLocator: () -> Unit,



    onSelectBackground: (KaraokeBackground) -> Unit,



    onChangeMode: (BackgroundChangeMode) -> Unit,



    isCameraFrontFacing: Boolean = false,
    onSetCameraFrontFacing: (Boolean) -> Unit = {},
    onSelectBgVisualMode: (BackgroundVisualMode) -> Unit = {},



    onToggleContinuousVideoChangePerSong: (Boolean) -> Unit = {},



    onRescanMedia: () -> Unit,



    onDeleteBackground: (KaraokeBackground) -> Unit = {},



    onDismiss: () -> Unit



) {



    var selectedTab by remember { mutableStateOf(0) } // 0: All, 1: Videos, 2: Images, 3: Stage



    val filtered = when (selectedTab) {



        1 -> backgrounds.filter { it.type == BackgroundType.VIDEO }



        2 -> backgrounds.filter { it.type == BackgroundType.IMAGE }



        3 -> backgrounds.filter { it.type == BackgroundType.STAGE }



        else -> backgrounds



    }



    val btnTextClr = getContrastingTextColor(theme.primaryColor)







    Dialog(



        onDismissRequest = onDismiss,



        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)



    ) {



        Surface(



            shape = RoundedCornerShape(18.dp),



            color = Color(theme.surfaceColor),



            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(theme.primaryColor)),



            modifier = Modifier



                .fillMaxWidth(0.95f)



                .fillMaxHeight(0.85f)



                .padding(4.dp)



        ) {



            Column(modifier = Modifier.padding(14.dp)) {



                // Header



                Row(



                    modifier = Modifier.fillMaxWidth(),



                    horizontalArrangement = Arrangement.SpaceBetween,



                    verticalAlignment = Alignment.CenterVertically



                ) {



                    Text(



                        text = if (isTagalog) "🎬 BACKGROUND NG VIDEOKEY" else "🎬 VIDEOKEY BACKGROUND",



                        fontWeight = FontWeight.Black,



                        fontFamily = CleanFont,



                        color = Color(theme.primaryColor),



                        fontSize = 13.5.sp



                    )



                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {



                        Text(text = "✕", color = Color(theme.textColor), fontFamily = CleanFont, fontSize = 16.sp, fontWeight = FontWeight.Bold)



                    }



                }







                // BG MODE: Videos, Photos, Camera



                Text(



                    text = if (isTagalog) "📹 BG MODE (URI NG BACKGROUND):" else "📹 BACKGROUND MODE:",



                    fontSize = 10.sp,



                    fontFamily = CleanFont,



                    color = Color(theme.textMutedColor),



                    fontWeight = FontWeight.Bold,



                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)



                )







                Row(



                    modifier = Modifier.fillMaxWidth(),



                    horizontalArrangement = Arrangement.spacedBy(6.dp)



                ) {



                    BackgroundVisualMode.values().forEach { mode ->



                        val isModeSel = bgVisualMode == mode



                        val icon = when (mode) {



                            BackgroundVisualMode.VIDEOS -> "🎬"



                            BackgroundVisualMode.PHOTOS -> "🖼️"



                            BackgroundVisualMode.CAMERA -> "📷"



                        }



                        Box(



                            modifier = Modifier



                                .weight(1f)



                                .clip(RoundedCornerShape(8.dp))



                                .background(if (isModeSel) Color(theme.primaryColor) else Color(theme.buttonBgColor))



                                .border(1.dp, if (isModeSel) Color(theme.primaryColor) else Color(theme.buttonBorderColor), RoundedCornerShape(8.dp))



                                .clickable { onSelectBgVisualMode(mode) }



                                .padding(vertical = 7.dp),



                            contentAlignment = Alignment.Center



                        ) {



                            Text(
                                text = "$icon ${mode.label(isTagalog)}",
                                color = if (isModeSel) btnTextClr else Color(theme.textColor),
                                fontWeight = FontWeight.Bold,
                                fontFamily = CleanFont,
                                fontSize = 10.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (bgVisualMode == BackgroundVisualMode.CAMERA) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isFront = isCameraFrontFacing
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isFront) Color(0xFF0284C7) else Color(theme.buttonBgColor))
                                .border(1.dp, if (isFront) Color(0xFF38BDF8) else Color(theme.buttonBorderColor), RoundedCornerShape(8.dp))
                                .clickable { onSetCameraFrontFacing(true) }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isTagalog) "🤳 Harap (Front Cam)" else "🤳 Front Camera",
                                color = if (isFront) Color.White else Color(theme.textColor),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!isFront) Color(0xFF0284C7) else Color(theme.buttonBgColor))
                                .border(1.dp, if (!isFront) Color(0xFF38BDF8) else Color(theme.buttonBorderColor), RoundedCornerShape(8.dp))
                                .clickable { onSetCameraFrontFacing(false) }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isTagalog) "📷 Likod (Back Cam)" else "📷 Back Camera",
                                color = if (!isFront) Color.White else Color(theme.textColor),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }







                Spacer(modifier = Modifier.height(4.dp))







                // Dedicated Single Background Folder Locator Card



                Card(



                    colors = CardDefaults.cardColors(containerColor = Color(theme.buttonBgColor)),



                    shape = RoundedCornerShape(10.dp),



                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(theme.buttonBorderColor)),



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



                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {



                            Text(



                                text = if (isTagalog) "📁 BACKGROUND FOLDER:" else "📁 BACKGROUND FOLDER:",



                                fontSize = 9.sp,



                                fontFamily = CleanFont,



                                color = Color(theme.primaryColor),



                                fontWeight = FontWeight.Black



                            )



                            Text(



                                text = currentBackgroundFolder,



                                fontSize = 11.sp,



                                color = Color(theme.textColor),



                                maxLines = 1,



                                overflow = TextOverflow.Ellipsis,



                                fontFamily = CleanFont,



                                fontWeight = FontWeight.Bold



                            )



                            Text(



                                text = if (isTagalog) "Nahanap: ${backgrounds.size} media files" else "Found: ${backgrounds.size} media files",



                                fontSize = 9.sp,



                                fontFamily = CleanFont,



                                color = Color(theme.textMutedColor),



                                maxLines = 1,



                                overflow = TextOverflow.Ellipsis



                            )



                        }



                        Button(



                            onClick = onOpenFolderLocator,



                            colors = ButtonDefaults.buttonColors(containerColor = Color(theme.primaryColor)),



                            shape = RoundedCornerShape(8.dp),



                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),



                            modifier = Modifier.height(28.dp)



                        ) {



                            Text(



                                text = if (isTagalog) "📂 Palitan" else "📂 Change",



                                fontSize = 10.sp,



                                fontFamily = CleanFont,



                                fontWeight = FontWeight.Bold,



                                color = btnTextClr



                            )



                        }



                    }



                }







                // Mode Selector: Auto-Change vs Continuous



                Text(



                    text = if (isTagalog) "PARAAN NG PAGPALIT (CHANGE MODE):" else "BACKGROUND CHANGE MODE:",



                    fontSize = 10.sp,



                    fontFamily = CleanFont,



                    color = Color(theme.textMutedColor),



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



                            .background(if (isAuto) Color(theme.primaryColor) else Color(theme.buttonBgColor))



                            .border(1.dp, if (isAuto) Color(theme.primaryColor) else Color(theme.buttonBorderColor), RoundedCornerShape(8.dp))



                            .clickable { onChangeMode(BackgroundChangeMode.AUTO_CHANGE_PER_SONG) }



                            .padding(vertical = 8.dp),



                        contentAlignment = Alignment.Center



                    ) {



                        Text(



                            text = if (isTagalog) "🔀 Bawat Kanta" else "🔀 Per Song",



                            color = if (isAuto) btnTextClr else Color(theme.textColor),



                            fontWeight = FontWeight.Bold,



                            fontFamily = CleanFont,



                            fontSize = 10.5.sp,



                            maxLines = 1,



                            overflow = TextOverflow.Ellipsis



                        )



                    }







                    // Continuous Pill



                    val isContinuous = changeMode == BackgroundChangeMode.CONTINUOUS



                    Box(



                        modifier = Modifier



                            .weight(1f)



                            .clip(RoundedCornerShape(8.dp))



                            .background(if (isContinuous) Color(theme.secondaryColor) else Color(theme.buttonBgColor))



                            .border(1.dp, if (isContinuous) Color(theme.secondaryColor) else Color(theme.buttonBorderColor), RoundedCornerShape(8.dp))



                            .clickable { onChangeMode(BackgroundChangeMode.CONTINUOUS) }



                            .padding(vertical = 8.dp),



                        contentAlignment = Alignment.Center



                    ) {



                        Text(



                            text = if (isTagalog) "🔁 Tuloy-tuloy" else "🔁 Continuous",



                            color = if (isContinuous) getContrastingTextColor(theme.secondaryColor) else Color(theme.textColor),



                            fontWeight = FontWeight.Bold,



                            fontFamily = CleanFont,



                            fontSize = 10.5.sp,



                            maxLines = 1,



                            overflow = TextOverflow.Ellipsis



                        )



                    }



                }







                // Continuous Video Sub-Option: Change clip on every new song ON / OFF



                Row(



                    modifier = Modifier



                        .fillMaxWidth()



                        .padding(top = 4.dp, bottom = 4.dp)



                        .clip(RoundedCornerShape(8.dp))



                        .background(Color(theme.buttonBgColor).copy(alpha = 0.5f))



                        .padding(horizontal = 8.dp, vertical = 4.dp),



                    horizontalArrangement = Arrangement.SpaceBetween,



                    verticalAlignment = Alignment.CenterVertically



                ) {



                    Column(modifier = Modifier.weight(1f)) {



                        Text(



                            text = if (isTagalog) "Palitan ang clip bawat bagong kanta" else "Change clip on every new song",



                            fontSize = 10.sp,



                            fontFamily = CleanFont,



                            fontWeight = FontWeight.Bold,



                            color = Color(theme.textColor)



                        )



                        Text(



                            text = if (continuousVideoChangePerSong)



                                (if (isTagalog) "BUKAS (Lilipat ng clip sa bawat kanta)" else "ON (Advances clip on new song)")



                            else



                                (if (isTagalog) "SARADO (Tuloy ang clip kahit magbago ang kanta)" else "OFF (Continuous playback across songs)"),



                            fontSize = 8.5.sp,



                            fontFamily = CleanFont,



                            color = if (continuousVideoChangePerSong) Color(0xFF00FF88) else Color(theme.textMutedColor)



                        )



                    }



                    Switch(



                        checked = continuousVideoChangePerSong,



                        onCheckedChange = { onToggleContinuousVideoChangePerSong(it) },



                        colors = SwitchDefaults.colors(



                            checkedThumbColor = Color(theme.primaryColor),



                            checkedTrackColor = Color(theme.primaryColor).copy(alpha = 0.35f)



                        )



                    )



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



                                .background(if (isSel) Color(theme.primaryColor) else Color(theme.buttonBgColor))



                                .border(1.dp, if (isSel) Color(theme.primaryColor) else Color(theme.buttonBorderColor), RoundedCornerShape(6.dp))



                                .clickable { selectedTab = idx }



                                .padding(vertical = 6.dp),



                            contentAlignment = Alignment.Center



                        ) {



                            Text(



                                text = tabTitle,



                                color = if (isSel) btnTextClr else Color(theme.textColor),



                                fontWeight = FontWeight.Bold,



                                fontFamily = CleanFont,



                                fontSize = 10.sp



                            )



                        }



                    }



                }







                Spacer(modifier = Modifier.height(6.dp))







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



                                .background(if (isSelected) Color(theme.primaryColor).copy(alpha = 0.22f) else Color(theme.buttonBgColor))



                                .border(



                                    width = if (isSelected) 1.5.dp else 1.dp,



                                    color = if (isSelected) Color(theme.primaryColor) else Color(theme.buttonBorderColor),



                                    shape = RoundedCornerShape(10.dp)



                                )



                                .clickable { onSelectBackground(bg) }



                                .padding(horizontal = 10.dp, vertical = 9.dp),



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



                                Column(modifier = Modifier.weight(1f)) {



                                    Text(



                                        text = bg.name,



                                        color = if (isSelected) Color(theme.primaryColor) else Color(theme.textColor),



                                        fontWeight = FontWeight.Bold,



                                        fontFamily = CleanFont,



                                        fontSize = 12.sp,



                                        maxLines = 1,



                                        overflow = TextOverflow.Ellipsis



                                    )



                                    Text(



                                        text = when (bg.type) {



                                            BackgroundType.VIDEO -> if (isTagalog) "Video (MP4 Loop)" else "Video (MP4 Loop)"



                                            BackgroundType.IMAGE -> if (isTagalog) "Larawan (Wallpaper)" else "Picture (Wallpaper)"



                                            BackgroundType.STAGE -> if (isTagalog) "Stage Neon Canvas" else "Animated Stage Canvas"



                                        },



                                        color = Color(theme.textMutedColor),



                                        fontFamily = CleanFont,



                                        fontSize = 9.sp,



                                        maxLines = 1,



                                        overflow = TextOverflow.Ellipsis



                                    )



                                }



                            }







                            Row(

                                verticalAlignment = Alignment.CenterVertically,

                                horizontalArrangement = Arrangement.spacedBy(6.dp)

                            ) {

                                if (isSelected) {

                                    Text(

                                        text = if (isTagalog) "✓ GAMIT" else "✓ ACTIVE",

                                        color = Color(theme.primaryColor),

                                        fontWeight = FontWeight.Black,

                                        fontFamily = CleanFont,

                                        fontSize = 11.sp

                                    )

                                }

                                if (bg.path.isNotBlank() && java.io.File(bg.path).exists()) {

                                    IconButton(

                                        onClick = { onDeleteBackground(bg) },

                                        modifier = Modifier.size(28.dp)

                                    ) {

                                        Text("🗑️", fontSize = 14.sp)

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







/**



 * Lyric Font Style & Visual Customization Modal Dialog



 */



@Composable



fun LyricStyleDialog(



    config: LyricCustomization,



    isTagalog: Boolean = false,



    isLandscape: Boolean = false,



    theme: com.example.videokey.ui.main.AppTheme = com.example.videokey.ui.main.AppTheme.CYBER_NEON,



    onSelectFontFamily: (LyricFontFamily) -> Unit,



    onSelectWipeColor: (LyricWipeColor) -> Unit,



    onSelectShadowStyle: (LyricShadowStyle) -> Unit,



    onSelectOutlineColor: (LyricOutlineColor) -> Unit = {},



    onToggleAllCaps: () -> Unit,



    onToggleItalic: () -> Unit,



    onAdjustFontScale: (Float) -> Unit,



    onAdjustOutlineThickness: (Float) -> Unit = {},



    onAdjustFontSize: (Float, Float) -> Unit = { _, _ -> },



    onAdjustLineSpacing: (Float) -> Unit = {},



    onToggleDropShadow: () -> Unit = {},



    onSelectLineCount: (LyricLineCount) -> Unit,



    onSelectPosition: (LyricScreenPosition) -> Unit,



    onSelectCountdownMode: (LyricCountdownMode) -> Unit = {},

    onSelectCountdownPhoto: (String) -> Unit = {},

    onSetCountdownStepImage: (Int, String?) -> Unit = { _, _ -> },

    onSetCountdownAllImages: (String) -> Unit = {},

    onClearAllCountdownImages: () -> Unit = {},

    onAdjustTimingOffset: (Int) -> Unit = {},



    onApplyPreset: (String) -> Unit,



    onToggleWipeHighlight: () -> Unit = {},



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







    Dialog(



        onDismissRequest = onDismiss,



        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)



    ) {



        Surface(



            shape = RoundedCornerShape(18.dp),



            color = Color(theme.surfaceColor),



            border = androidx.compose.foundation.BorderStroke(2.dp, Color(theme.primaryColor)),



            modifier = Modifier



                .fillMaxWidth(if (isLandscape) 0.88f else 0.95f)



                .fillMaxHeight(if (isLandscape) 0.94f else 0.90f)



                .padding(4.dp)



        ) {



            Column(



                modifier = Modifier



                    .fillMaxSize()



                    .padding(if (isLandscape) 14.dp else 14.dp)



            ) {



                // Header Bar



                Row(



                    modifier = Modifier.fillMaxWidth(),



                    horizontalArrangement = Arrangement.SpaceBetween,



                    verticalAlignment = Alignment.CenterVertically



                ) {



                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {



                        Text(



                            text = if (isTagalog) "🔤 FONT, ESTILO AT POSISYON" else "🔤 LYRIC FONT, STYLE & POSITION",



                            fontWeight = FontWeight.Black,



                            fontFamily = CleanFont,



                            color = Color(theme.primaryColor),



                            fontSize = if (isLandscape) 14.sp else 12.sp,



                            maxLines = 1,



                            overflow = TextOverflow.Ellipsis



                        )



                        Surface(



                            shape = RoundedCornerShape(4.dp),



                            color = Color(theme.buttonBgColor)



                        ) {



                            Text(



                                text = if (isTagalog) "BAGUHIN" else "CUSTOMIZE",



                                fontSize = 8.sp,



                                fontWeight = FontWeight.Bold,



                                fontFamily = CleanFont,



                                color = Color(theme.primaryColor),



                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)



                            )



                        }



                    }



                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {



                        Text(text = "✕", color = Color(theme.textColor), fontFamily = CleanFont, fontSize = 16.sp, fontWeight = FontWeight.Bold)



                    }



                }







                Spacer(modifier = Modifier.height(4.dp))



                // VIP Wipe Highlight Toggle (Always visible in both Portrait & Landscape!)

                Row(

                    modifier = Modifier

                        .fillMaxWidth()

                        .clip(RoundedCornerShape(10.dp))

                        .background(if (config.isWipeHighlightEnabled) Color(0xFFEAB308).copy(alpha = 0.16f) else Color(theme.buttonBgColor))

                        .border(1.2.dp, if (config.isWipeHighlightEnabled) Color(0xFFEAB308) else Color(theme.buttonBorderColor), RoundedCornerShape(10.dp))

                        .clickable { onToggleWipeHighlight() }

                        .padding(horizontal = 12.dp, vertical = 8.dp),

                    horizontalArrangement = Arrangement.SpaceBetween,

                    verticalAlignment = Alignment.CenterVertically

                ) {

                    Row(

                        modifier = Modifier.weight(1f).padding(end = 12.dp),

                        verticalAlignment = Alignment.CenterVertically,

                        horizontalArrangement = Arrangement.spacedBy(10.dp)

                    ) {

                        Text("👑", fontSize = 18.sp)

                        Column(modifier = Modifier.weight(1f)) {

                            Text(

                                text = if (isTagalog) "Liriko Highlight Wipe (VIP)" else "Lyric Highlight Wipe (VIP)",

                                color = Color.White,

                                fontWeight = FontWeight.Black,

                                fontSize = 12.sp,

                                maxLines = 1,

                                overflow = TextOverflow.Ellipsis

                            )

                            Text(

                                text = if (config.isWipeHighlightEnabled)

                                    (if (isTagalog) "BUKAS: May kulay na sweep wipe habang kumakanta" else "ON: Sweeping color wipe highlight while singing")

                                else

                                    (if (isTagalog) "SARADO: Solidong puti, walang sweep wipe" else "OFF: Solid clean text, no sweep wipe"),

                                color = if (config.isWipeHighlightEnabled) Color(0xFF00FF88) else Color(theme.textMutedColor),

                                fontSize = 10.sp,

                                maxLines = 1,

                                overflow = TextOverflow.Ellipsis

                            )

                        }

                    }

                    Switch(

                        checked = config.isWipeHighlightEnabled,

                        onCheckedChange = { onToggleWipeHighlight() },

                        colors = SwitchDefaults.colors(

                            checkedThumbColor = Color(0xFFEAB308),

                            checkedTrackColor = Color(0xFFEAB308).copy(alpha = 0.5f)

                        )

                    )

                }



                Spacer(modifier = Modifier.height(6.dp))



                if (isLandscape) {



                    // Two-Column Layout for TV / Landscape



                    Row(



                        modifier = Modifier



                            .weight(1f)



                            .fillMaxWidth(),



                        horizontalArrangement = Arrangement.spacedBy(14.dp)



                    ) {



                        // Left Column: Live TV Preview & Layout Options



                        Column(



                            modifier = Modifier



                                .weight(0.44f)



                                .fillMaxHeight(),



                            verticalArrangement = Arrangement.spacedBy(8.dp)



                        ) {



                            // Live Preview Box



                            LyricLivePreviewCard(config = config, previewWipe = previewWipe, isTagalog = isTagalog)







                            // Line Count & Position



                            LyricLineAndPositionControls(



                                config = config,



                                isTagalog = isTagalog,



                                theme = theme,



                                onSelectLineCount = onSelectLineCount,



                                onSelectPosition = onSelectPosition



                            )







                            Spacer(modifier = Modifier.weight(1f))







                            Button(



                                onClick = onDismiss,



                                colors = ButtonDefaults.buttonColors(containerColor = Color(theme.primaryColor)),



                                shape = RoundedCornerShape(10.dp),



                                modifier = Modifier



                                    .fillMaxWidth()



                                    .height(38.dp)



                            ) {



                                val btnTextColor = if (theme.primaryColor == 0xFF00E676L || theme.primaryColor == 0xFFFFD54FL || theme.primaryColor == 0xFFFFC107L) Color.Black else Color.White



                                Text(



                                    text = if (isTagalog) "ILAPAT AT ISARA ✓" else "APPLY & CLOSE ✓",



                                    fontWeight = FontWeight.Black,



                                    fontFamily = CleanFont,



                                    fontSize = 11.5.sp,



                                    color = btnTextColor,



                                    maxLines = 1



                                )



                            }



                        }







                        // Right Column: Scrollable controls



                        LyricControlsScrollable(



                            config = config,



                            isTagalog = isTagalog,



                            theme = theme,



                            onSelectFontFamily = onSelectFontFamily,



                            onSelectWipeColor = onSelectWipeColor,



                            onSelectShadowStyle = onSelectShadowStyle,



                            onSelectOutlineColor = onSelectOutlineColor,



                            onToggleAllCaps = onToggleAllCaps,



                            onToggleItalic = onToggleItalic,



                            onAdjustFontScale = onAdjustFontScale,



                            onAdjustOutlineThickness = onAdjustOutlineThickness,



                            onAdjustFontSize = onAdjustFontSize,



                            onAdjustLineSpacing = onAdjustLineSpacing,



                            onToggleDropShadow = onToggleDropShadow,



                            onSelectCountdownMode = onSelectCountdownMode,

                            onSetCountdownStepImage = onSetCountdownStepImage,

                            onSetCountdownAllImages = onSetCountdownAllImages,

                            onClearAllCountdownImages = onClearAllCountdownImages,

                            onAdjustTimingOffset = onAdjustTimingOffset,



                            onApplyPreset = onApplyPreset,



                            modifier = Modifier



                                .weight(0.56f)



                                .fillMaxHeight()



                        )



                    }



                } else {



                    // Portrait Layout: Single Column with preview on top



                    LyricLivePreviewCard(config = config, previewWipe = previewWipe, isTagalog = isTagalog)



                    Spacer(modifier = Modifier.height(8.dp))



                    Column(



                        modifier = Modifier



                            .weight(1f)



                            .fillMaxWidth()



                            .verticalScroll(rememberScrollState()),



                        verticalArrangement = Arrangement.spacedBy(10.dp)



                    ) {



                        LyricLineAndPositionControls(



                            config = config,



                            isTagalog = isTagalog,



                            theme = theme,



                            onSelectLineCount = onSelectLineCount,



                            onSelectPosition = onSelectPosition



                        )



                        LyricControlsInner(



                            config = config,



                            isTagalog = isTagalog,



                            theme = theme,



                            onSelectFontFamily = onSelectFontFamily,



                            onSelectWipeColor = onSelectWipeColor,



                            onSelectShadowStyle = onSelectShadowStyle,



                            onSelectOutlineColor = onSelectOutlineColor,



                            onToggleAllCaps = onToggleAllCaps,



                            onToggleItalic = onToggleItalic,



                            onAdjustFontScale = onAdjustFontScale,



                            onAdjustOutlineThickness = onAdjustOutlineThickness,



                            onAdjustFontSize = onAdjustFontSize,



                            onAdjustLineSpacing = onAdjustLineSpacing,



                            onToggleDropShadow = onToggleDropShadow,



                            onSelectCountdownMode = onSelectCountdownMode,

                            onSetCountdownStepImage = onSetCountdownStepImage,

                            onSetCountdownAllImages = onSetCountdownAllImages,

                            onClearAllCountdownImages = onClearAllCountdownImages,

                            onAdjustTimingOffset = onAdjustTimingOffset,



                            onApplyPreset = onApplyPreset



                        )



                    }



                    Spacer(modifier = Modifier.height(8.dp))



                    Button(



                        onClick = onDismiss,



                        colors = ButtonDefaults.buttonColors(containerColor = Color(theme.primaryColor)),



                        shape = RoundedCornerShape(10.dp),



                        modifier = Modifier



                            .fillMaxWidth()



                            .height(40.dp)



                    ) {



                        val btnTextColor = if (theme.primaryColor == 0xFF00E676L || theme.primaryColor == 0xFFFFD54FL || theme.primaryColor == 0xFFFFC107L) Color.Black else Color.White



                        Text(



                            text = if (isTagalog) "ILAPAT AT ISARA ✓" else "APPLY & CLOSE ✓",



                            fontWeight = FontWeight.Black,



                            fontFamily = CleanFont,



                            fontSize = 12.sp,



                            color = btnTextColor,



                            maxLines = 1



                        )



                    }



                }



            }



        }



    }



}







@Composable



private fun LyricLivePreviewCard(



    config: LyricCustomization,



    previewWipe: Float,



    isTagalog: Boolean = false,



    modifier: Modifier = Modifier



) {



    Box(



        modifier = modifier



            .fillMaxWidth()



            .clip(RoundedCornerShape(12.dp))



            .background(



                Brush.verticalGradient(



                    listOf(Color(0xFF020617), Color(0xFF0B1120), Color(0xFF020617))



                )



            )



            .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))



            .padding(horizontal = 10.dp, vertical = 8.dp)



    ) {



        Column(



            modifier = Modifier.fillMaxWidth(),



            horizontalAlignment = Alignment.CenterHorizontally,



            verticalArrangement = Arrangement.spacedBy(3.dp)



        ) {



            Row(



                verticalAlignment = Alignment.CenterVertically,



                horizontalArrangement = Arrangement.spacedBy(6.dp)



            ) {



                Text(



                    text = if (isTagalog) "PREVIEW NG LYRICS" else "LIVE TV PREVIEW",



                    fontSize = 8.sp,



                    color = Color(0xFF94A3B8),



                    fontWeight = FontWeight.Bold,



                    letterSpacing = 1.sp,



                    maxLines = 1



                )



                Surface(



                    shape = RoundedCornerShape(4.dp),



                    color = Color(0xFF312E81)



                ) {



                    Text(



                        text = "${config.lineCount.shortBadge(isTagalog)} • ${config.position.label(isTagalog)}",



                        fontSize = 8.sp,



                        fontWeight = FontWeight.Bold,



                        color = Color(0xFFA5B4FC),



                        maxLines = 1,



                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)



                    )



                }



            }



            KaraokeLyricLine(



                text = "STILL THE ONE",



                isActive = true,



                wipePercent = previewWipe,



                fontSize = 15.sp,



                lyricConfig = config



            )



            KaraokeLyricLine(



                text = "YOU'RE STILL THE ONE THAT I LOVE",



                isActive = false,



                wipePercent = 0f,



                fontSize = 15.sp,



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



}







@Composable



private fun LyricLineAndPositionControls(



    config: LyricCustomization,



    isTagalog: Boolean = false,



    theme: AppTheme = AppTheme.CYBER_NEON,



    onSelectLineCount: (LyricLineCount) -> Unit,



    onSelectPosition: (LyricScreenPosition) -> Unit,



    modifier: Modifier = Modifier



) {



    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {



        // Line count



        Text(



            text = if (isTagalog) "📄 BILANG NG LINYA:" else "📄 LINE COUNT:",



            fontSize = 10.sp,



            fontFamily = CleanFont,



            color = Color(theme.primaryColor),



            fontWeight = FontWeight.Black



        )



        Row(



            modifier = Modifier.fillMaxWidth(),



            horizontalArrangement = Arrangement.spacedBy(6.dp)



        ) {



            LyricLineCount.values().forEach { lineOption ->



                val isSelected = config.lineCount == lineOption



                Surface(



                    shape = RoundedCornerShape(8.dp),



                    color = if (isSelected) Color(theme.primaryColor).copy(alpha = 0.25f) else Color(theme.buttonBgColor),



                    border = androidx.compose.foundation.BorderStroke(



                        width = if (isSelected) 1.5.dp else 1.dp,



                        color = if (isSelected) Color(theme.primaryColor) else Color(theme.buttonBorderColor)



                    ),



                    modifier = Modifier



                        .weight(1f)



                        .clickable { onSelectLineCount(lineOption) }



                ) {



                    Box(



                        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp, horizontal = 4.dp),



                        contentAlignment = Alignment.Center



                    ) {



                        Text(



                            text = if (isSelected) "✓ ${lineOption.label(isTagalog)}" else lineOption.label(isTagalog),



                            fontSize = 9.5.sp,



                            fontFamily = CleanFont,



                            color = if (isSelected) Color(theme.primaryColor) else Color(theme.textColor),



                            fontWeight = FontWeight.Bold,



                            maxLines = 1,



                            overflow = TextOverflow.Ellipsis



                        )



                    }



                }



            }



        }







        // Position



        Text(



            text = if (isTagalog) "📍 POSISYON SA SCREEN:" else "📍 SCREEN POSITION:",



            fontSize = 10.sp,



            fontFamily = CleanFont,



            color = Color(theme.primaryColor),



            fontWeight = FontWeight.Black



        )



        Row(



            modifier = Modifier.fillMaxWidth(),



            horizontalArrangement = Arrangement.spacedBy(6.dp)



        ) {



            LyricScreenPosition.values().forEach { posOption ->



                val isSelected = config.position == posOption



                Surface(



                    shape = RoundedCornerShape(8.dp),



                    color = if (isSelected) Color(theme.primaryColor).copy(alpha = 0.25f) else Color(theme.buttonBgColor),



                    border = androidx.compose.foundation.BorderStroke(



                        width = if (isSelected) 1.5.dp else 1.dp,



                        color = if (isSelected) Color(theme.primaryColor) else Color(theme.buttonBorderColor)



                    ),



                    modifier = Modifier



                        .weight(1f)



                        .clickable { onSelectPosition(posOption) }



                ) {



                    Box(



                        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp, horizontal = 4.dp),



                        contentAlignment = Alignment.Center



                    ) {



                        Text(



                            text = if (isSelected) "✓ ${posOption.label(isTagalog)}" else posOption.label(isTagalog),



                            fontSize = 9.5.sp,



                            fontFamily = CleanFont,



                            color = if (isSelected) Color(theme.primaryColor) else Color(theme.textColor),



                            fontWeight = FontWeight.Bold,



                            maxLines = 1,



                            overflow = TextOverflow.Ellipsis



                        )



                    }



                }



            }



        }



    }



}







@Composable



private fun LyricControlsScrollable(



    config: LyricCustomization,



    isTagalog: Boolean = false,



    theme: AppTheme = AppTheme.CYBER_NEON,



    onSelectFontFamily: (LyricFontFamily) -> Unit,



    onSelectWipeColor: (LyricWipeColor) -> Unit,



    onSelectShadowStyle: (LyricShadowStyle) -> Unit,



    onSelectOutlineColor: (LyricOutlineColor) -> Unit,



    onToggleAllCaps: () -> Unit,



    onToggleItalic: () -> Unit,



    onAdjustFontScale: (Float) -> Unit,



    onAdjustOutlineThickness: (Float) -> Unit = {},



    onAdjustFontSize: (Float, Float) -> Unit = { _, _ -> },



    onAdjustLineSpacing: (Float) -> Unit = {},



    onToggleDropShadow: () -> Unit = {},



    onSelectCountdownMode: (LyricCountdownMode) -> Unit = {},

    onSelectCountdownPhoto: (String) -> Unit = {},

    onSetCountdownStepImage: (Int, String?) -> Unit = { _, _ -> },

    onSetCountdownAllImages: (String) -> Unit = {},

    onClearAllCountdownImages: () -> Unit = {},

    onAdjustTimingOffset: (Int) -> Unit = {},



    onApplyPreset: (String) -> Unit,



    modifier: Modifier = Modifier



) {



    Column(



        modifier = modifier.verticalScroll(rememberScrollState()),



        verticalArrangement = Arrangement.spacedBy(10.dp)



    ) {



        LyricControlsInner(



            config = config,



            isTagalog = isTagalog,



            theme = theme,



            onSelectFontFamily = onSelectFontFamily,



            onSelectWipeColor = onSelectWipeColor,



            onSelectShadowStyle = onSelectShadowStyle,



            onSelectOutlineColor = onSelectOutlineColor,



            onToggleAllCaps = onToggleAllCaps,



            onToggleItalic = onToggleItalic,



            onAdjustFontScale = onAdjustFontScale,



            onAdjustOutlineThickness = onAdjustOutlineThickness,



            onAdjustFontSize = onAdjustFontSize,



            onAdjustLineSpacing = onAdjustLineSpacing,



            onToggleDropShadow = onToggleDropShadow,



            onSelectCountdownMode = onSelectCountdownMode,

            onSelectCountdownPhoto = onSelectCountdownPhoto,

            onSetCountdownStepImage = onSetCountdownStepImage,

            onSetCountdownAllImages = onSetCountdownAllImages,

            onClearAllCountdownImages = onClearAllCountdownImages,

            onAdjustTimingOffset = onAdjustTimingOffset,



            onApplyPreset = onApplyPreset



        )



    }



}







@Composable



private fun LyricControlsInner(



    config: LyricCustomization,



    isTagalog: Boolean = false,



    theme: AppTheme = AppTheme.CYBER_NEON,



    onSelectFontFamily: (LyricFontFamily) -> Unit,



    onSelectWipeColor: (LyricWipeColor) -> Unit,



    onSelectShadowStyle: (LyricShadowStyle) -> Unit,



    onSelectOutlineColor: (LyricOutlineColor) -> Unit,



    onToggleAllCaps: () -> Unit,



    onToggleItalic: () -> Unit,



    onAdjustFontScale: (Float) -> Unit,



    onAdjustOutlineThickness: (Float) -> Unit = {},



    onAdjustFontSize: (Float, Float) -> Unit = { _, _ -> },



    onAdjustLineSpacing: (Float) -> Unit = {},



    onToggleDropShadow: () -> Unit = {},



    onSelectCountdownMode: (LyricCountdownMode) -> Unit = {},

    onSelectCountdownPhoto: (String) -> Unit = {},

    onSetCountdownStepImage: (Int, String?) -> Unit = { _, _ -> },

    onSetCountdownAllImages: (String) -> Unit = {},

    onClearAllCountdownImages: () -> Unit = {},

    onAdjustTimingOffset: (Int) -> Unit = {},



    onApplyPreset: (String) -> Unit



) {



    // SECTION 1: INSTANT PRESETS



    Column {



        Text(



            text = if (isTagalog) "⚡ MABILISANG TEMA (PRESETS):" else "⚡ QUICK STYLE PRESETS:",



            fontSize = 10.5.sp,



            fontFamily = CleanFont,



            color = Color(theme.primaryColor),



            fontWeight = FontWeight.Black



        )



        Spacer(modifier = Modifier.height(4.dp))



        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {



            val presetRow1 = listOf(



                "live_tv" to "📺 Live TV Blue",



                "platinum" to "🌟 Platinum",



                "cyber" to "🪩 Cyber Neon"



            )



            val presetRow2 = listOf(



                "pop" to "🎤 Party Pink",



                "gold" to "👑 Pure Gold",



                "ballad" to "🍂 Ballad"



            )







            listOf(presetRow1, presetRow2).forEach { rowPresets ->



                Row(



                    modifier = Modifier.fillMaxWidth(),



                    horizontalArrangement = Arrangement.spacedBy(6.dp)



                ) {



                    rowPresets.forEach { (presetKey, presetLabel) ->



                        Surface(



                            shape = RoundedCornerShape(8.dp),



                            color = Color(theme.buttonBgColor),



                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(theme.buttonBorderColor)),



                            modifier = Modifier



                                .weight(1f)



                                .height(32.dp)



                                .clickable { onApplyPreset(presetKey) }



                        ) {



                            Box(



                                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),



                                contentAlignment = Alignment.Center



                            ) {



                                Text(



                                    text = presetLabel,



                                    fontSize = 9.5.sp,



                                    fontFamily = CleanFont,



                                    color = Color(theme.textColor),



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



        }



    }







    // SECTION 2: FONT FAMILY (TYPEFACE - 12 FONTS)



    Column {



        Text(



            text = if (isTagalog) "🔤 ESTILO NG TITIK (12 FONTS):" else "🔤 FONT FAMILY (12 STYLES):",



            fontSize = 10.5.sp,



            fontFamily = CleanFont,



            color = Color(theme.primaryColor),



            fontWeight = FontWeight.Black



        )



        Spacer(modifier = Modifier.height(4.dp))



        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {



            LyricFontFamily.values().toList().chunked(2).forEach { rowFamilies ->



                Row(



                    modifier = Modifier.fillMaxWidth(),



                    horizontalArrangement = Arrangement.spacedBy(6.dp)



                ) {



                    rowFamilies.forEach { family ->



                        val isSelected = config.fontFamily == family



                        val (sampleFont, sampleWeight, _) = remember(family) {



                            resolveLyricFontParams(family)



                        }



                        Row(



                            modifier = Modifier



                                .weight(1f)



                                .clip(RoundedCornerShape(8.dp))



                                .background(if (isSelected) Color(theme.primaryColor).copy(alpha = 0.22f) else Color(theme.buttonBgColor))



                                .border(



                                    width = if (isSelected) 1.5.dp else 1.dp,



                                    color = if (isSelected) Color(theme.primaryColor) else Color(theme.buttonBorderColor),



                                    shape = RoundedCornerShape(8.dp)



                                )



                                .clickable { onSelectFontFamily(family) }



                                .padding(horizontal = 8.dp, vertical = 7.dp),



                            horizontalArrangement = Arrangement.SpaceBetween,



                            verticalAlignment = Alignment.CenterVertically



                        ) {



                            Text(



                                text = family.displayName,



                                fontFamily = sampleFont,



                                fontWeight = sampleWeight,



                                fontSize = 10.sp,



                                color = if (isSelected) Color(theme.primaryColor) else Color(theme.textColor),



                                maxLines = 1,



                                overflow = TextOverflow.Ellipsis,



                                modifier = Modifier.weight(1f)



                            )



                            if (isSelected) {



                                Text(



                                    text = " ✓",



                                    color = Color(theme.primaryColor),



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







    // SECTION 3: WIPE COLOR (KULAY NG KANTA)



    Column {



        Text(



            text = if (isTagalog) "🎨 KULAY NG KINAKANTA (WIPE COLOR):" else "🎨 SINGING WIPE COLOR:",



            fontSize = 10.5.sp,



            fontFamily = CleanFont,



            color = Color(theme.primaryColor),



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



    Column {



        Text(



            text = if (isTagalog) "✨ ANINO AT EPEKTO (SHADOW STYLE):" else "✨ SHADOW & GLOW EFFECTS:",



            fontSize = 10.5.sp,



            fontFamily = CleanFont,



            color = Color(theme.primaryColor),



            fontWeight = FontWeight.Black



        )



        Spacer(modifier = Modifier.height(4.dp))



        Row(



            modifier = Modifier.fillMaxWidth(),



            horizontalArrangement = Arrangement.spacedBy(6.dp)



        ) {



            LyricShadowStyle.values().forEach { shadow ->



                val isSelected = config.shadowStyle == shadow



                Surface(



                    shape = RoundedCornerShape(8.dp),



                    color = if (isSelected) Color(theme.primaryColor).copy(alpha = 0.25f) else Color(theme.buttonBgColor),



                    border = androidx.compose.foundation.BorderStroke(



                        width = if (isSelected) 1.5.dp else 1.dp,



                        color = if (isSelected) Color(theme.primaryColor) else Color(theme.buttonBorderColor)



                    ),



                    modifier = Modifier



                        .weight(1f)



                        .clickable { onSelectShadowStyle(shadow) }



                ) {



                    Text(



                        text = shadow.label(isTagalog),



                        fontSize = 9.5.sp,



                        fontFamily = CleanFont,



                        color = if (isSelected) Color(theme.primaryColor) else Color(theme.textColor),



                        fontWeight = FontWeight.Bold,



                        textAlign = TextAlign.Center,



                        maxLines = 1,



                        overflow = TextOverflow.Ellipsis,



                        modifier = Modifier.padding(vertical = 7.dp, horizontal = 2.dp)



                    )



                }



            }



        }



    }







    // SECTION 4B: OUTLINE COLOR (KULAY NG OUTLINE NG TITIK)



    Column {



        Text(



            text = if (isTagalog) "🖍️ KULAY NG OUTLINE NG TITIK:" else "🖍️ TEXT OUTLINE COLOR:",



            fontSize = 10.5.sp,



            fontFamily = CleanFont,



            color = Color(theme.primaryColor),



            fontWeight = FontWeight.Black



        )



        Spacer(modifier = Modifier.height(6.dp))



        Row(



            modifier = Modifier.fillMaxWidth(),



            horizontalArrangement = Arrangement.SpaceBetween,



            verticalAlignment = Alignment.CenterVertically



        ) {



            LyricOutlineColor.values().forEach { outlineColor ->



                val isSelected = config.outlineColor == outlineColor



                val c = if (outlineColor == LyricOutlineColor.NONE) Color.Transparent else Color(outlineColor.colorLong)



                Column(



                    horizontalAlignment = Alignment.CenterHorizontally,



                    modifier = Modifier.clickable { onSelectOutlineColor(outlineColor) }



                ) {



                    Box(



                        modifier = Modifier



                            .size(34.dp)



                            .clip(CircleShape)



                            .background(c)



                            .border(



                                width = if (isSelected) 2.5.dp else 1.dp,



                                color = if (isSelected) Color(theme.primaryColor) else Color(theme.buttonBorderColor),



                                shape = CircleShape



                            ),



                        contentAlignment = Alignment.Center



                    ) {



                        if (outlineColor == LyricOutlineColor.NONE) {



                            Text(



                                text = "✕",



                                color = Color(0xFFEF4444),



                                fontWeight = FontWeight.Black,



                                fontSize = 13.sp



                            )



                        } else if (isSelected) {



                            Text(



                                text = "✓",



                                color = if (outlineColor == LyricOutlineColor.WHITE || outlineColor == LyricOutlineColor.GOLD || outlineColor == LyricOutlineColor.CYAN) Color.Black else Color.White,



                                fontWeight = FontWeight.Black,



                                fontSize = 13.sp



                            )



                        }



                    }



                    Spacer(modifier = Modifier.height(2.dp))



                    Text(



                        text = outlineColor.label(isTagalog),



                        fontSize = 8.5.sp,



                        fontFamily = CleanFont,



                        color = if (isSelected) Color(theme.primaryColor) else Color(theme.textMutedColor),



                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,



                        maxLines = 1



                    )



                }



            }



        }



    }







    // SECTION 5: TEXT FORMATTING (ALL CAPS & ITALIC)



    Column {



        Text(



            text = if (isTagalog) "✍️ AYOS NG MGA LETRA:" else "✍️ TEXT FORMAT & CASING:",



            fontSize = 10.5.sp,



            fontFamily = CleanFont,



            color = Color(theme.primaryColor),



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



                color = if (config.isAllCaps) Color(theme.primaryColor).copy(alpha = 0.25f) else Color(theme.buttonBgColor),



                border = androidx.compose.foundation.BorderStroke(



                    width = if (config.isAllCaps) 1.5.dp else 1.dp,



                    color = if (config.isAllCaps) Color(theme.primaryColor) else Color(theme.buttonBorderColor)



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



                        text = if (config.isAllCaps) "🔠 ALL CAPS: ON" else (if (isTagalog) "🔡 Normal na Titik" else "🔡 Normal Case"),



                        fontSize = 9.5.sp,



                        fontFamily = CleanFont,



                        color = if (config.isAllCaps) Color(theme.primaryColor) else Color(theme.textColor),



                        fontWeight = FontWeight.Bold,



                        maxLines = 1,



                        overflow = TextOverflow.Ellipsis



                    )



                }



            }







            // Italic Toggle



            Surface(



                shape = RoundedCornerShape(8.dp),



                color = if (config.isItalic) Color(theme.primaryColor).copy(alpha = 0.25f) else Color(theme.buttonBgColor),



                border = androidx.compose.foundation.BorderStroke(



                    width = if (config.isItalic) 1.5.dp else 1.dp,



                    color = if (config.isItalic) Color(theme.primaryColor) else Color(theme.buttonBorderColor)



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



                        text = if (config.isItalic) "🖋️ Italic: ON" else (if (isTagalog) "🖋️ Tuwid (Normal)" else "🖋️ Upright (Normal)"),



                        fontSize = 9.5.sp,



                        fontFamily = CleanFont,



                        color = if (config.isItalic) Color(theme.primaryColor) else Color(theme.textColor),



                        fontWeight = FontWeight.Bold,



                        maxLines = 1,



                        overflow = TextOverflow.Ellipsis



                    )



                }



            }



        }



    }







    // SECTION 6: FONT SIZE SCALE



    Column {



        Text(



            text = if (isTagalog) "📐 LAKI NG TITIK (FONT SIZE):" else "📐 FONT SIZE SCALE:",



            fontSize = 10.5.sp,



            fontFamily = CleanFont,



            color = Color(theme.primaryColor),



            fontWeight = FontWeight.Black



        )



        Spacer(modifier = Modifier.height(4.dp))



        Row(



            modifier = Modifier.fillMaxWidth(),



            horizontalArrangement = Arrangement.spacedBy(6.dp),



            verticalAlignment = Alignment.CenterVertically



        ) {



            val scales = listOf(



                0.85f to (if (isTagalog) "Maliit 85%" else "Small 85%"),



                1.0f to "Normal 100%",



                1.15f to (if (isTagalog) "Malaki 115%" else "Large 115%"),



                1.30f to "Extra 130%"



            )



            scales.forEach { (scale, label) ->



                val isSelected = Math.abs(config.fontScale - scale) < 0.05f



                Surface(



                    shape = RoundedCornerShape(8.dp),



                    color = if (isSelected) Color(theme.primaryColor).copy(alpha = 0.25f) else Color(theme.buttonBgColor),



                    border = androidx.compose.foundation.BorderStroke(



                        width = if (isSelected) 1.5.dp else 1.dp,



                        color = if (isSelected) Color(theme.primaryColor) else Color(theme.buttonBorderColor)



                    ),



                    modifier = Modifier



                        .weight(1f)



                        .clickable { onAdjustFontScale(scale) }



                ) {



                    Text(



                        text = label,



                        fontSize = 9.sp,



                        fontFamily = CleanFont,



                        color = if (isSelected) Color(theme.primaryColor) else Color(theme.textColor),



                        fontWeight = FontWeight.Bold,



                        textAlign = TextAlign.Center,



                        maxLines = 1,



                        overflow = TextOverflow.Ellipsis,



                        modifier = Modifier.padding(vertical = 7.dp, horizontal = 2.dp)



                    )



                }



            }



        }



    }







    // SECTION 7: 3D DROP SHADOW



    Column {



        Row(



            modifier = Modifier.fillMaxWidth(),



            horizontalArrangement = Arrangement.SpaceBetween,



            verticalAlignment = Alignment.CenterVertically



        ) {



            Column(modifier = Modifier.weight(1f)) {



                Text(



                    text = if (isTagalog) "✨ 3D DROP SHADOW (PAMPALINAW):" else "✨ 3D DROP SHADOW (LEGIBILITY):",



                    fontSize = 10.5.sp,



                    fontFamily = CleanFont,



                    color = Color(theme.primaryColor),



                    fontWeight = FontWeight.Black



                )



                Text(



                    text = if (config.hasDropShadow)



                        (if (isTagalog) "BUKAS (Malinaw sa kahit anong video background)" else "ON (Enhanced clarity on all backgrounds)")



                    else



                        (if (isTagalog) "SARADO (Flat text lamang)" else "OFF (Flat text only)"),



                    fontSize = 9.sp,



                    fontFamily = CleanFont,



                    color = if (config.hasDropShadow) Color(0xFF00FF88) else Color(theme.textMutedColor)



                )



            }



            Switch(



                checked = config.hasDropShadow,



                onCheckedChange = { onToggleDropShadow() },



                colors = SwitchDefaults.colors(



                    checkedThumbColor = Color(0xFF00E676),



                    checkedTrackColor = Color(0xFF064E3B)



                )



            )



        }



    }







    // SECTION 8: LYRICS OUTLINE THICKNESS (Default 6.0)



    Column {



        Row(



            modifier = Modifier.fillMaxWidth(),



            horizontalArrangement = Arrangement.SpaceBetween,



            verticalAlignment = Alignment.CenterVertically



        ) {



            Text(



                text = if (isTagalog) "🖋️ KAPAL NG OUTLINE: ${config.outlineThickness.toInt()}px" else "🖋️ OUTLINE THICKNESS: ${config.outlineThickness.toInt()}px",



                fontSize = 10.5.sp,



                fontFamily = CleanFont,



                color = Color(theme.primaryColor),



                fontWeight = FontWeight.Black



            )



            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {



                Button(



                    onClick = { onAdjustOutlineThickness(config.outlineThickness - 1f) },



                    colors = ButtonDefaults.buttonColors(containerColor = Color(theme.buttonBgColor)),



                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),



                    shape = RoundedCornerShape(4.dp),



                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(theme.buttonBorderColor)),



                    modifier = Modifier.height(24.dp)



                ) {



                    Text("–", color = Color(theme.textColor), fontWeight = FontWeight.Bold, fontFamily = CleanFont, fontSize = 13.sp)



                }



                Button(



                    onClick = { onAdjustOutlineThickness(config.outlineThickness + 1f) },



                    colors = ButtonDefaults.buttonColors(containerColor = Color(theme.primaryColor)),



                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),



                    shape = RoundedCornerShape(4.dp),



                    modifier = Modifier.height(24.dp)



                ) {



                    val plusTextColor = if (theme.primaryColor == 0xFF00E676L || theme.primaryColor == 0xFFFFD54FL || theme.primaryColor == 0xFFFFC107L) Color.Black else Color.White



                    Text("+", color = plusTextColor, fontWeight = FontWeight.Bold, fontFamily = CleanFont, fontSize = 13.sp)



                }



            }



        }



        Spacer(modifier = Modifier.height(4.dp))



        Row(



            modifier = Modifier.fillMaxWidth(),



            horizontalArrangement = Arrangement.spacedBy(6.dp)



        ) {



            listOf(



                3.0f to "3.0px",



                4.5f to "4.5px",



                6.0f to (if (isTagalog) "6.0px (Default)" else "6.0px (Default)"),



                8.0f to "8.0px",



                10.0f to "10.0px"



            ).forEach { (thick, label) ->



                val isSelected = Math.abs(config.outlineThickness - thick) < 0.2f



                Surface(



                    shape = RoundedCornerShape(6.dp),



                    color = if (isSelected) Color(theme.primaryColor).copy(alpha = 0.25f) else Color(theme.buttonBgColor),



                    border = androidx.compose.foundation.BorderStroke(



                        width = if (isSelected) 1.5.dp else 1.dp,



                        color = if (isSelected) Color(theme.primaryColor) else Color(theme.buttonBorderColor)



                    ),



                    modifier = Modifier



                        .weight(1f)



                        .clickable { onAdjustOutlineThickness(thick) }



                ) {



                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {



                        Text(



                            text = if (isSelected) "✓ $label" else label,



                            fontSize = 8.5.sp,



                            fontFamily = CleanFont,



                            fontWeight = FontWeight.Bold,



                            color = if (isSelected) Color(theme.primaryColor) else Color(theme.textColor),



                            maxLines = 1,



                            overflow = TextOverflow.Ellipsis



                        )



                    }



                }



            }



        }



    }







    // SECTION 9: EXACT FONT SIZE (PORTRAIT 42SP / LANDSCAPE 63SP ADJUSTABLE)



    Column {



        Row(



            modifier = Modifier.fillMaxWidth(),



            horizontalArrangement = Arrangement.SpaceBetween,



            verticalAlignment = Alignment.CenterVertically



        ) {



            Text(



                text = if (isTagalog)



                    "🔤 SUKAT: P ${config.fontSizePortraitSp.toInt()}sp / L ${config.fontSizeLandscapeSp.toInt()}sp"



                else



                    "🔤 SIZE: P ${config.fontSizePortraitSp.toInt()}sp / L ${config.fontSizeLandscapeSp.toInt()}sp",



                fontSize = 10.5.sp,



                fontFamily = CleanFont,



                color = Color(theme.primaryColor),



                fontWeight = FontWeight.Black



            )



            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {



                Button(



                    onClick = {



                        onAdjustFontSize(config.fontSizePortraitSp - 3f, config.fontSizeLandscapeSp - 4.5f)



                    },



                    colors = ButtonDefaults.buttonColors(containerColor = Color(theme.buttonBgColor)),



                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(theme.buttonBorderColor)),



                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),



                    shape = RoundedCornerShape(4.dp),



                    modifier = Modifier.height(24.dp)



                ) {



                    Text("–", color = Color(theme.textColor), fontWeight = FontWeight.Bold, fontFamily = CleanFont, fontSize = 13.sp)



                }



                Button(



                    onClick = {



                        onAdjustFontSize(config.fontSizePortraitSp + 3f, config.fontSizeLandscapeSp + 4.5f)



                    },



                    colors = ButtonDefaults.buttonColors(containerColor = Color(theme.primaryColor)),



                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),



                    shape = RoundedCornerShape(4.dp),



                    modifier = Modifier.height(24.dp)



                ) {



                    val plusTextColor = if (theme.primaryColor == 0xFF00E676L || theme.primaryColor == 0xFFFFD54FL || theme.primaryColor == 0xFFFFC107L) Color.Black else Color.White



                    Text("+", color = plusTextColor, fontWeight = FontWeight.Bold, fontFamily = CleanFont, fontSize = 13.sp)



                }



            }



        }



        Spacer(modifier = Modifier.height(4.dp))



        Row(



            modifier = Modifier.fillMaxWidth(),



            horizontalArrangement = Arrangement.spacedBy(6.dp)



        ) {



            listOf(



                Triple(36f, 54f, if (isTagalog) "36/54 (Maliit)" else "36/54 (Small)"),



                Triple(42f, 63f, if (isTagalog) "42/63 (Default)" else "42/63 (Default)"),



                Triple(48f, 72f, if (isTagalog) "48/72 (Malaki)" else "48/72 (Large)"),



                Triple(54f, 81f, if (isTagalog) "54/81 (Extra)" else "54/81 (Extra)")



            ).forEach { (port, land, label) ->



                val isSelected = Math.abs(config.fontSizePortraitSp - port) < 1f



                Surface(



                    shape = RoundedCornerShape(6.dp),



                    color = if (isSelected) Color(theme.primaryColor).copy(alpha = 0.25f) else Color(theme.buttonBgColor),



                    border = androidx.compose.foundation.BorderStroke(



                        width = if (isSelected) 1.5.dp else 1.dp,



                        color = if (isSelected) Color(theme.primaryColor) else Color(theme.buttonBorderColor)



                    ),



                    modifier = Modifier



                        .weight(1f)



                        .clickable { onAdjustFontSize(port, land) }



                ) {



                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {



                        Text(



                            text = if (isSelected) "✓ $label" else label,



                            fontSize = 8.5.sp,



                            fontFamily = CleanFont,



                            fontWeight = FontWeight.Bold,



                            color = if (isSelected) Color(theme.primaryColor) else Color(theme.textColor),



                            maxLines = 1,



                            overflow = TextOverflow.Ellipsis



                        )



                    }



                }



            }



        }



    }







    // SECTION 10: LYRIC LINE SPACING (DISTANSYA NG MGA LINYA)



    Column {



        Row(



            modifier = Modifier.fillMaxWidth(),



            horizontalArrangement = Arrangement.SpaceBetween,



            verticalAlignment = Alignment.CenterVertically



        ) {



            Text(



                text = if (isTagalog) "↕️ DISTANSYA NG LINYA: ${config.lineSpacingDp.toInt()}dp" else "↕️ LINE SPACING: ${config.lineSpacingDp.toInt()}dp",



                fontSize = 10.5.sp,



                fontFamily = CleanFont,



                color = Color(theme.primaryColor),



                fontWeight = FontWeight.Black



            )



            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {



                Button(



                    onClick = { onAdjustLineSpacing(config.lineSpacingDp - 2f) },



                    colors = ButtonDefaults.buttonColors(containerColor = Color(theme.buttonBgColor)),



                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(theme.buttonBorderColor)),



                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),



                    shape = RoundedCornerShape(4.dp),



                    modifier = Modifier.height(24.dp)



                ) {



                    Text("–", color = Color(theme.textColor), fontWeight = FontWeight.Bold, fontFamily = CleanFont, fontSize = 13.sp)



                }



                Button(



                    onClick = { onAdjustLineSpacing(config.lineSpacingDp + 2f) },



                    colors = ButtonDefaults.buttonColors(containerColor = Color(theme.primaryColor)),



                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),



                    shape = RoundedCornerShape(4.dp),



                    modifier = Modifier.height(24.dp)



                ) {



                    val plusTextColor = if (theme.primaryColor == 0xFF00E676L || theme.primaryColor == 0xFFFFD54FL || theme.primaryColor == 0xFFFFC107L) Color.Black else Color.White



                    Text("+", color = plusTextColor, fontWeight = FontWeight.Bold, fontFamily = CleanFont, fontSize = 13.sp)



                }



            }



        }



        Spacer(modifier = Modifier.height(4.dp))



        Row(



            modifier = Modifier.fillMaxWidth(),



            horizontalArrangement = Arrangement.spacedBy(6.dp)



        ) {



            listOf(



                8f to "8dp",



                12f to "12dp",



                16f to (if (isTagalog) "16dp (Default)" else "16dp (Default)"),



                22f to "22dp",



                28f to "28dp"



            ).forEach { (spacing, label) ->



                val isSelected = Math.abs(config.lineSpacingDp - spacing) < 1f



                Surface(



                    shape = RoundedCornerShape(6.dp),



                    color = if (isSelected) Color(theme.primaryColor).copy(alpha = 0.25f) else Color(theme.buttonBgColor),



                    border = androidx.compose.foundation.BorderStroke(



                        width = if (isSelected) 1.5.dp else 1.dp,



                        color = if (isSelected) Color(theme.primaryColor) else Color(theme.buttonBorderColor)



                    ),



                    modifier = Modifier



                        .weight(1f)



                        .clickable { onAdjustLineSpacing(spacing) }



                ) {



                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {



                        Text(



                            text = if (isSelected) "✓ $label" else label,



                            fontSize = 8.5.sp,



                            fontFamily = CleanFont,



                            fontWeight = FontWeight.Bold,



                            color = if (isSelected) Color(theme.primaryColor) else Color(theme.textColor),



                            maxLines = 1,



                            overflow = TextOverflow.Ellipsis



                        )



                    }



                }



            }



        }



    }







    // SECTION 11: COUNTDOWN / COUNTING MODE (COUNTING CUSTOMIZATION)



    Column {



        Text(



            text = if (isTagalog) "⏱️ ESTILO NG COUNTING (COUNTDOWN):" else "⏱️ COUNTDOWN & COUNTING STYLE:",



            fontSize = 10.5.sp,



            fontFamily = CleanFont,



            color = Color(theme.primaryColor),



            fontWeight = FontWeight.Black



        )



        Spacer(modifier = Modifier.height(4.dp))



        Row(



            modifier = Modifier.fillMaxWidth(),



            horizontalArrangement = Arrangement.spacedBy(6.dp)



        ) {



            LyricCountdownMode.values().forEach { mode ->



                val isSelected = config.countdownMode == mode



                Surface(



                    shape = RoundedCornerShape(8.dp),



                    color = if (isSelected) Color(theme.primaryColor).copy(alpha = 0.25f) else Color(theme.buttonBgColor),



                    border = androidx.compose.foundation.BorderStroke(



                        width = if (isSelected) 1.5.dp else 1.dp,



                        color = if (isSelected) Color(theme.primaryColor) else Color(theme.buttonBorderColor)



                    ),



                    modifier = Modifier



                        .weight(1f)



                        .clickable { onSelectCountdownMode(mode) }



                ) {



                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp, horizontal = 4.dp), contentAlignment = Alignment.Center) {



                        Text(



                            text = if (isSelected) "✓ ${mode.label(isTagalog)}" else mode.label(isTagalog),



                            fontSize = 8.5.sp,



                            fontFamily = CleanFont,



                            fontWeight = FontWeight.Bold,



                            color = if (isSelected) Color(theme.primaryColor) else Color(theme.textColor),



                            maxLines = 1,



                            overflow = TextOverflow.Ellipsis



                        )



                    }



                }



            }



        }



        if (config.countdownMode == LyricCountdownMode.PHOTO_BADGE) {
            Spacer(modifier = Modifier.height(8.dp))

            var pendingStepSlot by remember { mutableStateOf<Int?>(null) }
            val singleStepPhotoLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
            ) { uri: android.net.Uri? ->
                val slot = pendingStepSlot
                if (uri != null && slot != null) {
                    onSetCountdownStepImage(slot, uri.toString())
                }
            }

            val allStepsPhotoLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
            ) { uri: android.net.Uri? ->
                if (uri != null) {
                    onSetCountdownAllImages(uri.toString())
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.9f),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFEAB308).copy(alpha = 0.8f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isTagalog) "🖼️ 5 IMAHE NG COUNTDOWN (4, 3, 2, 1, GO!)" else "🖼️ 5 COUNTDOWN IMAGES (4, 3, 2, 1, GO!)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFDE047)
                        )

                    }

                    Text(
                        text = if (isTagalog)
                            "Pindutin ang bawat kahon para mag-upload ng kanya-kanyang larawan. Auto-resize nang maganda at makinis sa screen!"
                        else
                            "Tap each slot to upload custom photos for 4, 3, 2, 1, and GO! Auto-resizes smoothly on screen.",
                        fontSize = 9.5.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                    )

                    val context = androidx.compose.ui.platform.LocalContext.current
                    val stepList = listOf(
                        Triple(4, "4", config.countdownImg4Uri ?: config.customCountdownPhotoUri),
                        Triple(3, "3", config.countdownImg3Uri ?: config.customCountdownPhotoUri),
                        Triple(2, "2", config.countdownImg2Uri ?: config.customCountdownPhotoUri),
                        Triple(1, "1", config.countdownImg1Uri ?: config.customCountdownPhotoUri),
                        Triple(5, "GO!", config.countdownImgGoUri ?: config.customCountdownPhotoUri)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for ((stepNum, stepLabel, stepUri) in stepList) {
                            val bitmap = remember(stepUri) {
                                if (!stepUri.isNullOrEmpty()) {
                                    try {
                                        val u = android.net.Uri.parse(stepUri)
                                        context.contentResolver.openInputStream(u)?.use { stream ->
                                            android.graphics.BitmapFactory.decodeStream(stream)
                                        }
                                    } catch (_: Exception) {
                                        null
                                    }
                                } else null
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(0.85f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1E293B))
                                    .border(
                                        width = 1.dp,
                                        color = if (bitmap != null) Color(0xFF22C55E) else Color(0xFF475569),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        pendingStepSlot = stepNum
                                        singleStepPhotoLauncher.launch("image/*")
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Step $stepLabel",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 6.dp),
                                        color = Color.Black.copy(alpha = 0.75f),
                                        modifier = Modifier.align(Alignment.TopStart)
                                    ) {
                                        Text(
                                            text = stepLabel,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFFDE047),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(18.dp)
                                            .background(Color.Red.copy(alpha = 0.85f), CircleShape)
                                            .clickable { onSetCountdownStepImage(stepNum, null) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("✕", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = stepLabel,
                                            fontSize = if (stepNum == 5) 11.sp else 13.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (stepNum == 5) Color(0xFFFF5252) else Color(0xFF94A3B8)
                                        )
                                        Text(
                                            text = "+ Pili",
                                            fontSize = 8.sp,
                                            color = Color(0xFF38BDF8)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF0284C7).copy(alpha = 0.25f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7)),
                            modifier = Modifier.weight(1f).clickable { allStepsPhotoLauncher.launch("image/*") }
                        ) {
                            Text(
                                text = if (isTagalog) "⚡ 1 Larawan sa Lahat" else "⚡ 1 Photo for All",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFDC2626).copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDC2626)),
                            modifier = Modifier.weight(1f).clickable { onClearAllCountdownImages() }
                        ) {
                            Text(
                                text = if (isTagalog) "🗑️ I-reset Lahat" else "🗑️ Reset All",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFCA5A5),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

    }







    // SECTION 12: LYRIC TIMING SYNC OFFSET (PAG-SYNC NG TIMING)



    Column {



        Row(



            modifier = Modifier.fillMaxWidth(),



            horizontalArrangement = Arrangement.SpaceBetween,



            verticalAlignment = Alignment.CenterVertically



        ) {



            val offsetLabel = when {



                config.timingOffsetMs > 0 -> "+${config.timingOffsetMs}ms (${if (isTagalog) "Abante" else "Fast"})"



                config.timingOffsetMs < 0 -> "${config.timingOffsetMs}ms (${if (isTagalog) "Atras" else "Slow"})"



                else -> "0ms (${if (isTagalog) "Sakto" else "Sync"})"



            }



            Text(



                text = if (isTagalog) "🎯 TIMING SYNC OFFSET: $offsetLabel" else "🎯 LYRIC TIMING SYNC: $offsetLabel",



                fontSize = 10.5.sp,



                fontFamily = CleanFont,



                color = Color(theme.primaryColor),



                fontWeight = FontWeight.Black



            )



            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {



                Button(



                    onClick = { onAdjustTimingOffset(config.timingOffsetMs - 25) },



                    colors = ButtonDefaults.buttonColors(containerColor = Color(theme.buttonBgColor)),



                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(theme.buttonBorderColor)),



                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),



                    shape = RoundedCornerShape(4.dp),



                    modifier = Modifier.height(24.dp)



                ) {



                    Text("–25ms", color = Color(theme.textColor), fontWeight = FontWeight.Bold, fontFamily = CleanFont, fontSize = 11.sp)



                }



                Button(



                    onClick = { onAdjustTimingOffset(0) },



                    colors = ButtonDefaults.buttonColors(containerColor = Color(theme.buttonBgColor)),



                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(theme.buttonBorderColor)),



                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),



                    shape = RoundedCornerShape(4.dp),



                    modifier = Modifier.height(24.dp)



                ) {



                    Text("Reset", color = Color(theme.textMutedColor), fontWeight = FontWeight.Bold, fontFamily = CleanFont, fontSize = 11.sp)



                }



                Button(



                    onClick = { onAdjustTimingOffset(config.timingOffsetMs + 25) },



                    colors = ButtonDefaults.buttonColors(containerColor = Color(theme.primaryColor)),



                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),



                    shape = RoundedCornerShape(4.dp),



                    modifier = Modifier.height(24.dp)



                ) {



                    val plusTextColor = if (theme.primaryColor == 0xFF00E676L || theme.primaryColor == 0xFFFFD54FL || theme.primaryColor == 0xFFFFC107L) Color.Black else Color.White



                    Text("+25ms", color = plusTextColor, fontWeight = FontWeight.Bold, fontFamily = CleanFont, fontSize = 11.sp)



                }



            }



        }



        Spacer(modifier = Modifier.height(4.dp))



        Row(



            modifier = Modifier.fillMaxWidth(),



            horizontalArrangement = Arrangement.spacedBy(6.dp)



        ) {



            listOf(



                -150 to "-150ms",



                -75 to "-75ms",



                0 to (if (isTagalog) "0ms (Sakto)" else "0ms (Sync)"),



                75 to "+75ms",



                150 to "+150ms"



            ).forEach { (offsetVal, label) ->



                val isSelected = config.timingOffsetMs == offsetVal



                Surface(



                    shape = RoundedCornerShape(6.dp),



                    color = if (isSelected) Color(theme.primaryColor).copy(alpha = 0.25f) else Color(theme.buttonBgColor),



                    border = androidx.compose.foundation.BorderStroke(



                        width = if (isSelected) 1.5.dp else 1.dp,



                        color = if (isSelected) Color(theme.primaryColor) else Color(theme.buttonBorderColor)



                    ),



                    modifier = Modifier



                        .weight(1f)



                        .clickable { onAdjustTimingOffset(offsetVal) }



                ) {



                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {



                        Text(



                            text = if (isSelected) "✓ $label" else label,



                            fontSize = 8.5.sp,



                            fontFamily = CleanFont,



                            fontWeight = FontWeight.Bold,



                            color = if (isSelected) Color(theme.primaryColor) else Color(theme.textColor),



                            maxLines = 1,



                            overflow = TextOverflow.Ellipsis



                        )



                    }



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



    onSelectScoreMode: (ScoreMode) -> Unit = {},



    onSelectScoringDifficulty: (ScoringDifficulty) -> Unit = {},



    onSelectFanfareSoundLevel: (FanfareSoundLevel) -> Unit = {},
    onSelectScoringSoundStyle: (com.example.videokey.data.ScoringSoundStyle) -> Unit = {},
    onPickCustomScoringAudio: (String?) -> Unit = {},



    onTestFanfare: () -> Unit = {},



    onToggleScoringSound: (Boolean) -> Unit = {},



    onAdjustMicSensitivity: (Float) -> Unit,



    onToggleKeypadVoice: (Boolean) -> Unit,



    onToggleTvMode: () -> Unit,



    onAdjustMelody: (Int) -> Unit = {},



    onSelectLanguage: (com.example.videokey.ui.main.AppLanguage) -> Unit = {},



    onSelectTheme: (com.example.videokey.ui.main.AppTheme) -> Unit = {},



    onCheckForUpdates: () -> Unit = {},

    onOpenDownloadCenter: () -> Unit = {},

    onResetSettingsToDefault: () -> Unit = {},

    onDismiss: () -> Unit

) {



    val isTagalog = state.appLanguage == com.example.videokey.ui.main.AppLanguage.TAGALOG







    Dialog(



        onDismissRequest = onDismiss,



        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)



    ) {



                Surface(

            shape = RoundedCornerShape(20.dp),

            color = Color(0xFF0F172A),

            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(state.appTheme.primaryColor)),

            modifier = Modifier

                .fillMaxWidth(if (isLandscape) 0.70f else 0.88f)

                .widthIn(max = 480.dp)

                .fillMaxHeight(if (isLandscape) 0.85f else 0.72f)

                .heightIn(max = 560.dp)

                .padding(vertical = 12.dp)

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



                            fontFamily = FontFamily.SansSerif,



                            color = Color(state.appTheme.primaryColor),



                            fontSize = 13.sp



                        )



                    }



                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {



                        Text(text = "✕", color = Color(state.appTheme.textColor), fontSize = 16.sp, fontWeight = FontWeight.Bold)



                    }



                }







                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(state.appTheme.buttonBorderColor), thickness = 1.dp)







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



                                text = "🇵🇭 Tagalog",



                                fontWeight = if (isTagalog) FontWeight.Black else FontWeight.Bold,



                                color = if (isTagalog) Color.White else Color(0xFF94A3B8),



                                fontSize = 11.sp



                            )



                        }







                        // English Button (Default)



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



                                text = "🇺🇸 English (Default)",



                                fontWeight = if (!isTagalog) FontWeight.Black else FontWeight.Bold,



                                color = if (!isTagalog) Color.White else Color(0xFF94A3B8),



                                fontSize = 11.sp



                            )



                        }



                    }



                }







                // OPTION 1: SCORING (PAG-ISKO) & SOUND EFFECTS



                Column(



                    modifier = Modifier



                        .fillMaxWidth()



                        .clip(RoundedCornerShape(12.dp))



                        .background(Color(0xFF1E293B))



                        .padding(12.dp),



                    verticalArrangement = Arrangement.spacedBy(10.dp)



                ) {



                    // Header



                    Row(



                        modifier = Modifier.fillMaxWidth(),



                        horizontalArrangement = Arrangement.SpaceBetween,



                        verticalAlignment = Alignment.CenterVertically



                    ) {



                        Column(modifier = Modifier.weight(1f)) {



                            Text(



                                text = if (isTagalog) "🏆 SCORING SYSTEM (PAG-ISKO PAGKATAPOS NG KANTA)" else "🏆 SONG SCORING SYSTEM",



                                fontWeight = FontWeight.Black,



                                fontSize = 12.sp,



                                color = Color.White



                            )



                            Text(



                                text = when (state.scoreMode) {



                                    ScoreMode.OFF -> if (isTagalog) "SARADO (Walang score, diretso next song)" else "OFF (No score modal, direct next song)"



                                    ScoreMode.RANDOM -> if (isTagalog) "RANDOM (Suwerte/random score pagtapos ng kanta)" else "RANDOM (Fun randomized score)"



                                    ScoreMode.MICROPHONE -> if (isTagalog) "MICROPHONE (Totoong pagsukat ng boses sa mic)" else "MICROPHONE (Real-time voice pitch & energy)"



                                },



                                fontSize = 10.sp,



                                color = if (state.scoreMode != ScoreMode.OFF) Color(0xFF00FF88) else Color(0xFF94A3B8)



                            )



                        }



                    }







                    // 1. SCORE MODE: RANDOM, OFF, MICROPHONE



                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {



                        Text(



                            text = if (isTagalog) "🎯 SCORE MODE:" else "🎯 SCORE MODE:",



                            fontSize = 10.sp,



                            fontWeight = FontWeight.Bold,



                            color = Color(0xFFA5B4FC)



                        )



                        Row(



                            modifier = Modifier.fillMaxWidth(),



                            horizontalArrangement = Arrangement.spacedBy(6.dp)



                        ) {



                            listOf(



                                ScoreMode.RANDOM to (if (isTagalog) "🎲 Random" else "🎲 Random"),



                                ScoreMode.OFF to (if (isTagalog) "⛔ Off" else "⛔ Off"),



                                ScoreMode.MICROPHONE to (if (isTagalog) "🎙️ Microphone" else "🎙️ Microphone")



                            ).forEach { (mode, label) ->



                                val isSelected = state.scoreMode == mode



                                Surface(



                                    shape = RoundedCornerShape(8.dp),



                                    color = if (isSelected) Color(0xFF2563EB) else Color(0xFF0F172A),



                                    border = androidx.compose.foundation.BorderStroke(



                                        width = if (isSelected) 1.5.dp else 1.dp,



                                        color = if (isSelected) Color(0xFF60A5FA) else Color(0xFF334155)



                                    ),



                                    modifier = Modifier



                                        .weight(1f)



                                        .clickable { onSelectScoreMode(mode) }



                                ) {



                                    Box(



                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),



                                        contentAlignment = Alignment.Center



                                    ) {



                                        Text(



                                            text = if (isSelected) "✓ $label" else label,



                                            fontSize = 10.5.sp,



                                            fontWeight = FontWeight.Bold,



                                            color = if (isSelected) Color.White else Color(0xFFCBD5E1),



                                            maxLines = 1



                                        )



                                    }



                                }



                            }



                        }



                    }







                    // 2. SCORING DIFFICULTY (EASY, MEDIUM, HARD) - When score mode is not OFF



                    if (state.scoreMode != ScoreMode.OFF) {



                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {



                            Text(



                                text = if (isTagalog) "🎚️ SCORING DIFFICULTY (ANTAS NG HIRAP):" else "🎚️ SCORING DIFFICULTY:",



                                fontSize = 10.sp,



                                fontWeight = FontWeight.Bold,



                                color = Color(0xFFFFD54F)



                            )



                            Row(



                                modifier = Modifier.fillMaxWidth(),



                                horizontalArrangement = Arrangement.spacedBy(6.dp)



                            ) {



                                listOf(



                                    ScoringDifficulty.EASY to (if (isTagalog) "🟢 Easy (80-99)" else "🟢 Easy (80-99)"),



                                    ScoringDifficulty.MEDIUM to (if (isTagalog) "🟡 Medium (74-99)" else "🟡 Medium (74-99)"),



                                    ScoringDifficulty.HARD to (if (isTagalog) "🔴 Hard (65-99)" else "🔴 Hard (65-99)")



                                ).forEach { (diff, label) ->



                                    val isSelected = state.scoringDifficulty == diff



                                    Surface(



                                        shape = RoundedCornerShape(8.dp),



                                        color = if (isSelected) Color(0xFFD97706) else Color(0xFF0F172A),



                                        border = androidx.compose.foundation.BorderStroke(



                                            width = if (isSelected) 1.5.dp else 1.dp,



                                            color = if (isSelected) Color(0xFFFCD34D) else Color(0xFF334155)



                                        ),



                                        modifier = Modifier



                                            .weight(1f)



                                            .clickable { onSelectScoringDifficulty(diff) }



                                    ) {



                                        Box(



                                            modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp, horizontal = 2.dp),



                                            contentAlignment = Alignment.Center



                                        ) {



                                            Text(



                                                text = if (isSelected) "✓ $label" else label,



                                                fontSize = 9.5.sp,



                                                fontWeight = FontWeight.Bold,



                                                color = if (isSelected) Color.White else Color(0xFFCBD5E1),



                                                maxLines = 1



                                            )



                                        }



                                    }



                                }



                            }



                        }



                    }







                    // 3. FANFARE SOUND (OFF, LOW, MID, HIGH) + TEST BUTTON



                    Column(



                        modifier = Modifier



                            .fillMaxWidth()



                            .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))



                            .padding(10.dp),



                        verticalArrangement = Arrangement.spacedBy(6.dp)



                    ) {



                        Row(



                            modifier = Modifier.fillMaxWidth(),



                            horizontalArrangement = Arrangement.SpaceBetween,



                            verticalAlignment = Alignment.CenterVertically



                        ) {



                            Text(



                                text = if (isTagalog) "🎉 FANFARE SOUND (LAKAS NG TUNOG):" else "🎉 FANFARE SOUND (VOLUME LEVEL):",



                                fontSize = 10.sp,



                                fontWeight = FontWeight.Bold,



                                color = Color(0xFFFFD54F)



                            )



                        }







                        Row(



                            modifier = Modifier.fillMaxWidth(),



                            horizontalArrangement = Arrangement.spacedBy(4.dp)



                        ) {



                            listOf(



                                FanfareSoundLevel.OFF to (if (isTagalog) "🔇 Off" else "🔇 Off"),



                                FanfareSoundLevel.LOW to (if (isTagalog) "🔉 Low" else "🔉 Low"),



                                FanfareSoundLevel.MID to (if (isTagalog) "🔊 Mid" else "🔊 Mid"),



                                FanfareSoundLevel.HIGH to (if (isTagalog) "📢 High" else "📢 High")



                            ).forEach { (level, label) ->



                                val isSelected = state.fanfareSoundLevel == level



                                Surface(



                                    shape = RoundedCornerShape(6.dp),



                                    color = if (isSelected) Color(0xFF7C3AED) else Color(0xFF1E293B),



                                    border = androidx.compose.foundation.BorderStroke(



                                        width = if (isSelected) 1.5.dp else 1.dp,



                                        color = if (isSelected) Color(0xFFA78BFA) else Color(0xFF334155)



                                    ),



                                    modifier = Modifier



                                        .weight(1f)



                                        .clickable { onSelectFanfareSoundLevel(level) }



                                ) {



                                    Box(



                                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),



                                        contentAlignment = Alignment.Center



                                    ) {



                                        Text(



                                            text = if (isSelected) "✓ $label" else label,



                                            fontSize = 9.5.sp,



                                            fontWeight = FontWeight.Bold,



                                            color = if (isSelected) Color.White else Color(0xFFCBD5E1),



                                            maxLines = 1



                                        )



                                    }



                                }



                            }



                        }







                        // SCORING FANFARE STYLE CUSTOMIZATION
                        Text(
                            text = if (isTagalog) "TUNOG NG SCORING / FANFARE STYLE" else "SCORING SOUND & FANFARE STYLE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF1F5F9)
                        )

                        // Sound Style options (Grid / Rows of styles)
                        val soundStylesList: List<com.example.videokey.data.ScoringSoundStyle> = com.example.videokey.data.ScoringSoundStyle.values().toList()
                        val soundStyleChunks: List<List<com.example.videokey.data.ScoringSoundStyle>> = soundStylesList.chunked(3)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            soundStyleChunks.forEach { chunk: List<com.example.videokey.data.ScoringSoundStyle> ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    chunk.forEach { style: com.example.videokey.data.ScoringSoundStyle ->
                                        val isSelected = state.scoringSoundStyle == style
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isSelected) Color(0xFF2563EB) else Color(0xFF1E293B),
                                            border = androidx.compose.foundation.BorderStroke(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) Color(0xFF60A5FA) else Color(0xFF334155)
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { onSelectScoringSoundStyle(style) }
                                        ) {
                                            Box(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${style.icon} ${if (isTagalog) style.displayNameTagalog else style.displayNameEnglish}",
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                    for (emptySlot in chunk.size until 3) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        // If CUSTOM_AUDIO is selected, show file chooser button and selected file path
                        if (state.scoringSoundStyle == com.example.videokey.data.ScoringSoundStyle.CUSTOM_AUDIO) {
                            val audioPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                                contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
                            ) { uri: android.net.Uri? ->
                                if (uri != null) {
                                    onPickCustomScoringAudio(uri.toString())
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF0F172A))
                                    .border(1.dp, Color(0xFF3B82F6), RoundedCornerShape(6.dp))
                                    .clickable { audioPickerLauncher.launch("audio/*") }
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = "📁", fontSize = 14.sp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isTagalog) "Pumili ng Custom MP3/WAV File..." else "Choose Custom MP3/WAV Audio...",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF60A5FA)
                                    )
                                    Text(
                                        text = state.customScoringSoundUri ?: if (isTagalog) "Walang napiling file (default arcade fanfare)" else "No custom file selected",
                                        fontSize = 8.5.sp,
                                        color = Color(0xFF94A3B8),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (state.customScoringSoundUri != null) {
                                    Text(
                                        text = "✕",
                                        color = Color(0xFFEF4444),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.clickable { onPickCustomScoringAudio(null) }
                                    )
                                }
                            }
                        }

                        // Test Fanfare Button
                        Button(
                            onClick = { onTestFanfare() },



                            colors = ButtonDefaults.buttonColors(



                                containerColor = if (state.fanfareSoundLevel == FanfareSoundLevel.OFF) Color(0xFF334155) else Color(0xFF059669)



                            ),



                            shape = RoundedCornerShape(6.dp),



                            modifier = Modifier



                                .fillMaxWidth()



                                .height(32.dp),



                            contentPadding = PaddingValues(0.dp)



                        ) {



                            Text(



                                text = if (isTagalog) "▶ 🔊 SUBUKAN / I-TEST ANG FANFARE (PATUGTUGIN)" else "▶ 🔊 TEST FANFARE SOUND (PLAY NOW)",



                                fontSize = 10.sp,



                                fontWeight = FontWeight.Bold,



                                color = Color.White



                            )



                        }



                    }







                    // 4. MICROPHONE LIVE SENSITIVITY & INDICATOR (Shown only when in MICROPHONE mode)



                    if (state.scoreMode == ScoreMode.MICROPHONE) {



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



                            "Piliin ang gusto mong Score Mode: Random para sa suwerteng score, Off para walang score, o Microphone para sa live pitch scoring."



                        else



                            "Choose Score Mode: Random for party fun, Off to bypass scoring, or Microphone for real-time singing evaluation.",



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







                // OPTION 7: DOWNLOAD CENTER (MGA KANTA AT SELECTIVE BACKGROUNDS)



                Column(



                    modifier = Modifier



                        .fillMaxWidth()



                        .clip(RoundedCornerShape(12.dp))



                        .background(Color(0xFF1E293B))



                        .border(1.5.dp, Color(0xFFFFD54F), RoundedCornerShape(12.dp))



                        .padding(12.dp),



                    verticalArrangement = Arrangement.spacedBy(8.dp)



                ) {



                    Row(



                        verticalAlignment = Alignment.CenterVertically,



                        horizontalArrangement = Arrangement.spacedBy(6.dp)



                    ) {



                        Text(text = "📦", fontSize = 16.sp)



                        Text(



                            text = if (isTagalog) "DOWNLOAD CENTER (KANTA & BACKGROUNDS)" else "DOWNLOAD CENTER (SONGS & BACKGROUNDS)",



                            fontWeight = FontWeight.Black,



                            fontSize = 12.sp,



                            color = Color(0xFFFFD54F)



                        )



                    }



                    Text(



                        text = if (isTagalog)



                            "Mag-download ng mga kanta, lahat ng SoundFonts, at lahat ng video backgrounds (3D Sayaw, Motion, Nature) gamit ang Wi-Fi o data sa iisang pindutan lamang bawat isa."



                        else



                            "Download song packs, complete SoundFonts, and all video backgrounds (3D Dance, Motion, Nature) over Wi-Fi or data in a single tap.",



                        fontSize = 10.sp,



                        color = Color(0xFFCBD5E1)



                    )



                    Button(



                        onClick = onOpenDownloadCenter,



                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAB308)),



                        shape = RoundedCornerShape(8.dp),



                        modifier = Modifier



                            .fillMaxWidth()



                            .height(38.dp)



                    ) {



                        Text(



                            text = if (isTagalog) "📥 BUKSAN ANG DOWNLOAD CENTER" else "📥 OPEN DOWNLOAD CENTER",



                            fontWeight = FontWeight.Black,



                            fontSize = 11.sp,



                            color = Color.Black



                        )



                    }



                }



                // OPTION 8: RESET ALL SETTINGS TO DEFAULT (Properly inside Scrollable Column)

                Column(

                    modifier = Modifier

                        .fillMaxWidth()

                        .clip(RoundedCornerShape(12.dp))

                        .background(Color(0xFFEF4444).copy(alpha = 0.15f))

                        .border(1.5.dp, Color(0xFFEF4444).copy(alpha = 0.6f), RoundedCornerShape(12.dp))

                        .padding(12.dp),

                    verticalArrangement = Arrangement.spacedBy(8.dp)

                ) {

                    Row(

                        verticalAlignment = Alignment.CenterVertically,

                        horizontalArrangement = Arrangement.spacedBy(6.dp)

                    ) {

                        Text(text = "🔄", fontSize = 16.sp)

                        Text(

                            text = if (isTagalog) "I-RESET SA DEFAULT ANG MGA SETTINGS" else "RESET ALL SETTINGS TO DEFAULT",

                            fontWeight = FontWeight.Black,

                            fontSize = 12.sp,

                            color = Color(0xFFF87171)

                        )

                    }

                    Text(

                        text = if (isTagalog)

                            "Ibabalik nito ang tema, scoring, liriko, keypad voice, at volume controls sa orihinal na factory default settings."

                        else

                            "Restores theme, scoring, lyrics, keypad voice, and volume controls to factory default settings.",

                        fontSize = 10.sp,

                        color = Color(0xFFCBD5E1)

                    )

                    Button(

                        onClick = onResetSettingsToDefault,

                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),

                        shape = RoundedCornerShape(8.dp),

                        modifier = Modifier

                            .fillMaxWidth()

                            .height(38.dp)

                    ) {

                        Text(

                            text = if (isTagalog) "🔄 I-RESET NA SA FACTORY DEFAULT" else "🔄 RESET ALL SETTINGS NOW",

                            fontWeight = FontWeight.Black,

                            fontSize = 11.sp,

                            color = Color.White

                        )

                    }

                }



            } // Closes Scrollable Column



            Spacer(modifier = Modifier.height(10.dp))



            // Done / Close Action Button (Properly inside parent Column)

            Button(

                onClick = onDismiss,

                colors = ButtonDefaults.buttonColors(containerColor = Color(state.appTheme.primaryColor)),

                shape = RoundedCornerShape(10.dp),

                modifier = Modifier

                    .fillMaxWidth()

                    .height(44.dp)

            ) {

                Text(

                    text = if (isTagalog) "TAPOS NA / ISARA ✓" else "DONE / CLOSE ✓",

                    fontWeight = FontWeight.Bold,

                    color = getContrastingTextColor(state.appTheme.primaryColor)

                )

            }



        } // Closes parent Column



    } // Closes Surface



} // Closes Dialog

} // Closes fun AppSettingsDialog







/**



 * In-App Folder Browser Dialog for selecting exact Song Folder or Background Folder.



 * Allows effortless browsing of device directories and quick bookmarks without external SAF picker issues.



 */



@Composable



fun FolderBrowserDialog(



    title: String,



    initialPath: String,



    isTagalog: Boolean = false,



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



                        fontSize = 13.sp,



                        maxLines = 1,



                        overflow = TextOverflow.Ellipsis,



                        modifier = Modifier.weight(1f)



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



                                text = if (isTagalog) "KASALUKUYANG FOLDER:" else "CURRENT FOLDER:",



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



                                Text(



                                    text = if (isTagalog) "⬆️ Taas" else "⬆️ Up",



                                    fontSize = 10.sp,



                                    color = Color.White,



                                    fontWeight = FontWeight.Bold



                                )



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



                    text = if (isTagalog) "MGA SUBFOLDER (${subFolders.size}):" else "SUBFOLDERS (${subFolders.size}):",



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



                                    text = if (isTagalog)



                                        "Walang karagdagang subfolder dito.\nI-click ang 'Piliin ang Folder na Ito' sa ibaba."



                                    else



                                        "No additional subfolders here.\nTap 'Select This Folder' below.",



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



                                    text = if (isTagalog) "Buksan ▶" else "Open ▶",



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



                        text = if (isTagalog) "✓ PILIIN ANG FOLDER NA ITO" else "✓ SELECT THIS FOLDER",



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



    onStartScreenRecording: () -> Unit = {},



    onStopScreenRecording: () -> Unit = {},



    onAdjustVocalSync: (Int) -> Unit = {},



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



            color = Color(state.appTheme.surfaceColor),



            border = androidx.compose.foundation.BorderStroke(2.dp, Color(state.appTheme.primaryColor)),



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



                            fontFamily = CleanFont,



                            color = Color(state.appTheme.primaryColor),



                            fontSize = 13.sp



                        )



                    }



                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {



                        Text(text = "✕", color = Color(state.appTheme.textColor), fontSize = 16.sp, fontWeight = FontWeight.Bold)



                    }



                }







                HorizontalDivider(color = Color(state.appTheme.buttonBorderColor), thickness = 1.dp)







                var recordingTab by remember { mutableStateOf(if (state.isScreenRecording) 1 else 0) }



                Row(

                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement = Arrangement.spacedBy(8.dp)

                ) {

                    Surface(

                        shape = RoundedCornerShape(8.dp),

                        color = if (recordingTab == 0) Color(state.appTheme.primaryColor).copy(alpha = 0.2f) else Color(state.appTheme.buttonBgColor),

                        border = androidx.compose.foundation.BorderStroke(1.dp, if (recordingTab == 0) Color(state.appTheme.primaryColor) else Color(state.appTheme.buttonBorderColor)),

                        modifier = Modifier.weight(1f).clickable { recordingTab = 0 }

                    ) {

                        Text(

                            text = if (isTagalog) "🎙️ Audio (WAV)" else "🎙️ Audio (WAV)",

                            fontFamily = CleanFont,

                            fontWeight = FontWeight.Bold,

                            fontSize = 11.sp,

                            color = if (recordingTab == 0) Color(state.appTheme.primaryColor) else Color.White,

                            textAlign = TextAlign.Center,

                            modifier = Modifier.padding(vertical = 8.dp)

                        )

                    }

                    Surface(

                        shape = RoundedCornerShape(8.dp),

                        color = if (recordingTab == 1) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(state.appTheme.buttonBgColor),

                        border = androidx.compose.foundation.BorderStroke(1.dp, if (recordingTab == 1) Color(0xFFEF4444) else Color(state.appTheme.buttonBorderColor)),

                        modifier = Modifier.weight(1f).clickable { recordingTab = 1 }

                    ) {

                        Text(

                            text = if (isTagalog) "🎥 Screen Record + Sound" else "🎥 Screen Record + Sound",

                            fontFamily = CleanFont,

                            fontWeight = FontWeight.Bold,

                            fontSize = 11.sp,

                            color = if (recordingTab == 1) Color(0xFFEF4444) else Color.White,

                            textAlign = TextAlign.Center,

                            modifier = Modifier.padding(vertical = 8.dp)

                        )

                    }

                }



                // Recording Control Card

                Card(

                    colors = CardDefaults.cardColors(

                        containerColor = if (state.isRecording || state.isScreenRecording) Color(0xFF450A0A) else Color(state.appTheme.buttonBgColor)

                    ),

                    shape = RoundedCornerShape(12.dp),

                    border = androidx.compose.foundation.BorderStroke(

                        1.5.dp,

                        if (state.isRecording || state.isScreenRecording) Color(0xFFEF4444) else Color(state.appTheme.buttonBorderColor)

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

                        if (recordingTab == 1) {

                            if (state.isScreenRecording) {

                                val scrMins = state.screenRecordingSeconds / 60

                                val scrSecs = state.screenRecordingSeconds % 60

                                Text(

                                    text = if (isTagalog)

                                        String.format("🔴 NAGRE-RECORD NG SCREEN: %02d:%02d", scrMins, scrSecs)

                                    else

                                        String.format("🔴 RECORDING SCREEN: %02d:%02d", scrMins, scrSecs),

                                    color = Color(0xFFFF5252),

                                    fontFamily = CleanFont,

                                    fontWeight = FontWeight.Black,

                                    fontSize = 17.sp

                                )

                                Text(

                                    text = if (isTagalog)

                                        "Kinukunan ang buong screen kasama ang sound (tugtog at boses mo)."

                                    else

                                        "Capturing full screen video accompanied with audio and vocals.",

                                    color = Color(state.appTheme.textColor).copy(alpha = 0.85f),

                                    fontFamily = CleanFont,

                                    fontSize = 10.sp,

                                    textAlign = TextAlign.Center

                                )

                                Button(

                                    onClick = onStopScreenRecording,

                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),

                                    shape = RoundedCornerShape(8.dp),

                                    modifier = Modifier

                                        .fillMaxWidth(if (isLandscape) 0.6f else 0.85f)

                                        .height(40.dp)

                                ) {

                                    Text(

                                        text = if (isTagalog) "⏹ ITIGIL AT I-SAVE ANG SCREEN RECORD" else "⏹ STOP & SAVE SCREEN RECORD",

                                        fontWeight = FontWeight.Black,

                                        fontFamily = CleanFont,

                                        fontSize = 11.5.sp,

                                        color = Color.White

                                    )

                                }

                            } else {

                                Text(

                                    text = if (isTagalog) "🎥 SCREEN RECORDING (MAY SOUND)" else "🎥 SCREEN RECORDING (WITH AUDIO)",

                                    color = Color(0xFFEF4444),

                                    fontFamily = CleanFont,

                                    fontWeight = FontWeight.Black,

                                    fontSize = 14.sp

                                )

                                Text(

                                    text = if (isTagalog)

                                        "I-record ang buong screen habang kumakanta kasama ang tugtog at boses bilang MP4 video sa Gallery!"

                                    else

                                        "Record full screen video while singing with accompaniment and vocals into MP4 in Gallery!",

                                    color = Color(state.appTheme.textMutedColor),

                                    fontFamily = CleanFont,

                                    fontSize = 10.sp,

                                    textAlign = TextAlign.Center

                                )

                                Button(

                                    onClick = onStartScreenRecording,

                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),

                                    shape = RoundedCornerShape(8.dp),

                                    modifier = Modifier

                                        .fillMaxWidth(if (isLandscape) 0.6f else 0.85f)

                                        .height(40.dp)

                                ) {

                                    Text(

                                        text = if (isTagalog) "🔴 SIMULAN ANG SCREEN RECORDING" else "🔴 START SCREEN RECORDING",

                                        fontWeight = FontWeight.Black,

                                        fontFamily = CleanFont,

                                        fontSize = 11.5.sp,

                                        color = Color.White

                                    )

                                }

                            }

                        } else if (state.isRecording) {



                            val recMins = state.recordingSeconds / 60



                            val recSecs = state.recordingSeconds % 60



                            Text(



                                text = if (isTagalog)



                                    String.format("🔴 NAGRE-RECORD: %02d:%02d", recMins, recSecs)



                                else



                                    String.format("🔴 RECORDING: %02d:%02d", recMins, recSecs),



                                color = Color(0xFFFF5252),



                                fontFamily = CleanFont,



                                fontWeight = FontWeight.Black,



                                fontSize = 18.sp



                            )



                            Text(



                                text = if (isTagalog)



                                    "Nire-record nang buo at malinaw (48kHz True Stereo) ang lahat ng instrumento at boses."



                                else



                                    "Recording full 48kHz True Stereo SoundFont instruments mixed with studio vocals.",



                                color = Color(state.appTheme.textColor).copy(alpha = 0.85f),



                                fontFamily = CleanFont,



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



                                    fontFamily = CleanFont,



                                    fontSize = 11.5.sp,



                                    color = Color.White



                                )



                            }



                        } else {



                            Text(



                                text = if (isTagalog) "🎙️ HANDANG MAG-RECORD (TRUE STEREO)" else "🎙️ READY TO RECORD (TRUE STEREO)",



                                color = Color(state.appTheme.primaryColor),



                                fontFamily = CleanFont,



                                fontWeight = FontWeight.Black,



                                fontSize = 14.sp



                            )



                            Text(



                                text = if (isTagalog)



                                    "Pindutin ang pindutan sa ibaba habang kumakanta upang mai-save nang buo ang tugtog at boses bilang Stereo WAV."



                                else



                                    "Press the button below while playing a song to save full stereo music and vocals as a WAV file.",



                                color = Color(state.appTheme.textMutedColor),



                                fontFamily = CleanFont,



                                fontSize = 10.sp,



                                textAlign = TextAlign.Center



                            )



                            val startBtnTextColor = if (state.appTheme.primaryColor == 0xFF00E676L || state.appTheme.primaryColor == 0xFFFFD54FL || state.appTheme.primaryColor == 0xFFFFC107L) Color.Black else Color.White



                            Button(



                                onClick = onToggleRecording,



                                colors = ButtonDefaults.buttonColors(containerColor = Color(state.appTheme.primaryColor)),



                                shape = RoundedCornerShape(8.dp),



                                modifier = Modifier



                                    .fillMaxWidth(if (isLandscape) 0.6f else 0.85f)



                                    .height(40.dp)



                            ) {



                                Text(



                                    text = if (isTagalog) "🔴 SIMULAN ANG RECORDING" else "🔴 START RECORDING",



                                    fontWeight = FontWeight.Black,



                                    fontFamily = CleanFont,



                                    fontSize = 11.5.sp,



                                    color = startBtnTextColor



                                )



                            }



                        }



                    }



                }







                // Vocal Sync / Timing Offset Card

                Card(

                    colors = CardDefaults.cardColors(

                        containerColor = Color(state.appTheme.buttonBgColor)

                    ),

                    shape = RoundedCornerShape(12.dp),

                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(state.appTheme.buttonBorderColor)),

                    modifier = Modifier.fillMaxWidth()

                ) {

                    Row(

                        modifier = Modifier

                            .fillMaxWidth()

                            .padding(horizontal = 12.dp, vertical = 8.dp),

                        horizontalArrangement = Arrangement.SpaceBetween,

                        verticalAlignment = Alignment.CenterVertically

                    ) {

                        Column(modifier = Modifier.weight(1f)) {

                            Text(

                                text = if (isTagalog) "⏱️ TIMING NG BOSES (VOCAL SYNC)" else "⏱️ VOCAL RECORDING SYNC",

                                color = Color(state.appTheme.primaryColor),

                                fontFamily = CleanFont,

                                fontWeight = FontWeight.Black,

                                fontSize = 11.5.sp

                            )

                            Text(

                                text = if (isTagalog) "Inaayos ang delay para eksaktong sabay ang boses sa tugtog" else "Compensates microphone audio latency",

                                color = Color(state.appTheme.textMutedColor),

                                fontFamily = CleanFont,

                                fontSize = 9.5.sp

                            )

                        }

                        Row(

                            verticalAlignment = Alignment.CenterVertically,

                            horizontalArrangement = Arrangement.spacedBy(6.dp)

                        ) {

                            Button(

                                onClick = { onAdjustVocalSync(-20) },

                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),

                                shape = RoundedCornerShape(6.dp),

                                contentPadding = PaddingValues(0.dp),

                                modifier = Modifier.size(width = 32.dp, height = 30.dp)

                            ) {

                                Text("-", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)

                            }

                            Text(

                                text = "${state.vocalSyncOffsetMs}ms",

                                color = Color(0xFF00FF88),

                                fontFamily = CleanFont,

                                fontSize = 12.5.sp,

                                fontWeight = FontWeight.Black

                            )

                            Button(

                                onClick = { onAdjustVocalSync(20) },

                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),

                                shape = RoundedCornerShape(6.dp),

                                contentPadding = PaddingValues(0.dp),

                                modifier = Modifier.size(width = 32.dp, height = 30.dp)

                            ) {

                                Text("+", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)

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



                        color = Color(state.appTheme.secondaryColor),



                        fontFamily = CleanFont,



                        fontSize = 11.sp,



                        fontWeight = FontWeight.Black



                    )



                    Text(



                        text = "/VideoKey/recordings",



                        color = Color(state.appTheme.textMutedColor),



                        fontSize = 9.sp,



                        fontFamily = CleanFont



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



                                    color = Color(state.appTheme.textMutedColor),



                                    fontFamily = CleanFont,



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



                                    .background(if (isPlayingThis) Color(state.appTheme.primaryColor).copy(alpha = 0.2f) else Color(state.appTheme.buttonBgColor))



                                    .border(1.dp, if (isPlayingThis) Color(state.appTheme.primaryColor) else Color(state.appTheme.buttonBorderColor), RoundedCornerShape(8.dp))



                                    .padding(horizontal = 10.dp, vertical = 6.dp),



                                horizontalArrangement = Arrangement.SpaceBetween,



                                verticalAlignment = Alignment.CenterVertically



                            ) {



                                Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {



                                    Text(



                                        text = rec.songTitle,



                                        color = if (isPlayingThis) Color(state.appTheme.primaryColor) else Color(state.appTheme.textColor),



                                        fontWeight = FontWeight.Bold,



                                        fontFamily = CleanFont,



                                        fontSize = 12.sp,



                                        maxLines = 1,



                                        overflow = TextOverflow.Ellipsis



                                    )



                                    Text(



                                        text = "${rec.dateLabel} • ${rec.durationLabel} • ${rec.sizeKb} KB",



                                        color = Color(state.appTheme.textMutedColor),



                                        fontSize = 9.5.sp,



                                        fontFamily = CleanFont



                                    )



                                }







                                Row(



                                    verticalAlignment = Alignment.CenterVertically,



                                    horizontalArrangement = Arrangement.spacedBy(6.dp)



                                ) {



                                    // Play / Stop Playback Button



                                    val playBtnColor = if (isPlayingThis) Color(0xFFFF1744) else Color(state.appTheme.primaryColor)



                                    val playBtnTextColor = if (isPlayingThis) Color.White else if (state.appTheme.primaryColor == 0xFF00E676L || state.appTheme.primaryColor == 0xFFFFD54FL || state.appTheme.primaryColor == 0xFFFFC107L) Color.Black else Color.White



                                    Surface(



                                        shape = RoundedCornerShape(6.dp),



                                        color = playBtnColor,



                                        modifier = Modifier.clickable { onPlayRecording(rec.filePath) }



                                    ) {



                                        Text(



                                            text = if (isPlayingThis) {



                                                if (isTagalog) "⏹ ITIGIL" else "⏹ STOP"



                                            } else {



                                                if (isTagalog) "▶ TUGTUGIN" else "▶ PLAY"



                                            },



                                            color = playBtnTextColor,



                                            fontFamily = CleanFont,



                                            fontSize = 10.sp,



                                            fontWeight = FontWeight.Black,



                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)



                                        )



                                    }







                                    // Delete Button



                                    Surface(



                                        shape = RoundedCornerShape(6.dp),



                                        color = Color(state.appTheme.buttonBgColor),



                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(state.appTheme.buttonBorderColor)),



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



                val doneBtnTextColor = if (state.appTheme.primaryColor == 0xFF00E676L || state.appTheme.primaryColor == 0xFFFFD54FL || state.appTheme.primaryColor == 0xFFFFC107L) Color.Black else Color.White



                Button(



                    onClick = onDismiss,



                    colors = ButtonDefaults.buttonColors(containerColor = Color(state.appTheme.primaryColor)),



                    shape = RoundedCornerShape(10.dp),



                    modifier = Modifier



                        .fillMaxWidth()



                        .height(40.dp)



                ) {



                    Text(



                        text = if (isTagalog) "TAPOS NA / ISARA ✓" else "DONE / CLOSE ✓",



                        fontWeight = FontWeight.Bold,



                        fontFamily = CleanFont,



                        color = doneBtnTextColor



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



    isTagalog: Boolean = false,



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



                            text = if (isTagalog) "SINI-SURI ANG UPDATE..." else "CHECKING FOR UPDATES...",



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







                    is com.example.videokey.update.AppUpdateManager.UpdateState.UpToDate -> {



                        Text(text = "✅", fontSize = 42.sp)



                        Text(



                            text = if (isTagalog) "PINAKABAGONG BERSYON NA!" else "UP TO DATE!",



                            fontWeight = FontWeight.Black,



                            fontSize = 17.sp,



                            color = Color(0xFF00FF88),



                            textAlign = TextAlign.Center



                        )



                        Text(



                            text = if (isTagalog)



                                "Nasa pinakabagong bersyon na ang iyong VideoKey (v${updateState.currentVersion}). Walang kailangang i-update sa ngayon."



                            else



                                "Your VideoKey is already on the latest version (v${updateState.currentVersion}). No update is needed right now.",



                            fontSize = 12.sp,



                            color = Color.LightGray,



                            textAlign = TextAlign.Center



                        )



                        Button(



                            onClick = onDismiss,



                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),



                            shape = RoundedCornerShape(10.dp),



                            modifier = Modifier



                                .fillMaxWidth()



                                .height(44.dp)



                        ) {



                            Text(



                                text = if (isTagalog) "OK, SALAMAT" else "OK, GOT IT",



                                fontWeight = FontWeight.Bold,



                                color = Color.White



                            )



                        }



                    }







                    is com.example.videokey.update.AppUpdateManager.UpdateState.UpdateAvailable -> {



                        Text(text = "🚀", fontSize = 38.sp)



                        Text(



                            text = if (isTagalog) "BAGONG UPDATE: v${updateState.info.versionName}" else "NEW UPDATE: v${updateState.info.versionName}",



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



                                    text = if (isTagalog) "MGA BAGONG DAGDAG / PAGBABAGO:" else "WHAT'S NEW:",



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



                        androidx.compose.material3.Button(



                            onClick = onRetry,



                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88)),



                            shape = RoundedCornerShape(12.dp),



                            modifier = Modifier.fillMaxWidth().height(48.dp)



                        ) {



                            Text(



                                text = if (isTagalog) "⬇️ I-DOWNLOAD ANG UPDATE (v${updateState.info.versionName})" else "⬇️ DOWNLOAD UPDATE (v${updateState.info.versionName})",



                                fontWeight = FontWeight.Black,



                                fontSize = 14.sp,



                                color = Color.Black



                            )



                        }



                        if (!isForce) {



                            androidx.compose.material3.TextButton(onClick = onDismiss) {



                                Text(



                                    text = if (isTagalog) "Mamaya Na" else "Later",



                                    color = Color.LightGray,



                                    fontSize = 12.sp



                                )



                            }



                        }



                    }







                    is com.example.videokey.update.AppUpdateManager.UpdateState.Downloading -> {



                        Text(text = "⬇️", fontSize = 38.sp)



                        Text(



                            text = if (isTagalog) "KINAKAILANGANG I-UPDATE ANG VIDEOKEY" else "UPDATING VIDEOKEY",



                            fontWeight = FontWeight.Black,



                            fontSize = 15.sp,



                            color = Color(0xFF00E5FF),



                            textAlign = TextAlign.Center



                        )



                        Text(



                            text = if (isTagalog)



                                "Bersyon ${updateState.info.versionName} • Huwag munang isara ang app habang dina-download ang update."



                            else



                                "Version ${updateState.info.versionName} • Please keep the app open while downloading the update.",



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



                            text = if (isTagalog) "TAPOS NA ANG PAG-DOWNLOAD!" else "DOWNLOAD COMPLETE!",



                            fontWeight = FontWeight.Black,



                            fontSize = 17.sp,



                            color = Color(0xFF00FF88)



                        )



                        Text(



                            text = if (isTagalog)



                                "Handa nang i-install ang VideoKey v${updateState.info.versionName}. Pindutin ang pindutan sa ibaba para ituloy ang pag-install:"



                            else



                                "VideoKey v${updateState.info.versionName} is ready to install. Tap the button below to proceed:",



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



                                text = if (isTagalog) "📦 I-INSTALL ANG UPDATE NGAYON" else "📦 INSTALL UPDATE NOW",



                                fontWeight = FontWeight.Black,



                                fontSize = 13.sp,



                                color = Color.Black



                            )



                        }



                        Text(



                            text = if (isTagalog)



                                "💡 Hindi mawawala ang iyong mga kanta, paborito, at recordings."



                            else



                                "💡 Your songs, favorites, and recordings will be preserved.",



                            fontSize = 10.sp,



                            color = Color(0xFF94A3B8),



                            textAlign = TextAlign.Center



                        )



                        Button(



                            onClick = onDismiss,



                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),



                            shape = RoundedCornerShape(10.dp),



                            modifier = Modifier



                                .fillMaxWidth()



                                .height(38.dp)



                        ) {



                            Text(



                                text = if (isTagalog) "ISARA ✕" else "CLOSE ✕",



                                fontWeight = FontWeight.Bold,



                                fontSize = 12.sp,



                                color = Color.White



                            )



                        }



                    }







                    is com.example.videokey.update.AppUpdateManager.UpdateState.Error -> {



                        Text(text = "⚠️", fontSize = 38.sp)



                        Text(



                            text = if (isTagalog) "HINDI MATAPOS ANG UPDATE" else "UPDATE FAILED",



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



                                    text = if (isTagalog) "🔄 SUBUKANG MULI" else "🔄 RETRY",



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



                                        text = if (isTagalog) "ISARA" else "CLOSE",



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







/**



 * In-App Download Center Dialog:



 * Allows user to selectively download song packs, SoundFonts, and choose which background videos to download.



 */



@Composable



fun DownloadCenterDialog(



    state: VideoKeyUiState,



    isLandscape: Boolean = false,



    onDownloadItem: (com.example.videokey.data.ContentDownloadManager.DownloadItem) -> Unit,



    onCancelDownload: () -> Unit,



    onDismiss: () -> Unit,



    isItemInstalled: (com.example.videokey.data.ContentDownloadManager.DownloadItem) -> Boolean = { false }



) {



    val isTagalog = state.appLanguage == com.example.videokey.ui.main.AppLanguage.TAGALOG



    val currentStatus = state.downloadStatus







    Dialog(



        onDismissRequest = onDismiss,



        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)



    ) {



        Surface(



            shape = RoundedCornerShape(18.dp),



            color = Color(0xFF0F172A),



            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFD54F)),



            modifier = Modifier



                .fillMaxWidth(if (isLandscape) 0.82f else 0.95f)



                .fillMaxHeight(if (isLandscape) 0.92f else 0.88f)



                .padding(if (isLandscape) 8.dp else 4.dp)



        ) {



            Column(



                modifier = Modifier



                    .fillMaxSize()



                    .padding(if (isLandscape) 12.dp else 16.dp)



            ) {



                // Header Bar



                Row(



                    modifier = Modifier.fillMaxWidth(),



                    horizontalArrangement = Arrangement.SpaceBetween,



                    verticalAlignment = Alignment.CenterVertically



                ) {



                    Row(



                        verticalAlignment = Alignment.CenterVertically,



                        horizontalArrangement = Arrangement.spacedBy(8.dp)



                    ) {



                        Text(text = "📦", fontSize = 20.sp)



                        Column {



                            Text(



                                text = "VIDEOKEY DOWNLOAD CENTER",



                                fontWeight = FontWeight.Black,



                                color = Color(0xFFFFD54F),



                                fontSize = 14.sp



                            )



                            Text(



                                text = if (isTagalog)



                                    "Piliin ang nais mong kanta at video background na i-download."



                                else



                                    "Select which songs and video backgrounds you want to download.",



                                fontSize = 10.sp,



                                color = Color(0xFF94A3B8)



                            )



                        }



                    }



                    Row(



                        verticalAlignment = Alignment.CenterVertically,



                        horizontalArrangement = Arrangement.spacedBy(6.dp)



                    ) {



                        Surface(



                            shape = RoundedCornerShape(8.dp),



                            color = Color(0xFF1E293B),



                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f)),



                            modifier = Modifier.clickable { onDismiss() }



                        ) {



                            Row(



                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),



                                verticalAlignment = Alignment.CenterVertically,



                                horizontalArrangement = Arrangement.spacedBy(4.dp)



                            ) {



                                Text(text = "🗕", fontSize = 12.sp, color = Color(0xFF00E5FF))



                                Text(



                                    text = if (isTagalog) "MINIMIZE" else "MINIMIZE",



                                    fontSize = 10.sp,



                                    fontWeight = FontWeight.Bold,



                                    color = Color(0xFF00E5FF)



                                )



                            }



                        }



                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {



                            Text(text = "✕", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)



                        }



                    }



                }







                HorizontalDivider(



                    modifier = Modifier.padding(vertical = 8.dp),



                    color = Color(0xFF334155),



                    thickness = 1.dp



                )







                // List of Downloadable Packs



                val songsPacks = state.downloadCatalog.filter {



                    it.category == com.example.videokey.data.ContentDownloadManager.ContentCategory.SONGS ||



                            it.category == com.example.videokey.data.ContentDownloadManager.ContentCategory.SOUNDFONT



                }



                val backgroundPacks = state.downloadCatalog.filter {



                    it.category == com.example.videokey.data.ContentDownloadManager.ContentCategory.BACKGROUND_VIDEO ||



                            it.category == com.example.videokey.data.ContentDownloadManager.ContentCategory.BACKGROUND_IMAGE



                }







                LazyColumn(



                    modifier = Modifier



                        .weight(1f)



                        .fillMaxWidth(),



                    verticalArrangement = Arrangement.spacedBy(10.dp)



                ) {



                    // Section 1: Songs & Audio



                    item {



                        Text(



                            text = if (isTagalog) "🎵 MGA KANTA AT TUNOG (CORE AUDIO)" else "🎵 SONGS & AUDIO PACKS",



                            fontWeight = FontWeight.Black,



                            fontSize = 11.5.sp,



                            color = Color(0xFF00E5FF),



                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)



                        )



                    }







                    items(songsPacks) { item ->



                        DownloadItemCard(



                            item = item,



                            isTagalog = isTagalog,



                            downloadStatus = currentStatus,



                            isItemInstalled = isItemInstalled,



                            onDownload = { onDownloadItem(item) },



                            onCancel = onCancelDownload



                        )



                    }







                    // Section 2: Selective Video Backgrounds



                    item {



                        Spacer(modifier = Modifier.height(6.dp))



                        Text(



                            text = if (isTagalog) "🎬 MGA VIDEO BACKGROUND (ALL-IN-ONE)" else "🎬 ALL-IN-ONE VIDEO BACKGROUNDS",



                            fontWeight = FontWeight.Black,



                            fontSize = 11.5.sp,



                            color = Color(0xFFFFD54F),



                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)



                        )



                        Text(



                            text = if (isTagalog)



                                "Isang pindot lang, awtomatikong mado-download ang lahat ng video backgrounds (3D Sayaw, Motion, Nature, Wallpapers):"



                            else



                                "One tap automatically downloads all background videos (3D Dance, Motion, Nature, Wallpapers):",



                            fontSize = 9.5.sp,



                            color = Color(0xFF94A3B8),



                            modifier = Modifier.padding(bottom = 4.dp)



                        )



                    }







                    items(backgroundPacks) { item ->



                        DownloadItemCard(



                            item = item,



                            isTagalog = isTagalog,



                            downloadStatus = currentStatus,



                            isItemInstalled = isItemInstalled,



                            onDownload = { onDownloadItem(item) },



                            onCancel = onCancelDownload



                        )



                    }



                }







                Spacer(modifier = Modifier.height(8.dp))







                val isAnyActive = currentStatus is com.example.videokey.data.ContentDownloadManager.DownloadStatus.Downloading ||



                        currentStatus is com.example.videokey.data.ContentDownloadManager.DownloadStatus.Extracting







                Button(



                    onClick = onDismiss,



                    colors = ButtonDefaults.buttonColors(



                        containerColor = if (isAnyActive) Color(0xFF0D9488) else Color(0xFF0284C7)



                    ),



                    shape = RoundedCornerShape(10.dp),



                    modifier = Modifier



                        .fillMaxWidth()



                        .height(44.dp)



                ) {



                    Text(



                        text = if (isAnyActive) {



                            if (isTagalog) "🗕 I-MINIMIZE (MAG-DOWNLOAD SA BACKGROUND) ✓" else "🗕 MINIMIZE (DOWNLOAD IN BACKGROUND) ✓"



                        } else {



                            if (isTagalog) "ISARA ANG DOWNLOAD CENTER ✓" else "CLOSE DOWNLOAD CENTER ✓"



                        },



                        fontWeight = FontWeight.Bold,



                        color = Color.White,



                        fontSize = 12.sp



                    )



                }



            }



        }



    }



}







@Composable



fun DownloadItemCard(



    item: com.example.videokey.data.ContentDownloadManager.DownloadItem,



    isTagalog: Boolean = false,



    downloadStatus: com.example.videokey.data.ContentDownloadManager.DownloadStatus,



    isItemInstalled: (com.example.videokey.data.ContentDownloadManager.DownloadItem) -> Boolean = { false },



    onDownload: () -> Unit,



    onCancel: () -> Unit



) {



    val isDownloadingThis = downloadStatus is com.example.videokey.data.ContentDownloadManager.DownloadStatus.Downloading && downloadStatus.itemId == item.id



    val isExtractingThis = downloadStatus is com.example.videokey.data.ContentDownloadManager.DownloadStatus.Extracting && downloadStatus.itemId == item.id



    val isAnyDownloading = downloadStatus is com.example.videokey.data.ContentDownloadManager.DownloadStatus.Downloading || downloadStatus is com.example.videokey.data.ContentDownloadManager.DownloadStatus.Extracting







    // Check if already installed on disk or freshly completed in current session



    val isInstalled = remember(item, downloadStatus) {



        if (downloadStatus is com.example.videokey.data.ContentDownloadManager.DownloadStatus.Completed && downloadStatus.itemId == item.id) {



            true



        } else {



            isItemInstalled(item)



        }



    }







    Surface(



        shape = RoundedCornerShape(12.dp),



        color = Color(0xFF1E293B),



        border = androidx.compose.foundation.BorderStroke(



            width = if (isDownloadingThis) 1.5.dp else 1.dp,



            color = if (isDownloadingThis) Color(0xFF00E5FF) else if (isInstalled) Color(0xFF065F46) else Color(0xFF334155)



        ),



        modifier = Modifier.fillMaxWidth()



    ) {



        Column(



            modifier = Modifier



                .fillMaxWidth()



                .padding(12.dp),



            verticalArrangement = Arrangement.spacedBy(8.dp)



        ) {



            Row(



                modifier = Modifier.fillMaxWidth(),



                horizontalArrangement = Arrangement.SpaceBetween,



                verticalAlignment = Alignment.Top



            ) {



                Column(modifier = Modifier.weight(1f)) {



                    Text(



                        text = item.title,



                        fontWeight = FontWeight.Black,



                        fontSize = 12.sp,



                        color = Color.White



                    )



                    Spacer(modifier = Modifier.height(2.dp))



                    Text(



                        text = item.description,



                        fontSize = 10.sp,



                        color = Color(0xFF94A3B8)



                    )



                }







                Surface(



                    shape = RoundedCornerShape(6.dp),



                    color = Color(0xFF0F172A),



                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),



                    modifier = Modifier.padding(start = 8.dp)



                ) {



                    Text(



                        text = item.sizeFormatted,



                        fontSize = 10.sp,



                        fontFamily = FontFamily.Monospace,



                        fontWeight = FontWeight.Bold,



                        color = Color(0xFFFFD54F),



                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)



                    )



                }



            }







            // Status or Action Button



            if (isDownloadingThis) {



                val dl = downloadStatus as com.example.videokey.data.ContentDownloadManager.DownloadStatus.Downloading



                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {



                    LinearProgressIndicator(



                        progress = { dl.percent / 100f },



                        modifier = Modifier



                            .fillMaxWidth()



                            .height(8.dp)



                            .clip(RoundedCornerShape(4.dp)),



                        color = Color(0xFF00FF88),



                        trackColor = Color(0xFF334155)



                    )



                    val mbDownloaded = dl.downloadedBytes / (1024f * 1024f)



                    val mbTotal = dl.totalBytes / (1024f * 1024f)



                    Row(



                        modifier = Modifier.fillMaxWidth(),



                        horizontalArrangement = Arrangement.SpaceBetween,



                        verticalAlignment = Alignment.CenterVertically



                    ) {



                        Text(



                            text = "${dl.percent}%  (${String.format("%.1f", mbDownloaded)} MB / ${String.format("%.1f", mbTotal)} MB)",



                            fontFamily = FontFamily.Monospace,



                            fontWeight = FontWeight.Bold,



                            fontSize = 10.5.sp,



                            color = Color(0xFFFFD54F)



                        )



                        Button(



                            onClick = onCancel,



                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),



                            shape = RoundedCornerShape(6.dp),



                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),



                            modifier = Modifier.height(28.dp)



                        ) {



                            Text(



                                text = if (isTagalog) "✕ KANSELAHIN" else "✕ CANCEL",



                                fontSize = 9.sp,



                                fontWeight = FontWeight.Bold,



                                color = Color.White



                            )



                        }



                    }



                }



            } else if (isExtractingThis) {



                Row(



                    modifier = Modifier.fillMaxWidth(),



                    verticalAlignment = Alignment.CenterVertically,



                    horizontalArrangement = Arrangement.spacedBy(8.dp)



                ) {



                    CircularProgressIndicator(



                        modifier = Modifier.size(16.dp),



                        color = Color(0xFF00FF88),



                        strokeWidth = 2.dp



                    )



                    Text(



                        text = if (isTagalog) "Ina-unpack at inaayos ang mga files..." else "Extracting and installing files...",



                        fontSize = 11.sp,



                        fontWeight = FontWeight.Bold,



                        color = Color(0xFF00FF88)



                    )



                }



            } else if (isInstalled) {



                Row(



                    modifier = Modifier.fillMaxWidth(),



                    horizontalArrangement = Arrangement.SpaceBetween,



                    verticalAlignment = Alignment.CenterVertically



                ) {



                    Surface(



                        shape = RoundedCornerShape(6.dp),



                        color = Color(0xFF064E3B),



                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF059669))



                    ) {



                        Text(



                            text = if (isTagalog) "✓ NAKA-INSTALL NA" else "✓ INSTALLED",



                            color = Color(0xFF00FF88),



                            fontWeight = FontWeight.Black,



                            fontSize = 10.sp,



                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)



                        )



                    }







                    Button(



                        onClick = onDownload,



                        enabled = !isAnyDownloading,



                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),



                        shape = RoundedCornerShape(6.dp),



                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),



                        modifier = Modifier.height(28.dp)



                    ) {



                        Text(



                            text = if (isTagalog) "🔄 I-download Muli" else "🔄 Redownload",



                            fontSize = 9.sp,



                            color = Color.LightGray



                        )



                    }



                }



            } else {



                Button(



                    onClick = onDownload,



                    enabled = !isAnyDownloading,



                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),



                    shape = RoundedCornerShape(8.dp),



                    modifier = Modifier



                        .fillMaxWidth()



                        .height(34.dp)



                ) {



                    Text(



                        text = if (isTagalog) "⬇️ I-DOWNLOAD ANG PACK (${item.sizeFormatted})" else "⬇️ DOWNLOAD PACK (${item.sizeFormatted})",



                        fontWeight = FontWeight.Bold,



                        fontSize = 10.5.sp,



                        color = Color.White,



                        maxLines = 1



                    )



                }



            }



        }



    }



}







/**



 * Modern Exit Confirmation Modal with bilingual support and smooth animations



 */



@Composable



fun ExitConfirmDialog(



    isTagalog: Boolean,



    primaryColor: Long,



    onConfirmExit: () -> Unit,



    onDismiss: () -> Unit



) {



    AlertDialog(



        onDismissRequest = onDismiss,



        containerColor = Color(0xFF141923),



        shape = RoundedCornerShape(20.dp),



        tonalElevation = 8.dp,



        icon = {



            Box(



                modifier = Modifier



                    .size(54.dp)



                    .background(Color(0xFFEF4444).copy(alpha = 0.15f), CircleShape),



                contentAlignment = Alignment.Center



            ) {



                Text(



                    text = "🚪",



                    fontSize = 26.sp



                )



            }



        },



        title = {



            Text(



                text = if (isTagalog) "Lumabas sa VideoKey?" else "Exit VideoKey?",



                fontSize = 19.sp,



                fontWeight = FontWeight.Bold,



                color = Color.White,



                textAlign = TextAlign.Center,



                modifier = Modifier.fillMaxWidth()



            )



        },



        text = {



            Text(



                text = if (isTagalog)



                    "Sigurado ka bang nais mong lumabas sa application? Naka-save ang iyong listahan ng kanta at mga settings."



                else



                    "Are you sure you want to exit the application? Your song queue and settings are safely saved.",



                fontSize = 13.5.sp,



                color = Color(0xFFCBD5E1),



                textAlign = TextAlign.Center,



                lineHeight = 20.sp,



                modifier = Modifier.fillMaxWidth()



            )



        },



        confirmButton = {



            Button(



                onClick = onConfirmExit,



                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),



                shape = RoundedCornerShape(12.dp),



                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)



            ) {



                Text(



                    text = if (isTagalog) "Oo, Lumabas" else "Exit App",



                    fontWeight = FontWeight.Bold,



                    color = Color.White,



                    fontSize = 13.5.sp



                )



            }



        },



        dismissButton = {



            OutlinedButton(



                onClick = onDismiss,



                shape = RoundedCornerShape(12.dp),



                border = BorderStroke(1.dp, Color(0xFF475569)),



                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)



            ) {



                Text(



                    text = if (isTagalog) "Kanselahin" else "Cancel",



                    fontWeight = FontWeight.SemiBold,



                    color = Color(0xFF94A3B8),



                    fontSize = 13.5.sp



                )



            }



        }



    )



}







/**



 * Minimized Floating Background Download Pill:



 * Displays a non-intrusive floating progress pill at the top of the videoke screen



 * when a download is ongoing in the background, allowing the user to sing or choose songs



 * uninterrupted. Users can tap to maximize back to Download Center or cancel.



 */



@Composable



fun MinimizedDownloadFloatingPill(



    downloadStatus: com.example.videokey.data.ContentDownloadManager.DownloadStatus,



    downloadCatalog: List<com.example.videokey.data.ContentDownloadManager.DownloadItem>,



    isTagalog: Boolean,



    onMaximize: () -> Unit,



    onCancel: () -> Unit



) {



    val itemId = when (downloadStatus) {



        is com.example.videokey.data.ContentDownloadManager.DownloadStatus.Downloading -> downloadStatus.itemId



        is com.example.videokey.data.ContentDownloadManager.DownloadStatus.Extracting -> downloadStatus.itemId



        else -> ""



    }



    val currentItem = downloadCatalog.find { it.id == itemId }



    val title = currentItem?.title ?: if (isTagalog) "Nilalamang Media" else "Media Content"







    val infiniteTransition = rememberInfiniteTransition(label = "pulse_dl")



    val pulseAlpha by infiniteTransition.animateFloat(



        initialValue = 0.6f,



        targetValue = 1.0f,



        animationSpec = infiniteRepeatable(



            animation = tween(800, easing = LinearEasing),



            repeatMode = RepeatMode.Reverse



        ),



        label = "pulse_alpha"



    )







    Surface(



        shape = RoundedCornerShape(30.dp),



        color = Color(0xFF0F172A).copy(alpha = 0.95f),



        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E5FF).copy(alpha = pulseAlpha)),



        shadowElevation = 8.dp,



        modifier = Modifier



            .wrapContentWidth()



            .height(44.dp)



            .clickable { onMaximize() }



    ) {



        Row(



            modifier = Modifier



                .padding(horizontal = 12.dp, vertical = 6.dp),



            verticalAlignment = Alignment.CenterVertically,



            horizontalArrangement = Arrangement.spacedBy(10.dp)



        ) {



            // Animated Icon / Spinner



            if (downloadStatus is com.example.videokey.data.ContentDownloadManager.DownloadStatus.Extracting) {



                CircularProgressIndicator(



                    modifier = Modifier.size(16.dp),



                    strokeWidth = 2.dp,



                    color = Color(0xFF00FF88)



                )



            } else {



                Text(text = "📥", fontSize = 14.sp)



            }







            // Info column & mini progress bar



            Column(



                verticalArrangement = Arrangement.Center,



                modifier = Modifier.widthIn(min = 140.dp, max = 240.dp)



            ) {



                Row(



                    modifier = Modifier.fillMaxWidth(),



                    horizontalArrangement = Arrangement.SpaceBetween,



                    verticalAlignment = Alignment.CenterVertically



                ) {



                    Text(



                        text = title,



                        fontSize = 11.sp,



                        fontWeight = FontWeight.Bold,



                        color = Color.White,



                        maxLines = 1,



                        overflow = TextOverflow.Ellipsis,



                        modifier = Modifier.weight(1f, fill = false)



                    )



                    Spacer(modifier = Modifier.width(6.dp))



                    if (downloadStatus is com.example.videokey.data.ContentDownloadManager.DownloadStatus.Downloading) {



                        Text(



                            text = "${downloadStatus.percent}%",



                            fontSize = 10.5.sp,



                            fontFamily = FontFamily.Monospace,



                            fontWeight = FontWeight.Black,



                            color = Color(0xFFFFD54F)



                        )



                    } else if (downloadStatus is com.example.videokey.data.ContentDownloadManager.DownloadStatus.Extracting) {



                        Text(



                            text = if (isTagalog) "Inaayos..." else "Installing...",



                            fontSize = 9.sp,



                            fontWeight = FontWeight.Bold,



                            color = Color(0xFF00FF88)



                        )



                    }



                }







                if (downloadStatus is com.example.videokey.data.ContentDownloadManager.DownloadStatus.Downloading) {



                    Spacer(modifier = Modifier.height(3.dp))



                    LinearProgressIndicator(



                        progress = { downloadStatus.percent / 100f },



                        modifier = Modifier



                            .fillMaxWidth()



                            .height(4.dp)



                            .clip(RoundedCornerShape(2.dp)),



                        color = Color(0xFF00FF88),



                        trackColor = Color(0xFF334155)



                    )



                }



            }







            // Action: Maximize Button



            Surface(



                shape = RoundedCornerShape(12.dp),



                color = Color(0xFF0284C7),



                modifier = Modifier.clickable { onMaximize() }



            ) {



                Text(



                    text = if (isTagalog) "🔍 BUKSAN" else "🔍 OPEN",



                    fontSize = 9.5.sp,



                    fontWeight = FontWeight.Black,



                    color = Color.White,



                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)



                )



            }







            // Action: Cancel Download Button



            IconButton(



                onClick = onCancel,



                modifier = Modifier.size(24.dp)



            ) {



                Text(



                    text = "✕",



                    color = Color(0xFFEF4444),



                    fontSize = 13.sp,



                    fontWeight = FontWeight.Bold



                )



            }



        }



    }



}









@Composable
fun FloatingLyricsOverlay(
    state: VideoKeyUiState,
    onExpandToPlayer: () -> Unit,
    onTogglePause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = state.appTheme
    val activeLineText = when (state.activeLineNumber) {
        1 -> state.line1
        2 -> state.line2
        3 -> state.line3
        4 -> state.line4
        else -> state.line1
    }.trim()

    val nextLineText = when (state.activeLineNumber) {
        1 -> state.line2
        2 -> state.line3
        3 -> state.line4
        else -> state.line1
    }.trim()

    val isPlayingActive = state.isPlaying && !state.isPlaybackPaused

    Surface(
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.98f),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(theme.primaryColor).copy(alpha = 0.85f)),
        shadowElevation = 14.dp,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onExpandToPlayer() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Row 1: Song Info & Quick Transport
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "🎵", fontSize = 16.sp)
                    Column {
                        Text(
                            text = "${state.currentSong.code} • ${state.currentSong.title}",
                            color = Color(0xFFFDE047),
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = state.currentSong.artist,
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Play / Pause Toggle
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isPlayingActive) Color(0xFFF59E0B) else Color(theme.primaryColor))
                            .clickable { onTogglePause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isPlayingActive) "⏸" else "▶",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Next Song
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF334155))
                            .clickable { onNext() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⏭",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }

                    // Expand to full player button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(theme.primaryColor).copy(alpha = 0.25f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(theme.primaryColor)),
                        modifier = Modifier.clickable { onExpandToPlayer() }
                    ) {
                        Text(
                            text = "EXPAND ⤢",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Row 2: Live Synchronized Lyrics Line
            HorizontalDivider(color = Color(theme.primaryColor).copy(alpha = 0.25f), thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = "🎤", fontSize = 13.sp)
                Text(
                    text = activeLineText.ifBlank { "..." },
                    color = Color(0xFF38BDF8),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            if (nextLineText.isNotBlank() && nextLineText != activeLineText) {
                Text(
                    text = nextLineText,
                    color = Color.White.copy(alpha = 0.60f),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 22.dp)
                )
            }
        }
    }
}
