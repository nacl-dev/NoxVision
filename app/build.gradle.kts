import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

val localProperties = Properties()
val localPropertiesFile: File = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

fun readSecret(name: String): String? =
    localProperties.getProperty(name)?.takeIf { it.isNotBlank() }
        ?: System.getenv(name)?.takeIf { it.isNotBlank() }

val uploadStoreFilePath = readSecret("UPLOAD_STORE_FILE")
val uploadStorePassword = readSecret("UPLOAD_STORE_PASSWORD")
val uploadKeyAlias = readSecret("UPLOAD_KEY_ALIAS")
val uploadKeyPassword = readSecret("UPLOAD_KEY_PASSWORD")
val hasUploadSigning = listOf(
    uploadStoreFilePath,
    uploadStorePassword,
    uploadKeyAlias,
    uploadKeyPassword
).all { !it.isNullOrBlank() }

android {
    namespace = "com.noxvision.app"
    compileSdk = 35  // Korrigiert!

    androidResources {
        noCompress += "tflite"
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }

    defaultConfig {
        applicationId = "com.noxvision.app"
        minSdk = 24
        targetSdk = 35  // Angepasst an compileSdk
        versionCode = 3
        versionName = "1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.clear()
            abiFilters.add("arm64-v8a")
        }

        // Set APK name
        base.archivesName.set("NoxVision-v${versionName}")

        val apiKey = readSecret("OPENWEATHER_API_KEY")
            ?.trim()
            ?.removeSurrounding("\"")
            ?: ""
        buildConfigField("String", "OPENWEATHER_API_KEY", "\"$apiKey\"")
    }

    signingConfigs {
        if (hasUploadSigning) {
            create("release") {
                storeFile = rootProject.file(uploadStoreFilePath!!)
                storePassword = uploadStorePassword
                keyAlias = uploadKeyAlias
                keyPassword = uploadKeyPassword
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        release {
            if (hasUploadSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    testImplementation(libs.json)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // AppCompat for per-app language preferences
    implementation(libs.androidx.appcompat)

    // LibVLC und andere
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.libvlc)
    implementation(libs.okhttp)
    implementation(libs.coil.compose)

    implementation(libs.tensorflow.lite)
    implementation(libs.billing.ktx)

    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // OSMDroid (Offline Maps)
    implementation(libs.osmdroid)

    // PDF Export
    implementation(libs.pdfbox.android)

    // Location Services
    implementation(libs.play.services.location)
}
