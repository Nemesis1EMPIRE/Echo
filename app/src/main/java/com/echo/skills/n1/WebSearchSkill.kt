// app/src/main/java/com/echo/skills/n1/WebSearchSkill.kt
package com.echo.skills.n1

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import com.echo.core.SkillResult

class WebSearchSkill(private val context: Context) {

    fun invoke(query: String): SkillResult {
        if (query.isBlank()) {
            return SkillResult.failure("Chercher quoi, chef ?")
        }
        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) == null) {
                return SkillResult.failure("Aucun navigateur pour chercher, chef.")
            }
            context.startActivity(intent)
            SkillResult.success("Je cherche \"$query\".")
        } catch (e: Exception) {
            SkillResult.failure("La recherche a échoué, chef.")
        }
    }
}