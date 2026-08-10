import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

// Release signing credentials are read from keystore.properties (git-ignored) so no
// passwords live in tracked files. If the file is absent (e.g. debug-only work or CI
// without secrets), the release signingConfig is simply not created and release builds
// fall back to unsigned — nothing breaks.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

android {
    namespace = "com.devbytes.vixplayer.app"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.devbytes.vixplayer.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 58
        versionName = "3.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // Signed only when keystore.properties is present; otherwise unsigned.
            signingConfig = signingConfigs.findByName("release")
            optimization {
                enable = false
            }
            // Bundle native debug symbols (mmkv/BouncyCastle/etc.) so Play can symbolicate
            // native crashes/ANRs. Lives in bundle metadata — does not affect user download size.
            ndk {
                debugSymbolLevel = "FULL"
            }
            configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                // Minify is disabled, so there is no R8 mapping file to upload.
                mappingFileUploadEnabled = false
                // Native crashes are captured by firebase-crashlytics-ndk. Symbol upload for
                // precise native symbolication can be turned on later (it adds a network step to
                // release assembly); Play already receives FULL debug symbols for Android vitals.
                nativeSymbolUploadEnabled = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // SMBJ uses java.time / java.nio APIs that need desugaring below API 26.
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.exoplayer.rtsp)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DataStore
    implementation(libs.datastore.preferences)

    // MMKV
    implementation(libs.mmkv)

    // Lifecycle
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)

    // Splash screen
    implementation(libs.core.splashscreen)

    // Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.video)

    // Ads (AdMob) + EEA consent
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)

    // Play In-App Review (post-positive-signal review prompt)
    implementation(libs.play.review)
    implementation(libs.play.review.ktx)

    // Firebase Crashlytics — crash, ANR, and native-crash reporting
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.crashlytics.ndk)

    // WorkManager + Hilt worker injection (Library Pulse notifications)
    implementation(libs.work.runtime.ktx)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Network shares (SMB + FTP)
    implementation(libs.smbj)
    implementation(libs.commons.net)
    runtimeOnly(libs.slf4j.nop)
    implementation(libs.security.crypto)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}