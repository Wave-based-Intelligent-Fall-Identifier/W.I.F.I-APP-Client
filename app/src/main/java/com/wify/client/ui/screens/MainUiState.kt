package com.wify.client.ui.screens

import com.wify.client.data.model.Device

/** 메인 모니터링 화면 상태 */
data class MainUiState(
    val device: Device? = null,
    val inUse: Boolean = true,
)
