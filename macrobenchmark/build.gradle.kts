plugins {
    id("com.android.test")
}

android {
    namespace = "com.calculator.app.macrobenchmark"
    compileSdk = 37

    targetProjectPath = ":app"

    // AGP 9 renamed `managedDevices.devices` to `managedDevices.localDevices`.
    testOptions.managedDevices.localDevices {
        create("pixel6Api33") {
            device = "Pixel 6"
            apiLevel = 33
            systemImageSource = "aosp"
        }
    }

    lint {
        lintConfig = rootProject.file("config/lint/lint.xml")
        baseline = rootProject.file("lint-baseline.xml")
        abortOnError = true
        checkDependencies = false
    }
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.uiautomator)
}

// Hardcoded to match `app/build.gradle.kts`'s `namespace = "com.calculator.app"`.
// `project(":app").android.namespace` from a `com.android.test` module crashes
// the script with an `ApplicationExtension cannot be cast to TestExtension`
// ClassCastException on AGP 9.x, and `androidComponents.namespace` isn't
// exposed on the test plugin's components extension. Keep this in sync if
// the appId ever changes.
val appId: String = "com.calculator.app"