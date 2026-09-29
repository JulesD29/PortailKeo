package fr.julesdupont.portail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoordsTest {

    @Test
    fun `formats acceptes`() {
        listOf("48.856600, 2.352200", "48.8566,2.3522", "  48.8566   2.3522 ", "48.8566 N, 2.3522 E")
            .forEach { assertEquals(it, 48.8566 to 2.3522, Coords.parse(it)) }
        assertEquals(-33.8688 to 151.2093, Coords.parse("-33.8688, 151.2093"))
    }

    @Test
    fun `textes refuses`() {
        listOf("", "48.8566", "1, 2, 3", "95, 10", "10, 190", "48,8566, 2,3522", "abc")
            .forEach { assertNull(it, Coords.parse(it)) }
    }

    @Test
    fun `format puis relecture identiques`() {
        val text = Coords.format(47.218371, -1.553621)
        assertEquals("47.218371, -1.553621", text)
        assertEquals(47.218371 to -1.553621, Coords.parse(text))
    }
}
