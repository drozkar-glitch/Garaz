// Modul composeApp pro multiplatformni aplikaci Garaz (Android SDK 36 + iOS)
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.application")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

val rawBuildNum = System.getenv("PROJECT_BUILD_NUMBER")?.toIntOrNull()
    ?: System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()
    ?: 1
val ciBuildNumber = rawBuildNum + 100
val appVersionString = System.getenv("APP_VERSION")?.takeIf { it.isNotBlank() } ?: "1.0"

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    val iosTargets = listOf(iosX64(), iosArm64(), iosSimulatorArm64())
    iosTargets.forEach { target ->
        target.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("androidx.activity:activity-compose:1.9.2")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
            }
        }
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
    }
}

android {
    namespace = "cz.mares.garaz"
    compileSdk = 36

    defaultConfig {
        applicationId = "cz.mares.garaz"
        minSdk = 24
        targetSdk = 36
        versionCode = ciBuildNumber
        versionName = appVersionString
    }

    signingConfigs {
        create("release") {
            val ksPath = System.getenv("CM_KEYSTORE_PATH")
            if (!ksPath.isNullOrBlank() && file(ksPath).exists()) {
                storeFile = file(ksPath)
                storePassword = System.getenv("CM_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("CM_KEY_ALIAS")
                keyPassword = System.getenv("CM_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            isShrinkResources = false
            val ksPath = System.getenv("CM_KEYSTORE_PATH")
            if (!ksPath.isNullOrBlank() && file(ksPath).exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Generovani 1024x1024 PNG ikony pro iOS bez zavislosti na java.awt
tasks.register("generateIosAppIcon") {
    doLast {
        val iconDir = file("${rootProject.projectDir}/iosApp/iosApp/Assets.xcassets/AppIcon.appiconset")
        iconDir.mkdirs()
        val iconFile = File(iconDir, "Icon-1024.png")
        val width = 1024
        val height = 1024

        val rawBytes = ByteArrayOutputStream()
        for (y in 0 until height) {
            rawBytes.write(0) // PNG filter None
            for (x in 0 until width) {
                val dx = x - 512
                val dy = y - 512
                val distSq = dx * dx + dy * dy
                // Motiv garaze: tmave modro-bridlicove pozadi se svetlym mezikruzim
                if (distSq in 85000..135000) {
                    rawBytes.write(56)  // R
                    rawBytes.write(189) // G
                    rawBytes.write(248) // B
                } else if (distSq < 85000 && (x in 460..564 || y in 460..564)) {
                    rawBytes.write(16)  // R
                    rawBytes.write(185) // G
                    rawBytes.write(129) // B
                } else {
                    rawBytes.write(15)  // R
                    rawBytes.write(23)  // G
                    rawBytes.write(42)  // B
                }
            }
        }

        val compressed = ByteArrayOutputStream()
        DeflaterOutputStream(compressed, Deflater(Deflater.BEST_SPEED)).use {
            it.write(rawBytes.toByteArray())
        }

        fun writeChunk(out: ByteArrayOutputStream, type: String, data: ByteArray) {
            val len = data.size
            out.write(byteArrayOf((len shr 24).toByte(), (len shr 16).toByte(), (len shr 8).toByte(), len.toByte()))
            val typeBytes = type.toByteArray(Charsets.US_ASCII)
            out.write(typeBytes)
            out.write(data)
            val crc = CRC32()
            crc.update(typeBytes)
            crc.update(data)
            val c = crc.value.toInt()
            out.write(byteArrayOf((c shr 24).toByte(), (c shr 16).toByte(), (c shr 8).toByte(), c.toByte()))
        }

        val pngOut = ByteArrayOutputStream()
        pngOut.write(byteArrayOf(137.toByte(), 80, 78, 71, 13, 10, 26, 10))
        val ihdr = byteArrayOf(
            0, 0, 4, 0,
            0, 0, 4, 0,
            8, 2, 0, 0, 0
        )
        writeChunk(pngOut, "IHDR", ihdr)
        writeChunk(pngOut, "IDAT", compressed.toByteArray())
        writeChunk(pngOut, "IEND", ByteArray(0))

        FileOutputStream(iconFile).use { it.write(pngOut.toByteArray()) }

        val contentsJson = File(iconDir, "Contents.json")
        contentsJson.writeText(
            """
            {
              "images" : [
                {
                  "filename" : "Icon-1024.png",
                  "idiom" : "universal",
                  "platform" : "ios",
                  "size" : "1024x1024"
                }
              ],
              "info" : { "author" : "xcode", "version" : 1 }
            }
            """.trimIndent()
        )
    }
}
