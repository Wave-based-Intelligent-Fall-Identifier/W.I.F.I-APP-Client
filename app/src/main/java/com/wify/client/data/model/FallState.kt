package com.wify.client.data.model

/** 현재 모니터링 중인 기기의 실시간 낙상 상태 */
data class FallState(
    val deviceId: String,
    val status: DeviceStatus = DeviceStatus.NORMAL,
    val inUse: Boolean = true,
    val updatedAt: Long = 0L,
)
