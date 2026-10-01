package fr.julesdupont.portail

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast

/**
 * Écran invisible qui lance l'appel du portail puis se ferme.
 * - Tuile, raccourci, widget, commande vocale : appel manuel immédiat.
 * - Appel automatique téléphone verrouillé (EXTRA_AUTO) : s'affiche par-dessus le verrouillage
 *   et allume l'écran, pour lancer l'appel au premier plan.
 */
class CallActivity : Activity() {

    companion object {
        const val EXTRA_AUTO = "auto"

        fun autoIntent(ctx: Context) = Intent(ctx, CallActivity::class.java)
            .putExtra(EXTRA_AUTO, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra(EXTRA_AUTO, false)) {
            showOverLockScreen()
            Notifier.cancelLockedCall(this)
            if (AutoCall.callNow(this)) Prefs(this).log("Appel lancé depuis l'écran verrouillé")
            finish()
            return
        }
        val phone = Prefs(this).phone
        if (phone.isBlank() || !Perms.call(this)) {
            Toast.makeText(this, "Configurez d'abord ${getString(R.string.app_name)}", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, MainActivity::class.java))
        } else if (CallHelper.call(this, phone, automatic = false)) {
            Toast.makeText(this, "Appel du portail…", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
    }
}
