package com.example.videokey.data

import kotlin.math.abs

/**
 * Studio MIDI Analyzer & SoundFont Mastering Profile Resolver for BASSMIDI (.sf2) Engine:
 * - Preserves 100% of the song's original arranged GM/GS/XG instruments (Program Changes)
 *   and natural multi-layer velocities ("kuhang-kuha ang original na tunog ng kanta").
 * - Accurately identifies Melody / Vocal Guide track(s) and channel(s) via lyric-tick correlation.
 * - Classifies all 16 MIDI channels into Instrument Mixer groups:
 *   DRUMS, BASS, GUITAR_PIANO, STRINGS_BRASS, and MELODY for real-time BASSMIDI mixing.
 */
object MidiMelodyScaler {

    enum class InstrumentCategory {
        MELODY,
        DRUMS,
        BASS,
        GUITAR_PIANO,
        STRINGS_BRASS
    }

    enum class SongMixStyle(
        val labelTagalog: String,
        val labelEnglish: String,
        val emoji: String,
        val melody: Int,
        val drums: Int,
        val bass: Int,
        val guitarPiano: Int,
        val stringsBrass: Int,
        val reverb: Int,
        val descriptionTagalog: String,
        val descriptionEnglish: String
    ) {
        BALLAD(
            labelTagalog = "Ballad / Mabagal",
            labelEnglish = "Ballad & Love Song",
            emoji = "🎻",
            melody = 50,
            drums = 85,
            bass = 95,
            guitarPiano = 110,
            stringsBrass = 115,
            reverb = 62,
            descriptionTagalog = "Lutang ang piano & strings, banayad ang drums, may concert hall reverb.",
            descriptionEnglish = "Lush piano & orchestral strings, soft drums, spacious concert reverb."
        ),
        ROCK_UPBEAT(
            labelTagalog = "Rock / Mabilis na Beat",
            labelEnglish = "Rock & Upbeat Band",
            emoji = "🎸",
            melody = 55,
            drums = 118,
            bass = 112,
            guitarPiano = 100,
            stringsBrass = 95,
            reverb = 38,
            descriptionTagalog = "Punchy drums at kalabit ng bass guitar, tight reverb para sa mabilis na tugtog.",
            descriptionEnglish = "Punchy kick & snare, driven bass guitar, tight reverb for fast live rhythms."
        ),
        ACOUSTIC(
            labelTagalog = "Acoustic / Unplugged",
            labelEnglish = "Acoustic Unplugged",
            emoji = "🎹",
            melody = 55,
            drums = 75,
            bass = 90,
            guitarPiano = 120,
            stringsBrass = 95,
            reverb = 45,
            descriptionTagalog = "Malinaw na acoustic guitar & piano, natural na studio ambience.",
            descriptionEnglish = "Warm acoustic guitar and piano upfront, gentle natural studio ambience."
        ),
        DANCE_DISCO(
            labelTagalog = "Dance / Disco Party",
            labelEnglish = "Dance & Disco Party",
            emoji = "🪩",
            melody = 60,
            drums = 120,
            bass = 115,
            guitarPiano = 95,
            stringsBrass = 105,
            reverb = 42,
            descriptionTagalog = "Mabigat na 4-on-the-floor beat at synth/groove bass, energetic party sound.",
            descriptionEnglish = "High-energy dance groove, boosted kick & bass, dynamic party brass & synths."
        ),
        STANDARD_POP(
            labelTagalog = "Balanseng Live Band",
            labelEnglish = "Balanced Live Band",
            emoji = "🎤",
            melody = 60,
            drums = 100,
            bass = 100,
            guitarPiano = 100,
            stringsBrass = 100,
            reverb = 50,
            descriptionTagalog = "Pantay at balanseng timpla ng buong banda para sa standard pop songs.",
            descriptionEnglish = "Even, studio-balanced mix across all instruments for classic pop karaoke."
        )
    }

    data class MidiAnalysis(
        val numTracks: Int,
        val melodyTracks: Set<Int>,
        val melodyChannels: Set<Int>,
        val exclusiveMelodyChannels: Set<Int>,
        val drumChannels: Set<Int>,
        val channelPrograms: IntArray,
        val channelNoteCounts: IntArray,
        val channelAvgVelocities: IntArray = IntArray(16) { 80 },
        val detectedBpm: Int = 120,
        val detectedMixStyle: SongMixStyle = SongMixStyle.STANDARD_POP
    ) {
        fun getCategoryForChannel(channel: Int, currentProgram: Int = -1): InstrumentCategory {
            val ch = channel.coerceIn(0, 15)
            if (ch == 9 || drumChannels.contains(ch)) return InstrumentCategory.DRUMS
            if (exclusiveMelodyChannels.contains(ch)) return InstrumentCategory.MELODY
            if (numTracks <= 1 && melodyChannels.contains(ch)) return InstrumentCategory.MELODY
            val prog = if (currentProgram in 0..127) currentProgram else channelPrograms[ch]
            return when (prog) {
                in 32..39 -> InstrumentCategory.BASS
                in 0..31 -> InstrumentCategory.GUITAR_PIANO
                else -> InstrumentCategory.STRINGS_BRASS
            }
        }
    }

    data class SoundFontProfile(
        val name: String,
        val fontGain: Float,          // Clean master SoundFont headroom (prevents any clipping)
        // 10-band studio Mastering EQ in dB: [31Hz, 62Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz]
        val eqGainsDb: FloatArray,
        val reverbLevel: Float        // Default BASSMIDI reverb level (0.0f .. 1.0f)
    )

    fun resolveProfile(soundFontName: String): SoundFontProfile {
        val lower = soundFontName.trim().lowercase()
        return when {
            lower.contains("rock") || lower.contains("band") -> SoundFontProfile(
                name = soundFontName,
                fontGain = 0.84f,
                eqGainsDb = floatArrayOf(2.5f, 3.2f, 1.8f, 0.4f, -0.6f, 0.8f, 1.8f, 2.4f, 2.2f, 1.8f),
                reverbLevel = 0.38f
            )
            lower.contains("acoustic") || lower.contains("ballad") -> SoundFontProfile(
                name = soundFontName,
                fontGain = 0.80f,
                eqGainsDb = floatArrayOf(1.2f, 1.8f, 1.0f, 0.2f, 0.8f, 1.2f, 1.5f, 1.8f, 2.0f, 1.6f),
                reverbLevel = 0.50f
            )
            lower.contains("disco") || lower.contains("dance") || lower.contains("synth") -> SoundFontProfile(
                name = soundFontName,
                fontGain = 0.82f,
                eqGainsDb = floatArrayOf(2.0f, 2.6f, 1.6f, 0.0f, -0.8f, 0.4f, 1.2f, 1.8f, 2.2f, 2.0f),
                reverbLevel = 0.42f
            )
            lower.contains("party") -> SoundFontProfile(
                name = soundFontName,
                fontGain = 0.82f,
                eqGainsDb = floatArrayOf(2.2f, 3.2f, 1.8f, 0.2f, -0.6f, 0.5f, 1.4f, 1.8f, 2.0f, 1.8f),
                reverbLevel = 0.40f
            )
            lower.contains("yamaha") || lower.contains("korg") || lower.contains("ketron") -> SoundFontProfile(
                name = soundFontName,
                fontGain = 0.78f,
                eqGainsDb = floatArrayOf(1.5f, 2.2f, 1.2f, 0.0f, -0.8f, 0.2f, 0.8f, 1.4f, 1.8f, 1.5f),
                reverbLevel = 0.45f
            )
            lower.contains("dream") || lower.contains("sam") || lower.contains("2634") -> SoundFontProfile(
                name = soundFontName,
                fontGain = 0.78f,
                eqGainsDb = floatArrayOf(1.6f, 2.2f, 1.4f, 0.0f, -0.8f, 0.2f, 0.9f, 1.4f, 1.6f, 1.4f),
                reverbLevel = 0.45f
            )
            lower.contains("mega") -> SoundFontProfile(
                name = soundFontName,
                fontGain = 0.76f,
                eqGainsDb = floatArrayOf(1.4f, 2.0f, 1.2f, 0.0f, -0.6f, 0.4f, 1.1f, 1.5f, 1.6f, 1.4f),
                reverbLevel = 0.40f
            )
            lower.contains("minus") || lower.contains("studio") || lower.contains("videokey") || lower.contains("pop") || lower.contains("opm") -> SoundFontProfile(
                name = soundFontName,
                fontGain = 0.80f,
                eqGainsDb = floatArrayOf(1.8f, 2.5f, 1.5f, 0.2f, -1.0f, 0.3f, 1.0f, 1.5f, 1.8f, 1.6f),
                reverbLevel = 0.42f
            )
            else -> SoundFontProfile(
                name = soundFontName,
                fontGain = 0.80f,
                eqGainsDb = floatArrayOf(1.8f, 2.5f, 1.5f, 0.2f, -1.0f, 0.3f, 1.0f, 1.5f, 1.8f, 1.6f),
                reverbLevel = 0.42f
            )
        }
    }

    /**
     * Fast non-destructive analysis of a MIDI file to identify:
     * - Melody Guide track(s) and channel(s)
     * - Drum channel(s)
     * - Original GM Program (0..127) per channel (0..15)
     */
    fun analyzeMidi(midiBytes: ByteArray): MidiAnalysis {
        val defaultPrograms = IntArray(16) { ch -> if (ch == 1) 33 else 0 }
        val noteCounts = IntArray(16)
        if (midiBytes.size < 22) {
            return MidiAnalysis(
                numTracks = 1,
                melodyTracks = emptySet(),
                melodyChannels = setOf(0, 3),
                exclusiveMelodyChannels = setOf(3),
                drumChannels = setOf(9),
                channelPrograms = defaultPrograms,
                channelNoteCounts = noteCounts
            )
        }

        val len = midiBytes.size
        val lyricTicks = HashSet<Long>()
        val namedMelodyTracks = HashSet<Int>()
        var initialBpm = 0
        val channelLyricMatchCounts = IntArray(16)
        val trackNoteCounts = HashMap<Int, Int>()
        val trackLyricMatchCounts = HashMap<Int, Int>()
        val trackChannelsUsed = HashMap<Int, MutableSet<Int>>()
        val programNoteWeights = Array(16) { IntArray(128) }
        val firstProgramSeen = IntArray(16) { -1 }
        val drumChannels = HashSet<Int>().apply { add(9) }

        // Pass 1: Collect lyric ticks and track names
        var checkPos = 14
        var tIdx = 0
        while (checkPos + 8 <= len) {
            val tag = String(midiBytes, checkPos, 4, Charsets.US_ASCII)
            if (tag != "MTrk") break
            val trackLen = ((midiBytes[checkPos + 4].toInt() and 0xFF) shl 24) or
                    ((midiBytes[checkPos + 5].toInt() and 0xFF) shl 16) or
                    ((midiBytes[checkPos + 6].toInt() and 0xFF) shl 8) or
                    (midiBytes[checkPos + 7].toInt() and 0xFF)
            val trackStart = checkPos + 8
            val trackEnd = (trackStart + trackLen).coerceAtMost(len)

            var p = trackStart
            var runningStatus = 0
            var curTick = 0L
            while (p < trackEnd) {
                var delta = 0L
                while (p < trackEnd) {
                    val b = midiBytes[p++].toInt() and 0xFF
                    delta = (delta shl 7) or (b and 0x7F).toLong()
                    if ((b and 0x80) == 0) break
                }
                curTick += delta
                if (p >= trackEnd) break

                var status = midiBytes[p].toInt() and 0xFF
                if (status >= 0x80) {
                    runningStatus = status
                    p++
                } else {
                    status = runningStatus
                }

                val msgType = status and 0xF0
                when (msgType) {
                    0x80, 0x90, 0xA0, 0xB0, 0xE0 -> p += 2
                    0xC0, 0xD0 -> p += 1
                    0xF0 -> {
                        if (status == 0xFF) {
                            if (p >= trackEnd) break
                            val metaType = midiBytes[p++].toInt() and 0xFF
                            var metaLen = 0
                            while (p < trackEnd) {
                                val b = midiBytes[p++].toInt() and 0xFF
                                metaLen = (metaLen shl 7) or (b and 0x7F)
                                if ((b and 0x80) == 0) break
                            }
                            if (p + metaLen <= trackEnd) {
                                if (metaType == 0x03) {
                                    val name = String(midiBytes, p, metaLen, Charsets.ISO_8859_1).lowercase()
                                    if (name.contains("melody") || name.contains("guide") || name.contains("vocal") ||
                                        name.contains("lead") || name.contains("bbm") || name.contains("voice")
                                    ) {
                                        namedMelodyTracks.add(tIdx)
                                    }
                                } else if (metaType == 0x01 || metaType == 0x05) {
                                    val txt = String(midiBytes, p, metaLen, Charsets.ISO_8859_1)
                                    if (txt.isNotEmpty() && !txt.startsWith("@")) {
                                        val baseBucket = curTick / 16L
                                        lyricTicks.add(baseBucket)
                                        lyricTicks.add(baseBucket - 1L)
                                        lyricTicks.add(baseBucket + 1L)
                                        lyricTicks.add(baseBucket - 2L)
                                        lyricTicks.add(baseBucket + 2L)
                                    }
                                } else if (metaType == 0x51 && metaLen == 3 && p + 3 <= trackEnd) {
                                    val tempoMicros = ((midiBytes[p].toInt() and 0xFF) shl 16) or
                                            ((midiBytes[p + 1].toInt() and 0xFF) shl 8) or
                                            (midiBytes[p + 2].toInt() and 0xFF)
                                    if (tempoMicros > 0 && initialBpm == 0) {
                                        initialBpm = (60000000L / tempoMicros).toInt().coerceIn(40, 260)
                                    }
                                }
                            }
                            p += metaLen
                        } else {
                            var sysLen = 0
                            while (p < trackEnd) {
                                val b = midiBytes[p++].toInt() and 0xFF
                                sysLen = (sysLen shl 7) or (b and 0x7F)
                                if ((b and 0x80) == 0) break
                            }
                            p += sysLen
                        }
                    }
                    else -> break
                }
            }
            checkPos = trackEnd
            tIdx++
        }
        val totalTracks = tIdx.coerceAtLeast(1)

        // Pass 2: Correlate Note-On events with lyric ticks & track Program Changes per channel
        val activeProgPerChan = IntArray(16) { 0 }
        val channelPitchSums = LongArray(16)
        val channelVelocitySums = LongArray(16)
        checkPos = 14
        tIdx = 0
        while (checkPos + 8 <= len) {
            val tag = String(midiBytes, checkPos, 4, Charsets.US_ASCII)
            if (tag != "MTrk") break
            val trackLen = ((midiBytes[checkPos + 4].toInt() and 0xFF) shl 24) or
                    ((midiBytes[checkPos + 5].toInt() and 0xFF) shl 16) or
                    ((midiBytes[checkPos + 6].toInt() and 0xFF) shl 8) or
                    (midiBytes[checkPos + 7].toInt() and 0xFF)
            val trackStart = checkPos + 8
            val trackEnd = (trackStart + trackLen).coerceAtMost(len)

            var p = trackStart
            var runningStatus = 0
            var curTick = 0L
            while (p < trackEnd) {
                var delta = 0L
                while (p < trackEnd) {
                    val b = midiBytes[p++].toInt() and 0xFF
                    delta = (delta shl 7) or (b and 0x7F).toLong()
                    if ((b and 0x80) == 0) break
                }
                curTick += delta
                if (p >= trackEnd) break

                var status = midiBytes[p].toInt() and 0xFF
                if (status >= 0x80) {
                    runningStatus = status
                    p++
                } else {
                    status = runningStatus
                }

                val msgType = status and 0xF0
                val channel = status and 0x0F
                when (msgType) {
                    0x90 -> {
                        if (p + 1 < trackEnd) {
                            val note = midiBytes[p].toInt() and 0x7F
                            val vel = midiBytes[p + 1].toInt() and 0xFF
                            if (vel > 0) {
                                noteCounts[channel]++
                                channelPitchSums[channel] += note.toLong()
                                channelVelocitySums[channel] += vel.toLong()
                                trackChannelsUsed.getOrPut(tIdx) { HashSet() }.add(channel)
                                val curProg = activeProgPerChan[channel].coerceIn(0, 127)
                                programNoteWeights[channel][curProg]++
                                if (channel != 9) {
                                    trackNoteCounts[tIdx] = (trackNoteCounts[tIdx] ?: 0) + 1
                                    if (lyricTicks.contains(curTick / 16L)) {
                                        channelLyricMatchCounts[channel]++
                                        trackLyricMatchCounts[tIdx] = (trackLyricMatchCounts[tIdx] ?: 0) + 1
                                    }
                                }
                            }
                            p += 2
                        } else break
                    }
                    0xC0 -> {
                        if (p < trackEnd) {
                            val prog = midiBytes[p].toInt() and 0x7F
                            activeProgPerChan[channel] = prog
                            if (firstProgramSeen[channel] == -1) {
                                firstProgramSeen[channel] = prog
                            }
                            p += 1
                        } else break
                    }
                    0x80, 0xA0, 0xB0, 0xE0 -> p += 2
                    0xD0 -> p += 1
                    0xF0 -> {
                        if (status == 0xFF) {
                            if (p >= trackEnd) break
                            p++
                            var metaLen = 0
                            while (p < trackEnd) {
                                val b = midiBytes[p++].toInt() and 0xFF
                                metaLen = (metaLen shl 7) or (b and 0x7F)
                                if ((b and 0x80) == 0) break
                            }
                            p += metaLen
                        } else {
                            var sysLen = 0
                            while (p < trackEnd) {
                                val b = midiBytes[p++].toInt() and 0xFF
                                sysLen = (sysLen shl 7) or (b and 0x7F)
                                if ((b and 0x80) == 0) break
                            }
                            p += sysLen
                        }
                    }
                    else -> break
                }
            }
            checkPos = trackEnd
            tIdx++
        }

        // Determine dominant GM program per channel
        for (ch in 0..15) {
            var bestProg = firstProgramSeen[ch].coerceAtLeast(0)
            var bestWeight = -1
            for (prog in 0..127) {
                val w = programNoteWeights[ch][prog]
                if (w > bestWeight && w > 0) {
                    bestWeight = w
                    bestProg = prog
                }
            }
            // Heuristic: if a channel has no explicit Program Change or program 0, check if its average pitch is bass (< 48) on ch 1/2
            if (bestProg == 0 && (ch == 1 || ch == 2) && noteCounts[ch] > 10) {
                val avgPitch = channelPitchSums[ch] / noteCounts[ch]
                if (avgPitch in 24..50) {
                    bestProg = 33 // Fingered Bass
                }
            }
            defaultPrograms[ch] = bestProg
        }

        // Determine Melody Channel(s) and Track(s)
        val melodyChannels = HashSet<Int>()
        var bestLyricChannel = -1
        var bestLyricScore = 0
        for (ch in 0..15) {
            if (ch == 9 || defaultPrograms[ch] in 32..39) continue // Skip drums and bass
            val matches = channelLyricMatchCounts[ch]
            if (matches > bestLyricScore) {
                bestLyricScore = matches
                bestLyricChannel = ch
            }
        }
        if (bestLyricChannel >= 0 && bestLyricScore >= 5) {
            melodyChannels.add(bestLyricChannel)
        }
        if (melodyChannels.isEmpty()) {
            if (noteCounts[3] > 0 && defaultPrograms[3] !in 32..39) {
                melodyChannels.add(3)
            } else if (noteCounts[0] > 0 && defaultPrograms[0] !in 32..39) {
                melodyChannels.add(0)
            }
        }

        var bestLyricTrack = -1
        var bestTrackMatches = 0
        for ((trk, matches) in trackLyricMatchCounts) {
            if (matches > bestTrackMatches) {
                bestTrackMatches = matches
                bestLyricTrack = trk
            }
        }
        if (bestLyricTrack >= 0 && bestTrackMatches >= 5) {
            namedMelodyTracks.add(bestLyricTrack)
        }
        if (namedMelodyTracks.isEmpty() && totalTracks > 1) {
            // Find track that exclusively uses bestLyricChannel
            for ((trk, chans) in trackChannelsUsed) {
                if (chans.size == 1 && melodyChannels.contains(chans.first())) {
                    namedMelodyTracks.add(trk)
                }
            }
        }

        // Determine which melody channels are exclusively used by melody tracks
        val nonMelodyChannels = HashSet<Int>()
        if (totalTracks > 1 && namedMelodyTracks.isNotEmpty()) {
            for ((trk, chans) in trackChannelsUsed) {
                if (!namedMelodyTracks.contains(trk)) {
                    nonMelodyChannels.addAll(chans)
                }
            }
        }
        val exclusiveMelodyChannels = melodyChannels.filter { !nonMelodyChannels.contains(it) }.toSet()

        val finalBpm = if (initialBpm > 0) initialBpm else 120
        val totalNotes = noteCounts.sum()
        val drumNotes = noteCounts[9] + drumChannels.filter { it != 9 }.sumOf { noteCounts[it] }

        var hasDistortionGuitar = false
        var hasPianoOrStrings = false
        var hasSynth = false

        for (ch in 0..15) {
            if (ch in drumChannels) continue
            val prog = defaultPrograms[ch]
            val notes = noteCounts[ch]
            if (notes > 25) {
                if (prog in 29..31) hasDistortionGuitar = true
                if (prog in 0..5 || prog in 40..51) hasPianoOrStrings = true
                if (prog in 38..39 || prog in 80..103) hasSynth = true
            }
        }

        val detectedStyle = when {
            // 1. Acoustic / Unplugged: Very low drum notes or no drums at all
            totalNotes > 50 && (drumNotes < 20 || (drumNotes.toFloat() / totalNotes) < 0.07f) -> SongMixStyle.ACOUSTIC

            // 2. Rock / Heavy Overdrive: Distortion / Overdriven guitar or Fast Tempo with heavy drums
            hasDistortionGuitar || (finalBpm >= 132 && drumNotes > 100) -> SongMixStyle.ROCK_UPBEAT

            // 3. Dance / Disco: Fast 118-138 BPM with synths or very heavy drum density
            finalBpm in 118..138 && (hasSynth || drumNotes > totalNotes * 0.28f) -> SongMixStyle.DANCE_DISCO

            // 4. Ballad / Slow Love Songs: BPM <= 88, or BPM <= 98 with heavy piano/strings and no distortion
            finalBpm in 1..88 || (finalBpm in 89..98 && hasPianoOrStrings && !hasDistortionGuitar) -> SongMixStyle.BALLAD

            // 5. Default: Balanced standard pop
            else -> SongMixStyle.STANDARD_POP
        }

        val avgVelocities = IntArray(16) { ch ->
            val c = noteCounts[ch]
            if (c > 0) (channelVelocitySums[ch] / c).toInt().coerceIn(1, 127) else 80
        }

        return MidiAnalysis(
            numTracks = totalTracks,
            melodyTracks = namedMelodyTracks,
            melodyChannels = melodyChannels,
            exclusiveMelodyChannels = if (exclusiveMelodyChannels.isNotEmpty()) exclusiveMelodyChannels else melodyChannels,
            drumChannels = drumChannels,
            channelPrograms = defaultPrograms,
            channelNoteCounts = noteCounts,
            channelAvgVelocities = avgVelocities,
            detectedBpm = finalBpm,
            detectedMixStyle = detectedStyle
        )
    }

    /**
     * Preserves 100% of the original MIDI arrangement (Program Changes, velocities, CC curves)
     * so BASSMIDI synthesizes the authentic song arrangement directly from the .sf2 SoundFont.
     */
    fun scaleMelody(
        midiBytes: ByteArray,
        volumePercent: Int,
        soundFontName: String = ""
    ): ByteArray {
        return midiBytes
    }
}
