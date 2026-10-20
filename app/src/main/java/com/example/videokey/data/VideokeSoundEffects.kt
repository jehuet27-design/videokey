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
object VideokeSoundEffects {

    private var activeJob: Job? = null
    private var activeTrack: AudioTrack? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    fun playScoringFanfare(score: Int, volumeMultiplier: Float = 1.0f) {
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
