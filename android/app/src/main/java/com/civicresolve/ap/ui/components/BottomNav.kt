package com.civicresolve.ap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CitizenBottomNav(
    activeTab: String,
    onTabSelected: (String) -> Unit,
    onExplore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomItem("Home", Icons.Outlined.Dashboard, activeTab == "overview") { onTabSelected("overview") }
        BottomItem("New", Icons.Outlined.AddLocation, activeTab == "new_complaint") { onTabSelected("new_complaint") }
        BottomItem("Explore", Icons.Outlined.Explore, activeTab == "explore") { onExplore() }
        BottomItem("Chats", Icons.Outlined.Chat, activeTab == "chats") { onTabSelected("chats") }
        BottomItem("Profile", Icons.Outlined.Person, activeTab == "profile") { onTabSelected("profile") }
    }
}

@Composable
fun AdminBottomNav(activeTab: String, onTabSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        BottomItem("Overview", Icons.Outlined.Dashboard, activeTab == "overview") { onTabSelected("overview") }
        BottomItem("Complaints", Icons.Outlined.List, activeTab == "complaints") { onTabSelected("complaints") }
        BottomItem("Chats", Icons.Outlined.Chat, activeTab == "chats") { onTabSelected("chats") }
    }
}

@Composable
private fun BottomItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(18.dp), tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(label, fontSize = 10.sp, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
