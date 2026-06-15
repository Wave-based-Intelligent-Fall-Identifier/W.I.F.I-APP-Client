package com.wify.client.data.model

/**
 * 로그 종류 (docs/WIFY_MQTT_APP_PROTOCOL.md §5 에스컬레이션):
 *  - [FALL_DETECTED] : AI=DAN (위험/빨강) — 낙상 확정.
 *  - [WARNING]       : AI=WARN (주의/노랑) — 낙상 의심.
 *  - [BATTERY_LOW]   : 배터리 부족(노랑) — 현재 MQTT 데이터 소스 없음(§6). 코드 seam 으로만 유지.
 */
enum class LogType { FALL_DETECTED, WARNING, BATTERY_LOW }

/** 로그 1건 */
data class LogEntry(
    val id: String,
    val deviceId: String,
    val type: LogType,
    val message: String,
    val timestamp: Long,
    /** 표시용 시각 라벨 (예: "3:00") */
    val timeLabel: String,
)
