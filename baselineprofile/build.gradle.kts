plugins {
    id("com.android.test")
    id("androidx.baselineprofile")
}

android {
    namespace = "com.calculator.app.baselineprofile"
    compileSdk = 37

    targetProjectPath = ":app"

    // AGP 9 renamed `managedDevices.devices` to `managedDevices.localDevices`,
    // and `localDevices` is now a `NamedDomainObjectContainer` (not
    // polymorphic), so we use the unqualified `create("name") { }` form.
    testOptions.managedDevices.localDevices {
        create("pixel6Api33") {
            device = "Pixel 6"
            apiLevel = 33
            systemImageSource = "aosp"
        }
    }
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.ext.junit)
}

baselineProfile {
    managedDevices += "pixel6Api33"
    useConnectedDevices = false
}

// Hardcoded to match `app/build.gradle.kts`'s `namespace = "com.calculator.app"`.
// Reading `project(":app").android.namespace` from a `com.android.test` module
// crashes the script with an `ApplicationExtension cannot be cast to
// TestExtension` ClassCastException on AGP 9.x (issuetracker 443311090
// related), and `androidComponents.namespace` isn't exposed on the test
// plugin's components extension. Keep this in sync if the appId ever changes.
val appId: String = "com.calculator.app"