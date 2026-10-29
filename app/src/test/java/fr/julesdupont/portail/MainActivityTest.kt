package fr.julesdupont.portail

import android.view.View
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class MainActivityTest {
    private val app get() = TestSupport.app

    @Before fun setUp() {
        TestSupport.setUp()
        TestSupport.configuredPrefs().apply {
            enabled = false
            setupDone = true
            lastSeenWhatsNew = WhatsNew.CURRENT
            lastUpdateCheck = LocalDate.now().toString()
        }
    }
    @After fun tearDown() = TestSupport.tearDown()

    /** Créée et démarrée (le bouton Retour n'agit qu'à partir de là), sans onResume (zones, mises à jour). */
    private fun launch(): ActivityController<MainActivity> =
        Robolectric.buildActivity(MainActivity::class.java).create().start()

    private fun MainActivity.v(id: Int): View = findViewById(id)
    private fun View.shown() = visibility == View.VISIBLE

    @Test
    fun `onglets et page d'un reglage`() {
        val a = launch().get()
        assertEquals(true, a.v(R.id.pageHome).shown())
        assertEquals(false, a.v(R.id.pageSettings).shown())

        a.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)
            .selectedItemId = R.id.navSettings
        assertEquals(false, a.v(R.id.pageHome).shown())
        assertEquals(true, a.v(R.id.pageSettings).shown())

        a.v(R.id.rowCall).performClick()
        assertEquals(true, a.v(R.id.pageDetail).shown())
        assertEquals(true, a.v(R.id.secCall).shown())
        assertEquals(false, a.v(R.id.secPortal).shown())
        assertEquals("Appel automatique",
            a.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.detailToolbar).title)

        a.onBackPressedDispatcher.onBackPressed()
        assertEquals("retour à la liste des réglages", true, a.v(R.id.pageSettings).shown())
        assertEquals(false, a.v(R.id.pageDetail).shown())

        a.onBackPressedDispatcher.onBackPressed()
        assertEquals("puis à l'accueil", true, a.v(R.id.pageHome).shown())
    }

    @Test
    fun `resumes des reglages`() {
        val a = launch().get()
        val subtitle = a.v(R.id.rowSchedule).findViewById<android.widget.TextView>(R.id.rowSubtitle)
        assertEquals("Lun → ven · 07:00–10:00", subtitle.text.toString())
        val phone = a.v(R.id.rowPhone).findViewById<android.widget.TextView>(R.id.rowSubtitle)
        assertEquals(TestSupport.PHONE, phone.text.toString())
        assertEquals("outils de test cachés hors version de test", false, a.v(R.id.rowTest).shown())
    }

    @Test
    fun `suivi de l'appel en direct sur l'accueil`() {
        val a = launch().get()
        LiveCall.state = LiveCall.State.Calling(elapsedMs = 12_000, hangupSeconds = 20, attempt = 1)
        a.renderLive()
        assertEquals(true, a.v(R.id.cardLive).shown())
        assertEquals(false, a.v(R.id.cardStatus).shown())
        assertEquals(false, a.v(R.id.layoutGate).shown())
        assertEquals("12 s", a.findViewById<android.widget.TextView>(R.id.liveBig).text.toString())
        assertEquals("Raccrocher maintenant",
            a.findViewById<android.widget.TextView>(R.id.btnLiveAction).text.toString())

        LiveCall.state = LiveCall.State.Idle
        a.renderLive()
        assertEquals(true, a.v(R.id.cardStatus).shown())
        assertEquals(true, a.v(R.id.layoutGate).shown())
        assertEquals(false, a.v(R.id.cardLive).shown())
    }

    @Test
    fun `historique semaine et journal`() {
        Prefs(app).history = History.add("", TestSupport.MONDAY_8H)
        Prefs(app).log("Appel en cours (essai 1)")
        val a = launch().get()
        a.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)
            .selectedItemId = R.id.navHistory
        assertEquals(true, a.v(R.id.pageHistory).shown())
        assertEquals("lun → ven", 5, a.findViewById<android.widget.LinearLayout>(R.id.weekRow).childCount)
        assertEquals(1, a.findViewById<android.widget.LinearLayout>(R.id.logList).childCount)
    }
}
