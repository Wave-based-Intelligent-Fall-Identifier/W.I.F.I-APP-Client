package com.wify.client.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * W.I.F.Y Typography — Wanted Sans 적용.
 * 디자인 캡처 기준: 화면 타이틀(등록된 기기/기록 등)은 굵게, 본문/라벨은 보통.
 */
val Typography = Typography(
    // 화면 대형 타이틀 — "등록된 기기", "찾은 기기", "기록"
    headlineLarge = TextStyle(
        fontFamily = WantedSans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = WantedSans,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    // 섹션 타이틀 — "최근 로그", 카드 기기명
    titleLarge = TextStyle(
        fontFamily = WantedSans,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = WantedSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    // 본문 / 안내 문구
    bodyLarge = TextStyle(
        fontFamily = WantedSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = WantedSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    // 버튼 / 라벨 — "선택하기", "연결하기", "초기화", "등록일"
    labelLarge = TextStyle(
        fontFamily = WantedSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = WantedSans,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = WantedSans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
    ),
)
