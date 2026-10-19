package com.example.videokey.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.videokey.R
import com.example.videokey.data.Song

private val cleanFont = FontFamily(Font(R.font.radio_canada))

/**
 * Extracts 1-2 letter uppercase initials from the user's profile name.
 */
fun getInitials(name: String): String {
    return name.trim().split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .map { it.first().toString() }
        .take(2)
        .joinToString("")
        .uppercase()
        .ifEmpty { "JH" }
}

/**
 * SingPulse Bottom Navigation Bar (4 Tabs: Home, Sing, Saved, Profile)
 * Synchronized with the active AppTheme.
 */
@Composable
fun SingPulseBottomNavBar(
    selectedTab: Int,
    isPlaying: Boolean,
    reserveCount: Int,
    theme: AppTheme = AppTheme.CYBER_NEON,
    isTagalog: Boolean = false,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val navItems = listOf(
        Triple(0, if (isTagalog) "Tahanan" else "Home", "🏠"),
        Triple(1, if (isTagalog) "Kantahan" else "Sing", "🎤"),
        Triple(2, if (isTagalog) "Naka-save" else "Saved", "💖"),
        Triple(3, "Profile", "👤")
    )

    val bgColor = Color(theme.backgroundColor)
    val borderColor = Color(theme.buttonBorderColor)
    val activeColor = Color(theme.primaryColor)
    val inactiveColor = Color(theme.textMutedColor)

    Surface(
        color = bgColor.copy(alpha = 0.98f),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { (index, title, icon) ->
                val isSelected = selectedTab == index

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectTab(index) }
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        Text(
                            text = icon,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(2.dp)
                        )
                        // Sing tab live pulse dot when playing
                        if (index == 1 && isPlaying) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(activeColor)
                            )
                        }
                        // Saved tab badge count if queue has items
                        if (index == 2 && reserveCount > 0) {
                            Box(
                                modifier = Modifier
                                    .offset(x = 6.dp, y = (-2).dp)
                                    .clip(CircleShape)
                                    .background(activeColor)
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "$reserveCount",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Text(
                        text = title,
                        color = if (isSelected) activeColor else inactiveColor,
                        fontFamily = cleanFont,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Luminance-based contrast helper to guarantee 100% visible text regardless of theme brightness.
 */
fun getContrastingTextColor(color: Color): Color {
    val luminance = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
    return if (luminance > 0.55f) Color(0xFF090D16) else Color.White
}

fun getContrastingTextColor(colorLong: Long): Color {
    val r = ((colorLong shr 16) and 0xFF) / 255f
    val g = ((colorLong shr 8) and 0xFF) / 255f
    val b = (colorLong and 0xFF) / 255f
    val luminance = 0.299f * r + 0.587f * g + 0.114f * b
    return if (luminance > 0.55f) Color(0xFF090D16) else Color.White
}

/**
 * SingPulse Tab 0: Home Screen (Browse, Search, Categories, Huling Kinanta, Trending)
 */
@Composable
fun SingPulseHomeScreen(
    state: VideoKeyUiState,
    onQueryChange: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
    onPlaySong: (Song) -> Unit,
    onReserveSong: (Song) -> Unit,
    onEnterTvMode: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenEditName: () -> Unit = onOpenProfile,
    onOpenYouTubeDownloader: () -> Unit = {},
    onSpeakDigit: ((Char) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val theme = state.appTheme
    val bgColor = Color(theme.backgroundColor)
    val cardBg = Color(theme.surfaceColor)
    val cardBorder = Color(theme.buttonBorderColor)
    val primaryColor = Color(theme.primaryColor)
    val secondaryColor = Color(theme.secondaryColor)
    val buttonBgColor = Color(theme.buttonBgColor)
    val textColor = Color(theme.textColor)
    val textMutedColor = Color(theme.textMutedColor)

    val isTagalog = state.appLanguage == AppLanguage.TAGALOG
    val categories = listOf(
        Pair("ALL", if (isTagalog) "🔥 Lahat" else "🔥 All Hits"),
        Pair("OPM", "🇵🇭 OPM Pride"),
        Pair("Rock", "🎸 Rock Anthems"),
        Pair("Pop", "🎤 Pop Ballads"),
        Pair("Medley", "🎶 Medley")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // 1. Header Greeting & Avatar + YT Downloader Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.clickable { onOpenEditName() }
            ) {
                Text(
                    text = if (isTagalog) "Maligayang pagbabalik," else "Welcome back,",
                    color = textMutedColor,
                    fontSize = 12.sp,
                    fontFamily = cleanFont,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${state.userName} ✨",
                    fontSize = 20.sp,
                    fontFamily = cleanFont,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFDC2626),
                    modifier = Modifier.clickable { onOpenYouTubeDownloader() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("▶", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Text("YT & MP4", color = Color.White, fontSize = 11.sp, fontFamily = cleanFont, fontWeight = FontWeight.Bold)
                    }
                }

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(primaryColor, secondaryColor))
                        )
                        .clickable { onOpenProfile() }
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(cardBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = getInitials(state.userName), color = primaryColor, fontFamily = cleanFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Search Bar (Directly bound to state.songbookQuery with clean theme typography & digit voice)
        OutlinedTextField(
            value = state.songbookQuery,
            onValueChange = { newText ->
                if (newText.length > state.songbookQuery.length) {
                    val addedChar = newText.last()
                    if (addedChar in '0'..'9') {
                        onSpeakDigit?.invoke(addedChar)
                    }
                }
                onQueryChange(newText)
            },
            textStyle = TextStyle(
                fontFamily = cleanFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = textColor
            ),
            placeholder = {
                Text(
                    text = if (isTagalog) "Maghanap ng titulo, artist, o numero ng kanta..." else "Search title, artist, or song code...",
                    fontFamily = cleanFont,
                    color = textMutedColor.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            },
            leadingIcon = { Text("🔍", fontSize = 14.sp) },
            trailingIcon = {
                if (state.songbookQuery.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                onQueryChange(state.songbookQuery.dropLast(1))
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Text("⌫", color = Color(0xFFFFD54F), fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                        IconButton(
                            onClick = {
                                onQueryChange("")
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Text("✕", color = primaryColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = cardBg,
                unfocusedContainerColor = cardBg,
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = cardBorder,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Category Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { (catId, catLabel) ->
                val isSelected = state.selectedCategoryFilter == catId
                val chipTextColor = if (isSelected) getContrastingTextColor(primaryColor) else textMutedColor
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) {
                                Brush.horizontalGradient(listOf(primaryColor, secondaryColor))
                            } else {
                                Brush.horizontalGradient(listOf(cardBg, cardBg))
                            }
                        )
                        .border(
                            1.dp,
                            if (isSelected) primaryColor else cardBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectCategory(catId) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = catLabel,
                        color = chipTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Featured Card -> Huling Kinanta (Last Sung Song)
        val featuredSong = state.lastPlayedSong ?: state.currentSong
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                Brush.horizontalGradient(listOf(primaryColor.copy(alpha = 0.7f), secondaryColor.copy(alpha = 0.7f)))
            ),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(primaryColor)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isTagalog) "HULING KINANTA" else "LAST SUNG SONG",
                        color = getContrastingTextColor(primaryColor),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = featuredSong.title,
                    color = textColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${featuredSong.artist} • Track #${featuredSong.code}",
                    color = textMutedColor,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { onPlaySong(featuredSong) },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (isTagalog) "🎤 Kantahin Muli" else "🎤 Sing Again",
                            color = getContrastingTextColor(primaryColor),
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                    OutlinedButton(
                        onClick = onEnterTvMode,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = secondaryColor),
                        border = androidx.compose.foundation.BorderStroke(1.dp, secondaryColor.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "⛶ TV Mode", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Trending Songs List Header
        val displaySongs = if (state.songbookQuery.isNotBlank()) state.searchResults else state.trendingSongs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isTagalog) "MGA SIKAT NA KANTA" else "TRENDING KARAOKE SONGS",
                color = textMutedColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Text(
                text = if (isTagalog) "${displaySongs.size} Kanta" else "${displaySongs.size} Songs",
                color = primaryColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 6. Songs LazyColumn (Randomized per open or filtered)
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(displaySongs.take(60)) { song ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(cardBg)
                        .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(secondaryColor.copy(alpha = 0.16f))
                                    .border(0.8.dp, secondaryColor.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = song.code,
                                    color = secondaryColor,
                                    fontSize = 11.sp,
                                    fontFamily = cleanFont,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = song.title,
                                color = textColor,
                                fontFamily = cleanFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (song.artist.isNotBlank()) "🎤 ${song.artist}" else "🎤 OPM / Karaoke",
                            color = textMutedColor,
                            fontFamily = cleanFont,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(primaryColor)
                            .clickable { onPlaySong(song) }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isTagalog) "Kanta" else "Sing",
                            color = getContrastingTextColor(primaryColor),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * SingPulse Tab 2: Saved Songs Screen (Reserved Queue & Favorites)
 */
@Composable
fun SingPulseSavedScreen(
    state: VideoKeyUiState,
    onPlaySong: (Song) -> Unit,
    onRemoveReserved: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSavedTab by remember { mutableStateOf(0) } // 0 = Reserved Queue, 1 = Favorites
    val isTagalog = state.appLanguage == AppLanguage.TAGALOG
    val theme = state.appTheme
    val bgColor = Color(theme.backgroundColor)
    val cardBg = Color(theme.surfaceColor)
    val cardBorder = Color(theme.buttonBorderColor)
    val primaryColor = Color(theme.primaryColor)
    val secondaryColor = Color(theme.secondaryColor)
    val textColor = Color(theme.textColor)
    val textMutedColor = Color(theme.textMutedColor)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Text(
            text = if (isTagalog) "Mga Naka-save na Kanta" else "Saved Songs",
            fontSize = 22.sp,
            fontFamily = cleanFont,
            fontWeight = FontWeight.ExtraBold,
            color = textColor
        )
        Text(
            text = if (isTagalog) "Iyong personal na karaoke songbook at nakareserbang pila" else "Your personal karaoke songbook & active queue",
            fontSize = 12.sp,
            fontFamily = cleanFont,
            color = textMutedColor
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Switcher Tab
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(cardBg)
                .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selectedSavedTab == 0) primaryColor else Color.Transparent)
                    .clickable { selectedSavedTab = 0 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isTagalog) "📋 Nakareserba (${state.reserveQueue.size})" else "📋 Reserved (${state.reserveQueue.size})",
                    color = if (selectedSavedTab == 0) getContrastingTextColor(primaryColor) else textMutedColor,
                    fontFamily = cleanFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selectedSavedTab == 1) primaryColor else Color.Transparent)
                    .clickable { selectedSavedTab = 1 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isTagalog) "❤️ Mga Paborito (${state.favoriteSongs.size})" else "❤️ Favorites (${state.favoriteSongs.size})",
                    color = if (selectedSavedTab == 1) getContrastingTextColor(primaryColor) else textMutedColor,
                    fontFamily = cleanFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        val songsList = if (selectedSavedTab == 0) state.reserveQueue else state.favoriteSongs

        if (songsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = if (selectedSavedTab == 0) "📋" else "❤️", fontSize = 38.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (selectedSavedTab == 0) {
                            if (isTagalog) "Walang nakapilang kanta sa Reserve" else "No songs in reserve queue"
                        } else {
                            if (isTagalog) "Walang laman ang Mga Paborito" else "No favorite songs yet"
                        },
                        color = textMutedColor,
                        fontFamily = cleanFont,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(songsList.size) { index ->
                    val song = songsList[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(cardBg)
                            .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(secondaryColor.copy(alpha = 0.18f))
                                        .border(0.8.dp, secondaryColor.copy(alpha = 0.5f), RoundedCornerShape(5.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = song.code,
                                        color = secondaryColor,
                                        fontFamily = cleanFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = song.title,
                                    color = textColor,
                                    fontFamily = cleanFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (song.artist.isNotBlank()) "🎤 ${song.artist}" else "🎤 OPM / Videoke",
                                color = textMutedColor,
                                fontFamily = cleanFont,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(primaryColor)
                                    .clickable { onPlaySong(song) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isTagalog) "▶ Patugtugin" else "▶ Play",
                                    color = getContrastingTextColor(primaryColor),
                                    fontFamily = cleanFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                            if (selectedSavedTab == 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFEF4444).copy(alpha = 0.18f))
                                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .clickable { onRemoveReserved(index) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "✕",
                                        color = Color(0xFFFF5252),
                                        fontFamily = cleanFont,
                                        fontWeight = FontWeight.Bold,
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
}

/**
 * SingPulse Tab 3: Profile & Studio Settings Screen
 */
@Composable
fun SingPulseProfileScreen(
    state: VideoKeyUiState,
    onEnterTvMode: () -> Unit,
    onOpenSoundFontDialog: () -> Unit,
    onOpenAudioMixerDialog: () -> Unit,
    onOpenBackgroundDialog: () -> Unit,
    onOpenSettingsDialog: () -> Unit,
    onSelectTheme: (AppTheme) -> Unit,
    onToggleRecording: () -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit = {},
    onOpenEditName: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val theme = state.appTheme
    val isTagalog = state.appLanguage == AppLanguage.TAGALOG
    val bgColor = Color(theme.backgroundColor)
    val cardBg = Color(theme.surfaceColor)
    val cardBorder = Color(theme.buttonBorderColor)
    val primaryColor = Color(theme.primaryColor)
    val secondaryColor = Color(theme.secondaryColor)
    val textColor = Color(theme.textColor)
    val textMutedColor = Color(theme.textMutedColor)
    val buttonBgColor = Color(theme.buttonBgColor)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Profile Card
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(primaryColor, secondaryColor)))
                        .clickable { onOpenEditName() }
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(bgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = getInitials(state.userName), color = primaryColor, fontWeight = FontWeight.Black, fontSize = 22.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenEditName() }
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(text = state.userName, color = textColor, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "✏️", fontSize = 13.sp)
                }
                Text(text = "✨ VIP Karaoke Superstar • Lvl 24", color = primaryColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "${state.searchResults.size}", color = textColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(text = if (isTagalog) "MGA KANTA" else "SONGS", color = textMutedColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = state.activeSoundFontName.take(8), color = secondaryColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(text = "SOUNDFONT", color = textMutedColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "98%", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(text = "PITCH AVG", color = textMutedColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Dedicated Profile Username Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenEditName() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isTagalog) "👤 PANGALAN NG PROFILE" else "👤 PROFILE USERNAME",
                        color = textMutedColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = state.userName,
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = if (isTagalog) "Pindutin para palitan ang iyong pangalan" else "Tap to change your username anytime",
                        color = textMutedColor,
                        fontSize = 11.sp
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = primaryColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor)
                ) {
                    Text(
                        text = if (isTagalog) "PALITAN ✏️" else "CHANGE ✏️",
                        color = primaryColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Language Switcher Card (Switch between English & Tagalog)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isTagalog) "🌐 WIKA NG APP (APP LANGUAGE)" else "🌐 APP LANGUAGE",
                    color = textMutedColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // English Button (Default)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (!isTagalog) primaryColor else buttonBgColor)
                            .border(1.dp, if (!isTagalog) primaryColor else cardBorder, RoundedCornerShape(12.dp))
                            .clickable { onSelectLanguage(AppLanguage.ENGLISH) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🇺🇸 English (Default)",
                            color = if (!isTagalog) getContrastingTextColor(primaryColor) else textMutedColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    // Tagalog Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isTagalog) primaryColor else buttonBgColor)
                            .border(1.dp, if (isTagalog) primaryColor else cardBorder, RoundedCornerShape(12.dp))
                            .clickable { onSelectLanguage(AppLanguage.TAGALOG) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🇵🇭 Tagalog",
                            color = if (isTagalog) getContrastingTextColor(primaryColor) else textMutedColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Theme Switcher Card (Dynamic Color Reactive)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isTagalog) "🎨 PINILING TEMA (APP THEME)" else "🎨 APP THEMES",
                    color = textMutedColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(AppTheme.values()) { t ->
                        val isSelected = state.appTheme == t
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(t.primaryColor).copy(alpha = 0.2f) else buttonBgColor)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(t.primaryColor) else cardBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onSelectTheme(t) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(t.primaryColor))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = t.label(isTagalog),
                                color = if (isSelected) Color.White else textMutedColor,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Karaoke Studio Settings Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isTagalog) "🎛️ MGA KONTROL SA STUDIO" else "🎛️ KARAOKE STUDIO CONTROLS",
                    color = textMutedColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )

                // TV Mode Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(buttonBgColor)
                        .clickable { onEnterTvMode() }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "📺 Fullscreen TV Mode", color = textColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            text = if (isTagalog) "I-rotate ang screen na may 130% laking lyrics" else "Rotate screen with extra large 130% lyrics",
                            color = textMutedColor,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = if (isTagalog) "BUKSAN" else "OPEN",
                        color = secondaryColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Audio Mixer Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(buttonBgColor)
                        .clickable { onOpenAudioMixerDialog() }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "🎚️ Audio & Instrument Mixer", color = textColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            text = if (isTagalog) "Reverb, Drums, Bass, Gitara, Strings" else "Reverb, Drums, Bass, Guitars, Strings",
                            color = textMutedColor,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = if (isTagalog) "ITONO" else "TUNE",
                        color = primaryColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // SoundFont Selector Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(buttonBgColor)
                        .clickable { onOpenSoundFontDialog() }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "🎹 SoundFont Synthesizer", color = textColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(text = state.activeSoundFontName, color = secondaryColor, fontSize = 11.sp)
                    }
                    Text(
                        text = if (isTagalog) "PALITAN" else "CHANGE",
                        color = secondaryColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Background Video Changer Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(buttonBgColor)
                        .clickable { onOpenBackgroundDialog() }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "🎬 Background Video & Wallpaper", color = textColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            text = if (isTagalog) "Palitan ang background video loop" else "Change background video loop",
                            color = textMutedColor,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = if (isTagalog) "PUMILI" else "SELECT",
                        color = secondaryColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // App Settings Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(buttonBgColor)
                        .clickable { onOpenSettingsDialog() }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isTagalog) "⚙️ Mga Setting ng App" else "⚙️ Advanced Settings",
                            color = textColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (isTagalog) "QR remote, voice prompts, tunog ng scoring" else "QR remote, voice prompts, scoring sound",
                            color = textMutedColor,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = if (isTagalog) "I-AYOS" else "CONFIG",
                        color = textMutedColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(60.dp))
    }
}

/**
 * Dialog to edit user profile name with live initials preview and save to preferences.
 */
@Composable
fun EditProfileNameDialog(
    currentName: String,
    theme: AppTheme = AppTheme.CYBER_NEON,
    isTagalog: Boolean = false,
    onSaveName: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var nameInput by remember(currentName) { mutableStateOf(currentName) }
    val bgColor = Color(theme.surfaceColor)
    val cardBorder = Color(theme.buttonBorderColor)
    val primaryColor = Color(theme.primaryColor)
    val secondaryColor = Color(theme.secondaryColor)
    val textColor = Color(theme.textColor)
    val textMutedColor = Color(theme.textMutedColor)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = bgColor,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, primaryColor),
            shadowElevation = 16.dp,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header & Avatar Preview
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(primaryColor, secondaryColor)))
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(theme.backgroundColor)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = getInitials(nameInput),
                            color = primaryColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        )
                    }
                }

                Text(
                    text = if (isTagalog) "Palitan ang Pangalan" else "Edit Profile Name",
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    fontFamily = cleanFont
                )

                Text(
                    text = if (isTagalog)
                        "I-type ang iyong pangalan o palayaw na gagamitin sa karaoke:"
                    else
                        "Enter your name or nickname to display across SingPulse:",
                    color = textMutedColor,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    fontFamily = cleanFont
                )

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { if (it.length <= 30) nameInput = it },
                    singleLine = true,
                    placeholder = { Text("e.g. Maria Santos", color = textMutedColor) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = cardBorder,
                        focusedContainerColor = Color(theme.backgroundColor).copy(alpha = 0.5f),
                        unfocusedContainerColor = Color(theme.backgroundColor).copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textMutedColor),
                        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
                    ) {
                        Text(if (isTagalog) "Kanselahin" else "Cancel", fontFamily = cleanFont, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val trimmed = nameInput.trim()
                            if (trimmed.isNotEmpty()) {
                                onSaveName(trimmed)
                            }
                        },
                        enabled = nameInput.trim().isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = getContrastingTextColor(primaryColor)
                        )
                    ) {
                        Text(if (isTagalog) "I-save" else "Save", fontFamily = cleanFont, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

