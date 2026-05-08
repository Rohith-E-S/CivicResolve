package com.example.complaintportal.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.complaintportal.data.remote.ApiService
import com.example.complaintportal.data.model.NearbyComplaint
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── Colors ────────────────────────────────────────────────────────────────────
private val NavyPrimary   = Color(0xFF1A3A6E)
private val TealAccent    = Color(0xFF7ECFC0)
private val AmberActive   = Color(0xFFE67E22)
private val CardWhite     = Color(0xFFFFFFFF)
private val BgLight       = Color(0xFFF2F4F8)
private val TextPrimary   = Color(0xFF0D2247)
private val TextSecondary = Color(0xFF6A7F9A)
private val DividerColor  = Color(0xFFE8EDF5)
private val WarnBg        = Color(0xFFFFF8E1)
private val WarnBorder    = Color(0xFFFFE082)
private val WarnText      = Color(0xFF8B6200)

// ── ViewModel ─────────────────────────────────────────────────────────────────
class NearbyViewModel(private val api: ApiService) : ViewModel() {

    private val _nearby  = MutableStateFlow<List<NearbyComplaint>>(emptyList())
    val nearby: StateFlow<List<NearbyComplaint>> = _nearby.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun fetchNearby(lat: Double, lng: Double, radiusMeters: Int = 500) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val response = api.getNearbyComplaints(lat, lng, radiusMeters)
                if (response.isSuccessful) {
                    _nearby.value = response.body()?.complaints ?: emptyList()
                }
            } catch (_: Exception) {
                _nearby.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }
}

// ── Nearby Banner — shown on Report page ─────────────────────────────────────
/**
 * Add this inside your ReportScreen composable, right below the
 * "New Request" title section and ABOVE the photo upload card.
 *
 * Call viewModel.fetchNearby(lat, lng) in LaunchedEffect when
 * the screen opens and location is available.
 *
 * Usage:
 *   val nearby by nearbyViewModel.nearby.collectAsState()
 *
 *   NearbyIssuesBanner(
 *       nearbyComplaints = nearby,
 *       onViewAll        = { showNearbySheet = true },
 *   )
 */
@Composable
fun NearbyIssuesBanner(
    nearbyComplaints: List<NearbyComplaint>,
    onViewAll:        () -> Unit,
    modifier:         Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = nearbyComplaints.isNotEmpty(),
        enter   = fadeIn(tween(400)) + expandVertically(tween(400)),
        exit    = fadeOut(tween(300)) + shrinkVertically(tween(300)),
        modifier = modifier.padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(WarnBg)
                .border(1.dp, WarnBorder, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Warning icon
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AmberActive.copy(alpha = 0.12f)),
            ) {
                Icon(
                    Icons.Outlined.Warning,
                    contentDescription = null,
                    tint               = AmberActive,
                    modifier           = Modifier.size(18.dp),
                )
            }

            // Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${nearbyComplaints.size} issue${if (nearbyComplaints.size > 1) "s" else ""} " +
                           "already reported nearby",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = WarnText,
                )
                Text(
                    "within 500m of your location",
                    fontSize = 11.sp,
                    color    = WarnText.copy(alpha = 0.7f),
                )
            }

            // View button
            TextButton(
                onClick      = onViewAll,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    "View",
                    fontSize   = 12.sp,
                    color      = NavyPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

// ── Nearby Issues Bottom Sheet ────────────────────────────────────────────────
/**
 * Triggered when user taps "View" on the banner.
 * Shows a list of nearby complaints so user can check if their
 * issue is already reported before submitting a duplicate.
 *
 * Usage:
 *   if (showNearbySheet) {
 *       NearbyIssuesSheet(
 *           complaints    = nearby,
 *           onDismiss     = { showNearbySheet = false },
 *           onStillReport = { showNearbySheet = false }, // user proceeds anyway
 *       )
 *   }
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyIssuesSheet(
    complaints:    List<NearbyComplaint>,
    onDismiss:     () -> Unit,
    onStillReport: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = CardWhite,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle       = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(DividerColor)
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            // Header
            Row(
                modifier              = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "Nearby Issues",
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TextPrimary,
                    )
                    Text(
                        "${complaints.size} reported within 500m",
                        fontSize = 12.sp,
                        color    = TextSecondary,
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(AmberActive.copy(alpha = 0.1f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        "📍 500m radius",
                        fontSize = 11.sp,
                        color    = AmberActive,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            // Info message
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(NavyPrimary.copy(alpha = 0.05f))
                    .padding(10.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    tint     = NavyPrimary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    "Check if your issue is already listed. If not, go ahead and report it!",
                    fontSize   = 12.sp,
                    color      = NavyPrimary,
                    lineHeight = 17.sp,
                )
            }

            Spacer(Modifier.height(14.dp))

            // Complaint list
            LazyColumn(
                modifier            = Modifier.heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(complaints, key = { it.id }) { complaint ->
                    NearbyComplaintCard(complaint = complaint)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Action buttons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Still report — proceed anyway
                Button(
                    onClick  = onStillReport,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Report Anyway — It's Different", fontWeight = FontWeight.Medium)
                }

                // Dismiss — cancel report
                OutlinedButton(
                    onClick  = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape    = RoundedCornerShape(14.dp),
                    border   = BorderStroke(1.dp, DividerColor),
                ) {
                    Text("Cancel — Already Reported", color = TextSecondary)
                }
            }
        }
    }
}

// ── Single nearby complaint card ──────────────────────────────────────────────
@Composable
private fun NearbyComplaintCard(complaint: NearbyComplaint) {
    val categoryEmoji = categoryEmoji(complaint.category)
    val statusColor   = statusColor(complaint.status)
    val statusLabel   = complaint.status
        .replace("_", " ")
        .replaceFirstChar { it.uppercase() }
    val distanceLabel = when {
        complaint.distanceMeters < 100  -> "< 100m away"
        complaint.distanceMeters < 1000 -> "${complaint.distanceMeters}m away"
        else -> "${"%.1f".format(complaint.distanceMeters / 1000f)}km away"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BgLight)
            .padding(12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Category emoji circle
        Box(
            contentAlignment = Alignment.Center,
            modifier         = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(CardWhite),
        ) {
            Text(categoryEmoji, fontSize = 20.sp)
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                complaint.category
                    .replace("_", " ")
                    .replaceFirstChar { it.uppercase() },
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color      = TextPrimary,
            )
            Text(
                complaint.description,
                fontSize  = 12.sp,
                color     = TextSecondary,
                maxLines  = 1,
                overflow  = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                // Distance
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = TealAccent, modifier = Modifier.size(12.dp))
                    Text(distanceLabel, fontSize = 10.sp, color = TealAccent, fontWeight = FontWeight.Medium)
                }
                // Status pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(statusColor.copy(alpha = 0.1f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(statusLabel, fontSize = 9.sp, color = statusColor, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────
private fun categoryEmoji(category: String): String = when (category.lowercase()) {
    "road_damage" -> "🕳️"
    "garbage_issue" -> "🗑️"
    "water_leakage" -> "💧"
    "electricity_issue" -> "⚡"
    "tree_fallen" -> "🧹"
    "accident" -> "🚦"
    "fire" -> "🔥"
    "drainage_problem" -> "🚰"
    "noise_issue" -> "🔊"
    else -> "📦"
}

private fun statusColor(status: String): Color = when (status.lowercase()) {
    "resolved"    -> Color(0xFF1D9E75)
    "in_progress" -> Color(0xFFE67E22)
    "under_review"-> Color(0xFF1A3A6E)
    else          -> Color(0xFFE53935)
}
