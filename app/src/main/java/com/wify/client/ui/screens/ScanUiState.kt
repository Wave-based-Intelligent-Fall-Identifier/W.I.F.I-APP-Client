package com.wify.client.ui.screens

import com.wify.client.data.model.ScannedDevice

/** 찾은 기기(스캔) 화면 상태 */
data class ScanUiState(
    val scanning: Boolean = false,
    val found: List<ScannedDevice> = emptyList(),
)
