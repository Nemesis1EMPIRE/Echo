package com.echo.core

import org.junit.Assert.assertEquals
import org.junit.Test

class IntentRouterTest {
    private val router = IntentRouter()

    @Test
    fun recognizesLaunchAndAccentedContact() {
        assertEquals(EchoAction.LaunchApp("Spotify"), router.route("Démarre Spotify"))
        assertEquals(EchoAction.Call("Céline Dupont"), router.route("Appelle Céline Dupont"))
    }

    @Test
    fun parsesNumericAndFrenchWordAlarms() {
        assertEquals(EchoAction.SetAlarm(7, 0), router.route("Réveille-moi à 7h"))
        assertEquals(EchoAction.SetAlarm(7, 0), router.route("Alarme à sept heures"))
        assertEquals(EchoAction.SetAlarm(7, 30), router.route("Alarme 7h30"))
    }

    @Test
    fun extractsWeatherCityAndSmsMessage() {
        assertEquals(EchoAction.GetWeather("Lyon"), router.route("Quelle météo à Lyon ?"))
        assertEquals(EchoAction.SendSms("Céline", "Je suis en route"), router.route("Envoie un SMS à Céline disant Je suis en route"))
        assertEquals(EchoAction.OpenUrl("https://example.com"), router.route("Ouvre example.com"))
    }
}