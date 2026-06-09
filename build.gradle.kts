// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    // :shared 가 사용. AGP 9 부터 KMP 와 com.android.library 호환 불가.
    // com.android.kotlin.multiplatform.library 가 AGP 9 의 공식 KMP 전용 플러그인.
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.services) apply false
    // :web 멀티플랫폼 모듈용. 루트에 apply false로 선언해야
    // :app의 kotlin.compose가 buildscript classpath에 올린 kotlin-gradle-plugin과
    // 충돌하지 않고 버전이 명시적으로 등록됨.
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.multiplatform) apply false
}
