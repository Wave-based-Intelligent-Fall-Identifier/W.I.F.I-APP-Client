package com.wify.client.mqtt

import android.util.Log
import com.wify.client.data.model.ScannedDevice
import com.wify.client.transport.DeviceEvent
import com.wify.client.transport.DeviceTransport
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

/**
 * MQTT 기반 [DeviceTransport] 구현 (HiveMQ MQTT Client, docs/WIFY_MQTT_APP_PROTOCOL.md).
 *
 * - 발견(§2): `wify/+/status` (retained) 구독 → 살아있는 device_id 수집.
 * - 연결(§2): `wify/{id}/#` 구독 시작 / 해제 = 구독 해지.
 * - 수신(§3): 토픽 suffix + 페이로드 → [DeviceEvent] 매핑.
 * - 발행(§4): baseline/cmd, edit/nownetwork, edit/editnetwork.
 *
 * 자동 재연결(`automaticReconnect`)으로 끊겨도 events 흐름이 유지된다.
 * 브로커 미연결/오류 시 안전하게 no-op 에 가깝게 동작한다(빈 결과/실패 Result).
 */
class MqttDeviceTransport(
    private val config: MqttConfig = MqttConfig(),
) : DeviceTransport {

    private val topics = WifyTopics(config.prefix)
    private val qos: MqttQos = MqttQos.fromCode(config.qos) ?: MqttQos.AT_LEAST_ONCE

    private val client: Mqtt3AsyncClient =
        MqttClient.builder()
            .useMqttVersion3()
            .identifier(config.clientId)
            .serverHost(config.host)
            .serverPort(config.port)
            .automaticReconnectWithDefaultConfig()
            .buildAsync()

    @Volatile
    private var connected = false

    // 발견된 device_id -> ScannedDevice (status retained 기준).
    private val discovered = MutableStateFlow<Map<String, ScannedDevice>>(emptyMap())

    // device_id -> 이벤트 SharedFlow (구독 중인 기기마다).
    private val eventFlows = ConcurrentHashMap<String, MutableSharedFlow<DeviceEvent>>()

    // 전체 메시지를 한 번만 받기 위한 글로벌 콜백 — connect 시 publishes 스트림으로 등록.
    private fun ensureConnected(): Boolean {
        if (connected) return true
        return runCatching {
            // 모든 수신 메시지를 글로벌 콜백으로 받아 토픽으로 분기한다.
            client.publishes(com.hivemq.client.mqtt.MqttGlobalPublishFilter.ALL) { publish ->
                runCatching { onMessage(publish.topic.toString(), publish.payloadAsBytes) }
                    .onFailure { Log.w(TAG, "onMessage error", it) }
            }
            client.connect().get()
            connected = true
            true
        }.getOrElse {
            Log.w(TAG, "connect failed: ${it.message}")
            false
        }
    }

    override val isAvailable: Boolean
        get() = connected || ensureConnected()

    override fun scannedDevices(): Flow<List<ScannedDevice>> =
        discovered.map { it.values.sortedBy { d -> d.deviceId } }

    override suspend fun startScan() {
        if (!ensureConnected()) return
        // 발견은 retained status 와일드카드 구독으로 즉시 채워진다(§2).
        runCatching {
            client.subscribeWith()
                .topicFilter(topics.statusWildcard)
                .qos(qos)
                .send()
                .get()
        }.onFailure { Log.w(TAG, "scan subscribe failed", it) }
    }

    override fun stopScan() {
        // status 와일드카드는 발견 유지를 위해 굳이 해지하지 않는다(no-op).
    }

    override suspend fun connect(deviceId: String): Result<Unit> {
        if (!ensureConnected()) return Result.failure(IllegalStateException("broker not connected"))
        eventFlows.getOrPut(deviceId) { newEventFlow() }
        return runCatching {
            client.subscribeWith()
                .topicFilter(topics.deviceWildcard(deviceId))
                .qos(qos)
                .send()
                .get()
            Unit
        }
    }

    override fun deviceEvents(deviceId: String): Flow<DeviceEvent> =
        eventFlows.getOrPut(deviceId) { newEventFlow() }

    override suspend fun sendBaselineRebuild(deviceId: String): Result<Unit> =
        publish(topics.baselineCmd(deviceId), WifyTopics.BASELINE_REBUILD_PAYLOAD)

    override suspend fun sendWifiEdit(deviceId: String, ssid: String, password: String): Result<Unit> {
        // §4: 새 자격증명 입력 → 적용. ⚠️ 민감 작업(§7).
        val now = publish(topics.editNowNetwork(deviceId), WifyTopics.nowNetworkPayload(ssid, password))
        if (now.isFailure) return now
        return publish(topics.editNetwork(deviceId), WifyTopics.NEWNETWORK_EDIT_PAYLOAD)
    }

    override fun disconnect(deviceId: String) {
        if (!connected) return
        runCatching {
            client.unsubscribeWith()
                .topicFilter(topics.deviceWildcard(deviceId))
                .send()
        }.onFailure { Log.w(TAG, "unsubscribe failed", it) }
    }

    // ---- 내부 ----

    private fun newEventFlow() = MutableSharedFlow<DeviceEvent>(replay = 1, extraBufferCapacity = 16)

    private fun publish(topic: String, payload: String): Result<Unit> {
        if (!ensureConnected()) return Result.failure(IllegalStateException("broker not connected"))
        return runCatching {
            client.publishWith()
                .topic(topic)
                .qos(qos)
                .payload(payload.toByteArray(StandardCharsets.UTF_8))
                .send()
                .get()
            Unit
        }
    }

    /** 수신 메시지 → 토픽 분기 → [DeviceEvent] 매핑/발견 갱신. */
    private fun onMessage(topic: String, payloadBytes: ByteArray) {
        val parsed = topics.parse(topic) ?: return
        val payload = String(payloadBytes, StandardCharsets.UTF_8).trim()
        val deviceId = parsed.deviceId

        when (parsed.suffix) {
            WifyTopics.STATUS -> {
                val online = payload.equals(WifyTopics.PAYLOAD_ONLINE, ignoreCase = true)
                // 발견 목록 갱신(announce 겸용): online 이면 추가, offline 이면 표시만.
                discovered.value = discovered.value + (deviceId to ScannedDevice(deviceId, deviceId, online))
                emit(deviceId, DeviceEvent.Online(online))
            }
            WifyTopics.AI -> mapAi(payload)?.let { emit(deviceId, DeviceEvent.Ai(it)) }
            WifyTopics.RESTROOM -> mapRestroom(payload)?.let { emit(deviceId, DeviceEvent.Restroom(it)) }
            WifyTopics.BASELINE_STATUS -> when (payload.uppercase()) {
                WifyTopics.BASELINE_MEASURING -> emit(deviceId, DeviceEvent.BaselineStatus(true))
                WifyTopics.BASELINE_DONE -> emit(deviceId, DeviceEvent.BaselineStatus(false))
            }
            WifyTopics.NOWNETWORK_STATUS -> when (payload.lowercase()) {
                WifyTopics.NETWORK_CONNECT -> emit(deviceId, DeviceEvent.NetworkStatus(true))
                WifyTopics.NETWORK_DISCONNECT -> emit(deviceId, DeviceEvent.NetworkStatus(false))
            }
            // heartbeat / nownetwork(현재값) 등은 현재 별도 이벤트로 매핑하지 않음(필요 시 §8 확장).
        }
    }

    private fun emit(deviceId: String, event: DeviceEvent) {
        eventFlows.getOrPut(deviceId) { newEventFlow() }.tryEmit(event)
    }

    private fun mapAi(payload: String): DeviceEvent.Judgment? = when (payload.uppercase()) {
        WifyTopics.AI_DANGER -> DeviceEvent.Judgment.DANGER
        WifyTopics.AI_WARNING -> DeviceEvent.Judgment.WARNING
        WifyTopics.AI_NORMAL -> DeviceEvent.Judgment.NORMAL
        else -> null
    }

    private fun mapRestroom(payload: String): DeviceEvent.RestroomState? = when (payload.uppercase()) {
        WifyTopics.RESTROOM_ACTIVE -> DeviceEvent.RestroomState.ACTIVE
        WifyTopics.RESTROOM_DEACTIVE -> DeviceEvent.RestroomState.DEACTIVE
        WifyTopics.RESTROOM_LOAD -> DeviceEvent.RestroomState.LOAD
        else -> null
    }

    companion object {
        private const val TAG = "WifyMqtt"
    }
}
