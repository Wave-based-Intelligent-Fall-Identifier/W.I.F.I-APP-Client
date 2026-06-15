package com.example.wify_app_android.ui.screens

import com.example.wify_app_android.data.model.ScannedDevice

/** 찾은 기기(스캔) 화면 상태 */
data class ScanUiState(
    val scanning: Boolean = false,
    val found: List<ScannedDevice> = emptyList(),
)
