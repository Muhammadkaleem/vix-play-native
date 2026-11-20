package com.devbytes.vixplayer.app.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Product-funnel events (update prompt, review prompt, Pro offer). Ad-network events live in
 * [com.devbytes.vixplayer.app.ads.AdEvents]. Collection follows the app-wide switch in
 * `VixPlayApp` (release only), and a missing Play Services can never crash a caller.
 */
object Analytics {

    fun log(context: Context, event: String, vararg params: Pair<String, Any?>) {
        runCatching {
            FirebaseAnalytics.getInstance(context.applicationContext).logEvent(
                event,
                Bundle().apply {
                    params.forEach { (k, v) ->
                        when (v) {
                            is String -> putString(k, v)
                            is Int -> putLong(k, v.toLong())
                            is Long -> putLong(k, v)
                            is Boolean -> putString(k, v.toString())
                            null -> Unit
                            else -> putString(k, v.toString())
                        }
                    }
                },
            )
        }
    }

    // ── In-app update prompt ────────────────────────────────────────────────
    const val UPDATE_PROMPT_SHOWN = "update_prompt_shown"
    const val UPDATE_PROMPT_RESULT = "update_prompt_result"
    const val UPDATE_DOWNLOADED = "update_downloaded"
    const val UPDATE_INSTALL_STARTED = "update_install_started"

    // ── In-app review prompt ────────────────────────────────────────────────
    const val REVIEW_PROMPT_REQUESTED = "review_prompt_requested"
    const val REVIEW_FLOW_FINISHED = "review_flow_finished"

    // ── End-of-video Up Next ────────────────────────────────────────────────
    /** result: autoplay / play_next / pick / replay / cancel / not_now / close; episodic: true/false. */
    const val UP_NEXT_RESULT = "up_next_result"

    // ── Shows (Home shelf + Show screen) ────────────────────────────────────
    /** source: hero / episode; kind: start / resume / next / rewatch (the show's main action). */
    const val SHOW_PLAY = "show_play"

    // ── Pro (rewarded ad-free) offer ────────────────────────────────────────
    const val PRO_CARD_VIEWED = "pro_card_viewed"
    const val PRO_DETAILS_OPENED = "pro_details_opened"
    const val PRO_SUCCESS_SHOWN = "pro_success_shown"
}
