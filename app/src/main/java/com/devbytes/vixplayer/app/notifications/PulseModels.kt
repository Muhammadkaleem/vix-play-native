package com.devbytes.vixplayer.app.notifications

/** Per-trigger enablement + the master switch, surfaced in the Notifications settings screen. */
data class PulseSettings(
    val master: Boolean = true,
    val resume: Boolean = true,
    val newVideos: Boolean = true,
    val digest: Boolean = true,
)

/** Timestamps/ids the worker reads to decide what (if anything) to post. All epoch millis. */
data class PulseState(
    val lastAppOpenAt: Long = 0L,
    val lastNotificationAt: Long = 0L,
    val lastSeenNewestVideoDate: Long = 0L,
    val lastDigestAt: Long = 0L,
    val lastResumeNudgeId: Long = 0L,
)

enum class PulseTrigger { RESUME, NEW_VIDEOS, DIGEST }

/**
 * A resolved notification to post. [playUri] non-null ⇒ tapping deep-links into the player on
 * that video (auto-resumes); null ⇒ opens the library home.
 */
data class PulseNotification(
    val trigger: PulseTrigger,
    val title: String,
    val text: String,
    val playUri: String? = null,
)
