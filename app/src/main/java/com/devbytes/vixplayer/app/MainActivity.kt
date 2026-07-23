package com.devbytes.vixplayer.app

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.devbytes.vixplayer.app.player.PipController
import com.devbytes.vixplayer.app.ui.VixPlayNavGraph
import com.devbytes.vixplayer.app.ui.splash.SplashViewModel
import com.devbytes.vixplayer.app.ui.theme.VixPlayTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val splashViewModel: SplashViewModel by viewModels()

    /** PlayerScreen registers its PiP contract here; the OS hooks below call it. */
    val pipController = PipController()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { !splashViewModel.checkDone.value }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VixPlayTheme {
                VixPlayNavGraph()
            }
        }
    }

    // Home / recents while a video is actively playing → seamless PiP. The decision
    // (is this a playing video on the player route?) is owned by PlayerScreen.
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && pipController.canEnterPip()) {
            pipController.buildParams()?.let { enterPictureInPictureMode(it) }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        pipController.onModeChanged(isInPictureInPictureMode)
    }
}
