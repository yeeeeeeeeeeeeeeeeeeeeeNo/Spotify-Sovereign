package com.spotify.sovereign.ui.theme

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SpotifyLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    circleColor: Color = SpotifyColors.Green,
    waveColor: Color = Color(0xFF08080A),
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawCircle(color = circleColor, radius = w / 2f)

        val topWave = Path().apply {
            moveTo(w * 0.22f, h * 0.36f)
            cubicTo(w * 0.40f, h * 0.30f, w * 0.64f, h * 0.32f, w * 0.79f, h * 0.42f)
        }
        drawPath(topWave, color = waveColor, style = Stroke(width = w * 0.082f, cap = StrokeCap.Round))

        val midWave = Path().apply {
            moveTo(w * 0.26f, h * 0.52f)
            cubicTo(w * 0.42f, h * 0.47f, w * 0.61f, h * 0.49f, w * 0.73f, h * 0.57f)
        }
        drawPath(midWave, color = waveColor, style = Stroke(width = w * 0.07f, cap = StrokeCap.Round))

        val botWave = Path().apply {
            moveTo(w * 0.30f, h * 0.66f)
            cubicTo(w * 0.43f, h * 0.62f, w * 0.58f, h * 0.64f, w * 0.68f, h * 0.70f)
        }
        drawPath(botWave, color = waveColor, style = Stroke(width = w * 0.058f, cap = StrokeCap.Round))
    }
}

@Composable
fun LikedSongsArt(
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
) {
    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                colors = listOf(Color(0xFF450AF5), Color(0xFF7B5CF6), Color(0xFFC4EFD9)),
                start = Offset.Zero,
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
            )
        ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = "Liked Songs",
            tint = Color.White,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
fun SpotifyEqualizerBars(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = SpotifyColors.Green,
) {
    val transition = rememberInfiniteTransition(label = "eq")
    val b1 by transition.animateFloat(
        initialValue = 0.25f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(410), RepeatMode.Reverse), label = "b1"
    )
    val b2 by transition.animateFloat(
        initialValue = 0.95f, targetValue = 0.28f,
        animationSpec = infiniteRepeatable(tween(340), RepeatMode.Reverse), label = "b2"
    )
    val b3 by transition.animateFloat(
        initialValue = 0.45f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(470), RepeatMode.Reverse), label = "b3"
    )
    val heights = if (isPlaying) listOf(b1, b2, b3) else listOf(0.28f, 0.28f, 0.28f)
    Row(
        modifier = modifier.height(14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        heights.forEach { frac ->
            Box(
                modifier = Modifier
                    .width(2.6.dp)
                    .height((14f * frac).dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(barColor)
            )
        }
    }
}

@Composable
fun InstrumentalDotsPulse(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "dots")
    val a1 by transition.animateFloat(0.25f, 1f, infiniteRepeatable(tween(620), RepeatMode.Reverse), label = "d1")
    val a2 by transition.animateFloat(0.25f, 1f, infiniteRepeatable(tween(620, delayMillis = 180), RepeatMode.Reverse), label = "d2")
    val a3 by transition.animateFloat(0.25f, 1f, infiniteRepeatable(tween(620, delayMillis = 360), RepeatMode.Reverse), label = "d3")
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(a1, a2, a3).forEach { alpha ->
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = alpha))
            )
        }
    }
}

@Composable
fun ExplicitBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(14.dp)
            .clip(RoundedCornerShape(2.5.dp))
            .background(SpotifyColors.ExplicitBg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "E",
            color = Color(0xFF0A0A0C),
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 9.sp,
        )
    }
}

@Composable
fun SpotifyLibraryIcon(
    selected: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val stroke = if (selected) 2.5.dp.toPx() else 1.85.dp.toPx()
        val w = size.width
        val h = size.height
        drawLine(tint, Offset(w * 0.18f, h * 0.18f), Offset(w * 0.18f, h * 0.84f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.42f, h * 0.18f), Offset(w * 0.42f, h * 0.84f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.64f, h * 0.20f), Offset(w * 0.86f, h * 0.82f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}
