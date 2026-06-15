package com.wify.client.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wify.client.data.repository.WifyRepository
import com.wify.client.transport.DeviceTransport
import com.wify.client.ui.screens.MainUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 메인 모니터링 화면 ViewModel — 선택된 기기의 낙상 상태, 베이스라인 재구축. */
class MainViewModel(
    private val repo: WifyRepository,
    private val transport: DeviceTransport,
) : ViewModel() {

    val uiState: StateFlow<MainUiState> =
        repo.observeSelectedDevice()
            .map { MainUiState(device = it, inUse = it?.inUse ?: false) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainUiState())

    /**
     * 베이스라인(환경 기준선) 재구축 요청 — `wify/{id}/baseline/cmd` ← BASELINE_REBUILD (§4).
     * 기기가 현재 환경 기준으로 CSI 베이스라인을 다시 잡는다. 낙상 기록/상태는 그대로 유지한다.
     */
    fun recalibrate() = viewModelScope.launch {
        uiState.value.device?.let { device ->
            transport.sendBaselineRebuild(device.deviceId)
        }
    }
}
