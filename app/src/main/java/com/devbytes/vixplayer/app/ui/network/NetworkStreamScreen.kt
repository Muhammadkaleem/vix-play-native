package com.devbytes.vixplayer.app.ui.network

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.data.db.entity.NetworkStream

/** Schemes DefaultMediaSourceFactory can open once the HLS/DASH/RTSP modules are present. */
private val SUPPORTED_SCHEMES = setOf("http", "https", "rtsp")

/**
 * "Stream by URL": paste a link, validate its scheme, hand it to the shared player, and
 * keep a recents list. Playback, buffering and error handling all live in the player —
 * this screen only produces a validated [Uri] and records it.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NetworkStreamScreen(
    onPlayUrl: (Uri) -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: NetworkStreamViewModel = hiltViewModel(),
) {
    val recents by viewModel.recents.collectAsState()
    val clipboard = LocalClipboardManager.current

    var url by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun play(candidate: String) {
        val trimmed = candidate.trim()
        val scheme = runCatching { Uri.parse(trimmed).scheme?.lowercase() }.getOrNull()
        if (scheme == null || scheme !in SUPPORTED_SCHEMES) {
            error = "Enter a valid http, https, or rtsp URL."
            return
        }
        error = null
        viewModel.onPlayed(trimmed)
        onPlayUrl(Uri.parse(trimmed))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stream by URL") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            OutlinedTextField(
                value = url,
                onValueChange = {
                    url = it
                    error = null
                },
                label = { Text("Video URL") },
                placeholder = { Text("https://…  ·  .m3u8  ·  .mpd  ·  rtsp://") },
                singleLine = true,
                isError = error != null,
                supportingText = error?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go,
                ),
                keyboardActions = KeyboardActions(
                    onGo = { if (url.isNotBlank()) play(url) },
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.size(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = {
                    clipboard.getText()?.text?.let {
                        url = it
                        error = null
                    }
                }) {
                    Text("Paste")
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { play(url) },
                    enabled = url.isNotBlank(),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_play),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Play")
                }
            }

            if (recents.isNotEmpty()) {
                Spacer(Modifier.size(16.dp))
                Text(
                    text = "Recent",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(recents, key = { it.url }) { stream ->
                        RecentRow(
                            stream = stream,
                            onPlay = { play(stream.url) },
                            onRemove = { viewModel.remove(stream) },
                        )
                    }
                }
            }
        }
    }
}

/** Tap to replay, long-press or the trailing ✕ to remove (the grill locked long-press;
 *  the visible ✕ keeps it discoverable). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun RecentRow(
    stream: NetworkStream,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
) {
    ListItem(
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_nav_network),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        headlineContent = { Text(stream.label, maxLines = 1) },
        supportingContent = {
            Text(
                text = stream.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        },
        trailingContent = {
            IconButton(onClick = onRemove) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
        modifier = Modifier.combinedClickable(
            onClick = onPlay,
            onLongClick = onRemove,
        ),
    )
}
