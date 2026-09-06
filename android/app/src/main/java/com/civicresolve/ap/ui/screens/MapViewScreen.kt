package com.civicresolve.ap.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.civicresolve.ap.di.AppContainer
import com.civicresolve.ap.ui.components.AppHeader
import com.civicresolve.ap.ui.components.ExploreMapSimple
import com.civicresolve.ap.ui.theme.MonoFontFamily
import com.civicresolve.ap.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun MapViewScreen(
    appContainer: AppContainer,
    authViewModel: AuthViewModel,
    onDashboard: () -> Unit,
    onOpenComplaint: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val authState by authViewModel.authState.collectAsState()
    var scope by remember { mutableStateOf("MY_DISTRICT") }
    var complaints by remember { mutableStateOf<List<com.civicresolve.ap.data.model.Complaint>>(emptyList()) }
    var stats by remember { mutableStateOf<com.civicresolve.ap.data.model.PublicStats?>(null) }
    var loading by remember { mutableStateOf(false) }
    val userDistrict = authState.user?.homeDistrict ?: ""
    val scopeLocal = rememberCoroutineScope()

    fun fetch() {
        loading = true
        val districtParam = if (scope == "MY_DISTRICT") userDistrict else "all"
        scopeLocal.launch {
            val r = appContainer.complaintRepository.getPublicFeed(districtParam.ifBlank { "all" }, 1, 50)
            r.onSuccess { res -> complaints = res.complaints ?: emptyList(); loading = false }
                .onFailure { loading = false }
            val s = appContainer.complaintRepository.getPublicStats(districtParam.ifBlank { "all" })
            s.onSuccess { res -> stats = res.stats }
        }
    }
    LaunchedEffect(scope, userDistrict) { fetch() }

    Column(modifier = modifier.fillMaxSize()) {
        AppHeader(
            brandLabel = "CivicResolve",
            brandInitial = "⬢",
            title = "Map view",
            subtitle = if (scope == "MY_DISTRICT") "Surveillance • ${userDistrict.ifBlank { "Set district" }}" else "Surveillance • Nationwide",
            actions = {
                Text(authState.user?.fullName ?: "Citizen", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = onDashboard) { Text("Dashboard", fontSize = 11.sp) }
            }
        )
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { scope = "MY_DISTRICT" }, colors = if (scope == "MY_DISTRICT") ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()) { Text("My District", fontSize = 11.sp) }
                OutlinedButton(onClick = { scope = "GLOBAL_FEED" }) { Text("Global", fontSize = 11.sp) }
            }
            if (stats != null) Text("${stats!!.totalActive} active • ${stats!!.totalResolved} resolved", fontFamily = MonoFontFamily, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = { fetch() }) { Text("Refresh", fontSize = 11.sp) }
        }
        if (loading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) { Text("Loading surveillance dots…", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            ExploreMapSimple(complaints = complaints, scope = scope, onSelect = { onOpenComplaint(it.id) }, modifier = Modifier.weight(1f))
        }
    }
}
