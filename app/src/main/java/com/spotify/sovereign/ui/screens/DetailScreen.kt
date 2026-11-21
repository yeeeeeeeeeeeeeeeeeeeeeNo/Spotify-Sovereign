package com.spotify.sovereign.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.spotify.sovereign.model.DetailPageData
import com.spotify.sovereign.model.TrackItem
import com.spotify.sovereign.ui.SovereignUiState
import com.spotify.sovereign.ui.theme.ExplicitBadge
import com.spotify.sovereign.ui.theme.LikedSongsArt
import com.spotify.sovereign.ui.theme.SovereignTheme
import com.spotify.sovereign.ui.theme.SpotifyEqualizerBars

@Composable
fun DetailScreen(
    page: DetailPageData,
    state: SovereignUiState,
    onBack: () -> Unit,
    onToggleSave: () -> Unit,
    onToggleShuffle: () -> Unit,
    onPlayTrack: (TrackItem, List<TrackItem>) -> Unit,
    onTogglePlayPause: () -> Unit,
    onToggleLikeTrack: (TrackItem) -> Unit,
) {
    val palette = SovereignTheme.colors
    val isSaved = state.savedCollections.any { it.id == page.id }
    val isPlayingHere = state.isPlaying && page.tracks.any { it.id == state.currentTrack?.id }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(palette.backgroundDeep),
        contentPadding = PaddingValues(bottom = 186.dp),
    ) {
        item(key = "hero") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            0.0f to state.detailAccentColor,
                            0.62f to state.detailAccentColor.copy(alpha = 0.36f),
                            1.0f to palette.backgroundDeep,
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                Spacer(Modifier.height(10.dp))

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (page.type == "liked") {
                        LikedSongsArt(Modifier.size(230.dp).shadow(28.dp, RoundedCornerShape(10.dp)).clip(RoundedCornerShape(10.dp)), 84.dp)
                    } else {
                        AsyncImage(
                            model = page.thumbnailUrl,
                            contentDescription = page.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(230.dp)
                                .shadow(28.dp, RoundedCornerShape(10.dp))
                                .clip(RoundedCornerShape(10.dp))
                                .background(palette.card),
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text(page.title, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
                if (page.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(page.description, color = palette.textMuted, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                if (page.subtitle.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(page.subtitle, color = Color.White.copy(alpha = 0.78f), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    if (page.type != "liked") {
                        IconButton(onClick = onToggleSave) {
                            Icon(
                                imageVector = if (isSaved) Icons.Filled.CheckCircle else Icons.Filled.AddCircleOutline,
                                contentDescription = "Save",
                                tint = if (isSaved) palette.accent else palette.textMuted,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    } else Spacer(Modifier.size(26.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(contentAlignment = Alignment.BottomCenter) {
                            IconButton(onClick = onToggleShuffle) {
                                Icon(
                                    Icons.Filled.Shuffle,
                                    contentDescription = "Shuffle",
                                    tint = if (state.shuffleEnabled) palette.accent else palette.textMuted,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                            if (state.shuffleEnabled) {
                                Box(Modifier.size(4.dp).clip(CircleShape).background(palette.accent))
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .shadow(14.dp, CircleShape)
                                .clip(CircleShape)
                                .background(palette.accent)
                                .clickable {
                                    if (isPlayingHere) onTogglePlayPause()
                                    else if (page.tracks.isNotEmpty()) {
                                        val pick = if (state.shuffleEnabled) page.tracks.random() else page.tracks.first()
                                        onPlayTrack(pick, page.tracks)
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (isPlayingHere) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(30.dp),
                            )
                        }
                    }
                }
            }
        }

        if (state.isLoadingDetail && page.tracks.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = palette.accent, strokeWidth = 2.5.dp)
                }
            }
        } else {
            items(page.tracks, key = { "dt_${it.id}" }) { track ->
                val isCurrent = state.currentTrack?.id == track.id
                val isLiked = state.likedTracks.any { it.id == track.id }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlayTrack(track, page.tracks) }
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (track.thumbnailUrl.isNotBlank()) {
                        AsyncImage(
                            model = track.thumbnailUrl,
                            contentDescription = track.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)).background(palette.card),
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isCurrent) {
                                SpotifyEqualizerBars(isPlaying = state.isPlaying, barColor = palette.accent)
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(track.title, color = if (isCurrent) palette.accent else Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (track.explicit) {
                                ExplicitBadge()
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(track.artist, color = palette.textMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    IconButton(onClick = { onToggleLikeTrack(track) }) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) palette.accent else palette.textMuted,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}
