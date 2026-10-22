package com.cashi.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/** Palette of the "Payments Demo App" mockup. */
object CashiColors {
    val Background = Color(0xFFF4F5F7)
    val Surface = Color(0xFFFFFFFF)
    val TextPrimary = Color(0xFF0F172A)
    val TextMuted = Color(0xFF5B6475)
    val Placeholder = Color(0xFF8A93A3)
    val Accent = Color(0xFF4F2C82)
    val AccentDark = Color(0xFF3B1F66)
    val Yellow = Color(0xFFF7B731)
    val OnYellow = Color(0xFF2E1A4F)
    val CardBorder = Color(0xFFECEEF2)
    val InputBorder = Color(0xFFDDE1E7)
    val BackButtonBorder = Color(0xFFE2E5EA)
    val SuccessBackground = Color(0xFFE5F3E9)
    val SuccessText = Color(0xFF17652F)
    val Error = Color(0xFFC62828)
    val Skeleton = Color(0xFFE6E8EC)
}

/** Text styles taken from the mockup (Manrope there; the platform sans-serif here). */
object CashiText {
    private fun style(size: TextUnit, weight: FontWeight, color: Color = CashiColors.TextPrimary, spacing: TextUnit = 0.sp) =
        TextStyle(fontFamily = FontFamily.Default, fontSize = size, fontWeight = weight, color = color, letterSpacing = spacing)

    val ScreenTitle = style(28.sp, FontWeight.ExtraBold, spacing = (-0.5).sp)
    val TopBarTitle = style(20.sp, FontWeight.ExtraBold)
    val Greeting = style(14.sp, FontWeight.Medium, CashiColors.TextMuted)
    val SectionTitle = style(18.sp, FontWeight.ExtraBold)
    val Link = style(14.sp, FontWeight.Bold, CashiColors.Accent)
    val Label = style(14.sp, FontWeight.Bold)
    val Input = style(16.sp, FontWeight.Medium)
    val Button = style(16.sp, FontWeight.Bold, CashiColors.OnYellow)
    val Helper = style(12.sp, FontWeight.Medium, CashiColors.TextMuted)
    val ErrorText = style(12.sp, FontWeight.SemiBold, CashiColors.Error)
    val Body = style(14.sp, FontWeight.Medium, CashiColors.TextMuted)
}

@Composable
fun CashiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = CashiColors.Accent,
            onPrimary = Color.White,
            secondary = CashiColors.Yellow,
            onSecondary = CashiColors.OnYellow,
            background = CashiColors.Background,
            onBackground = CashiColors.TextPrimary,
            surface = CashiColors.Surface,
            onSurface = CashiColors.TextPrimary,
            error = CashiColors.Error,
        ),
        content = content,
    )
}
