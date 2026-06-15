package com.wify.client.mqtt

/**
 * W.I.F.Y MQTT 토픽 빌더/파서 (docs/WIFY_MQTT_APP_PROTOCOL.md §3/§4).
 *
 * 모든 토픽은 `{prefix}/{device_id}/{suffix}` 형태. 표준 prefix = `wify`.
 * 서버/펌웨어와 1:1 로 일치해야 한다(§7 불일치 주의).
 */
class WifyTopics(private val prefix: String = MqttConfig.DEFAULT_PREFIX) {

    // ---- 발견(§2): wify/+/status (retained) ----
    /** 모든 기기의 status 와일드카드 구독 토픽 — announce 겸용(§2). */
    val statusWildcard: String get() = "$prefix/+/status"

    /** 등록된 기기의 전체 토픽 와일드카드 — connect 시 구독(§2). */
    fun deviceWildcard(deviceId: String): String = "$prefix/$deviceId/#"

    // ---- 구독 토픽 suffix (§3) ----
    fun status(deviceId: String): String = "$prefix/$deviceId/$STATUS"
    fun heartbeat(deviceId: String): String = "$prefix/$deviceId/$HEARTBEAT"
    fun ai(deviceId: String): String = "$prefix/$deviceId/$AI"
    fun restroom(deviceId: String): String = "$prefix/$deviceId/$RESTROOM"
    fun nowNetwork(deviceId: String): String = "$prefix/$deviceId/$NOWNETWORK"
    fun nowNetworkStatus(deviceId: String): String = "$prefix/$deviceId/$NOWNETWORK_STATUS"
    fun baselineStatus(deviceId: String): String = "$prefix/$deviceId/$BASELINE_STATUS"

    // ---- 발행 토픽 suffix (§4) ----
    fun baselineCmd(deviceId: String): String = "$prefix/$deviceId/$BASELINE_CMD"
    fun editNowNetwork(deviceId: String): String = "$prefix/$deviceId/$EDIT_NOWNETWORK"
    fun editNetwork(deviceId: String): String = "$prefix/$deviceId/$EDIT_EDITNETWORK"

    /**
     * `{prefix}/{device_id}/{suffix}` 토픽에서 device_id 와 suffix 를 추출한다.
     * prefix 가 안 맞거나 형식이 깨지면 null.
     */
    fun parse(topic: String): Parsed? {
        val parts = topic.split("/")
        if (parts.size < 3) return null
        if (parts[0] != prefix) return null
        val deviceId = parts[1]
        val suffix = parts.subList(2, parts.size).joinToString("/")
        return Parsed(deviceId, suffix)
    }

    /** parse 결과: device_id + suffix(예: `baseline/status`). */
    data class Parsed(val deviceId: String, val suffix: String)

    companion object {
        // 구독 suffix
        const val STATUS = "status"
        const val HEARTBEAT = "heartbeat"
        const val AI = "AI"
        const val RESTROOM = "restroom"
        const val NOWNETWORK = "nownetwork"
        const val NOWNETWORK_STATUS = "nownetwork/status"
        const val BASELINE_STATUS = "baseline/status"

        // 발행 suffix
        const val BASELINE_CMD = "baseline/cmd"
        const val EDIT_NOWNETWORK = "edit/nownetwork"
        const val EDIT_EDITNETWORK = "edit/editnetwork"

        // ---- 페이로드 상수 (§3/§4) ----
        const val PAYLOAD_ONLINE = "online"
        const val PAYLOAD_OFFLINE = "offline"

        const val AI_DANGER = "DAN"
        const val AI_WARNING = "WARN"
        const val AI_NORMAL = "NOR"

        const val RESTROOM_ACTIVE = "ACT"
        const val RESTROOM_DEACTIVE = "DEACT"
        const val RESTROOM_LOAD = "LOAD"

        const val BASELINE_MEASURING = "MEASURING"
        const val BASELINE_DONE = "DONE"

        const val NETWORK_CONNECT = "connect"
        const val NETWORK_DISCONNECT = "disconnect"

        /** baseline/cmd 발행 페이로드(JSON, §4/§7). */
        const val BASELINE_REBUILD_PAYLOAD = """{"cmd":"BASELINE_REBUILD"}"""

        /** edit/editnetwork 발행 페이로드(§4). */
        const val NEWNETWORK_EDIT_PAYLOAD = "NEWNETWORKEDIT"

        /** edit/nownetwork 발행 페이로드 빌더: `id:%s/passwd:%s` (§4). */
        fun nowNetworkPayload(ssid: String, password: String): String = "id:$ssid/passwd:$password"
    }
}
