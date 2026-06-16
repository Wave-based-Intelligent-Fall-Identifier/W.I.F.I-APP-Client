package com.wify.client.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wify.client.data.model.LogType
import com.wify.client.ui.theme.TextSecondary

/**
 * 로그 행 — 좌측 경고 아이콘 + 메시지(+기기 이름) + 우측 시각, 상태색 박스.
 *
 * @param deviceName null 이 아니면 메시지 아래에 어떤 기기의 로그인지 표시.
 *   기기 상세 화면처럼 단일 기기 맥락에선 null 로 두고, 전체 기록 화면처럼
 *   여러 기기 로그가 섞일 때만 전달한다.
 */
@Composable
fun LogRow(
    type: LogType,
    message: String,
    timeLabel: String,
    modifier: Modifier = Modifier,
    deviceName: String? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(type.background())
            .border(1.dp, type.foreground().copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text("⚠", color = type.foreground(), fontSize = 16.sp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                message,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (deviceName != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    deviceName,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
            }
        }
        Text(timeLabel, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
    }
}
