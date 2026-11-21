package com.spotify.sovereign.ui.screens

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.spotify.sovereign.model.OnboardingCandidate
import com.spotify.sovereign.ui.SovereignUiState
import com.spotify.sovereign.ui.theme.SpotifyColors
import com.spotify.sovereign.ui.theme.SpotifyLogoMark

@Composable
fun OnboardingScreen(
    state: SovereignUiState,
    onSearchChange: (String) -> Unit,
    onTogglePick: (OnboardingCandidate) -> Unit,
    onFinish: () -> Unit,
    onImportPlaylistUrl: (String) -> Unit,
) {
    var showImportDialog by remember { mutableStateOf(false) }
    var playlistUrl by remember { mutableStateOf("") }
    val count = state.onboardingSelected.size

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A2414), Color(0xFF050706), Color(0xFF000000))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SpotifyLogoMark(size = 30.dp)
                    Column {
                        Text(
                            text = "Train Your Taste AI",
                            color = Color.White,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp,
                        )
                        Text(
                            text = "Pick up to 10 artists or songs (${count}/10)",
                            color = SpotifyColors.Green,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                // 1-Tap Public Spotify Playlist Importer
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SpotifyColors.GlassSurface)
                        .border(1.dp, SpotifyColors.GlassBorder, CircleShape)
                        .clickable { showImportDialog = true }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(Icons.Filled.Link, contentDescription = null, tint = SpotifyColors.Green, modifier = Modifier.size(15.dp))
                        Text("Import", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Live Artist & Song Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF121212), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (state.onboardingSearchQuery.isEmpty()) {
                            Text("Search any artist or song to add...", color = Color(0xFF555555), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        BasicTextField(
                            value = state.onboardingSearchQuery,
                            onValueChange = onSearchChange,
                            singleLine = true,
                            cursorBrush = SolidColor(Color(0xFF121212)),
                            textStyle = TextStyle(color = Color(0xFF121212), fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (state.onboardingSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear", tint = Color(0xFF121212), modifier = Modifier.size(17.dp))
                        }
                    }
                }
            }

            // Selected Taste Chips Bar
            if (state.onboardingSelected.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.onboardingSelected, key = { "sel_${it.id}" }) { sel ->
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(SpotifyColors.Green.copy(alpha = 0.16f))
                                .border(1.dp, SpotifyColors.Green.copy(alpha = 0.55f), CircleShape)
                                .clickable { onTogglePick(sel) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(sel.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Icon(Icons.Filled.Close, contentDescription = null, tint = SpotifyColors.Green, modifier = Modifier.size(13.dp))
                        }
                    }
                }
            } else {
                Spacer(Modifier.height(8.dp))
            }

            // Search Results OR Dynamic Expanding 3-Column Artist Bubble Grid
            if (state.onboardingSearchQuery.isNotBlank()) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 110.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.onboardingSearchResults, key = { "osr_${it.id}" }) { cand ->
                        val picked = state.onboardingSelected.any { it.id == cand.id || it.name.equals(cand.name, ignoreCase = true) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (picked) SpotifyColors.Green.copy(alpha = 0.14f) else SpotifyColors.Card)
                                .border(
                                    1.dp,
                                    if (picked) SpotifyColors.Green.copy(alpha = 0.6f) else SpotifyColors.GlassBorder,
                                    RoundedCornerShape(12.dp),
                                )
                                .clickable { onTogglePick(cand) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AsyncImage(
                                model = cand.imageUrl,
                                contentDescription = cand.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(if (cand.isTrack) RoundedCornerShape(6.dp) else CircleShape)
                                    .background(Color(0xFF282830)),
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(cand.name, color = Color.White, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(cand.subtitle, color = SpotifyColors.TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (picked) SpotifyColors.Green else Color.White.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = if (picked) Color.Black else Color.White.copy(alpha = 0.55f),
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            } else if (state.isLoadingOnboarding && state.onboardingCandidates.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SpotifyColors.Green, strokeWidth = 2.5.dp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 112.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    items(state.onboardingCandidates, key = { it.id }) { cand ->
                        val picked = state.onboardingSelected.any { it.id == cand.id || it.name.equals(cand.name, ignoreCase = true) }
                        val ringColor by animateColorAsState(
                            if (picked) SpotifyColors.Green else Color.White.copy(alpha = 0.14f),
                            label = "ring",
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onTogglePick(cand) },
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                AsyncImage(
                                    model = cand.imageUrl,
                                    contentDescription = cand.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(CircleShape)
                                        .border(if (picked) 3.dp else 1.dp, ringColor, CircleShape)
                                        .background(Color(0xFF1F1F26)),
                                )
                                if (picked) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(SpotifyColors.Green)
                                            .border(2.dp, Color.Black, CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.height(7.dp))
                            Text(
                                text = cand.name,
                                color = if (picked) SpotifyColors.Green else Color.White,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }

        // Bottom Frosted Launch CTA Bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.92f), Color.Black))
                )
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Button(
                onClick = onFinish,
                enabled = count >= 3 && !state.isImportingPlaylist,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpotifyColors.Green,
                    contentColor = Color.Black,
                    disabledContainerColor = Color(0xFF282830),
                    disabledContentColor = SpotifyColors.TextMuted,
                ),
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (state.isImportingPlaylist) {
                    CircularProgressIndicator(color = Color.Black, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Importing playlist & training AI...", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                } else {
                    Text(
                        text = if (count < 3) "Pick at least ${3 - count} more to launch" else "Build My Personalized Feed ($count picked)",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                    )
                }
            }
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            containerColor = Color(0xFF1A1A20),
            title = { Text("Import Spotify Playlist", color = Color.White, fontWeight = FontWeight.ExtraBold) },
            text = {
                Column {
                    Text(
                        "Paste any public Spotify playlist URL. Zero login required — we'll resolve studio tracks and train your AI brain instantly.",
                        color = SpotifyColors.TextMuted,
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = playlistUrl,
                        onValueChange = { playlistUrl = it },
                        singleLine = true,
                        placeholder = { Text("https://open.spotify.com/playlist/...", fontSize = 12.5.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SpotifyColors.Green,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                        ),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showImportDialog = false
                        onImportPlaylistUrl(playlistUrl)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SpotifyColors.Green, contentColor = Color.Black),
                ) {
                    Text("Import & Train", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel", color = SpotifyColors.TextMuted)
                }
            },
        )
    }
}
