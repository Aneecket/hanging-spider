import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.hangingspider.game"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hangingspider.game"
        minSdk = 24
        targetSdk = 36
        versionCode = 6
        versionName = "0.3.0"
        vectorDrawables { useSupportLibrary = true }
    }

    // Release signing reads from local.properties (never commit these values):
    //   releaseStoreFile=/absolute/path/to/hanging-spider.jks
    //   releaseStorePassword=...
    //   releaseKeyAlias=...
    //   releaseKeyPassword=...
    val localProps = Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) FileInputStream(f).use { load(it) }
    }
    val releaseStoreFile = localProps.getProperty("releaseStoreFile")
    signingConfigs {
        if (!releaseStoreFile.isNullOrBlank() && file(releaseStoreFile).exists()) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = localProps.getProperty("releaseStorePassword")
                keyAlias = localProps.getProperty("releaseKeyAlias")
                keyPassword = localProps.getProperty("releaseKeyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (signingConfigs.findByName("release") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.02")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.navigation:navigation-compose:2.8.0")
    implementation("androidx.core:core-splashscreen:1.0.1")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Google Sign-In (Credential Manager)
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // AdMob
    implementation("com.google.android.gms:play-services-ads:23.3.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
