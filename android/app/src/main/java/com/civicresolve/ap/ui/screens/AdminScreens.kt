package com.civicresolve.ap.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.civicresolve.ap.di.AppContainer
import com.civicresolve.ap.ui.components.*
import com.civicresolve.ap.ui.utils.statusLabel
import com.civicresolve.ap.ui.theme.DisplayFontFamily
import com.civicresolve.ap.ui.theme.MonoFontFamily
import com.civicresolve.ap.ui.viewmodel.AdminViewModel
import com.civicresolve.ap.ui.viewmodel.AdminViewModelFactory
import com.civicresolve.ap.ui.viewmodel.ComplaintDetailViewModel
import com.civicresolve.ap.ui.viewmodel.ComplaintDetailViewModelFactory

@Composable
fun AdminDashboardScreen(
    appContainer: AppContainer,
    onOpenComplaint: (String) -> Unit,
    onOpenChat: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf("overview") }
    Column(modifier = modifier.fillMaxSize()) {
        AppHeader(
            brandLabel = "CivicResolve",
            brandInitial = "A",
            title = "Admin dashboard",
            subtitle = "Review and resolve complaints",
            actions = { OutlinedButton(onClick = onLogout) { Text("Log out", fontSize = 11.sp) } }
        )
        Row(modifier = Modifier.weight(1f)) {
            if (!isCompactAdmin()) {
                AdminSidebar(activeTab = activeTab, onTabSelected = { activeTab = it }, onLogout = onLogout)
            }
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (activeTab) {
                    "overview" -> AdminOverviewPane(appContainer)
                    "complaints" -> AllComplaintsPane(appContainer, onOpenComplaint)
                    "chats" -> AdminChatsPane(appContainer, onOpenChat)
                }
            }
        }
        if (isCompactAdmin()) {
            AdminBottomNav(activeTab = activeTab, onTabSelected = { activeTab = it })
        }
    }
}

@Composable
private fun isCompactAdmin(): Boolean = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 700

@Composable
fun AdminOverviewPane(appContainer: AppContainer) {
    val vm: AdminViewModel = viewModel(factory = AdminViewModelFactory(appContainer.complaintRepository))
    val stats by vm.stats.collectAsState()
    LaunchedEffect(Unit) { vm.fetchStats() }
    val data = stats.data
    val total = (data?.newComplaint?.size ?: 0) + (data?.inProgressComplaint?.size ?: 0) + (data?.resolvedComplaint?.size ?: 0)
    UiCard {
        Text("Admin overview", style = MaterialTheme.typography.headlineSmall, fontFamily = DisplayFontFamily)
        Text("Track system load and complaint resolution progress.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        listOf("Total" to total, "New" to (data?.newComplaint?.size ?: 0), "In progress" to (data?.inProgressComplaint?.size ?: 0), "Resolved" to (data?.resolvedComplaint?.size ?: 0)).forEach { (label, value) ->
            UiCard(modifier = Modifier.weight(1f)) { Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("$value", style = MaterialTheme.typography.headlineSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) }
        }
    }
    if (data != null) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            UiCard(modifier = Modifier.weight(1f)) {
                Text("Complaints by category", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                val all = (data.newComplaint ?: emptyList()) + (data.inProgressComplaint ?: emptyList()) + (data.resolvedComplaint ?: emptyList())
                val byCat = all.groupBy { it.category ?: "other" }.map { (k, v) -> k to v.size }
                if (byCat.isNotEmpty()) {
                    byCat.forEach { (cat, count) ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(cat.replace("_", " "), fontSize = 11.sp)
                            Text("$count", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 11.sp)
                        }
                        LinearProgressIndicator(progress = { count.toFloat() / (byCat.maxOf { it.second }.toFloat().coerceAtLeast(1f)) }, modifier = Modifier.fillMaxWidth().height(4.dp))
                        Spacer(Modifier.height(4.dp))
                    }
                } else Text("No data", fontSize = 11.sp)
            }
            UiCard(modifier = Modifier.weight(1f)) {
                Text("Status split", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                val pieData = listOf(
                    "New" to (data.newComplaint?.size ?: 0),
                    "In Progress" to (data.inProgressComplaint?.size ?: 0),
                    "Resolved" to (data.resolvedComplaint?.size ?: 0)
                )
                pieData.forEach { (name, v) ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(name, fontSize = 12.sp)
                        Text("$v", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AllComplaintsPane(appContainer: AppContainer, onOpen: (String) -> Unit) {
    val vm: AdminViewModel = viewModel(factory = AdminViewModelFactory(appContainer.complaintRepository))
    val state by vm.list.collectAsState()
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") }
    var page by remember { mutableStateOf(1) }
    LaunchedEffect(filter, search, page) { vm.fetchComplaints(page, filter, search) }
    // debounce search
    var debounced by remember { mutableStateOf(search) }
    LaunchedEffect(search) { kotlinx.coroutines.delay(400); debounced = search; page = 1 }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        UiCard {
            Text("All complaints", style = MaterialTheme.typography.headlineSmall)
            Text("Review, search, and open complaint details.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(value = search, onValueChange = { search = it }, placeholder = { Text("Search by complaint details", fontSize = 12.sp) }, modifier = Modifier.weight(1f), singleLine = true)
            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { expanded = true }) { Text(filter, fontSize = 11.sp) }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    listOf("all", "new", "in_progress", "resolved").forEach { f -> DropdownMenuItem(text = { Text(if (f == "all") f else statusLabel(f)) }, onClick = { filter = f; page = 1; expanded = false }) }
                }
            }
        }
        if (state.isLoading) Text("Loading complaints...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        else if (state.complaints.isEmpty()) Text("No complaints found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        else {
            state.complaints.forEach { c ->
                UiCard(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("#${c.id.takeLast(6).uppercase()} • ${c.user?.fullName ?: "Citizen"}", style = MaterialTheme.typography.titleSmall)
                            Text(c.description.take(80), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${c.city} • ${c.createdAt?.take(10) ?: ""}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            StatusBadge(c.status)
                            TextButton(onClick = { onOpen(c.id) }) { Text("Open", fontSize = 11.sp) }
                        }
                    }
                }
            }
            val totalPages = state.pagination?.totalPages ?: 1
            if (totalPages > 1) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { if (page > 1) page-- }, enabled = page > 1) { Text("Previous") }
                    Text("Page $page of $totalPages", fontSize = 11.sp, modifier = Modifier.padding(horizontal = 12.dp))
                    OutlinedButton(onClick = { if (page < totalPages) page++ }, enabled = page < totalPages) { Text("Next") }
                }
            }
        }
    }
}

@Composable
fun AdminChatsPane(appContainer: AppContainer, onOpenChat: (String) -> Unit) {
    var chats by remember { mutableStateOf<List<com.civicresolve.ap.data.model.Complaint>>(emptyList()) }
    var filter by remember { mutableStateOf("all") }
    var page by remember { mutableStateOf(1) }
    var totalPages by remember { mutableStateOf(1) }
    var loading by remember { mutableStateOf(false) }
    val vm: AdminViewModel = viewModel(factory = AdminViewModelFactory(appContainer.complaintRepository))
    LaunchedEffect(filter, page) {
        loading = true
        vm.fetchAdminChats(page, filter) { list, pagination -> chats = list; totalPages = pagination?.totalPages ?: 1; loading = false }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Active chats", style = MaterialTheme.typography.titleSmall)
            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { expanded = true }) { Text(filter, fontSize = 11.sp) }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    listOf("all", "new", "in_progress", "resolved").forEach { f -> DropdownMenuItem(text = { Text(if (f == "all") f else statusLabel(f)) }, onClick = { filter = f; page = 1; expanded = false }) }
                }
            }
        }
        if (loading) Text("Loading chats...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        else if (chats.isEmpty()) Text("No chats found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        else {
            chats.forEach { c ->
                UiCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text("#${c.id.takeLast(8).uppercase()}", style = MaterialTheme.typography.titleSmall); Text("${c.createdAt?.take(10) ?: ""} • ${c.user?.fullName ?: "Citizen"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        StatusBadge(c.status)
                    }
                    TextButton(onClick = { onOpenChat(c.id) }) { Text("Open chat") }
                }
            }
            if (totalPages > 1) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    OutlinedButton(onClick = { if (page > 1) page-- }, enabled = page > 1) { Text("Previous") }
                    Text("Page $page of $totalPages", modifier = Modifier.padding(horizontal = 8.dp).align(Alignment.CenterVertically))
                    OutlinedButton(onClick = { if (page < totalPages) page++ }, enabled = page < totalPages) { Text("Next") }
                }
            }
        }
    }
}

@Composable
fun AdminComplaintOverviewScreen(
    complaintId: String,
    appContainer: AppContainer,
    onBack: () -> Unit,
    onChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val vm: ComplaintDetailViewModel = viewModel(factory = ComplaintDetailViewModelFactory(appContainer.complaintRepository))
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    var selectedStatus by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(complaintId) { vm.fetchComplaint(complaintId) }
    val c = state.complaint
    if (state.isLoading) { Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return }
    if (c == null) { Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Complaint not found.") }; return }
    LaunchedEffect(c.status) { selectedStatus = c.status }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val file = createTempFileFromUri(context, it)
            if (file != null) vm.updateStatusWithImage(c.id, selectedStatus, file)
        }
    }
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        AppHeader(
            brandLabel = "CivicResolve",
            brandInitial = "A",
            title = "Manage complaint",
            subtitle = "ID #${c.id.takeLast(8).uppercase()}",
            actions = {
                OutlinedButton(onClick = onBack) { Text("Dashboard", fontSize = 11.sp) }
                Spacer(Modifier.width(6.dp))
                Button(onClick = onChat) { Text("Chat", fontSize = 11.sp) }
            }
        )
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            UiCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        StatusBadge(c.status)
                        Text(c.description.ifBlank { "Reported issue" }, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 8.dp))
                        Text("#${c.id.takeLast(8).uppercase()} • ${c.city}, ${c.state}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(onClick = { expanded = true }) { Text(selectedStatus ?: c.status, fontSize = 11.sp) }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                listOf("new", "in_progress", "resolved").forEach { s ->
                                    DropdownMenuItem(text = { Text(statusLabel(s)) }, onClick = {
                                        if (s == "resolved" && c.afterImageUrl.isNullOrBlank()) {
                                            // need after image
                                        }
                                        selectedStatus = s; expanded = false
                                        // if no image needed, update immediately via file null
                                        // we need to handle status-only update: create empty file handling via VM
                                        if (s != "resolved" || !c.afterImageUrl.isNullOrBlank()) {
                                            // status only: call VM with no file (we pass null, VM handles)
                                            vm.updateStatusWithImage(c.id, s, null)
                                        }
                                    })
                                }
                            }
                        }
                        Button(onClick = { pickImage.launch("image/*") }) { Text("Upload after image", fontSize = 11.sp) }
                        OutlinedButton(onClick = onChat) { Text("Open chat", fontSize = 11.sp) }
                    }
                }
                if (!state.error.isNullOrBlank()) Text(state.error!!, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                if (!state.successMessage.isNullOrBlank()) Text(state.successMessage!!, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                UiCard(modifier = Modifier.weight(1f)) {
                    Text("Reporter details", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    Text("Name: ${c.user?.fullName ?: "Citizen"}", fontSize = 12.sp)
                    Text("Email: ${c.user?.email ?: "-"}", fontSize = 12.sp)
                    Text("Landmark: ${c.landmark ?: "-"}", fontSize = 12.sp)
                    Text("Rating: ${if (c.rating > 0) "${c.rating}/5" else "Not rated"}", fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    ComplaintMap(lat = c.latitude, lng = c.longitude)
                }
                UiCard(modifier = Modifier.weight(1f)) {
                    Text("Evidence images", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    Text("Before image", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!c.beforeImageUrl.isNullOrBlank()) coil.compose.AsyncImage(model = c.beforeImageUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().height(160.dp))
                    else Text("No image", fontSize = 11.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("After image", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!c.afterImageUrl.isNullOrBlank()) coil.compose.AsyncImage(model = c.afterImageUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().height(160.dp))
                    else Text("No image uploaded yet", fontSize = 11.sp)
                }
            }
        }
    }
}

private fun createTempFileFromUri(context: android.content.Context, uri: Uri): java.io.File? {
    return try {
        val input = context.contentResolver.openInputStream(uri) ?: return null
        val file = java.io.File.createTempFile("upload", ".jpg", context.cacheDir)
        file.outputStream().use { output -> input.copyTo(output) }
        file
    } catch (_: Exception) { null }
}
