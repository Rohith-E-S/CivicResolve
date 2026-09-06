package com.civicresolve.ap.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SidebarItem(val key: String, val label: String, val icon: ImageVector)

private val citizenItems = listOf(
    SidebarItem("overview", "Overview", Icons.Outlined.Dashboard),
    SidebarItem("new_complaint", "New complaint", Icons.Outlined.AddLocation),
    SidebarItem("chats", "Chats", Icons.Outlined.Chat),
    SidebarItem("profile", "Profile", Icons.Outlined.Person),
)

@Composable
fun Sidebar(
    activeTab: String,
    onTabSelected: (String) -> Unit,
    onExplore: () -> Unit,
    onMap: () -> Unit,
    onLogout: () -> Unit,
    collapsed: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val width by animateDpAsState(if (collapsed) 56.dp else 224.dp, label = "sidebarWidth")
    Column(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline)
            .padding(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            if (!collapsed) Text("Citizen dashboard", style = MaterialTheme.typography.titleSmall, maxLines = 1)
            IconButton(onClick = onToggle, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Outlined.ChevronRight, null, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        citizenItems.forEach { item ->
            SidebarButton(item.label, item.icon, selected = activeTab == item.key, collapsed = collapsed, onClick = { onTabSelected(item.key) })
        }
        SidebarButton("Explore", Icons.Outlined.Explore, selected = activeTab == "explore", collapsed = collapsed, onClick = onExplore)
        SidebarButton("Map view", Icons.Outlined.Map, selected = activeTab == "map", collapsed = collapsed, onClick = onMap)
        Spacer(Modifier.weight(1f))
        SidebarButton("Log out", Icons.Outlined.Logout, selected = false, collapsed = collapsed, onClick = onLogout)
    }
}

@Composable
fun AdminSidebar(
    activeTab: String,
    onTabSelected: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(220.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline)
            .padding(12.dp)
    ) {
        Text("Admin", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(12.dp))
        listOf("overview" to "Overview", "complaints" to "Complaints", "chats" to "Chats").forEach { (k, l) ->
            val selected = activeTab == k
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                    .clickable { onTabSelected(k) }
                    .padding(12.dp)
            ) {
                Text(l, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
            }
            Spacer(Modifier.height(6.dp))
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("Log out") }
    }
}

@Composable
private fun SidebarButton(label: String, icon: ImageVector, selected: Boolean, collapsed: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
        if (!collapsed) {
            Spacer(Modifier.width(10.dp))
            Text(label, color = fg, fontSize = 13.sp, maxLines = 1)
        }
    }
    Spacer(Modifier.height(6.dp))
}
