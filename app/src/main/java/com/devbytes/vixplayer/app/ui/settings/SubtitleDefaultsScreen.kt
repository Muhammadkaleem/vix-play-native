package com.devbytes.vixplayer.app.ui.settings

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.ui.player.components.SubtitleStyleEditor

/**
 * Subtitle appearance defaults. Hosts the same editor the player sheet uses, writing the
 * same DataStore-backed style, so the two can never disagree.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleDefaultsScreen(
    onBack: () -> Unit = {},
    viewModel: SubtitleDefaultsViewModel = hiltViewModel(),
) {
    val style by viewModel.style.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subtitles") },
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
        SubtitleStyleEditor(
            style = style,
            onChange = { viewModel.setStyle(it) },
            showPreview = true,
            modifier = Modifier.padding(padding),
        )
    }
}
