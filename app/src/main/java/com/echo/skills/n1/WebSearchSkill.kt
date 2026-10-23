package com.echo.skills.n1

import android.content.Context
import android.content.Intent
import android.provider.SearchManager
import com.echo.core.SkillResult

class WebSearchSkill(private val context: Context) {
    fun execute(query: String): SkillResult = runCatching {
        val intent = Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        SkillResult(true, "Je cherche $query.")
    }.getOrElse { SkillResult(false, "Aucune application de recherche n'est disponible, chef.") }
}