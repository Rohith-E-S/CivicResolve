package com.example.complaintportal.ui.screens.user

import com.example.complaintportal.ui.screens.*
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
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

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun UserDashboardScreen(
    viewModel: ComplaintViewModel,
    userName: String,
    userId: String,
    district: String? = null,
    notificationViewModel: NotificationViewModel? = null,
    onNavigateToCreate: () -> Unit,
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val displayUserName = if (userName.isBlank() || userName == "User") "Himanshu Singh" else userName
    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(SortOption.DATE_DESC) }
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: My Reports, 1: Community Hub
    var communityTabScope by remember { mutableIntStateOf(0) } // 0: My District, 1: Global Feed
    var isMapView by remember { mutableStateOf(false) }
    
    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val onRefresh = {
        if (selectedTab == 1) {
            val scope = if (communityTabScope == 0) (district ?: "all") else "all"
            viewModel.fetchCommunityFeed(scope, userId)
            viewModel.fetchPublicStats(scope)
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
            viewModel.fetchUserComplaints(userId) // Added this
            viewModel.fetchPublicStats()
        }
    }


    Scaffold(
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
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
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
                Icon(Icons.Default.Add, contentDescription = "Create Complaint", modifier = Modifier.size(28.dp))
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
                    kotlinx.coroutines.delay(15_000L)
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
                    Text("Hi, ", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(displayUserName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    Text(" 👋", style = MaterialTheme.typography.headlineSmall)
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

            
                Card(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clickable { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedTab = 1 // Switch to Community Hub
                            coroutineScope.launch { pagerState.animateScrollToPage(2) } // Switch to Resolved
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            val resolvedCount = if (selectedTab == 1) state.communityResolvedCount else state.resolvedComplaints.size
                            val scopeLabel = if (selectedTab == 1) {
                                if (communityTabScope == 0 && district != null) "in ${district.split(" ").first()}" else "nationwide"
                            } else "you resolved"
                            
                            val bannerTitle = when (resolvedCount) {
                                0 -> "Report your first issue to get started!"
                                1 -> "1 issue resolved. Great start!"
                                else -> String.format("%,d issues %s 🎉", resolvedCount, scopeLabel)
                            }

                            Text(
                                text = bannerTitle, 
                                style = MaterialTheme.typography.titleSmall, 
                                fontWeight = FontWeight.Bold, 
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text("Your community is improving.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = "Analytics", tint = MaterialTheme.colorScheme.primary.copy(alpha=0.4f))
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
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
                                contentDescription = "Search",
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

                IconButton(
                    onClick = { showSortMenu = true },
                    modifier = Modifier
                        .size(46.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Sort",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Newest First") },
                            onClick = { sortOption = SortOption.DATE_DESC; showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Oldest First") },
                            onClick = { sortOption = SortOption.DATE_ASC; showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Highest Rated") },
                            onClick = { sortOption = SortOption.RATING_DESC; showSortMenu = false }
                        )
                    }
                }

                IconButton(
                    onClick = { isMapView = !isMapView },
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            if (isMapView) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isMapView) Icons.Default.FormatListBulleted else Icons.Default.Map,
                        contentDescription = if (isMapView) "Switch to List" else "Switch to Map",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // The Toggle: [ My Reports | Community Hub ]
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
                    text = { Text("My Reports", style = MaterialTheme.typography.titleSmall, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedTab = 1 
                    },
                    text = { Text("Community Hub", style = MaterialTheme.typography.titleSmall, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium) }
                )
            }

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
                val newCount = if (selectedTab == 0) state.newComplaints.size else state.communityComplaints.count { it.status.lowercase() == "new" }
                val activeCount = if (selectedTab == 0) state.inProgressComplaints.size else state.communityComplaints.count { it.status.lowercase() == "in progress" }
                val resolvedCount = if (selectedTab == 0) state.resolvedComplaints.size else state.communityComplaints.count { it.status.lowercase() == "resolved" }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp, top = 8.dp)
                ) {
                    item {
                        StatCard(
                            title = "New",
                            count = newCount.toString(),
                            color = Color(0xFFE57373),
                            isSelected = pagerState.currentPage == 0,
                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } }
                        )
                    }
                    item {
                        StatCard(
                            title = "Active",
                            count = activeCount.toString(),
                            color = Color(0xFFFFB74D),
                            isSelected = pagerState.currentPage == 1,
                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } }
                        )
                    }
                    item {
                        StatCard(
                            title = "Resolved",
                            count = resolvedCount.toString(),
                            color = Color(0xFF81C784),
                            isSelected = pagerState.currentPage == 2,
                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(2) } }
                        )
                    }
                    item {
                        StatCard(
                            title = "Analytics",
                            count = "Charts",
                            color = MaterialTheme.colorScheme.outline,
                            isSelected = pagerState.currentPage == 3,
                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(3) } }
                        )
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    if (page == 3) {
                        val allComplaints = state.newComplaints + state.inProgressComplaints + state.resolvedComplaints
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Complaints by Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            StatusBarChart(state.newComplaints.size, state.inProgressComplaints.size, state.resolvedComplaints.size)
                            
                            Text("Complaints by Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (allComplaints.isNotEmpty()) {
                                CategoryPieChart(allComplaints)
                            } else {
                                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                    Text("No data for pie chart", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    } else {
                        val list = when (page) {
                            0 -> state.newComplaints
                            1 -> state.inProgressComplaints
                            2 -> state.resolvedComplaints
                            else -> emptyList()
                        }
                        
                        val displayList = if (selectedTab == 0) list else {
                            val status = when (page) {
                                0 -> "new"
                                1 -> "in progress"
                                2 -> "resolved"
                                else -> "all"
                            }
                            state.communityComplaints.filter { 
                                status == "all" || it.status.trim().lowercase() == status.lowercase()
                            }
                        }

                        @OptIn(ExperimentalMaterial3Api::class)
                        val pullToRefreshState = rememberPullToRefreshState()
                        Column(modifier = Modifier.fillMaxSize()) {
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
                                        val inactiveColor = Color.Transparent
                                        val activeColor = MaterialTheme.colorScheme.surface

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (communityTabScope == 0) activeColor else inactiveColor)
                                                .clickable { communityTabScope = 0 },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.LocationOn, 
                                                    contentDescription = null, 
                                                    modifier = Modifier.size(16.dp),
                                                    tint = if (communityTabScope == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    if (district != null) "My District" else "Local", 
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = if (communityTabScope == 0) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (communityTabScope == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (communityTabScope == 1) activeColor else inactiveColor)
                                                .clickable { communityTabScope = 1 },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.Public, 
                                                    contentDescription = null, 
                                                    modifier = Modifier.size(16.dp),
                                                    tint = if (communityTabScope == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    "Global Feed", 
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = if (communityTabScope == 1) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (communityTabScope == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }
                                    }
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
                                val filteredList = if (searchQuery.isBlank()) {
                                    displayList
                                } else {
                                    displayList.filter {
                                        it.category.contains(searchQuery, ignoreCase = true) ||
                                        it.city.contains(searchQuery, ignoreCase = true) ||
                                        it.description.contains(searchQuery, ignoreCase = true)
                                    }
                                }.let { unsorted ->
                                    when (sortOption) {
                                        SortOption.DATE_DESC -> unsorted.sortedByDescending { it.createdAt }
                                        SortOption.DATE_ASC -> unsorted.sortedBy { it.createdAt }
                                        SortOption.RATING_DESC -> unsorted.sortedByDescending { it.rating }
                                    }
                                }

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
                                                Box(
                                                    modifier = Modifier.size(120.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha=0.2f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                                                }
                                                Spacer(modifier = Modifier.height(24.dp))
                                                Text("You're all caught up!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                                                    Text("Report an Issue")
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
                                                            viewModel.supportComplaint(complaint.id) {
                                                                // Optional: refresh specific lists or just rely on state sync
                                                            }
                                                        }
                                                    }
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
        }
    }
}
