package com.spotify.sovereign.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.spotify.sovereign.model.AppThemePreset
import com.spotify.sovereign.model.CustomThemeColors
import com.spotify.sovereign.model.TrackColorPalette

/**
 * Legacy static fallback tokens.
 */
object SpotifyColors {
    val OledBlack = Color(0xFF000000)
    val Background = Color(0xFF0A0A0C)
    val Surface = Color(0xFF141418)
    val GlassSurface = Color(0xCC18181E)
    val GlassBorder = Color(0x1FFFFFFF)
    val Card = Color(0xFF1C1C22)
    val Pill = Color(0xFF22222A)
    val Green = Color(0xFF1ED760)
    val GreenGlow = Color(0x331ED760)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextMuted = Color(0xFFA8A8B3)
    val TextSubtle = Color(0xFF6B6B76)
    val ExplicitBg = Color(0xFFA8A8B3)
    val DefaultAccent = Color(0xFF2B2E3B)
    val DefaultMiniBg = Color(0xFF181A22)
}

/**
 * Live Reactive Sovereign Palette driven by:
 * - Match Current Song (real extracted album colors, monochrome silver-white on black covers)
 * - AMOLED Black (#000000)
 * - Tokyo Night (#1A1B26 / #C0CAF5 / #7AA2F7)
 * - Custom (User-picked Background / Foreground / Accent!)
 */
data class SovereignPalette(
    val accent: Color = Color(0xFFE6E8F0),
    val accentGlow: Color = Color(0x33E6E8F0),
    val background: Color = Color(0xFF08080B),
    val backgroundDeep: Color = Color(0xFF000000),
    val surface: Color = Color(0xFF121218),
    val card: Color = Color(0xFF1A1A22),
    val pill: Color = Color(0xFF22222C),
    val glassSurface: Color = Color(0xD614141B),
    val glassBorder: Color = Color(0x1FFFFFFF),
    val textPrimary: Color = Color(0xFFFFFFFF),
    val textMuted: Color = Color(0xFFA8A8B3),
    val textSubtle: Color = Color(0xFF6B6B76),
)

val LocalSovereignPalette = staticCompositionLocalOf { SovereignPalette() }

object SovereignTheme {
    val colors: SovereignPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalSovereignPalette.current
}

private fun liftSurface(base: Color, delta: Float): Color = Color(
    red = (base.red + delta).coerceIn(0f, 1f),
    green = (base.green + delta).coerceIn(0f, 1f),
    blue = (base.blue + delta).coerceIn(0f, 1f),
    alpha = 1f,
)

private fun deepen(base: Color, factor: Float): Color = Color(
    red = (base.red * factor).coerceIn(0f, 1f),
    green = (base.green * factor).coerceIn(0f, 1f),
    blue = (base.blue * factor).coerceIn(0f, 1f),
    alpha = 1f,
)

@Composable
fun SpotifySovereignTheme(
    themePreset: AppThemePreset = AppThemePreset.MATCH_CURRENT_SONG,
    customColors: CustomThemeColors = CustomThemeColors(),
    trackPalette: TrackColorPalette = TrackColorPalette(),
    content: @Composable () -> Unit,
) {
    val (targetBg, targetFg, targetAccent) = when (themePreset) {
        AppThemePreset.MATCH_CURRENT_SONG -> Triple(
            trackPalette.subtleBgTint,
            Color(0xFFF6F7FA),
            trackPalette.highlightAccent,
        )
        AppThemePreset.AMOLED_BLACK -> Triple(
            Color(AppThemePreset.AMOLED_BLACK.backgroundHex),
            Color(AppThemePreset.AMOLED_BLACK.foregroundHex),
            Color(AppThemePreset.AMOLED_BLACK.accentHex),
        )
        AppThemePreset.TOKYO_NIGHT -> Triple(
            Color(AppThemePreset.TOKYO_NIGHT.backgroundHex),
            Color(AppThemePreset.TOKYO_NIGHT.foregroundHex),
            Color(AppThemePreset.TOKYO_NIGHT.accentHex),
        )
        AppThemePreset.CUSTOM -> Triple(
            Color(customColors.backgroundHex),
            Color(customColors.foregroundHex),
            Color(customColors.accentHex),
        )
    }

    val targetDeep = when (themePreset) {
        AppThemePreset.AMOLED_BLACK -> Color.Black
        AppThemePreset.CUSTOM -> deepen(targetBg, 0.72f)
        else -> deepen(targetBg, 0.55f)
    }
    val targetSurface = liftSurface(targetBg, if (targetBg == Color.Black) 0.055f else 0.045f)
    val targetCard = liftSurface(targetBg, if (targetBg == Color.Black) 0.095f else 0.085f)

    val animAccent by animateColorAsState(targetAccent, tween(480), label = "themeAccent")
    val animBg by animateColorAsState(targetBg, tween(480), label = "themeBg")
    val animDeep by animateColorAsState(targetDeep, tween(480), label = "themeDeep")
    val animSurface by animateColorAsState(targetSurface, tween(480), label = "themeSurface")
    val animCard by animateColorAsState(targetCard, tween(480), label = "themeCard")
    val animFg by animateColorAsState(targetFg, tween(480), label = "themeFg")

    val palette = SovereignPalette(
        accent = animAccent,
        accentGlow = animAccent.copy(alpha = 0.28f),
        background = animBg,
        backgroundDeep = animDeep,
        surface = animSurface,
        card = animCard,
        pill = liftSurface(animCard, 0.04f),
        glassSurface = animSurface.copy(alpha = 0.88f),
        glassBorder = animFg.copy(alpha = 0.13f),
        textPrimary = animFg,
        textMuted = animFg.copy(alpha = 0.64f),
        textSubtle = animFg.copy(alpha = 0.42f),
    )

    val typography = Typography(
        headlineMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 24.sp,
            letterSpacing = (-0.6).sp,
            color = animFg,
        ),
        titleLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 21.sp,
            letterSpacing = (-0.45).sp,
            color = animFg,
        ),
        titleMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = animFg,
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = animFg.copy(alpha = 0.64f),
        ),
    )

    val colorScheme = darkColorScheme(
        primary = animAccent,
        onPrimary = Color.Black,
        background = animBg,
        onBackground = animFg,
        surface = animSurface,
        onSurface = animFg,
        surfaceVariant = animCard,
        onSurfaceVariant = animFg.copy(alpha = 0.64f),
    )

    CompositionLocalProvider(LocalSovereignPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content,
        )
    }
}
