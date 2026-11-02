package fr.julesdupont.portail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class LogShareTest {
    @Test
    fun `numero masque sauf les 2 premiers et 2 derniers chiffres`() {
        assertEquals("02••••••29", LogShare.maskPhone("0253591329"))
        assertEquals("+33 • •• •• •• 44", LogShare.maskPhone("+33 6 11 22 33 44"))
    }

    @Test
    fun `numeros masques dans le journal, heures et distances intactes`() {
        val log = "02/11 10:11:05  Appel manuel lancé vers 0253591329\n02/11 08:20:25  Arrivée détectée par GPS (35 m, ±8 m)"
        assertEquals(
            "02/11 10:11:05  Appel manuel lancé vers 02••••••29\n02/11 08:20:25  Arrivée détectée par GPS (35 m, ±8 m)",
            LogShare.maskPhones(log),
        )
    }

    private val info = LogShare.Info(
        versionName = "1.16", testBuild = true, device = "Samsung SM-S921B", androidVersion = "15",
        now = LocalDateTime.of(2026, 11, 2, 9, 15), statusTitle = "Actif", enabled = true,
        days = setOf(1, 2, 3, 4, 5), startMinutes = 420, endMinutes = 600, skipHolidays = true,
        hasLocation = true, radius = 250, twoStage = true, approachRadius = 1000,
        countdownSeconds = 5, hangupSeconds = 20, retryIfBusy = false, missingPermissions = emptyList(),
        calls = listOf(LocalDateTime.of(2026, 10, 30, 8, 14)),
        log = "02/11 08:20:31  Appel automatique lancé vers 0611223344\n",
    )

    @Test
    fun `texte partage avec le contexte puis le journal`() {
        val text = LogShare.text(info)
        val lines = text.lines()
        assertEquals("Journal Portail Keo 1.16 (version de test) — 02/11/2026 09:15", lines[0])
        assertEquals("Téléphone : Samsung SM-S921B · Android 15", lines[1])
        assertEquals("État : Actif · appel automatique activé", lines[2])
        assertEquals("Horaires : Lun → ven · 07:00–10:00 · pas d'appel les jours fériés", lines[3])
        assertEquals("Zones : portail 250 m · approche 1 km · GPS précis à l'approche", lines[4])
        assertEquals("Appel : Compte à rebours 5 s · raccroche après 20 s · rappel si occupé : non", lines[5])
        assertEquals("Autorisations : tout est accordé", lines[6])
        assertTrue(lines[7].startsWith("Derniers appels : "))
        assertEquals("", lines[8])
        assertEquals("02/11 08:20:31  Appel automatique lancé vers 06••••••44", lines[9])
        assertFalse("numéro jamais en clair", text.contains("0611223344"))
    }

    @Test
    fun `position jamais incluse et autorisations manquantes listees`() {
        val text = LogShare.text(info.copy(hasLocation = false, missingPermissions = listOf("Passer des appels"), calls = emptyList(), log = ""))
        assertTrue(text.contains("Zones : position du portail non renseignée"))
        assertTrue(text.contains("Autorisations : manquent Passer des appels"))
        assertTrue(text.contains("Derniers appels : aucun"))
        assertTrue(text.endsWith("(journal vide)"))
    }
}
