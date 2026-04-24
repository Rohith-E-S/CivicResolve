package com.example.complaintportal.ui.screens.user

import android.content.Intent
import android.net.Uri
import com.example.complaintportal.ui.screens.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.complaintportal.ui.viewmodel.ComplaintViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserComplaintDetailScreen(
    viewModel: ComplaintViewModel,
    complaintId: String,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showZoomDialog by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(complaintId) {
        viewModel.fetchComplaint(complaintId)
    }

    val complaint = state.currentComplaint

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                title = { Text("CivicResolve", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primaryContainer) },
                actions = {
                    if (complaint != null) {
                        IconButton(onClick = {
                            val mapUri = Uri.parse("geo:${complaint.latitude},${complaint.longitude}?q=${complaint.latitude},${complaint.longitude}(${Uri.encode(complaint.category)})")
                            val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
                            context.startActivity(mapIntent)
                        }) {
                            Icon(Icons.Default.Map, contentDescription = "Open in Maps", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TITLE, "Complaint Details")
                                putExtra(Intent.EXTRA_TEXT, """
                                    CivicResolve Complaint #${complaint.id?.takeLast(6)?.uppercase()}
                                    Category: ${complaint.category}
                                    Status: ${complaint.status.uppercase()}
                                    Location: ${complaint.landmark}, ${complaint.city}, ${complaint.state}
                                    
                                    Description:
                                    ${complaint.description}
                                    
                                    Map Location: https://www.google.com/maps/search/?api=1&query=${complaint.latitude},${complaint.longitude}
                                """.trimIndent())
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Complaint"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = "Profile", tint = MaterialTheme.colorScheme.outline)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background.copy(alpha=0.8f))
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (complaint != null) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Hero Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.2f), RoundedCornerShape(16.dp))
                ) {
                    Column {
                        Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                            if (!complaint.beforeImageUrl.isNullOrBlank()) {
                                Image(
                                    painter = rememberAsyncImagePainter(complaint.beforeImageUrl),
                                    contentDescription = "Complaint Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clickable { showZoomDialog = complaint.beforeImageUrl }
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.ImageNotSupported, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                                }
                            }
                            
                            val statusColor = when (complaint.status.lowercase()) {
                                "resolved" -> MaterialTheme.colorScheme.secondaryContainer
                                "in progress" -> MaterialTheme.colorScheme.tertiaryContainer
                                else -> MaterialTheme.colorScheme.primaryContainer
                            }
                            val onStatusColor = when (complaint.status.lowercase()) {
                                "resolved" -> MaterialTheme.colorScheme.onSecondaryContainer
                                "in progress" -> MaterialTheme.colorScheme.onTertiaryContainer
                                else -> MaterialTheme.colorScheme.onPrimaryContainer
                            }

                            Box(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .align(Alignment.TopEnd)
                                    .clip(RoundedCornerShape(100))
                                    .background(statusColor)
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text(complaint.status.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = onStatusColor)
                            }
                        }
                        
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text("Case ID #${complaint.id?.takeLast(6)?.uppercase()}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(complaint.category, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("${complaint.landmark}, ${complaint.city}, ${complaint.state}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Description
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(24.dp)
                ) {
                    Column {
                        Text("Issue Description", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(complaint.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = MaterialTheme.typography.bodyLarge.lineHeight)
                    }
                }

                if (!complaint.afterImageUrl.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(24.dp)
                    ) {
                        Column {
                            Text("Resolution Proof", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))
                            Image(
                                painter = rememberAsyncImagePainter(complaint.afterImageUrl),
                                contentDescription = "After Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)).clickable { showZoomDialog = complaint.afterImageUrl }
                            )
                        }
                    }
                }

                if (complaint.status.equals("resolved", ignoreCase = true)) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(24.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text("Rate Resolution", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                for (i in 1..5) {
                                    val isSelected = i <= complaint.rating
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.Star else Icons.Default.StarOutline,
                                        contentDescription = "Rate $i",
                                        tint = if (isSelected) Color(0xFFFFC107) else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clickable {
                                                if (complaint.rating == 0 && complaint.id != null) {
                                                    viewModel.rateComplaint(complaint.id, i) {
                                                        viewModel.fetchComplaint(complaint.id)
                                                    }
                                                }
                                            }
                                    )
                                }
                            }
                            if (complaint.rating > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Thank you for your feedback!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha=0.05f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha=0.1f), RoundedCornerShape(16.dp))
                        .padding(24.dp)
                ) {
                    Column {
                        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ChatBubble, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Need an update?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Our case managers are ready to provide detailed updates regarding this request.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { onNavigateToChat(complaint.id ?: "") },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Open Support Chat", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
            
            showZoomDialog?.let { imageUrl ->
                ZoomableImageDialog(imageUrl = imageUrl) {
                    showZoomDialog = null
                }
            }
        }
    }
}