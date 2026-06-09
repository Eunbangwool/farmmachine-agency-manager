package com.sangwolnongsan.nongdori.shared.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// ============================================================
// 농돌이(대리점) 컬러 팔레트 — 농기계 정체성의 차콜/그레이 톤.
// commonMain — Android(:app, 엔지니어) 와 Web(:web, 디스패처) 양쪽 공용.
//
// 다크모드: MaterialTheme.colorScheme.background 의 luminance 로 자동 판단.
//   NongdoriTheme(mode) 가 light/dark colorScheme 을 set 하면 토큰 자동 분기.
// ============================================================

@Composable
@ReadOnlyComposable
private fun isAppDark(): Boolean =
    MaterialTheme.colorScheme.background.luminance() < 0.5f

@Composable
@ReadOnlyComposable
private fun pick(light: Color, dark: Color): Color =
    if (isAppDark()) dark else light

// ============================================================
// 브랜드 컬러 — 다크 분기 안 함
// ============================================================
val Forest = Color(0xFF2A2826)
val Primary = Color(0xFF555350)
val Fresh = Color(0xFF92908A)
val Tint = Color(0xFFEFEEE6)

// ============================================================
// 라이트 / 다크 raw 정의 (private)
// ============================================================
private val LightSurfacePrimary = Color(0xFFFAF9F4)
private val LightSurfaceSecondary = Color(0xFFEFEEE6)
private val LightTextPrimary = Color(0xFF1F2421)
private val LightTextSecondary = Color(0xFF6B7068)
private val LightTextTertiary = Color(0xFFA8AAA5)
private val LightBorderColor = Color(0xFFDDD9CC)
private val LightActionPrimary = Color(0xFF555350)
private val LightActionPrimaryText = Color(0xFFFAF9F4)
private val LightActionDanger = Color(0xFF8C3A33)

private val DarkSurfacePrimary = Color(0xFF1B1A19)
private val DarkSurfaceSecondary = Color(0xFF121110)
private val DarkTextPrimary = Color(0xFFE8E6DE)
private val DarkTextSecondary = Color(0xFFB4B8B0)
private val DarkTextTertiary = Color(0xFF7A7D75)
private val DarkBorderColor = Color(0xFF2E2D2B)
private val DarkActionPrimary = Color(0xFFAAA8A2)
private val DarkActionPrimaryText = Color(0xFF121110)
private val DarkActionDanger = Color(0xFFD68077)

// ============================================================
// 공개 토큰 — @Composable getter (다크 자동 분기)
// ============================================================
val SurfacePrimary: Color
    @Composable @ReadOnlyComposable
    get() = pick(LightSurfacePrimary, DarkSurfacePrimary)
val SurfaceSecondary: Color
    @Composable @ReadOnlyComposable
    get() = pick(LightSurfaceSecondary, DarkSurfaceSecondary)

val TextPrimary: Color
    @Composable @ReadOnlyComposable
    get() = pick(LightTextPrimary, DarkTextPrimary)
val TextSecondary: Color
    @Composable @ReadOnlyComposable
    get() = pick(LightTextSecondary, DarkTextSecondary)
val TextTertiary: Color
    @Composable @ReadOnlyComposable
    get() = pick(LightTextTertiary, DarkTextTertiary)

val BorderColor: Color
    @Composable @ReadOnlyComposable
    get() = pick(LightBorderColor, DarkBorderColor)

val ActionPrimary: Color
    @Composable @ReadOnlyComposable
    get() = pick(LightActionPrimary, DarkActionPrimary)
val ActionPrimaryText: Color
    @Composable @ReadOnlyComposable
    get() = pick(LightActionPrimaryText, DarkActionPrimaryText)
val ActionDanger: Color
    @Composable @ReadOnlyComposable
    get() = pick(LightActionDanger, DarkActionDanger)

// ============================================================
// 수리 상태(RepairStatus) accent — 의미 색이므로 다크 분기 안 함.
// 접수/배정/출동/진행중/완료/취소 6단계에 대응.
// ============================================================
val StatusReceived = Color(0xFF757575)   // 접수 — 중립 그레이
val StatusScheduled = Color(0xFF5C7CB0)  // 배정 — 블루
val StatusDispatched = Color(0xFFCC8B27) // 출동 — 앰버
val StatusInProgress = Color(0xFFE69138) // 진행중 — 오렌지
val StatusDone = Color(0xFF6AA84F)       // 완료 — 그린
val StatusCanceled = Color(0xFFB54B40)   // 취소 — 레드

// 우선순위
val PriorityUrgent = Color(0xFFB54B40)   // 긴급
val PriorityNormal = Color(0xFF92908A)   // 일반
