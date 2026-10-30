package cz.mares.garaz

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDefaults
import platform.Foundation.create
import platform.Foundation.dataTaskWithRequest
import platform.Foundation.setValue
import platform.UIKit.UIPasteboard
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

    actual fun copyToClipboard(text: String) {
        UIPasteboard.generalPasteboard.string = text
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
