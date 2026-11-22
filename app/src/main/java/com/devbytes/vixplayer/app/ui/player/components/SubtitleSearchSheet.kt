package com.devbytes.vixplayer.app.ui.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.net.Uri
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.data.subtitles.DownloadedSubtitle
import com.devbytes.vixplayer.app.data.subtitles.OnlineSubtitle
import com.devbytes.vixplayer.app.data.subtitles.SubtitleQuery
import com.devbytes.vixplayer.app.ui.common.focusRow
import com.devbytes.vixplayer.app.ui.player.SubtitleSearchViewModel
import java.util.Locale

/**
 * Finds subtitles on OpenSubtitles.com for the video playing: pre-filled with what its name says
 * it is (a show's episode, or a film and its year) and matched by the file's hash where it's on
 * the device, so a file made for this exact release comes first. A tap downloads it and hands it
 * to the player.
 */
@Composable
fun SubtitleSearchSheet(
    initialQuery: SubtitleQuery,
    video: Uri?,
    onSaved: (DownloadedSubtitle) -> Unit,
    onFailed: (String) -> Unit,
    viewModel: SubtitleSearchViewModel = hiltViewModel(),
) {
    LaunchedEffect(video) { viewModel.start(initialQuery, video) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                "Search online",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                "from OpenSubtitles.com",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OutlinedTextField(
            value = state.text,
            onValueChange = viewModel::setText,
            singleLine = true,
            placeholder = { Text("Title of the film or show") },
            trailingIcon = {
                IconButton(onClick = { keyboard?.hide(); viewModel.search() }) {
                    Icon(painterResource(R.drawable.ic_search), contentDescription = "Search")
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); viewModel.search() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                // A text field keeps ↓ as cursor movement; a remote moves on to the filters.
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown) {
                        keyboard?.hide()
                        focusManager.moveFocus(FocusDirection.Down)
                    } else {
                        false
                    }
                },
        )

        LazyRow(
            modifier = Modifier.focusRow(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.season != null && state.episode != null) {
                item {
                    FilterChip(
                        selected = state.episodeOnly,
                        onClick = { viewModel.setEpisodeOnly(!state.episodeOnly) },
                        label = { Text("S%02dE%02d".format(state.season, state.episode)) },
                    )
                }
            }
            items(state.languages) { language ->
                FilterChip(
                    selected = language == state.language,
                    onClick = { viewModel.setLanguage(language) },
                    label = { Text(language.label) },
                )
            }
        }

        Box(Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 420.dp)) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center).padding(24.dp))
                state.error != null -> Column(
                    Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        state.error.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = viewModel::search) { Text("Try again") }
                }
                state.results?.isEmpty() == true -> Text(
                    "No subtitles found. Try another title or language.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )
                else -> LazyColumn(Modifier.fillMaxWidth()) {
                    items(state.results.orEmpty(), key = { it.fileId }) { subtitle ->
                        ResultRow(
                            subtitle = subtitle,
                            downloading = state.downloadingId == subtitle.fileId,
                            onClick = { viewModel.download(subtitle, onSaved, onFailed) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(subtitle: OnlineSubtitle, downloading: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = !downloading, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            subtitle.language.uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = colors.primary,
            modifier = Modifier
                .background(colors.primary.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                subtitle.release,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val details = listOfNotNull(
                "Matches this file".takeIf { subtitle.hashMatch },
                "${formatCount(subtitle.downloads)} downloads",
                "Hearing impaired".takeIf { subtitle.hearingImpaired },
                "Machine-translated".takeIf { subtitle.translated },
            )
            Text(
                details.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = if (subtitle.hashMatch) colors.primary else colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        if (downloading) {
            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
        } else {
            Icon(
                painterResource(R.drawable.ic_download),
                contentDescription = "Download",
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/** 1234 → "1.2k", 1500000 → "1.5M". */
internal fun formatCount(n: Int): String = when {
    n >= 1_000_000 -> "%.1fM".format(Locale.ROOT, n / 1_000_000f).replace(".0M", "M")
    n >= 1_000 -> "%.1fk".format(Locale.ROOT, n / 1_000f).replace(".0k", "k")
    else -> n.toString()
}
