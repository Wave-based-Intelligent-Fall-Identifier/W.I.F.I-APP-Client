package com.wify.client.data.model

/** 기기/낙상 상태 — 디자인의 정상(초록)/주의(노랑)/위험(빨강)에 대응 */
enum class DeviceStatus { NORMAL, WARNING, DANGER }

/** 등록된 기기 (로컬 전용). Room 영속화 시 동일 필드를 @Entity로 매핑 예정. */
data class Device(
    val id: String,
    val name: String,
    /** device_id(= 등록 코드). MQTT 토픽 `wify/{deviceId}/...` 식별자(구 bleAddress 대체, §6). */
    val deviceId: String,
    val registeredAt: Long,
    val status: DeviceStatus = DeviceStatus.NORMAL,
    /** 기기 MQTT 온라인 여부(`status` retained, §3). */
    val online: Boolean = false,
    /**
     * 배터리 부족 플래그 — **현재 MQTT 데이터 소스 없음**(§6). UI 미표시.
     * 펌웨어가 `wify/{id}/battery` 토픽을 추가하면 즉시 복원할 수 있도록 코드 seam 만 유지.
     * TODO(§7): battery 토픽 추가 시 surface.
     */
    val batteryLow: Boolean = false,
    val inUse: Boolean = true,
    /** 베이스라인(환경 기준선) 측정 진행 중 — `baseline/status` MEASURING 미러(§3) */
    val calibrating: Boolean = false,
    /** 위험 상태 발생 시각 라벨 (예: "15:40"), 없으면 null */
    val alertAtLabel: String? = null,
    /** 사용자가 설정한 기기 사진 URI (content://...), 없으면 기본 공유기 이미지 */
    val imageUri: String? = null,
)
