package com.spotify.sovereign.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.SpatialAudio
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.spotify.sovereign.model.AudioFxSettings
import com.spotify.sovereign.model.DspEngineStatus
import com.spotify.sovereign.model.EQ_BAND_LABELS
import com.spotify.sovereign.model.SoundProfile
import com.spotify.sovereign.model.TrackColorPalette
import com.spotify.sovereign.ui.theme.SovereignTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Living Apple-Music-grade Ambient Aurora Mesh Backdrop.
 * Smoothly crossfades album colors and gently orbits 4 luminous mesh orbs while music plays.
 */
@Composable
fun AmbientAuroraBackground(
    palette: TrackColorPalette,
    isPlaying: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val c1 by animateColorAsState(palette.vibrant, tween(950), label = "aurora1")
    val c2 by animateColorAsState(palette.darkVibrant, tween(950), label = "aurora2")
    val c3 by animateColorAsState(palette.muted, tween(950), label = "aurora3")
    val c4 by animateColorAsState(palette.highlightAccent, tween(950), label = "aurora4")

    var phase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying, enabled) {
        if (!enabled) return@LaunchedEffect
        var lastNs = 0L
        while (true) {
            withFrameNanos { ns ->
                if (lastNs != 0L) {
                    val dt = ((ns - lastNs) / 1_000_000_000f).coerceIn(0f, 0.05f)
                    val speed = if (isPlaying) 0.42f else 0.08f
                    phase = (phase + dt * speed) % (2f * PI.toFloat())
                }
                lastNs = ns
            }
        }
    }

    val intensity by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.72f,
        animationSpec = tween(700),
        label = "auroraIntensity",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        // 100% OPAQUE solid base — guarantees zero bleed-through from Search / Home behind the player!
        drawRect(color = Color(0xFF050508))
        // Deep velvet gradient layer
        drawRect(
            brush = Brush.verticalGradient(
                0.0f to c2.copy(alpha = 0.92f),
                0.55f to Color(0xFF0A090E),
                1.0f to Color(0xFF030305),
            )
        )

        if (!enabled) return@Canvas

        // Orb 1: Primary Vibrant Top-Left Drift
        val o1x = w * (0.24f + 0.18f * cos(phase))
        val o1y = h * (0.18f + 0.10f * sin(phase * 1.3f))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(c1.copy(alpha = 0.62f * intensity), Color.Transparent),
                center = Offset(o1x, o1y),
                radius = w * 0.92f,
            ),
            radius = w * 0.92f,
            center = Offset(o1x, o1y),
        )

        // Orb 2: Secondary Accent Mid-Right Orbit
        val o2x = w * (0.78f + 0.16f * sin(phase * 0.85f + 1.2f))
        val o2y = h * (0.38f + 0.14f * cos(phase * 0.95f))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(c4.copy(alpha = 0.34f * intensity), Color.Transparent),
                center = Offset(o2x, o2y),
                radius = w * 0.82f,
            ),
            radius = w * 0.82f,
            center = Offset(o2x, o2y),
        )

        // Orb 3: Deep Muted Lower-Left Glow
        val o3x = w * (0.28f + 0.22f * sin(phase * 0.7f + 3.1f))
        val o3y = h * (0.68f + 0.10f * cos(phase * 1.1f))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(c3.copy(alpha = 0.48f * intensity), Color.Transparent),
                center = Offset(o3x, o3y),
                radius = w * 0.88f,
            ),
            radius = w * 0.88f,
            center = Offset(o3x, o3y),
        )

        // Subtle dark readability vignette at top & bottom
        drawRect(
            brush = Brush.verticalGradient(
                0.0f to Color.Black.copy(alpha = 0.28f),
                0.22f to Color.Transparent,
                0.72f to Color.Black.copy(alpha = 0.32f),
                1.0f to Color.Black.copy(alpha = 0.82f),
            )
        )
    }
}

/**
 * Expressive Squiggly / Wave Animated Progress Bar.
 * - Flows smoothly as a living sine wave with glowing tail while playing.
 * - Flattens smoothly into a razor-sharp bar when paused or scrubbing.
 * - Supports tap-to-seek, drag-to-seek, floating timestamp badge, and haptics.
 */
@Composable
fun SquigglyProgressBar(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    squigglyEnabled: Boolean,
    hapticEnabled: Boolean = true,
    accentColor: Color = Color.White,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val haptics = LocalHapticFeedback.current
    val safeDuration = durationMs.coerceAtLeast(1L)
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var trackWidthPx by remember { mutableFloatStateOf(1f) }

    val actualFraction = (positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    val displayFraction = if (isDragging) dragFraction else actualFraction
    val displayMs = (displayFraction * safeDuration).toLong()

    // Continuous smooth horizontal wave phase
    val infinite = rememberInfiniteTransition(label = "squiggle")
    val wavePhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1550, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wavePhase",
    )

    // Smoothly collapse wave amplitude when paused, disabled, or scrubbing
    val targetAmp = if (isPlaying && squigglyEnabled && !isDragging) {
        if (compact) 3.0f else 4.2f
    } else 0f
    val ampDp by animateFloatAsState(
        targetValue = targetAmp,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 280f),
        label = "waveAmp",
    )
    val thumbScale by animateFloatAsState(
        targetValue = if (isDragging) 1.38f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 420f),
        label = "thumbScale",
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 30.dp else 38.dp)
                .onSizeChanged { trackWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                .pointerInput(safeDuration) {
                    detectTapGestures { offset ->
                        val frac = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                        if (hapticEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSeekTo((frac * safeDuration).toLong())
                    }
                }
                .pointerInput(safeDuration) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragFraction = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                            if (hapticEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            dragFraction = (change.position.x / trackWidthPx).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            onSeekTo((dragFraction * safeDuration).toLong())
                            isDragging = false
                        },
                        onDragCancel = { isDragging = false },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            // Floating Scrub Time Pill above thumb when dragging
            if (isDragging && !compact) {
                val pillX = ((displayFraction * trackWidthPx) - 64f).coerceIn(0f, (trackWidthPx - 128f).coerceAtLeast(0f))
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset { IntOffset(pillX.roundToInt(), -28) }
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = formatDurationMs(displayMs),
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerY = size.height / 2f
                val padX = 7.dp.toPx()
                val usableW = (size.width - padX * 2f).coerceAtLeast(1f)
                val activeX = padX + usableW * displayFraction
                val trackStroke = (if (compact) 3.0.dp else 3.6.dp).toPx()

                // Inactive smooth rail
                drawLine(
                    color = Color.White.copy(alpha = 0.22f),
                    start = Offset(activeX, centerY),
                    end = Offset(padX + usableW, centerY),
                    strokeWidth = trackStroke,
                    cap = StrokeCap.Round,
                )

                // Active Squiggly Sine Path (or crisp straight line when amp == 0)
                val ampPx = ampDp.dp.toPx()
                val wavelengthPx = (if (compact) 26.dp else 30.dp).toPx()
                val wavePath = Path()
                val stepPx = 2.5f
                var x = padX
                var started = false
                while (x <= activeX) {
                    val distFromEnd = (activeX - x).coerceAtLeast(0f)
                    // Smooth taper near the thumb so the wave meets the playhead cleanly
                    val taper = (distFromEnd / 18.dp.toPx()).coerceIn(0f, 1f)
                    val angle = ((x - padX) / wavelengthPx) * (2f * PI.toFloat()) - wavePhase
                    val y = centerY + sin(angle) * ampPx * taper
                    if (!started) {
                        wavePath.moveTo(x, y)
                        started = true
                    } else {
                        wavePath.lineTo(x, y)
                    }
                    x += stepPx
                }
                if (!started) {
                    wavePath.moveTo(padX, centerY)
                }
                wavePath.lineTo(activeX, centerY)

                // Soft luminous under-glow behind the wave (true live accent color!)
                drawPath(
                    path = wavePath,
                    color = accentColor.copy(alpha = 0.42f),
                    style = Stroke(width = trackStroke * 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
                // Crisp vivid accent wave stroke (clearly changes with every song!)
                drawPath(
                    path = wavePath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(accentColor, accentColor.copy(alpha = 0.94f)),
                        startX = padX,
                        endX = (activeX + 1f),
                    ),
                    style = Stroke(width = trackStroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )

                // Expressive Playhead Pill Thumb tinted cleanly with luminous accent halo
                val pillW = (if (compact) 4.2.dp else 5.0.dp).toPx() * thumbScale
                val pillH = (if (compact) 14.dp else 18.dp).toPx() * thumbScale
                drawCircle(
                    color = accentColor.copy(alpha = if (isDragging) 0.45f else 0.28f),
                    radius = pillH * 0.78f,
                    center = Offset(activeX, centerY),
                )
                drawRoundRect(
                    color = accentColor,
                    topLeft = Offset(activeX - pillW / 2f, centerY - pillH / 2f),
                    size = Size(pillW, pillH),
                    cornerRadius = CornerRadius(pillW / 2f, pillW / 2f),
                )
                // Subtle bright center specular line on the thumb
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.65f),
                    topLeft = Offset(activeX - pillW * 0.22f, centerY - pillH * 0.34f),
                    size = Size(pillW * 0.44f, pillH * 0.68f),
                    cornerRadius = CornerRadius(pillW / 4f, pillW / 4f),
                )
            }
        }

        if (!compact) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formatDurationMs(displayMs),
                    color = if (isDragging) Color.White else Color.White.copy(alpha = 0.68f),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "-${formatDurationMs((safeDuration - displayMs).coerceAtLeast(0L))}",
                    color = Color.White.copy(alpha = 0.68f),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/**
 * Glossy Animated Turntable Vinyl Disc with persistent angle (never snaps back on pause!).
 */
@Composable
fun SpinningVinylArtwork(
    artworkUrl: String,
    title: String,
    isPlaying: Boolean,
    glowColor: Color,
    modifier: Modifier = Modifier,
) {
    var rotationDeg by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        var lastNs = 0L
        while (true) {
            withFrameNanos { ns ->
                if (lastNs != 0L) {
                    val dt = ((ns - lastNs) / 1_000_000_000f).coerceIn(0f, 0.05f)
                    rotationDeg = (rotationDeg + dt * 28f) % 360f
                }
                lastNs = ns
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        // Outer ambient halo
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor.copy(alpha = if (isPlaying) 0.55f else 0.25f), Color.Transparent),
                    radius = size.minDimension * 0.54f,
                ),
                radius = size.minDimension * 0.54f,
            )
        }

        // Rotating Vinyl Record
        Box(
            modifier = Modifier
                .fillMaxSize(0.92f)
                .graphicsLayer { rotationZ = rotationDeg }
                .shadow(28.dp, CircleShape)
                .clip(CircleShape)
                .background(Color(0xFF0A0A0D)),
            contentAlignment = Alignment.Center,
        ) {
            // Concentric realistic vinyl grooves & specular sheen
            Canvas(modifier = Modifier.fillMaxSize()) {
                val rMax = size.minDimension / 2f
                drawCircle(
                    brush = Brush.sweepGradient(
                        0.0f to Color(0xFF15151A),
                        0.18f to Color(0xFF2B2B34),
                        0.32f to Color(0xFF0E0E12),
                        0.52f to Color(0xFF16161C),
                        0.68f to Color(0xFF2E2E38),
                        0.85f to Color(0xFF0C0C10),
                        1.0f to Color(0xFF15151A),
                    )
                )
                var r = rMax * 0.42f
                while (r < rMax * 0.96f) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.055f),
                        radius = r,
                        style = Stroke(width = 1.dp.toPx()),
                    )
                    r += 4.5.dp.toPx()
                }
                drawCircle(
                    color = Color.White.copy(alpha = 0.18f),
                    radius = rMax - 1.dp.toPx(),
                    style = Stroke(width = 1.5.dp.toPx()),
                )
            }

            // Center Album Label
            AsyncImage(
                model = artworkUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize(0.46f)
                    .clip(CircleShape)
                    .border(2.dp, Color.Black.copy(alpha = 0.7f), CircleShape),
            )

            // Center Spindle Hole
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0A0A0C))
                    .border(1.5.dp, Color.White.copy(alpha = 0.35f), CircleShape)
            )
        }
    }
}

/**
 * Interactive Like Button with Spring Pop + Expanding Glow Particle Burst!
 */
@Composable
fun LikeBurstButton(
    isLiked: Boolean,
    accentColor: Color,
    hapticEnabled: Boolean = true,
    onClick: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val burst = remember { Animatable(0f) }
    val scale by animateFloatAsState(
        targetValue = if (isLiked) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 500f),
        label = "likeScale",
    )

    LaunchedEffect(isLiked) {
        if (isLiked) {
            burst.snapTo(0f)
            burst.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
        }
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable {
                if (hapticEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        if (burst.value in 0.01f..0.99f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val p = burst.value
                val alpha = (1f - p).coerceIn(0f, 1f)
                val ringR = size.minDimension * 0.22f + size.minDimension * 0.28f * p
                drawCircle(
                    color = accentColor.copy(alpha = alpha * 0.75f),
                    radius = ringR,
                    style = Stroke(width = (3f * (1f - p)).dp.toPx()),
                )
                for (i in 0 until 8) {
                    val angle = (i * 45.0) * PI / 180.0
                    val dist = size.minDimension * (0.20f + 0.28f * p)
                    val px = center.x + (cos(angle) * dist).toFloat()
                    val py = center.y + (sin(angle) * dist).toFloat()
                    drawCircle(
                        color = accentColor.copy(alpha = alpha),
                        radius = (2.6f * (1f - p * 0.5f)).dp.toPx(),
                        center = Offset(px, py),
                    )
                }
            }
        }
        Icon(
            imageVector = if (isLiked) Icons.Filled.CheckCircle else Icons.Filled.AddCircleOutline,
            contentDescription = "Like",
            tint = if (isLiked) accentColor else Color.White.copy(alpha = 0.88f),
            modifier = Modifier.size(28.dp).scale(scale),
        )
    }
}

/**
 * Live 10-Band Frequency Response Spline Graph (31Hz → 16kHz) with Neon Fill.
 */
@Composable
fun EqualizerCurveCanvas(
    bandsDb: List<Float>,
    enabled: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val lineColor = if (enabled) accentColor else Color.White.copy(alpha = 0.35f)
    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black.copy(alpha = 0.38f))
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            val w = size.width
            val h = size.height
            val midY = h / 2f

            // Reference dB lines (+6, 0, -6)
            listOf(0.2f, 0.5f, 0.8f).forEachIndexed { idx, frac ->
                val y = h * frac
                drawLine(
                    color = Color.White.copy(alpha = if (idx == 1) 0.18f else 0.07f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = if (idx == 1) 1.2.dp.toPx() else 0.8.dp.toPx(),
                )
            }

            if (bandsDb.isEmpty()) return@Canvas
            val pts = bandsDb.mapIndexed { idx, db ->
                val x = if (bandsDb.size == 1) w / 2f else (idx.toFloat() / (bandsDb.size - 1)) * w
                val norm = (db.coerceIn(-10f, 10f) / 10f) // -1..+1
                val y = midY - norm * (h * 0.44f)
                Offset(x, y)
            }

            val curvePath = Path().apply {
                moveTo(pts.first().x, pts.first().y)
                for (i in 0 until pts.lastIndex) {
                    val p0 = pts[i]
                    val p1 = pts[i + 1]
                    val cx = (p0.x + p1.x) / 2f
                    cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                }
            }

            val fillPath = Path().apply {
                addPath(curvePath)
                lineTo(pts.last().x, h)
                lineTo(pts.first().x, h)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.36f), Color.Transparent),
                ),
            )
            drawPath(
                path = curvePath,
                color = lineColor.copy(alpha = 0.35f),
                style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round),
            )
            drawPath(
                path = curvePath,
                color = lineColor,
                style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round),
            )

            pts.forEach { p ->
                drawCircle(color = Color.Black, radius = 4.2.dp.toPx(), center = p)
                drawCircle(color = Color.White, radius = 2.8.dp.toPx(), center = p)
            }
        }
        Spacer(Modifier.height(5.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            EQ_BAND_LABELS.forEach { lbl ->
                Text(lbl, color = Color.White.copy(alpha = 0.48f), fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * In-Player Instant Sovereign Sound Engine & Dolby Atmos Bottom Sheet.
 * Change presets, toggle Atmos 3D, and feel the audio transform live mid-song!
 */
@Composable
fun AudioFxBottomSheet(
    audioFx: AudioFxSettings,
    dspStatus: DspEngineStatus,
    streamBadge: String,
    onUpdateAudioFx: (AudioFxSettings) -> Unit,
    onSelectProfile: (SoundProfile) -> Unit,
    onOpenFullEqSettings: () -> Unit,
) {
    val palette = SovereignTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.78f)
            .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
            .background(Color(0xFF101016))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .width(42.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.28f))
        )
        Spacer(Modifier.height(14.dp))

        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.accent.copy(alpha = 0.18f))
                        .border(1.dp, palette.accent.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.SpatialAudio, contentDescription = null, tint = palette.accent, modifier = Modifier.size(22.dp))
                }
                Column {
                    Text("Sovereign Sound Engine", color = Color.White, fontSize = 17.5.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        text = "$streamBadge · ${dspStatus.engineSummary}",
                        color = palette.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Switch(
                checked = audioFx.masterEnabled,
                onCheckedChange = { onUpdateAudioFx(audioFx.copy(masterEnabled = it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = palette.accent,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color.White.copy(alpha = 0.18f),
                ),
            )
        }

        Spacer(Modifier.height(14.dp))

        // Live Frequency Response Curve
        EqualizerCurveCanvas(
            bandsDb = audioFx.effectiveBandsDb(),
            enabled = audioFx.masterEnabled && audioFx.profile != SoundProfile.FLAT_REFERENCE,
            accentColor = palette.accent,
        )

        Spacer(Modifier.height(16.dp))
        Text("STUDIO SOUND PROFILES", color = Color.White.copy(alpha = 0.56f), fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.1.sp)
        Spacer(Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 8.dp),
        ) {
            items(SoundProfile.entries) { prof ->
                val selected = audioFx.profile == prof && audioFx.masterEnabled
                Column(
                    modifier = Modifier
                        .width(142.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) palette.accent.copy(alpha = 0.18f) else Color(0xFF1A1A22))
                        .border(
                            width = if (selected) 1.4.dp else 0.9.dp,
                            color = if (selected) palette.accent else Color.White.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(14.dp),
                        )
                        .clickable { onSelectProfile(prof) }
                        .padding(12.dp),
                ) {
                    Text(
                        text = prof.shortBadge,
                        color = if (selected) palette.accent else Color.White.copy(alpha = 0.55f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(prof.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(2.dp))
                    Text(prof.subtitle, color = Color.White.copy(alpha = 0.62f), fontSize = 10.5.sp, lineHeight = 13.5.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Dolby Atmos Spatial Toggle Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF181820))
                .border(1.dp, if (audioFx.atmosSpatialEnabled && audioFx.masterEnabled) palette.accent.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Dolby Atmos 3D Spatializer", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                Text("Widens stereo soundstage & 3D instrument separation", color = Color.White.copy(alpha = 0.62f), fontSize = 11.5.sp)
            }
            Switch(
                checked = audioFx.atmosSpatialEnabled,
                enabled = audioFx.masterEnabled,
                onCheckedChange = { onUpdateAudioFx(audioFx.copy(atmosSpatialEnabled = it)) },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = palette.accent),
            )
        }

        Spacer(Modifier.height(12.dp))

        // Live Precision Sliders
        FxQuickSlider(
            label = "3D Spatial Width",
            value = audioFx.spatialWidth,
            enabled = audioFx.masterEnabled && audioFx.atmosSpatialEnabled,
            accentColor = palette.accent,
            onChange = { onUpdateAudioFx(audioFx.copy(spatialWidth = it)) },
        )
        FxQuickSlider(
            label = "Sub-Bass Punch",
            value = audioFx.bassBoost,
            enabled = audioFx.masterEnabled,
            accentColor = palette.accent,
            onChange = { onUpdateAudioFx(audioFx.copy(bassBoost = it)) },
        )
        FxQuickSlider(
            label = "Studio Clarity & Air Exciter",
            value = audioFx.clarityExciter,
            enabled = audioFx.masterEnabled,
            accentColor = palette.accent,
            onChange = { onUpdateAudioFx(audioFx.copy(clarityExciter = it)) },
        )

        Spacer(Modifier.height(12.dp))

        // Open Full 10-Band EQ & Settings Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(palette.accent)
                .clickable(onClick = onOpenFullEqSettings)
                .padding(vertical = 13.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Tune, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Open 10-Band Graphic EQ & Full Settings", color = Color.Black, fontSize = 13.5.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun FxQuickSlider(
    label: String,
    value: Float,
    enabled: Boolean,
    accentColor: Color,
    onChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White.copy(alpha = if (enabled) 0.90f else 0.42f), fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            Text("${(value * 100).roundToInt()}%", color = if (enabled) accentColor else Color.White.copy(alpha = 0.35f), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        }
        Slider(
            value = value.coerceIn(0f, 1f),
            onValueChange = onChange,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = accentColor,
                inactiveTrackColor = Color.White.copy(alpha = 0.16f),
            ),
            modifier = Modifier.fillMaxWidth().height(28.dp),
        )
    }
}

fun formatDurationMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return "$mins:${if (secs < 10) "0" else ""}$secs"
}
