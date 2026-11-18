package com.devbytes.vixplayer.app.ui.dashboard

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devbytes.vixplayer.app.ui.library.VideoWithPosition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFramePercent
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.data.repository.VideoFile
import com.devbytes.vixplayer.app.ui.premium.PremiumCard
import com.devbytes.vixplayer.app.ui.premium.PremiumViewModel
import com.devbytes.vixplayer.app.ui.premium.ProDetailsSheet
import com.devbytes.vixplayer.app.ui.premium.ProSuccessSheet
import com.devbytes.vixplayer.app.ui.library.VideoHeaderTitle
import com.devbytes.vixplayer.app.ui.library.components.VideoCard
import com.devbytes.vixplayer.app.ui.library.components.formatDuration
import com.devbytes.vixplayer.app.ui.theme.BrandEnd
import com.devbytes.vixplayer.app.ui.theme.BrandStart
import com.devbytes.vixplayer.app.ui.theme.Error
import com.devbytes.vixplayer.app.ui.theme.OnScrim
import com.devbytes.vixplayer.app.ui.theme.Success
import com.devbytes.vixplayer.app.ui.theme.Warning
import com.devbytes.vixplayer.app.ui.theme.ScrimStrong
import com.devbytes.vixplayer.app.ui.theme.TrackInactive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onVideoClick: (VideoFile) -> Unit = {},
    onFolderClick: (bucketId: Long, name: String) -> Unit = { _, _ -> },
    onBrowseFolders: () -> Unit = {},
    onViewAll: (DashboardSource) -> Unit = {},
    onOpenShorts: () -> Unit = {},
    onSearch: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel(),
    premiumViewModel: PremiumViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isProActive by premiumViewModel.isProActive.collectAsStateWithLifecycle()
    val proUntil by premiumViewModel.proUntil.collectAsStateWithLifecycle()
    val rewardedLoading by premiumViewModel.rewardedLoading.collectAsStateWithLifecycle()

    val context = LocalContext.current
    // null = no modal; "details" = Pro perks + watch-ad; "success" = Pro unlocked.
    var premiumModal by remember { mutableStateOf<String?>(null) }

    // Shared rewarded-ad launch — used by the card's gold button and the details modal.
    val startWatchAd: (String) -> Unit = { entryPoint ->
        val activity = context.findActivity()
        if (activity == null) {
            android.widget.Toast.makeText(context, "Can't show the ad right now", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            premiumViewModel.watchAdForPro(activity, entryPoint) { result ->
                when (result) {
                    PremiumViewModel.Result.Granted -> premiumModal = "success"
                    PremiumViewModel.Result.Unavailable ->
                        android.widget.Toast.makeText(context, "No ad available — try again shortly", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Re-read every time the dashboard comes to the foreground (tab re-selection, returning from
    // the player), so favourites / pins / new videos / resume positions reflect back — a plain
    // LaunchedEffect(Unit) only fires on first composition and would show stale data.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { VideoHeaderTitle() },
                actions = {
                    if (isProActive) ProHeaderBadge()
                    IconButton(onClick = onSearch) {
                        Icon(painterResource(R.drawable.ic_search), contentDescription = "Search")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 1. Continue watching — swipeable carousel of up to 5 in-progress videos.
            if (state.continueWatching.isNotEmpty()) {
                item { ContinueWatchingCarousel(state.continueWatching, onResume = onVideoClick) }
            }

            // 2. From this folder — sits right under Continue Watching (no header, just the rail).
            if (state.fromThisFolder.isNotEmpty()) {
                item { FolderPillRow(state.fromThisFolder, onVideoClick) }
            }

            // 3. Pro — rewarded-ad offer (or the active-entitlement card).
            item {
                // Warm a rewarded ad only once the offer is actually on screen.
                LaunchedEffect(Unit) { premiumViewModel.preloadRewarded() }
                PremiumCard(
                    isProActive = isProActive,
                    proUntil = proUntil,
                    loading = rewardedLoading,
                    onWatchAd = { startWatchAd("home_card") },
                    onOpenDetails = { premiumModal = "details" },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            // 3. How much time do you have? — pick a window, see videos that finish within it.
            if (state.timeWindowPool.isNotEmpty()) {
                item { TimeWindowSection(state.timeWindowPool, onVideoClick) }
            }

            // 4. New on this device — recently added (last 7 days), top 5 + "View all".
            if (state.newOnDevice.isNotEmpty()) {
                item {
                    NewOnDeviceSection(
                        videos = state.newOnDevice,
                        onPlay = onVideoClick,
                        onFavorite = { viewModel.toggleFavorite(it.mediaStoreId) },
                        onShare = { shareVideo(context, it) },
                        onViewAll = { onViewAll(DashboardSource.NEW_ON_DEVICE) },
                    )
                }
            }

            // 6. Your folders
            if (state.folders.isNotEmpty()) {
                item { SectionHeader("Your folders", onViewAll = onBrowseFolders, iconRes = R.drawable.ic_folder) }
                items2(state.folders) { pf ->
                    FolderCard(
                        name = pf.folder.name,
                        count = pf.folder.videoCount,
                        sampleUri = pf.folder.sampleUri,
                        isPinned = pf.isPinned,
                        onClick = { onFolderClick(pf.folder.bucketId, pf.folder.name) },
                        onLongClick = { viewModel.togglePin(pf.folder.bucketId) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Your Shorts — sits right under Your folders. Opens the TikTok-style vertical feed.
            if (state.shortsCount > 0) {
                item { SectionHeader("Your Shorts", onViewAll = null, iconRes = R.drawable.ic_play) }
                item {
                    ShortsCard(
                        samples = state.shortsSamples,
                        count = state.shortsCount,
                        onClick = onOpenShorts,
                    )
                }
            }

            // 4b. HD & 4K — highest-resolution videos in storage (top 5), "View all" for the rest.
            if (state.highRes.isNotEmpty()) {
                item { SectionHeader("HD & 4K", onViewAll = { onViewAll(DashboardSource.HIGH_RES) }, iconRes = R.drawable.ic_aspect) }
                item { VideoRail(state.highRes, onVideoClick) }
            }

            // 5. Favorites
            if (state.favorites.isNotEmpty()) {
                item { SectionHeader("Favorites", onViewAll = { onViewAll(DashboardSource.FAVORITES) }, iconRes = R.drawable.ic_favorite) }
                items(state.favorites.size) { i ->
                    val video = state.favorites[i]
                    FavoriteRow(
                        video = video,
                        onClick = { onVideoClick(video) },
                        onUnfavorite = { viewModel.toggleFavorite(video.mediaStoreId) },
                    )
                }
            }

            // 7. Your library — overview stats + category breakdown (last on the dashboard).
            state.stats?.let { s ->
                if (s.videoCount > 0) {
                    item {
                        LibraryStatsCard(
                            stats = s,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }

        when (premiumModal) {
            "details" -> ProDetailsSheet(
                onDismiss = { premiumModal = null },
                loading = rewardedLoading,
                onWatchAd = { premiumModal = null; startWatchAd("home_modal") },
            )
            "success" -> ProSuccessSheet(onDismiss = { premiumModal = null })
        }
    }
}

/** Gold "PRO" pill shown in the dashboard top bar while a Pro entitlement is active. */
@Composable
private fun ProHeaderBadge() {
    Row(
        modifier = Modifier
            .padding(end = 4.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0xFFE4B968).copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            painterResource(R.drawable.ic_crown_outline),
            contentDescription = "Pro active",
            tint = Color(0xFFE4B968),
            modifier = Modifier.size(15.dp),
        )
        Text(
            "PRO",
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.5.sp),
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE4B968),
        )
    }
}

/** Unwraps the hosting Activity from a Compose context (for showing a full-screen rewarded ad). */
private fun android.content.Context.findActivity(): android.app.Activity? {
    var ctx: android.content.Context? = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is android.app.Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/** Lays [items] out two-per-row inside a LazyColumn (can't nest a LazyVerticalGrid). */
private fun <T> androidx.compose.foundation.lazy.LazyListScope.items2(
    items: List<T>,
    content: @Composable androidx.compose.foundation.layout.RowScope.(T) -> Unit,
) {
    val rows = items.chunked(2)
    items(rows.size) { r ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            rows[r].forEach { content(it) }
            if (rows[r].size == 1) androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun SectionHeader(title: String, onViewAll: (() -> Unit)?, iconRes: Int? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconRes != null) {
            Icon(
                painterResource(iconRes),
                contentDescription = null,
                tint = Error, // consistent with the "New on this device" / "How much time" headers
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        if (onViewAll != null) {
            TextButton(onClick = onViewAll) { Text("View all") }
        }
    }
}

@Composable
private fun thumbRequest(uri: Any?) = ImageRequest.Builder(LocalContext.current)
    .data(uri)
    .decoderFactory(VideoFrameDecoder.Factory())
    .videoFramePercent(0.1)
    .build()

/**
 * Swipeable Continue Watching carousel — up to 5 in-progress videos (newest first). Each slide
 * is the cinematic card (full-bleed frame, bottom-to-top gradient, overlaid title/progress and a
 * circular Resume button); pagination dots sit inside the card at the bottom centre. Horizontal
 * swipe pages the carousel; vertical drags fall through to the dashboard's own scroll.
 */
@Composable
private fun ContinueWatchingCarousel(videos: List<VideoWithPosition>, onResume: (VideoFile) -> Unit) {
    val pager = rememberPagerState(pageCount = { videos.size })
    Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        HorizontalPager(state = pager) { page ->
            ContinueSlide(videos[page], onResume)
        }
        if (videos.size > 1) {
            PagerDots(
                count = videos.size,
                current = pager.currentPage,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
            )
        }
    }
}

@Composable
private fun ContinueSlide(item: VideoWithPosition, onResume: (VideoFile) -> Unit) {
    val video = item.video
    val accent = MaterialTheme.colorScheme.primary
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1.9f)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onResume(video) },
    ) {
        AsyncImage(
            model = thumbRequest(video.uri),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // Cinematic bottom-to-top gradient: clear at the top, darkening to near-opaque at the
        // bottom so the title/progress stay readable without a separate container.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.0f to Color.Transparent,
                    0.28f to Color.Black.copy(alpha = 0.22f),
                    0.6f to Color.Black.copy(alpha = 0.62f),
                    1.0f to Color.Black.copy(alpha = 0.94f),
                ),
            ),
        )
        // Pill label, top-left — dark backing + sparkle icon, matching the section-header style.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 12.dp, vertical = 7.dp),
        ) {
            Icon(
                painterResource(R.drawable.ic_sparkle),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(15.dp),
            )
            Text(
                "Continue Watching",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = OnScrim,
            )
        }
        // Bottom info (left of the play button).
        Column(
            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 16.dp, end = 82.dp, bottom = 30.dp),
        ) {
            Text(
                video.name.substringBeforeLast('.'),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = OnScrim,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(10.dp))
            // Gradient progress bar with the remaining time to its right.
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(
                    Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp))
                        .background(OnScrim.copy(alpha = 0.3f)),
                ) {
                    Box(
                        Modifier.fillMaxWidth(item.progressFraction.coerceIn(0f, 1f)).height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Brush.horizontalGradient(listOf(BrandStart, BrandEnd))),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    remainingLabel(item.positionMs, video.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnScrim.copy(alpha = 0.85f),
                    maxLines = 1,
                )
            }
        }
        // Secondary "glass" Resume button — translucent frosted circle, not a solid accent fill.
        // Tap-only; a swipe over it pages the carousel instead.
        Box(
            Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 26.dp)
                .size(50.dp).clip(CircleShape)
                .background(OnScrim.copy(alpha = 0.16f))
                .border(1.dp, OnScrim.copy(alpha = 0.4f), CircleShape)
                .clickable { onResume(video) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_play), "Resume", tint = OnScrim, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun PagerDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            val active = i == current
            val w by animateDpAsState(if (active) 16.dp else 5.dp, label = "dotWidth")
            Box(
                Modifier.width(w).height(5.dp).clip(RoundedCornerShape(3.dp))
                    .background(if (active) Color.White else Color.White.copy(alpha = 0.35f)),
            )
        }
    }
}

@Composable
private fun FolderPillRow(videos: List<VideoWithPosition>, onClick: (VideoFile) -> Unit) {
    androidx.compose.foundation.lazy.LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(videos.size) { i -> FolderPillCard(videos[i]) { onClick(videos[i].video) } }
    }
}

/** Horizontal "pill" card: thumbnail (with a red resume bar if in progress) + title + time. */
@Composable
private fun FolderPillCard(item: VideoWithPosition, onClick: () -> Unit) {
    val video = item.video
    val inProgress = item.progressFraction in 0.02f..0.97f
    Row(
        Modifier.width(280.dp).clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(104.dp).height(64.dp).clip(RoundedCornerShape(12.dp))) {
            AsyncImage(
                model = thumbRequest(video.uri),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (inProgress) {
                Box(
                    Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp)
                        .background(Color.White.copy(alpha = 0.25f)),
                ) {
                    Box(
                        Modifier.fillMaxWidth(item.progressFraction.coerceIn(0f, 1f)).height(3.dp)
                            .background(Error),
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                video.name.substringBeforeLast('.'),
                // Allow up to 2 lines; smaller type so a wrapped title still fits the card height.
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 16.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                if (inProgress) {
                    "${clockLabel((video.durationMs - item.positionMs).coerceAtLeast(0))} left"
                } else {
                    clockLabel(video.durationMs)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun VideoRail(videos: List<VideoFile>, onClick: (VideoFile) -> Unit) {
    androidx.compose.foundation.lazy.LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(videos.size) { i ->
            CinematicVideoCard(videos[i], modifier = Modifier.width(156.dp), onClick = { onClick(videos[i]) })
        }
    }
}

/**
 * Compact sibling of the Continue Watching card — same cinematic language (full-card frame +
 * bottom-to-top gradient + overlaid info, no separate metadata block), just smaller: a 4:5
 * poster so more fit per row. Used by "From this folder", "Recently added", and "HD & 4K".
 */
@Composable
private fun CinematicVideoCard(video: VideoFile, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .aspectRatio(4f / 5f)
            .clip(RoundedCornerShape(13.dp))
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = thumbRequest(video.uri),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.0f to Color.Transparent,
                    0.45f to Color.Black.copy(alpha = 0.15f),
                    1.0f to Color.Black.copy(alpha = 0.88f),
                ),
            ),
        )
        // Subtle quality badge, top-right.
        qualityLabel(video.width, video.height)?.let { q ->
            Box(
                Modifier.align(Alignment.TopEnd).padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(ScrimStrong)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    q,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = OnScrim,
                )
            }
        }
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp)) {
            Text(
                video.name.substringBeforeLast('.'),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, lineHeight = 15.sp),
                fontWeight = FontWeight.SemiBold,
                color = OnScrim,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${formatDuration(video.durationMs)} • ${formatSize(video.sizeBytes)}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                color = OnScrim.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Category colours for the library breakdown bar + legend (by size rank). */
private val categoryColors = listOf(Error, Warning, Success)

@Composable
private fun categoryColor(index: Int) =
    categoryColors.getOrNull(index) ?: MaterialTheme.colorScheme.onSurfaceVariant

/**
 * "Your library" overview: a drive header + Scan, three stat tiles (videos / size / unwatched),
 * a stacked category bar, and a 2-column legend. All values are live from MediaStore + history.
 */
@Composable
private fun LibraryStatsCard(
    stats: DashboardViewModel.LibraryStats,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_folder), null, tint = Error, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text("Your library", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(Modifier.weight(1f), "%,d".format(stats.videoCount), "videos")
            StatTile(Modifier.weight(1f), formatSize(stats.totalBytes), "on device")
            StatTile(Modifier.weight(1f), "${stats.unwatchedMs / 3_600_000}h", "unwatched")
        }
        if (stats.categories.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            val total = stats.categories.sumOf { it.count }.coerceAtLeast(1)
            Row(
                Modifier.fillMaxWidth().height(8.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                stats.categories.forEachIndexed { i, c ->
                    Box(
                        Modifier.weight(c.count.toFloat().coerceAtLeast(0.01f))
                            .fillMaxHeight().clip(RoundedCornerShape(50)).background(categoryColor(i)),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            stats.categories.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    pair.forEachIndexed { j, c ->
                        val globalIndex = stats.categories.indexOf(c)
                        LegendItem(Modifier.weight(1f), c.name, c.count, categoryColor(globalIndex))
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatTile(modifier: Modifier, value: String, label: String) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LegendItem(modifier: Modifier, name: String, count: Int, color: Color) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text("%,d".format(count), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private data class TimeWindow(val label: String, val maxMs: Long)

private val timeWindows = listOf(
    TimeWindow("15 min", 15L * 60_000L),
    TimeWindow("30 min", 30L * 60_000L),
    TimeWindow("1 hour", 60L * 60_000L),
    TimeWindow("All night", 8L * 3_600_000L),
)

/**
 * "How much time do you have?" — a window picker (15 min … All night) over a rail of videos
 * that finish within the chosen window. Filters the pre-loaded pool client-side, so switching
 * windows is instant (no re-query).
 */
@Composable
private fun TimeWindowSection(pool: List<VideoFile>, onClick: (VideoFile) -> Unit) {
    var selected by remember { mutableStateOf(0) }
    val filtered = remember(pool, selected) {
        pool.filter { it.durationMs in 1..timeWindows[selected].maxMs }.take(12)
    }
    Column(Modifier.padding(top = 10.dp)) {
        // Header.
        Row(
            Modifier.padding(start = 16.dp, end = 16.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                painterResource(R.drawable.ic_timer),
                contentDescription = null,
                tint = Error,
                modifier = Modifier.size(22.dp),
            )
            Column {
                Text("How much time do you have?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Videos that finish within your window",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        // Segmented window picker.
        val onSurface = MaterialTheme.colorScheme.onSurface
        Row(
            Modifier.padding(horizontal = 16.dp).clip(RoundedCornerShape(12.dp))
                .background(onSurface.copy(alpha = 0.06f)).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            timeWindows.forEachIndexed { i, w ->
                val active = i == selected
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(9.dp))
                        .background(if (active) onSurface.copy(alpha = 0.14f) else Color.Transparent)
                        .clickable { selected = i }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        w.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (active) onSurface else onSurface.copy(alpha = 0.55f),
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (filtered.isEmpty()) {
            Text(
                "No videos finish within ${timeWindows[selected].label}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        } else {
            androidx.compose.foundation.lazy.LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(filtered.size) { i -> TimeCard(filtered[i], onClick = { onClick(filtered[i]) }) }
            }
        }
    }
}

/** Landscape card for the time-window rail: thumbnail + quality/duration badges, title below. */
@Composable
private fun TimeCard(video: VideoFile, onClick: () -> Unit) {
    Column(Modifier.width(236.dp).clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(14.dp))) {
            AsyncImage(
                model = thumbRequest(video.uri),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0.55f to Color.Transparent, 1.0f to Color.Black.copy(alpha = 0.55f)),
                ),
            )
            qualityLabel(video.width, video.height)?.let { q ->
                BadgePill(q, Modifier.align(Alignment.TopStart).padding(8.dp))
            }
            BadgePill(clockLabel(video.durationMs), Modifier.align(Alignment.BottomEnd).padding(8.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            video.name.substringBeforeLast('.'),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            folderExtLabel(video),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BadgePill(text: String, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(7.dp)).background(ScrimStrong).padding(horizontal = 8.dp, vertical = 3.dp)) {
        Text(text, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), fontWeight = FontWeight.SemiBold, color = OnScrim)
    }
}

/**
 * "New on this device" — a vertical list of videos added in the last 7 days, with a count pill,
 * thumbnail + duration badge, rich metadata (folder · quality · size · when), and a ⋮ menu.
 */
@Composable
private fun NewOnDeviceSection(
    videos: List<VideoFile>,
    onPlay: (VideoFile) -> Unit,
    onFavorite: (VideoFile) -> Unit,
    onShare: (VideoFile) -> Unit,
    onViewAll: () -> Unit,
) {
    Column(Modifier.padding(top = 12.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painterResource(R.drawable.ic_sparkle),
                contentDescription = null,
                tint = Error,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("New on this device", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(Error.copy(alpha = 0.16f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    "${videos.size} new",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Error,
                )
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onViewAll) { Text("View all") }
        }
        Spacer(Modifier.height(4.dp))
        videos.take(5).forEach { v ->
            NewRow(v, onPlay, onFavorite, onShare)
            androidx.compose.material3.HorizontalDivider(
                modifier = Modifier.padding(start = 16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            )
        }
    }
}

@Composable
private fun NewRow(
    video: VideoFile,
    onPlay: (VideoFile) -> Unit,
    onFavorite: (VideoFile) -> Unit,
    onShare: (VideoFile) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clickable { onPlay(video) }.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(96.dp).height(54.dp).clip(RoundedCornerShape(10.dp))) {
            AsyncImage(
                model = thumbRequest(video.uri),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            BadgePill(clockLabel(video.durationMs), Modifier.align(Alignment.BottomEnd).padding(4.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                video.name.substringBeforeLast('.'),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                newMeta(video),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(
                    painterResource(R.drawable.ic_more_vert),
                    contentDescription = "More",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            androidx.compose.material3.DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text("Play") },
                    onClick = { menu = false; onPlay(video) },
                )
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text("Add to favorites") },
                    onClick = { menu = false; onFavorite(video) },
                )
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text("Share") },
                    onClick = { menu = false; onShare(video) },
                )
            }
        }
    }
}

/** "Camera · 1080p · 412 MB · 2h" for the New-on-device rows. */
private fun newMeta(video: VideoFile): String {
    val folder = video.path.substringBeforeLast('/', "").substringAfterLast('/', "")
    return listOfNotNull(
        folder.ifBlank { null },
        qualityLabel(video.width, video.height),
        formatSize(video.sizeBytes),
        relativeAdded(video.dateAddedMs).ifBlank { null },
    ).joinToString(" · ")
}

/** Compact "2h" / "Yesterday" / "3d" from an added-at timestamp. */
private fun relativeAdded(ms: Long): String {
    if (ms <= 0) return ""
    val diff = System.currentTimeMillis() - ms
    val min = diff / 60_000
    val h = diff / 3_600_000
    val d = diff / 86_400_000
    return when {
        d >= 2 -> "${d}d"
        d >= 1 -> "Yesterday"
        h >= 1 -> "${h}h"
        min >= 1 -> "${min}m"
        else -> "Just now"
    }
}

/** Fires an ACTION_SEND chooser for a video's MediaStore URI. */
private fun shareVideo(context: android.content.Context, video: VideoFile) {
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "video/*"
        putExtra(android.content.Intent.EXTRA_STREAM, video.uri)
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(android.content.Intent.createChooser(intent, "Share video")) }
}

/** "45s" / "9m" / "1h 20m" from a duration in ms. */
private fun clockLabel(ms: Long): String {
    val totalSec = (ms / 1000).toInt()
    if (totalSec < 60) return "${totalSec}s"
    val totalMin = totalSec / 60
    val h = totalMin / 60
    val m = totalMin % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

/** "Camera · MP4" from a video's folder + extension. */
private fun folderExtLabel(video: VideoFile): String {
    val folder = video.path.substringBeforeLast('/', "").substringAfterLast('/', "")
    val ext = video.name.substringAfterLast('.', "").uppercase()
    return listOf(folder, ext).filter { it.isNotBlank() }.joinToString(" · ")
}

/** Compact quality badge label from pixel dimensions; null below 720p / when unknown. */
private fun qualityLabel(width: Int, height: Int): String? {
    val p = minOf(width, height)
    return when {
        p >= 2160 -> "4K"
        p >= 1080 -> "1080p"
        p >= 720 -> "720p"
        else -> null
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderCard(
    name: String,
    count: Int,
    sampleUri: Any?,
    isPinned: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AsyncImage(
                model = thumbRequest(sampleUri),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(width = 64.dp, height = 44.dp).clip(RoundedCornerShape(6.dp)),
            )
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isPinned) Text("📌 ", style = MaterialTheme.typography.bodySmall)
                    Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text("$count ${if (count == 1) "video" else "videos"}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun FavoriteRow(video: VideoFile, onClick: () -> Unit, onUnfavorite: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .combinedClickableSafe(onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model = thumbRequest(video.uri),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(width = 72.dp, height = 44.dp).clip(RoundedCornerShape(6.dp)),
        )
        Text(video.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        IconButton(onClick = onUnfavorite) {
            Icon(painterResource(R.drawable.ic_favorite), contentDescription = "Remove from favorites",
                tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ShortsCard(samples: List<VideoFile>, count: Int, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF3C3658), Color(0xFF272338))))
            .combinedClickableSafe(onClick)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            // Overlapping "deck" of the latest 3 vertical thumbnails (first on top).
            Box(Modifier.size(width = 86.dp, height = 66.dp)) {
                samples.take(3).forEachIndexed { i, v ->
                    AsyncImage(
                        model = thumbRequest(v.uri),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .zIndex((3 - i).toFloat())
                            .offset(x = (i * 22).dp)
                            .size(width = 42.dp, height = 66.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(2.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "Your vertical videos",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = OnScrim,
                )
                Text(
                    "$count ${if (count == 1) "video" else "videos"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnScrim.copy(alpha = 0.7f),
                )
            }
            Icon(
                painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = OnScrim.copy(alpha = 0.8f),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.combinedClickableSafe(onClick: () -> Unit): Modifier =
    this.combinedClickable(onClick = onClick, onLongClick = null)

/** m:ss / h:mm:ss for the resume position. */
private fun formatClock(ms: Long): String {
    val s = ms / 1000
    val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}

/** "12m left" / "1h 5m left" from the current position to the end. */
private fun remainingLabel(positionMs: Long, durationMs: Long): String {
    val rem = ((durationMs - positionMs).coerceAtLeast(0)) / 1000
    val h = rem / 3600; val m = (rem % 3600) / 60
    return when {
        h > 0 -> "${h}h ${m}m left"
        m > 0 -> "${m}m left"
        else -> "${rem}s left"
    }
}

/** "2.4 GB" / "856 MB" / "512 KB". */
private fun formatSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> "%.1f GB".format(gb)
        mb >= 1.0 -> "%.0f MB".format(mb)
        else -> "%.0f KB".format(kb)
    }
}
