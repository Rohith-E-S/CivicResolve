package com.civicresolve.ap.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.civicresolve.ap.di.AppContainer
import com.civicresolve.ap.ui.components.*
import com.civicresolve.ap.ui.theme.DisplayFontFamily
import com.civicresolve.ap.ui.theme.MonoFontFamily
import com.civicresolve.ap.ui.viewmodel.ComplaintDetailViewModel
import com.civicresolve.ap.ui.viewmodel.ComplaintDetailViewModelFactory

@Composable
fun ComplaintOverviewScreen(
    complaintId: String,
    appContainer: AppContainer,
    onBack: () -> Unit,
    onChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val vm: ComplaintDetailViewModel = viewModel(factory = ComplaintDetailViewModelFactory(appContainer.complaintRepository))
    val state by vm.state.collectAsState()
    var hoverRating by remember { mutableStateOf(0) }
    LaunchedEffect(complaintId) { vm.fetchComplaint(complaintId) }

    if (state.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val c = state.complaint
    if (c == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { Text("Complaint not found.") }
        return
    }
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        AppHeader(
            brandLabel = "CivicResolve",
            brandInitial = "C",
            title = "Complaint details",
            subtitle = "ID #${c.id.takeLast(8).uppercase()}",
            actions = {
                OutlinedButton(onClick = onBack) { Text("Dashboard", fontSize = 11.sp) }
                Spacer(Modifier.width(6.dp))
                Button(onClick = onChat) { Text("Chat", fontSize = 11.sp) }
            }
        )
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HeroBlueprint {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IdStencil(c.id)
                    StatusBadge(c.status)
                    Text("• ${c.category?.replace("_", " ")}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(c.description.ifBlank { "Reported issue" }, style = MaterialTheme.typography.headlineSmall, fontFamily = DisplayFontFamily)
                Text("Filed ${c.createdAt?.take(10) ?: "-"} • ${c.city} • ID #${c.id.takeLast(8).uppercase()}", fontFamily = MonoFontFamily, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onChat, modifier = Modifier.padding(top = 10.dp)) { Text("Open chat") }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(2f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    UiCard {
                        Text("Complaint information", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column { Text("Category", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(c.category?.replace("_", " ") ?: "Other", fontWeight = androidx.compose.ui.text.font.FontWeight.Medium) }
                            Column { Text("Location", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(c.landmark.ifBlank { c.city }, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium) }
                        }
                        Spacer(Modifier.height(10.dp))
                        ComplaintMap(lat = c.latitude, lng = c.longitude)
                    }
                    UiCard {
                        Text("Before and after", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Before", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (!c.beforeImageUrl.isNullOrBlank()) coil.compose.AsyncImage(model = c.beforeImageUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().height(180.dp))
                                else Text("No image", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("After", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (!c.afterImageUrl.isNullOrBlank()) coil.compose.AsyncImage(model = c.afterImageUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().height(180.dp))
                                else Text("Pending resolution image", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            }
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (c.status == "resolved" && !state.isAdmin) {
                        UiCard {
                            Text("Rate resolution", style = MaterialTheme.typography.titleSmall)
                            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                (1..5).forEach { star ->
                                    OutlinedButton(onClick = { vm.rateComplaint(c.id, star) }) { Text("★", color = if (star <= (hoverRating.takeIf { it != 0 } ?: c.rating)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                                }
                            }
                        }
                    }
                    UiCard {
                        Text("Timeline", style = MaterialTheme.typography.titleSmall, fontFamily = DisplayFontFamily)
                        Spacer(Modifier.height(10.dp))
                        LedgerLine(label = "Reported", date = c.createdAt ?: "-", dotColor = androidx.compose.ui.graphics.Color(0xFFE53935))
                        Spacer(Modifier.height(8.dp))
                        LedgerLine(label = "Last update", date = c.updatedAt ?: "-", dotColor = if (c.status == "resolved") androidx.compose.ui.graphics.Color(0xFF0E9F6E) else androidx.compose.ui.graphics.Color(0xFFFFB74D))
                        Spacer(Modifier.height(8.dp))
                        Text("Status: ${c.status} • Support: ${c.supportCount ?: 0}", fontFamily = MonoFontFamily, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}
