import java.util.Properties

import com.android.build.api.variant.FilterConfiguration

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

val keystoreProps = Properties().apply {
    val file = rootProject.file("keystore/keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.unanimetv.android"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.unanimetv.android"
        minSdk = 28
        targetSdk = 37
        versionCode = 6
        versionName = "1.0.5"
    }

    flavorDimensions += "env"
    productFlavors {
        create("dev") {
            dimension = "env"
        }
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("keystore/${keystoreProps.getProperty("storeFile", "unanime-release.jks")}")
            storePassword = keystoreProps.getProperty("storePassword")
            keyAlias = keystoreProps.getProperty("keyAlias")
            keyPassword = keystoreProps.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = false
        }
    }
}

androidComponents {
    onVariants(selector().withFlavor("env" to "dev").withBuildType("debug")) { variant ->
        variant.applicationId.set("com.unanimetv.android.dev")
        variant.outputs.forEach { output ->
            val abi = output.filters.firstOrNull { it.filterType == FilterConfiguration.FilterType.ABI }?.identifier
            output.outputFileName.set(
                if (abi != null) "UnAnime-dev-${variant.buildType}-$abi.apk" else "UnAnime-${variant.buildType}-dev.apk"
            )
        }
    }
}

dependencies {
    implementation(project(":NotAnimeES"))
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.datasource.okhttp)
    implementation(libs.media3.datasource)
    implementation(libs.media3.database)
    implementation(libs.media3.ui)
    implementation(libs.media3.session)
    implementation(libs.nextlib.media3ext)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.work.runtime)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
