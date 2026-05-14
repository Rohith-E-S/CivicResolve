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
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.res.stringResource
import com.example.complaintportal.R

// ── Colors ────────────────────────────────────────────────────────────────────
private val NavyPrimary   @Composable get() = MaterialTheme.colorScheme.primary
private val NavyDark      @Composable get() = MaterialTheme.colorScheme.onPrimaryContainer
private val TealAccent    @Composable get() = MaterialTheme.colorScheme.secondary
private val GoldAccent    @Composable get() = Color(0xFFF4A700)   // Admin-exclusive gold
private val BgLight       @Composable get() = MaterialTheme.colorScheme.background
private val CardWhite     @Composable get() = MaterialTheme.colorScheme.surface
private val TextPrimary   @Composable get() = MaterialTheme.colorScheme.onSurface
private val TextSecondary @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
private val DangerRed     @Composable get() = MaterialTheme.colorScheme.error
private val DividerColor  @Composable get() = MaterialTheme.colorScheme.outlineVariant
private val GreenResolved @Composable get() = Color(0xFF1D9E75)
private val AmberActive   @Composable get() = Color(0xFFE67E22)

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
    onNavigateToAnalytics: () -> Unit,
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
                        stringResource(R.string.admin_profile),
                        fontWeight = FontWeight.SemiBold,
                        color      = NavyPrimary,
                        fontSize   = 18.sp,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back), tint = NavyPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit), tint = NavyPrimary)
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

            // ── Admin info ────────────────────────────────────────────────────
            ProfileSection(title = stringResource(R.string.admin_info)) {
                InfoRow(icon = Icons.Outlined.Person,         label = stringResource(R.string.full_name),    value = profile.name)
                SectionDivider()
                InfoRow(icon = Icons.Outlined.Email,          label = stringResource(R.string.email),        value = profile.email)
                if (profile.phone.isNotBlank()) {
                    SectionDivider()
                    InfoRow(icon = Icons.Outlined.Phone,      label = stringResource(R.string.phone),        value = profile.phone)
                }
                if (profile.jurisdiction.isNotBlank()) {
                    SectionDivider()
                    InfoRow(icon = Icons.Outlined.LocationCity, label = stringResource(R.string.jurisdiction), value = profile.jurisdiction)
                }
                if (profile.adminId.isNotBlank()) {
                    SectionDivider()
                    InfoRow(icon = Icons.Outlined.Badge,      label = stringResource(R.string.admin_id),     value = profile.adminId)
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Permissions ───────────────────────────────────────────────────
            ProfileSection(title = stringResource(R.string.permissions)) {
                PermissionRow(label = stringResource(R.string.manage_users),    granted = profile.canManageUsers)
                SectionDivider()
                PermissionRow(label = stringResource(R.string.export_reports),  granted = profile.canExportReports)
                SectionDivider()
                PermissionRow(label = stringResource(R.string.broadcast_alerts),granted = profile.canBroadcast)
            }

            Spacer(Modifier.height(12.dp))

            // ── Admin actions ─────────────────────────────────────────────────
            ProfileSection(title = stringResource(R.string.admin_actions)) {
                ActionRow(
                    icon    = Icons.Outlined.Description,
                    label   = stringResource(R.string.all_complaints),
                    badge   = profile.totalAssigned.toString(),
                    onClick = onViewAllComplaints,
                )
                SectionDivider()
                ActionRow(
                    icon    = Icons.Outlined.Download,
                    label   = stringResource(R.string.export_reports),
                    enabled = profile.canExportReports,
                    onClick = onExportReports,
                )
                SectionDivider()
                ActionRow(
                    icon    = Icons.Outlined.BarChart,
                    label   = "Analytics Dashboard",
                    onClick = onNavigateToAnalytics,
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Notification settings ─────────────────────────────────────────
            ProfileSection(title = stringResource(R.string.notifications_title)) {
                ToggleRow(
                    icon     = Icons.Outlined.Notifications,
                    label    = stringResource(R.string.push_notifications),
                    subtitle = stringResource(R.string.notif_subtitle),
                    checked  = notifEnabled,
                    onToggle = { notifEnabled = it },
                )
                SectionDivider()
                ToggleRow(
                    icon     = Icons.Outlined.Email,
                    label    = stringResource(R.string.email_alerts),
                    subtitle = stringResource(R.string.email_alerts_subtitle),
                    checked  = emailEnabled,
                    onToggle = { emailEnabled = it },
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Preferences ───────────────────────────────────────────────────
            ProfileSection(title = stringResource(R.string.preferences)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment   = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NavyPrimary.copy(alpha = 0.08f)),
                        ) {
                            Icon(Icons.Outlined.Language, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.language), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                    }
                    com.example.complaintportal.ui.components.LanguageSelector()
                }
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
                    tint     = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.logout), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Medium, fontSize = 15.sp)
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
            title   = { Text(stringResource(R.string.logout_question), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = TextPrimary) },
            text    = { Text(stringResource(R.string.admin_logout_message), fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center) },
            confirmButton = {
                Button(
                    onClick  = { 
                        showLogoutDialog = false
                        authViewModel.logout(context)
                    },
                    colors   = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    shape    = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.logout), fontWeight = FontWeight.Medium) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick  = { showLogoutDialog = false },
                    shape    = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border   = BorderStroke(1.dp, DividerColor),
                ) { Text(stringResource(R.string.cancel), color = TextSecondary) }
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
                        contentDescription = stringResource(R.string.profile_picture),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text       = initials,
                        color      = MaterialTheme.colorScheme.onPrimary,
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
                    .offset(x = (-4).dp, y = (-4).dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(GoldAccent)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .clickable { onEditClick() },
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = stringResource(R.string.edit),
                    tint     = Color.White, // Keeps white tint as it's on GoldAccent which doesn't adapt to dark mode
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
                text      = if (granted) stringResource(R.string.granted) else stringResource(R.string.denied),
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
                Icon(Icons.Default.Lock, contentDescription = stringResource(R.string.no_permission), tint = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
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
                checkedThumbColor   = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor   = NavyPrimary,
                uncheckedThumbColor = MaterialTheme.colorScheme.surface,
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
