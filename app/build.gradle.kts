plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.learning.a2st"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.learning.a2st"
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
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)

    // 引入 OkHttp 网络请求库
    implementation(libs.okhttp)

    // 添加 Retrofit 核心库
    implementation(libs.retrofit)
    // 添加 Gson 转换器（用于 JSON 解析）
    implementation(libs.retrofit.converter.gson)
    // 添加 Gson 依赖
    implementation(libs.gson)
    // 引入 RxJava3 及 Retrofit 的 RxJava3 适配器
    implementation(libs.rxjava3)
    implementation(libs.rxandroid)
    implementation(libs.retrofit.adapter.rxjava3)

    // Kotlin
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)

//    // 使用 EncryptedSharedPreferences
//    implementation(libs.security.crypto)
}

