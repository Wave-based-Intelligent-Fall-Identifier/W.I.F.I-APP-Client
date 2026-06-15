package com.wify.client.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wify.client.data.repository.WifyRepository
import com.wify.client.transport.DeviceTransport
import com.wify.client.ui.screens.ScanUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 찾은 기기(발견) 화면 ViewModel — announce 구독으로 발견된 기기 표시, 연결+등록(§2).
 * 발견 = `wify/+/status` retained 구독. 등록 = 코드(device_id) 로 `wify/{id}/#` 구독.
 */
class ScanViewModel(
    private val repo: WifyRepository,
    private val transport: DeviceTransport,
) : ViewModel() {

    private val scanning = MutableStateFlow(false)

    val uiState: StateFlow<ScanUiState> =
        combine(transport.scannedDevices(), scanning) { found, isScanning ->
            ScanUiState(scanning = isScanning, found = found)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScanUiState())

    init { rescan() }

    fun rescan() = viewModelScope.launch {
        scanning.value = true
        transport.startScan()
        scanning.value = false
    }

    /** 연결 + 등록(코드 = device_id). 성공 시 등록된 deviceId 콜백. */
    fun connect(deviceId: String, name: String, onRegistered: (String) -> Unit) =
        viewModelScope.launch {
            transport.connect(deviceId).onSuccess {
                val device = repo.registerDevice(deviceId, name)
                onRegistered(device.id)
            }
        }
}
