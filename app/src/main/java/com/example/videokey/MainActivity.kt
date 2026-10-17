package com.example.videokey

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.videokey.data.Song
import com.example.videokey.theme.VideoKeyTheme
import com.example.videokey.ui.main.VideoKeyViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: VideoKeyViewModel by viewModels()

    private var remoteServer: com.example.videokey.remote.VideokeRemoteServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start Built-in Wi-Fi Videoke Remote Server on port 8080
        remoteServer = com.example.videokey.remote.VideokeRemoteServer(viewModel)
        remoteServer?.start()

        // Request runtime permissions
        requestPermissionsIfNeeded()

        enableEdgeToEdge()

        // Keep screen awake at all times while VideoKey is running
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            VideoKeyTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainNavigation(viewModel = viewModel)
                }
            }
        }

        handleIntent(intent)
    }

    override fun onPause() {
        super.onPause()
        viewModel.onAppPause()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAppResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        remoteServer?.stop()
    }

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        val state = viewModel.uiState.value
        // When Songbook search is open, let Compose OutlinedTextField handle Backspace (KEYCODE_DEL), digits, and typing first!
        if (state.isSongbookOpen) {
            if (event.keyCode == android.view.KeyEvent.KEYCODE_BACK && event.action == android.view.KeyEvent.ACTION_DOWN) {
                viewModel.closeSongbook()
                return true
            }
            return super.dispatchKeyEvent(event)
        }

        if (event.action == android.view.KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                in android.view.KeyEvent.KEYCODE_0..android.view.KeyEvent.KEYCODE_9 -> {
                    val digit = '0' + (event.keyCode - android.view.KeyEvent.KEYCODE_0)
                    viewModel.onDigitPressed(digit)
                    return true
                }
                in android.view.KeyEvent.KEYCODE_NUMPAD_0..android.view.KeyEvent.KEYCODE_NUMPAD_9 -> {
                    val digit = '0' + (event.keyCode - android.view.KeyEvent.KEYCODE_NUMPAD_0)
                    viewModel.onDigitPressed(digit)
                    return true
                }
                android.view.KeyEvent.KEYCODE_MEDIA_PLAY, android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                    viewModel.onPlay()
                    return true
                }
                android.view.KeyEvent.KEYCODE_MEDIA_NEXT, android.view.KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, android.view.KeyEvent.KEYCODE_MEDIA_STEP_FORWARD -> {
                    viewModel.onNext()
                    return true
                }
                android.view.KeyEvent.KEYCODE_MEDIA_STOP -> {
                    viewModel.onStop()
                    return true
                }
                android.view.KeyEvent.KEYCODE_DEL, android.view.KeyEvent.KEYCODE_FORWARD_DEL -> {
                    viewModel.onDeleteDigit()
                    return true
                }
                android.view.KeyEvent.KEYCODE_CLEAR -> {
                    viewModel.onClearCode()
                    return true
                }
                android.view.KeyEvent.KEYCODE_ENTER, android.view.KeyEvent.KEYCODE_NUMPAD_ENTER, android.view.KeyEvent.KEYCODE_DPAD_CENTER -> {
                    if (viewModel.uiState.value.enteredCode.isNotEmpty()) {
                        viewModel.onPlay()
                        return true
                    } else if (!viewModel.uiState.value.isPlaying) {
                        viewModel.onPlay()
                        return true
                    } else {
                        viewModel.onNext()
                        return true
                    }
                }
                android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                    viewModel.adjustKey(-1)
                    return true
                }
                android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    viewModel.adjustKey(1)
                    return true
                }
                android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                    viewModel.adjustTempo(5)
                    return true
                }
                android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                    viewModel.adjustTempo(-5)
                    return true
                }
                android.view.KeyEvent.KEYCODE_MENU -> {
                    viewModel.openSettingsDialog()
                    return true
                }
                android.view.KeyEvent.KEYCODE_SEARCH -> {
                    viewModel.openSongbook()
                    return true
                }
                android.view.KeyEvent.KEYCODE_BACK -> {
                    if (viewModel.uiState.value.isTvMode) {
                        viewModel.toggleTvMode()
                        return true
                    }
                }
                android.view.KeyEvent.KEYCODE_PROG_RED -> {
                    viewModel.openSongbook()
                    return true
                }
                android.view.KeyEvent.KEYCODE_PROG_GREEN -> {
                    viewModel.onReserve()
                    return true
                }
                android.view.KeyEvent.KEYCODE_PROG_YELLOW -> {
                    viewModel.openBackgroundDialog()
                    return true
                }
                android.view.KeyEvent.KEYCODE_PROG_BLUE -> {
                    viewModel.openSettingsDialog()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        android.util.Log.d("MainActivity", "onNewIntent extras: ${intent.extras}")
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        android.util.Log.d("MainActivity", "handleIntent extras: ${intent?.extras}")
        val playCode = intent?.getStringExtra("play_code")
        if (!playCode.isNullOrBlank()) {
            lifecycleScope.launch {
                var song = viewModel.findSongByCode(playCode)
                if (song == null) {
                    var attempts = 0
                    while (attempts < 40) {
                        delay(100)
                        song = viewModel.findSongByCode(playCode)
                        if (song != null) break
                        attempts++
                    }
                }
                val finalSong = song ?: Song(
                    code = playCode.padStart(6, '0'),
                    title = "Track ${playCode.padStart(6, '0')}",
                    artist = "Videoke",
                    filePath = ""
                )
                viewModel.selectSong(finalSong)
                android.util.Log.d("MainActivity", "Successfully started playing: ${finalSong.title} (${finalSong.code})")
            }
        }

        val reserveCode = intent?.getStringExtra("reserve_code")
        if (!reserveCode.isNullOrBlank()) {
            lifecycleScope.launch {
                var song = viewModel.findSongByCode(reserveCode)
                if (song == null) {
                    var attempts = 0
                    while (attempts < 40) {
                        delay(100)
                        song = viewModel.findSongByCode(reserveCode)
                        if (song != null) break
                        attempts++
                    }
                }
                val finalSong = song ?: Song(
                    code = reserveCode.padStart(6, '0'),
                    title = "Track ${reserveCode.padStart(6, '0')}",
                    artist = "Videoke",
                    filePath = ""
                )
                viewModel.reserveSong(finalSong)
                android.util.Log.d("MainActivity", "Successfully reserved song: ${finalSong.title} (${finalSong.code})")
            }
        }

        val enterCode = intent?.getStringExtra("enter_code")
        if (!enterCode.isNullOrBlank()) {
            viewModel.onClearCode()
            enterCode.forEach { viewModel.onDigitPressed(it) }
        }

        if (intent?.getBooleanExtra("do_reserve", false) == true) {
            viewModel.onReserve()
        }

        val action = intent?.getStringExtra("action")
        if (!action.isNullOrBlank()) {
            when (action) {
                "play" -> viewModel.onPlay()
                "next" -> viewModel.onNext()
                "stop" -> viewModel.onStop()
                "toggle_tv" -> viewModel.toggleTvMode()
                "open_settings" -> viewModel.openSettingsDialog()
                "open_mixer" -> viewModel.openAudioMixerDialog()
                "open_soundfont" -> viewModel.openSoundFontDialog()
                "open_download" -> viewModel.openDownloadCenter()
                "open_lyric_style" -> viewModel.openLyricStyleDialog()
                "open_bg_dialog" -> viewModel.openBackgroundDialog()
                "open_songbook" -> viewModel.openSongbook()
                "search_songbook" -> {
                    viewModel.openSongbook()
                    val q = intent.getStringExtra("query") ?: ""
                    viewModel.searchSongbook(q)
                }
                "delete_digit" -> viewModel.onDeleteDigit()
                "clear_code" -> viewModel.onClearCode()
                "set_lyric_font" -> {
                    val fontName = intent.getStringExtra("font") ?: "SAN_SERIF"
                    runCatching {
                        com.example.videokey.ui.main.LyricFontFamily.valueOf(fontName)
                    }.onSuccess { family ->
                        viewModel.setLyricFontFamily(family)
                    }
                }
                "start_recording" -> viewModel.startRecordingNow()
                "stop_recording" -> viewModel.stopAndSaveRecording()
                "open_recordings" -> viewModel.openRecordingsDialog()
                "play_latest_recording" -> {
                    val latest = viewModel.uiState.value.savedRecordings.firstOrNull()
                    if (latest != null) {
                        viewModel.playSavedRecording(latest.filePath)
                    }
                }
                "close_all_dialogs" -> {
                    viewModel.closeSongbook()
                    viewModel.closeLyricStyleDialog()
                    viewModel.closeAudioMixerDialog()
                    viewModel.closeRecordingsDialog()
                    viewModel.closeSoundFontDialog()
                    viewModel.closeSettingsDialog()
                }
                "set_language_en" -> viewModel.setAppLanguage(com.example.videokey.ui.main.AppLanguage.ENGLISH)
                "set_language_tl" -> viewModel.setAppLanguage(com.example.videokey.ui.main.AppLanguage.TAGALOG)
            }
        }
    }

    private fun requestPermissionsIfNeeded() {
        val permissions = mutableListOf(
            android.Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(android.Manifest.permission.READ_MEDIA_VIDEO)
            permissions.add(android.Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 101)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            }
        }
    }
}
