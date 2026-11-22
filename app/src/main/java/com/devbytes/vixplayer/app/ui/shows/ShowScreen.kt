package com.devbytes.vixplayer.app.ui.shows

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.unit.min
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFramePercent
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.analytics.Analytics
import com.devbytes.vixplayer.app.data.repository.ShowWithVideos
import com.devbytes.vixplayer.app.data.repository.VideoFile
import com.devbytes.vixplayer.app.data.series.ShowEpisode
import com.devbytes.vixplayer.app.ui.common.bottomOnly
import com.devbytes.vixplayer.app.ui.common.screenInsets
import com.devbytes.vixplayer.app.ui.common.withoutBottom
import com.devbytes.vixplayer.app.ui.library.components.FolderListSkeleton
import com.devbytes.vixplayer.app.ui.library.components.LibraryEmptyState
import com.devbytes.vixplayer.app.ui.theme.OnScrim
import com.devbytes.vixplayer.app.ui.theme.ScrimStrong
import com.devbytes.vixplayer.app.ui.theme.TimecodeStyle
import com.devbytes.vixplayer.app.ui.common.focusRow

/**
 * One show: a hero with the main action (resume / next / start / watch again), season chips,
 * and the season's episodes in order with progress. Re-read on every return from the player.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowScreen(
    showKey: String,
    onPlay: (VideoFile) -> Unit,
    onBack: () -> Unit,
    viewModel: ShowViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, showKey) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.load(showKey)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val loaded = (state as? ShowUiState.Loaded)?.show
    Scaffold(
        contentWindowInsets = screenInsets(),
        topBar = {
            TopAppBar(
                title = { Text(loaded?.show?.title.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_back), contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        when (val s = state) {
            ShowUiState.Loading -> FolderListSkeleton(modifier = Modifier.padding(padding))
            ShowUiState.Missing -> LibraryEmptyState(
                iconRes = R.drawable.ic_nav_video,
                title = "Show not found",
                body = "Its episodes may have been moved, renamed or deleted.",
                modifier = Modifier.padding(padding),
            )
            is ShowUiState.Loaded -> ShowContent(
                data = s.show,
                padding = padding,
                onPlay = { episode, source ->
                    s.show.videoOf(episode)?.let { video ->
                        Analytics.log(context, Analytics.SHOW_PLAY, "source" to source, "kind" to s.show.show.continueKind.name.lowercase())
                        onPlay(video)
                    }
                },
            )
        }
    }
}

@Composable
private fun ShowContent(data: ShowWithVideos, padding: PaddingValues, onPlay: (ShowEpisode, String) -> Unit) {
    val show = data.show
    // Opens on the season you're up to; your own pick then holds across refreshes.
    var season by remember(show.key) { mutableStateOf(show.continueEpisode.info.season) }
    if (season !in show.seasons) season = show.continueEpisode.info.season

    // Tablets in landscape and TVs: a full-width 16:9 hero would fill the screen, so it sits beside
    // the summary and Play button instead.
    val wide = LocalConfiguration.current.screenWidthDp.dp >= WideHero
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding.withoutBottom()),
        contentPadding = padding.bottomOnly(extra = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            Hero(data, wide, onPlay = { onPlay(show.continueEpisode, "hero") })
        }
        if (!wide) item {
            Text(
                showSummary(show),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        if (show.seasons.size > 1) {
            item {
                LazyRow(
                    modifier = Modifier.focusRow(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(show.seasons) { number ->
                        val seasonEpisodes = show.episodes.filter { it.info.season == number }
                        FilterChip(
                            selected = number == season,
                            onClick = { season = number },
                            label = { Text("${seasonLabel(number)} · ${seasonEpisodes.size}") },
                            leadingIcon = if (seasonEpisodes.all { it.watched }) {
                                { Icon(painterResource(R.drawable.ic_check), contentDescription = "Watched", modifier = Modifier.size(16.dp)) }
                            } else null,
                        )
                    }
                }
            }
        }
        val seasonEpisodes = show.episodes.filter { it.info.season == season }
        item(key = "season-$season") { SeasonOverview(season, seasonEpisodes) }
        items(seasonEpisodes, key = { it.file.id }) { episode ->
            EpisodeRow(
                episode = episode,
                video = data.videoOf(episode),
                upNext = episode == show.continueEpisode,
                onClick = { onPlay(episode, "episode") },
            )
        }
    }
}

/** The open season: its size and running time, how much is watched, and what's left. */
@Composable
private fun SeasonOverview(season: Int, episodes: List<ShowEpisode>) {
    val watched = episodes.count { it.watched }
    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)) {
        Text(
            seasonLabel(season),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            seasonSummary(episodes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (watched > 0) {
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Box(
                    Modifier.fillMaxWidth(watched.toFloat() / episodes.size).height(4.dp)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
            seasonRemaining(episodes)?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** From this width the hero sits beside its Play button rather than filling the screen. */
private val WideHero = 600.dp

@Composable
private fun Hero(data: ShowWithVideos, wide: Boolean, onPlay: () -> Unit) {
    val show = data.show
    val episode = show.continueEpisode
    val frame: @Composable (Modifier) -> Unit = { modifier ->
        Box(
            modifier
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClickLabel = continueLabel(show), onClick = onPlay),
        ) {
            AsyncImage(
                model = frameRequest(data.videoOf(episode)),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
            )
            // Over-video scrim: theme-independent, like the other cinematic cards.
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.85f)),
                ),
            )
            Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                Text(
                    "${episode.info.code}${episode.info.episodeTitle?.let { " · $it" }.orEmpty()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = OnScrim,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (episode.inProgress) {
                    Spacer(Modifier.height(8.dp))
                    ProgressLine(episode.progress, Modifier.width(160.dp))
                }
            }
        }
    }
    val playButton: @Composable (Modifier) -> Unit = { modifier ->
        Button(onClick = onPlay, modifier = modifier) {
            Icon(painterResource(R.drawable.ic_play), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(continueLabel(show), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    BoxWithConstraints(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        if (wide) {
            val frameWidth = min(maxWidth * 0.55f, 560.dp)
            Row(verticalAlignment = Alignment.Bottom) {
                frame(Modifier.width(frameWidth))
                Spacer(Modifier.width(24.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        showSummary(show),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    playButton(Modifier.fillMaxWidth())
                }
            }
        } else {
            Column {
                frame(Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                playButton(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun EpisodeRow(episode: ShowEpisode, video: VideoFile?, upNext: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(128.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            AsyncImage(
                model = frameRequest(video),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (episode.watched) {
                Box(Modifier.fillMaxSize().background(ScrimStrong.copy(alpha = 0.45f)))
                Box(
                    Modifier.align(Alignment.Center).size(28.dp).clip(CircleShape).background(ScrimStrong),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.ic_check), contentDescription = "Watched", tint = OnScrim, modifier = Modifier.size(16.dp))
                }
            } else if (episode.inProgress) {
                ProgressLine(episode.progress, Modifier.align(Alignment.BottomStart).fillMaxWidth())
            }
            if (!episode.watched && episode.file.durationMs > 0) {
                Text(
                    runtime(episode),
                    style = TimecodeStyle,
                    color = OnScrim,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(ScrimStrong)
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                episodeLabel(episode),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            // An unstarted episode's running time is already on its thumbnail.
            val status = when {
                episode.watched -> "Watched"
                episode.inProgress -> "${timeLeft(episode)} left · ${(episode.progress * 100).toInt()}%"
                else -> null
            }
            if (upNext || status != null) Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (upNext) {
                    Text(
                        "Up next",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                if (status != null) {
                    Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            val details = listOfNotNull(fileDetails(video).ifBlank { null }, lastPlayedLabel(episode)).joinToString(" · ")
            if (details.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    details,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** A thin progress bar in the brand colour on a dim track. */
@Composable
private fun ProgressLine(progress: Float, modifier: Modifier = Modifier) {
    Box(modifier.height(3.dp).clip(RoundedCornerShape(2.dp)).background(OnScrim.copy(alpha = 0.3f))) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(3.dp).background(MaterialTheme.colorScheme.primary))
    }
}

@Composable
private fun frameRequest(video: VideoFile?) = ImageRequest.Builder(LocalContext.current)
    .data(video?.uri)
    .decoderFactory(VideoFrameDecoder.Factory())
    .videoFramePercent(0.1)
    .build()
