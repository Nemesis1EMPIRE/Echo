// app/src/main/java/com/echo/skills/n1/TimeSkill.kt
package com.echo.skills.n1

import com.echo.core.SkillResult
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class TimeSkill {
    fun invoke(): SkillResult {
        val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH'h'mm", Locale.FRANCE))
        return SkillResult.success("Il est $time, chef.")
    }
}