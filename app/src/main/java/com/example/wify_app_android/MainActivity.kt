package com.example.wify_app_android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.example.wify_app_android.di.ServiceLocator
import com.example.wify_app_android.notification.AndroidWifyNotifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.wify_app_android.ui.navigation.WifyNavHost
import com.example.wify_app_android.ui.theme.WIFYAPPAndroidTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { /* 결과 무관: 거부 시 스캔/알림이 비활성 */ }

    /** (13+) 알림 권한을 요청한다. (MQTT 전환 후 BLE 권한 불필요 — INTERNET 은 런타임 권한 아님.) */
    private val permissionsToRequest: Array<String>
        get() {
            val list = mutableListOf<String>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                list += Manifest.permission.POST_NOTIFICATIONS
            }
            return list.toTypedArray()
        }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 아직 안 부여된 권한이 있을 때만 요청(이미 부여 시 권한 컨트롤러 불필요 호출 방지).
        val missing = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) requestPermissions.launch(missing.toTypedArray())
        handleNotificationIntent(intent)
        enableEdgeToEdge()
        setContent {
            WIFYAPPAndroidTheme {
                // 터치 시 회색 리플(물결) 효과 전체 제거
                CompositionLocalProvider(LocalRippleConfiguration provides null) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        WifyNavHost()
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    /** 알림 탭으로 진입한 경우, 알림이 가리키는 기기를 모니터링 대상으로 선택한다. */
    private fun handleNotificationIntent(intent: Intent?) {
        val deviceId = intent?.getStringExtra(AndroidWifyNotifier.EXTRA_DEVICE_ID) ?: return
        lifecycleScope.launch { ServiceLocator.repository.selectDevice(deviceId) }
    }
}
