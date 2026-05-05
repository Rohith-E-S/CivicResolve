package com.example.complaintportal.ui.screens

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
import com.example.complaintportal.ui.viewmodel.ComplaintState
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

// ── Colors (matches your app theme) ──────────────────────────────────────────
private val NavyPrimary   = Color(0xFF1A3A6E)
private val NavyDark      = Color(0xFF0D2247)
private val TealAccent    = Color(0xFF7ECFC0)
private val BgLight       = Color(0xFFF2F4F8)
private val CardWhite     = Color(0xFFFFFFFF)
private val TextPrimary   = Color(0xFF0D2247)
private val TextSecondary = Color(0xFF6A7F9A)
private val DangerRed     = Color(0xFFE53935)
private val DividerColor  = Color(0xFFE8EDF5)

// ── Data classes ──────────────────────────────────────────────────────────────
data class UserProfile(
    val name:         String,
    val email:        String,
    val phone:        String    = "",
    val district:     String    = "",
    val memberSince:  String    = "",
    val reportsCount: Int       = 0,
    val resolvedCount:Int       = 0,
    val upvotesCount: Int       = 0,
    val role:         String    = "Citizen Advocate",
    val initials:     String    = "",
    val notificationsEnabled: Boolean = true,
    val darkModeEnabled:      Boolean = false,
)

// ── Main Screen ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    complaintViewModel: ComplaintViewModel,
    onNavigateBack: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    val complaintState by complaintViewModel.state.collectAsState()
    val user = authState.user
    val context = LocalContext.current

    // Activity launchers for image picking and cropping
    val uCropLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val resultUri = com.yalantis.ucrop.UCrop.getOutput(result.data!!)
            resultUri?.let {
                val inputStream = context.contentResolver.openInputStream(it)
                val uploadFile = File(context.cacheDir, "profile_${System.currentTimeMillis()}.jpg")
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
            val destinationUri = Uri.fromFile(File(context.cacheDir, "crop_${System.currentTimeMillis()}.jpg"))
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

    // Refresh data on entry
    LaunchedEffect(Unit) {
        complaintViewModel.fetchUserComplaints(user?.id)
    }
    
    // Calculate real stats with explicit names to avoid shadowing
    val userReportsList = complaintState.newComplaints + 
                         complaintState.inProgressComplaints + 
                         complaintState.resolvedComplaints
    
    val calculatedTotalReports = userReportsList.size
    val calculatedResolvedCount = complaintState.resolvedComplaints.size
    val calculatedTotalUpvotes = userReportsList.sumOf { it.supportCount ?: 0 }

    // Map existing user data to the new profile structure
    val profile = UserProfile(
        name = user?.fullName ?: "Citizen",
        email = user?.email ?: "N/A",
        phone = user?.address ?: "",
        district = user?.homeDistrict ?: "",
        memberSince = "Citizen Advocate",
        reportsCount = calculatedTotalReports,
        resolvedCount = calculatedResolvedCount,
        upvotesCount = calculatedTotalUpvotes,
        role = if (user?.isAdmin == true) "System Administrator" else "Citizen Advocate",
        initials = user?.fullName?.take(2)?.uppercase() ?: "C"
    )

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLogoutDialog  by remember { mutableStateOf(false) }
    var notifEnabled      by remember { mutableStateOf(profile.notificationsEnabled) }
    var darkEnabled       by remember { mutableStateOf(profile.darkModeEnabled) }

    // Animate stats on entry
    var statsVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { statsVisible = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "CivicResolve",
                        fontWeight = FontWeight.SemiBold,
                        color      = NavyPrimary,
                        fontSize   = 18.sp,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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

            // ── Avatar + name + badge ─────────────────────────────────────────
            AvatarSection(
                initials     = profile.initials.ifBlank {
                    profile.name.take(2).uppercase()
                },
                name         = profile.name,
                role         = profile.role,
                profilePic   = user?.profilePic,
                isLoading    = authState.isLoading,
                onEditClick  = { galleryLauncher.launch("image/*") },
            )

            Spacer(Modifier.height(20.dp))

            // ── Stats row ─────────────────────────────────────────────────────
            AnimatedVisibility(
                visible = statsVisible,
                enter   = fadeIn(tween(600)) + slideInVertically(tween(600)) { fullHeight -> fullHeight / 2 },
            ) {
                StatsRow(
                    reports  = profile.reportsCount,
                    resolved = profile.resolvedCount,
                    upvotes  = profile.upvotesCount,
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Account info ──────────────────────────────────────────────────
            ProfileSection(title = "ACCOUNT INFO") {
                InfoRow(icon = Icons.Outlined.Person,      label = "Full Name",     value = profile.name)
                SectionDivider()
                InfoRow(icon = Icons.Outlined.Email,       label = "Email Address", value = profile.email)
                if (profile.phone.isNotBlank()) {
                    SectionDivider()
                    InfoRow(icon = Icons.Outlined.Phone,   label = "Phone",         value = profile.phone)
                }
                if (profile.district.isNotBlank()) {
                    SectionDivider()
                    InfoRow(icon = Icons.Outlined.LocationOn, label = "District",   value = profile.district)
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Settings ──────────────────────────────────────────────────────
            ProfileSection(title = "SETTINGS") {
                ToggleRow(
                    icon     = Icons.Outlined.Notifications,
                    label    = "Push Notifications",
                    checked  = notifEnabled,
                    onToggle = {
                        notifEnabled = it
                    },
                )
                SectionDivider()
                ToggleRow(
                    icon     = Icons.Outlined.DarkMode,
                    label    = "Dark Mode",
                    checked  = darkEnabled,
                    onToggle = {
                        darkEnabled = it
                    },
                )
                SectionDivider()
                ActionRow(
                    icon    = Icons.Outlined.Lock,
                    label   = "Change Password",
                    onClick = { /* onChangePassword */ },
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Logout button ─────────────────────────────────────────────────
            Button(
                onClick = { showLogoutDialog = true },
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
                    tint   = Color.White,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Logout", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            }

            Spacer(Modifier.height(12.dp))

            // ── Delete account ────────────────────────────────────────────────
            TextButton(
                onClick  = { showDeleteDialog = true },
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                Text(
                    "Delete Account",
                    color    = DangerRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // ── Logout confirmation dialog ────────────────────────────────────────────
    if (showLogoutDialog) {
        CivicAlertDialog(
            icon        = Icons.Default.Logout,
            iconColor   = NavyPrimary,
            title       = "Logout?",
            message     = "You'll need to sign in again to report or track issues.",
            confirmText = "Logout",
            confirmColor = NavyPrimary,
            onConfirm   = { 
                showLogoutDialog = false
                authViewModel.logout(context) 
            },
            onDismiss   = { showLogoutDialog = false },
        )
    }

    // ── Delete account confirmation dialog ────────────────────────────────────
    if (showDeleteDialog) {
        CivicAlertDialog(
            icon        = Icons.Default.DeleteForever,
            iconColor   = DangerRed,
            title       = "Delete Account?",
            message     = "This will permanently delete your account and all your reports. This cannot be undone.",
            confirmText = "Delete",
            confirmColor = DangerRed,
            onConfirm   = { showDeleteDialog = false },
            onDismiss   = { showDeleteDialog = false },
        )
    }
}

// ── Avatar section ────────────────────────────────────────────────────────────
@Composable
private fun AvatarSection(
    initials:    String,
    name:        String,
    role:        String,
    profilePic:  String? = null,
    isLoading:   Boolean = false,
    onEditClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.BottomEnd) {
            // Avatar circle with initials or image
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(NavyPrimary, Color(0xFF1A5C9E))
                        )
                    )
                    .border(3.dp, Color.White, CircleShape)
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
                        fontWeight = FontWeight.SemiBold,
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
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            // Edit pencil button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(TealAccent)
                    .border(2.dp, Color.White, CircleShape)
                    .clickable { onEditClick() }
                    .offset(x = (-4).dp, y = (-4).dp),
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit profile",
                    tint     = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text       = name,
            fontSize   = 20.sp,
            fontWeight = FontWeight.Bold,
            color      = TextPrimary,
        )

        Spacer(Modifier.height(6.dp))

        // Role badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(TealAccent.copy(alpha = 0.15f))
                .border(1.dp, TealAccent.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text(
                text       = "🏅 $role",
                fontSize   = 12.sp,
                fontWeight = FontWeight.Medium,
                color      = Color(0xFF0D6E5A),
            )
        }
    }
}

// ── Stats row ─────────────────────────────────────────────────────────────────
@Composable
private fun StatsRow(reports: Int, resolved: Int, upvotes: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardWhite)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        StatItem(value = reports,  label = "Reports",  icon = "📋")
        StatDivider()
        StatItem(value = resolved, label = "Resolved", icon = "✅")
        StatDivider()
        StatItem(value = upvotes,  label = "Upvotes",  icon = "⭐")
    }
}

@Composable
private fun StatItem(value: Int, label: String, icon: String) {
    // Animate number count-up
    val animatedValue by animateIntAsState(
        targetValue  = value,
        animationSpec = tween(durationMillis = 800, easing = EaseOutCubic),
        label        = "stat_$label",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 18.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            text       = animatedValue.toString(),
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold,
            color      = NavyPrimary,
        )
        Text(
            text     = label,
            fontSize = 11.sp,
            color    = TextSecondary,
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(48.dp)
            .background(DividerColor)
    )
}

// ── Section wrapper ───────────────────────────────────────────────────────────
@Composable
private fun ProfileSection(
    title:   String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text     = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color    = TextSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            letterSpacing = 1.sp,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardWhite),
            content = content,
        )
    }
}

// ── Info row (read-only field) ────────────────────────────────────────────────
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

// ── Action row (tappable) ─────────────────────────────────────────────────────
@Composable
private fun ActionRow(
    icon:    ImageVector,
    label:   String,
    badge:   String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
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
                    .background(NavyPrimary.copy(alpha = 0.08f)),
            ) {
                Icon(icon, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
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
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
    }
}

// ── Toggle row ────────────────────────────────────────────────────────────────
@Composable
private fun ToggleRow(
    icon:     ImageVector,
    label:    String,
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
        Row(verticalAlignment = Alignment.CenterVertically) {
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
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
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

// ── Divider between rows ──────────────────────────────────────────────────────
@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier  = Modifier.padding(start = 64.dp),
        thickness = 0.5.dp,
        color     = DividerColor,
    )
}

// ── Reusable alert dialog ─────────────────────────────────────────────────────
@Composable
private fun CivicAlertDialog(
    icon:         ImageVector,
    iconColor:    Color,
    title:        String,
    message:      String,
    confirmText:  String,
    confirmColor: Color,
    onConfirm:    () -> Unit,
    onDismiss:    () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.1f)),
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(26.dp))
            }
        },
        title = {
            Text(title, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = TextPrimary)
        },
        text = {
            Text(message, fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors  = ButtonDefaults.buttonColors(containerColor = confirmColor),
                shape   = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(confirmText, fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick  = onDismiss,
                shape    = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
                border   = BorderStroke(1.dp, DividerColor),
            ) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = CardWhite,
        shape          = RoundedCornerShape(20.dp),
    )
}
