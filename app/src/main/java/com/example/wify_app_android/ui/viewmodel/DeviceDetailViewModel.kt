package com.example.wify_app_android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wify_app_android.data.repository.WifyRepository
import com.example.wify_app_android.ui.screens.DeviceDetailUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 기기 상세 화면 ViewModel — 상세/로그 조회, 이름 수정, 사진 변경, 선택. */
class DeviceDetailViewModel(
    private val repo: WifyRepository,
    private val deviceId: String,
) : ViewModel() {

    private val editing = MutableStateFlow(false)
    private val editName = MutableStateFlow("")

    val uiState: StateFlow<DeviceDetailUiState> =
        combine(
            repo.observeDevice(deviceId),
            repo.observeLogs(deviceId),
            editing,
            editName,
        ) { device, logs, isEditing, name ->
            DeviceDetailUiState(device = device, logs = logs, editing = isEditing, editName = name)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DeviceDetailUiState())

    fun startEdit() {
        editName.value = uiState.value.device?.name ?: ""
        editing.value = true
    }

    fun onNameChange(value: String) { editName.value = value }

    fun saveName() = viewModelScope.launch {
        repo.renameDevice(deviceId, editName.value.trim().ifEmpty { uiState.value.device?.name ?: "" })
        editing.value = false
    }

    fun select(onSelected: () -> Unit) = viewModelScope.launch {
        repo.selectDevice(deviceId)
        onSelected()
    }

    fun setImage(uri: String?) = viewModelScope.launch { repo.setDeviceImage(deviceId, uri) }
}
