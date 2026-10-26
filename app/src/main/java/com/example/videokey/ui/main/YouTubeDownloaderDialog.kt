package com.example.videokey.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import com.example.videokey.data.Song
import com.example.videokey.data.YouTubeVideoItem
import java.io.File
import java.util.Locale

@Composable
fun YouTubeDownloaderDialog(
    state: VideoKeyUiState,
    isLandscape: Boolean = false,
    onSearch: (String) -> Unit,
    onPlayDirect: (YouTubeVideoItem) -> Unit,
    onDownloadSong: (YouTubeVideoItem) -> Unit,
    onDownloadBackground: (YouTubeVideoItem) -> Unit,
    onPlayDownloadedSong: (Song) -> Unit = {},
    onDeleteDownloadedSong: (Song) -> Unit = {},
    onActivateVip: (String) -> Boolean = { false },
    onClearVipError: () -> Unit = {},
    onOpenAdminKeyGenerator: () -> Unit = {},
    onPayViaGcash: () -> Unit = {},
    onCheckVipStatus: () -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = state.appTheme
    val isTagalog = state.appLanguage == AppLanguage.TAGALOG
    var searchQuery by remember { mutableStateOf(state.youTubeSearchQuery) }
    var selectedTab by remember { mutableStateOf(0) } // 0 = YouTube Search, 1 = Downloaded MP4s
    var songToDelete by remember { mutableStateOf<Song?>(null) }

    LaunchedEffect(state.youTubeSearchQuery) {
        if (state.youTubeSearchQuery.isNotBlank() && searchQuery != state.youTubeSearchQuery) {
            searchQuery = state.youTubeSearchQuery
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(theme.surfaceColor).copy(alpha = 0.98f),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(theme.primaryColor)),
            modifier = modifier
                .fillMaxWidth(if (isLandscape) 0.84f else 0.94f)
                .fillMaxHeight(if (isLandscape) 0.85f else 0.88f)
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Header Bar with YouTube Badge & Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFDC2626)
                        ) {
                            Text(
                                text = "▶ YT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "YT DOWNLOADER",
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif,
                            color = Color(theme.primaryColor),
                            fontSize = 13.5.sp,
                            maxLines = 1
                        )
                        if (state.isVipActivated) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFEAB308)
                            ) {
                                Text(
                                    text = "⭐ VIP",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF334155).copy(alpha = 0.6f))
                    ) {
                        Text(text = "✕", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (!state.isVipActivated) {
                    VipActivationCard(
                        deviceId = state.deviceId,
                        error = state.vipActivationError,
                        theme = theme,
                        isTagalog = isTagalog,
                        onActivate = onActivateVip,
                        onClearError = onClearVipError,
                        onOpenAdminKeyGenerator = onOpenAdminKeyGenerator,
                        onPayViaGcash = onPayViaGcash,
                        onCheckVipStatus = onCheckVipStatus,
                        isCheckingVip = state.isCheckingVipStatus,
                        isInitiatingPayment = state.isInitiatingGcashPayment
                    )
                } else {
                    // 2. Segmented Navigation Tabs (Search vs Downloaded MP4s)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Tab 0: Maghanap sa YouTube
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = if (selectedTab == 0) Color(theme.primaryColor) else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = 0 }
                    ) {
                        val txtColor = if (selectedTab == 0) getContrastingTextColor(theme.primaryColor) else Color(theme.textMutedColor)
                        Text(
                            text = if (isTagalog) "🔍 Maghanap sa YouTube" else "🔍 Search YouTube",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.5.sp,
                            color = txtColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    // Tab 1: Mga Na-download na MP4
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = if (selectedTab == 1) Color(theme.primaryColor) else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = 1 }
                    ) {
                        val txtColor = if (selectedTab == 1) getContrastingTextColor(theme.primaryColor) else Color(theme.textMutedColor)
                        val count = state.downloadedMp4Songs.size
                        Text(
                            text = if (isTagalog) "📁 Na-download na MP4 ($count)" else "📁 Downloaded MP4s ($count)",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.5.sp,
                            color = txtColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                if (selectedTab == 0) {
                    // TAB 0: YOUTUBE SEARCH & DOWNLOAD
                    // Search Bar + Action Button
                    val clipboardManager = LocalClipboardManager.current
                    val context = LocalContext.current

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it.replace("\n", "").replace("\r", "") },
                            placeholder = {
                                Text(
                                    text = if (isTagalog) "Maghanap o mag-paste ng link..." else "Search or paste YouTube URL...",
                                    fontSize = 11.5.sp,
                                    color = Color(theme.textMutedColor).copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            trailingIcon = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(26.dp)) {
                                            Text("✕", color = Color(theme.primaryColor), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            val clipText = clipboardManager.getText()?.text?.trim() ?: ""
                                            if (clipText.isNotBlank()) {
                                                val clean = clipText.replace("\n", "").replace("\r", "").trim()
                                                searchQuery = clean
                                                Toast.makeText(context, if (isTagalog) "Na-paste ang link!" else "Link pasted!", Toast.LENGTH_SHORT).show()
                                                onSearch(clean)
                                            } else {
                                                Toast.makeText(context, if (isTagalog) "Walang laman ang clipboard" else "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Text("📋", fontSize = 14.sp)
                                    }
                                }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(theme.primaryColor),
                                unfocusedBorderColor = Color(theme.buttonBorderColor),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color(theme.primaryColor)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 50.dp),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Button(
                            onClick = { onSearch(searchQuery.trim()) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(theme.primaryColor)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 50.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            val txtColor = getContrastingTextColor(theme.primaryColor)
                            Text(
                                text = if (isTagalog) "🔍 HANAP" else "🔍 SEARCH",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = txtColor,
                                maxLines = 1
                            )
                        }
                    }

                    // Quick Search Suggestions (Smooth horizontal scrollable row, zero text truncation!)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val presets = listOf("🇵🇭 OPM Karaoke", "🎸 Rock Karaoke", "🎹 Ballad Karaoke", "✨ Acoustic Hits", "🌿 Nature Backgrounds")
                        presets.forEach { preset ->
                            val cleanTag = preset.substringAfter(" ")
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(theme.buttonBgColor),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(theme.buttonBorderColor)),
                                modifier = Modifier.clickable {
                                    searchQuery = cleanTag
                                    onSearch(cleanTag)
                                }
                            ) {
                                Text(
                                    text = preset,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(theme.textColor),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 12.dp)
                                )
                            }
                        }
                    }

                    // Active Download Progress Banner (Moving smoothly & Never frozen at 0%)
                    val dlProg = state.youTubeDownloadProgress
                    if (dlProg != null) {
                        val animatedPct by animateFloatAsState(
                            targetValue = dlProg.percent.coerceIn(0, 100) / 100f,
                            label = "dlProgressAnim"
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0F172A))
                                .border(1.5.dp, Color(theme.primaryColor), RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dlProg.videoTitle.take(34),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = if (dlProg.percent > 0) "${dlProg.percent}%" else "Inihahanda...",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(theme.primaryColor)
                                )
                            }

                            // Dynamic Linear Progress: indeterminate when 0, determinate when progressing
                            if (dlProg.percent > 0) {
                                LinearProgressIndicator(
                                    progress = { animatedPct },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = if (dlProg.isCompleted) Color(0xFF10B981) else Color(theme.primaryColor),
                                    trackColor = Color(0xFF334155)
                                )
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = Color(theme.primaryColor),
                                    trackColor = Color(0xFF334155)
                                )
                            }

                            Text(
                                text = dlProg.statusText,
                                fontSize = 10.sp,
                                color = if (dlProg.isCompleted) Color(0xFF10B981) else Color(theme.textMutedColor),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Search Results or Empty / Loading State
                    if (state.isYouTubeSearching) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color(theme.primaryColor))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (isTagalog) "Naghahanap sa YouTube..." else "Searching YouTube...",
                                    color = Color(theme.textColor),
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                    } else if (state.youTubeSearchResults.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "📺", fontSize = 38.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isTagalog)
                                        "Mag-search ng kanta para mag-download ng 1080p MP4 sa Songbook o video background!"
                                    else
                                        "Search songs to download 1080p MP4 into Songbook or video backgrounds!",
                                    fontSize = 11.5.sp,
                                    color = Color(theme.textMutedColor),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.youTubeSearchResults, key = { it.id }) { item ->
                                YouTubeResultCard(
                                    item = item,
                                    theme = theme,
                                    isTagalog = isTagalog,
                                    onPlayDirect = {
                                        onPlayDirect(item)
                                        onDismiss()
                                    },
                                    onDownloadSong = { onDownloadSong(item) },
                                    onDownloadBackground = { onDownloadBackground(item) }
                                )
                            }
                        }
                    }
                } else {
                    // TAB 1: LIST OF DOWNLOADED MP4 VIDEOS
                    val downloadedList = state.downloadedMp4Songs
                    if (downloadedList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = "📂", fontSize = 42.sp)
                                Text(
                                    text = if (isTagalog) "Wala pang na-download na MP4 na kanta." else "No downloaded MP4 karaoke videos yet.",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isTagalog)
                                        "Pumunta sa tab na \"Maghanap sa YouTube\" para mag-download ng mga 1080p karaoke videos!"
                                    else
                                        "Go to \"Search YouTube\" tab to download 1080p karaoke videos!",
                                    color = Color(theme.textMutedColor),
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                                Button(
                                    onClick = { selectedTab = 0 },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(theme.primaryColor)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    val txtColor = getContrastingTextColor(theme.primaryColor)
                                    Text(
                                        text = if (isTagalog) "🔍 Maghanap Ngayon" else "🔍 Search Now",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.5.sp,
                                        color = txtColor
                                    )
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(downloadedList, key = { it.filePath }) { song ->
                                DownloadedMp4Card(
                                    song = song,
                                    theme = theme,
                                    isTagalog = isTagalog,
                                    onPlay = {
                                        onPlayDownloadedSong(song)
                                    },
                                    onDelete = {
                                        songToDelete = song
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }

    // Confirmation Modal for deleting an MP4 file
    if (songToDelete != null) {
        val song = songToDelete!!
        AlertDialog(
            onDismissRequest = { songToDelete = null },
            title = {
                Text(
                    text = if (isTagalog) "Burahin ang Kanta?" else "Delete Video Song?",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = if (isTagalog)
                        "Sigurado ka bang nais mong burahin ang \"${song.title}\" (${song.code}) mula sa iyong storage?"
                    else
                        "Are you sure you want to delete \"${song.title}\" (${song.code}) from your device?",
                    color = Color(theme.textColor),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteDownloadedSong(song)
                        songToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isTagalog) "🗑️ Burahin" else "🗑️ Delete",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { songToDelete = null },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = if (isTagalog) "Kanselahin" else "Cancel", color = Color.White)
                }
            },
            containerColor = Color(theme.surfaceColor),
            shape = RoundedCornerShape(14.dp)
        )
    }
}

/**
 * Enhanced YouTube Result Card with spacious, well-organized, high-contrast action buttons.
 */
@Composable
private fun YouTubeResultCard(
    item: YouTubeVideoItem,
    theme: AppTheme,
    isTagalog: Boolean,
    onPlayDirect: () -> Unit,
    onDownloadSong: () -> Unit,
    onDownloadBackground: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(theme.buttonBgColor))
            .border(1.dp, Color(theme.buttonBorderColor), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Top Row: Sleek 16:9 Video Aspect Thumbnail + Title & Uploader Info
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "▶", fontSize = 10.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Black)
                    Text(
                        text = item.durationFormatted,
                        fontSize = 8.sp,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.uploader} • ${item.durationFormatted}",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = Color(theme.textMutedColor),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Bottom Row: Compact Sleek Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Button 1: ⬇ MP4 Download
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(theme.primaryColor),
                modifier = Modifier
                    .weight(1f)
                    .height(30.dp)
                    .clickable { onDownloadSong() }
            ) {
                val txtColor = getContrastingTextColor(theme.primaryColor)
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("⬇ ", fontSize = 11.sp, color = txtColor, fontWeight = FontWeight.Black)
                    Text(
                        text = if (isTagalog) "I-download MP4" else "Download MP4",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = txtColor,
                        maxLines = 1
                    )
                }
            }

            // Button 2: 🎬 Background Download
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF1E293B),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(theme.buttonBorderColor)),
                modifier = Modifier
                    .weight(1f)
                    .height(30.dp)
                    .clickable { onDownloadBackground() }
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("🎬 ", fontSize = 11.sp)
                    Text(
                        text = if (isTagalog) "I-save Background" else "Save Background",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(theme.textColor),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Card item for each downloaded MP4 file in the Downloaded list tab.
 */
@Composable
private fun DownloadedMp4Card(
    song: Song,
    theme: AppTheme,
    isTagalog: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    val file = remember(song.filePath) { File(song.filePath) }
    val fileSizeMb = remember(file) {
        if (file.exists()) {
            String.format(Locale.US, "%.1f MB", file.length() / (1024.0 * 1024.0))
        } else {
            "MP4"
        }
    }
    val isBackgroundOnly = remember(song.filePath, song.title) {
        song.filePath.contains("/backgrounds/", ignoreCase = true) ||
        song.filePath.contains("\\backgrounds\\", ignoreCase = true) ||
        song.title.contains("Background", ignoreCase = true)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(theme.buttonBgColor))
            .border(1.dp, Color(theme.buttonBorderColor), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Song Code Badge (Shows MP4 or BG)
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isBackgroundOnly) Color(0xFF6366F1).copy(alpha = 0.2f) else Color(theme.primaryColor).copy(alpha = 0.2f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isBackgroundOnly) Color(0xFF6366F1).copy(alpha = 0.5f) else Color(theme.primaryColor).copy(alpha = 0.5f)
            ),
            modifier = Modifier.size(width = 56.dp, height = 44.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isBackgroundOnly) "BG" else song.code,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isBackgroundOnly) Color(0xFFA5B4FC) else Color(theme.primaryColor),
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = if (isBackgroundOnly) "VISUAL" else "MP4",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD54F)
                )
            }
        }

        // Title, Artist, and File Size
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (isBackgroundOnly) "Video Background • $fileSizeMb" else "${song.artist} • $fileSizeMb",
                fontSize = 10.sp,
                fontFamily = FontFamily.SansSerif,
                color = Color(theme.textMutedColor),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Action Buttons: If background only, NO Sing/Kanta button (Background visual only)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isBackgroundOnly) {
                // Play / Sing Button
                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = if (isTagalog) "▶ Kanta" else "▶ Sing",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E293B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(theme.buttonBorderColor))
                ) {
                    Text(
                        text = if (isTagalog) "🎬 Background" else "🎬 Background",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(theme.textColor),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }

            // Delete Button
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFDC2626).copy(alpha = 0.2f))
                    .border(1.dp, Color(0xFFDC2626).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            ) {
                Text(text = "🗑️", fontSize = 14.sp)
            }
        }
    }
}

/**
 * VIP Device Activation Screen with offline cryptographic verification.
 * Displays unique Device ID, one-tap copy button, GCash/Maya instructions,
 * and 8-digit key input.
 */
@Composable
private fun VipActivationCard(
    deviceId: String,
    error: String?,
    theme: AppTheme,
    isTagalog: Boolean,
    onActivate: (String) -> Boolean,
    onClearError: () -> Unit,
    onOpenAdminKeyGenerator: () -> Unit = {},
    onPayViaGcash: () -> Unit = {},
    onCheckVipStatus: () -> Unit = {},
    isCheckingVip: Boolean = false,
    isInitiatingPayment: Boolean = false
) {
    var enteredKey by remember { mutableStateOf("") }
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Compact Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = "👑", fontSize = 28.sp, modifier = Modifier.padding(end = 8.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isTagalog) "VIP FEATURE ACTIVATION" else "VIP FEATURE ACTIVATION",
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    color = Color(0xFFEAB308)
                )
                Text(
                    text = if (isTagalog) "Awtomatikong SoundFont, Bass & Treble, at YouTube Downloader"
                           else "Smart Auto SoundFonts, Bass & Treble, and YouTube Downloader",
                    fontSize = 11.sp,
                    color = Color(theme.textMutedColor),
                    textAlign = TextAlign.Center
                )
            }
        }

        // 2. PRIMARY: Automated PayMongo GCash Payment Card (₱110) - Placed Upfront!
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFF005CE6), Color(0xFF00378A))))
                .border(1.5.dp, Color(0xFF38BDF8), RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                    Text(text = "💳", fontSize = 24.sp)
                    Column {
                        Text(
                            text = if (isTagalog) "AUTOMATED GCASH PAYMENT" else "AUTOMATED GCASH PAYMENT",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.5.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (isTagalog) "₱110.00 • Awtomatikong Mag-a-activate" else "₱110.00 • Instant Auto-Activation",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFDE047)
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.25f),
                    modifier = Modifier.padding(start = 6.dp)
                ) {
                    Text(
                        text = "GCASH/MAYA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = if (isTagalog)
                    "Pagkapindot ng button sa ibaba, bubukas ang opisyal na PayMongo GCash checkout. Pagkabayad, kusang mag-a-activate ang VIP para sa Device ID na ito!"
                else
                    "Clicking below opens the official PayMongo GCash checkout. Once paid, VIP will automatically activate on this Device ID!",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.95f),
                lineHeight = 15.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Button 1: Pay via GCash (₱110)
                Button(
                    onClick = onPayViaGcash,
                    enabled = !isInitiatingPayment,
                    modifier = Modifier
                        .weight(1.3f)
                        .defaultMinSize(minHeight = 50.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00E676),
                        contentColor = Color(0xFF0F172A),
                        disabledContainerColor = Color(0xFF1E293B)
                    )
                ) {
                    if (isInitiatingPayment) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color(0xFF0F172A),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isTagalog) "⚡ MAGBAYAD SA GCASH" else "⚡ PAY VIA GCASH",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.5.sp,
                            maxLines = 1
                        )
                    }
                }

                // Button 2: Check VIP Status
                Button(
                    onClick = onCheckVipStatus,
                    enabled = !isCheckingVip,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 50.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E293B),
                        contentColor = Color(0xFF38BDF8),
                        disabledContainerColor = Color(0xFF0F172A)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
                ) {
                    if (isCheckingVip) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color(0xFF38BDF8),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isTagalog) "🔄 I-CHECK STATUS" else "🔄 CHECK STATUS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            // Direktang Numero ng GCash (Manual Send)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isTagalog) "📱 GCash Account (Manual Send Money):" else "📱 GCash Account (Manual Send Money):",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "09203732202",
                            color = Color(0xFF00E676),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                    Button(
                        onClick = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString("09203732202"))
                            android.widget.Toast.makeText(context, if (isTagalog) "✓ Nakopya ang 09203732202!" else "✓ 09203732202 copied!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E676),
                            contentColor = Color(0xFF0F172A)
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 34.dp)
                    ) {
                        Text(
                            text = if (isTagalog) "KOPYAHIN" else "COPY",
                            fontWeight = FontWeight.Black,
                            fontSize = 10.5.sp
                        )
                    }
                }
            }
        }

        // 3. Device ID Box with Copy Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0F172A))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (isTagalog) "📱 IYONG DEVICE ID:" else "📱 YOUR DEVICE ID:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = deviceId.ifBlank { "VK-DEVICE-XXXX" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }

                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(deviceId))
                        Toast.makeText(
                            context,
                            if (isTagalog) "Nakopya ang Device ID sa clipboard!" else "Device ID copied to clipboard!",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(theme.primaryColor)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isTagalog) "📋 KOPYAHIN" else "📋 COPY ID",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(theme.primaryColor)
                    )
                }
            }
        }

        // 4. VIP Privileges & Benefits Summary
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1B4B).copy(alpha = 0.6f))
                .border(1.dp, Color(0xFFEAB308).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (isTagalog) "👑 MGA VIP EXCLUSIVE FEATURES:" else "👑 VIP EXCLUSIVE FEATURES:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFDE047)
            )
            val benefits = if (isTagalog) listOf(
                "🎬 Walang limitasyong YouTube Karaoke MP4 Downloader",
                "🎛️ Hiwalay na Instrument Mixer & VIP SoundFont Presets",
                "🎚️ Pro Bass Boost, Treble & Smart Auto-Mixer",
                "🎵 Eksklusibong VIP MP3 Player para sa mga lokal na kanta",
                "🎨 Custom Background Pictures, Textures & Wipe Highlights"
            ) else listOf(
                "🎬 Unlimited YouTube Karaoke MP4 Downloader",
                "🎛️ Instrument Separation & VIP SoundFont Presets",
                "🎚️ Pro Bass Boost, Treble & Smart Auto-Mixer",
                "🎵 Exclusive VIP MP3 Player for local audio files",
                "🎨 Custom Background Pictures, Textures & Wipe Highlights"
            )
            benefits.forEach { b ->
                Text(
                    text = "• $b",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = 14.sp
                )
            }
        }

        // 5. License Key Input Box (or type RESET/DEACTIVATE)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0F172A))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = if (isTagalog) "🔑 MAYROON KA BANG VIP KEY?" else "🔑 ALREADY HAVE A VIP KEY?",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(theme.primaryColor)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = enteredKey,
                    onValueChange = {
                        enteredKey = it
                        onClearError()
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = "Hal. VIP-XXXX-XXXX",
                            fontSize = 11.sp,
                            color = Color(theme.textMutedColor)
                        )
                    },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(theme.primaryColor),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B)
                    ),
                    singleLine = true
                )

                Button(
                    onClick = {
                        val success = onActivate(enteredKey.trim())
                        if (success) {
                            Toast.makeText(
                                context,
                                if (isTagalog) "Mabuhay! Na-activate na ang VIP!" else "Success! VIP Activated!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    enabled = enteredKey.trim().length >= 4,
                    modifier = Modifier.defaultMinSize(minHeight = 44.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEAB308),
                        contentColor = Color(0xFF0F172A),
                        disabledContainerColor = Color(0xFF1E293B)
                    )
                ) {
                    Text(
                        text = if (isTagalog) "I-ACTIVATE" else "ACTIVATE",
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                }
            }

            if (!error.isNullOrBlank()) {
                Text(
                    text = "⚠️ $error",
                    color = Color(0xFFEF4444),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
