plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "tv.own.owntv.baselineprofile"
    compileSdk {
        version = release(37)
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        // Same reason as :app — androidx.benchmark's prebuilt .so are already stripped, so the strip
        // step only ever printed "Unable to strip …" without changing a byte.
        jniLibs {
            keepDebugSymbols += "**/*.so"
        }
    }

    defaultConfig {
        // Macrobenchmark needs API 28+ to read the compilation state it drives.
        minSdk = 28
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val benchmarkAbi = providers.gradleProperty("owntv.benchmarkAbi").orElse("x86_64").get()
        require(benchmarkAbi in setOf("standard", "x86_64")) { "Use standard for ARM TV boxes or x86_64 for the emulator." }
        missingDimensionStrategy("abi", benchmarkAbi)
    }

    // Default profile collection stays on the x86_64 emulator. UI benchmarks on ARM TV boxes use
    // -Powntv.benchmarkAbi=standard; the selected target must match the connected device.
    // `mergeIntoMain` still makes the collected profile available to every app ABI.
    targetProjectPath = ":app"
}

// Run on a single connected device/emulator. Set `useConnectedDevices = false` and add a managed
// device here if this ever needs to run unattended in CI.
baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.junit)
    implementation(libs.androidx.espresso.core)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
