package com.example.wify_app_android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.wify_app_android.ui.theme.ActionBlack
import com.example.wify_app_android.ui.theme.ResetOutline

/** 초기화 등 — 빨강 외곽선 알약 버튼 */
@Composable
fun ResetPill(
    text: String = "초기화",
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.5.dp, ResetOutline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = ResetOutline),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        modifier = modifier,
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = ResetOutline)
    }
}

/** 회색 외곽선 알약 (이름수정, 연결하기 등 보조 액션) */
@Composable
fun OutlinePill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = Color.Transparent,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, Color(0xFFBDBDBD)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = background,
            contentColor = contentColor,
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        modifier = modifier,
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium)
    }
}

/** 풀폭 검정 액션 버튼 — 선택하기 / 저장하기 */
@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ActionBlack,
            contentColor = Color.White,
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = Color.White)
    }
}
