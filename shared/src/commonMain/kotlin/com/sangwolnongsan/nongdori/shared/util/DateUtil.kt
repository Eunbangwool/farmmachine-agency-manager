package com.sangwolnongsan.nongdori.shared.util

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** epoch millis → "yyyy-MM-dd". 0 이하이면 빈 문자열. */
fun formatDate(millis: Long): String {
    if (millis <= 0L) return ""
    val dt = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dt.year}-${pad(dt.monthNumber)}-${pad(dt.dayOfMonth)}"
}

/** epoch millis → "yyyy-MM-dd HH:mm". */
fun formatDateTime(millis: Long): String {
    if (millis <= 0L) return ""
    val dt = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dt.year}-${pad(dt.monthNumber)}-${pad(dt.dayOfMonth)} ${pad(dt.hour)}:${pad(dt.minute)}"
}

/**
 * 사람이 읽는 출장 번호: "yyMMdd-NNN" (날짜 + 랜덤 3자리).
 * 같은 날 같은 난수가 나와야만 충돌 — 기존 순수 4자리 난수보다 충돌 확률이 낮고,
 * 번호만 보고 접수일을 알 수 있다.
 */
fun generateOrderNo(nowMillis: Long): String {
    val dt = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${pad(dt.year % 100)}${pad(dt.monthNumber)}${pad(dt.dayOfMonth)}-${(100..999).random()}"
}

private fun pad(n: Int): String = if (n < 10) "0$n" else "$n"
