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
    compileSdk = 36

    defaultConfig {
        applicationId = "com.docuvio.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 8
        versionName = "1.1.0"

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
    implementation(platform(libs.androidx.compose.bom))

    // ===============================
    // FORCE MATERIAL3 1.3+
    // ===============================
    implementation(libs.androidx.material3)

    // ===============================
    // COMPOSE CORE
    // ===============================
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation("androidx.compose.material:material-icons-extended")

    debugImplementation(libs.androidx.compose.ui.tooling)

    // ===============================
    // ACTIVITY + NAVIGATION
    // ===============================
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    // ===============================
    // LIFECYCLE
    // ===============================
    implementation(libs.androidx.lifecycle.runtime.ktx)
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
    implementation(libs.razorpay.checkout)

    // ===============================
    // CORE
    // ===============================
    implementation(libs.androidx.core.ktx)

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
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

// ===============================
// FIREBASE (FCM)
// ===============================
    implementation(platform(libs.firebase.bom))
    implementation("com.google.firebase:firebase-messaging")



    implementation("androidx.datastore:datastore-preferences:1.1.1")

// ===============================
// Play Store API
// ===============================
    implementation("com.google.android.play:app-update:2.1.0")
    implementation("com.google.android.play:app-update-ktx:2.1.0")
    testImplementation(kotlin("test"))
}
