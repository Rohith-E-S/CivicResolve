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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.graphics.Color
import com.example.complaintportal.ui.viewmodel.ComplaintViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun UserDashboardScreen(
    viewModel: ComplaintViewModel,
    userName: String,
    district: String? = null,
    onNavigateToCreate: () -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val displayUserName = if (userName.isBlank() || userName == "User") "Himanshu Singh" else userName
    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(SortOption.DATE_DESC) }
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: My Reports, 1: Community Hub
    var communityTabScope by remember { mutableIntStateOf(0) } // 0: My District, 1: Global Feed
    
    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val onRefresh = {
        if (selectedTab == 1) {
            val scope = if (communityTabScope == 0) (district ?: "all") else "all"
            viewModel.fetchCommunityFeed(scope)
            viewModel.fetchPublicStats(scope)
        } else {
            viewModel.fetchUserComplaints()
            viewModel.fetchPublicStats()
        }
    }

    LaunchedEffect(selectedTab, communityTabScope) {
        if (selectedTab == 1) {
            val scope = if (communityTabScope == 0) (district ?: "all") else "all"
            viewModel.fetchCommunityFeed(scope)
            viewModel.fetchPublicStats(scope)
        } else {
            viewModel.fetchUserComplaints() // Added this
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
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { onNavigateToDetail("profile") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayUserName.firstOrNull()?.toString()?.uppercase() ?: "U",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
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
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Complaint")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            
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
                    Text(
                        text = "Connected to $district Civic Portal",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
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
                        
                        Text(
                            text = String.format("%,d issues %s", resolvedCount, scopeLabel), 
                            style = MaterialTheme.typography.titleSmall, 
                            fontWeight = FontWeight.Bold, 
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("Your community is improving.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = "Analytics", tint = MaterialTheme.colorScheme.primary.copy(alpha=0.4f))
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
                            status == "all" || it.status.lowercase() == status
                        }
                    }

                    @OptIn(ExperimentalMaterial3Api::class)
                    val pullToRefreshState = rememberPullToRefreshState()
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (selectedTab == 1) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = communityTabScope == 0,
                                    onClick = { communityTabScope = 0 },
                                    label = { Text(if (district != null) "My District" else "My District (not set)") },
                                    leadingIcon = if (communityTabScope == 0) {
                                        { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    shape = CircleShape
                                )
                                FilterChip(
                                    selected = communityTabScope == 1,
                                    onClick = { communityTabScope = 1 },
                                    label = { Text("Global Feed") },
                                    leadingIcon = if (communityTabScope == 1) {
                                        { Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    shape = CircleShape
                                )
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
                                            Text("No Complaints Found", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                            Text(
                                                "Reporting a pothole or streetlight helps the community. Get started by reporting your first issue!", 
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
                                                showCommunityFeatures = selectedTab == 1,
                                                isSupported = state.supportedIds.contains(complaint.id),
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
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
