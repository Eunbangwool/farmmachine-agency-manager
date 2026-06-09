package com.sangwolnongsan.nongdori.data

import android.content.Context

/**
 * 현재 선택된 대리점 코드를 로컬(SharedPreferences)에 보관.
 * 로그인 후 가입/생성한 대리점 코드를 저장하고, 재실행 시 자동 진입에 사용.
 */
class DealerCodeManager(context: Context) {
    private val prefs = context.getSharedPreferences("nongdori_prefs", Context.MODE_PRIVATE)

    var dealerCode: String?
        get() = prefs.getString(KEY_DEALER_CODE, null)
        set(value) = prefs.edit().apply {
            if (value.isNullOrBlank()) remove(KEY_DEALER_CODE) else putString(KEY_DEALER_CODE, value)
        }.apply()

    fun clearCode() {
        prefs.edit().remove(KEY_DEALER_CODE).apply()
    }

    companion object {
        private const val KEY_DEALER_CODE = "dealer_code"
    }
}
