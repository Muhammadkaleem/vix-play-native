package com.devbytes.vixplayer.app.ui.splash

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    /**
     * The permission that gates entry. Video is the primary library, so navigation waits
     * on this one; audio being denied narrows the app rather than blocking it.
     */
    val requiredPermission: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    /**
     * Everything requested at cold start. Android 13+ splits media permissions by type,
     * so audio must be asked for explicitly — requesting only video leaves the whole
     * audio library empty with no visible reason.
     */
    val requiredPermissions: Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO,
                // Declared in the manifest but previously never requested, which meant
                // the media notification could never appear on Android 13+ even with a
                // live MediaSession. Playback is the app's whole purpose, so it is asked
                // for up front alongside media access rather than mid-playback.
                Manifest.permission.POST_NOTIFICATIONS,
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    private val _permissionGranted = MutableStateFlow(false)
    val permissionGranted: StateFlow<Boolean> = _permissionGranted.asStateFlow()

    // Flips true once the synchronous check resolves — used by setKeepOnScreenCondition.
    private val _checkDone = MutableStateFlow(false)
    val checkDone: StateFlow<Boolean> = _checkDone.asStateFlow()

    init {
        checkPermission()
    }

    private fun granted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /**
     * Whether the app has enough video access to be usable. On Android 14+ a **partial**
     * grant ("Select photos and videos") leaves [Manifest.permission.READ_MEDIA_VIDEO]
     * denied but `READ_MEDIA_VISUAL_USER_SELECTED` granted — that is a usable state, not a
     * denial, so entry must not be gated behind full access alone.
     */
    fun hasVideoAccess(): Boolean = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
            granted(Manifest.permission.READ_MEDIA_VIDEO) ||
                granted("android.permission.READ_MEDIA_VISUAL_USER_SELECTED")
        else -> granted(requiredPermission)
    }

    fun checkPermission() {
        _permissionGranted.value = hasVideoAccess()
        _checkDone.value = true
    }
}
