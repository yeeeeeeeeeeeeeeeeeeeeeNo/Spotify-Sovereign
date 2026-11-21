package com.spotify.sovereign.model

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

/**
 * 10-Band Studio Mastering Frequencies (Hz):
 * 31, 62, 125, 250, 500, 1k, 2k, 4k, 8k, 16k
 */
val EQ_BAND_FREQUENCIES_HZ = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
val EQ_BAND_LABELS = listOf("31", "62", "125", "250", "500", "1K", "2K", "4K", "8K", "16K")

@Serializable
enum class SoundProfile(
    val title: String,
    val shortBadge: String,
    val subtitle: String,
    val bandsDb: List<Float>,
    val spatialWidth: Float,
    val bassBoost: Float,
    val clarityExciter: Float,
    val inputGainDb: Float,
    val multibandComp: Boolean,
    val atmosSpatial: Boolean,
) {
    DOLBY_ATMOS(
        title = "Dolby Atmos Spatial",
        shortBadge = "ATMOS",
        subtitle = "3D soundstage · sculpted sub · crystal studio air",
        bandsDb = listOf(3.8f, 3.2f, 1.4f, -1.5f, -0.4f, 0.8f, 2.2f, 3.0f, 3.6f, 4.4f),
        spatialWidth = 0.72f,
        bassBoost = 0.34f,
        clarityExciter = 0.55f,
        inputGainDb = 2.8f,
        multibandComp = true,
        atmosSpatial = true,
    ),
    STUDIO_MASTER(
        title = "Studio Master HD",
        shortBadge = "STUDIO HD",
        subtitle = "YT Premium / Lossless punch · warm lows · zero mud",
        bandsDb = listOf(2.8f, 2.4f, 0.9f, -1.6f, -0.6f, 0.9f, 1.8f, 2.6f, 3.2f, 3.6f),
        spatialWidth = 0.36f,
        bassBoost = 0.24f,
        clarityExciter = 0.45f,
        inputGainDb = 2.5f,
        multibandComp = true,
        atmosSpatial = false,
    ),
    DEEP_BASS(
        title = "808 & Club Sub",
        shortBadge = "SUB BASS",
        subtitle = "Tactile sub-harmonic punch with crisp hi-hats",
        bandsDb = listOf(6.2f, 5.4f, 2.6f, -1.2f, -0.8f, 0.4f, 1.4f, 2.2f, 2.8f, 3.0f),
        spatialWidth = 0.42f,
        bassBoost = 0.68f,
        clarityExciter = 0.50f,
        inputGainDb = 2.6f,
        multibandComp = true,
        atmosSpatial = true,
    ),
    VOCAL_CRYSTAL(
        title = "Vocal Crystal",
        shortBadge = "VOCAL+",
        subtitle = "Forward velvet vocals & intimate acoustic detail",
        bandsDb = listOf(-0.6f, 0.2f, 0.5f, -1.4f, 1.2f, 2.8f, 3.6f, 3.2f, 2.4f, 2.8f),
        spatialWidth = 0.45f,
        bassBoost = 0.10f,
        clarityExciter = 0.48f,
        inputGainDb = 2.2f,
        multibandComp = true,
        atmosSpatial = false,
    ),
    CONCERT_3D(
        title = "Concert Hall 3D",
        shortBadge = "3D HALL",
        subtitle = "Ultra-wide holographic arena ambience",
        bandsDb = listOf(3.2f, 2.5f, 1.0f, -1.0f, 0.2f, 1.2f, 2.6f, 3.4f, 4.0f, 4.6f),
        spatialWidth = 0.92f,
        bassBoost = 0.38f,
        clarityExciter = 0.62f,
        inputGainDb = 3.0f,
        multibandComp = true,
        atmosSpatial = true,
    ),
    LATE_NIGHT(
        title = "Midnight Warmth",
        shortBadge = "WARM",
        subtitle = "Silky low-volume mastering · softened sibilance",
        bandsDb = listOf(3.0f, 2.6f, 1.8f, 0.6f, 0.4f, 0.6f, 0.4f, -0.8f, -1.4f, -1.8f),
        spatialWidth = 0.30f,
        bassBoost = 0.28f,
        clarityExciter = 0.25f,
        inputGainDb = 1.8f,
        multibandComp = true,
        atmosSpatial = false,
    ),
    FLAT_REFERENCE(
        title = "Pure Reference",
        shortBadge = "FLAT",
        subtitle = "Uncolored transparent studio bypass",
        bandsDb = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
        spatialWidth = 0f,
        bassBoost = 0f,
        clarityExciter = 0f,
        inputGainDb = 0f,
        multibandComp = false,
        atmosSpatial = false,
    ),
    CUSTOM(
        title = "Custom Studio EQ",
        shortBadge = "CUSTOM EQ",
        subtitle = "Your personal 10-band mastering curve",
        bandsDb = listOf(2.5f, 2.0f, 0.8f, -1.0f, 0f, 0.8f, 1.6f, 2.4f, 3.0f, 3.5f),
        spatialWidth = 0.55f,
        bassBoost = 0.30f,
        clarityExciter = 0.45f,
        inputGainDb = 2.4f,
        multibandComp = true,
        atmosSpatial = true,
    ),
}

/**
 * Curated 4-Preset Theme Architecture:
 * 1. Match Current Song (Dynamic album art extraction — sleek monochrome fallback on black covers)
 * 2. AMOLED Black (True #000000 pure OLED)
 * 3. Tokyo Night (Authentic #1A1B26 / #C0CAF5 / #7AA2F7)
 * 4. Custom (Manual Background / Foreground / Accent hex & color picker studio!)
 */
@Serializable
enum class AppThemePreset(
    val title: String,
    val subtitle: String,
    val backgroundHex: Long,
    val foregroundHex: Long,
    val accentHex: Long,
) {
    MATCH_CURRENT_SONG(
        title = "Match Current Song",
        subtitle = "Live colors extracted from every track",
        backgroundHex = 0xFF07070A,
        foregroundHex = 0xFFF4F5F8,
        accentHex = 0xFFE6E8F0,
    ),
    AMOLED_BLACK(
        title = "AMOLED Black",
        subtitle = "True #000000 pitch-black OLED",
        backgroundHex = 0xFF000000,
        foregroundHex = 0xFFF4F4F6,
        accentHex = 0xFF1ED760,
    ),
    TOKYO_NIGHT(
        title = "Tokyo Night",
        subtitle = "Deep indigo · periwinkle · electric blue",
        backgroundHex = 0xFF1A1B26,
        foregroundHex = 0xFFC0CAF5,
        accentHex = 0xFF7AA2F7,
    ),
    CUSTOM(
        title = "Custom",
        subtitle = "Pick your own Background, Foreground & Accent",
        backgroundHex = 0xFF000000,
        foregroundHex = 0xFF967ADB,
        accentHex = 0xFFFFC799,
    ),
}

@Serializable
data class CustomThemeColors(
    val backgroundHex: Long = 0xFF000000,
    val foregroundHex: Long = 0xFF967ADB,
    val accentHex: Long = 0xFFFFC799,
)

@Serializable
enum class ArtworkStyle(val title: String, val subtitle: String) {
    HERO_SQUIRCLE("Hero Squircle", "Floating rounded art with breathing glow"),
    VINYL_DISC("Spinning Vinyl", "Glossy animated turntable disc"),
    FULL_IMMERSIVE("Cinematic Full", "Edge-to-edge tall artwork stage"),
}

@Serializable
data class AudioFxSettings(
    val masterEnabled: Boolean = true,
    val profile: SoundProfile = SoundProfile.DOLBY_ATMOS,
    val atmosSpatialEnabled: Boolean = true,
    val spatialWidth: Float = SoundProfile.DOLBY_ATMOS.spatialWidth,
    val bassBoost: Float = SoundProfile.DOLBY_ATMOS.bassBoost,
    val clarityExciter: Float = SoundProfile.DOLBY_ATMOS.clarityExciter,
    val multibandLimiter: Boolean = true,
    val inputGainDb: Float = SoundProfile.DOLBY_ATMOS.inputGainDb,
    val customBandsDb: List<Float> = SoundProfile.DOLBY_ATMOS.bandsDb,
) {
    fun effectiveBandsDb(): List<Float> =
        if (profile == SoundProfile.CUSTOM) customBandsDb else profile.bandsDb

    fun activeBadgeLabel(): String = when {
        !masterEnabled || profile == SoundProfile.FLAT_REFERENCE -> "PURE"
        atmosSpatialEnabled && profile == SoundProfile.DOLBY_ATMOS -> "DOLBY ATMOS"
        atmosSpatialEnabled -> "${profile.shortBadge} · 3D"
        else -> profile.shortBadge
    }
}

@Serializable
data class VisualSettings(
    val themePreset: AppThemePreset = AppThemePreset.MATCH_CURRENT_SONG,
    val customColors: CustomThemeColors = CustomThemeColors(),
    val artworkStyle: ArtworkStyle = ArtworkStyle.HERO_SQUIRCLE,
    val squigglyProgressBar: Boolean = true,
    val ambientAuroraMesh: Boolean = true,
    val artworkBreathingGlow: Boolean = true,
    val lyricsDepthBlur: Boolean = true,
    val hapticFeedback: Boolean = true,
)

@Serializable
data class AppSettings(
    val audioFx: AudioFxSettings = AudioFxSettings(),
    val visual: VisualSettings = VisualSettings(),
)

@Serializable
data class YouTubeAccountProfile(
    val name: String = "",
    val handle: String = "",
    val avatarUrl: String = "",
    val signedIn: Boolean = false,
)

enum class MusicMoodCategory(val label: String, val query: String) {
    FOR_YOU("For You", ""),
    LATE_NIGHT("Late Night", "late night dark r&b moody chill songs"),
    HYPE("Hype & Trap", "hype rap trap bass banger songs"),
    CHILL("Chill & Soul", "chill alternative r&b smooth songs"),
    ELECTRONIC("Electronic", "electronic synthwave house dance songs"),
    INDIE("Indie & Alt", "indie alternative dream pop songs"),
    THROWBACK("Throwbacks", "iconic 2000s 2010s hip hop pop classics"),
}

data class SongCategoryBlock(
    val id: String,
    val title: String,
    val subtitle: String,
    val songs: List<TrackItem>,
)

data class TrackColorPalette(
    val vibrant: Color = Color(0xFF424656),
    val darkVibrant: Color = Color(0xFF1A1C24),
    val muted: Color = Color(0xFF232530),
    val highlightAccent: Color = Color(0xFFE6E8F0),
    val subtleBgTint: Color = Color(0xFF0A0B0F),
    val isMonochrome: Boolean = true,
)

data class DspEngineStatus(
    val sessionId: Int = 0,
    val dynamicsProcessingActive: Boolean = false,
    val virtualizerActive: Boolean = false,
    val bassBoostActive: Boolean = false,
    val loudnessActive: Boolean = false,
    val engineSummary: String = "Standby",
)
