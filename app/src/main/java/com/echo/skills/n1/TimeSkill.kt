package com.echo.skills.n1

import com.echo.core.SkillResult
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class TimeSkill {
    fun execute(): SkillResult {
        val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH'h'mm", Locale.FRANCE))
        return SkillResult(true, "Il est $time, chef.")
    }
}