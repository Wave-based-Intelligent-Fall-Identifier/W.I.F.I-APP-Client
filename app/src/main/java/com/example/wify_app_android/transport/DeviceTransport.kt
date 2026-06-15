package com.example.wify_app_android.transport

import com.example.wify_app_android.data.model.ScannedDevice
import kotlinx.coroutines.flow.Flow

/**
 * 기기 통신 "API 형식" (계약) — 전송 수단 중립.
 *
 * 구 `ble/BleController` 를 대체한다. UI/ViewModel/Bridge 는 이 인터페이스에만 의존하므로
 * 실제 전송 구현(MQTT)은 교체 가능하다.
 * 현재 구현: [com.example.wify_app_android.mqtt.MqttDeviceTransport] (HiveMQ),
 *           [FakeDeviceTransport] (프리뷰/테스트).
 *
 * MQTT 매핑(docs/WIFY_MQTT_APP_PROTOCOL.md):
 *  - "발견(scan)" = `wify/+/status` (retained) 구독으로 살아있는 device_id 수집(§2).
 *  - "연결(connect)" = 해당 기기의 `wify/{id}/#` 구독 시작(§2).
 *  - "해제(disconnect)" = 구독 해지(등록 정보는 로컬 유지).
 */
interface DeviceTransport {

    /** 전송 계층(MQTT 브로커) 연결 가능/활성 상태. (구 BLE `isBluetoothEnabled` 대체.) */
    val isAvailable: Boolean

    /**
     * 발견된(announce) 기기 스트림. `wify/+/status` retained 수신으로 채워진다(§2).
     * `ScannedDevice.deviceId` = device_id(등록 코드). 구 스캔 UI/리스트를 그대로 재활용한다.
     */
    fun scannedDevices(): Flow<List<ScannedDevice>>

    /** 발견(구독) 시작/재시작 — `wify/+/status` 구독. */
    suspend fun startScan()

    /** 발견 구독 중지(또는 no-op). */
    fun stopScan()

    /** 기기 연결 = `wify/{deviceId}/#` 구독 시작(§2). */
    suspend fun connect(deviceId: String): Result<Unit>

    /** 연결된 기기의 이벤트 구독 — 토픽 페이로드를 [DeviceEvent] 로 매핑한다. */
    fun deviceEvents(deviceId: String): Flow<DeviceEvent>

    /**
     * 베이스라인 재구축 명령(§4): `wify/{deviceId}/baseline/cmd` ← `{"cmd":"BASELINE_REBUILD"}`.
     * 미연결/오프라인이면 실패할 수 있다.
     */
    suspend fun sendBaselineRebuild(deviceId: String): Result<Unit>

    /**
     * Wi-Fi 재설정(§4): `edit/nownetwork` ← `id:%s/passwd:%s`, 이어서 `edit/editnetwork` ← `NEWNETWORKEDIT`.
     * ⚠️ 자격증명 직접 발행은 민감 작업(§4/§7) — MVP 한정.
     */
    suspend fun sendWifiEdit(deviceId: String, ssid: String, password: String): Result<Unit>

    /** 연결 해제 = `wify/{deviceId}/#` 구독 해지(§2). */
    fun disconnect(deviceId: String)
}
