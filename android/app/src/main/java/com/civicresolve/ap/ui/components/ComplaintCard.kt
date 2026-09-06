package com.civicresolve.ap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.civicresolve.ap.data.model.Complaint
import com.civicresolve.ap.ui.theme.CivicRounded
import com.civicresolve.ap.ui.utils.formatDate
import com.civicresolve.ap.ui.utils.formatTime
import com.civicresolve.ap.ui.utils.statusDotColor

@Composable
fun ComplaintCard(
    complaint: Complaint,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val dot = statusDotColor(complaint.status)
    androidx.compose.material3.Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CivicRounded.lg))
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        shape = RoundedCornerShape(CivicRounded.lg),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.width(3.dp).height(96.dp).background(dot))
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                UiDot(color = dot, pulse = true, size = 9.dp, modifier = Modifier.padding(top = 5.dp))
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IdStencil(complaint.id)
                        StatusBadge(complaint.status)
                        if (!complaint.category.isNullOrBlank()) {
                            Text(
                                text = "• ${complaint.category.replace("_", " ")}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if ((complaint.supportCount ?: 0) > 0) {
                            Text(text = "▲ ${complaint.supportCount}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        text = complaint.description.ifBlank { "Reported issue" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(Icons.Outlined.CalendarToday, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Text(text = formatDate(complaint.createdAt), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = " • ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Icon(Icons.Outlined.LocationOn, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (complaint.landmark.isNotBlank()) "${complaint.landmark}, ${complaint.city}" else complaint.city.ifBlank { "Unknown" },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (complaint.status == "resolved" && complaint.rating > 0) {
                    Text(text = "★ ${complaint.rating}/5", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                } else {
                    Text(text = formatTime(complaint.createdAt), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}
