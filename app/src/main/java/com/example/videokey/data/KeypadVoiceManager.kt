package com.example.videokey.data

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Keypad Voice Feedback Manager:
 * Uses Android TextToSpeech to pronounce numbers (0-9) when numeric keys are pressed.
 * Non-number keys (PLAY, RESERVE, STOP, CLR, DEL) produce no voice.
 */
class KeypadVoiceManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    @Volatile private var isInitialized = false

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
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e("KeypadVoiceManager", "Error initializing TextToSpeech", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.setSpeechRate(1.15f) // crisp, quick speech response for fast keypad typing
            tts?.setPitch(1.05f)
            isInitialized = true
            Log.d("KeypadVoiceManager", "TextToSpeech successfully initialized")
        } else {
            Log.w("KeypadVoiceManager", "TextToSpeech init returned status: $status")
        }
    }

    /**
     * Speaks the digit if and only if it is a number ('0'..'9').
     * Other inputs or action buttons are silently ignored.
     */
    fun speakDigit(digit: Char) {
        if (!isInitialized || tts == null) return
        val word = digitWords[digit] ?: return
        try {
            tts?.speak(word, TextToSpeech.QUEUE_FLUSH, null, "digit_$digit")
        } catch (e: Exception) {
            Log.e("KeypadVoiceManager", "Error speaking digit $digit", e)
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {}
        tts = null
        isInitialized = false
    }
}
