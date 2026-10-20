package com.example.videokey.remote

import android.util.Log
import com.example.videokey.data.Song
import com.example.videokey.ui.main.*
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * Built-in Wi-Fi Mobile Videoke Remote Server.
 * Runs on port 8080.
 * Any phone, tablet, or browser on the same Wi-Fi network can navigate to:
 * http://<AndroidTV-IP>:8080
 * and instantly use their phone as a full-featured Videoke Remote Control!
 */
class VideokeRemoteServer(
    private val viewModel: VideoKeyViewModel,
    private val port: Int = 8080
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverSocket: ServerSocket? = null
    private var isRunning = false

    @Volatile
    private var lastClientActivityMs: Long = 0L

    fun start() {
        if (isRunning) return
        isRunning = true

        // Background monitor for connected remote client status
        scope.launch {
            while (isRunning) {
                delay(2000)
                val now = System.currentTimeMillis()
                val isConnected = (now - lastClientActivityMs) < 6500L
                viewModel.setRemoteConnected(isConnected)
            }
        }

        scope.launch {
            try {
                serverSocket = ServerSocket(port)
                Log.d("VideokeRemoteServer", "Videoke Remote Server running on port $port (IP: ${getLocalIpAddress()})")
                while (isRunning) {
                    try {
                        val client = serverSocket?.accept() ?: break
                        launch { handleClient(client) }
                    } catch (e: Exception) {
                        if (!isRunning) break
                    }
                }
            } catch (e: Exception) {
                Log.e("VideokeRemoteServer", "Error starting remote server on port $port", e)
            }
        }
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            // ignore
        }
        viewModel.setRemoteConnected(false)
        scope.cancel()
    }

    private fun recordClientActivity() {
        lastClientActivityMs = System.currentTimeMillis()
        viewModel.setRemoteConnected(true)
    }

    private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
        try {
            socket.use { s ->
                val reader = BufferedReader(InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8))
                val output = BufferedOutputStream(s.getOutputStream())

                val requestLine = reader.readLine() ?: return@use
                val tokens = requestLine.split(" ")
                if (tokens.size < 2) return@use

                recordClientActivity()

                val method = tokens[0].uppercase()
                val fullPath = tokens[1]
                val path = fullPath.substringBefore("?")
                val queryString = if (fullPath.contains("?")) fullPath.substringAfter("?") else ""

                // Read headers to check for Content-Length in POST
                var contentLength = 0
                var line = reader.readLine()
                while (!line.isNullOrEmpty()) {
                    if (line.lowercase().startsWith("content-length:")) {
                        contentLength = line.substringAfter(":").trim().toIntOrNull() ?: 0
                    }
                    line = reader.readLine()
                }

                var postBody = ""
                if (contentLength > 0) {
                    val charArray = CharArray(contentLength)
                    var readTotal = 0
                    while (readTotal < contentLength) {
                        val read = reader.read(charArray, readTotal, contentLength - readTotal)
                        if (read == -1) break
                        readTotal += read
                    }
                    postBody = String(charArray, 0, readTotal)
                }

                when {
                    path == "/" || path == "/index.html" -> {
                        val html = getRemoteHtml()
                        sendResponse(output, "text/html; charset=utf-8", html.toByteArray(StandardCharsets.UTF_8))
                    }
                    path == "/api/state" -> {
                        val json = getStateJson()
                        sendResponse(output, "application/json; charset=utf-8", json.toByteArray(StandardCharsets.UTF_8))
                    }
                    path == "/api/action" -> {
                        val params = parseQueryString(if (method == "POST" && postBody.isNotEmpty()) postBody else queryString)
                        handleAction(params)
                        val json = getStateJson()
                        sendResponse(output, "application/json; charset=utf-8", json.toByteArray(StandardCharsets.UTF_8))
                    }
                    path == "/api/songs" -> {
                        val params = parseQueryString(queryString)
                        val query = params["q"] ?: ""
                        val songs = viewModel.searchSongs(query)
                        val jsonArray = JSONArray()
                        songs.take(250).forEach { song ->
                            val sObj = JSONObject().apply {
                                put("code", song.code)
                                put("title", song.title)
                                put("artist", song.artist)
                                put("category", song.category)
                            }
                            jsonArray.put(sObj)
                        }
                        sendResponse(output, "application/json; charset=utf-8", jsonArray.toString().toByteArray(StandardCharsets.UTF_8))
                    }
                    else -> {
                        send404(output)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("VideokeRemoteServer", "Client handler error", e)
        }
    }

    private fun handleAction(params: Map<String, String>) {
        val action = params["action"] ?: params["cmd"] ?: return
        recordClientActivity()
        withContextScopeMain {
            when (action) {
                "play" -> viewModel.onPlay()
                "next" -> viewModel.onNext()
                "stop" -> viewModel.onStop()
                "reserve" -> viewModel.onReserve()
                "clear" -> viewModel.onClearCode()
                "del", "delete" -> viewModel.onDeleteDigit()
                "digit" -> {
                    val digit = params["value"]?.firstOrNull() ?: params["digit"]?.firstOrNull()
                    if (digit != null && digit in '0'..'9') {
                        viewModel.onDigitPressed(digit)
                    }
                }
                "play_code" -> {
                    val code = params["code"]
                    if (!code.isNullOrBlank()) {
                        val song = viewModel.findSongByCode(code) ?: Song(code.padStart(6, '0'), "Track $code", "Videoke", "")
                        viewModel.selectSong(song)
                    }
                }
                "search" -> {
                    val q = params["q"] ?: params["query"] ?: ""
                    viewModel.searchSongbook(q)
                }
                "open_downloader" -> {
                    val q = params["q"] ?: params["query"]
                    viewModel.openYouTubeDownloader(q)
                }
                "set_vip" -> {
                    val active = params["active"]?.toBooleanStrictOrNull() ?: false
                    viewModel.setVipActivated(active)
                }
                "open_admin_key_gen" -> {
                    viewModel.openAdminKeyGenerator()
                }
                "scan_files" -> {
                    viewModel.checkAndAutoDetectFiles(triggerAutoDownloadIfMissing = true)
                }
                "reserve_code" -> {
                    val code = params["code"]
                    if (!code.isNullOrBlank()) {
                        val song = viewModel.findSongByCode(code) ?: Song(code.padStart(6, '0'), "Track $code", "Videoke", "")
                        viewModel.reserveSong(song)
                    }
                }
                "key" -> {
                    val delta = params["delta"]?.toIntOrNull() ?: 0
                    viewModel.adjustKey(delta)
                }
                "tempo" -> {
                    val delta = params["delta"]?.toIntOrNull() ?: 0
                    viewModel.adjustTempo(delta)
                }
                "melody" -> {
                    val delta = params["delta"]?.toIntOrNull() ?: 0
                    viewModel.adjustMelody(delta)
                }
                "drums" -> {
                    val delta = params["delta"]?.toIntOrNull() ?: 0
                    viewModel.adjustDrums(delta)
                }
                "bass" -> {
                    val delta = params["delta"]?.toIntOrNull() ?: 0
                    viewModel.adjustBass(delta)
                }
                "guitar_piano" -> {
                    val delta = params["delta"]?.toIntOrNull() ?: 0
                    viewModel.adjustGuitarPiano(delta)
                }
                "strings_brass" -> {
                    val delta = params["delta"]?.toIntOrNull() ?: 0
                    viewModel.adjustStringsBrass(delta)
                }
                "reverb" -> {
                    val delta = params["delta"]?.toIntOrNull() ?: 0
                    viewModel.adjustReverb(delta)
                }
                "mic_echo" -> {
                    val delta = params["delta"]?.toIntOrNull() ?: 0
                    val curr = viewModel.uiState.value.micEchoPercent
                    viewModel.setMicEcho(curr + delta)
                }
                "reset_mixer" -> {
                    viewModel.resetInstrumentMixer()
                }
                "tv" -> viewModel.toggleTvMode()
                "soundfont" -> {
                    val sf = params["name"] ?: params["font"] ?: ""
                    if (sf.isNotBlank()) viewModel.selectSoundFont(sf)
                }
                "app_theme" -> {
                    val themeName = params["theme"]?.uppercase() ?: ""
                    try {
                        viewModel.setAppTheme(AppTheme.valueOf(themeName))
                    } catch (e: Exception) {}
                }
                "tab" -> {
                    val tabIdx = params["index"]?.toIntOrNull() ?: params["tab"]?.toIntOrNull() ?: 0
                    viewModel.selectBottomNavTab(tabIdx)
                }
                "filter", "category" -> {
                    val cat = params["cat"] ?: params["category"] ?: "ALL"
                    viewModel.selectCategoryFilter(cat)
                }
                "font_family" -> {
                    val fontName = params["font"]?.uppercase() ?: ""
                    try {
                        viewModel.setLyricFontFamily(LyricFontFamily.valueOf(fontName))
                    } catch (e: Exception) {}
                }
                "outline_color" -> {
                    val colorName = params["color"]?.uppercase() ?: "BLACK"
                    try {
                        val oc = LyricOutlineColor.valueOf(colorName)
                        viewModel.setLyricOutlineColor(oc)
                    } catch (e: Exception) {}
                }
                "wipe_color" -> {
                    val colorName = params["color"]?.uppercase() ?: ""
                    try {
                        val wc = LyricWipeColor.valueOf(colorName)
                        viewModel.setLyricWipeColor(wc)
                    } catch (e: Exception) {}
                }
                "font_scale" -> {
                    val delta = params["delta"]?.toFloatOrNull() ?: 0f
                    val curr = viewModel.uiState.value.lyricConfig.fontScale
                    viewModel.setLyricFontScale(curr + delta)
                }
                "line_count" -> {
                    val count = params["count"]?.toIntOrNull() ?: 2
                    if (count == 4) {
                        viewModel.setLyricLineCount(LyricLineCount.FOUR_LINES)
                    } else {
                        viewModel.setLyricLineCount(LyricLineCount.TWO_LINES)
                    }
                }
                "open_lyric_dialog" -> viewModel.openLyricStyleDialog()
                "close_lyric_dialog" -> viewModel.closeLyricStyleDialog()
                "toggle_mic_scoring" -> {
                    val enabled = params["enabled"]?.toBooleanStrictOrNull() ?: !viewModel.uiState.value.isMicScoringEnabled
                    viewModel.toggleMicScoring(enabled)
                }
                "toggle_scoring_sound" -> {
                    val enabled = params["enabled"]?.toBooleanStrictOrNull() ?: !viewModel.uiState.value.isScoringSoundEnabled
                    viewModel.toggleScoringSound(enabled)
                }
                "toggle_keypad_voice" -> {
                    val enabled = params["enabled"]?.toBooleanStrictOrNull() ?: !viewModel.uiState.value.isKeypadVoiceEnabled
                    viewModel.toggleKeypadVoice(enabled)
                }
                "next_bg" -> {
                    viewModel.advanceNextBackground()
                }
            }
        }
    }

    private fun withContextScopeMain(block: () -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            block()
        }
    }

    private fun getStateJson(): String {
        val state = viewModel.uiState.value
        val json = JSONObject().apply {
            put("isPlaying", state.isPlaying)
            put("enteredCode", state.enteredCode)
            put("keyTranspose", state.keyTranspose)
            put("tempoPercent", state.tempoPercent)
            put("melodyPercent", state.melodyPercent)
            put("drumsPercent", state.drumsPercent)
            put("bassPercent", state.bassPercent)
            put("guitarPianoPercent", state.guitarPianoPercent)
            put("stringsBrassPercent", state.stringsBrassPercent)
            put("reverbPercent", state.reverbPercent)
            put("micEchoPercent", state.micEchoPercent)
            put("isMicScoringEnabled", state.isMicScoringEnabled)
            put("isScoringSoundEnabled", state.isScoringSoundEnabled)
            put("isKeypadVoiceEnabled", state.isKeypadVoiceEnabled)
            put("isTvMode", state.isTvMode)
            put("activeSoundFont", state.activeSoundFontName)

            val sfArray = JSONArray()
            state.availableSoundFonts.forEach { sfArray.put(it) }
            put("availableSoundFonts", sfArray)

            put("appTheme", state.appTheme.name)
            put("lyricFontFamily", state.lyricConfig.fontFamily.name)
            put("lyricOutlineColor", state.lyricConfig.outlineColor.name)
            put("lyricWipeColor", state.lyricConfig.wipeColor.name)
            put("lyricFontScale", state.lyricConfig.fontScale)
            put("lyricLineCount", if (state.lyricConfig.lineCount == LyricLineCount.FOUR_LINES) 4 else 2)

            val currentSongObj = JSONObject().apply {
                put("code", state.currentSong.code)
                put("title", state.currentSong.title)
                put("artist", state.currentSong.artist)
            }
            put("currentSong", currentSongObj)

            val rQueueArray = JSONArray()
            state.reserveQueue.forEach { song ->
                val sObj = JSONObject().apply {
                    put("code", song.code)
                    put("title", song.title)
                    put("artist", song.artist)
                }
                rQueueArray.put(sObj)
            }
            put("reserveQueue", rQueueArray)
        }
        return json.toString()
    }

    private fun sendResponse(output: OutputStream, contentType: String, body: ByteArray) {
        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: $contentType\r\n" +
                "Content-Length: ${body.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
        output.write(header.toByteArray(StandardCharsets.UTF_8))
        output.write(body)
        output.flush()
    }

    private fun send404(output: OutputStream) {
        val body = "Not Found".toByteArray(StandardCharsets.UTF_8)
        val header = "HTTP/1.1 404 Not Found\r\nContent-Type: text/plain\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n"
        output.write(header.toByteArray(StandardCharsets.UTF_8))
        output.write(body)
        output.flush()
    }

    private fun parseQueryString(query: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        if (query.isBlank()) return result
        query.split("&").forEach { pair ->
            val parts = pair.split("=")
            if (parts.isNotEmpty()) {
                val key = URLDecoder.decode(parts[0], "UTF-8")
                val value = if (parts.size > 1) URLDecoder.decode(parts[1], "UTF-8") else ""
                result[key] = value
            }
        }
        return result
    }

    companion object {
        fun getLocalIpAddress(): String {
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces()
                while (interfaces.hasMoreElements()) {
                    val intf = interfaces.nextElement()
                    val addrs = intf.inetAddresses
                    while (addrs.hasMoreElements()) {
                        val addr = addrs.nextElement()
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            val ip = addr.hostAddress ?: ""
                            if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                                return ip
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // ignore
            }
            return "127.0.0.1"
        }
    }

    private fun getRemoteHtml(): String {
        return """
<!DOCTYPE html>
<html lang="tl">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <title>VideoKey TV Remote & Settings</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; -webkit-tap-highlight-color: transparent; }
    body {
      background: #090D16;
      color: #FFF;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial, sans-serif;
      padding: 8px 10px 24px 10px;
      display: flex;
      flex-direction: column;
      align-items: center;
      min-height: 100vh;
    }
    .remote-container {
      width: 100%;
      max-width: 480px;
      display: flex;
      flex-direction: column;
      gap: 10px;
    }
    /* Tab Navigation */
    .tab-bar {
      display: flex;
      background: #0F172A;
      border: 1.5px solid #334155;
      border-radius: 12px;
      padding: 4px;
      gap: 4px;
      width: 100%;
    }
    .tab-btn {
      flex: 1;
      padding: 10px 4px;
      border: none;
      border-radius: 8px;
      background: transparent;
      color: #94A3B8;
      font-weight: 800;
      font-size: 13px;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
      transition: all 0.15s ease;
    }
    .tab-btn.active {
      background: #38BDF8;
      color: #000;
      box-shadow: 0 4px 12px rgba(56, 189, 248, 0.35);
    }
    .tab-content { display: none; width: 100%; flex-direction: column; gap: 10px; }
    .tab-content.active { display: flex; }

    /* Top Card / Now Playing */
    .top-card {
      background: linear-gradient(135deg, #1E293B, #0F172A);
      border: 1.5px solid #38BDF8;
      border-radius: 14px;
      padding: 12px;
      box-shadow: 0 8px 24px rgba(0,0,0,0.5);
    }
    .status-badge {
      display: inline-block;
      padding: 3px 8px;
      border-radius: 6px;
      font-size: 11px;
      font-weight: 900;
      letter-spacing: 0.5px;
      text-transform: uppercase;
      background: #00FF88;
      color: #000;
      margin-bottom: 6px;
    }
    .song-title {
      font-size: 16px;
      font-weight: 900;
      color: #FFF;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .song-artist {
      font-size: 13px;
      font-weight: 700;
      color: #FFD54F;
      margin-top: 2px;
    }
    .next-box {
      margin-top: 8px;
      padding-top: 6px;
      border-top: 1px dashed #334155;
      font-size: 12px;
      color: #94A3B8;
      display: flex;
      align-items: center;
      gap: 6px;
    }
    .next-badge {
      color: #FF5252;
      font-weight: 900;
      font-size: 11px;
    }
    /* Song Code Display */
    .code-display {
      background: #020617;
      border: 2px solid #00FF88;
      border-radius: 12px;
      padding: 10px;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .code-label {
      font-size: 11px;
      font-weight: 800;
      color: #64748B;
      letter-spacing: 1px;
    }
    .code-digits {
      font-size: 26px;
      font-weight: 900;
      letter-spacing: 4px;
      color: #00FF88;
      font-family: monospace;
    }
    /* Keypad Grid */
    .keypad-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 8px;
    }
    button.key-btn {
      background: #1E293B;
      color: #FFF;
      border: 1px solid #334155;
      border-radius: 10px;
      height: 52px;
      font-size: 20px;
      font-weight: 900;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.1s ease;
      box-shadow: 0 4px 10px rgba(0,0,0,0.3);
    }
    button.key-btn:active {
      transform: scale(0.95);
      background: #38BDF8;
      color: #000;
    }
    button.action-clr {
      background: #7F1D1D;
      border-color: #EF4444;
      color: #FECACA;
      font-size: 15px;
    }
    button.action-del {
      background: #78350F;
      border-color: #F59E0B;
      color: #FEF3C7;
      font-size: 15px;
    }
    /* Control Row Buttons */
    .control-row {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 8px;
    }
    button.ctrl-btn {
      height: 48px;
      border-radius: 10px;
      font-size: 13px;
      font-weight: 900;
      border: none;
      cursor: pointer;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 2px;
      color: #FFF;
      box-shadow: 0 4px 12px rgba(0,0,0,0.4);
    }
    button.ctrl-btn:active { transform: scale(0.94); }
    .btn-reserve { background: linear-gradient(135deg, #F59E0B, #D97706); }
    .btn-play { background: linear-gradient(135deg, #10B981, #059669); }
    .btn-next { background: linear-gradient(135deg, #0284C7, #0369A1); }
    .btn-stop { background: linear-gradient(135deg, #EF4444, #B91C1C); }
    
    /* Pitch & Tempo Sliders/Pills */
    .tuning-row {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 8px;
    }
    .tuning-box {
      background: #0F172A;
      border: 1px solid #1E293B;
      border-radius: 10px;
      padding: 6px;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 4px;
    }
    .tuning-label { font-size: 10px; font-weight: 800; color: #94A3B8; text-transform: uppercase; }
    .tuning-value { font-size: 13px; font-weight: 900; color: #38BDF8; font-family: monospace; }
    .tuning-btns { display: flex; gap: 6px; width: 100%; justify-content: center; }
    .tuning-btn {
      width: 32px;
      height: 28px;
      background: #1E293B;
      color: #FFF;
      border: 1px solid #475569;
      border-radius: 6px;
      font-weight: 900;
      font-size: 14px;
      cursor: pointer;
    }

    /* FULLSCREEN SONGBOOK TAB */
    .songbook-container {
      display: flex;
      flex-direction: column;
      gap: 8px;
      height: calc(100vh - 80px);
      width: 100%;
    }
    .search-box-sticky {
      position: sticky;
      top: 0;
      z-index: 10;
      background: #090D16;
      padding-bottom: 4px;
    }
    .search-input {
      width: 100%;
      height: 44px;
      background: #0F172A;
      border: 2px solid #6366F1;
      border-radius: 10px;
      padding: 0 14px;
      color: #FFF;
      font-size: 15px;
      outline: none;
      box-shadow: 0 4px 14px rgba(99, 102, 241, 0.25);
    }
    .search-input:focus { border-color: #A5B4FC; }
    .song-full-list {
      flex: 1;
      overflow-y: auto;
      display: flex;
      flex-direction: column;
      gap: 8px;
      padding-right: 2px;
      -webkit-overflow-scrolling: touch;
    }
    .song-card {
      background: #131D31;
      border: 1px solid #1E293B;
      border-radius: 10px;
      padding: 10px 12px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 10px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.3);
    }
    .song-meta { flex: 1; min-width: 0; }
    .song-code-pill {
      font-family: monospace;
      font-size: 11px;
      font-weight: 900;
      color: #00FF88;
      background: #021F11;
      border: 1px solid #00FF88;
      border-radius: 4px;
      padding: 1px 5px;
      display: inline-block;
      margin-bottom: 3px;
    }
    .song-name {
      font-size: 14px;
      font-weight: 800;
      color: #FFF;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .song-singer {
      font-size: 12px;
      font-weight: 600;
      color: #94A3B8;
      margin-top: 1px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .song-actions { display: flex; gap: 6px; }
    .btn-song-act {
      padding: 8px 12px;
      border-radius: 6px;
      font-size: 11px;
      font-weight: 900;
      border: none;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .btn-song-res { background: #F59E0B; color: #000; }
    .btn-song-play { background: #10B981; color: #000; }

    /* SETTINGS TAB */
    .settings-container {
      display: flex;
      flex-direction: column;
      gap: 14px;
      width: 100%;
      padding-bottom: 40px;
    }
    .settings-section {
      background: #0F172A;
      border: 1px solid #1E293B;
      border-radius: 12px;
      padding: 12px;
      display: flex;
      flex-direction: column;
      gap: 10px;
    }
    .section-title {
      font-size: 13px;
      font-weight: 900;
      color: #38BDF8;
      display: flex;
      align-items: center;
      gap: 6px;
      border-bottom: 1px solid #1E293B;
      padding-bottom: 6px;
    }
    .setting-item-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 4px 0;
    }
    .setting-label { font-size: 13px; font-weight: 700; color: #E2E8F0; }
    .setting-sub { font-size: 11px; color: #64748B; margin-top: 2px; }
    .stepper-box {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .stepper-val {
      font-family: monospace;
      font-size: 13px;
      font-weight: 900;
      color: #00FF88;
      min-width: 44px;
      text-align: center;
    }
    .btn-step {
      width: 32px;
      height: 32px;
      background: #1E293B;
      border: 1px solid #334155;
      color: #FFF;
      border-radius: 6px;
      font-weight: 900;
      font-size: 16px;
      cursor: pointer;
    }
    .btn-step:active { background: #38BDF8; color: #000; }
    .toggle-switch {
      position: relative;
      display: inline-block;
      width: 46px;
      height: 26px;
    }
    .toggle-switch input { opacity: 0; width: 0; height: 0; }
    .slider-track {
      position: absolute; cursor: pointer; top: 0; left: 0; right: 0; bottom: 0;
      background-color: #334155; border-radius: 26px; transition: .2s;
    }
    .slider-track:before {
      position: absolute; content: ""; height: 20px; width: 20px; left: 3px; bottom: 3px;
      background-color: white; border-radius: 50%; transition: .2s;
    }
    input:checked + .slider-track { background-color: #10B981; }
    input:checked + .slider-track:before { transform: translateX(20px); }

    .pills-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 6px;
    }
    .pill-btn {
      padding: 8px 10px;
      background: #1E293B;
      border: 1px solid #334155;
      border-radius: 8px;
      color: #CBD5E1;
      font-size: 11.5px;
      font-weight: 800;
      cursor: pointer;
      text-align: center;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .pill-btn.active {
      background: #0284C7;
      border-color: #38BDF8;
      color: #FFF;
      box-shadow: 0 2px 8px rgba(56, 189, 248, 0.4);
    }
  </style>
</head>
<body>
  <div class="remote-container">
    <!-- Top Navigation Tabs -->
    <div class="tab-bar">
      <button id="tabBtnRemote" class="tab-btn active" onclick="switchTab('remote')">🎮 REMOTE</button>
      <button id="tabBtnSongbook" class="tab-btn" onclick="switchTab('songbook')">📚 SONGBOOK</button>
      <button id="tabBtnSettings" class="tab-btn" onclick="switchTab('settings')">⚙️ SETTINGS</button>
    </div>

    <!-- ==================== TAB 1: REMOTE KEYPAD ==================== -->
    <div id="tabContentRemote" class="tab-content active">
      <!-- Header / Now Playing -->
      <div class="top-card">
        <span id="statusBadge" class="status-badge">VIDEOKEY TV</span>
        <div id="playingTitle" class="song-title">READY TO SING...</div>
        <div id="playingArtist" class="song-artist">PHILIPPINE VIDEOKEY</div>
        <div class="next-box">
          <span class="next-badge">NEXT:</span>
          <span id="nextSongText">WALA (ENTER CODE + RESERVE)</span>
        </div>
      </div>

      <!-- Code Display -->
      <div class="code-display">
        <span class="code-label">SONG CODE:</span>
        <span id="codeDigits" class="code-digits">000000</span>
      </div>

      <!-- Keypad 0-9 -->
      <div class="keypad-grid">
        <button class="key-btn" onclick="sendDigit('1')">1</button>
        <button class="key-btn" onclick="sendDigit('2')">2</button>
        <button class="key-btn" onclick="sendDigit('3')">3</button>
        <button class="key-btn" onclick="sendDigit('4')">4</button>
        <button class="key-btn" onclick="sendDigit('5')">5</button>
        <button class="key-btn" onclick="sendDigit('6')">6</button>
        <button class="key-btn" onclick="sendDigit('7')">7</button>
        <button class="key-btn" onclick="sendDigit('8')">8</button>
        <button class="key-btn" onclick="sendDigit('9')">9</button>
        <button class="key-btn action-clr" onclick="sendAction('clear')">CLR</button>
        <button class="key-btn" onclick="sendDigit('0')">0</button>
        <button class="key-btn action-del" onclick="sendAction('del')">DEL</button>
      </div>

      <!-- Videoke Main Action Row -->
      <div class="control-row">
        <button class="ctrl-btn btn-reserve" onclick="sendAction('reserve')">
          <span>★</span>
          <span>RESERVE</span>
        </button>
        <button class="ctrl-btn btn-play" onclick="sendAction('play')">
          <span>▶</span>
          <span>PLAY</span>
        </button>
        <button class="ctrl-btn btn-next" onclick="sendAction('next')">
          <span>⏭</span>
          <span>NEXT</span>
        </button>
        <button class="ctrl-btn btn-stop" onclick="sendAction('stop')">
          <span>⏹</span>
          <span>STOP</span>
        </button>
      </div>

      <!-- Key, Tempo & Melody Tuning -->
      <div class="tuning-row">
        <div class="tuning-box">
          <span class="tuning-label">KEY (PITCH)</span>
          <span id="keyVal" class="tuning-value">0</span>
          <div class="tuning-btns">
            <button class="tuning-btn" onclick="adjustTuning('key', -1)">♭</button>
            <button class="tuning-btn" onclick="adjustTuning('key', 1)">♯</button>
          </div>
        </div>
        <div class="tuning-box">
          <span class="tuning-label">TEMPO</span>
          <span id="tempoVal" class="tuning-value">100%</span>
          <div class="tuning-btns">
            <button class="tuning-btn" onclick="adjustTuning('tempo', -5)">-</button>
            <button class="tuning-btn" onclick="adjustTuning('tempo', 5)">+</button>
          </div>
        </div>
        <div class="tuning-box">
          <span class="tuning-label">MELODY</span>
          <span id="melodyVal" class="tuning-value">100%</span>
          <div class="tuning-btns">
            <button class="tuning-btn" onclick="adjustTuning('melody', -10)">-</button>
            <button class="tuning-btn" onclick="adjustTuning('melody', 10)">+</button>
          </div>
        </div>
      </div>
    </div>

    <!-- ==================== TAB 2: FULLSCREEN SONGBOOK ==================== -->
    <div id="tabContentSongbook" class="tab-content">
      <div class="songbook-container">
        <div class="search-box-sticky">
          <input type="text" id="searchInput" class="search-input" placeholder="🔍 I-type ang Pamagat o Mang-aawit..." oninput="onSearchInput()">
          <div style="font-size: 11px; color: #64748B; margin-top: 4px; padding-left: 4px;">
            Pumili o maghanap ng kanta sa buong listahan pababa:
          </div>
        </div>
        <div id="searchResults" class="song-full-list">
          <div style="font-size: 13px; color: #64748B; text-align: center; padding: 24px;">Naglo-load ng listahan ng mga kanta...</div>
        </div>
      </div>
    </div>

    <!-- ==================== TAB 3: FULL APP SETTINGS ==================== -->
    <div id="tabContentSettings" class="tab-content">
      <div class="settings-container">
        
        <!-- SECTION 1: INSTRUMENT & AUDIO MIXER -->
        <div class="settings-section">
          <div class="section-title">🎛️ INSTRUMENT & AUDIO MIXER</div>
          
          <div class="setting-item-row">
            <div>
              <div class="setting-label">🥁 Drums (Tambol)</div>
              <div class="setting-sub">Lakas ng tunog ng drums</div>
            </div>
            <div class="stepper-box">
              <button class="btn-step" onclick="adjustTuning('drums', -10)">-</button>
              <span id="drumsVal" class="stepper-val">100%</span>
              <button class="btn-step" onclick="adjustTuning('drums', 10)">+</button>
            </div>
          </div>

          <div class="setting-item-row">
            <div>
              <div class="setting-label">🎸 Bass (Gitara sa Ibaba)</div>
              <div class="setting-sub">Lalim at bigat ng bass line</div>
            </div>
            <div class="stepper-box">
              <button class="btn-step" onclick="adjustTuning('bass', -10)">-</button>
              <span id="bassVal" class="stepper-val">100%</span>
              <button class="btn-step" onclick="adjustTuning('bass', 10)">+</button>
            </div>
          </div>

          <div class="setting-item-row">
            <div>
              <div class="setting-label">🎹 Guitar & Piano (Plucking/Keys)</div>
              <div class="setting-sub">Gitara at tiklado ng kanta</div>
            </div>
            <div class="stepper-box">
              <button class="btn-step" onclick="adjustTuning('guitar_piano', -10)">-</button>
              <span id="gpVal" class="stepper-val">100%</span>
              <button class="btn-step" onclick="adjustTuning('guitar_piano', 10)">+</button>
            </div>
          </div>

          <div class="setting-item-row">
            <div>
              <div class="setting-label">🎺 Strings & Brass (Orkestra/Hipan)</div>
              <div class="setting-sub">Violin, trumpeta, at pads</div>
            </div>
            <div class="stepper-box">
              <button class="btn-step" onclick="adjustTuning('strings_brass', -10)">-</button>
              <span id="sbVal" class="stepper-val">100%</span>
              <button class="btn-step" onclick="adjustTuning('strings_brass', 10)">+</button>
            </div>
          </div>

          <div class="setting-item-row">
            <div>
              <div class="setting-label">🌊 Reverb Echo (Paligid/Hall)</div>
              <div class="setting-sub">Lalim ng studio acoustic</div>
            </div>
            <div class="stepper-box">
              <button class="btn-step" onclick="adjustTuning('reverb', -10)">-</button>
              <span id="reverbVal" class="stepper-val">50%</span>
              <button class="btn-step" onclick="adjustTuning('reverb', 10)">+</button>
            </div>
          </div>

          <div class="setting-item-row">
            <div>
              <div class="setting-label">🎤 Microphone Echo</div>
              <div class="setting-sub">Echo ng boses ng kumakanta</div>
            </div>
            <div class="stepper-box">
              <button class="btn-step" onclick="adjustTuning('mic_echo', -10)">-</button>
              <span id="micEchoVal" class="stepper-val">50%</span>
              <button class="btn-step" onclick="adjustTuning('mic_echo', 10)">+</button>
            </div>
          </div>

          <button style="margin-top: 6px; padding: 10px; background: #334155; border: none; border-radius: 8px; color: #FFF; font-weight: 800; font-size: 12px; cursor: pointer;" onclick="sendAction('reset_mixer')">
            🔄 I-RESET ANG LAHAT NG INSTRUMENTS SA DEFAULT
          </button>
        </div>

        <!-- SECTION 2: SOUNDFONT ENGINE -->
        <div class="settings-section">
          <div class="section-title">🎼 SOUNDFONT MIDI ENGINE</div>
          <div style="font-size: 11px; color: #94A3B8;">Piliin ang Midi sound pack para sa kalidad ng tugtog:</div>
          <div class="pills-grid" id="soundFontList">
            <!-- Dynamically populated -->
          </div>
        </div>

        <!-- SECTION 3: LYRIC CUSTOMIZATION -->
        <div class="settings-section">
          <div class="section-title">📝 FONT & LYRIC STYLING</div>
          
          <div>
            <div class="setting-label" style="margin-bottom: 6px;">Estilo ng Font (Font Family):</div>
            <div class="pills-grid">
              <button class="pill-btn" onclick="sendLyricFont('ROUNDED_CLEAN')">Verdana Wide (TV)</button>
              <button class="pill-btn" onclick="sendLyricFont('SAN_SERIF')">Arial Bold</button>
              <button class="pill-btn" onclick="sendLyricFont('IMPACT_HEAVY')">Impact Heavy</button>
              <button class="pill-btn" onclick="sendLyricFont('CONDENSED_TALL')">Bebas Tall</button>
              <button class="pill-btn" onclick="sendLyricFont('GEOMETRIC_POP')">Montserrat Pop</button>
              <button class="pill-btn" onclick="sendLyricFont('CASUAL_FUN')">Comic Casual</button>
            </div>
          </div>

          <div style="margin-top: 6px;">
            <div class="setting-label" style="margin-bottom: 6px;">Kulay ng Outline (Border):</div>
            <div class="pills-grid">
              <button class="pill-btn" onclick="sendLyricOutline('BLACK')">Itim (Black)</button>
              <button class="pill-btn" onclick="sendLyricOutline('WHITE')">Puti (White)</button>
              <button class="pill-btn" onclick="sendLyricOutline('GOLD')">Ginto (Gold)</button>
              <button class="pill-btn" onclick="sendLyricOutline('NAVY')">Navy Blue</button>
              <button class="pill-btn" onclick="sendLyricOutline('CYAN')">Cyan Neon</button>
              <button class="pill-btn" onclick="sendLyricOutline('NONE')">Walang Outline</button>
            </div>
          </div>

          <div style="margin-top: 6px;">
            <div class="setting-label" style="margin-bottom: 6px;">Kulay ng Wipe (Singing Color):</div>
            <div class="pills-grid">
              <button class="pill-btn" onclick="sendLyricWipe('DARK_BLUE')">Dark Blue</button>
              <button class="pill-btn" onclick="sendLyricWipe('OCEAN_BLUE')">Karaoke Blue</button>
              <button class="pill-btn" onclick="sendLyricWipe('SUNSHINE_YELLOW')">Sunshine Yellow</button>
              <button class="pill-btn" onclick="sendLyricWipe('ELECTRIC_CYAN')">Electric Cyan</button>
              <button class="pill-btn" onclick="sendLyricWipe('NEON_GREEN')">Lime Green</button>
              <button class="pill-btn" onclick="sendLyricWipe('HOT_PINK')">Hot Pink</button>
            </div>
          </div>

          <div class="setting-item-row" style="margin-top: 6px;">
            <div>
              <div class="setting-label">Sukat ng Lyrics (Font Scale)</div>
              <div class="setting-sub">Laki o liit ng titik sa screen</div>
            </div>
            <div class="stepper-box">
              <button class="btn-step" onclick="sendFontScale(-0.05)">-</button>
              <span id="fontScaleVal" class="stepper-val">100%</span>
              <button class="btn-step" onclick="sendFontScale(0.05)">+</button>
            </div>
          </div>
        </div>

        <!-- SECTION 4: SCORING & SYSTEM -->
        <div class="settings-section">
          <div class="section-title">🎤 VIDEOKE SCORING & SYSTEM</div>

          <div class="setting-item-row">
            <div>
              <div class="setting-label">Microphone Scoring</div>
              <div class="setting-sub">Grado at puntos sa boses ng kumakanta</div>
            </div>
            <label class="toggle-switch">
              <input type="checkbox" id="toggleMicScoring" onchange="sendToggle('toggle_mic_scoring', this.checked)">
              <span class="slider-track"></span>
            </label>
          </div>

          <div class="setting-item-row">
            <div>
              <div class="setting-label">Cheer & Applause Sound</div>
              <div class="setting-sub">Palakpak at sigawan pagkatapos kumanta</div>
            </div>
            <label class="toggle-switch">
              <input type="checkbox" id="toggleScoringSound" onchange="sendToggle('toggle_scoring_sound', this.checked)">
              <span class="slider-track"></span>
            </label>
          </div>

          <div class="setting-item-row">
            <div>
              <div class="setting-label">Keypad Voice Numbers</div>
              <div class="setting-sub">Boses na nagsasalita sa pagpindot ng 0-9</div>
            </div>
            <label class="toggle-switch">
              <input type="checkbox" id="toggleKeypadVoice" onchange="sendToggle('toggle_keypad_voice', this.checked)">
              <span class="slider-track"></span>
            </label>
          </div>

          <div class="setting-item-row">
            <div>
              <div class="setting-label">📺 TV Mode Display</div>
              <div class="setting-sub">Fullscreen TV mode layout</div>
            </div>
            <label class="toggle-switch">
              <input type="checkbox" id="toggleTvMode" onchange="sendAction('tv')">
              <span class="slider-track"></span>
            </label>
          </div>

          <button style="margin-top: 8px; padding: 10px; background: #0284C7; border: none; border-radius: 8px; color: #FFF; font-weight: 800; font-size: 12px; cursor: pointer;" onclick="sendAction('next_bg')">
            ⏭ LIPAT SA SUSUNOD NA BACKGROUND VIDEO / STAGE
          </button>
        </div>

      </div>
    </div>
  </div>

  <script>
    let activeTab = 'remote';
    function switchTab(tab) {
      activeTab = tab;
      document.getElementById('tabBtnRemote').classList.toggle('active', tab === 'remote');
      document.getElementById('tabBtnSongbook').classList.toggle('active', tab === 'songbook');
      document.getElementById('tabBtnSettings').classList.toggle('active', tab === 'settings');

      document.getElementById('tabContentRemote').classList.toggle('active', tab === 'remote');
      document.getElementById('tabContentSongbook').classList.toggle('active', tab === 'songbook');
      document.getElementById('tabContentSettings').classList.toggle('active', tab === 'settings');

      if (tab === 'songbook') {
        loadSongbookList('');
      }
    }

    async function sendAction(act) {
      try {
        const res = await fetch('/api/action', {
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body: 'action=' + encodeURIComponent(act)
        });
        const state = await res.json();
        updateUI(state);
      } catch (e) {
        console.error(e);
      }
    }

    async function sendDigit(d) {
      try {
        const res = await fetch('/api/action', {
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body: 'action=digit&value=' + encodeURIComponent(d)
        });
        const state = await res.json();
        updateUI(state);
      } catch (e) {
        console.error(e);
      }
    }

    async function adjustTuning(type, delta) {
      try {
        const res = await fetch('/api/action', {
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body: 'action=' + type + '&delta=' + delta
        });
        const state = await res.json();
        updateUI(state);
      } catch (e) {
        console.error(e);
      }
    }

    async function sendSoundFont(name) {
      try {
        const res = await fetch('/api/action', {
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body: 'action=soundfont&name=' + encodeURIComponent(name)
        });
        const state = await res.json();
        updateUI(state);
      } catch (e) {}
    }

    async function sendLyricFont(font) {
      try {
        const res = await fetch('/api/action', {
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body: 'action=font_family&font=' + encodeURIComponent(font)
        });
        const state = await res.json();
        updateUI(state);
      } catch (e) {}
    }

    async function sendLyricOutline(color) {
      try {
        const res = await fetch('/api/action', {
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body: 'action=outline_color&color=' + encodeURIComponent(color)
        });
        const state = await res.json();
        updateUI(state);
      } catch (e) {}
    }

    async function sendLyricWipe(color) {
      try {
        const res = await fetch('/api/action', {
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body: 'action=wipe_color&color=' + encodeURIComponent(color)
        });
        const state = await res.json();
        updateUI(state);
      } catch (e) {}
    }

    async function sendFontScale(delta) {
      try {
        const res = await fetch('/api/action', {
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body: 'action=font_scale&delta=' + delta
        });
        const state = await res.json();
        updateUI(state);
      } catch (e) {}
    }

    async function sendToggle(actionName, enabled) {
      try {
        const res = await fetch('/api/action', {
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body: 'action=' + encodeURIComponent(actionName) + '&enabled=' + enabled
        });
        const state = await res.json();
        updateUI(state);
      } catch (e) {}
    }

    async function playSongCode(code) {
      try {
        const res = await fetch('/api/action', {
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body: 'action=play_code&code=' + encodeURIComponent(code)
        });
        const state = await res.json();
        updateUI(state);
        switchTab('remote');
      } catch (e) {
        console.error(e);
      }
    }

    async function reserveSongCode(code) {
      try {
        const res = await fetch('/api/action', {
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body: 'action=reserve_code&code=' + encodeURIComponent(code)
        });
        const state = await res.json();
        updateUI(state);
      } catch (e) {
        console.error(e);
      }
    }

    let searchTimer = null;
    function onSearchInput() {
      clearTimeout(searchTimer);
      searchTimer = setTimeout(() => {
        const q = document.getElementById('searchInput').value.trim();
        loadSongbookList(q);
      }, 250);
    }

    async function loadSongbookList(q) {
      try {
        const res = await fetch('/api/songs?q=' + encodeURIComponent(q));
        const list = await res.json();
        renderSearchResults(list);
      } catch (e) {
        console.error(e);
      }
    }

    function renderSearchResults(songs) {
      const container = document.getElementById('searchResults');
      if (!songs || songs.length === 0) {
        container.innerHTML = '<div style="font-size: 14px; color: #EF4444; text-align: center; padding: 28px;">Walang nakitang kanta sa iyong hinahanap.</div>';
        return;
      }
      container.innerHTML = songs.map(s => `
        <div class="song-card">
          <div class="song-meta">
            <span class="song-code-pill">[${'$'}{s.code}]</span>
            <div class="song-name">${'$'}{s.title}</div>
            <div class="song-singer">${'$'}{s.artist}</div>
          </div>
          <div class="song-actions">
            <button class="btn-song-act btn-song-res" onclick="reserveSongCode('${'$'}{s.code}')">★ RESERVE</button>
            <button class="btn-song-act btn-song-play" onclick="playSongCode('${'$'}{s.code}')">▶ PLAY</button>
          </div>
        </div>
      `).join('');
    }

    function updateUI(state) {
      if (!state) return;
      document.getElementById('codeDigits').innerText = (state.enteredCode || '').padStart(6, '0');
      document.getElementById('keyVal').innerText = (state.keyTranspose > 0 ? '+' : '') + (state.keyTranspose || 0);
      document.getElementById('tempoVal').innerText = (state.tempoPercent || 100) + '%';
      document.getElementById('melodyVal').innerText = (state.melodyPercent || 100) + '%';

      // Settings Steppers
      if (document.getElementById('drumsVal')) document.getElementById('drumsVal').innerText = (state.drumsPercent || 100) + '%';
      if (document.getElementById('bassVal')) document.getElementById('bassVal').innerText = (state.bassPercent || 100) + '%';
      if (document.getElementById('gpVal')) document.getElementById('gpVal').innerText = (state.guitarPianoPercent || 100) + '%';
      if (document.getElementById('sbVal')) document.getElementById('sbVal').innerText = (state.stringsBrassPercent || 100) + '%';
      if (document.getElementById('reverbVal')) document.getElementById('reverbVal').innerText = (state.reverbPercent || 50) + '%';
      if (document.getElementById('micEchoVal')) document.getElementById('micEchoVal').innerText = (state.micEchoPercent || 50) + '%';
      if (document.getElementById('fontScaleVal')) document.getElementById('fontScaleVal').innerText = Math.round((state.lyricFontScale || 1.0) * 100) + '%';

      // Settings Switches
      if (document.getElementById('toggleMicScoring')) document.getElementById('toggleMicScoring').checked = !!state.isMicScoringEnabled;
      if (document.getElementById('toggleScoringSound')) document.getElementById('toggleScoringSound').checked = !!state.isScoringSoundEnabled;
      if (document.getElementById('toggleKeypadVoice')) document.getElementById('toggleKeypadVoice').checked = !!state.isKeypadVoiceEnabled;
      if (document.getElementById('toggleTvMode')) document.getElementById('toggleTvMode').checked = !!state.isTvMode;

      // SoundFont list rendering
      const sfContainer = document.getElementById('soundFontList');
      if (sfContainer && state.availableSoundFonts) {
        sfContainer.innerHTML = state.availableSoundFonts.map(sf => `
          <button class="pill-btn ${'$'}{sf === state.activeSoundFont ? 'active' : ''}" onclick="sendSoundFont('${'$'}{sf}')">
            ${'$'}{sf === state.activeSoundFont ? '✓ ' : ''}${'$'}{sf}
          </button>
        `).join('');
      }

      if (state.isPlaying && state.currentSong) {
        document.getElementById('statusBadge').innerText = '▶ NOW PLAYING';
        document.getElementById('statusBadge').style.background = '#00FF88';
        document.getElementById('playingTitle').innerText = state.currentSong.title;
        document.getElementById('playingArtist').innerText = state.currentSong.artist;
      } else {
        document.getElementById('statusBadge').innerText = '⏹ STANDBY / READY';
        document.getElementById('statusBadge').style.background = '#38BDF8';
        document.getElementById('playingTitle').innerText = 'PUMILI NG KANTA...';
        document.getElementById('playingArtist').innerText = 'PHILIPPINE VIDEOKEY';
      }

      if (state.reserveQueue && state.reserveQueue.length > 0) {
        const next = state.reserveQueue[0];
        document.getElementById('nextSongText').innerText = `[${'$'}{next.code}] ${'$'}{next.title} • ${'$'}{next.artist} (${'$'}{state.reserveQueue.length} nakareserba)`;
      } else {
        document.getElementById('nextSongText').innerText = 'WALA (ENTER CODE + RESERVE)';
      }
    }

    async function pollState() {
      try {
        const res = await fetch('/api/state');
        const state = await res.json();
        updateUI(state);
      } catch (e) {
        // ignore
      }
    }

    setInterval(pollState, 1200);
    pollState();
  </script>
</body>
</html>
        """.trimIndent()
    }
}
