package com.example.wify_app_android.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.example.wify_app_android.ui.theme.StatusDanger
import com.example.wify_app_android.ui.theme.TextSecondary

/** 재사용 확인 다이얼로그 — "정말 ~ 하시겠습니까?" */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "확인",
    dismissText: String = "취소",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = StatusDanger, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText, color = TextSecondary)
            }
        },
    )
}
