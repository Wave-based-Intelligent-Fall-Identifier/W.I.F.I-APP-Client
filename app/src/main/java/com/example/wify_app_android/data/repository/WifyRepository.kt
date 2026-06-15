package com.example.wify_app_android.data.repository

import com.example.wify_app_android.data.model.Device
import com.example.wify_app_android.data.model.FallState
import com.example.wify_app_android.data.model.LogEntry
import kotlinx.coroutines.flow.Flow

/**
 * W.I.F.Y 데이터 "API 형식" (계약).
 *
 * 로컬 전용. UI는 이 인터페이스에만 의존한다.
 * 현재 구현: [InMemoryWifyRepository] (시드 데이터).
 * 후속: Room 기반 구현으로 교체(동일 시그니처 유지).
 *
 * MQTT 이벤트 반영 메서드는 [com.example.wify_app_android.transport.DeviceEvent] 를
 * [com.example.wify_app_android.transport.DeviceEventBridge] 가 매핑해 호출한다.
 */
interface WifyRepository {

    // ---- 조회 (reactive) ----
    /** 등록된 기기 목록 */
    fun observeDevices(): Flow<List<Device>>

    /** 단일 기기 */
    fun observeDevice(deviceId: String): Flow<Device?>

    /** 기기별 로그 (deviceId == null 이면 전체) */
    fun observeLogs(deviceId: String?): Flow<List<LogEntry>>

    /** 현재 선택(모니터링)된 기기의 낙상 상태 */
    fun observeSelectedFallState(): Flow<FallState?>

    /** 현재 선택된 기기 */
    fun observeSelectedDevice(): Flow<Device?>

    // ---- 변경 ----
    /** 발견/코드 입력으로 기기를 등록한다(= device_id). 이미 있으면 기존 것 반환. */
    suspend fun registerDevice(deviceId: String, name: String): Device

    /** 기기 이름 수정 */
    suspend fun renameDevice(deviceId: String, newName: String)

    /** 기기 사진 변경 (null이면 기본 이미지로) */
    suspend fun setDeviceImage(deviceId: String, imageUri: String?)

    /** 기기 1개 삭제 */
    suspend fun deleteDevice(deviceId: String)

    /** 여러 기기 선택 삭제 */
    suspend fun deleteDevices(deviceIds: Set<String>)

    /** 모니터링 대상 기기 선택 (메인 화면 "선택하기") */
    suspend fun selectDevice(deviceId: String)

    /** 낙상 상태 초기화 */
    suspend fun resetFallState(deviceId: String)

    /** 전체 초기화 (등록된 기기 화면 "초기화") */
    suspend fun resetAll()

    // ---- MQTT 이벤트 반영 (DeviceEventBridge 가 호출, §3/§5) ----

    /**
     * AI=NOR — 상태 NORMAL 로 미러링. 경보 라벨 해제. **로그 없음**(미러).
     * 로그/알림 발생 판단(상태 전이)은 [DeviceEventBridge] 가 담당한다.
     */
    suspend fun markNormal(deviceId: String)

    /** AI=WARN — 상태 WARNING + 경고 로그 추가(낙상 의심, §5). */
    suspend fun raiseWarning(deviceId: String)

    /** AI=DAN — 상태 DANGER + 경보 라벨 + 위험 로그 추가(낙상 확정, §5). */
    suspend fun raiseFall(deviceId: String)

    /** `status` online/offline 미러(§3). 로그 없음. */
    suspend fun setOnline(deviceId: String, online: Boolean)

    /** `restroom` 사용중 여부 미러(§3). 로그 없음. */
    suspend fun setInUse(deviceId: String, inUse: Boolean)

    /** `baseline/status` MEASURING/DONE 미러(§3). 로그 없음. */
    suspend fun setCalibrating(deviceId: String, calibrating: Boolean)
}
