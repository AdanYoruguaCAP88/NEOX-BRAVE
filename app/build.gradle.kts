plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    testImplementation("junit:junit:4.13.2")
}

android {
    namespace = "com.neox.brave"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.neox.brave"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
}

kotlin {
    jvmToolchain(17)
}