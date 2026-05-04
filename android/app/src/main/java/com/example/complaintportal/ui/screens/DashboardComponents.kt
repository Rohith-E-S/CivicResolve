package com.example.complaintportal.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onSupportClick: () -> Unit = {}
) {
    val sharedTransitionScope = com.example.complaintportal.ui.navigation.LocalSharedTransitionScope.current
    val animatedVisibilityScope = com.example.complaintportal.ui.navigation.LocalNavAnimatedVisibilityScope.current

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
                        contentDescription = "Complaint Image",
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
                    "in progress" -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.primaryContainer
                }
                val onStatusColor = when (statusClean) {
                    "resolved" -> MaterialTheme.colorScheme.onSecondaryContainer
                    "in progress" -> MaterialTheme.colorScheme.onTertiaryContainer
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
                        if (showCommunityFeatures) {
                            Text(
                                text = "Citizen in ${complaint.city}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary.copy(alpha=0.7f),
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                        Text(
                            text = complaint.category,
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
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${complaint.city}, ${complaint.state}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.outline,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (showCommunityFeatures) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha=0.2f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "📍",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "${(100..900).random()}m away",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        }
                        
                        val dateString = try {
                            complaint.createdAt?.substring(0, 10) ?: "Just now"
                        } catch (e: Exception) {
                            "Just now"
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dateString,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Medium,
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
                        enabled = complaint.status.trim().lowercase() != "resolved" && !isAdmin,
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
            contentDescription = "Refreshing",
            tint = color,
            modifier = Modifier
                .size(36.dp)
                .graphicsLayer {
                    rotationZ = if (isRefreshing) rotation else state.distanceFraction * 360f
                }
        )
    }
}

enum class SortOption {
    DATE_DESC, DATE_ASC, RATING_DESC
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
            imageVector = Icons.Rounded.KeyboardArrowUp,
            contentDescription = "Upvote",
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
                contentDescription = "Zoomed Image",
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
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }
    }
}
