package com.civicresolve.ap.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.civicresolve.ap.data.model.Complaint
import com.civicresolve.ap.di.AppContainer
import com.civicresolve.ap.ui.components.*
import com.civicresolve.ap.ui.utils.statusLabel
import com.civicresolve.ap.ui.theme.MonoFontFamily
import com.civicresolve.ap.ui.viewmodel.AuthViewModel
import com.civicresolve.ap.ui.viewmodel.DashboardViewModel
import com.civicresolve.ap.ui.viewmodel.DashboardViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

@Composable
fun DashboardScreen(
    appContainer: AppContainer,
    authViewModel: AuthViewModel,
    onNavigateExplore: () -> Unit,
    onNavigateMap: () -> Unit,
    onOpenComplaint: (String) -> Unit,
    onOpenChat: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val authState by authViewModel.authState.collectAsState()
    val dashboardViewModel: DashboardViewModel = viewModel(factory = DashboardViewModelFactory(appContainer.complaintRepository))
    val dashState by dashboardViewModel.state.collectAsState()
    var activeTab by remember { mutableStateOf("overview") }
    var collapsed by remember { mutableStateOf(false) }
    var showOnboarding by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(authState.user) {
        if (authState.user != null && authState.user?.homeDistrict.isNullOrBlank() == true) showOnboarding = true
    }
    LaunchedEffect(Unit) { dashboardViewModel.fetchStats() }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppHeader(
                brandLabel = "CivicResolve",
                brandInitial = "⬢",
                title = "Citizen ledger",
                subtitle = "Case tracking and submissions",
                actions = {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(authState.user?.fullName ?: "Citizen", style = MaterialTheme.typography.titleSmall)
                        Text("CIV-${String.format("%04d", dashState.stats.total)} • Ledger", fontFamily = MonoFontFamily, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
            Row(modifier = Modifier.weight(1f)) {
                if (!isCompactScreen()) {
                    Sidebar(
                        activeTab = activeTab,
                        onTabSelected = { activeTab = it },
                        onExplore = onNavigateExplore,
                        onMap = onNavigateMap,
                        onLogout = onLogout,
                        collapsed = collapsed,
                        onToggle = { collapsed = !collapsed }
                    )
                }
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (activeTab) {
                        "overview" -> OverviewPane(
                            stats = dashState.stats,
                            authName = authState.user?.fullName,
                            onNewComplaint = { activeTab = "new_complaint" },
                            onExplore = onNavigateExplore,
                            complaints = dashState.complaints,
                            dashboardViewModel = dashboardViewModel,
                            onOpenComplaint = onOpenComplaint
                        )
                        "new_complaint" -> NewComplaintPane(
                            appContainer = appContainer,
                            dashboardViewModel = dashboardViewModel,
                            onDone = { activeTab = "overview"; dashboardViewModel.fetchStats() }
                        )
                        "chats" -> UserChatsPane(appContainer = appContainer, onOpenChat = onOpenChat)
                        "profile" -> ProfilePane(appContainer = appContainer, authViewModel = authViewModel)
                        else -> OverviewPane(stats = dashState.stats, authName = authState.user?.fullName, onNewComplaint = { activeTab = "new_complaint" }, onExplore = onNavigateExplore, complaints = dashState.complaints, dashboardViewModel = dashboardViewModel, onOpenComplaint = onOpenComplaint)
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { activeTab = "new_complaint" },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = if (isCompactScreen()) 72.dp else 16.dp),
            containerColor = MaterialTheme.colorScheme.secondary
        ) {
            Icon(Icons.Outlined.AddLocation, null)
        }
        if (isCompactScreen()) {
            CitizenBottomNav(
                activeTab = activeTab,
                onTabSelected = { activeTab = it },
                onExplore = onNavigateExplore,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
        if (showOnboarding) {
            var locLoading by remember { mutableStateOf(false) }
            var msg by remember { mutableStateOf<String?>(null) }
            var detectedCity by remember { mutableStateOf<String?>(null) }
            val context = LocalContext.current
            fun hasLocationPermission(): Boolean =
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

            fun detectLocation() {
                locLoading = true
                val fused = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
                try {
                    fused.lastLocation.addOnSuccessListener { loc ->
                        if (loc != null) {
                            scope.launch {
                                val res = com.civicresolve.ap.ui.utils.reverseGeocodeNominatim(loc.latitude, loc.longitude)
                                detectedCity = res?.city
                                msg = if (res?.city != null) "Detected ${res.city} — confirm below" else "Detected location, select manually"
                                locLoading = false
                            }
                        } else { msg = "Location unavailable"; locLoading = false }
                    }.addOnFailureListener { msg = "Location denied"; locLoading = false }
                } catch (_: SecurityException) { msg = "Permission denied"; locLoading = false }
            }

            val locPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { grants ->
                if (grants.values.any { it }) detectLocation()
                else { msg = "Location permission denied"; locLoading = false }
            }

            OnboardingDistrictDialog(
                currentDistrict = authState.user?.homeDistrict,
                isLoading = authState.isLoading,
                message = msg ?: authState.error,
                locLoading = locLoading,
                detectedDistrict = detectedCity,
                onDetectLocation = {
                    if (hasLocationPermission()) detectLocation()
                    else locPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                onSave = { district ->
                    authViewModel.completeOnboarding(district) { showOnboarding = false; dashboardViewModel.fetchStats() }
                }
            )
        }
    }
}

@Composable
private fun isCompactScreen(): Boolean {
    val cfg = LocalConfiguration.current
    return cfg.screenWidthDp < 700
}

@Composable
fun OverviewPane(
    stats: com.civicresolve.ap.ui.viewmodel.DashboardStats,
    authName: String?,
    onNewComplaint: () -> Unit,
    onExplore: () -> Unit,
    complaints: List<Complaint>,
    dashboardViewModel: DashboardViewModel,
    onOpenComplaint: (String) -> Unit
) {
    val active = stats.newComplaint + stats.inProgressComplaint
    HeroBlueprint {
        Text("Ledger for ${authName?.split(" ")?.firstOrNull() ?: "Citizen"} • CIV-${String.format("%04d", stats.total)}", fontFamily = MonoFontFamily, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
        Text("Welcome back, ${authName?.split(" ")?.firstOrNull() ?: "Citizen"}", style = MaterialTheme.typography.headlineSmall)
        Text("You have $active active ${if (active == 1) "report" else "reports"} on the district ledger. ${if (active > 0) "Follow their dots on the map." else "Your first dot is one report away."}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
        Row(modifier = Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onNewComplaint) { Text("Report") }
            OutlinedButton(onClick = onExplore) { Text("Explore") }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        listOf(
            "Total reports" to stats.total.toString(),
            "New" to stats.newComplaint.toString(),
            "In progress" to stats.inProgressComplaint.toString(),
            "Resolved" to stats.resolvedComplaint.toString()
        ).forEach { (label, value) ->
            UiCard(modifier = Modifier.weight(1f)) {
                Text(label, fontFamily = MonoFontFamily, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Black)
            }
        }
    }
    MyComplaintsList(complaints = complaints, dashboardViewModel = dashboardViewModel, onOpenComplaint = onOpenComplaint, onNewComplaint = onNewComplaint)
}

@Composable
fun MyComplaintsList(
    complaints: List<Complaint>,
    dashboardViewModel: DashboardViewModel,
    onOpenComplaint: (String) -> Unit,
    onNewComplaint: () -> Unit
) {
    var filter by remember { mutableStateOf("all") }
    var page by remember { mutableStateOf(1) }
    val state by dashboardViewModel.state.collectAsState()
    LaunchedEffect(filter, page) { dashboardViewModel.fetchMyComplaints(page, 10, filter) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("My ledger • ${state.complaints.size} entries", fontFamily = MonoFontFamily, fontSize = 10.sp)
            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { expanded = true }) { Text(filter, fontSize = 11.sp) }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    listOf("all", "new", "in_progress", "resolved").forEach { f ->
                        DropdownMenuItem(text = { Text(if (f == "all") f else statusLabel(f)) }, onClick = { filter = f; page = 1; expanded = false })
                    }
                }
            }
        }
        if (state.complaintsLoading) {
            Text("Indexing ledger…", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
        } else if (state.complaints.isEmpty()) {
            UiCard(modifier = Modifier.fillMaxWidth()) {
                Text("No dots yet", style = MaterialTheme.typography.titleMedium)
                Text("Your first report will appear as a red dot on the district map. It takes 30 seconds.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onNewComplaint, modifier = Modifier.padding(top = 8.dp)) { Text("Create first report") }
            }
        } else {
            state.complaints.forEach { c ->
                ComplaintCard(complaint = c, onClick = { onOpenComplaint(c.id) })
            }
            if ((state.pagination?.totalPages ?: 1) > 1) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { if (page > 1) page-- }, enabled = page > 1) { Text("Previous") }
                    Text("Page $page of ${state.pagination?.totalPages}", modifier = Modifier.padding(horizontal = 12.dp), fontSize = 12.sp)
                    OutlinedButton(onClick = { if (page < (state.pagination?.totalPages ?: 1)) page++ }, enabled = page < (state.pagination?.totalPages ?: 1)) { Text("Next") }
                }
            }
        }
    }
}

@Composable
fun NewComplaintPane(
    appContainer: AppContainer,
    dashboardViewModel: DashboardViewModel,
    onDone: () -> Unit
) {
    var description by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var stateVal by remember { mutableStateOf("") }
    var landmark by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var showNearby by remember { mutableStateOf<List<com.civicresolve.ap.data.model.NearbyComplaint>?>(null) }
    var pendingSubmit by remember { mutableStateOf<(() -> Unit)?>(null) }
    val context = LocalContext.current
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> imageUri = uri }
    val scope = rememberCoroutineScope()

    LaunchedEffect(latitude, longitude) {
        val lat = latitude.toDoubleOrNull(); val lng = longitude.toDoubleOrNull()
        if (lat != null && lng != null) {
            delay(800)
            val r = com.civicresolve.ap.ui.utils.reverseGeocodeNominatim(lat, lng)
            if (r != null) {
                if (city.isBlank() && r.city.isNotBlank()) city = r.city
                if (stateVal.isBlank() && r.state.isNotBlank()) stateVal = r.state
                if (landmark.isBlank() && r.landmark.isNotBlank()) landmark = r.landmark
            }
        }
    }

    fun fetchLocation() {
        try {
            val fused = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
            message = "Fetching location..."
            fused.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    latitude = String.format("%.6f", loc.latitude)
                    longitude = String.format("%.6f", loc.longitude)
                    message = "Location fetched. Drag map to adjust pin."
                } else message = "Unable to fetch location"
            }.addOnFailureListener { message = "Unable to fetch location" }
        } catch (_: SecurityException) { message = "Permission denied" }
    }

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    val locPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) fetchLocation()
        else message = "Location permission denied"
    }

    fun getLocation() {
        if (hasLocationPermission()) fetchLocation()
        else locPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    suspend fun doSubmit() {
        loading = true
        message = null
        try {
            val descBody = description.toRequestBody("text/plain".toMediaType())
            val latBody = latitude.toRequestBody("text/plain".toMediaType())
            val lngBody = longitude.toRequestBody("text/plain".toMediaType())
            val cityBody = city.toRequestBody("text/plain".toMediaType())
            val stateBody = stateVal.toRequestBody("text/plain".toMediaType())
            val landmarkBody = landmark.toRequestBody("text/plain".toMediaType())
            var part: MultipartBody.Part? = null
            imageUri?.let { uri ->
                // Reading a multi-MB photo on the main thread janks/ANRs
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } ?: return@let
                val req = bytes.toRequestBody("image/*".toMediaType())
                part = MultipartBody.Part.createFormData("imageUrl", "image.jpg", req)
            }
            val r = appContainer.complaintRepository.createComplaint(descBody, latBody, lngBody, cityBody, stateBody, landmarkBody, null, part)
            r.onSuccess { res ->
                if (res.success) {
                    message = "Complaint submitted successfully."
                    isSuccess = true
                    description = ""; city = ""; stateVal = ""; landmark = ""; latitude = ""; longitude = ""; imageUri = null
                    dashboardViewModel.fetchStats()
                    scope.launch { delay(1000); onDone() }
                } else { message = res.message ?: "Submission failed" }
            }.onFailure { message = it.message ?: "Submission failed" }
        } finally { loading = false }
    }

    UiCard(modifier = Modifier.fillMaxWidth()) {
        Text("New complaint", style = MaterialTheme.typography.headlineSmall)
        Text("Provide accurate details for faster resolution.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (message != null) {
            Text(message!!, color = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, placeholder = { Text("Describe the issue clearly.") }, modifier = Modifier.fillMaxWidth(), minLines = 4, shape = RoundedCornerShape(10.dp))
        Spacer(Modifier.height(12.dp))
        UiCardMuted {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Drag map to adjust pin, or use current location.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = { getLocation() }) { Text("Use current location", fontSize = 11.sp) }
            }
            if (latitude.isNotBlank() && longitude.isNotBlank()) {
                Text("Coordinates: $latitude, $longitude", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                if (city.isNotBlank() || stateVal.isNotBlank()) Text("Address: ${listOf(landmark, city, stateVal).filter { it.isNotBlank() }.joinToString(", ")}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(8.dp))
        MapPicker(latitude = latitude, longitude = longitude, onChange = { lat, lng -> latitude = lat; longitude = lng })
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(value = city, onValueChange = { city = it }, placeholder = { Text("City") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(10.dp))
            OutlinedTextField(value = stateVal, onValueChange = { stateVal = it }, placeholder = { Text("State") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(10.dp))
            OutlinedTextField(value = landmark, onValueChange = { landmark = it }, placeholder = { Text("Landmark") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(10.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text("Evidence image", fontFamily = MonoFontFamily, fontSize = 10.sp)
        Spacer(Modifier.height(4.dp))
        OutlinedButton(onClick = { pickImage.launch("image/*") }, modifier = Modifier.fillMaxWidth()) { Text(if (imageUri != null) "Change image" else "Pick image") }
        if (imageUri != null) Text("Selected: ${imageUri?.lastPathSegment}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                if (latitude.isBlank() || longitude.isBlank()) { message = "Please fetch your location or drag map to set pin."; return@Button }
                scope.launch {
                    val lat = latitude.toDoubleOrNull() ?: 0.0; val lng = longitude.toDoubleOrNull() ?: 0.0
                    val nearby = appContainer.complaintRepository.getNearbyComplaints(lat, lng, 500)
                    nearby.onSuccess { res ->
                        if (!res.complaints.isNullOrEmpty()) {
                            showNearby = res.complaints
                            pendingSubmit = { scope.launch { doSubmit() } }
                        } else {
                            doSubmit()
                        }
                    }.onFailure { doSubmit() }
                }
            }, enabled = !loading) { Text(if (loading) "Submitting..." else "Submit complaint") }
            OutlinedButton(onClick = onDone) { Text("Cancel") }
        }
    }
    if (showNearby != null) {
        AlertDialog(
            onDismissRequest = { showNearby = null },
            title = { Text("Nearby issues found (${showNearby!!.size} within 500m)", fontSize = 14.sp) },
            text = {
                Column {
                    Text("These issues are very close to your pin. Check if yours is duplicate.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    showNearby!!.take(3).forEach { r ->
                        Text("${r.category.replace("_", " ")} • ${r.status} • ${r.distanceMeters}m away", fontSize = 11.sp)
                        Text(r.description.take(80), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                    }
                }
            },
            confirmButton = {
                Button(onClick = { val p = pendingSubmit; showNearby = null; pendingSubmit = null; p?.invoke() }) { Text("Submit anyway") }
            },
            dismissButton = { TextButton(onClick = { showNearby = null; pendingSubmit = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun UserChatsPane(appContainer: AppContainer, onOpenChat: (String) -> Unit) {
    var chats by remember { mutableStateOf<List<Complaint>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var filter by remember { mutableStateOf("all") }
    var page by remember { mutableStateOf(1) }
    var totalPages by remember { mutableStateOf(1) }
    LaunchedEffect(filter, page) {
        loading = true
        val r = appContainer.complaintRepository.getMyActiveChats(page, 10, if (filter == "all") null else filter)
        r.onSuccess { res -> chats = res.complaints ?: emptyList(); totalPages = res.pagination?.totalPages ?: 1; loading = false }
            .onFailure { loading = false }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Complaint chats", style = MaterialTheme.typography.titleSmall)
            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { expanded = true }) { Text(filter, fontSize = 11.sp) }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    listOf("all", "new", "in_progress", "resolved").forEach { f ->
                        DropdownMenuItem(text = { Text(if (f == "all") f else statusLabel(f)) }, onClick = { filter = f; page = 1; expanded = false })
                    }
                }
            }
        }
        if (loading) Text("Loading chats...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        else if (chats.isEmpty()) {
            UiCard { Text("No active chats found.", style = MaterialTheme.typography.bodySmall); Button(onClick = {}, modifier = Modifier.padding(top = 8.dp)) { Text("Create complaint") } }
        } else {
            chats.forEach { c ->
                UiCard(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("#${c.id.takeLast(8).uppercase()}", style = MaterialTheme.typography.titleSmall)
                            Text("${c.createdAt?.take(10) ?: ""} • ${c.description.take(60)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        StatusBadge(c.status)
                    }
                    TextButton(onClick = { onOpenChat(c.id) }) { Text("Open chat") }
                }
            }
            if (totalPages > 1) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    OutlinedButton(onClick = { if (page > 1) page-- }, enabled = page > 1) { Text("Previous") }
                    Text("Page $page of $totalPages", modifier = Modifier.padding(horizontal = 12.dp).align(Alignment.CenterVertically))
                    OutlinedButton(onClick = { if (page < totalPages) page++ }, enabled = page < totalPages) { Text("Next") }
                }
            }
        }
    }
}

@Composable
fun ProfilePane(appContainer: AppContainer, authViewModel: AuthViewModel) {
    val authState by authViewModel.authState.collectAsState()
    var fullName by remember { mutableStateOf(authState.user?.fullName ?: "") }
    var address by remember { mutableStateOf(authState.user?.address ?: "") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> imageUri = uri }
    var themeMode by remember { mutableStateOf(com.civicresolve.ap.ui.theme.ThemeMode.SYSTEM) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(authState.user) { fullName = authState.user?.fullName ?: ""; address = authState.user?.address ?: "" }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        UiCard {
            Text("Profile", style = MaterialTheme.typography.headlineSmall)
            Text("Keep your personal information up to date.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (message != null) Text(message!!, color = if (isSuccess) Color(0xFF0E9F6E) else MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    Text(authState.user?.fullName?.firstOrNull()?.uppercase() ?: "C", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.width(12.dp))
                OutlinedButton(onClick = { pickImage.launch("image/*") }) { Text("Change photo") }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth(), minLines = 3, shape = RoundedCornerShape(10.dp))
            Spacer(Modifier.height(10.dp))
            Text("Theme", fontFamily = MonoFontFamily, fontSize = 10.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("System" to com.civicresolve.ap.ui.theme.ThemeMode.SYSTEM, "Light" to com.civicresolve.ap.ui.theme.ThemeMode.LIGHT, "Dark" to com.civicresolve.ap.ui.theme.ThemeMode.DARK).forEach { (label, mode) ->
                    FilterChip(selected = themeMode == mode, onClick = {
                        themeMode = mode
                        scope.launch { appContainer.themePreferences.setThemeMode(mode) }
                    }, label = { Text(label, fontSize = 11.sp) })
                }
            }
            Spacer(Modifier.height(10.dp))
            Button(onClick = {
                scope.launch {
                    try {
                        val fullNameBody = fullName.toRequestBody("text/plain".toMediaType())
                        val addressBody = address.toRequestBody("text/plain".toMediaType())
                        var part: MultipartBody.Part? = null
                        imageUri?.let { uri ->
                            // Reading a multi-MB photo on the main thread janks/ANRs
                            val bytes = withContext(Dispatchers.IO) {
                                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            }
                            if (bytes != null) {
                                val req = bytes.toRequestBody("image/*".toMediaType())
                                part = MultipartBody.Part.createFormData("profilePic", "profile.jpg", req)
                            }
                        }
                        val r = appContainer.authRepository.updateProfile(fullNameBody, addressBody, part)
                        r.onSuccess { res ->
                            if (res.success) { message = "Profile updated."; isSuccess = true } else { message = res.message; isSuccess = false }
                        }.onFailure { message = it.message; isSuccess = false }
                    } catch (e: Exception) { message = e.message; isSuccess = false }
                }
            }, enabled = !authState.isLoading) { Text(if (authState.isLoading) "Updating..." else "Update profile") }
            Spacer(Modifier.height(8.dp))
            Text("Home district: ${authState.user?.homeDistrict ?: "Not set"}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
