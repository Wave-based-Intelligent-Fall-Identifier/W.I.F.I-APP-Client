package com.wify.client.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.wify.client.MainActivity
import com.wify.client.R
import com.wify.client.di.ServiceLocator

/**
 * 백그라운드 모니터링 포그라운드 서비스.
 *
 * 앱이 백그라운드/종료 상태여도 프로세스를 살려, MQTT 구독([com.wify.client.transport.DeviceEventBridge])과
 * 낙상 알림이 계속 동작하도록 한다. 실제 구독/연결 로직은 [ServiceLocator] 의 앱 스코프에 있고,
 * 이 서비스는 그 프로세스를 포그라운드로 승격해 OS가 죽이지 않게 하는 역할이다.
 *
 * 낙상 감지는 안전 기능이므로, 화면을 꺼도 모니터링이 유지되어야 한다.
 */
class MonitoringService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // 프로세스가 새로 떠서 서비스만 살아난 경우에도 BLE/브리지 초기화 보장.
        ServiceLocator.init(applicationContext)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ServiceCompat.startForeground(
            this,
            NOTIF_ID,
            buildNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
        )
        // 시스템이 종료해도 가능한 한 다시 시작.
        return START_STICKY
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_MONITOR, "모니터링", NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "기기를 백그라운드에서 모니터링하는 중임을 표시합니다."
            setShowBadge(false)
        }
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    private fun buildNotification() =
        NotificationCompat.Builder(this, CHANNEL_MONITOR)
            .setSmallIcon(R.drawable.ic_stat_wify)
            .setContentTitle("WIFY 모니터링 중")
            .setContentText("등록된 기기의 낙상을 감지하고 있습니다.")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java)
                        .apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .build()

    companion object {
        private const val CHANNEL_MONITOR = "wify_monitor"
        private const val NOTIF_ID = 1001

        /** 모니터링 서비스를 시작한다(앱 진입 시 호출). */
        fun start(context: Context) {
            val intent = Intent(context, MonitoringService::class.java)
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }
    }
}
