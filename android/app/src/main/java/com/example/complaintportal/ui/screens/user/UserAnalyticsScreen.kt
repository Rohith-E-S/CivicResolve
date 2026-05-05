package com.example.complaintportal.ui.screens.user

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*

// ── Status Colors (maintained for clarity, but used theme-awarely) ─────────────
private val TealAccent    = Color(0xFF7ECFC0)
private val GoldAccent    = Color(0xFFF4A700)
private val GreenResolved = Color(0xFF1D9E75)
private val AmberActive   = Color(0xFFE67E22)
private val RedNew        = Color(0xFFE53935)

// ── Data models ───────────────────────────────────────────────────────────────
data class AnalyticsData(
    val totalReports:     Int,
    val resolvedCount:    Int,
    val activeCount:      Int,
    val newCount:         Int,
    val communityRankPct: Int,           // e.g. 15 = top 15%
    val location:         String,
    val period:           String,        // e.g. "This Month"

    // Last 7 days report counts — index 0 = 6 days ago, index 6 = today
    val weeklyTrend: List<Int>,          // e.g. [1, 0, 2, 1, 3, 0, 2]
    val weekLabels:  List<String>,       // e.g. ["Mon","Tue",..."Sun"]

    // Category breakdown — list of (label, count)
    val categoryBreakdown: List<Pair<String, Int>>,
)

// ── Main Screen ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserAnalyticsScreen(
    data: AnalyticsData,
    onBack: () -> Unit,
    onPeriodChange: (String) -> Unit = {},
) {
    // Animate everything in on load
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { loaded = true }

    val globalAlpha by animateFloatAsState(
        targetValue   = if (loaded) 1f else 0f,
        animationSpec = tween(500),
        label         = "globalAlpha",
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Analytics",
                            fontWeight = FontWeight.Bold,
                            fontSize   = 18.sp,
                            color      = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            data.period,
                            fontSize = 11.sp,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    // Period Selector
                    var showPeriodMenu by remember { mutableStateOf(false) }
                    val periods = listOf("This Week", "This Month", "Last 3 Months", "All Time")

                    Box(modifier = Modifier.padding(end = 16.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                .clickable { showPeriodMenu = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(data.period, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            }
                        }

                        DropdownMenu(
                            expanded = showPeriodMenu,
                            onDismissRequest = { showPeriodMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            periods.forEach { period ->
                                DropdownMenuItem(
                                    text = { Text(period, color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        showPeriodMenu = false
                                        onPeriodChange(period)
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .alpha(globalAlpha)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {

            Spacer(Modifier.height(2.dp))

            // ── 1. Impact summary card ────────────────────────────────────────
            ImpactSummaryCard(data = data, loaded = loaded)

            // ── 2. Activity trend (last 7 days) ───────────────────────────────
            SectionCard(title = "Activity Trend", subtitle = "Last 7 days") {
                WeeklyTrendChart(
                    values = data.weeklyTrend,
                    labels = data.weekLabels,
                    loaded = loaded,
                )
            }

            // ── 3. Complaints by status ───────────────────────────────────────
            SectionCard(title = "Complaints by Status", subtitle = "Current breakdown") {
                StatusBarChart(
                    new      = data.newCount,
                    active   = data.activeCount,
                    resolved = data.resolvedCount,
                    total    = data.totalReports,
                    loaded   = loaded,
                )
            }

            // ── 4. Complaints by category ─────────────────────────────────────
            SectionCard(title = "Complaints by Category", subtitle = "Issue types you've reported") {
                CategoryBreakdown(
                    items  = data.categoryBreakdown,
                    total  = data.totalReports,
                    loaded = loaded,
                )
            }

            // ── 5. Community rank ─────────────────────────────────────────────
            CommunityRankCard(
                rankPct  = data.communityRankPct,
                location = data.location,
                loaded   = loaded,
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── 1. Impact Summary Card ────────────────────────────────────────────────────
@Composable
private fun ImpactSummaryCard(data: AnalyticsData, loaded: Boolean) {
    val gradientStart = MaterialTheme.colorScheme.primary
    val gradientEnd   = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(gradientStart, gradientEnd)
                )
            )
            .padding(20.dp),
    ) {
        Text(
            "Your Impact",
            fontSize   = 12.sp,
            color      = Color.White.copy(alpha = 0.7f),
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "📍 ${data.location}",
            fontSize = 13.sp,
            color    = TealAccent,
        )

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ImpactStat(value = data.totalReports,  label = "Total",    color = Color.White,  loaded = loaded)
            ImpactDivider()
            ImpactStat(value = data.resolvedCount, label = "Resolved", color = TealAccent,   loaded = loaded)
            ImpactDivider()
            ImpactStat(value = data.activeCount,   label = "Active",   color = GoldAccent,   loaded = loaded)
            ImpactDivider()
            ImpactStat(value = data.newCount,      label = "New",      color = Color(0xFFFF8A80), loaded = loaded)
        }
    }
}

@Composable
private fun ImpactStat(value: Int, label: String, color: Color, loaded: Boolean) {
    val animated by animateIntAsState(
        targetValue   = if (loaded) value else 0,
        animationSpec = tween(800, easing = EaseOutCubic),
        label         = "impact_$label",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(animated.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
    }
}

@Composable
private fun ImpactDivider() {
    Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.White.copy(alpha = 0.15f)))
}

// ── 2. Weekly Trend Line Chart ────────────────────────────────────────────────
@Composable
private fun WeeklyTrendChart(values: List<Int>, labels: List<String>, loaded: Boolean) {
    if (values.isEmpty()) return
    val maxVal = (values.maxOrNull() ?: 1).coerceAtLeast(1).toFloat()
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface

    val animProgress by animateFloatAsState(
        targetValue   = if (loaded) 1f else 0f,
        animationSpec = tween(1200, easing = EaseOutCubic),
        label         = "trendAnim",
    )

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(horizontal = 8.dp)
        ) {
            val w      = size.width
            val h      = size.height - 24.dp.toPx()
            val count  = values.size
            val stepX  = w / (count - 1).coerceAtLeast(1).toFloat()

            // Grid lines
            for (i in 0..4) {
                val y = h * (1f - i / 4f)
                drawLine(
                    color       = gridColor,
                    start       = Offset(0f, y),
                    end         = Offset(w, y),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            // Points
            val pts = values.mapIndexed { idx, v ->
                Offset(stepX * idx, h * (1f - (v / maxVal)))
            }

            // Animated clip — reveals left to right
            val clipWidth = w * animProgress

            clipRect(right = clipWidth) {
                // Fill gradient under line
                val fillPath = Path().apply {
                    moveTo(pts.first().x, h)
                    pts.forEach { lineTo(it.x, it.y) }
                    lineTo(pts.last().x, h)
                    close()
                }
                drawPath(
                    path  = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(TealAccent.copy(alpha = 0.3f), TealAccent.copy(alpha = 0f)),
                        startY = 0f,
                        endY   = h,
                    ),
                )

                // Line
                val linePath = Path().apply {
                    moveTo(pts.first().x, pts.first().y)
                    for (i in 1 until pts.size) {
                        // Smooth curve
                        val cx = (pts[i - 1].x + pts[i].x) / 2f
                        cubicTo(cx, pts[i - 1].y, cx, pts[i].y, pts[i].x, pts[i].y)
                    }
                }
                drawPath(
                    path        = linePath,
                    color       = TealAccent,
                    style       = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )

                // Dots
                pts.forEach { pt ->
                    drawCircle(color = surfaceColor, radius = 5.dp.toPx(), center = pt)
                    drawCircle(color = TealAccent,   radius = 3.5.dp.toPx(), center = pt)
                }
            }
        }

        // X-axis labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            labels.forEach { label ->
                Text(label, fontSize = 10.sp, color = labelColor, textAlign = TextAlign.Center)
            }
        }
    }
}

// ── 3. Status Bar Chart ───────────────────────────────────────────────────────
@Composable
private fun StatusBarChart(new: Int, active: Int, resolved: Int, total: Int, loaded: Boolean) {
    val items = listOf(
        Triple("New",      new,      RedNew),
        Triple("Active",   active,   AmberActive),
        Triple("Resolved", resolved, GreenResolved),
    )
    val max = items.maxOf { it.second }.coerceAtLeast(1)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        items.forEach { (label, value, color) ->
            val animWidth by animateFloatAsState(
                targetValue   = if (loaded) value / max.toFloat() else 0f,
                animationSpec = tween(900, delayMillis = 200, easing = EaseOutCubic),
                label         = "bar_$label",
            )
            val animValue by animateIntAsState(
                targetValue   = if (loaded) value else 0,
                animationSpec = tween(900, delayMillis = 200, easing = EaseOutCubic),
                label         = "barVal_$label",
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    label,
                    fontSize  = 12.sp,
                    color     = labelColor,
                    modifier  = Modifier.width(56.dp),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(color.copy(alpha = 0.08f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animWidth)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(color, color.copy(alpha = 0.7f))
                                )
                            )
                    )
                    // Percentage label inside bar
                    val pct = if (total > 0) (value * 100 / total) else 0
                    Text(
                        "$pct%",
                        fontSize  = 10.sp,
                        color     = color,
                        fontWeight = FontWeight.SemiBold,
                        modifier  = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 8.dp),
                    )
                }
                Text(
                    animValue.toString(),
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color      = color,
                    modifier   = Modifier.width(20.dp),
                    textAlign  = TextAlign.End,
                )
            }
        }
    }
}

// ── 4. Category Breakdown (horizontal bars) ───────────────────────────────────
@Composable
private fun CategoryBreakdown(items: List<Pair<String, Int>>, total: Int, loaded: Boolean) {
    val categoryColors = listOf(
        MaterialTheme.colorScheme.primary,
        TealAccent,
        Color(0xFF8E44AD),
        AmberActive,
        GreenResolved,
        RedNew,
        Color(0xFF1565C0),
        MaterialTheme.colorScheme.secondary,
    )
    val maxVal = items.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    val labelColor = MaterialTheme.colorScheme.onSurface
    val subLabelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEachIndexed { idx, (rawLabel, count) ->
            val label    = rawLabel.replace("_", " ").replaceFirstChar { it.uppercase() }
            val color    = categoryColors[idx % categoryColors.size]
            val pct      = if (total > 0) (count * 100f / total) else 0f
            val barWidth = count / maxVal.toFloat()

            val animWidth by animateFloatAsState(
                targetValue   = if (loaded) barWidth else 0f,
                animationSpec = tween(900, delayMillis = idx * 80, easing = EaseOutCubic),
                label         = "cat_$label",
            )
            val animPct by animateFloatAsState(
                targetValue   = if (loaded) pct else 0f,
                animationSpec = tween(900, delayMillis = idx * 80, easing = EaseOutCubic),
                label         = "catPct_$label",
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Text(label, fontSize = 12.sp, color = labelColor, fontWeight = FontWeight.Medium)
                    }
                    Text(
                        "$count (${animPct.toInt()}%)",
                        fontSize = 12.sp,
                        color    = subLabelColor,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(color.copy(alpha = 0.08f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animWidth)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(color)
                    )
                }
            }
        }
    }
}

// ── 5. Community Rank Card ────────────────────────────────────────────────────
@Composable
private fun CommunityRankCard(rankPct: Int, location: String, loaded: Boolean) {
    val animRank by animateIntAsState(
        targetValue   = if (loaded) rankPct else 0,
        animationSpec = tween(1000, easing = EaseOutCubic),
        label         = "rank",
    )
    val (emoji, message, color) = when {
        rankPct <= 10 -> Triple("🏆", "Outstanding! You're in the top $animRank% of reporters", GoldAccent)
        rankPct <= 25 -> Triple("🥈", "Great work! You're in the top $animRank% of reporters",  TealAccent)
        rankPct <= 50 -> Triple("🥉", "Good effort! You're in the top $animRank% of reporters",  AmberActive)
        else          -> Triple("🌱", "Keep reporting! You're building your community impact",    GreenResolved)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "COMMUNITY RANK",
            fontSize  = 11.sp,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
        )

        Spacer(Modifier.height(16.dp))

        Text(emoji, fontSize = 40.sp)

        Spacer(Modifier.height(8.dp))

        Text(
            "Top $animRank%",
            fontSize   = 28.sp,
            fontWeight = FontWeight.Bold,
            color      = color,
        )

        Text(
            "in $location",
            fontSize = 13.sp,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))

        // Rank progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.outlineVariant)
        ) {
            val barProgress by animateFloatAsState(
                targetValue   = if (loaded) 1f - (rankPct / 100f) else 0f,
                animationSpec = tween(1000, easing = EaseOutCubic),
                label         = "rankBar",
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(barProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(MaterialTheme.colorScheme.primary, TealAccent)
                        )
                    )
            )
        }

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Top 1%", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Top 100%", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.08f))
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text(
                message,
                fontSize  = 12.sp,
                color     = color,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

// ── Shared: Section card wrapper ──────────────────────────────────────────────
@Composable
private fun SectionCard(
    title:    String,
    subtitle: String,
    content:  @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(16.dp))
        content()
    }
}
