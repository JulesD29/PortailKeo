package fr.julesdupont.portail

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class TestToolsTest {
    private val app get() = TestSupport.app

    @Before fun setUp() {
        TestSupport.setUp()
        TestSupport.grantCall()
    }
    @After fun tearDown() = TestSupport.tearDown()

    @Test
    fun `desactives dans l'app normale`() = assertFalse(TestTools.enabled)

    @Test
    fun `arrivee simulee passe par les vraies regles et appelle`() {
        TestSupport.configuredPrefs(countdown = 0)
        TestTools.simulateArrival(app)
        assertEquals(listOf(TestSupport.PHONE), TestSupport.calls)
        assertTrue(Prefs(app).logText.contains("[TEST] Arrivée simulée"))
    }

    @Test
    fun `arrivee simulee respecte les regles`() {
        TestSupport.configuredPrefs(countdown = 0).enabled = false
        TestTools.simulateArrival(app)
        assertTrue(TestSupport.calls.isEmpty())
        assertTrue(Prefs(app).logText.contains("automatisation désactivée"))
    }

    @Test
    fun `approche simulee demarre le GPS precis`() {
        TestSupport.configuredPrefs()
        TestTools.simulateApproach(app)
        assertEquals(PortalService.ACTION_APPROACH, shadowOf(app).nextStartedService.action)
    }

    @Test
    fun `revoir quoi de neuf`() {
        Prefs(app).lastSeenWhatsNew = WhatsNew.CURRENT
        TestTools.resetWhatsNew(app)
        assertEquals(0, Prefs(app).lastSeenWhatsNew)
    }

    @Test
    fun `reinitialiser permet de retester le meme jour`() {
        TestSupport.configuredPrefs(countdown = 0)
        TestTools.simulateArrival(app)
        TestTools.simulateArrival(app)
        assertEquals("bloqué la 2e fois", 1, TestSupport.calls.size)
        TestTools.resetToday(app)
        TestTools.simulateArrival(app)
        assertEquals(2, TestSupport.calls.size)
    }
}
