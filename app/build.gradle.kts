import java.util.Properties
import java.io.FileInputStream

// Load local properties (sensitive keys) if present
val localPropsFile = rootProject.file("local.properties")
val localProps = Properties()
if (localPropsFile.exists()) {
    FileInputStream(localPropsFile).use { fis ->
        localProps.load(fis)
    }
}

fun prop(key: String, default: String = ""): String = localProps.getProperty(key) ?: default

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.devsapiens.phonemagic"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.devsapiens.phonemagic"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "adHomeBanner", "\"${prop("adHomeBannerPro", "")}\"")
            buildConfigField("String", "entryScreenIntersticial", "\"${prop("entryScreenIntersticialPro", "")}\"")
            manifestPlaceholders["adUnitIdMain"] = prop("adUnitIdMainPro", "")
        }
        debug {
            buildConfigField("String", "adHomeBanner", "\"${prop("adHomeBanner", "")}\"")
            buildConfigField("String", "entryScreenIntersticial", "\"${prop("entryScreenIntersticial", "")}\"")
            manifestPlaceholders["adUnitIdMain"] = prop("adUnitIdMain", "")
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
    implementation(libs.coil.compose)
    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.navigation.compose.v260)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(libs.androidx.animation)
    implementation(libs.androidx.splashScren)
    implementation(libs.play.services.ads)
}