package com.devbytes.vixplayer.app.ads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devbytes.vixplayer.app.data.repository.ProRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Exposes ad readiness to the Compose tree (the nav-graph banner host reads it). */
@HiltViewModel
class AdsViewModel @Inject constructor(
    controller: AdsController,
    pro: ProRepository,
) : ViewModel() {
    /**
     * Ads are shown only when the controller allows them AND the user is NOT currently Pro.
     * A rewarded-ad grant flips [ProRepository.isProActive] true, which drops this to false
     * live — every banner/native host reads it, so ads vanish at once (and return at expiry).
     */
    val adsEnabled: StateFlow<Boolean> = pro.isProActive
        .map { isPro -> controller.adsEnabled && !isPro }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), controller.adsEnabled)

    val canRequestAds: StateFlow<Boolean> = controller.canRequestAds
}
