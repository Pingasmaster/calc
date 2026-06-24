pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

// Adopt Gradle 9.6.0 feature previews:
// - NO_IMPLICIT_LOOKUP_IN_PARENT_PROJECTS: forces explicit providers.gradleProperty
//   accessors; removes silent findProperty/property/hasProperty coupling to rootProject.
// - STABLE_CONFIGURATION_CACHE: opt in to the stable CC contract ahead of Gradle 10.
enableFeaturePreview("NO_IMPLICIT_LOOKUP_IN_PARENT_PROJECTS")
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

plugins {
    // Required since Gradle 8.0 to enable JDK auto-provisioning for toolchains.
    // Verified to remain enforced in 9.6.0.
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs")
    }
}

rootProject.name = "Calculator"
include(":app")
