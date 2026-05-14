package com.example.complaintportal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.complaintportal.ui.viewmodel.ComplaintViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapViewScreen(
    viewModel: ComplaintViewModel,
    isAdmin: Boolean,
    userId: String,
    statusFilter: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    
    val baseComplaints = if (isAdmin) {
        state.newComplaints + state.inProgressComplaints + state.pendingVerificationComplaints + state.disputedComplaints + state.resolvedComplaints
    } else {
        (state.newComplaints + state.inProgressComplaints + state.pendingVerificationComplaints + state.disputedComplaints + state.resolvedComplaints + state.communityComplaints).distinctBy { it.id }
    }

    val complaints = if (statusFilter == null || statusFilter == "All") {
        baseComplaints
    } else {
        when (statusFilter) {
            "New" -> state.newComplaints
            "Active" -> state.inProgressComplaints + state.pendingVerificationComplaints + state.disputedComplaints
            "Resolved" -> state.resolvedComplaints
            else -> baseComplaints
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Issue Map") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        OsmDashboardMap(
            complaints = complaints,
            onComplaintClick = onNavigateToDetail,
            scope = if (isAdmin) MapScope.MY_REPORTS else MapScope.GLOBAL_FEED,
            modifier = Modifier.padding(padding).fillMaxSize()
        )
    }
}