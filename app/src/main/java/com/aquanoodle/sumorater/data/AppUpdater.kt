package com.aquanoodle.sumorater.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val build: Int, val apkUrl: String) : UpdateState
    data class Downloading(val build: Int, val progress: Float) : UpdateState
    data class ReadyToInstall(val build: Int, val file: File) : UpdateState
    data class NeedsInstallPermission(val build: Int, val file: File) : UpdateState
    data class Failed(val message: String) : UpdateState
}

/**
 * Updates the app from the repo's GitHub releases (tagged "apk-N", where N is the CI
 * build number and also the APK's versionCode). Every release is signed with the same
 * key, so Android installs it over the current app and keeps all data.
 */
class AppUpdater(private val context: Context) {

    var state by mutableStateOf<UpdateState>(UpdateState.Idle)
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val updatesDir get() = File(context.cacheDir, "updates")

    val currentBuild: Long
        get() = PackageInfoCompat.getLongVersionCode(context.packageManager.getPackageInfo(context.packageName, 0))

    /** What tapping the update row does in each state. */
    fun onTap() {
        when (val s = state) {
            UpdateState.Checking, is UpdateState.Downloading -> Unit
            is UpdateState.Available -> download(s)
            is UpdateState.ReadyToInstall -> install(s.build, s.file)
            is UpdateState.NeedsInstallPermission -> install(s.build, s.file)
            else -> check()
        }
    }

    fun check() {
        if (state == UpdateState.Checking) return
        state = UpdateState.Checking
        scope.launch {
            state = try {
                val latest = withContext(Dispatchers.IO) { fetchLatest() }
                when {
                    latest == null -> UpdateState.Failed("no builds found")
                    latest.first > currentBuild -> UpdateState.Available(latest.first, latest.second)
                    else -> UpdateState.UpToDate
                }
            } catch (e: Exception) {
                UpdateState.Failed("couldn't reach GitHub")
            }
        }
    }

    /** Newest release's (build number, APK download URL). Releases are prereleases, so /latest won't do. */
    private fun fetchLatest(): Pair<Int, String>? {
        val conn = URL("https://api.github.com/repos/$REPO/releases?per_page=30").openConnection() as HttpURLConnection
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.setRequestProperty("User-Agent", "SumoRater")
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        val body = try {
            if (conn.responseCode !in 200..299) throw IllegalStateException("HTTP ${conn.responseCode}")
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
        val releases = JSONArray(body)
        var best: Pair<Int, String>? = null
        for (i in 0 until releases.length()) {
            val r = releases.getJSONObject(i)
            if (r.optBoolean("draft")) continue
            val build = r.optString("tag_name").removePrefix("apk-").toIntOrNull() ?: continue
            val assets = r.optJSONArray("assets") ?: continue
            for (j in 0 until assets.length()) {
                val a = assets.getJSONObject(j)
                if (a.optString("name") == APK_NAME && (best == null || build > best.first)) {
                    best = build to a.optString("browser_download_url")
                }
            }
        }
        return best
    }

    private fun download(target: UpdateState.Available) {
        state = UpdateState.Downloading(target.build, 0f)
        scope.launch {
            val file = withContext(Dispatchers.IO) {
                runCatching {
                    updatesDir.deleteRecursively()
                    updatesDir.mkdirs()
                    val out = File(updatesDir, "sumo-rater-${target.build}.apk")
                    // GitHub redirects asset downloads to its storage host; https->https redirects are followed.
                    val conn = URL(target.apkUrl).openConnection() as HttpURLConnection
                    conn.setRequestProperty("User-Agent", "SumoRater")
                    conn.connectTimeout = 15000
                    conn.readTimeout = 30000
                    try {
                        if (conn.responseCode !in 200..299) throw IllegalStateException("HTTP ${conn.responseCode}")
                        val total = conn.contentLengthLong
                        conn.inputStream.use { input ->
                            out.outputStream().use { output ->
                                val buf = ByteArray(64 * 1024)
                                var done = 0L
                                var lastShown = -1
                                while (true) {
                                    val n = input.read(buf)
                                    if (n < 0) break
                                    output.write(buf, 0, n)
                                    done += n
                                    if (total > 0) {
                                        val pct = (done * 100 / total).toInt()
                                        if (pct != lastShown) {
                                            lastShown = pct
                                            withContext(Dispatchers.Main) { state = UpdateState.Downloading(target.build, pct / 100f) }
                                        }
                                    }
                                }
                            }
                        }
                    } finally {
                        conn.disconnect()
                    }
                    out
                }.getOrNull()
            }
            if (file == null) {
                state = UpdateState.Failed("download failed")
            } else {
                install(target.build, file)
            }
        }
    }

    private fun install(build: Int, file: File) {
        // Android asks once per installing app; until then, send the user to that switch.
        if (!context.packageManager.canRequestPackageInstalls()) {
            state = UpdateState.NeedsInstallPermission(build, file)
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        }
        state = UpdateState.ReadyToInstall(build, file)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private companion object {
        const val REPO = "Aqua-Noodle/sumo-rater"
        const val APK_NAME = "app-debug.apk"
    }
}
