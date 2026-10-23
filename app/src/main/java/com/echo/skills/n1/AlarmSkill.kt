// app/src/main/java/com/echo/skills/n1/AlarmSkill.kt
package com.echo.skills.n1

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import com.echo.core.SkillResult

class AlarmSkill(private val context: Context) {
    fun invoke(hour: Int, minute: Int): SkillResult = runCatching {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, "ECHO")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (intent.resolveActivity(context.packageManager) == null) return@runCatching SkillResult.failure("Aucune application d'alarme disponible, chef.")
        context.startActivity(intent)
        SkillResult.success("Alarme réglée à %02d:%02d, chef.".format(hour, minute))
    }.getOrElse { SkillResult.failure("Je n'ai pas pu régler l'alarme, chef.") }
}