package com.example.videokey.ui.main

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.videokey.data.Song
import java.util.Locale

@Composable
fun VipMp3PlayerDialog(
    state: VideoKeyUiState,
    isLandscape: Boolean = false,
    onPlayMp3: (Song) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeek: (Int) -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onRescan: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = state.appTheme
    val isTagalog = state.appLanguage == AppLanguage.TAGALOG
    var searchQuery by remember { mutableStateOf("") }

    val filteredMp3s = remember(state.mp3PlayerSongList, searchQuery) {
        if (searchQuery.isBlank()) state.mp3PlayerSongList
        else {
            val q = searchQuery.trim().lowercase()
            state.mp3PlayerSongList.filter {
                it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) || it.code.contains(q)
            }
        }
    }

    // Vinyl spinning rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_spin")
    val vinylAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinyl_angle"
    )

    fun formatDuration(ms: Int): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format(Locale.US, "%02d:%02d", min, sec)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(theme.surfaceColor).copy(alpha = 0.98f),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFEAB308)),
            modifier = modifier
                .fillMaxWidth(if (isLandscape) 0.92f else 0.95f)
                .fillMaxHeight(if (isLandscape) 0.88f else 0.90f)
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEAB308).copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEAB308))
                        ) {
                            Text(
                                text = "👑 VIP",
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = Color(0xFFFDE047),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = if (isTagalog) "VIP MP3 AUDIO PLAYER" else "VIP MP3 AUDIO PLAYER",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Text("✕", color = Color(theme.textMutedColor), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (isLandscape) {
                    // Landscape Mode: Side-by-side (Left: Now Playing & Vinyl / Right: Playlist & Search)
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Left: Now Playing Player Card
                        Column(
                            modifier = Modifier
                                .weight(0.48f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF0F172A))
                                .border(1.dp, Color(theme.buttonBorderColor), RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Rotating Vinyl Graphic
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color.Black)
                                        )
                                    )
                                    .border(2.dp, Color(0xFFEAB308).copy(alpha = 0.7f), CircleShape)
                                    .rotate(if (state.isMp3Playing) vinylAngle else 0f),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(theme.primaryColor)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🎵", fontSize = 16.sp)
                                }
                            }

                            // Song Title & Artist
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = state.activeMp3Song?.title ?: (if (isTagalog) "Walang pinatutugtog" else "No track playing"),
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = state.activeMp3Song?.artist ?: "Select an MP3 track below",
                                    color = Color(theme.primaryColor),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Seek bar & Timestamps
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Slider(
                                    value = if (state.mp3DurationMs > 0) state.mp3CurrentPositionMs.toFloat() else 0f,
                                    onValueChange = { onSeek(it.toInt()) },
                                    valueRange = 0f..(if (state.mp3DurationMs > 0) state.mp3DurationMs.toFloat() else 1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFEAB308),
                                        activeTrackColor = Color(0xFFEAB308),
                                        inactiveTrackColor = Color(0xFF334155)
                                    ),
                                    modifier = Modifier.fillMaxWidth().height(20.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = formatDuration(state.mp3CurrentPositionMs),
                                        fontSize = 10.sp,
                                        color = Color(theme.textMutedColor)
                                    )
                                    Text(
                                        text = formatDuration(state.mp3DurationMs),
                                        fontSize = 10.sp,
                                        color = Color(theme.textMutedColor)
                                    )
                                }
                            }

                            // Playback Buttons
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = onPrev) {
                                    Text("⏮", fontSize = 20.sp, color = Color.White)
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFEAB308),
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clickable { onTogglePlayPause() }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = if (state.isMp3Playing) "⏸" else "▶",
                                            fontSize = 20.sp,
                                            color = Color.Black,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                                IconButton(onClick = onNext) {
                                    Text("⏭", fontSize = 20.sp, color = Color.White)
                                }
                            }
                        }

                        // Right: Song Search & Playlist
                        Column(
                            modifier = Modifier
                                .weight(0.52f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                leadingIcon = { Text("🔍", fontSize = 14.sp) },
                                placeholder = {
                                    Text(
                                        text = if (isTagalog) "Maghanap ng titulo, artist, o MP3..." else "Search title, artist, or MP3...",
                                        fontSize = 12.sp,
                                        color = Color(theme.textMutedColor)
                                    )
                                },
                                singleLine = true,
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(28.dp)) {
                                                Text("✕", color = Color(theme.primaryColor), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        IconButton(onClick = onRescan, modifier = Modifier.size(28.dp)) {
                                            Text("🔄", fontSize = 14.sp)
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(theme.primaryColor),
                                    unfocusedBorderColor = Color(theme.buttonBorderColor),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF0F172A),
                                    unfocusedContainerColor = Color(0xFF0F172A)
                                ),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            )
                            Text(
                                text = "${filteredMp3s.size} MP3 tracks",
                                fontSize = 10.5.sp,
                                color = Color(theme.primaryColor),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                itemsIndexed(filteredMp3s, key = { index, song -> "${song.filePath}_${song.code}_$index" }) { _, song ->
                                    val isSelected = state.activeMp3Song?.filePath == song.filePath
                                    Mp3SongItemCard(
                                        song = song,
                                        isSelected = isSelected,
                                        isPlaying = isSelected && state.isMp3Playing,
                                        theme = theme,
                                        onClick = { onPlayMp3(song) }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Portrait Mode: Vertical layout
                    // 1. Player Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, Color(theme.buttonBorderColor), RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color.Black)
                                        )
                                    )
                                    .border(2.dp, Color(0xFFEAB308).copy(alpha = 0.7f), CircleShape)
                                    .rotate(if (state.isMp3Playing) vinylAngle else 0f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🎵", fontSize = 18.sp)
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = state.activeMp3Song?.title ?: (if (isTagalog) "Walang pinatutugtog" else "No track playing"),
                                    color = Color.White,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = state.activeMp3Song?.artist ?: "Pumili ng MP3 track sa ibaba",
                                    color = Color(theme.primaryColor),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFEAB308),
                                modifier = Modifier
                                    .size(42.dp)
                                    .clickable { onTogglePlayPause() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (state.isMp3Playing) "⏸" else "▶",
                                        fontSize = 18.sp,
                                        color = Color.Black,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }

                        // Seek bar & Timestamps
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Slider(
                                value = if (state.mp3DurationMs > 0) state.mp3CurrentPositionMs.toFloat() else 0f,
                                onValueChange = { onSeek(it.toInt()) },
                                valueRange = 0f..(if (state.mp3DurationMs > 0) state.mp3DurationMs.toFloat() else 1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFEAB308),
                                    activeTrackColor = Color(0xFFEAB308),
                                    inactiveTrackColor = Color(0xFF334155)
                                ),
                                modifier = Modifier.fillMaxWidth().height(18.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatDuration(state.mp3CurrentPositionMs),
                                    fontSize = 10.sp,
                                    color = Color(theme.textMutedColor)
                                )
                                Text(
                                    text = formatDuration(state.mp3DurationMs),
                                    fontSize = 10.sp,
                                    color = Color(theme.textMutedColor)
                                )
                            }
                        }
                    }

                    // 2. Search & Rescan with instant clear & count
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            leadingIcon = { Text("🔍", fontSize = 14.sp) },
                            placeholder = {
                                Text(
                                    text = if (isTagalog) "Maghanap ng kanta o artist..." else "Search MP3 title or artist...",
                                    fontSize = 12.sp,
                                    color = Color(theme.textMutedColor)
                                )
                            },
                            singleLine = true,
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(28.dp)) {
                                        Text("✕", color = Color(theme.primaryColor), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(theme.primaryColor),
                                unfocusedBorderColor = Color(theme.buttonBorderColor),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A)
                            ),
                            modifier = Modifier.weight(1f).height(48.dp)
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(theme.buttonBgColor),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(theme.buttonBorderColor)),
                            modifier = Modifier.height(48.dp).clickable { onRescan() }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                                Text("🔄", fontSize = 16.sp)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isTagalog) "PLAYLIST NG MGA KANTA" else "LOCAL MP3 PLAYLIST",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(theme.textMutedColor)
                        )
                        Text(
                            text = "${filteredMp3s.size} MP3s",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(theme.primaryColor)
                        )
                    }

                    // 3. Playlist
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(filteredMp3s, key = { index, song -> "${song.filePath}_${song.code}_$index" }) { _, song ->
                            val isSelected = state.activeMp3Song?.filePath == song.filePath
                            Mp3SongItemCard(
                                song = song,
                                isSelected = isSelected,
                                isPlaying = isSelected && state.isMp3Playing,
                                theme = theme,
                                onClick = { onPlayMp3(song) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Mp3SongItemCard(
    song: Song,
    isSelected: Boolean,
    isPlaying: Boolean,
    theme: AppTheme,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) Color(0xFFEAB308).copy(alpha = 0.18f) else Color(theme.buttonBgColor))
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) Color(0xFFEAB308) else Color(theme.buttonBorderColor),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (isPlaying) "🔊" else "🎵",
                fontSize = 15.sp
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = if (isSelected) Color(0xFFFDE047) else Color(theme.textColor),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    color = Color(theme.textMutedColor),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (isSelected) {
            Text(
                text = if (isPlaying) "PLAYING" else "PAUSED",
                color = Color(0xFFEAB308),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}
