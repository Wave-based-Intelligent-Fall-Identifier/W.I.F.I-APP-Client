# 디버그 주입 서버 (가상 펌웨어 데이터 테스트)

> 실제 ESP32/BLE 기기 없이 **Postman·curl 로 가상 펌웨어 데이터**를 앱에 주입해,
> 화면/로그/알림이 제대로 뜨는지 확인하는 **디버그 빌드 전용** 도구.
>
> 주입된 데이터는 **실제 BLE 와 동일한 반영 경로**(`DeviceEventBridge.applyEvent`)를 타므로,
> 여기서 동작하면 실제 기기에서도 동일하게 동작한다.

## 1. 켜지는 조건

- **디버그 빌드에서만** 자동 시작한다(`ServiceLocator` 가 `FLAG_DEBUGGABLE` 확인). 릴리스 빌드에는 동작하지 않는다.
- 서버 포트: **8765** (앱 프로세스 안에서 `ServerSocket` 으로 listen).

## 2. 접속 방법

### (A) 에뮬레이터 — adb 포트 포워딩
```bash
adb forward tcp:8765 tcp:8765
# 이제 PC 의 Postman/curl 에서 http://localhost:8765 로 접근
```

### (B) 실기기 — 같은 Wi-Fi
```
http://<폰_IP>:8765      # 폰 설정 > Wi-Fi 에서 IP 확인
```

## 3. 엔드포인트

| 메서드·경로 | 설명 |
|------------|------|
| `GET  /devices` | 등록된 기기 목록(id/name/addr) 조회 |
| `POST /register?name=&addr=` | 가상 기기 등록(등록 화면에 표시). 생략 시 name=`wify_test` |
| `POST /fall?device=ID` | 낙상 감지 → 위험(빨강) + 로그 + **알림**. device 생략 시 첫 기기 |
| `POST /battery?device=ID&percent=NN` | 배터리 부족 → 주의(노랑) + 로그 + 알림 |
| `POST /normal?device=ID` | 정상 복귀 → 초록, 경보/알림 해제 |
| `POST /status?device=ID&hex=...` | **Status(0xFA12) 7바이트** 페이로드를 그대로 주입(상태 미러, 로그 없음) |
| `POST /fallevent?device=ID&hex=...` | **Fall Event(0xFA13) 8바이트** 페이로드를 그대로 주입(로그/알림) |
| `POST /calibrating?device=ID&on=1` | 베이스라인 **"측정 중…"** 표시 켜기(`on=1`)/끄기(`on=0`). 펌웨어 `flags.calibrating` 비트 시뮬레이션 |

> `hex` 는 BLE 페이로드 그대로다. 공백·콜론은 무시한다. 자세한 바이트 포맷은 `WIFY_BLE_PROTOCOL.md` 부록 B 참고.

## 4. 빠른 시나리오 (curl)

```bash
adb forward tcp:8765 tcp:8765

# 1) 가상 기기 등록
curl -X POST "http://localhost:8765/register?name=wify_test"
#   → {"registered":"dev_AABB...","name":"wify_test","addr":"AA:BB:.."}

# 2) 낙상 발생 (메인 화면 빨강 + 상단 알림 + 기록에 '낙상 감지')
curl -X POST "http://localhost:8765/fall"

# 3) 정상 복귀
curl -X POST "http://localhost:8765/normal"

# 4) 배터리 부족 (노랑 + 알림)
curl -X POST "http://localhost:8765/battery?percent=12"

# 5) 실제 펌웨어 바이트 그대로 주입 — Status: state=DANGER, battery=90%
curl -X POST "http://localhost:8765/status?hex=025A012C010000"

# 6) Fall Event 바이트 그대로 — FALL_DETECTED, seq=7, battery=90%
curl -X POST "http://localhost:8765/fallevent?hex=01075A0010270000"
```

## 5. Postman 사용

- Method `POST`, URL `http://localhost:8765/fall` (포워딩 후) 또는 `http://<폰IP>:8765/fall`.
- 파라미터는 URL 쿼리스트링으로(예: `.../battery?device=dev_x&percent=10`). Body 는 불필요.
- 먼저 `GET /devices` 로 등록된 기기의 `id` 를 확인한 뒤 `device=` 에 넣으면 특정 기기를 지정할 수 있다.

## 6. 동작 매핑 (주입 → 앱)

| 요청 | 화면 | 로그 | 알림 |
|------|------|------|------|
| `/fall` | 메인 "낙상 상태: 위험"(빨강) | "낙상 감지" 추가 | 낙상 헤드업 알림 |
| `/battery` | "주의"(노랑), 배터리 부족 표시 | "배터리 부족" 추가 | 배터리 알림 |
| `/normal` | "정상"(초록) | — | 낙상 알림 제거 |
| `/status` (스냅샷) | state/배터리 미러링 | **없음**(스팸 방지) | 없음 |
| `/fallevent` | type 에 따라 | type 에 따라 | type 에 따라 |
