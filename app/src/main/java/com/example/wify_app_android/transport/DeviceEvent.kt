package com.example.wify_app_android.transport

/**
 * 기기(ESP)/서버에서 브로커를 거쳐 앱으로 올라오는 이벤트.
 *
 * MQTT 전환(docs/WIFY_MQTT_APP_PROTOCOL.md)에 맞춰, 토픽별 페이로드를 전송-중립 이벤트로 정규화한다.
 * 두 가지 성격을 구분한다(기존 BLE 규약 §5 "스냅샷=로그 없음, 에지=로그" 정신 계승):
 *  - **미러형**(상태 동기화 전용, 로그 없음): [Online], [Restroom], [BaselineStatus], [NetworkStatus].
 *  - **에지형**(상태 전이에서 로그 + 알림): [Ai] (값 전이에서만 로그 — DeviceEventBridge 가 판단).
 *
 * AI 판단은 §3 의 `wify/{id}/AI` (`DAN`/`WARN`/`NOR`) 에서 온다.
 */
sealed interface DeviceEvent {

    /**
     * AI 낙상 판단 (`wify/{id}/AI`). §5 에스컬레이션:
     * DAN→DANGER(낙상 확정), WARN→WARNING(낙상 의심), NOR→NORMAL.
     * 로그는 **상태 전이(에지)** 에서만 남긴다([DeviceEventBridge] 가 직전 상태와 비교).
     */
    data class Ai(val judgment: Judgment) : DeviceEvent

    /** 기기 온라인 여부 (`wify/{id}/status` retained, `online`/`offline`). 상태 미러(로그 없음). */
    data class Online(val online: Boolean) : DeviceEvent

    /**
     * 화장실/PIR 사람 감지 (`wify/{id}/restroom`). ACT→사용중, DEACT→해제, LOAD→초기화.
     * inUse 상태 미러(로그 없음).
     */
    data class Restroom(val state: RestroomState) : DeviceEvent

    /** 베이스라인 측정 상태 (`wify/{id}/baseline/status`). MEASURING→측정중, DONE→완료. calibrating 미러. */
    data class BaselineStatus(val measuring: Boolean) : DeviceEvent

    /** Wi-Fi 연결 상태 (`wify/{id}/nownetwork/status`, `connect`/`disconnect`). 상태 미러(로그 없음). */
    data class NetworkStatus(val connected: Boolean) : DeviceEvent

    // TODO(§7): 펌웨어가 `wify/{id}/battery` 토픽을 추가하면 BatteryLow 이벤트 복원(현재 데이터 소스 없음).

    /** AI 판단 3단계 (§3/§5). */
    enum class Judgment { NORMAL, WARNING, DANGER }

    /** 화장실 PIR 상태 (§3). */
    enum class RestroomState { ACTIVE, DEACTIVE, LOAD }
}
