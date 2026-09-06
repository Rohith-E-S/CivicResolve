package com.civicresolve.ap.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.civicresolve.ap.ui.components.*
import com.civicresolve.ap.ui.theme.CivicRounded
import com.civicresolve.ap.ui.theme.DisplayFontFamily
import com.civicresolve.ap.ui.theme.MonoFontFamily
import com.civicresolve.ap.ui.viewmodel.AuthViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.civicresolve.ap.di.AppContainer

@Composable
fun LandingScreen(
    isLoggedIn: Boolean,
    stats: com.civicresolve.ap.data.model.PublicStats?,
    onLogin: () -> Unit,
    onSignup: () -> Unit,
    onDashboard: () -> Unit,
    onExplore: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        AppHeader(
            brandLabel = "CivicResolve",
            brandInitial = "⬢",
            title = "Municipal ledger",
            subtitle = "Report • Verify • Resolve",
            actions = {
                if (isLoggedIn) {
                    OutlinedButton(onClick = onExplore) { Text("Explore map", fontSize = 12.sp) }
                    Spacer(Modifier.width(6.dp))
                    Button(onClick = onDashboard) { Text("Dashboard", fontSize = 12.sp) }
                    Spacer(Modifier.width(6.dp))
                    TextButton(onClick = onLogout) { Text("Log out", fontSize = 12.sp) }
                } else {
                    OutlinedButton(onClick = onLogin) { Text("Sign in", fontSize = 12.sp) }
                    Spacer(Modifier.width(6.dp))
                    Button(onClick = onSignup) { Text("Create account", fontSize = 12.sp) }
                }
            }
        )
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HeroBlueprint {
                val total = (stats?.totalActive ?: 0) + (stats?.totalResolved ?: 0)
                Text("Civic ledger • Every dot is a neighborhood report", fontFamily = MonoFontFamily, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.6.sp)
                Spacer(Modifier.height(8.dp))
                Text("Your street,", style = MaterialTheme.typography.displayMedium, fontFamily = DisplayFontFamily)
                Text("on the record.", style = MaterialTheme.typography.displayMedium, fontFamily = DisplayFontFamily, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Normal)
                Spacer(Modifier.height(10.dp))
                Text("File a report, watch it appear on the communal map, and follow it from new → verification → resolved. Not a ticket — a visible entry in the neighborhood ledger.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isLoggedIn) {
                        Button(onClick = onDashboard) { Text("Open dashboard") }
                        OutlinedButton(onClick = onExplore) { Text("Explore live map") }
                    } else {
                        Button(onClick = onSignup, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B2B))) { Text("Submit a complaint") }
                        OutlinedButton(onClick = onExplore) { Text("View public map") }
                        TextButton(onClick = onLogin) { Text("Track existing") }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("CIV-LEDGER 2024 • Surveyor grid 32×32", fontFamily = MonoFontFamily, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                // Stats preview
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFFFFF7ED)).border(1.dp, Color(0xFFFFEDD5), RoundedCornerShape(8.dp)).padding(12.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${stats?.totalActive ?: 0}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF9A3412))
                            Text("Active", fontFamily = MonoFontFamily, fontSize = 9.sp, color = Color(0xFF9A3412))
                        }
                    }
                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFFECFDF5)).border(1.dp, Color(0xFFBBF7D0), RoundedCornerShape(8.dp)).padding(12.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${stats?.totalResolved ?: 0}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF14532D))
                            Text("Resolved", fontFamily = MonoFontFamily, fontSize = 9.sp, color = Color(0xFF14532D))
                        }
                    }
                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)).padding(12.dp)) {
                        Text("${total} dots", fontFamily = MonoFontFamily, fontSize = 11.sp)
                    }
                }
                Text(stats?.scope ?: "Nationwide", fontFamily = MonoFontFamily, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(
                    Triple("01", "Report", "Drop a pin, describe the issue, add a photo. It becomes a dot on the district map within seconds."),
                    Triple("02", "Verify", "Neighbors within 500m confirm fixes. Three verifications move a pending report to resolved."),
                    Triple("03", "Resolve", "Admins update status, you rate the result, and the dot turns green — visible to everyone.")
                ).forEach { (n, title, desc) ->
                    UiCard(modifier = Modifier.weight(1f)) {
                        Text("Step $n", fontFamily = MonoFontFamily, fontSize = 9.sp, color = MaterialTheme.colorScheme.primary)
                        Text(title, style = MaterialTheme.typography.titleMedium, fontFamily = DisplayFontFamily)
                        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }

            UiCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Public by default • No login to browse", fontFamily = MonoFontFamily, fontSize = 10.sp)
                        Text("Even without an account, the map and recent reports are visible. Sign in to contribute.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(onClick = onExplore) { Text("Open explore →") }
                }
            }
        }
    }
}
