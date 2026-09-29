package fr.julesdupont.portail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigShareTest {
    private val config = SharedConfig(
        phone = "+33 6 11 22 33 44", lat = 47.218371, lng = -1.553621, radius = 250, approach = 2000,
        twoStage = true, days = setOf(1, 2, 3, 4, 5), startMinutes = 7 * 60 + 30, endMinutes = 9 * 60,
        countdown = 5, skipHolidays = true,
    )

    @Test
    fun `aller-retour identique`() = assertEquals(config, ConfigShare.decode(ConfigShare.encode(config)))

    @Test
    fun `aller-retour avec d'autres valeurs`() {
        val other = config.copy(phone = "0611223344", twoStage = false, days = setOf(6, 7), countdown = 0,
            skipHolidays = false, startMinutes = 22 * 60, endMinutes = 60)
        assertEquals(other, ConfigShare.decode(ConfigShare.encode(other)))
    }

    @Test
    fun `lien lisible et court pour un QR code`() {
        val link = ConfigShare.encode(config)
        assertTrue(link.startsWith("portailkeo://config?v=1&n=%2B33+6+11+22+33+44&lat=47.218371&lng=-1.553621"))
        assertTrue("longueur ${link.length}", link.length < 200)
    }

    @Test
    fun `version de test acceptee`() =
        assertEquals(config, ConfigShare.decode(ConfigShare.encode(config, ConfigShare.TEST_SCHEME)))

    @Test
    fun `champs facultatifs absents valeurs par defaut`() {
        val c = ConfigShare.decode("portailkeo://config?v=1&n=0611&lat=47.2&lng=-1.5&r=250")!!
        assertEquals(2000, c.approach)
        assertEquals(setOf(1, 2, 3, 4, 5), c.days)
        assertEquals(7 * 60, c.startMinutes)
        assertEquals(5, c.countdown)
        assertTrue(c.twoStage && c.skipHolidays)
    }

    @Test
    fun `textes refuses`() {
        val ok = ConfigShare.encode(config)
        listOf(
            "", "bonjour", "https://example.com/config?v=1", "autreapp://config?v=1&n=0611&lat=1&lng=1&r=250",
            ok.replace("://config", "://autre"), ok.replace("v=1", "v=2"),
            ok.replace("n=%2B33+6+11+22+33+44", "n="), ok.replace("n=%2B33+6+11+22+33+44", "n=abc"),
            ok.replace("lat=47.218371", "lat=95"), ok.replace("lat=47.218371", "lat=x"),
            ok.replace("r=250", "r=10"), ok.replace("a=2000", "a=300"),
            ok.replace("d=12345", "d=189"), ok.replace("d=12345", "d="), ok.replace("d=12345", "d=1a"),
            ok.replace("s=450", "s=2000"), ok.replace("c=5", "c=99"),
        ).forEach { assertNull(it, ConfigShare.decode(it)) }
    }

    @Test
    fun `resume lisible`() {
        val s = ConfigShare.summary(config)
        assertTrue(s.contains("Numéro du portail : +33 6 11 22 33 44"))
        assertTrue(s.contains("Jours : lun, mar, mer, jeu, ven"))
        assertTrue(s.contains("Horaires : 07:30–09:00"))
        assertTrue(s.contains("Zone d'approche : 2 km"))
    }
}
