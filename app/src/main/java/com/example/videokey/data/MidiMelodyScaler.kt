package com.example.videokey.data

import kotlin.math.abs

/**
 * High-performance real-time MIDI Melody Scaler & SoundFont Timbre Processor:
 * - Detects the dedicated Melody / Guide vocal track in Philippine videoke SMF files
 * - Scales Note-On velocities and Channel Volume (CC 7) for Melody Guide (0% to 100%)
 * - Applies distinct SoundFont (.sf2) instrument voicings (Program Changes for Melody, Piano,
 *   Guitar, Bass, Strings), Filter Brightness (CC 74), Reverb (CC 91), Chorus (CC 93),
 *   and Drum/Bass dynamic punch so selecting a new SoundFont immediately transforms the sound!
 */
object MidiMelodyScaler {

    data class SoundFontProfile(
        val name: String,
        val melodyProgram: Int,      // GM program (0-127) for melody guide track (-1 = keep)
        val pianoProgram: Int,       // GM program for piano channels (0=Grand, 1=Bright, 2=ElecGrand, 4=EP1, 5=DX7)
        val guitarProgram: Int,      // GM program for guitar (24=Nylon, 25=Steel, 26=Jazz, 27=Clean)
        val bassProgram: Int,        // GM program for bass (32=Acoustic, 33=Finger, 34=Pick, 35=Fretless, 36=Slap, 38=Synth)
        val stringsProgram: Int,     // GM program for strings (48=Strings1, 49=Strings2, 50=SynthStrings)
        val reverbCc91: Int,         // CC 91 Reverb Send (0-127)
        val chorusCc93: Int,         // CC 93 Chorus Send (0-127)
        val brightnessCc74: Int,     // CC 74 Brightness / Filter Cutoff (0-127)
        val drumVelMultiplier: Float,
        val bassVelMultiplier: Float,
        val accompVelMultiplier: Float,
        // 5-band EQ millibel offsets (60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz), BassBoost (0..1000), Virtualizer (0..1000)
        val eqBandOffsetsMb: IntArray,
        val bassBoostPerMille: Int,
        val virtualizerPerMille: Int,
        val reverbPreset: Short // android.media.audiofx.PresetReverb constants (6=LargeHall, 4=LargeRoom, 2=SmallRoom, 5=Plate)
    )

    fun resolveProfile(soundFontName: String): SoundFontProfile {
        val lower = soundFontName.trim().lowercase()
        return when {
            lower.contains("platinum") || lower.contains("hd 10") || lower.contains("hd10") -> SoundFontProfile(
                name = soundFontName,
                melodyProgram = 65,      // Alto Sax (Classic Platinum Videoke lead)
                pianoProgram = 1,        // Bright Acoustic Piano
                guitarProgram = 25,      // Acoustic Steel Guitar
                bassProgram = 36,        // Slap Bass 1
                stringsProgram = 48,     // Lush String Ensemble 1
                reverbCc91 = 110,
                chorusCc93 = 88,
                brightnessCc74 = 112,
                drumVelMultiplier = 1.22f,
                bassVelMultiplier = 1.20f,
                accompVelMultiplier = 1.08f,
                eqBandOffsetsMb = intArrayOf(600, 350, -100, 450, 700),
                bassBoostPerMille = 650,
                virtualizerPerMille = 600,
                reverbPreset = 6 // PRESET_LARGEHALL
            )
            lower.contains("dream") || lower.contains("sam") || lower.contains("2634") -> SoundFontProfile(
                name = soundFontName,
                melodyProgram = 73,      // Sweet Acoustic Flute guide (clear and pleasant)
                pianoProgram = 0,        // Acoustic Concert Grand Piano (full rich body)
                guitarProgram = 25,      // Acoustic Steel String Guitar
                bassProgram = 33,        // Electric Finger Bass (solid, deep, punchy)
                stringsProgram = 48,     // Lush String Ensemble 1
                reverbCc91 = 95,
                chorusCc93 = 70,
                brightnessCc74 = 85,
                drumVelMultiplier = 1.25f,  // Punchy live kick and snare
                bassVelMultiplier = 1.28f,  // Solid warm low-end
                accompVelMultiplier = 1.12f, // Full rich accompaniment
                eqBandOffsetsMb = intArrayOf(750, 450, 100, 200, 400),
                bassBoostPerMille = 750,
                virtualizerPerMille = 700,
                reverbPreset = 6 // PRESET_LARGEHALL (Lush concert videoke acoustic)
            )
            lower.contains("mega") -> SoundFontProfile(
                name = soundFontName,
                melodyProgram = 22,      // Harmonica / MegaPro lead
                pianoProgram = 2,        // Electric Grand Piano CP80
                guitarProgram = 27,      // Clean Electric Guitar
                bassProgram = 34,        // Picked Electric Bass
                stringsProgram = 50,     // Synth Strings 1
                reverbCc91 = 75,
                chorusCc93 = 70,
                brightnessCc74 = 120,
                drumVelMultiplier = 1.28f,
                bassVelMultiplier = 1.18f,
                accompVelMultiplier = 1.15f,
                eqBandOffsetsMb = intArrayOf(450, 200, 400, 650, 850),
                bassBoostPerMille = 500,
                virtualizerPerMille = 450,
                reverbPreset = 2 // PRESET_SMALLROOM
            )
            lower.contains("yamaha") || lower.contains("korg") || lower.contains("ketron") -> SoundFontProfile(
                name = soundFontName,
                melodyProgram = 64,      // Soprano Sax
                pianoProgram = 0,        // Concert Grand Piano
                guitarProgram = 26,      // Jazz Electric Guitar
                bassProgram = 32,        // Upright Acoustic Bass
                stringsProgram = 48,     // Concert Strings
                reverbCc91 = 118,
                chorusCc93 = 96,
                brightnessCc74 = 96,
                drumVelMultiplier = 1.15f,
                bassVelMultiplier = 1.15f,
                accompVelMultiplier = 1.05f,
                eqBandOffsetsMb = intArrayOf(550, 300, 200, 500, 600),
                bassBoostPerMille = 600,
                virtualizerPerMille = 850,
                reverbPreset = 5 // PRESET_PLATE
            )
            lower.contains("2015") || lower.contains("8mb") -> SoundFontProfile(
                name = soundFontName,
                melodyProgram = 75,      // Pan Flute
                pianoProgram = 4,        // Rhodes Electric Piano
                guitarProgram = 25,      // Steel Guitar
                bassProgram = 38,        // Synth Bass 1
                stringsProgram = 50,     // Synth Strings
                reverbCc91 = 100,
                chorusCc93 = 90,
                brightnessCc74 = 104,
                drumVelMultiplier = 1.20f,
                bassVelMultiplier = 1.22f,
                accompVelMultiplier = 1.10f,
                eqBandOffsetsMb = intArrayOf(700, 100, -150, 550, 750),
                bassBoostPerMille = 720,
                virtualizerPerMille = 650,
                reverbPreset = 6
            )
            lower.contains("songhub") || lower.isBlank() -> SoundFontProfile(
                name = soundFontName,
                melodyProgram = 73,      // Classic Flute Guide
                pianoProgram = 0,        // Acoustic Grand
                guitarProgram = 24,      // Nylon Guitar
                bassProgram = 33,        // Fingered Bass
                stringsProgram = 48,     // String Ensemble
                reverbCc91 = 80,
                chorusCc93 = 50,
                brightnessCc74 = 84,
                drumVelMultiplier = 1.05f,
                bassVelMultiplier = 1.05f,
                accompVelMultiplier = 1.0f,
                eqBandOffsetsMb = intArrayOf(250, 150, 0, 250, 350),
                bassBoostPerMille = 350,
                virtualizerPerMille = 350,
                reverbPreset = 4
            )
            else -> {
                // Deterministic distinct acoustic profile for any other custom .sf2 file
                val hash = abs(lower.hashCode())
                val melodyChoices = intArrayOf(65, 71, 21, 66, 75, 40, 68, 73, 22, 64)
                val pianoChoices = intArrayOf(1, 2, 4, 5, 0)
                val guitarChoices = intArrayOf(24, 25, 26, 27)
                val bassChoices = intArrayOf(33, 34, 35, 36, 38, 32)
                val stringChoices = intArrayOf(48, 49, 50)
                val reverbChoices = shortArrayOf(2, 4, 5, 6)
                SoundFontProfile(
                    name = soundFontName,
                    melodyProgram = melodyChoices[hash % melodyChoices.size],
                    pianoProgram = pianoChoices[(hash / 3) % pianoChoices.size],
                    guitarProgram = guitarChoices[(hash / 7) % guitarChoices.size],
                    bassProgram = bassChoices[(hash / 11) % bassChoices.size],
                    stringsProgram = stringChoices[(hash / 13) % stringChoices.size],
                    reverbCc91 = 75 + (hash % 48),
                    chorusCc93 = 55 + ((hash / 5) % 65),
                    brightnessCc74 = 65 + ((hash / 9) % 58),
                    drumVelMultiplier = 1.08f + ((hash % 20) * 0.01f),
                    bassVelMultiplier = 1.08f + (((hash / 2) % 20) * 0.01f),
                    accompVelMultiplier = 1.0f + (((hash / 4) % 15) * 0.01f),
                    eqBandOffsetsMb = intArrayOf(
                        300 + (hash % 500),
                        100 + ((hash / 2) % 400),
                        -150 + ((hash / 3) % 400),
                        250 + ((hash / 5) % 500),
                        400 + ((hash / 7) % 500)
                    ),
                    bassBoostPerMille = 400 + (hash % 500),
                    virtualizerPerMille = 400 + ((hash / 3) % 500),
                    reverbPreset = reverbChoices[hash % reverbChoices.size]
                )
            }
        }
    }

    fun scaleMelody(
        midiBytes: ByteArray,
        volumePercent: Int,
        soundFontName: String = ""
    ): ByteArray {
        if (midiBytes.size < 22) return midiBytes
        val percent = volumePercent.coerceIn(0, 100)
        val profile = resolveProfile(soundFontName)

        val copy = midiBytes.copyOf()
        val len = copy.size

        // PASS 1: Scan all tracks to collect lyric ticks, explicit melody track names,
        // and correlate Note-On events per channel with lyric ticks.
        val lyricTicks = HashSet<Long>()
        val namedMelodyTracks = HashSet<Int>()
        val channelNoteCounts = IntArray(16)
        val channelVelSums = LongArray(16)
        val channelLyricMatchCounts = IntArray(16)
        val trackNoteCounts = HashMap<Int, Int>()
        val trackLyricMatchCounts = HashMap<Int, Int>()

        // Pass 1A: Collect lyric ticks and track names
        var checkPos = 14
        var tIdx = 0
        while (checkPos + 8 <= len) {
            val tag = String(copy, checkPos, 4, Charsets.US_ASCII)
            if (tag != "MTrk") break
            val trackLen = ((copy[checkPos + 4].toInt() and 0xFF) shl 24) or
                    ((copy[checkPos + 5].toInt() and 0xFF) shl 16) or
                    ((copy[checkPos + 6].toInt() and 0xFF) shl 8) or
                    (copy[checkPos + 7].toInt() and 0xFF)
            val trackStart = checkPos + 8
            val trackEnd = (trackStart + trackLen).coerceAtMost(len)

            var p = trackStart
            var runningStatus = 0
            var curTick = 0L
            while (p < trackEnd) {
                var delta = 0L
                while (p < trackEnd) {
                    val b = copy[p++].toInt() and 0xFF
                    delta = (delta shl 7) or (b and 0x7F).toLong()
                    if ((b and 0x80) == 0) break
                }
                curTick += delta
                if (p >= trackEnd) break

                var status = copy[p].toInt() and 0xFF
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
                            val metaType = copy[p++].toInt() and 0xFF
                            var metaLen = 0
                            while (p < trackEnd) {
                                val b = copy[p++].toInt() and 0xFF
                                metaLen = (metaLen shl 7) or (b and 0x7F)
                                if ((b and 0x80) == 0) break
                            }
                            if (p + metaLen <= trackEnd) {
                                if (metaType == 0x03) {
                                    val name = String(copy, p, metaLen, Charsets.ISO_8859_1).lowercase()
                                    if (name.contains("melody") || name.contains("guide") || name.contains("vocal") ||
                                        name.contains("lead") || name.contains("bbm") || name.contains("voice")
                                    ) {
                                        namedMelodyTracks.add(tIdx)
                                    }
                                } else if (metaType == 0x01 || metaType == 0x05) {
                                    val txt = String(copy, p, metaLen, Charsets.ISO_8859_1)
                                    if (txt.isNotEmpty() && !txt.startsWith("@")) {
                                        // Quantize tick to 16-tick bucket for fast O(1) correlation lookup
                                        val baseBucket = curTick / 16L
                                        lyricTicks.add(baseBucket)
                                        lyricTicks.add(baseBucket - 1L)
                                        lyricTicks.add(baseBucket + 1L)
                                        lyricTicks.add(baseBucket - 2L)
                                        lyricTicks.add(baseBucket + 2L)
                                    }
                                }
                            }
                            p += metaLen
                        } else {
                            var sysLen = 0
                            while (p < trackEnd) {
                                val b = copy[p++].toInt() and 0xFF
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

        // Pass 1B: Correlate Note-On events with lyric ticks to find the exact melody channel & track
        checkPos = 14
        tIdx = 0
        while (checkPos + 8 <= len) {
            val tag = String(copy, checkPos, 4, Charsets.US_ASCII)
            if (tag != "MTrk") break
            val trackLen = ((copy[checkPos + 4].toInt() and 0xFF) shl 24) or
                    ((copy[checkPos + 5].toInt() and 0xFF) shl 16) or
                    ((copy[checkPos + 6].toInt() and 0xFF) shl 8) or
                    (copy[checkPos + 7].toInt() and 0xFF)
            val trackStart = checkPos + 8
            val trackEnd = (trackStart + trackLen).coerceAtMost(len)

            var p = trackStart
            var runningStatus = 0
            var curTick = 0L
            while (p < trackEnd) {
                var delta = 0L
                while (p < trackEnd) {
                    val b = copy[p++].toInt() and 0xFF
                    delta = (delta shl 7) or (b and 0x7F).toLong()
                    if ((b and 0x80) == 0) break
                }
                curTick += delta
                if (p >= trackEnd) break

                var status = copy[p].toInt() and 0xFF
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
                            val vel = copy[p + 1].toInt() and 0xFF
                            if (vel > 0 && channel != 9) {
                                channelNoteCounts[channel]++
                                channelVelSums[channel] += vel.toLong()
                                trackNoteCounts[tIdx] = (trackNoteCounts[tIdx] ?: 0) + 1
                                if (lyricTicks.contains(curTick / 16L)) {
                                    channelLyricMatchCounts[channel]++
                                    trackLyricMatchCounts[tIdx] = (trackLyricMatchCounts[tIdx] ?: 0) + 1
                                }
                            }
                            p += 2
                        } else break
                    }
                    0x80, 0xA0, 0xB0, 0xE0 -> p += 2
                    0xC0, 0xD0 -> p += 1
                    0xF0 -> {
                        if (status == 0xFF) {
                            if (p >= trackEnd) break
                            p++
                            var metaLen = 0
                            while (p < trackEnd) {
                                val b = copy[p++].toInt() and 0xFF
                                metaLen = (metaLen shl 7) or (b and 0x7F)
                                if ((b and 0x80) == 0) break
                            }
                            p += metaLen
                        } else {
                            var sysLen = 0
                            while (p < trackEnd) {
                                val b = copy[p++].toInt() and 0xFF
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

        // Determine which channel(s) and track(s) carry the vocal melody guide
        val melodyChannels = HashSet<Int>()
        var bestLyricChannel = -1
        var bestLyricScore = 0
        for (ch in 0..15) {
            if (ch == 9 || ch == 1) continue // Skip drums (9) and bass (1)
            val matches = channelLyricMatchCounts[ch]
            if (matches > bestLyricScore) {
                bestLyricScore = matches
                bestLyricChannel = ch
            }
        }
        if (bestLyricChannel >= 0 && bestLyricScore >= 5) {
            melodyChannels.add(bestLyricChannel)
        }
        // Standard GM / Philippine Videoke melody channels: Channel 0 (Ch 1) and Channel 3 (Ch 4)
        if (melodyChannels.isEmpty()) {
            if (channelNoteCounts[3] > 0) melodyChannels.add(3)
            if (channelNoteCounts[0] > 0) melodyChannels.add(0)
        } else {
            // Also include channel 3 if it has strong lyric correlation
            if (channelLyricMatchCounts[3] >= 8) melodyChannels.add(3)
            if (channelLyricMatchCounts[0] >= 8 && bestLyricChannel == 0) melodyChannels.add(0)
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

        // Calculate average melody velocity to auto-adjust and equalize volume across all songs!
        var totalMelodyVel = 0L
        var countMelodyNotes = 0
        for (ch in melodyChannels) {
            totalMelodyVel += channelVelSums[ch]
            countMelodyNotes += channelNoteCounts[ch]
        }
        val avgMelodyVel = if (countMelodyNotes > 0) (totalMelodyVel.toFloat() / countMelodyNotes) else 80f
        // Auto-scale factor: normalizes quiet songs (vel ~40-60) up and very loud songs down to target 96
        val melodyAutoGain = (96.0f / avgMelodyVel.coerceAtLeast(35f)).coerceIn(0.75f, 2.2f)

        // PASS 2: Scale Melody Guide notes/volume & apply SoundFont profile
        var pos = 14
        var trackIndex = 0
        while (pos + 8 <= len) {
            val tag = String(copy, pos, 4, Charsets.US_ASCII)
            if (tag != "MTrk") break
            val trackLen = ((copy[pos + 4].toInt() and 0xFF) shl 24) or
                    ((copy[pos + 5].toInt() and 0xFF) shl 16) or
                    ((copy[pos + 6].toInt() and 0xFF) shl 8) or
                    (copy[pos + 7].toInt() and 0xFF)

            val trackStart = pos + 8
            val trackEnd = (trackStart + trackLen).coerceAtMost(len)
            val isMelodyTrack = namedMelodyTracks.contains(trackIndex) || (namedMelodyTracks.isEmpty() && trackIndex == 1 && (trackNoteCounts[1] ?: 0) > 0)

            var p = trackStart
            var runningStatus = 0
            while (p < trackEnd) {
                // Skip variable-length delta time
                while (p < trackEnd) {
                    val b = copy[p++].toInt() and 0xFF
                    if ((b and 0x80) == 0) break
                }
                if (p >= trackEnd) break

                var status = copy[p].toInt() and 0xFF
                if (status >= 0x80) {
                    runningStatus = status
                    p++
                } else {
                    status = runningStatus
                }

                val msgType = status and 0xF0
                val channel = status and 0x0F
                val isMelodyEvent = (channel != 9 && channel != 1) && (isMelodyTrack || melodyChannels.contains(channel))

                when (msgType) {
                    0x80 -> p += 2 // Note Off: note, vel
                    0x90 -> {
                        // Note On: note, vel
                        if (p + 1 < trackEnd) {
                            val vel = copy[p + 1].toInt() and 0xFF
                            if (vel > 0) {
                                val newVel = if (isMelodyEvent) {
                                    if (percent == 0) 0 else {
                                        val normalizedVel = (vel * melodyAutoGain).coerceIn(35f, 127f)
                                        ((normalizedVel * percent) / 100).toInt().coerceIn(1, 127)
                                    }
                                } else if (channel == 9) {
                                    // Drums channel
                                    (vel * profile.drumVelMultiplier).toInt().coerceIn(1, 127)
                                } else if (channel == 1) {
                                    // Bass channel
                                    (vel * profile.bassVelMultiplier).toInt().coerceIn(1, 127)
                                } else {
                                    (vel * profile.accompVelMultiplier).toInt().coerceIn(1, 127)
                                }
                                copy[p + 1] = newVel.toByte()
                            }
                            p += 2
                        } else break
                    }
                    0xA0 -> p += 2 // Polyphonic Key Pressure
                    0xB0 -> {
                        // Control Change: controller, value
                        if (p + 1 < trackEnd) {
                            val ctrl = copy[p].toInt() and 0xFF
                            val value = copy[p + 1].toInt() and 0xFF
                            when (ctrl) {
                                7, 11 -> { // CC 7: Channel Volume, CC 11: Expression
                                    if (isMelodyEvent) {
                                        val baseVol = if (value < 85) 105 else value
                                        copy[p + 1] = if (percent == 0) 0 else ((baseVol * percent) / 100).coerceIn(1, 127).toByte()
                                    } else if (channel == 9 && ctrl == 7) {
                                        copy[p + 1] = (value * profile.drumVelMultiplier).toInt().coerceIn(0, 127).toByte()
                                    } else if (channel == 1 && ctrl == 7) {
                                        copy[p + 1] = (value * profile.bassVelMultiplier).toInt().coerceIn(0, 127).toByte()
                                    }
                                }
                                74 -> { // CC 74: Filter Brightness
                                    copy[p + 1] = profile.brightnessCc74.coerceIn(0, 127).toByte()
                                }
                                91 -> { // CC 91: Reverb Send
                                    copy[p + 1] = profile.reverbCc91.coerceIn(0, 127).toByte()
                                }
                                93 -> { // CC 93: Chorus Send
                                    copy[p + 1] = profile.chorusCc93.coerceIn(0, 127).toByte()
                                }
                            }
                            p += 2
                        } else break
                    }
                    0xC0 -> {
                        // Program Change: remap instruments according to SoundFont profile!
                        if (p < trackEnd) {
                            val origProg = copy[p].toInt() and 0x7F
                            if (channel != 9) { // Never remap GM drum channel 10
                                val newProg = when {
                                    isMelodyEvent && profile.melodyProgram in 0..127 -> profile.melodyProgram
                                    origProg in 0..7 -> profile.pianoProgram
                                    origProg in 24..28 -> profile.guitarProgram
                                    origProg in 32..39 -> profile.bassProgram
                                    origProg in 48..51 -> profile.stringsProgram
                                    else -> origProg
                                }
                                copy[p] = (newProg and 0x7F).toByte()
                            }
                            p += 1
                        } else break
                    }
                    0xD0 -> p += 1 // Channel Pressure
                    0xE0 -> p += 2 // Pitch Bend
                    0xF0 -> {
                        if (status == 0xFF) {
                            if (p >= trackEnd) break
                            p++ // meta type
                            var metaLen = 0
                            while (p < trackEnd) {
                                val b = copy[p++].toInt() and 0xFF
                                metaLen = (metaLen shl 7) or (b and 0x7F)
                                if ((b and 0x80) == 0) break
                            }
                            p += metaLen
                        } else {
                            var sysLen = 0
                            while (p < trackEnd) {
                                val b = copy[p++].toInt() and 0xFF
                                sysLen = (sysLen shl 7) or (b and 0x7F)
                                if ((b and 0x80) == 0) break
                            }
                            p += sysLen
                        }
                    }
                    else -> break
                }
            }

            pos = trackEnd
            trackIndex++
        }

        return copy
    }
}

