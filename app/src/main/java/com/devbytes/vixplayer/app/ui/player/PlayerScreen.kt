package com.devbytes.vixplayer.app.ui.player

import android.app.Activity
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
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
import com.devbytes.vixplayer.app.data.repository.VideoFile
import com.devbytes.vixplayer.app.player.PlaybackService
import com.devbytes.vixplayer.app.player.gesture.GestureEvent
import com.devbytes.vixplayer.app.player.gesture.SeekPrecision
import com.devbytes.vixplayer.app.player.gesture.SeekSide
import com.devbytes.vixplayer.app.player.gesture.playerGestures
import com.devbytes.vixplayer.app.player.thumbnail.ThumbnailProvider
import com.devbytes.vixplayer.app.ui.player.components.AspectHud
import com.devbytes.vixplayer.app.ui.player.components.AspectMode
import com.devbytes.vixplayer.app.ui.player.components.AudioTrackSheet
import com.devbytes.vixplayer.app.ui.player.components.AudioTrackUi
import com.devbytes.vixplayer.app.ui.player.components.DoubleTapSeekOverlay
import com.devbytes.vixplayer.app.ui.player.components.EndedOverlay
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
import com.devbytes.vixplayer.app.ui.player.components.SleepTimerMode
import com.devbytes.vixplayer.app.ui.player.components.SleepTimerSheet
import com.devbytes.vixplayer.app.ui.player.components.SpeedHud
import com.devbytes.vixplayer.app.ui.player.components.SpeedSheet
import com.devbytes.vixplayer.app.ui.player.components.ResumeChip
import com.devbytes.vixplayer.app.ui.player.components.SubtitleSheet
import com.devbytes.vixplayer.app.ui.player.components.SubtitleStyleEditor
import com.devbytes.vixplayer.app.ui.player.components.SubtitleSyncHud
import com.devbytes.vixplayer.app.ui.player.components.applySubtitleStyle
import com.devbytes.vixplayer.app.ui.player.components.SubtitleTrackUi
import com.devbytes.vixplayer.app.ui.theme.AmoledBackground
import com.devbytes.vixplayer.app.ui.theme.OnScrim
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    uri: String,
    onBack: () -> Unit,
    skin: PlayerSkin = PlayerSkin.Classic,
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

    // Long-press speed-hold (drag-to-vary), restored on release.
    var speedHolding by remember { mutableStateOf(false) }
    var savedSpeed by remember { mutableFloatStateOf(1f) }
    var speedMultiplier by remember { mutableFloatStateOf(1f) }
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

    // Declared early: the ON_STOP background-pause rule below must consult it, and PiP is
    // the one case where backgrounding must NOT pause (the video is still on screen).
    var pipMode by remember { mutableStateOf(false) }

    // App-scoped player (owned by PlayerController so playback can outlive this Activity).
    // Never released here — only prepared, and paused on exit when appropriate.
    val player = viewModel.player
    val backgroundPlayback by viewModel.backgroundPlayback.collectAsState()
    val subtitleStyle by viewModel.subtitleStyle.collectAsState()

    // Opening a video is one call so per-file state can't leak from the previous one.
    LaunchedEffect(Unit) { viewModel.prepareFor(uri, subtitleOffsetMs) }

    // Host the MediaSession while the player screen is live, so the notification and
    // lock-screen controls exist. The service self-stops once nothing is playing.
    LaunchedEffect(Unit) {
        context.startService(Intent(context, PlaybackService::class.java))
    }

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
            viewModel.savePositionFast(player.currentPosition)
        }
    }

    // Authoritative playback state, driven by the player itself.
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var isBuffering by remember { mutableStateOf(player.playbackState == Player.STATE_BUFFERING) }
    var playbackError by remember { mutableStateOf<PlaybackException?>(player.playerError) }
    // Video dimensions — drive screen orientation to the video's shape.
    var videoWidth by remember { mutableIntStateOf(player.videoSize.width) }
    var videoHeight by remember { mutableIntStateOf(player.videoSize.height) }

    // Terminal "finished" state (STATE_ENDED) + the folder-next candidate (null when
    // there's no next). Computed once the video ends; drives the EndedOverlay.
    var ended by remember { mutableStateOf(player.playbackState == Player.STATE_ENDED) }
    var nextVideo by remember { mutableStateOf<VideoFile?>(null) }
    LaunchedEffect(ended) {
        nextVideo = if (ended) viewModel.nextInFolder() else null
    }

    // Persisted global playback speed — the stored value drives the player on load
    // and on every pick. The speed-hold gesture reverts to this (reads player speed).
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    LaunchedEffect(playbackSpeed) { player.setPlaybackSpeed(playbackSpeed) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                // Pausing flushes the resume point immediately (steady-state 5s tick
                // is too coarse for a pause-then-exit).
                if (!playing) flushPositionFast(player, viewModel)
            }

            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                ended = state == Player.STATE_ENDED
            }

            override fun onPlayerError(error: PlaybackException) {
                playbackError = error
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
                if (reason == Player.DISCONTINUITY_REASON_SEEK) {
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
            if (event == Lifecycle.Event.ON_STOP) {
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
            viewModel.persistPosition(player.currentPosition)
            player.stop()
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
    var menuOpen by remember { mutableStateOf(false) }
    var seekBarDragging by remember { mutableStateOf(false) }
    val sheetOpen = currentSheet != null
    val controlsHeldOpen = sheetOpen || menuOpen || seekBarDragging
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

    val title = remember(currentUri) {
        currentUri.substringAfterLast('/').substringBeforeLast('.').ifBlank { "Now Playing" }
    }

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
        viewModel.prepareFor(newUri, subtitleOffsetMs)
        showResumeChip = false
        ended = false
        nextVideo = null
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
            )
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
    // Keep the auto-enter predicate + params fresh with current playback state.
    LaunchedEffect(pipController, supportsPip, isPlaying, playbackError, ended, videoWidth, videoHeight) {
        pipController?.canEnterPip = { supportsPip && isPlaying && playbackError == null && !ended }
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
            // Locked → LockOverlay owns all touches; the surface below is inert.
            .playerGestures(enabled = !locked) { event ->
                when (event) {
                    GestureEvent.ToggleControls -> controlsVisible = !controlsVisible
                    is GestureEvent.DoubleTapSeek -> {
                        val side = event.side
                        val delta = if (side == SeekSide.LEFT) -10_000L else 10_000L
                        val now = System.currentTimeMillis()
                        if (lastTapSide == side && now - lastTapTime < 800L) {
                            accumulatedSeekMs += delta
                        } else {
                            accumulatedSeekMs = delta
                        }
                        lastTapSide = side
                        lastTapTime = now
                        doubleTapSide = side
                        doubleTapKey++
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val newPos = (player.currentPosition + delta).coerceAtLeast(0L)
                        player.seekTo(newPos)
                    }
                    is GestureEvent.VolumeDrag -> {
                        volumeLevel = (volumeLevel + event.deltaFraction * maxVolume)
                            .coerceIn(0f, maxVolume.toFloat())
                        audioManager.setStreamVolume(
                            AudioManager.STREAM_MUSIC, volumeLevel.roundToInt(), 0,
                        )
                        hudKind = GestureHudKind.VOLUME
                        hudFraction = volumeLevel / maxVolume
                    }
                    is GestureEvent.BrightnessDrag -> {
                        brightness = (brightness + event.deltaFraction).coerceIn(0.01f, 1f)
                        activity?.window?.let { w ->
                            w.attributes = w.attributes.also { it.screenBrightness = brightness }
                        }
                        hudKind = GestureHudKind.BRIGHTNESS
                        hudFraction = brightness
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
                        savedSpeed = player.playbackParameters.speed
                        speedMultiplier = 2f
                        player.setPlaybackSpeed(2f)
                        speedHolding = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    is GestureEvent.SpeedHoldChange -> {
                        speedMultiplier = (2f + event.totalFraction * 3f).coerceIn(1f, 4f)
                        player.setPlaybackSpeed(speedMultiplier)
                    }
                    GestureEvent.SpeedHoldEnd -> {
                        player.setPlaybackSpeed(savedSpeed)
                        speedHolding = false
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
            modifier = Modifier.fillMaxSize(),
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

        SpeedHud(
            visible = speedHolding,
            multiplier = speedMultiplier,
            modifier = Modifier.fillMaxSize(),
        )

        AspectHud(
            visible = showAspectHud,
            mode = aspectMode,
            modifier = Modifier.fillMaxSize(),
        )

        OrientationHud(
            visible = showOrientationHud,
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

        // Buffering spinner — hidden once the first frame is ready or on error.
        if (isBuffering && playbackError == null && !pipMode) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OnScrim)
            }
        }

        PlayerHud(
            player = player,
            visible = controlsVisible && !locked && playbackError == null && !pipMode,
            title = title,
            skin = skin,
            onBack = onBack,
            aspectMode = aspectMode,
            orientationLabel = orientationMode.label,
            sleepTimerLabel = sleepTimerLabel,
            showAudioTrack = audioTracks.size >= 2,
            showPip = supportsPip,
            onCycleAspect = {
                aspectMode = aspectMode.next()
                aspectHudKey++
            },
            onCycleOrientation = {
                orientationMode = orientationMode.next()
                orientationHudKey++
            },
            onSpeedClick = { currentSheet = PlayerSheet.Speed },
            onSubtitlesClick = { currentSheet = PlayerSheet.Subtitles },
            onAudioClick = { currentSheet = PlayerSheet.Audio },
            onEnterPip = { enterPip() },
            onSleepTimerClick = { currentSheet = PlayerSheet.SleepTimer },
            onScreenshotClick = { takeScreenshot() },
            onFileInfoClick = { currentSheet = PlayerSheet.FileInfo },
            onLockClick = { locked = true; controlsVisible = false },
            onMenuOpenChange = { menuOpen = it },
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
            visible = showResumeChip && playbackError == null && !pipMode,
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

        // Terminal finished state — Replay always, Play next only when a folder-next
        // exists; no silent auto-advance.
        if (ended && playbackError == null && !pipMode && !sleepPausedByTimer) {
            EndedOverlay(
                hasNext = nextVideo != null,
                onReplay = {
                    player.seekTo(0L)
                    player.playWhenReady = true
                    ended = false
                },
                onPlayNext = { nextVideo?.let { playNext(it) } },
                onBack = onBack,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Terminal error state — actionable, never a bare crash.
        playbackError?.let { error ->
            ErrorOverlay(
                message = "Codec: ${error.errorCodeName}",
                onBack = onBack,
                modifier = Modifier.fillMaxSize(),
            )
        }
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
                    onSelect = { selectSubtitle(it) },
                    onDisableSubtitles = { disableSubtitles() },
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
                    onSelect = { selectAudioTrack(it) },
                )
                is PlayerSheet.Speed     -> SpeedSheet(
                    currentSpeed = playbackSpeed,
                    onSelect = { viewModel.setPlaybackSpeed(it) },
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

/** Unwraps the host Activity from a (possibly wrapped) Compose context. */
private fun Context.findActivity(): Activity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
