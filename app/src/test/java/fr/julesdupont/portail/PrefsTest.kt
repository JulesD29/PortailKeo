package fr.julesdupont.portail

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PrefsTest {
    @Before fun setUp() = TestSupport.setUp()
    @After fun tearDown() = TestSupport.tearDown()

    @Test
    fun `valeurs par defaut`() {
        val p = Prefs(TestSupport.app)
        assertFalse(p.enabled)
        assertEquals("", p.phone)
        assertFalse(p.hasLocation)
        assertEquals(200, p.radius)
        assertEquals(7 * 60, p.startMinutes)
        assertEquals(10 * 60, p.endMinutes)
        assertEquals(setOf(1, 2, 3, 4, 5), p.days)
        assertTrue(p.skipHolidays)
        assertTrue(p.twoStage)
        assertEquals(2000, p.approachRadius)
        assertEquals(5, p.countdownSeconds)
        assertEquals("", p.pauseUntil)
    }

    @Test
    fun `les valeurs sont conservees`() {
        Prefs(TestSupport.app).apply {
            phone = " 0611 "; lat = 48.1; lng = -1.7; radius = 300; days = setOf(6, 7)
            pauseUntil = "2026-12-31"; countdownSeconds = 0; twoStage = false
        }
        val p = Prefs(TestSupport.app)
        assertEquals("0611", p.phone) // espaces retirés
        assertTrue(p.hasLocation)
        assertEquals(48.1, p.lat, 0.0)
        assertEquals(-1.7, p.lng, 0.0)
        assertEquals(300, p.radius)
        assertEquals(setOf(6, 7), p.days)
        assertEquals("2026-12-31", p.pauseUntil)
        assertEquals(0, p.countdownSeconds)
        assertFalse(p.twoStage)
    }

    @Test
    fun `journal limite a 40 lignes, plus recent en premier`() {
        val p = Prefs(TestSupport.app)
        repeat(45) { p.log("message $it") }
        val lines = p.logText.lines()
        assertEquals(40, lines.size)
        assertTrue(lines.first().endsWith("message 44"))
        assertTrue(lines.first().startsWith("05/10 08:00:00"))
    }
}
