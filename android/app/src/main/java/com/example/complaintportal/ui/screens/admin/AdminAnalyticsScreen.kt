package com.example.complaintportal.ui.screens.admin

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*

// ── Theme-Aware Colors ────────────────────────────────────────────────────────
data class AdminAnalyticsColors(
    val NavyPrimary: Color,
    val NavyLight: Color,
    val TealAccent: Color,
    val GoldAccent: Color,
    val BgLight: Color,
    val CardWhite: Color,
    val TextPrimary: Color,
    val TextSecondary: Color,
    val GreenResolved: Color,
    val AmberActive: Color,
    val RedNew: Color,
    val PurpleColor: Color,
    val DividerColor: Color
)

@Composable
fun adminAnalyticsColors(): AdminAnalyticsColors = if (isSystemInDarkTheme()) {
    AdminAnalyticsColors(
        NavyPrimary   = Color(0xFF4A8BFF),
        NavyLight     = Color(0xFF1A3A6E),
        TealAccent    = Color(0xFF7ECFC0),
        GoldAccent    = Color(0xFFF4A700),
        BgLight       = Color(0xFF0F172A),
        CardWhite     = Color(0xFF1E293B),
        TextPrimary   = Color(0xFFF1F5F9),
        TextSecondary = Color(0xFF94A3B8),
        GreenResolved = Color(0xFF34D399),
        AmberActive   = Color(0xFFF59E0B),
        RedNew        = Color(0xFFF87171),
        PurpleColor   = Color(0xFFA78BFA),
        DividerColor  = Color(0xFF334155)
    )
} else {
    AdminAnalyticsColors(
        NavyPrimary   = Color(0xFF1A3A6E),
        NavyLight     = Color(0xFF1A5C9E),
        TealAccent    = Color(0xFF7ECFC0),
        GoldAccent    = Color(0xFFF4A700),
        BgLight       = Color(0xFFF2F4F8),
        CardWhite     = Color(0xFFFFFFFF),
        TextPrimary   = Color(0xFF0D2247),
        TextSecondary = Color(0xFF6A7F9A),
        GreenResolved = Color(0xFF1D9E75),
        AmberActive   = Color(0xFFE67E22),
        RedNew        = Color(0xFFE53935),
        PurpleColor   = Color(0xFF8E44AD),
        DividerColor  = Color(0xFFE8EDF5)
    )
}

// ── Data models ───────────────────────────────────────────────────────────────
data class AdminAnalyticsData(
    val jurisdiction:        String,
    val period:              String,

    // Overall stats
    val totalComplaints:     Int,
    val resolvedCount:       Int,
    val activeCount:         Int,
    val newCount:            Int,
    val avgResolutionDays:   Float,
    val resolutionRatePct:   Int,      // 0–100

    // Trend — last 7 days incoming complaints
    val weeklyIncoming:      List<Int>,
    val weeklyResolved:      List<Int>,
    val weekLabels:          List<String>,

    // Category breakdown
    val categoryBreakdown:   List<Pair<String, Int>>,

    // District/area breakdown
    val districtBreakdown:   List<Pair<String, Int>>,

    // Top reporters (citizen name + count)
    val topReporters:        List<Pair<String, Int>>,

    // Resolution time buckets: <1 day, 1–3 days, 3–7 days, 7+ days
    val resolutionTimeBuckets: List<Pair<String, Int>>,
)

// ── Main Screen ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAnalyticsScreen(
    data:   AdminAnalyticsData,
    onBack: () -> Unit,
    onPeriodChange: (String) -> Unit = {},
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val exportCsv = {
        val csv = StringBuilder()
        csv.append("Admin Analytics Report - ${data.jurisdiction}\n")
        csv.append("Period: ${data.period}\n\n")
        
        csv.append("METRIC,VALUE\n")
        csv.append("Total Complaints,${data.totalComplaints}\n")
        csv.append("Resolved,${data.resolvedCount}\n")
        csv.append("Active,${data.activeCount}\n")
        csv.append("New,${data.newCount}\n")
        csv.append("Resolution Rate,${data.resolutionRatePct}%\n")
        csv.append("Avg Resolution Days,${String.format("%.1f", data.avgResolutionDays)}\n\n")
        
        csv.append("CATEGORY BREAKDOWN\n")
        data.categoryBreakdown.forEach { (cat, count) ->
            csv.append("${cat.replace("_", " ").uppercase()},${count}\n")
        }
        csv.append("\nDISTRICT BREAKDOWN\n")
        data.districtBreakdown.forEach { (dist, count) ->
            csv.append("${dist.replaceFirstChar { it.uppercase() }},${count}\n")
        }

        val sendIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_TEXT, csv.toString())
            type = "text/csv"
        }
        val shareIntent = android.content.Intent.createChooser(sendIntent, "Export Analytics CSV")
        context.startActivity(shareIntent)
    }
    val colors = adminAnalyticsColors()
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { loaded = true }

    val globalAlpha by animateFloatAsState(
        targetValue   = if (loaded) 1f else 0f,
        animationSpec = tween(500),
        label         = "alpha",
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Admin Analytics", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.TextPrimary)
                        Text(data.period, fontSize = 11.sp, color = colors.TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = colors.NavyPrimary)
                    }
                },
                actions = {
                    // Export CSV button
                    IconButton(onClick = exportCsv) {
                        Icon(Icons.Outlined.Download, contentDescription = "Export", tint = colors.NavyPrimary)
                    }

                    // Period Selector
                    var showPeriodMenu by remember { mutableStateOf(false) }
                    val periods = listOf("This Week", "This Month", "Last 3 Months", "All Time")

                    Box(modifier = Modifier.padding(end = 8.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(colors.NavyPrimary.copy(alpha = 0.08f))
                                .clickable { showPeriodMenu = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(data.period, fontSize = 12.sp, color = colors.NavyPrimary, fontWeight = FontWeight.Medium)
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = colors.NavyPrimary, modifier = Modifier.size(14.dp))
                            }
                        }

                        DropdownMenu(
                            expanded = showPeriodMenu,
                            onDismissRequest = { showPeriodMenu = false },
                            modifier = Modifier.background(colors.CardWhite)
                        ) {
                            periods.forEach { period ->
                                DropdownMenuItem(
                                    text = { Text(period, color = colors.TextPrimary) },
                                    onClick = {
                                        showPeriodMenu = false
                                        onPeriodChange(period)
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.BgLight),
            )
        },
        containerColor = colors.BgLight,
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

            // ── 1. KPI Summary ────────────────────────────────────────────────
            AdminKpiCard(data = data, loaded = loaded)

            // ── 2. Resolution Rate ────────────────────────────────────────────
            ResolutionRateCard(
                ratePct  = data.resolutionRatePct,
                avgDays  = data.avgResolutionDays,
                loaded   = loaded,
            )

            // ── 3. Incoming vs Resolved Trend ─────────────────────────────────
            AdminSectionCard(title = "Weekly Overview", subtitle = "Incoming vs Resolved") {
                DualTrendChart(
                    incoming = data.weeklyIncoming,
                    resolved = data.weeklyResolved,
                    labels   = data.weekLabels,
                    loaded   = loaded,
                )
                Spacer(Modifier.height(8.dp))
                // Legend
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    LegendItem(color = colors.NavyPrimary, label = "Incoming")
                    LegendItem(color = colors.TealAccent,  label = "Resolved")
                }
            }

            // ── 4. Status Breakdown ───────────────────────────────────────────
            AdminSectionCard(title = "Status Breakdown", subtitle = "Current queue") {
                AdminStatusBars(
                    new      = data.newCount,
                    active   = data.activeCount,
                    resolved = data.resolvedCount,
                    total    = data.totalComplaints,
                    loaded   = loaded,
                )
            }

            // ── 5. Category Breakdown ─────────────────────────────────────────
            AdminSectionCard(title = "Issues by Category", subtitle = "What citizens are reporting") {
                AdminCategoryBars(
                    items  = data.categoryBreakdown,
                    total  = data.totalComplaints,
                    loaded = loaded,
                )
            }

            // ── 6. District Hotspots ──────────────────────────────────────────
            AdminSectionCard(title = "District Hotspots", subtitle = "Where issues are concentrated") {
                DistrictHeatList(
                    items  = data.districtBreakdown,
                    total  = data.totalComplaints,
                    loaded = loaded,
                )
            }

            // ── 7. Resolution Time ────────────────────────────────────────────
            AdminSectionCard(title = "Resolution Time", subtitle = "How fast issues are being closed") {
                ResolutionTimeBuckets(
                    buckets = data.resolutionTimeBuckets,
                    loaded  = loaded,
                )
            }

            // ── 8. Top Reporters ──────────────────────────────────────────────
            AdminSectionCard(title = "Top Reporters", subtitle = "Most active citizens this period") {
                TopReportersList(reporters = data.topReporters)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── 1. KPI Summary Card ───────────────────────────────────────────────────────
@Composable
private fun AdminKpiCard(data: AdminAnalyticsData, loaded: Boolean) {
    val colors = adminAnalyticsColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(colors.NavyPrimary, colors.NavyLight)))
            .padding(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Admin Overview", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f), letterSpacing = 1.sp)
                Text("📍 ${data.jurisdiction}", fontSize = 13.sp, color = colors.TealAccent)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text("⭐ Admin", fontSize = 11.sp, color = colors.GoldAccent, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(20.dp))

        // Top row — 4 stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            AdminKpiStat(value = data.totalComplaints, label = "Total",    color = Color.White,       loaded = loaded)
            AdminKpiDivider()
            AdminKpiStat(value = data.newCount,        label = "New",      color = Color(0xFFFF8A80), loaded = loaded)
            AdminKpiDivider()
            AdminKpiStat(value = data.activeCount,     label = "Active",   color = colors.GoldAccent,        loaded = loaded)
            AdminKpiDivider()
            AdminKpiStat(value = data.resolvedCount,   label = "Resolved", color = colors.TealAccent,        loaded = loaded)
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
        Spacer(Modifier.height(12.dp))

        // Bottom row — avg resolution days
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Outlined.Timer, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                Text("Avg. Resolution Time", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
            }
            val animDays by animateFloatAsState(
                targetValue   = if (loaded) data.avgResolutionDays else 0f,
                animationSpec = tween(1000, easing = EaseOutCubic),
                label         = "avgDays",
            )
            Text(
                "${"%.1f".format(animDays)} days",
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold,
                color      = colors.GoldAccent,
            )
        }
    }
}

@Composable
private fun AdminKpiStat(value: Int, label: String, color: Color, loaded: Boolean) {
    val animated by animateIntAsState(
        targetValue   = if (loaded) value else 0,
        animationSpec = tween(800, easing = EaseOutCubic),
        label         = "kpi_$label",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(animated.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.55f))
    }
}

@Composable
private fun AdminKpiDivider() {
    val colors = adminAnalyticsColors()
    Box(modifier = Modifier.width(1.dp).height(36.dp).background(Color.White.copy(alpha = 0.12f)))
}

// ── 2. Resolution Rate Card ───────────────────────────────────────────────────
@Composable
private fun ResolutionRateCard(ratePct: Int, avgDays: Float, loaded: Boolean) {
    val colors = adminAnalyticsColors()
    val animRate by animateIntAsState(
        targetValue   = if (loaded) ratePct else 0,
        animationSpec = tween(1000, easing = EaseOutCubic),
        label         = "rate",
    )
    val animProgress by animateFloatAsState(
        targetValue   = if (loaded) ratePct / 100f else 0f,
        animationSpec = tween(1200, easing = EaseOutCubic),
        label         = "rateBar",
    )
    val rateColor = when {
        ratePct >= 70 -> colors.GreenResolved
        ratePct >= 40 -> colors.AmberActive
        else          -> colors.RedNew
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(colors.CardWhite)
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        // Circle progress indicator
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
            Canvas(modifier = Modifier.size(80.dp)) {
                // Background circle
                drawArc(
                    color      = colors.DividerColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter  = false,
                    style      = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
                )
                // Progress arc
                drawArc(
                    color      = rateColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animProgress,
                    useCenter  = false,
                    style      = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$animRate%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = rateColor)
                Text("Rate", fontSize = 9.sp, color = colors.TextSecondary)
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text("Resolution Rate", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(
                when {
                    ratePct >= 70 -> "🟢 Excellent performance"
                    ratePct >= 40 -> "🟡 Needs improvement"
                    else          -> "🔴 Attention required"
                },
                fontSize = 12.sp,
                color    = rateColor,
            )
            Spacer(Modifier.height(10.dp))

            // Full-width bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(colors.DividerColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(listOf(rateColor, rateColor.copy(alpha = 0.6f)))
                        )
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "${"%.1f".format(avgDays)} avg days to resolve",
                fontSize = 11.sp,
                color    = colors.TextSecondary,
            )
        }
    }
}

// ── 3. Dual Trend Chart (Incoming vs Resolved) ────────────────────────────────
@Composable
private fun DualTrendChart(
    incoming: List<Int>,
    resolved: List<Int>,
    labels:   List<String>,
    loaded:   Boolean,
) {
    val colors = adminAnalyticsColors()
    if (incoming.isEmpty()) return
    val maxVal = (incoming + resolved).maxOrNull()?.coerceAtLeast(1)?.toFloat() ?: 1f

    val animProgress by animateFloatAsState(
        targetValue   = if (loaded) 1f else 0f,
        animationSpec = tween(1200, easing = EaseOutCubic),
        label         = "dualTrend",
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .padding(horizontal = 8.dp)
    ) {
        val w     = size.width
        val h     = size.height - 24.dp.toPx()
        val count = incoming.size
        val stepX = w / (count - 1).coerceAtLeast(1).toFloat()

        // Grid lines
        for (i in 0..4) {
            drawLine(colors.DividerColor, Offset(0f, h * (1f - i / 4f)), Offset(w, h * (1f - i / 4f)), 1.dp.toPx())
        }

        fun List<Int>.toPoints() = mapIndexed { idx, v ->
            Offset(stepX * idx, h * (1f - v / maxVal))
        }

        fun drawTrendLine(pts: List<Offset>, lineColor: Color, fillColor: Color) {
            clipRect(right = w * animProgress) {
                // Fill
                val fill = Path().apply {
                    moveTo(pts.first().x, h)
                    pts.forEach { lineTo(it.x, it.y) }
                    lineTo(pts.last().x, h)
                    close()
                }
                drawPath(fill, Brush.verticalGradient(
                    listOf(fillColor.copy(alpha = 0.25f), fillColor.copy(alpha = 0f)),
                    startY = 0f, endY = h,
                ))
                // Line
                val path = Path().apply {
                    moveTo(pts.first().x, pts.first().y)
                    for (i in 1 until pts.size) {
                        val cx = (pts[i - 1].x + pts[i].x) / 2f
                        cubicTo(cx, pts[i - 1].y, cx, pts[i].y, pts[i].x, pts[i].y)
                    }
                }
                drawPath(path, lineColor, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                pts.forEach {
                    drawCircle(colors.CardWhite,    5.dp.toPx(), it)
                    drawCircle(lineColor,    3.5.dp.toPx(), it)
                }
            }
        }

        drawTrendLine(incoming.toPoints(), colors.NavyPrimary, colors.NavyPrimary)
        drawTrendLine(resolved.toPoints(), colors.TealAccent,  colors.TealAccent)
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        labels.forEach { Text(it, fontSize = 10.sp, color = colors.TextSecondary) }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    val colors = adminAnalyticsColors()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, fontSize = 11.sp, color = colors.TextSecondary)
    }
}

// ── 4. Status Bars ────────────────────────────────────────────────────────────
@Composable
private fun AdminStatusBars(new: Int, active: Int, resolved: Int, total: Int, loaded: Boolean) {
    val colors = adminAnalyticsColors()
    listOf(
        Triple("New",      new,      colors.RedNew),
        Triple("Active",   active,   colors.AmberActive),
        Triple("Resolved", resolved, colors.GreenResolved),
    ).forEach { (label, value, color) ->
        val animW by animateFloatAsState(
            targetValue   = if (loaded && total > 0) value / total.toFloat() else 0f,
            animationSpec = tween(900, delayMillis = 150, easing = EaseOutCubic),
            label         = "status_$label",
        )
        val animV by animateIntAsState(
            targetValue   = if (loaded) value else 0,
            animationSpec = tween(900, easing = EaseOutCubic),
            label         = "statusV_$label",
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(label, fontSize = 12.sp, color = colors.TextSecondary, modifier = Modifier.width(58.dp))
            Box(
                modifier = Modifier.weight(1f).height(28.dp)
                    .clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.08f))
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(animW).fillMaxHeight()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.7f))))
                )
                val pct = if (total > 0) (value * 100 / total) else 0
                Text(
                    "$pct%", fontSize = 10.sp, color = color, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
                )
            }
            Text(animV.toString(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color, modifier = Modifier.width(24.dp), textAlign = TextAlign.End)
        }
    }
}

// ── 5. Category Bars ──────────────────────────────────────────────────────────
@Composable
private fun AdminCategoryBars(items: List<Pair<String, Int>>, total: Int, loaded: Boolean) {
    val colors = adminAnalyticsColors()
    val barColors = listOf(colors.NavyPrimary, colors.TealAccent, colors.PurpleColor, colors.AmberActive, colors.GreenResolved, colors.RedNew, Color(0xFF1565C0), colors.TextSecondary)
    val maxVal = items.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEachIndexed { idx, (raw, count) ->
            val label  = raw.replace("_", " ").replaceFirstChar { it.uppercase() }
            val color  = barColors[idx % barColors.size]
            val pct    = if (total > 0) (count * 100f / total) else 0f
            val animW  by animateFloatAsState(
                targetValue   = if (loaded) count / maxVal.toFloat() else 0f,
                animationSpec = tween(900, delayMillis = idx * 60, easing = EaseOutCubic),
                label         = "cat_$label",
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                        Text(label, fontSize = 12.sp, color = colors.TextPrimary, fontWeight = FontWeight.Medium)
                    }
                    Text("$count (${pct.toInt()}%)", fontSize = 12.sp, color = colors.TextSecondary)
                }
                Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(color.copy(alpha = 0.08f))) {
                    Box(modifier = Modifier.fillMaxWidth(animW).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(color))
                }
            }
        }
    }
}

// ── 6. District Hotspots ──────────────────────────────────────────────────────
@Composable
private fun DistrictHeatList(items: List<Pair<String, Int>>, total: Int, loaded: Boolean) {
    val colors = adminAnalyticsColors()
    val maxVal = items.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEachIndexed { idx, (district, count) ->
            val animW by animateFloatAsState(
                targetValue   = if (loaded) count / maxVal.toFloat() else 0f,
                animationSpec = tween(900, delayMillis = idx * 60, easing = EaseOutCubic),
                label         = "dist_$district",
            )
            // Heat color: more issues = more red
            val heatIntensity = count.toFloat() / maxVal
            val heatColor = lerp(colors.TealAccent, colors.RedNew, heatIntensity * 0.8f)
            val pct = if (total > 0) (count * 100 / total) else 0

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Rank number
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (idx == 0) colors.GoldAccent.copy(alpha = 0.15f)
                            else colors.NavyPrimary.copy(alpha = 0.06f)
                        ),
                ) {
                    Text(
                        "${idx + 1}",
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color      = if (idx == 0) colors.GoldAccent else colors.TextSecondary,
                    )
                }
                Text(district.replaceFirstChar { it.uppercase() }, fontSize = 12.sp, color = colors.TextPrimary, modifier = Modifier.width(90.dp))
                Box(modifier = Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(heatColor.copy(alpha = 0.1f))) {
                    Box(modifier = Modifier.fillMaxWidth(animW).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(heatColor))
                }
                Text("$count ($pct%)", fontSize = 11.sp, color = colors.TextSecondary, modifier = Modifier.width(56.dp), textAlign = TextAlign.End)
            }
        }
    }
}

// ── 7. Resolution Time Buckets ────────────────────────────────────────────────
@Composable
private fun ResolutionTimeBuckets(buckets: List<Pair<String, Int>>, loaded: Boolean) {
    val colors = adminAnalyticsColors()
    val total  = buckets.sumOf { it.second }.coerceAtLeast(1)
    val barColors = listOf(colors.GreenResolved, colors.TealAccent, colors.AmberActive, colors.RedNew)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        buckets.forEachIndexed { idx, (label, count) ->
            val pct   = count * 100 / total
            val color = barColors[idx % barColors.size]
            val animH by animateFloatAsState(
                targetValue   = if (loaded) count / total.toFloat() else 0f,
                animationSpec = tween(900, delayMillis = idx * 80, easing = EaseOutCubic),
                label         = "bucket_$label",
            )

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("$pct%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
                Box(
                    modifier = Modifier.fillMaxWidth().height(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(animH.coerceAtLeast(0.05f))
                            .clip(RoundedCornerShape(8.dp))
                            .background(color)
                    )
                }
                Text(label, fontSize = 9.sp, color = colors.TextSecondary, textAlign = TextAlign.Center)
                Text("$count issues", fontSize = 9.sp, color = colors.TextSecondary)
            }
        }
    }
}

// ── 8. Top Reporters ──────────────────────────────────────────────────────────
@Composable
private fun TopReportersList(reporters: List<Pair<String, Int>>) {
    val colors = adminAnalyticsColors()
    val rankEmojis = listOf("🥇", "🥈", "🥉")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        reporters.forEachIndexed { idx, (name, count) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (idx == 0) colors.GoldAccent.copy(alpha = 0.06f) else colors.BgLight
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(rankEmojis.getOrElse(idx) { "  ${idx + 1}" }, fontSize = 20.sp)
                // Initials avatar
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colors.NavyPrimary.copy(alpha = 0.08f)),
                ) {
                    Text(
                        name.take(2).uppercase(),
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color      = colors.NavyPrimary,
                    )
                }
                Text(name.replaceFirstChar { it.uppercase() }, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.TextPrimary, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.NavyPrimary.copy(alpha = 0.08f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text("$count reports", fontSize = 11.sp, color = colors.NavyPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ── Shared section card ───────────────────────────────────────────────────────
@Composable
private fun AdminSectionCard(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = adminAnalyticsColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(colors.CardWhite)
            .padding(20.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.TextPrimary)
            Text(subtitle, fontSize = 11.sp, color = colors.TextSecondary)
        }
        Spacer(Modifier.height(16.dp))
        content()
    }
}
