package com.example.wify_app_android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.example.wify_app_android.data.model.LogType
import com.example.wify_app_android.ui.theme.TextSecondary

/** 로그 행 — 좌측 경고 아이콘 + 메시지 + 우측 시각, 상태색 박스 */
@Composable
fun LogRow(
    type: LogType,
    message: String,
    timeLabel: String,
    modifier: Modifier = Modifier,
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
        Text(
            message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(timeLabel, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
    }
}
