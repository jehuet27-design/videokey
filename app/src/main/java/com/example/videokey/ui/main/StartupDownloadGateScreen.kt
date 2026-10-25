package com.example.videokey.ui.main

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.videokey.data.ContentDownloadManager

/**
 * Startup File Scanner & Mandatory Auto-Download Gate Screen.
 * Scans if all required files (SoundFonts, Songs, and Background Videos) are installed.
 * If any file is missing:
 * - Automatically initiates the download sequence immediately.
 * - STRICTLY LOCKS APP OPENING ("hindi mabubuksan ang apps") until 100% of required files exist.
 */
@Composable
fun StartupDownloadGateScreen(
    state: VideoKeyUiState,
    hasSoundFont: Boolean,
    hasSongs: Boolean,
    hasBackgrounds: Boolean,
    onStartDownload: (ContentDownloadManager.DownloadItem) -> Unit,
    onStartSequentialDownload: () -> Unit = {},
    onCancelDownload: () -> Unit,
    onOpenApp: () -> Unit,
    onExitApp: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val theme = state.appTheme
    val isTagalog = state.appLanguage == AppLanguage.TAGALOG

    val isAllReady = hasSoundFont && hasSongs && hasBackgrounds

    val downloadStatus = state.downloadStatus
    val isDownloading = downloadStatus is ContentDownloadManager.DownloadStatus.Downloading
    val isExtracting = downloadStatus is ContentDownloadManager.DownloadStatus.Extracting

    // Automatically trigger sequential download immediately if any files are missing!
    LaunchedEffect(isAllReady, isDownloading, isExtracting) {
        if (!isAllReady && !isDownloading && !isExtracting) {
            onStartSequentialDownload()
        }
    }

    // Automatically open app as soon as all files are confirmed ready!
    LaunchedEffect(isAllReady) {
        if (isAllReady) {
            kotlinx.coroutines.delay(400)
            onOpenApp()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF030712),
                        Color(0xFF0B1120),
                        Color(0xFF030712)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(theme.surfaceColor).copy(alpha = 0.98f))
                .border(2.dp, Color(theme.primaryColor), RoundedCornerShape(22.dp))
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Scanner Icon + Radar Header
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(theme.primaryColor).copy(alpha = 0.2f * pulseGlow))
                    .border(2.dp, Color(theme.primaryColor).copy(alpha = pulseGlow), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = if (isAllReady) "✅" else "🔍", fontSize = 28.sp)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isAllReady)
                        (if (isTagalog) "KUMPLETO NA ANG MGA FILES!" else "ALL FILES READY!")
                    else
                        (if (isTagalog) "VIDEOKEY FILE SCANNER" else "VIDEOKEY FILE SCANNER"),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    color = Color(theme.primaryColor),
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isAllReady) {
                        if (isTagalog) "Nandito na ang lahat ng files. Binubuksan na ang VideoKey..."
                        else "All required files installed. Opening VideoKey..."
                    } else {
                        if (isTagalog)
                            "Sinusuri ang storage kung kumpleto na ang files. Awtomatiko itong idodownload at hindi mabubuksan ang app hangga't may kulang."
                        else
                            "Scanning storage for required app files. Missing files will auto-download. The app cannot be opened until all files are ready."
                    },
                    fontSize = 11.5.sp,
                    color = Color(theme.textMutedColor),
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }

            HorizontalDivider(color = Color(theme.buttonBorderColor), thickness = 1.dp)

            // Item 1: SoundFont Audio Engine (Dream SAM2634 / MegaPro / Studio Minus-One)
            GateItemCard(
                title = "🎛️ SoundFont Audio Engine",
                subtitle = "345 MB • Dream SAM2634, MegaPro, & Studio Minus-One",
                isInstalled = hasSoundFont,
                isCurrentlyDownloading = isDownloading && (downloadStatus as? ContentDownloadManager.DownloadStatus.Downloading)?.itemId == "all_soundfonts",
                isCurrentlyExtracting = isExtracting && (downloadStatus as? ContentDownloadManager.DownloadStatus.Extracting)?.itemId == "all_soundfonts",
                theme = theme,
                isTagalog = isTagalog
            )

            // Item 2: Core / VIP Videoke Songs Pack
            val isSongsDl = isDownloading && (downloadStatus as? ContentDownloadManager.DownloadStatus.Downloading)?.itemId in listOf("core_songs", "core_songs_47k", "vip_songs_54k", "vip_songs_57k")
            val isSongsEx = isExtracting && (downloadStatus as? ContentDownloadManager.DownloadStatus.Extracting)?.itemId in listOf("core_songs", "core_songs_47k", "vip_songs_54k", "vip_songs_57k")
            val songTitle = if (state.isVipActivated) "⭐ VIP Videoke Songs Pack (57k Songs)" else "🎵 Videoke Songs Pack (47k Songs)"
            val songSubtitle = if (state.isVipActivated) {
                "599 MB • Eksklusibong 57,000 pinakabagong kanta para sa VIP"
            } else {
                "473 MB • Koleksyon ng 47,000 OPM & Pop videoke songs"
            }

            GateItemCard(
                title = songTitle,
                subtitle = songSubtitle,
                isInstalled = hasSongs,
                isCurrentlyDownloading = isSongsDl,
                isCurrentlyExtracting = isSongsEx,
                theme = theme,
                isTagalog = isTagalog
            )

            // Item 3: HD Background Videos Pack
            GateItemCard(
                title = "🎬 HD Background Videos Pack",
                subtitle = "204 MB • HD Drone Shoot nature backgrounds para sa videoke",
                isInstalled = hasBackgrounds,
                isCurrentlyDownloading = isDownloading && (downloadStatus as? ContentDownloadManager.DownloadStatus.Downloading)?.itemId == "all_backgrounds",
                isCurrentlyExtracting = isExtracting && (downloadStatus as? ContentDownloadManager.DownloadStatus.Extracting)?.itemId == "all_backgrounds",
                theme = theme,
                isTagalog = isTagalog
            )

            // Live Download Progress Bar (if actively downloading)
            if (isDownloading) {
                val dl = downloadStatus as ContentDownloadManager.DownloadStatus.Downloading
                val mbDownloaded = dl.downloadedBytes / (1024 * 1024)
                val mbTotal = dl.totalBytes / (1024 * 1024)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, Color(theme.primaryColor).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isTagalog) "Awtomatikong dina-download..." else "Auto-downloading...",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(theme.primaryColor)
                        )
                        Text(
                            text = "${dl.percent}% • $mbDownloaded MB / $mbTotal MB (${dl.speedKbps} KB/s)",
                            fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    LinearProgressIndicator(
                        progress = { dl.percent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(theme.primaryColor),
                        trackColor = Color(0xFF334155)
                    )
                }
            } else if (isExtracting) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F172A))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color(theme.primaryColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isTagalog) "Ina-unzip at ini-install ang mga files..." else "Extracting & installing files...",
                        fontSize = 11.5.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Main Action Button (Locked until completed, or shows downloading status)
            if (isAllReady) {
                Button(
                    onClick = onOpenApp,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(theme.primaryColor)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    val textColor = getContrastingTextColor(theme.primaryColor)
                    Text(
                        text = if (isTagalog) "BUKSAN ANG VIDEOKEY ▶" else "OPEN VIDEOKEY ▶",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        color = textColor
                    )
                }
            } else if (isDownloading || isExtracting) {
                Button(
                    onClick = {},
                    enabled = false,
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = Color(theme.primaryColor).copy(alpha = 0.35f),
                        disabledContentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isTagalog) "DINA-DOWNLOAD NA... MAGHINTAY" else "DOWNLOADING... PLEASE WAIT",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            } else {
                Button(
                    onClick = onStartSequentialDownload,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(theme.primaryColor)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    val textColor = getContrastingTextColor(theme.primaryColor)
                    Text(
                        text = if (isTagalog) "SIMULAN ANG PAG-DOWNLOAD 📥" else "START DOWNLOAD 📥",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        color = textColor
                    )
                }
            }

            // Lock Indicator: Strictly enforces that app cannot be opened until all files are ready
            if (!isAllReady) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F172A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(text = "🔒", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isTagalog)
                                "Naka-lock: Magbubukas lamang kapag kumpleto ang files"
                            else
                                "Locked: Will open only after all files are installed",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                // Exit App Button if user chooses to quit while downloading
                TextButton(
                    onClick = onExitApp,
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = if (isTagalog) "Isara ang App" else "Exit App",
                        fontSize = 11.5.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun GateItemCard(
    title: String,
    subtitle: String,
    isInstalled: Boolean,
    isCurrentlyDownloading: Boolean,
    isCurrentlyExtracting: Boolean,
    theme: AppTheme,
    isTagalog: Boolean
) {
    val borderColor = when {
        isInstalled -> Color(0xFF10B981)
        isCurrentlyDownloading || isCurrentlyExtracting -> Color(theme.primaryColor)
        else -> Color(theme.buttonBorderColor)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(theme.buttonBgColor))
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                fontFamily = FontFamily.SansSerif,
                color = Color(theme.textMutedColor),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        when {
            isInstalled -> {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF065F46)
                ) {
                    Text(
                        text = if (isTagalog) "✓ NAKAINSTALL" else "✓ INSTALLED",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF34D399),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
            isCurrentlyDownloading -> {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(theme.primaryColor).copy(alpha = 0.25f)
                ) {
                    Text(
                        text = if (isTagalog) "⏳ DINA-DOWNLOAD" else "⏳ DOWNLOADING",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(theme.primaryColor),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
            isCurrentlyExtracting -> {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF3730A3)
                ) {
                    Text(
                        text = if (isTagalog) "📦 INA-UNPACK" else "📦 EXTRACTING",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFA5B4FC),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
            else -> {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF334155)
                ) {
                    Text(
                        text = if (isTagalog) "⏸️ KULANG" else "⏸️ MISSING",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF87171),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}
