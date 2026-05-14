package com.example.complaintportal.ui.notification

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.complaintportal.data.notification.NotificationItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import com.example.complaintportal.R

// ── Theme colors (matched to app's NavyPrimary/TealAccent palette) ────────────
private val NavyPrimary @Composable get() = MaterialTheme.colorScheme.primary
private val BgLight     @Composable get() = MaterialTheme.colorScheme.background
private val CardWhite   @Composable get() = MaterialTheme.colorScheme.surface
private val TextPrimary @Composable get() = MaterialTheme.colorScheme.onSurface
private val TextSecond  @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
private val UnreadBg    @Composable get() = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)

// ── Bell icon with animated red badge — drop into any TopBar ─────────────────
@Composable
fun NotificationBell(
    unreadCount: Int,
    onClick:     () -> Unit,
    modifier:    Modifier = Modifier,
) {
    val shake = remember { Animatable(0f) }
    LaunchedEffect(unreadCount) {
        if (unreadCount > 0) {
            repeat(3) {
                shake.animateTo(6f,  animationSpec = tween(60))
                shake.animateTo(-6f, animationSpec = tween(60))
            }
            shake.animateTo(0f, animationSpec = tween(60))
        }
    }

    Box(
        modifier = modifier
            .size(46.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        // Background as separate element to avoid clipping the badge
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(NavyPrimary.copy(alpha = 0.08f))
        )

        Icon(
            imageVector        = Icons.Default.Notifications,
            contentDescription = stringResource(R.string.notifications),
            tint               = NavyPrimary,
            modifier           = Modifier.size(22.dp).offset(x = shake.value.dp),
        )

        AnimatedVisibility(
            visible  = unreadCount > 0,
            enter    = scaleIn() + fadeIn(),
            exit     = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 4.dp, y = (-4).dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .sizeIn(minWidth = 16.dp, minHeight = 16.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE53935))
                    .padding(horizontal = 3.dp),
            ) {
                Text(
                    text       = if (unreadCount > 99) "99+" else unreadCount.toString(),
                    color      = Color.White,
                    fontSize   = 9.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

// ── Full Notification Screen ──────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    viewModel:        NotificationViewModel,
    onBack:           () -> Unit,
    onComplaintClick: (String) -> Unit,
    onChatClick:      (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.markAllRead() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.notifications), 
                        fontWeight = FontWeight.ExtraBold, 
                        color = NavyPrimary,
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back), tint = NavyPrimary)
                    }
                },
                actions = {
                    if (uiState.notifications.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearAll() }) {
                            Text(stringResource(R.string.clear_all), color = Color(0xFFE53935), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgLight),
            )
        },
        containerColor = BgLight,
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NavyPrimary)
            }
            uiState.notifications.isEmpty() -> EmptyNotifications(Modifier.padding(padding))
            else -> LazyColumn(
                modifier            = Modifier.padding(padding).fillMaxSize(),
                contentPadding      = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items = uiState.notifications, key = { it.id }) { notification ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.StartToEnd) {
                                viewModel.clearOne(notification.id)
                                true
                            } else {
                                false
                            }
                        }
                    )
                    
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = true,
                        enableDismissFromEndToStart = false,
                        modifier = Modifier.animateItem(),
                        backgroundContent = {
                            val color by animateColorAsState(
                                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) Color(0xFFE53935) else Color.Transparent,
                                label = "color"
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(color)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                                }
                            }
                        },
                        content = {
                            NotificationCard(
                                notification = notification,
                                onClick = {
                                    viewModel.markOneRead(notification.id)
                                    notification.complaintId?.let { id ->
                                        if (notification.type == "admin_comment" || notification.type == "new_message") {
                                            onChatClick(id)
                                        } else {
                                            onComplaintClick(id)
                                        }
                                    }
                                },
                            )
                        }
                    )
                }
            }
        }
    }
}

// ── Individual card ───────────────────────────────────────────────────────────
@Composable
private fun NotificationCard(notification: NotificationItem, onClick: () -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) CardWhite else UnreadBg
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (notification.isRead) 1.dp else 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(notification.iconBgColor().copy(alpha = 0.1f))
            ) {
                Icon(notification.icon(), contentDescription = null, tint = notification.iconBgColor(), modifier = Modifier.size(24.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (notification.isRead) FontWeight.Medium else FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = notification.timeAgo(context), 
                        style = MaterialTheme.typography.labelSmall, 
                        color = TextSecond
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecond,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(NavyPrimary)
                        .align(Alignment.CenterVertically)
                )
            }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────
@Composable
private fun EmptyNotifications(modifier: Modifier = Modifier) {
    Column(
        modifier            = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(120.dp).clip(CircleShape).background(NavyPrimary.copy(alpha = 0.1f)),
        ) {
            Icon(Icons.Default.NotificationsNone, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(48.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.you_re_all_caught_up), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text(
            text       = stringResource(R.string.notifications_empty_subtitle),
            style      = MaterialTheme.typography.bodyMedium,
            color      = TextSecond,
            textAlign  = TextAlign.Center,
            lineHeight = 20.sp,
        )
    }
}

// ── Extension helpers ─────────────────────────────────────────────────────────
private fun NotificationItem.icon(): ImageVector = when (type) {
    "status_changed"           -> Icons.Default.Autorenew
    "upvoted"                  -> Icons.Default.ThumbUp
    "admin_comment"            -> Icons.Default.Comment
    "new_message"              -> Icons.Default.Email
    "new_in_district"          -> Icons.Default.LocationOn
    "disputed"                 -> Icons.Default.Warning
    "admin_new_report"         -> Icons.Default.Assignment
    "admin_verification_alert" -> Icons.Default.VerifiedUser
    "points_earned"            -> Icons.Default.Star
    "rank_up"                  -> Icons.Default.EmojiEvents
    else                       -> Icons.Default.Notifications
}

private fun NotificationItem.iconBgColor(): Color = when (type) {
    "status_changed"           -> Color(0xFF1A3A6E)
    "upvoted"                  -> Color(0xFF1D9E75)
    "admin_comment"            -> Color(0xFFE67E22)
    "new_message"              -> Color(0xFF27AE60)
    "new_in_district"          -> Color(0xFF8E44AD)
    "disputed"                 -> Color(0xFFC0392B)
    "admin_new_report"         -> Color(0xFF2980B9)
    "admin_verification_alert" -> Color(0xFF27AE60)
    "points_earned"            -> Color(0xFFF39C12) // Golden Yellow
    "rank_up"                  -> Color(0xFF9B59B6) // Purple
    else                       -> Color(0xFF6A7F9A)
}

private fun NotificationItem.timeAgo(context: Context): String = try {
    val instant = Instant.parse(createdAt)
    val now     = Instant.now()
    val mins    = ChronoUnit.MINUTES.between(instant, now)
    val hours   = ChronoUnit.HOURS.between(instant, now)
    val days    = ChronoUnit.DAYS.between(instant, now)
    when {
        mins  < 1  -> context.getString(R.string.just_now)
        mins  < 60 -> context.getString(R.string.mins_ago_short, mins)
        hours < 24 -> context.getString(R.string.hours_ago_short, hours)
        days  < 7  -> context.getString(R.string.days_ago_short, days)
        else       -> DateTimeFormatter.ofPattern("MMM d").withZone(ZoneId.systemDefault()).format(instant)
    }
} catch (_: Exception) { "" }
