package com.example.videokey.data

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Keypad Voice Feedback Manager:
 * Uses SoundPool with pre-cached audio samples for 0-9 numeric keys to guarantee
 * 0ms instant, lag-free pronunciation when typing on the keypad.
 * Non-number keys produce no voice.
 */
class KeypadVoiceManager(context: Context) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    @Volatile private var isTtsInitialized = false

    private val soundPool: SoundPool
    private val soundMap = ConcurrentHashMap<Char, Int>()
    private val readySoundIds = CopyOnWriteArraySet<Int>()

    private val digitWords = mapOf(
        '0' to "Zero",
        '1' to "One",
        '2' to "Two",
        '3' to "Three",
        '4' to "Four",
        '5' to "Five",
        '6' to "Six",
        '7' to "Seven",
        '8' to "Eight",
        '9' to "Nine"
    )

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                readySoundIds.add(sampleId)
                Log.d("KeypadVoiceManager", "Sound sample $sampleId ready in SoundPool")
            }
        }

        // Preload any already synthesized digit wav files from cache
        try {
            for (ch in '0'..'9') {
                val file = File(appContext.cacheDir, "digit_$ch.wav")
                if (file.exists() && file.length() > 200L) {
                    val soundId = soundPool.load(file.absolutePath, 1)
                    soundMap[ch] = soundId
                }
            }
        } catch (e: Exception) {
            Log.w("KeypadVoiceManager", "Error preloading existing digit wavs: ${e.message}")
        }

        // Initialize Android TTS to synthesize any missing digit files
        try {
            tts = TextToSpeech(appContext, this)
        } catch (e: Exception) {
            Log.e("KeypadVoiceManager", "Error initializing TextToSpeech", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val engine = tts ?: return
            engine.language = Locale.US
            engine.setSpeechRate(1.2f)
            engine.setPitch(1.05f)
            isTtsInitialized = true

            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    if (utteranceId != null && utteranceId.startsWith("digit_synth_")) {
                        val ch = utteranceId.substringAfter("digit_synth_").firstOrNull() ?: return
                        val file = File(appContext.cacheDir, "digit_$ch.wav")
                        if (file.exists() && file.length() > 200L) {
                            val soundId = soundPool.load(file.absolutePath, 1)
                            soundMap[ch] = soundId
                        }
                    }
                }
                override fun onError(utteranceId: String?) {
                    Log.w("KeypadVoiceManager", "Error synthesizing utterance $utteranceId")
                }
            })

            // Synthesize all digits 0-9 into cache files for SoundPool zero-delay playback
            for (ch in '0'..'9') {
                val file = File(appContext.cacheDir, "digit_$ch.wav")
                if (!file.exists() || file.length() < 200L) {
                    val word = digitWords[ch] ?: continue
                    val params = Bundle()
                    engine.synthesizeToFile(word, params, file, "digit_synth_$ch")
                }
            }
            Log.d("KeypadVoiceManager", "TextToSpeech initialized & digit pre-caching scheduled")
        } else {
            Log.w("KeypadVoiceManager", "TextToSpeech init returned status: $status")
        }
    }

    /**
     * Speaks the digit with 0ms instantaneous SoundPool playback if cached,
     * or instant TTS fallback.
     */
    fun speakDigit(digit: Char) {
        val soundId = soundMap[digit]
        if (soundId != null && readySoundIds.contains(soundId)) {
            // Instant 0ms latency playback via SoundPool
            soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
            return
        }

        // Fallback to active TTS engine
        if (isTtsInitialized && tts != null) {
            val word = digitWords[digit] ?: return
            try {
                tts?.speak(word, TextToSpeech.QUEUE_FLUSH, null, "digit_$digit")
            } catch (e: Exception) {
                Log.e("KeypadVoiceManager", "Error speaking digit $digit", e)
            }
        }
    }

    fun shutdown() {
        try {
            soundPool.release()
        } catch (e: Exception) {}
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {}
        tts = null
        isTtsInitialized = false
        soundMap.clear()
        readySoundIds.clear()
    }
}
