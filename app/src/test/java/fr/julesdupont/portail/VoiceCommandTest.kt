package fr.julesdupont.portail

import android.content.ComponentName
import android.content.Intent
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VoiceCommandTest {
    private val app get() = TestSupport.app

    @Before fun setUp() = TestSupport.setUp()
    @After fun tearDown() = TestSupport.tearDown()

    @Test
    fun `desactivee par defaut`() = assertFalse(VoiceCommand.isEnabled(app))

    @Test
    fun `activer puis desactiver`() {
        VoiceCommand.setEnabled(app, true)
        assertTrue(VoiceCommand.isEnabled(app))
        assertTrue(Prefs(app).logText.contains("Commande vocale activée"))
        VoiceCommand.setEnabled(app, false)
        assertFalse(VoiceCommand.isEnabled(app))
    }

    @Test
    fun `nom a prononcer`() = assertEquals("Portail", VoiceCommand.spokenName(app))

    @Test
    fun `l'icone vocale lance l'ecran d'appel`() {
        val alias = ComponentName(app, "fr.julesdupont.portail.VoiceLauncher")
        val info = app.packageManager.getActivityInfo(alias, android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS)
        assertEquals(CallActivity::class.java.name, info.targetActivity)
        val launch = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(alias)
        assertEquals(alias, launch.component)
    }
}
