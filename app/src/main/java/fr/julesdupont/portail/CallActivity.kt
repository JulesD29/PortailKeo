package fr.julesdupont.portail

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

/**
 * Écran invisible : lance l'appel du portail puis se ferme.
 * Utilisé par la tuile des réglages rapides et le raccourci de l'icône.
 */
class CallActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val phone = Prefs(this).phone
        if (phone.isBlank() || !Perms.call(this)) {
            Toast.makeText(this, "Configurez d'abord ${getString(R.string.app_name)}", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, MainActivity::class.java))
        } else if (CallHelper.call(this, phone, automatic = false)) {
            Toast.makeText(this, "Appel du portail…", Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}
