package fr.julesdupont.portail

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
class AutoCallTest {
    private val app get() = TestSupport.app

    @Before fun setUp() {
        TestSupport.setUp()
        TestSupport.grantCall()
    }
    @After fun tearDown() = TestSupport.tearDown()

    @Test
    fun `sans compte a rebours l'appel part tout de suite`() {
        TestSupport.configuredPrefs(countdown = 0)
        AutoCall.attempt(app, "Entrée dans la zone du portail")
        assertEquals(listOf(TestSupport.PHONE), TestSupport.calls)
        assertTrue(Prefs(app).logText.contains("Entrée dans la zone du portail"))
    }

    @Test
    fun `avec compte a rebours le service demarre et l'appel attend`() {
        TestSupport.configuredPrefs(countdown = 5)
        AutoCall.attempt(app, "Arrivée")
        assertTrue(TestSupport.calls.isEmpty())
        val started = shadowOf(app).nextStartedService
        assertEquals(PortalService::class.java.name, started.component?.className)
        assertEquals(PortalService.ACTION_COUNTDOWN, started.action)
    }

    @Test
    fun `regles non remplies pas d'appel et raison dans le journal`() {
        TestSupport.setUp(LocalDateTime.of(2026, 10, 10, 8, 0)) // samedi
        TestSupport.grantCall()
        TestSupport.configuredPrefs(countdown = 0)
        AutoCall.attempt(app, "Entrée dans la zone du portail")
        assertTrue(TestSupport.calls.isEmpty())
        assertTrue(Prefs(app).logText.contains("pas d'appel : jour non actif"))
        assertNull(shadowOf(app).nextStartedService)
    }

    @Test
    fun `un seul appel meme si callNow est declenche deux fois`() {
        TestSupport.configuredPrefs(countdown = 0)
        AutoCall.callNow(app)
        AutoCall.callNow(app)
        assertEquals(1, TestSupport.calls.size)
    }

    @Test
    fun `deuxieme entree dans la zone le meme jour ignoree`() {
        TestSupport.configuredPrefs(countdown = 0)
        AutoCall.attempt(app, "Entrée 1")
        AutoCall.attempt(app, "Entrée 2")
        assertEquals(1, TestSupport.calls.size)
        assertTrue(Prefs(app).logText.contains("déjà appelé aujourd'hui"))
    }
}
