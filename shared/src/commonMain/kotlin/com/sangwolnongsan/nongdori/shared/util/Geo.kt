package com.sangwolnongsan.nongdori.shared.util

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** 두 좌표 간 직선(haversine) 거리 km. */
fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val r = 6371.0
    val dLat = toRad(lat2 - lat1)
    val dLng = toRad(lng2 - lng1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(toRad(lat1)) * cos(toRad(lat2)) * sin(dLng / 2) * sin(dLng / 2)
    return r * 2 * atan2(sqrt(a), sqrt(1 - a))
}

/** 직선거리 기반 단순 ETA(분). 평균 시속 km/h 가정. */
fun straightLineEtaMinutes(distanceKm: Double, avgSpeedKmh: Double = 40.0): Int =
    if (distanceKm <= 0) 0 else ((distanceKm / avgSpeedKmh) * 60).toInt()

private fun toRad(deg: Double): Double = deg * 0.017453292519943295
