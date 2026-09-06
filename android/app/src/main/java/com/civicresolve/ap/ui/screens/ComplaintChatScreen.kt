package com.civicresolve.ap.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.civicresolve.ap.di.AppContainer
import com.civicresolve.ap.ui.components.AppHeader
import com.civicresolve.ap.ui.components.ChatBox
import com.civicresolve.ap.ui.viewmodel.AuthViewModel
import com.civicresolve.ap.ui.viewmodel.ChatViewModel
import com.civicresolve.ap.ui.viewmodel.ChatViewModelFactory
import com.civicresolve.ap.ui.viewmodel.ComplaintDetailViewModel
import com.civicresolve.ap.ui.viewmodel.ComplaintDetailViewModelFactory

@Composable
fun ComplaintChatScreen(
    complaintId: String,
    appContainer: AppContainer,
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val authState by authViewModel.authState.collectAsState()
    val detailVm: ComplaintDetailViewModel = viewModel(factory = ComplaintDetailViewModelFactory(appContainer.complaintRepository))
    val chatVm: ChatViewModel = viewModel(factory = ChatViewModelFactory(appContainer.complaintRepository, appContainer))
    val detailState by detailVm.state.collectAsState()
    val chatState by chatVm.state.collectAsState()
    var toUserId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(complaintId) {
        detailVm.fetchComplaint(complaintId)
        chatVm.loadMessages(complaintId)
    }
    LaunchedEffect(detailState.complaint, authState.user) {
        val c = detailState.complaint
        val user = authState.user
        if (c != null && user != null) {
            toUserId = c.user?.id
            chatVm.connect(complaintId, user.id ?: "")
        }
    }
    DisposableEffect(complaintId) { onDispose { chatVm.disconnect() } }

    Column(modifier = modifier.fillMaxSize()) {
        AppHeader(
            brandLabel = "CivicResolve",
            brandInitial = "C",
            title = "Complaint chat",
            subtitle = "ID #${complaintId.takeLast(8).uppercase()}",
            actions = {
                OutlinedButton(onClick = onBack) { Text("Dashboard", fontSize = 11.sp) }
                Spacer(Modifier.width(6.dp))
                Button(onClick = onDetails) { Text("Details", fontSize = 11.sp) }
            }
        )
        if (chatState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
        } else {
            ChatBox(
                messages = chatState.messages,
                currentUserId = authState.user?.id,
                chattingWith = if (authState.user?.isAdmin == true) detailState.complaint?.user?.fullName ?: "Citizen" else "Admin",
                onSendMessage = { msg ->
                    val to = toUserId ?: detailState.complaint?.user?.id ?: return@ChatBox
                    chatVm.sendMessage(complaintId, to, msg)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
