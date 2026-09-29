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
