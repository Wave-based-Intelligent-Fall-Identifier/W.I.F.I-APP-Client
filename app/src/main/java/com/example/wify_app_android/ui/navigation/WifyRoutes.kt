package com.example.wify_app_android.ui.navigation

/**
 * 흐름(flow) 기반 라우트 정의. (하단탭 아님)
 *
 * 등록된 기기 → (등록) 스캔 → 그리드 → 상세 → (선택) 메인 ↔ 기록
 * 이름수정은 상세 화면 내 인라인 모드로 처리(별도 라우트 아님).
 */
object WifyRoutes {
    const val DEVICE_LIST = "device_list"   // 등록된 기기 (BT off / 빈 / 그리드)
    const val SCAN = "scan"                 // 찾은 기기 (BLE 스캔)
    const val DEVICE_DETAIL = "device_detail" // device_detail/{deviceId}
    const val MAIN = "main"                 // 메인 모니터링
    const val RECORDS = "records"           // 기록 (로그)

    const val ARG_DEVICE_ID = "deviceId"

    fun deviceDetail(deviceId: String) = "$DEVICE_DETAIL/$deviceId"
    const val DEVICE_DETAIL_PATTERN = "$DEVICE_DETAIL/{$ARG_DEVICE_ID}"
}
