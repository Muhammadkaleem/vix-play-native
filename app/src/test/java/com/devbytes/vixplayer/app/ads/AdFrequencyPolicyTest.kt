package com.devbytes.vixplayer.app.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every interstitial rule, against the release defaults (not the shortened debug ones). */
class AdFrequencyPolicyTest {

    private val min = 60_000L
    private val hour = 60 * min
    private val now = 1_000 * hour

    private val cfg = AdConfig() // release values: 4 min gap, 3/session, 8/day, 24 h + 2 sessions grace

    /** A user well past every grace period with nothing shown yet. */
    private val eligible = AdFrequencyState(
        installedAt = now - 48 * hour,
        sessionCount = 5,
        lastFullScreenAt = 0,
        lastRewardedAt = 0,
        todayCount = 0,
        sessionShownCount = 0,
    )
    private val watched = BreakContext(isPro = false, watchedMs = 5 * min, externalOpen = false)

    private fun decide(
        s: AdFrequencyState = eligible,
        c: AdConfig = cfg,
        ctx: BreakContext = watched,
    ) = AdFrequencyPolicy.decide(now, s, c, ctx)

    private fun skip(reason: String) = AdDecision.Skip(reason)

    @Test fun showsWhenEveryRulePasses() = assertEquals(AdDecision.Show, decide())

    @Test fun remoteKillSwitch() {
        assertEquals(skip("disabled"), decide(c = cfg.copy(interstitialEnabled = false)))
        assertEquals(skip("disabled"), decide(c = cfg.copy(adsEnabled = false)))
    }

    @Test fun proNeverSeesInterstitials() =
        assertEquals(skip("pro"), decide(ctx = watched.copy(isPro = true)))

    @Test fun externalOpenExitNeverShows() =
        assertEquals(skip("external_open"), decide(ctx = watched.copy(externalOpen = true)))

    @Test fun firstDayAfterInstallIsProtected() {
        assertEquals(skip("install_grace"), decide(s = eligible.copy(installedAt = now - 23 * hour)))
        assertEquals(AdDecision.Show, decide(s = eligible.copy(installedAt = now - 24 * hour)))
    }

    @Test fun firstTwoSessionsAreProtected() {
        assertEquals(skip("session_grace"), decide(s = eligible.copy(sessionCount = 2)))
        assertEquals(AdDecision.Show, decide(s = eligible.copy(sessionCount = 3)))
    }

    @Test fun shortWatchDoesNotEarnAnAd() {
        assertEquals(skip("short_watch"), decide(ctx = watched.copy(watchedMs = 59_999)))
        assertEquals(AdDecision.Show, decide(ctx = watched.copy(watchedMs = 60_000)))
    }

    @Test fun minimumGapBetweenFullScreenAds() {
        assertEquals(skip("min_gap"), decide(s = eligible.copy(lastFullScreenAt = now - 3 * min)))
        assertEquals(AdDecision.Show, decide(s = eligible.copy(lastFullScreenAt = now - 4 * min)))
    }

    @Test fun rewardedCooldownOutlastsTheGap() {
        // A rewarded ad 5 min ago clears the 4 min gap but not the 10 min rewarded cooldown.
        val s = eligible.copy(lastFullScreenAt = now - 5 * min, lastRewardedAt = now - 5 * min)
        assertEquals(skip("rewarded_cooldown"), decide(s = s))
        val later = eligible.copy(lastFullScreenAt = now - 10 * min, lastRewardedAt = now - 10 * min)
        assertEquals(AdDecision.Show, decide(s = later))
    }

    @Test fun sessionCap() {
        assertEquals(skip("session_cap"), decide(s = eligible.copy(sessionShownCount = 3)))
        assertEquals(AdDecision.Show, decide(s = eligible.copy(sessionShownCount = 2)))
    }

    @Test fun dailyCap() {
        assertEquals(skip("daily_cap"), decide(s = eligible.copy(todayCount = 8)))
        assertEquals(AdDecision.Show, decide(s = eligible.copy(todayCount = 7)))
    }

    @Test fun cheapestBlockingReasonIsReportedFirst() {
        // Pro + inside grace + capped: Pro wins, so analytics attribute skips to the real cause.
        val s = eligible.copy(installedAt = now, sessionShownCount = 9, todayCount = 9)
        assertEquals(skip("pro"), decide(s = s, ctx = watched.copy(isPro = true)))
    }

    // ── "Go ad-free" after an interstitial ────────────────────────────────────────────────

    @Test fun adFreeIsOfferedTheFirstTime() =
        assertTrue(AdFrequencyPolicy.offersAdFree(now, lastPromptAt = 0, c = cfg, isPro = false))

    @Test fun adFreeIsOfferedAtMostEveryThreeDays() {
        assertFalse(AdFrequencyPolicy.offersAdFree(now, lastPromptAt = now - 71 * hour, c = cfg, isPro = false))
        assertTrue(AdFrequencyPolicy.offersAdFree(now, lastPromptAt = now - 72 * hour, c = cfg, isPro = false))
    }

    @Test fun adFreeIsNeverOfferedToProOrWhenSwitchedOff() {
        assertFalse(AdFrequencyPolicy.offersAdFree(now, 0, cfg, isPro = true))
        assertFalse(AdFrequencyPolicy.offersAdFree(now, 0, cfg.copy(adFreePromptEnabled = false), isPro = false))
        assertFalse(AdFrequencyPolicy.offersAdFree(now, 0, cfg.copy(adsEnabled = false), isPro = false))
    }
}
