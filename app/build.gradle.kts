plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.calculator.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.calculator.app"
        minSdk = 35
        targetSdk = 37
        versionCode = 61
        versionName = "1.0.60"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
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
        buildConfig = false
    }

    composeCompiler {
        // Generates per-class stability/skippability reports under build/compose-reports
        // when explicitly requested via -Pcompose.reports=true (kept off by default to
        // avoid extra compile time on regular builds).
        if (project.findProperty("compose.reports") == "true") {
            reportsDestination = layout.buildDirectory.dir("compose-reports")
            metricsDestination = layout.buildDirectory.dir("compose-metrics")
        }
    }

    packaging {
        resources {
            // Strip duplicated LICENSE files, Kotlin reflection metadata we don't use,
            // and the kotlinx-coroutines debug-probes artifact (release-only waste).
            excludes += setOf(
                "META-INF/*.txt",
                "META-INF/LICENSE*",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/androidx/**/LICENSE.txt",
                "META-INF/versions/**",
                "**/*.kotlin_module",
                "DebugProbesKt.bin",
            )
        }
        jniLibs {
            // These prebuilt AAR-shipped .so files can't be re-stripped by AGP's
            // strip tool. Telling AGP to keep their debug symbols (i.e. not
            // attempt the strip pass at all) is functionally identical to the
            // current "packaged as-is" fallback and silences the noisy
            // "Unable to strip the following libraries" build messages.
            keepDebugSymbols += setOf(
                "**/libandroidx.graphics.path.so",
                "**/libdatastore_shared_counter.so",
            )
        }
    }

    // Debug-signed release APK never goes to Play; the dependency-info blob
    // AGP injects for Play upload is pure overhead here.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    lint {
        abortOnError = true
        warningsAsErrors = false
        checkDependencies = true
        checkReleaseBuilds = true
        explainIssues = true
        showAll = true
        htmlReport = true
        xmlReport = true
        sarifReport = true
        baseline = file("lint-baseline.xml")
        lintConfig = rootProject.file("config/lint/lint.xml")
        // We intentionally ship the adaptive launcher icon at only xxhdpi+xxxhdpi
        // (minSdk=33 universally supports adaptive icons; lower-density rasters
        // were dead weight). Don't flag the missing folders.
        disable += "IconMissingDensityFolder"
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

ktlint {
    version.set(libs.versions.ktlint.engine.get())
    android.set(true)
    ignoreFailures.set(false)
    filter {
        exclude { it.file.path.contains("/build/") }
    }
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.SARIF)
    }
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    baseline = rootProject.file("config/detekt/detekt-baseline.xml")
    parallel = true
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // Compose BOM
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)

    // Compose (pinned for M3 Expressive compatibility)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.foundation)
    implementation(libs.compose.animation)

    // Material 3 Expressive
    implementation(libs.material3)

    // Material 3 Adaptive (WindowSizeClass)
    implementation(libs.material3.adaptive)

    // Activity Compose
    implementation(libs.activity.compose)

    // Lifecycle
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // Room (for history)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DataStore (for preferences)
    implementation(libs.datastore.preferences)

    // Coroutines
    implementation(libs.coroutines.android)

    // Immutable collections (stable Compose params for List/Set types)
    implementation(libs.collections.immutable)

    // Core KTX
    implementation(libs.core.ktx)

    // Splash screen API (Android 12+ system splash)
    implementation(libs.core.splashscreen)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.room.testing)

    detektPlugins(libs.detekt.compose)
    lintChecks(libs.lint.slack.checks)
    lintChecks(libs.lint.slack.compose)
}
