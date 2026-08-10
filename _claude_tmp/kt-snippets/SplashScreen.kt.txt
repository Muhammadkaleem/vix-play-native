package com.devbytes.vixplayer.app.ui.splash

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.ui.theme.BrandEnd
import com.devbytes.vixplayer.app.ui.theme.BrandStart
import com.devbytes.vixplayer.app.ui.theme.DarkAccent
import com.devbytes.vixplayer.app.ui.theme.DarkBackground
import com.devbytes.vixplayer.app.ui.theme.DarkContentSecondary
import com.devbytes.vixplayer.app.ui.theme.OnAccent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onNavigateToLibrary: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val permissionGranted by viewModel.permissionGranted.collectAsStateWithLifecycle()
    var showRationale by remember { mutableStateOf(false) }
    var permanentlyDenied by remember { mutableStateOf(false) }
    var showProgress by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        // Entry is gated on the video permission; audio may be declined independently.
        val granted = results[viewModel.requiredPermission] == true
        viewModel.onPermissionResult(granted)
        if (!granted) {
            val activity = context as? Activity
            val canAskAgain = activity
                ?.shouldShowRequestPermissionRationale(viewModel.requiredPermission) == true
            if (canAskAgain) showRationale = true else permanentlyDenied = true
        }
    }

    // Splash owns the cold-start permission request — auto-request on entry if not granted.
    LaunchedEffect(Unit) {
        if (!permissionGranted) launcher.launch(viewModel.requiredPermissions)
    }

    // Navigate as soon as the permission flips to granted.
    LaunchedEffect(permissionGranted) {
        if (permissionGranted) onNavigateToLibrary()
    }

    // PRD: no progress indicator unless init takes longer than 400ms.
    LaunchedEffect(Unit) {
        delay(400)
        showProgress = true
    }

    // Brand entrance motion — logo settles with a soft spring, content fades in.
    val logoScale = remember { Animatable(0.82f) }
    val contentAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { contentAlpha.animateTo(1f, tween(durationMillis = 450)) }
        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center,
    ) {
        // Ambient brand backglow behind the mark.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(BrandStart.copy(alpha = 0.22f), Color.Transparent),
                    ),
                ),
        )

        Column(
            modifier = Modifier.graphicsLayer { alpha = contentAlpha.value },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer {
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                    },
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "VixPlay",
                style = MaterialTheme.typography.displayLarge.copy(
                    brush = Brush.linearGradient(
                        colors = listOf(BrandStart, BrandEnd),
                        start = Offset(0f, 0f),
                        end = Offset(Float.MAX_VALUE, 0f),
                    ),
                ),
            )
        }

        // Delayed init indicator — only while waiting, never during the rationale.
        AnimatedVisibility(
            visible = showProgress && !permissionGranted && !showRationale && !permanentlyDenied,
            enter = fadeIn(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp),
        ) {
            CircularProgressIndicator(
                color = DarkAccent,
                strokeWidth = 3.dp,
                modifier = Modifier.size(28.dp),
            )
        }

        if (showRationale || permanentlyDenied) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 32.dp, vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "VixPlay needs access to your media files to show your videos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DarkContentSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                if (permanentlyDenied) {
                    Button(
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null),
                            )
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkAccent,
                            contentColor = OnAccent,
                        ),
                    ) {
                        Text("Open Settings")
                    }
                } else {
                    Button(
                        onClick = { launcher.launch(viewModel.requiredPermissions) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkAccent,
                            contentColor = OnAccent,
                        ),
                    ) {
                        Text("Grant Permission")
                    }
                }
            }
        }
    }
}
