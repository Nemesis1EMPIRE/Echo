// app/src/main/java/com/echo/skills/n1/DeviceSkill.kt
package com.echo.skills.n1

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import com.echo.core.SkillResult
import com.echo.core.VolumeLevel

class DeviceSkill(context: Context) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val cameraManager = context.getSystemService(CameraManager::class.java)

    fun setVolume(level: VolumeLevel): SkillResult = runCatching {
        val stream = AudioManager.STREAM_MUSIC
        val current = audioManager.getStreamVolume(stream)
        when (level) {
            VolumeLevel.UP -> audioManager.setStreamVolume(stream, (current + 1).coerceAtMost(audioManager.getStreamMaxVolume(stream)), 0)
            VolumeLevel.DOWN -> audioManager.setStreamVolume(stream, (current - 1).coerceAtLeast(0), 0)
            VolumeLevel.MUTE -> audioManager.setStreamVolume(stream, 0, 0)
        }
        SkillResult.success(when (level) {
            VolumeLevel.UP -> "Volume augmenté, chef."
            VolumeLevel.DOWN -> "Volume baissé, chef."
            VolumeLevel.MUTE -> "Silence radio, chef."
        })
    }.getOrElse { SkillResult.failure("Je ne peux pas régler le volume, chef.") }

    fun setTorch(enabled: Boolean): SkillResult = runCatching {
        val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return@runCatching SkillResult.failure("Aucun flash disponible sur cet appareil, chef.")
        cameraManager.setTorchMode(cameraId, enabled)
        SkillResult.success(if (enabled) "Torche allumée, chef." else "Torche éteinte, chef.")
    }.getOrElse { SkillResult.failure("Je ne peux pas contrôler la torche. Vérifie l'accès à la caméra, chef.") }
}