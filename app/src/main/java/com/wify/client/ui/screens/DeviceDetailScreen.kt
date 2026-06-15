package com.wify.client.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wify.client.data.model.Device
import com.wify.client.data.model.DeviceStatus
import com.wify.client.data.model.LogEntry
import com.wify.client.data.model.LogType
import com.wify.client.ui.components.LogRow
import com.wify.client.ui.components.OutlinePill
import com.wify.client.ui.components.PrimaryActionButton
import com.wify.client.ui.components.RouterImage
import com.wify.client.ui.theme.ActionBlack
import com.wify.client.ui.theme.CardNeutralBg
import com.wify.client.ui.theme.TextPrimary
import com.wify.client.ui.theme.WIFYAPPAndroidTheme

/**
 * 기기 상세 화면 — 무상태(stateless).
 *
 * 두 가지 모드:
 *  - editing=false (선택 모드): 타이틀 "‹ {device.name}" + 우상단 "이름수정" 알약,
 *    하단 풀폭 "선택하기" 버튼.
 *  - editing=true (이름수정 모드): 타이틀 자리에 회색 배경 편집 입력필드,
 *    하단 풀폭 "저장하기" 버튼.
 *
 * 공통: RouterImage, "최근 로그" 섹션 + state.logs LogRow 렌더.
 */
@Composable
fun DeviceDetailScreen(
    state: DeviceDetailUiState,
    onBack: () -> Unit,
    onEditNameStart: () -> Unit,
    onEditNameChange: (String) -> Unit,
    onSaveName: () -> Unit,
    onSelect: () -> Unit,
    onChangeImage: (String?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val deviceName = state.device?.name ?: ""

    // 사진 선택기 (PickVisualMedia — 별도 권한 불필요)
    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let { onChangeImage(it.toString()) } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // ── 상단 타이틀 영역 ─────────────────────────────
            DetailTopBar(
                editing = state.editing,
                deviceName = deviceName,
                editName = state.editName,
                onBack = onBack,
                onEditNameChange = onEditNameChange,
                onEditNameStart = onEditNameStart,
            )

            Spacer(Modifier.height(24.dp))

            // ── 기기 사진 (탭하여 변경) ───────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center,
            ) {
                RouterImage(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            pickImage.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                    imageUri = state.device?.imageUri,
                )
                // 우하단 사진 변경 배지
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .clip(RoundedCornerShape(50))
                        .background(ActionBlack)
                        .clickable {
                            pickImage.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoCamera,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "사진 변경",
                        style = MaterialTheme.typography.labelMedium,
                        color = androidx.compose.ui.graphics.Color.White,
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── 최근 로그 섹션 ───────────────────────────────
            Text(
                text = "최근 로그",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
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

            // ── 하단 풀폭 액션 버튼 ──────────────────────────
            if (state.editing) {
                PrimaryActionButton(
                    text = "저장하기",
                    onClick = onSaveName,
                )
            } else {
                PrimaryActionButton(
                    text = "선택하기",
                    onClick = onSelect,
                )
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

/** 상단 바: 뒤로가기 + (타이틀 | 편집 입력필드) + (이름수정 알약) */
@Composable
private fun DetailTopBar(
    editing: Boolean,
    deviceName: String,
    editName: String,
    onBack: () -> Unit,
    onEditNameChange: (String) -> Unit,
    onEditNameStart: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "뒤로가기",
                tint = TextPrimary,
            )
        }

        Spacer(Modifier.width(8.dp))

        if (editing) {
            // 회색 하이라이트 편집 입력필드
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CardNeutralBg)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicTextField(
                    value = editName,
                    onValueChange = onEditNameChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineLarge.copy(color = TextPrimary),
                    cursorBrush = SolidColor(ActionBlack),
                )
            }
        } else {
            Text(
                text = deviceName,
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary,
                modifier = Modifier.weight(1f, fill = false),
            )

            Spacer(Modifier.width(12.dp))

            OutlinePill(
                text = "이름수정",
                onClick = onEditNameStart,
                contentColor = TextPrimary,
            )
        }
    }
}

// ───────────────────────────── Preview ─────────────────────────────

private val previewDevice = Device(
    id = "d1",
    name = "wify_device_1",
    deviceId = "wify-001",
    registeredAt = 0L,
    status = DeviceStatus.NORMAL,
)

private val previewLogs = listOf(
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
        timeLabel = "3:00",
    ),
)

@Preview(showBackground = true, name = "기기 상세 - 선택 모드")
@Composable
private fun DeviceDetailScreenSelectPreview() {
    WIFYAPPAndroidTheme {
        DeviceDetailScreen(
            state = DeviceDetailUiState(
                device = previewDevice,
                logs = previewLogs,
                editing = false,
                editName = previewDevice.name,
            ),
            onBack = {},
            onEditNameStart = {},
            onEditNameChange = {},
            onSaveName = {},
            onSelect = {},
        )
    }
}

@Preview(showBackground = true, name = "기기 상세 - 이름수정 모드")
@Composable
private fun DeviceDetailScreenEditPreview() {
    WIFYAPPAndroidTheme {
        DeviceDetailScreen(
            state = DeviceDetailUiState(
                device = previewDevice,
                logs = previewLogs,
                editing = true,
                editName = "기기 1",
            ),
            onBack = {},
            onEditNameStart = {},
            onEditNameChange = {},
            onSaveName = {},
            onSelect = {},
        )
    }
}
