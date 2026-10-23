package com.echo.skills.n1

import android.content.Context
import android.provider.ContactsContract
import java.text.Normalizer

internal fun findPhoneNumber(context: Context, requestedName: String): String? {
    val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER)
    val cursor = context.contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        projection,
        null,
        null,
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
    ) ?: return null
    cursor.use {
        val nameColumn = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val numberColumn = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val target = normalizeContact(requestedName)
        while (it.moveToNext()) {
            val name = it.getString(nameColumn).orEmpty()
            val candidate = normalizeContact(name)
            if (candidate == target || candidate.contains(target) || target.contains(candidate)) return it.getString(numberColumn)
        }
    }
    return null
}

private fun normalizeContact(value: String) = Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "").trim()