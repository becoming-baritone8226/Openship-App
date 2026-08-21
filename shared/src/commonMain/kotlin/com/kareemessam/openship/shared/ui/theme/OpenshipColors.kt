package com.kareemessam.openship.shared.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object OpenshipColors {
    // ── Brand Accents ───────────────────────────────────────────────
    val GradientStart = Color(0xFF7C3AED) // Purple
    val GradientEnd = Color(0xFF3B82F6)   // Blue
    val BrandGradient = Brush.horizontalGradient(listOf(GradientStart, GradientEnd))

    // ── Mac Window Traffic Lights ───────────────────────────────────
    val MacClose = Color(0xFFFF5F56)
    val MacMinimize = Color(0xFFFFBD2E)
    val MacMaximize = Color(0xFF27C93F)

    // ── Dark Theme Tokens (from Openship theme.css) ─────────────────
    object Dark {
        val BgPage = Color(0xFF000000)
        val BgCard = Color(0xFF0A0A0A)
        val BgCardElevated = Color(0xFF141414)
        val BgSubtle = Color(0x08FFFFFF) // 3% white
        val BgHover = Color(0x0DFFFFFF) // 5% white
        val BgPill = Color(0xFF161616)
        val BgTerminal = Color(0xFF050505)

        val TextHeading = Color(0xFFFFFFFF)
        val TextTitle = Color(0xF2FFFFFF) // 95% white
        val TextStrong = Color(0xD9FFFFFF) // 85% white
        val TextBody = Color(0xA8FFFFFF) // 66% white
        val TextSecondary = Color(0x94FFFFFF) // 58% white
        val TextMuted = Color(0x80FFFFFF) // 50% white
        val TextGhost = Color(0x38FFFFFF) // 22% white

        val BorderDefault = Color(0x14FFFFFF) // 8% white
        val BorderSubtle = Color(0x0DFFFFFF) // 5% white
        val BorderStrong = Color(0x24FFFFFF) // 14% white
        val BorderCard = Color(0x12FFFFFF) // 7% white

        val InputBg = Color(0x0AFFFFFF)
        val InputBorder = Color(0x14FFFFFF)
        val InputBorderFocus = Color(0x3DFFFFFF)
        val ButtonPrimaryBg = Color(0xFFFFFFFF)
        val ButtonPrimaryText = Color(0xFF000000)

        val StatusSuccessFg = Color(0xFF34D399)
        val StatusSuccessBg = Color(0x1A10B981)
        val StatusSuccessBd = Color(0x3310B981)
        val StatusSuccessSolid = Color(0xFF10B981)

        val StatusDangerFg = Color(0xFFF04848)
        val StatusDangerBg = Color(0x1AEF4444)
        val StatusDangerBd = Color(0x33EF4444)
        val StatusDangerSolid = Color(0xFFEF4444)

        val StatusWarningFg = Color(0xFFFBBF24)
        val StatusWarningBg = Color(0x1AF59E0B)
        val StatusWarningBd = Color(0x33F59E0B)
        val StatusWarningSolid = Color(0xFFF59E0B)

        val StatusInfoFg = Color(0xFF60A5FA)
        val StatusInfoBg = Color(0x1A3B82F6)
        val StatusInfoBd = Color(0x333B82F6)
        val StatusInfoSolid = Color(0xFF3B82F6)

        val StatusNeutralFg = Color(0x80FFFFFF)
        val StatusNeutralBg = Color(0x0DFFFFFF)
        val StatusNeutralBd = Color(0x1AFFFFFF)
        val StatusNeutralSolid = Color(0xFF8B8F98)
    }

    // ── Light Theme Tokens (from Openship theme.css) ────────────────
    object Light {
        val BgPage = Color(0xFFF9F9F9)
        val BgCard = Color(0xFFFFFFFF)
        val BgCardElevated = Color(0xFFFFFFFF)
        val BgSubtle = Color(0x05000000) // 2% black
        val BgHover = Color(0x08000000) // 3% black
        val BgPill = Color(0xFFF3F4F6)
        val BgTerminal = Color(0xFF0D1117)

        val TextHeading = Color(0xFF000000)
        val TextTitle = Color(0xEB000000) // 92% black
        val TextStrong = Color(0xD1000000) // 82% black
        val TextBody = Color(0xA8000000) // 66% black
        val TextSecondary = Color(0x94000000) // 58% black
        val TextMuted = Color(0x85000000) // 52% black
        val TextGhost = Color(0x38000000) // 22% black

        val BorderDefault = Color(0xFFE8E8E8)
        val BorderSubtle = Color(0xFFF0F0F0)
        val BorderStrong = Color(0xFFD0D0D0)
        val BorderCard = Color(0xFFEAEAEA)

        val InputBg = Color(0xFFFFFFFF)
        val InputBorder = Color(0xFFE0E0E0)
        val InputBorderFocus = Color(0xFF000000)
        val ButtonPrimaryBg = Color(0xFF000000)
        val ButtonPrimaryText = Color(0xFFFFFFFF)

        val StatusSuccessFg = Color(0xFF059669)
        val StatusSuccessBg = Color(0x1A10B981)
        val StatusSuccessBd = Color(0x3310B981)
        val StatusSuccessSolid = Color(0xFF10B981)

        val StatusDangerFg = Color(0xFFDC2626)
        val StatusDangerBg = Color(0x1AEF4444)
        val StatusDangerBd = Color(0x33EF4444)
        val StatusDangerSolid = Color(0xFFEF4444)

        val StatusWarningFg = Color(0xFFD97706)
        val StatusWarningBg = Color(0x1AF59E0B)
        val StatusWarningBd = Color(0x33F59E0B)
        val StatusWarningSolid = Color(0xFFF59E0B)

        val StatusInfoFg = Color(0xFF2563EB)
        val StatusInfoBg = Color(0x1A3B82F6)
        val StatusInfoBd = Color(0x333B82F6)
        val StatusInfoSolid = Color(0xFF3B82F6)

        val StatusNeutralFg = Color(0xFF52525B)
        val StatusNeutralBg = Color(0xFFF4F4F5)
        val StatusNeutralBd = Color(0xFFE4E4E7)
        val StatusNeutralSolid = Color(0xFF9CA3AF)
    }
}

@Immutable
data class StatusStyle(
    val fg: Color,
    val bg: Color,
    val border: Color,
    val solid: Color
)

@Immutable
data class OpenshipCustomColors(
    val isDark: Boolean = true,
    val bgPage: Color = OpenshipColors.Dark.BgPage,
    val bgCard: Color = OpenshipColors.Dark.BgCard,
    val bgCardElevated: Color = OpenshipColors.Dark.BgCardElevated,
    val bgSubtle: Color = OpenshipColors.Dark.BgSubtle,
    val bgHover: Color = OpenshipColors.Dark.BgHover,
    val bgPill: Color = OpenshipColors.Dark.BgPill,
    val bgTerminal: Color = OpenshipColors.Dark.BgTerminal,
    val borderCard: Color = OpenshipColors.Dark.BorderCard,
    val borderDefault: Color = OpenshipColors.Dark.BorderDefault,
    val borderSubtle: Color = OpenshipColors.Dark.BorderSubtle,
    val borderStrong: Color = OpenshipColors.Dark.BorderStrong,
    val borderInput: Color = OpenshipColors.Dark.InputBorder,
    val borderFocus: Color = OpenshipColors.Dark.InputBorderFocus,
    val btnPrimaryBg: Color = OpenshipColors.Dark.ButtonPrimaryBg,
    val btnPrimaryText: Color = OpenshipColors.Dark.ButtonPrimaryText,
    val textHeading: Color = OpenshipColors.Dark.TextHeading,
    val textPrimary: Color = OpenshipColors.Dark.TextTitle,
    val textBody: Color = OpenshipColors.Dark.TextBody,
    val textSecondary: Color = OpenshipColors.Dark.TextSecondary,
    val textMuted: Color = OpenshipColors.Dark.TextMuted,
    val textGhost: Color = OpenshipColors.Dark.TextGhost,
    val statusActive: Color = OpenshipColors.Dark.StatusSuccessSolid,
    val statusFailed: Color = OpenshipColors.Dark.StatusDangerSolid,
    val statusFailedBorder: Color = OpenshipColors.Dark.StatusDangerBd,
    val brandGradient: Brush = OpenshipColors.BrandGradient,
    val success: StatusStyle = StatusStyle(
        OpenshipColors.Dark.StatusSuccessFg,
        OpenshipColors.Dark.StatusSuccessBg,
        OpenshipColors.Dark.StatusSuccessBd,
        OpenshipColors.Dark.StatusSuccessSolid
    ),
    val danger: StatusStyle = StatusStyle(
        OpenshipColors.Dark.StatusDangerFg,
        OpenshipColors.Dark.StatusDangerBg,
        OpenshipColors.Dark.StatusDangerBd,
        OpenshipColors.Dark.StatusDangerSolid
    ),
    val warning: StatusStyle = StatusStyle(
        OpenshipColors.Dark.StatusWarningFg,
        OpenshipColors.Dark.StatusWarningBg,
        OpenshipColors.Dark.StatusWarningBd,
        OpenshipColors.Dark.StatusWarningSolid
    ),
    val info: StatusStyle = StatusStyle(
        OpenshipColors.Dark.StatusInfoFg,
        OpenshipColors.Dark.StatusInfoBg,
        OpenshipColors.Dark.StatusInfoBd,
        OpenshipColors.Dark.StatusInfoSolid
    ),
    val neutral: StatusStyle = StatusStyle(
        OpenshipColors.Dark.StatusNeutralFg,
        OpenshipColors.Dark.StatusNeutralBg,
        OpenshipColors.Dark.StatusNeutralBd,
        OpenshipColors.Dark.StatusNeutralSolid
    )
)

fun darkOpenshipColors(): OpenshipCustomColors = OpenshipCustomColors(
    isDark = true,
    bgPage = OpenshipColors.Dark.BgPage,
    bgCard = OpenshipColors.Dark.BgCard,
    bgCardElevated = OpenshipColors.Dark.BgCardElevated,
    bgSubtle = OpenshipColors.Dark.BgSubtle,
    bgHover = OpenshipColors.Dark.BgHover,
    bgPill = OpenshipColors.Dark.BgPill,
    bgTerminal = OpenshipColors.Dark.BgTerminal,
    borderCard = OpenshipColors.Dark.BorderCard,
    borderDefault = OpenshipColors.Dark.BorderDefault,
    borderSubtle = OpenshipColors.Dark.BorderSubtle,
    borderStrong = OpenshipColors.Dark.BorderStrong,
    borderInput = OpenshipColors.Dark.InputBorder,
    borderFocus = OpenshipColors.Dark.InputBorderFocus,
    btnPrimaryBg = OpenshipColors.Dark.ButtonPrimaryBg,
    btnPrimaryText = OpenshipColors.Dark.ButtonPrimaryText,
    textHeading = OpenshipColors.Dark.TextHeading,
    textPrimary = OpenshipColors.Dark.TextTitle,
    textBody = OpenshipColors.Dark.TextBody,
    textSecondary = OpenshipColors.Dark.TextSecondary,
    textMuted = OpenshipColors.Dark.TextMuted,
    textGhost = OpenshipColors.Dark.TextGhost,
    statusActive = OpenshipColors.Dark.StatusSuccessSolid,
    statusFailed = OpenshipColors.Dark.StatusDangerSolid,
    statusFailedBorder = OpenshipColors.Dark.StatusDangerBd,
    brandGradient = OpenshipColors.BrandGradient,
    success = StatusStyle(OpenshipColors.Dark.StatusSuccessFg, OpenshipColors.Dark.StatusSuccessBg, OpenshipColors.Dark.StatusSuccessBd, OpenshipColors.Dark.StatusSuccessSolid),
    danger = StatusStyle(OpenshipColors.Dark.StatusDangerFg, OpenshipColors.Dark.StatusDangerBg, OpenshipColors.Dark.StatusDangerBd, OpenshipColors.Dark.StatusDangerSolid),
    warning = StatusStyle(OpenshipColors.Dark.StatusWarningFg, OpenshipColors.Dark.StatusWarningBg, OpenshipColors.Dark.StatusWarningBd, OpenshipColors.Dark.StatusWarningSolid),
    info = StatusStyle(OpenshipColors.Dark.StatusInfoFg, OpenshipColors.Dark.StatusInfoBg, OpenshipColors.Dark.StatusInfoBd, OpenshipColors.Dark.StatusInfoSolid),
    neutral = StatusStyle(OpenshipColors.Dark.StatusNeutralFg, OpenshipColors.Dark.StatusNeutralBg, OpenshipColors.Dark.StatusNeutralBd, OpenshipColors.Dark.StatusNeutralSolid)
)

fun lightOpenshipColors(): OpenshipCustomColors = OpenshipCustomColors(
    isDark = false,
    bgPage = OpenshipColors.Light.BgPage,
    bgCard = OpenshipColors.Light.BgCard,
    bgCardElevated = OpenshipColors.Light.BgCardElevated,
    bgSubtle = OpenshipColors.Light.BgSubtle,
    bgHover = OpenshipColors.Light.BgHover,
    bgPill = OpenshipColors.Light.BgPill,
    bgTerminal = OpenshipColors.Light.BgTerminal,
    borderCard = OpenshipColors.Light.BorderCard,
    borderDefault = OpenshipColors.Light.BorderDefault,
    borderSubtle = OpenshipColors.Light.BorderSubtle,
    borderStrong = OpenshipColors.Light.BorderStrong,
    borderInput = OpenshipColors.Light.InputBorder,
    borderFocus = OpenshipColors.Light.InputBorderFocus,
    btnPrimaryBg = OpenshipColors.Light.ButtonPrimaryBg,
    btnPrimaryText = OpenshipColors.Light.ButtonPrimaryText,
    textHeading = OpenshipColors.Light.TextHeading,
    textPrimary = OpenshipColors.Light.TextTitle,
    textBody = OpenshipColors.Light.TextBody,
    textSecondary = OpenshipColors.Light.TextSecondary,
    textMuted = OpenshipColors.Light.TextMuted,
    textGhost = OpenshipColors.Light.TextGhost,
    statusActive = OpenshipColors.Light.StatusSuccessSolid,
    statusFailed = OpenshipColors.Light.StatusDangerSolid,
    statusFailedBorder = OpenshipColors.Light.StatusDangerBd,
    brandGradient = OpenshipColors.BrandGradient,
    success = StatusStyle(OpenshipColors.Light.StatusSuccessFg, OpenshipColors.Light.StatusSuccessBg, OpenshipColors.Light.StatusSuccessBd, OpenshipColors.Light.StatusSuccessSolid),
    danger = StatusStyle(OpenshipColors.Light.StatusDangerFg, OpenshipColors.Light.StatusDangerBg, OpenshipColors.Light.StatusDangerBd, OpenshipColors.Light.StatusDangerSolid),
    warning = StatusStyle(OpenshipColors.Light.StatusWarningFg, OpenshipColors.Light.StatusWarningBg, OpenshipColors.Light.StatusWarningBd, OpenshipColors.Light.StatusWarningSolid),
    info = StatusStyle(OpenshipColors.Light.StatusInfoFg, OpenshipColors.Light.StatusInfoBg, OpenshipColors.Light.StatusInfoBd, OpenshipColors.Light.StatusInfoSolid),
    neutral = StatusStyle(OpenshipColors.Light.StatusNeutralFg, OpenshipColors.Light.StatusNeutralBg, OpenshipColors.Light.StatusNeutralBd, OpenshipColors.Light.StatusNeutralSolid)
)

val LocalOpenshipColors = staticCompositionLocalOf { OpenshipCustomColors() }
