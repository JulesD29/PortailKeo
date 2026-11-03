package fr.julesdupont.portail

/** Écran « Quoi de neuf » affiché une fois après une mise à jour. */
object WhatsNew {
    data class Item(val edition: Int, val title: String, val text: String)

    /** À incrémenter quand on ajoute des nouveautés à présenter. */
    const val CURRENT = 11

    val items = listOf(
        Item(1, "Portail sur une carte", "Placez le portail et réglez les deux zones directement sur la carte."),
        Item(1, "Assistant de configuration", "Les réglages guidés étape par étape au premier lancement."),
        Item(1, "Partage par QR code", "Affichez votre configuration : un collègue la scanne et tout est rempli."),
        Item(2, "Nouvelle interface", "Carte d'état en haut, bouton « Ouvrir le portail », enregistrement automatique et mode sombre."),
        Item(2, "Widget d'écran d'accueil", "L'état et un bouton pour ouvrir le portail, sans ouvrir l'app (appui long sur l'écran d'accueil › Widgets)."),
        Item(2, "Historique des arrivées", "Nombre d'appels de la semaine, heure d'arrivée moyenne et derniers appels."),
        Item(3, "Commande vocale", "« Ok Google, ouvre Portail » appelle le portail, pratique en conduisant. À activer dans les réglages."),
        Item(4, "Appel plus fiable", "Si l'appel automatique se coupe sans sonner, l'app rappelle toute seule (jusqu'à 3 essais), puis raccroche une fois le portail ouvert. Téléphone verrouillé, l'appel part depuis l'écran verrouillé. Réglable dans « Appel automatique »."),
        Item(5, "Retour vocal", "Une vibration quand l'appel part, une double vibration et l'annonce « Portail appelé » dans vos écouteurs quand il se termine."),
        Item(6, "Portail occupé", "Si quelqu'un appelle le portail en même temps que vous, l'app le détecte : elle s'arrête là (le portail est sans doute déjà ouvert) ou rappelle 15 s plus tard si vous cochez « Rappeler si le portail est occupé » dans « Appel automatique »."),
        Item(6, "Interface repensée", "Trois onglets (Accueil, Réglages, Historique), un gros bouton pour ouvrir le portail et le suivi de l'appel en direct : compte à rebours, appel, raccrochage."),
        Item(7, "Partage du journal", "Onglet Historique › Partager : envoyez le journal et vos réglages pour comprendre un souci (le numéro du portail est masqué, la position n'est pas incluse). Le journal garde maintenant environ deux semaines d'événements."),
        Item(7, "Historique plus lisible", "Le nombre d'appels de la semaine en grand, et le journal regroupé par jour avec une frise colorée. « Tout afficher » est en bas du journal."),
        Item(8, "Nouvelles couleurs", "Vert et rose, inspirés de l'icône du portail qui s'ouvre, avec une nouvelle police. Les couleurs ne changent plus avec le fond d'écran."),
        Item(8, "Thème clair ou sombre", "Réglages › Apparence : clair, sombre ou comme le système."),
        Item(9, "Nouveau logo et ouverture animée", "Le K de Keo, dont les battants roses s'ouvrent : sur l'écran d'accueil du téléphone, dans les notifications et à chaque ouverture de l'app (touchez l'écran pour passer l'animation)."),
        Item(10, "Ouverture plus fluide", "À la fin de l'animation, le K se pose sur le bouton « Ouvrir le portail ». Et l'animation ne coupe plus la musique de vos écouteurs."),
        Item(11, "Mises à jour avec Obtainium", "L'app ne s'installe plus elle-même, ce qui évite les alertes de Play Protect et des antivirus. Pour recevoir les nouvelles versions, installez Obtainium : Réglages › À propos › Mises à jour vous guide."),
    )

    /** Nouveautés pas encore vues. */
    fun toShow(lastSeen: Int): List<Item> = items.filter { it.edition > lastSeen }

    fun message(items: List<Item>): String = items.joinToString("\n\n") { "• ${it.title}\n${it.text}" }
}
