package fr.julesdupont.portail

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/** Tuile « Portail » dans les réglages rapides : un appui = un appel. */
class PortalTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        val prefs = Prefs(this)
        tile.state = Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_label)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_phone)
        if (Build.VERSION.SDK_INT >= 29) {
            tile.subtitle = when {
                prefs.phone.isBlank() -> "À configurer"
                Rules.pauseLabel(prefs) != null -> "Auto en pause"
                else -> "Appuyer pour ouvrir"
            }
        }
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (isLocked) unlockAndRun { launchCall() } else launchCall()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun launchCall() {
        val intent = Intent(this, CallActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
