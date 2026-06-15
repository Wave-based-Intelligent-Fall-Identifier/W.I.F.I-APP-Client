package com.example.wify_app_android.data.model

/**
 * announce 로 발견된(미등록) 기기 (docs/WIFY_MQTT_APP_PROTOCOL.md §2).
 *
 * `wify/+/status` retained 수신으로 채워진다. 식별자는 device_id(= 등록 코드).
 * BLE 의 RSSI 신호세기는 MQTT 에 없으므로 제거하고, online 여부만 둔다(§6).
 */
data class ScannedDevice(
    /** device_id (= 등록 코드) */
    val deviceId: String,
    /** 표시 이름(없으면 device_id 그대로) */
    val name: String,
    /** 현재 온라인(announce=online) 여부 */
    val online: Boolean = true,
)
