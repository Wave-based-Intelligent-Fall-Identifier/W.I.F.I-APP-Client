package com.wify.client.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * W.I.F.Y 앱 시맨틱 색상 토큰.
 * 디자인 캡처(design/) 기반 추출값. 정확한 HEX는 추후 Figma 수치로 보정.
 * 낙상 감지 모니터링 앱의 상태 표현(정상/주의/위험)에 사용.
 */

// 상태 — 낙상/기기 상태
val StatusNormal = Color(0xFF3DB16B)        // 정상 (초록)
val StatusWarning = Color(0xFFF2C94C)       // 주의 / 배터리 부족 (노랑)
val StatusDanger = Color(0xFFF0635A)        // 위험 / 낙상 감지 (빨강/코랄)

// 상태 박스 배경 (로그 행 / 카드 강조)
val StatusWarningBg = Color(0xFFFFF8E1)     // 노랑 박스 배경
val StatusDangerBg = Color(0xFFFDECEC)      // 빨강 박스 배경
val CardNeutralBg = Color(0xFFEFEFF4)       // 기본 카드 배경 (연회색)
val CardWarningBg = Color(0xFFF9E59A)       // 주의 카드 배경 (부드러운 노랑)

// 액션
val ActionBlack = Color(0xFF111111)         // 선택하기 / 저장하기 버튼 (검정)
val ResetOutline = Color(0xFFF0635A)        // 초기화 알약 (빨강 테두리/텍스트)

// 텍스트
val TextPrimary = Color(0xFF1A1A1A)
val TextSecondary = Color(0xFF8A8A8E)
