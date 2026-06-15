package com.wify.client.di

import android.content.Context
import android.content.pm.ApplicationInfo
import com.wify.client.data.repository.InMemoryWifyRepository
import com.wify.client.data.repository.WifyRepository
import com.wify.client.data.repository.WifyStore
import com.wify.client.debug.DebugInjectionServer
import com.wify.client.mqtt.MqttConfig
import com.wify.client.mqtt.MqttDeviceTransport
import com.wify.client.notification.AndroidWifyNotifier
import com.wify.client.transport.DeviceEventBridge
import com.wify.client.transport.DeviceTransport
import com.wify.client.transport.FakeDeviceTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * 경량 의존성 제공자 (Hilt 미사용).
 *
 * [init] 를 Application 에서 1회 호출하면 실제 MQTT 전송([MqttDeviceTransport])을 사용한다.
 * 미초기화 상태(프리뷰/테스트)에서는 [FakeDeviceTransport] 로 폴백한다.
 */
object ServiceLocator {

    @Volatile
    private var _repo: WifyRepository? = null

    /** 미초기화(프리뷰/테스트) 시 시드 데이터 InMemory 로 폴백. [init] 후엔 영속 저장소와 연결됨. */
    val repository: WifyRepository
        get() = _repo ?: InMemoryWifyRepository().also { _repo = it }

    private val appScope = CoroutineScope(SupervisorJob())

    @Volatile
    private var _transport: DeviceTransport? = null

    val transport: DeviceTransport
        get() = _transport ?: FakeDeviceTransport().also { _transport = it }

    /** Application.onCreate 에서 호출. 실제 MQTT 전송 연결 + 이벤트 브리지 시작. */
    fun init(context: Context) {
        if (_transport is MqttDeviceTransport) return
        val app = context.applicationContext

        // 영속 저장소에서 복원(첫 실행이면 빈 상태로 시작).
        val store = WifyStore(app)
        val snapshot = store.load()
        val repo = InMemoryWifyRepository(
            initialDevices = snapshot?.devices ?: emptyList(),
            initialLogs = snapshot?.logs ?: emptyList(),
        )
        _repo = repo
        // 기기/로그가 바뀔 때마다 디스크에 저장.
        appScope.launch {
            combine(repo.observeDevices(), repo.observeLogs(null)) { d, l -> d to l }
                .collect { (d, l) -> store.save(d, l) }
        }

        // MQTT 브로커 직접 연결(§1). host/port 는 MqttConfig 기본값(앱 설정에서 override 가능).
        // TODO(§8): 설정 화면에서 host/port override 입력 연결.
        val transport = MqttDeviceTransport(MqttConfig())
        _transport = transport
        // MQTT 수신 → Repository 반영 + 시스템 알림
        val notifier = AndroidWifyNotifier(app)
        val bridge = DeviceEventBridge(repo, transport, notifier, appScope)
        bridge.start()
        // announce(`wify/+/status`) 구독 시작 → 발견 목록 채움(§2).
        appScope.launch { transport.startScan() }

        // 디버그 빌드 한정: 가상 MQTT 페이로드 HTTP 주입 서버(Postman 등으로 테스트).
        if ((app.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            DebugInjectionServer(repo, bridge, appScope).start()
        }
    }
}
