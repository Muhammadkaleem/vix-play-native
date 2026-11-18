package com.devbytes.vixplayer.app.ui.premium

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.ui.theme.BrandEnd
import com.devbytes.vixplayer.app.ui.theme.BrandStart
import com.devbytes.vixplayer.app.ui.theme.OnScrim
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val GoldLightScreen = Color(0xFFFBE9A8)
private val GoldDeepScreen = Color(0xFFE0A32E)

/**
 * The dedicated Vix Pro Rewards screen: live status, Watch-Ad-to-earn, the accumulation table,
 * the Pro benefits, and the (paid) upgrade placeholder. Reached from Settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProRewardsScreen(
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
    viewModel: PremiumViewModel = hiltViewModel(),
) {
    val isProActive by viewModel.isProActive.collectAsStateWithLifecycle()
    val proUntil by viewModel.proUntil.collectAsStateWithLifecycle()
    val totalEarned by viewModel.totalEarned.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val rewardedLoading by viewModel.rewardedLoading.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.logScreenViewed()
        viewModel.preloadRewarded()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vix Pro Rewards") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_back), contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(painterResource(R.drawable.ic_history), contentDescription = "Reward history")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatusCard(isProActive, proUntil, totalEarned)

            EarnCard(
                rewardLabel = viewModel.rewardLabel,
                loading = rewardedLoading,
                onWatch = {
                    val activity = context.findActivity()
                    if (activity == null) {
                        Toast.makeText(context, "Can't show the ad right now", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.watchAdForPro(activity, entryPoint = "rewards_screen") { result ->
                            val msg = when (result) {
                                PremiumViewModel.Result.Granted -> "Pro extended by ${viewModel.rewardLabel}"
                                PremiumViewModel.Result.Unavailable -> "No ad available — try again shortly"
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
            )

            AccumulationCard()

            SectionLabel("Pro benefits")
            CardSurface { ProBenefits(Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) }

            SectionLabel("Upgrade")
            PaidUpgradeRow()

            Text(
                "Rewarded Pro is earned by watching opt-in ads and is used only inside VixPlay. " +
                    "It keeps working offline until it expires.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun StatusCard(isProActive: Boolean, proUntil: Long, totalEarned: Int) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(proUntil, isProActive) {
        while (isProActive && System.currentTimeMillis() < proUntil) {
            now = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
        now = System.currentTimeMillis()
    }
    val remaining = (proUntil - now).coerceAtLeast(0)
    val d = remaining / 86_400_000
    val h = (remaining % 86_400_000) / 3_600_000
    val m = (remaining % 3_600_000) / 60_000
    val s = (remaining % 60_000) / 1000
    val remainingText = if (d > 0) "%dd %02d:%02d:%02d".format(d, h, m, s) else "%02d:%02d:%02d".format(h, m, s)
    val statusText = when {
        isProActive -> "Active"
        proUntil > 0 -> "Expired"
        else -> "Inactive"
    }

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF14151E), Color(0xFF1A1630), Color(0xFF241A3C)),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
            .padding(20.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(
                    painterResource(R.drawable.ic_crown),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(44.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        "Vix Pro",
                        style = MaterialTheme.typography.titleLarge.copy(
                            brush = Brush.horizontalGradient(listOf(GoldLightScreen, GoldDeepScreen)),
                        ),
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isProActive) Color(0xFF7CE7A6) else Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            if (isProActive) {
                Spacer(Modifier.height(16.dp))
                Text("Time remaining", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.6f))
                Text(
                    remainingText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnScrim,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Expires ${formatDateTime(proUntil)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                )
            } else {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Watch an ad below to activate Pro.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "$totalEarned reward${if (totalEarned == 1) "" else "s"} earned",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.55f),
            )
        }
    }
}

@Composable
private fun EarnCard(rewardLabel: String, loading: Boolean, onWatch: () -> Unit) {
    CardSurface {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Earn more Pro time",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Watch one short ad and get $rewardLabel of Pro, added on top of any time you already have.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onWatch,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Icon(painterResource(R.drawable.ic_play), null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    if (loading) "Loading ad…" else "Watch Ad · Earn $rewardLabel",
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun AccumulationCard() {
    CardSurface {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Watch more, earn more",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(10.dp))
            RewardConfig.tiers.forEach { (ads, total) ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "$ads ad${if (ads == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        total,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun PaidUpgradeRow() {
    CardSurface {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                painterResource(R.drawable.ic_crown),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(32.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    "Lifetime Pro",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "One-time purchase, no ads ever — coming soon",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                Modifier.clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    "Soon",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
    )
}

@Composable
private fun CardSurface(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface),
    ) { content() }
}

internal fun formatDateTime(epochMs: Long): String =
    SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(epochMs))

internal fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
