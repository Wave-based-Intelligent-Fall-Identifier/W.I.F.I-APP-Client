# W.I.F.Y 펌웨어 ↔ 앱 데이터 교환 명세

> ESP32 낙상 감지 기기(BLE 레퍼런스 펌웨어)와 Android 앱이 **실제로 주고받는 정보**와,
> 앱이 그 정보를 **어떻게 화면/상태/로그에 반영하는지**를 정리한 통합(integration) 문서다.
>
> - 바이트 단위 와이어 규약은 [`WIFY_BLE_PROTOCOL.md`](./WIFY_BLE_PROTOCOL.md) v1.0 (단일 기준)을 따른다. 이 문서는 그 위의 **의미/매핑 계약**이다.
> - 대상 펌웨어: `W.I.F.I-APP-Android/firmware/` (NimBLE GATT 서버) — **이 문서 작성 시점에 앱과 동일하게 검증됨**.
> - 최종 갱신: 2026-06-11

---

## 0. 전체 그림

```
        ┌────────────────────────┐   BLE GATT    ┌────────────────────────┐
        │   ESP32 (Peripheral)   │ ◄───────────► │   Android 앱 (Central) │
        │   BLE 레퍼런스 펌웨어  │   Notify/Write │   BleController          │
        └────────────────────────┘               └────────────────────────┘
            ▲ 광고 wify_devkit                        │ 스캔→연결→구독→명령
            │                                         ▼
   (별개 경로) ESP-NOW/CSI                        Repository → UI(상태색/로그/사용중)
   AT-Embedded ↔ STA-Embedded
```

- 앱↔기기 실시간 통신은 **전적으로 BLE**다. (WiFi/ESP-NOW/CSI는 기기 내부 센싱 경로이며 앱과 직접 주고받지 않는다 — `WIFY_BLE_PROTOCOL.md` §9.)
- `W.I.F.I-AT-Embedded` / `W.I.F.I-STA-Embedded` 는 CSI·PIR 기반 **센싱 측** 펌웨어로, 서로 ESP-NOW로 통신한다. 앱의 통신 대상이 아니다.

---

## 1. 주고받는 정보 요약

### 1.1 기기 → 앱 (Notify / Read)

| 정보 | 특성 | 성격 | 주기 | 앱에서의 용도 |
|------|------|------|------|---------------|
| **Status 스냅샷** | `0xFA12` (Read+Notify, 7B) | **상태 스냅샷** | 연결 직후 1회 read + 값 변화 시 + 10초 heartbeat | 기기 현재 상태를 **미러링**(상태색/사용중/배터리 플래그). **로그를 만들지 않는다.** |
| **Fall Event** | `0xFA13` (Notify, 8B) | **에지 트리거** | 사건 발생 순간 1회 | **로그 1줄** 생성(낙상/배터리부족/복귀) + 상태 갱신. |

### 1.2 앱 → 기기 (Write With Response)

| 정보 | 특성 | 트리거(앱) | 기기 동작 |
|------|------|-----------|-----------|
| **RECALIBRATE** (`0x01`) | `0xFA14` | 메인 화면 "초기화"(베이스라인) | 현재 환경 기준으로 CSI **베이스라인 재측정**. 낙상 기록과 무관. |
| IDENTIFY (`0x02`) | `0xFA14` | (현재 앱 UI 미노출 — §5) | LED 약 2초 점멸 |
| SET_TIME (`0x03`) | `0xFA14` | (현재 앱 UI 미노출 — §5) | 기기 내부 시각 설정(선택) |

---

## 2. 핵심 계약: "스냅샷"과 "에지"의 분리 ⭐

펌웨어는 **하나의 실제 사건**(예: 낙상)이 발생하면 두 가지를 **모두** 보낸다.

1. `0xFA13` **Fall Event** 1회 (`FALL_DETECTED`) — *사건이 일어난 순간*
2. `0xFA12` **Status** 갱신 (`state=DANGER`) — *그리고 이후 10초마다 같은 스냅샷 반복*

> 펌웨어 근거: `firmware/main/main.c`의 `trigger_fall()`은 `wify_notify_fall(...)` 직후 `push_status()`를 호출한다. 배터리 부족도 `BATTERY_LOW` 이벤트 1회 + 매 heartbeat Status(`batteryLow=1`)로 이어진다.

따라서 앱은 두 채널을 **다르게** 취급해야 한다 (그렇지 않으면 한 사건이 **두 번 로그**되거나, 배터리 부족이 **10초마다 무한 로그**된다):

| 채널 | 앱 처리 | 로그 |
|------|---------|------|
| **Status (`0xFA12`)** | 기기 상태를 그대로 **미러링**한다. 상태색/사용중/배터리 플래그만 갱신. | **남기지 않음** |
| **Fall Event (`0xFA13`)** | **에지 사건**으로 받아 로그 1줄 + 상태 갱신. | **남김** |

이 계약은 앱 코드에서 다음과 같이 구현된다:

```
AndroidBleController.handleStatus()    → DeviceEvent.StatusSnapshot   ─┐ (로그 X)
                                                                        ├─ DeviceEventBridge
AndroidBleController.handleFallEvent()  → DeviceEvent.FallDetected /    │
                                          BatteryLow / Recovered       ─┘ (로그 O)
```

- `StatusSnapshot` → `WifyRepository.syncDeviceState(...)` : 상태/플래그 미러, **로그 없음**
- `FallDetected` → `raiseFall(...)` : 상태 DANGER + 경보 라벨 + 로그
- `BatteryLow` → `raiseBatteryLow(...)` : 상태 WARNING + 배터리 플래그 + 로그
- `Recovered` → `markNormal(...)` : 상태 NORMAL + 경보/배터리 해제

---

## 3. 필드별 매핑 (기기 → 앱)

### 3.1 Status (`0xFA12`, 7바이트, little-endian)

| 오프셋 | 필드 | 앱 반영 (`syncDeviceState` 경유) |
|:------:|------|-----------------------------------|
| 0 | `state` (0/1/2) | `Device.status` ← NORMAL/WARNING/DANGER (메인 화면 "낙상 상태" 색·텍스트) |
| 1 | `battery` (0–100) | `DeviceEvent.StatusSnapshot.battery` 로 전달 (현재 UI 미표시 — §5) |
| 2 | `flags` bit0 `in_use` | `Device.inUse` ← 메인 화면 "사용중/미사용" |
| 2 | `flags` bit1 `battery_low` | `Device.batteryLow` (배터리 부족 표시 플래그) |
| 2 | `flags` bit2 `charging` | (현재 미사용) |
| 2 | `flags` bit3 `calibrating` | `Device.calibrating` ← 메인 화면 "측정 중…" 표시. 펌웨어가 베이스라인 측정 중이면 1. |
| 3–6 | `uptime_s` (u32) | (현재 미사용) |

> `state` 미러링 규칙: 펌웨어 `g_state`가 단일 진실원이다. 스냅샷이 `NORMAL`이면 앱은 경보 라벨(`alertAtLabel`)도 함께 해제한다. `DANGER`/`WARNING`이면 라벨은 보존(Fall Event가 세팅).

### 3.2 Fall Event (`0xFA13`, 8바이트, little-endian)

| `event_type` | 앱 `DeviceEvent` | Repository 반영 | 로그 |
|:--:|------|-----------------|------|
| 1 `FALL_DETECTED` | `FallDetected(now)` | status=DANGER, 경보 라벨 세팅 | "낙상 감지"(빨강) |
| 2 `BATTERY_LOW` | `BatteryLow(battery)` | status=WARNING(이미 DANGER면 유지), batteryLow=true | "기기 배터리 부족 알림"(노랑) |
| 3 `FALL_CLEARED` | `Recovered(now)` | status=NORMAL, 경보/배터리 해제 | — |
| 4 `RECOVERED` | `Recovered(now)` | status=NORMAL, 경보/배터리 해제 | — |

- `seq`, `battery`, `device_ms` 도 페이로드에 있으나(중복제거·상대시각용), 현재 앱은 수신 **벽시계 시각**으로 로그 라벨(`H:mm`)을 만든다. (`device_ms`는 RTC 없는 기기의 상대값이므로 — `WIFY_BLE_PROTOCOL.md` §5의 시각 처리 규약.)

---

## 4. 명령 매핑 (앱 → 기기)

| 앱 동작 | 코드 경로 | 전송 바이트 | 기기 반응 |
|---------|-----------|-------------|-----------|
| 메인 "초기화"(베이스라인) | `MainViewModel.recalibrate()` → `ble.sendRecalibrate()` | `0xFA14` ← `01` (RECALIBRATE) | 현재 환경 기준으로 CSI 베이스라인 재측정 |

- **낙상 기록/상태는 그대로 유지**한다. 이 버튼은 센서 보정만 요청하며, 앱 로컬 데이터를 지우지 않는다.
- 미연결 상태에서 `sendRecalibrate`는 안전하게 무시된다.
- Write With Response: 기기는 Write Response로 ACK한다.
- **측정 진행 표시**: 측정은 시간이 걸리므로, 펌웨어가 Status `flags.calibrating`(bit3)을 1→0 으로 notify 하면 앱이 "측정 중…" 버튼을 표시/해제한다. 앱은 임의 타이머로 추정하지 않고 **기기 신호를 그대로 미러**한다.

---

## 5. 현재 앱이 **수신은 하지만 화면에 노출하지 않는** 정보

디자인(화면 캡처) 범위상 아래 정보는 프로토콜로 받지만 UI에 노출하지 않는다. 데이터 경로는 살아 있어, UI만 추가하면 바로 표시 가능하다.

| 정보 | 상태 | 확장 시 |
|------|------|---------|
| `battery` 정확한 % | `StatusSnapshot.battery`로 수신됨, 미표시 | 기기 상세 화면에 배터리 게이지 추가 |
| IDENTIFY 명령 | `WifyGatt.identifyCommand()` 인코더만 존재, 버튼 없음 | 상세 화면 "기기 찾기(LED)" 버튼 → `BleController`에 `sendIdentify` 추가 |
| SET_TIME 명령 | 미구현(펌웨어는 수신 시 로그만) | 연결 직후 epoch 1회 전송(선택) |

> 이들은 **펌웨어를 수정하지 않고** 앱 UI만 추가하면 되는 향후 작업이다.

---

## 6. 연결 수명주기 (앱 측)

1. **스캔**: Service UUID `0xFA11` 또는 이름 접두사 `wify_`로 필터 → "찾은 기기".
2. **연결+등록**: `ScanViewModel.connect()` → GATT connect → 서비스 디스커버리 → Status·Fall CCCD 구독 → Status 1회 read → `registerDevice`.
3. **이벤트 구독**: `DeviceEventBridge`가 등록된 모든 기기에 대해 연결을 보장(`ble.connect`, 멱등)하고 `deviceEvents`를 구독.
4. **끊김 회복**: 연결이 끊기면(`STATE_DISCONNECTED`) 컨트롤러가 같은 GATT 핸들로 **자동 재연결**을 건다(`g.connect()`). 기기가 다시 광고하면 재연결 → 서비스 재탐색/구독 재개. 사용자가 명시적으로 `disconnect()`한 경우에만 재연결하지 않는다.
   - 낙상 감지는 안전 기능이므로, 일시적 BLE 단절이 모니터링을 영구히 멈추지 않도록 한 설계.

---

## 7. 불변식 / 주의사항

- **로그는 Fall Event(에지)에서만 생성**된다. Status(스냅샷)는 절대 로그를 만들지 않는다. (중복·스팸 방지의 핵심 계약 — §2.)
- 모든 다중 바이트 정수는 **little-endian**.
- 펌웨어 `g_state`가 상태의 단일 진실원이며, 앱 `Device.status`는 그 미러다. 앱이 임의로 상태를 만들지 않는다(에지 사건과 스냅샷만 반영).
- 바이트 규약 변경 시 **양측(`WIFY_BLE_PROTOCOL.md` + 펌웨어 `wify_protocol.h` + 앱 `WifyGatt.kt`)을 동시에** 갱신해야 한다.
