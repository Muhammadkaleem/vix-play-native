package com.devbytes.vixplayer.app.ads

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Exposes ad readiness to the Compose tree (the nav-graph banner host reads it). */
@HiltViewModel
class AdsViewModel @Inject constructor(
    controller: AdsController,
) : ViewModel() {
    val adsEnabled: Boolean = controller.adsEnabled
    val canRequestAds: StateFlow<Boolean> = controller.canRequestAds
}
