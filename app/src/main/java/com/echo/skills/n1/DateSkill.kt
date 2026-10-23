// app/src/main/java/com/echo/skills/n1/DateSkill.kt
package com.echo.skills.n1

import com.echo.core.SkillResult
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class DateSkill {
    fun invoke(): SkillResult {
        val date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRANCE))
        return SkillResult.success("Nous sommes $date, chef.")
    }
}