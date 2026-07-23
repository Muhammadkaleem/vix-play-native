package com.devbytes.vixplayer.app.ui.audio

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.ui.library.components.LibraryEmptyState

/**
 * Placeholder until Roadmap Step 5 ("Modern shell") lands the audio subsystem —
 * MediaSessionService, audio ViewModel, library, and full player UI ship
 * together. Shown as an honest, token-consistent empty state rather than dead
 * transport chrome (see "no dead UI" scope bar). The route + mediaStoreId are
 * already wired so Step 5 can swap real content in with no navigation change.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerScreen(
    mediaStoreId: Long,
    onBack: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Now Playing") },
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
            iconRes = R.drawable.ic_nav_audio,
            title = "Audio player coming soon",
            body = "Background audio playback, queue, and equalizer arrive in a future update.",
            modifier = Modifier.padding(padding),
        )
    }
}
