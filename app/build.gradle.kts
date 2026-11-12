plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.tvhomebridge"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.tvhomebridge"
        minSdk = 21          // Android TV 5.0+
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // 사이드로딩용: 디버그 키로 서명해서 바로 설치 가능하게 함
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
// 외부 라이브러리 의존성 없음 (순수 Android SDK만 사용)
