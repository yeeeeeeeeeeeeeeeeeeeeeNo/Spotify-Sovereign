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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.spotify.sovereign.model.CollectionItem
import com.spotify.sovereign.model.LibraryFilter
import com.spotify.sovereign.model.TrackItem
import com.spotify.sovereign.ui.SovereignUiState
import com.spotify.sovereign.ui.theme.LikedSongsArt
import com.spotify.sovereign.ui.theme.SovereignTheme

@Composable
fun LibraryScreen(
    state: SovereignUiState,
    onOpenProfile: () -> Unit,
    onOpenSearch: () -> Unit,
    onSelectFilter: (LibraryFilter) -> Unit,
    onToggleSort: () -> Unit,
    onToggleGrid: () -> Unit,
    onClickCollection: (CollectionItem) -> Unit,
    onPlayTrack: (TrackItem, List<TrackItem>) -> Unit,
) {
    val palette = SovereignTheme.colors
    val likedCard = CollectionItem(
        id = "liked-songs",
        browseId = "LOCAL_LIKED",
        title = "Liked Songs",
        subtitle = "Playlist · ${state.likedTracks.size} songs",
        type = "liked",
        pinned = true,
        tracks = state.likedTracks,
    )

    val list = buildList {
        if (state.libraryFilter == LibraryFilter.ALL || state.libraryFilter == LibraryFilter.PLAYLISTS) {
            add(likedCard)
        }
        val filtered = state.savedCollections.filter { col ->
            when (state.libraryFilter) {
                LibraryFilter.ALL -> true
                LibraryFilter.PLAYLISTS -> col.type == "playlist" || col.type == "ai_mix"
                LibraryFilter.ALBUMS -> col.type == "album"
                LibraryFilter.ARTISTS -> col.type == "artist"
            }
        }
        addAll(if (state.librarySortAlpha) filtered.sortedBy { it.title.lowercase() } else filtered)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(palette.background, palette.backgroundDeep)))
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.ytAccount.signedIn && state.ytAccount.avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = state.ytAccount.avatarUrl,
                        contentDescription = "Account",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(34.dp).clip(CircleShape).border(1.4.dp, palette.accent, CircleShape).clickable(onClick = onOpenProfile),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(palette.accent, palette.accent.copy(alpha = 0.55f))))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            .clickable(onClick = onOpenProfile),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(state.ytAccount.name.take(1).uppercase().ifBlank { "S" }, color = Color.Black, fontWeight = FontWeight.Black, fontSize = 13.5.sp)
                    }
                }
                Text("Your Library", color = palette.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
            }
            IconButton(onClick = onOpenSearch) {
                Icon(Icons.Filled.Search, contentDescription = "Search", tint = Color.White)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LibraryFilter.entries.forEach { f ->
                val active = state.libraryFilter == f
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(CircleShape)
                        .background(if (active) palette.accent else palette.glassSurface)
                        .border(1.dp, if (active) palette.accent else palette.glassBorder, CircleShape)
                        .clickable { onSelectFilter(f) }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(f.label, color = if (active) Color.Black else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.clickable(onClick = onToggleSort),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.SwapVert, contentDescription = "Sort", tint = Color.White, modifier = Modifier.size(16.dp))
                Text(if (state.librarySortAlpha) "Alphabetical" else "Recents", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            IconButton(onClick = onToggleGrid, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = if (state.libraryGridMode) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.GridView,
                    contentDescription = "Layout",
                    tint = Color.White,
                    modifier = Modifier.size(19.dp),
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 186.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.libraryGridMode) {
                items(list.chunked(3)) { rowItems ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rowItems.forEach { item ->
                            Column(modifier = Modifier.weight(1f).clickable { onClickCollection(item) }) {
                                if (item.type == "liked") {
                                    LikedSongsArt(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(8.dp)), 34.dp)
                                } else {
                                    AsyncImage(
                                        model = item.thumbnailUrl,
                                        contentDescription = item.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .clip(if (item.type == "artist") CircleShape else RoundedCornerShape(8.dp))
                                            .background(palette.card),
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(item.title, color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(item.subtitle, color = palette.textMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            } else {
                items(list, key = { "lb_${it.id}" }) { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onClickCollection(item) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (item.type == "liked") {
                            LikedSongsArt(Modifier.size(62.dp).clip(RoundedCornerShape(8.dp)), 24.dp)
                        } else {
                            AsyncImage(
                                model = item.thumbnailUrl,
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(62.dp)
                                    .clip(if (item.type == "artist") CircleShape else RoundedCornerShape(8.dp))
                                    .background(palette.card),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, color = Color.White, fontSize = 15.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                if (item.pinned) {
                                    Icon(Icons.Filled.PushPin, contentDescription = null, tint = palette.accent, modifier = Modifier.size(13.dp))
                                }
                                Text(item.subtitle, color = palette.textMuted, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            if (state.recentTracks.isNotEmpty() && state.libraryFilter == LibraryFilter.ALL) {
                item {
                    Text("Recently played", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 12.dp))
                }
                items(state.recentTracks.take(14), key = { "lrec_${it.id}" }) { track ->
                    val isCurrent = state.currentTrack?.id == track.id
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onPlayTrack(track, state.recentTracks) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncImage(
                            model = track.thumbnailUrl,
                            contentDescription = track.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(50.dp).clip(RoundedCornerShape(6.dp)).background(palette.card),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(track.title, color = if (isCurrent) palette.accent else Color.White, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(track.artist, color = palette.textMuted, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}
