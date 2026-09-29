package fr.julesdupont.portail

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Commande vocale : une icône d'app supplémentaire nommée « Portail » (alias de CallActivity).
 * Les assistants vocaux ouvrent les apps par leur nom : « Ok Google, ouvre Portail » appelle le portail.
 * Désactivée par défaut, activable dans les réglages.
 */
object VoiceCommand {
    private const val ALIAS = "fr.julesdupont.portail.VoiceLauncher"

    private fun component(ctx: Context) = ComponentName(ctx, ALIAS)

    fun isEnabled(ctx: Context): Boolean =
        ctx.packageManager.getComponentEnabledSetting(component(ctx)) ==
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED

    fun setEnabled(ctx: Context, enabled: Boolean) {
        ctx.packageManager.setComponentEnabledSetting(
            component(ctx),
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
        Prefs(ctx).log(if (enabled) "Commande vocale activée" else "Commande vocale désactivée")
    }

    /** Nom à prononcer après « Ok Google, ouvre … ». */
    fun spokenName(ctx: Context): String = ctx.getString(R.string.voice_label)
}
