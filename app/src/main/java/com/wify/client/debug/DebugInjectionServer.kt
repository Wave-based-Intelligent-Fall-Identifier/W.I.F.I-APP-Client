package com.wify.client.debug

import android.util.Log
import com.wify.client.data.repository.WifyRepository
import com.wify.client.mqtt.WifyTopics
import com.wify.client.transport.DeviceEvent
import com.wify.client.transport.DeviceEventBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.ServerSocket
import java.net.Socket

/**
 * **디버그 전용** 초경량 HTTP 주입 서버. (의존성 0 — `ServerSocket` 직접 사용)
 *
 * 실제 ESP32/브로커 없이 Postman 등으로 가상 MQTT 페이로드를 쏘면, **실제 MQTT 수신과 동일한
 * 반영 경로**([DeviceEventBridge.applyEvent])를 타고 화면/로그/알림에 뜬다. 디버그 빌드에서만 시작한다.
 *
 * 폰과 같은 Wi-Fi 에서 `http://<폰IP>:8765` 로 접근. 페이로드는 MQTT 토픽 페이로드와 동일 문자열.
 * ```
 * GET  /devices                         등록 기기 목록(id/name/deviceId) 조회
 * POST /register?name=&id=              가상 기기 등록(id = device_id)
 * POST /ai?device=ID&v=DAN|WARN|NOR     wify/{id}/AI 주입(에스컬레이션 §5)
 * POST /status?device=ID&v=online|offline   wify/{id}/status 주입
 * POST /restroom?device=ID&v=ACT|DEACT|LOAD wify/{id}/restroom 주입
 * POST /baseline?device=ID&v=MEASURING|DONE wify/{id}/baseline/status 주입
 * POST /network?device=ID&v=connect|disconnect wify/{id}/nownetwork/status 주입
 * ```
 */
class DebugInjectionServer(
    private val repo: WifyRepository,
    private val bridge: DeviceEventBridge,
    private val scope: CoroutineScope,
    private val port: Int = 8765,
) {
    @Volatile
    private var server: ServerSocket? = null

    fun start() {
        scope.launch(Dispatchers.IO) {
            runCatching {
                val s = ServerSocket(port)
                server = s
                Log.i(TAG, "디버그 주입 서버 시작: http://0.0.0.0:$port")
                while (!s.isClosed) {
                    val client = runCatching { s.accept() }.getOrNull() ?: break
                    launch(Dispatchers.IO) { runCatching { handle(client) } }
                }
            }.onFailure { Log.w(TAG, "서버 오류", it) }
        }
    }

    private suspend fun handle(client: Socket) {
        client.use { sock ->
            val input = sock.getInputStream().bufferedReader()
            val requestLine = input.readLine() ?: return
            // 헤더는 소비만(본문은 쿼리 파라미터로 충분).
            while (true) {
                val line = input.readLine() ?: break
                if (line.isEmpty()) break
            }

            val parts = requestLine.split(" ")
            if (parts.size < 2) {
                respond(sock, 400, json("error" to "bad request"))
                return
            }
            val rawPath = parts[1]
            val path = rawPath.substringBefore("?")
            val query = parseQuery(rawPath.substringAfter("?", ""))

            val result = runCatching { route(path, query) }
            result.onSuccess { respond(sock, 200, it) }
                .onFailure { respond(sock, 400, json("error" to (it.message ?: "error"))) }
        }
    }

    private suspend fun route(path: String, q: Map<String, String>): JSONObject = when (path) {
        "/devices" -> devicesJson()

        "/register" -> {
            val name = q["name"] ?: "wify_test"
            val id = q["id"] ?: name
            val device = repo.registerDevice(id, name)
            json("registered" to device.id, "name" to device.name, "deviceId" to device.deviceId)
        }

        "/ai" -> {
            val id = resolveDeviceId(q["device"])
            val judgment = when ((q["v"] ?: WifyTopics.AI_NORMAL).uppercase()) {
                WifyTopics.AI_DANGER -> DeviceEvent.Judgment.DANGER
                WifyTopics.AI_WARNING -> DeviceEvent.Judgment.WARNING
                WifyTopics.AI_NORMAL -> DeviceEvent.Judgment.NORMAL
                else -> error("v 는 DAN|WARN|NOR")
            }
            bridge.applyEvent(id, nameOf(id), DeviceEvent.Ai(judgment))
            json("ok" to true, "device" to id, "AI" to judgment.name)
        }

        "/status" -> {
            val id = resolveDeviceId(q["device"])
            val online = (q["v"] ?: WifyTopics.PAYLOAD_ONLINE).equals(WifyTopics.PAYLOAD_ONLINE, ignoreCase = true)
            bridge.applyEvent(id, nameOf(id), DeviceEvent.Online(online))
            json("ok" to true, "device" to id, "online" to online)
        }

        "/restroom" -> {
            val id = resolveDeviceId(q["device"])
            val state = when ((q["v"] ?: WifyTopics.RESTROOM_ACTIVE).uppercase()) {
                WifyTopics.RESTROOM_ACTIVE -> DeviceEvent.RestroomState.ACTIVE
                WifyTopics.RESTROOM_DEACTIVE -> DeviceEvent.RestroomState.DEACTIVE
                WifyTopics.RESTROOM_LOAD -> DeviceEvent.RestroomState.LOAD
                else -> error("v 는 ACT|DEACT|LOAD")
            }
            bridge.applyEvent(id, nameOf(id), DeviceEvent.Restroom(state))
            json("ok" to true, "device" to id, "restroom" to state.name)
        }

        "/baseline" -> {
            val id = resolveDeviceId(q["device"])
            val measuring = (q["v"] ?: WifyTopics.BASELINE_MEASURING).uppercase() == WifyTopics.BASELINE_MEASURING
            bridge.applyEvent(id, nameOf(id), DeviceEvent.BaselineStatus(measuring))
            json("ok" to true, "device" to id, "measuring" to measuring)
        }

        "/network" -> {
            val id = resolveDeviceId(q["device"])
            val connected = (q["v"] ?: WifyTopics.NETWORK_CONNECT).equals(WifyTopics.NETWORK_CONNECT, ignoreCase = true)
            bridge.applyEvent(id, nameOf(id), DeviceEvent.NetworkStatus(connected))
            json("ok" to true, "device" to id, "connected" to connected)
        }

        else -> error("알 수 없는 경로: $path")
    }

    // ---- 헬퍼 ----
    private suspend fun resolveDeviceId(param: String?): String =
        param ?: repo.observeDevices().first().firstOrNull()?.id
        ?: error("등록된 기기가 없습니다. 먼저 POST /register 하세요.")

    private suspend fun nameOf(id: String): String =
        repo.observeDevice(id).first()?.name ?: id

    private suspend fun devicesJson(): JSONObject {
        val arr = JSONArray()
        repo.observeDevices().first().forEach {
            arr.put(JSONObject().put("id", it.id).put("name", it.name).put("deviceId", it.deviceId))
        }
        return JSONObject().put("devices", arr)
    }

    private fun parseQuery(raw: String): Map<String, String> =
        if (raw.isEmpty()) emptyMap()
        else raw.split("&").mapNotNull {
            val i = it.indexOf("="); if (i < 0) null
            else java.net.URLDecoder.decode(it.substring(0, i), "UTF-8") to
                java.net.URLDecoder.decode(it.substring(i + 1), "UTF-8")
        }.toMap()

    private fun json(vararg pairs: Pair<String, Any?>): JSONObject =
        JSONObject().apply { pairs.forEach { put(it.first, it.second) } }

    private fun respond(sock: Socket, code: Int, body: JSONObject) {
        val payload = body.toString()
        val status = if (code == 200) "200 OK" else "$code Error"
        val out = sock.getOutputStream()
        out.write(
            ("HTTP/1.1 $status\r\n" +
                "Content-Type: application/json; charset=utf-8\r\n" +
                "Content-Length: ${payload.toByteArray().size}\r\n" +
                "Connection: close\r\n\r\n" +
                payload).toByteArray(),
        )
        out.flush()
    }

    companion object {
        private const val TAG = "WifyDebugServer"
    }
}
