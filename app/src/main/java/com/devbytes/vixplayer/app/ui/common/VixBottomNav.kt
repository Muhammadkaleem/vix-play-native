package com.devbytes.vixplayer.app.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.navigation.TAB_AUDIO_ROUTE
import com.devbytes.vixplayer.app.navigation.TAB_HOME_ROUTE
import com.devbytes.vixplayer.app.navigation.TAB_NETWORK_ROUTE
import com.devbytes.vixplayer.app.navigation.TAB_SETTINGS_ROUTE
import com.devbytes.vixplayer.app.navigation.TAB_VIDEO_ROUTE
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private data class NavItem(
    val tabRoute: String,
    val iconRes: Int,
    val selectedIconRes: Int,
    val label: String,
)

private val NAV_ITEMS = listOf(
    NavItem(TAB_HOME_ROUTE,     R.drawable.ic_nav_home,    R.drawable.ic_nav_home_filled,     "Home"),
    NavItem(TAB_VIDEO_ROUTE,    R.drawable.ic_nav_video,   R.drawable.ic_nav_video_filled,    "Video"),
    NavItem(TAB_AUDIO_ROUTE,    R.drawable.ic_nav_audio,   R.drawable.ic_nav_audio_filled,    "Audio"),
    NavItem(TAB_NETWORK_ROUTE,  R.drawable.ic_nav_network, R.drawable.ic_nav_network_filled,  "Network"),
    NavItem(TAB_SETTINGS_ROUTE, R.drawable.ic_settings,    R.drawable.ic_nav_settings_filled, "Settings"),
)

// ── Geometry (measured off the reference recording, scaled to dp) ─────────────
private val BarHeight = 68.dp
private val BarSideMargin = 12.dp
private val BarInnerPadding = 5.dp
private val CapsuleInset = 2.dp

// ── Motion ───────────────────────────────────────────────────────────────────
// The capsule rides a spring, so a one-slot and a four-slot move take about the same time
// (reference: ~120 ms vs ~175 ms to arrive), then swells slightly and settles (~150 ms).
private val CapsuleSpring = spring<Float>(dampingRatio = 0.86f, stiffness = 520f)
/** Within this many slots of its target, the capsule counts as arrived and swells. */
private const val ARRIVED_SLOTS = 0.08f
private const val SWELL_SCALE = 1.06f
/** Horizontal stretch per (slots / second) of travel speed, capped at [MAX_STRETCH]. */
private const val STRETCH_PER_VELOCITY = 0.012f
private const val MAX_STRETCH = 0.28f
/** The capsule thins out while travelling (reference: lighter mid-flight, solid on arrival). */
private const val MAX_TRAVEL_FADE = 0.35f
private const val FADE_VELOCITY = 20f

/**
 * Floating pill bottom bar: a rounded, inset glass bar whose selected tab sits in a capsule that
 * springs to the tapped tab (stretching with speed, then swelling into place). The selected icon
 * switches to its filled glyph and the label bolds the instant the tab is tapped; only the capsule
 * travels. Every per-frame value is applied in graphics layers, so animation never recomposes.
 */
@Composable
fun VixBottomNav(
    currentDestination: NavDestination?,
    onNavigate: (String) -> Unit,
) {
    val selectedIndex = NAV_ITEMS.indexOfFirst { item ->
        currentDestination?.hierarchy?.any { it.route == item.tabRoute } == true
    }

    val position = remember { Animatable(selectedIndex.coerceAtLeast(0).toFloat()) }
    val swell = remember { Animatable(1f) }
    LaunchedEffect(selectedIndex) {
        if (selectedIndex < 0) return@LaunchedEffect
        launch {
            // Swell once the capsule has actually arrived (whatever the distance), as in the reference.
            val target = selectedIndex.toFloat()
            snapshotFlow { abs(position.value - target) < ARRIVED_SLOTS }.first { it }
            swell.animateTo(SWELL_SCALE, tween(durationMillis = 100))
            swell.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow))
        }
        // Rapid taps retarget from the current position and velocity — no jump.
        position.animateTo(selectedIndex.toFloat(), CapsuleSpring)
    }

    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = BarSideMargin, vertical = 8.dp),
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(BarHeight)
                .shadow(elevation = 14.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(colors.surfaceVariant.copy(alpha = 0.94f))
                .border(1.dp, colors.onSurface.copy(alpha = 0.07f), CircleShape)
                .padding(BarInnerPadding),
        ) {
            val itemWidth = maxWidth / NAV_ITEMS.size
            val itemWidthPx = constraints.maxWidth.toFloat() / NAV_ITEMS.size

            // Selection capsule (hidden on routes outside the five tabs).
            if (selectedIndex >= 0) {
                Box(
                    Modifier
                        .offset { IntOffset((position.value * itemWidthPx).roundToInt(), 0) }
                        .width(itemWidth)
                        .fillMaxHeight()
                        .padding(horizontal = CapsuleInset)
                        .graphicsLayer {
                            val speed = abs(position.velocity)
                            val stretch = (speed * STRETCH_PER_VELOCITY).coerceAtMost(MAX_STRETCH)
                            scaleX = swell.value * (1f + stretch)
                            scaleY = swell.value
                            alpha = 1f - MAX_TRAVEL_FADE * (speed / FADE_VELOCITY).coerceAtMost(1f)
                        }
                        .clip(CircleShape)
                        .background(colors.primary.copy(alpha = 0.22f)),
                )
            }

            Row(Modifier.fillMaxSize()) {
                NAV_ITEMS.forEachIndexed { index, item ->
                    NavTab(
                        item = item,
                        selected = index == selectedIndex,
                        onClick = { onNavigate(item.tabRoute) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun NavTab(
    item: NavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val tint by animateColorAsState(
        targetValue = if (selected) colors.onSurface else colors.onSurfaceVariant,
        animationSpec = tween(durationMillis = 150),
        label = "nav_tint",
    )
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(CircleShape)
            // The capsule is the touch feedback (as in the reference), so no ripple.
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(if (selected) item.selectedIconRes else item.iconRes),
            contentDescription = null, // the label below names the tab
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = tint,
            maxLines = 1,
        )
    }
}
