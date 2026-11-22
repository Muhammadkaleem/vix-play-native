package com.devbytes.vixplayer.app.ads

import android.app.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Amazon Appstore build: no rewarded ads (and no Pro offer is shown to ask for one). */
@Singleton
class RewardedAdManager @Inject constructor() {
    val loading: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()

    fun preload() = Unit

    fun show(
        activity: Activity,
        onReward: () -> Unit,
        onUnavailable: () -> Unit,
        entryPoint: String = "unknown",
    ) = onUnavailable()
}
