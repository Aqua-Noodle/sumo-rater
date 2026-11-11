plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.aquanoodle.sumorater"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aquanoodle.sumorater"
        minSdk = 26
        targetSdk = 35
        // Increases with every CI build so each APK installs as an update over the last.
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionName = "1.1.$versionCode"
    }

    // One fixed key for every build. Android only lets an APK update an installed
    // app (keeping its data) when both are signed with the same key; without it,
    // each CI machine signs with its own throwaway debug key.
    val sharedKeystore = rootProject.file("keystore/sumo-rater.jks")
    // CI sets this to "" (not unset) when no SIGNING_PASSWORD secret exists.
    val signingPassword = System.getenv("SIGNING_PASSWORD")?.takeIf { it.isNotBlank() } ?: "sumorater"
    signingConfigs {
        if (sharedKeystore.exists()) {
            create("shared") {
                storeFile = sharedKeystore
                storePassword = signingPassword
                keyAlias = "sumorater"
                keyPassword = signingPassword
            }
        }
    }

    buildTypes {
        debug {
            if (sharedKeystore.exists()) signingConfig = signingConfigs.getByName("shared")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (sharedKeystore.exists()) signingConfig = signingConfigs.getByName("shared")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.androidx.savedstate.ktx)
    debugImplementation(libs.androidx.ui.tooling)
}
