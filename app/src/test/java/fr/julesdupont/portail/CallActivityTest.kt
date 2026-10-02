package fr.julesdupont.portail

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CallActivityTest {
    private val app get() = TestSupport.app

    @Before fun setUp() {
        TestSupport.setUp()
        TestSupport.grantCall()
        TestSupport.configuredPrefs()
    }
    @After fun tearDown() = TestSupport.tearDown()

    @Test
    fun `appel automatique depuis l'ecran verrouille`() {
        Robolectric.buildActivity(CallActivity::class.java, CallActivity.autoIntent(app)).create()
        assertEquals(listOf(TestSupport.PHONE), TestSupport.calls)
        assertEquals("compté comme l'appel automatique du jour", "2026-10-05", Prefs(app).lastCallDate)
        assertTrue(Prefs(app).logText.contains("Appel lancé depuis l'écran verrouillé"))
    }

    @Test
    fun `pas de double appel si l'appel du jour est deja parti`() {
        Prefs(app).lastCallDate = "2026-10-05"
        Robolectric.buildActivity(CallActivity::class.java, CallActivity.autoIntent(app)).create()
        assertTrue(TestSupport.calls.isEmpty())
    }

    @Test
    fun `tuile et raccourci appel manuel`() {
        Robolectric.buildActivity(CallActivity::class.java,
            android.content.Intent(app, CallActivity::class.java)).create()
        assertEquals(listOf(TestSupport.PHONE), TestSupport.calls)
        assertEquals("pas compté comme appel automatique", "", Prefs(app).lastCallDate)
    }
}
