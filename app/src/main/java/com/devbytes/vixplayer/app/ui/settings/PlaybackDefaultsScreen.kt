package com.devbytes.vixplayer.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.ui.common.screenInsets

/**
 * Playback defaults. Only settings with a real consuming feature appear here — an inert
 * toggle would be dead UI, so decoder/resume/autoplay stay out until they exist.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackDefaultsScreen(
    onBack: () -> Unit = {},
    viewModel: PlaybackDefaultsViewModel = hiltViewModel(),
) {
    val backgroundPlayback by viewModel.backgroundPlayback.collectAsState()
    val autoplayNext by viewModel.autoplayNext.collectAsState()
    val askBeforePlayingNext by viewModel.askBeforePlayingNext.collectAsState()

    Scaffold(
        contentWindowInsets = screenInsets(),
        topBar = {
            TopAppBar(
                title = { Text("Playback Defaults") },
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
        Column(modifier = Modifier.padding(padding)) {
            SectionHeader("Playback & Autoplay")
            ListItem(
                headlineContent = { Text("Autoplay next video") },
                supportingContent = {
                    Text("When one finishes, play the next episode of the show — or the next video in the folder.")
                },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_skip_next),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                trailingContent = {
                    Switch(
                        checked = autoplayNext,
                        onCheckedChange = { viewModel.setAutoplayNext(it) },
                    )
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
            )
            ListItem(
                headlineContent = { Text("Ask before playing next") },
                supportingContent = {
                    Text("When autoplay is off, show a \"Play next?\" prompt at the end of a video.")
                },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_play),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                trailingContent = {
                    Switch(
                        checked = askBeforePlayingNext,
                        onCheckedChange = { viewModel.setAskBeforePlayingNext(it) },
                    )
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
            )
            ListItem(
                modifier = Modifier.clickable { viewModel.resetPlaybackPrompts() },
                headlineContent = { Text("Reset playback prompts") },
                supportingContent = { Text("Restore the \"Play next?\" prompt (clears \"Don't ask again\").") },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_repeat),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
            )

            SectionHeader("Audio")
            ListItem(
                headlineContent = { Text("Background playback") },
                supportingContent = {
                    Text("Keep playing audio when you leave the app or turn off the screen.")
                },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_nav_audio),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                trailingContent = {
                    Switch(
                        checked = backgroundPlayback,
                        onCheckedChange = { viewModel.setBackgroundPlayback(it) },
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}
