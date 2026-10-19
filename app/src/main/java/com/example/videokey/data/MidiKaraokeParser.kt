package com.example.videokey.data

import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * High-performance Parser for Standard MIDI (.mid) and Karaoke (.kar) files.
 * Extracts synchronized lyric events with millisecond timestamps and tempo maps.
 */
object MidiKaraokeParser {

    data class MelodyNote(
        val midiNote: Int,
        val startMs: Long,
        val endMs: Long,
        val velocity: Int = 100,
        val channel: Int = 0
    )

    data class ParsedKaraoke(
        val lines: List<KaraokeLine>,
        val durationMs: Long,
        val initialBpm: Int,
        val melodyNotes: List<MelodyNote> = emptyList(),
        val accompNotes: List<MelodyNote> = emptyList()
    )

    fun parse(file: File): ParsedKaraoke {
        return parse(file.readBytes())
    }

    private data class RawTrackNote(
        val midiNote: Int,
        val startTick: Long,
        val endTick: Long,
        val velocity: Int,
        val channel: Int
    )

    fun parse(bytes: ByteArray): ParsedKaraoke {
        if (bytes.size < 14) return ParsedKaraoke(emptyList(), 0, 120)

        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)

        // Read header "MThd"
        val headerTag = ByteArray(4)
        buffer.get(headerTag)
        if (String(headerTag) != "MThd") return ParsedKaraoke(emptyList(), 0, 120)

        val headerLen = buffer.int
        val format = buffer.short.toInt()
        val numTracks = buffer.short.toInt()
        val division = buffer.short.toInt()
        if (headerLen > 6 && 8 + headerLen <= buffer.limit()) {
            buffer.position(8 + headerLen)
        }

        // Division: ticks per quarter note
        val ppq = if (division > 0) division else 480

        val tempoMap = mutableListOf<Pair<Long, Long>>() // tick to microseconds per quarter note
        val rawLyricMeta = mutableListOf<Pair<Long, String>>() // 0x05 Lyric events
        val rawTextMeta = mutableListOf<Pair<Long, String>>()  // 0x01 Text events
        val melodyNoteSpans = mutableListOf<Pair<Long, Long>>() // startTick to endTick for melody notes
        val allNoteSpans = mutableListOf<Pair<Long, Long>>()
        val rawMelodyNotes = mutableListOf<RawTrackNote>()
        val rawAllNotes = mutableListOf<RawTrackNote>()
        var maxTick = 0L

        // Iterate through all tracks
        for (t in 0 until numTracks) {
            if (buffer.remaining() < 8) break
            val trackTag = ByteArray(4)
            buffer.get(trackTag)
            val tagStr = String(trackTag, Charsets.ISO_8859_1)
            if (tagStr != "MTrk") {
                break
            }
            val trackLen = buffer.int.coerceAtLeast(0)
            val trackEndPos = (buffer.position() + trackLen).coerceAtMost(buffer.limit())

            var currentTick = 0L
            var runningStatus: Int = 0
            var isMelodyTrack = (t == 1)
            val trackActiveNotes = mutableMapOf<Int, Pair<Long, Int>>() // note -> (startTick, vel)
            val trackNotes = mutableListOf<Pair<Long, Long>>()
            val trackRawNotes = mutableListOf<RawTrackNote>()

            while (buffer.position() < trackEndPos && buffer.hasRemaining()) {
                val delta = readVariableLength(buffer)
                currentTick += delta
                if (currentTick > maxTick) maxTick = currentTick

                if (buffer.position() >= trackEndPos || !buffer.hasRemaining()) break
                var status = buffer.get().toInt() and 0xFF
                if (status < 0x80) {
                    // Running status
                    buffer.position(buffer.position() - 1)
                    status = runningStatus
                } else {
                    runningStatus = status
                }

                if (status == 0xFF) {
                    // Meta event
                    if (!buffer.hasRemaining()) break
                    val metaType = buffer.get().toInt() and 0xFF
                    val length = readVariableLength(buffer).toInt().coerceIn(0, buffer.remaining())
                    val data = ByteArray(length)
                    buffer.get(data)

                    when (metaType) {
                        0x03 -> {
                            // Track Name
                            val name = String(data, Charsets.ISO_8859_1).lowercase()
                            if (name.contains("melody") || name.contains("vocal") || name.contains("lead") || name.contains("guide") || name.contains("bbm")) {
                                isMelodyTrack = true
                            }
                        }
                        0x51 -> {
                            // Set Tempo (3 bytes)
                            if (length == 3) {
                                val tempo = ((data[0].toInt() and 0xFF) shl 16) or
                                        ((data[1].toInt() and 0xFF) shl 8) or
                                        (data[2].toInt() and 0xFF)
                                tempoMap.add(Pair(currentTick, tempo.toLong()))
                            }
                        }
                        0x05 -> {
                            // 0x05: Dedicated Lyric Meta Event
                            val text = String(data, Charsets.ISO_8859_1)
                            if (text.isNotEmpty() && !isMetadataTag(text)) {
                                rawLyricMeta.add(Pair(currentTick, text))
                                isMelodyTrack = true
                            }
                        }
                        0x01 -> {
                            // 0x01: Text Meta Event (used by .kar files)
                            val text = String(data, Charsets.ISO_8859_1)
                            if (text.isNotEmpty() && !isMetadataTag(text)) {
                                // Ignore tick-0 header comments in KAR files (e.g., "Words and Music by...")
                                val trimmed = text.trim()
                                val isTickZeroHeader = currentTick == 0L && (
                                    trimmed.length > 24 ||
                                    trimmed.startsWith("http", ignoreCase = true) ||
                                    trimmed.startsWith("www.", ignoreCase = true) ||
                                    trimmed.contains("copyright", ignoreCase = true) ||
                                    trimmed.contains("sequenced", ignoreCase = true) ||
                                    trimmed.contains("karaoke", ignoreCase = true) ||
                                    trimmed.contains("midi", ignoreCase = true)
                                )
                                if (!isTickZeroHeader) {
                                    rawTextMeta.add(Pair(currentTick, text))
                                    isMelodyTrack = true
                                }
                            }
                        }
                    }
                } else if (status == 0xF0 || status == 0xF7) {
                    // SysEx event
                    val length = readVariableLength(buffer).toInt().coerceIn(0, buffer.remaining())
                    buffer.position(buffer.position() + length)
                } else {
                    // Channel message
                    val command = status and 0xF0
                    val channel = status and 0x0F
                    when (command) {
                        0x90 -> {
                            if (buffer.remaining() < 2) break
                            val note = buffer.get().toInt() and 0x7F
                            val vel = buffer.get().toInt() and 0x7F
                            val key = (channel shl 8) or note
                            if (vel > 0) {
                                trackActiveNotes[key] = Pair(currentTick, vel)
                            } else {
                                val startInfo = trackActiveNotes.remove(key)
                                if (startInfo != null && currentTick > startInfo.first) {
                                    trackNotes.add(Pair(startInfo.first, currentTick))
                                    trackRawNotes.add(RawTrackNote(note, startInfo.first, currentTick, startInfo.second, channel))
                                }
                            }
                        }
                        0x80 -> {
                            if (buffer.remaining() < 2) break
                            val note = buffer.get().toInt() and 0x7F
                            buffer.get() // velocity
                            val key = (channel shl 8) or note
                            val startInfo = trackActiveNotes.remove(key)
                            if (startInfo != null && currentTick > startInfo.first) {
                                trackNotes.add(Pair(startInfo.first, currentTick))
                                trackRawNotes.add(RawTrackNote(note, startInfo.first, currentTick, startInfo.second, channel))
                            }
                        }
                        0xC0, 0xD0 -> if (buffer.hasRemaining()) buffer.get() // 1 data byte
                        0xA0, 0xB0, 0xE0 -> {
                            if (buffer.remaining() >= 2) {
                                buffer.get()
                                buffer.get()
                            }
                        } // 2 data bytes
                    }
                }
            }

            // Always align buffer position to the exact end of the track chunk
            buffer.position(trackEndPos)

            allNoteSpans.addAll(trackNotes)
            rawAllNotes.addAll(trackRawNotes)
            if (isMelodyTrack) {
                melodyNoteSpans.addAll(trackNotes)
                rawMelodyNotes.addAll(trackRawNotes.filter { it.channel != 9 })
            }
        }

        // Prefer 0x05 Lyric events if present; otherwise use 0x01 Text events (do not mix both to avoid duplicates/headers)
        val rawLyrics = if (rawLyricMeta.size >= 5) {
            rawLyricMeta
        } else if (rawTextMeta.isNotEmpty()) {
            rawTextMeta
        } else {
            rawLyricMeta
        }

        // 1. Correlate note events with rawLyrics to detect true vocal melody channel
        val channelLyricMatches = IntArray(16)
        val correlationThreshold = (ppq / 2).coerceAtLeast(30)
        for (ch in 0..15) {
            if (ch == 9 || ch == 1) continue // Skip drums and bass
            val chNotes = rawAllNotes.filter { it.channel == ch }
            if (chNotes.isEmpty()) continue
            var matches = 0
            for (lyr in rawLyrics) {
                val lt = lyr.first
                if (chNotes.any { kotlin.math.abs(it.startTick - lt) <= correlationThreshold }) {
                    matches++
                }
            }
            channelLyricMatches[ch] = matches
        }

        var bestLyricCh = -1
        var bestMatches = 0
        for (ch in 0..15) {
            if (channelLyricMatches[ch] > bestMatches) {
                bestMatches = channelLyricMatches[ch]
                bestLyricCh = ch
            }
        }

        val effectiveRawMelody = when {
            rawMelodyNotes.isNotEmpty() -> rawMelodyNotes
            bestLyricCh >= 0 && bestMatches >= 5 -> rawAllNotes.filter { it.channel == bestLyricCh }
            else -> {
                val ch0or3 = rawAllNotes.filter { it.channel == 0 || it.channel == 3 }
                if (ch0or3.isNotEmpty()) ch0or3 else rawAllNotes.filter { it.channel != 9 }
            }
        }

        val effectiveMelodyNotes = effectiveRawMelody.map { Pair(it.startTick, it.endTick) }.sortedBy { it.first }

        // Stable sort tempo and lyrics by tick
        tempoMap.sortBy { it.first }
        rawLyrics.sortBy { it.first }

        // Helper to convert ticks to milliseconds
        fun tickToMs(tick: Long): Long {
            if (tempoMap.isEmpty()) {
                val defaultTempo = 500_000L // 120 BPM
                return (tick * defaultTempo) / (ppq * 1000L)
            }

            var ms = 0.0
            var prevTick = 0L
            var currentTempo = 500_000L

            for (entry in tempoMap) {
                if (tick <= entry.first) {
                    break
                }
                val deltaTicks = entry.first - prevTick
                ms += (deltaTicks.toDouble() * currentTempo.toDouble()) / (ppq * 1000.0)
                prevTick = entry.first
                currentTempo = entry.second
            }

            val remainingTicks = tick - prevTick
            ms += (remainingTicks.toDouble() * currentTempo.toDouble()) / (ppq * 1000.0)
            return ms.toLong()
        }

        val durationMs = tickToMs(maxTick)
        val initialBpm = if (tempoMap.isNotEmpty()) (60_000_000L / tempoMap[0].second).toInt() else 120

        val parsedMelodyNotes = effectiveRawMelody.map {
            MelodyNote(
                midiNote = it.midiNote,
                startMs = tickToMs(it.startTick),
                endMs = tickToMs(it.endTick).coerceAtLeast(tickToMs(it.startTick) + 80L),
                velocity = it.velocity,
                channel = it.channel
            )
        }.sortedBy { it.startMs }

        val parsedAccompNotes = rawAllNotes.filter { it.channel != 9 }.take(4000).map {
            MelodyNote(
                midiNote = it.midiNote,
                startMs = tickToMs(it.startTick),
                endMs = tickToMs(it.endTick).coerceAtLeast(tickToMs(it.startTick) + 80L),
                velocity = it.velocity,
                channel = it.channel
            )
        }.sortedBy { it.startMs }

        // Tokenize raw lyrics into normalized syllable tokens with accurate breakBefore / breakAfter flags.
        // This prevents trailing '\r\n' on the last syllable of a line from cutting that syllable onto the next line!
        val tokens = tokenizeLyrics(rawLyrics)
        val hasExplicitBreaks = tokens.count { it.breakBefore || it.breakAfter } >= 3

        val rawLines = mutableListOf<MutableList<KaraokeLyric>>()
        var currentSyllables = mutableListOf<KaraokeLyric>()
        var currentLineChars = 0

        fun flushCurrentLine() {
            if (currentSyllables.isEmpty()) return
            // Clean up internal syllable hyphens within words (e.g., "UM-" + "BRELLA" -> "UMBRELLA", "LOV-" + "ING" -> "LOVING")
            val cleaned = cleanupSyllableHyphens(currentSyllables)
            val full = cleaned.joinToString("") { it.text }.trim()
            if (full.isNotEmpty()) {
                rawLines.add(cleaned.toMutableList())
            }
            currentSyllables = mutableListOf()
            currentLineChars = 0
        }

        for (i in tokens.indices) {
            val token = tokens[i]
            val tick = token.tick
            val cleanText = token.text
            if (cleanText.isEmpty()) {
                if (token.breakBefore || token.breakAfter) {
                    flushCurrentLine()
                }
                continue
            }

            // Use exact authored syllable tick to prevent grouping distinct syllables into the same timestamp
            val timestampMs = tickToMs(tick)

            val matchedNote = effectiveMelodyNotes.minByOrNull { kotlin.math.abs(it.first - tick) }
            val isNoteMatched = matchedNote != null && kotlin.math.abs(matchedNote.first - tick) <= (ppq / 3).coerceAtLeast(30)
            val noteDurationMs = if (isNoteMatched) {
                (tickToMs(matchedNote!!.second) - tickToMs(matchedNote.first)).coerceIn(80L, 3500L)
            } else {
                null
            }

            val nextToken = if (i + 1 < tokens.size && !tokens[i].breakAfter && !tokens[i + 1].breakBefore) tokens[i + 1] else null
            val nextTick = nextToken?.tick ?: (if (i + 1 < tokens.size) tokens[i + 1].tick else tick + ppq)
            val timeToNextMs = (tickToMs(nextTick) - timestampMs).coerceAtLeast(60L)
            val duration = if (noteDurationMs != null && noteDurationMs < timeToNextMs) {
                noteDurationMs.coerceAtLeast(60L)
            } else {
                timeToNextMs.coerceIn(60L, 3500L)
            }

            val prevTick = if (i > 0) tokens[i - 1].tick else tick
            val gapMs = tickToMs(tick) - tickToMs(prevTick)

            val prevText = currentSyllables.lastOrNull()?.text ?: ""
            val prevEndsWithHyphen = prevText.trimEnd().endsWith("-")
            val isWordBoundary = (cleanText.startsWith(" ") || prevText.endsWith(" ")) && !prevEndsWithHyphen

            // How many syllables/chars remain before the next explicit break?
            var charsUntilNextExplicitBreak = 0
            var syllablesUntilNextExplicitBreak = 0
            if (hasExplicitBreaks) {
                for (j in i until minOf(i + 12, tokens.size)) {
                    if (j > i && tokens[j].breakBefore) break
                    charsUntilNextExplicitBreak += tokens[j].text.length
                    syllablesUntilNextExplicitBreak++
                    if (tokens[j].breakAfter) break
                }
            }

            val shouldBreakBefore = when {
                currentSyllables.isEmpty() -> false
                token.breakBefore -> true // Always honor explicit line break (/ or \) from MIDI author!
                !hasExplicitBreaks && isWordBoundary && gapMs > 2000L -> true
                hasExplicitBreaks && isWordBoundary && gapMs > 3500L -> true
                // Prevent lines from exceeding safe screen width (keep line length within 24 chars)
                isWordBoundary && currentLineChars >= 22 -> true
                else -> false
            }

            if (shouldBreakBefore) {
                flushCurrentLine()
            }

            // If we are at the start of a new line, strip leading spaces; if continuing after a hyphen on same word, ensure no stray space
            val normalizedText = if (currentSyllables.isEmpty()) {
                cleanText.trimStart()
            } else {
                cleanText
            }

            if (normalizedText.isNotEmpty()) {
                currentSyllables.add(KaraokeLyric(normalizedText, timestampMs, duration))
                currentLineChars += normalizedText.length
            }

            // If this syllable had a trailing line break (e.g., "love\r\n"), flush AFTER adding it
            if (token.breakAfter) {
                flushCurrentLine()
            }
        }

        flushCurrentLine()

        // Post-process: merge any stray/orphan short fragments (e.g. 1-3 letters or trailing hyphenated fragments)
        val mergedRawLines = mergeStrayFragments(rawLines)

        // Build final KaraokeLine list with smooth, contiguous syllable durations
        val lines = mutableListOf<KaraokeLine>()
        for (idx in mergedRawLines.indices) {
            val syls = mergedRawLines[idx]
            if (syls.isEmpty()) continue
            val full = syls.joinToString("") { it.text }.trim()
            if (full.isEmpty()) continue

            val start = syls.first().timestampMs
            val lastSyl = syls.last()
            val nextLineStart = mergedRawLines.getOrNull(idx + 1)?.firstOrNull()?.timestampMs ?: (lastSyl.timestampMs + lastSyl.durationMs + 500L)
            // Give the last syllable of the line enough hold time so it never cuts off abruptly
            val minLastDuration = lastSyl.durationMs.coerceAtLeast(350L)
            val maxAllowedEnd = (nextLineStart - 60L).coerceAtLeast(lastSyl.timestampMs + 180L)
            val end = (lastSyl.timestampMs + minLastDuration).coerceAtMost(maxAllowedEnd).coerceAtLeast(start + 250L)

            lines.add(KaraokeLine(idx, syls, start, end, full))
        }

        return ParsedKaraoke(lines, durationMs, initialBpm, parsedMelodyNotes, parsedAccompNotes)
    }

    private data class SyllableToken(
        val tick: Long,
        val text: String,
        val breakBefore: Boolean,
        val breakAfter: Boolean
    )

    private fun isMetadataTag(text: String): Boolean {
        val t = text.trim()
        if (t.isEmpty()) return false
        // Standard KAR/MIDI metadata prefixes (@K, @L, @T, @V, @I, %..., #...)
        if (t.startsWith("@") || t.startsWith("%") || t.startsWith("#")) return true
        return false
    }

    private fun isBreakChar(c: Char): Boolean = c == '\r' || c == '\n' || c == '/' || c == '\\'

    private fun tokenizeLyrics(rawLyrics: List<Pair<Long, String>>): List<SyllableToken> {
        val result = mutableListOf<SyllableToken>()
        var pendingBreakBefore = false

        for ((tick, raw) in rawLyrics) {
            // Normalize control characters (ignore ASCII < 32 except \r, \n, \t)
            val sb = StringBuilder(raw.length)
            for (ch in raw) {
                when {
                    ch == '\t' -> sb.append(' ')
                    ch == '\r' || ch == '\n' -> sb.append(ch)
                    ch.code >= 32 -> sb.append(ch)
                }
            }
            val s = sb.toString()
            if (s.isEmpty()) continue

            var idx = 0
            val n = s.length
            var leadingBreak = pendingBreakBefore
            pendingBreakBefore = false

            while (idx < n && isBreakChar(s[idx])) {
                leadingBreak = true
                idx++
            }

            if (idx >= n) {
                // Entire event was just break characters (e.g., "\r\n" or "/")
                pendingBreakBefore = true
                continue
            }

            // Scan through remaining characters; split if there are embedded break chars
            while (idx < n) {
                val segStart = idx
                while (idx < n && !isBreakChar(s[idx])) {
                    idx++
                }
                val segText = s.substring(segStart, idx)
                var trailingBreak = false
                while (idx < n && isBreakChar(s[idx])) {
                    trailingBreak = true
                    idx++
                }
                if (segText.isNotEmpty()) {
                    result.add(
                        SyllableToken(
                            tick = tick,
                            text = segText,
                            breakBefore = leadingBreak,
                            breakAfter = trailingBreak && idx >= n
                        )
                    )
                    leadingBreak = trailingBreak && idx < n
                } else if (trailingBreak) {
                    leadingBreak = true
                }
            }
        }
        return result
    }

    private fun cleanupSyllableHyphens(syllables: List<KaraokeLyric>): List<KaraokeLyric> {
        if (syllables.isEmpty()) return syllables
        val out = ArrayList<KaraokeLyric>(syllables.size)
        for (i in syllables.indices) {
            val cur = syllables[i]
            var txt = cur.text
            val trimmedEnd = txt.trimEnd()
            if (trimmedEnd.endsWith("-") && trimmedEnd.length >= 2) {
                if (i + 1 < syllables.size) {
                    val nextTxt = syllables[i + 1].text
                    // If the hyphen is an internal syllable divider inside a word, remove the hyphen
                    if (!nextTxt.startsWith(" ")) {
                        txt = trimmedEnd.dropLast(1)
                    } else {
                        txt = trimmedEnd.dropLast(1) + " "
                    }
                } else {
                    // Trailing hyphen at the very end of a line ("LONG-") -> remove dangling hyphen
                    txt = trimmedEnd.trimEnd('-', ' ')
                }
            }
            if (txt.isNotEmpty()) {
                out.add(cur.copy(text = txt))
            }
        }
        return out
    }

    private fun mergeStrayFragments(lines: List<MutableList<KaraokeLyric>>): List<MutableList<KaraokeLyric>> {
        if (lines.size <= 1) return lines
        val result = mutableListOf<MutableList<KaraokeLyric>>()

        var i = 0
        while (i < lines.size) {
            val current = lines[i]
            val currentFull = current.joinToString("") { it.text }.trim()

            // Check if this line is a tiny stray fragment (<= 3 letters, single syllable/fragment)
            val isTinyStray = current.size == 1 && currentFull.length in 1..3 && !currentFull.contains(" ")
            if (isTinyStray && result.isNotEmpty()) {
                val prev = result.last()
                val prevEndMs = prev.last().timestampMs + prev.last().durationMs
                val curStartMs = current.first().timestampMs
                val prevChars = prev.sumOf { it.text.length }
                if (curStartMs - prevEndMs <= 1000L && prevChars + currentFull.length <= 22) {
                    // Attach to the end of the previous line
                    val firstSyl = current.first()
                    val needsSpace = !prev.last().text.endsWith(" ") && !firstSyl.text.startsWith(" ") &&
                        firstSyl.text.first().isUpperCase()
                    prev.add(firstSyl.copy(text = if (needsSpace) " ${firstSyl.text}" else firstSyl.text))
                    i++
                    continue
                }
            }
            result.add(current)
            i++
        }
        return result
    }

    private fun readVariableLength(buffer: ByteBuffer): Long {
        var value = 0L
        var byte: Int
        do {
            if (!buffer.hasRemaining()) break
            byte = buffer.get().toInt() and 0xFF
            value = (value shl 7) or (byte and 0x7F).toLong()
        } while ((byte and 0x80) != 0)
        return value
    }
}
