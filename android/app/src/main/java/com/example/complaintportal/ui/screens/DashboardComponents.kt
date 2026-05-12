package com.example.complaintportal.ui.screens

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import com.example.complaintportal.data.model.Complaint
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.charts.PieChart
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.complaintportal.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext


@Composable
fun Modifier.shimmerEffect(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f),
    )

    return this.background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = androidx.compose.ui.geometry.Offset(translateAnim, translateAnim),
            end = androidx.compose.ui.geometry.Offset(translateAnim + 500f, translateAnim + 500f)
        )
    )
}

@Composable
fun ShimmerComplaintCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(144.dp)
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
                    .shimmerEffect()
            )
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Box(modifier = Modifier.height(20.dp).fillMaxWidth(0.8f).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.height(14.dp).fillMaxWidth().clip(RoundedCornerShape(4.dp)).shimmerEffect())
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.height(14.dp).fillMaxWidth(0.6f).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Box(modifier = Modifier.size(60.dp, 12.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                    Box(modifier = Modifier.size(40.dp, 12.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ComplaintCard(
    complaint: Complaint, 
    isAdmin: Boolean, 
    onClick: () -> Unit, 
    onUpdateStatusClick: () -> Unit,
    showCommunityFeatures: Boolean = false,
    isSupported: Boolean = false,
    isOwner: Boolean = false,
    onSupportClick: () -> Unit = {},
    distanceMeters: Double? = null,
    showDistance: Boolean = false,
    showYouReportedTag: Boolean = true,
    currentUserId: String? = null
) {
    val sharedTransitionScope = com.example.complaintportal.ui.navigation.LocalSharedTransitionScope.current
    val animatedVisibilityScope = com.example.complaintportal.ui.navigation.LocalNavAnimatedVisibilityScope.current
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(144.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Side: Image & Status Badge
            Box(
                modifier = Modifier
                    .weight(0.35f)
                    .fillMaxHeight()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (!complaint.beforeImageUrl.isNullOrBlank()) {
                    var imageModifier = Modifier.fillMaxSize()
                    if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                        with(sharedTransitionScope) {
                            imageModifier = imageModifier.sharedElement(
                                rememberSharedContentState(key = "image-${complaint.id}"),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        }
                    }

                    Image(
                        painter = rememberAsyncImagePainter(complaint.beforeImageUrl),
                        contentDescription = stringResource(R.string.complaint_image),
                        contentScale = ContentScale.Crop,
                        modifier = imageModifier
                    )
                } else {
                    Icon(
                        Icons.Default.ImageNotSupported,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Center).size(32.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }

                // Status Badge
                val statusClean = complaint.status.trim().lowercase()
                val statusColor = when (statusClean) {
                    "resolved" -> MaterialTheme.colorScheme.secondaryContainer
                    "in_progress" -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.primaryContainer
                }
                val onStatusColor = when (statusClean) {
                    "resolved" -> MaterialTheme.colorScheme.onSecondaryContainer
                    "in_progress" -> MaterialTheme.colorScheme.onTertiaryContainer
                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                }

                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = complaint.status.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        fontWeight = FontWeight.Bold,
                        color = onStatusColor
                    )
                }
            }

            // Right Side: Content
            Box(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight()
                    .padding(10.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            if (showCommunityFeatures) {
                                if (currentUserId != null && complaint.user?.id == currentUserId && showYouReportedTag) {
                                    YouReportedBadge()
                                } else {
                                    Text(
                                        text = stringResource(R.string.citizen_in, complaint.city.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha=0.7f),
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = complaint.category.replace("_", " ").replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(end = if (isAdmin || showCommunityFeatures) 48.dp else 0.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = complaint.description,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary, // Teal/Primary accent
                                    modifier = Modifier.size(14.dp) // Slightly larger
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${complaint.city.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}, ${complaint.state.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.outline,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            ComplaintDistanceLabel(
                                distanceMeters = distanceMeters,
                                showDistance = showDistance
                            )
                        }
                        
                        val dateDisplay = try {
                            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                            val createdDate = sdf.parse(complaint.createdAt!!)
                            val now = Date()
                            val diffInMillis = now.time - createdDate!!.time
                            val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)
                            val diffInHours = TimeUnit.MILLISECONDS.toHours(diffInMillis)
                            val diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis)

                            when {
                                diffInDays > 7 -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(createdDate)
                                diffInDays >= 1 -> context.getString(R.string.days_ago, diffInDays)
                                diffInHours >= 1 -> context.getString(R.string.hours_ago, diffInHours)
                                diffInMinutes >= 1 -> context.getString(R.string.mins_ago, diffInMinutes)
                                else -> context.getString(R.string.just_now)
                            }
                        } catch (e: Exception) {
                            complaint.createdAt?.substring(0, 10) ?: context.getString(R.string.just_now)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dateDisplay,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (showCommunityFeatures || isAdmin) {
                    PriorityUpvoteButton(
                        supportCount = complaint.supportCount ?: 0,
                        onSupportClick = onSupportClick,
                        isSupported = isSupported,
                        enabled = complaint.status.trim().lowercase() != "resolved" && !isAdmin && !isOwner,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .graphicsLayer {
                                scaleX = 0.7f
                                scaleY = 0.7f
                            }
                            .offset(x = 8.dp, y = (-8).dp)
                    )
                }

            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomPullToRefreshIndicator(
    isRefreshing: Boolean,
    state: PullToRefreshState,
    modifier: Modifier = Modifier
) {
    val scaleFraction = if (isRefreshing) 1f else 
        LinearOutSlowInEasing.transform(state.distanceFraction).coerceIn(0f, 1f)

    val transition = rememberInfiniteTransition(label = "refreshing")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val color by animateColorAsState(
        targetValue = if (isRefreshing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
        label = "color"
    )

    Box(
        modifier = modifier
            .padding(top = 16.dp)
            .size(48.dp)
            .graphicsLayer {
                scaleX = scaleFraction
                scaleY = scaleFraction
                alpha = scaleFraction
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = stringResource(R.string.refreshing),
            tint = color,
            modifier = Modifier
                .size(36.dp)
                .graphicsLayer {
                    rotationZ = if (isRefreshing) rotation else state.distanceFraction * 360f
                }
        )
    }
}

enum class SortOption(val label: String) {
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First"),
    RATING_DESC("Highest Rated"),
    UPVOTES_DESC("Most Upvotes"),
    NEAREST("Nearest First")
}

@Composable
fun StatCard(
    title: String,
    count: String,
    color: Color,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    val backgroundColor = if (isSelected) color.copy(alpha = 0.2f) else color.copy(alpha = 0.05f)
    val contentColor = if (isSelected) color else color.copy(alpha = 0.8f)

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(backgroundColor)
            .then(if (isSelected) Modifier.border(1.5.dp, color, CircleShape) else Modifier)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(contentColor))
        Text(title, style = MaterialTheme.typography.labelLarge, color = contentColor, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
        Text(count, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = contentColor)
    }
}

@Composable
fun PriorityUpvoteButton(
    supportCount: Int,
    onSupportClick: () -> Unit,
    isSupported: Boolean = false,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val count by animateIntAsState(
        targetValue = supportCount,
        label = "count"
    )

    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSupported) MaterialTheme.colorScheme.primary 
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .then(
                if (enabled) {
                    Modifier.clickable {
                        onSupportClick()
                        scope.launch {
                            if (!isSupported) {
                                scale.animateTo(1.2f, tween(100))
                                scale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy))
                            } else {
                                scale.animateTo(0.8f, tween(100))
                                scale.animateTo(1f, spring(Spring.DampingRatioLowBouncy))
                            }
                        }
                    }
                } else Modifier
            )
            .padding(vertical = 6.dp, horizontal = 12.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.ThumbUp,
            contentDescription = stringResource(R.string.upvote),
            tint = if (isSupported) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        AnimatedContent(
            targetState = count,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInVertically { height -> height } + fadeIn() togetherWith
                            slideOutVertically { height -> -height } + fadeOut()
                } else {
                    slideInVertically { height -> -height } + fadeIn() togetherWith
                            slideOutVertically { height -> height } + fadeOut()
                }.using(
                    SizeTransform(clip = false)
                )
            },
            label = "counter"
        ) { targetCount ->
            Text(
                text = "$targetCount",
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                fontWeight = FontWeight.ExtraBold,
                color = if (isSupported) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ZoomableImageDialog(imageUrl: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = rememberAsyncImagePainter(imageUrl),
                contentDescription = stringResource(R.string.zoomed_image),
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(enabled = false) {},
                contentScale = ContentScale.Fit
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.close),
                    tint = Color.White
                )
            }
        }
    }
}
// ── Priority Logic ────────────────────────────────────────────────────────────
enum class Priority { HIGH, MEDIUM, LOW }

/**
 * Determines priority based on upvote count and complaint age.
 */
fun calculatePriority(upvotes: Int, createdAtIso: String?): Priority {
    if (createdAtIso == null) return Priority.LOW
    val ageInDays = try {
        ChronoUnit.DAYS.between(Instant.parse(createdAtIso), Instant.now())
    } catch (_: Exception) { 0L }

    return when {
        upvotes >= 5 || ageInDays >= 7 -> Priority.HIGH
        upvotes >= 2 || ageInDays >= 3 -> Priority.MEDIUM
        else                            -> Priority.LOW
    }
}

@Composable
fun PriorityBadge(priority: Priority, modifier: Modifier = Modifier) {
    val (bg, textColor, label, icon) = when (priority) {
        Priority.HIGH   -> listOf(Color(0xFFFFEBEE), Color(0xFFD32F2F), "HIGH", "🔴")
        Priority.MEDIUM -> listOf(Color(0xFFFFF3E0), Color(0xFFE65100), "MEDIUM", "🟡")
        Priority.LOW    -> listOf(Color(0xFFE8F5E9), Color(0xFF2E7D32), "LOW", "🟢")
        else -> listOf(Color.Gray, Color.White, "UNKNOWN", "⚪")
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg as Color)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(icon as String, fontSize = 8.sp)
            Text(
                text       = label as String,
                fontSize   = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color      = textColor as Color,
                letterSpacing = 0.3.sp,
            )
        }
    }
}

@Composable
fun AdminStatsBar(
    totalCount:    Int,
    newCount:      Int,
    activeCount:   Int,
    resolvedCount: Int,
    modifier:      Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        StatCell(value = totalCount,    label = "Total",    color = MaterialTheme.colorScheme.primary)
        StatBarDivider()
        StatCell(value = newCount,      label = "New",      color = Color(0xFFE53935))
        StatBarDivider()
        StatCell(value = activeCount,   label = "Active",   color = Color(0xFFE67E22))
        StatBarDivider()
        StatCell(value = resolvedCount, label = "Resolved", color = Color(0xFF1D9E75))
    }
}

@Composable
private fun StatCell(value: Int, label: String, color: Color) {
    val animatedValue by animateIntAsState(
        targetValue   = value,
        animationSpec = tween(durationMillis = 700, easing = EaseOutCubic),
        label         = "stat_$label",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text       = animatedValue.toString(),
            fontSize   = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = color,
        )
        Text(
            text     = label,
            fontSize = 10.sp,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatBarDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

// ── Haversine distance (meters) ───────────────────────────────────────────────
fun haversineDistance(
    lat1: Double, lng1: Double,
    lat2: Double, lng2: Double,
): Double {
    val R    = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val sinDLat2 = kotlin.math.sin(dLat / 2)
    val sinDLng2 = kotlin.math.sin(dLng / 2)
    val a    = sinDLat2 * sinDLat2 +
               kotlin.math.cos(Math.toRadians(lat1)) *
               kotlin.math.cos(Math.toRadians(lat2)) *
               sinDLng2 * sinDLng2
    return R * 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
}

// ── Distance label + color helpers ───────────────────────────────────────────
fun distanceLabel(meters: Double): String = when {
    meters < 1000 -> "${meters.toInt()}m away"
    else          -> "${"%.1f".format(meters / 1000)}km away"
}

fun distanceColor(meters: Double): Color = when {
    meters <= 1000.0 -> Color(0xFF2E7D32) // <= 1km: Green
    meters <= 5000.0 -> Color(0xFFE67E22) // <= 5km: Orange
    else -> Color(0xFFE53935)             // > 5km: Red
}

// ── Sort dropdown ─────────────────────────────────────────────────────────────
@Composable
fun SortFilterDropdown(
    selectedSort:  SortOption,
    activeTab:     Int,
    onSortChanged: (SortOption) -> Unit,
    modifier:      Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    // Available options based on active tab
    val availableOptions = remember(activeTab) {
        buildList {
            add(SortOption.DATE_ASC)
            add(SortOption.RATING_DESC)
            add(SortOption.UPVOTES_DESC)
            if (activeTab == 0) { // Only show Nearest for Community Hub
                add(SortOption.NEAREST)
            }
        }
    }

    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(
                    if (selectedSort != SortOption.DATE_DESC)
                        Color(0xFF1A3A6E).copy(alpha = 0.12f)
                    else
                        Color(0xFF1A3A6E).copy(alpha = 0.08f)
                ),
        ) {
            Icon(
                Icons.Default.FilterList,
                contentDescription = "Sort",
                tint = Color(0xFF1A3A6E),
            )
            if (selectedSort != SortOption.DATE_DESC) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A3A6E))
                )
            }
        }

        DropdownMenu(
            expanded         = expanded,
            onDismissRequest = { expanded = false },
            modifier         = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFFFFFFF))
                .width(220.dp),
        ) {
            Text(
                "Sort & Filter",
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color      = Color(0xFF6A7F9A),
                modifier   = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )

            HorizontalDivider(color = Color(0xFFE8EDF5), thickness = 0.5.dp)

            availableOptions.forEach { option ->
                val isSelected = selectedSort == option
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                option.label,
                                fontSize   = 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color      = if (isSelected) Color(0xFF1A3A6E) else Color(0xFF0D2247),
                            )
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint     = Color(0xFF1A3A6E),
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    },
                    onClick = {
                        onSortChanged(option)
                        expanded = false
                    },
                    modifier = Modifier.background(
                        if (isSelected) Color(0xFF1A3A6E).copy(alpha = 0.05f) else Color.Transparent
                    ),
                )
            }
        }
    }
}

// ── Distance label composable — for complaint cards ───────────────────────────
@Composable
fun ComplaintDistanceLabel(
    distanceMeters: Double?,
    showDistance:   Boolean,
    modifier:       Modifier = Modifier,
) {
    if (!showDistance || distanceMeters == null) return

    val color = distanceColor(distanceMeters)
    val label = distanceLabel(distanceMeters)

    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier              = modifier,
    ) {
        Icon(
            Icons.Default.LocationOn,
            contentDescription = null,
            tint     = color,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text       = label,
            fontSize   = 11.sp,
            color      = color,
            fontWeight = FontWeight.Medium,
        )
    }
}

// ── "Beyond 5km" footer banner ────────────────────────────────────────────────
@Composable
fun BeyondRadiusBanner(
    hiddenCount: Int,
    onShowAll:   () -> Unit,
    modifier:    Modifier = Modifier,
) {
    if (hiddenCount <= 0) return

    Column(
        modifier            = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HorizontalDivider(color = Color(0xFFE8EDF5))

        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                Icons.Rounded.Warning,
                contentDescription = null,
                tint     = Color(0xFFE67E22),
                modifier = Modifier.size(14.dp),
            )
            Text(
                "$hiddenCount more issue${if (hiddenCount > 1) "s" else ""} beyond 5km",
                fontSize = 12.sp,
                color    = Color(0xFF6A7F9A),
            )
        }

        OutlinedButton(
            onClick  = onShowAll,
            shape    = RoundedCornerShape(20.dp),
            border   = BorderStroke(1.dp, Color(0xFF1A3A6E)),
            modifier = Modifier.height(36.dp),
        ) {
            Text(
                "Show all in district",
                fontSize = 12.sp,
                color    = Color(0xFF1A3A6E),
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
@Composable
fun YouReportedBadge() {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.padding(bottom = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(10.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "YOU REPORTED",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
