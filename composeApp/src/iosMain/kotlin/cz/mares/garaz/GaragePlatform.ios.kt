package cz.mares.garaz

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.dataTaskWithRequest
import platform.Foundation.setValue
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.writeToFile
import platform.UIKit.UIPasteboard
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

actual object GaragePlatform {
    actual fun loadString(key: String, default: String): String {
        return NSUserDefaults.standardUserDefaults.stringForKey(key) ?: default
    }

    actual fun saveString(key: String, value: String) {
        NSUserDefaults.standardUserDefaults.setObject(value, forKey = key)
        NSUserDefaults.standardUserDefaults.synchronize()
    }

    actual fun currentDateString(): String {
        val formatter = NSDateFormatter()
        formatter.dateFormat = "dd.MM.yyyy"
        return formatter.stringFromDate(NSDate())
    }

    actual fun currentTimeMillis(): Long {
        return (NSDate().timeIntervalSince1970 * 1000.0).toLong()
    }

    actual fun copyToClipboard(text: String) {
        UIPasteboard.generalPasteboard.string = text
    }

    actual fun showServiceNotification(title: String, message: String) {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        center.requestAuthorizationWithOptions(
            options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound
        ) { granted, _ ->
            if (granted) {
                val content = UNMutableNotificationContent().apply {
                    setTitle(title)
                    setBody(message)
                }
                val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(1.0, false)
                val req = UNNotificationRequest.requestWithIdentifier("garaz_service_alert", content, trigger)
                center.addNotificationRequest(req, null)
            }
        }
    }

    private fun defaultIosBackupPath(fileName: String): String {
        val paths = NSFileManager.defaultManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask)
        val docUrl = paths.firstOrNull() as? NSURL
        return "${docUrl?.path ?: ""}/$fileName"
    }

    actual fun selectBackupFileForSave(suggestedName: String, initialContent: String, onResult: (String?) -> Unit) {
        val path = defaultIosBackupPath(suggestedName)
        if (writeBackupFileUri(path, initialContent)) {
            onResult(path)
        } else {
            onResult(null)
        }
    }

    actual fun selectBackupFileForOpen(onResult: (uri: String?, content: String?) -> Unit) {
        val savedUri = loadString("garaz_onedrive_uri_v1", "").ifBlank { defaultIosBackupPath("garaz_zaloha.txt") }
        val content = readBackupFileUri(savedUri)
        if (content != null) {
            onResult(savedUri, content)
        } else {
            onResult(null, null)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun writeBackupFileUri(uriString: String, content: String): Boolean {
        return try {
            val nsStr = NSString.create(string = content)
            nsStr.writeToFile(uriString, atomically = true, encoding = NSUTF8StringEncoding, error = null)
        } catch (_: Exception) {
            false
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun readBackupFileUri(uriString: String): String? {
        return try {
            NSString.stringWithContentsOfFile(uriString, encoding = NSUTF8StringEncoding, error = null)
        } catch (_: Exception) {
            null
        }
    }

    actual suspend fun httpGet(url: String, headers: Map<String, String>): Result<String> =
        suspendCancellableCoroutine { cont ->
            val nsUrl = NSURL.URLWithString(url)
            if (nsUrl == null) {
                cont.resume(Result.failure(Exception("Neplatná URL: $url")))
                return@suspendCancellableCoroutine
            }
            val request = NSMutableURLRequest.requestWithURL(nsUrl)
            request.setValue("Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) GarazMares/1.0", forHTTPHeaderField = "User-Agent")
            headers.forEach { (k, v) ->
                request.setValue(v, forHTTPHeaderField = k)
            }
            val task = NSURLSession.sharedSession.dataTaskWithRequest(request) { data, _, error ->
                if (error != null) {
                    cont.resume(Result.failure(Exception(error.localizedDescription)))
                } else if (data != null) {
                    val str = NSString.create(data = data, encoding = NSUTF8StringEncoding)?.toString() ?: ""
                    cont.resume(Result.success(str))
                } else {
                    cont.resume(Result.failure(Exception("Prázdná odpověď")))
                }
            }
            task.resume()
        }
}
