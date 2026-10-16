package com.example.videokey.remote

import android.util.Log
import com.example.videokey.data.Song
import com.example.videokey.ui.main.VideoKeyViewModel
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

    fun start() {
        if (isRunning) return
        isRunning = true
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
        scope.cancel()
    }

    private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
        try {
            socket.use { s ->
                val reader = BufferedReader(InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8))
                val output = BufferedOutputStream(s.getOutputStream())

                val requestLine = reader.readLine() ?: return@use
                val tokens = requestLine.split(" ")
                if (tokens.size < 2) return@use

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
                        songs.take(60).forEach { song ->
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
                "tv" -> viewModel.toggleTvMode()
                "settings" -> viewModel.openSettingsDialog()
                "settings_close" -> viewModel.closeSettingsDialog()
                "songbook" -> if (viewModel.uiState.value.isSongbookOpen) viewModel.closeSongbook() else viewModel.openSongbook()
                "reserve_list" -> viewModel.toggleReserveQueueOverlay()
                "favorites" -> viewModel.toggleFavoritesOverlay()
                "record" -> viewModel.toggleRecording()
                "recordings_dialog" -> viewModel.openRecordingsDialog()
                "soundfont" -> {
                    val sf = params["name"] ?: params["font"] ?: ""
                    if (sf.isNotBlank()) viewModel.selectSoundFont(sf)
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
  <title>VideoKey TV Remote</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; -webkit-tap-highlight-color: transparent; }
    body {
      background: #090D16;
      color: #FFF;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial, sans-serif;
      padding: 12px;
      display: flex;
      flex-direction: column;
      align-items: center;
      min-height: 100vh;
    }
    .remote-container {
      width: 100%;
      max-width: 420px;
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
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

    /* Songbook Drawer */
    .songbook-box {
      background: #0F172A;
      border: 1.5px solid #6366F1;
      border-radius: 12px;
      padding: 10px;
    }
    .search-input {
      width: 100%;
      height: 38px;
      background: #020617;
      border: 1px solid #475569;
      border-radius: 8px;
      padding: 0 10px;
      color: #FFF;
      font-size: 14px;
      outline: none;
    }
    .search-input:focus { border-color: #818CF8; }
    .song-results {
      margin-top: 8px;
      max-height: 180px;
      overflow-y: auto;
      display: flex;
      flex-direction: column;
      gap: 6px;
    }
    .song-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 6px 8px;
      background: #1E293B;
      border-radius: 6px;
      font-size: 12px;
    }
    .song-info { flex: 1; min-width: 0; padding-right: 8px; }
    .item-title { font-weight: bold; color: #FFF; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .item-sub { color: #94A3B8; font-size: 10.5px; }
    .item-btns { display: flex; gap: 4px; }
    .item-btn {
      padding: 4px 8px;
      border-radius: 4px;
      font-size: 10px;
      font-weight: 900;
      border: none;
      cursor: pointer;
    }
    .btn-item-play { background: #10B981; color: #000; }
    .btn-item-res { background: #F59E0B; color: #000; }
  </style>
</head>
<body>
  <div class="remote-container">
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

    <!-- Quick Songbook Search -->
    <div class="songbook-box">
      <input type="text" id="searchInput" class="search-input" placeholder="🔍 Maghanap ng Pamagat o Artist..." oninput="onSearchInput()">
      <div id="searchResults" class="song-results">
        <div style="font-size: 11px; color: #64748B; text-align: center; padding: 10px;">I-type ang pamagat o mang-aawit upang maghanap...</div>
      </div>
    </div>
  </div>

  <script>
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

    async function playSongCode(code) {
      try {
        const res = await fetch('/api/action', {
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body: 'action=play_code&code=' + encodeURIComponent(code)
        });
        const state = await res.json();
        updateUI(state);
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
      searchTimer = setTimeout(async () => {
        const q = document.getElementById('searchInput').value.trim();
        if (!q) {
          document.getElementById('searchResults').innerHTML = '<div style="font-size: 11px; color: #64748B; text-align: center; padding: 10px;">I-type ang pamagat o mang-aawit upang maghanap...</div>';
          return;
        }
        try {
          const res = await fetch('/api/songs?q=' + encodeURIComponent(q));
          const list = await res.json();
          renderSearchResults(list);
        } catch (e) {
          console.error(e);
        }
      }, 250);
    }

    function renderSearchResults(songs) {
      const container = document.getElementById('searchResults');
      if (!songs || songs.length === 0) {
        container.innerHTML = '<div style="font-size: 11px; color: #EF4444; text-align: center; padding: 10px;">Walang nakitang kanta</div>';
        return;
      }
      container.innerHTML = songs.map(s => `
        <div class="song-item">
          <div class="song-info">
            <div class="item-title">[${'$'}{s.code}] ${'$'}{s.title}</div>
            <div class="item-sub">${'$'}{s.artist}</div>
          </div>
          <div class="item-btns">
            <button class="item-btn btn-item-res" onclick="reserveSongCode('${'$'}{s.code}')">RESERVE</button>
            <button class="item-btn btn-item-play" onclick="playSongCode('${'$'}{s.code}')">PLAY</button>
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
