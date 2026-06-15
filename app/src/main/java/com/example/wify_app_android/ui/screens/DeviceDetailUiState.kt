package com.example.wify_app_android.ui.screens

import com.example.wify_app_android.data.model.Device
import com.example.wify_app_android.data.model.LogEntry

/** 기기 상세 화면 상태 (선택 + 인라인 이름수정 모드 포함) */
data class DeviceDetailUiState(
    val device: Device? = null,
    val logs: List<LogEntry> = emptyList(),
    val editing: Boolean = false,
    val editName: String = "",
)
