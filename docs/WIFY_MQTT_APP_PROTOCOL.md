# W.I.F.Y 앱 ↔ MQTT 통신 규약 (Client App) v0.1

> 안드로이드 **클라이언트 앱**이 MQTT 브로커와 주고받는 메시지 계약 + 마이그레이션 결정 기록.
> 기존 BLE(앱↔ESP 직접) 방식을 **MQTT(앱↔브로커↔서버↔ESP)** 로 전환하면서 작성.
> 서버(`W.I.F.I-Server`, FastAPI+aiomqtt)·펌웨어 팀이 **앱 쪽 가정**을 확인하는 용도.
>
> - 작성: 2026-06-15 / 대상 앱: `W.I.F.I-APP-Android`
> - 참조: Notion `MQTT part Dev note`, `Client`(앱 기능 명세서), org repo `W.I.F.I-Server`
> - ⚠️ 이 문서는 **앱 팀의 결정/가정**이다. 서버·펌웨어와 토픽/페이로드가 어긋나면 이 문서 §7(불일치)로 합의한다.

---

## 1. 아키텍처

```
[Android 앱]                         [ESP32]
   │  MQTT(pub/sub)                     │  MQTT(pub/sub)
   └──────────►  Mosquitto Broker  ◄────┘
                    ▲     │
                    │     ▼
              [ FastAPI 서버 ]  (상태 저장 · 이력 · REST/WS · 관리)
```

- 앱·ESP·서버 **모두 MQTT 클라이언트**. 앱은 브로커에 직접 연결한다(프로토콜=MQTT).
- 서버(FastAPI)는 별도로 상태/이력 저장·관리·푸시(FCM 등)를 담당한다. 앱은 서버 REST를 **필수로 쓰지 않는다**(MVP). 추후 이력/인증은 서버 REST로 보강(§8).
- **어드민 / 클라이언트 분리**: 토픽 관리·기기 프로비저닝 등 민감 작업은 **어드민**이 담당. 본 앱(클라이언트)은 **모니터링·알림·기본 제어**에 집중한다.

### 연결 파라미터 (기본값)
| 항목 | 값 | 비고 |
|------|----|------|
| Broker host | `192.168.0.10` | `.env`/서버 기준. 앱 설정에서 변경 가능. 에뮬레이터→호스트 PC는 `10.0.2.2`. |
| MQTT port | `1883` | TCP. (브로커 WebSocket은 `9001`) |
| Topic prefix | `wify` | |
| QoS | 기본 1 | 상태/이벤트/명령 모두 1(at-least-once). |
| Client ID | `wify-app-<설치UUID>` | 설치별 고유. |

---

## 2. 기기 발견 / 등록 (BLE 스캔 대체)

BLE 스캔이 사라졌으므로 **announce(공지) 토픽** 방식으로 대체한다.

- 기기(ESP)는 부팅·브로커 연결 시 **자기 id를 retained 로 공지**한다:
  `wify/{device_id}/status` = `online` (**retained=true**), LWT = `offline`.
- 앱은 `wify/+/status` 를 구독해 **현재 살아있는 device_id 목록**을 즉시 받는다(retained 라 구독 직후 수신).
- 사용자는 **기기 등록 코드(= device_id)** 를 입력하거나, 발견된 목록에서 선택해 등록한다(Client 명세서 "기기 추가 - 코드 입력").
- 등록 = 앱이 해당 기기의 `wify/{device_id}/#` 를 구독하기 시작(= "Connect").
- 해제(Disconnect) = 해당 기기 토픽 구독 해지. 등록 정보는 로컬에 유지.

> 결정: 별도 `wify/announce` 레지스트리 토픽을 따로 두지 않고 **retained `status` 를 announce 겸용**으로 쓴다(토픽 최소화). 어드민이 별도 레지스트리를 원하면 §7에서 합의.

---

## 3. 앱이 **구독**하는 토픽 (ESP/서버 → 앱)

`wify/{device_id}/...`. 페이로드는 문자열(아래 명시된 것만 JSON).

| Topic suffix | 의미 | Payload | 앱 처리 |
|--------------|------|---------|---------|
| `status` | 기기 온라인 여부 | `online` / `offline` | 기기 연결상태 표시(Connected/Disconnected). retained. |
| `heartbeat` | 작동 heartbeat | (임의) | 마지막 통신시각 갱신. |
| `AI` | AI 낙상 판단 | `DAN` / `WARN` / `NOR` | 에스컬레이션(§5). DAN=Critical, WARN=Warning, NOR=Normal. |
| `restroom` | PIR 사람 감지 | `ACT` / `DEACT` / `LOAD` | ACT→사용중(inUse=true), DEACT→해제, LOAD→초기화. |
| `nownetwork` | 현재 Wi-Fi 정보 | `id:%s/passwd:%s` 또는 JSON | Wi-Fi 설정 화면 현재값 표시. |
| `nownetwork/status` | Wi-Fi 연결상태 | `connect` / `disconnect` | Wi-Fi 상태 표시. |
| `baseline/status` | 베이스라인 측정 상태 | `MEASURING` / `DONE` | 진행중 표시(기존 calibrating 미러). |

---

## 4. 앱이 **발행**하는 토픽 (앱 → ESP/서버)

| Topic suffix | 의미 | Payload | 트리거 |
|--------------|------|---------|--------|
| `baseline/cmd` | 베이스라인 재구축 | `{"cmd":"BASELINE_REBUILD"}` (JSON) | 기준선 설정 버튼 |
| `edit/nownetwork` | 새 Wi-Fi 입력 | `id:%s/passwd:%s` | Wi-Fi 재설정(입력) |
| `edit/editnetwork` | 새 Wi-Fi 적용 | `NEWNETWORKEDIT` | Wi-Fi 재설정(적용) |

> ⚠️ 어드민/클라이언트 분리에 따라 **네트워크 자격증명 발행은 민감 작업**이다. MVP에선 앱이 직접 발행하지만, 운영에선 서버 REST(`POST /devices/{id}/network`) 또는 어드민 경유로 옮기는 것을 권장(§7·§8).

---

## 5. 로그 에스컬레이션 매핑 (Client 명세서 기준)

| 단계 | 색 | 트리거 | 앱 동작 |
|------|----|--------|---------|
| Normal | 회색 | `AI=NOR`, 연결/기준선/Wi-Fi 등 일반 이벤트 | 상태 NORMAL, 일반 로그 |
| Warning | 노랑 | `AI=WARN` | 상태 WARNING, 경고 로그 + 알림 |
| Critical | 빨강 | `AI=DAN` | 상태 DANGER(낙상 확정), 위험 로그 + 알림 |

- 기존 앱 `DeviceStatus{NORMAL,WARNING,DANGER}` 와 1:1 매핑.
- **알림**: WARN(낙상 의심)·DAN(낙상 확정) 수신 시 시스템 알림 발생.
- 로그는 **AI 상태 전이(에지)** 에서만 남긴다(같은 값 반복 수신은 상태 미러만, 로그 스팸 방지 — 기존 BLE 규약의 "스냅샷=로그 없음" 정신 계승).

---

## 6. BLE → MQTT 데이터 대응 (기존 자산 처리)

| 기존(BLE) | MQTT 대체 | 결정 |
|-----------|-----------|------|
| Status.state 0/1/2 | `AI` NOR/WARN/DAN | 동일 매핑 |
| Status.flags.in_use | `restroom` ACT/DEACT | 매핑 |
| Status.flags.calibrating | `baseline/status` MEASURING/DONE | 매핑 |
| **battery / battery_low / charging** | (MQTT에 없음) | **제거**. 배터리 UI 미표시. 추후 토픽 추가 시 복원(§7). |
| Fall Event(0xFA13) | `AI=DAN` | 별도 이벤트 특성 없음 → AI로 통합 |
| 기기 식별 = bleAddress | = device_id(코드) | 식별자 교체 |
| 스캔 신호세기(RSSI) | (없음) | 제거. 발견은 announce(§2) |

> 결정: 배터리는 현재 데이터 소스가 없어 **클라이언트 UI에서 제외**한다. 펌웨어가 `wify/{id}/battery` 같은 토픽을 추가하면 즉시 복원 가능하도록 코드 분기만 남긴다.

---

## 7. ⚠️ 발견된 불일치 (서버/펌웨어 팀 확인 필요)

조사 중 **Notion(갱신본) ↔ 서버 코드** 간 차이를 발견했다. 앱은 **Notion 갱신본**을 따랐다.

1. **baseline 토픽**
   - 서버 코드(`app/bridge.py`): `wify/{id}/baseline` ← `"BASELINE"`
   - Notion(갱신): `wify/{id}/baseline/cmd` ← `{"cmd":"BASELINE_REBUILD"}` + `wify/{id}/baseline/status` ← `MEASURING|DONE`
   - 👉 앱은 **Notion(cmd/status 분리, JSON)** 채택. **서버 `send_baseline()` 수정 필요.**
2. **토픽 prefix 오타**: Notion baseline 행이 `wifi/...`(i) 로 표기됨. 표준은 `wify`(y). 앱은 `wify` 사용.
3. **edit 토픽 오타**: Notion `edif/nownetwork`. 서버 코드는 `edit/nownetwork`. 앱은 **`edit/nownetwork`**(서버 코드 기준) 사용.
4. **페이로드 포맷**: MQTT 노트 Point에 "페이로드는 JSON"이라 했으나 status/AI/restroom 등은 현재 단순 문자열. 앱은 **단순 문자열 + baseline/cmd만 JSON**으로 처리. 전체 JSON 표준화 시 재합의.
5. **network 자격증명 경로**: 민감정보 직접 MQTT 발행 vs 서버 REST 경유 — 어드민 분리 정책에 맞춰 합의 필요(§4).

---

## 8. 범위 밖 / 향후 (TODO)

- [ ] 회원가입 / 로그인(인증) — 서버 발급 토큰으로 브로커 인증(예: `username/password` 또는 JWT) 연동. 현재는 익명 접속.
- [ ] 어드민 앱/패널 분리 — 토픽 관리·기기 프로비저닝.
- [ ] 이력 영속화 — 서버 REST(`GET /devices/{id}/events`)로 과거 로그 동기화(현재는 앱 로컬 보관).
- [ ] 위험(DAN) 발생 시 서버 FCM 푸시(앱이 백그라운드/종료여도 수신).
- [ ] 배터리 등 추가 텔레메트리 토픽.

---

## 9. 앱 구현 메모

- MQTT 라이브러리: **HiveMQ MQTT Client**(`com.hivemq:hivemq-mqtt-client`) — Android/Kotlin friendly, TCP+WS, 자동 재연결.
- 기존 추상화 계승: `DeviceTransport`(구 `BleController`) 인터페이스 뒤에서 MQTT 구현 교체. UI/ViewModel은 인터페이스에만 의존.
- 권한: BLE 권한 제거, `INTERNET`(+ `ACCESS_NETWORK_STATE`) 사용.
- 포그라운드 서비스 타입: `connectedDevice` → 네트워크 상시 연결 유지 용도로 유지(낙상 = 안전기능, 백그라운드 모니터링 지속).
