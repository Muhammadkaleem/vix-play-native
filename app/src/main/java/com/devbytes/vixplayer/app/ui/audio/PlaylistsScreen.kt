package com.devbytes.vixplayer.app.ui.audio

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.ui.library.components.LibraryEmptyState

/** Placeholder until playlists land (Roadmap Step 7 — Polish). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    onBack: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Playlists") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        LibraryEmptyState(
            iconRes = R.drawable.ic_playlist,
            title = "Playlists coming soon",
            body = "Group tracks into playlists and queue them up for continuous playback. Arriving in a future update.",
            modifier = Modifier.padding(padding),
        )
    }
}
