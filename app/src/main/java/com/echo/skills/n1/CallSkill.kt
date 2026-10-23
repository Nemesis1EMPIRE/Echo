package com.echo.skills.n1

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import com.echo.core.SkillResult

class CallSkill(private val context: Context) {
    fun execute(contactName: String): SkillResult {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return SkillResult(false, "Autorise l'accès aux contacts pour que je trouve ce numéro, chef.")
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return SkillResult(false, "Autorise les appels dans les réglages, chef.")
        }
        val number = findPhoneNumber(context, contactName)
            ?: return SkillResult(false, "Je ne trouve pas le numéro de $contactName, chef.")
        return runCatching {
            context.startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:${Uri.encode(number)}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            SkillResult(true, "J'appelle $contactName.")
        }.getOrElse { SkillResult(false, "L'appel n'a pas pu être lancé, chef.") }
    }
}