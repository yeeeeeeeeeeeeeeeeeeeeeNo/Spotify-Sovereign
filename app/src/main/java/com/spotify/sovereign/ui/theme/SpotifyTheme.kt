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

/**
 * Legacy static fallback tokens (still used as defaults/constants).
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
    val DefaultAccent = Color(0xFF4F1D32)
    val DefaultMiniBg = Color(0xFF2B121D)
}

/**
 * Live Reactive Sovereign Palette driven by User Theme Preset + Dynamic Album Accent.
 */
data class SovereignPalette(
    val accent: Color = SpotifyColors.Green,
    val accentGlow: Color = SpotifyColors.GreenGlow,
    val background: Color = SpotifyColors.Background,
    val backgroundDeep: Color = SpotifyColors.OledBlack,
    val surface: Color = SpotifyColors.Surface,
    val card: Color = SpotifyColors.Card,
    val pill: Color = SpotifyColors.Pill,
    val glassSurface: Color = SpotifyColors.GlassSurface,
    val glassBorder: Color = SpotifyColors.GlassBorder,
    val textPrimary: Color = SpotifyColors.TextPrimary,
    val textMuted: Color = SpotifyColors.TextMuted,
    val textSubtle: Color = SpotifyColors.TextSubtle,
)

val LocalSovereignPalette = staticCompositionLocalOf { SovereignPalette() }

object SovereignTheme {
    val colors: SovereignPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalSovereignPalette.current
}

private val SovereignTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp,
        letterSpacing = (-0.6).sp,
        color = Color.White,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 21.sp,
        letterSpacing = (-0.45).sp,
        color = Color.White,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        color = Color.White,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        color = SpotifyColors.TextMuted,
    ),
)

@Composable
fun SpotifySovereignTheme(
    themePreset: AppThemePreset = AppThemePreset.SPOTIFY_OBSIDIAN,
    dynamicAlbumAccent: Color = SpotifyColors.Green,
    content: @Composable () -> Unit,
) {
    val targetAccent = if (themePreset.isDynamicAlbum) dynamicAlbumAccent else Color(themePreset.accentHex)
    val targetBg = Color(themePreset.backgroundHex)
    val targetDeep = if (themePreset.isPureOled) Color.Black else Color(
        red = (targetBg.red * 0.45f).coerceIn(0f, 1f),
        green = (targetBg.green * 0.45f).coerceIn(0f, 1f),
        blue = (targetBg.blue * 0.45f).coerceIn(0f, 1f),
        alpha = 1f,
    )
    val targetSurface = Color(themePreset.surfaceHex)
    val targetCard = Color(themePreset.cardHex)

    val animAccent by animateColorAsState(targetAccent, tween(450), label = "themeAccent")
    val animBg by animateColorAsState(targetBg, tween(450), label = "themeBg")
    val animDeep by animateColorAsState(targetDeep, tween(450), label = "themeDeep")
    val animSurface by animateColorAsState(targetSurface, tween(450), label = "themeSurface")
    val animCard by animateColorAsState(targetCard, tween(450), label = "themeCard")

    val palette = SovereignPalette(
        accent = animAccent,
        accentGlow = animAccent.copy(alpha = 0.25f),
        background = animBg,
        backgroundDeep = animDeep,
        surface = animSurface,
        card = animCard,
        pill = animCard.copy(alpha = 0.92f),
        glassSurface = animSurface.copy(alpha = 0.86f),
        glassBorder = Color.White.copy(alpha = if (themePreset.isPureOled) 0.14f else 0.12f),
    )

    val colorScheme = darkColorScheme(
        primary = animAccent,
        onPrimary = Color.Black,
        background = animBg,
        onBackground = Color.White,
        surface = animSurface,
        onSurface = Color.White,
        surfaceVariant = animCard,
        onSurfaceVariant = SpotifyColors.TextMuted,
    )

    CompositionLocalProvider(LocalSovereignPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SovereignTypography,
            content = content,
        )
    }
}
