package com.devbytes.vixplayer.app.briefing.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.briefing.BriefingConfig
import com.devbytes.vixplayer.app.briefing.BriefingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class BriefingHomeCardViewModel @Inject constructor(
    repository: BriefingRepository,
) : ViewModel() {
    val config: StateFlow<BriefingConfig> = repository.config
        .stateIn(viewModelScope, SharingStarted.Eagerly, BriefingConfig())
}

/**
 * A dismissible "your morning brief is ready" card for the Video home. Self-hides unless the
 * briefing is live and it's still morning (before noon local), so it never lingers all day.
 */
@Composable
fun BriefingHomeCard(
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BriefingHomeCardViewModel = hiltViewModel(),
) {
    val config by viewModel.config.collectAsState()
    var dismissed by remember { mutableStateOf(false) }
    val beforeNoon = LocalTime.now().hour < 12

    if (!config.isLive || !beforeNoon || dismissed) return

    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).clickable(onClick = onOpen),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Your morning brief", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("Tap to see today at a glance.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { dismissed = true }) {
                Icon(painterResource(R.drawable.ic_close), contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
