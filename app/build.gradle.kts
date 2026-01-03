plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "cn.mapleisle.osaka"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "cn.mapleisle.osaka"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a")
        }
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
        freeCompilerArgs += listOf("-Xuse-fir-lt=false")
    }
    buildFeatures {
        compose = true
    }

    composeOptions {
        // ✅ 必须加上这一行！这是 Kotlin 1.9.24 对应的 Compose 编译器版本
        kotlinCompilerExtensionVersion = "1.5.14"
    }

}

dependencies {
    // 1. 基础 UI 和 Compat (解决 ViewTree 问题)
    implementation("androidx.appcompat:appcompat:1.6.1") // ⚠️ 必须加这个！很多 ViewTree 逻辑在这里
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // 2. Lifecycle (降级到 2.6.2，最稳)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-service:2.6.2")

    // 3. SavedState (核心库，版本要新)
    implementation("androidx.savedstate:savedstate-ktx:1.2.1")

    // 4. Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")

    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("io.noties.markwon:core:4.6.2")

    // 5. 网络和多媒体
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.arthenica:ffmpeg-kit-full-gpl:6.0")

    // 测试库 (保持默认)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}