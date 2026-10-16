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

        // Division: ticks per quarter note
        val ppq = if (division > 0) division else 480

        val tempoMap = mutableListOf<Pair<Long, Long>>() // tick to microseconds per quarter note
        val rawLyrics = mutableListOf<Pair<Long, String>>() // tick to text
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
            val trackLen = buffer.int
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

                if (!buffer.hasRemaining()) break
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
                    val length = readVariableLength(buffer).toInt().coerceAtMost(buffer.remaining())
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
                        0x01, 0x05 -> {
                            // 0x01: Text, 0x05: Lyric
                            val text = String(data, Charsets.ISO_8859_1)
                            if (text.isNotEmpty() && !text.startsWith("@")) {
                                rawLyrics.add(Pair(currentTick, text))
                                isMelodyTrack = true
                            }
                        }
                    }
                } else if (status == 0xF0 || status == 0xF7) {
                    // SysEx event
                    val length = readVariableLength(buffer).toInt().coerceAtMost(buffer.remaining())
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

            allNoteSpans.addAll(trackNotes)
            rawAllNotes.addAll(trackRawNotes)
            if (isMelodyTrack) {
                melodyNoteSpans.addAll(trackNotes)
                rawMelodyNotes.addAll(trackRawNotes.filter { it.channel != 9 })
            }
        }

        val effectiveMelodyNotes = if (melodyNoteSpans.isNotEmpty()) melodyNoteSpans else allNoteSpans
        val effectiveRawMelody = if (rawMelodyNotes.isNotEmpty()) {
            rawMelodyNotes
        } else {
            val ch0or3 = rawAllNotes.filter { it.channel == 0 || it.channel == 3 }
            if (ch0or3.isNotEmpty()) ch0or3 else rawAllNotes.filter { it.channel != 9 }
        }

        // Sort tempo and lyrics by tick
        tempoMap.sortBy { it.first }
        rawLyrics.sortBy { it.first }
        effectiveMelodyNotes.sortBy { it.first }

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

        // Group into lines - synchronized with melody notes and syllables
        val lines = mutableListOf<KaraokeLine>()
        var currentSyllables = mutableListOf<KaraokeLyric>()
        var lineIndex = 0
        var currentLineChars = 0

        for (i in rawLyrics.indices) {
            val (tick, text) = rawLyrics[i]
            val timestampMs = tickToMs(tick)
            
            // Sync syllable duration with the nearest melody note if available
            val matchedNote = effectiveMelodyNotes.minByOrNull { kotlin.math.abs(it.first - tick) }
            val noteDurationMs = if (matchedNote != null && kotlin.math.abs(matchedNote.first - tick) <= ppq) {
                (tickToMs(matchedNote.second) - tickToMs(matchedNote.first)).coerceIn(120L, 3500L)
            } else {
                null
            }

            val nextTick = if (i + 1 < rawLyrics.size) rawLyrics[i + 1].first else tick + ppq
            val defaultDuration = (tickToMs(nextTick) - timestampMs).coerceIn(120L, 2500L)
            val duration = noteDurationMs ?: defaultDuration

            val prevTick = if (i > 0) rawLyrics[i - 1].first else tick
            val gapMs = tickToMs(tick) - tickToMs(prevTick)

            val isExplicitBreak = text.contains("\r") || text.contains("\n") ||
                    text.startsWith("/") || text.startsWith("\\")
            val isTimeGapBreak = gapMs > 1300L && currentSyllables.isNotEmpty()
            val prevText = currentSyllables.lastOrNull()?.text ?: ""
            val isWordBoundary = text.startsWith(" ") || prevText.endsWith(" ")
            val isLengthBreak = (currentLineChars >= 28 || currentSyllables.size >= 7) && isWordBoundary

            if ((isExplicitBreak || isTimeGapBreak || isLengthBreak) && currentSyllables.isNotEmpty()) {
                val start = currentSyllables.first().timestampMs
                val end = currentSyllables.last().timestampMs + currentSyllables.last().durationMs
                val full = currentSyllables.joinToString("") { it.text }.trim()
                if (full.isNotEmpty()) {
                    lines.add(KaraokeLine(lineIndex++, currentSyllables, start, end, full))
                }
                currentSyllables = mutableListOf()
                currentLineChars = 0
            }

            val cleanText = text.removePrefix("/").removePrefix("\\").replace("\r", "").replace("\n", "")
            if (cleanText.isNotEmpty()) {
                currentSyllables.add(KaraokeLyric(cleanText, timestampMs, duration))
                currentLineChars += cleanText.length
            }
        }

        if (currentSyllables.isNotEmpty()) {
            val start = currentSyllables.first().timestampMs
            val end = currentSyllables.last().timestampMs + currentSyllables.last().durationMs
            val full = currentSyllables.joinToString("") { it.text }.trim()
            if (full.isNotEmpty()) {
                lines.add(KaraokeLine(lineIndex, currentSyllables, start, end, full))
            }
        }

        return ParsedKaraoke(lines, durationMs, initialBpm, parsedMelodyNotes, parsedAccompNotes)
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
