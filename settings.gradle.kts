pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    // PREFER_SETTINGS: settings 저장소를 우선 사용. Kotlin/WASM 플러그인이
    // 프로젝트 레벨에서 추가하려는 nodejs.org 저장소는 무시됨 → settings에서
    // org.nodejs / com.yarnpkg 그룹용 Ivy 저장소를 직접 선언해야 함.
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        // Node.js 바이너리 - Kotlin/WASM 빌드에 필수.
        // Ivy 레이아웃: nodejs.org/dist/v22.0.0/node-v22.0.0-linux-x64.tar.gz
        exclusiveContent {
            forRepository {
                ivy("https://nodejs.org/dist/") {
                    name = "Node Distributions"
                    patternLayout { artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]") }
                    metadataSources { artifact() }
                    content { includeModule("org.nodejs", "node") }
                }
            }
            filter { includeGroup("org.nodejs") }
        }
        // Yarn 바이너리 - Kotlin/JS 패키지 관리자.
        exclusiveContent {
            forRepository {
                ivy("https://github.com/yarnpkg/yarn/releases/download") {
                    name = "Yarn Distributions"
                    patternLayout { artifact("v[revision]/[artifact](-v[revision]).[ext]") }
                    metadataSources { artifact() }
                    content { includeModule("com.yarnpkg", "yarn") }
                }
            }
            filter { includeGroup("com.yarnpkg") }
        }
        // Binaryen - WASM 최적화 도구체인 (wasm-opt 등). Kotlin/WASM 프로덕션 빌드에 필요.
        // GitHub Releases 태그가 'version_NNN' 형식이라 패턴에 version_ 접두어 포함.
        exclusiveContent {
            forRepository {
                ivy("https://github.com/WebAssembly/binaryen/releases/download") {
                    name = "Binaryen Distributions"
                    patternLayout { artifact("version_[revision]/[artifact]-version_[revision]-[classifier].[ext]") }
                    metadataSources { artifact() }
                    content { includeModule("com.github.webassembly", "binaryen") }
                }
            }
            filter { includeGroup("com.github.webassembly") }
        }
    }
}

rootProject.name = "Nongdori"
include(":shared")
include(":app")
include(":web")
