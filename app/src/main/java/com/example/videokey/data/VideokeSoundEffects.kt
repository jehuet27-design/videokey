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
 * Generates celebratory brass fanfare chords and crowd cheering/applause
 * when a singer finishes a song and receives their score.
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
                val durationSec = if (score >= 90) 2.5 else 2.0
                val numSamples = (sampleRate * durationSec).toInt()
                val samples = ShortArray(numSamples)
                val rng = Random()

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    var sample = 0.0

                    // Brass Fanfare Chords
                    if (t < 0.22) {
                        val env = min(1.0, t / 0.02) * max(0.0, 1.0 - (t - 0.18) / 0.04)
                        sample += env * (brassNote(t, 392.0) * 0.4 + brassNote(t, 523.25) * 0.5)
                    } else if (t < 0.44) {
                        val localT = t - 0.22
                        val env = min(1.0, localT / 0.02) * max(0.0, 1.0 - (localT - 0.18) / 0.04)
                        sample += env * (brassNote(t, 523.25) * 0.4 + brassNote(t, 659.25) * 0.5)
                    } else {
                        val localT = t - 0.44
                        val env = min(1.0, localT / 0.02) * exp(-localT * 1.5)
                        sample += env * (
                                brassNote(t, 261.63) * 0.3 +
                                brassNote(t, 523.25) * 0.35 +
                                brassNote(t, 659.25) * 0.35 +
                                brassNote(t, 783.99) * 0.3 +
                                brassNote(t, 1046.50) * 0.25
                        )
                    }

                    // Cheering crowd applause noise
                    if (t > 0.35) {
                        val cheerEnv = min(1.0, (t - 0.35) / 0.3) * max(0.0, (durationSec - t) / 1.5)
                        var clapBurst = (rng.nextDouble() - 0.5) * 0.24
                        if (rng.nextDouble() < 0.08) clapBurst *= 2.2
                        sample += clapBurst * cheerEnv
                    }

                    // Drum roll / cymbal crash
                    if (t in 0.44..1.8) {
                        val crashEnv = exp(-(t - 0.44) * 3.5)
                        sample += (rng.nextDouble() - 0.5) * 0.25 * crashEnv
                    }

                    samples[i] = max(-32767.0, min(32767.0, sample * 24000.0)).toInt().toShort()
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
