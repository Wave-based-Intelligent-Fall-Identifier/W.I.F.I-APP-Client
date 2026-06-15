# W.I.F.I-APP-Client 문서

이 앱은 **사용자용 모니터링/알림 클라이언트**이며, 기기와의 통신은 **MQTT**(Mosquitto 브로커 경유)로 동작합니다.

## 현재 유효 문서

- **[WIFY_MQTT_APP_PROTOCOL.md](WIFY_MQTT_APP_PROTOCOL.md)** — 앱↔브로커 MQTT 계약(토픽/페이로드/발견/에스컬레이션). 서버팀과 공유하는 단일 기준 문서.

## 레거시

- [legacy/](legacy/) — BLE 시절(2026-06-15 이전) 문서. 보관용, 현재 미적용.

## 관련 레포

- `W.I.F.I-APP-Admin` — 개발자/운영진용 관리 앱(토픽 모니터/명령 발행).
- `W.I.F.I-Server` — FastAPI + aiomqtt 서버.
