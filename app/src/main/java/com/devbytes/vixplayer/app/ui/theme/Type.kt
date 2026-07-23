package com.devbytes.vixplayer.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.devbytes.vixplayer.app.R

val Inter: FontFamily = FontFamily(
    Font(R.font.inter_regular,  FontWeight.Normal),
    Font(R.font.inter_medium,   FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold,     FontWeight.Bold),
)
val Mono: FontFamily = FontFamily(Font(R.font.jetbrainsmono_medium, FontWeight.Medium))

val PlayerTypography = Typography(
    displayLarge  = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold,     fontSize = 28.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold,     fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium   = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall    = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge     = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal,   fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium    = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal,   fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall     = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal,   fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge    = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium,   fontSize = 14.sp, lineHeight = 18.sp),
    labelSmall    = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium,   fontSize = 11.sp, lineHeight = 14.sp),
)

val TimecodeStyle = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 16.sp)
