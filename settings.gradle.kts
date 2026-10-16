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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Only the native engine is resolved here; customized Core modules keep their own source.
        exclusiveContent {
            forRepository {
                maven {
                    name = "OwnTVNative"
                    url = uri("https://ahxn00.github.io/OwnTV_Core/maven")
                }
            }
            filter { includeModule("tv.own.owntv", "libmpv") }
        }
    }
}

// Local and CI builds use the same customized Core inputs; no fallback to an old public artifact.
apply(from = "gradle/custom-core.settings.gradle.kts")

rootProject.name = "OwnTV"
include(":app")
// Baseline-profile generator (audit ST1). Test-only module: it ships nothing to users, it records
// the cold-start journey on a device and writes the profile :app packages.
include(":baselineprofile")
