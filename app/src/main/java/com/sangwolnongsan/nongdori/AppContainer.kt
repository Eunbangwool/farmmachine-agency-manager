package com.sangwolnongsan.nongdori

import android.content.Context
import com.sangwolnongsan.nongdori.auth.UserManager
import com.sangwolnongsan.nongdori.data.DealerCodeManager
import com.sangwolnongsan.nongdori.data.FirebaseAvailability

/**
 * 서비스 로케이터 — UserManager / DealerCodeManager / Firebase 가용성.
 * Repository(WorkOrder/Customer/Machine) 는 다음 단계에서 추가.
 */
object AppContainer {

    private lateinit var _userManager: UserManager
    val userManager: UserManager get() = _userManager

    lateinit var dealerCodeManager: DealerCodeManager
        private set

    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        FirebaseAvailability.check(context)
        dealerCodeManager = DealerCodeManager(context)
        _userManager = UserManager(context)
    }

    val isFirebaseReady: Boolean get() = FirebaseAvailability.isAvailable
}
