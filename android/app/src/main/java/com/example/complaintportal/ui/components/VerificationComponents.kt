package com.example.complaintportal.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import coil.compose.rememberAsyncImagePainter
import com.example.complaintportal.data.model.Complaint
import com.example.complaintportal.data.model.isRealDispute
import com.example.complaintportal.R
import androidx.compose.ui.res.stringResource
import com.example.complaintportal.ui.theme.bounceClick

@Composable
fun VerificationCard(
    complaint: Complaint,
    isWithinRange: Boolean,
    hasAlreadyVerified: Boolean,
    onVerifyClick: () -> Unit,
    onDisputeClick: () -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Rounded.FactCheck,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Verification Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "The authorities have marked this issue as fixed. As a local citizen, please verify if the work is completed correctly.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Progress for verification
            val verificationCount = complaint.verificationCount ?: 0
            val progress = verificationCount / 3f

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "Community Progress",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "$verificationCount/3 confirmed",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (!isWithinRange) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.LocationOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "You must be within 500m to verify.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            val context = androidx.compose.ui.platform.LocalContext.current
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                var showConfirmDialog by remember { mutableStateOf(false) }

                if (showConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showConfirmDialog = false },
                        title = { 
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF1D9E75))
                                Text("Confirm Resolution", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                        },
                        text = { 
                            Text(
                                "Are you sure this issue is fixed? Your verification directly impacts the community status of this report.",
                                style = MaterialTheme.typography.bodyMedium
                            ) 
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showConfirmDialog = false
                                    onVerifyClick()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D9E75)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Yes, It's Fixed", fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showConfirmDialog = false }) {
                                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        shape = RoundedCornerShape(24.dp)
                    )
                }

                Button(
                    onClick = {
                        if (hasAlreadyVerified) {
                            android.widget.Toast.makeText(context, "You have already verified this report!", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            showConfirmDialog = true
                        }
                    },
                    enabled = isWithinRange && !isLoading,
                    modifier = Modifier.weight(1f).bounceClick { 
                        if (hasAlreadyVerified) {
                            android.widget.Toast.makeText(context, "You have already verified this report!", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            showConfirmDialog = true
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasAlreadyVerified) Color(0xFF157053) else Color(0xFF1D9E75) // Darker green if verified
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(
                            if (hasAlreadyVerified) Icons.Rounded.CheckCircle else Icons.Rounded.DoneAll, 
                            contentDescription = null, 
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (hasAlreadyVerified) "Verified" else "Yes, Fixed", fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onDisputeClick,
                    enabled = !isLoading && !hasAlreadyVerified,
                    modifier = Modifier.weight(1f).bounceClick { 
                        if (!hasAlreadyVerified) onDisputeClick() 
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (hasAlreadyVerified) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, if (hasAlreadyVerified) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Rounded.ReportProblem, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Not Fixed", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DisputeInfoCard(
    complaint: Complaint,
    isAdmin: Boolean = false,
    onResolveClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dispute = complaint.dispute ?: return
    if (!dispute.isRealDispute()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Rounded.Gavel,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Dispute Active",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "A citizen has disputed the resolution, claiming the issue still exists.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                // Evidence Image
                Card(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(dispute.photo ?: ""),
                        contentDescription = "Dispute Evidence",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "CITIZEN FEEDBACK",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                    Text(
                        text = dispute.description ?: "No description provided.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AI Analysis Section
            Surface(
                color = Color.Black.copy(alpha = 0.03f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF673AB7),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI EVIDENCE ANALYSIS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF673AB7)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val aiStatus = if (dispute.aiAnalysis?.issueStillPresent == true) "ISSUE DETECTED" else "ISSUE NOT DETECTED"
                    val aiColor = if (dispute.aiAnalysis?.issueStillPresent == true) MaterialTheme.colorScheme.error else Color(0xFF1D9E75)
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(aiColor))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = aiStatus,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = aiColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${dispute.aiAnalysis?.confidence ?: "?"}% confidence",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dispute.aiAnalysis?.reasoning ?: "Analysis pending...",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            if (isAdmin) {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onResolveClick("reopen") },
                        modifier = Modifier.weight(1f).bounceClick { onResolveClick("reopen") },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Re-open Issue", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onResolveClick("confirm") },
                        modifier = Modifier.weight(1f).bounceClick { onResolveClick("confirm") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D9E75)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Confirm Fixed", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
