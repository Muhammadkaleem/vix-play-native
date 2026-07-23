package com.devbytes.vixplayer.app.ui.player.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.devbytes.vixplayer.app.ui.theme.DarkAccent
import com.devbytes.vixplayer.app.ui.theme.OnScrim
import com.devbytes.vixplayer.app.ui.theme.ScrimStrong
import com.devbytes.vixplayer.app.ui.theme.TimecodeStyle
import com.devbytes.vixplayer.app.ui.theme.TrackInactive

/** Preview card size — 16:9, large enough to recognise a scene, small enough not to cover it. */
private val PREVIEW_WIDTH = 160.dp
private val PREVIEW_HEIGHT = 90.dp

/**
 * Scrub preview shown while seeking: the frame at the target position, the target
 * timecode, and (for the surface gesture) the signed delta and active precision tier.
 *
 * Shared by both scrub surfaces — the seek bar positions it above the thumb, the gesture
 * layer centres it — so a thumbnail never appears on one seek path but not the other.
 *
 * [frame] is null until the first decode lands, and stays null for sources the retriever
 * can't open; the card then degrades to timecode-only rather than disappearing.
 */
@Composable
fun ScrubPreview(
    frame: Bitmap?,
    targetMs: Long,
    modifier: Modifier = Modifier,
    deltaMs: Long? = null,
    tierLabel: String? = null,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ScrimStrong)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(PREVIEW_WIDTH, PREVIEW_HEIGHT)
                .clip(RoundedCornerShape(8.dp))
                .background(TrackInactive),
            contentAlignment = Alignment.Center,
        ) {
            if (frame != null) {
                Image(
                    bitmap = frame.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(PREVIEW_WIDTH, PREVIEW_HEIGHT),
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = formatTimeMs(targetMs),
                style = TimecodeStyle,
                color = OnScrim,
            )
            if (deltaMs != null) {
                Spacer(Modifier.width(10.dp))
                Text(
                    text = formatSignedDelta(deltaMs),
                    style = TimecodeStyle,
                    color = DarkAccent,
                )
            }
        }

        if (tierLabel != null) {
            Text(
                text = tierLabel,
                style = MaterialTheme.typography.labelSmall,
                color = OnScrim.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/** "+45s" / "−2:10" — seconds while small, m:ss once past a minute. */
private fun formatSignedDelta(ms: Long): String {
    val sign = if (ms < 0) "−" else "+"
    val totalSec = kotlin.math.abs(ms) / 1000
    return if (totalSec < 60) {
        "$sign${totalSec}s"
    } else {
        "$sign${totalSec / 60}:${(totalSec % 60).toString().padStart(2, '0')}"
    }
}
