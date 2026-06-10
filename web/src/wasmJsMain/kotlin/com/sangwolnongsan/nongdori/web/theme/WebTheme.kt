package com.sangwolnongsan.nongdori.web.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sangwolnongsan.nongdori.web.resources.Res
// pretendard_{regular,bold} 는 Compose Resources 가 생성한 top-level extension property
// (Res.font.pretendard_X). 명시적 import 필요 — 없으면 'Unresolved reference' 컴파일 에러.
import com.sangwolnongsan.nongdori.web.resources.pretendard_bold
import com.sangwolnongsan.nongdori.web.resources.pretendard_regular
import org.jetbrains.compose.resources.Font

/**
 * Pretendard 한글 폰트 FontFamily.
 * composeResources/font/pretendard_{regular,bold}.otf 를 Compose Resources 의
 * Font(resource = Res.font.X) 로 로드. Res 는 publicResClass=false (internal) 라
 * 같은 :web 모듈 안에서만 접근 — 본 파일 동일 모듈.
 */
@Composable
fun pretendardFamily(): FontFamily = FontFamily(
    Font(Res.font.pretendard_regular, FontWeight.Normal),
    Font(Res.font.pretendard_bold, FontWeight.Bold),
)

@Composable
fun pretendardTypography(): Typography {
    val ff = pretendardFamily()
    return Typography(
        displayLarge = TextStyle(fontFamily = ff, fontSize = 40.sp, fontWeight = FontWeight.Bold),
        headlineLarge = TextStyle(fontFamily = ff, fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
        headlineMedium = TextStyle(fontFamily = ff, fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
        titleLarge = TextStyle(fontFamily = ff, fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
        titleMedium = TextStyle(fontFamily = ff, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
        titleSmall = TextStyle(fontFamily = ff, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = TextStyle(fontFamily = ff, fontSize = 16.sp),
        bodyMedium = TextStyle(fontFamily = ff, fontSize = 14.sp),
        bodySmall = TextStyle(fontFamily = ff, fontSize = 12.sp),
        labelLarge = TextStyle(fontFamily = ff, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
        labelMedium = TextStyle(fontFamily = ff, fontSize = 12.sp, fontWeight = FontWeight.Medium),
        labelSmall = TextStyle(fontFamily = ff, fontSize = 11.sp),
    )
}

/**
 * Pretendard 적용 래퍼. NongdoriTheme(색상) 안쪽에서 호출 — 부모 colorScheme/shapes 는
 * 그대로 상속하고 typography 만 Pretendard 로 override.
 *
 * 핵심: typography 만 바꾸면 style 미지정 Material3 Text 가 LocalTextStyle.current
 * (fontFamily 없는 기본값) 로 폴백 → 한글 깨짐. 그래서 LocalTextStyle 도
 * CompositionLocalProvider 로 명시 제공 → 모든 Text 가 Pretendard 로 렌더.
 */
@Composable
fun PretendardWrapper(content: @Composable () -> Unit) {
    val ff = pretendardFamily()
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme,
        typography = pretendardTypography(),
        shapes = MaterialTheme.shapes,
    ) {
        CompositionLocalProvider(LocalTextStyle provides TextStyle(fontFamily = ff)) {
            content()
        }
    }
}
