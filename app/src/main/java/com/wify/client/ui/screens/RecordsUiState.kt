package com.wify.client.ui.screens

import com.wify.client.data.model.Device
import com.wify.client.data.model.LogEntry

/** 기록 화면 상태 (로그 + 기기 필터, selectedDeviceId == null 이면 전체) */
data class RecordsUiState(
    val logs: List<LogEntry> = emptyList(),
    val devices: List<Device> = emptyList(),
    val selectedDeviceId: String? = null,
)
