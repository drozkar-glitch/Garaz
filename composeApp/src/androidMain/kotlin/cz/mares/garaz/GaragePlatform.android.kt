package cz.mares.garaz

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
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
    var launchCreateDocument: ((String) -> Unit)? = null
    var launchOpenDocument: (() -> Unit)? = null
    var onCreateDocumentResult: ((String?) -> Unit)? = null
    var onOpenDocumentResult: ((String?, String?) -> Unit)? = null

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

    actual fun currentTimeMillis(): Long = System.currentTimeMillis()

    actual fun copyToClipboard(text: String) {
        val ctx = GaragePlatformInit.appContext ?: return
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText("GarazBackup", text))
    }

    actual fun selectBackupFileForSave(suggestedName: String, initialContent: String, onResult: (String?) -> Unit) {
        GaragePlatformInit.onCreateDocumentResult = { uriStr ->
            if (!uriStr.isNullOrBlank()) {
                writeBackupFileUri(uriStr, initialContent)
                onResult(uriStr)
            } else {
                onResult(null)
            }
        }
        GaragePlatformInit.launchCreateDocument?.invoke(suggestedName) ?: onResult(null)
    }

    actual fun selectBackupFileForOpen(onResult: (uri: String?, content: String?) -> Unit) {
        GaragePlatformInit.onOpenDocumentResult = { uriStr, content ->
            onResult(uriStr, content)
        }
        GaragePlatformInit.launchOpenDocument?.invoke() ?: onResult(null, null)
    }

    actual fun writeBackupFileUri(uriString: String, content: String): Boolean {
        val ctx = GaragePlatformInit.appContext ?: return false
        return try {
            val uri = Uri.parse(uriString)
            ctx.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: return false
            true
        } catch (_: Exception) {
            false
        }
    }

    actual fun readBackupFileUri(uriString: String): String? {
        val ctx = GaragePlatformInit.appContext ?: return null
        return try {
            val uri = Uri.parse(uriString)
            ctx.contentResolver.openInputStream(uri)?.use { inp ->
                BufferedReader(InputStreamReader(inp, Charsets.UTF_8)).readText()
            }
        } catch (_: Exception) {
            null
        }
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
