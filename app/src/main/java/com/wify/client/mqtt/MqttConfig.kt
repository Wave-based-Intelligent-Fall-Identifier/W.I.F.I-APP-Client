package com.wify.client.mqtt

import java.util.UUID

/**
 * MQTT 연결 파라미터 (docs/WIFY_MQTT_APP_PROTOCOL.md §1 "연결 파라미터").
 *
 * 기본값은 서버 `.env` 기준. 앱 설정에서 override 가능(host/port).
 * 에뮬레이터에서 호스트 PC 브로커에 붙으려면 host 를 `10.0.2.2` 로.
 */
data class MqttConfig(
    val host: String = DEFAULT_HOST,
    val port: Int = DEFAULT_PORT,
    /** 설치별 고유 client id: `wify-app-<UUID>`. */
    val clientId: String = "wify-app-${UUID.randomUUID()}",
    /** 토픽 prefix (표준 `wify`). */
    val prefix: String = DEFAULT_PREFIX,
    /** 모든 publish/subscribe 기본 QoS(at-least-once). */
    val qos: Int = DEFAULT_QOS,
) {
    companion object {
        const val DEFAULT_HOST = "192.168.0.10"
        const val DEFAULT_PORT = 1883
        const val DEFAULT_PREFIX = "wify"
        const val DEFAULT_QOS = 1

        // 에뮬레이터 → 호스트 PC 브로커.
        const val EMULATOR_HOST = "10.0.2.2"
    }
}
