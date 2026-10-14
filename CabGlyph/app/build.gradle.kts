plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.cabglyph"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.example.cabglyph"
        minSdk = 34
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    // Copy glyph-matrix-sdk-2.0.aar from the GlyphMatrix-Developer-Kit repo into app/libs/
    implementation(files("libs/glyph-matrix-sdk-2.0.aar"))
}
