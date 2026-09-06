package com.civicresolve.ap.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.civicresolve.ap.ui.theme.MonoFontFamily

private val DISTRICTS = listOf("Gobi","Erode","Coimbatore","Chennai","Salem","Madurai","Tiruchirappalli","Thanjavur","Dindigul","Tiruppur")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingDistrictDialog(
    currentDistrict: String?,
    isLoading: Boolean,
    message: String?,
    onDetectLocation: () -> Unit,
    onSave: (String) -> Unit,
    locLoading: Boolean,
    detectedDistrict: String? = null
) {
    var district by remember { mutableStateOf(currentDistrict ?: "") }
    var expanded by remember { mutableStateOf(false) }

    // Preselect the reverse-geocoded city so "Use current location" actually
    // fills the field instead of only showing a message
    LaunchedEffect(detectedDistrict) {
        if (!detectedDistrict.isNullOrBlank() && district.isBlank()) district = detectedDistrict
    }

    Dialog(onDismissRequest = {}) {
        UiCard {
            Text("Step 1 • Set your ledger district", fontFamily = MonoFontFamily, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(8.dp))
            Text("Where do you live?", style = MaterialTheme.typography.headlineSmall)
            Text("We use this to show My District reports and to keep your Map view centered.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    value = district,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Home district") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    (listOfNotNull(detectedDistrict?.takeIf { it.isNotBlank() }) + DISTRICTS).distinct().forEach { d ->
                        DropdownMenuItem(text = { Text(d) }, onClick = { district = d; expanded = false })
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onDetectLocation, enabled = !locLoading, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.MyLocation, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (locLoading) "Detecting…" else "Use current location", fontSize = 13.sp)
            }
            if (!message.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(message, fontFamily = MonoFontFamily, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = { if (district.isNotBlank()) onSave(district) }, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) {
                Text(if (isLoading) "Saving…" else "Save & continue")
            }
            Spacer(Modifier.height(6.dp))
            Text("Takes 5 seconds • You can skip by selecting manually", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
