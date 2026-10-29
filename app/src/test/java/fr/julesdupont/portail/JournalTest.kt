package fr.julesdupont.portail

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class JournalTest {
    private val today = LocalDate.of(2026, 10, 5)

    @Test
    fun `lignes du jour et des jours precedents`() {
        val log = "05/10 08:20:31  Appel en cours (essai 1)\n04/10 18:02:00  Pause levée\n\n"
        assertEquals(
            listOf(
                Journal.Entry("08:20", null, "Appel en cours (essai 1)"),
                Journal.Entry("18:02", "04/10", "Pause levée"),
            ),
            Journal.parse(log, today),
        )
    }

    @Test
    fun `ligne non reconnue gardee telle quelle`() =
        assertEquals(listOf(Journal.Entry("", null, "texte libre")), Journal.parse("texte libre", today))

    @Test
    fun `journal vide`() = assertEquals(emptyList<Journal.Entry>(), Journal.parse("", today))
}
