package com.devbytes.vixplayer.app.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.union
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Height of the app's bottom chrome (banner + mini-player + floating nav pill), measured by the
 * app Scaffold. Screens draw *behind* that chrome so content shows through the translucent pill
 * and its clear margins; they reserve this space inside their scrolling content instead.
 */
val LocalBottomBarInset = compositionLocalOf { 0.dp }

/**
 * Content insets for every screen Scaffold: the usual system bars, plus the bottom chrome. A
 * screen that hands its Scaffold padding to a list as `contentPadding` scrolls behind the bar
 * with its last item still reachable; FABs and snackbars sit above the bar automatically.
 */
@Composable
fun screenInsets(): WindowInsets =
    ScaffoldDefaults.contentWindowInsets.union(WindowInsets(bottom = LocalBottomBarInset.current))

/** These padding values without the bottom — for a container whose list pads its own bottom. */
@Composable
fun PaddingValues.withoutBottom(): PaddingValues {
    val dir = LocalLayoutDirection.current
    return PaddingValues(
        start = calculateStartPadding(dir),
        top = calculateTopPadding(),
        end = calculateEndPadding(dir),
        bottom = 0.dp,
    )
}

/** Bottom-only padding: this padding's bottom plus [extra] — for a list's `contentPadding`. */
fun PaddingValues.bottomOnly(extra: Dp = 0.dp): PaddingValues =
    PaddingValues(bottom = calculateBottomPadding() + extra)
