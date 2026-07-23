# Data Models

## Room entities (sketch)
```kotlin
@Entity data class MediaItem(
  @PrimaryKey val id: String,        // stable hash of uri
  val uri: String, val displayName: String, val parentFolder: String,
  val type: MediaType,               // VIDEO | AUDIO
  val durationMs: Long, val sizeBytes: Long, val dateModified: Long,
  val width: Int?, val height: Int?, val mimeType: String?,
  val isPrivate: Boolean = false, val isHidden: Boolean = false,
)

@Entity data class PlaybackState(
  @PrimaryKey val mediaId: String,
  val positionMs: Long, val lastPlayed: Long,
  val watched: Boolean, val lastDecoder: String?,     // HW/SW
  val subtitleOffsetMs: Long = 0, val selectedAudioTrack: Int? = null,
  val selectedSubtitle: String? = null,
)

@Entity data class Playlist(@PrimaryKey val id: String, val name: String, val createdAt: Long)
@Entity data class PlaylistItem(val playlistId: String, val mediaId: String, val order: Int)
@Entity data class NetworkServer(
  @PrimaryKey val id: String, val protocol: String, val host: String,
  val port: Int, val username: String?, val encryptedPassword: String?, val path: String?,
)
@Entity data class AudioMeta(
  @PrimaryKey val mediaId: String,
  val title: String?, val artist: String?, val album: String?,
  val albumArtist: String?, val genre: String?, val trackNo: Int?, val artUri: String?,
)
```
Add a Room **FTS4** table over `displayName`/audio metadata for search.

## Config schemas (DataStore / MMKV)
- **GestureConfig** — see gesture/GestureModels.kt (bindings, speedHold, precisionSeek, haptics).
- **ThemeConfig** — baseTheme, useDynamicColor, manualAccent, skin, controlBarOrder, ambientBacklight, adaptiveFullscreenDim.
- **SubtitleDefaults** — enabled, languagePriority, stylePreset + overrides, encoding.
- **PlaybackDefaults** — decoderMode, resumeBehavior, autoplayNext, defaultSpeed, defaultZoom, seekSteps.

## Serialization
- Persist config objects as JSON (kotlinx.serialization). Version each schema; write migrations.
