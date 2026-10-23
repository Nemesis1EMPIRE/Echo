package com.echo.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.echoDataStore by preferencesDataStore(name = "echo_settings")

data class EchoSettings(
    val userName: String = "",
    val llmApiKey: String = "",
    val femaleVoice: Boolean = true
)

class EchoSettingsRepository(private val context: Context) {
    private object Keys {
        val userName = stringPreferencesKey("user_name")
        val apiKey = stringPreferencesKey("llm_api_key")
        val femaleVoice = booleanPreferencesKey("female_voice")
    }

    val settings: Flow<EchoSettings> = context.echoDataStore.data.map { values ->
        EchoSettings(
            userName = values[Keys.userName].orEmpty(),
            llmApiKey = values[Keys.apiKey].orEmpty(),
            femaleVoice = values[Keys.femaleVoice] ?: true
        )
    }

    suspend fun update(userName: String, apiKey: String, femaleVoice: Boolean) {
        context.echoDataStore.edit { values ->
            values[Keys.userName] = userName
            values[Keys.apiKey] = apiKey
            values[Keys.femaleVoice] = femaleVoice
        }
    }
}