package com.devbytes.vixplayer.app.ui.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.ui.common.LocalIsTv
import com.devbytes.vixplayer.app.ui.common.TvSafeHorizontal
import com.devbytes.vixplayer.app.player.CreditsSource
import com.devbytes.vixplayer.app.player.IntroSource
import com.devbytes.vixplayer.app.player.formatIntroTime
import com.devbytes.vixplayer.app.ui.theme.AmoledBackground
import com.devbytes.vixplayer.app.ui.theme.DarkAccent
import com.devbytes.vixplayer.app.ui.theme.OnScrim
import com.devbytes.vixplayer.app.ui.theme.ScrimStrong
import com.devbytes.vixplayer.app.ui.theme.TimecodeStyle

/**
 * Marks a show's intro *over the video*, like the subtitle-sync panel: play or scrub to where
 * the intro starts and set it, then to where it ends. Saved for the whole show (its other
 * episodes then offer "Skip intro") or for this episode only (a longer cold open, say).
 *
 * Interactive, so it sits above the gesture layer; taps outside it fall through to the video.
 */
@Composable
fun IntroMarkHud(
    visible: Boolean,
    positionMs: Long,
    startMs: Long?,
    endMs: Long?,
    /** Why the marks can't be saved yet; null when they can. */
    error: String?,
    /** Where the intro currently in use came from; null when none is marked. */
    existing: IntroSource?,
    /** A scan of this season for its intro is under way. */
    detecting: Boolean,
    autoSkip: Boolean,
    onSetStart: () -> Unit,
    onSetEnd: () -> Unit,
    onSaveForShow: () -> Unit,
    onSaveForEpisode: () -> Unit,
    onRemove: () -> Unit,
    onToggleAutoSkip: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 104.dp)
                    .widthIn(max = 460.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(ScrimStrong)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Mark intro", style = MaterialTheme.typography.labelMedium, color = OnScrim.copy(alpha = 0.7f))
                val origin = when (existing) {
                    IntroSource.DETECTED -> "Found automatically"
                    IntroSource.SHOW -> "Marked for this show"
                    IntroSource.EPISODE -> "Marked for this episode"
                    null -> if (detecting) "Looking for the intro in this season…" else null
                }
                if (origin != null) {
                    Text(origin, style = MaterialTheme.typography.labelSmall, color = DarkAccent)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MarkColumn("Starts", startMs, "Set to ${formatIntroTime(positionMs)}", onSetStart)
                    MarkColumn("Ends", endMs, "Set to ${formatIntroTime(positionMs)}", onSetEnd)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    error ?: if (existing == IntroSource.DETECTED) "Found in this episode's audio — set new times to correct it"
                    else "Episodes of this show will offer “Skip intro” here",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnScrim.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HudButton("Save for show", enabled = error == null, accent = true, onClick = onSaveForShow)
                    HudButton("This episode only", enabled = error == null, onClick = onSaveForEpisode)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HudButton(if (autoSkip) "Auto-skip: On" else "Auto-skip: Off", onClick = onToggleAutoSkip)
                    if (existing != null) {
                        HudButton(
                            when (existing) {
                                IntroSource.EPISODE -> "Remove for episode"
                                IntroSource.DETECTED -> "Not the intro"
                                IntroSource.SHOW -> "Remove"
                            },
                            onClick = onRemove,
                        )
                    }
                    HudButton("Close", onClick = onClose)
                }
            }
        }
    }
}

/**
 * Marks where an episode's end credits start, over the video like [IntroMarkHud] — for shows
 * where detection found none, or found the wrong place. Saved for the whole show (as time
 * before the end, so episodes of other lengths line up) or for this episode only.
 */
@Composable
fun CreditsMarkHud(
    visible: Boolean,
    positionMs: Long,
    startMs: Long?,
    /** Why the mark can't be saved yet; null when it can. */
    error: String?,
    /** Where the credits currently in use came from; null when there are none. */
    existing: CreditsSource?,
    /** A scan of this season is under way. */
    detecting: Boolean,
    onSetStart: () -> Unit,
    onSaveForShow: () -> Unit,
    onSaveForEpisode: () -> Unit,
    onRemove: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 104.dp)
                    .widthIn(max = 460.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(ScrimStrong)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Mark end credits", style = MaterialTheme.typography.labelMedium, color = OnScrim.copy(alpha = 0.7f))
                val origin = when (existing) {
                    CreditsSource.DETECTED -> "Found automatically"
                    CreditsSource.SHOW -> "Marked for this show"
                    CreditsSource.EPISODE -> "Marked for this episode"
                    null -> if (detecting) "Looking for the credits in this season…" else null
                }
                if (origin != null) {
                    Text(origin, style = MaterialTheme.typography.labelSmall, color = DarkAccent)
                }
                Spacer(Modifier.height(10.dp))
                MarkColumn("Credits start", startMs, "Set to ${formatIntroTime(positionMs)}", onSetStart)
                Spacer(Modifier.height(8.dp))
                Text(
                    error ?: if (existing == CreditsSource.DETECTED) "Found in this episode's audio — set a new time to correct it"
                    else "Episodes of this show will offer “Next episode” here",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnScrim.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HudButton("Save for show", enabled = error == null, accent = true, onClick = onSaveForShow)
                    HudButton("This episode only", enabled = error == null, onClick = onSaveForEpisode)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (existing != null) {
                        HudButton(
                            when (existing) {
                                CreditsSource.EPISODE -> "Remove for episode"
                                CreditsSource.DETECTED -> "Not the credits"
                                CreditsSource.SHOW -> "Remove"
                            },
                            onClick = onRemove,
                        )
                    }
                    HudButton("Close", onClick = onClose)
                }
            }
        }
    }
}

@Composable
private fun MarkColumn(label: String, valueMs: Long?, action: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(150.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = OnScrim.copy(alpha = 0.6f))
        Text(
            valueMs?.let { formatIntroTime(it) } ?: "—",
            style = TimecodeStyle,
            color = OnScrim,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        HudButton(action, accent = true, outlined = true, onClick = onClick)
    }
}

@Composable
private fun HudButton(
    label: String,
    enabled: Boolean = true,
    accent: Boolean = false,
    outlined: Boolean = false,
    onClick: () -> Unit,
) {
    val color = when {
        !enabled -> OnScrim.copy(alpha = 0.3f)
        accent -> DarkAccent
        else -> OnScrim.copy(alpha = 0.8f)
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = color,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .then(if (outlined) Modifier.border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(50)) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

/**
 * "Skip intro" — shown while the playhead is inside a marked intro, bottom-right above the
 * seek bar like streaming apps. After an automatic skip the same pill says "Intro skipped"
 * with a "Watch" action that goes back to it.
 */
@Composable
fun SkipIntroPill(
    visible: Boolean,
    label: String,
    actionLabel: String?,
    onClick: () -> Unit,
    /** Lets the player hand the pill remote focus when it appears (TV). */
    focusRequester: FocusRequester = remember { FocusRequester() },
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
        AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
            Row(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(end = pillEnd(), bottom = 112.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ScrimStrong)
                    .border(1.dp, OnScrim.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .focusRequester(focusRequester)
                    .clickable(role = Role.Button, onClick = onClick)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = OnScrim)
                if (actionLabel != null) {
                    Spacer(Modifier.width(10.dp))
                    Text(actionLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = DarkAccent)
                } else {
                    Spacer(Modifier.width(8.dp))
                    Icon(painterResource(R.drawable.ic_skip_next), contentDescription = null, tint = OnScrim, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

/**
 * At the end credits (Series Mode phase 4): "Watch credits" and "Next episode", where the
 * skip-intro pill sits. With autoplay on, Next episode fills up as [countdownProgress] runs
 * (0 → 1) and plays when full; null means no countdown — just the button.
 */
@Composable
fun NextEpisodePill(
    visible: Boolean,
    countdownSeconds: Int?,
    countdownProgress: Float?,
    onNext: () -> Unit,
    onWatchCredits: () -> Unit,
    /** On Next episode, so the player can hand it remote focus when it appears (TV). */
    focusRequester: FocusRequester = remember { FocusRequester() },
    modifier: Modifier = Modifier,
) {
    // Eases each one-second step so the fill moves continuously rather than in jumps.
    val fill by animateFloatAsState(
        targetValue = countdownProgress ?: 1f,
        animationSpec = tween(durationMillis = if (countdownProgress == null) 0 else 1_000, easing = LinearEasing),
        label = "nextEpisodeFill",
    )
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
        AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
            Row(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(end = pillEnd(), bottom = 112.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Watch credits",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = OnScrim,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(ScrimStrong)
                        .border(1.dp, OnScrim.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button, onClick = onWatchCredits)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(OnScrim.copy(alpha = 0.6f))
                        .drawBehind { drawRect(OnScrim, size = Size(size.width * fill, size.height)) }
                        .focusRequester(focusRequester)
                        .clickable(role = Role.Button, onClick = onNext)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(painterResource(R.drawable.ic_play), contentDescription = null, tint = AmoledBackground, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (countdownSeconds != null) "Next episode in ${countdownSeconds}s" else "Next episode",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AmoledBackground,
                    )
                }
            }
        }
    }
}

/** Clear of the right edge, and of a TV's overscan. */
@Composable
private fun pillEnd() = if (LocalIsTv.current) 24.dp + TvSafeHorizontal else 24.dp
