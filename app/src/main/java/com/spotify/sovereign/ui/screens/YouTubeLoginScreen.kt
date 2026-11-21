package com.spotify.sovereign.ui.screens

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.spotify.sovereign.ui.theme.SovereignTheme

private const val YT_MUSIC_LOGIN_URL =
    "https://accounts.google.com/ServiceLogin?service=youtube&uilel=3&passive=true&continue=https%3A%2F%2Fmusic.youtube.com%2F"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeLoginScreen(
    onClose: () -> Unit,
    onCookiesCaptured: (String) -> Unit,
) {
    val palette = SovereignTheme.colors
    var loadProgress by remember { mutableIntStateOf(15) }
    var captured by remember { mutableStateOf(false) }

    fun checkAndCaptureCookies(url: String?) {
        if (captured || url.isNullOrBlank()) return
        if (!url.contains("music.youtube.com") && !url.contains("youtube.com")) return
        val cm = CookieManager.getInstance()
        cm.flush()
        val ytMusicCookies = cm.getCookie("https://music.youtube.com").orEmpty()
        val ytRootCookies = cm.getCookie("https://www.youtube.com").orEmpty()
        val combined = listOf(ytMusicCookies, ytRootCookies)
            .filter { it.isNotBlank() }
            .joinToString("; ")
        if (combined.contains("SAPISID=") || combined.contains("__Secure-3PAPISID=")) {
            captured = true
            onCookiesCaptured(ytMusicCookies.ifBlank { combined })
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.surface)
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose, modifier = Modifier.size(38.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close", tint = palette.textPrimary)
            }
            Spacer(Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Sign in with YouTube Music", color = palette.textPrimary, fontSize = 16.5.sp, fontWeight = FontWeight.ExtraBold)
                Text("Sync Liked Songs, Playlists & Avatar", color = palette.accent, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        if (loadProgress in 1..99) {
            LinearProgressIndicator(
                progress = { loadProgress / 100f },
                color = palette.accent,
                trackColor = palette.card,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        AndroidView(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            factory = { ctx ->
                val cm = CookieManager.getInstance()
                cm.setAcceptCookie(true)
                WebView(ctx).apply {
                    cm.setAcceptThirdPartyCookies(this, true)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    // Clean modern mobile Chrome UA so Google OAuth allows sign-in smoothly
                    settings.userAgentString =
                        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            loadProgress = newProgress
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            checkAndCaptureCookies(request?.url?.toString())
                            return false
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            checkAndCaptureCookies(url)
                        }
                    }
                    loadUrl(YT_MUSIC_LOGIN_URL)
                }
            },
        )
    }
}
