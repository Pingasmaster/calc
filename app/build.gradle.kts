plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.calculator.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.calculator.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.0.2"
    }

    signingConfigs {
        create("release") {
            val passwordFile = rootProject.file(".password-signing-keys")
            val signingPassword = if (passwordFile.exists()) {
                passwordFile.readText().trim()
            } else ""
            storeFile = file("../release-keystore.jks")
            storePassword = signingPassword
            keyAlias = "calculator"
            keyPassword = signingPassword
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2026.03.00")
    implementation(composeBom)

    // Compose (pinned to 1.11.0-beta01 for M3 Expressive compatibility)
    implementation("androidx.compose.runtime:runtime:1.11.0-beta01")
    implementation("androidx.compose.ui:ui:1.11.0-beta01")
    implementation("androidx.compose.ui:ui-graphics:1.11.0-beta01")
    implementation("androidx.compose.ui:ui-tooling-preview:1.11.0-beta01")
    debugImplementation("androidx.compose.ui:ui-tooling:1.11.0-beta01")
    implementation("androidx.compose.foundation:foundation:1.11.0-beta01")
    implementation("androidx.compose.animation:animation:1.11.0-beta01")

    // Material 3 Expressive
    implementation("androidx.compose.material3:material3:1.5.0-alpha15")

    // Material Icons
    implementation("androidx.compose.material:material-icons-extended")

    // Material 3 Adaptive (WindowSizeClass)
    implementation("androidx.compose.material3.adaptive:adaptive:1.2.0-alpha01")

    // Graphics Shapes (for MaterialShapes)
    implementation("androidx.graphics:graphics-shapes:1.1.0")

    // Activity Compose
    implementation("androidx.activity:activity-compose:1.13.0")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0-alpha02")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0-alpha02")

    // Room (for history)
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    // DataStore (for preferences)
    implementation("androidx.datastore:datastore-preferences:1.3.0-alpha07")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // Core KTX
    implementation("androidx.core:core-ktx:1.18.0")

    // Desugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.1.5")
}
