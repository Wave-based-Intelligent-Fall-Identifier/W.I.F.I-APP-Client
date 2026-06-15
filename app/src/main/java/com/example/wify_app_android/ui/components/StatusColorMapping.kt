package com.example.wify_app_android.ui.components

import androidx.compose.ui.graphics.Color
import com.example.wify_app_android.data.model.DeviceStatus
import com.example.wify_app_android.data.model.LogType
import com.example.wify_app_android.ui.theme.StatusDanger
import com.example.wify_app_android.ui.theme.StatusDangerBg
import com.example.wify_app_android.ui.theme.StatusNormal
import com.example.wify_app_android.ui.theme.StatusWarning
import com.example.wify_app_android.ui.theme.StatusWarningBg

/** 상태/로그 종류 → 색상 매핑 (정상=초록 / 주의=노랑 / 위험=빨강) */

fun DeviceStatus.foreground(): Color = when (this) {
    DeviceStatus.NORMAL -> StatusNormal
    DeviceStatus.WARNING -> StatusWarning
    DeviceStatus.DANGER -> StatusDanger
}

fun LogType.foreground(): Color = when (this) {
    LogType.FALL_DETECTED -> StatusDanger
    LogType.WARNING -> StatusWarning
    LogType.BATTERY_LOW -> StatusWarning
}

fun LogType.background(): Color = when (this) {
    LogType.FALL_DETECTED -> StatusDangerBg
    LogType.WARNING -> StatusWarningBg
    LogType.BATTERY_LOW -> StatusWarningBg
}
