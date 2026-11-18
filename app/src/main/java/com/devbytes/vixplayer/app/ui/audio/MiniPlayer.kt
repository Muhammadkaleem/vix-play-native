package com.devbytes.vixplayer.app.ui.audio

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.player.PlaybackKind
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.exp
import kotlin.math.max

// ── Swipe thresholds ─────────────────────────────────────────────────────────
// All in dp (converted per density) and measured on the *total* drag, so a swipe needs the
// same finger travel on every screen regardless of density or touch-sampling rate.

/** A downward drag past this fraction of the card's height commits a dismissal. */
private const val DISMISS_FRACTION = 0.5f
private val MinDismissDistance = 32.dp

/** Upward finger travel that opens the full player. */
private val ExpandDistance = 40.dp

/** A release this fast (dp/s) commits in its direction even short of the distance… */
private const val FLING_VELOCITY_DP = 900f

/** …provided the finger travelled at least this far, so a twitch on lift can't dismiss. */
private val FlingMinDistance = 12.dp

/** Upward drags are resisted and approach this, hinting there's more above. */
private val MaxUpwardStretch = 20.dp

/** How far below its resting place the card travels (fading out) on dismissal. */
private val ExitOvershoot = 24.dp

private const val DISMISS_EXIT_MS = 180
private val CardShape = RoundedCornerShape(20.dp)

/**
 * Persistent card above the bottom nav while audio is loaded. Tap or swipe up opens the full
 * player; swipe down stops playback (with an Undo via [onDismissed]).
 *
 * Visibility keys off [PlaybackKind] rather than "is something queued": the player is
 * shared with video and `stop()` keeps the playlist, so a queued-item check would
 * advertise a video the user just exited. It also stays up while **paused** — a
 * mini-player that vanishes on pause leaves no way to resume.
 *
 * The swipe is one `draggable` tracking the total offset 1:1, settled on release by
 * distance or fling velocity. Every per-frame value is applied in `graphicsLayer` /
 * `drawBehind`, so neither dragging nor the progress line recomposes the card.
 */
@OptIn(UnstableApi::class)
@Composable
fun MiniPlayer(
    onExpand: () -> Unit,
    onDismissed: (undo: () -> Unit) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MiniPlayerViewModel = hiltViewModel(),
) {
    val kind by viewModel.kind.collectAsState()
    val player = viewModel.player
    val visible = kind == PlaybackKind.AUDIO

    var metadata by remember { mutableStateOf(player.mediaMetadata) }
    // Media3's own rule: shows Pause while buffering-to-play, Play once ended or errored.
    var showPlay by remember { mutableStateOf(Util.shouldShowPlayButton(player)) }
    var hasNext by remember { mutableStateOf(player.hasNextMediaItem()) }
    var progress by remember { mutableFloatStateOf(0f) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                metadata = player.mediaMetadata
                showPlay = Util.shouldShowPlayButton(player)
                hasNext = player.hasNextMediaItem()
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(visible) {
        while (visible) {
            val duration = player.duration
            progress = if (duration > 0) {
                (player.currentPosition.toFloat() / duration).coerceIn(0f, 1f)
            } else {
                0f
            }
            delay(if (player.isPlaying) 250 else 1_000)
        }
    }

    // ── Swipe state ──────────────────────────────────────────────────────────
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var cardHeight by remember { mutableIntStateOf(0) }
    /** Raw finger travel since the drag began: + down, − up. */
    var dragPx by remember { mutableFloatStateOf(0f) }
    var dismissing by remember { mutableStateOf(false) }
    var settleJob by remember { mutableStateOf<Job?>(null) }

    val dismissPx = max(cardHeight * DISMISS_FRACTION, with(density) { MinDismissDistance.toPx() })
    val expandPx = with(density) { ExpandDistance.toPx() }
    val flingPx = FLING_VELOCITY_DP * density.density
    val flingMinPx = with(density) { FlingMinDistance.toPx() }
    val stretchPx = with(density) { MaxUpwardStretch.toPx() }
    val exitPx = cardHeight + with(density) { ExitOvershoot.toPx() }

    // A fresh appearance always starts at rest (e.g. after an Undo).
    LaunchedEffect(visible) {
        if (visible) {
            settleJob?.cancel()
            dragPx = 0f
            dismissing = false
        }
    }

    fun settleTo(target: Float, spec: AnimationSpec<Float>, velocity: Float = 0f, then: () -> Unit = {}) {
        settleJob?.cancel()
        settleJob = scope.launch {
            animate(dragPx, target, velocity, spec) { value, _ -> dragPx = value }
            then()
        }
    }

    fun springBack(velocity: Float = 0f) =
        settleTo(0f, spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium), velocity)

    fun dismiss() {
        if (dismissing) return
        dismissing = true
        settleTo(exitPx, tween(DISMISS_EXIT_MS, easing = FastOutLinearInEasing)) {
            viewModel.dismiss()
            onDismissed(viewModel::undoDismiss)
        }
    }

    fun expand() {
        onExpand()
        springBack()
    }

    val dragState = rememberDraggableState { delta ->
        val before = dragPx
        dragPx += delta
        // One light tick when the finger crosses a commit point, so the release outcome is
        // felt before letting go.
        val crossedDismiss = before < dismissPx && dragPx >= dismissPx
        val crossedExpand = before > -expandPx && dragPx <= -expandPx
        if (crossedDismiss || crossedExpand) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(tween(220)) + fadeIn(tween(220)),
        exit = shrinkVertically(tween(200)) + fadeOut(tween(120)),
        modifier = modifier,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 6.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { cardHeight = it.height }
                    // The drag sits outside graphicsLayer so it measures in untranslated
                    // space; inside, the card moving with the finger would cancel the delta.
                    .draggable(
                        state = dragState,
                        orientation = Orientation.Vertical,
                        enabled = !dismissing,
                        // Grabbing the card mid-settle continues from where it is — no jump.
                        onDragStarted = { settleJob?.cancel() },
                        onDragStopped = { velocity ->
                            when {
                                dragPx >= dismissPx ||
                                    (velocity >= flingPx && dragPx >= flingMinPx) -> dismiss()
                                dragPx <= -expandPx ||
                                    (velocity <= -flingPx && dragPx <= -flingMinPx) -> expand()
                                else -> springBack(velocity)
                            }
                        },
                    )
                    // A swipe can't be performed with TalkBack; expose stopping as an action.
                    .semantics {
                        customActions = listOf(
                            CustomAccessibilityAction("Stop playback") { dismiss(); true },
                        )
                    }
                    .graphicsLayer {
                        val drag = dragPx
                        // Down: follows the finger 1:1. Up: rubber-band toward MaxUpwardStretch.
                        val y = if (drag >= 0f) drag else -stretchPx * (1f - exp(drag / stretchPx))
                        translationY = y
                        val fraction = if (exitPx > 0f) (y / exitPx).coerceIn(0f, 1f) else 0f
                        alpha = 1f - fraction
                        val scale = 1f - 0.05f * fraction
                        scaleX = scale
                        scaleY = scale
                    }
                    .shadow(elevation = 10.dp, shape = CardShape)
                    .clip(CardShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), CardShape)
                    .clickable(onClickLabel = "Open player", onClick = ::expand),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AlbumArt(uri = metadata.artworkUri?.toString().orEmpty(), size = 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = metadata.title?.toString() ?: "Not playing",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        metadata.artist?.toString()?.let { artist ->
                            Text(
                                text = artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    IconButton(onClick = { Util.handlePlayPauseButtonAction(player) }) {
                        Icon(
                            painter = painterResource(if (showPlay) R.drawable.ic_play else R.drawable.ic_pause),
                            contentDescription = if (showPlay) "Play" else "Pause",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    IconButton(onClick = { player.seekToNextMediaItem() }, enabled = hasNext) {
                        Icon(
                            painter = painterResource(R.drawable.ic_next),
                            contentDescription = "Next",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (hasNext) 1f else 0.38f),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                // Progress along the card's bottom edge (clipped to its corners). Read in the
                // draw phase only, so the 250 ms ticks never recompose the card.
                val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                val fill = MaterialTheme.colorScheme.primary
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(3.dp)
                        .drawBehind {
                            drawRect(track)
                            drawRect(fill, size = Size(size.width * progress, size.height))
                        },
                )
            }
        }
    }
}
