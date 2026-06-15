package com.example.wify_app_android.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.wify_app_android.R

/**
 * 기기 이미지. [imageUri] 가 있으면 사용자가 지정한 사진을, 없으면 기본 공유기 이미지를 렌더.
 * 크기는 [modifier] 로 제어한다.
 */
@Composable
fun RouterImage(
    modifier: Modifier = Modifier,
    imageUri: String? = null,
    contentScale: ContentScale = ContentScale.Fit,
) {
    if (!imageUri.isNullOrBlank()) {
        val context = LocalContext.current
        val bitmap = remember(imageUri) {
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(imageUri)).use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            }.getOrNull()
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "기기 사진",
                contentScale = ContentScale.Crop,
                modifier = modifier,
            )
            return
        }
    }
    Image(
        painter = painterResource(R.drawable.ic_router),
        contentDescription = "기기",
        contentScale = contentScale,
        modifier = modifier,
    )
}
