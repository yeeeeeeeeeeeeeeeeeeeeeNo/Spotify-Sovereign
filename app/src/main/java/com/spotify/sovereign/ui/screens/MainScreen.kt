package com.spotify.sovereign.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SpatialAudio
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.spotify.sovereign.engine.CastTvDevice
import com.spotify.sovereign.model.AppTab
import com.spotify.sovereign.ui.SovereignUiState
import com.spotify.sovereign.ui.SpotifyViewModel
import com.spotify.sovereign.ui.components.AudioFxBottomSheet
import com.spotify.sovereign.ui.theme.SovereignTheme
import com.spotify.sovereign.ui.theme.SpotifyLibraryIcon
import com.spotify.sovereign.ui.theme.SpotifyLogoMark

@Composable
fun MainScreen(viewModel: SpotifyViewModel) {
    val state by viewModel.uiState.collectAsState()
    val palette = SovereignTheme.colors
    var showImportDialog by remember { mutableStateOf(false) }
    var importUrl by remember { mutableStateOf("") }

    // First-Launch Smart Pick-10 Taste AI Onboarding Gate
    if (state.showOnboarding) {
        OnboardingScreen(
            state = state,
            onSearchChange = viewModel::onOnboardingSearchChange,
            onTogglePick = viewModel::toggleOnboardingPick,
            onFinish = viewModel::finishOnboarding,
            onImportPlaylistUrl = { url -> viewModel.importPublicSpotifyPlaylist(url, fromOnboarding = true) },
        )
        return
    }

    BackHandler(
        enabled = state.castSheetOpen || state.ytLoginOpen || state.soundFxSheetOpen || state.settingsOpen || state.fullLyricsOpen ||
            state.queueSheetOpen || state.nowPlayingOpen || state.profileDrawerOpen ||
            state.detailPage != null || state.activeTab != AppTab.HOME
    ) {
        when {
            state.castSheetOpen -> viewModel.closeCastSheet()
            state.ytLoginOpen -> viewModel.closeYouTubeLogin()
            state.soundFxSheetOpen -> viewModel.setSoundFxSheet(false)
            state.settingsOpen -> viewModel.closeSettings()
            state.fullLyricsOpen -> viewModel.closeFullLyrics()
            state.queueSheetOpen -> viewModel.setQueueSheet(false)
            state.nowPlayingOpen -> viewModel.closeNowPlaying()
            state.profileDrawerOpen -> viewModel.setProfileDrawer(false)
            state.detailPage != null -> viewModel.closeDetail()
            state.activeTab != AppTab.HOME -> viewModel.selectTab(AppTab.HOME)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(palette.backgroundDeep)) {
        val detail = state.detailPage
        if (detail != null) {
            DetailScreen(
                page = detail,
                state = state,
                onBack = viewModel::closeDetail,
                onToggleSave = viewModel::toggleSaveDetail,
                onToggleShuffle = viewModel::toggleShuffle,
                onPlayTrack = { t, q -> viewModel.playTrack(t, q) },
                onTogglePlayPause = viewModel::togglePlayPause,
                onToggleLikeTrack = viewModel::toggleLikeTrack,
            )
        } else {
            when (state.activeTab) {
                AppTab.HOME -> HomeScreen(
                    state = state,
                    onOpenProfile = { viewModel.setProfileDrawer(true) },
                    onOpenSettings = viewModel::openSettings,
                    onOpenCast = viewModel::openCastSheet,
                    onSelectFilter = viewModel::setHomeFilter,
                    onSelectMusicMood = viewModel::selectMusicMood,
                    onClickCollection = viewModel::openCollection,
                    onPlayTrack = { t, q -> viewModel.playTrack(t, q) },
                    onToggleLikeTrack = viewModel::toggleLikeTrack,
                )
                AppTab.SEARCH -> SearchScreen(
                    state = state,
                    onOpenProfile = { viewModel.setProfileDrawer(true) },
                    onQueryChange = viewModel::onSearchQueryChange,
                    onSubmitSearch = viewModel::executeSearch,
                    onClearSearch = viewModel::clearSearch,
                    onPlayTrack = { t, q -> viewModel.playTrack(t, q) },
                    onClickCollection = viewModel::openCollection,
                )
                AppTab.LIBRARY -> LibraryScreen(
                    state = state,
                    onOpenProfile = { viewModel.setProfileDrawer(true) },
                    onOpenSearch = { viewModel.selectTab(AppTab.SEARCH) },
                    onSelectFilter = viewModel::setLibraryFilter,
                    onToggleSort = viewModel::toggleLibrarySort,
                    onToggleGrid = viewModel::toggleLibraryGrid,
                    onClickCollection = viewModel::openCollection,
                    onPlayTrack = { t, q -> viewModel.playTrack(t, q) },
                )
            }
        }

        // Ultra-Modern Frosted Glass Floating Mini-Player + Glass Bottom Navigation
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.38f to palette.backgroundDeep.copy(alpha = 0.84f),
                        1.0f to palette.backgroundDeep,
                    )
                )
                .padding(top = 22.dp, bottom = 6.dp)
                .navigationBarsPadding(),
        ) {
            state.currentTrack?.let {
                SwipeableGlassMiniPlayer(
                    state = state,
                    onOpenPlayer = viewModel::openNowPlaying,
                    onTogglePlayPause = viewModel::togglePlayPause,
                    onSkipNext = viewModel::skipNext,
                    onSkipPrev = viewModel::skipPrevious,
                )
                Spacer(Modifier.height(8.dp))
            }

            // Floating Frosted Glass Pill Nav Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .height(60.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(palette.glassSurface)
                    .border(0.9.dp, palette.glassBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassNavTab("Home", state.activeTab == AppTab.HOME && state.detailPage == null, { viewModel.selectTab(AppTab.HOME) }, Modifier.weight(1f)) { sel, tint ->
                    Icon(if (sel) Icons.Filled.Home else Icons.Outlined.Home, contentDescription = "Home", tint = tint, modifier = Modifier.size(24.dp))
                }
                GlassNavTab("Search", state.activeTab == AppTab.SEARCH && state.detailPage == null, { viewModel.selectTab(AppTab.SEARCH) }, Modifier.weight(1f)) { _, tint ->
                    Icon(Icons.Filled.Search, contentDescription = "Search", tint = tint, modifier = Modifier.size(24.dp))
                }
                GlassNavTab("Your Library", state.activeTab == AppTab.LIBRARY && state.detailPage == null, { viewModel.selectTab(AppTab.LIBRARY) }, Modifier.weight(1f)) { sel, tint ->
                    SpotifyLibraryIcon(selected = sel, tint = tint)
                }
            }
        }

        // Full-Screen Next-Gen Now Playing Sheet
        AnimatedVisibility(
            visible = state.nowPlayingOpen && state.currentTrack != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
        ) {
            NowPlayingSheet(
                state = state,
                onClose = viewModel::closeNowPlaying,
                onTogglePlayPause = viewModel::togglePlayPause,
                onSeekTo = viewModel::seekTo,
                onSkipNext = viewModel::skipNext,
                onSkipPrevious = viewModel::skipPrevious,
                onToggleShuffle = viewModel::toggleShuffle,
                onCycleRepeat = viewModel::cycleRepeat,
                onToggleLike = viewModel::toggleLikeTrack,
                onOpenQueue = { viewModel.setQueueSheet(true) },
                onOpenFullLyrics = viewModel::openFullLyrics,
                onOpenSoundFx = { viewModel.setSoundFxSheet(true) },
                onOpenCast = viewModel::openCastSheet,
                onCycleArtworkStyle = viewModel::cycleArtworkStyle,
            )
        }

        // Cinema-Grade Full-Screen Synced Lyrics Overlay
        AnimatedVisibility(
            visible = state.fullLyricsOpen && state.currentTrack != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
        ) {
            FullScreenLyricsView(
                state = state,
                onClose = viewModel::closeFullLyrics,
                onSeekTo = viewModel::seekTo,
                onTogglePlayPause = viewModel::togglePlayPause,
                onSkipNext = viewModel::skipNext,
                onSkipPrevious = viewModel::skipPrevious,
                onAdjustOffset = viewModel::adjustLyricsOffset,
                onResetOffset = viewModel::resetLyricsOffset,
                onOpenSoundFx = { viewModel.setSoundFxSheet(true) },
            )
        }

        // Full-Featured Settings, Account Sync, Dolby Atmos EQ & Theme Studio Screen
        AnimatedVisibility(
            visible = state.settingsOpen,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
        ) {
            SettingsScreen(
                state = state,
                onBack = viewModel::closeSettings,
                onOpenYouTubeLogin = viewModel::openYouTubeLogin,
                onSyncYouTubeAccount = { viewModel.syncYouTubeAccountNow(true) },
                onSignOutYouTubeAccount = viewModel::signOutYouTubeAccount,
                onUpdateAudioFx = viewModel::updateAudioFx,
                onSelectSoundProfile = viewModel::selectSoundProfile,
                onUpdateEqBand = viewModel::updateCustomEqBand,
                onResetCustomEq = viewModel::resetCustomEq,
                onUpdateVisual = viewModel::updateVisualSettings,
                onToggleUltraHighAudio = viewModel::toggleUltraHighAudio,
                onClearAudioCache = viewModel::clearAudioCache,
                onRefreshAiFeed = {
                    viewModel.closeSettings()
                    viewModel.refreshPersonalizedHome()
                    viewModel.showToast("Recalculating AI Taste shelves...")
                },
                onRetrainAi = {
                    viewModel.closeSettings()
                    viewModel.reopenOnboardingToRetrain()
                },
                onOpenImport = {
                    viewModel.closeSettings()
                    showImportDialog = true
                },
            )
        }

        // Built-In YouTube Music Account WebView Sign-In Screen
        AnimatedVisibility(
            visible = state.ytLoginOpen,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
        ) {
            YouTubeLoginScreen(
                onClose = viewModel::closeYouTubeLogin,
                onCookiesCaptured = viewModel::onYouTubeCookiesCaptured,
            )
        }

        // Slide-Out Profile & Taste DNA AI Brain Drawer
        AnimatedVisibility(visible = state.profileDrawerOpen, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.68f)).clickable { viewModel.setProfileDrawer(false) })
        }
        AnimatedVisibility(
            visible = state.profileDrawerOpen,
            enter = slideInHorizontally(initialOffsetX = { -it }),
            exit = slideOutHorizontally(targetOffsetX = { -it }),
        ) {
            TasteDnaProfileDrawer(
                state = state,
                onOpenYouTubeLogin = viewModel::openYouTubeLogin,
                onSyncYouTubeAccount = {
                    viewModel.setProfileDrawer(false)
                    viewModel.syncYouTubeAccountNow(true)
                },
                onOpenSettings = viewModel::openSettings,
                onOpenSoundFx = {
                    viewModel.setProfileDrawer(false)
                    viewModel.setSoundFxSheet(true)
                },
                onRefreshAiFeed = {
                    viewModel.setProfileDrawer(false)
                    viewModel.refreshPersonalizedHome()
                    viewModel.showToast("Recalculating AI Taste shelves...")
                },
                onRetrainAi = viewModel::reopenOnboardingToRetrain,
                onOpenImport = {
                    viewModel.setProfileDrawer(false)
                    showImportDialog = true
                },
            )
        }

        // Up Next Queue Modal Sheet
        AnimatedVisibility(visible = state.queueSheetOpen, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.68f)).clickable { viewModel.setQueueSheet(false) })
        }
        AnimatedVisibility(
            visible = state.queueSheetOpen,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            SovereignQueueSheet(
                state = state,
                onSelectTrack = { t ->
                    viewModel.playTrack(t, state.queue)
                    viewModel.setQueueSheet(false)
                },
            )
        }

        // In-Player Instant Dolby Atmos & Sound Engine Bottom Sheet
        AnimatedVisibility(visible = state.soundFxSheetOpen, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.68f)).clickable { viewModel.setSoundFxSheet(false) })
        }
        AnimatedVisibility(
            visible = state.soundFxSheetOpen,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            AudioFxBottomSheet(
                audioFx = state.settings.audioFx,
                dspStatus = state.dspStatus,
                streamBadge = state.streamBadge,
                onUpdateAudioFx = viewModel::updateAudioFx,
                onSelectProfile = viewModel::selectSoundProfile,
                onOpenFullEqSettings = {
                    viewModel.setSoundFxSheet(false)
                    viewModel.openSettings()
                },
            )
        }

        // Sovereign Cast to Xiaomi TV Modal Sheet
        AnimatedVisibility(visible = state.castSheetOpen, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.68f)).clickable { viewModel.closeCastSheet() })
        }
        AnimatedVisibility(
            visible = state.castSheetOpen,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            SovereignCastSheet(
                state = state,
                onScan = viewModel::scanForTvs,
                onConnectDevice = viewModel::connectCastTv,
                onConnectByIp = viewModel::connectCastByIp,
                onDisconnect = viewModel::disconnectCastTv,
                onVolumeDelta = viewModel::adjustCastVolume,
            )
        }

        // Floating White Spotify Toast
        AnimatedVisibility(
            visible = state.toastMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 148.dp),
        ) {
            Box(
                modifier = Modifier
                    .shadow(16.dp, RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            ) {
                Text(state.toastMessage.orEmpty(), color = Color(0xFF0A0A0C), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            containerColor = palette.surface,
            title = { Text("Import Spotify Playlist", color = Color.White, fontWeight = FontWeight.ExtraBold) },
            text = {
                Column {
                    Text("Paste any public Spotify playlist URL to pull studio tracks and train your Taste AI.", color = palette.textMuted, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = importUrl,
                        onValueChange = { importUrl = it },
                        singleLine = true,
                        placeholder = { Text("https://open.spotify.com/playlist/...", fontSize = 12.5.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = palette.accent,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                        ),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showImportDialog = false; viewModel.importPublicSpotifyPlaylist(importUrl, false) },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = Color.Black),
                ) { Text("Import", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("Cancel", color = palette.textMuted) }
            },
        )
    }
}

@Composable
private fun SwipeableGlassMiniPlayer(
    state: SovereignUiState,
    onOpenPlayer: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrev: () -> Unit,
) {
    val track = state.currentTrack ?: return
    val palette = SovereignTheme.colors
    val progress = if (state.durationMs > 0L) (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    var dragAccum by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .height(62.dp)
            .shadow(18.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(state.miniPlayerColor.copy(alpha = 0.96f), state.dominantColor.copy(alpha = 0.86f))
                )
            )
            .border(0.9.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
            .pointerInput(track.id) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAccum = 0f },
                    onHorizontalDrag = { _, amt -> dragAccum += amt },
                    onDragEnd = {
                        if (dragAccum < -90f) onSkipNext()
                        else if (dragAccum > 90f) onSkipPrev()
                        dragAccum = 0f
                    },
                )
            }
            .clickable(onClick = onOpenPlayer),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(start = 10.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = track.thumbnailUrl,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF1C1C22)),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(track.title, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, color = Color.White.copy(alpha = 0.78f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onSkipNext, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color.White.copy(alpha = 0.88f), modifier = Modifier.size(22.dp))
            }
            IconButton(onClick = onTogglePlayPause, modifier = Modifier.size(38.dp)) {
                if (state.isBuffering) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Icon(if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = "Play/Pause", tint = Color.White, modifier = Modifier.size(26.dp))
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .height(2.6.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.22f)),
        ) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(progress).background(palette.accent))
        }
    }
}

@Composable
private fun GlassNavTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    icon: @Composable (Boolean, Color) -> Unit,
) {
    val palette = SovereignTheme.colors
    val tint = if (selected) palette.accent else palette.textMuted
    Column(
        modifier = modifier.fillMaxHeight().clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        icon(selected, tint)
        Spacer(Modifier.height(3.dp))
        Text(label, color = tint, fontSize = 10.5.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun TasteDnaProfileDrawer(
    state: SovereignUiState,
    onOpenYouTubeLogin: () -> Unit,
    onSyncYouTubeAccount: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSoundFx: () -> Unit,
    onRefreshAiFeed: () -> Unit,
    onRetrainAi: () -> Unit,
    onOpenImport: () -> Unit,
) {
    val palette = SovereignTheme.colors
    val yt = state.ytAccount
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.84f)
            .background(palette.surface)
            .border(1.dp, palette.glassBorder)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (yt.signedIn && yt.avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = yt.avatarUrl,
                    contentDescription = yt.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(44.dp).clip(CircleShape).border(1.5.dp, palette.accent, CircleShape),
                )
            } else {
                SpotifyLogoMark(size = 42.dp, circleColor = palette.accent)
            }
            Column {
                Text(
                    text = if (yt.signedIn) yt.name.ifBlank { "YouTube Music" } else "Spotify Sovereign",
                    color = palette.textPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (yt.signedIn) yt.handle.ifBlank { "YT Music 2-Way Sync" } else "v4.0 · Atmos & Theme Studio",
                    color = palette.accent,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = palette.glassBorder)
        Spacer(Modifier.height(14.dp))

        // Live Taste DNA Telemetry Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(palette.card)
                .border(1.dp, palette.glassBorder, RoundedCornerShape(14.dp))
                .padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = palette.accent, modifier = Modifier.size(16.dp))
                Text("YOUR TASTE DNA (LIVE)", color = palette.accent, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Vibe: ${state.tasteDna.currentTimeBucket}  ·  Plays: ${state.tasteDna.totalPlays}  ·  Skips Buried: ${state.tasteDna.totalSkipsBlocked}",
                color = palette.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Theme: ${state.settings.visual.themePreset.title} · Sound: ${state.settings.audioFx.profile.shortBadge}",
                color = palette.textMuted,
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (state.tasteDna.topArtists.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                state.tasteDna.topArtists.take(4).forEach { a ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(a.name, color = palette.textPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("+${String.format(java.util.Locale.US, "%.1f", a.score)}", color = palette.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        if (yt.signedIn) {
            DrawerActionRow(Icons.Filled.Sync, "Sync YouTube Music Library", palette.accent, onSyncYouTubeAccount)
        } else {
            DrawerActionRow(Icons.Filled.AccountCircle, "Sign in with YouTube Music", palette.accent, onOpenYouTubeLogin)
        }
        DrawerActionRow(Icons.Filled.Settings, "Settings, Themes & 10-Band EQ", palette.accent, onOpenSettings)
        DrawerActionRow(Icons.Filled.SpatialAudio, "Dolby Atmos & Sound Profiles", palette.accent, onOpenSoundFx)
        Spacer(Modifier.height(4.dp))
        HorizontalDivider(color = palette.glassBorder)
        Spacer(Modifier.height(4.dp))
        DrawerActionRow(Icons.Filled.Refresh, "Refresh AI Recommendations", palette.textPrimary, onRefreshAiFeed)
        DrawerActionRow(Icons.Filled.Link, "Import Spotify Playlist Link", palette.textPrimary, onOpenImport)
        DrawerActionRow(Icons.Filled.AutoAwesome, "Retrain AI (Pick 10 Artists)", palette.textPrimary, onRetrainAi)

        Spacer(Modifier.weight(1f))
        HorizontalDivider(color = palette.glassBorder)
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Spotify Sovereign v4.0 · YouTube Music 2-Way Sync · 32-bit Atmos DSP · Manual Color Studio",
            color = palette.textMuted,
            fontSize = 11.sp,
            lineHeight = 15.sp,
        )
    }
}

@Composable
private fun DrawerActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
) {
    val palette = SovereignTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Text(label, color = palette.textPrimary, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SovereignQueueSheet(
    state: SovereignUiState,
    onSelectTrack: (com.spotify.sovereign.model.TrackItem) -> Unit,
) {
    val palette = SovereignTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.68f)
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(palette.surface)
            .border(1.dp, palette.glassBorder, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Box(Modifier.align(Alignment.CenterHorizontally).width(40.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f)))
        Spacer(Modifier.height(14.dp))
        Text("Smart AI Queue", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(state.queue, key = { "sq_${it.id}" }) { item ->
                val isCurrent = item.id == state.currentTrack?.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isCurrent) palette.accent.copy(alpha = 0.14f) else Color.Transparent)
                        .clickable { onSelectTrack(item) }
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(model = item.thumbnailUrl, contentDescription = item.title, contentScale = ContentScale.Crop, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title, color = if (isCurrent) palette.accent else Color.White, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(item.artist, color = palette.textMuted, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun SovereignCastSheet(
    state: SovereignUiState,
    onScan: () -> Unit,
    onConnectDevice: (CastTvDevice) -> Unit,
    onConnectByIp: (String) -> Unit,
    onDisconnect: () -> Unit,
    onVolumeDelta: (Int) -> Unit,
) {
    val palette = SovereignTheme.colors
    var manualIp by remember { mutableStateOf(state.activeCastTv?.ip ?: "192.168.1.8") }
    val activeTv = state.activeCastTv

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .background(palette.surface)
            .border(1.dp, palette.glassBorder, RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .width(40.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.28f))
        )
        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (activeTv != null) Icons.Filled.CastConnected else Icons.Filled.Cast,
                contentDescription = null,
                tint = if (activeTv != null) palette.accent else Color.White,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Sovereign Cast", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    text = if (activeTv != null) "Connected to ${activeTv.name}" else "Cinema cover, synced lyrics & Dolby Audio on TV",
                    color = if (activeTv != null) palette.accent else palette.textMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (state.isScanningTvs) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = palette.accent,
                    strokeWidth = 2.dp,
                )
            } else {
                IconButton(onClick = onScan, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Rescan TVs", tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(20.dp))
                }
            }
        }

        if (activeTv != null) {
            Spacer(Modifier.height(14.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.accent.copy(alpha = 0.12f))
                    .border(1.dp, palette.accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .padding(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Tv, contentDescription = null, tint = palette.accent, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(activeTv.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${activeTv.ip}:${activeTv.port} · Live TV Remote Active", color = palette.textMuted, fontSize = 11.5.sp)
                    }
                    TextButton(onClick = onDisconnect) {
                        Text("Disconnect", color = Color(0xFFFF6B6B), fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("TV Volume", color = Color.White.copy(alpha = 0.8f), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable { onVolumeDelta(-1) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.VolumeDown, contentDescription = "Volume Down", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable { onVolumeDelta(1) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.VolumeUp, contentDescription = "Volume Up", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Text("Discovered TVs on Wi-Fi", color = palette.textMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        if (state.discoveredTvs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .padding(horizontal = 14.dp, vertical = 14.dp),
            ) {
                Text(
                    text = if (state.isScanningTvs) "Scanning local network for Mono TV..." else "No TVs auto-detected yet. Tap refresh or connect by IP below.",
                    color = palette.textMuted,
                    fontSize = 12.5.sp,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.discoveredTvs.forEach { tv ->
                    val selected = activeTv?.ip == tv.ip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) palette.accent.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.05f))
                            .border(
                                1.dp,
                                if (selected) palette.accent.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.08f),
                                RoundedCornerShape(12.dp),
                            )
                            .clickable { onConnectDevice(tv) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Tv, contentDescription = null, tint = if (selected) palette.accent else Color.White, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tv.name, color = Color.White, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${tv.ip}:${tv.port}", color = palette.textMuted, fontSize = 12.sp)
                        }
                        Text(
                            text = if (selected) "Active" else "Cast",
                            color = if (selected) palette.accent else Color.White.copy(alpha = 0.85f),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
        Spacer(Modifier.height(12.dp))
        Text("Direct TV IP", color = palette.textMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = manualIp,
                onValueChange = { manualIp = it },
                singleLine = true,
                placeholder = { Text("192.168.1.8", fontSize = 13.sp) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = palette.accent,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                ),
            )
            Button(
                onClick = { onConnectByIp(manualIp) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = Color.Black),
                modifier = Modifier.height(52.dp),
            ) {
                Text("Connect", fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp)
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}
