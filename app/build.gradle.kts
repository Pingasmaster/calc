plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "com.calculator.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.calculator.app"
        minSdk = 35
        targetSdk = 37
        versionCode = 74
        versionName = "1.0.73"
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
        // Strong skipping is on by default in Kotlin Compose Compiler 2.3.21+
        // (the `enableStrongSkippingMode` Property was deprecated in favor of
        // `featureFlags`, but `featureFlags.add(StrongSkipping)` was deprecated
        // in 2.3.21 in turn, leaving "do nothing" as the only non-deprecated
        // way to get the default behavior). Audit reports produced via
        // `-Pcompose.reports=true` to confirm.
        // Strong skipping treats @Stable/@Immutable-annotated classes and unannotated
        // classes whose properties are all stable (val, String, primitives, etc.) as
        // skippable, eliminating redundant recompositions even when the conservative
        // stability inference would mark them unstable.
        // Generates per-class stability/skippability reports under build/compose-reports
        // when explicitly requested via -Pcompose.reports=true (kept off by default to
        // avoid extra compile time on regular builds).
        if (providers.gradleProperty("compose.reports").orNull == "true") {
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

    // Profile installer: ships baseline-prof.txt + startup-prof.txt alongside
    // the APK and applies them on first run for AOT/JIT optimization.
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":baselineprofile"))

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.room.testing)

    // LeakCanary: debug-only, auto-installed via AppStartup. R8 strips it
    // from the release APK so there's zero production cost.
    debugImplementation(libs.leakcanary.android)

    detektPlugins(libs.detekt.compose)
    lintChecks(libs.lint.slack.checks)
    lintChecks(libs.lint.slack.compose)
}

// Regenerate baseline-prof.txt + startup-prof.txt on every release build,
// keep them in source control so PRs can show the diff. The CI workflow in
// .github/workflows/baseline-profile.yml re-runs this in a Pixel 6 API 33
// managed device for an authoritative regeneration on push to master.
//
// Auto-generation is gated on the `baseline.profile.auto.generate` Gradle
// property so `./build.sh` (clean + ktlintCheck + detekt + lintRelease +
// assembleDebug + assembleRelease + testDebugUnitTest) does NOT need a
// connected emulator — the BaselineProfileGenerator test runs as a
// macrobenchmark on the `pixel6Api33` managed device, which only exists
// in CI. CI opts in by passing `-Pbaseline.profile.auto.generate=true`.
// Local devs with a device can opt in the same way.
val autoGenerateBaselineProfile: Boolean = providers.gradleProperty("baseline.profile.auto.generate")
    .map { it.toBoolean() }
    .getOrElse(false)
baselineProfile {
    automaticGenerationDuringBuild = autoGenerateBaselineProfile
    saveInSrc = true
}
