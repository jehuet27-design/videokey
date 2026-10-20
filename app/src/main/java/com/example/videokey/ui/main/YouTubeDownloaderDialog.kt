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
import com.example.videokey.data.Song
import com.example.videokey.data.YouTubeVideoItem
import java.io.File
import java.util.Locale

@Composable
fun YouTubeDownloaderDialog(
    state: VideoKeyUiState,
    onSearch: (String) -> Unit,
    onPlayDirect: (YouTubeVideoItem) -> Unit,
    onDownloadSong: (YouTubeVideoItem) -> Unit,
    onDownloadBackground: (YouTubeVideoItem) -> Unit,
    onPlayDownloadedSong: (Song) -> Unit = {},
    onDeleteDownloadedSong: (Song) -> Unit = {},
    onActivateVip: (String) -> Boolean = { false },
    onClearVipError: () -> Unit = {},
    onOpenAdminKeyGenerator: () -> Unit = {},
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
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
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
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                            text = if (isTagalog) "YT DOWNLOADER" else "YT DOWNLOADER",
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif,
                            color = Color(theme.primaryColor),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (state.isVipActivated) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFEAB308)
                            ) {
                                Text(
                                    text = if (isTagalog) "⭐ VIP AKTIBO" else "⭐ VIP ACTIVATED",
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
                        onOpenAdminKeyGenerator = onOpenAdminKeyGenerator
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
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it.replace("\n", "").replace("\r", "") },
                            placeholder = {
                                Text(
                                    text = if (isTagalog) "Maghanap o mag-paste ng YouTube link..." else "Search or paste YouTube URL...",
                                    fontSize = 11.5.sp,
                                    color = Color(theme.textMutedColor).copy(alpha = 0.7f)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                        Text("✕", color = Color(theme.primaryColor), fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )

                        // Dedicated 📋 PASTE button for 1-tap clipboard paste!
                        Button(
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(theme.buttonBgColor)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(theme.buttonBorderColor)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(52.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Text(
                                text = "📋",
                                fontSize = 16.sp
                            )
                        }

                        Button(
                            onClick = { onSearch(searchQuery.trim()) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(theme.primaryColor)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(52.dp)
                        ) {
                            val txtColor = getContrastingTextColor(theme.primaryColor)
                            Text(
                                text = if (isTagalog) "🔍 HANAP" else "🔍 SEARCH",
                                fontWeight = FontWeight.Black,
                                fontSize = 11.5.sp,
                                color = txtColor
                            )
                        }
                    }

                    // Quick Search Suggestions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val presets = listOf("OPM Karaoke", "Rock Karaoke", "Ballad Karaoke", "Nature Background")
                        presets.forEach { preset ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(theme.buttonBgColor),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(theme.buttonBorderColor)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        searchQuery = preset
                                        onSearch(preset)
                                    }
                            ) {
                                Text(
                                    text = preset,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(theme.textColor),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp)
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
            .clip(RoundedCornerShape(12.dp))
            .background(Color(theme.buttonBgColor))
            .border(1.dp, Color(theme.buttonBorderColor), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Top Row: Thumbnail + Title & Uploader Info
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "🎬", fontSize = 16.sp)
                    Text(
                        text = item.durationFormatted,
                        fontSize = 8.5.sp,
                        color = Color(theme.primaryColor),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${item.uploader} • ${item.durationFormatted}",
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = Color(theme.textMutedColor),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Bottom Row: Refined, Spacious, High-Contrast Download Action Buttons (Download Only)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Button 1: ⬇ 1080p MP4 (Download to Songbook)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(theme.primaryColor),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onDownloadSong() }
            ) {
                val txtColor = getContrastingTextColor(theme.primaryColor)
                Row(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("⬇ ", fontSize = 12.sp, color = txtColor, fontWeight = FontWeight.Black)
                    Text(
                        text = if (isTagalog) "I-download MP4" else "Download MP4",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = txtColor,
                        maxLines = 1
                    )
                }
            }

            // Button 2: 🎬 Background (Download stage visual)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF1E293B),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(theme.buttonBorderColor)),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onDownloadBackground() }
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("🎬 ", fontSize = 12.sp)
                    Text(
                        text = if (isTagalog) "I-save Background" else "Save Background",
                        fontSize = 11.sp,
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
    onOpenAdminKeyGenerator: () -> Unit = {}
) {
    var enteredKey by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var vipTapCount by remember { mutableStateOf(0) }
    var lastVipTapTime by remember { mutableStateOf(0L) }
    val onVipCrownTapped = {
        val now = System.currentTimeMillis()
        if (now - lastVipTapTime > 2000L) {
            vipTapCount = 1
        } else {
            vipTapCount++
        }
        lastVipTapTime = now
        if (vipTapCount >= 10) {
            vipTapCount = 0
            onOpenAdminKeyGenerator()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // VIP Banner / Lock Icon (Tapping 10 times opens secret Admin Key Generator)
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFFEAB308).copy(alpha = 0.15f))
                .border(2.dp, Color(0xFFEAB308), CircleShape)
                .clickable { onVipCrownTapped() },
            contentAlignment = Alignment.Center
        ) {
            Text(text = "👑", fontSize = 28.sp)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { onVipCrownTapped() }
        ) {
            Text(
                text = if (isTagalog) "VIP FEATURE ACTIVATION" else "VIP FEATURE ACTIVATION",
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                fontSize = 16.sp,
                color = Color(0xFFEAB308)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isTagalog)
                    "I-unlock ang walang limitasyong YouTube Karaoke & Video Background Downloader para sa iyong device!"
                else
                    "Unlock unlimited YouTube Karaoke & Video Background Downloader for your device!",
                fontSize = 11.5.sp,
                color = Color(theme.textMutedColor),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        // Card 1: Unique Device ID Box with Copy Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0F172A))
                .border(1.5.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (isTagalog) "📱 IYONG DEVICE ID (Ipadala sa GCash/Maya):" else "📱 YOUR DEVICE ID (Send via GCash/Maya):",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(theme.primaryColor)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = deviceId.ifBlank { "VK-DEVICE-XXXX" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )

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
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isTagalog) "📋 KOPYAHIN" else "📋 COPY ID",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(theme.primaryColor)
                    )
                }
            }
        }

        // Card 2: 1-Time Lifetime Activation Instructions & Payment Details
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF161B2E))
                .border(1.5.dp, Color(0xFFEAB308).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "💰", fontSize = 16.sp)
                    Text(
                        text = if (isTagalog) "1-TIME FEE: ₱99 LAMANG" else "1-TIME FEE: ₱99 ONLY",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFEAB308)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF00E676).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFF00E676).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isTagalog) "WALANG MONTHLY" else "NO MONTHLY",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF00E676)
                    )
                }
            }

            // Dedicated Account Number and Name Box with 1-Tap Copy
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isTagalog) "📲 GCash / Maya Payment Details:" else "📲 GCash / Maya Payment Details:",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                    Text(
                        text = "Amount: ₱99.00",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFEAB308)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "09203732202",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFFACC15)
                        )
                        Text(
                            text = "Name: J***** H.",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }

                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString("09203732202"))
                            Toast.makeText(
                                context,
                                if (isTagalog) "Nakopya ang account number!" else "Account number copied!",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isTagalog) "📋 KOPYAHIN" else "📋 COPY",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    }
                }
            }

            // Dedicated Facebook / Messenger Contact Box with 1-Tap Open & Copy
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color(0xFF1877F2).copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(text = "🌐", fontSize = 12.sp)
                        Text(
                            text = if (isTagalog) "Facebook Contact & Support:" else "Facebook Contact & Support:",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    }
                    Text(
                        text = if (isTagalog) "I-send dito ang Device ID" else "Send Device ID here",
                        fontSize = 9.5.sp,
                        color = Color(0xFF00FF88),
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "https://www.facebook.com/jehuet27",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF60A5FA),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Jerome Huet",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString("https://www.facebook.com/jehuet27"))
                                Toast.makeText(
                                    context,
                                    if (isTagalog) "Nakopya ang Facebook link!" else "Facebook link copied!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = "📋",
                                fontSize = 11.sp
                            )
                        }

                        Button(
                            onClick = {
                                try {
                                    val fbIntent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://www.facebook.com/jehuet27")
                                    ).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(fbIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = if (isTagalog) "💬 I-CHAT" else "💬 CHAT",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Text(
                text = if (isTagalog)
                    "1. Magbayad ng ₱99 (One-Time Lifetime Access) sa GCash o Maya sa itaas.\n2. Pindutin ang '💬 I-CHAT' sa itaas para mag-message sa Facebook (facebook.com/jehuet27) at i-send ang iyong Device ID kasama ang resibo upang matanggap agad ang iyong 8-digit Activation Key."
                else
                    "1. Send ₱99 (One-Time Lifetime Access) to GCash or Maya details above.\n2. Tap '💬 CHAT' above to message on Facebook (facebook.com/jehuet27) and send your Device ID with payment proof to receive your instant 8-digit Activation Key.",
                fontSize = 11.sp,
                color = Color(theme.textColor),
                lineHeight = 16.sp
            )
        }

        // Card 3: Activation Key Input Field & Submit
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0F172A))
                .border(1.5.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (isTagalog) "🔑 ILAGAY ANG 8-DIGIT ACTIVATION KEY:" else "🔑 ENTER 8-DIGIT ACTIVATION KEY:",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            OutlinedTextField(
                value = enteredKey,
                onValueChange = {
                    enteredKey = it.uppercase().take(14)
                    onClearError()
                },
                placeholder = {
                    Text(
                        text = "XXXX-XXXX",
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(theme.textMutedColor).copy(alpha = 0.6f)
                    )
                },
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1E293B),
                    unfocusedContainerColor = Color(0xFF1E293B),
                    focusedBorderColor = Color(0xFFEAB308),
                    unfocusedBorderColor = Color(0xFF334155)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (!error.isNullOrBlank()) {
                Text(
                    text = "⚠️ $error",
                    color = Color(0xFFEF4444),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAB308)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text(
                    text = if (isTagalog) "⚡ I-ACTIVATE ANG VIP" else "⚡ ACTIVATE VIP NOW",
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = Color(0xFF0F172A)
                )
            }
        }

        Text(
            text = if (isTagalog)
                "🔒 Offline Cryptographic License • Walang internet na kailangan para ma-verify."
            else
                "🔒 Offline Cryptographic License • No internet connection needed to verify.",
            fontSize = 9.5.sp,
            color = Color(theme.textMutedColor).copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
    }
}

