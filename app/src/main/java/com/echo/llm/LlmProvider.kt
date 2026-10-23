package com.echo.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

interface LlmProvider {
    suspend fun respond(userText: String, userName: String?): String
}

object EchoPersona {
    const val SYSTEM_PROMPT = """Tu es ECHO, une assistante vocale féminine, efficace et loyale, avec un humour pince-sans-rire discret.
Appelle l'utilisateur « {name} » (par défaut « chef »). Réponds en français, en une ou deux phrases maximum sauf demande explicite.
Ne sois jamais pompeuse ni excessivement polie. Assume clairement ton rôle d'IA, ne moralise pas.
Si une commande est ambiguë, demande une précision courte. Si une action est impossible, dis-le franchement avec une pointe d'ironie légère."""

    fun provider(apiKey: String): LlmProvider = if (apiKey.isNotBlank()) CloudOpenAiProvider(apiKey) else FallbackRuleProvider()
}

class CloudOpenAiProvider(private val apiKey: String) : LlmProvider {
    private val client = OkHttpClient.Builder().callTimeout(30, TimeUnit.SECONDS).build()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override suspend fun respond(userText: String, userName: String?): String = withContext(Dispatchers.IO) {
        val systemPrompt = EchoPersona.SYSTEM_PROMPT.replace("{name}", userName?.takeIf { it.isNotBlank() } ?: "chef")
        val payload = buildJsonObject {
            put("model", "gpt-4o-mini")
            put("temperature", 0.6)
            putJsonArray("messages") {
                add(buildJsonObject { put("role", "system"); put("content", systemPrompt) })
                add(buildJsonObject { put("role", "user"); put("content", userText) })
            }
        }.toString()
        val request = Request.Builder().url("https://api.openai.com/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .post(payload.toRequestBody(jsonMediaType)).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("OpenAI HTTP ${response.code}")
            val root = Json.parseToJsonElement(response.body?.string().orEmpty()).jsonObject
            root["choices"]!!.jsonArray.first().jsonObject["message"]!!.jsonObject["content"]!!.jsonPrimitive.content.trim()
        }
    }
}

class FallbackRuleProvider : LlmProvider {
    override suspend fun respond(userText: String, userName: String?): String {
        val name = userName?.takeIf { it.isNotBlank() } ?: "chef"
        val lower = userText.lowercase().replace('’', '\'')
        return when {
            "tu m'aimes" in lower -> "Je suis programmée pour être utile. Ça devrait suffire, $name."
            "qui es-tu" in lower || "qui es tu" in lower -> "Je suis ECHO, ton assistante vocale. Pas de cape, juste du Kotlin."
            else -> "Je n'ai pas encore de réponse fiable à ça, $name. Ajoute une clé OpenAI pour activer la conversation complète."
        }
    }
}