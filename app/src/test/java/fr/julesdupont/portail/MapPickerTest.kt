package fr.julesdupont.portail

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MapPickerTest {

    @Test
    fun `le choix sur la carte revient intact a l'ecran de reglages`() {
        val chosen = MapPickerActivity.Selection(47.218371, -1.553621, 250, 2000)
        assertEquals(chosen, MapPickerActivity.parseResult(MapPickerActivity.resultIntent(chosen)))
    }

    @Test
    fun `retour sans choix ignore`() {
        assertNull(MapPickerActivity.parseResult(null))
        assertNull(MapPickerActivity.parseResult(Intent()))
    }

    @Test
    fun `ouverture de la carte avec la position actuelle`() {
        val i = MapPickerActivity.intent(TestSupport.app, 47.2 to -1.5, 250, 2000, twoStage = true)
        assertEquals(MapPickerActivity::class.java.name, i.component?.className)
        assertEquals(true, i.getBooleanExtra("hasPoint", false))
        assertEquals(47.2, i.getDoubleExtra("lat", 0.0), 0.0)
        assertEquals(250, i.getIntExtra("radius", 0))
    }

    @Test
    fun `ouverture sans position connue`() =
        assertEquals(false, MapPickerActivity.intent(TestSupport.app, null, 200, 2000, true).getBooleanExtra("hasPoint", true))
}
