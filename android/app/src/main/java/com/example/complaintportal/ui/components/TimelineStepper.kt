package com.example.complaintportal.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.complaintportal.data.model.ComplaintTimestamps
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ── Colors ────────────────────────────────────────────────────────────────────
private val NavyPrimary   = Color(0xFF1A3A6E)
private val TealAccent    = Color(0xFF7ECFC0)
private val GreenResolved = Color(0xFF1D9E75)
private val AmberActive   = Color(0xFFE67E22)
private val BgLight       = Color(0xFFF2F4F8)
private val CardWhite     = Color(0xFFFFFFFF)
private val TextPrimary   = Color(0xFF0D2247)
private val TextSecondary = Color(0xFF6A7F9A)
private val GrayLine      = Color(0xFFDDE3EE)

// ── Step model ────────────────────────────────────────────────────────────────
private data class TimelineStep(
    val label:       String,
    val description: String,
    val icon:        ImageVector,
    val timestamp:   String?,      // ISO string or null
    val state:       StepState,
)

private enum class StepState { COMPLETED, CURRENT, PENDING }

// ── Main composable ───────────────────────────────────────────────────────────
/**
 * Drop inside your ComplaintDetailScreen:
 *
 *   ComplaintTimeline(
 *       currentStatus = complaint.status,
 *       timestamps    = complaint.timestamps,
 *   )
 *
 * currentStatus values: "new" | "under_review" | "in_progress" | "resolved"
 */
@Composable
fun ComplaintTimeline(
    currentStatus: String,
    timestamps:    ComplaintTimestamps?,
    modifier:      Modifier = Modifier,
) {
    // Build ordered steps
    val steps = buildSteps(currentStatus, timestamps)

    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { loaded = true }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardWhite)
            .padding(20.dp),
    ) {
        Text(
            "Issue Progress",
            fontSize   = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color      = TextPrimary,
        )
        Spacer(Modifier.height(16.dp))

        steps.forEachIndexed { idx, step ->
            TimelineRow(
                step       = step,
                isLast     = idx == steps.lastIndex,
                stepIndex  = idx,
                loaded     = loaded,
            )
        }
    }
}

// ── Single timeline row ───────────────────────────────────────────────────────
@Composable
private fun TimelineRow(
    step:      TimelineStep,
    isLast:    Boolean,
    stepIndex: Int,
    loaded:    Boolean,
) {
    val (iconBg, iconTint, lineColor) = when (step.state) {
        StepState.COMPLETED -> Triple(GreenResolved, Color.White,         GreenResolved)
        StepState.CURRENT   -> Triple(NavyPrimary,   Color.White,         GrayLine)
        StepState.PENDING   -> Triple(BgLight,        TextSecondary,       GrayLine)
    }

    // Animate icon scale on load
    val scale by animateFloatAsState(
        targetValue   = if (loaded) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMedium,
        ),
        label = "step_$stepIndex",
    )

    Row(modifier = Modifier.fillMaxWidth()) {

        // Left column: icon + connecting line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.width(40.dp),
        ) {
            // Step icon circle
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconBg),
            ) {
                if (step.state == StepState.CURRENT) {
                    // Pulsing ring for current step
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse_$stepIndex")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue  = 0.3f,
                        targetValue   = 0f,
                        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart),
                        label         = "pulseAlpha",
                    )
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue  = 1f,
                        targetValue   = 1.6f,
                        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart),
                        label         = "pulseScale",
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NavyPrimary.copy(alpha = pulseAlpha))
                            .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }
                    )
                }

                Icon(
                    imageVector        = step.icon,
                    contentDescription = null,
                    tint               = iconTint,
                    modifier           = Modifier
                        .size(18.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale },
                )
            }

            // Connecting vertical line (except last step)
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(48.dp)
                        .background(lineColor),
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        // Right column: label + date + description
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 6.dp, bottom = if (isLast) 0.dp else 24.dp),
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    text       = step.label,
                    fontSize   = 14.sp,
                    fontWeight = if (step.state != StepState.PENDING) FontWeight.SemiBold else FontWeight.Normal,
                    color      = if (step.state != StepState.PENDING) TextPrimary else TextSecondary,
                )

                // Date chip or "Pending"
                if (step.timestamp != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(GreenResolved.copy(alpha = 0.08f))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            formatDate(step.timestamp),
                            fontSize = 10.sp,
                            color    = GreenResolved,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                } else if (step.state == StepState.CURRENT) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(NavyPrimary.copy(alpha = 0.08f))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text("In progress", fontSize = 10.sp, color = NavyPrimary, fontWeight = FontWeight.Medium)
                    }
                } else {
                    Text("Pending", fontSize = 10.sp, color = TextSecondary)
                }
            }

            Spacer(Modifier.height(2.dp))

            Text(
                text     = step.description,
                fontSize = 12.sp,
                color    = TextSecondary,
                lineHeight = 17.sp,
            )
        }
    }
}

// ── Build steps from current status + timestamps ──────────────────────────────
private fun buildSteps(status: String, ts: ComplaintTimestamps?): List<TimelineStep> {
    val s = status.lowercase()

    // Map every backend status to a step index (0-based)
    val currentIdx = when (s) {
        "new", "under_review"  -> 0   // still at "Reported"
        "in_progress",
        "re_opened"            -> 1   // field work happening
        "pending_verification",
        "disputed"             -> 2   // admin resolved, awaiting community
        "resolved",
        "confirmed_resolved"   -> 3   // citizens confirmed it
        else                   -> 0
    }

    fun stepState(stepIdx: Int) = when {
        currentIdx > stepIdx  -> StepState.COMPLETED
        currentIdx == stepIdx -> StepState.CURRENT
        else                  -> StepState.PENDING
    }

    return listOf(
        // ── Step 0: Reported ────────────────────────────────
        TimelineStep(
            label       = "Reported",
            description = "Issue submitted by citizen",
            icon        = Icons.Default.Flag,
            timestamp   = ts?.reported,
            state       = StepState.COMPLETED, // always done once complaint exists
        ),

        // ── Step 1: In Progress ──────────────────────────────
        TimelineStep(
            label       = if (s == "re_opened") "Re-opened" else "In Progress",
            description = if (s == "re_opened")
                              "Issue re-opened after citizen dispute"
                          else
                              "Admin assigned — field team working on it",
            icon        = if (s == "re_opened") Icons.Default.Replay else Icons.Default.Build,
            timestamp   = ts?.inProgress ?: ts?.reopened,
            state       = stepState(1),
        ),

        // ── Step 2: Resolved by Admin ────────────────────────
        TimelineStep(
            label       = if (s == "disputed") "Disputed" else "Resolved by Admin",
            description = when (s) {
                "disputed"             -> "A citizen disputed — admin review pending"
                "pending_verification" -> "Admin marked resolved — awaiting citizen verification"
                else                   -> "Admin closed the issue"
            },
            icon        = when (s) {
                "disputed" -> Icons.Default.ReportProblem
                else       -> Icons.Default.AdminPanelSettings
            },
            timestamp   = ts?.pendingVerification ?: ts?.disputed,
            state       = stepState(2),
        ),

        // ── Step 3: Citizen Verification ─────────────────────
        TimelineStep(
            label       = if (s == "confirmed_resolved") "Confirmed Resolved" else "Citizen Verification",
            description = when (s) {
                "confirmed_resolved" -> "Community confirmed the fix. Issue fully closed ✅"
                "resolved"           -> "3 citizens verified the fix on-site ✅"
                else                 -> "Nearby citizens verify the fix on-site (3 needed)"
            },
            icon        = if (currentIdx == 3) Icons.Default.VerifiedUser else Icons.Default.Groups,
            timestamp   = ts?.resolved ?: ts?.confirmedResolved,
            state       = stepState(3),
        ),
    )
}

// ── Format ISO date to readable ───────────────────────────────────────────────
private fun formatDate(isoString: String): String {
    return try {
        val instant   = Instant.parse(isoString)
        val formatter = DateTimeFormatter
            .ofPattern("MMM d, h:mm a")
            .withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (_: Exception) { "" }
}
