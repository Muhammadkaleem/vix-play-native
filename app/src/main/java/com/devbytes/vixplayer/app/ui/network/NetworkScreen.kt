package com.devbytes.vixplayer.app.ui.network

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

/** Network tab root. Placeholder until network streaming lands (Roadmap Step 6 — Reach). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScreen(
    onStreamClick: () -> Unit = {},
    onSmbClick: () -> Unit = {},
    onCastClick: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Network") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        LibraryEmptyState(
            iconRes = R.drawable.ic_nav_network,
            title = "Network streaming coming soon",
            body = "Play from URLs, browse SMB shares, and cast to your TV. Arriving in a future update.",
            modifier = Modifier.padding(padding),
        )
    }
}
