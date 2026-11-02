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
}
