// ===================================================================
// :web 모듈 — Compose Multiplatform Web (Kotlin/WASM)
//
// 농돌이 대리점 웹 앱 — 사무실 디스패처용 (출장 생성·배차 보드·고객·이력).
// 브라우저에서 동작하며 PWA로 설치 가능. Android 앱(:app, 현장 엔지니어)과는 별개 모듈.
// ===================================================================

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "nongdori-web"
        browser {
            val rootDirPath = project.rootDir.path
            val projectDirPath = project.projectDir.path
            commonWebpackConfig {
                outputFileName = "nongdori-web.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    static = (static ?: mutableListOf()).apply {
                        // 정적 리소스 디렉터리 (index.html 등)
                        add(rootDirPath)
                        add(projectDirPath)
                    }
                }
            }
        }
        // browser 실행 가능 바이너리
        binaries.executable()
    }

    sourceSets {
        val wasmJsMain by getting {
            dependencies {
                // Compose MP 1.10.x: 전통적 accessor 패턴 정상 동작.
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(libs.kotlinx.serialization.json)

                // :shared — commonMain UI/모델 공유 모듈. Android(:app) 와 동일한 디자인 토큰·
                // 도메인 모델 사용을 위해 web 도 의존.
                implementation(project(":shared"))
            }
        }
    }
}

// Compose compiler 메타데이터 — Kotlin 2.0+ 표준 설정
compose.resources {
    publicResClass = false
    packageOfResClass = "com.sangwolnongsan.nongdori.web.resources"
    generateResClass = always
}
