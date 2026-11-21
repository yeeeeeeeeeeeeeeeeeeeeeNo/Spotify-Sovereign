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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.spotify.sovereign.model.CollectionItem
import com.spotify.sovereign.model.HomeFilter
import com.spotify.sovereign.model.HomeShelf
import com.spotify.sovereign.ui.SovereignUiState
import com.spotify.sovereign.ui.theme.LikedSongsArt
import com.spotify.sovereign.ui.theme.SovereignTheme
import com.spotify.sovereign.ui.theme.SpotifyEqualizerBars
import com.spotify.sovereign.ui.theme.SpotifyLogoMark

@Composable
fun HomeScreen(
    state: SovereignUiState,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectFilter: (HomeFilter) -> Unit,
    onClickCollection: (CollectionItem) -> Unit,
) {
    val palette = SovereignTheme.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0.0f to state.dominantColor.copy(alpha = 0.36f),
                    0.26f to palette.background,
                    1.0f to palette.backgroundDeep,
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // Ultra-Modern Frosted Top Header with Direct Settings Gear Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFFFF6B4A), Color(0xFFD92B6B), Color(0xFF6B21A8))))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        .clickable(onClick = onOpenProfile),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("A", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp)
                }

                HomeFilter.entries.forEach { f ->
                    val active = state.homeFilter == f
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .clip(CircleShape)
                            .background(if (active) palette.accent else palette.glassSurface)
                            .border(
                                1.dp,
                                if (active) palette.accent else palette.glassBorder,
                                CircleShape,
                            )
                            .clickable { onSelectFilter(f) }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = f.label,
                            color = if (active) Color.Black else Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                // 1-Tap Settings & Sound Studio Gear
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(palette.glassSurface)
                        .border(1.dp, palette.glassBorder, CircleShape)
                        .clickable(onClick = onOpenSettings),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            if (state.isBuildingHome && state.homeShelves.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = palette.accent, strokeWidth = 2.6.dp)
                        Spacer(Modifier.height(14.dp))
                        Text("Synthesizing your Taste AI mixes...", color = Color.White, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("${state.tasteDna.currentTimeBucket} Vibe Mode", color = palette.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                return
            }

            val visibleShelves = when (state.homeFilter) {
                HomeFilter.DISCOVER -> state.homeShelves.filter {
                    it.id.contains("fresh") || it.id.contains("artists") || it.id.contains("made")
                }
                else -> state.homeShelves
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 186.dp),
                verticalArrangement = Arrangement.spacedBy(26.dp),
            ) {
                if (state.quickPicks.isNotEmpty()) {
                    item(key = "quick_picks") {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            state.quickPicks.chunked(2).forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    pair.forEach { item ->
                                        val isActive = state.currentTrack != null &&
                                            item.tracks.any { it.id == state.currentTrack.id }
                                        QuickPickGlassTile(
                                            item = item,
                                            isActive = isActive,
                                            isPlaying = state.isPlaying,
                                            onClick = { onClickCollection(item) },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                items(visibleShelves, key = { it.id }) { shelf ->
                    HomeShelfRow(shelf = shelf, onClickItem = onClickCollection)
                }
            }
        }
    }
}

@Composable
private fun QuickPickGlassTile(
    item: CollectionItem,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = SovereignTheme.colors
    Row(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(palette.card.copy(alpha = 0.88f))
            .border(0.8.dp, palette.glassBorder, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item.type == "liked") {
            LikedSongsArt(modifier = Modifier.size(56.dp), iconSize = 22.dp)
        } else {
            Box(modifier = Modifier.size(56.dp).background(Color(item.accentHex))) {
                if (item.thumbnailUrl.isNotBlank()) {
                    AsyncImage(
                        model = item.thumbnailUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        Row(
            modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = item.title,
                color = if (isActive) palette.accent else Color.White,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (isActive) {
                Spacer(Modifier.width(4.dp))
                SpotifyEqualizerBars(isPlaying = isPlaying, barColor = palette.accent)
            }
        }
    }
}

@Composable
private fun HomeShelfRow(
    shelf: HomeShelf,
    onClickItem: (CollectionItem) -> Unit,
) {
    val palette = SovereignTheme.colors
    Column {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = shelf.title,
                color = Color.White,
                fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.45).sp,
            )
            if (shelf.subtitle.isNotBlank()) {
                Text(
                    text = shelf.subtitle,
                    color = palette.textMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Spacer(Modifier.height(11.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(shelf.items, key = { "${shelf.id}_${it.id}" }) { item ->
                if (shelf.isArtistShelf || item.type == "artist") {
                    Column(
                        modifier = Modifier.width(132.dp).clickable { onClickItem(item) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        AsyncImage(
                            model = item.thumbnailUrl,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(132.dp)
                                .clip(CircleShape)
                                .border(1.dp, palette.glassBorder, CircleShape)
                                .background(palette.card),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(item.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                        if (item.subtitle.isNotBlank()) {
                            Text(item.subtitle, color = palette.textMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    val dim = if (shelf.isCompact) 118.dp else 152.dp
                    Column(modifier = Modifier.width(dim).clickable { onClickItem(item) }) {
                        Box(
                            modifier = Modifier
                                .size(dim)
                                .clip(RoundedCornerShape(8.dp))
                                .border(0.8.dp, palette.glassBorder, RoundedCornerShape(8.dp))
                                .background(Color(item.accentHex)),
                        ) {
                            if (item.thumbnailUrl.isNotBlank()) {
                                AsyncImage(
                                    model = item.thumbnailUrl,
                                    contentDescription = item.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            if (item.type == "ai_mix") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                0.45f to Color.Transparent,
                                                1.0f to Color.Black.copy(alpha = 0.80f),
                                            )
                                        )
                                )
                                Text(
                                    text = item.title,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-0.4).sp,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(start = 10.dp, end = 10.dp, bottom = 12.dp),
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .background(Color(item.accentHex)),
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(item.title, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (item.subtitle.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(item.subtitle, color = palette.textMuted, fontSize = 12.sp, lineHeight = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}
