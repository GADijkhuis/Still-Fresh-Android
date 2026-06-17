import java.util.Properties

plugins {
    id("com.android.application") version "9.2.1"
    kotlin("android") version "2.2.10"
    kotlin("plugin.serialization") version "2.1.10"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"
}

android {
    namespace = "com.stillfresh"
    compileSdk = 37
    buildFeatures.buildConfig = true

    defaultConfig {
        applicationId = "com.gadijkh.stillfresh"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        val properties = Properties()
        properties.load(project.rootProject.file("local.properties").inputStream())

        buildConfigField(
            type = "String",
            name = "SUPABASE_URL",
            value = properties.getProperty("SUPABASE_URL") ?: ""
        )

        buildConfigField(
            type = "String",
            name = "SUPABASE_API_KEY",
            value = properties.getProperty("SUPABASE_API_KEY") ?: ""
        )

        buildConfigField(
            type = "String",
            name = "OPENFOODFACTS_API_URL",
            value = properties.getProperty("OPENFOODFACTS_API_URL") ?: ""
        )

        buildConfigField(
            type = "String",
            name = "OPENFOODFACTS_SEARCH_URL",
            value = properties.getProperty("OPENFOODFACTS_SEARCH_URL") ?: ""
        )

        buildConfigField(
            type = "String",
            name = "OPENFOODFACTS_API_USER",
            value = properties.getProperty("OPENFOODFACTS_API_USER") ?: ""
        )

        buildConfigField(
            type = "String",
            name = "OPENFOODFACTS_API_PASS",
            value = properties.getProperty("OPENFOODFACTS_API_PASS") ?: ""
        )

        buildConfigField(
            type = "String",
            name = "OPENFOODFACTS_API_MAIL",
            value = properties.getProperty("OPENFOODFACTS_API_MAIL") ?: ""
        )
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform("io.github.jan-tennert.supabase:bom:3.6.0"))
    implementation("androidx.activity:activity-ktx:1.13.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("com.google.android.material:material:1.14.0")
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:realtime-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")

    implementation("io.ktor:ktor-client-android:3.5.0")
    implementation("io.ktor:ktor-client-auth:3.5.0")
    implementation("io.ktor:ktor-client-content-negotiation:3.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")

    implementation(platform("androidx.compose:compose-bom:2024.02.02"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.mindrot:jbcrypt:0.4")
    implementation("io.coil-kt:coil-compose:2.6.0")

    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    implementation("androidx.camera:camera-camera2:1.6.1")
    implementation("androidx.camera:camera-lifecycle:1.6.1")
    implementation("androidx.camera:camera-view:1.6.1")

    implementation("io.coil-kt.coil3:coil-compose:3.4.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.4.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
