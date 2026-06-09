// ===================================================================
// :shared 모듈 — Compose Multiplatform 공유 UI/모델 (농돌이 대리점 앱)
//
// AGP 9 부터 com.android.library + kotlin-multiplatform 호환 불가.
// com.android.kotlin.multiplatform.library 가 AGP 9 의 공식 KMP 전용 플러그인.
// DSL 도 다름 — kotlin.androidLibrary { ... } 블록 사용 (별도 android { } 블록 X).
//
// :app(Android, 현장 엔지니어) 과 :web(디스패처) 가 동일한 도메인 모델·디자인
// 토큰을 공유. 모델은 @Serializable 로 선언 → web JSON 브리지와 round-trip.
// ===================================================================

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidLibrary {
        namespace = "com.sangwolnongsan.nongdori.shared"
        compileSdk = 36
        minSdk = 24
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
