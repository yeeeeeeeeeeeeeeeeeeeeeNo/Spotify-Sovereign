package com.spotify.sovereign.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SpatialAudio
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.spotify.sovereign.model.AppThemePreset
import com.spotify.sovereign.model.ArtworkStyle
import com.spotify.sovereign.model.AudioFxSettings
import com.spotify.sovereign.model.CustomThemeColors
import com.spotify.sovereign.model.EQ_BAND_LABELS
import com.spotify.sovereign.model.SoundProfile
import com.spotify.sovereign.model.VisualSettings
import com.spotify.sovereign.ui.SovereignUiState
import com.spotify.sovereign.ui.components.EqualizerCurveCanvas
import com.spotify.sovereign.ui.theme.SovereignTheme
import java.util.Locale
import kotlin.math.roundToInt

private enum class ColorSlot(val title: String) {
    BACKGROUND("Background"),
    FOREGROUND("Foreground"),
    ACCENT("Accent"),
}

private data class StarterCustomPalette(
    val name: String,
    val bg: Long,
    val fg: Long,
    val accent: Long,
)

private val STARTER_CUSTOM_PALETTES = listOf(
    StarterCustomPalette("Vesper", 0xFF000000, 0xFF967ADB, 0xFFFFC799),
    StarterCustomPalette("Tokyo Night", 0xFF1A1B26, 0xFFC0CAF5, 0xFF7AA2F7),
    StarterCustomPalette("Catppuccin", 0xFF11111B, 0xFFCDD6F4, 0xFFCBA6F7),
    StarterCustomPalette("Rose Pine", 0xFF191724, 0xFFE0DEF4, 0xFFEBBCBA),
    StarterCustomPalette("Crimson Noir", 0xFF0B0508, 0xFFF5D0DC, 0xFFFF3B6B),
    StarterCustomPalette("AMOLED Mint", 0xFF000000, 0xFFEAF7F0, 0xFF1ED760),
)

@Composable
fun SettingsScreen(
    state: SovereignUiState,
    onBack: () -> Unit,
    onOpenYouTubeLogin: () -> Unit,
    onSyncYouTubeAccount: () -> Unit,
    onSignOutYouTubeAccount: () -> Unit,
    onUpdateAudioFx: (AudioFxSettings) -> Unit,
    onSelectSoundProfile: (SoundProfile) -> Unit,
    onUpdateEqBand: (Int, Float) -> Unit,
    onResetCustomEq: () -> Unit,
    onUpdateVisual: (VisualSettings) -> Unit,
    onToggleUltraHighAudio: () -> Unit,
    onClearAudioCache: () -> Unit,
    onRefreshAiFeed: () -> Unit,
    onRetrainAi: () -> Unit,
    onOpenImport: () -> Unit,
) {
    val palette = SovereignTheme.colors
    val audioFx = state.settings.audioFx
    val visual = state.settings.visual
    val bands = audioFx.effectiveBandsDb()

    // Active color slot for the interactive Color Picker Modal
    var editingSlot by remember { mutableStateOf<ColorSlot?>(null) }

    // Effective displayed hex values in the Dark Theme card
    val displayedBgHex = when (visual.themePreset) {
        AppThemePreset.CUSTOM -> visual.customColors.backgroundHex
        AppThemePreset.MATCH_CURRENT_SONG -> state.trackPalette.subtleBgTint.toArgb().toLong() and 0xFFFFFFFFL
        else -> visual.themePreset.backgroundHex
    }
    val displayedFgHex = when (visual.themePreset) {
        AppThemePreset.CUSTOM -> visual.customColors.foregroundHex
        else -> visual.themePreset.foregroundHex
    }
    val displayedAccentHex = when (visual.themePreset) {
        AppThemePreset.CUSTOM -> visual.customColors.accentHex
        AppThemePreset.MATCH_CURRENT_SONG -> state.trackPalette.highlightAccent.toArgb().toLong() and 0xFFFFFFFFL
        else -> visual.themePreset.accentHex
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0.0f to palette.surface,
                    0.22f to palette.background,
                    1.0f to palette.backgroundDeep,
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(38.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.textPrimary)
            }
            Spacer(Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Settings & Studio", color = palette.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                Text("Sovereign v4.0 · Account · Theme · Atmos", color = palette.accent, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 52.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // =====================================================================
            // SECTION 0: YOUTUBE MUSIC ACCOUNT & 2-WAY SYNC
            // =====================================================================
            item(key = "sec_account") {
                val yt = state.ytAccount
                SettingsSectionCard(
                    icon = Icons.Filled.AccountCircle,
                    title = "YouTube Music Account",
                    subtitle = if (yt.signedIn) "2-Way Sync Active · Liked Songs & Playlists" else "Sign in to sync your real likes, playlists & avatar",
                    accent = palette.accent,
                ) {
                    if (yt.signedIn) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(palette.card)
                                .border(1.dp, palette.glassBorder, RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (yt.avatarUrl.isNotBlank()) {
                                AsyncImage(
                                    model = yt.avatarUrl,
                                    contentDescription = yt.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(46.dp).clip(CircleShape).border(1.5.dp, palette.accent, CircleShape),
                                )
                            } else {
                                Box(
                                    modifier = Modifier.size(46.dp).clip(CircleShape).background(palette.accent),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(yt.name.take(1).uppercase().ifBlank { "Y" }, color = Color.Black, fontWeight = FontWeight.Black, fontSize = 18.sp)
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(yt.name.ifBlank { "YouTube Music" }, color = palette.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (yt.handle.isNotBlank()) {
                                    Text(yt.handle, color = palette.textMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Text("Connected · 2-Way Like Sync ON", color = palette.accent, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(palette.accent)
                                    .clickable(enabled = !state.isSyncingAccount, onClick = onSyncYouTubeAccount)
                                    .padding(vertical = 11.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (state.isSyncingAccount) {
                                    CircularProgressIndicator(color = Color.Black, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                } else {
                                    Icon(Icons.Filled.Sync, contentDescription = null, tint = Color.Black, modifier = Modifier.size(17.dp))
                                }
                                Spacer(Modifier.width(6.dp))
                                Text(if (state.isSyncingAccount) "Syncing..." else "Sync Library", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Black)
                            }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(palette.card)
                                    .border(1.dp, palette.glassBorder, RoundedCornerShape(12.dp))
                                    .clickable(onClick = onSignOutYouTubeAccount)
                                    .padding(horizontal = 14.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = palette.textMuted, modifier = Modifier.size(16.dp))
                                Text("Sign Out", color = palette.textPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(palette.accent)
                                .clickable(onClick = onOpenYouTubeLogin)
                                .padding(vertical = 13.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = Color.Black, modifier = Modifier.size(19.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Sign in with YouTube Music", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            // =====================================================================
            // SECTION 1: DARK THEME STUDIO (EXACT REFERENCE-STYLE CUSTOMIZER!)
            // =====================================================================
            item(key = "sec_theme") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Dark Theme",
                        color = palette.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
                    )

                    // Sleek Grouped Rounded Dark Card matching the reference design!
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(palette.surface)
                            .border(1.dp, palette.glassBorder, RoundedCornerShape(18.dp)),
                    ) {
                        // Row 1: Preset selector + Reset arrow
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Preset", color = palette.textPrimary, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(
                                    onClick = {
                                        onUpdateVisual(
                                            visual.copy(
                                                themePreset = AppThemePreset.MATCH_CURRENT_SONG,
                                                customColors = CustomThemeColors(),
                                            )
                                        )
                                    },
                                    modifier = Modifier.size(28.dp),
                                ) {
                                    Icon(Icons.Filled.Undo, contentDescription = "Reset theme", tint = palette.textMuted, modifier = Modifier.size(16.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(palette.card)
                                        .border(1.dp, palette.glassBorder, CircleShape)
                                        .clickable {
                                            val all = AppThemePreset.entries
                                            val next = all[(all.indexOf(visual.themePreset) + 1) % all.size]
                                            onUpdateVisual(visual.copy(themePreset = next))
                                        }
                                        .padding(horizontal = 14.dp, vertical = 7.dp),
                                ) {
                                    Text(
                                        text = "${visual.themePreset.title}  ▾",
                                        color = palette.textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }

                        // 4 Direct Preset Pills (Match Current Song · AMOLED Black · Tokyo Night · Custom)
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 14.dp),
                        ) {
                            items(AppThemePreset.entries) { preset ->
                                val selected = visual.themePreset == preset
                                val dotColor = when (preset) {
                                    AppThemePreset.MATCH_CURRENT_SONG -> state.trackPalette.highlightAccent
                                    AppThemePreset.CUSTOM -> Color(visual.customColors.accentHex)
                                    else -> Color(preset.accentHex)
                                }
                                Row(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (selected) palette.accent.copy(alpha = 0.20f) else palette.card)
                                        .border(
                                            width = if (selected) 1.4.dp else 0.9.dp,
                                            color = if (selected) palette.accent else palette.glassBorder,
                                            shape = CircleShape,
                                        )
                                        .clickable { onUpdateVisual(visual.copy(themePreset = preset)) }
                                        .padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(dotColor)
                                            .border(0.8.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                                    )
                                    Text(
                                        text = preset.title,
                                        color = if (selected) palette.accent else palette.textPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = palette.glassBorder)

                        // Row 2: Background Color Pill (#000000)
                        ThemeHexColorRow(
                            label = "Background",
                            colorHex = displayedBgHex,
                            onClick = { editingSlot = ColorSlot.BACKGROUND },
                        )
                        HorizontalDivider(color = palette.glassBorder)

                        // Row 3: Foreground Color Pill (#967ADB)
                        ThemeHexColorRow(
                            label = "Foreground",
                            colorHex = displayedFgHex,
                            onClick = { editingSlot = ColorSlot.FOREGROUND },
                        )
                        HorizontalDivider(color = palette.glassBorder)

                        // Row 4: Accent Color Pill (#FFC799)
                        ThemeHexColorRow(
                            label = "Accent",
                            colorHex = displayedAccentHex,
                            onClick = { editingSlot = ColorSlot.ACCENT },
                        )

                        // Quick Custom Starter Swatches Strip (1-Tap Vesper / Catppuccin / Rose Pine / etc.)
                        HorizontalDivider(color = palette.glassBorder)
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                            Text("QUICK CUSTOM PALETTES", color = palette.textSubtle, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.9.sp)
                            Spacer(Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(STARTER_CUSTOM_PALETTES) { starter ->
                                    Row(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Color(starter.bg))
                                            .border(1.dp, Color(starter.accent).copy(alpha = 0.55f), CircleShape)
                                            .clickable {
                                                onUpdateVisual(
                                                    visual.copy(
                                                        themePreset = AppThemePreset.CUSTOM,
                                                        customColors = CustomThemeColors(
                                                            backgroundHex = starter.bg,
                                                            foregroundHex = starter.fg,
                                                            accentHex = starter.accent,
                                                        ),
                                                    )
                                                )
                                            }
                                            .padding(horizontal = 11.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    ) {
                                        Box(Modifier.size(9.dp).clip(CircleShape).background(Color(starter.fg)))
                                        Box(Modifier.size(9.dp).clip(CircleShape).background(Color(starter.accent)))
                                        Text(starter.name, color = Color(starter.fg), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Player Artwork Stage & Motion Controls Card
                    SettingsSectionCard(
                        icon = Icons.Filled.Palette,
                        title = "Player Visuals & Motion",
                        subtitle = visual.artworkStyle.title,
                        accent = palette.accent,
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ArtworkStyle.entries.forEach { style ->
                                val selected = visual.artworkStyle == style
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selected) palette.accent.copy(alpha = 0.18f) else palette.card)
                                        .border(
                                            width = if (selected) 1.4.dp else 0.9.dp,
                                            color = if (selected) palette.accent else palette.glassBorder,
                                            shape = RoundedCornerShape(12.dp),
                                        )
                                        .clickable { onUpdateVisual(visual.copy(artworkStyle = style)) }
                                        .padding(horizontal = 8.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(style.title, color = if (selected) palette.accent else palette.textPrimary, fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        SettingsSwitchRow(
                            title = "Squiggly Wave Progress Bar",
                            subtitle = "Living animated wave tinted live to theme/song color",
                            checked = visual.squigglyProgressBar,
                            accent = palette.accent,
                            onCheckedChange = { onUpdateVisual(visual.copy(squigglyProgressBar = it)) },
                        )
                        SettingsSwitchRow(
                            title = "Ambient Aurora Mesh Backdrop",
                            subtitle = "Living multi-orb album glow in Player & Lyrics",
                            checked = visual.ambientAuroraMesh,
                            accent = palette.accent,
                            onCheckedChange = { onUpdateVisual(visual.copy(ambientAuroraMesh = it)) },
                        )
                        SettingsSwitchRow(
                            title = "Artwork Breathing & Halo Glow",
                            subtitle = "Dynamic pulse shadow & scale physics when playing",
                            checked = visual.artworkBreathingGlow,
                            accent = palette.accent,
                            onCheckedChange = { onUpdateVisual(visual.copy(artworkBreathingGlow = it)) },
                        )
                        SettingsSwitchRow(
                            title = "Lyrics Depth-of-Field Blur",
                            subtitle = "Optical focus blur on inactive lyrics lines",
                            checked = visual.lyricsDepthBlur,
                            accent = palette.accent,
                            onCheckedChange = { onUpdateVisual(visual.copy(lyricsDepthBlur = it)) },
                        )
                        SettingsSwitchRow(
                            title = "Tactile Haptic Feedback",
                            subtitle = "Crisp vibration ticks on scrub, skip, and likes",
                            checked = visual.hapticFeedback,
                            accent = palette.accent,
                            onCheckedChange = { onUpdateVisual(visual.copy(hapticFeedback = it)) },
                        )
                    }
                }
            }

            // =====================================================================
            // SECTION 2: SOVEREIGN SOUND ENGINE & DOLBY ATMOS STUDIO
            // =====================================================================
            item(key = "sec_audio") {
                SettingsSectionCard(
                    icon = Icons.Filled.SpatialAudio,
                    title = "Sound Engine & Dolby Atmos",
                    subtitle = state.dspStatus.engineSummary,
                    accent = palette.accent,
                ) {
                    SettingsSwitchRow(
                        title = "Sovereign Studio Mastering DSP",
                        subtitle = if (audioFx.masterEnabled) "Active on Session #${state.dspStatus.sessionId} · 32-bit Pipeline" else "Bypassed (Raw stream)",
                        checked = audioFx.masterEnabled,
                        accent = palette.accent,
                        onCheckedChange = { onUpdateAudioFx(audioFx.copy(masterEnabled = it)) },
                    )

                    Spacer(Modifier.height(12.dp))
                    EqualizerCurveCanvas(
                        bandsDb = bands,
                        enabled = audioFx.masterEnabled && audioFx.profile != SoundProfile.FLAT_REFERENCE,
                        accentColor = palette.accent,
                    )

                    Spacer(Modifier.height(14.dp))
                    Text("SOUND PROFILES", color = palette.textSubtle, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(SoundProfile.entries) { prof ->
                            val selected = audioFx.profile == prof && audioFx.masterEnabled
                            Column(
                                modifier = Modifier
                                    .width(136.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (selected) palette.accent.copy(alpha = 0.18f) else palette.card)
                                    .border(
                                        width = if (selected) 1.4.dp else 0.9.dp,
                                        color = if (selected) palette.accent else palette.glassBorder,
                                        shape = RoundedCornerShape(14.dp),
                                    )
                                    .clickable { onSelectSoundProfile(prof) }
                                    .padding(11.dp),
                            ) {
                                Text(prof.shortBadge, color = if (selected) palette.accent else palette.textSubtle, fontSize = 9.5.sp, fontWeight = FontWeight.Black, letterSpacing = 0.7.sp)
                                Spacer(Modifier.height(2.dp))
                                Text(prof.title, color = palette.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(2.dp))
                                Text(prof.subtitle, color = palette.textMuted, fontSize = 10.sp, lineHeight = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = palette.glassBorder)
                    Spacer(Modifier.height(10.dp))

                    SettingsSwitchRow(
                        title = "Dolby Atmos 3D Spatializer",
                        subtitle = "Expands stereo soundstage & 3D instrument depth",
                        checked = audioFx.atmosSpatialEnabled,
                        enabled = audioFx.masterEnabled,
                        accent = palette.accent,
                        onCheckedChange = { onUpdateAudioFx(audioFx.copy(atmosSpatialEnabled = it)) },
                    )
                    SettingsSliderRow(
                        label = "Spatial Soundstage Width",
                        value = audioFx.spatialWidth,
                        enabled = audioFx.masterEnabled && audioFx.atmosSpatialEnabled,
                        accent = palette.accent,
                        onChange = { onUpdateAudioFx(audioFx.copy(spatialWidth = it)) },
                    )
                    SettingsSliderRow(
                        label = "Sub-Bass Harmonic Punch",
                        value = audioFx.bassBoost,
                        enabled = audioFx.masterEnabled,
                        accent = palette.accent,
                        onChange = { onUpdateAudioFx(audioFx.copy(bassBoost = it)) },
                    )
                    SettingsSliderRow(
                        label = "Studio Clarity & Air Exciter",
                        value = audioFx.clarityExciter,
                        enabled = audioFx.masterEnabled,
                        accent = palette.accent,
                        onChange = { onUpdateAudioFx(audioFx.copy(clarityExciter = it)) },
                    )
                    Spacer(Modifier.height(4.dp))
                    SettingsSwitchRow(
                        title = "5-Band Multiband Mastering & Limiter",
                        subtitle = "Lifts studio detail & blocks clipping at high bass/volume",
                        checked = audioFx.multibandLimiter,
                        enabled = audioFx.masterEnabled,
                        accent = palette.accent,
                        onCheckedChange = { onUpdateAudioFx(audioFx.copy(multibandLimiter = it)) },
                    )

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = palette.glassBorder)
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = palette.accent, modifier = Modifier.size(16.dp))
                            Text("10-BAND GRAPHIC EQUALIZER", color = palette.textPrimary, fontSize = 11.5.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
                        }
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(palette.card)
                                .clickable(onClick = onResetCustomEq)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(Icons.Filled.RestartAlt, contentDescription = null, tint = palette.textPrimary, modifier = Modifier.size(13.dp))
                            Text("Flatten", color = palette.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    bands.forEachIndexed { idx, db ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = EQ_BAND_LABELS[idx],
                                color = palette.textMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(34.dp),
                            )
                            Slider(
                                value = db.coerceIn(-10f, 10f),
                                onValueChange = { onUpdateEqBand(idx, it) },
                                valueRange = -10f..10f,
                                enabled = audioFx.masterEnabled,
                                colors = SliderDefaults.colors(
                                    thumbColor = palette.textPrimary,
                                    activeTrackColor = palette.accent,
                                    inactiveTrackColor = palette.textPrimary.copy(alpha = 0.14f),
                                ),
                                modifier = Modifier.weight(1f).height(26.dp),
                            )
                            Text(
                                text = String.format(Locale.US, "%+.1f", db),
                                color = if (db > 0.1f) palette.accent else palette.textMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.width(40.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = palette.glassBorder)
                    Spacer(Modifier.height(10.dp))

                    SettingsSwitchRow(
                        title = "Ultra-High Stream Bitrate",
                        subtitle = if (state.ultraHighAudio) "Highest OPUS / AAC studio stream (${state.streamBadge})" else "Balanced 128k Data Saver",
                        checked = state.ultraHighAudio,
                        accent = palette.accent,
                        onCheckedChange = { onToggleUltraHighAudio() },
                    )
                }
            }

            // =====================================================================
            // SECTION 3: AI TASTE BRAIN & STORAGE CACHE
            // =====================================================================
            item(key = "sec_ai") {
                val cacheMb = String.format(Locale.US, "%.1f MB", state.audioCacheBytes / (1024.0 * 1024.0))
                SettingsSectionCard(
                    icon = Icons.Filled.AutoAwesome,
                    title = "AI Taste Brain & Storage",
                    subtitle = "${state.tasteDna.totalPlays} plays learned · $cacheMb audio cached",
                    accent = palette.accent,
                ) {
                    SettingsActionRow(
                        icon = Icons.Filled.Refresh,
                        title = "Refresh Personalized AI Mixes",
                        subtitle = "Rebuild Home shelves for ${state.tasteDna.currentTimeBucket} mood",
                        accent = palette.accent,
                        onClick = onRefreshAiFeed,
                    )
                    SettingsActionRow(
                        icon = Icons.Filled.Link,
                        title = "Import Public Spotify Playlist",
                        subtitle = "Pull studio tracks & train your local AI instantly",
                        accent = palette.accent,
                        onClick = onOpenImport,
                    )
                    SettingsActionRow(
                        icon = Icons.Filled.AutoAwesome,
                        title = "Retrain AI Taste Profile",
                        subtitle = "Re-pick your favorite artists & tracks",
                        accent = palette.accent,
                        onClick = onRetrainAi,
                    )
                    SettingsActionRow(
                        icon = Icons.Filled.CleaningServices,
                        title = "Clear Audio Disk Cache ($cacheMb)",
                        subtitle = "Free pre-buffered chunks from 512MB SimpleCache",
                        accent = palette.accent,
                        onClick = onClearAudioCache,
                    )
                }
            }

            // =====================================================================
            // SECTION 4: ENGINE DIAGNOSTICS FOOTER
            // =====================================================================
            item(key = "sec_about") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(palette.surface)
                        .border(1.dp, palette.glassBorder, RoundedCornerShape(18.dp))
                        .padding(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.HighQuality, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
                        Text("Spotify Sovereign v4.0 Studio", color = palette.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "BoundedChunk 512KB HTTP/2 Streamer · 32-bit DynamicsProcessing 10-Band EQ + Dolby 3D Spatial · YouTube Music Account 2-Way Sync · Manual Theme Studio",
                        color = palette.textMuted,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp,
                    )
                }
            }
        }
    }

    // Interactive Manual Color Picker Modal (Hue / Sat / Brightness + Quick Swatches + Direct #HEX Input!)
    editingSlot?.let { slot ->
        val initialHex = when (slot) {
            ColorSlot.BACKGROUND -> displayedBgHex
            ColorSlot.FOREGROUND -> displayedFgHex
            ColorSlot.ACCENT -> displayedAccentHex
        }
        SovereignColorPickerDialog(
            title = "Pick ${slot.title} Color",
            initialColorHex = initialHex,
            onDismiss = { editingSlot = null },
            onConfirm = { newHex ->
                val baseCustom = if (visual.themePreset == AppThemePreset.CUSTOM) {
                    visual.customColors
                } else {
                    CustomThemeColors(
                        backgroundHex = displayedBgHex,
                        foregroundHex = displayedFgHex,
                        accentHex = displayedAccentHex,
                    )
                }
                val updatedCustom = when (slot) {
                    ColorSlot.BACKGROUND -> baseCustom.copy(backgroundHex = newHex)
                    ColorSlot.FOREGROUND -> baseCustom.copy(foregroundHex = newHex)
                    ColorSlot.ACCENT -> baseCustom.copy(accentHex = newHex)
                }
                onUpdateVisual(
                    visual.copy(
                        themePreset = AppThemePreset.CUSTOM,
                        customColors = updatedCustom,
                    )
                )
                editingSlot = null
            },
        )
    }
}

@Composable
private fun ThemeHexColorRow(
    label: String,
    colorHex: Long,
    onClick: () -> Unit,
) {
    val palette = SovereignTheme.colors
    val hexStr = String.format(Locale.US, "%06X", colorHex and 0xFFFFFFL)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = palette.textPrimary, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(palette.card)
                .border(1.dp, palette.glassBorder, CircleShape)
                .padding(start = 10.dp, end = 14.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(colorHex))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
            )
            Text(
                text = "# $hexStr",
                color = palette.textPrimary,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SovereignColorPickerDialog(
    title: String,
    initialColorHex: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val palette = SovereignTheme.colors
    val initHsv = remember(initialColorHex) {
        val arr = FloatArray(3)
        android.graphics.Color.colorToHSV((initialColorHex or 0xFF000000L).toInt(), arr)
        arr
    }
    var hue by remember(initialColorHex) { mutableFloatStateOf(initHsv[0]) }
    var sat by remember(initialColorHex) { mutableFloatStateOf(initHsv[1]) }
    var value by remember(initialColorHex) { mutableFloatStateOf(initHsv[2]) }
    var hexInput by remember(initialColorHex) {
        mutableStateOf(String.format(Locale.US, "%06X", initialColorHex and 0xFFFFFFL))
    }

    val currentColorInt = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))
    val currentHexLong = (currentColorInt.toLong() and 0xFFFFFFL) or 0xFF000000L

    fun syncFromHsv() {
        val c = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))
        hexInput = String.format(Locale.US, "%06X", c and 0xFFFFFF)
    }

    val quickSwatches = listOf(
        0xFF000000L, 0xFF0A0A0EL, 0xFF1A1B26L, 0xFF11111BL,
        0xFFFFFFFFL, 0xFFC0CAF5L, 0xFF967ADBL, 0xFFE0DEF4L,
        0xFFFFC799L, 0xFF7AA2F7L, 0xFF1ED760L, 0xFFFF3B6BL,
        0xFFA855F7L, 0xFF38BDF8L, 0xFFF59E0BL, 0xFFEBBCBAL,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        title = {
            Text(title, color = palette.textPrimary, fontWeight = FontWeight.Black, fontSize = 17.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Live Preview Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(currentHexLong))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "#${String.format(Locale.US, "%06X", currentHexLong and 0xFFFFFFL)}",
                        color = if (value > 0.62f && sat < 0.55f) Color.Black else Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                    )
                }

                // Direct Hex Code Input
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { raw ->
                        val clean = raw.uppercase().filter { it in "0123456789ABCDEF" }.take(6)
                        hexInput = clean
                        if (clean.length == 6) {
                            runCatching {
                                val parsed = clean.toLong(16) or 0xFF000000L
                                val arr = FloatArray(3)
                                android.graphics.Color.colorToHSV(parsed.toInt(), arr)
                                hue = arr[0]; sat = arr[1]; value = arr[2]
                            }
                        }
                    },
                    singleLine = true,
                    prefix = { Text("#", color = palette.accent, fontWeight = FontWeight.Black) },
                    label = { Text("Hex Code", color = palette.textMuted, fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.accent,
                        unfocusedBorderColor = palette.glassBorder,
                        focusedTextColor = palette.textPrimary,
                        unfocusedTextColor = palette.textPrimary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                // Rainbow Hue Bar + Slider
                Column {
                    Text("Hue", color = palette.textMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Canvas(modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape)) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                            )
                        )
                    }
                    Slider(
                        value = hue,
                        onValueChange = { hue = it; syncFromHsv() },
                        valueRange = 0f..360f,
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color(currentHexLong)),
                        modifier = Modifier.fillMaxWidth().height(26.dp),
                    )
                }

                // Saturation
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Saturation", color = palette.textMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("${(sat * 100).roundToInt()}%", color = palette.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = sat,
                        onValueChange = { sat = it; syncFromHsv() },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = palette.accent),
                        modifier = Modifier.fillMaxWidth().height(26.dp),
                    )
                }

                // Brightness
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Brightness", color = palette.textMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("${(value * 100).roundToInt()}%", color = palette.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = value,
                        onValueChange = { value = it; syncFromHsv() },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = palette.accent),
                        modifier = Modifier.fillMaxWidth().height(26.dp),
                    )
                }

                // Curated 16 Quick Swatches
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    quickSwatches.chunked(8).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            row.forEach { sw ->
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(Color(sw))
                                        .border(
                                            width = if (sw == currentHexLong) 2.dp else 1.dp,
                                            color = if (sw == currentHexLong) Color.White else Color.White.copy(alpha = 0.25f),
                                            shape = CircleShape,
                                        )
                                        .clickable {
                                            val arr = FloatArray(3)
                                            android.graphics.Color.colorToHSV(sw.toInt(), arr)
                                            hue = arr[0]; sat = arr[1]; value = arr[2]
                                            hexInput = String.format(Locale.US, "%06X", sw and 0xFFFFFFL)
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (sw == currentHexLong) {
                                        Icon(Icons.Filled.Check, contentDescription = null, tint = if (sw == 0xFFFFFFFFL) Color.Black else Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(currentHexLong) },
                colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = Color.Black),
            ) { Text("Apply Color", fontWeight = FontWeight.Black) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = palette.textMuted) }
        },
    )
}

@Composable
private fun SettingsSectionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    content: @Composable () -> Unit,
) {
    val palette = SovereignTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(palette.surface)
            .border(1.dp, palette.glassBorder, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.16f))
                    .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = palette.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Text(subtitle, color = palette.textMuted, fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(14.dp))
        content()
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    accent: Color,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val palette = SovereignTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = palette.textPrimary.copy(alpha = if (enabled) 1f else 0.45f), fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = palette.textMuted.copy(alpha = if (enabled) 0.90f else 0.40f), fontSize = 11.sp, lineHeight = 14.5.sp)
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = accent,
                uncheckedThumbColor = palette.textPrimary,
                uncheckedTrackColor = palette.textPrimary.copy(alpha = 0.16f),
            ),
        )
    }
}

@Composable
private fun SettingsSliderRow(
    label: String,
    value: Float,
    enabled: Boolean,
    accent: Color,
    onChange: (Float) -> Unit,
) {
    val palette = SovereignTheme.colors
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = palette.textPrimary.copy(alpha = if (enabled) 0.90f else 0.40f), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            Text("${(value * 100).roundToInt()}%", color = if (enabled) accent else palette.textSubtle, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        }
        Slider(
            value = value.coerceIn(0f, 1f),
            onValueChange = onChange,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = palette.textPrimary,
                activeTrackColor = accent,
                inactiveTrackColor = palette.textPrimary.copy(alpha = 0.15f),
            ),
            modifier = Modifier.fillMaxWidth().height(26.dp),
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
) {
    val palette = SovereignTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = palette.textPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = palette.textMuted, fontSize = 11.sp)
        }
    }
}
