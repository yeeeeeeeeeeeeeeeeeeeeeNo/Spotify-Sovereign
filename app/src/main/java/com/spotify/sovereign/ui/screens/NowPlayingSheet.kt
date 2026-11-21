package com.spotify.sovereign.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.spotify.sovereign.model.ArtworkStyle
import com.spotify.sovereign.model.RepeatMode
import com.spotify.sovereign.model.TrackItem
import com.spotify.sovereign.ui.SovereignUiState
import com.spotify.sovereign.ui.components.AmbientAuroraBackground
import com.spotify.sovereign.ui.components.LikeBurstButton
import com.spotify.sovereign.ui.components.SpinningVinylArtwork
import com.spotify.sovereign.ui.components.SquigglyProgressBar
import com.spotify.sovereign.ui.theme.SovereignTheme
import com.spotify.sovereign.ui.theme.SpotifyEqualizerBars

@Composable
fun NowPlayingSheet(
    state: SovereignUiState,
    onClose: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleLike: (TrackItem) -> Unit,
    onOpenQueue: () -> Unit,
    onOpenFullLyrics: () -> Unit,
    onOpenSoundFx: () -> Unit,
    onCycleArtworkStyle: () -> Unit,
) {
    val track = state.currentTrack ?: return
    val isLiked = state.likedTracks.any { it.id == track.id }
    val palette = SovereignTheme.colors
    val visual = state.settings.visual
    val audioFx = state.settings.audioFx
    val haptics = LocalHapticFeedback.current

    // Breathing scale & glow pulse while playing
    val baseArtScale by animateFloatAsState(
        targetValue = if (state.isPlaying) 1f else 0.90f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 320f),
        label = "artScale",
    )
    val pulseTransition = rememberInfiniteTransition(label = "artPulse")
    val breathingGlow by pulseTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = AnimRepeatMode.Reverse,
        ),
        label = "glowBreath",
    )

    // Interactive Horizontal Swipe-to-Skip Artwork Gesture
    var swipeOffsetX by remember(track.id) { mutableFloatStateOf(0f) }
    val animSwipeX by animateFloatAsState(
        targetValue = swipeOffsetX,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 420f),
        label = "swipeX",
    )

    // Swipe-down-to-close accumulator on top header
    var headerDragY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050508))
            .clickable(enabled = false) {},
    ) {
        // 1. Living Multi-Orb Ambient Aurora Mesh Background (100% opaque base)
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
            // 2. Ultra-Clean Minimal Top Bar (Swipe Down to Minimize + Subtle Handle + Clean EQ Icon)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { headerDragY = 0f },
                            onVerticalDrag = { _, dy -> if (dy > 0) headerDragY += dy },
                            onDragEnd = {
                                if (headerDragY > 110f) onClose()
                                headerDragY = 0f
                            },
                        )
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onClose, modifier = Modifier.size(38.dp)) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Minimize",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp),
                    )
                }

                // Minimal Frosted Pull Handle
                Box(
                    modifier = Modifier
                        .width(38.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.28f))
                )

                // Clean Minimal Sound FX Icon (Zero text/pill clutter)
                IconButton(
                    onClick = {
                        if (visual.hapticFeedback) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onOpenSoundFx()
                    },
                    modifier = Modifier.size(38.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.GraphicEq,
                        contentDescription = "Sound Engine",
                        tint = if (audioFx.masterEnabled) palette.accent else Color.White.copy(alpha = 0.78f),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            // 3. Scrollable Main Stage + Interactive Controls + Frosted Lyrics Card
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
            ) {
                Spacer(Modifier.height(10.dp))

                // Interactive Animated Artwork Stage (Swipe Left/Right to Skip, Tap Style Chip to Switch!)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(track.id) {
                            detectHorizontalDragGestures(
                                onDragStart = { swipeOffsetX = 0f },
                                onHorizontalDrag = { _, dragAmount ->
                                    swipeOffsetX = (swipeOffsetX + dragAmount).coerceIn(-220f, 220f)
                                },
                                onDragEnd = {
                                    if (swipeOffsetX < -105f) {
                                        if (visual.hapticFeedback) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onSkipNext()
                                    } else if (swipeOffsetX > 105f) {
                                        if (visual.hapticFeedback) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onSkipPrevious()
                                    }
                                    swipeOffsetX = 0f
                                },
                                onDragCancel = { swipeOffsetX = 0f },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedContent(
                        targetState = track,
                        transitionSpec = {
                            (fadeIn(tween(320)) + scaleIn(initialScale = 0.92f, animationSpec = tween(320))) togetherWith
                                (fadeOut(tween(220)) + scaleOut(targetScale = 0.92f, animationSpec = tween(220)))
                        },
                        label = "artworkTrackTransition",
                    ) { animTrack ->
                        val swipeModifier = Modifier.graphicsLayer {
                            translationX = animSwipeX
                            rotationZ = (animSwipeX / 22f).coerceIn(-9f, 9f)
                            scaleX = baseArtScale * (1f - (kotlin.math.abs(animSwipeX) / 1800f))
                            scaleY = baseArtScale * (1f - (kotlin.math.abs(animSwipeX) / 1800f))
                        }

                        when (visual.artworkStyle) {
                            ArtworkStyle.VINYL_DISC -> {
                                SpinningVinylArtwork(
                                    artworkUrl = animTrack.thumbnailUrl,
                                    title = animTrack.title,
                                    isPlaying = state.isPlaying,
                                    glowColor = state.trackPalette.vibrant,
                                    modifier = swipeModifier.clickable(onClick = onCycleArtworkStyle),
                                )
                            }

                            ArtworkStyle.FULL_IMMERSIVE -> {
                                Box(
                                    modifier = swipeModifier
                                        .fillMaxWidth()
                                        .aspectRatio(0.86f)
                                        .shadow(34.dp, RoundedCornerShape(22.dp), ambientColor = state.trackPalette.vibrant, spotColor = state.trackPalette.vibrant)
                                        .clip(RoundedCornerShape(22.dp))
                                        .border(1.2.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(22.dp))
                                        .background(Color(0xFF15151C))
                                        .clickable(onClick = onCycleArtworkStyle),
                                ) {
                                    AsyncImage(
                                        model = animTrack.thumbnailUrl,
                                        contentDescription = animTrack.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    0.0f to Color.White.copy(alpha = 0.08f),
                                                    0.28f to Color.Transparent,
                                                    0.72f to Color.Transparent,
                                                    1.0f to Color.Black.copy(alpha = 0.62f),
                                                )
                                            )
                                    )
                                }
                            }

                            ArtworkStyle.HERO_SQUIRCLE -> {
                                Box(
                                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    // Reactive Breathing Colored Ambient Halo behind Artwork
                                    if (visual.artworkBreathingGlow) {
                                        val haloAlpha = if (state.isPlaying) 0.56f * breathingGlow else 0.22f
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            drawCircle(
                                                brush = Brush.radialGradient(
                                                    colors = listOf(
                                                        state.trackPalette.vibrant.copy(alpha = haloAlpha),
                                                        state.trackPalette.highlightAccent.copy(alpha = haloAlpha * 0.35f),
                                                        Color.Transparent,
                                                    ),
                                                    radius = size.minDimension * 0.58f,
                                                ),
                                                radius = size.minDimension * 0.58f,
                                            )
                                        }
                                    }

                                    // Floating Elevated Squircle Artwork Card with Specular Glass Rim
                                    Box(
                                        modifier = swipeModifier
                                            .fillMaxSize(0.94f)
                                            .shadow(
                                                elevation = if (state.isPlaying) 36.dp else 16.dp,
                                                shape = RoundedCornerShape(20.dp),
                                                ambientColor = state.trackPalette.vibrant,
                                                spotColor = state.trackPalette.vibrant,
                                            )
                                            .clip(RoundedCornerShape(20.dp))
                                            .border(
                                                width = 1.2.dp,
                                                brush = Brush.linearGradient(
                                                    listOf(
                                                        Color.White.copy(alpha = 0.34f),
                                                        Color.White.copy(alpha = 0.08f),
                                                        state.trackPalette.highlightAccent.copy(alpha = 0.24f),
                                                    )
                                                ),
                                                shape = RoundedCornerShape(20.dp),
                                            )
                                            .background(Color(0xFF16161E))
                                            .clickable(onClick = onCycleArtworkStyle),
                                    ) {
                                        AsyncImage(
                                            model = animTrack.thumbnailUrl,
                                            contentDescription = animTrack.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                        // Subtle diagonal top-left studio glass sheen
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.linearGradient(
                                                        0.0f to Color.White.copy(alpha = 0.11f),
                                                        0.36f to Color.Transparent,
                                                        1.0f to Color.Black.copy(alpha = 0.18f),
                                                    )
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))

                // 4. Track Title + Artist + Animated Like Particle Burst
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                        AnimatedContent(
                            targetState = track.title,
                            transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(180)) },
                            label = "titleAnim",
                        ) { titleText ->
                            Text(
                                text = titleText,
                                color = Color.White,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.6).sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (state.isPlaying) {
                                SpotifyEqualizerBars(isPlaying = true, barColor = palette.accent)
                            }
                            Text(
                                text = track.artist,
                                color = Color.White.copy(alpha = 0.76f),
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    LikeBurstButton(
                        isLiked = isLiked,
                        accentColor = palette.accent,
                        hapticEnabled = visual.hapticFeedback,
                        onClick = { onToggleLike(track) },
                    )
                }

                Spacer(Modifier.height(14.dp))

                // 5. Expressive Animated Squiggly / Wave Progress Bar!
                SquigglyProgressBar(
                    positionMs = state.positionMs,
                    durationMs = state.durationMs,
                    isPlaying = state.isPlaying,
                    squigglyEnabled = visual.squigglyProgressBar,
                    hapticEnabled = visual.hapticFeedback,
                    accentColor = palette.accent,
                    onSeekTo = onSeekTo,
                )

                Spacer(Modifier.height(12.dp))

                // 6. Expressive Transport Controls with Spring Morph Play/Pause
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Box(contentAlignment = Alignment.BottomCenter) {
                        IconButton(onClick = {
                            if (visual.hapticFeedback) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleShuffle()
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (state.shuffleEnabled) palette.accent else Color.White.copy(alpha = 0.78f),
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        if (state.shuffleEnabled) {
                            Box(Modifier.size(4.5.dp).clip(CircleShape).background(palette.accent))
                        }
                    }

                    IconButton(
                        onClick = {
                            if (visual.hapticFeedback) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSkipPrevious()
                        },
                        modifier = Modifier.size(50.dp),
                    ) {
                        Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(38.dp))
                    }

                    // Expressive Morphing Play/Pause Button (Rounded Squircle when Playing, Circle when Paused!)
                    val playCorner by animateFloatAsState(
                        targetValue = if (state.isPlaying) 24f else 36f,
                        animationSpec = spring(dampingRatio = 0.65f, stiffness = 380f),
                        label = "playCorner",
                    )
                    val playScale by animateFloatAsState(
                        targetValue = if (state.isPlaying) 1.0f else 0.96f,
                        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
                        label = "playScale",
                    )
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .scale(playScale)
                            .shadow(22.dp, RoundedCornerShape(playCorner.dp), ambientColor = Color.White, spotColor = palette.accent)
                            .clip(RoundedCornerShape(playCorner.dp))
                            .background(Color.White)
                            .clickable {
                                if (visual.hapticFeedback) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onTogglePlayPause()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (state.isBuffering) {
                            CircularProgressIndicator(color = Color.Black, strokeWidth = 2.8.dp, modifier = Modifier.size(28.dp))
                        } else {
                            AnimatedContent(
                                targetState = state.isPlaying,
                                transitionSpec = {
                                    (scaleIn(tween(180)) + fadeIn(tween(180))) togetherWith
                                        (scaleOut(tween(150)) + fadeOut(tween(150)))
                                },
                                label = "playIconMorph",
                            ) { playing ->
                                Icon(
                                    imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.Black,
                                    modifier = Modifier.size(38.dp),
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            if (visual.hapticFeedback) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSkipNext()
                        },
                        modifier = Modifier.size(50.dp),
                    ) {
                        Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(38.dp))
                    }

                    Box(contentAlignment = Alignment.BottomCenter) {
                        IconButton(onClick = {
                            if (visual.hapticFeedback) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onCycleRepeat()
                        }) {
                            Icon(
                                imageVector = if (state.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                                contentDescription = "Repeat",
                                tint = if (state.repeatMode != RepeatMode.OFF) palette.accent else Color.White.copy(alpha = 0.78f),
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        if (state.repeatMode != RepeatMode.OFF) {
                            Box(Modifier.size(4.5.dp).clip(CircleShape).background(palette.accent))
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // 7. Clean Utility Bar: Lyrics Pill + Queue
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                            .clickable(onClick = onOpenFullLyrics)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Filled.Lyrics, contentDescription = "Lyrics", tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("Lyrics", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }

                    IconButton(onClick = onOpenQueue, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Queue", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }

                Spacer(Modifier.height(18.dp))

                // 8. Upgraded Frosted Interactive Synced Lyrics Preview Card
                val lines = state.lyricsPayload.lines
                val adjustedPos = (state.positionMs - state.lyricsOffsetMs).coerceAtLeast(0L)
                val activeIdx = if (state.lyricsPayload.isSynced && lines.isNotEmpty()) {
                    lines.indexOfLast { it.timeMs <= adjustedPos }.coerceAtLeast(0)
                } else 0

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    state.trackPalette.vibrant.copy(alpha = 0.72f),
                                    state.trackPalette.darkVibrant.copy(alpha = 0.88f),
                                )
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(22.dp))
                        .clickable(onClick = onOpenFullLyrics)
                        .padding(18.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("Synced Lyrics", color = Color.White, fontSize = 15.5.sp, fontWeight = FontWeight.Black)
                            Text(
                                text = "${state.lyricsPayload.source} · Tap to expand",
                                color = Color.White.copy(alpha = 0.70f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.30f))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.OpenInFull, contentDescription = "Expand", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    if (state.isLoadingLyrics) {
                        Text("Locking exact studio timestamps...", color = Color.White.copy(alpha = 0.85f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    } else if (lines.isEmpty()) {
                        Text("No synced lyrics available for this track.", color = Color.White.copy(alpha = 0.72f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    } else {
                        val windowStart = (activeIdx - 1).coerceAtLeast(0)
                        val window = lines.subList(windowStart, (windowStart + 5).coerceAtMost(lines.size))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            window.forEachIndexed { localIdx, line ->
                                val realIdx = windowStart + localIdx
                                val isActive = state.lyricsPayload.isSynced && realIdx == activeIdx
                                val isPast = state.lyricsPayload.isSynced && realIdx < activeIdx
                                Text(
                                    text = line.text,
                                    color = when {
                                        isActive -> Color.White
                                        isPast -> Color.White.copy(alpha = 0.34f)
                                        else -> Color.White.copy(alpha = 0.56f)
                                    },
                                    fontSize = if (isActive) 20.5.sp else 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    lineHeight = 26.sp,
                                    letterSpacing = (-0.4).sp,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(36.dp))
            }
        }
    }
}
