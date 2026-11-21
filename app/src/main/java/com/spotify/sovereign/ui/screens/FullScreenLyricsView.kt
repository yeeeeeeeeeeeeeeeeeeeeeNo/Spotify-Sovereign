package com.spotify.sovereign.ui.screens

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SpatialAudio
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.spotify.sovereign.ui.SovereignUiState
import com.spotify.sovereign.ui.components.AmbientAuroraBackground
import com.spotify.sovereign.ui.components.SquigglyProgressBar
import com.spotify.sovereign.ui.theme.InstrumentalDotsPulse
import com.spotify.sovereign.ui.theme.SovereignTheme
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs

/**
 * Cinema-Grade Apple-Music-meets-Spotify Full-Screen Synced Lyrics View.
 * - Living Ambient Aurora Mesh Background
 * - Real-time Optical Depth-of-Field Blur on distant lines (auto-lifts while user scrolls!)
 * - Progressive Luminous Karaoke Sweep across the active line
 * - Soft top & bottom gradient edge fades
 * - Animated interlude breather dots during intros & instrumental breaks
 * - Squiggly Wave Progress Bar + Live Sync Offset Tuner + Sound Engine access
 */
@Composable
fun FullScreenLyricsView(
    state: SovereignUiState,
    onClose: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onAdjustOffset: (Long) -> Unit,
    onResetOffset: () -> Unit,
    onOpenSoundFx: () -> Unit,
) {
    val track = state.currentTrack ?: return
    val lines = state.lyricsPayload.lines
    val isSynced = state.lyricsPayload.isSynced
    val listState = rememberLazyListState()
    val palette = SovereignTheme.colors
    val visual = state.settings.visual
    val haptics = LocalHapticFeedback.current

    var showSyncTuner by remember { mutableStateOf(false) }
    var userIsScrolling by remember { mutableStateOf(false) }

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            userIsScrolling = true
        } else if (userIsScrolling) {
            delay(2800L)
            userIsScrolling = false
        }
    }

    val adjustedPositionMs = (state.positionMs - state.lyricsOffsetMs).coerceAtLeast(0L)
    val activeIdx = if (isSynced && lines.isNotEmpty()) {
        lines.indexOfLast { it.timeMs <= adjustedPositionMs }
    } else {
        -1
    }

    // Smooth auto-scroll keeping the active lyric centered in the upper-middle sweet spot
    LaunchedEffect(activeIdx, userIsScrolling) {
        if (!userIsScrolling && activeIdx >= 0 && activeIdx < lines.size) {
            listState.animateScrollToItem(index = activeIdx, scrollOffset = -290)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF050508))) {
        // Living Aurora Mesh Background (100% opaque base)
        AmbientAuroraBackground(
            palette = state.trackPalette,
            isPlaying = state.isPlaying,
            enabled = visual.ambientAuroraMesh,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            // Top Header with Track Art + Live Sound Engine + Sync Offset Tuner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Close lyrics", tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(8.dp))
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(8.dp)),
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(track.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = "${track.artist} · ${state.lyricsPayload.source}",
                        color = Color.White.copy(alpha = 0.70f),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Quick Sound FX Icon
                IconButton(onClick = onOpenSoundFx, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Filled.SpatialAudio,
                        contentDescription = "Sound FX",
                        tint = if (state.settings.audioFx.masterEnabled) palette.accent else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(4.dp))

                // Sync Offset Chip
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (showSyncTuner || state.lyricsOffsetMs != 0L) palette.accent.copy(alpha = 0.24f) else Color.Black.copy(alpha = 0.28f))
                        .border(1.dp, if (state.lyricsOffsetMs != 0L) palette.accent else Color.White.copy(alpha = 0.18f), CircleShape)
                        .clickable { showSyncTuner = !showSyncTuner }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.Tune, contentDescription = "Sync", tint = Color.White, modifier = Modifier.size(14.dp))
                        val secStr = String.format(Locale.US, "%+.2fs", state.lyricsOffsetMs / 1000.0)
                        Text(
                            text = if (state.lyricsOffsetMs == 0L) "Sync" else secStr,
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            // Animated Expandable Live Sync Offset Tuner
            AnimatedVisibility(
                visible = showSyncTuner,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.48f))
                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    SyncChip("-1s") { onAdjustOffset(-1000L) }
                    SyncChip("-0.25s") { onAdjustOffset(-250L) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onResetOffset)) {
                        Text(
                            text = String.format(Locale.US, "%+.2fs", state.lyricsOffsetMs / 1000.0),
                            color = palette.accent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text("Tap reset", color = Color.White.copy(alpha = 0.55f), fontSize = 9.5.sp)
                    }
                    SyncChip("+0.25s") { onAdjustOffset(250L) }
                    SyncChip("+1s") { onAdjustOffset(1000L) }
                }
            }

            // Lyrics Cinema Canvas with Top & Bottom Soft Fade Masks
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.isLoadingLyrics -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.6.dp)
                                Spacer(Modifier.height(14.dp))
                                Text("Matching exact studio lyrics...", color = Color.White.copy(alpha = 0.85f), fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    lines.isEmpty() -> {
                        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No synced lyrics found for this track yet.",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 26.dp, end = 26.dp, top = 54.dp, bottom = 160.dp),
                            verticalArrangement = Arrangement.spacedBy(22.dp),
                        ) {
                            if (isSynced && activeIdx == -1) {
                                item(key = "intro_dots") {
                                    InstrumentalDotsPulse(modifier = Modifier.padding(vertical = 10.dp))
                                }
                            }

                            itemsIndexed(lines, key = { idx, line -> "${idx}_${line.timeMs}" }) { idx, line ->
                                val isActive = isSynced && idx == activeIdx
                                val isPast = isSynced && activeIdx >= 0 && idx < activeIdx
                                val distance = if (activeIdx >= 0) abs(idx - activeIdx) else 0

                                // Calculate live progress inside the active lyric line for smooth luminous sweep!
                                val lineProgress = if (isActive) {
                                    val nextStart = lines.getOrNull(idx + 1)?.timeMs ?: state.durationMs
                                    val span = (nextStart - line.timeMs).coerceAtLeast(1200L)
                                    ((adjustedPositionMs - line.timeMs).toFloat() / span.toFloat()).coerceIn(0f, 1f)
                                } else 0f

                                // Detect long instrumental break after this line (> 7.5s gap)
                                val nextLineTime = lines.getOrNull(idx + 1)?.timeMs ?: 0L
                                val inInstrumentalBreakAfter = isActive && nextLineTime > 0L &&
                                    (nextLineTime - line.timeMs) > 7500L &&
                                    (adjustedPositionMs - line.timeMs) > 4500L

                                val targetColor by animateColorAsState(
                                    targetValue = when {
                                        !isSynced -> Color.White.copy(alpha = 0.92f)
                                        isActive -> Color.White
                                        userIsScrolling -> Color.White.copy(alpha = 0.78f)
                                        isPast -> Color.White.copy(alpha = 0.26f)
                                        distance == 1 -> Color.White.copy(alpha = 0.56f)
                                        else -> Color.White.copy(alpha = 0.30f)
                                    },
                                    animationSpec = tween(260),
                                    label = "lyricColor",
                                )

                                val targetScale by animateFloatAsState(
                                    targetValue = when {
                                        isActive -> 1.06f
                                        distance == 1 -> 0.98f
                                        else -> 0.94f
                                    },
                                    animationSpec = spring(dampingRatio = 0.78f, stiffness = 310f),
                                    label = "lyricScale",
                                )

                                // Optical Depth-of-Field Blur on Android 12+ (API 31+)
                                val targetBlurDp by animateDpAsState(
                                    targetValue = when {
                                        !visual.lyricsDepthBlur || !isSynced || userIsScrolling || isActive -> 0.dp
                                        distance == 1 -> 1.2.dp
                                        distance == 2 -> 2.4.dp
                                        else -> 3.4.dp
                                    },
                                    animationSpec = tween(320),
                                    label = "lyricBlur",
                                )

                                val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && targetBlurDp > 0.dp) {
                                    Modifier.blur(targetBlurDp)
                                } else {
                                    Modifier
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer {
                                            scaleX = targetScale
                                            scaleY = targetScale
                                            transformOrigin = TransformOrigin(0f, 0.5f)
                                        }
                                        .then(blurModifier)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            if (isSynced) {
                                                if (visual.hapticFeedback) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                userIsScrolling = false
                                                onSeekTo((line.timeMs + state.lyricsOffsetMs).coerceAtLeast(0L))
                                            }
                                        }
                                        .padding(vertical = 2.dp),
                                ) {
                                    if (isActive) {
                                        // Progressive luminous karaoke highlight brush across active lyric!
                                        val sweepStop = (lineProgress * 1.15f).coerceIn(0.05f, 1f)
                                        val activeBrush = Brush.horizontalGradient(
                                            0.0f to Color.White,
                                            sweepStop to Color.White,
                                            (sweepStop + 0.12f).coerceAtMost(1f) to Color.White.copy(alpha = 0.62f),
                                            1.0f to Color.White.copy(alpha = 0.55f),
                                        )
                                        Text(
                                            text = line.text,
                                            style = TextStyle(
                                                brush = activeBrush,
                                                fontSize = 27.sp,
                                                fontWeight = FontWeight.Black,
                                                lineHeight = 35.sp,
                                                letterSpacing = (-0.6).sp,
                                            ),
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    } else {
                                        Text(
                                            text = line.text,
                                            color = targetColor,
                                            fontSize = 25.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            lineHeight = 33.sp,
                                            letterSpacing = (-0.5).sp,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }

                                    if (inInstrumentalBreakAfter) {
                                        Spacer(Modifier.height(14.dp))
                                        InstrumentalDotsPulse()
                                    }
                                }
                            }
                        }
                    }
                }

                // Soft Top & Bottom Gradient Edge Masks for Seamless Cinema Melt
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(34.dp)
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent)))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.50f))))
                )
            }

            // Frosted Bottom Dock with Squiggly Wave Scrubber + Expressive Transport Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.38f))
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                SquigglyProgressBar(
                    positionMs = state.positionMs,
                    durationMs = state.durationMs,
                    isPlaying = state.isPlaying,
                    squigglyEnabled = visual.squigglyProgressBar,
                    hapticEnabled = visual.hapticFeedback,
                    accentColor = palette.accent,
                    onSeekTo = onSeekTo,
                    compact = true,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    IconButton(onClick = onSkipPrevious, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                    Spacer(Modifier.width(24.dp))
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable(onClick = onTogglePlayPause),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    Spacer(Modifier.width(24.dp))
                    IconButton(onClick = onSkipNext, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.14f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(label, color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
    }
}
