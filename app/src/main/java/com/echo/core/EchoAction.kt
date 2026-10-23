package com.echo.core

sealed interface EchoAction {
    data object GetTime : EchoAction
    data object GetDate : EchoAction
    data class LaunchApp(val name: String) : EchoAction
    data class Call(val contactName: String) : EchoAction
    data class SendSms(val contactName: String, val message: String) : EchoAction
    data class SetAlarm(val hour: Int, val minute: Int) : EchoAction
    data class WebSearch(val query: String) : EchoAction
    data class OpenUrl(val url: String) : EchoAction
    data class SetVolume(val level: VolumeLevel) : EchoAction
    data class Torch(val enabled: Boolean) : EchoAction
    data class MediaControl(val command: MediaCommand) : EchoAction
    data class GetWeather(val city: String?) : EchoAction
    data class Chat(val freeText: String) : EchoAction
}

enum class VolumeLevel { UP, DOWN, MUTE }
enum class MediaCommand { PLAY, PAUSE, NEXT, PREVIOUS }

data class SkillResult(val success: Boolean, val spokenReply: String)