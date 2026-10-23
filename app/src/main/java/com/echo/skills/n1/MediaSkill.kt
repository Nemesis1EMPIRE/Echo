// app/src/main/java/com/echo/skills/n1/MediaSkill.kt
package com.echo.skills.n1

import android.content.Context
import android.media.AudioManager
import android.view.KeyEvent
import com.echo.core.MediaCommand
import com.echo.core.SkillResult

class MediaSkill(context: Context) {
    private val audioManager = context.getSystemService(AudioManager::class.java)

    fun execute(command: MediaCommand): SkillResult = runCatching {
        val keyCode = when (command) {
            MediaCommand.PLAY -> KeyEvent.KEYCODE_MEDIA_PLAY
            MediaCommand.PAUSE -> KeyEvent.KEYCODE_MEDIA_PAUSE
            MediaCommand.NEXT -> KeyEvent.KEYCODE_MEDIA_NEXT
            MediaCommand.PREVIOUS -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
        }
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        SkillResult.success(when (command) {
            MediaCommand.PLAY -> "Lecture lancée, chef."
            MediaCommand.PAUSE -> "Lecture en pause, chef."
            MediaCommand.NEXT -> "Piste suivante, chef."
            MediaCommand.PREVIOUS -> "Piste précédente, chef."
        })
    }.getOrElse { SkillResult.failure("Aucun contrôle multimédia ne répond, chef.") }
}