package com.example.wify_app_android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.wify_app_android.di.ServiceLocator

/**
 * 경량 ViewModel 팩토리 (Hilt 미사용).
 * deviceId가 필요한 상세 VM은 [deviceId] 인자로 생성.
 */
class WifyViewModelFactory(
    private val deviceId: String? = null,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo = ServiceLocator.repository
        val transport = ServiceLocator.transport
        return when {
            modelClass.isAssignableFrom(DeviceListViewModel::class.java) ->
                DeviceListViewModel(repo, transport) as T
            modelClass.isAssignableFrom(ScanViewModel::class.java) ->
                ScanViewModel(repo, transport) as T
            modelClass.isAssignableFrom(DeviceDetailViewModel::class.java) ->
                DeviceDetailViewModel(repo, requireNotNull(deviceId) { "deviceId required" }) as T
            modelClass.isAssignableFrom(MainViewModel::class.java) ->
                MainViewModel(repo, transport) as T
            modelClass.isAssignableFrom(RecordsViewModel::class.java) ->
                RecordsViewModel(repo) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
