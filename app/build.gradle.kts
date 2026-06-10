import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// google-services.json 이 존재할 때만 Firebase 플러그인 적용.
// → Firebase 프로젝트 설정 전이라도 빌드 가능. 사용자가 새 Firebase 프로젝트의
//   google-services.json 을 app/ 에 배치하면 자동으로 Firebase 활성화됨.
if (project.file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

// CI 빌드 번호 — versionCode 자동 증가용. 로컬 빌드는 1.
val ciRunNumber: Int = (System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()) ?: 1

// Google Sign-In Web Client ID 를 local.properties / GitHub Secrets 에서 읽어 주입.
val googleWebClientId: String = (project.findProperty("GOOGLE_WEB_CLIENT_ID") as? String)
    ?: Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }.getProperty("GOOGLE_WEB_CLIENT_ID", "")

// `-PdebugBuild=true` 로 빌드하면 라벨("농돌이 디버그")·versionName(-debug) 만 구분.
// applicationId 는 Firebase 등록 패키지(com.sanwolnongsan.farmmachineagency) 와
// 일치해야 google-services 가 매칭되므로 .debug suffix 는 붙이지 않는다.
val debugBuild: Boolean = (project.findProperty("debugBuild") as? String)?.toBoolean() ?: false

android {
    namespace = "com.sangwolnongsan.nongdori"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.sanwolnongsan.farmmachineagency"
        minSdk = 24
        targetSdk = 36
        versionCode = ciRunNumber
        versionName = "0.1.$ciRunNumber"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 빌드 시간 (epoch ms) — 설정 화면 진단용.
        buildConfigField("long", "BUILD_TIME_MS", "${System.currentTimeMillis()}L")
        buildConfigField("boolean", "IS_DEBUG_APP", debugBuild.toString())
        // Google Sign-In Web Client ID
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${googleWebClientId}\"")
        // 운영자(대표) 대리점 코드 — 이 코드의 사용자는 OWNER 로 자동 bootstrap.
        val ownerDealerCode = (project.findProperty("ownerDealerCode") as? String) ?: ""
        buildConfigField("String", "OWNER_DEALER_CODE", "\"${ownerDealerCode}\"")
        // 운영자(admin) 이메일 — owner 멤버 bootstrap / 가입 요청 승인 권한.
        val adminEmail = (project.findProperty("adminEmail") as? String) ?: ""
        buildConfigField("String", "ADMIN_EMAIL", "\"${adminEmail}\"")

        // APK 용량 최적화: 한국어/영어 locale 만.
        resourceConfigurations += listOf("ko", "en")

        // app_name 기본값 (운영 빌드). 디버그 빌드는 buildType 에서 덮어씀.
        resValue("string", "app_name", "농돌이")
    }

    // CI 빌드마다 같은 keystore 사용 → 새 APK 를 기존 앱 위에 덮어 설치 가능.
    signingConfigs {
        getByName("debug") {
            val stable = file("../debug.keystore")
            if (stable.exists()) {
                storeFile = stable
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
            if (debugBuild) {
                versionNameSuffix = "-debug"
                resValue("string", "app_name", "농돌이 디버그")
            }
        }
        release {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (debugBuild) {
                versionNameSuffix = "-debug"
                resValue("string", "app_name", "농돌이 디버그")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    lint {
        disable.add("InvalidFragmentVersionForActivityResult")
        abortOnError = false
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }
}

dependencies {
    // :shared — Compose Multiplatform 공유 모듈 (도메인 모델 + 디자인 토큰).
    implementation(project(":shared"))
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    // Google Sign-In via Credential Manager (최신 권장 방식)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    // 엔지니어 라이브 위치 추적용
    implementation(libs.play.services.location)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
