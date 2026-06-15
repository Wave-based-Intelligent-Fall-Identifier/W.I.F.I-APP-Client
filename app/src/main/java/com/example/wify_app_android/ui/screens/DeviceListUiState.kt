package com.example.wify_app_android.ui.screens

import com.example.wify_app_android.data.model.Device

/** 등록된 기기 화면 상태 (3상태: 전송 불가 / 빈 / 그리드) */
data class DeviceListUiState(
    /** MQTT 브로커 연결 가능 여부(구 bluetoothEnabled 대체). */
    val transportAvailable: Boolean = true,
    val devices: List<Device> = emptyList(),
) {
    val isEmpty: Boolean get() = devices.isEmpty()
}
