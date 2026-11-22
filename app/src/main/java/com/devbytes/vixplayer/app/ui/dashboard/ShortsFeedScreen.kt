package com.devbytes.vixplayer.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFramePercent
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.ui.library.components.LibraryEmptyState
import com.devbytes.vixplayer.app.ui.theme.OnScrim
import androidx.compose.foundation.focusable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import com.devbytes.vixplayer.app.ui.common.LocalIsTv
import kotlinx.coroutines.launch

/**
 * TikTok-style Shorts feed: a full-screen vertical pager over the user's portrait videos.
 * A single [ExoPlayer] follows the centered page and loops it; other pages show a thumbnail.
 */
@Composable
fun ShortsFeedScreen(
    onBack: () -> Unit = {},
    viewModel: ShortsFeedViewModel = hiltViewModel(),
) {
    val shorts by viewModel.shorts.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val player = remember {
        ExoPlayer.Builder(context).build().apply { repeatMode = Player.REPEAT_MODE_ONE }
    }
    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        val list = shorts
        when {
            list == null ->
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Center),
                )

            list.isEmpty() ->
                LibraryEmptyState(
                    iconRes = R.drawable.ic_nav_video,
                    title = "No Shorts yet",
                    body = "Portrait videos on your device will show up here.",
                    modifier = Modifier.align(Alignment.Center),
                )

            else -> {
                val pagerState = rememberPagerState(pageCount = { list.size })
                var paused by remember { mutableStateOf(false) }

                // Follow the centered page: load + loop that short, resetting the paused flag.
                LaunchedEffect(pagerState.currentPage, list) {
                    val video = list.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
                    player.setMediaItem(MediaItem.fromUri(video.uri))
                    player.prepare()
                    player.playWhenReady = true
                    paused = false
                }

                // TV remote: ↑/↓ move between shorts, OK or Play/Pause pauses. The pager takes focus
                // on TV so the keys always arrive; Back leaves as usual.
                val isTv = LocalIsTv.current
                val scope = rememberCoroutineScope()
                val feedFocus = remember { FocusRequester() }
                LaunchedEffect(isTv) { if (isTv) runCatching { feedFocus.requestFocus() } }
                fun togglePause() {
                    paused = !paused
                    player.playWhenReady = !paused
                }
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(feedFocus)
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            val step = when (event.key) {
                                Key.DirectionUp -> -1
                                Key.DirectionDown -> 1
                                Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.MediaPlayPause -> {
                                    togglePause(); return@onPreviewKeyEvent true
                                }
                                else -> return@onPreviewKeyEvent false
                            }
                            val target = (pagerState.currentPage + step).coerceIn(0, list.lastIndex)
                            scope.launch { pagerState.animateScrollToPage(target) }
                            true
                        }
                        .focusable(enabled = isTv),
                ) { page ->
                    val video = list[page]
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                if (page == pagerState.currentPage) togglePause()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (page == pagerState.currentPage) {
                            AndroidView(
                                factory = { ctx ->
                                    PlayerView(ctx).apply {
                                        useController = false
                                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                        setPlayer(player)
                                    }
                                },
                                update = { it.setPlayer(player) },
                                modifier = Modifier.fillMaxSize(),
                            )
                            if (paused) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_play),
                                    contentDescription = "Play",
                                    tint = OnScrim,
                                    modifier = Modifier.size(64.dp).align(Alignment.Center),
                                )
                            }
                        } else {
                            // Off-screen pages: a cheap frame thumbnail, no player attached.
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(video.uri)
                                    .decoderFactory(VideoFrameDecoder.Factory())
                                    .videoFramePercent(0.1)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        // Title along the bottom. The gradient reaches the screen edge, but the
                        // text is padded above the gesture nav bar so it isn't clipped.
                        Column(
                            Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .background(
                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                        0f to Color.Transparent,
                                        1f to Color.Black.copy(alpha = 0.6f),
                                    )
                                )
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                        ) {
                            Text(
                                video.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = OnScrim,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        // Back control, always on top — padded below the status bar with breathing room.
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 4.dp, top = 8.dp),
        ) {
            Icon(painterResource(R.drawable.ic_back), contentDescription = "Back", tint = OnScrim)
        }
    }
}
