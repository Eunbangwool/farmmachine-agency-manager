package com.sangwolnongsan.nongdori.shared.util

/**
 * 현재 시각 epoch milliseconds.
 * commonMain 데이터 모델의 createdAt/timestamp 기본값으로 사용.
 * Android: System.currentTimeMillis() / Web: JS Date.now()
 */
expect fun nowMs(): Long
