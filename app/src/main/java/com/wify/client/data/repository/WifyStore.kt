package com.wify.client.data.repository

import android.content.Context
import com.wify.client.data.model.Device
import com.wify.client.data.model.DeviceStatus
import com.wify.client.data.model.LogEntry
import com.wify.client.data.model.LogType
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 등록 기기/로그를 JSON 파일 하나로 영속화한다.
 *
 * Room 대신 의존성 0(`org.json` 내장) 경량 구현. 파일: `filesDir/wify_state.json`.
 * 데이터 양(기기 수십 개·로그 수백 줄)이 작아 전체 직렬화로 충분하다.
 */
class WifyStore(context: Context) {

    private val file = File(context.filesDir, FILE_NAME)

    data class Snapshot(val devices: List<Device>, val logs: List<LogEntry>)

    /** 저장된 상태를 읽는다. 파일이 없거나 깨졌으면 null. */
    fun load(): Snapshot? {
        if (!file.exists()) return null
        return runCatching {
            val root = JSONObject(file.readText())
            val devices = root.optJSONArray("devices").toList { deviceFromJson(it) }
            val logs = root.optJSONArray("logs").toList { logFromJson(it) }
            Snapshot(devices, logs)
        }.getOrNull()
    }

    /** 현재 상태를 통째로 저장한다(원자적 쓰기). */
    fun save(devices: List<Device>, logs: List<LogEntry>) {
        runCatching {
            val root = JSONObject().apply {
                put("devices", JSONArray().apply { devices.forEach { put(deviceToJson(it)) } })
                put("logs", JSONArray().apply { logs.forEach { put(logToJson(it)) } })
            }
            val tmp = File(file.parentFile, "$FILE_NAME.tmp")
            tmp.writeText(root.toString())
            if (!tmp.renameTo(file)) file.writeText(root.toString())
        }
    }

    // ---- 직렬화 ----
    private fun deviceToJson(d: Device) = JSONObject().apply {
        put("id", d.id)
        put("name", d.name)
        put("deviceId", d.deviceId)
        put("registeredAt", d.registeredAt)
        put("status", d.status.name)
        put("online", d.online)
        put("batteryLow", d.batteryLow)
        put("inUse", d.inUse)
        put("alertAtLabel", d.alertAtLabel ?: JSONObject.NULL)
        put("imageUri", d.imageUri ?: JSONObject.NULL)
    }

    private fun deviceFromJson(o: JSONObject) = Device(
        id = o.getString("id"),
        name = o.getString("name"),
        deviceId = o.optString("deviceId", o.getString("id")),
        registeredAt = o.getLong("registeredAt"),
        status = runCatching { DeviceStatus.valueOf(o.getString("status")) }.getOrDefault(DeviceStatus.NORMAL),
        online = o.optBoolean("online", false),
        batteryLow = o.optBoolean("batteryLow", false),
        inUse = o.optBoolean("inUse", true),
        alertAtLabel = o.optStringOrNull("alertAtLabel"),
        imageUri = o.optStringOrNull("imageUri"),
    )

    private fun logToJson(e: LogEntry) = JSONObject().apply {
        put("id", e.id)
        put("deviceId", e.deviceId)
        put("type", e.type.name)
        put("message", e.message)
        put("timestamp", e.timestamp)
        put("timeLabel", e.timeLabel)
    }

    private fun logFromJson(o: JSONObject) = LogEntry(
        id = o.getString("id"),
        deviceId = o.getString("deviceId"),
        type = runCatching { LogType.valueOf(o.getString("type")) }.getOrDefault(LogType.FALL_DETECTED),
        message = o.getString("message"),
        timestamp = o.getLong("timestamp"),
        timeLabel = o.getString("timeLabel"),
    )

    private fun <T> JSONArray?.toList(map: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        return (0 until length()).map { map(getJSONObject(it)) }
    }

    private fun JSONObject.optStringOrNull(name: String): String? =
        if (isNull(name) || !has(name)) null else getString(name)

    companion object {
        private const val FILE_NAME = "wify_state.json"
    }
}
