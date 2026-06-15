package com.wify.client.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wify.client.data.model.Device
import com.wify.client.data.model.DeviceStatus
import com.wify.client.ui.components.ConfirmDialog
import com.wify.client.ui.components.ResetPill
import com.wify.client.ui.components.RouterImage
import com.wify.client.ui.theme.ActionBlack
import com.wify.client.ui.theme.CardNeutralBg
import com.wify.client.ui.theme.CardWarningBg
import com.wify.client.ui.theme.StatusDanger
import com.wify.client.ui.theme.TextPrimary
import com.wify.client.ui.theme.TextSecondary
import com.wify.client.ui.theme.WIFYAPPAndroidTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 등록된 기기 화면 — this.png 디자인 기준.
 * 3상태: 블루투스 OFF / 빈 상태 / 그리드.
 * 카드 이미지 = 기기 사진 영역(기본 공유기). 톱니 메뉴로 개별 삭제. 초기화는 확인 모달.
 */
@Composable
fun DeviceListScreen(
    state: DeviceListUiState,
    onRegisterClick: () -> Unit,
    onDeviceClick: (deviceId: String) -> Unit,
    onReset: () -> Unit,
    onDeleteDevice: (deviceId: String) -> Unit,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showResetConfirm by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Device?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding(),
    ) {
        // 상단: ‹ 등록된 기기  +  초기화
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 12.dp),
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBackIos,
                    contentDescription = "뒤로가기",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = "등록된 기기",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )
            ResetPill(onClick = { showResetConfirm = true })
        }

        when {
            !state.transportAvailable -> CenterMessage("서버에 연결할 수 없어요.\n네트워크 연결을 확인해 주세요")
            state.isEmpty -> EmptyDeviceContent(onRegisterClick = onRegisterClick)
            else -> DeviceGrid(
                devices = state.devices,
                onDeviceClick = onDeviceClick,
                onDeleteRequest = { deleteTarget = it },
                onAddClick = onRegisterClick,
            )
        }
    }

    if (showResetConfirm) {
        ConfirmDialog(
            title = "전체 초기화",
            message = "등록된 모든 기기를 초기화하시겠습니까?",
            confirmText = "초기화",
            onConfirm = { showResetConfirm = false; onReset() },
            onDismiss = { showResetConfirm = false },
        )
    }
    deleteTarget?.let { target ->
        ConfirmDialog(
            title = "기기 삭제",
            message = "‘${target.name}’ 기기를 삭제하시겠습니까?",
            confirmText = "삭제",
            onConfirm = { onDeleteDevice(target.id); deleteTarget = null },
            onDismiss = { deleteTarget = null },
        )
    }
}

@Composable
private fun CenterMessage(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EmptyDeviceContent(onRegisterClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "등록된 기기가 없어요.\n기기를 등록해주세요.",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        OutlinedButton(
            onClick = onRegisterClick,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, ActionBlack),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ActionBlack),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text("기기 등록하기", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
        }
    }
}

@Composable
private fun DeviceGrid(
    devices: List<Device>,
    onDeviceClick: (String) -> Unit,
    onDeleteRequest: (Device) -> Unit,
    onAddClick: () -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(devices, key = { it.id }) { device ->
            DeviceCard(
                device = device,
                onClick = { onDeviceClick(device.id) },
                onDelete = { onDeleteRequest(device) },
            )
        }
        item(key = "__add__") {
            AddDeviceCard(onClick = onAddClick)
        }
    }
}

/** 그리드 끝의 "기기 추가" 카드 (기기가 있어도 추가 가능) */
@Composable
private fun AddDeviceCard(onClick: () -> Unit) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, Color(0xFFD9D9DE)),
        modifier = Modifier
            .fillMaxWidth()
            .height(184.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "기기 추가",
                tint = TextSecondary,
                modifier = Modifier.size(36.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text("기기 추가", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
        }
    }
}

/** 등록 시각(Long) → "yyyy.MM.dd" */
private fun formatRegisteredAt(millis: Long): String =
    SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(Date(millis))

/** 톱니 아이콘 + 삭제 드롭다운 */
@Composable
private fun GearMenu(tint: Color, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Icon(
            imageVector = Icons.Filled.Delete,
            contentDescription = "기기 삭제",
            tint = tint,
            modifier = Modifier
                .size(22.dp)
                .clickable { open = true },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("삭제하기", color = StatusDanger) },
                onClick = { open = false; onDelete() },
            )
        }
    }
}

/**
 * 기기 카드 — 모든 상태가 동일한 레이아웃(공유기 크기·위치 동일).
 * 정상=연회색 / 주의=연노랑 / 위험=빨강.
 * 위험일 때만 흰색 "낙상" 밴드가 공유기 하단을 덮는다.
 */
@Composable
private fun DeviceCard(device: Device, onClick: () -> Unit, onDelete: () -> Unit) {
    val isDanger = device.status == DeviceStatus.DANGER
    val bg = when (device.status) {
        DeviceStatus.WARNING -> CardWarningBg
        DeviceStatus.DANGER -> StatusDanger
        else -> CardNeutralBg
    }
    val nameColor = if (isDanger) Color.White else TextPrimary
    val dateColor = if (isDanger) Color.White else TextSecondary

    Surface(
        color = bg,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Column {
            // 공유기 이미지 영역 — 모든 카드 동일 비율·위치
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.3f),
            ) {
                RouterImage(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    imageUri = device.imageUri,
                )
                GearMenu(
                    tint = if (isDanger) Color.White else TextSecondary,
                    onDelete = onDelete,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                )
                // 위험: 흰색 "낙상" 밴드가 카드 가로 전체로 공유기 가운데를 덮음
                if (isDanger) {
                    Column(
                        Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "낙상",
                            style = MaterialTheme.typography.titleLarge,
                            color = StatusDanger,
                            fontWeight = FontWeight.Bold,
                        )
                        device.alertAtLabel?.let {
                            Text(it, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                        }
                    }
                }
            }
            // 이름 + 날짜
            Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 14.dp)) {
                Text(device.name, style = MaterialTheme.typography.titleMedium, color = nameColor, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(formatRegisteredAt(device.registeredAt), style = MaterialTheme.typography.labelMedium, color = dateColor)
            }
        }
    }
}

// ---------------------------------------------------------------- Previews
private fun previewDevices(): List<Device> = listOf(
    Device("1", "기기 1", "wify-001", 1_781_308_800_000L, DeviceStatus.NORMAL),
    Device("2", "wify_device_2", "wify-002", 1_781_308_800_000L, DeviceStatus.DANGER, alertAtLabel = "15:40"),
    Device("3", "wify_device_3", "wify-003", 1_781_308_800_000L, DeviceStatus.WARNING),
    Device("4", "wify_device_4", "wify-004", 1_781_308_800_000L, DeviceStatus.NORMAL),
    Device("5", "wify_device_5", "wify-005", 1_781_308_800_000L, DeviceStatus.NORMAL),
)

@Preview(name = "그리드", showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun DeviceListGridPreview() {
    WIFYAPPAndroidTheme {
        DeviceListScreen(
            state = DeviceListUiState(transportAvailable = true, devices = previewDevices()),
            onRegisterClick = {}, onDeviceClick = {}, onReset = {}, onDeleteDevice = {},
        )
    }
}

@Preview(name = "빈 상태", showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun DeviceListEmptyPreview() {
    WIFYAPPAndroidTheme {
        DeviceListScreen(
            state = DeviceListUiState(transportAvailable = true, devices = emptyList()),
            onRegisterClick = {}, onDeviceClick = {}, onReset = {}, onDeleteDevice = {},
        )
    }
}

@Preview(name = "연결 불가", showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun DeviceListBtOffPreview() {
    WIFYAPPAndroidTheme {
        DeviceListScreen(
            state = DeviceListUiState(transportAvailable = false, devices = emptyList()),
            onRegisterClick = {}, onDeviceClick = {}, onReset = {}, onDeleteDevice = {},
        )
    }
}
