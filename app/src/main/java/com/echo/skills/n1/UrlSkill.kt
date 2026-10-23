// app/src/main/java/com/echo/skills/n1/UrlSkill.kt
package com.echo.skills.n1

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.echo.core.SkillResult

class UrlSkill(private val context: Context) {
    fun invoke(url: String): SkillResult = runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        SkillResult.success("J'ouvre le site, chef.")
    }.getOrElse { SkillResult.failure("Impossible d'ouvrir cette adresse, chef.") }
}