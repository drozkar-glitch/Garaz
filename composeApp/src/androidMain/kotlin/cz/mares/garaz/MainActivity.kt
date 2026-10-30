package cz.mares.garaz

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

// Hlavni aktivita s podporou Google Drive zalohy (SAF) a povolenim notifikaci
class MainActivity : ComponentActivity() {

    private lateinit var createBackupDocLauncher: ActivityResultLauncher<String>
    private lateinit var openBackupDocLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GaragePlatformInit.init(applicationContext)

        notificationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { _ -> }

        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        createBackupDocLauncher = registerForActivityResult(
            ActivityResultContracts.CreateDocument("text/plain")
        ) { uri: Uri? ->
            if (uri != null) {
                try {
                    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    contentResolver.takePersistableUriPermission(uri, flags)
                } catch (_: Exception) {
                }
                GaragePlatformInit.onCreateDocumentResult?.invoke(uri.toString())
            } else {
                GaragePlatformInit.onCreateDocumentResult?.invoke(null)
            }
        }

        openBackupDocLauncher = registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            if (uri != null) {
                try {
                    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    contentResolver.takePersistableUriPermission(uri, flags)
                } catch (_: Exception) {
                }
                val content = GaragePlatform.readBackupFileUri(uri.toString())
                GaragePlatformInit.onOpenDocumentResult?.invoke(uri.toString(), content)
            } else {
                GaragePlatformInit.onOpenDocumentResult?.invoke(null, null)
            }
        }

        GaragePlatformInit.launchCreateDocument = { suggestedName ->
            createBackupDocLauncher.launch(suggestedName)
        }
        GaragePlatformInit.launchOpenDocument = {
            openBackupDocLauncher.launch(arrayOf("text/plain", "*/*"))
        }

        setContent {
            App()
        }
    }
}
