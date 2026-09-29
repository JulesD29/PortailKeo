package fr.julesdupont.portail

import com.google.android.gms.location.Geofence
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class GeofenceLogicTest {
    private val app get() = TestSupport.app
    private val enter = Geofence.GEOFENCE_TRANSITION_ENTER
    private val exit = Geofence.GEOFENCE_TRANSITION_EXIT

    @Before fun setUp() {
        TestSupport.setUp()
        TestSupport.grantCall()
    }
    @After fun tearDown() = TestSupport.tearDown()

    @Test
    fun `entree dans la zone du portail declenche l'appel`() {
        TestSupport.configuredPrefs(countdown = 0)
        GeofenceReceiver.handleTransition(app, enter, listOf(GeofenceManager.GEOFENCE_ID))
        assertEquals(listOf(TestSupport.PHONE), TestSupport.calls)
    }

    @Test
    fun `entree dans la zone d'approche demarre le GPS precis`() {
        TestSupport.configuredPrefs()
        GeofenceReceiver.handleTransition(app, enter, listOf(GeofenceManager.APPROACH_ID))
        val started = shadowOf(app).nextStartedService
        assertEquals(PortalService.ACTION_APPROACH, started.action)
        assertTrue(TestSupport.calls.isEmpty())
    }

    @Test
    fun `approche un jour sans appel pas de GPS`() {
        TestSupport.configuredPrefs().pauseUntil = "2026-10-09"
        GeofenceReceiver.handleTransition(app, enter, listOf(GeofenceManager.APPROACH_ID))
        assertNull(shadowOf(app).nextStartedService)
        assertTrue(Prefs(app).logText.contains("pas de suivi GPS : en pause"))
    }

    @Test
    fun `sortie de la zone d'approche arrete le GPS`() {
        TestSupport.configuredPrefs()
        GeofenceReceiver.handleTransition(app, exit, listOf(GeofenceManager.APPROACH_ID))
        assertEquals(PortalService::class.java.name, shadowOf(app).nextStoppedService?.component?.className)
    }

    @Test
    fun `sortie de la zone du portail ignoree`() {
        TestSupport.configuredPrefs(countdown = 0)
        GeofenceReceiver.handleTransition(app, exit, listOf(GeofenceManager.GEOFENCE_ID))
        assertTrue(TestSupport.calls.isEmpty())
        assertNull(shadowOf(app).nextStoppedService)
    }
}
