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

    val requiredPermission: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    private val _permissionGranted = MutableStateFlow(false)
    val permissionGranted: StateFlow<Boolean> = _permissionGranted.asStateFlow()

    // Flips true once the synchronous check resolves — used by setKeepOnScreenCondition.
    private val _checkDone = MutableStateFlow(false)
    val checkDone: StateFlow<Boolean> = _checkDone.asStateFlow()

    init {
        checkPermission()
    }

    fun checkPermission() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            requiredPermission,
        ) == PackageManager.PERMISSION_GRANTED
        _permissionGranted.value = granted
        _checkDone.value = true
    }

    fun onPermissionResult(granted: Boolean) {
        _permissionGranted.value = granted
    }
}
