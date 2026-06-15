package com.example.wify_app_android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wify_app_android.data.repository.WifyRepository
import com.example.wify_app_android.ui.screens.RecordsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** 기록(로그) 화면 ViewModel — 기기별/전체 필터링된 로그. */
class RecordsViewModel(
    private val repo: WifyRepository,
) : ViewModel() {

    private val filterId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<RecordsUiState> =
        combine(
            filterId,
            repo.observeDevices(),
            repo.observeLogs(null),
        ) { fid, devices, allLogs ->
            val logs = if (fid == null) allLogs else allLogs.filter { it.deviceId == fid }
            RecordsUiState(logs = logs, devices = devices, selectedDeviceId = fid)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RecordsUiState())

    /** null = 전체 */
    fun setFilter(deviceId: String?) { filterId.value = deviceId }
}
