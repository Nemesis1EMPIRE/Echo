package com.echo.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class SttManager(context: Context, private val listener: Listener) {
    interface Listener {
        fun onResult(text: String)
        fun onPartial(text: String)
        fun onError(message: String)
    }

    private val appContext = context.applicationContext
    private var recognizer: SpeechRecognizer? = null

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            listener.onError("La reconnaissance vocale n'est pas disponible sur cet appareil.")
            return
        }
        cancel()
        recognizer = SpeechRecognizer.createSpeechRecognizer(appContext).also { speechRecognizer ->
            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit

                override fun onError(error: Int) {
                    listener.onError(if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        "Je n'ai pas compris, chef."
                    } else {
                        "La reconnaissance vocale a échoué, chef."
                    })
                    cancel()
                }

                override fun onResults(results: Bundle?) {
                    val phrase = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (phrase.isNullOrBlank()) listener.onError("Je n'ai pas compris, chef.") else listener.onResult(phrase)
                    cancel()
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(listener::onPartial)
                }
            })
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            runCatching { speechRecognizer.startListening(intent) }
                .onFailure { listener.onError("Je ne peux pas démarrer l'écoute, chef."); cancel() }
        }
    }

    fun cancel() {
        recognizer?.runCatching { cancel(); destroy() }
        recognizer = null
    }

    fun destroy() = cancel()
}