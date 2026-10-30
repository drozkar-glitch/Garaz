// Konfigurace sestaveni projektu Garaz (cz.mares.garaz)
rootProject.name = "Garaz"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    // Repozitare pro KMP a Android Gradle pluginy
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    // Centralni repozitare zavislosti aplikace Garaz
    repositories {
        google()
        mavenCentral()
    }
}

include(":composeApp")
