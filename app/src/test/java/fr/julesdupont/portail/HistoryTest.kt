package fr.julesdupont.portail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class HistoryTest {
    private val today = LocalDate.of(2026, 10, 7) // mercredi
    private fun call(day: Int, h: Int, m: Int, month: Int = 10) = LocalDateTime.of(2026, month, day, h, m, 30)

    @Test
    fun `ajout en tete et limite`() {
        var text = ""
        repeat(History.MAX + 5) { text = History.add(text, call(1, 8, it % 60)) }
        val calls = History.parse(text)
        assertEquals(History.MAX, calls.size)
        assertEquals(call(1, 8, (History.MAX + 4) % 60), calls.first())
    }

    @Test
    fun `lignes invalides ignorees`() =
        assertEquals(listOf(call(5, 8, 12)), History.parse("n'importe quoi\n${call(5, 8, 12)}\n\n"))

    @Test
    fun `appels de la semaine depuis lundi`() {
        val calls = listOf(call(7, 8, 0), call(6, 8, 10), call(5, 8, 20), call(2, 8, 5), call(30, 8, 0, month = 9))
        assertEquals(3, History.thisWeek(calls, today))
    }

    @Test
    fun `heure moyenne`() {
        assertEquals("8 h 10", History.averageTime(listOf(call(7, 8, 0), call(6, 8, 20))))
        assertNull(History.averageTime(emptyList()))
    }

    @Test
    fun `resume et derniers appels`() {
        val calls = listOf(call(7, 8, 0), call(6, 8, 20))
        assertEquals("Cette semaine : 2 appels · arrivée moyenne 8 h 10", History.summary(calls, today))
        assertEquals("Cette semaine : 1 appel · arrivée moyenne 8 h 00", History.summary(calls.take(1), today))
        assertEquals("Aucun appel automatique pour l'instant", History.summary(emptyList(), today))
        assertEquals(listOf("mer. 7 oct. · 08:00", "mar. 6 oct. · 08:20"), History.recent(calls))
    }
}
