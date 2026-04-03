plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")

}
val baseUrl: String =
    project.findProperty("BASE_URL") as? String
        ?: error("BASE_URL is missing in local.properties")

val razorpayKeyId: String =
    project.findProperty("RAZORPAY_KEY_ID") as? String
        ?: error("RAZORPAY_KEY_ID is missing in gradle.properties")
android {
    namespace = "com.docuvio.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.docuvio.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 5
        versionName = "1.0.3"

        buildConfigField(
            "String",
            "BASE_URL",
            "\"$baseUrl\""
        )
        buildConfigField(
            "String",
            "RAZORPAY_KEY_ID",
            "\"$razorpayKeyId\""
        )
        buildConfigField(
            "String",
            "CONVERTER_URL",
            "\"${project.findProperty("CONVERTER_URL")}\""
        )

        buildConfigField(
            "String",
            "CONVERTER_API_KEY",
            "\"${project.findProperty("CONVERTER_API_KEY")}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.material3)
    // -------------------------
    // UNIT TESTS
    // -------------------------
    testImplementation("junit:junit:4.13.2")

    // ===============================
    // COMPOSE BOM
    // ===============================
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))

    // ===============================
    // FORCE MATERIAL3 1.3+
    // ===============================
    implementation("androidx.compose.material3:material3:1.3.0")

    // ===============================
    // COMPOSE CORE
    // ===============================
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-extended")

    debugImplementation("androidx.compose.ui:ui-tooling")

    // ===============================
    // ACTIVITY + NAVIGATION
    // ===============================
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.navigation:navigation-compose:2.8.0")

    // ===============================
    // LIFECYCLE
    // ===============================
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")

    // ===============================
    // NETWORKING
    // ===============================
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // ===============================
    // DATASTORE
    // ===============================
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // ===============================
    // IMAGE LOADING
    // ===============================
    implementation("io.coil-kt:coil-compose:2.6.0")

    // ===============================
    // PAYMENTS
    // ===============================
    implementation("com.razorpay:checkout:1.6.33")

    // ===============================
    // CORE
    // ===============================
    implementation("androidx.core:core-ktx:1.13.1")

    // ===============================
    // OPTIONAL
    // ===============================
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")

    //Animation
    implementation("com.google.accompanist:accompanist-navigation-animation:0.34.0")

    //Top Notification
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.34.0")

    // -------------------------
    // ANDROID INSTRUMENTATION TESTS
    // -------------------------
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

// ===============================
// FIREBASE (FCM)
// ===============================
    implementation(platform("com.google.firebase:firebase-bom:32.8.0"))
    implementation("com.google.firebase:firebase-messaging")



    implementation("androidx.datastore:datastore-preferences:1.1.1")

// ===============================
// Play Store API
// ===============================
    implementation("com.google.android.play:app-update:2.1.0")
    implementation("com.google.android.play:app-update-ktx:2.1.0")
    testImplementation(kotlin("test"))
}
