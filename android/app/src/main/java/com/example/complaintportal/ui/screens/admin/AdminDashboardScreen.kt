package com.example.complaintportal.ui.screens.admin

import com.example.complaintportal.ui.screens.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.foundation.text.BasicTextField
import com.example.complaintportal.ui.viewmodel.ComplaintViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: ComplaintViewModel,
    userId: String,
    onNavigateToDetail: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var isRefreshing by remember { mutableStateOf(false) }
    var sortOption by remember { mutableStateOf(SortOption.DATE_DESC) }
    var showSortMenu by remember { mutableStateOf(false) }
    var isMapView by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()

    val onRefresh = {
        isRefreshing = true
        viewModel.fetchAdminComplaints(userId)
    }

    LaunchedEffect(state.isLoading) {
        if (!state.isLoading) {
            isRefreshing = false
        }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchAdminComplaints(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CivicResolve Admin", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primaryContainer) },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(androidx.compose.ui.graphics.Color(0xFF1A3A6E))
                            .clickable { onNavigateToDetail("profile") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "A",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Admin Console,", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Overview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                }

                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search...",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                innerTextField()
                            }
                        }
                    }
                )

                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
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

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                item {
                    StatCard(
                        title = "New Actions",
                        count = state.newComplaints.size.toString(),
                        color = MaterialTheme.colorScheme.primary,
                        isSelected = pagerState.currentPage == 0,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } }
                    )
                }
                item {
                    StatCard(
                        title = "Active Processing",
                        count = state.inProgressComplaints.size.toString(),
                        color = MaterialTheme.colorScheme.tertiary,
                        isSelected = pagerState.currentPage == 1,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } }
                    )
                }
                item {
                    StatCard(
                        title = "Resolved By Team",
                        count = state.resolvedComplaints.size.toString(),
                        color = MaterialTheme.colorScheme.secondary,
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

            if (isMapView) {
                val mapBaseList = state.newComplaints + state.inProgressComplaints + state.resolvedComplaints
                
                val mapFilteredList = if (searchQuery.isBlank()) {
                    mapBaseList
                } else {
                    mapBaseList.filter {
                        it.category.contains(searchQuery, ignoreCase = true) ||
                        it.city.contains(searchQuery, ignoreCase = true) ||
                        it.description.contains(searchQuery, ignoreCase = true)
                    }
                }

                OsmDashboardMap(
                    complaints = mapFilteredList,
                    onComplaintClick = onNavigateToDetail,
                    scope = MapScope.MY_REPORTS,
                    modifier = Modifier.weight(1f)
                )
            } else {
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

                        val filteredList = if (searchQuery.isBlank()) {
                            list
                        } else {
                            list.filter {
                                it.category.contains(searchQuery, ignoreCase = true) ||
                                it.city.contains(searchQuery, ignoreCase = true) ||
                                it.description.contains(searchQuery, ignoreCase = true)
                            }
                        }.let {
                            when (sortOption) {
                                SortOption.DATE_DESC -> it.sortedByDescending { item -> item.createdAt }
                                SortOption.DATE_ASC -> it.sortedBy { item -> item.createdAt }
                                SortOption.RATING_DESC -> it.sortedByDescending { item -> item.rating }
                            }
                        }

                        @OptIn(ExperimentalMaterial3Api::class)
                        val pullToRefreshState = rememberPullToRefreshState()
                        PullToRefreshBox(
                            isRefreshing = isRefreshing,
                            onRefresh = onRefresh,
                            modifier = Modifier.fillMaxSize(),
                            state = pullToRefreshState,
                            indicator = {
                                CustomPullToRefreshIndicator(
                                    isRefreshing = isRefreshing,
                                    state = pullToRefreshState,
                                    modifier = Modifier.align(Alignment.TopCenter)
                                )
                            }
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                if (filteredList.isEmpty() && !state.isLoading) {
                                    item {
                                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("No pending issues found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                } else {
                                    itemsIndexed(items = filteredList, key = { _, item -> item.id }) { _, complaint ->
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 16.dp)
                                                .animateItem()
                                        ) {
                                            ComplaintCard(
                                                complaint = complaint,
                                                isAdmin = true,
                                                onClick = { onNavigateToDetail(complaint.id) },
                                                onUpdateStatusClick = { onNavigateToDetail(complaint.id) }
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