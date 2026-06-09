package com.sangwolnongsan.nongdori.service

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.sangwolnongsan.nongdori.AppContainer
import com.sangwolnongsan.nongdori.data.location.EngineerLocationRepository
import com.sangwolnongsan.nongdori.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 출동 중 엔지니어 위치 공유 Foreground Service.
 * 엔지니어가 출장을 '출동(DISPATCHED)' 으로 전환하면 시작, '완료' 시 중지.
 * 10초 간격 GPS → dealerships/{code}/liveLocations/{uid} 업로드.
 */
class LocationTrackingService : Service() {

    private lateinit var fusedClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopTracking(); stopSelf(); return START_NOT_STICKY
        }
        startTracking()
        return START_STICKY
    }

    private fun startTracking() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "위치 권한 없음 - 중지"); stopSelf(); return
        }
        ServiceCompat.startForeground(
            this,
            NotificationHelper.NOTIF_ID_TRACKING,
            NotificationHelper.buildTrackingNotification(this, "현재 위치를 대리점과 공유 중"),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
        )
        val cb = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val code = AppContainer.dealerCodeManager.dealerCode.orEmpty()
                if (code.isNotBlank()) {
                    scope.launch {
                        EngineerLocationRepository.upload(code, loc.latitude, loc.longitude, loc.accuracy.toDouble())
                    }
                }
            }
        }
        locationCallback = cb
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10_000L)
            .setMinUpdateIntervalMillis(5_000L).build()
        try {
            fusedClient.requestLocationUpdates(request, cb, Looper.getMainLooper())
            Log.i(TAG, "tracking started")
        } catch (e: SecurityException) {
            Log.e(TAG, "requestLocationUpdates 실패", e); stopSelf()
        }
    }

    private fun stopTracking() {
        locationCallback?.let { runCatching { fusedClient.removeLocationUpdates(it) } }
        val code = AppContainer.dealerCodeManager.dealerCode.orEmpty()
        if (code.isNotBlank()) scope.launch { EngineerLocationRepository.clear(code) }
        Log.i(TAG, "tracking stopped")
    }

    override fun onDestroy() {
        locationCallback?.let { runCatching { fusedClient.removeLocationUpdates(it) } }
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "LocationTracking"
        private const val ACTION_STOP = "ACTION_STOP"

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, LocationTrackingService::class.java))
        }

        fun stop(context: Context) {
            context.startService(Intent(context, LocationTrackingService::class.java).apply { action = ACTION_STOP })
        }
    }
}
