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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.spotify.sovereign.model.CollectionItem
import com.spotify.sovereign.model.TrackItem
import com.spotify.sovereign.ui.SovereignUiState
import com.spotify.sovereign.ui.theme.ExplicitBadge
import com.spotify.sovereign.ui.theme.SovereignTheme
import com.spotify.sovereign.ui.theme.SpotifyEqualizerBars

private data class BrowseGenre(val title: String, val query: String, val color: Color, val accent: Color)

private val GENRES = listOf(
    BrowseGenre("Music", "top hits 2026", Color(0xFFDC148C), Color(0xFFFF75BE)),
    BrowseGenre("Hip-Hop", "rap trap hits", Color(0xFFBC5900), Color(0xFFFF9F45)),
    BrowseGenre("R&B", "modern r&b dark soul", Color(0xFFE91429), Color(0xFFFF6D7C)),
    BrowseGenre("Electronic", "synthwave house electronic", Color(0xFFD84000), Color(0xFFFF8652)),
    BrowseGenre("Pop", "pop hits viral", Color(0xFF148A08), Color(0xFF56E549)),
    BrowseGenre("Chill", "late night lofi chill", Color(0xFF3E6F88), Color(0xFF83C6E6)),
    BrowseGenre("Indie", "indie alternative dream pop", Color(0xFF5B2C6F), Color(0xFFA569BD)),
    BrowseGenre("Workout", "gym phonk bass hype", Color(0xFF006450), Color(0xFF3FD1B3)),
)

@Composable
fun SearchScreen(
    state: SovereignUiState,
    onOpenProfile: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSubmitSearch: (String) -> Unit,
    onClearSearch: () -> Unit,
    onPlayTrack: (TrackItem, List<TrackItem>) -> Unit,
    onClickCollection: (CollectionItem) -> Unit,
) {
    val palette = SovereignTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(palette.background, palette.backgroundDeep)))
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
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
            Text("Search", color = palette.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
        }

        // Pure White Spotify Search Input
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .height(46.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF121212), modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (state.searchQuery.isEmpty()) {
                        Text("What do you want to listen to?", color = Color(0xFF4B4B4B), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                    BasicTextField(
                        value = state.searchQuery,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        cursorBrush = SolidColor(Color(0xFF121212)),
                        textStyle = TextStyle(color = Color(0xFF121212), fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSubmitSearch(state.searchQuery) }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = onClearSearch, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear", tint = Color(0xFF121212), modifier = Modifier.size(19.dp))
                    }
                }
            }
        }

        if (state.isSearching) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = palette.accent, strokeWidth = 2.5.dp)
            }
        } else if (state.searchResults != null && state.searchQuery.isNotBlank()) {
            val res = state.searchResults
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 186.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (res.songs.isNotEmpty()) {
                    item {
                        Text("Songs", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    items(res.songs.take(15), key = { "sr_${it.id}" }) { track ->
                        val isCurrent = state.currentTrack?.id == track.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onPlayTrack(track, res.songs) }
                                .padding(vertical = 4.dp),
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
                                    Text("Song · ${track.artist}", color = palette.textMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }

                val cols = (res.artists + res.albums + res.playlists).take(12)
                if (cols.isNotEmpty()) {
                    item {
                        Text("Artists, Albums & Playlists", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 10.dp))
                    }
                    items(cols, key = { "sc_${it.id}_${it.title}" }) { col ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onClickCollection(col) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AsyncImage(
                                model = col.thumbnailUrl,
                                contentDescription = col.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(if (col.type == "artist") CircleShape else RoundedCornerShape(6.dp))
                                    .background(palette.card),
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(col.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(col.subtitle.ifBlank { col.type }, color = palette.textMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 186.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                if (state.searchHistory.isNotEmpty()) {
                    item {
                        Column {
                            Text("Recent searches", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(10.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(state.searchHistory) { h ->
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(palette.glassSurface)
                                            .border(1.dp, palette.glassBorder, CircleShape)
                                            .clickable { onSubmitSearch(h) }
                                            .padding(horizontal = 14.dp, vertical = 7.dp),
                                    ) {
                                        Text(h, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Column {
                        Text("Explore your genres", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SearchMoodCard("#synthwave", listOf(Color(0xFF8C1428), Color(0xFF1B0409)), Modifier.weight(1f)) { onSubmitSearch("synthwave dark pop") }
                            SearchMoodCard("#dark trap", listOf(Color(0xFF5A402D), Color(0xFF140D09)), Modifier.weight(1f)) { onSubmitSearch("dark trap utopia") }
                            SearchMoodCard("#alt r&b", listOf(Color(0xFF1A7362), Color(0xFF081B18)), Modifier.weight(1f)) { onSubmitSearch("alternative r&b") }
                        }
                    }
                }

                item {
                    Column {
                        Text("Browse all", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            GENRES.chunked(2).forEach { pair ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    pair.forEach { cat ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(94.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(cat.color)
                                                .clickable { onSubmitSearch(cat.query) }
                                                .padding(12.dp),
                                        ) {
                                            Text(cat.title, color = Color.White, fontSize = 15.5.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.fillMaxWidth(0.7f))
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .offset(x = 18.dp, y = 14.dp)
                                                    .size(64.dp)
                                                    .rotate(25f)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Brush.linearGradient(listOf(cat.accent, Color.Black.copy(alpha = 0.65f))))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchMoodCard(label: String, colors: List<Color>, modifier: Modifier, onClick: () -> Unit) {
    val palette = SovereignTheme.colors
    Box(
        modifier = modifier
            .height(156.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(colors))
            .border(0.8.dp, palette.glassBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
    }
}
