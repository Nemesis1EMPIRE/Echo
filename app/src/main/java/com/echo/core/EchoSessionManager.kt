package com.echo.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.echo.data.EchoSettingsRepository
import com.echo.llm.EchoPersona
import com.echo.llm.FallbackRuleProvider
import com.echo.skills.n1.AlarmSkill
import com.echo.skills.n1.AppLaunchSkill
import com.echo.skills.n1.CallSkill
import com.echo.skills.n1.DateSkill
import com.echo.skills.n1.DeviceSkill
import com.echo.skills.n1.MediaSkill
import com.echo.skills.n1.SmsSkill
import com.echo.skills.n1.TimeSkill
import com.echo.skills.n1.UrlSkill
import com.echo.skills.n1.WeatherSkill
import com.echo.skills.n1.WebSearchSkill
import com.echo.voice.SttManager
import com.echo.voice.TtsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class EchoState { IDLE, LISTENING, THINKING, SPEAKING }

class EchoSessionManager private constructor(context: Context) : SttManager.Listener {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val repository = EchoSettingsRepository(appContext)
    private val router = IntentRouter()
    private val timeSkill = TimeSkill()
    private val dateSkill = DateSkill()
    private val appSkill = AppLaunchSkill(appContext)
    private val callSkill = CallSkill(appContext)
    private val smsSkill = SmsSkill(appContext)
    private val alarmSkill = AlarmSkill(appContext)
    private val searchSkill = WebSearchSkill(appContext)
    private val urlSkill = UrlSkill(appContext)
    private val deviceSkill = DeviceSkill(appContext)
    private val mediaSkill = MediaSkill(appContext)
    private val weatherSkill = WeatherSkill()
    private val stt = SttManager(appContext, this)
    private val _state = MutableStateFlow(EchoState.IDLE)
    val state: StateFlow<EchoState> = _state.asStateFlow()
    private val _lastResponse = MutableStateFlow("")
    val lastResponse: StateFlow<String> = _lastResponse.asStateFlow()
    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript.asStateFlow()
    private val _permissionRequest = MutableStateFlow<List<String>>(emptyList())
    val permissionRequest: StateFlow<List<String>> = _permissionRequest.asStateFlow()
    private var pendingAction: EchoAction? = null
    private val tts = TtsManager(appContext, onReady = { ready ->
        if (!ready) {
            _lastResponse.value = "La synthèse vocale est indisponible sur cet appareil, chef."
            _state.value = EchoState.IDLE
        }
    }, onError = {
        _lastResponse.value = "Je n'arrive pas à parler avec le moteur vocal installé, chef."
        _state.value = EchoState.IDLE
    })

    init {
        scope.launch { tts.preferFemaleVoice = repository.settings.first().femaleVoice }
    }

    fun listen() {
        if (!hasPermission(Manifest.permission.RECORD_AUDIO)) {
            _permissionRequest.value = listOf(Manifest.permission.RECORD_AUDIO)
            return
        }
        tts.stop()
        _transcript.value = ""
        _state.value = EchoState.LISTENING
        stt.startListening()
    }

    fun onPermissionsResult(granted: Boolean) {
        val action = pendingAction
        pendingAction = null
        _permissionRequest.value = emptyList()
        if (!granted) {
            speak("Cette autorisation est nécessaire pour exécuter la commande, chef.")
        } else if (action == null) {
            listen()
        } else {
            runAction(action)
        }
    }

    override fun onPartial(text: String) { _transcript.value = text }

    override fun onResult(text: String) {
        _transcript.value = text
        _state.value = EchoState.THINKING
        val action = router.route(text)
        // Request sensitive capabilities only when a command actually needs them.
        val missing = permissionsFor(action).filterNot(::hasPermission)
        if (missing.isNotEmpty()) {
            pendingAction = action
            _permissionRequest.value = missing
            _state.value = EchoState.IDLE
        } else runAction(action)
    }

    override fun onError(message: String) = speak(message)

    fun updateFemaleVoice(enabled: Boolean) { tts.preferFemaleVoice = enabled }

    private fun runAction(action: EchoAction) {
        scope.launch {
            _state.value = EchoState.THINKING
            val settings = repository.settings.first()
            val result = when (action) {
                EchoAction.GetTime -> timeSkill.execute()
                EchoAction.GetDate -> dateSkill.execute()
                is EchoAction.LaunchApp -> appSkill.execute(action.name)
                is EchoAction.Call -> callSkill.execute(action.contactName)
                is EchoAction.SendSms -> smsSkill.execute(action.contactName, action.message)
                is EchoAction.SetAlarm -> alarmSkill.execute(action.hour, action.minute)
                is EchoAction.WebSearch -> searchSkill.execute(action.query)
                is EchoAction.OpenUrl -> urlSkill.execute(action.url)
                is EchoAction.SetVolume -> deviceSkill.setVolume(action.level)
                is EchoAction.Torch -> deviceSkill.setTorch(action.enabled)
                is EchoAction.MediaControl -> mediaSkill.execute(action.command)
                is EchoAction.GetWeather -> weatherSkill.execute(action.city)
                is EchoAction.Chat -> {
                    val provider = EchoPersona.provider(settings.llmApiKey.ifBlank { com.echo.BuildConfig.OPENAI_API_KEY })
                    SkillResult(true, runCatching { provider.respond(action.freeText, settings.userName) }
                        .getOrElse { FallbackRuleProvider().respond(action.freeText, settings.userName) })
                }
            }
            val name = settings.userName.takeIf { it.isNotBlank() }
            speak(if (name == null) result.spokenReply else result.spokenReply.replace("chef", name, ignoreCase = true))
        }
    }

    private fun speak(text: String) {
        _lastResponse.value = text
        _state.value = EchoState.SPEAKING
        tts.speak(text) { _state.value = EchoState.IDLE }
    }

    private fun permissionsFor(action: EchoAction): List<String> = when (action) {
        is EchoAction.Call -> listOf(Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE)
        is EchoAction.SendSms -> listOf(Manifest.permission.READ_CONTACTS, Manifest.permission.SEND_SMS)
        is EchoAction.Torch -> listOf(Manifest.permission.CAMERA)
        else -> emptyList()
    }

    private fun hasPermission(permission: String) = ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED

    companion object {
        @Volatile private var instance: EchoSessionManager? = null
        fun get(context: Context): EchoSessionManager = instance ?: synchronized(this) {
            instance ?: EchoSessionManager(context).also { instance = it }
        }
    }
}