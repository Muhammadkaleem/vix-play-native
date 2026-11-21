package com.devbytes.vixplayer.app.ui.player

import android.app.Activity
import android.app.ActivityManager
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.ContentValues
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Rational
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import com.devbytes.vixplayer.app.analytics.Analytics
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.firebase.crashlytics.FirebaseCrashlytics
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.devbytes.vixplayer.app.MainActivity
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.data.repository.UpNext
import com.devbytes.vixplayer.app.data.repository.VideoFile
import com.devbytes.vixplayer.app.player.PlaybackService
import com.devbytes.vixplayer.app.player.PlayerOwner
import com.devbytes.vixplayer.app.player.label
import com.devbytes.vixplayer.app.player.introMarkError
import com.devbytes.vixplayer.app.player.IntroWindow
import com.devbytes.vixplayer.app.player.IntroSettings
import com.devbytes.vixplayer.app.player.candidates
import com.devbytes.vixplayer.app.player.languageOf
import com.devbytes.vixplayer.app.player.planTracks
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.devbytes.vixplayer.app.player.gesture.DragAction
import com.devbytes.vixplayer.app.player.gesture.GestureEvent
import com.devbytes.vixplayer.app.player.gesture.SeekPrecision
import com.devbytes.vixplayer.app.player.gesture.SeekSide
import com.devbytes.vixplayer.app.player.gesture.playerGestures
import com.devbytes.vixplayer.app.player.thumbnail.ThumbnailProvider
import com.devbytes.vixplayer.app.ui.player.components.AspectHud
import com.devbytes.vixplayer.app.ui.player.components.AspectMode
import com.devbytes.vixplayer.app.ui.player.components.AudioTrackSheet
import com.devbytes.vixplayer.app.ui.player.components.AudioTrackUi
import com.devbytes.vixplayer.app.ui.player.components.PlayerOptionsPanel
import com.devbytes.vixplayer.app.ui.player.components.PlayerOptionsValues
import com.devbytes.vixplayer.app.ui.player.components.DoubleTapSeekOverlay
import com.devbytes.vixplayer.app.ui.player.components.UpNextOverlay
import com.devbytes.vixplayer.app.ui.player.components.ErrorOverlay
import com.devbytes.vixplayer.app.ui.player.components.FileInfo
import com.devbytes.vixplayer.app.ui.player.components.FileInfoSheet
import com.devbytes.vixplayer.app.ui.player.components.GestureHud
import com.devbytes.vixplayer.app.ui.player.components.GestureHudKind
import com.devbytes.vixplayer.app.ui.player.components.LockOverlay
import com.devbytes.vixplayer.app.ui.player.components.OrientationHud
import com.devbytes.vixplayer.app.ui.player.components.OrientationMode
import com.devbytes.vixplayer.app.ui.player.components.PlayerHud
import com.devbytes.vixplayer.app.ui.player.components.ScreenshotHud
import com.devbytes.vixplayer.app.ui.player.components.ScrubPreview
import com.devbytes.vixplayer.app.ui.player.components.SleepTimerHud
import com.devbytes.vixplayer.app.ui.player.components.UnsupportedCodecHud
import com.devbytes.vixplayer.app.ui.player.components.prettyCodec
import com.devbytes.vixplayer.app.player.UnsupportedCodecException
import com.devbytes.vixplayer.app.player.unsupportedMime
import com.devbytes.vixplayer.app.ui.player.components.SleepTimerMode
import com.devbytes.vixplayer.app.ui.player.components.SleepTimerSheet
import com.devbytes.vixplayer.app.ui.player.components.SPEED_DRAG_STEPS_PER_WIDTH
import com.devbytes.vixplayer.app.ui.player.components.SPEED_PRESETS
import com.devbytes.vixplayer.app.ui.player.components.SpeedControlPill
import com.devbytes.vixplayer.app.ui.player.components.formatSpeed
import com.devbytes.vixplayer.app.ui.player.components.nearestSpeedPreset
import com.devbytes.vixplayer.app.ui.player.components.ResumeChip
import com.devbytes.vixplayer.app.ui.player.components.SubtitleSheet
import com.devbytes.vixplayer.app.ui.player.components.SubtitleStyleEditor
import com.devbytes.vixplayer.app.ui.player.components.IntroMarkHud
import com.devbytes.vixplayer.app.ui.player.components.SkipIntroPill
import com.devbytes.vixplayer.app.ui.player.components.SubtitleSyncHud
import com.devbytes.vixplayer.app.ui.player.components.applySubtitleStyle
import com.devbytes.vixplayer.app.ui.player.components.SubtitleTrackUi
import com.devbytes.vixplayer.app.ui.theme.AmoledBackground
import com.devbytes.vixplayer.app.ui.theme.OnScrim
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    uri: String,
    onBack: () -> Unit,
    skin: PlayerSkin = PlayerSkin.Classic,
    /** Another screen loaded a different file into the shared player — leave quietly. */
    onTakenOver: () -> Unit = onBack,
    onOpenEqualizer: () -> Unit = {},
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    var currentSheet by remember { mutableStateOf<PlayerSheet?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // System hooks for vertical-drag gestures.
    val activity = remember { context.findActivity() }
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var volumeLevel by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat())
    }
    var brightness by remember {
        mutableFloatStateOf(
            activity?.window?.attributes?.screenBrightness?.takeIf { it in 0f..1f }
                ?: (Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128) / 255f)
        )
    }
    // Transient drag HUD (brightness/volume) — orthogonal to controlsVisible.
    var hudKind by remember { mutableStateOf<GestureHudKind?>(null) }
    var hudFraction by remember { mutableFloatStateOf(0f) }
    var hudDismissKey by remember { mutableIntStateOf(0) }

    // Horizontal seek scrub — preview-only until release (commit-on-release).
    var scrubbing by remember { mutableStateOf(false) }
    var scrubStartPos by remember { mutableLongStateOf(0L) }
    var scrubTargetMs by remember { mutableLongStateOf(0L) }
    var scrubLastFraction by remember { mutableFloatStateOf(0f) }
    var scrubPrecision by remember { mutableStateOf(SeekPrecision.COARSE) }

    // Long-press on the video opens the speed picker pill (see speedPillOpen below) and a
    // horizontal slide drives its dot selection live; speedPreview holds that in-gesture value
    // (null when not dragging), speedStartIndex is the dot the slide began on.
    var speedPreview by remember { mutableStateOf<Float?>(null) }
    var speedStartIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(hudDismissKey) {
        if (hudDismissKey > 0) {
            delay(600)
            hudKind = null
        }
    }

    // Synchronous MMKV read — safe on main thread.
    val resumePos = remember { viewModel.initMedia(uri) }

    // The URI currently loaded. Swapped in place on "Play next" so per-URI state
    // (title, subtitle configs, resume id) re-derives without recreating the screen.
    var currentUri by remember { mutableStateOf(uri) }

    // Per-file subtitle timing offset. Read synchronously (like resumePos) so prepareFor
    // seeds the holder before prepare() — a remembered offset costs no extra re-prepare.
    val subtitleOffsetHolder = viewModel.subtitleOffset
    var subtitleOffsetMs by remember { mutableLongStateOf(viewModel.getSubtitleOffsetMs()) }

    // Pinch-zoom transform, applied to the video surface. 1x = no zoom.
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var zoomOffset by remember { mutableStateOf(Offset.Zero) }
    // Surface size in px, used to clamp pan to the zoomed content's bounds.
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    // Remappable gesture assignments; PlayerScreen resolves side -> action.
    val bindings by viewModel.gestureBindings.collectAsState()

    // Declared early: the ON_STOP background-pause rule below must consult it, and PiP is
    // the one case where backgrounding must NOT pause (the video is still on screen).
    var pipMode by remember { mutableStateOf(false) }

    // App-scoped player (owned by PlayerController so playback can outlive this Activity).
    // Never released here — only prepared, and paused on exit when appropriate.
    val player = viewModel.player

    // The player is shared by every player screen, and an "open with" from another app stacks a
    // second one over the first. Only the screen whose file is loaded may save its position,
    // pause it or stop it — otherwise a screen left behind writes another file's position under
    // its own episode, or pauses the video that just opened.
    fun ownsPlayer() = player.currentMediaItem?.localConfiguration?.uri?.toString() == currentUri
    val playerOwner = remember { PlayerOwner(activity?.taskId ?: -1) }
    val backgroundPlayback by viewModel.backgroundPlayback.collectAsState()
    val subtitleStyle by viewModel.subtitleStyle.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val canFavorite by viewModel.canFavorite.collectAsState()

    // For a foreign content:// URI (external open) MediaStore has no row to name it, so
    // resolve the display name via OpenableColumns once and hand it to prepareFor as the
    // immediate title. MediaStore URIs still get their canonical name from the async lookup.
    val externalName = remember(uri) {
        queryDisplayName(context, Uri.parse(uri))?.substringBeforeLast('.')
    }

    // Opening a video is one call so per-file state can't leak from the previous one.
    LaunchedEffect(Unit) { viewModel.prepareFor(uri, subtitleOffsetMs, externalName, playerOwner) }

    // Held so the screenshot capture can reach the video SurfaceView for PixelCopy.
    var playerView by remember { mutableStateOf<PlayerView?>(null) }
    val scope = rememberCoroutineScope()

    // Scrub-preview frames. One retriever kept open per file; released on dispose and
    // rebuilt when playNext swaps currentUri.
    val thumbnails = remember(currentUri) { ThumbnailProvider(context, Uri.parse(currentUri)) }
    DisposableEffect(thumbnails) { onDispose { thumbnails.release() } }
    var scrubFrame by remember { mutableStateOf<Bitmap?>(null) }
    // Keyed on the 5s bucket, so a drag only decodes when it crosses a boundary; the
    // effect's cancellation gives latest-wins for free on a fast drag.
    LaunchedEffect(scrubbing, scrubTargetMs / 5_000L) {
        scrubFrame = if (scrubbing) thumbnails.frameAt(scrubTargetMs) else null
    }

    // Externally side-loaded subtitle configs (survive the MediaItem rebuild) + a live
    // snapshot of the player's tracks, refreshed by the listener's onTracksChanged.
    val externalSubs = remember { mutableStateListOf<MediaItem.SubtitleConfiguration>() }
    var tracks by remember { mutableStateOf(player.currentTracks) }
    // Mime of a track with no decoder on this device (see UnsupportedCodec.kt); derived
    // from `tracks` below, read by File Info and the unsupported-codec UI.
    var unsupportedVideoMime by remember { mutableStateOf<String?>(null) }
    var unsupportedAudioMime by remember { mutableStateOf<String?>(null) }

    // Rebuild the item with the current external subs, preserving position + play state.
    fun reloadWithSubtitles() {
        val pos = player.currentPosition
        val wasPlaying = player.playWhenReady
        player.setMediaItem(
            MediaItem.Builder()
                .setUri(currentUri)
                .setSubtitleConfigurations(externalSubs.toList())
                .build()
        )
        player.prepare()
        player.seekTo(pos)
        player.playWhenReady = wasPlaying
    }

    // Sync-adjust surface + debounced commit. Stepping updates the readout instantly,
    // but the re-prepare (Media3 stamps cue timings at parse time, so a new offset only
    // reaches already-buffered cues via a re-parse) waits for the user to settle.
    var showSyncHud by remember { mutableStateOf(false) }

    // Skip intro (Series Mode phase 3a): the current episode's intro, the marking panel's
    // unsaved marks, and the playhead they're judged against.
    var intro by remember { mutableStateOf(IntroSettings.NONE) }
    var showIntroHud by remember { mutableStateOf(false) }
    var markStartMs by remember { mutableStateOf<Long?>(null) }
    var markEndMs by remember { mutableStateOf<Long?>(null) }
    var introPositionMs by remember { mutableLongStateOf(0L) }
    // Auto-skip fires once per opened file, so seeking back to watch the intro sticks.
    var autoSkippedUri by remember { mutableStateOf<String?>(null) }
    var showIntroSkipped by remember { mutableStateOf(false) }
    // A background scan of this season for its intro is queued or running (phase 3b).
    var introDetecting by remember { mutableStateOf(false) }
    var pendingOffsetKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(pendingOffsetKey) {
        if (pendingOffsetKey > 0) {
            delay(350)
            subtitleOffsetHolder.offsetUs = subtitleOffsetMs * 1_000L
            viewModel.saveSubtitleOffsetMs(subtitleOffsetMs)
            reloadWithSubtitles()
        }
    }
    fun stepSubtitleOffset(deltaMs: Long) {
        subtitleOffsetMs = (subtitleOffsetMs + deltaMs).coerceIn(-60_000L, 60_000L)
        pendingOffsetKey++
    }

    val subtitlePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { picked ->
        if (picked != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    picked,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            val name = queryDisplayName(context, picked) ?: picked.lastPathSegment ?: "External"
            externalSubs.add(
                MediaItem.SubtitleConfiguration.Builder(picked)
                    .setMimeType(subtitleMimeFor(name))
                    .setLabel(name)
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build()
            )
            reloadWithSubtitles()
        }
    }

    // Resume decision, made once duration is known:
    //  - trivial (<=5s in) → start from 0, no chip;
    //  - finished (>=95%) → start over silently;
    //  - otherwise → seek to saved position and offer a transient Restart chip.
    var showResumeChip by remember { mutableStateOf(false) }
    LaunchedEffect(resumePos) {
        if (resumePos <= 5_000L) return@LaunchedEffect
        var duration = player.duration
        while (duration <= 0L) {
            delay(50)
            duration = player.duration
        }
        if (resumePos >= (duration * 0.95f).toLong()) {
            player.seekTo(0L)
        } else {
            player.seekTo(resumePos)
            showResumeChip = true
        }
    }
    // Auto-dismiss the resume chip.
    LaunchedEffect(showResumeChip) {
        if (showResumeChip) {
            delay(5_000)
            showResumeChip = false
        }
    }

    // Save position to MMKV every 5 s while playing.
    LaunchedEffect(Unit) {
        while (true) {
            delay(5_000)
            if (ownsPlayer()) viewModel.savePositionFast(player.currentPosition)
        }
    }

    // Authoritative playback state, driven by the player itself.
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    // Stamp "last played = now" once when this item actually starts, so the floating resume
    // button targets it immediately (reset per file in playNext).
    var playedStamped by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(player.playbackState == Player.STATE_BUFFERING) }
    var playbackError by remember { mutableStateOf<PlaybackException?>(player.playerError) }
    // Video dimensions — drive screen orientation to the video's shape.
    var videoWidth by remember { mutableIntStateOf(player.videoSize.width) }
    var videoHeight by remember { mutableIntStateOf(player.videoSize.height) }

    // Terminal "finished" state (STATE_ENDED) + what plays next: the show's following
    // episode, or the next file in the folder in natural order. Resolved when a file starts —
    // so the options panel's Play Next works mid-video — and refreshed when it ends.
    var ended by remember { mutableStateOf(player.playbackState == Player.STATE_ENDED) }
    var upNext by remember { mutableStateOf(UpNext.NONE) }
    // The file [upNext] was resolved for — track choice waits until it's this one.
    var upNextUri by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(currentUri, ended) {
        upNext = viewModel.resolveUpNext(8)
        upNextUri = currentUri
    }
    val nextVideo = upNext.next

    // ── Skip intro ───────────────────────────────────────────────────────────────────────
    // Only episodes of a show (Up Next resolved a show key for this very file) have one.
    suspend fun reloadIntro() {
        val key = upNext.showKey.takeIf { upNextUri == currentUri }
        intro = if (key == null) IntroSettings.NONE else viewModel.loadIntro(key, player.duration)
    }
    fun saveIntroMarks(episodeOnly: Boolean) {
        val key = upNext.showKey ?: return
        val start = markStartMs ?: return
        val end = markEndMs ?: return
        if (introMarkError(start, end, player.duration) != null) return
        scope.launch {
            viewModel.saveIntro(key, IntroWindow(start, end), episodeOnly)
            Analytics.log(context, Analytics.INTRO_MARKED, "scope" to if (episodeOnly) "episode" else "show")
            reloadIntro()
            showIntroHud = false
            // Just marked while watching it — don't immediately auto-skip what was marked.
            autoSkippedUri = currentUri
        }
    }
    // Load once Up Next knows the show and the duration is known (the window must fit in it).
    LaunchedEffect(currentUri, upNextUri) {
        if (upNextUri != currentUri) return@LaunchedEffect
        withTimeoutOrNull(5_000) { while (player.duration <= 0) delay(200) }
        reloadIntro()
        upNext.showKey?.let { viewModel.startIntroDetection(it) }
    }
    // As soon as this episode's result is in (before the rest of the season), pick it up.
    LaunchedEffect(upNext.showKey, upNextUri == currentUri) {
        val key = upNext.showKey.takeIf { upNextUri == currentUri } ?: return@LaunchedEffect
        viewModel.introEpisodeReady(key).drop(1).collect { reloadIntro() }
    }
    // When a scan finishes, pick up what it found for this episode.
    LaunchedEffect(upNext.showKey, upNextUri == currentUri) {
        val key = upNext.showKey.takeIf { upNextUri == currentUri } ?: return@LaunchedEffect
        viewModel.introDetecting(key).collect { running ->
            if (introDetecting && !running) reloadIntro()
            introDetecting = running
        }
    }
    // The playhead, while there's an intro to judge it against or the marking panel is open.
    LaunchedEffect(intro.window, showIntroHud) {
        while (intro.window != null || showIntroHud) {
            if (ownsPlayer()) introPositionMs = player.currentPosition
            delay(250)
        }
    }
    // Auto-skip: once per opened file, when the playhead first lands inside the intro.
    LaunchedEffect(introPositionMs) {
        val window = intro.window ?: return@LaunchedEffect
        if (intro.autoSkip && !showIntroHud && autoSkippedUri != currentUri && window.offersSkipAt(introPositionMs)) {
            autoSkippedUri = currentUri
            player.seekTo(window.endMs)
            Analytics.log(context, Analytics.INTRO_SKIP, "source" to "auto")
            showIntroSkipped = true
        }
    }
    LaunchedEffect(showIntroSkipped) {
        if (showIntroSkipped) {
            delay(4_000)
            showIntroSkipped = false
        }
    }

    // Autoplay preferences (single source of truth, shared with Settings).
    val autoplayNext by viewModel.autoplayNext.collectAsState()
    val askBeforePlayingNext by viewModel.askBeforePlayingNext.collectAsState()
    // Autoplay countdown: non-null while an auto-advance is pending at end-of-video. A
    // "Not Now"/"Cancel" suppresses this transition's auto-advance (the effect is below,
    // after playNext is declared). confirmMode = ask-but-don't-autoplay.
    var autoCountdown by remember { mutableStateOf<Int?>(null) }
    var autoplaySuppressed by remember { mutableStateOf(false) }
    val confirmMode = !autoplayNext && askBeforePlayingNext && nextVideo != null

    // In-app review: watching a video to the end is a positive signal. Track whether a codec
    // error occurred this visit (never ask after a bad experience), ask at most once per
    // screen instance, and let the Play API enforce its own frequency/quota. No-op unless the
    // build was installed from Play — runCatching keeps a failed flow from touching playback.
    var hadPlaybackError by remember { mutableStateOf(false) }
    var reviewRequested by remember { mutableStateOf(false) }
    val reviewManager = remember { ReviewManagerFactory.create(context) }
    LaunchedEffect(ended) {
        // Count the completion either way, but don't prompt while autoplay is about to start
        // the next video under the sheet — a later ending without autoplay will ask instead.
        if (ended && !hadPlaybackError && !reviewRequested &&
            viewModel.recordCompletionAndShouldAskReview() &&
            !(autoplayNext && nextVideo != null)
        ) {
            reviewRequested = true
            activity?.let { act ->
                // Play decides whether the dialog actually appears and never says, so these
                // record that we asked and whether the flow ran — not that a review was left.
                Analytics.log(context, Analytics.REVIEW_PROMPT_REQUESTED)
                val outcome = runCatching {
                    reviewManager.launchReview(act, reviewManager.requestReview())
                }
                Analytics.log(
                    context,
                    Analytics.REVIEW_FLOW_FINISHED,
                    "outcome" to if (outcome.isSuccess) "completed" else "failed",
                )
            }
        }
    }

    // Persisted global playback speed — the stored value drives the player on load
    // and on every pick. The speed-hold gesture reverts to this (reads player speed).
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    LaunchedEffect(playbackSpeed) { player.setPlaybackSpeed(playbackSpeed) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                // First time this file actually plays → record it as the most-recent play.
                if (playing && !playedStamped && ownsPlayer()) {
                    playedStamped = true
                    viewModel.markPlayed()
                }
                // Pausing flushes the resume point immediately (steady-state 5s tick
                // is too coarse for a pause-then-exit).
                if (!playing && ownsPlayer()) flushPositionFast(player, viewModel)
            }

            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                ended = state == Player.STATE_ENDED
            }

            override fun onPlayerError(error: PlaybackException) {
                playbackError = error
                // A failed playback must never lead into a review prompt.
                hadPlaybackError = true
                // The one curated non-fatal: quantifies the real "plays everything" gap
                // (the FFmpeg go/no-go signal) — a swallowed error the UI shows but no one
                // would otherwise see aggregated. Keyed by code so Crashlytics groups them.
                FirebaseCrashlytics.getInstance().apply {
                    setCustomKey("codec_error", error.errorCodeName)
                    recordException(error)
                }
            }

            override fun onTracksChanged(newTracks: Tracks) {
                tracks = newTracks
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                videoWidth = videoSize.width
                videoHeight = videoSize.height
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int,
            ) {
                // A user seek lands the resume point somewhere new — flush at once.
                if (reason == Player.DISCONTINUITY_REASON_SEEK && ownsPlayer()) {
                    flushPositionFast(player, viewModel)
                }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    // Backgrounding may precede an OS kill — flush the fast (resume) path on ON_STOP.
    // MMKV alone is enough here: resume reads MMKV and it survives process death;
    // the durable Room persist stays on onDispose (deterministic teardown).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, backgroundPlayback, pipMode) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && ownsPlayer()) {
                flushPositionFast(player, viewModel)
                // Default is to pause on background — this is a video player, and silent
                // continued playback reads as a battery bug. PiP is its own case: the
                // video is still on screen, so it must keep playing.
                if (!backgroundPlayback && !pipMode) player.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Persist to Room on navigation away. The player is app-scoped now, so it is NOT
    // released here — it is stopped, which frees decoders while keeping it usable.
    DisposableEffect(Unit) {
        onDispose {
            if (ownsPlayer()) {
                viewModel.persistPosition(player.currentPosition)
                player.stop()
            }
        }
    }

    // Immersive full-screen — hide the system bars on the player surface (sticky:
    // an edge swipe peeks them, then they auto-hide). Restore on exit, and reset
    // orientation to the device default so the library isn't left rotated.
    DisposableEffect(activity) {
        val controller = activity?.window?.let {
            WindowInsetsControllerCompat(it, it.decorView)
        }
        controller?.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Orientation override — session-local, persists across a Play-next swap (a
    // per-session viewing choice, not a per-file property), resets to Auto only on
    // leaving the player. A cycle flashes a transient OrientationHud pill.
    var orientationMode by remember { mutableStateOf(OrientationMode.AUTO) }
    var showOrientationHud by remember { mutableStateOf(false) }
    var orientationHudKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(orientationHudKey) {
        if (orientationHudKey > 0) {
            showOrientationHud = true
            delay(800)
            showOrientationHud = false
        }
    }

    // Single writer of requestedOrientation. AUTO follows the video's shape:
    // landscape (16:9) → sensor-landscape, portrait (9:16) → sensor-portrait
    // (flippable within the axis), square/unknown → device default. A manual
    // override hard-pins the device (sensor out), winning over the auto mapping
    // even after a Play-next swap re-fires this effect.
    LaunchedEffect(videoWidth, videoHeight, orientationMode) {
        activity?.requestedOrientation = when (orientationMode) {
            OrientationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            OrientationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            OrientationMode.AUTO -> when {
                videoWidth > videoHeight -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                videoHeight > videoWidth -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    // Gesture-lock state — when locked, only the unlock chip is live.
    var locked by remember { mutableStateOf(false) }

    // Video surface scaling — session-local, resets to Fit on each open. Applied to
    // the PlayerView's resizeMode; a cycle flashes a transient AspectHud pill.
    var aspectMode by remember { mutableStateOf(AspectMode.FIT) }
    var showAspectHud by remember { mutableStateOf(false) }
    var aspectHudKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(aspectHudKey) {
        if (aspectHudKey > 0) {
            showAspectHud = true
            delay(800)
            showAspectHud = false
        }
    }

    // Controls visibility state machine:
    //  - auto-hide (3s) only while playing, unlocked, and not "held open";
    //  - pause forces controls shown;
    //  - locked hides the HUD entirely (LockOverlay owns the surface);
    //  - "held open" = an open sheet, the overflow menu, or an active seek-bar
    //    scrub — any user engagement that must not vanish mid-interaction.
    var controlsVisible by remember { mutableStateOf(true) }
    var optionsPanelOpen by remember { mutableStateOf(false) }
    var speedPillOpen by remember { mutableStateOf(false) }
    var seekBarDragging by remember { mutableStateOf(false) }
    val sheetOpen = currentSheet != null
    val controlsHeldOpen = sheetOpen || optionsPanelOpen || speedPillOpen || seekBarDragging || showIntroHud

    // Centralized overlay coordination: the user only sees the control they're using. Any of
    // these "exclusive" surfaces suppresses the standard HUD controls and the resume chip, so
    // overlays never stack. Priority runs modal (panel/sheet) > speed pill > gesture slider
    // (volume/brightness) > surface seek-scrub. (Player gestures are already disabled while the
    // panel is open, so a slider can't fire over it; dragging the seek bar itself is NOT
    // exclusive — its scrub preview is part of the HUD.)
    val gestureSliderActive = hudKind != null
    val exclusiveOverlayActive =
        optionsPanelOpen || speedPillOpen || sheetOpen || gestureSliderActive || scrubbing
    LaunchedEffect(controlsVisible, isPlaying, controlsHeldOpen, locked) {
        if (controlsVisible && isPlaying && !controlsHeldOpen && !locked) {
            delay(3_000)
            controlsVisible = false
        }
    }
    // Pausing always reveals the controls.
    LaunchedEffect(isPlaying) {
        if (!isPlaying) controlsVisible = true
    }

    // Double-tap seek state
    var doubleTapSide by remember { mutableStateOf<SeekSide?>(null) }
    var accumulatedSeekMs by remember { mutableLongStateOf(0L) }
    var doubleTapKey by remember { mutableIntStateOf(0) }
    var lastTapSide by remember { mutableStateOf<SeekSide?>(null) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(doubleTapKey) {
        if (doubleTapKey > 0) {
            delay(800)
            doubleTapSide = null
        }
    }

    // Resolved from MediaStore by the ViewModel; slicing the URI yields the numeric id
    // for content:// sources, which is what used to be displayed here.
    val resolvedTitle by viewModel.displayTitle.collectAsState()
    val title = resolvedTitle.ifBlank { "Now Playing" }

    // In-place swap to the next folder video (reuses the MediaItem-rebuild pattern):
    // re-seed the resume id, clear external subs, start fresh at 0, dismiss the overlay.
    fun playNext(video: VideoFile) {
        val newUri = video.uri.toString()
        currentUri = newUri
        externalSubs.clear()
        viewModel.initMedia(newUri)
        // Offsets are per file — pick up the incoming file's stored value before
        // prepare() so its cues parse at the right offset, not the previous file's.
        subtitleOffsetMs = viewModel.getSubtitleOffsetMs()
        showSyncHud = false
        intro = IntroSettings.NONE
        showIntroHud = false
        showIntroSkipped = false
        viewModel.prepareFor(newUri, subtitleOffsetMs, owner = playerOwner)
        showResumeChip = false
        ended = false
        playedStamped = false
        upNext = UpNext.NONE
        autoplaySuppressed = false
        // Fresh file → fresh error-tracking for the review signal (a prior file's error
        // shouldn't suppress a prompt after this one plays cleanly to the end).
        hadPlaybackError = false
        // A zoom is a "look closer at this shot" intent tied to the current file; a new
        // video (likely a different resolution) starts un-zoomed and centred.
        zoomScale = 1f
        zoomOffset = Offset.Zero
    }

    // How the end-of-video screen was answered — Up Next acceptance, split by episodes.
    fun logUpNext(result: String) = Analytics.log(
        context, Analytics.UP_NEXT_RESULT, "result" to result, "episodic" to (upNext.showTitle != null),
    )

    // Autoplay auto-advance: when the video ends with autoplay on and a next video exists,
    // count down (giving a Cancel/Not Now window) then advance. Suppressed per-transition by
    // Not Now/Cancel. Repeat-one never reaches here (the player loops instead of ending).
    LaunchedEffect(ended, autoplayNext, nextVideo, autoplaySuppressed) {
        if (ended && autoplayNext && nextVideo != null && !autoplaySuppressed) {
            var n = 5
            autoCountdown = n
            while (n > 0) {
                delay(1000)
                n--
                autoCountdown = n
            }
            autoCountdown = null
            nextVideo?.let {
                logUpNext("autoplay")
                playNext(it)
            }
        } else {
            autoCountdown = null
        }
    }

    // File Info sheet data — loaded once on first open (file metadata via MediaStore,
    // stream info snapshotted off the player).
    var fileInfo by remember { mutableStateOf<FileInfo?>(null) }
    LaunchedEffect(currentSheet) {
        if (currentSheet is PlayerSheet.FileInfo && fileInfo == null) {
            val vf = viewModel.loadFileInfo()
            val videoFormat = player.videoFormat
            val size = player.videoSize
            fileInfo = FileInfo(
                name = vf?.name ?: title,
                path = vf?.path,
                sizeBytes = vf?.sizeBytes?.takeIf { it > 0 },
                durationMs = (vf?.durationMs?.takeIf { it > 0 }) ?: player.duration.takeIf { it > 0 },
                width = size.width.takeIf { it > 0 },
                height = size.height.takeIf { it > 0 },
                videoCodecMime = videoFormat?.sampleMimeType,
                audioCodecMime = player.audioFormat?.sampleMimeType,
                frameRate = videoFormat?.frameRate?.takeIf { it > 0f },
                videoDecoder = viewModel.videoDecoder.value,
                audioDecoder = viewModel.audioDecoder.value,
                unsupportedVideoMime = unsupportedVideoMime,
                unsupportedAudioMime = unsupportedAudioMime,
            )
        }
    }

    // Unsupported codec = a track no renderer can decode. ExoPlayer raises no error for it
    // (it just leaves the track unselected), so detect it from the track list: audio →
    // one transient pill per file (the video still plays), video → terminal overlay (there
    // is nothing to watch). Both record a non-fatal so Crashlytics counts how often the
    // excluded AC-3/DTS decoders bite — the data the "revisit at 1.0" decision needs.
    var showCodecHud by remember { mutableStateOf(false) }
    var codecHudLabel by remember { mutableStateOf("") }
    var codecHudKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(codecHudKey) {
        if (codecHudKey > 0) {
            showCodecHud = true
            delay(4_000)
            showCodecHud = false
        }
    }
    var notifiedUnsupportedFor by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(tracks, currentUri) {
        unsupportedVideoMime = tracks.unsupportedMime(C.TRACK_TYPE_VIDEO)
        unsupportedAudioMime = tracks.unsupportedMime(C.TRACK_TYPE_AUDIO)
        val audioMime = unsupportedAudioMime ?: return@LaunchedEffect
        val key = "$currentUri|$audioMime"
        if (notifiedUnsupportedFor == key) return@LaunchedEffect
        notifiedUnsupportedFor = key
        codecHudLabel = "No audio — this device can't play ${prettyCodec(audioMime)}"
        codecHudKey++
        Log.w("PlayerScreen", "Unsupported audio codec: $audioMime")
        FirebaseCrashlytics.getInstance().apply {
            setCustomKey("unsupported_codec", audioMime)
            recordException(UnsupportedCodecException(audioMime, "audio"))
        }
    }
    LaunchedEffect(unsupportedVideoMime, currentUri) {
        val mime = unsupportedVideoMime ?: return@LaunchedEffect
        // Without this the audio keeps playing under the overlay and runs into Up Next.
        player.pause()
        FirebaseCrashlytics.getInstance().apply {
            setCustomKey("unsupported_codec", mime)
            recordException(UnsupportedCodecException(mime, "video"))
        }
    }

    // Presentational subtitle track list, derived from the live tracks snapshot.
    val subtitleTracks = remember(tracks) {
        buildList {
            tracks.groups.forEachIndexed { groupIndex, group ->
                if (group.type == C.TRACK_TYPE_TEXT) {
                    for (trackIndex in 0 until group.length) {
                        val format = group.getTrackFormat(trackIndex)
                        val label = format.label
                            ?: format.language?.let { Locale.forLanguageTag(it).displayName.ifBlank { null } }
                            ?: "Track ${groupIndex + 1}.${trackIndex + 1}"
                        add(
                            SubtitleTrackUi(
                                id = "$groupIndex:$trackIndex",
                                label = label,
                                selected = group.isTrackSelected(trackIndex),
                            )
                        )
                    }
                }
            }
        }
    }
    val subtitlesDisabled = subtitleTracks.none { it.selected }

    fun selectSubtitle(id: String) {
        val (groupIndex, trackIndex) = id.split(":").map { it.toInt() }
        val group = tracks.groups[groupIndex]
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, trackIndex))
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .build()
    }

    fun disableSubtitles() {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()
    }

    // Presentational audio track list, derived from the live tracks snapshot. Unlike
    // subtitles there's no "Off" (a video always plays one audio track); the picker
    // only surfaces when there's a real choice (>= 2 tracks).
    val audioTracks = remember(tracks) {
        buildList {
            tracks.groups.forEachIndexed { groupIndex, group ->
                if (group.type == C.TRACK_TYPE_AUDIO) {
                    for (trackIndex in 0 until group.length) {
                        val format = group.getTrackFormat(trackIndex)
                        val label = format.label
                            ?: format.language?.let { Locale.forLanguageTag(it).displayName.ifBlank { null } }
                            ?: "Track ${groupIndex + 1}.${trackIndex + 1}"
                        add(
                            AudioTrackUi(
                                id = "$groupIndex:$trackIndex",
                                label = label,
                                selected = group.isTrackSelected(trackIndex),
                            )
                        )
                    }
                }
            }
        }
    }

    fun selectAudioTrack(id: String) {
        val (groupIndex, trackIndex) = id.split(":").map { it.toInt() }
        val group = tracks.groups[groupIndex]
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, trackIndex))
            .build()
    }

    // Once per file, when its tracks are listed: the show's remembered audio/subtitle choice,
    // else "Turn on subtitles automatically" (see planTracks). Waits until Up Next knows this
    // file's show, and checks the tracks are this file's, not the episode just left. Once only,
    // so a pick in the sheet holds for the rest of the file.
    val autoSubtitles by viewModel.autoSubtitles.collectAsState()
    var tracksPlanned by remember(currentUri) { mutableStateOf(false) }
    LaunchedEffect(currentUri, tracks, upNextUri, autoSubtitles) {
        val ready = upNextUri == currentUri && tracks.groups.isNotEmpty() && tracks == player.currentTracks &&
            player.currentMediaItem?.localConfiguration?.uri?.toString() == currentUri
        if (tracksPlanned || !ready) return@LaunchedEffect
        tracksPlanned = true
        val remembered = upNext.showKey?.let { viewModel.rememberedTracks(it) }
        val plan = planTracks(
            tracks.candidates(C.TRACK_TYPE_AUDIO), tracks.candidates(C.TRACK_TYPE_TEXT),
            remembered, autoSubtitles, Locale.getDefault(),
        )
        plan.audioId?.let(::selectAudioTrack)
        if (plan.subtitlesOff) disableSubtitles() else plan.subtitleId?.let(::selectSubtitle)
    }

    // Picture-in-Picture — only on API 26+ devices that advertise the feature.
    val supportsPip = remember {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    // The one in-PiP control (touch doesn't reach the surface there): a play/pause
    // RemoteAction routes through this receiver to toggle the player.
    DisposableEffect(supportsPip) {
        if (!supportsPip) return@DisposableEffect onDispose { }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                if (player.isPlaying) player.pause() else player.play()
            }
        }
        ContextCompat.registerReceiver(
            context, receiver, IntentFilter(ACTION_PIP_TOGGLE),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose { context.unregisterReceiver(receiver) }
    }

    fun enterPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
            activity.enterPictureInPictureMode(
                buildPipParams(context, videoWidth, videoHeight, player.isPlaying)
            )
        }
    }

    // Register the leave-hint contract with the Activity while on screen; clear on exit.
    val pipController = remember(activity) { (activity as? MainActivity)?.pipController }
    DisposableEffect(pipController) {
        pipController?.onModeChanged = { pipMode = it }
        onDispose {
            pipController?.canEnterPip = { false }
            pipController?.buildParams = { null }
            pipController?.onModeChanged = {}
        }
    }
    // Taken over (see PlayerOwner): the player still holds this file's position, so save it
    // in both stores first, then leave the player screen. A PiP window would go on showing
    // the other file: when the new screen opened inside this PiP task (an "open with" that
    // asks for a new task joins ours), the task stays in PiP, so re-launch the task's own
    // root intent — like a launcher tap, that brings it back full screen with the new screen
    // on top (moveTaskToFront doesn't leave PiP); when it opened elsewhere, close this activity.
    val currentOnTakenOver by rememberUpdatedState(onTakenOver)
    SideEffect {
        playerOwner.onTakenOver = { byTaskId ->
            val position = player.currentPosition
            scope.launch {
                if (position > 0) viewModel.persistPositionNow(position)
                val host = activity
                when {
                    host == null || !pipMode -> currentOnTakenOver()
                    byTaskId == host.taskId -> {
                        host.taskBaseIntent()?.let { base ->
                            runCatching {
                                host.startActivity(
                                    Intent(base).addFlags(
                                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                                    )
                                )
                            }
                        }
                        currentOnTakenOver()
                    }
                    else -> host.finish()
                }
            }
        }
    }
    DisposableEffect(Unit) { onDispose { viewModel.releaseOwner(playerOwner) } }

    // Keep the auto-enter predicate + params fresh with current playback state.
    LaunchedEffect(pipController, supportsPip, isPlaying, playbackError, ended, videoWidth, videoHeight) {
        pipController?.canEnterPip = { supportsPip && isPlaying && playbackError == null && !ended && ownsPlayer() }
        pipController?.buildParams = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                buildPipParams(context, videoWidth, videoHeight, isPlaying)
            else null
        }
    }
    // While in PiP, refresh the params so the action icon flips play↔pause.
    LaunchedEffect(pipMode, isPlaying) {
        if (pipMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity?.setPictureInPictureParams(
                buildPipParams(context, videoWidth, videoHeight, isPlaying)
            )
        }
    }

    // Sleep timer — session-local, wall-clock (independent of isPlaying). Minute modes
    // run a deadline countdown that pauses on elapse; End-of-video pauses when the file
    // finishes (suppressing the Ended overlay). No persistence — a per-sitting choice.
    var sleepMode by remember { mutableStateOf<SleepTimerMode>(SleepTimerMode.Off) }
    var sleepDeadline by remember { mutableLongStateOf(0L) }
    var sleepRemainingMs by remember { mutableLongStateOf(0L) }
    // Suppresses the Ended/replay overlay after an End-of-video timer fires.
    var sleepPausedByTimer by remember { mutableStateOf(false) }
    // Transient confirmation pill (arm / off / paused), on its own dismiss timer.
    var showSleepHud by remember { mutableStateOf(false) }
    var sleepHudLabel by remember { mutableStateOf("") }
    var sleepHudKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(sleepHudKey) {
        if (sleepHudKey > 0) {
            showSleepHud = true
            delay(1_200)
            showSleepHud = false
        }
    }

    fun setSleepTimer(mode: SleepTimerMode) {
        sleepMode = mode
        sleepPausedByTimer = false
        when (mode) {
            is SleepTimerMode.Minutes -> {
                sleepDeadline = SystemClock.elapsedRealtime() + mode.value * 60_000L
                sleepRemainingMs = mode.value * 60_000L
                sleepHudLabel = "Sleep timer: ${mode.value} min"
            }
            SleepTimerMode.EndOfVideo -> {
                sleepRemainingMs = 0L
                sleepHudLabel = "Sleep timer: end of video"
            }
            SleepTimerMode.Off -> {
                sleepRemainingMs = 0L
                sleepHudLabel = "Sleep timer off"
            }
        }
        sleepHudKey++
    }

    // Minute-mode countdown: tick the remaining time (for the overflow label) and pause
    // when the deadline passes. Reads sleepDeadline live, so re-arming extends it.
    LaunchedEffect(sleepMode) {
        if (sleepMode is SleepTimerMode.Minutes) {
            while (true) {
                val remaining = sleepDeadline - SystemClock.elapsedRealtime()
                sleepRemainingMs = remaining.coerceAtLeast(0L)
                if (remaining <= 0L) {
                    player.pause()
                    sleepHudLabel = "Paused by sleep timer"
                    sleepHudKey++
                    sleepMode = SleepTimerMode.Off
                    break
                }
                delay(1_000)
            }
        }
    }

    // End-of-video mode: when the file finishes, pause stays (STATE_ENDED) and we flash
    // the pill + suppress the replay overlay, then disarm.
    LaunchedEffect(ended) {
        if (ended && sleepMode is SleepTimerMode.EndOfVideo) {
            sleepPausedByTimer = true
            sleepHudLabel = "Paused by sleep timer"
            sleepHudKey++
            sleepMode = SleepTimerMode.Off
        }
    }

    // Self-describing overflow label: idle name, or the live remaining / mode when armed.
    val sleepTimerLabel = when (sleepMode) {
        SleepTimerMode.Off -> "Sleep timer"
        is SleepTimerMode.Minutes -> "Sleep timer: ${formatSleepRemaining(sleepRemainingMs)}"
        SleepTimerMode.EndOfVideo -> "Sleep timer: end of video"
    }

    // ── Options panel: repeat mode, share, and the tiles' live value labels ──
    var repeatMode by remember { mutableIntStateOf(player.repeatMode) }
    fun cycleRepeat() {
        val next = when (repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
        player.repeatMode = next
        repeatMode = next
    }
    val repeatLabel = when (repeatMode) {
        Player.REPEAT_MODE_ONE -> "One"
        Player.REPEAT_MODE_ALL -> "All"
        else -> "Off"
    }

    fun shareVideo() {
        runCatching {
            val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "video/*"
                putExtra(android.content.Intent.EXTRA_STREAM, android.net.Uri.parse(currentUri))
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(send, null))
        }
    }

    val panelValues = PlayerOptionsValues(
        speedLabel = run {
            val s = playbackSpeed
            (if (s == s.toLong().toFloat()) "%.1f".format(s) else s.toString()) + "×"
        },
        audioLabel = if (audioTracks.size >= 2)
            audioTracks.firstOrNull { it.selected }?.label ?: "Audio" else null,
        subtitlesLabel = if (tracks.groups.any { it.type == C.TRACK_TYPE_TEXT && it.isSelected }) "On" else "Off",
        aspectLabel = aspectMode.label,
        repeatLabel = repeatLabel,
        sleepLabel = when (val m = sleepMode) {
            SleepTimerMode.Off -> "Off"
            is SleepTimerMode.Minutes -> formatSleepRemaining(sleepRemainingMs)
            SleepTimerMode.EndOfVideo -> "End"
        },
        orientationLabel = orientationMode.label,
        backgroundOn = backgroundPlayback,
        isFavorite = isFavorite,
        canFavorite = canFavorite,
        hasNext = nextVideo != null,
        supportsPip = supportsPip,
        introLabel = if (upNext.showKey != null) {
            intro.window?.label() ?: if (introDetecting) "Finding…" else "Not set"
        } else null,
    )

    // Screenshot — clean-frame PixelCopy off the video SurfaceView (no chrome/subs),
    // saved to Pictures/VixPlay via MediaStore. Confirmed by a transient pill.
    var showScreenshotHud by remember { mutableStateOf(false) }
    var screenshotHudLabel by remember { mutableStateOf("") }
    // Non-null once a shot has saved, so the pill can offer Share.
    var lastScreenshotUri by remember { mutableStateOf<Uri?>(null) }
    var screenshotHudKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(screenshotHudKey) {
        if (screenshotHudKey > 0) {
            showScreenshotHud = true
            // A pill you're meant to tap needs long enough to notice and reach;
            // a plain failure message keeps the original quick flash.
            delay(if (lastScreenshotUri != null) 4_000 else 1_200)
            showScreenshotHud = false
        }
    }
    fun flashScreenshotHud(label: String, savedUri: Uri? = null) {
        screenshotHudLabel = label
        lastScreenshotUri = savedUri
        screenshotHudKey++
    }
    // Capture the surface, then compress + insert off the main thread.
    fun captureScreenshot() {
        val surface = playerView?.videoSurfaceView as? SurfaceView
        if (surface == null || surface.width == 0 || surface.height == 0) {
            flashScreenshotHud("Couldn't capture frame")
            return
        }
        val bitmap = Bitmap.createBitmap(surface.width, surface.height, Bitmap.Config.ARGB_8888)
        PixelCopy.request(
            surface,
            bitmap,
            { result ->
                if (result == PixelCopy.SUCCESS) {
                    scope.launch {
                        val saved = withContext(Dispatchers.IO) {
                            saveBitmapToGallery(context, bitmap, title)
                        }
                        if (saved != null) {
                            flashScreenshotHud("Screenshot saved", saved)
                        } else {
                            flashScreenshotHud("Couldn't save screenshot")
                        }
                    }
                } else {
                    flashScreenshotHud("Couldn't capture frame")
                }
            },
            Handler(Looper.getMainLooper()),
        )
    }
    // API 24–28 need runtime WRITE_EXTERNAL_STORAGE; 29+ writes via scoped storage.
    val screenshotPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) captureScreenshot() else flashScreenshotHud("Permission needed")
    }
    fun takeScreenshot() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            screenshotPermLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }
        captureScreenshot()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBackground)
            // Surface size, so pan can be clamped to the zoomed content's bounds.
            .onSizeChanged { containerSize = it }
            // Locked → LockOverlay owns all touches; ended → the Up Next overlay owns them;
            // optionsPanelOpen → the slide-out menu owns them (so its vertical scroll can't leak
            // into volume/brightness/seek). The surface gesture layer is inert in all three.
            // (The speed pill is intentionally NOT excluded: its slide-to-pick is driven by
            // this very gesture layer.)
            .playerGestures(
                enabled = !locked && !ended && !optionsPanelOpen,
                zoomed = zoomScale > 1f,
            ) { event ->
                when (event) {
                    GestureEvent.ToggleControls -> controlsVisible = !controlsVisible
                    is GestureEvent.DoubleTapSeek -> {
                        val side = event.side
                        // While zoomed, double-tap resets the zoom instead of seeking, so
                        // the familiar seek gesture is unchanged unless you've zoomed in.
                        if (zoomScale > 1f) {
                            zoomScale = 1f
                            zoomOffset = Offset.Zero
                        } else {
                            val skipMs = bindings.tapFor(side).skipMs
                            if (skipMs != 0L) {
                                val delta = if (side == SeekSide.LEFT) -skipMs else skipMs
                                val now = System.currentTimeMillis()
                                accumulatedSeekMs =
                                    if (lastTapSide == side && now - lastTapTime < 800L) {
                                        accumulatedSeekMs + delta
                                    } else {
                                        delta
                                    }
                                lastTapSide = side
                                lastTapTime = now
                                doubleTapSide = side
                                doubleTapKey++
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                player.seekTo((player.currentPosition + delta).coerceAtLeast(0L))
                            }
                        }
                    }
                    is GestureEvent.VerticalDrag -> {
                        when (bindings.dragFor(event.side)) {
                            DragAction.VOLUME -> {
                                volumeLevel = (volumeLevel + event.deltaFraction * maxVolume)
                                    .coerceIn(0f, maxVolume.toFloat())
                                audioManager.setStreamVolume(
                                    AudioManager.STREAM_MUSIC, volumeLevel.roundToInt(), 0,
                                )
                                hudKind = GestureHudKind.VOLUME
                                hudFraction = volumeLevel / maxVolume
                            }
                            DragAction.BRIGHTNESS -> {
                                brightness = (brightness + event.deltaFraction).coerceIn(0.01f, 1f)
                                activity?.window?.let { w ->
                                    w.attributes = w.attributes.also { it.screenBrightness = brightness }
                                }
                                hudKind = GestureHudKind.BRIGHTNESS
                                hudFraction = brightness
                            }
                            DragAction.NONE -> Unit
                        }
                    }
                    is GestureEvent.Zoom -> {
                        zoomScale = (zoomScale * event.scale).coerceIn(1f, 4f)
                        // Snap back to centre once fully zoomed out; otherwise re-clamp the
                        // existing offset to the new (smaller, when pinching out) bounds so
                        // the image can't stay stranded off-centre.
                        zoomOffset = if (zoomScale == 1f) Offset.Zero else clampPan(zoomOffset, zoomScale, containerSize)
                    }
                    is GestureEvent.Pan -> {
                        if (zoomScale > 1f) {
                            zoomOffset = clampPan(
                                Offset(zoomOffset.x + event.dx, zoomOffset.y + event.dy),
                                zoomScale,
                                containerSize,
                            )
                        }
                    }
                    GestureEvent.SeekScrubStart -> {
                        scrubStartPos = player.currentPosition
                        scrubTargetMs = scrubStartPos
                        scrubLastFraction = 0f
                        scrubPrecision = SeekPrecision.COARSE
                        scrubbing = true
                    }
                    is GestureEvent.SeekScrub -> {
                        // Accumulate incrementally rather than recomputing from the total,
                        // so changing tier mid-drag alters sensitivity going forward
                        // instead of snapping the target to a new mapping.
                        val tier = SeekPrecision.fromVerticalFraction(event.verticalFraction)
                        scrubPrecision = tier
                        val stepFraction = event.totalFraction - scrubLastFraction
                        scrubLastFraction = event.totalFraction
                        val dur = player.duration
                        val max = if (dur > 0) dur else Long.MAX_VALUE
                        scrubTargetMs = (scrubTargetMs + (stepFraction * tier.rangeMs).toLong())
                            .coerceIn(0L, max)
                    }
                    GestureEvent.SeekCommit -> {
                        if (scrubbing) {
                            player.seekTo(scrubTargetMs)
                            scrubbing = false
                        }
                    }
                    GestureEvent.SpeedHoldStart -> {
                        // Long-press opens the speed picker (replaces the old temporary-boost
                        // gesture); a slide then drives the dots, committing on release.
                        speedPillOpen = true
                        speedStartIndex = SPEED_PRESETS.indexOf(nearestSpeedPreset(playbackSpeed))
                        speedPreview = playbackSpeed
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    is GestureEvent.SpeedHoldChange -> {
                        // Map the horizontal slide to a dot; SPEED_DRAG_STEPS_PER_WIDTH sets
                        // how sensitive that mapping is (short drags switch dots).
                        val steps = (event.totalFraction * SPEED_DRAG_STEPS_PER_WIDTH).roundToInt()
                        val idx = (speedStartIndex + steps).coerceIn(0, SPEED_PRESETS.lastIndex)
                        val preset = SPEED_PRESETS[idx]
                        speedPreview = preset
                        player.setPlaybackSpeed(preset) // live preview, persisted on release
                    }
                    GestureEvent.SpeedHoldEnd -> {
                        // Commit the slid-to preset as the persisted default.
                        speedPreview?.let { viewModel.setPlaybackSpeed(it) }
                    }
                    GestureEvent.DragEnd -> hudDismissKey++
                }
            },
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    keepScreenOn = true
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = android.view.ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    playerView = this
                }
            },
            update = { it.resizeMode = aspectMode.resizeMode },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoomScale
                    scaleY = zoomScale
                    translationX = zoomOffset.x
                    translationY = zoomOffset.y
                },
            onRelease = { it.player = null; playerView = null },
        )

        // Push the persisted appearance onto Media3's SubtitleView; re-applied whenever
        // the style changes so edits from the sheet land on the live subtitles.
        LaunchedEffect(playerView, subtitleStyle) {
            playerView?.subtitleView?.applySubtitleStyle(subtitleStyle)
        }

        DoubleTapSeekOverlay(
            side = doubleTapSide,
            accumulatedMs = accumulatedSeekMs,
            triggerKey = doubleTapKey,
            modifier = Modifier.fillMaxSize(),
        )

        GestureHud(
            kind = hudKind,
            fraction = hudFraction,
            modifier = Modifier.fillMaxSize(),
        )

        // Surface-gesture scrub: frame + timecode + delta + active precision tier.
        AnimatedVisibility(
            visible = scrubbing,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            ScrubPreview(
                frame = scrubFrame,
                targetMs = scrubTargetMs,
                deltaMs = scrubTargetMs - scrubStartPos,
                tierLabel = scrubPrecision.label,
            )
        }

        // Inline over-video speed picker (replaces the old bottom sheet). Gated like the
        // other player surfaces; freezes the controls auto-hide via controlsHeldOpen.
        SpeedControlPill(
            visible = speedPillOpen && !locked && !pipMode && playbackError == null,
            currentSpeed = speedPreview ?: playbackSpeed,
            onSelect = { viewModel.setPlaybackSpeed(it) },
            onDismiss = { speedPillOpen = false; speedPreview = null },
            modifier = Modifier.fillMaxSize(),
        )

        // Cycled in-place from the panel, where the tile already shows the new value — so the
        // confirmation toast is suppressed while the panel is open (it would overlap it).
        AspectHud(
            visible = showAspectHud && !optionsPanelOpen,
            mode = aspectMode,
            modifier = Modifier.fillMaxSize(),
        )

        OrientationHud(
            visible = showOrientationHud && !optionsPanelOpen,
            mode = orientationMode,
            modifier = Modifier.fillMaxSize(),
        )

        SleepTimerHud(
            visible = showSleepHud,
            label = sleepHudLabel,
            modifier = Modifier.fillMaxSize(),
        )

        ScreenshotHud(
            visible = showScreenshotHud,
            label = screenshotHudLabel,
            actionLabel = lastScreenshotUri?.let { "Share" },
            onAction = lastScreenshotUri?.let { uri ->
                {
                    showScreenshotHud = false
                    shareScreenshot(context, uri)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Sync adjust — interactive, so it renders above the gesture layer. Hidden in
        // PiP and while locked, like the rest of the chrome.
        SubtitleSyncHud(
            visible = showSyncHud && !locked && !pipMode && playbackError == null,
            offsetMs = subtitleOffsetMs,
            onStep = { stepSubtitleOffset(it) },
            onReset = { if (subtitleOffsetMs != 0L) stepSubtitleOffset(-subtitleOffsetMs) },
            onDone = { showSyncHud = false },
            modifier = Modifier.fillMaxSize(),
        )

        // Skip intro: the marking panel (interactive, like sync) and the skip pill.
        val showKey = upNext.showKey
        IntroMarkHud(
            visible = showIntroHud && showKey != null && !locked && !pipMode && playbackError == null,
            positionMs = introPositionMs,
            startMs = markStartMs,
            endMs = markEndMs,
            error = introMarkError(markStartMs, markEndMs, player.duration),
            existing = intro.source,
            detecting = introDetecting,
            autoSkip = intro.autoSkip,
            onSetStart = { markStartMs = player.currentPosition },
            onSetEnd = { markEndMs = player.currentPosition },
            onSaveForShow = { saveIntroMarks(episodeOnly = false) },
            onSaveForEpisode = { saveIntroMarks(episodeOnly = true) },
            onRemove = {
                val source = intro.source ?: return@IntroMarkHud
                scope.launch {
                    showKey?.let { viewModel.removeIntro(it, source) }
                    Analytics.log(context, Analytics.INTRO_MARKED, "scope" to "removed")
                    reloadIntro()
                    markStartMs = intro.window?.startMs
                    markEndMs = intro.window?.endMs
                }
            },
            onToggleAutoSkip = {
                scope.launch {
                    showKey?.let { viewModel.setIntroAutoSkip(it, !intro.autoSkip) }
                    reloadIntro()
                }
            },
            onClose = { showIntroHud = false },
            modifier = Modifier.fillMaxSize(),
        )
        val introChromeAllowed = !showIntroHud && !locked && !pipMode && playbackError == null && !ended
        val offerSkip = intro.window?.offersSkipAt(introPositionMs) == true && !showIntroSkipped
        SkipIntroPill(
            visible = introChromeAllowed && (offerSkip || showIntroSkipped),
            label = if (showIntroSkipped) "Intro skipped" else "Skip intro",
            actionLabel = if (showIntroSkipped) "Watch" else null,
            onClick = {
                val window = intro.window ?: return@SkipIntroPill
                if (showIntroSkipped) {
                    showIntroSkipped = false
                    player.seekTo(window.startMs)
                } else {
                    player.seekTo(window.endMs)
                    Analytics.log(context, Analytics.INTRO_SKIP, "source" to "button")
                }
                introPositionMs = player.currentPosition
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Buffering spinner — hidden once the first frame is ready or on error.
        if (isBuffering && playbackError == null && !pipMode) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OnScrim)
            }
        }

        PlayerHud(
            player = player,
            // Hidden once the video ends (Up Next owns the surface) and whenever any exclusive
            // overlay is active (panel/sheet/speed pill/gesture slider/scrub) — only the
            // actively-used control shows.
            visible = controlsVisible && !locked && playbackError == null && !pipMode &&
                !ended && !exclusiveOverlayActive,
            title = title,
            skin = skin,
            onBack = onBack,
            aspectMode = aspectMode,
            orientationLabel = orientationMode.label,
            sleepTimerLabel = sleepTimerLabel,
            // Shown whenever the clip carries audio — the top-right audio-track control. The
            // picker lists the track(s) with language/label; switching needs 2+ tracks.
            showAudioTrack = audioTracks.isNotEmpty(),
            showPip = supportsPip,
            onCycleAspect = {
                aspectMode = aspectMode.next()
                aspectHudKey++
            },
            onCycleOrientation = {
                orientationMode = orientationMode.next()
                orientationHudKey++
            },
            onSpeedClick = { speedPillOpen = true },
            // Non-1× speed shown loud in the HUD so a stray boost is never silently stuck.
            speedLabel = playbackSpeed.takeIf { it != 1f }?.let { formatSpeed(it) },
            onSubtitlesClick = { currentSheet = PlayerSheet.Subtitles },
            onAudioClick = { currentSheet = PlayerSheet.Audio },
            onEnterPip = { enterPip() },
            onSleepTimerClick = { currentSheet = PlayerSheet.SleepTimer },
            onScreenshotClick = { takeScreenshot() },
            onFileInfoClick = { currentSheet = PlayerSheet.FileInfo },
            onLockClick = { locked = true; controlsVisible = false },
            onMoreClick = { optionsPanelOpen = true },
            onScrubbingChange = { seekBarDragging = it },
            frameLoader = { thumbnails.frameAt(it) },
            modifier = Modifier.fillMaxSize(),
        )

        // Gesture-lock surface — only the unlock chip is live.
        if (locked && playbackError == null) {
            LockOverlay(
                onUnlock = { locked = false; controlsVisible = true },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Transient resume affordance — sits above the HUD controls, taps fall through.
        AnimatedVisibility(
            visible = showResumeChip && playbackError == null && !pipMode && !locked && !exclusiveOverlayActive,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 96.dp),
        ) {
            ResumeChip(
                positionMs = resumePos,
                onRestart = {
                    player.seekTo(0L)
                    showResumeChip = false
                },
            )
        }

        // Terminal finished state — Up Next with autoplay/confirm per the preference matrix.
        // Not after an undecodable video: the "can't play" overlay owns that end state.
        if (ended && playbackError == null && unsupportedVideoMime == null && !pipMode && !sleepPausedByTimer) {
            UpNextOverlay(
                currentUri = currentUri,
                upcoming = upNext.upcoming,
                next = nextVideo,
                positionLabel = upNext.positionLabel,
                showTitle = upNext.showTitle,
                episodeCodes = upNext.codes,
                autoplayEnabled = autoplayNext,
                confirmMode = confirmMode && !autoplaySuppressed,
                countdownSec = autoCountdown,
                onToggleAutoplay = { viewModel.setAutoplayNext(it) },
                onReplay = {
                    logUpNext("replay")
                    player.seekTo(0L)
                    player.playWhenReady = true
                    ended = false
                    autoplaySuppressed = false
                },
                onPlay = { video, dontAsk ->
                    // Don't-ask + Play Next → remember autoplay ON for next time.
                    if (dontAsk) viewModel.setAutoplayNext(true)
                    logUpNext(if (video == nextVideo) "play_next" else "pick")
                    playNext(video)
                },
                onNotNow = { dontAsk ->
                    logUpNext(if (autoCountdown != null) "cancel" else "not_now")
                    // Don't-ask + Not Now → autoplay OFF and stop prompting.
                    if (dontAsk) {
                        viewModel.setAutoplayNext(false)
                        viewModel.setAskBeforePlayingNext(false)
                    }
                    autoplaySuppressed = true
                    autoCountdown = null
                },
                onClose = {
                    logUpNext("close")
                    onBack()
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // A video track with no decoder is terminal too, but arrives via the track list
        // rather than onPlayerError (ExoPlayer would otherwise play audio over a black frame).
        if (playbackError == null) unsupportedVideoMime?.let { mime ->
            ErrorOverlay(
                message = "This device can't decode ${prettyCodec(mime)} video.",
                title = "Can't play this video",
                onBack = onBack,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Terminal error state — actionable, never a bare crash. Network failures are the
        // expected case for streamed URLs, so they get a recoverable message + Retry;
        // codec failures keep the informative code string with no (futile) retry.
        playbackError?.let { error ->
            val networkMessage = networkErrorMessageFor(error)
            ErrorOverlay(
                message = networkMessage ?: "Codec: ${error.errorCodeName}",
                title = if (networkMessage != null) "Can't play this stream" else "Can't play this video",
                onBack = onBack,
                onRetry = if (networkMessage != null) {
                    { playbackError = null; player.prepare() }
                } else null,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Above the Up Next / error overlays on purpose: a short clip with undecodable audio
        // can reach its end before the 4 s pill is done, and the message must still be seen.
        UnsupportedCodecHud(
            visible = showCodecHud && !pipMode && !optionsPanelOpen,
            label = codecHudLabel,
            modifier = Modifier.fillMaxSize(),
        )

        // Slide-out options panel (opened by the top-bar ⋮) — the new overflow surface.
        PlayerOptionsPanel(
            visible = optionsPanelOpen,
            values = panelValues,
            onDismiss = { optionsPanelOpen = false },
            onSpeed = { optionsPanelOpen = false; speedPillOpen = true },
            onAudio = { currentSheet = PlayerSheet.Audio },
            onSubtitles = { currentSheet = PlayerSheet.Subtitles },
            onAspect = { aspectMode = aspectMode.next(); aspectHudKey++ },
            onRepeat = { cycleRepeat() },
            onSleepTimer = { currentSheet = PlayerSheet.SleepTimer },
            onLock = { locked = true; controlsVisible = false },
            onBackgroundPlay = { viewModel.setBackgroundPlayback(!backgroundPlayback) },
            onOrientation = { orientationMode = orientationMode.next(); orientationHudKey++ },
            onScreenshot = { takeScreenshot() },
            onVideoInfo = { currentSheet = PlayerSheet.FileInfo },
            onEqualizer = onOpenEqualizer,
            onPip = { enterPip() },
            onFavorite = { viewModel.toggleFavorite() },
            onPlayNext = { nextVideo?.let { playNext(it) } },
            onShare = { shareVideo() },
            onIntro = {
                markStartMs = intro.window?.startMs
                markEndMs = intro.window?.endMs
                showIntroHud = true
            },
            modifier = Modifier.fillMaxSize(),
        )
    }

    currentSheet?.let { sheet ->
        ModalBottomSheet(
            onDismissRequest = { currentSheet = null },
            sheetState = sheetState,
        ) {
            when (sheet) {
                is PlayerSheet.Subtitles -> SubtitleSheet(
                    tracks = subtitleTracks,
                    subtitlesDisabled = subtitlesDisabled,
                    // A pick is remembered for the show, so its next episodes start the same way.
                    onSelect = { id ->
                        selectSubtitle(id)
                        upNext.showKey?.let { viewModel.rememberSubtitles(it, on = true, language = tracks.languageOf(id)) }
                    },
                    onDisableSubtitles = {
                        disableSubtitles()
                        upNext.showKey?.let { viewModel.rememberSubtitles(it, on = false, language = null) }
                    },
                    onLoadFromFile = { subtitlePicker.launch(arrayOf("*/*")) },
                    offsetMs = subtitleOffsetMs,
                    onSyncOffset = {
                        currentSheet = null
                        showSyncHud = true
                    },
                    onStyle = { currentSheet = PlayerSheet.SubtitleStyle },
                )
                is PlayerSheet.SubtitleStyle -> SubtitleStyleEditor(
                    style = subtitleStyle,
                    onChange = { viewModel.setSubtitleStyle(it) },
                    // The real subtitles on the video are a better preview than a sample.
                    showPreview = false,
                )
                is PlayerSheet.Audio     -> AudioTrackSheet(
                    tracks = audioTracks,
                    onSelect = { id ->
                        selectAudioTrack(id)
                        upNext.showKey?.let { key -> tracks.languageOf(id)?.let { viewModel.rememberAudio(key, it) } }
                    },
                )
                is PlayerSheet.SleepTimer -> SleepTimerSheet(
                    activeMode = sleepMode,
                    onSelect = { setSleepTimer(it) },
                )
                is PlayerSheet.Cast      -> Text("Cast — Step 6")
                is PlayerSheet.FileInfo  -> FileInfoSheet(info = fileInfo)
            }
        }
    }
}

/** "14m left" for a minute-scale remainder, "45s left" under a minute. */
private fun formatSleepRemaining(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    return if (totalSec >= 60) "${(totalSec + 59) / 60}m left" else "${totalSec}s left"
}

/**
 * Clamps a pan [offset] so the content scaled by [scale] about the centre always covers the
 * viewport [size] — i.e. panning stops at the zoomed image's edges and never reveals a gap.
 * At scale s the content overhangs each side by (s-1)/2 of the dimension.
 */
private fun clampPan(offset: Offset, scale: Float, size: IntSize): Offset {
    val maxX = (scale - 1f) * size.width / 2f
    val maxY = (scale - 1f) * size.height / 2f
    return Offset(
        offset.x.coerceIn(-maxX, maxX),
        offset.y.coerceIn(-maxY, maxY),
    )
}

/**
 * Writes [bitmap] as a PNG into Pictures/VixPlay via MediaStore, gallery-visible.
 * On API 29+ uses the IS_PENDING scoped-storage flow (no permission); on 24–28 the
 * caller has already secured WRITE_EXTERNAL_STORAGE.
 *
 * Returns the new item's URI so it can be shared, or null on any failure — sharing must
 * hand over the file that actually landed in the gallery, not a second capture.
 */
private fun saveBitmapToGallery(context: Context, bitmap: Bitmap, title: String): Uri? {
    val safeTitle = title.replace(Regex("[^A-Za-z0-9._-]"), "_").take(40).ifBlank { "video" }
    val name = "VixPlay_${safeTitle}_${System.currentTimeMillis()}.png"
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, name)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/VixPlay")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
    return try {
        resolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        } ?: return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        uri
    } catch (e: Exception) {
        resolver.delete(uri, null, null)
        null
    }
}

/** Hands the saved shot to the system chooser, which is itself the confirmation step. */
private fun shareScreenshot(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share screenshot"))
}

/** Writes the current position to the fast MMKV resume path, guarded against 0. */
private fun flushPositionFast(player: ExoPlayer, viewModel: PlayerViewModel) {
    val pos = player.currentPosition
    if (pos > 0) viewModel.savePositionFast(pos)
}

/** Broadcast action for the in-PiP play/pause RemoteAction. */
private const val ACTION_PIP_TOGGLE = "com.devbytes.vixplayer.app.action.PIP_TOGGLE"

/** The intent that started this activity's task (its root), or null if the task isn't found. */
private fun Activity.taskBaseIntent(): Intent? =
    getSystemService(ActivityManager::class.java)?.appTasks?.firstOrNull { task ->
        val info = task.taskInfo
        val id = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) info.taskId else @Suppress("DEPRECATION") info.persistentId
        id == taskId
    }?.taskInfo?.baseIntent

/**
 * PiP params: the video's aspect ratio (clamped to Android's allowed PiP range) plus a
 * single play/pause RemoteAction — the only control that works in the tiny window.
 */
@androidx.annotation.RequiresApi(Build.VERSION_CODES.O)
private fun buildPipParams(
    context: Context,
    width: Int,
    height: Int,
    isPlaying: Boolean,
): PictureInPictureParams {
    val ratio = if (width > 0 && height > 0) {
        val value = width.toFloat() / height
        when {
            value < 0.4185f -> Rational(100, 239)
            value > 2.39f -> Rational(239, 100)
            else -> Rational(width, height)
        }
    } else {
        Rational(16, 9)
    }
    val icon = Icon.createWithResource(
        context, if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
    )
    val label = if (isPlaying) "Pause" else "Play"
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(ACTION_PIP_TOGGLE).setPackage(context.packageName),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    return PictureInPictureParams.Builder()
        .setAspectRatio(ratio)
        .setActions(listOf(RemoteAction(icon, label, label, pendingIntent)))
        .build()
}

/** Best-effort MIME from a subtitle filename's extension; defaults to SubRip. */
private fun subtitleMimeFor(name: String): String =
    when (name.substringAfterLast('.', "").lowercase()) {
        "vtt" -> MimeTypes.TEXT_VTT
        "ssa", "ass" -> MimeTypes.TEXT_SSA
        "ttml", "xml", "dfxp" -> MimeTypes.APPLICATION_TTML
        else -> MimeTypes.APPLICATION_SUBRIP
    }

private fun queryDisplayName(context: Context, uri: Uri): String? =
    context.contentResolver
        .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { if (it.moveToFirst()) it.getString(0) else null }

/**
 * Maps a recoverable network/IO [PlaybackException] to a user-facing message, or null for
 * anything else (codec/decoder errors, where re-preparing won't help). A non-null result
 * is the signal that the error overlay should offer Retry.
 */
private fun networkErrorMessageFor(error: PlaybackException): String? =
    when (error.errorCode) {
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
        PlaybackException.ERROR_CODE_IO_NO_PERMISSION,
        PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED ->
            "Can't reach the server. Check your connection and the URL."
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
            "Access denied or not found. Check the URL or credentials."
        PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
            "This link doesn't point to a playable stream."
        else -> null
    }

/** Unwraps the host Activity from a (possibly wrapped) Compose context. */
private fun Context.findActivity(): Activity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
