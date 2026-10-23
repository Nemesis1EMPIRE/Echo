// app/src/main/java/com/echo/skills/n1/AppLaunchSkill.kt
package com.echo.skills.n1

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.echo.core.SkillResult
import java.text.Normalizer

class AppLaunchSkill(private val context: Context) {
    fun invoke(appName: String): SkillResult {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val activities = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        val target = normalize(appName)
        val match = activities.firstOrNull { normalize(it.loadLabel(context.packageManager).toString()) == target }
            ?: activities.firstOrNull { normalize(it.loadLabel(context.packageManager).toString()).contains(target) }
        val launchIntent = match?.activityInfo?.let { context.packageManager.getLaunchIntentForPackage(it.packageName) }
            ?: return SkillResult.failure("Je ne trouve pas l'application $appName, chef.")
        return runCatching {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            SkillResult.success("${match.loadLabel(context.packageManager)} est lancé.")
        }.getOrElse { SkillResult.failure("Impossible de lancer $appName, chef.") }
    }

    private fun normalize(value: String) = Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").trim()
}