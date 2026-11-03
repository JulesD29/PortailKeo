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
        assertEquals("en-tête « Aujourd'hui » + 1 événement", 2, a.findViewById<android.widget.LinearLayout>(R.id.logList).childCount)
        assertEquals("1", a.findViewById<android.widget.TextView>(R.id.txtWeekCount).text.toString())
        assertEquals("appel cette semaine", a.findViewById<android.widget.TextView>(R.id.txtWeekLabel).text.toString())
        assertEquals("pas de « Tout afficher » avec peu d'événements", false, a.v(R.id.btnLogMore).shown())
    }

    @Test
    fun `partage du journal numero masque`() {
        Prefs(app).log("Appel manuel lancé vers ${TestSupport.PHONE}")
        val a = launch().get()
        a.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)
            .selectedItemId = R.id.navHistory
        a.findViewById<View>(R.id.btnLogShare).performClick()
        val chooser = org.robolectric.Shadows.shadowOf(a).nextStartedActivity
        assertEquals(android.content.Intent.ACTION_CHOOSER, chooser.action)
        @Suppress("DEPRECATION")
        val send = chooser.getParcelableExtra<android.content.Intent>(android.content.Intent.EXTRA_INTENT)!!
        val text = send.getStringExtra(android.content.Intent.EXTRA_TEXT)!!
        org.junit.Assert.assertTrue(text.startsWith("Journal Portail Keo"))
        org.junit.Assert.assertTrue(text.contains("Appel manuel lancé vers 06••••••44"))
        org.junit.Assert.assertTrue("numéro jamais en clair", !text.contains(TestSupport.PHONE))
    }

    @Test
    fun `tout afficher en bas du journal`() {
        repeat(12) { Prefs(app).log("GPS précis activé $it") }
        val a = launch().get()
        a.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)
            .selectedItemId = R.id.navHistory
        val list = a.findViewById<android.widget.LinearLayout>(R.id.logList)
        val more = a.findViewById<android.widget.TextView>(R.id.btnLogMore)
        assertEquals(true, a.v(R.id.btnLogMore).shown())
        assertEquals("Tout afficher (12 événements)", more.text.toString())
        assertEquals("en-tête + 8 événements", 9, list.childCount)
        more.performClick()
        assertEquals("Réduire", more.text.toString())
        assertEquals("en-tête + 12 événements", 13, list.childCount)
    }

    @Test
    fun `animation de demarrage au lancement, pas apres une rotation`() {
        val c = launch()
        assertEquals(true, c.get().v(R.id.splashOverlay).shown())
        c.recreate()
        assertEquals(false, c.get().v(R.id.splashOverlay).shown())
    }

    @Test
    fun `page apparence choix du theme`() {
        val a = launch().get()
        a.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)
            .selectedItemId = R.id.navSettings
        val subtitle = a.v(R.id.rowTheme).findViewById<android.widget.TextView>(R.id.rowSubtitle)
        assertEquals("Comme le système", subtitle.text.toString())
        a.v(R.id.rowTheme).performClick()
        assertEquals(true, a.v(R.id.secTheme).shown())
        val group = a.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.toggleTheme)
        assertEquals(R.id.btnThemeSystem, group.checkedButtonId)
        group.check(R.id.btnThemeDark)
        assertEquals(AppTheme.Mode.DARK, Prefs(app).theme)
        assertEquals(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES,
            androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode())
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }
}
