package com.example.wify_app_android.ui.screens

import com.example.wify_app_android.data.model.Device

/** 메인 모니터링 화면 상태 */
data class MainUiState(
    val device: Device? = null,
    val inUse: Boolean = true,
)
