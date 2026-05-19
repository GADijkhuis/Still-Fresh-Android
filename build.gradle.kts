import java.util.Properties

plugins {
    id("com.android.application") version "8.4.0"
    kotlin("android") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
}

android {
    namespace = "com.stillfresh"
    compileSdk = 34
    buildFeatures.buildConfig = true

    defaultConfig {
        applicationId = "com.gadijkh.stillfresh"
        minSdk = 26
        targetSdk = 34
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
}

dependencies {
    implementation(platform("io.github.jan-tennert.supabase:bom:3.1.1"))
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:realtime-kt")

    implementation("io.ktor:ktor-client-android:3.0.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
