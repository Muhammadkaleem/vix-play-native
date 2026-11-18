package com.devbytes.vixplayer.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.devbytes.vixplayer.app.notifications.PulseSettings
import com.devbytes.vixplayer.app.notifications.PulseState
import com.devbytes.vixplayer.app.player.gesture.DragAction
import com.devbytes.vixplayer.app.player.gesture.GestureBindings
import com.devbytes.vixplayer.app.player.gesture.TapAction
import com.devbytes.vixplayer.app.ui.theme.AccentChoice
import com.devbytes.vixplayer.app.ui.theme.ThemeMode
import com.devbytes.vixplayer.app.ui.theme.ThemeState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private inline fun <reified T : Enum<T>> enumOrNull(name: String): T? =
    runCatching { enumValueOf<T>(name) }.getOrNull()

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vixplay_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val PLAYBACK_SPEED          = floatPreferencesKey("playback_speed")
        val THEME_MODE              = stringPreferencesKey("theme_mode")
        val ACCENT                  = stringPreferencesKey("accent")
        val RECENT_SEARCHES         = stringPreferencesKey("recent_searches")
        val SHOW_SUBTITLES_DEFAULT  = booleanPreferencesKey("show_subtitles_by_default")
        val BACKGROUND_PLAYBACK     = booleanPreferencesKey("background_playback")
        // Up Next / autoplay preferences (single source of truth, shared with Settings).
        val AUTOPLAY_NEXT           = booleanPreferencesKey("autoplay_next")
        val ASK_BEFORE_NEXT         = booleanPreferencesKey("ask_before_next")
        val VIEW_MODE               = stringPreferencesKey("library_view_mode")
        val FOLDER_SORT             = stringPreferencesKey("folder_sort")
        val GESTURE_LEFT_DRAG       = stringPreferencesKey("gesture_left_drag")
        val GESTURE_RIGHT_DRAG      = stringPreferencesKey("gesture_right_drag")
        val GESTURE_LEFT_TAP        = stringPreferencesKey("gesture_left_tap")
        val GESTURE_RIGHT_TAP       = stringPreferencesKey("gesture_right_tap")
        val SUB_PRESET              = stringPreferencesKey("subtitle_preset")
        val SUB_TEXT_SIZE           = floatPreferencesKey("subtitle_text_size")
        val SUB_BOTTOM_PADDING      = floatPreferencesKey("subtitle_bottom_padding")
        val SUB_TEXT_COLOR          = intPreferencesKey("subtitle_text_color")
        val SUB_BACKGROUND_COLOR    = intPreferencesKey("subtitle_background_color")
        val SUB_EDGE_TYPE           = intPreferencesKey("subtitle_edge_type")
        val SUB_TYPEFACE            = stringPreferencesKey("subtitle_typeface")
        val SUB_OVERRIDE_EMBEDDED   = booleanPreferencesKey("subtitle_override_embedded")
        val COMPLETED_PLAYBACKS     = intPreferencesKey("completed_playbacks")
        val PINNED_FOLDERS          = stringSetPreferencesKey("pinned_folders")
        // Library Pulse notifications: per-trigger toggles + scheduling bookkeeping.
        val PULSE_MASTER            = booleanPreferencesKey("pulse_master")
        val PULSE_RESUME            = booleanPreferencesKey("pulse_resume")
        val PULSE_NEW_VIDEOS        = booleanPreferencesKey("pulse_new_videos")
        val PULSE_DIGEST            = booleanPreferencesKey("pulse_digest")
        val PULSE_LAST_OPEN         = longPreferencesKey("pulse_last_open")
        val PULSE_LAST_NOTIF        = longPreferencesKey("pulse_last_notif")
        val PULSE_LAST_SEEN_VIDEO   = longPreferencesKey("pulse_last_seen_video")
        val PULSE_LAST_DIGEST       = longPreferencesKey("pulse_last_digest")
        val PULSE_LAST_RESUME_ID    = longPreferencesKey("pulse_last_resume_id")
    }

    val playbackSpeed: Flow<Float> = context.dataStore.data
        .map { it[Keys.PLAYBACK_SPEED] ?: 1.0f }

    /**
     * Base theme + accent, read together so the theme resolves in one place. Defaults to
     * DARK to preserve the app's original dark-only behaviour — an existing user sees no
     * change until they opt into Light/System/Dynamic.
     */
    val themeState: Flow<ThemeState> = context.dataStore.data.map { p ->
        ThemeState(
            mode = p[Keys.THEME_MODE]?.let { enumOrNull<ThemeMode>(it) } ?: ThemeMode.LIGHT,
            accent = p[Keys.ACCENT]?.let { enumOrNull<AccentChoice>(it) } ?: AccentChoice.DEFAULT,
        )
    }

    val showSubtitlesByDefault: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.SHOW_SUBTITLES_DEFAULT] ?: true }

    /**
     * Keep playing audio when the app goes to background. Defaults to **off**: this is a
     * video player, and silently continuing playback after backgrounding reads as a
     * battery bug rather than a feature. PiP already covers the Home-press case.
     */
    val backgroundPlayback: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.BACKGROUND_PLAYBACK] ?: false }

    /** Autoplay the next video when the current one ends. Default off. */
    val autoplayNext: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.AUTOPLAY_NEXT] ?: false }

    /** When autoplay is off, show the "Play next?" confirmation at video end. Default on. */
    val askBeforePlayingNext: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.ASK_BEFORE_NEXT] ?: true }

    suspend fun setAutoplayNext(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTOPLAY_NEXT] = enabled }
    }

    suspend fun setAskBeforePlayingNext(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ASK_BEFORE_NEXT] = enabled }
    }

    /** Recent search queries, newest first (max 8). Newline-delimited to preserve order. */
    val recentSearches: Flow<List<String>> = context.dataStore.data.map { p ->
        p[Keys.RECENT_SEARCHES]?.split('\n')?.filter { it.isNotBlank() } ?: emptyList()
    }

    /** Records a successful search query (de-duplicated, moved to front, capped at 8). */
    suspend fun addRecentSearch(query: String) {
        val q = query.trim()
        if (q.isBlank()) return
        context.dataStore.edit { p ->
            val current = p[Keys.RECENT_SEARCHES]?.split('\n')?.filter { it.isNotBlank() } ?: emptyList()
            val updated = (listOf(q) + current.filterNot { it.equals(q, ignoreCase = true) }).take(8)
            p[Keys.RECENT_SEARCHES] = updated.joinToString("\n")
        }
    }

    suspend fun clearRecentSearches() {
        context.dataStore.edit { it.remove(Keys.RECENT_SEARCHES) }
    }

    /** Restores the default end-of-video prompt behaviour (clears "Don't ask again"). */
    suspend fun resetPlaybackPrompts() {
        context.dataStore.edit { it[Keys.ASK_BEFORE_NEXT] = true }
    }

    suspend fun setPlaybackSpeed(speed: Float) {
        context.dataStore.edit { it[Keys.PLAYBACK_SPEED] = speed }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAccent(accent: AccentChoice) {
        context.dataStore.edit { it[Keys.ACCENT] = accent.name }
    }

    suspend fun setShowSubtitlesByDefault(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_SUBTITLES_DEFAULT] = enabled }
    }

    suspend fun setBackgroundPlayback(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BACKGROUND_PLAYBACK] = enabled }
    }

    /** Grid vs list, shared by the video library and the folder browser. Defaults to LIST. */
    val viewMode: Flow<String> = context.dataStore.data.map { it[Keys.VIEW_MODE] ?: "LIST" }

    suspend fun setViewMode(mode: String) {
        context.dataStore.edit { it[Keys.VIEW_MODE] = mode }
    }

    /** Folder-browser ordering. Default NAME: stable against file operations. */
    val folderSort: Flow<String> = context.dataStore.data.map { it[Keys.FOLDER_SORT] ?: "NAME" }

    suspend fun setFolderSort(sort: String) {
        context.dataStore.edit { it[Keys.FOLDER_SORT] = sort }
    }

    /** Remappable player gestures. Defaults reproduce the original hardcoded behaviour. */
    val gestureBindings: Flow<GestureBindings> = context.dataStore.data.map { p ->
        val d = GestureBindings.DEFAULT
        GestureBindings(
            leftDrag = p[Keys.GESTURE_LEFT_DRAG]?.let { enumOrNull<DragAction>(it) } ?: d.leftDrag,
            rightDrag = p[Keys.GESTURE_RIGHT_DRAG]?.let { enumOrNull<DragAction>(it) } ?: d.rightDrag,
            leftTap = p[Keys.GESTURE_LEFT_TAP]?.let { enumOrNull<TapAction>(it) } ?: d.leftTap,
            rightTap = p[Keys.GESTURE_RIGHT_TAP]?.let { enumOrNull<TapAction>(it) } ?: d.rightTap,
        )
    }

    suspend fun setGestureBindings(b: GestureBindings) {
        context.dataStore.edit {
            it[Keys.GESTURE_LEFT_DRAG] = b.leftDrag.name
            it[Keys.GESTURE_RIGHT_DRAG] = b.rightDrag.name
            it[Keys.GESTURE_LEFT_TAP] = b.leftTap.name
            it[Keys.GESTURE_RIGHT_TAP] = b.rightTap.name
        }
    }

    /**
     * Subtitle appearance. Global rather than per-file: this is a stable preference like
     * playback speed, unlike the sync offset which is a property of one release.
     */
    val subtitleStyle: Flow<SubtitleStyle> = context.dataStore.data.map { prefs ->
        val default = SubtitleStyle.DEFAULT
        SubtitleStyle(
            preset = prefs[Keys.SUB_PRESET]?.let { name ->
                SubtitlePreset.entries.firstOrNull { it.name == name }
            } ?: default.preset,
            textSizeFraction = prefs[Keys.SUB_TEXT_SIZE] ?: default.textSizeFraction,
            bottomPaddingFraction = prefs[Keys.SUB_BOTTOM_PADDING] ?: default.bottomPaddingFraction,
            textColor = prefs[Keys.SUB_TEXT_COLOR] ?: default.textColor,
            backgroundColor = prefs[Keys.SUB_BACKGROUND_COLOR] ?: default.backgroundColor,
            edgeType = prefs[Keys.SUB_EDGE_TYPE] ?: default.edgeType,
            typeface = prefs[Keys.SUB_TYPEFACE]?.let { name ->
                SubtitleTypeface.entries.firstOrNull { it.name == name }
            } ?: default.typeface,
            overrideEmbedded = prefs[Keys.SUB_OVERRIDE_EMBEDDED] ?: default.overrideEmbedded,
        )
    }

    /** Folders the user pinned to the top of the folder lists (a small config-like set). */
    val pinnedFolders: Flow<Set<Long>> = context.dataStore.data.map { p ->
        (p[Keys.PINNED_FOLDERS] ?: emptySet()).mapNotNull { it.toLongOrNull() }.toSet()
    }

    suspend fun togglePinnedFolder(bucketId: Long) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.PINNED_FOLDERS] ?: emptySet()
            val key = bucketId.toString()
            prefs[Keys.PINNED_FOLDERS] = if (key in current) current - key else current + key
        }
    }

    /** Library Pulse per-trigger toggles + master switch, for the Notifications screen. */
    val pulseSettings: Flow<PulseSettings> = context.dataStore.data.map { p ->
        PulseSettings(
            master = p[Keys.PULSE_MASTER] ?: true,
            resume = p[Keys.PULSE_RESUME] ?: true,
            newVideos = p[Keys.PULSE_NEW_VIDEOS] ?: true,
            digest = p[Keys.PULSE_DIGEST] ?: true,
        )
    }

    suspend fun setPulseMaster(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PULSE_MASTER] = enabled }
    }

    suspend fun setPulseResume(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PULSE_RESUME] = enabled }
    }

    suspend fun setPulseNewVideos(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PULSE_NEW_VIDEOS] = enabled }
    }

    suspend fun setPulseDigest(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PULSE_DIGEST] = enabled }
    }

    /** One-shot read of the scheduling bookkeeping the Pulse worker evaluates. */
    suspend fun pulseState(): PulseState = context.dataStore.data.first().let { p ->
        PulseState(
            lastAppOpenAt = p[Keys.PULSE_LAST_OPEN] ?: 0L,
            lastNotificationAt = p[Keys.PULSE_LAST_NOTIF] ?: 0L,
            lastSeenNewestVideoDate = p[Keys.PULSE_LAST_SEEN_VIDEO] ?: 0L,
            lastDigestAt = p[Keys.PULSE_LAST_DIGEST] ?: 0L,
            lastResumeNudgeId = p[Keys.PULSE_LAST_RESUME_ID] ?: 0L,
        )
    }

    suspend fun setPulseLastAppOpenAt(ts: Long) {
        context.dataStore.edit { it[Keys.PULSE_LAST_OPEN] = ts }
    }

    suspend fun setPulseLastNotificationAt(ts: Long) {
        context.dataStore.edit { it[Keys.PULSE_LAST_NOTIF] = ts }
    }

    suspend fun setPulseLastSeenNewestVideoDate(ts: Long) {
        context.dataStore.edit { it[Keys.PULSE_LAST_SEEN_VIDEO] = ts }
    }

    suspend fun setPulseLastDigestAt(ts: Long) {
        context.dataStore.edit { it[Keys.PULSE_LAST_DIGEST] = ts }
    }

    suspend fun setPulseLastResumeNudgeId(id: Long) {
        context.dataStore.edit { it[Keys.PULSE_LAST_RESUME_ID] = id }
    }

    /**
     * Lifetime count of videos watched to completion — the positive signal that gates the
     * Play in-app review prompt. Increments atomically and returns the new total so the
     * caller can decide in one step.
     */
    suspend fun recordCompletedPlaybackAndGet(): Int {
        val prefs = context.dataStore.edit {
            it[Keys.COMPLETED_PLAYBACKS] = (it[Keys.COMPLETED_PLAYBACKS] ?: 0) + 1
        }
        return prefs[Keys.COMPLETED_PLAYBACKS] ?: 0
    }

    suspend fun setSubtitleStyle(style: SubtitleStyle) {
        context.dataStore.edit {
            it[Keys.SUB_PRESET] = style.preset.name
            it[Keys.SUB_TEXT_SIZE] = style.textSizeFraction
            it[Keys.SUB_BOTTOM_PADDING] = style.bottomPaddingFraction
            it[Keys.SUB_TEXT_COLOR] = style.textColor
            it[Keys.SUB_BACKGROUND_COLOR] = style.backgroundColor
            it[Keys.SUB_EDGE_TYPE] = style.edgeType
            it[Keys.SUB_TYPEFACE] = style.typeface.name
            it[Keys.SUB_OVERRIDE_EMBEDDED] = style.overrideEmbedded
        }
    }
}
