// app/src/main/java/com/echo/skills/n1/SmsSkill.kt
package com.echo.skills.n1

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.echo.core.SkillResult

class SmsSkill(private val context: Context) {
    suspend fun invoke(contactName: String, message: String): SkillResult {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED
        ) return SkillResult.failure("Autorise l'accès aux contacts et aux SMS avant l'envoi, chef.")
        val number = findPhoneNumber(context, contactName)
            ?: return SkillResult.failure("Je ne trouve pas le numéro de $contactName, chef.")
        return runCatching {
            SmsManager.getDefault().sendTextMessage(number, null, message, null, null)
            SkillResult.success("SMS envoyé à $contactName.")
        }.getOrElse { SkillResult.failure("Le SMS n'a pas pu être envoyé, chef.") }
    }
}