package com.example.complaintportal.ui.screens.admin

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.io.FileOutputStream
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import com.example.complaintportal.ui.screens.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.complaintportal.ui.viewmodel.ComplaintViewModel
import androidx.compose.ui.res.stringResource
import com.example.complaintportal.R

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun AdminComplaintDetailScreen(
    viewModel: ComplaintViewModel,
    complaintId: String,
    userId: String,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val uCropLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val resultUri = com.yalantis.ucrop.UCrop.getOutput(result.data!!)
            resultUri?.let {
                val inputStream = context.contentResolver.openInputStream(it)
                val uploadFile = File(context.cacheDir, "after_image_${System.currentTimeMillis()}.jpg")
                val outputStream = FileOutputStream(uploadFile)
                inputStream?.copyTo(outputStream)
                outputStream.close()
                inputStream?.close()

                val requestFile = uploadFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("imageUrl", uploadFile.name, requestFile)
                viewModel.uploadAfterImage(complaintId, body) {
                    viewModel.fetchComplaint(complaintId, userId)
                    viewModel.fetchAdminComplaints(userId)
                }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val destinationUri = Uri.fromFile(File(context.cacheDir, "crop_${System.currentTimeMillis()}.jpg"))
            val options = com.yalantis.ucrop.UCrop.Options().apply {
                setCompressionQuality(70)
                setFreeStyleCropEnabled(true)
            }
            val intent = com.yalantis.ucrop.UCrop.of(it, destinationUri)
                .withOptions(options)
                .getIntent(context)
            uCropLauncher.launch(intent)
        }
    }

    LaunchedEffect(complaintId) {
        viewModel.fetchComplaint(complaintId, userId)
    }

    val complaint = state.currentComplaint
    var showZoomDialog by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Show errors from ViewModel
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = MaterialTheme.colorScheme.primary)
                    }
                },
                title = { Text(stringResource(R.string.case_management), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primaryContainer) },
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
                                    CivicResolve Complaint #${complaint.id.takeLast(6).uppercase()}
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
                            text = "A",
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
                                    Text("Case ID #${complaint.id.takeLast(6).uppercase()}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(complaint.category.replace("_", " ").replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                                }

                                PriorityUpvoteButton(
                                    supportCount = complaint.supportCount ?: 0,
                                    isSupported = state.supportedIds.contains(complaint.id),
                                    enabled = false, // Admins cannot upvote
                                    onSupportClick = {
                                        viewModel.supportComplaint(complaint.id) {
                                            viewModel.fetchComplaint(complaint.id, userId)
                                        }
                                    },
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

                Spacer(modifier = Modifier.height(24.dp))

                // Admin Controls
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha=0.3f))
                        .border(1.dp, MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(16.dp))
                        .padding(24.dp)
                ) {
                    Column {
                        Text(stringResource(R.string.administrative_actions), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            val statusLower = complaint.status.lowercase()
                            if (statusLower == "new" || statusLower == "under_review") {
                                Button(
                                    onClick = { 
                                        viewModel.updateComplaintStatus(complaintId, "in_progress") {
                                            viewModel.fetchComplaint(complaintId, userId)
                                            viewModel.fetchAdminComplaints(userId)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(stringResource(R.string.process_started))
                                }
                            } else {
                                Button(
                                    onClick = {},
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = false
                                ) {
                                    Text(if (statusLower == "resolved") stringResource(R.string.resolved) else stringResource(R.string.in_process))
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Button(
                                onClick = { onNavigateToChat(complaint.id) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                            ) {
                                Text(stringResource(R.string.open_chat))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Reporter Details
                if (complaint.user != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(24.dp)
                    ) {
                        Column {
                            Text(stringResource(R.string.reporter_details), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(complaint.user.fullName ?: "Unknown User", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                    Text(complaint.user.email ?: "No Email", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

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

                Spacer(modifier = Modifier.height(24.dp))

                // Resolution Proof
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
                        if (!complaint.afterImageUrl.isNullOrBlank()) {
                            Image(
                                painter = rememberAsyncImagePainter(complaint.afterImageUrl),
                                contentDescription = stringResource(R.string.after_image),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showZoomDialog = complaint.afterImageUrl }
                            )
                        } else {
                            OutlinedButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Upload, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.upload_resolution_image))
                            }
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