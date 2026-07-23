package com.devbytes.vixplayer.app.ui.audio

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.ui.library.components.LibraryEmptyState

/** Audio tab root. Placeholder until the audio library lands (Roadmap Step 5 — Modern shell). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioLibraryScreen(
    onAudioPlayerClick: (Long) -> Unit = {},
    onPlaylistsClick: () -> Unit = {},
    onEqualizerClick: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audio") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        LibraryEmptyState(
            iconRes = R.drawable.ic_nav_audio,
            title = "Audio library coming soon",
            body = "Browse your music, build playlists, and tune playback with the equalizer. Arriving in a future update.",
            modifier = Modifier.padding(padding),
        )
    }
}
