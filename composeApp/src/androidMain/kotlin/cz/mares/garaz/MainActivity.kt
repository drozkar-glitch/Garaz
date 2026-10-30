package cz.mares.garaz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

// Hlavni aktivita pro Android verzi aplikace Garaz
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GaragePlatformInit.init(applicationContext)
        setContent {
            App()
        }
    }
}
