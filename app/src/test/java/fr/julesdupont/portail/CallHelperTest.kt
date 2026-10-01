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
class CallHelperTest {
    private val app get() = TestSupport.app

    @Before fun setUp() = TestSupport.setUp()
    @After fun tearDown() = TestSupport.tearDown()

    @Test
    fun `appel automatique memorise la date du jour`() {
        TestSupport.grantCall()
        assertTrue(CallHelper.call(app, "0611", automatic = true))
        assertEquals(listOf("0611"), TestSupport.calls)
        assertEquals("2026-10-05", Prefs(app).lastCallDate)
        assertTrue(Prefs(app).logText.contains("Appel automatique lancé vers 0611"))
        assertEquals(listOf(TestSupport.MONDAY_8H), History.parse(Prefs(app).history))
    }

    @Test
    fun `appel manuel ne bloque pas l'appel automatique du jour`() {
        TestSupport.grantCall()
        assertTrue(CallHelper.call(app, "0611", automatic = false))
        assertEquals("", Prefs(app).lastCallDate)
        assertTrue(Prefs(app).logText.contains("Appel manuel"))
        assertEquals("pas dans l'historique des arrivées", "", Prefs(app).history)
    }

    @Test
    fun `nouvel essai sans doublon dans l'historique`() {
        TestSupport.grantCall()
        assertTrue(CallHelper.call(app, "0611", automatic = true))
        assertTrue(CallHelper.call(app, "0611", automatic = true, retry = true))
        assertEquals(listOf("0611", "0611"), TestSupport.calls)
        assertEquals("un seul appel dans l'historique", 1, History.parse(Prefs(app).history).size)
        assertTrue(Prefs(app).logText.contains("Nouvel essai d'appel vers 0611"))
    }

    @Test
    fun `sans permission d'appel rien n'est appele`() {
        assertFalse(CallHelper.call(app, "0611", automatic = true))
        assertTrue(TestSupport.calls.isEmpty())
        assertTrue(Prefs(app).logText.contains("permission d'appel manquante"))
    }

    @Test
    fun `erreur du telephone geree sans plantage`() {
        TestSupport.grantCall()
        CallHelper.placer = { _, _ -> throw SecurityException("refusé") }
        assertFalse(CallHelper.call(app, "0611", automatic = true))
        assertEquals("", Prefs(app).lastCallDate)
        assertTrue(Prefs(app).logText.contains("Échec de l'appel"))
    }
}
