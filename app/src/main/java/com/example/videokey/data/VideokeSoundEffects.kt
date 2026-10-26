package com.example.videokey.data

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Authentic Videoke Sound Effects Synthesizer:
 * Generates celebratory brass fanfare chords, crowd cheering/applause,
 * or comedic sad-trombone fail sounds dynamically tailored to the achieved score.
 */
enum class ScoringSoundStyle(
    val id: String,
    val displayNameTagalog: String,
    val displayNameEnglish: String,
    val icon: String
) {
    CLASSIC_ARCADE("classic_arcade", "Klasikong Arcade Brass", "Classic Arcade Brass", "🎺"),
    STADIUM_APPLAUSE("stadium_applause", "Stadium Palakpakan at Hiyawan", "Stadium Applause and Cheers", "👏"),
    PARTY_HORNS("party_horns", "Party Torotot at Pito", "Party Horns and Whistles", "🎉"),
    RETRO_8BIT("retro_8bit", "Retro 8-Bit Chime", "Retro 8-Bit Chime", "🎮"),
    COMEDY_TROMBONE("comedy_fail", "Komedyang Trombone", "Comedy Trombone", "🎭"),
    CUSTOM_AUDIO("custom_audio", "Sariling Audio File (.mp3/.wav)", "Custom Audio File (.mp3/.wav)", "📁")
}

object VideokeSoundEffects {

    private var activeJob: Job? = null
    private var activeTrack: AudioTrack? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    fun playScoringFanfare(score: Int, volumeMultiplier: Float = 1.0f, style: ScoringSoundStyle = ScoringSoundStyle.CLASSIC_ARCADE) {
        if (volumeMultiplier <= 0.01f) return
        stop()
        activeJob = scope.launch {
            try {
                val sampleRate = 22050
                val durationSec = when {
                    score >= 95 -> 2.8
                    score >= 85 -> 2.3
                    score >= 75 -> 2.0
                    else -> 2.5 // Low score sad trombone
                }
                val numSamples = (sampleRate * durationSec).toInt()
                val samples = ShortArray(numSamples)
                val rng = Random()

                when (style) {
                    ScoringSoundStyle.STADIUM_APPLAUSE -> {
                        // High-Energy Stadium Cheering & Continuous Applause
                        for (i in 0 until numSamples) {
                            val t = i.toDouble() / sampleRate
                            val cheerEnv = min(1.0, t / 0.25) * max(0.0, (durationSec - t) / 1.0)
                            val crowdRoar = (rng.nextDouble() - 0.5) * 0.45
                            val clap = if ((i % 800) < 70) (rng.nextDouble() - 0.5) * 0.55 else 0.0
                            val whistle1 = if (t in 0.3..1.5) sin(2.0 * PI * 2400.0 * t + sin(2.0 * PI * 7.0 * t) * 6.0) * 0.12 else 0.0
                            val whistle2 = if (t in 0.8..2.2) sin(2.0 * PI * 2800.0 * t + sin(2.0 * PI * 9.0 * t) * 5.0) * 0.10 else 0.0
                            val sample = (crowdRoar + clap + whistle1 + whistle2) * cheerEnv
                            samples[i] = max(-32767.0, min(32767.0, sample * 25000.0)).toInt().toShort()
                        }
                    }

                    ScoringSoundStyle.PARTY_HORNS -> {
                        // Festive Party Horns, Torotot Glissando & Party Whistle
                        for (i in 0 until numSamples) {
                            val t = i.toDouble() / sampleRate
                            var sample = 0.0
                            if (t < 0.6) {
                                val hornFreq = 220.0 + (t * 280.0)
                                val ph = 2.0 * PI * hornFreq * t
                                val hornWave = 0.5 * sin(ph) + 0.3 * sin(ph * 2) + 0.2 * sin(ph * 3)
                                val env = min(1.0, t / 0.03) * max(0.0, (0.6 - t) / 0.1)
                                sample += hornWave * env * 0.6
                            }
                            if (t in 0.5..1.4) {
                                val localT = t - 0.5
                                val hornFreq = 440.0 + sin(2.0 * PI * 12.0 * localT) * 40.0
                                val ph = 2.0 * PI * hornFreq * localT
                                val hornWave = 0.6 * sin(ph) + 0.25 * sin(ph * 2)
                                val env = min(1.0, localT / 0.03) * max(0.0, (0.9 - localT) / 0.15)
                                sample += hornWave * env * 0.65
                            }
                            if (t in 0.3..durationSec) {
                                val cheerEnv = min(1.0, (t - 0.3) / 0.2) * max(0.0, (durationSec - t) / 0.8)
                                val noise = (rng.nextDouble() - 0.5) * 0.35
                                val clap = if ((i % 1000) < 60) (rng.nextDouble() - 0.5) * 0.40 else 0.0
                                sample += (noise + clap) * cheerEnv
                            }
                            samples[i] = max(-32767.0, min(32767.0, sample * 25000.0)).toInt().toShort()
                        }
                    }

                    ScoringSoundStyle.RETRO_8BIT -> {
                        // 8-Bit Arcade Victory Chime & Rising Arpeggio
                        val notes = doubleArrayOf(261.63, 329.63, 392.0, 523.25, 659.25, 783.99, 1046.50, 1318.51)
                        val noteStep = 0.12
                        for (i in 0 until numSamples) {
                            val t = i.toDouble() / sampleRate
                            val noteIdx = (t / noteStep).toInt().coerceIn(0, notes.size - 1)
                            val noteFreq = notes[noteIdx]
                            val ph = (t * noteFreq) % 1.0
                            val squareWave = if (ph < 0.5) 0.4 else -0.4
                            val env = exp(-(t % noteStep) * 6.0)
                            var sample = squareWave * env
                            if (t > (notes.size * noteStep)) {
                                val tailT = t - (notes.size * noteStep)
                                val tailEnv = exp(-tailT * 1.5)
                                val c1 = if (((t * 1046.5) % 1.0) < 0.5) 0.3 else -0.3
                                val c2 = if (((t * 1318.5) % 1.0) < 0.5) 0.25 else -0.25
                                sample += (c1 + c2) * tailEnv
                            }
                            samples[i] = max(-32767.0, min(32767.0, sample * 22000.0)).toInt().toShort()
                        }
                    }

                    ScoringSoundStyle.COMEDY_TROMBONE -> {
                        // Comedic Fail / Wah-wah-wah Trombone
                        for (i in 0 until numSamples) {
                            val t = i.toDouble() / sampleRate
                            var sample = 0.0
                            if (t in 0.00..0.35) {
                                val env = min(1.0, t / 0.03) * max(0.0, 1.0 - (t - 0.28) / 0.07)
                                sample += env * brassNote(t, 311.13) * 0.75
                            } else if (t in 0.38..0.73) {
                                val localT = t - 0.38
                                val env = min(1.0, localT / 0.03) * max(0.0, 1.0 - (localT - 0.28) / 0.07)
                                sample += env * brassNote(t, 293.66) * 0.75
                            } else if (t in 0.76..1.11) {
                                val localT = t - 0.76
                                val env = min(1.0, localT / 0.03) * max(0.0, 1.0 - (localT - 0.28) / 0.07)
                                sample += env * brassNote(t, 277.18) * 0.75
                            } else if (t in 1.15..2.40) {
                                val localT = t - 1.15
                                val env = min(1.0, localT / 0.04) * exp(-localT * 0.9)
                                val pitchSlide = 261.63 - (localT * 26.0)
                                val vibrato = sin(2.0 * PI * 6.5 * localT) * 7.0
                                sample += env * brassNote(t, max(180.0, pitchSlide + vibrato)) * 0.85
                            }
                            if (t in 1.4..2.4) {
                                val laughEnv = min(1.0, (t - 1.4) / 0.3) * max(0.0, (2.4 - t) / 0.7)
                                val chuckles = (sin(2.0 * PI * 180.0 * t) * 0.5 + sin(2.0 * PI * 220.0 * t) * 0.5) *
                                        (if (sin(2.0 * PI * 4.5 * t) > 0.0) 1.0 else 0.0)
                                sample += chuckles * 0.18 * laughEnv
                            }
                            samples[i] = max(-32767.0, min(32767.0, sample * 24000.0)).toInt().toShort()
                        }
                    }

                    else -> {
                when {
                    score >= 95 -> {
                        // TIER 1: Grand Superstar / Perfect Score Fanfare (95 - 100)
                        for (i in 0 until numSamples) {
                            val t = i.toDouble() / sampleRate
                            var sample = 0.0

                            if (t < 0.18) {
                                val env = min(1.0, t / 0.015) * max(0.0, 1.0 - (t - 0.15) / 0.03)
                                sample += env * (brassNote(t, 392.0) * 0.4 + brassNote(t, 523.25) * 0.5)
                            } else if (t < 0.36) {
                                val localT = t - 0.18
                                val env = min(1.0, localT / 0.015) * max(0.0, 1.0 - (localT - 0.15) / 0.03)
                                sample += env * (brassNote(t, 523.25) * 0.4 + brassNote(t, 659.25) * 0.5)
                            } else if (t < 0.54) {
                                val localT = t - 0.36
                                val env = min(1.0, localT / 0.015) * max(0.0, 1.0 - (localT - 0.15) / 0.03)
                                sample += env * (brassNote(t, 659.25) * 0.4 + brassNote(t, 783.99) * 0.55)
                            } else {
                                val localT = t - 0.54
                                val env = min(1.0, localT / 0.02) * exp(-localT * 1.2)
                                sample += env * (
                                    brassNote(t, 261.63) * 0.25 +
                                    brassNote(t, 523.25) * 0.35 +
                                    brassNote(t, 659.25) * 0.35 +
                                    brassNote(t, 783.99) * 0.35 +
                                    brassNote(t, 1046.50) * 0.40
                                )
                            }

                            // Roaring applause & cheering crowd
                            if (t > 0.45) {
                                val cheerEnv = min(1.0, (t - 0.45) / 0.3) * max(0.0, (durationSec - t) / 1.5)
                                var clapBurst = (rng.nextDouble() - 0.5) * 0.28
                                if (rng.nextDouble() < 0.12) clapBurst *= 2.4
                                sample += clapBurst * cheerEnv
                            }

                            // Cymbal crash
                            if (t in 0.54..2.0) {
                                val crashEnv = exp(-(t - 0.54) * 3.0)
                                sample += (rng.nextDouble() - 0.5) * 0.30 * crashEnv
                            }

                            samples[i] = max(-32767.0, min(32767.0, sample * 25000.0)).toInt().toShort()
                        }
                    }

                    score >= 85 -> {
                        // TIER 2: Great Victory Fanfare (85 - 94)
                        for (i in 0 until numSamples) {
                            val t = i.toDouble() / sampleRate
                            var sample = 0.0

                            if (t < 0.20) {
                                val env = min(1.0, t / 0.02) * max(0.0, 1.0 - (t - 0.16) / 0.04)
                                sample += env * (brassNote(t, 392.0) * 0.45 + brassNote(t, 587.33) * 0.45)
                            } else if (t < 0.40) {
                                val localT = t - 0.20
                                val env = min(1.0, localT / 0.02) * max(0.0, 1.0 - (localT - 0.16) / 0.04)
                                sample += env * (brassNote(t, 493.88) * 0.45 + brassNote(t, 783.99) * 0.45)
                            } else {
                                val localT = t - 0.40
                                val env = min(1.0, localT / 0.02) * exp(-localT * 1.5)
                                sample += env * (
                                    brassNote(t, 523.25) * 0.35 +
                                    brassNote(t, 659.25) * 0.35 +
                                    brassNote(t, 783.99) * 0.35
                                )
                            }

                            // Cheerful applause
                            if (t > 0.35) {
                                val cheerEnv = min(1.0, (t - 0.35) / 0.3) * max(0.0, (durationSec - t) / 1.4)
                                var clapBurst = (rng.nextDouble() - 0.5) * 0.22
                                if (rng.nextDouble() < 0.08) clapBurst *= 2.0
                                sample += clapBurst * cheerEnv
                            }

                            // Cymbal accent
                            if (t in 0.40..1.6) {
                                val crashEnv = exp(-(t - 0.40) * 4.0)
                                sample += (rng.nextDouble() - 0.5) * 0.22 * crashEnv
                            }

                            samples[i] = max(-32767.0, min(32767.0, sample * 24000.0)).toInt().toShort()
                        }
                    }

                    score >= 75 -> {
                        // TIER 3: Average / Encouraging Chime & Mild Clapping (75 - 84)
                        for (i in 0 until numSamples) {
                            val t = i.toDouble() / sampleRate
                            var sample = 0.0

                            if (t < 0.22) {
                                val env = min(1.0, t / 0.02) * max(0.0, 1.0 - (t - 0.18) / 0.04)
                                sample += env * (brassNote(t, 329.63) * 0.4 + brassNote(t, 392.00) * 0.4)
                            } else if (t < 0.44) {
                                val localT = t - 0.22
                                val env = min(1.0, localT / 0.02) * max(0.0, 1.0 - (localT - 0.18) / 0.04)
                                sample += env * (brassNote(t, 440.00) * 0.4 + brassNote(t, 523.25) * 0.4)
                            } else {
                                val localT = t - 0.44
                                val env = min(1.0, localT / 0.02) * exp(-localT * 1.8)
                                sample += env * (
                                    brassNote(t, 392.00) * 0.35 +
                                    brassNote(t, 493.88) * 0.35 +
                                    brassNote(t, 587.33) * 0.30
                                )
                            }

                            // Mild polite clapping
                            if (t > 0.40) {
                                val cheerEnv = min(1.0, (t - 0.40) / 0.3) * max(0.0, (durationSec - t) / 1.3)
                                val clapBurst = (rng.nextDouble() - 0.5) * 0.16
                                sample += clapBurst * cheerEnv
                            }

                            samples[i] = max(-32767.0, min(32767.0, sample * 22000.0)).toInt().toShort()
                        }
                    }

                    else -> {
                        // TIER 4: Low Score / Comedic Fail ("Wah-wah-wah-waaaah" Sad Trombone) (< 75)
                        for (i in 0 until numSamples) {
                            val t = i.toDouble() / sampleRate
                            var sample = 0.0

                            // Note 1: Eb4 (311.13 Hz) 0.00s - 0.35s
                            if (t in 0.00..0.35) {
                                val env = min(1.0, t / 0.03) * max(0.0, 1.0 - (t - 0.28) / 0.07)
                                sample += env * brassNote(t, 311.13) * 0.75
                            }
                            // Note 2: D4 (293.66 Hz) 0.38s - 0.73s
                            else if (t in 0.38..0.73) {
                                val localT = t - 0.38
                                val env = min(1.0, localT / 0.03) * max(0.0, 1.0 - (localT - 0.28) / 0.07)
                                sample += env * brassNote(t, 293.66) * 0.75
                            }
                            // Note 3: C#4 (277.18 Hz) 0.76s - 1.11s
                            else if (t in 0.76..1.11) {
                                val localT = t - 0.76
                                val env = min(1.0, localT / 0.03) * max(0.0, 1.0 - (localT - 0.28) / 0.07)
                                sample += env * brassNote(t, 277.18) * 0.75
                            }
                            // Note 4: C4 (261.63 Hz) sliding down to B3 with comic vibrato (1.15s - 2.40s)
                            else if (t in 1.15..2.40) {
                                val localT = t - 1.15
                                val env = min(1.0, localT / 0.04) * exp(-localT * 1.3)
                                val pitchDrop = 18.0 * min(1.0, localT / 0.9)
                                val vibrato = if (localT > 0.2) sin(localT * 26.0) * 3.5 else 0.0
                                val freq = 261.63 - pitchDrop + vibrato
                                sample += env * brassNote(t, freq) * 0.85

                                // Comedic boing spring effect at 1.4s
                                if (t in 1.40..1.90) {
                                    val bT = t - 1.40
                                    val boingFreq = 160.0 + sin(bT * 40.0) * 80.0 * exp(-bT * 5.0)
                                    val boingEnv = exp(-bT * 6.0)
                                    sample += boingEnv * sin(2.0 * PI * boingFreq * t) * 0.35
                                }
                            }

                            samples[i] = max(-32767.0, min(32767.0, sample * 24000.0)).toInt().toShort()
                        }
                    }
                }

                    }
                }

                val bufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(samples.size * 2)

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                activeTrack = audioTrack
                audioTrack.setVolume(volumeMultiplier.coerceIn(0f, 1f))
                audioTrack.play()
                audioTrack.write(samples, 0, samples.size)

                // Wait for playback to finish
                val playMillis = (durationSec * 1000).toLong()
                kotlinx.coroutines.delay(playMillis)
                audioTrack.stop()
                audioTrack.release()
                activeTrack = null
            } catch (e: Exception) {
                Log.w("VideokeSoundEffects", "Could not play scoring fanfare: ${e.message}")
            }
        }
    }

    private fun brassNote(t: Double, freq: Double): Double {
        val ph = 2.0 * PI * freq * t
        return 0.55 * sin(ph) + 0.25 * sin(ph * 2.0) + 0.15 * sin(ph * 3.0) + 0.05 * sin(ph * 4.0)
    }

    fun stop() {
        try {
            activeJob?.cancel()
            activeJob = null
            activeTrack?.let {
                it.pause()
                it.flush()
                it.stop()
                it.release()
            }
            activeTrack = null
        } catch (e: Exception) {
            // Ignore clean teardown exceptions
        }
    }
}
