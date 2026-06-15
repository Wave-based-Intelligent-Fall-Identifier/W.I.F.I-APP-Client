package com.wify.client.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.wify.client.MainActivity
import com.wify.client.R

/**
 * 낙상(의심/확정) 등 기기 이벤트를 **시스템 알림**으로 사용자에게 알리는 계약.
 *
 * MQTT 수신 → [com.wify.client.transport.DeviceEventBridge] → (Repository 반영 + 이 알림).
 * 앱이 백그라운드/종료 상태여도 사용자가 인지할 수 있게 하는 안전 알림 경로다(§5).
 */
interface WifyNotifier {
    /** AI=DAN 낙상 확정 — 고우선순위(heads-up) 알림. */
    fun notifyFall(deviceId: String, deviceName: String)

    /** AI=WARN 낙상 의심 — 경고 알림(§5). */
    fun notifyWarning(deviceId: String, deviceName: String)

    /** 정상 복귀 시 해당 기기의 낙상 알림 제거(선택). */
    fun clearFall(deviceId: String)
}

/**
 * Android `NotificationManager` 기반 구현.
 *
 * - 채널 2개: 낙상(IMPORTANCE_HIGH, 소리·진동·heads-up) / 배터리(IMPORTANCE_DEFAULT).
 * - Android 13+ 에서 `POST_NOTIFICATIONS` 권한이 없으면 조용히 무시(앱 동작엔 지장 없음).
 * - 알림 탭 → [MainActivity] 재진입 + 해당 기기 선택(extra [EXTRA_DEVICE_ID]).
 */
class AndroidWifyNotifier(
    private val context: Context,
) : WifyNotifier {

    private val manager = NotificationManagerCompat.from(context)

    init {
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val fall = NotificationChannel(
            CHANNEL_FALL, "낙상 감지", NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "기기에서 낙상이 감지되면 즉시 알립니다."
            enableVibration(true)
        }
        val warning = NotificationChannel(
            CHANNEL_WARNING, "낙상 의심", NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "낙상이 의심되는 상황이 감지되면 알립니다."
            enableVibration(true)
        }
        manager.createNotificationChannel(fall)
        manager.createNotificationChannel(warning)
    }

    override fun notifyFall(deviceId: String, deviceName: String) {
        notify(
            channelId = CHANNEL_FALL,
            notifId = fallNotifId(deviceId),
            title = "낙상 감지",
            text = "${deviceName}에서 낙상이 감지되었습니다.",
            deviceId = deviceId,
            priority = NotificationCompat.PRIORITY_HIGH,
            category = NotificationCompat.CATEGORY_ALARM,
        )
    }

    override fun notifyWarning(deviceId: String, deviceName: String) {
        notify(
            channelId = CHANNEL_WARNING,
            notifId = warningNotifId(deviceId),
            title = "낙상 의심",
            text = "${deviceName}에서 낙상이 의심되는 상황이 감지되었습니다.",
            deviceId = deviceId,
            priority = NotificationCompat.PRIORITY_HIGH,
            category = NotificationCompat.CATEGORY_ALARM,
        )
    }

    override fun clearFall(deviceId: String) {
        manager.cancel(fallNotifId(deviceId))
    }

    private fun notify(
        channelId: String,
        notifId: Int,
        title: String,
        text: String,
        deviceId: String,
        priority: Int,
        category: String,
    ) {
        if (!hasPostPermission()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DEVICE_ID, deviceId)
        }
        val pending = PendingIntent.getActivity(
            context,
            notifId, // 기기별 고유 요청코드 → extra 가 덮어써지지 않음
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_wify)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(priority)
            .setCategory(category)
            .setColor(ALERT_COLOR)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        // 권한 확인을 마쳤으므로 안전.
        manager.notify(notifId, notification)
    }

    private fun hasPostPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // 13 미만은 별도 런타임 권한 없음. 사용자가 설정에서 끈 경우만 비활성.
            manager.areNotificationsEnabled()
        }

    private fun fallNotifId(deviceId: String): Int = deviceId.hashCode()
    private fun warningNotifId(deviceId: String): Int = deviceId.hashCode() xor WARNING_ID_MASK

    companion object {
        const val CHANNEL_FALL = "wify_fall"
        const val CHANNEL_WARNING = "wify_warning"

        /** 알림 탭 시 [MainActivity] 로 전달되는 기기 ID extra 키. */
        const val EXTRA_DEVICE_ID = "com.wify.client.extra.DEVICE_ID"

        private const val ALERT_COLOR = 0xFFE53935.toInt() // 위험(빨강)
        private const val WARNING_ID_MASK = 0x7FFF0000
    }
}

/** 프리뷰/테스트용 No-op 구현. */
class NoOpWifyNotifier : WifyNotifier {
    override fun notifyFall(deviceId: String, deviceName: String) {}
    override fun notifyWarning(deviceId: String, deviceName: String) {}
    override fun clearFall(deviceId: String) {}
}
