package com.echo.core

import java.text.Normalizer
import java.util.Locale

class IntentRouter {
    fun route(input: String): EchoAction {
        val text = input.trim().replace(Regex("\\s+"), " ")
        val normalized = normalize(text)
        if (text.isBlank()) return EchoAction.Chat(text)

        if (Regex("\\b(quelle heure|l'heure|heure est-il|donne.*heure)\\b").containsMatchIn(normalized)) {
            return EchoAction.GetTime
        }
        if (Regex("\\b(quelle date|quel jour|date sommes-nous|on est quel jour)\\b").containsMatchIn(normalized)) {
            return EchoAction.GetDate
        }
        if (Regex("\\b(meteo|temps qu'il fait|temps a)\\b").containsMatchIn(normalized)) {
            val city = Regex("(?:à|a|pour|sur)\\s+([\\p{L}][\\p{L} -]*?)[?.!,]?$", RegexOption.IGNORE_CASE)
                .find(text)?.groupValues?.get(1)?.trim()
            return EchoAction.GetWeather(city)
        }

        val sms = Regex("(?:envoie|envois|envoyer)\\s+(?:un\\s+)?(?:sms|message)\\s+a\\s+(.+?)\\s+(?:disant|pour lui dire|pour leur dire)\\s+(.+)$", RegexOption.IGNORE_CASE)
            .find(normalized)
        if (sms != null) {
            val originalMatch = Regex("(?:envoie|envois|envoyer)\\s+(?:un\\s+)?(?:sms|message)\\s+[àa]\\s+(.+?)\\s+(?:disant|pour lui dire|pour leur dire)\\s+(.+)$", RegexOption.IGNORE_CASE).find(text)
            if (originalMatch != null) return EchoAction.SendSms(originalMatch.groupValues[1].trim(), originalMatch.groupValues[2].trim())
        }

        val call = Regex("^(?:appelle|appel|telephone a|telephoner a|compose le numero de)\\s+(.+)$", RegexOption.IGNORE_CASE).find(normalized)
        if (call != null) {
            val original = Regex("^(?:appelle|appel|t[eé]l[eé]phone [àa]|t[eé]l[eé]phoner [àa]|compose le num[eé]ro de)\\s+(.+)$", RegexOption.IGNORE_CASE).find(text)
            return EchoAction.Call(original?.groupValues?.get(1)?.trim() ?: call.groupValues[1].trim())
        }

        if (Regex("\\b(mets?\\s+le\\s+volume\\s+en\\s+muet|coupe\\s+le\\s+son|mode\\s+silencieux)\\b").containsMatchIn(normalized)) {
            return EchoAction.SetVolume(VolumeLevel.MUTE)
        }
        if (Regex("\\b(monte|augmente)\\s+(?:le\\s+)?volume\\b").containsMatchIn(normalized)) {
            return EchoAction.SetVolume(VolumeLevel.UP)
        }
        if (Regex("\\b(baisse|diminue)\\s+(?:le\\s+)?volume\\b").containsMatchIn(normalized)) {
            return EchoAction.SetVolume(VolumeLevel.DOWN)
        }

        if (Regex("\\b(allume|active)\\s+(?:la\\s+)?(?:lampe|torche|flash)\\b").containsMatchIn(normalized)) return EchoAction.Torch(true)
        if (Regex("\\b(eteins|desactive)\\s+(?:la\\s+)?(?:lampe|torche|flash)\\b").containsMatchIn(normalized)) return EchoAction.Torch(false)

        when {
            Regex("\\b(passe|mets|lance)\\s+(?:a\\s+)?(?:la\\s+)?suivante\\b|\\bnext\\b").containsMatchIn(normalized) -> return EchoAction.MediaControl(MediaCommand.NEXT)
            Regex("\\bprecedente\\b|\\bretourne\\s+a\\s+la\\s+precedente\\b").containsMatchIn(normalized) -> return EchoAction.MediaControl(MediaCommand.PREVIOUS)
            Regex("\\b(pause|mets\\s+en\\s+pause)\\b").containsMatchIn(normalized) -> return EchoAction.MediaControl(MediaCommand.PAUSE)
            Regex("\\b(lis|reprends|reprendre)\\b").containsMatchIn(normalized) -> return EchoAction.MediaControl(MediaCommand.PLAY)
        }

        val alarm = Regex("(?:reveil(?:le-moi)?|alarme)\\s+(?:a\\s+)?([0-9]{1,2})(?:\\s*h(?:eures?)?\\s*([0-9]{1,2})?|:([0-9]{2}))?", RegexOption.IGNORE_CASE).find(normalized)
        if (alarm != null) {
            val parsedHour = alarm.groupValues[1].toIntOrNull()
            val parsedMinute = (alarm.groupValues[2].ifBlank { alarm.groupValues[3] }).toIntOrNull() ?: 0
            if (parsedHour != null && parsedHour in 0..23 && parsedMinute in 0..59) return EchoAction.SetAlarm(parsedHour, parsedMinute)
        }
        val wordAlarm = parseWordAlarm(normalized)
        if (wordAlarm != null) return EchoAction.SetAlarm(wordAlarm.first, wordAlarm.second)

        val url = Regex("(?:ouvre|va sur|visite)\\s+(https?://\\S+|www\\.\\S+|(?:[\\p{L}0-9-]+\\.)+[\\p{L}]{2,}(?:/\\S*)?)", RegexOption.IGNORE_CASE).find(text)
        if (url != null) {
            val address = url.groupValues[1]
            return EchoAction.OpenUrl(if (address.startsWith("http", ignoreCase = true)) address else "https://$address")
        }

        val search = Regex("(?:cherche|recherche|google)\\s+(.+?)(?:\\s+sur\\s+(?:internet|le web))?$", RegexOption.IGNORE_CASE).find(text)
        if (search != null) return EchoAction.WebSearch(search.groupValues[1].trim())

        val launch = Regex("^(?:ouvre|lance|d[eé]marre)\\s+(.+)$", RegexOption.IGNORE_CASE).find(text)
        if (launch != null) return EchoAction.LaunchApp(launch.groupValues[1].trim())

        return EchoAction.Chat(text)
    }

    private fun parseWordAlarm(text: String): Pair<Int, Int>? {
        val match = Regex("(?:reveil(?:le-moi)?|alarme)\\s+(?:a\\s+)?([a-z]+)(?:\\s+heures?(?:\\s+([a-z]+))?)?").find(text) ?: return null
        val hour = frenchNumber(match.groupValues[1]) ?: return null
        val minute = match.groupValues[2].takeIf { it.isNotBlank() }?.let(::frenchNumber) ?: 0
        return if (hour in 0..23 && minute in 0..59) hour to minute else null
    }

    private fun frenchNumber(word: String): Int? = mapOf(
        "zero" to 0, "une" to 1, "un" to 1, "deux" to 2, "trois" to 3, "quatre" to 4,
        "cinq" to 5, "six" to 6, "sept" to 7, "huit" to 8, "neuf" to 9, "dix" to 10,
        "onze" to 11, "douze" to 12, "treize" to 13, "quatorze" to 14, "quinze" to 15,
        "seize" to 16, "vingt" to 20, "trente" to 30, "quarante" to 40, "cinquante" to 50
    )[word]

    private fun normalize(value: String): String = Normalizer.normalize(value.lowercase(Locale.FRENCH), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
}