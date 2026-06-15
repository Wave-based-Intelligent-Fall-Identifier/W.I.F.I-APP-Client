package com.wify.client.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.wify.client.R

/**
 * Wanted Sans 폰트 패밀리.
 * 폰트 파일: app/src/main/res/font/wanted_sans_*.ttf (SIL OFL 1.1)
 */
val WantedSans = FontFamily(
    Font(R.font.wanted_sans_regular, FontWeight.Normal),
    Font(R.font.wanted_sans_medium, FontWeight.Medium),
    Font(R.font.wanted_sans_semibold, FontWeight.SemiBold),
    Font(R.font.wanted_sans_bold, FontWeight.Bold),
    Font(R.font.wanted_sans_extrabold, FontWeight.ExtraBold),
    Font(R.font.wanted_sans_black, FontWeight.Black),
)
