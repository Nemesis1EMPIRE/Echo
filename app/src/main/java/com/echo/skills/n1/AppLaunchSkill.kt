package com.echo.skills.n1

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.echo.core.SkillResult
import java.text.Normalizer

class AppLaunchSkill(private val context: Context) {
    fun execute(name: String): SkillResult {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val activities = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        val target = normalize(name)
        val match = activities.firstOrNull { normalize(it.loadLabel(context.packageManager).toString()) == target }
            ?: activities.firstOrNull { normalize(it.loadLabel(context.packageManager).toString()).contains(target) }
        val launchIntent = match?.activityInfo?.let { context.packageManager.getLaunchIntentForPackage(it.packageName) }
            ?: return SkillResult(false, "Je ne trouve pas l'application $name, chef.")
        return runCatching {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            SkillResult(true, "${match.loadLabel(context.packageManager)} est lancé.")
        }.getOrElse { SkillResult(false, "Impossible de lancer $name, chef.") }
    }

    private fun normalize(value: String) = Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").trim()
}