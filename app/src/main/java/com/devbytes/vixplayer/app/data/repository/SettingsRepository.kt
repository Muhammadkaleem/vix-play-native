package com.devbytes.vixplayer.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.devbytes.vixplayer.app.player.gesture.DragAction
import com.devbytes.vixplayer.app.player.gesture.GestureBindings
import com.devbytes.vixplayer.app.player.gesture.TapAction
import com.devbytes.vixplayer.app.ui.theme.AccentChoice
import com.devbytes.vixplayer.app.ui.theme.ThemeMode
import com.devbytes.vixplayer.app.ui.theme.ThemeState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
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
        val SHOW_SUBTITLES_DEFAULT  = booleanPreferencesKey("show_subtitles_by_default")
        val BACKGROUND_PLAYBACK     = booleanPreferencesKey("background_playback")
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
            mode = p[Keys.THEME_MODE]?.let { enumOrNull<ThemeMode>(it) } ?: ThemeMode.DARK,
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
