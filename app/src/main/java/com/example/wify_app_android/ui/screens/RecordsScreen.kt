package com.example.wify_app_android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.wify_app_android.data.model.Device
import com.example.wify_app_android.data.model.DeviceStatus
import com.example.wify_app_android.data.model.LogEntry
import com.example.wify_app_android.data.model.LogType
import com.example.wify_app_android.ui.components.LogRow
import com.example.wify_app_android.ui.theme.TextPrimary
import com.example.wify_app_android.ui.theme.TextSecondary
import com.example.wify_app_android.ui.theme.WIFYAPPAndroidTheme

/**
 * 기록 화면 — 로그 리스트 + 기기 필터 드롭다운.
 * 무상태(stateless): [RecordsUiState] + 콜백만 입력으로 받는다.
 */
@Composable
fun RecordsScreen(
    state: RecordsUiState,
    onBack: () -> Unit,
    onSelectDeviceFilter: (deviceId: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var filterExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        // 상단 바: ‹ 기록  +  우상단 필터 아이콘 (기기 관리 화면과 동일 형식)
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
                text = "기록",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )

            Box {
                IconButton(onClick = { filterExpanded = true }) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = "기기 필터",
                        tint = TextPrimary,
                    )
                }
                DropdownMenu(
                    expanded = filterExpanded,
                    onDismissRequest = { filterExpanded = false },
                ) {
                    // 전체 (기기 구분 없이 모든 기록)
                    FilterMenuItem(
                        label = "전체",
                        selected = state.selectedDeviceId == null,
                        onClick = {
                            onSelectDeviceFilter(null)
                            filterExpanded = false
                        },
                    )
                    state.devices.forEach { device ->
                        FilterMenuItem(
                            label = device.name,
                            selected = device.id == state.selectedDeviceId,
                            onClick = {
                                onSelectDeviceFilter(device.id)
                                filterExpanded = false
                            },
                        )
                    }
                }
            }
        }

        // 본문: 로그 리스트
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(state.logs, key = { it.id }) { log ->
                LogRow(
                    type = log.type,
                    message = log.message,
                    timeLabel = log.timeLabel,
                )
            }
        }
    }
}

/** 기기 필터 드롭다운 항목 — 선택 시 굵게 강조 */
@Composable
private fun FilterMenuItem(label: String, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) TextPrimary else TextSecondary,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            )
        },
        onClick = onClick,
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun RecordsScreenPreview() {
    val devices = listOf(
        Device(
            id = "d1",
            name = "기기 1",
            deviceId = "wify-001",
            registeredAt = 0L,
            status = DeviceStatus.DANGER,
        ),
        Device(
            id = "d2",
            name = "기기 2",
            deviceId = "wify-002",
            registeredAt = 0L,
            status = DeviceStatus.NORMAL,
        ),
        Device(
            id = "d3",
            name = "기기 3",
            deviceId = "wify-003",
            registeredAt = 0L,
            status = DeviceStatus.WARNING,
        ),
    )
    val logs = listOf(
        LogEntry(
            id = "l1",
            deviceId = "d1",
            type = LogType.FALL_DETECTED,
            message = "낙상 감지",
            timestamp = 0L,
            timeLabel = "3:00",
        ),
        LogEntry(
            id = "l2",
            deviceId = "d1",
            type = LogType.BATTERY_LOW,
            message = "기기 배터리 부족 알림",
            timestamp = 0L,
            timeLabel = "2:40",
        ),
    )
    WIFYAPPAndroidTheme {
        RecordsScreen(
            state = RecordsUiState(
                logs = logs,
                devices = devices,
                selectedDeviceId = "d1",
            ),
            onBack = {},
            onSelectDeviceFilter = {},
        )
    }
}
