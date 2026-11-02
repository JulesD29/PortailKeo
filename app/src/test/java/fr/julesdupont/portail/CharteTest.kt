package fr.julesdupont.portail

import android.util.TypedValue
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Charte graphique (CHARTE.md) : couleurs de la marque, rôles principaux et police du thème. */
@RunWith(RobolectricTestRunner::class)
class CharteTest {
    private val app get() = TestSupport.app
    private fun color(id: Int) = ContextCompat.getColor(app, id)

    @Test
    fun `couleurs de la marque`() {
        assertEquals(0xFF0B3722.toInt(), color(R.color.brand_green))
        assertEquals(0xFFF884C9.toInt(), color(R.color.brand_pink))
        assertEquals(0xFFFCB1EC.toInt(), color(R.color.brand_pink_light))
    }

    @Test
    fun `roles du mode clair`() {
        assertEquals("primary = vert portail", 0xFF0B3722.toInt(), color(R.color.md_primary))
        assertEquals("secondaryContainer = rose clair", 0xFFFDCFF4.toInt(), color(R.color.md_secondaryContainer))
    }

    @Test
    fun `police Be Vietnam Pro dans le theme`() {
        val themed = ContextThemeWrapper(app, R.style.Theme_PortailAuto)
        val v = TypedValue()
        assertTrue(themed.theme.resolveAttribute(android.R.attr.fontFamily, v, true))
        assertEquals(R.font.be_vietnam_pro, v.resourceId)
    }

    @Test
    fun `logo et icones se chargent`() {
        for (id in listOf(R.drawable.ic_logo_k, R.drawable.ic_launcher_fg, R.drawable.ic_launcher_mono, R.drawable.ic_notif))
            assertTrue(androidx.appcompat.content.res.AppCompatResources.getDrawable(app, id) != null)
    }

    @Test
    fun `Keo en couleur d'accent dans le titre`() {
        val t = Splash.titleWithAccent("Portail Keo (test)", 0xFF8E3A6E.toInt()) as android.text.Spanned
        val spans = t.getSpans(0, t.length, android.text.style.ForegroundColorSpan::class.java)
        assertEquals(1, spans.size)
        assertEquals("Keo", t.subSequence(t.getSpanStart(spans[0]), t.getSpanEnd(spans[0])).toString())
    }
}
