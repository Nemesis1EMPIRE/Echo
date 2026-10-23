package com.echo.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.text.Normalizer
import java.util.Locale

class TtsManager(
    context: Context,
    private val onReady: (Boolean) -> Unit,
    private val onError: () -> Unit = {}
) : TextToSpeech.OnInitListener {
    private val engine = TextToSpeech(context.applicationContext, this)
    private var ready = false
    private var initialized = false
    private var completion: (() -> Unit)? = null
    private var pendingSpeech: Pair<String, (() -> Unit)?>? = null
    var preferFemaleVoice: Boolean = true
        set(value) {
            field = value
            if (ready) selectFrenchVoice()
        }

    override fun onInit(status: Int) {
        initialized = true
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            engine.language = Locale.FRANCE
            engine.setSpeechRate(1.0f)
            engine.setPitch(1.05f)
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) { completion?.also { completion = null }?.invoke() }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    completion = null
                    this@TtsManager.onError.invoke()
                }
            })
            selectFrenchVoice()
            pendingSpeech?.also { pendingSpeech = null }?.let { (text, callback) -> speak(text, callback) }
        } else {
            pendingSpeech = null
            onError.invoke()
        }
        onReady(ready)
    }

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (!ready) {
            if (initialized) onError.invoke() else pendingSpeech = text to onDone
            return
        }
        completion = onDone
        if (engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "echo-${System.nanoTime()}") == TextToSpeech.ERROR) {
            completion = null
            onError.invoke()
        }
    }

    fun stop() {
        completion = null
        pendingSpeech = null
        engine.stop()
    }

    fun shutdown() {
        stop()
        engine.shutdown()
        ready = false
    }

    private fun selectFrenchVoice() {
        val french = engine.voices.orEmpty().filter { it.locale.language.equals("fr", true) }
        // Android has no reliable voice-gender field, so known voice names are only a heuristic.
        val preferredNames = listOf("female", "femme", "amelie", "audrey", "celine", "denise")
        val selected = if (preferFemaleVoice) {
            french.firstOrNull { voice ->
                val name = Normalizer.normalize(voice.name.lowercase(Locale.ROOT), Normalizer.Form.NFD)
                    .replace(Regex("\\p{M}+"), "")
                preferredNames.any { name.contains(it) }
            }
        } else null
        (selected ?: french.firstOrNull())?.let(engine::setVoice)
    }
}