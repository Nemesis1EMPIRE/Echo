// app/src/main/java/com/echo/skills/n1/WeatherSkill.kt
package com.echo.skills.n1

import android.os.SystemClock
import com.echo.BuildConfig
import com.echo.core.SkillResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale
import java.util.concurrent.TimeUnit

class WeatherSkill(
    private val apiKey: () -> String = { BuildConfig.OPENWEATHER_API_KEY }
) {
    private data class Cached(val at: Long, val result: SkillResult)
    private val cache = mutableMapOf<String, Cached>()
    private val client = OkHttpClient.Builder().callTimeout(12, TimeUnit.SECONDS).build()

    suspend fun execute(city: String?): SkillResult = withContext(Dispatchers.IO) {
        val place = city?.takeIf { it.isNotBlank() } ?: "Paris"
        val key = apiKey().trim()
        if (key.isEmpty()) return@withContext SkillResult.failure("La clé OpenWeather manque dans la configuration, chef.")
        val cacheKey = place.lowercase(Locale.ROOT)
        // Keep successful responses briefly to reduce free-tier API calls.
        synchronized(cache) {
            cache[cacheKey]?.takeIf { SystemClock.elapsedRealtime() - it.at < 10 * 60 * 1000 }?.let { return@withContext it.result }
        }
        val url = "https://api.openweathermap.org/data/2.5/weather".toHttpUrl().newBuilder()
            .addQueryParameter("q", place)
            .addQueryParameter("appid", key)
            .addQueryParameter("units", "metric")
            .addQueryParameter("lang", "fr")
            .build()
        val result = runCatching {
            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code}")
                val root = Json.parseToJsonElement(response.body?.string().orEmpty()).jsonObject
                val description = root["weather"]!!.let { it.jsonArray.first().jsonObject["description"]!!.jsonPrimitive.content }
                val temperature = root["main"]!!.jsonObject["temp"]!!.jsonPrimitive.content.toDouble().toInt()
                SkillResult.success("À $place, $description et $temperature degrés, chef.")
            }
        }.getOrElse { SkillResult.failure("Je n'arrive pas à récupérer la météo pour $place, chef.") }
        if (result.isSuccess) synchronized(cache) { cache[cacheKey] = Cached(SystemClock.elapsedRealtime(), result) }
        result
    }
}