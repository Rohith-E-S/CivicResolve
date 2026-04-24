package com.example.complaintportal.ui.screens.user

import com.example.complaintportal.ui.screens.*
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.example.complaintportal.ui.viewmodel.ComplaintViewModel
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.tileprovider.tilesource.XYTileSource
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.Objects

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateComplaintScreen(
    viewModel: ComplaintViewModel,
    onNavigateBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var description by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var userState by rememberSaveable { mutableStateOf("") }
    var landmark by rememberSaveable { mutableStateOf("") }
    var selectedImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var lat by rememberSaveable { mutableStateOf("0.0") }
    var lng by rememberSaveable { mutableStateOf("0.0") }
    var locationMessage by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            try {
                locationMessage = "Fetching location..."
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        lat = location.latitude.toString()
                        lng = location.longitude.toString()
                        locationMessage = "Location fetched."
                        scope.launch(Dispatchers.IO) {
                            try {
                                val geocoder = Geocoder(context, Locale.getDefault())
                                @Suppress("DEPRECATION")
                                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                                if (!addresses.isNullOrEmpty()) {
                                    val address = addresses[0]
                                    city = address.locality ?: address.subAdminArea ?: address.adminArea ?: ""
                                    userState = address.adminArea ?: ""
                                    val extractedLandmark = address.featureName ?: address.thoroughfare ?: address.subLocality ?: ""
                                    if (extractedLandmark != city && extractedLandmark != userState) {
                                        landmark = extractedLandmark
                                    }
                                    locationMessage = "Location & city/state fetched."
                                } else {
                                    locationMessage = "Coordinates fetched. Add details manually."
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                locationMessage = "Coordinates fetched. Add details manually."
                            }
                        }
                    } else {
                        locationMessage = "Unable to fetch location. Turn on GPS."
                    }
                }
            } catch (e: SecurityException) {
                locationMessage = "Location permission denied"
            }
        } else {
            locationMessage = "Location permission denied"
        }
    }

    val getLocation = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            
            locationMessage = "Fetching location..."
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        lat = location.latitude.toString()
                        lng = location.longitude.toString()
                        locationMessage = "Location fetched."
                        scope.launch(Dispatchers.IO) {
                            try {
                                val geocoder = Geocoder(context, Locale.getDefault())
                                @Suppress("DEPRECATION")
                                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                                if (!addresses.isNullOrEmpty()) {
                                    val address = addresses[0]
                                    city = address.locality ?: address.subAdminArea ?: address.adminArea ?: ""
                                    userState = address.adminArea ?: ""
                                    val extractedLandmark = address.featureName ?: address.thoroughfare ?: address.subLocality ?: ""
                                    if (extractedLandmark != city && extractedLandmark != userState) {
                                        landmark = extractedLandmark
                                    }
                                    locationMessage = "Location & city/state fetched."
                                } else {
                                    locationMessage = "Coordinates fetched. Add details manually."
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                locationMessage = "Coordinates fetched. Add details manually."
                            }
                        }
                    } else {
                        locationMessage = "Unable to fetch location. Turn on GPS."
                    }
                }
            } catch (e: SecurityException) {
                locationMessage = "Location permission denied"
            }
        } else {
            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
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
    
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("CivicResolve", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primaryContainer) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Button(
                    onClick = {
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

                        viewModel.createComplaint(descReq, latReq, lngReq, cityReq, stateReq, landmarkReq, imagePart, onSuccess)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !state.isLoading && description.isNotBlank() && lat != "0.0" && lng != "0.0" && landmark.isNotBlank()
                ) {
                    Text(if (state.isLoading) "Submitting..." else "Submit Complaint", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
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
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("New Request", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
            Text("Submit an issue to your local administration for resolution.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            
            Spacer(modifier = Modifier.height(32.dp))

            // Description
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text("DESCRIPTION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                Text("${description.length} / 500", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
            }
            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= 500) description = it },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                placeholder = { Text("Describe the issue in detail...") },
                minLines = 5,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha=0.4f)
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Location
            Text("LOCATION DETAILS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)

            Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp)) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Tap map to drop a pin.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { getLocation() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                        ) {
                            Text("Current location", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))) {
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
                                    val mapController = controller
                                    mapController.setZoom(15.0)
                                    
                                    val startPoint = GeoPoint(20.5937, 78.9629)
                                    if (lat != "0.0" && lng != "0.0") {
                                        startPoint.latitude = lat.toDouble()
                                        startPoint.longitude = lng.toDouble()
                                    }
                                    mapController.setCenter(startPoint)

                                    val marker = Marker(this)
                                    marker.position = startPoint
                                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    overlays.add(marker)

                                    val mReceive: MapEventsReceiver = object : MapEventsReceiver {
                                        override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                            marker.position = p
                                            lat = p.latitude.toString()
                                            lng = p.longitude.toString()
                                            invalidate()
                                            
                                            locationMessage = "Pin dropped. Fetching details..."
                                            scope.launch(Dispatchers.IO) {
                                                try {
                                                    val geocoder = Geocoder(ctx, Locale.getDefault())
                                                    @Suppress("DEPRECATION")
                                                    val addresses = geocoder.getFromLocation(p.latitude, p.longitude, 1)
                                                    if (!addresses.isNullOrEmpty()) {
                                                        val address = addresses[0]
                                                        city = address.locality ?: address.subAdminArea ?: address.adminArea ?: ""
                                                        userState = address.adminArea ?: ""
                                                        val extractedLandmark = address.featureName ?: address.thoroughfare ?: address.subLocality ?: ""
                                                        if (extractedLandmark != city && extractedLandmark != userState) {
                                                            landmark = extractedLandmark
                                                        }
                                                        locationMessage = "Location pinned."
                                                    } else {
                                                        locationMessage = "Coordinates fetched. Add details manually."
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                    locationMessage = "Coordinates fetched. Add details manually."
                                                }
                                            }
                                            return false
                                        }

                                        override fun longPressHelper(p: GeoPoint): Boolean {
                                            return false
                                        }
                                    }
                                    overlays.add(MapEventsOverlay(mReceive))
                                }
                            },
                            update = { mapView ->
                                if (lat != "0.0" && lng != "0.0") {
                                    val pt = GeoPoint(lat.toDouble(), lng.toDouble())
                                    mapView.controller.animateTo(pt)
                                    
                                    val marker = mapView.overlays.filterIsInstance<Marker>().firstOrNull()
                                    marker?.position = pt
                                    mapView.invalidate()
                                }
                            }
                        )
                    }

                    if (locationMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(locationMessage, style = MaterialTheme.typography.labelSmall, color = if (locationMessage.contains("fetched") || locationMessage.contains("pinned")) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error)
                    }
                    if (lat != "0.0" && lng != "0.0") {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Coordinates: $lat, $lng", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (city.isNotEmpty() || userState.isNotEmpty() || landmark.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            val addressString = listOf(landmark, city, userState).filter { it.isNotBlank() }.joinToString(", ")
                            Text("Address: $addressString", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            if (lat != "0.0" && lng != "0.0") {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = landmark,
                    onValueChange = { landmark = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Landmark (Required)") },
                    placeholder = { Text("E.g., Near City Mall, Opp. Park") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha=0.4f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Evidence Photos
            Text("EVIDENCE PHOTOS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                
                selectedImageUri?.let { imgUri ->
                    Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp))) {
                        Image(painter = rememberAsyncImagePainter(imgUri), contentDescription = null, modifier = Modifier.fillMaxSize())
                        Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).clip(CircleShape).background(Color.Black.copy(alpha=0.4f)).clickable { selectedImageUri = null }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Box(
                    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainer).border(2.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.3f), RoundedCornerShape(12.dp)).clickable { galleryLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("GALLERY", style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Box(
                    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainer).border(2.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.3f), RoundedCornerShape(12.dp)).clickable { 
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            try {
                                val file = File(context.cacheDir, "camera_photo_${System.currentTimeMillis()}.jpg")
                                if (!file.exists()) file.createNewFile()
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                tempCameraUri = uri
                                cameraLauncher.launch(uri)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("CAMERA", style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp)) // padding for bottom bar
        }
    }
}
