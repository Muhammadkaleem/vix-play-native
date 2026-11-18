package com.devbytes.vixplayer.app.ui.premium

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.ui.theme.BrandEnd
import com.devbytes.vixplayer.app.ui.theme.BrandStart
import com.devbytes.vixplayer.app.ui.theme.OnScrim
import com.devbytes.vixplayer.app.ui.theme.Success

/** A single Pro perk with its neon icon + one-line description. */
private data class ProFeature(val icon: Int, val title: String, val subtitle: String)

/**
 * What Pro actually changes — only ads. Every player feature (background play, equalizer, speed,
 * sleep timer, every format) is free for everyone, so it is listed as such rather than sold as a
 * Pro perk.
 */
private val proFeatures = listOf(
    ProFeature(R.drawable.ic_pro_noads, "No banner or inline ads", "A clean Home, folders, search and history"),
    ProFeature(R.drawable.ic_pro_noads, "No full-screen ads", "Nothing between you and the next video"),
    ProFeature(R.drawable.ic_pro_formats, "Everything else stays free", "Background play, equalizer, speed, sleep timer and every format — Pro or not"),
)

private val GoldLabel = Color(0xFFE4B968)
private val GoldRing = Color(0xFFE0B257)
private val GoldBtnStart = Color(0xFFEBCB82)
private val GoldBtnEnd = Color(0xFFD2A24E)
private val GoldGlow = Color(0xFFE8B86A)
private val BtnTextDark = Color(0xFF201703)
private val CardDark = Color(0xFF141210)

private data class BenefitChip(val icon: Int, val label: String)

// Pro removes ads; nothing else is gated, so the chips only describe that.
private val benefitChips = listOf(
    BenefitChip(R.drawable.ic_no_ads, "No banners"),
    BenefitChip(R.drawable.ic_no_ads, "No full-screen ads"),
)

/**
 * Dashboard entry point for Pro — a dark "premium" card with a warm gold glow (shining left→right)
 * and a slow shine sweep. Offer state: "Watch 1 ad, get 8 hours of Pro" + the Unlock button.
 * Active state: a crown with a fill ring, a live "Hh MMm" balance + "bank up to 48h", and Extend.
 * The gold button runs the rewarded-ad flow directly; tapping the card body opens the details modal.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PremiumCard(
    isProActive: Boolean,
    proUntil: Long,
    loading: Boolean,
    onWatchAd: () -> Unit,
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(isProActive, proUntil) {
        while (isProActive && System.currentTimeMillis() < proUntil) {
            now = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
        now = System.currentTimeMillis()
    }
    val remaining = (proUntil - now).coerceAtLeast(0)

    val shineAnim = rememberInfiniteTransition(label = "shine")
    val shine by shineAnim.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Restart),
        label = "shineX",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardDark)
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
            .clickable { onOpenDetails() },
    ) {
        // Warm gold glow shining from the top-left corner.
        Box(
            Modifier.matchParentSize().background(
                Brush.radialGradient(
                    colors = listOf(GoldGlow.copy(alpha = 0.30f), Color.Transparent),
                    center = Offset(0f, 0f),
                    radius = 900f,
                ),
            ),
        )
        // Subtle left→right shine sweep.
        Box(
            Modifier.matchParentSize().drawBehind {
                val bandW = size.width * 0.26f
                val x = shine * (size.width + bandW) - bandW
                drawRect(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, GoldGlow.copy(alpha = 0.07f), Color.Transparent),
                        startX = x,
                        endX = x + bandW,
                    ),
                )
            },
        )

        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CrownBadge(
                    progress = if (isProActive) {
                        (remaining.toFloat() / RewardConfig.MAX_BANK_MS).coerceIn(0f, 1f)
                    } else null,
                )
                Column(Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "VIX PRO REWARDS",
                            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                            fontWeight = FontWeight.Bold,
                            color = GoldLabel,
                        )
                        if (isProActive) ActivePill()
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        if (isProActive) formatHm(remaining)
                        else "Watch 1 ad, get ${RewardConfig.label()} of Pro",
                        style = if (isProActive) {
                            MaterialTheme.typography.headlineSmall
                        } else {
                            MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp, lineHeight = 23.sp)
                        },
                        fontWeight = FontWeight.Bold,
                        color = OnScrim,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (isProActive) "of Pro left · bank up to ${RewardConfig.bankShort}"
                        else "Free. Optional. Stack more ads for more time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                benefitChips.forEach { BenefitChipView(it) }
            }
            Spacer(Modifier.height(14.dp))
            GoldButton(
                label = when {
                    loading -> "Loading ad…"
                    isProActive -> "Extend ${RewardConfig.perAdShort}"
                    else -> "Watch ad · Go ad-free"
                },
                onClick = onWatchAd,
            )
        }
    }
}

@Composable
private fun CrownBadge(progress: Float?) {
    Box(Modifier.size(62.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(62.dp)) {
            val sw = 3.dp.toPx()
            val d = size.minDimension - sw
            drawCircle(
                color = Color.White.copy(alpha = 0.14f),
                radius = d / 2,
                style = Stroke(width = sw),
            )
            if (progress != null) {
                drawArc(
                    color = GoldRing,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = sw, cap = StrokeCap.Round),
                    topLeft = Offset(sw / 2, sw / 2),
                    size = Size(d, d),
                )
            }
        }
        Icon(
            painterResource(R.drawable.ic_crown_outline),
            contentDescription = null,
            tint = GoldRing,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun ActivePill() {
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(Success.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            "ACTIVE",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.5.sp),
            fontWeight = FontWeight.Bold,
            color = Success,
        )
    }
}

@Composable
private fun BenefitChipView(chip: BenefitChip) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            painterResource(chip.icon),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.82f),
            modifier = Modifier.size(13.dp),
        )
        Text(
            chip.label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
            color = Color.White.copy(alpha = 0.85f),
            maxLines = 1,
        )
    }
}

@Composable
private fun GoldButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(listOf(GoldBtnStart, GoldBtnEnd)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(painterResource(R.drawable.ic_play), null, tint = BtnTextDark, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BtnTextDark)
        }
    }
}

private fun formatHm(ms: Long): String {
    val h = ms / 3_600_000
    val m = (ms % 3_600_000) / 60_000
    return if (h > 0) "${h}h %02dm".format(m) else "%dm".format(m)
}

/** The Pro details modal — crown header, the full perk list, and the "watch ad" CTA. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProDetailsSheet(onDismiss: () -> Unit, loading: Boolean, onWatchAd: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(64.dp).clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(BrandStart, BrandEnd))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_sparkle),
                    contentDescription = null,
                    tint = OnScrim,
                    modifier = Modifier.size(32.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "Vix Play Pro",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "Watch a short ad — earn ${RewardConfig.label()} ad-free. Stack up to ${RewardConfig.bankShort}.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            proFeatures.forEach { FeatureRow(it) }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onWatchAd,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Icon(painterResource(R.drawable.ic_play), null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    if (loading) "Loading ad…" else "Watch Ad · Earn ${RewardConfig.label()}",
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "One short ad • No payment • No account",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The Pro perk list, reused by the details modal and the dedicated rewards screen. */
@Composable
fun ProBenefits(modifier: Modifier = Modifier) {
    Column(modifier) { proFeatures.forEach { FeatureRow(it) } }
}

@Composable
private fun FeatureRow(f: ProFeature) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            painterResource(f.icon),
            contentDescription = null,
            tint = Color.Unspecified, // the neon icons carry their own colour
            modifier = Modifier.size(30.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                f.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                f.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Success modal shown after a completed watch — Pro is now active for 24h. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProSuccessSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(84.dp).clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(BrandStart, BrandEnd)))
                    .border(3.dp, OnScrim.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_pro_noads),
                    contentDescription = null,
                    tint = OnScrim,
                    modifier = Modifier.size(42.dp),
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "Pro Unlocked!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "You earned ${RewardConfig.label()} ad-free. Watch more anytime to stack up to ${RewardConfig.bankShort}.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text("Start Watching", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
