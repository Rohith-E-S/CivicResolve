package com.example.complaintportal.ui.screens.user

import com.example.complaintportal.ui.screens.*
import com.example.complaintportal.ui.screens.SortOption
import com.example.complaintportal.ui.screens.BeyondRadiusBanner
import com.example.complaintportal.ui.screens.haversineDistance
import com.example.complaintportal.data.model.Complaint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.graphics.Color
import com.example.complaintportal.ui.viewmodel.ComplaintViewModel
import com.example.complaintportal.ui.notification.NotificationBell
import com.example.complaintportal.ui.notification.NotificationViewModel
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.example.complaintportal.R
import android.Manifest
import com.example.complaintportal.ui.utils.detectLocation
import com.google.android.gms.location.LocationServices
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.complaintportal.ui.viewmodel.AuthViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
@Composable
fun UserDashboardScreen(
    viewModel: ComplaintViewModel,
    authViewModel: AuthViewModel,
    userName: String,
    userId: String,
    district: String? = null,
    notificationViewModel: NotificationViewModel? = null,
    onNavigateToCreate: () -> Unit,
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit,
    onNavigateToMap: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Location detection logic for header click
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var isUpdatingLocation by remember { mutableStateOf(false) }

    var userLat by rememberSaveable { mutableStateOf<Double?>(null) }
    var userLng by rememberSaveable { mutableStateOf<Double?>(null) }
    var showAllBeyond by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            scope.launch {
                isUpdatingLocation = true
                detectLocation(context, fusedLocationClient) { _, detectedDistrict, lat, lng ->
                    if (lat != null && lng != null) {
                        userLat = lat
                        userLng = lng
                    }
                    if (detectedDistrict != null) {
                        authViewModel.completeOnboarding(detectedDistrict) {
                            scope.launch {
                                isUpdatingLocation = false
                            }
                        }
                    } else {
                        scope.launch {
                            isUpdatingLocation = false
                        }
                    }
                }
            }
        } else {
            scope.launch {
                isUpdatingLocation = false
            }
        }
    }
    val displayUserName = if (userName.isBlank() || userName == "User") "Himanshu Singh" else userName
    var searchQuery by remember { mutableStateOf("") }
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedFilter by rememberSaveable { mutableStateOf("All") }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0: My Reports, 1: Community Hub
    var communityTabScope by rememberSaveable { mutableIntStateOf(0) } // 0: My District, 1: Global Feed
    var isMapView by rememberSaveable { mutableStateOf(false) }
    val NavyPrimary = Color(0xFF1A3A6E)
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val onRefresh = {
        val hasLocationPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
        androidx.core.content.ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasLocationPermission) {
            scope.launch {
                isUpdatingLocation = true
                detectLocation(context, fusedLocationClient) { _, detectedDistrict, lat, lng ->
                    if (lat != null && lng != null) {
                        userLat = lat
                        userLng = lng
                    }
                    if (detectedDistrict != null && detectedDistrict != district) {
                        authViewModel.completeOnboarding(detectedDistrict) {
                            scope.launch {
                                isUpdatingLocation = false
                            }
                        }
                    } else {
                        isUpdatingLocation = false
                    }
                }
            }
        }

        if (selectedTab == 1) {
            val feedScope = if (communityTabScope == 0) (district ?: "all") else "all"
            viewModel.fetchCommunityFeed(feedScope, userId)
            viewModel.fetchPublicStats(feedScope)
        } else {
            viewModel.fetchUserComplaints(userId)
            viewModel.fetchPublicStats()
        }
    }

    LaunchedEffect(selectedTab, communityTabScope) {
        if (selectedTab == 1) {
            val scope = if (communityTabScope == 0) (district ?: "all") else "all"
            viewModel.fetchCommunityFeed(scope, userId)
            viewModel.fetchPublicStats(scope)
        } else {
            // Reset sort if it was set to NEAREST (not supported in My Reports)
            if (state.sortOption == SortOption.NEAREST) {
                viewModel.updateSortOption(SortOption.DATE_DESC)
            }
            viewModel.fetchUserComplaints(userId) // Added this
            viewModel.fetchPublicStats()
        }
    }


    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "CivicResolve", 
                            fontWeight = FontWeight.ExtraBold, 
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        if (district != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.padding(top = 4.dp),
                                onClick = {
                                    if (!isUpdatingLocation) {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isUpdatingLocation) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(12.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = district.split(" ").first(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    // Notification bell (only for logged-in users)
                    val notifUiState = notificationViewModel?.uiState?.collectAsState()
                    val unreadCount = notifUiState?.value?.unreadCount ?: 0
                    NotificationBell(
                        unreadCount = unreadCount,
                        onClick     = onNavigateToNotifications,
                        modifier    = Modifier.padding(end = 4.dp),
                    )

                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1A3A6E))
                            .clickable { onNavigateToDetail("profile") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayUserName.split(" ").mapNotNull { it.firstOrNull() }.joinToString("").take(2).uppercase(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            val sharedTransitionScope = com.example.complaintportal.ui.navigation.LocalSharedTransitionScope.current
            val animatedVisibilityScope = com.example.complaintportal.ui.navigation.LocalNavAnimatedVisibilityScope.current
            
            var fabModifier: Modifier = Modifier
            if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                with(sharedTransitionScope) {
                    fabModifier = Modifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "fab_to_create"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        resizeMode = androidx.compose.animation.SharedTransitionScope.ResizeMode.ScaleToBounds()
                    )
                }
            }

            FloatingActionButton(
                onClick = onNavigateToCreate,
                modifier = fabModifier,
                containerColor = Color(0xFF1A3A6E),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_complaint), modifier = Modifier.size(28.dp))
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // Persistent banner logic
            val context = androidx.compose.ui.platform.LocalContext.current
            val prefs = remember { context.getSharedPreferences("dashboard_prefs", android.content.Context.MODE_PRIVATE) }
            var showBanner by remember {
                mutableStateOf(!prefs.getBoolean("banner_dismissed_${userId}", false))
            }

            // Auto-dismiss after 15 seconds
            LaunchedEffect(showBanner) {
                if (showBanner) {
                    kotlinx.coroutines.delay(8_000L)
                    prefs.edit().putBoolean("banner_dismissed_${userId}", true).apply()
                    showBanner = false
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = showBanner,
                enter   = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
                exit    = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically(),
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.hi), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(displayUserName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                        Text(stringResource(R.string.str), style = MaterialTheme.typography.headlineSmall)
                    }
                    if (district != null) {
                        val annotatedString = buildAnnotatedString {
                            append("Connected to ")
                            withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                                append(district)
                            }
                            append(" Civic Portal")
                        }
                        Text(
                            text = annotatedString,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), CircleShape),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = stringResource(R.string.search),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search issues...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    }
                )

                SortFilterDropdown(
                    selectedSort = state.sortOption,
                    activeTab = if (selectedTab == 1) 0 else 1,
                    onSortChanged = { viewModel.updateSortOption(it) }
                )

                IconButton(
                    onClick = { isMapView = !isMapView },
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            if (isMapView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isMapView) Icons.Default.List else Icons.Default.Map,
                        contentDescription = if (isMapView) "View List" else "View Map",
                        tint = if (isMapView) Color.White else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedTab = 0 
                    },
                    text = { Text(stringResource(R.string.my_reports), style = MaterialTheme.typography.titleSmall, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedTab = 1 
                    },
                    text = { Text(stringResource(R.string.community_hub), style = MaterialTheme.typography.titleSmall, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium) }
                )
            }

            val communityFiltered = state.communityComplaints.filter { it.user?.id != userId }
            
            if (isMapView) {
                val mapBaseList = if (selectedTab == 0) {
                    state.newComplaints + state.inProgressComplaints + state.resolvedComplaints
                } else {
                    state.communityComplaints
                }
                
                val mapFilteredList = if (searchQuery.isBlank()) {
                    mapBaseList
                } else {
                    mapBaseList.filter {
                        it.category.contains(searchQuery, ignoreCase = true) ||
                        it.city.contains(searchQuery, ignoreCase = true) ||
                        it.description.contains(searchQuery, ignoreCase = true)
                    }
                }

                val mapScope = if (selectedTab == 0) {
                    MapScope.MY_REPORTS
                } else if (communityTabScope == 0) {
                    MapScope.MY_DISTRICT
                } else {
                    MapScope.GLOBAL_FEED
                }

                OsmDashboardMap(
                    complaints = mapFilteredList,
                    onComplaintClick = onNavigateToDetail,
                    scope = mapScope,
                    modifier = Modifier.weight(1f)
                )
            } else {
                val complaints = if (selectedTab == 0) {
                    state.newComplaints + state.inProgressComplaints + state.resolvedComplaints
                } else {
                    state.communityComplaints
                }

                val filteredByStatus by remember(selectedFilter, complaints) {
                    derivedStateOf {
                        when (selectedFilter) {
                            "All"      -> complaints
                            "New"      -> complaints.filter { it.status.lowercase() == "new" || it.status.lowercase() == "under_review" }
                            "Active"   -> complaints.filter { it.status.lowercase() == "in_progress" }
                            "Resolved" -> complaints.filter { it.status.lowercase() == "resolved" }
                            else       -> complaints
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatusChip(
                        label      = "All",
                        count      = complaints.size,
                        color      = NavyPrimary,
                        isSelected = selectedFilter == "All",
                        onClick    = { selectedFilter = "All" },
                    )
                    StatusChip(
                        label      = "New",
                        count      = complaints.count { it.status.lowercase() == "new" || it.status.lowercase() == "under_review" },
                        color      = Color(0xFFE53935),
                        isSelected = selectedFilter == "New",
                        onClick    = { selectedFilter = "New" },
                    )
                    StatusChip(
                        label      = "Active",
                        count      = complaints.count { it.status.lowercase() == "in_progress" },
                        color      = Color(0xFFE67E22),
                        isSelected = selectedFilter == "Active",
                        onClick    = { selectedFilter = "Active" },
                    )
                    StatusChip(
                        label      = "Resolved",
                        count      = complaints.count { it.status.lowercase() == "resolved" },
                        color      = Color(0xFF1D9E75),
                        isSelected = selectedFilter == "Resolved",
                        onClick    = { selectedFilter = "Resolved" },
                    )
                }

                if (selectedTab == 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val activeColor = NavyPrimary
                        val inactiveColor = Color.Transparent

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (communityTabScope == 0) activeColor else inactiveColor)
                                .then(if (communityTabScope != 0) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)) else Modifier)
                                .clickable { communityTabScope = 0 },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.LocationOn, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(16.dp),
                                    tint = if (communityTabScope == 0) Color.White else MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (district != null) "My District" else "Local", 
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (communityTabScope == 0) FontWeight.Bold else FontWeight.Medium,
                                    color = if (communityTabScope == 0) Color.White else MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (communityTabScope == 1) activeColor else inactiveColor)
                                .then(if (communityTabScope != 1) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)) else Modifier)
                                .clickable { communityTabScope = 1 },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Language, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(16.dp),
                                    tint = if (communityTabScope == 1) Color.White else MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Global Feed", 
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (communityTabScope == 1) FontWeight.Bold else FontWeight.Medium,
                                    color = if (communityTabScope == 1) Color.White else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }

                @OptIn(ExperimentalMaterial3Api::class)
                val pullToRefreshState = rememberPullToRefreshState()
                
                LaunchedEffect(selectedTab, communityTabScope) {
                    showAllBeyond = false
                }

                PullToRefreshBox(
                    isRefreshing = state.isLoading || state.isCommunityLoading,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                    state = pullToRefreshState,
                    indicator = {
                        CustomPullToRefreshIndicator(
                            isRefreshing = state.isLoading || state.isCommunityLoading,
                            state = pullToRefreshState,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }
                ) {
                    val filteredList: List<Complaint> = if (searchQuery.isBlank()) {
                        filteredByStatus
                    } else {
                        filteredByStatus.filter {
                            it.category.contains(searchQuery, ignoreCase = true) ||
                            it.city.contains(searchQuery, ignoreCase = true) ||
                            it.description.contains(searchQuery, ignoreCase = true)
                        }
                    }.let { unsorted ->
                        when (state.sortOption) {
                            SortOption.DATE_DESC -> unsorted.sortedByDescending { it.createdAt }
                            SortOption.DATE_ASC -> unsorted.sortedBy { it.createdAt }
                            SortOption.RATING_DESC -> unsorted.sortedByDescending { it.rating }
                            SortOption.UPVOTES_DESC -> unsorted.sortedByDescending { it.supportCount ?: 0 }
                            SortOption.NEAREST -> {
                                val mapped: List<Pair<Complaint, Double>> = unsorted.map { complaint ->
                                    val lat = complaint.latitude.toDoubleOrNull() ?: 0.0
                                    val lng = complaint.longitude.toDoubleOrNull() ?: 0.0
                                    val uLat = userLat ?: 0.0
                                    val uLng = userLng ?: 0.0
                                    val dist = haversineDistance(uLat, uLng, lat, lng)
                                    Pair(complaint, dist)
                                }
                                val sorted = mapped.sortedBy { it.second }
                                if (showAllBeyond || userLat == null) {
                                    sorted.map { it.first }
                                } else {
                                    sorted.filter { it.second <= 5000.0 }.map { it.first }
                                }
                            }
                        }
                    }

                    val hiddenBeyondRadius = if (state.sortOption == SortOption.NEAREST && !showAllBeyond && userLat != null) {
                        filteredByStatus.count { complaint ->
                            val lat = complaint.latitude.toDoubleOrNull() ?: 0.0
                            val lng = complaint.longitude.toDoubleOrNull() ?: 0.0
                            haversineDistance(userLat ?: 0.0, userLng ?: 0.0, lat, lng) > 5000.0
                        }
                    } else 0

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (filteredList.isEmpty() && !state.isLoading && !state.isCommunityLoading) {
                            item {
                                Column(
                                    modifier = Modifier.fillParentMaxSize().padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    if (hiddenBeyondRadius > 0) {
                                        BeyondRadiusBanner(
                                            hiddenCount = hiddenBeyondRadius,
                                            onShowAll = { showAllBeyond = true }
                                        )
                                        Spacer(modifier = Modifier.height(24.dp))
                                    }
                                    Box(
                                        modifier = Modifier.size(120.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha=0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (selectedTab == 1) {
                                            Box(contentAlignment = Alignment.TopCenter) {
                                                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                                                Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(28.dp).padding(top=10.dp), tint = MaterialTheme.colorScheme.surface)
                                            }
                                        } else {
                                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text(stringResource(R.string.you_re_all_caught_up), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Everything looks good in your area! Tap + to report a new issue and help your community.", 
                                        style = MaterialTheme.typography.bodyMedium, 
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Button(
                                        onClick = onNavigateToCreate,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(stringResource(R.string.report_an_issue))
                                    }
                                }
                            }
                        } else {
                            itemsIndexed(items = filteredList, key = { _, item -> item.id }) { _, complaint ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                ) {
                                    ComplaintCard(
                                        complaint = complaint,
                                        isAdmin = false,
                                        onClick = { onNavigateToDetail(complaint.id) },
                                        onUpdateStatusClick = {},
                                        showCommunityFeatures = true,
                                        isSupported = state.supportedIds.contains(complaint.id),
                                        isOwner = complaint.user?.id == userId,
                                        onSupportClick = {
                                            if (selectedTab == 1) {
                                                viewModel.supportComplaint(complaint.id) {}
                                            }
                                        },
                                        distanceMeters = if (userLat != null && userLng != null) {
                                            val lat = complaint.latitude.toDoubleOrNull() ?: 0.0
                                            val lng = complaint.longitude.toDoubleOrNull() ?: 0.0
                                            haversineDistance(userLat!!, userLng!!, lat, lng)
                                        } else null,
                                        showDistance = selectedTab == 1 && district != null && (
                                            complaint.city.contains(district, ignoreCase = true) || 
                                            district.contains(complaint.city, ignoreCase = true)
                                        ),
                                        showYouReportedTag = selectedTab != 0,
                                        currentUserId = userId
                                    )
                                }
                            }

                            if (hiddenBeyondRadius > 0) {
                                item {
                                    BeyondRadiusBanner(
                                        hiddenCount = hiddenBeyondRadius,
                                        onShowAll = { showAllBeyond = true }
                                    )
                                }
                            }

                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp, horizontal = 32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "You're all caught up!",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Everything looks good in your area!\nTap + to report a new issue.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        lineHeight = 18.sp
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun StatusChip(
    label:      String,
    count:      Int,
    color:      Color,
    isSelected: Boolean,
    onClick:    () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) color.copy(alpha = 0.12f)
                else            Color(0xFFF2F4F8)
            )
            .border(
                width  = if (isSelected) 1.5.dp else 0.5.dp,
                color  = if (isSelected) color else Color(0xFFE8EDF5),
                shape  = RoundedCornerShape(20.dp),
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Dot indicator
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text       = "$label $count",
                fontSize   = 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color      = if (isSelected) color else Color(0xFF6A7F9A),
            )
        }
    }
}
