package com.wify.client.transport

import com.wify.client.data.model.DeviceStatus
import com.wify.client.data.repository.WifyRepository
import com.wify.client.notification.WifyNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Transport → Repository(+알림) 브리지 (구 `ble/DeviceEventBridge` 대체).
 *
 * 등록된 기기마다 [DeviceTransport.deviceEvents] 를 구독해서, 올라오는 [DeviceEvent] 를
 * Repository 상태/로그에 반영하고([WifyRepository]), AI 의심/확정은 [WifyNotifier] 로
 * 시스템 알림까지 띄운다(docs/WIFY_MQTT_APP_PROTOCOL.md §5).
 *
 * 로그 정책(§5): AI 판단은 **상태 전이(에지)** 에서만 로그/알림한다. 같은 값 반복 수신은
 * 상태 미러만 하고 로그 스팸을 막는다(기존 BLE "스냅샷=로그 없음" 정신 계승).
 *
 * 앱 스코프(Application)에서 [start] 1회 호출. Fake 구현에서는 deviceEvents 가 빈 스트림이라 무해.
 */
class DeviceEventBridge(
    private val repo: WifyRepository,
    private val transport: DeviceTransport,
    private val notifier: WifyNotifier,
    private val scope: CoroutineScope,
) {
    private val jobs = mutableMapOf<String, Job>() // deviceId -> 구독 job

    // deviceId -> 마지막으로 반영한 AI 판단(에지 검출용).
    private val lastJudgment = ConcurrentHashMap<String, DeviceEvent.Judgment>()

    fun start() {
        scope.launch {
            repo.observeDevices().collect { devices ->
                val currentIds = devices.map { it.id }.toSet()
                // 삭제된 기기의 구독 정리
                (jobs.keys - currentIds).forEach { id ->
                    jobs.remove(id)?.cancel()
                    lastJudgment.remove(id)
                }
                // 새 기기 구독 시작
                devices.forEach { device ->
                    if (device.id !in jobs) {
                        jobs[device.id] = scope.launch {
                            // 연결 보장(= wify/{id}/# 구독). 끊김 후 자동 재연결은 transport 가 담당.
                            transport.connect(device.deviceId)
                            transport.deviceEvents(device.deviceId).collect { event ->
                                applyEvent(device.id, device.name, event)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 하나의 [DeviceEvent] 를 Repository(+알림)에 반영한다.
     *
     * 실제 MQTT 경로와 디버그 주입 경로(DebugInjectionServer)가 **동일한 반영 로직**을
     * 공유하도록 공개한다.
     *
     * @param deviceId Repository 의 기기 id (= device_id).
     */
    suspend fun applyEvent(deviceId: String, deviceName: String, event: DeviceEvent) {
        when (event) {
            is DeviceEvent.Ai -> applyAi(deviceId, deviceName, event.judgment)
            is DeviceEvent.Online -> repo.setOnline(deviceId, event.online)
            is DeviceEvent.Restroom -> applyRestroom(deviceId, event.state)
            is DeviceEvent.BaselineStatus -> repo.setCalibrating(deviceId, event.measuring)
            // 네트워크 상태는 현재 별도 UI 미러 필드가 없어 상태 변경 없음.
            // TODO(§8): nownetwork/status 를 기기 상세 Wi-Fi 표시에 연결.
            is DeviceEvent.NetworkStatus -> { /* no-op (UI seam) */ }
        }
    }

    /** AI 판단 반영 — 상태 전이에서만 로그/알림(§5). */
    private suspend fun applyAi(deviceId: String, deviceName: String, judgment: DeviceEvent.Judgment) {
        val previous = lastJudgment.put(deviceId, judgment)
        val isEdge = previous != judgment
        when (judgment) {
            DeviceEvent.Judgment.NORMAL -> repo.markNormal(deviceId) // 미러(로그 없음)
            DeviceEvent.Judgment.WARNING -> if (isEdge) {
                repo.raiseWarning(deviceId)
                notifier.notifyWarning(deviceId, deviceName)
            }
            DeviceEvent.Judgment.DANGER -> if (isEdge) {
                repo.raiseFall(deviceId)
                notifier.notifyFall(deviceId, deviceName)
            }
        }
    }

    /** restroom ACT/DEACT/LOAD → inUse 미러(§3). */
    private suspend fun applyRestroom(deviceId: String, state: DeviceEvent.RestroomState) {
        when (state) {
            DeviceEvent.RestroomState.ACTIVE -> repo.setInUse(deviceId, true)
            DeviceEvent.RestroomState.DEACTIVE -> repo.setInUse(deviceId, false)
            DeviceEvent.RestroomState.LOAD -> repo.setInUse(deviceId, false) // 초기화 = 미사용
        }
    }

    /** [DeviceStatus] 미사용이지만 매핑 명세 보존용(§5). */
    @Suppress("unused")
    private fun DeviceEvent.Judgment.toDeviceStatus(): DeviceStatus = when (this) {
        DeviceEvent.Judgment.DANGER -> DeviceStatus.DANGER
        DeviceEvent.Judgment.WARNING -> DeviceStatus.WARNING
        DeviceEvent.Judgment.NORMAL -> DeviceStatus.NORMAL
    }
}
