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
class ConfigImportTest {
    private val app get() = TestSupport.app

    @Before fun setUp() = TestSupport.setUp()
    @After fun tearDown() = TestSupport.tearDown()

    @Test
    fun `partage puis import reproduit la configuration`() {
        val source = TestSupport.configuredPrefs(countdown = 3)
        val link = ConfigShare.encode(ConfigShare.fromPrefs(source))
        TestTools.resetSetup(app)

        ConfigShare.apply(Prefs(app), ConfigShare.decode(link)!!)
        val p = Prefs(app)
        assertEquals(TestSupport.PHONE, p.phone)
        assertEquals(47.2184, p.lat, 1e-6)
        assertEquals(250, p.radius)
        assertEquals(3, p.countdownSeconds)
        assertTrue(p.logText.contains("Configuration importée"))
    }

    @Test
    fun `l'import n'active pas l'automatisation et garde pause et appel du jour`() {
        val p = Prefs(app).apply { enabled = false; pauseUntil = "2026-10-09"; lastCallDate = "2026-10-05" }
        ConfigShare.apply(p, SharedConfig("0611", 47.2, -1.5, 250))
        assertFalse(p.enabled)
        assertEquals("2026-10-09", p.pauseUntil)
        assertEquals("2026-10-05", p.lastCallDate)
    }

    @Test
    fun `assistant non affiche aux installations deja configurees`() {
        assertFalse("nouvelle installation", Prefs(app).setupDone)
        TestSupport.configuredPrefs()
        assertTrue("déjà configurée", Prefs(app).setupDone)
        Prefs(app).setupDone = false
        assertFalse(Prefs(app).setupDone)
    }

    @Test
    fun `reinitialiser l'assistant efface tous les reglages`() {
        TestSupport.configuredPrefs().setupDone = true
        TestTools.resetSetup(app)
        val p = Prefs(app)
        assertFalse(p.setupDone)
        assertFalse(p.hasLocation)
        assertEquals("", p.phone)
    }
}
