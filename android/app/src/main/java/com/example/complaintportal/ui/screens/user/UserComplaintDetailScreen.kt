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
import androidx.compose.ui.res.stringResource
import com.example.complaintportal.R
import com.example.complaintportal.ui.components.ComplaintTimeline
import com.example.complaintportal.ui.components.VerificationCard
import androidx.compose.foundation.BorderStroke
import com.example.complaintportal.ui.components.DisputeInfoCard
import com.example.complaintportal.ui.screens.haversineDistance
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.FileOutputStream
import android.Manifest
import android.annotation.SuppressLint
import androidx.compose.runtime.LaunchedEffect
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun UserComplaintDetailScreen(
    viewModel: ComplaintViewModel,
    complaintId: String,
    userId: String,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showZoomDialog by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(complaintId) {
        viewModel.fetchComplaint(complaintId, userId)
    }

    val complaint = state.currentComplaint

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = MaterialTheme.colorScheme.primary)
                    }
                },
                title = { Text(stringResource(R.string.civicresolve), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primaryContainer) },
                actions = {
                    if (complaint != null) {
                        IconButton(onClick = {
                            val mapUri = Uri.parse("geo:${complaint.latitude},${complaint.longitude}?q=${complaint.latitude},${complaint.longitude}(${Uri.encode(complaint.category)})")
                            val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
                            context.startActivity(mapIntent)
                        }) {
                            Icon(Icons.Default.Map, contentDescription = stringResource(R.string.open_in_maps), tint = MaterialTheme.colorScheme.primary)
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
                            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(androidx.compose.ui.graphics.Color(0xFF1A3A6E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "U",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = androidx.compose.ui.graphics.Color.White
                        )
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
                                val sharedTransitionScope = com.example.complaintportal.ui.navigation.LocalSharedTransitionScope.current
                                val animatedVisibilityScope = com.example.complaintportal.ui.navigation.LocalNavAnimatedVisibilityScope.current
                                var imageModifier = Modifier.fillMaxSize().clickable { showZoomDialog = complaint.beforeImageUrl }
                                
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
                                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.ImageNotSupported, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                                }
                            }
                            
                            com.example.complaintportal.ui.theme.MorphingStatusBadge(
                                status = complaint.status,
                                modifier = Modifier
                                    .padding(16.dp)
                                    .align(Alignment.TopEnd)
                            )
                        }
                        
                        Column(modifier = Modifier.padding(24.dp)) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.fillMaxWidth().padding(end = 64.dp)) {
                                    Text("Case ID #${complaint.id?.takeLast(6)?.uppercase()}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(complaint.category.replace("_", " ").replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                                }

                                PriorityUpvoteButton(
                                    supportCount = complaint.supportCount ?: 0,
                                    isSupported = state.supportedIds.contains(complaint.id),
                                    onSupportClick = {
                                        if (complaint.id != null) {
                                            viewModel.supportComplaint(complaint.id) {
                                                viewModel.fetchComplaint(complaint.id, userId)
                                            }
                                        }
                                    },
                                    enabled = complaint.user?.id != userId,
                                    modifier = Modifier.align(Alignment.TopEnd)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("${complaint.landmark}, ${complaint.city}, ${complaint.state}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // Timeline Section
                ComplaintTimeline(
                    currentStatus = complaint.status,
                    timestamps = complaint.timestamps,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Verification & Dispute UI
                val status = complaint.status.lowercase()

                // Real GPS using FusedLocationProviderClient
                var userLat by remember { mutableStateOf<Double?>(null) }
                var userLng by remember { mutableStateOf<Double?>(null) }
                var locationError by remember { mutableStateOf<String?>(null) }

                @SuppressLint("MissingPermission")
                LaunchedEffect(Unit) {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasPermission) {
                        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                        val cts = CancellationTokenSource()
                        try {
                            val loc = fusedClient.getCurrentLocation(
                                Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token
                            ).await()
                            if (loc != null) {
                                userLat = loc.latitude
                                userLng = loc.longitude
                            } else {
                                // Fallback to last known
                                fusedClient.lastLocation.addOnSuccessListener { last ->
                                    if (last != null) { userLat = last.latitude; userLng = last.longitude }
                                    else locationError = "Could not get location. Move outdoors and try again."
                                }
                            }
                        } catch (e: Exception) {
                            locationError = "Location error: ${e.message}"
                        }
                    } else {
                        locationError = "Location permission required to verify or dispute."
                    }
                }

                val dist = if (userLat != null && userLng != null) {
                    haversineDistance(
                        userLat!!, userLng!!,
                        complaint.latitude.toDoubleOrNull() ?: 0.0,
                        complaint.longitude.toDoubleOrNull() ?: 0.0
                    )
                } else Double.MAX_VALUE // treat as out of range until location is known
                val isWithinRange = dist <= 500.0

                var showDisputeDialog by remember { mutableStateOf(false) }

                if (status == "pending_verification") {
                    VerificationCard(
                        complaint = complaint,
                        isWithinRange = isWithinRange,
                        isLoading = state.verifyLoading,
                        onVerifyClick = {
                            viewModel.verifyComplaint(complaint.id, userLat ?: 0.0, userLng ?: 0.0) {
                                viewModel.fetchComplaint(complaintId, userId)
                            }
                        },
                        onDisputeClick = {
                            showDisputeDialog = true
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                } else if (status == "disputed") {
                    DisputeInfoCard(
                        complaint = complaint,
                        isAdmin = false
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                if (showDisputeDialog) {
                    DisputeSubmissionDialog(
                        onDismiss = { showDisputeDialog = false },
                        onSubmit = { description, imageUri ->
                            // Convert Uri to MultipartBody.Part
                            val file = File(context.cacheDir, "dispute_photo.jpg")
                            val inputStream = context.contentResolver.openInputStream(imageUri)
                            val bitmap = BitmapFactory.decodeStream(inputStream)
                            val out = FileOutputStream(file)
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                            out.flush()
                            out.close()

                            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                            val body = MultipartBody.Part.createFormData("disputePhoto", file.name, requestFile)
                            
                            val latBody = (userLat ?: 0.0).toString().toRequestBody("text/plain".toMediaTypeOrNull())
                            val lngBody = (userLng ?: 0.0).toString().toRequestBody("text/plain".toMediaTypeOrNull())
                            val descBody = description.toRequestBody("text/plain".toMediaTypeOrNull())

                            viewModel.disputeComplaint(complaint.id, latBody, lngBody, descBody, body) {
                                showDisputeDialog = false
                                viewModel.fetchComplaint(complaintId, userId)
                            }
                        },
                        isLoading = state.disputeLoading,
                        error = state.disputeError
                    )
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
                        Text(stringResource(R.string.issue_description), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                            Text(stringResource(R.string.resolution_proof), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))
                            Image(
                                painter = rememberAsyncImagePainter(complaint.afterImageUrl),
                                contentDescription = stringResource(R.string.after_image),
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
                            Text(stringResource(R.string.rate_resolution), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                                Text(stringResource(R.string.thank_you_for_your_feedback), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
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
                        Text(stringResource(R.string.need_an_update), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.our_case_managers_are_ready_to_provide_detailed_updates_regarding_this_request), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { onNavigateToChat(complaint.id ?: "") },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(stringResource(R.string.open_support_chat), fontWeight = FontWeight.Bold)
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

@Composable
fun DisputeSubmissionDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, Uri) -> Unit,
    isLoading: Boolean = false,
    error: String? = null
) {
    var description by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ReportProblem, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Dispute Resolution", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Please provide evidence that the issue is still present. A photo and brief description are required.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("What is still wrong?") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(selectedImageUri),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Add Photo Evidence", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedImageUri?.let { onSubmit(description, it) } },
                enabled = description.isNotBlank() && selectedImageUri != null && !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                } else {
                    Text("Submit Dispute")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}