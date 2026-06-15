package com.example.wify_app_android.ui.screens

import com.example.wify_app_android.data.model.Device
import com.example.wify_app_android.data.model.LogEntry

/** 기록 화면 상태 (로그 + 기기 필터, selectedDeviceId == null 이면 전체) */
data class RecordsUiState(
    val logs: List<LogEntry> = emptyList(),
    val devices: List<Device> = emptyList(),
    val selectedDeviceId: String? = null,
)
