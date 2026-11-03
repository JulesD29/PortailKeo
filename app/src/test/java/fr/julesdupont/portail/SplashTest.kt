package fr.julesdupont.portail

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
        assertTrue("première image de la vidéo", AppCompatResources.getDrawable(TestSupport.app, R.drawable.ouverture_debut) != null)
        assertTrue("dernière image", AppCompatResources.getDrawable(TestSupport.app, R.drawable.ouverture_fin) != null)
        assertTrue("dernière image sans le K", AppCompatResources.getDrawable(TestSupport.app, R.drawable.ouverture_fin_sans_k) != null)
        assertTrue("K découpé dans la dernière image", AppCompatResources.getDrawable(TestSupport.app, R.drawable.ouverture_k) != null)
    }

    @Test
    fun `la tuile de la video recouvre celle de l'ecran de demarrage`() {
        val scale = Splash.videoWidthDp() / Splash.VIDEO_W_PX
        assertEquals("même taille de tuile", Splash.SPLASH_TILE_DP, Splash.TILE_PX * scale, 0.01f)
        assertEquals("vidéo au format 9:16", Splash.videoWidthDp() * 16 / 9, Splash.videoHeightDp(), 0.01f)
        assertEquals("tuile 40 px au-dessus du milieu de la vidéo : on descend de 17 dp", 16.97f, Splash.tileOffsetDp(), 0.01f)
    }

    @Test
    fun `le K de la fin de la video est place sur la video a l'ecran`() {
        val k = Splash.endK(videoLeft = 100f, videoTop = 50f, pxPerVideoPx = 0.5f)
        assertEquals(100f + 108.5f, k[0], 0.01f)
        assertEquals(50f + 175.5f, k[1], 0.01f)
        assertEquals(136f, k[2], 0.01f)
        // Pilier du K (x = 36 unités sur 108) mesuré à 308 px dans la dernière image.
        assertEquals(308f, Splash.END_K_LEFT_PX + 36f / 108f * Splash.END_K_SIZE_PX, 0.5f)
    }

    @Test
    fun `le K arrive avant que l'utilisateur s'impatiente`() {
        assertTrue(Splash.FLY_MS in 300L..900L)
        assertTrue(Splash.PULSE_SCALE in 1f..1.1f)
        assertTrue("fondu entre les deux K en fin de trajet", Splash.CROSSFADE_FROM in 0.3f..0.9f)
    }

    @Test
    fun `filet de securite plus long que la video de 5 s`() = assertTrue(Splash.MAX_MS > 5_000L)
}
