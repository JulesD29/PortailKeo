package fr.julesdupont.portail

import android.content.Intent
import android.os.Looper
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
class PortalServiceTest {
    private val app get() = TestSupport.app

    private fun intent(action: String) = Intent(app, PortalService::class.java).setAction(action)
    private fun idle(seconds: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(seconds))

    @Before fun setUp() {
        TestSupport.setUp()
        TestSupport.grantCall()
        TestSupport.configuredPrefs(countdown = 3)
    }
    @After fun tearDown() = TestSupport.tearDown()

    @Test
    fun `le compte a rebours appelle a la fin du delai`() {
        Robolectric.buildService(PortalService::class.java, intent(PortalService.ACTION_COUNTDOWN))
            .create().startCommand(0, 1)
        idle(2)
        assertTrue("pas d'appel avant la fin", TestSupport.calls.isEmpty())
        idle(2)
        assertEquals(listOf(TestSupport.PHONE), TestSupport.calls)
    }

    @Test
    fun `annuler empeche l'appel pour la journee`() {
        val controller = Robolectric.buildService(PortalService::class.java, intent(PortalService.ACTION_COUNTDOWN))
            .create().startCommand(0, 1)
        idle(1)
        controller.withIntent(intent(PortalService.ACTION_CANCEL)).startCommand(0, 2)
        idle(10)
        assertTrue(TestSupport.calls.isEmpty())
        assertEquals("2026-10-05", Prefs(app).lastCallDate)
        assertTrue(Prefs(app).logText.contains("Appel annulé"))
    }

    /** Démarre le suivi comme le fait l'app après un appel automatique. */
    private fun monitoredCall(): org.robolectric.android.controller.ServiceController<PortalService> {
        TestSupport.grantCallControl()
        val controller = Robolectric.buildService(PortalService::class.java, intent(PortalService.ACTION_CALL_NOW))
            .create().startCommand(0, 1)
        assertEquals(listOf(TestSupport.PHONE), TestSupport.calls)
        // L'app se demande à elle-même de suivre l'appel (startMonitor) : on livre cette demande.
        controller.withIntent(intent(PortalService.ACTION_MONITOR)).startCommand(0, 2)
        return controller
    }

    @Test
    fun `appel coupe sans sonner rappel automatique puis abandon`() {
        CallMonitor.inCallProbe = { false }
        monitoredCall()
        idle(9)  // vérification à 6 s, nouvel essai 2 s plus tard
        assertEquals(2, TestSupport.calls.size)
        assertTrue(Prefs(app).logText.contains("nouvel essai (2/3)"))
        idle(9)
        assertEquals(3, TestSupport.calls.size)
        idle(9)
        assertEquals("pas plus de 3 essais", 3, TestSupport.calls.size)
        assertTrue(Prefs(app).logText.contains("Échec : l'appel s'est coupé 3 fois sans sonner"))
        assertEquals("1 seul appel dans l'historique", 1, History.parse(Prefs(app).history).size)
    }

    @Test
    fun `appel qui sonne raccroche automatiquement apres le delai`() {
        var ended = false
        CallMonitor.inCallProbe = { true }
        CallMonitor.endCaller = { ended = true; true }
        Prefs(app).hangupSeconds = 20
        monitoredCall()
        idle(7)
        assertTrue(Prefs(app).logText.contains("Appel en cours (essai 1)"))
        assertTrue("pas encore raccroché", !ended)
        idle(14)
        assertTrue(ended)
        assertTrue(Prefs(app).logText.contains("Raccroché automatiquement après 20 s"))
        assertEquals("aucun nouvel essai", 1, TestSupport.calls.size)
    }

    @Test
    fun `raccrochage desactive`() {
        var ended = false
        CallMonitor.inCallProbe = { true }
        CallMonitor.endCaller = { ended = true; true }
        Prefs(app).hangupSeconds = 0
        monitoredCall()
        idle(60)
        assertTrue(!ended)
    }

    @Test
    fun `sans autorisation de gestion des appels pas de suivi`() {
        val controller = Robolectric.buildService(PortalService::class.java, intent(PortalService.ACTION_CALL_NOW))
            .create().startCommand(0, 1)
        controller.withIntent(intent(PortalService.ACTION_MONITOR)).startCommand(0, 2)
        assertTrue(Prefs(app).logText.contains("Suivi de l'appel impossible"))
    }

    @Test
    fun `appeler maintenant n'attend pas la fin du compte a rebours`() {
        val controller = Robolectric.buildService(PortalService::class.java, intent(PortalService.ACTION_COUNTDOWN))
            .create().startCommand(0, 1)
        controller.withIntent(intent(PortalService.ACTION_CALL_NOW)).startCommand(0, 2)
        assertEquals(listOf(TestSupport.PHONE), TestSupport.calls)
        idle(10)
        assertEquals("un seul appel", 1, TestSupport.calls.size)
    }
}
