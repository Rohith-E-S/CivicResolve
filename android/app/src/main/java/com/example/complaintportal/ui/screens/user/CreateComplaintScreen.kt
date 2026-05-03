package com.example.complaintportal.ui.screens.user

import com.example.complaintportal.ui.screens.*
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import android.speech.RecognizerIntent
import android.widget.Toast
import coil.compose.rememberAsyncImagePainter
import com.example.complaintportal.ui.viewmodel.ComplaintViewModel
import com.example.complaintportal.ui.viewmodel.AuthViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.airbnb.lottie.compose.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.tileprovider.tilesource.XYTileSource
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun CreateComplaintScreen(
    viewModel: ComplaintViewModel,
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    var description by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var userState by rememberSaveable { mutableStateOf("") }
    var landmark by rememberSaveable { mutableStateOf("") }
    var selectedImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var lat by rememberSaveable { mutableStateOf("0.0") }
    var lng by rememberSaveable { mutableStateOf("0.0") }
    var locationMessage by rememberSaveable { mutableStateOf("") }
    var showConfetti by rememberSaveable { mutableStateOf(false) }
    var showOutOfBoundsDialog by remember { mutableStateOf(false) }
    var isVoiceActive by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // --- Animation for Microphone ---
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val micScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isVoiceActive) 1.3f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "micScale"
    )

    val fetchFreshLocation = {
        try {
            locationMessage = "Fetching location..."
            val priority = Priority.PRIORITY_HIGH_ACCURACY
            fusedLocationClient.getCurrentLocation(priority, CancellationTokenSource().token)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        lat = location.latitude.toString()
                        lng = location.longitude.toString()
                        locationMessage = "Location fetched."
                    } else {
                        locationMessage = "Unable to fetch location. Please turn on GPS."
                    }
                }.addOnFailureListener {
                    locationMessage = "Failed to get location: ${it.message}"
                }
        } catch (e: SecurityException) {
            locationMessage = "Location permission error"
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            locationMessage = "Permission granted. Fetching..."
            fetchFreshLocation()
        } else {
            locationMessage = "Location permission denied"
            Toast.makeText(context, "Location permission is required to fetch address", Toast.LENGTH_SHORT).show()
        }
    }

    val getLocation = {
        val finePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarsePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        
        if (finePermission == PackageManager.PERMISSION_GRANTED || coarsePermission == PackageManager.PERMISSION_GRANTED) {
            locationMessage = "Fetching..."
            fetchFreshLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    LaunchedEffect(lat, lng) {
        if (lat != "0.0" && lng != "0.0") {
            delay(1000)
            scope.launch(Dispatchers.IO) {
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lat.toDouble(), lng.toDouble(), 1)
                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        val detectedDistrict = address.subAdminArea ?: address.locality ?: address.adminArea ?: ""
                        val detectedCity = address.locality ?: address.subAdminArea ?: ""
                        val detectedState = address.adminArea ?: ""
                        
                        withContext(Dispatchers.Main) {
                            city = detectedCity
                            userState = detectedState
                            val extractedLandmark = address.featureName ?: address.thoroughfare ?: address.subLocality ?: ""
                            if (extractedLandmark != detectedCity && extractedLandmark != detectedState) {
                                landmark = extractedLandmark
                            }
                            locationMessage = "Location updated."
                            
                            // NEW: Automatically save home district if it's currently null
                            if (authState.detectedDistrict.isNullOrBlank() && detectedDistrict.isNotBlank()) {
                                authViewModel.completeOnboarding(detectedDistrict) { /* Silently updated */ }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    var tempCameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val uCropLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val resultUri = com.yalantis.ucrop.UCrop.getOutput(result.data!!)
            if (resultUri != null) selectedImageUri = resultUri
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { resultUri ->
        resultUri?.let {
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

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempCameraUri != null) {
            val destinationUri = Uri.fromFile(File(context.cacheDir, "crop_${System.currentTimeMillis()}.jpg"))
            val options = com.yalantis.ucrop.UCrop.Options().apply {
                setCompressionQuality(70)
                setFreeStyleCropEnabled(true)
            }
            val intent = com.yalantis.ucrop.UCrop.of(tempCameraUri!!, destinationUri)
                .withOptions(options)
                .getIntent(context)
            uCropLauncher.launch(intent)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            try {
                val file = File(context.cacheDir, "camera_photo_${System.currentTimeMillis()}.jpg")
                if (!file.exists()) file.createNewFile()
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                tempCameraUri = uri
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isVoiceActive = false
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText: String? =
                result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
            spokenText?.let { description = if (description.isEmpty()) it else "$description $it" }
        }
    }

    val startVoiceInput = {
        val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Describe the issue...")
        }
        try {
            isVoiceActive = true
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            isVoiceActive = false
            scope.launch { snackbarHostState.showSnackbar("Speech recognition not available") }
        }
    }
    
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    val sharedTransitionScope = com.example.complaintportal.ui.navigation.LocalSharedTransitionScope.current
    val animatedVisibilityScope = com.example.complaintportal.ui.navigation.LocalNavAnimatedVisibilityScope.current
    var containerModifier: Modifier = Modifier.fillMaxSize()
    
    if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            containerModifier = containerModifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = "fab_to_create"),
                animatedVisibilityScope = animatedVisibilityScope,
                resizeMode = androidx.compose.animation.SharedTransitionScope.ResizeMode.ScaleToBounds()
            )
        }
    }

    Box(modifier = containerModifier) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("CivicResolve", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            },
            bottomBar = {
                val isSubmitEnabled = !state.isLoading && description.isNotBlank() && lat != "0.0" && lng != "0.0" && selectedImageUri != null
                
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp)) {
                    Button(
                        onClick = {
                            val homeDistrict = authState.detectedDistrict ?: ""
                            val isWithinBounds = homeDistrict.isBlank() || 
                                city.contains(homeDistrict, ignoreCase = true) || 
                                userState.contains(homeDistrict, ignoreCase = true) ||
                                homeDistrict.contains(city, ignoreCase = true) ||
                                homeDistrict.contains(landmark, ignoreCase = true)

                            if (!isWithinBounds && homeDistrict.isNotBlank()) {
                                showOutOfBoundsDialog = true
                                return@Button
                            }

                            var imagePart: MultipartBody.Part? = null
                            selectedImageUri?.let { imgUri ->
                                val inputStream = context.contentResolver.openInputStream(imgUri)
                                val uploadFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
                                val outputStream = FileOutputStream(uploadFile)
                                inputStream?.copyTo(outputStream)
                                inputStream?.close()
                                outputStream.close()
                                val requestFile = uploadFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                                imagePart = MultipartBody.Part.createFormData("imageUrl", uploadFile.name, requestFile)
                            }

                            val descReq = description.toRequestBody("text/plain".toMediaTypeOrNull())
                            val latReq = lat.toRequestBody("text/plain".toMediaTypeOrNull())
                            val lngReq = lng.toRequestBody("text/plain".toMediaTypeOrNull())
                            val cityReq = city.toRequestBody("text/plain".toMediaTypeOrNull())
                            val stateReq = userState.toRequestBody("text/plain".toMediaTypeOrNull())
                            val landmarkReq = landmark.toRequestBody("text/plain".toMediaTypeOrNull())

                            viewModel.createComplaint(descReq, latReq, lngReq, cityReq, stateReq, landmarkReq, imagePart) {
                                showConfetti = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp).shadow(if (isSubmitEnabled) 8.dp else 0.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSubmitEnabled) Color(0xFF00796B) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSubmitEnabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        enabled = isSubmitEnabled
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Submit Report", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text("New Request", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Help your community by reporting local issues.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                }
                
                Spacer(modifier = Modifier.height(32.dp))

                // Camera-First Header (Hero Card)
                Box(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.5f), RoundedCornerShape(28.dp))
                ) {
                    if (selectedImageUri == null) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Rounded.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha=0.4f), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Show us the issue", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                HeroActionButton(
                                    icon = Icons.Default.PhotoCamera,
                                    label = "Camera",
                                    onClick = {
                                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                            try {
                                                val file = File(context.cacheDir, "camera_photo_${System.currentTimeMillis()}.jpg")
                                                if (!file.exists()) file.createNewFile()
                                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                                tempCameraUri = uri
                                                cameraLauncher.launch(uri)
                                            } catch (e: Exception) { e.printStackTrace() }
                                        } else {
                                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    }
                                )
                                HeroActionButton(
                                    icon = Icons.Default.Image,
                                    label = "Gallery",
                                    onClick = { galleryLauncher.launch("image/*") }
                                )
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(
                                painter = rememberAsyncImagePainter(selectedImageUri),
                                contentDescription = "Evidence Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(16.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .clickable { selectedImageUri = null },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Retake Photo", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Dynamic Description Field
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text("DESCRIPTION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(
                        value = description,
                        onValueChange = { if (it.length <= 500) description = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        placeholder = { Text("What's happening?") },
                        minLines = 1,
                        maxLines = 8,
                        trailingIcon = {
                            Box(
                                modifier = Modifier
                                    .scale(micScale)
                                    .clip(CircleShape)
                                    .background(if (isVoiceActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .clickable { startVoiceInput() }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Mic, 
                                    contentDescription = "Voice Input", 
                                    tint = if (isVoiceActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Smart Location Card
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("LOCATION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        val readableAddress = listOf(landmark, city).filter { it.isNotBlank() }.joinToString(", ")
                        if (readableAddress.isNotBlank()) {
                            Text("Detecting: $readableAddress", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                Configuration.getInstance().apply {
                                    load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
                                    userAgentValue = ctx.packageName
                                }

                                val CARTO_TILE_SOURCE = XYTileSource(
                                    "CartoLight",
                                    0, 19, 256, ".png",
                                    arrayOf(
                                        "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
                                        "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
                                        "https://c.basemaps.cartocdn.com/rastertiles/voyager/"
                                    ),
                                    "© OpenStreetMap contributors © CARTO"
                                )

                                MapView(ctx).apply {
                                    setTileSource(CARTO_TILE_SOURCE)
                                    setMultiTouchControls(true)
                                    controller.setZoom(17.5)
                                    
                                    val startPoint = GeoPoint(20.5937, 78.9629)
                                    if (lat != "0.0" && lng != "0.0") {
                                        startPoint.latitude = lat.toDouble()
                                        startPoint.longitude = lng.toDouble()
                                    }
                                    controller.setCenter(startPoint)

                                    addMapListener(object : MapListener {
                                        override fun onScroll(event: ScrollEvent?): Boolean {
                                            val center = mapCenter as GeoPoint
                                            if (Math.abs(center.latitude - lat.toDouble()) > 0.00001 || 
                                                Math.abs(center.longitude - lng.toDouble()) > 0.00001) {
                                                lat = center.latitude.toString()
                                                lng = center.longitude.toString()
                                            }
                                            return true
                                        }
                                        override fun onZoom(event: ZoomEvent?): Boolean = false
                                    })
                                }
                            },
                            update = { mapView ->
                                val center = mapView.mapCenter as GeoPoint
                                if (Math.abs(center.latitude - lat.toDouble()) > 0.00001 || 
                                    Math.abs(center.longitude - lng.toDouble()) > 0.00001) {
                                    mapView.controller.animateTo(GeoPoint(lat.toDouble(), lng.toDouble()))
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        
                        // Center Location Pin (hops when dragging via composition but here static icon in center)
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.Center).size(44.dp).offset(y = (-22).dp),
                            tint = Color(0xFFD32F2F)
                        )

                        // Adjust Button Overlay
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                                .clickable { getLocation() },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            tonalElevation = 6.dp,
                            shadowElevation = 4.dp
                        ) {
                            Icon(Icons.Default.GpsFixed, contentDescription = "Recenter", tint = Color.White, modifier = Modifier.padding(12.dp).size(22.dp))
                        }
                        
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha=0.6f)
                        ) {
                            Text("Drag map to adjust pin", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(140.dp))
            }
        }

        if (showConfetti) {
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(com.example.complaintportal.R.raw.confetti))
            val progress by animateLottieCompositionAsState(
                composition,
                iterations = 1,
                isPlaying = true
            )
            
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.fillMaxSize()
            )

            LaunchedEffect(progress) {
                if (progress == 1f) {
                    delay(300)
                    onSuccess()
                }
            }
        }
        if (showOutOfBoundsDialog) {
            // Determine the "current city" from reverse-geocoded fields
            val currentCity = listOf(city, userState).firstOrNull { it.isNotBlank() } ?: "your current location"

            AlertDialog(
                onDismissRequest = { showOutOfBoundsDialog = false },
                shape = RoundedCornerShape(28.dp),
                icon = {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.LocationOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                title = {
                    Text(
                        "You're Out of Town! 🌍",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Looks like you've travelled away from your home district.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        // Location comparison card
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Home District
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Home", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        authState.detectedDistrict ?: "Unknown",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                // Arrow
                                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                                // Current Location
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("You are here", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        currentCity,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                        Text(
                            "Would you like to switch your home district to $currentCity so you can report issues here?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showOutOfBoundsDialog = false
                            authViewModel.completeOnboarding(currentCity) { /* district updated */ }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.SwitchAccount, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Switch to $currentCity", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showOutOfBoundsDialog = false },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Stay in ${authState.detectedDistrict ?: "Home"}")
                    }
                }
            )
        }
    }
}

@Composable
fun HeroActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .padding(horizontal = 28.dp, vertical = 20.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
        Spacer(modifier = Modifier.height(10.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
    }
}
