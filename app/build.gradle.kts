import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

/** Debug key lives OUTSIDE the repo (never commit): ../prayer-times-keystore/signing.properties */
fun loadProps(path: String): Properties? = rootProject.file(path).takeIf { it.isFile }
    ?.let { f -> Properties().apply { f.inputStream().use { load(it) } } }
val signingProps: Properties? = loadProps("../prayer-times-keystore/signing.properties")
/** Release (Play upload) key, also outside the repo: ../prayer-times-release-keystore/signing.properties */
val releaseProps: Properties? = loadProps("../prayer-times-release-keystore/signing.properties")

android {
    namespace = "com.midroid.prayertimes"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.midroid.prayertimes"
        minSdk = 26
        targetSdk = 35
        versionCode = 13
        versionName = "1.1.1"
    }
    signingConfigs {
        if (signingProps != null) create("app") {
            storeFile = file(signingProps.getProperty("storeFile"))
            storePassword = signingProps.getProperty("storePassword")
            keyAlias = signingProps.getProperty("keyAlias")
            keyPassword = signingProps.getProperty("keyPassword")
        }
        if (releaseProps != null) create("release") {
            storeFile = file(releaseProps.getProperty("storeFile"))
            storePassword = releaseProps.getProperty("storePassword")
            keyAlias = releaseProps.getProperty("keyAlias")
            keyPassword = releaseProps.getProperty("keyPassword")
        }
    }
    buildTypes {
        debug { signingConfigs.findByName("app")?.let { signingConfig = it } }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
            if (releaseProps == null) logger.warn("Mi5G Prayer: no release signing.properties - release output will be UNSIGNED")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true; buildConfig = true }
    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
        unitTests.all { it.maxHeapSize = "1536m" }
    }
}
kotlin { jvmToolchain(17) }

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons)
    implementation(libs.adhan)
    implementation(libs.androidx.appcompat)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.ui.test.junit4)
}
