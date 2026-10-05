package fr.julesdupont.portail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsNewTest {
    @Test
    fun `tout est montre a qui n'a rien vu`() =
        assertEquals(WhatsNew.items, WhatsNew.toShow(0))

    @Test
    fun `seulement les nouveautes non vues`() {
        val shown = WhatsNew.toShow(1)
        assertTrue(shown.isNotEmpty())
        assertTrue(shown.all { it.edition >= 2 })
        assertTrue(WhatsNew.toShow(2).all { it.edition >= 3 })
        assertTrue(WhatsNew.toShow(3).all { it.edition >= 4 })
        assertTrue(WhatsNew.toShow(4).all { it.edition == 5 })
    }

    @Test
    fun `rien apres avoir vu l'edition courante`() =
        assertTrue(WhatsNew.toShow(WhatsNew.CURRENT).isEmpty())

    @Test
    fun `l'edition courante correspond aux nouveautes`() {
        assertEquals(WhatsNew.CURRENT, WhatsNew.items.maxOf { it.edition })
        assertEquals("éditions dans l'ordre", WhatsNew.items.map { it.edition }.sorted(), WhatsNew.items.map { it.edition })
    }

    @Test
    fun `message lisible`() {
        val msg = WhatsNew.message(WhatsNew.toShow(1))
        assertTrue(msg.startsWith("• Nouvelle interface\n"))
    }
}
