package com.wify.client

import android.app.Application
import com.wify.client.di.ServiceLocator
import com.wify.client.service.MonitoringService

/** 앱 진입점 — 실제 BLE 컨트롤러/이벤트 브리지를 초기화하고 모니터링 서비스를 시작한다. */
class WifyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        // 백그라운드에서도 낙상 모니터링이 유지되도록 포그라운드 서비스 시작.
        MonitoringService.start(this)
    }
}
