package com.wify.client.ui.screens

import com.wify.client.data.model.Device
import com.wify.client.data.model.LogEntry

/** 기기 상세 화면 상태 (선택 + 인라인 이름수정 모드 포함) */
data class DeviceDetailUiState(
    val device: Device? = null,
    val logs: List<LogEntry> = emptyList(),
    val editing: Boolean = false,
    val editName: String = "",
)
