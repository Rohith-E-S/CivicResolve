package com.example.complaintportal.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.complaintportal.ui.viewmodel.AuthViewModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: AuthViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.authState.collectAsState()
    val user = state.user
    val context = LocalContext.current

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

                viewModel.updateProfile(fullNameReq, addressReq, imagePart) {}
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

    Scaffold(
        containerColor = Color(0xFFF9F9F9),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "CivicResolve", 
                        fontWeight = FontWeight.Bold, 
                        color = Color(0xFF1A1A1A),
                        fontSize = 18.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .clip(CircleShape)
                            .background(Color.Transparent)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF1A1A1A))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF9F9F9)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Info Section
            Box(
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .size(136.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(128.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE5E7EB))
                        .border(4.dp, Color(0xFFF9F9F9), CircleShape)
                        .align(Alignment.Center)
                        .clickable(enabled = !state.isLoading) { galleryLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (!user?.profilePic.isNullOrEmpty()) {
                        Image(
                            painter = rememberAsyncImagePainter(user?.profilePic),
                            contentDescription = "Profile Picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Default.Person, 
                            contentDescription = null, 
                            modifier = Modifier.size(60.dp), 
                            tint = Color(0xFF9CA3AF)
                        )
                    }
                    
                    if (state.isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha=0.3f)), 
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-4).dp, y = (-4).dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                        .border(2.dp, Color(0xFFF9F9F9), CircleShape)
                        .clickable { galleryLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Edit, 
                        contentDescription = "Edit Profile", 
                        tint = Color.White, 
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = user?.fullName ?: "Citizen", 
                style = MaterialTheme.typography.titleLarge, 
                fontWeight = FontWeight.Bold, 
                color = Color(0xFF1A1A1A),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            Text(
                text = if (user?.isAdmin == true) "System Administrator" else "Citizen Advocate", 
                style = MaterialTheme.typography.bodyMedium, 
                color = Color(0xFF494949),
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(24.dp))

            // User Details Cards
            Column(
                modifier = Modifier.fillMaxWidth(), 
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoCard("Full Name", user?.fullName ?: "N/A")
                InfoCard("Email Address", user?.email ?: "N/A")
            }

            Spacer(modifier = Modifier.weight(1f))

            // Logout Button
            Button(
                onClick = { viewModel.logout() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFB3261E),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun InfoCard(label: String, value: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(16.dp), clip = false)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFF4F3F3), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column {
            Text(
                text = label.uppercase(), 
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold, 
                color = Color(0xFF9CA3AF),
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value, 
                fontSize = 14.sp, 
                fontWeight = FontWeight.SemiBold, 
                color = Color(0xFF1A1A1A)
            )
        }
    }
}
