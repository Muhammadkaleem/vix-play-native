package com.devbytes.vixplayer.app.briefing.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.briefing.Brief
import com.devbytes.vixplayer.app.briefing.BriefEvent
import com.devbytes.vixplayer.app.ui.library.components.LibraryEmptyState
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BriefingScreen(
    onBack: () -> Unit = {},
    sample: Boolean = false,
    viewModel: BriefingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    val calendarPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.load() }

    LaunchedEffect(Unit) { viewModel.load() }

    val dateLine = remember { DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault()).format(java.time.LocalDate.now()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (sample) "Sample brief" else "Today") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_back), contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        when (val s = state) {
            BriefingViewModel.State.Loading ->
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }

            BriefingViewModel.State.NeedsPermission ->
                LibraryEmptyState(
                    iconRes = R.drawable.ic_timer,
                    title = "Connect your calendar",
                    body = "VixPlay reads today's events to build your brief. It stays on your device.",
                    primaryLabel = "Grant access",
                    onPrimary = { calendarPermission.launch(Manifest.permission.READ_CALENDAR) },
                    modifier = Modifier.padding(padding),
                )

            is BriefingViewModel.State.Ready ->
                BriefingContent(s.brief, sample, dateLine, Modifier.padding(padding))
        }
    }
}

/** The brief body, shared by the standalone screen and the wizard's Sample step. */
@Composable
fun BriefingContent(brief: Brief, sample: Boolean, dateLine: String, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (sample) item { SampleWatermark() }
        item { Header(dateLine, brief.daySummary) }
        if (brief.allDayEvents.isNotEmpty()) {
            item { AllDayStrip(brief.allDayEvents) }
        }
        items(brief.callouts) { Callout(it.text) }
        items(brief.timedEvents) { EventRow(it) }
        item { Footer(brief.isEmpty) }
    }
}

@Composable
private fun SampleWatermark() {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            "SAMPLE — not yet live",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun Header(dateLine: String, daySummary: String) {
    Column {
        Text(dateLine, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Good morning", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(daySummary, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun AllDayStrip(events: List<BriefEvent>) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            "All day: " + events.joinToString(" · ") { it.title },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Composable
private fun Callout(text: String) {
    Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("⚠️  ", style = MaterialTheme.typography.bodyMedium)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

private val timeFmt = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()).withZone(ZoneId.systemDefault())

@Composable
private fun EventRow(event: BriefEvent) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            timeFmt.format(event.start),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 2.dp),
        )
        Column(Modifier.fillMaxWidth()) {
            Text(event.title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            val meta = buildList {
                event.location?.let { add(it) }
                if (event.hasConferenceLink) add("Join link ↗")
                val mins = ChronoUnit.MINUTES.between(event.start, event.end)
                if (mins in 1..1440) add("${mins} min")
            }.joinToString(" · ")
            if (meta.isNotBlank()) {
                Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Footer(empty: Boolean) {
    Text(
        if (empty) "Enjoy the open day." else "That's everything. Have a good one.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}
