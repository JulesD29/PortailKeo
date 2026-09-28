package fr.julesdupont.portail

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat

object Perms {
    private fun granted(ctx: Context, p: String) =
        ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED

    fun fineLocation(ctx: Context) = granted(ctx, Manifest.permission.ACCESS_FINE_LOCATION)

    fun backgroundLocation(ctx: Context) =
        if (Build.VERSION.SDK_INT >= 29) granted(ctx, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        else fineLocation(ctx)

    fun call(ctx: Context) = granted(ctx, Manifest.permission.CALL_PHONE)

    fun notifications(ctx: Context) =
        if (Build.VERSION.SDK_INT >= 33) granted(ctx, Manifest.permission.POST_NOTIFICATIONS) else true

    fun batteryUnrestricted(ctx: Context): Boolean {
        val pm = ctx.getSystemService(PowerManager::class.java)
        return pm.isIgnoringBatteryOptimizations(ctx.packageName)
    }

    /** Permissions demandées en premier (la localisation "toujours" se demande à part). */
    fun basePermissions(): Array<String> {
        val list = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CALL_PHONE,
        )
        if (Build.VERSION.SDK_INT >= 33) list += Manifest.permission.POST_NOTIFICATIONS
        return list.toTypedArray()
    }
}
