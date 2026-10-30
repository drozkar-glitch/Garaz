package cz.mares.garaz

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GaragePlatformInit {
    var appContext: Context? = null
    fun init(context: Context) {
        appContext = context.applicationContext
    }
}

actual object GaragePlatform {
    private const val PREFS_NAME = "garaz_mares_storage_v1"

    actual fun loadString(key: String, default: String): String {
        val ctx = GaragePlatformInit.appContext ?: return default
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(key, default) ?: default
    }

    actual fun saveString(key: String, value: String) {
        val ctx = GaragePlatformInit.appContext ?: return
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(key, value).apply()
    }

    actual fun currentDateString(): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    actual fun copyToClipboard(text: String) {
        val ctx = GaragePlatformInit.appContext ?: return
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText("GarazBackup", text))
    }

    actual suspend fun httpGet(url: String, headers: Map<String, String>): Result<String> = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12000
                readTimeout = 12000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) GarazMares/1.0")
                headers.forEach { (k, v) -> setRequestProperty(k, v) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            if (code in 200..299) {
                Result.success(text)
            } else {
                Result.failure(Exception("HTTP $code: ${text.take(200)}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
