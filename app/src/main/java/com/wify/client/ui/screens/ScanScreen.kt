package com.wify.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wify.client.data.model.ScannedDevice
import com.wify.client.ui.components.OutlinePill
import com.wify.client.ui.theme.ActionBlack
import com.wify.client.ui.theme.CardNeutralBg
import com.wify.client.ui.theme.StatusDanger
import com.wify.client.ui.theme.StatusNormal
import com.wify.client.ui.theme.TextPrimary
import com.wify.client.ui.theme.WIFYAPPAndroidTheme

/**
 * 찾은 기기 (ScanScreen) — BLE 스캔 결과 리스트.
 * 무상태(stateless). 상단바는 다른 화면(등록된 기기/기록)과 동일 형식.
 */
@Composable
fun ScanScreen(
    state: ScanUiState,
    onRescan: () -> Unit,
    onConnect: (device: ScannedDevice) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        // 상단 바: ‹ 찾은 기기 + 새로고침 (등록된 기기 화면과 동일 형식)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                    contentDescription = "뒤로가기",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = "찾은 기기",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )
            if (state.scanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = ActionBlack,
                )
            } else {
                IconButton(onClick = onRescan, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "다시 검색",
                        tint = TextPrimary,
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        ) {
            items(state.found, key = { it.deviceId }) { device ->
                ScannedDeviceRow(device = device, onConnect = { onConnect(device) })
            }
        }
    }
}

@Composable
private fun ScannedDeviceRow(
    device: ScannedDevice,
    onConnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // BLE 신호세기(RSSI) 대신 announce online 여부 표시(§6).
        Box(
            Modifier
                .size(10.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(if (device.online) StatusNormal else StatusDanger),
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = device.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        OutlinePill(
            text = "연결하기",
            onClick = onConnect,
            background = CardNeutralBg,
            contentColor = TextPrimary,
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun ScanScreenPreview() {
    WIFYAPPAndroidTheme {
        ScanScreen(
            state = ScanUiState(
                scanning = false,
                found = listOf(
                    ScannedDevice("wify-001", "wify_device_1", online = true),
                    ScannedDevice("wify-002", "wify_device_2", online = true),
                    ScannedDevice("wify-003", "wify_device_3", online = false),
                ),
            ),
            onRescan = {},
            onConnect = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun ScanScreenScanningPreview() {
    WIFYAPPAndroidTheme {
        ScanScreen(
            state = ScanUiState(scanning = true, found = emptyList()),
            onRescan = {},
            onConnect = {},
            onBack = {},
        )
    }
}
