package com.spotify.sovereign.ui.screens

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spotify.sovereign.model.AppThemePreset
import com.spotify.sovereign.model.ArtworkStyle
import com.spotify.sovereign.model.AudioFxSettings
import com.spotify.sovereign.model.EQ_BAND_LABELS
import com.spotify.sovereign.model.SoundProfile
import com.spotify.sovereign.model.VisualSettings
import com.spotify.sovereign.ui.SovereignUiState
import com.spotify.sovereign.ui.components.EqualizerCurveCanvas
import com.spotify.sovereign.ui.theme.SovereignTheme
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    state: SovereignUiState,
    onBack: () -> Unit,
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Settings & Studio", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                Text("Sovereign v3.0 · Atmos DSP · Theme Studio", color = palette.accent, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // =====================================================================
            // SECTION 1: SOVEREIGN SOUND ENGINE & DOLBY ATMOS STUDIO
            // =====================================================================
            item(key = "sec_audio") {
                SettingsSectionCard(
                    icon = Icons.Filled.SpatialAudio,
                    title = "Sound Engine & Dolby Atmos",
                    subtitle = state.dspStatus.engineSummary,
                    accent = palette.accent,
                ) {
                    // Master DSP Toggle
                    SettingsSwitchRow(
                        title = "Sovereign Studio Mastering DSP",
                        subtitle = if (audioFx.masterEnabled) "Active on Session #${state.dspStatus.sessionId} · 32-bit Pipeline" else "Bypassed (Raw stream)",
                        checked = audioFx.masterEnabled,
                        accent = palette.accent,
                        onCheckedChange = { onUpdateAudioFx(audioFx.copy(masterEnabled = it)) },
                    )

                    Spacer(Modifier.height(12.dp))

                    // Live Frequency Curve Graph
                    EqualizerCurveCanvas(
                        bandsDb = bands,
                        enabled = audioFx.masterEnabled && audioFx.profile != SoundProfile.FLAT_REFERENCE,
                        accentColor = palette.accent,
                    )

                    Spacer(Modifier.height(14.dp))
                    Text("SOUND PROFILES", color = Color.White.copy(alpha = 0.55f), fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
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
                                Text(prof.shortBadge, color = if (selected) palette.accent else Color.White.copy(alpha = 0.55f), fontSize = 9.5.sp, fontWeight = FontWeight.Black, letterSpacing = 0.7.sp)
                                Spacer(Modifier.height(2.dp))
                                Text(prof.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(2.dp))
                                Text(prof.subtitle, color = palette.textMuted, fontSize = 10.sp, lineHeight = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    Spacer(Modifier.height(10.dp))

                    // Atmos Spatializer + Multiband Limiter
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
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    Spacer(Modifier.height(12.dp))

                    // Interactive 10-Band Graphic EQ
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = palette.accent, modifier = Modifier.size(16.dp))
                            Text("10-BAND GRAPHIC EQUALIZER", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
                        }
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.10f))
                                .clickable(onClick = onResetCustomEq)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(Icons.Filled.RestartAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                            Text("Flatten", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                color = Color.White.copy(alpha = 0.72f),
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
                                    thumbColor = Color.White,
                                    activeTrackColor = palette.accent,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.14f),
                                ),
                                modifier = Modifier.weight(1f).height(26.dp),
                            )
                            Text(
                                text = String.format(Locale.US, "%+.1f", db),
                                color = if (db > 0.1f) palette.accent else Color.White.copy(alpha = 0.65f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.width(40.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    Spacer(Modifier.height(10.dp))

                    // Stream Bitrate Selector
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
            // SECTION 2: APPEARANCE & THEME STUDIO
            // =====================================================================
            item(key = "sec_theme") {
                SettingsSectionCard(
                    icon = Icons.Filled.Palette,
                    title = "Appearance & Theme Studio",
                    subtitle = visual.themePreset.title,
                    accent = palette.accent,
                ) {
                    Text("COLOR THEME", color = Color.White.copy(alpha = 0.55f), fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(10.dp))

                    // 2-Column Theme Swatch Grid
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        AppThemePreset.entries.chunked(2).forEach { pair ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                pair.forEach { preset ->
                                    val selected = visual.themePreset == preset
                                    val swatchAccent = if (preset.isDynamicAlbum) state.trackPalette.highlightAccent else Color(preset.accentHex)
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(preset.cardHex))
                                            .border(
                                                width = if (selected) 1.6.dp else 0.9.dp,
                                                color = if (selected) swatchAccent else Color.White.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(14.dp),
                                            )
                                            .clickable { onUpdateVisual(visual.copy(themePreset = preset)) }
                                            .padding(11.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (preset.isDynamicAlbum) {
                                                        Brush.sweepGradient(listOf(Color(0xFF1ED760), Color(0xFF38BDF8), Color(0xFFFF3B6B), Color(0xFFA855F7), Color(0xFF1ED760)))
                                                    } else {
                                                        Brush.linearGradient(listOf(swatchAccent, swatchAccent.copy(alpha = 0.65f)))
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            if (selected) {
                                                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Spacer(Modifier.width(9.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(preset.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(preset.subtitle, color = Color.White.copy(alpha = 0.58f), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("PLAYER ARTWORK STAGE", color = Color.White.copy(alpha = 0.55f), fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(8.dp))

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
                                Text(style.title, color = if (selected) palette.accent else Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    Spacer(Modifier.height(8.dp))

                    SettingsSwitchRow(
                        title = "Squiggly Wave Progress Bar",
                        subtitle = "Living animated wave that flattens cleanly on pause",
                        checked = visual.squigglyProgressBar,
                        accent = palette.accent,
                        onCheckedChange = { onUpdateVisual(visual.copy(squigglyProgressBar = it)) },
                    )
                    SettingsSwitchRow(
                        title = "Ambient Aurora Mesh Backdrop",
                        subtitle = "Living multi-orb album color glow in Player & Lyrics",
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
                        subtitle = "Apple-Music optical blur on inactive lyrics lines",
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
                        Text("Spotify Sovereign v3.0 Studio", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "BoundedChunk 512KB HTTP/2 Streamer · 32-bit DynamicsProcessing 10-Band EQ + 5-Band MBC Limiter + Dolby 3D Spatializer · On-Device SQLite Taste Brain · Zero External Extractors",
                        color = palette.textMuted,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp,
                    )
                }
            }
        }
    }
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
                Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
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
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = Color.White.copy(alpha = if (enabled) 1f else 0.45f), fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color.White.copy(alpha = if (enabled) 0.60f else 0.30f), fontSize = 11.sp, lineHeight = 14.5.sp)
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = accent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.White.copy(alpha = 0.16f),
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
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White.copy(alpha = if (enabled) 0.88f else 0.40f), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            Text("${(value * 100).roundToInt()}%", color = if (enabled) accent else Color.White.copy(alpha = 0.32f), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        }
        Slider(
            value = value.coerceIn(0f, 1f),
            onValueChange = onChange,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = accent,
                inactiveTrackColor = Color.White.copy(alpha = 0.15f),
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
            Text(title, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color.White.copy(alpha = 0.58f), fontSize = 11.sp)
        }
    }
}
