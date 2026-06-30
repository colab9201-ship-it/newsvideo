package com.example.utils

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.io.File
import java.util.Locale

class TtsEngine(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var initCallback: ((Boolean) -> Unit)? = null

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            initCallback?.invoke(true)
            Log.d("TtsEngine", "TTS Initialized successfully")
        } else {
            isInitialized = false
            initCallback?.invoke(false)
            Log.e("TtsEngine", "TTS Initialization failed")
        }
    }

    fun setOnInitCallback(callback: (Boolean) -> Unit) {
        if (isInitialized) {
            callback(true)
        } else {
            initCallback = callback
        }
    }

    /**
     * Synthesizes text to a WAV/MP3 file.
     * @param text The text to read.
     * @param isBangla True for Bangla, false for English.
     * @param outputFile The file destination.
     * @param onComplete Callback when synthesis finishes.
     */
    fun synthesizeToFile(
        text: String,
        isBangla: Boolean,
        outputFile: File,
        onComplete: (Boolean) -> Unit
    ) {
        val ttsEngine = tts
        if (ttsEngine == null || !isInitialized) {
            onComplete(false)
            return
        }

        // Set Language
        val locale = if (isBangla) Locale("bn", "BD") else Locale.US
        val langResult = ttsEngine.setLanguage(locale)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w("TtsEngine", "Language $locale not supported. Falling back to default.")
            ttsEngine.setLanguage(Locale.US)
        }

        // Set progress listener
        val utteranceId = "NEWS_TTS_${System.currentTimeMillis()}"
        ttsEngine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            
            override fun onDone(id: String?) {
                if (id == utteranceId) {
                    onComplete(true)
                }
            }

            override fun onError(id: String?) {
                if (id == utteranceId) {
                    onComplete(false)
                }
            }
        })

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        try {
            // synthesizeToFile compiles perfectly for saving TTS files!
            ttsEngine.synthesizeToFile(text, params, outputFile, utteranceId)
        } catch (e: Exception) {
            Log.e("TtsEngine", "Error during synthesis", e)
            onComplete(false)
        }
    }

    fun speak(text: String, isBangla: Boolean) {
        val ttsEngine = tts ?: return
        val locale = if (isBangla) Locale("bn", "BD") else Locale.US
        ttsEngine.setLanguage(locale)
        ttsEngine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "PREVIEW")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
