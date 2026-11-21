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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.spotify.sovereign.model.MusicMoodCategory
import com.spotify.sovereign.model.SongCategoryBlock
import com.spotify.sovereign.model.TrackItem
import com.spotify.sovereign.ui.SovereignUiState
import com.spotify.sovereign.ui.theme.ExplicitBadge
import com.spotify.sovereign.ui.theme.LikedSongsArt
import com.spotify.sovereign.ui.theme.SovereignTheme
import com.spotify.sovereign.ui.theme.SpotifyEqualizerBars

@Composable
fun HomeScreen(
    state: SovereignUiState,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectFilter: (HomeFilter) -> Unit,
    onSelectMusicMood: (MusicMoodCategory) -> Unit,
    onClickCollection: (CollectionItem) -> Unit,
    onPlayTrack: (TrackItem, List<TrackItem>) -> Unit,
    onToggleLikeTrack: (TrackItem) -> Unit,
) {
    val palette = SovereignTheme.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0.0f to state.dominantColor.copy(alpha = 0.28f),
                    0.26f to palette.background,
                    1.0f to palette.backgroundDeep,
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // Ultra-Modern Frosted Top Header with Real YT Account Avatar + Filter Pills + Settings Gear
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.ytAccount.signedIn && state.ytAccount.avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = state.ytAccount.avatarUrl,
                        contentDescription = "Account",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .border(1.4.dp, palette.accent, CircleShape)
                            .clickable(onClick = onOpenProfile),
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
                        val initial = state.ytAccount.name.take(1).uppercase().ifBlank { "S" }
                        Text(initial, color = Color.Black, fontWeight = FontWeight.Black, fontSize = 13.5.sp)
                    }
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
                            color = if (active) Color.Black else palette.textPrimary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                // 1-Tap Settings & Theme Studio Gear
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
                        tint = palette.textPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            if (state.isBuildingHome && state.homeShelves.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = palette.accent, strokeWidth = 2.6.dp)
                        Spacer(Modifier.height(14.dp))
                        Text("Synthesizing your Taste AI mixes...", color = palette.textPrimary, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("${state.tasteDna.currentTimeBucket} Vibe Mode", color = palette.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                return
            }

            // =========================================================================
            // "MUSIC" TAB -> 100% SONGS-ONLY CATEGORIZED VIEW (ZERO ALBUMS/PLAYLISTS!)
            // =========================================================================
            if (state.homeFilter == HomeFilter.MUSIC) {
                MusicSongsFeed(
                    state = state,
                    onSelectMood = onSelectMusicMood,
                    onPlayTrack = onPlayTrack,
                    onToggleLikeTrack = onToggleLikeTrack,
                )
                return
            }

            // =========================================================================
            // "ALL" & "DISCOVER" TABS -> CLASSIC SPOTIFY HOME EXPERIENCE
            // =========================================================================
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
                if (state.homeFilter == HomeFilter.ALL && state.quickPicks.isNotEmpty()) {
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

/**
 * Pure Songs-Only Categorized Discovery Feed for the Home "Music" Tab.
 * Shows zero album covers/playlist tiles/artist circles — 100% instant-play songs grouped into categories & moods!
 */
@Composable
private fun MusicSongsFeed(
    state: SovereignUiState,
    onSelectMood: (MusicMoodCategory) -> Unit,
    onPlayTrack: (TrackItem, List<TrackItem>) -> Unit,
    onToggleLikeTrack: (TrackItem) -> Unit,
) {
    val palette = SovereignTheme.colors

    // Harvest & organize pure deduplicated songs from Taste AI shelves + recents + likes + selected mood
    val allShelfTracks = remember(state.homeShelves, state.quickPicks) {
        (state.homeShelves.flatMap { it.items.flatMap { c -> c.tracks } } +
            state.quickPicks.flatMap { it.tracks })
            .distinctBy { it.id }
    }

    val selectedMood = state.selectedMusicMood
    val moodTracks = state.moodSongsMap[selectedMood].orEmpty()

    val quickPlayPool = remember(selectedMood, moodTracks, allShelfTracks, state.recentTracks, state.likedTracks) {
        if (selectedMood != MusicMoodCategory.FOR_YOU && moodTracks.isNotEmpty()) {
            moodTracks
        } else {
            (state.recentTracks.take(6) + allShelfTracks + state.likedTracks).distinctBy { it.id }
        }
    }

    val spotlightTrack = quickPlayPool.firstOrNull()

    val categoryBlocks = remember(selectedMood, moodTracks, allShelfTracks, state.likedTracks, state.recentTracks, state.tasteDna.currentTimeBucket) {
        if (selectedMood != MusicMoodCategory.FOR_YOU) {
            listOfNotNull(
                moodTracks.takeIf { it.isNotEmpty() }?.let {
                    SongCategoryBlock(
                        id = "mood_${selectedMood.name}",
                        title = "${selectedMood.label} Cuts",
                        subtitle = "Pure studio tracks tuned for ${selectedMood.label.lowercase()}",
                        songs = it,
                    )
                }
            )
        } else {
            buildList {
                if (state.recentTracks.isNotEmpty()) {
                    add(
                        SongCategoryBlock(
                            id = "cat_repeat",
                            title = "On Repeat · Jump Back In",
                            subtitle = "Songs you keep spinning",
                            songs = state.recentTracks.distinctBy { it.id }.take(8),
                        )
                    )
                }
                if (state.likedTracks.isNotEmpty()) {
                    add(
                        SongCategoryBlock(
                            id = "cat_liked",
                            title = "Liked Songs",
                            subtitle = "${state.likedTracks.size} saved favorites",
                            songs = state.likedTracks.take(10),
                        )
                    )
                }
                // Group AI shelf tracks by artist/vibe chunks
                val fresh = allShelfTracks.filterNot { t -> state.recentTracks.any { it.id == t.id } }
                if (fresh.isNotEmpty()) {
                    add(
                        SongCategoryBlock(
                            id = "cat_vibe",
                            title = "${state.tasteDna.currentTimeBucket} Vibe Picks",
                            subtitle = "Hand-picked songs matching your current energy",
                            songs = fresh.take(10),
                        )
                    )
                }
                if (fresh.size > 10) {
                    add(
                        SongCategoryBlock(
                            id = "cat_deep",
                            title = "Deep Cuts & Fresh Drops",
                            subtitle = "Expand your rotation",
                            songs = fresh.drop(10).take(12),
                        )
                    )
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 186.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        // 1. Mood / Category Filter Strip
        item(key = "mood_chips") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(MusicMoodCategory.entries) { mood ->
                    val active = selectedMood == mood
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (active) palette.accent.copy(alpha = 0.22f) else palette.card)
                            .border(
                                width = if (active) 1.3.dp else 0.8.dp,
                                color = if (active) palette.accent else palette.glassBorder,
                                shape = RoundedCornerShape(10.dp),
                            )
                            .clickable { onSelectMood(mood) }
                            .padding(horizontal = 13.dp, vertical = 7.dp),
                    ) {
                        Text(
                            text = mood.label,
                            color = if (active) palette.accent else palette.textPrimary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        if (state.isLoadingMusicMood) {
            item(key = "mood_loading") {
                Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = palette.accent, strokeWidth = 2.5.dp)
                }
            }
        } else {
            // 2. Spotlight Instant-Play Hero Song Card
            if (spotlightTrack != null) {
                item(key = "spotlight_song") {
                    val isCurrent = state.currentTrack?.id == spotlightTrack.id
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(palette.card, palette.accent.copy(alpha = 0.22f))
                                )
                            )
                            .border(1.dp, palette.accent.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                            .clickable { onPlayTrack(spotlightTrack, quickPlayPool) }
                            .padding(14.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = spotlightTrack.thumbnailUrl,
                                contentDescription = spotlightTrack.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(66.dp)
                                    .shadow(12.dp, RoundedCornerShape(12.dp))
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(palette.surface),
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (selectedMood == MusicMoodCategory.FOR_YOU) "SPOTLIGHT TRACK" else "${selectedMood.label.uppercase()} PICK",
                                    color = palette.accent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = spotlightTrack.title,
                                    color = palette.textPrimary,
                                    fontSize = 16.5.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = spotlightTrack.artist,
                                    color = palette.textMuted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .shadow(10.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(palette.accent),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isCurrent && state.isPlaying) {
                                    SpotifyEqualizerBars(isPlaying = true, barColor = Color.Black)
                                } else {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color.Black, modifier = Modifier.size(28.dp))
                                }
                            }
                        }
                    }
                }
            }

            // 3. Swipeable 4-Songs-Per-Column "Quick Play Songs" Horizontal Strip
            if (quickPlayPool.size >= 4) {
                item(key = "quick_song_pager") {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text("Quick Play Songs", color = palette.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.4).sp)
                                Text("Swipe horizontally for instant tracks", color = palette.textMuted, fontSize = 12.sp)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(palette.card)
                                    .border(1.dp, palette.glassBorder, CircleShape)
                                    .clickable { onPlayTrack(quickPlayPool.first(), quickPlayPool) }
                                    .padding(horizontal = 12.dp, vertical = 5.dp),
                            ) {
                                Text("Play all", color = palette.accent, fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        val columns = quickPlayPool.take(20).chunked(4)
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            items(columns.size) { colIdx ->
                                val colSongs = columns[colIdx]
                                Column(
                                    modifier = Modifier.width(308.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    colSongs.forEach { song ->
                                        PureSongRow(
                                            track = song,
                                            isCurrent = state.currentTrack?.id == song.id,
                                            isPlaying = state.isPlaying,
                                            isLiked = state.likedTracks.any { it.id == song.id },
                                            onPlay = { onPlayTrack(song, quickPlayPool) },
                                            onToggleLike = { onToggleLikeTrack(song) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Categorized Vertical Song Sections (100% Playable Songs!)
            items(categoryBlocks, key = { it.id }) { block ->
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(block.title, color = palette.textPrimary, fontSize = 19.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.4).sp)
                            Text(block.subtitle, color = palette.textMuted, fontSize = 12.sp)
                        }
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(palette.accent.copy(alpha = 0.16f))
                                .border(1.dp, palette.accent.copy(alpha = 0.4f), CircleShape)
                                .clickable {
                                    block.songs.firstOrNull()?.let { onPlayTrack(it, block.songs) }
                                }
                                .padding(horizontal = 12.dp, vertical = 5.dp),
                        ) {
                            Text("Play all", color = palette.accent, fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        block.songs.forEach { song ->
                            PureSongRow(
                                track = song,
                                isCurrent = state.currentTrack?.id == song.id,
                                isPlaying = state.isPlaying,
                                isLiked = state.likedTracks.any { it.id == song.id },
                                onPlay = { onPlayTrack(song, block.songs) },
                                onToggleLike = { onToggleLikeTrack(song) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PureSongRow(
    track: TrackItem,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isLiked: Boolean,
    onPlay: () -> Unit,
    onToggleLike: () -> Unit,
) {
    val palette = SovereignTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isCurrent) palette.accent.copy(alpha = 0.14f) else palette.surface.copy(alpha = 0.55f))
            .border(
                width = 0.8.dp,
                color = if (isCurrent) palette.accent.copy(alpha = 0.42f) else palette.glassBorder,
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onPlay)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = track.thumbnailUrl,
            contentDescription = track.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(7.dp)).background(palette.card),
        )
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isCurrent) {
                    SpotifyEqualizerBars(isPlaying = isPlaying, barColor = palette.accent)
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = track.title,
                    color = if (isCurrent) palette.accent else palette.textPrimary,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (track.explicit) {
                    ExplicitBadge()
                    Spacer(Modifier.width(5.dp))
                }
                val durStr = if (track.durationSec > 0) " · ${track.durationSec / 60}:${"%02d".format(track.durationSec % 60)}" else ""
                Text(
                    text = "${track.artist}$durStr",
                    color = palette.textMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onToggleLike, modifier = Modifier.size(34.dp)) {
            Icon(
                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = "Like",
                tint = if (isLiked) palette.accent else palette.textMuted,
                modifier = Modifier.size(19.dp),
            )
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
                color = if (isActive) palette.accent else palette.textPrimary,
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
                color = palette.textPrimary,
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
                        Text(item.title, color = palette.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
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
                        Text(item.title, color = palette.textPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
