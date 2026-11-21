plugins {
    alias(libs.plugins.android.application) // AGP 9 compiles Kotlin itself (built-in Kotlin)
    alias(libs.plugins.kotlin.compose)      // the Compose compiler
}

android {
    namespace = "app.kairo"
    // Compose UI 1.13 needs compile SDK 37.1+. compileSdk only decides which APIs we can call;
    // targetSdk (below) decides runtime behavior.
    compileSdk {
        version = release(37) { minorApiLevel = 2 }
    }

    defaultConfig {
        applicationId = "app.kairo"
        minSdk = 31   // Android 12: dynamic color, widget checkboxes
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
