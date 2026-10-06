package fr.julesdupont.portail

/** Écran « Quoi de neuf » affiché une fois après une mise à jour. */
object WhatsNew {
    data class Item(val edition: Int, val title: String, val text: String)

    /** À incrémenter quand on ajoute des nouveautés à présenter. */
    const val CURRENT = 5

    val items = listOf(
        Item(1, "Portail sur une carte", "Placez le portail et réglez les deux zones directement sur la carte."),
        Item(1, "Assistant de configuration", "Les réglages guidés étape par étape au premier lancement."),
        Item(1, "Partage par QR code", "Affichez votre configuration : un collègue la scanne et tout est rempli."),
        Item(2, "Nouvelle interface", "Carte d'état en haut, bouton « Ouvrir le portail », enregistrement automatique et mode sombre."),
        Item(2, "Widget d'écran d'accueil", "L'état et un bouton pour ouvrir le portail, sans ouvrir l'app (appui long sur l'écran d'accueil › Widgets)."),
        Item(2, "Historique des arrivées", "Nombre d'appels de la semaine, heure d'arrivée moyenne et derniers appels."),
        Item(3, "Commande vocale", "« Ok Google, ouvre Portail » appelle le portail, pratique en conduisant. À activer dans les réglages."),
        Item(4, "Appel plus fiable", "Si l'appel automatique se coupe sans sonner, l'app rappelle toute seule (jusqu'à 3 essais), puis raccroche une fois le portail ouvert. Téléphone verrouillé, l'appel part depuis l'écran verrouillé. Réglable dans « Appel automatique »."),
        Item(5, "Retour téléphone en poche", "Une vibration quand l'appel part, une double vibration et l'annonce « Portail appelé » dans vos écouteurs quand il se termine."),
    )

    /** Nouveautés pas encore vues. */
    fun toShow(lastSeen: Int): List<Item> = items.filter { it.edition > lastSeen }

    fun message(items: List<Item>): String = items.joinToString("\n\n") { "• ${it.title}\n${it.text}" }
}
