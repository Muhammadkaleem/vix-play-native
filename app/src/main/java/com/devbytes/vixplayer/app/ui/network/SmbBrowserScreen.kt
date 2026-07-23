package com.devbytes.vixplayer.app.ui.network

import android.net.Uri
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

/** Placeholder until SMB browsing lands (Roadmap Step 6 — Reach). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmbBrowserScreen(
    initialPath: String? = null,
    onFileClick: (Uri) -> Unit = {},
    onBack: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SMB Browser") },
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
            iconRes = R.drawable.ic_folder,
            title = "Network folders coming soon",
            body = "Browse and play video from SMB shares on your local network. Arriving with network support.",
            modifier = Modifier.padding(padding),
        )
    }
}
