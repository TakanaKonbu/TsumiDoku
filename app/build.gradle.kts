plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp") // 追加
    alias(libs.plugins.hilt.android.gradle) // ← Hiltプラグインを適用
}

android {
    namespace = "com.takanakonbu.tsumidoku"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.takanakonbu.tsumidoku"
        minSdk = 28
        targetSdk = 35
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
    }
}

dependencies {
    implementation(libs.androidx.material.icons.extended)
    // 追加
    val room_version = "2.6.1" // 安定版を指定
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version") // KTX を追加
    ksp("androidx.room:room-compiler:$room_version") // バージョンを統一

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler) // Hiltのコード生成もKSPで行う
    // Compose Navigation で Hilt ViewModel を使う場合に必要
    implementation(libs.androidx.hilt.navigation.compose)

    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0") // バージョンは適宜確認

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}