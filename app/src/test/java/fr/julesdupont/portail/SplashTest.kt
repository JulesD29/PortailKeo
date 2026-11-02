package fr.julesdupont.portail

import android.graphics.drawable.Animatable
import androidx.appcompat.content.res.AppCompatResources
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SplashTest {
    @Test
    fun `nom et phrase seulement au lancement, animations actives`() {
        assertTrue(Splash.showIntro(enabledInBuild = true, coldStart = true, animationsEnabled = true))
        assertFalse("pas après une rotation", Splash.showIntro(true, coldStart = false, animationsEnabled = true))
        assertFalse("animations désactivées", Splash.showIntro(true, true, animationsEnabled = false))
        assertFalse("pas prévu dans cette version", Splash.showIntro(false, true, true))
    }

    @Test
    fun `l'icone animee se charge et demarre`() {
        val d = AppCompatResources.getDrawable(TestSupport.app, R.drawable.splash_icon)!!
        assertTrue(d is Animatable)
        (d as Animatable).start()
        assertTrue(AppCompatResources.getDrawable(TestSupport.app, R.drawable.splash_icon_open) != null)
    }

    @Test
    fun `phrase de l'ecran de demarrage`() =
        assertEquals("Arrivez, c'est ouvert", TestSupport.app.getString(R.string.splash_phrase))
}
