package com.civicresolve.ap.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.civicresolve.ap.di.AppContainer
import com.civicresolve.ap.ui.components.*
import com.civicresolve.ap.ui.theme.MonoFontFamily
import com.civicresolve.ap.ui.utils.haversineDistance
import com.civicresolve.ap.ui.viewmodel.AuthViewModel
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch

@Composable
fun ExploreScreen(
    appContainer: AppContainer,
    authViewModel: AuthViewModel,
    onOpenComplaint: (String) -> Unit,
    onDashboard: () -> Unit,
    onMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val authState by authViewModel.authState.collectAsState()
    var scope by remember { mutableStateOf("MY_DISTRICT") }
    var filter by remember { mutableStateOf("all") }
    var search by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf("newest") }
    var complaints by remember { mutableStateOf<List<com.civicresolve.ap.data.model.Complaint>>(emptyList()) }
    var stats by remember { mutableStateOf<com.civicresolve.ap.data.model.PublicStats?>(null) }
    var loading by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf(1) }
    var totalPages by remember { mutableStateOf(1) }
    var userLoc by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var showBeyond by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val userDistrict = authState.user?.homeDistrict ?: ""
    val scopeLocal = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            val fused = LocationServices.getFusedLocationProviderClient(context)
            fused.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    userLoc = loc.latitude to loc.longitude
                    scopeLocal.launch { appContainer.authRepository.updateUserLocation(loc.latitude, loc.longitude) }
                }
            }
        } catch (_: SecurityException) {}
    }

    fun fetchFeed(p: Int = 1) {
        loading = true
        val districtParam = if (scope == "MY_DISTRICT") userDistrict else "all"
        scopeLocal.launch {
            val r = appContainer.complaintRepository.getPublicFeed(districtParam.ifBlank { "all" }, p, 20)
            r.onSuccess { res ->
                if (res.success) {
                    complaints = res.complaints ?: emptyList()
                    page = p
                }
                loading = false
            }.onFailure { loading = false }
            val s = appContainer.complaintRepository.getPublicStats(districtParam.ifBlank { "all" })
            s.onSuccess { res -> stats = res.stats }
        }
    }

    LaunchedEffect(scope, userDistrict) { fetchFeed(1) }

    val filtered = remember(complaints, filter, search, sort, userLoc, showBeyond, scope) {
        var list = complaints.toMutableList()
        if (filter != "all") {
            list = when (filter) {
                "active" -> list.filter { it.status.lowercase() in listOf("in_progress", "in progress", "re_opened", "pending_verification", "disputed") }.toMutableList()
                "resolved" -> list.filter { it.status.lowercase() in listOf("resolved", "confirmed_resolved") }.toMutableList()
                else -> list.filter { it.status == filter }.toMutableList()
            }
        }
        if (search.isNotBlank()) {
            val q = search.lowercase()
            list = list.filter { "${it.description} ${it.category} ${it.city} ${it.landmark}".lowercase().contains(q) }.toMutableList()
        }
        when (sort) {
            "newest" -> list.sortByDescending { it.createdAt }
            "oldest" -> list.sortBy { it.createdAt }
            "nearest" -> if (userLoc != null) {
                val (lat, lng) = userLoc!!
                list.sortBy { c -> haversineDistance(lat, lng, c.latitude.toDoubleOrNull() ?: 0.0, c.longitude.toDoubleOrNull() ?: 0.0) }
                if (!showBeyond && scope == "MY_DISTRICT") {
                    val beyond = list.filter { c -> haversineDistance(lat, lng, c.latitude.toDoubleOrNull() ?: 0.0, c.longitude.toDoubleOrNull() ?: 0.0) > 5000 }
                    list = list.filter { c -> haversineDistance(lat, lng, c.latitude.toDoubleOrNull() ?: 0.0, c.longitude.toDoubleOrNull() ?: 0.0) <= 5000 }.toMutableList()
                    return@remember FilteredResult(list, beyond.size)
                }
            }
        }
        FilteredResult(list, 0)
    }
    val hiddenCount = filtered.hiddenCount
    val visible = filtered.visible

    Column(modifier = modifier.fillMaxSize()) {
        AppHeader(
            brandLabel = "CivicResolve",
            brandInitial = "⬢",
            title = "Explore",
            subtitle = if (scope == "MY_DISTRICT") "My District • ${userDistrict.ifBlank { "Set district in profile" }}" else "Global Feed • Nationwide",
            actions = {
                Column(horizontalAlignment = Alignment.End) {
                    Text(authState.user?.fullName ?: "Citizen", style = MaterialTheme.typography.titleSmall)
                    Text("Explore • Ledger", fontFamily = MonoFontFamily, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = onDashboard) { Text("Dashboard", fontSize = 11.sp) }
            }
        )
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (stats != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    UiCard(modifier = Modifier.weight(1f)) { Text("Scope", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(stats!!.scope ?: scope, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 13.sp) }
                    UiCard(modifier = Modifier.weight(1f)) { Text("Active issues", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${stats!!.totalActive}", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 16.sp) }
                    UiCard(modifier = Modifier.weight(1f)) { Text("Resolved", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${stats!!.totalResolved}", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { scope = "MY_DISTRICT" }, colors = if (scope == "MY_DISTRICT") ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()) { Text("My District", fontSize = 12.sp) }
                OutlinedButton(onClick = { scope = "GLOBAL_FEED" }) { Text("Global Feed", fontSize = 12.sp) }
            }
            UiCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = search, onValueChange = { search = it }, placeholder = { Text("Search by description, category, city…", fontSize = 12.sp) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        var fExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(onClick = { fExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text(filter, fontSize = 11.sp) }
                            DropdownMenu(expanded = fExpanded, onDismissRequest = { fExpanded = false }) {
                                listOf("all", "new", "active", "resolved").forEach { f -> DropdownMenuItem(text = { Text(f) }, onClick = { filter = f; fExpanded = false }) }
                            }
                        }
                        var sExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(onClick = { sExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text(sort, fontSize = 11.sp) }
                            DropdownMenu(expanded = sExpanded, onDismissRequest = { sExpanded = false }) {
                                listOf("newest", "oldest", "nearest").forEach { s -> DropdownMenuItem(text = { Text(s) }, onClick = { sort = s; sExpanded = false }) }
                            }
                        }
                        OutlinedButton(onClick = { fetchFeed(1) }) { Text("Refresh", fontSize = 11.sp) }
                    }
                    if (sort == "nearest" && userLoc == null) Text("Enable location for nearest", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                }
            }
            if (loading) {
                UiCard(modifier = Modifier.fillMaxWidth()) { Text("Loading ledger…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }
            } else {
                if (hiddenCount > 0) {
                    UiCard {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("$hiddenCount beyond 5km hidden.", fontSize = 12.sp)
                            OutlinedButton(onClick = { showBeyond = true }) { Text("Show all", fontSize = 11.sp) }
                        }
                    }
                }
                if (visible.isEmpty()) {
                    UiCard(modifier = Modifier.fillMaxWidth()) {
                        Text("No entries match", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 13.sp)
                        Text("Try Global feed or clear filters.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    visible.forEach { c ->
                        val dist = if (userLoc != null && c.latitude.toDoubleOrNull() != null) haversineDistance(userLoc!!.first, userLoc!!.second, c.latitude.toDouble(), c.longitude.toDouble()) else null
                        UiCard(modifier = Modifier.fillMaxWidth()) {
                            Row {
                                if (!c.beforeImageUrl.isNullOrBlank()) {
                                    androidx.compose.foundation.layout.Box(modifier = Modifier.size(64.dp)) {
                                        coil.compose.AsyncImage(model = c.beforeImageUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
                                    }
                                    Spacer(Modifier.width(10.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(c.description.take(88), fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontSize = 13.sp, maxLines = 2)
                                    Text("CIV-${c.id.takeLast(6).uppercase()} • ${c.category?.replace("_", " ")} • ${c.city}", fontFamily = MonoFontFamily, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                                        StatusBadge(c.status)
                                        if (dist != null) Text(String.format("%.1fkm", dist / 1000), fontFamily = MonoFontFamily, fontSize = 9.sp, color = when {
                                            dist <= 1000 -> androidx.compose.ui.graphics.Color(0xFF0E9F6E)
                                            dist <= 5000 -> androidx.compose.ui.graphics.Color(0xFFF59E0B)
                                            else -> androidx.compose.ui.graphics.Color(0xFFE53935)
                                        })
                                        if ((c.supportCount ?: 0) > 0) Text("▲ ${c.supportCount}", fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                        Spacer(Modifier.weight(1f))
                                        Text(c.createdAt?.take(10) ?: "", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            TextButton(onClick = { onOpenComplaint(c.id) }, modifier = Modifier.align(Alignment.End)) { Text("Open", fontSize = 11.sp) }
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { if (page > 1) fetchFeed(page - 1) }, enabled = page > 1) { Text("Previous") }
                    Text("Page $page of $totalPages", fontFamily = MonoFontFamily, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 12.dp))
                    OutlinedButton(onClick = { fetchFeed(page + 1) }, enabled = page < totalPages) { Text("Next") }
                }
            }
        }
        CitizenBottomNav(activeTab = "explore", onTabSelected = { if (it == "overview") onDashboard() else {} }, onExplore = {})
    }
}

private data class FilteredResult(val visible: List<com.civicresolve.ap.data.model.Complaint>, val hiddenCount: Int)
