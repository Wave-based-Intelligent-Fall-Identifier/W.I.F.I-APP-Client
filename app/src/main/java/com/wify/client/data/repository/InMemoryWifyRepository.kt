package com.wify.client.data.repository

import com.wify.client.data.model.Device
import com.wify.client.data.model.DeviceStatus
import com.wify.client.data.model.FallState
import com.wify.client.data.model.LogEntry
import com.wify.client.data.model.LogType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * 인메모리 Repository 구현 (시드 데이터).
 * 디자인 캡처(등록된 기기 그리드 / 기록)를 그대로 반영.
 * Room 구현으로 교체 가능하도록 [WifyRepository] 계약만 노출.
 */
class InMemoryWifyRepository(
    initialDevices: List<Device> = seedDevices(),
    initialLogs: List<LogEntry> = seedLogs(),
) : WifyRepository {

    private val devices = MutableStateFlow(initialDevices)
    private val logs = MutableStateFlow(initialLogs)
    private val selectedId = MutableStateFlow<String?>(initialDevices.firstOrNull()?.id)

    override fun observeDevices(): Flow<List<Device>> = devices

    override fun observeDevice(deviceId: String): Flow<Device?> =
        devices.map { list -> list.firstOrNull { it.id == deviceId } }

    override fun observeLogs(deviceId: String?): Flow<List<LogEntry>> =
        logs.map { list ->
            (if (deviceId == null) list else list.filter { it.deviceId == deviceId })
                .sortedByDescending { it.timestamp }
        }

    override fun observeSelectedFallState(): Flow<FallState?> =
        combine(devices, selectedId) { list, sel ->
            val d = list.firstOrNull { it.id == sel } ?: return@combine null
            FallState(d.id, d.status, d.inUse, d.registeredAt)
        }

    override fun observeSelectedDevice(): Flow<Device?> =
        combine(devices, selectedId) { list, sel -> list.firstOrNull { it.id == sel } }

    override suspend fun registerDevice(deviceId: String, name: String): Device {
        // 이미 등록된 기기(device_id 동일)면 중복 추가하지 않고 기존 것을 반환.
        devices.value.firstOrNull { it.deviceId == deviceId }?.let { return it }

        val newDevice = Device(
            id = deviceId,
            name = name,
            deviceId = deviceId,
            registeredAt = nextRegisteredAt(),
        )
        devices.update { it + newDevice }
        return newDevice
    }

    override suspend fun renameDevice(deviceId: String, newName: String) {
        devices.update { list -> list.map { if (it.id == deviceId) it.copy(name = newName) else it } }
    }

    override suspend fun setDeviceImage(deviceId: String, imageUri: String?) {
        devices.update { list -> list.map { if (it.id == deviceId) it.copy(imageUri = imageUri) else it } }
    }

    override suspend fun deleteDevice(deviceId: String) {
        devices.update { list -> list.filterNot { it.id == deviceId } }
        logs.update { list -> list.filterNot { it.deviceId == deviceId } }
        if (selectedId.value == deviceId) selectedId.value = devices.value.firstOrNull()?.id
    }

    override suspend fun deleteDevices(deviceIds: Set<String>) {
        devices.update { list -> list.filterNot { it.id in deviceIds } }
        logs.update { list -> list.filterNot { it.deviceId in deviceIds } }
        if (selectedId.value in deviceIds) selectedId.value = devices.value.firstOrNull()?.id
    }

    override suspend fun selectDevice(deviceId: String) {
        selectedId.value = deviceId
    }

    override suspend fun resetFallState(deviceId: String) {
        devices.update { list ->
            list.map {
                if (it.id == deviceId) it.copy(status = DeviceStatus.NORMAL, alertAtLabel = null) else it
            }
        }
    }

    override suspend fun resetAll() {
        devices.value = emptyList()
        logs.value = emptyList()
        selectedId.value = null
    }

    // ---- MQTT 이벤트 반영 (§3/§5) ----

    override suspend fun markNormal(deviceId: String) {
        devices.update { list ->
            list.map {
                if (it.id == deviceId)
                    it.copy(status = DeviceStatus.NORMAL, alertAtLabel = null)
                else it
            }
        }
    }

    override suspend fun raiseWarning(deviceId: String) {
        val now = System.currentTimeMillis()
        val label = timeLabel(now)
        devices.update { list ->
            list.map {
                if (it.id == deviceId) {
                    // 이미 위험(낙상)이면 상태는 유지. 그 외엔 WARNING 으로.
                    val status = if (it.status == DeviceStatus.DANGER) it.status else DeviceStatus.WARNING
                    it.copy(status = status)
                } else it
            }
        }
        appendLog(deviceId, LogType.WARNING, "낙상 의심 감지", now, label)
    }

    override suspend fun raiseFall(deviceId: String) {
        val now = System.currentTimeMillis()
        val label = timeLabel(now)
        devices.update { list ->
            list.map {
                if (it.id == deviceId) it.copy(status = DeviceStatus.DANGER, alertAtLabel = label) else it
            }
        }
        appendLog(deviceId, LogType.FALL_DETECTED, "낙상 감지", now, label)
    }

    override suspend fun setOnline(deviceId: String, online: Boolean) {
        devices.update { list -> list.map { if (it.id == deviceId) it.copy(online = online) else it } }
    }

    override suspend fun setInUse(deviceId: String, inUse: Boolean) {
        devices.update { list -> list.map { if (it.id == deviceId) it.copy(inUse = inUse) else it } }
    }

    override suspend fun setCalibrating(deviceId: String, calibrating: Boolean) {
        devices.update { list -> list.map { if (it.id == deviceId) it.copy(calibrating = calibrating) else it } }
    }

    private fun appendLog(deviceId: String, type: LogType, message: String, at: Long, label: String) {
        val entry = LogEntry("log_mqtt_${at}_${logSeq++}", deviceId, type, message, at, label)
        logs.update { it + entry }
    }

    private fun timeLabel(millis: Long): String =
        java.text.SimpleDateFormat("H:mm", java.util.Locale.KOREA).format(java.util.Date(millis))

    private var logSeq = 0

    // ---- 시드 데이터 (디자인 기준) ----
    private fun nextRegisteredAt(): Long = 1_749_500_000_000L + devices.value.size * 1000L

    companion object {
        private fun seedDevices(): List<Device> = listOf(
            Device("dev_1", "기기 1", "wify-001", 1_749_500_000_000L, DeviceStatus.NORMAL),
            Device("dev_2", "wify_device_2", "wify-002", 1_749_500_001_000L,
                DeviceStatus.DANGER, alertAtLabel = "15:40"),
            Device("dev_3", "wify_device_3", "wify-003", 1_749_500_002_000L, DeviceStatus.WARNING),
            Device("dev_4", "wify_device_4", "wify-004", 1_749_500_003_000L, DeviceStatus.NORMAL),
            Device("dev_5", "wify_device_5", "wify-005", 1_749_500_004_000L, DeviceStatus.NORMAL),
        )

        private fun seedLogs(): List<LogEntry> = listOf(
            LogEntry("log_1", "dev_1", LogType.FALL_DETECTED, "낙상 감지", 1_749_500_010_000L, "3:00"),
            LogEntry("log_2", "dev_1", LogType.WARNING, "낙상 의심 감지", 1_749_500_009_000L, "3:00"),
            LogEntry("log_3", "dev_2", LogType.FALL_DETECTED, "낙상 감지", 1_749_500_008_000L, "15:40"),
            LogEntry("log_4", "dev_3", LogType.WARNING, "낙상 의심 감지", 1_749_500_007_000L, "2:10"),
        )
    }
}
