package fr.julesdupont.portail

/** Décide si une position GPS correspond à une arrivée au portail. */
object Arrival {
    /** Tolérance minimale sur la précision GPS, en mètres. */
    private const val MIN_ACCURACY_TOLERANCE = 100f

    /**
     * Arrivé si la distance est dans le rayon ET si la position est assez précise
     * (sinon on attend une meilleure position pour éviter un appel prématuré).
     */
    fun isArrived(distanceM: Float, accuracyM: Float, radiusM: Int): Boolean =
        distanceM <= radiusM && accuracyM <= maxOf(MIN_ACCURACY_TOLERANCE, radiusM.toFloat())
}
