plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "kelompok.satu.raya_kitchen_mobile"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "kelompok.satu.raya_kitchen_mobile"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // VOlley & JSON Parser
    implementation("com.android.volley:volley:1.2.1")
    implementation("com.google.code.gson:gson:2.10.1")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-inappmessaging-display-ktx")
    implementation("com.google.firebase:firebase-messaging:23.3.1")

    // Supabase
    implementation(platform("io.github.jan-tennert.supabase:bom:2.1.3"))
    implementation("io.github.jan-tennert.supabase:storage-kt")
    implementation("io.ktor:ktor-client-android:2.3.7")
    implementation(libs.firebase.firestore)

    // Pilih Maps / lokasi
    implementation("org.osmdroid:osmdroid-android:6.1.18")

    // Buat Ambil koordinat di maps
    implementation("com.github.mumayank:AirLocation:2.5.2")

    // Glide buat gambar
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // QR Code
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // Swipe to Refresh
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}