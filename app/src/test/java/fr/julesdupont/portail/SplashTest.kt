package fr.julesdupont.portail

import androidx.appcompat.content.res.AppCompatResources
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SplashTest {
    @Test
    fun `video seulement au lancement, animations actives`() {
        assertTrue(Splash.showIntro(enabledInBuild = true, coldStart = true, animationsEnabled = true))
        assertFalse("pas après une rotation", Splash.showIntro(true, coldStart = false, animationsEnabled = true))
        assertFalse("animations désactivées", Splash.showIntro(true, true, animationsEnabled = false))
        assertFalse("pas prévu dans cette version", Splash.showIntro(false, true, true))
    }

    @Test
    fun `video d'ouverture et ecran de demarrage presents`() {
        TestSupport.app.resources.openRawResource(R.raw.ouverture).use { assertTrue(it.read() >= 0) }
        assertTrue(AppCompatResources.getDrawable(TestSupport.app, R.drawable.splash_icon_closed) != null)
    }

    @Test
    fun `filet de securite plus long que la video de 5 s`() = assertTrue(Splash.MAX_MS > 5_000L)
}
