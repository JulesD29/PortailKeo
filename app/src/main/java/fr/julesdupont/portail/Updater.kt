package fr.julesdupont.portail

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.pm.PackageInfoCompat
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.util.concurrent.Executors

/** Mises à jour depuis les « Releases » du dépôt GitHub public. */
object Updater {
    private const val REPO = "JulesD29/PortailKeo"
    const val ACTION_CHECK = "fr.julesdupont.portail.UPDATE_CHECK"

    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    data class Release(val versionCode: Long, val name: String, val apkUrl: String, val notes: String)

    fun currentVersionCode(ctx: Context): Long =
        PackageInfoCompat.getLongVersionCode(ctx.packageManager.getPackageInfo(ctx.packageName, 0))

    fun currentVersionName(ctx: Context): String =
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "?"

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "PortailKeo")
        }

    /** Dernière release publiée (appel bloquant), ou null s'il n'y en a pas. */
    private fun fetchLatest(): Release? {
        val c = open("https://api.github.com/repos/$REPO/releases/latest")
        c.setRequestProperty("Accept", "application/vnd.github+json")
        try {
            if (c.responseCode == 404) return null
            if (c.responseCode != 200) throw IOException("GitHub a répondu ${c.responseCode}")
            return parseRelease(c.inputStream.bufferedReader().use { it.readText() })
        } finally {
            c.disconnect()
        }
    }

    /** Lit la réponse JSON de l'API GitHub « releases/latest ». Null si pas d'APK ou tag invalide. */
    fun parseRelease(body: String): Release? {
        val json = JSONObject(body)
        val tag = json.getString("tag_name")
        val code = tag.filter { it.isDigit() }.toLongOrNull() ?: return null
        val assets = json.optJSONArray("assets") ?: return null
        var apk: String? = null
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            if (a.getString("name").endsWith(".apk")) {
                apk = a.getString("browser_download_url"); break
            }
        }
        val name = json.optString("name").ifBlank { tag }
        return Release(code, name, apk ?: return null, json.optString("body", ""))
    }

    /** Vrai si la release est plus récente que la version installée. */
    fun isNewer(release: Release, installedVersionCode: Long) = release.versionCode > installedVersionCode

    /** Cherche une version plus récente que celle installée ; callback sur le thread principal. */
    fun check(ctx: Context, callback: (Result<Release?>) -> Unit) {
        val app = ctx.applicationContext
        io.execute {
            val result = runCatching { fetchLatest()?.takeIf { isNewer(it, currentVersionCode(app)) } }
            if (result.isSuccess) Prefs(app).lastUpdateCheck = LocalDate.now().toString()
            main.post { callback(result) }
        }
    }

    /** Vérification discrète (1 fois par jour max) : notification si une version est disponible. */
    fun backgroundCheck(ctx: Context, done: () -> Unit = {}) {
        val app = ctx.applicationContext
        if (Prefs(app).lastUpdateCheck == LocalDate.now().toString()) return done()
        check(app) { r ->
            r.getOrNull()?.let {
                Prefs(app).log("Nouvelle version disponible : ${it.name}")
                Notifier.update(app, it.name)
            }
            done()
        }
    }

    /** Télécharge l'APK et le confie à l'installateur Android (l'utilisateur confirme). */
    fun downloadAndInstall(ctx: Context, release: Release, onError: (String) -> Unit) {
        val app = ctx.applicationContext
        io.execute {
            try {
                val installer = app.packageManager.packageInstaller
                val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
                val sessionId = installer.createSession(params)
                installer.openSession(sessionId).use { session ->
                    val c = open(release.apkUrl)
                    try {
                        if (c.responseCode != 200) throw IOException("téléchargement : HTTP ${c.responseCode}")
                        c.inputStream.use { input ->
                            session.openWrite("PortailKeo.apk", 0, -1).use { out ->
                                input.copyTo(out)
                                session.fsync(out)
                            }
                        }
                    } finally {
                        c.disconnect()
                    }
                    val intent = Intent(app, InstallReceiver::class.java).setAction(InstallReceiver.ACTION)
                    val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                        (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
                    session.commit(PendingIntent.getBroadcast(app, sessionId, intent, flags).intentSender)
                }
                Prefs(app).log("Téléchargement de ${release.name} terminé, installation…")
            } catch (e: Exception) {
                Prefs(app).log("Échec de la mise à jour : ${e.message}")
                main.post { onError(e.message ?: e.javaClass.simpleName) }
            }
        }
    }

    /** Vérification automatique quotidienne, même si l'app n'est pas ouverte. */
    fun scheduleDaily(ctx: Context) {
        val app = ctx.applicationContext
        val pi = PendingIntent.getBroadcast(
            app, 2,
            Intent(app, RegisterReceiver::class.java).setAction(ACTION_CHECK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        app.getSystemService(AlarmManager::class.java).setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_HOUR,
            AlarmManager.INTERVAL_HALF_DAY,
            pi
        )
    }
}
