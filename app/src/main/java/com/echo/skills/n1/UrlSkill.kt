package com.echo.skills.n1

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.echo.core.SkillResult

class UrlSkill(private val context: Context) {
    fun execute(url: String): SkillResult = runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        SkillResult(true, "J'ouvre le site, chef.")
    }.getOrElse { SkillResult(false, "Impossible d'ouvrir cette adresse, chef.") }
}