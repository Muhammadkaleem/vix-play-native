package com.devbytes.vixplayer.app.ui.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.player.IntroSource
import com.devbytes.vixplayer.app.player.formatIntroTime
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
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
        AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
            Row(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(end = 24.dp, bottom = 112.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ScrimStrong)
                    .border(1.dp, OnScrim.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
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
