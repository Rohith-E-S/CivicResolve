package com.example.complaintportal.ui.screens.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import coil.compose.rememberAsyncImagePainter
import com.example.complaintportal.ui.viewmodel.AuthViewModel
import com.example.complaintportal.ui.viewmodel.ComplaintViewModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

// ── Colors ────────────────────────────────────────────────────────────────────
private val NavyPrimary   = Color(0xFF1A3A6E)
private val NavyDark      = Color(0xFF0D2247)
private val TealAccent    = Color(0xFF7ECFC0)
private val GoldAccent    = Color(0xFFF4A700)   // Admin-exclusive gold
private val BgLight       = Color(0xFFF2F4F8)
private val CardWhite     = Color(0xFFFFFFFF)
private val TextPrimary   = Color(0xFF0D2247)
private val TextSecondary = Color(0xFF6A7F9A)
private val DangerRed     = Color(0xFFE53935)
private val DividerColor  = Color(0xFFE8EDF5)
private val GreenResolved = Color(0xFF1D9E75)
private val AmberActive   = Color(0xFFE67E22)

// ── Data model ────────────────────────────────────────────────────────────────
data class AdminProfile(
    val name:              String,
    val email:             String,
    val phone:             String  = "",
    val department:        String  = "Municipal Administration",
    val adminId:           String  = "",
    val jurisdiction:      String  = "",
    val memberSince:       String  = "",

    // Stats
    val totalAssigned:     Int     = 0,
    val resolvedCount:     Int     = 0,
    val pendingCount:      Int     = 0,
    val avgResolutionDays: Int     = 0,

    // Permissions
    val canManageUsers:    Boolean = true,
    val canExportReports:  Boolean = true,
    val canBroadcast:      Boolean = false,

    // Prefs
    val notificationsEnabled: Boolean = true,
    val emailAlertsEnabled:   Boolean = true,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProfileScreen(
    authViewModel: AuthViewModel,
    complaintViewModel: ComplaintViewModel,
    onBack:               () -> Unit,
    onManageUsers:        () -> Unit,
    onViewAllComplaints:  () -> Unit,
    onExportReports:      () -> Unit,
    onBroadcastMessage:   () -> Unit,
    onChangePassword:     () -> Unit,
    onActivityLog:        () -> Unit,
) {
    val authState by authViewModel.authState.collectAsState()
    val complaintState by complaintViewModel.state.collectAsState()
    val user = authState.user
    val context = LocalContext.current

    // Refresh data on entry
    LaunchedEffect(Unit) {
        complaintViewModel.fetchAdminComplaints(user?.id)
    }

    // Activity launchers for image picking and cropping
    val uCropLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val resultUri = com.yalantis.ucrop.UCrop.getOutput(result.data!!)
            resultUri?.let {
                val inputStream = context.contentResolver.openInputStream(it)
                val uploadFile = File(context.cacheDir, "profile_admin_${System.currentTimeMillis()}.jpg")
                val outputStream = FileOutputStream(uploadFile)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
                
                val requestFile = uploadFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("profilePic", uploadFile.name, requestFile)
                
                val fullNameReq = user?.fullName?.toRequestBody("text/plain".toMediaTypeOrNull())
                val addressReq = user?.address?.toRequestBody("text/plain".toMediaTypeOrNull())

                authViewModel.updateProfile(fullNameReq, addressReq, imagePart) {}
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val destinationUri = Uri.fromFile(File(context.cacheDir, "crop_admin_${System.currentTimeMillis()}.jpg"))
            val options = com.yalantis.ucrop.UCrop.Options().apply {
                setCompressionQuality(80)
                setCircleDimmedLayer(true)
            }
            val intent = com.yalantis.ucrop.UCrop.of(it, destinationUri)
                .withAspectRatio(1f, 1f)
                .withOptions(options)
                .getIntent(context)
            uCropLauncher.launch(intent)
        }
    }

    val totalAssigned = complaintState.newComplaints.size + complaintState.inProgressComplaints.size + complaintState.resolvedComplaints.size
    val resolvedCount = complaintState.resolvedComplaints.size
    val pendingCount = complaintState.newComplaints.size + complaintState.inProgressComplaints.size

    val profile = AdminProfile(
        name              = user?.fullName ?: "Admin",
        email             = user?.email ?: "N/A",
        phone             = user?.address ?: "",
        department        = "Municipal Administration",
        adminId           = user?.id?.takeLast(6)?.uppercase() ?: "N/A",
        jurisdiction      = user?.homeDistrict ?: "N/A",
        memberSince       = "Jan 2024",
        totalAssigned     = totalAssigned,
        resolvedCount     = resolvedCount,
        pendingCount      = pendingCount,
        avgResolutionDays = 2, // Mock for now
        canManageUsers    = true,
        canExportReports  = true,
        canBroadcast      = false,
    )

    var showLogoutDialog  by remember { mutableStateOf(false) }
    var notifEnabled      by remember { mutableStateOf(profile.notificationsEnabled) }
    var emailEnabled      by remember { mutableStateOf(profile.emailAlertsEnabled) }
    var statsVisible      by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { statsVisible = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Admin Profile",
                        fontWeight = FontWeight.SemiBold,
                        color      = NavyPrimary,
                        fontSize   = 18.sp,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NavyPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = NavyPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgLight),
            )
        },
        containerColor = BgLight,
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Spacer(Modifier.height(12.dp))

            // ── Admin avatar + name + badge ───────────────────────────────────
            AdminAvatarSection(
                initials    = profile.name.take(2).uppercase(),
                name        = profile.name,
                department  = profile.department,
                adminId     = profile.adminId,
                profilePic  = user?.profilePic,
                isLoading   = authState.isLoading,
                onEditClick = { galleryLauncher.launch("image/*") },
            )

            Spacer(Modifier.height(20.dp))

            // ── Performance stats ─────────────────────────────────────────────
            AnimatedVisibility(
                visible = statsVisible,
                enter   = fadeIn(tween(600)) + slideInVertically(tween(600)) { fullHeight -> fullHeight / 2 },
            ) {
                AdminStatsSection(
                    totalAssigned     = profile.totalAssigned,
                    resolvedCount     = profile.resolvedCount,
                    pendingCount      = profile.pendingCount,
                    avgResolutionDays = profile.avgResolutionDays,
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Admin info ────────────────────────────────────────────────────
            ProfileSection(title = "ADMIN INFO") {
                InfoRow(icon = Icons.Outlined.Person,         label = "Full Name",    value = profile.name)
                SectionDivider()
                InfoRow(icon = Icons.Outlined.Email,          label = "Email",        value = profile.email)
                if (profile.phone.isNotBlank()) {
                    SectionDivider()
                    InfoRow(icon = Icons.Outlined.Phone,      label = "Phone",        value = profile.phone)
                }
                if (profile.jurisdiction.isNotBlank()) {
                    SectionDivider()
                    InfoRow(icon = Icons.Outlined.LocationCity, label = "Jurisdiction", value = profile.jurisdiction)
                }
                if (profile.adminId.isNotBlank()) {
                    SectionDivider()
                    InfoRow(icon = Icons.Outlined.Badge,      label = "Admin ID",     value = profile.adminId)
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Permissions ───────────────────────────────────────────────────
            ProfileSection(title = "PERMISSIONS") {
                PermissionRow(label = "Manage Users",    granted = profile.canManageUsers)
                SectionDivider()
                PermissionRow(label = "Export Reports",  granted = profile.canExportReports)
                SectionDivider()
                PermissionRow(label = "Broadcast Alerts",granted = profile.canBroadcast)
            }

            Spacer(Modifier.height(12.dp))

            // ── Admin actions ─────────────────────────────────────────────────
            ProfileSection(title = "ADMIN ACTIONS") {
                ActionRow(
                    icon    = Icons.Outlined.People,
                    label   = "Manage Users",
                    enabled = profile.canManageUsers,
                    onClick = onManageUsers,
                )
                SectionDivider()
                ActionRow(
                    icon    = Icons.Outlined.Description,
                    label   = "All Complaints",
                    badge   = profile.totalAssigned.toString(),
                    onClick = onViewAllComplaints,
                )
                SectionDivider()
                ActionRow(
                    icon    = Icons.Outlined.Download,
                    label   = "Export Reports",
                    enabled = profile.canExportReports,
                    onClick = onExportReports,
                )
                SectionDivider()
                ActionRow(
                    icon    = Icons.Outlined.Campaign,
                    label   = "Broadcast Message",
                    enabled = profile.canBroadcast,
                    onClick = onBroadcastMessage,
                )
                SectionDivider()
                ActionRow(
                    icon    = Icons.Outlined.History,
                    label   = "Activity Log",
                    onClick = onActivityLog,
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Notification settings ─────────────────────────────────────────
            ProfileSection(title = "NOTIFICATIONS") {
                ToggleRow(
                    icon     = Icons.Outlined.Notifications,
                    label    = "Push Notifications",
                    subtitle = "New complaints & status updates",
                    checked  = notifEnabled,
                    onToggle = { notifEnabled = it },
                )
                SectionDivider()
                ToggleRow(
                    icon     = Icons.Outlined.Email,
                    label    = "Email Alerts",
                    subtitle = "Daily digest of pending issues",
                    checked  = emailEnabled,
                    onToggle = { emailEnabled = it },
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Security ──────────────────────────────────────────────────────
            ProfileSection(title = "SECURITY") {
                ActionRow(
                    icon    = Icons.Outlined.Lock,
                    label   = "Change Password",
                    onClick = onChangePassword,
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Logout ────────────────────────────────────────────────────────
            Button(
                onClick  = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(52.dp),
                shape  = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
            ) {
                Icon(
                    Icons.Default.Logout,
                    contentDescription = null,
                    tint     = Color.White,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Logout", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            }

            Spacer(Modifier.height(28.dp))
        }
    }

    // ── Logout dialog ─────────────────────────────────────────────────────────
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(NavyPrimary.copy(alpha = 0.1f)),
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(26.dp))
                }
            },
            title   = { Text("Logout?", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = TextPrimary) },
            text    = { Text("You'll need to sign in again to access the admin panel.", fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center) },
            confirmButton = {
                Button(
                    onClick  = { 
                        showLogoutDialog = false
                        authViewModel.logout(context)
                    },
                    colors   = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    shape    = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Logout", fontWeight = FontWeight.Medium) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick  = { showLogoutDialog = false },
                    shape    = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border   = BorderStroke(1.dp, DividerColor),
                ) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = CardWhite,
            shape          = RoundedCornerShape(20.dp),
        )
    }
}

// ── Admin Avatar ──────────────────────────────────────────────────────────────
@Composable
private fun AdminAvatarSection(
    initials:    String,
    name:        String,
    department:  String,
    adminId:     String,
    profilePic:  String? = null,
    isLoading:   Boolean = false,
    onEditClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.BottomEnd) {
            // Gold-rimmed avatar — distinguishes admin from citizen
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(NavyDark, NavyPrimary)
                        )
                    )
                    .border(3.dp, GoldAccent, CircleShape)
                    .clickable { onEditClick() },
            ) {
                if (!profilePic.isNullOrEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(profilePic),
                        contentDescription = "Profile Picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text       = initials,
                        color      = Color.White,
                        fontSize   = 32.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = GoldAccent,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            // Edit button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(GoldAccent)
                    .border(2.dp, Color.White, CircleShape)
                    .clickable { onEditClick() }
                    .offset(x = (-4).dp, y = (-4).dp),
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint     = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

        Spacer(Modifier.height(6.dp))

        // Gold admin badge — visually distinct from citizen teal badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(GoldAccent.copy(alpha = 0.12f))
                .border(1.dp, GoldAccent.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text(
                text       = "⭐ Admin",
                fontSize   = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color      = Color(0xFF8B6200),
            )
        }

        if (department.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(department, fontSize = 12.sp, color = TextSecondary)
        }
    }
}

// ── Admin Stats ───────────────────────────────────────────────────────────────
@Composable
private fun AdminStatsSection(
    totalAssigned:     Int,
    resolvedCount:     Int,
    pendingCount:      Int,
    avgResolutionDays: Int,
) {
    // Resolution rate percentage
    val resolutionRate = if (totalAssigned > 0)
        (resolvedCount * 100f / totalAssigned).toInt() else 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Top row — 3 stats
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardWhite)
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            AdminStatItem(value = totalAssigned, label = "Assigned",  color = NavyPrimary,   icon = "📋")
            StatDivider()
            AdminStatItem(value = resolvedCount, label = "Resolved",  color = GreenResolved, icon = "✅")
            StatDivider()
            AdminStatItem(value = pendingCount,  label = "Pending",   color = AmberActive,   icon = "⏳")
        }

        // Resolution rate card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(NavyPrimary)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("Resolution Rate", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    val animatedRate by animateIntAsState(
                        targetValue   = resolutionRate,
                        animationSpec = tween(1000, easing = EaseOutCubic),
                        label         = "rate",
                    )
                    Text(
                        "$animatedRate%",
                        fontSize   = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TealAccent,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("of issues resolved", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.padding(bottom = 4.dp))
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("Avg. Resolution", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                Spacer(Modifier.height(2.dp))
                val animatedDays by animateIntAsState(
                    targetValue   = avgResolutionDays,
                    animationSpec = tween(800, easing = EaseOutCubic),
                    label         = "days",
                )
                Text(
                    "$animatedDays days",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color      = GoldAccent,
                )
            }
        }
    }
}

@Composable
private fun AdminStatItem(value: Int, label: String, color: Color, icon: String) {
    val animatedValue by animateIntAsState(
        targetValue   = value,
        animationSpec = tween(800, easing = EaseOutCubic),
        label         = "stat_$label",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 18.sp)
        Spacer(Modifier.height(4.dp))
        Text(animatedValue.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 11.sp, color = TextSecondary)
    }
}

// ── Permission row (read-only, shows granted/denied) ─────────────────────────
@Composable
private fun PermissionRow(label: String, granted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment   = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (granted) GreenResolved.copy(alpha = 0.1f)
                        else         DangerRed.copy(alpha = 0.1f)
                    ),
            ) {
                Icon(
                    if (granted) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint     = if (granted) GreenResolved else DangerRed,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (granted) GreenResolved.copy(alpha = 0.1f)
                    else         DangerRed.copy(alpha = 0.1f)
                )
                .padding(horizontal = 10.dp, vertical = 3.dp),
        ) {
            Text(
                text      = if (granted) "Granted" else "Denied",
                fontSize  = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color     = if (granted) GreenResolved else DangerRed,
            )
        }
    }
}

// ── Shared composables ────────────────────────────────────────────────────────
@Composable
private fun ProfileSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text      = title,
            fontSize  = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color     = TextSecondary,
            modifier  = Modifier.padding(start = 4.dp, bottom = 8.dp),
            letterSpacing = 1.sp,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardWhite),
            content  = content,
        )
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(NavyPrimary.copy(alpha = 0.08f)),
        ) {
            Icon(icon, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 11.sp, color = TextSecondary)
            Spacer(Modifier.height(2.dp))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        }
    }
}

@Composable
private fun ActionRow(
    icon:    ImageVector,
    label:   String,
    badge:   String?  = null,
    enabled: Boolean  = true,
    onClick: () -> Unit,
) {
    val contentAlpha = if (enabled) 1f else 0.4f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment   = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(NavyPrimary.copy(alpha = 0.08f * contentAlpha)),
            ) {
                Icon(icon, contentDescription = null, tint = NavyPrimary.copy(alpha = contentAlpha), modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary.copy(alpha = contentAlpha))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (badge != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(NavyPrimary.copy(alpha = 0.08f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(badge, fontSize = 11.sp, color = NavyPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
            if (!enabled) {
                Icon(Icons.Default.Lock, contentDescription = "No permission", tint = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
            } else {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun ToggleRow(
    icon:     ImageVector,
    label:    String,
    subtitle: String   = "",
    checked:  Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment   = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(NavyPrimary.copy(alpha = 0.08f)),
            ) {
                Icon(icon, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                if (subtitle.isNotBlank()) {
                    Text(subtitle, fontSize = 11.sp, color = TextSecondary)
                }
            }
        }
        Switch(
            checked         = checked,
            onCheckedChange = onToggle,
            colors          = SwitchDefaults.colors(
                checkedThumbColor   = Color.White,
                checkedTrackColor   = NavyPrimary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = TextSecondary.copy(alpha = 0.3f),
            ),
        )
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(modifier = Modifier.padding(start = 64.dp), thickness = 0.5.dp, color = DividerColor)
}

@Composable
private fun StatDivider() {
    Box(modifier = Modifier.width(1.dp).height(48.dp).background(DividerColor))
}
