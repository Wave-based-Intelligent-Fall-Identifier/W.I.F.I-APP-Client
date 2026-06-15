package com.wify.client.transport

import com.wify.client.data.model.ScannedDevice
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * 전송 Fake 구현 — 디자인 시연/프리뷰/테스트용 (구 `FakeBleController` 대체).
 * "찾은 기기" 화면에 가상 device_id 목록을 emit 한다.
 */
class FakeDeviceTransport(
    override val isAvailable: Boolean = true,
) : DeviceTransport {

    private val scanned = MutableStateFlow<List<ScannedDevice>>(emptyList())

    override fun scannedDevices(): Flow<List<ScannedDevice>> = scanned

    override suspend fun startScan() {
        scanned.value = emptyList()
        delay(600) // 발견 지연 시뮬레이션
        scanned.value = listOf(
            ScannedDevice("wify-001", "wify_device_1", online = true),
            ScannedDevice("wify-002", "wify_device_2", online = true),
            ScannedDevice("wify-003", "wify_device_3", online = true),
        )
    }

    override fun stopScan() { /* no-op */ }

    override suspend fun connect(deviceId: String): Result<Unit> {
        delay(300)
        return Result.success(Unit)
    }

    override fun deviceEvents(deviceId: String): Flow<DeviceEvent> = emptyFlow()

    override suspend fun sendBaselineRebuild(deviceId: String): Result<Unit> = Result.success(Unit)

    override suspend fun sendWifiEdit(deviceId: String, ssid: String, password: String): Result<Unit> =
        Result.success(Unit)

    override fun disconnect(deviceId: String) { /* no-op */ }
}
