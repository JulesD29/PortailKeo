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

    @Test
    fun `en-tete de jour`() {
        assertEquals("Aujourd'hui", Journal.dayLabel(null, today))
        assertEquals("Aujourd'hui", Journal.dayLabel("05/10", today))
        assertEquals("Hier", Journal.dayLabel("04/10", today))
        assertEquals("Ven. 2 oct.", Journal.dayLabel("02/10", today))
        assertEquals("date « future » = année précédente", "Ven. 26 déc.", Journal.dayLabel("26/12", today))
        assertEquals("illisible gardé tel quel", "xx", Journal.dayLabel("xx", today))
    }

    @Test
    fun `type d'evenement pour la couleur du point`() {
        assertEquals(Journal.Kind.OK, Journal.kind("Raccroché automatiquement après 20 s"))
        assertEquals(Journal.Kind.OK, Journal.kind("Appel terminé après 12 s"))
        assertEquals(Journal.Kind.CALL, Journal.kind("Appel en cours (essai 1)"))
        assertEquals(Journal.Kind.CALL, Journal.kind("Arrivée détectée par GPS (35 m, ±8 m)"))
        assertEquals(Journal.Kind.PROBLEM, Journal.kind("Ligne du portail occupée : pas de nouvel essai"))
        assertEquals(Journal.Kind.PROBLEM, Journal.kind("Échec : ligne du portail toujours occupée après 3 essais"))
        assertEquals(Journal.Kind.PROBLEM, Journal.kind("L'appel n'est pas parti : autorisation d'appeler manquante"))
        assertEquals(Journal.Kind.INFO, Journal.kind("GPS précis activé"))
        assertEquals(Journal.Kind.INFO, Journal.kind("Pause terminée"))
    }
}
