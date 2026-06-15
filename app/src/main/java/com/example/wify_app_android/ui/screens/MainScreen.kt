package com.example.wify_app_android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.wify_app_android.data.model.Device
import com.example.wify_app_android.data.model.DeviceStatus
import com.example.wify_app_android.ui.components.ConfirmDialog
import com.example.wify_app_android.ui.components.foreground
import com.example.wify_app_android.ui.theme.ActionBlack
import com.example.wify_app_android.ui.theme.ResetOutline
import com.example.wify_app_android.ui.theme.TextPrimary
import com.example.wify_app_android.ui.theme.TextSecondary
import com.example.wify_app_android.ui.theme.WIFYAPPAndroidTheme

/**
 * 메인 모니터링 화면 (stateless).
 * 좌상단 현재 기기 / 우상단 기기·기록 이동 버튼,
 * 중앙 낙상 상태 + 빨강 외곽선 초기화 버튼, 하단 사용중 표시.
 */
@Composable
fun MainScreen(
    state: MainUiState,
    onDevicesClick: () -> Unit,
    onRecordsClick: () -> Unit,
    onRecalibrate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showResetConfirm by remember { mutableStateOf(false) }

    // 선택된 기기가 없으면 안내 화면
    if (state.device == null) {
        EmptyMain(onDevicesClick = onDevicesClick, modifier = modifier)
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        // 상단 바: 현재 기기 + 기기/기록 버튼
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "현재 기기 : ${state.device.name}",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            IconLabelPill(
                text = "기기",
                glyph = "≡", // ≡ 목록 아이콘
                onClick = onDevicesClick,
            )
            Spacer(Modifier.width(8.dp))
            IconLabelPill(
                text = "기록",
                glyph = "↧", // ↧ 정렬 아이콘
                onClick = onRecordsClick,
            )
        }

        // 중앙: 낙상 상태 + 초기화 버튼 + 사용중
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = fallStatusText(state.device.status),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(Modifier.height(16.dp))

            // 빨강 외곽선 큰 재측정 버튼 (가로 꽉). 측정 중이면 "측정 중…" 스피너로 비활성.
            val calibrating = state.device.calibrating
            OutlinedButton(
                onClick = { showResetConfirm = true },
                enabled = !calibrating,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, ResetOutline),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ResetOutline,
                    disabledContentColor = ResetOutline,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                if (calibrating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = ResetOutline,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "측정 중…",
                        style = MaterialTheme.typography.labelLarge,
                        color = ResetOutline,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        tint = ResetOutline,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "재측정",
                        style = MaterialTheme.typography.labelLarge,
                        color = ResetOutline,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = if (state.inUse) "사용중" else "미사용",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
        }
    }

    if (showResetConfirm) {
        ConfirmDialog(
            title = "베이스라인 재측정",
            message = "현재 환경을 기준으로 센서를 다시 보정합니다.\n낙상 기록은 그대로 유지됩니다.",
            confirmText = "재측정",
            onConfirm = { showResetConfirm = false; onRecalibrate() },
            onDismiss = { showResetConfirm = false },
        )
    }
}

/** 선택된 기기가 없을 때 안내 화면 */
@Composable
private fun EmptyMain(onDevicesClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "선택된 기기가 없어요.\n기기를 선택해주세요.",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "등록된 기기에서 모니터링할 기기를 선택하면\n낙상 상태를 확인할 수 있어요.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(
            onClick = onDevicesClick,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, ActionBlack),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ActionBlack),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text("기기 선택하러 가기", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
        }
    }
}

/** "낙상 상태 : " 라벨 + 상태 텍스트(상태색·굵게) */
@Composable
private fun fallStatusText(status: DeviceStatus?) = buildAnnotatedString {
    withStyle(SpanStyle(color = TextPrimary, fontWeight = FontWeight.Bold)) {
        append("낙상 상태 : ")
    }
    val (label, color) = when (status) {
        DeviceStatus.NORMAL -> "정상" to DeviceStatus.NORMAL.foreground()
        DeviceStatus.WARNING -> "주의" to DeviceStatus.WARNING.foreground()
        DeviceStatus.DANGER -> "위험" to DeviceStatus.DANGER.foreground()
        null -> "-" to TextPrimary
    }
    withStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold)) {
        append(label)
    }
}

/** 우상단 라벨 + 우측 아이콘이 들어가는 회색 외곽선 알약 */
@Composable
private fun IconLabelPill(
    text: String,
    glyph: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFFBDBDBD)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        contentPadding = PaddingValues(
            horizontal = 14.dp,
            vertical = 8.dp,
        ),
        modifier = modifier,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.width(6.dp))
        Text(glyph, style = MaterialTheme.typography.labelLarge)
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MainScreenPreview() {
    WIFYAPPAndroidTheme {
        Surface {
            MainScreen(
                state = MainUiState(
                    device = Device(
                        id = "1",
                        name = "기기명",
                        deviceId = "wify-001",
                        registeredAt = 0L,
                        status = DeviceStatus.NORMAL,
                    ),
                    inUse = true,
                ),
                onDevicesClick = {},
                onRecordsClick = {},
                onRecalibrate = {},
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MainScreenDangerPreview() {
    WIFYAPPAndroidTheme {
        Surface {
            MainScreen(
                state = MainUiState(
                    device = Device(
                        id = "2",
                        name = "침실 센서",
                        deviceId = "wify-002",
                        registeredAt = 0L,
                        status = DeviceStatus.DANGER,
                    ),
                    inUse = true,
                ),
                onDevicesClick = {},
                onRecordsClick = {},
                onRecalibrate = {},
            )
        }
    }
}
