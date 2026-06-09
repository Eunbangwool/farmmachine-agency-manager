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

private fun pad(n: Int): String = if (n < 10) "0$n" else "$n"
