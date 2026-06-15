package com.wify.client.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wify.client.data.repository.WifyRepository
import com.wify.client.transport.DeviceTransport
import com.wify.client.ui.screens.DeviceListUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 등록된 기기 목록 화면 ViewModel — 목록 조회, 전체 초기화, 기기 삭제. */
class DeviceListViewModel(
    private val repo: WifyRepository,
    private val transport: DeviceTransport,
) : ViewModel() {

    val uiState: StateFlow<DeviceListUiState> =
        repo.observeDevices()
            .map { DeviceListUiState(transportAvailable = transport.isAvailable, devices = it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DeviceListUiState(transport.isAvailable))

    fun resetAll() = viewModelScope.launch { repo.resetAll() }
    fun deleteDevice(deviceId: String) = viewModelScope.launch { repo.deleteDevice(deviceId) }
    fun deleteDevices(ids: Set<String>) = viewModelScope.launch { repo.deleteDevices(ids) }
}
