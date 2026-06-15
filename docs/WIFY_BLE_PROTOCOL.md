# W.I.F.Y BLE GATT 프로토콜 명세 v1.0

> 낙상 감지 기기(ESP32) ↔ Android 앱 간 Bluetooth Low Energy 통신 규약.
> 이 문서는 **양측 구현의 단일 기준(single source of truth)** 이다.
> 펌웨어(ESP32, ESP-IDF/NimBLE)와 Android(`BleController` 구현)는 모두 이 문서를 따른다.
>
> - 문서 버전: **1.0**
> - 최종 갱신: 2026-06-11
> - 대상 펌웨어: ESP32 (Bluetooth LE) — WiFi는 v1 통신 범위 밖(아래 §9)

---

## 1. 역할 (Roles)

| 주체 | BLE 역할 | 설명 |
|------|----------|------|
| **ESP32 기기** | Peripheral / **GATT Server** | 광고(advertise)하고, 센서 상태/낙상 이벤트를 notify 한다. |
| **Android 앱** | Central / **GATT Client** | 스캔→연결→구독(subscribe)하고, 명령을 write 한다. |

- 한 번에 **1 central(앱) ↔ 1 peripheral(기기)** 연결을 기본 가정한다.
- 앱은 등록된 여러 기기에 각각 별도 연결을 맺을 수 있다(기기마다 1 GATT 연결).

---

## 2. 광고 (Advertising)

기기는 전원이 켜지고 연결되지 않은 동안 계속 connectable 광고를 송출한다.

| 항목 | 값 | 비고 |
|------|----|------|
| Advertising type | `ADV_IND` (connectable, undirected) | |
| **Complete Local Name** | `wify_` + 식별자 | 예: `wify_devkit`, `wify_a1b2`. **반드시 `wify_` 접두사**로 시작. |
| **Service UUID (16/128)** | W.I.F.Y Service (§3) | 광고 또는 Scan Response 패킷에 **Complete List of 128-bit Service UUIDs**로 포함. |
| Tx Power | 선택 | 포함 시 앱이 신호 등급 보정에 활용 가능. |
| 광고 주기 | 100–500 ms 권장 | 스캔 응답성과 전력 소비의 균형. |

**앱 스캔 필터링**: 앱은 1차로 **Service UUID = `0xFA11…`(§3)** 로 필터링한다. 보조로 이름 접두사 `wify_` 도 허용한다. 둘 중 하나만 만족해도 "찾은 기기" 목록에 노출된다.

> 펌웨어 구현 팁: 광고 패킷(31바이트)에 128-bit UUID(16바이트) + 이름이 함께 들어가면 공간이 빠듯하다. **광고 패킷에는 128-bit Service UUID**, **Scan Response 패킷에는 Complete Local Name** 으로 나눠 담는 것을 권장한다.

---

## 3. GATT 서비스 / 특성 (UUID)

모든 UUID는 128-bit 커스텀이다. 베이스의 `5749-4659` 구간은 ASCII `W I F Y`(0x57 0x49 0x46 0x59)에서 따왔다.
첫 16비트(`0000XXXX`)만 특성마다 바뀐다.

```
베이스: 0000XXXX-5749-4659-a100-574946592025
```

| 이름 | UUID | 속성(Properties) | 길이 |
|------|------|------------------|------|
| **W.I.F.Y Service** | `0000fa11-5749-4659-a100-574946592025` | Primary Service | — |
| **Status** | `0000fa12-5749-4659-a100-574946592025` | Read, **Notify** | 7 bytes |
| **Fall Event** | `0000fa13-5749-4659-a100-574946592025` | **Notify** | 8 bytes |
| **Command** | `0000fa14-5749-4659-a100-574946592025` | **Write** (With Response) | 1–5 bytes |

- Notify 특성(Status, Fall Event)에는 표준 **CCCD 디스크립터** `0x2902`(`00002902-0000-1000-8000-00805f9b34fb`)를 둔다. 앱이 `0x0001`을 write 하면 notify 활성화.
- **바이트 순서: 모든 다중 바이트 정수는 little-endian (LSB first).**
- ATT MTU 기본값(23)으로 충분하다(페이로드 ≤ 20 bytes). MTU 협상 불필요.

---

## 4. Status 특성 (`0xFA12`) — Read + Notify

기기의 현재 상태 스냅샷. **연결 직후 read 1회**, 이후 **값이 바뀔 때 notify**, 추가로 **주기적 heartbeat notify**(권장 5–10초)로 송출한다.

**길이: 7 bytes**

| 오프셋 | 크기 | 필드 | 타입 | 설명 |
|:------:|:----:|------|------|------|
| 0 | 1 | `state` | uint8 | 0 = NORMAL(정상), 1 = WARNING(주의), 2 = DANGER(위험/낙상) |
| 1 | 1 | `battery` | uint8 | 배터리 잔량 0–100 (%) |
| 2 | 1 | `flags` | uint8 | 비트필드 (아래) |
| 3 | 4 | `uptime_s` | uint32 LE | 부팅 후 경과 초 |

`flags` 비트:

| 비트 | 이름 | 의미 |
|:----:|------|------|
| 0 | `in_use` | 1 = 사용중(센서 활성) |
| 1 | `battery_low` | 1 = 배터리 부족 경고 |
| 2 | `charging` | 1 = 충전 중 |
| 3 | `calibrating` | 1 = **베이스라인 측정(보정) 진행 중**. 앱은 이 비트로 "측정 중…"을 표시한다. |
| 4–7 | reserved | 0 |

**예시**: `00 64 01 2C 01 00 00`
→ state=NORMAL, battery=100%, flags=0x01(in_use), uptime=0x0000012C=300초.

**앱 매핑**: `state` → 기기 카드 색(정상/주의/위험). `battery_low` → 배터리 부족 표시. `in_use` → 메인 화면 "사용중".

---

## 5. Fall Event 특성 (`0xFA13`) — Notify

이벤트가 **발생한 순간 1회** notify. (Status가 주기 스냅샷이라면, 이쪽은 에지 트리거 이벤트.)

**길이: 8 bytes**

| 오프셋 | 크기 | 필드 | 타입 | 설명 |
|:------:|:----:|------|------|------|
| 0 | 1 | `event_type` | uint8 | 이벤트 종류(아래) |
| 1 | 1 | `seq` | uint8 | 0–255 순환 시퀀스. 재전송/중복 제거용. |
| 2 | 1 | `battery` | uint8 | 이벤트 시점 배터리 % |
| 3 | 1 | `reserved` | uint8 | 0 |
| 4 | 4 | `device_ms` | uint32 LE | 부팅 후 경과 ms (상대 시각). 앱이 수신 시 자체 벽시계로 라벨링. |

`event_type` 값:

| 값 | 이름 | 의미 | 앱 동작 |
|:--:|------|------|---------|
| 1 | `FALL_DETECTED` | 낙상 감지 | 상태 → DANGER, 로그 "낙상 감지"(빨강) 추가 |
| 2 | `BATTERY_LOW` | 배터리 부족 | 상태 → WARNING, 로그 "기기 배터리 부족 알림"(노랑) 추가 |
| 3 | `FALL_CLEARED` | 낙상 해제(오탐/회복) | 상태 → NORMAL |
| 4 | `RECOVERED` | 정상 복귀 | 상태 → NORMAL, 경보 라벨 제거 |

**예시**: `01 07 5A 00 10 27 00 00`
→ FALL_DETECTED, seq=7, battery=90%, device_ms=0x00002710=10000ms.

> **시각 처리 규약**: 기기는 RTC가 없을 수 있으므로 `device_ms`는 **상대값**이다. 절대 시각(로그의 "HH:mm" 라벨)은 **앱이 notify를 수신한 벽시계 시각**으로 만든다. 기기에 정확한 시각이 필요하면 §6의 `SET_TIME` 명령으로 앱이 epoch를 내려준다.

---

## 6. Command 특성 (`0xFA14`) — Write (With Response)

앱 → 기기 제어 명령.

**길이: 1–5 bytes**

| 오프셋 | 크기 | 필드 | 설명 |
|:------:|:----:|------|------|
| 0 | 1 | `cmd` | 명령 코드(아래) |
| 1 | 4 | `arg` | uint32 LE 인자(명령별, 없으면 생략 가능) |

`cmd` 값:

| 값 | 이름 | arg | 동작 |
|:--:|------|-----|------|
| 0x01 | `RECALIBRATE` | — | **베이스라인(CSI 환경 기준선) 재측정.** 기기는 현재 환경을 기준으로 baseline 을 다시 잡는다(예: 펌웨어 `baseline_init()`/재수집). **낙상 기록·상태와는 무관**(앱은 기록을 지우지 않는다). 앱 메인 "초기화(베이스라인)" 버튼에 대응. |
| 0x02 | `IDENTIFY` | — | LED 약 2초 점멸(기기 식별). |
| 0x03 | `SET_TIME` | epoch_s (uint32) | 기기 내부 시각 설정(선택). |

> **호환성 메모**: v1.0 초안에서 0x01 은 `RESET`(낙상/경보 상태 클리어)이었으나, 실제 제품 정의에 맞춰 **0x01 의 의미를 "베이스라인 재측정"으로 재정의**했다(코드 값 0x01 은 그대로). 펌웨어는 0x01 수신 시 베이스라인 재측정을 수행하면 된다.
>
> **측정 진행 표시**: 베이스라인 측정은 샘플 수집에 시간이 걸린다. 펌웨어는 **측정을 시작하면 Status `flags.calibrating`(bit3) = 1** 로, **완료하면 0** 으로 notify 한다. 앱은 이 비트를 받아 "측정 중…"을 표시/해제한다(앱이 임의 타이머로 추정하지 않는다). 흐름: `앱 0x01 write → 펌웨어 측정 시작+calibrating=1 notify → (수집) → 완료 calibrating=0 notify`.

**예시**: `01` → RECALIBRATE(베이스라인 재측정). `03 80 51 4A 67` → SET_TIME(epoch=0x674A5180).

기기는 Write Request에 대해 **Write Response**로 ACK 한다(With Response).

---

## 7. 연결 시퀀스 (Connection Flow)

```
[앱]                                   [ESP32]
  | --- 스캔(Service 0xFA11 필터) --->   | (광고 중)
  | <-- ADV: name=wify_*, svc=0xFA11 --- |
  |                                      |
  | ----------- GATT Connect ---------> |
  | <---------- Connected -------------- |
  | ------- Discover Services --------> |
  | <------ Service/Char/CCCD ---------- |
  |                                      |
  | -- Write CCCD(0xFA12)=0x0001 -----> |  (Status notify 활성)
  | -- Write CCCD(0xFA13)=0x0001 -----> |  (Fall Event notify 활성)
  | ------- Read Status(0xFA12) ------> |
  | <------ Status 7 bytes ------------- |
  |                                      |
  | <==== Notify Status (주기/변화) ===== |
  | <==== Notify Fall Event (발생시) ==== |
  |                                      |
  | -- Write Command(0xFA14)=RESET ---> |
  | <-------- Write Response ----------- |
```

1. 앱이 Service UUID로 스캔, `wify_*` 기기를 "찾은 기기"에 표시.
2. 사용자가 "연결하기" → GATT connect.
3. 서비스 디스커버리.
4. Status·Fall Event CCCD에 `0x0001` write(notify 구독).
5. Status read 1회(초기 상태).
6. 이후 notify 수신. 필요 시 Command write.
7. 연결 해제 시 기기는 다시 광고 재개.

---

## 8. Android 매핑 (참고)

앱 내부 `DeviceEvent`(sealed)와의 대응:

| BLE 소스 | Android `DeviceEvent` | Repository 반영 |
|----------|----------------------|-----------------|
| Status.state=2 또는 Fall Event=FALL_DETECTED | `FallDetected(atMillis)` | status=DANGER, 로그 추가 |
| Fall Event=BATTERY_LOW 또는 flags.battery_low | `BatteryLow(percent)` | status=WARNING, batteryLow=true, 로그 추가 |
| Status.state=0 또는 Fall Event=FALL_CLEARED/RECOVERED | `StatusNormal(atMillis)` | status=NORMAL |

---

## 9. WiFi (범위 밖, v1)

ESP32의 WiFi 모듈은 **v1의 앱↔기기 통신 경로가 아니다.** 앱과의 모든 실시간 통신은 BLE로 한다.
WiFi는 기기 자체 용도(예: 펌웨어 OTA, 서버 직접 업로드)로 독립 사용할 수 있다.
향후 필요 시 **BLE 기반 WiFi 프로비저닝**(SSID/비밀번호를 추가 특성으로 전달)을 v2에서 정의한다.

---

## 10. 버전 / 호환성

| 버전 | 변경 |
|------|------|
| 1.0 | 최초 정의: Status / Fall Event / Command 3특성. |

- 하위 호환을 위해 **기존 필드 오프셋/의미는 변경하지 않는다.** 확장은 reserved 바이트 또는 새 특성으로.
- 펌웨어와 앱은 추후 Status 또는 광고에 **프로토콜 버전 바이트**를 추가하는 것을 권장(v2).

---

## 부록 A. UUID 빠른 참조 (복붙용)

```
SERVICE   0000fa11-5749-4659-a100-574946592025
STATUS    0000fa12-5749-4659-a100-574946592025   (Read, Notify, 7B)
FALLEVENT 0000fa13-5749-4659-a100-574946592025   (Notify, 8B)
COMMAND   0000fa14-5749-4659-a100-574946592025   (Write, 1-5B)
CCCD      00002902-0000-1000-8000-00805f9b34fb
```

## 부록 B. 페이로드 한눈에

```
Status (0xFA12), 7 bytes, little-endian:
  [0] state(0/1/2) [1] battery(0-100) [2] flags [3..6] uptime_s(u32)
  flags: bit0 in_use, bit1 battery_low, bit2 charging, bit3 calibrating(측정 중)

Fall Event (0xFA13), 8 bytes, little-endian:
  [0] event_type(1..4) [1] seq [2] battery [3] reserved [4..7] device_ms(u32)
  event_type: 1 FALL_DETECTED, 2 BATTERY_LOW, 3 FALL_CLEARED, 4 RECOVERED

Command (0xFA14), 1-5 bytes:
  [0] cmd(0x01..0x03) [1..4] arg(u32, optional)
  cmd: 0x01 RECALIBRATE(베이스라인 재측정), 0x02 IDENTIFY, 0x03 SET_TIME(arg=epoch_s)
```
