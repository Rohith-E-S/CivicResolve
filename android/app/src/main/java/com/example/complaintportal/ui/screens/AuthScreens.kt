package com.example.complaintportal.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.example.complaintportal.R
import coil.compose.rememberAsyncImagePainter
import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import com.example.complaintportal.data.model.CreateAccountRequest
import com.example.complaintportal.data.model.GoogleLoginRequest
import com.example.complaintportal.data.model.LoginRequest
import com.example.complaintportal.ui.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes

// Background with subtle top gradient
@Composable
fun CleanBackground(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Subtle top gradient covering top ~25% of screen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.28f)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFEAF2FF), Color(0xFFF4F6F8))
                    )
                )
        )
        // Rest of screen is neutral
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF4F6F8))
        )
        // Re-draw gradient on top so it blends
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.28f)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFEAF2FF), Color(0x00F4F6F8))
                    )
                )
        )
        content()
    }
}

@Composable
fun CleanAuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    isSuccess: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    supportingText: String? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
            color = Color(0xFF374151),
            modifier = Modifier.padding(bottom = 6.dp, start = 2.dp),
            fontWeight = FontWeight.Bold
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    placeholder,
                    color = Color(0xFFB0B9C5),
                    fontSize = 14.sp
                )
            },
            leadingIcon = leadingIcon,
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSuccess) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp).padding(end = 4.dp)
                        )
                    }
                    trailingIcon?.invoke()
                }
            },
            visualTransformation = visualTransformation,
            shape = RoundedCornerShape(10.dp),
            keyboardOptions = keyboardOptions,
            singleLine = true,
            isError = isError,
            supportingText = supportingText?.let { { Text(it, fontSize = 11.sp, color = Color(0xFF9CA3AF)) } },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2D6CDF),
                unfocusedBorderColor = Color(0xFF9CA3AF),
                errorBorderColor = Color(0xFFDC2626),
                focusedLabelColor = Color(0xFF2D6CDF),
                unfocusedLabelColor = Color(0xFF6B7280),
                focusedTextColor = Color(0xFF111827),
                unfocusedTextColor = Color(0xFF111827),
                cursorColor = Color(0xFF2D6CDF),
                // Lightly tinted field background
                focusedContainerColor = Color(0xFFF0F6FF),
                unfocusedContainerColor = Color(0xFFF9FAFB)
            ),
            textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, fontWeight = FontWeight.Normal)
        )
    }
}

@Composable
fun CleanPrimaryButton(
    text: String,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(
                elevation = if (enabled && !isLoading) 4.dp else 0.dp,
                shape = RoundedCornerShape(10.dp)
            ),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF2D6CDF),
            contentColor = Color.White,
            disabledContainerColor = Color(0xFFD1D5DB),
            disabledContentColor = Color(0xFF9CA3AF)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 2.dp),
        enabled = enabled && !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = 0.3.sp
            )
        }
    }
}

@Composable
fun CleanSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = Color(0xFF6B7280)
        )
    }
}

@Composable
fun CleanGoogleButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFF9CA3AF)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color(0xFF1F2937)
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_google_logo),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "Continue with Google",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun LogoShield() {
    Box(
        modifier = Modifier
            .size(76.dp)
            .background(Color(0xFFDCEAFF), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(60.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Shield,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                tint = Color(0xFF1A3A6E)
            )
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                modifier = Modifier.size(28.dp).padding(top = 4.dp),
                tint = Color(0xFF7ECFC0)
            )
        }
    }
}

@Composable
fun IssueChips() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 12.dp)
    ) {
        listOf("🚧 Potholes", "🗑 Garbage", "💡 Streetlights").forEach { label ->
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = Color(0xFFE8F0FE),
                border = BorderStroke(1.dp, Color(0xFFBFD3F5))
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = Color(0xFF3A5FA0),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun CleanErrorCard(message: String) {
    AnimatedVisibility(
        visible = message.isNotEmpty(),
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFD32F2F).copy(alpha = 0.05f))
                .border(1.dp, Color(0xFFD32F2F).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(message, color = Color(0xFFD32F2F), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

private fun fetchLocation(
    context: android.content.Context,
    fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient,
    scope: kotlinx.coroutines.CoroutineScope,
    onResult: (String) -> Unit
) {
    try {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                scope.launch(Dispatchers.IO) {
                    try {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val addr = addresses[0]
                            val fullAddress = listOfNotNull(
                                addr.featureName,
                                addr.thoroughfare,
                                addr.subLocality,
                                addr.locality,
                                addr.adminArea
                            ).filter { it.isNotBlank() }.distinct().joinToString(", ")
                            launch(Dispatchers.Main) { onResult(fullAddress.ifBlank { "Coordinates: ${location.latitude}, ${location.longitude}" }) }
                        } else {
                            launch(Dispatchers.Main) { onResult("Coordinates: ${location.latitude}, ${location.longitude}") }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        launch(Dispatchers.Main) { onResult("Coordinates: ${location.latitude}, ${location.longitude}") }
                    }
                }
            } else {
                onResult("Unable to fetch location. Turn on GPS.")
            }
        }
    } catch (e: SecurityException) {
        onResult("Location permission denied")
    }
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToSignup: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val state by viewModel.authState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    val isEmailValid by remember { derivedStateOf { email.contains("@") && email.contains(".") } }
    
    val context = LocalContext.current
    val webClientId = remember(context) { context.getString(R.string.google_web_client_id).trim() }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account.toGoogleLoginRequest()?.let { viewModel.googleLogin(it, onLoginSuccess) }
        } catch (e: ApiException) {
            viewModel.setError("Google sign in failed")
        }
    }

    CleanBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Logo & App Name
            LogoShield()
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "CivicResolve",
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp),
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A3A6E)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Sign in to report or track issues in your area",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "Report issues in your area in seconds.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = Color(0xFF9CA3AF),
                textAlign = TextAlign.Center
            )

            // Category chips
            IssueChips()

            Spacer(modifier = Modifier.height(20.dp))

            // Form Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CleanAuthTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email Address",
                        placeholder = "e.g. name@example.com",
                        leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(20.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        isSuccess = isEmailValid && email.isNotBlank()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    CleanAuthTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        placeholder = "Enter your password",
                        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            val icon = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(icon, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(20.dp))
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        supportingText = "Minimum 6 characters",
                        isSuccess = password.length >= 6
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onNavigateToForgotPassword) {
                            Text("Forgot Password?", color = Color(0xFF1A3A6E), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    CleanPrimaryButton(
                        text = "Sign In",
                        onClick = { viewModel.login(LoginRequest(email, password), onLoginSuccess) },
                        isLoading = state.isLoading,
                        enabled = isEmailValid && password.length >= 6
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Continue as Guest
                    CleanSecondaryButton(
                        text = "Continue as Guest",
                        onClick = onLoginSuccess
                    )

                    CleanErrorCard(message = state.error ?: "")

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE5E7EB))
                        Text(
                            "or",
                            modifier = Modifier.padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9CA3AF)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE5E7EB))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    CleanGoogleButton(
                        onClick = {
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestEmail()
                                .apply { if (webClientId.isNotBlank()) requestIdToken(webClientId) }
                                .build()
                            googleSignInLauncher.launch(GoogleSignIn.getClient(context, gso).signInIntent)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onNavigateToSignup) {
                Row {
                    Text("New here? ", color = Color(0xFF6B7280))
                    Text("Create Account", color = Color(0xFF1A3A6E), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToOtpVerify: (String) -> Unit,
    onSignupSuccess: () -> Unit
) {
    val state by viewModel.authState.collectAsState()
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val isEmailValid by remember { derivedStateOf { email.contains("@") && email.contains(".") } }
    val isPasswordValid by remember { derivedStateOf { password.length >= 6 } }
    val isFormValid by remember { derivedStateOf { fullName.isNotBlank() && isEmailValid && isPasswordValid && address.isNotBlank() } }

    val context = LocalContext.current
    val webClientId = remember(context) { context.getString(R.string.google_web_client_id).trim() }

    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var isFetchingLocation by remember { mutableStateOf(false) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account.toGoogleLoginRequest()?.let { viewModel.googleLogin(it, onSignupSuccess) }
        } catch (e: ApiException) {
            viewModel.setError("Google sign in failed")
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            isFetchingLocation = true
            fetchLocation(context, fusedLocationClient, scope) { fetchedAddress ->
                address = fetchedAddress
                isFetchingLocation = false
            }
        }
    }

    CleanBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(52.dp))

            // Logo & App Name
            LogoShield()
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "CivicResolve",
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp),
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A3A6E)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Create an account to report issues in your area",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Help improve your community.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = Color(0xFF9CA3AF),
                textAlign = TextAlign.Center
            )

            // Category chips
            IssueChips()

            Spacer(modifier = Modifier.height(28.dp))

            // Form Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CleanAuthTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = "Full Name",
                        placeholder = "e.g. John Doe",
                        leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(20.dp)) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    CleanAuthTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email Address",
                        placeholder = "name@example.com",
                        leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(20.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    CleanAuthTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = "Residential Area / District",
                        placeholder = "Area, District",
                        leadingIcon = { Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            if (isFetchingLocation) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF1A3A6E))
                            } else {
                                IconButton(onClick = {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                        isFetchingLocation = true
                                        fetchLocation(context, fusedLocationClient, scope) { fetchedAddress ->
                                            address = fetchedAddress
                                            isFetchingLocation = false
                                        }
                                    } else {
                                        locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                                    }
                                }) {
                                    Icon(Icons.Rounded.GpsFixed, contentDescription = "Get Location", tint = Color(0xFF1A3A6E), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    CleanAuthTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        placeholder = "Create a password",
                        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            val icon = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(icon, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(20.dp))
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        supportingText = "Minimum 6 characters"
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    CleanPrimaryButton(
                        text = "Create Account",
                        onClick = {
                            viewModel.pendingSignupRequest = CreateAccountRequest(fullName, email, password, address)
                            viewModel.sendOtp(email) { onNavigateToOtpVerify(email) }
                        },
                        isLoading = state.isLoading,
                        enabled = isFormValid
                    )

                    CleanErrorCard(message = state.error ?: "")

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE5E7EB))
                        Text(
                            "or",
                            modifier = Modifier.padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9CA3AF)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE5E7EB))
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    CleanGoogleButton(
                        onClick = {
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestEmail()
                                .apply { if (webClientId.isNotBlank()) requestIdToken(webClientId) }
                                .build()
                            googleSignInLauncher.launch(GoogleSignIn.getClient(context, gso).signInIntent)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onNavigateToLogin) {
                Row {
                    Text("Already have an account? ", color = Color(0xFF6B7280))
                    Text("Sign In", color = Color(0xFF1A3A6E), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

private fun GoogleSignInAccount.toGoogleLoginRequest(): GoogleLoginRequest? {
    val emailValue = email?.trim().orEmpty()
    if (emailValue.isBlank()) return null

    val googleIdValue = id?.trim().orEmpty().ifBlank { emailValue }
    val fullNameValue = displayName?.trim().orEmpty().ifBlank { emailValue.substringBefore("@") }

    return GoogleLoginRequest(
        email = emailValue,
        fullName = fullNameValue,
        profilePic = photoUrl?.toString(),
        googleId = googleIdValue
    )
}

private fun ApiException.toGoogleAuthMessage(wasUsingIdToken: Boolean): String {
    return when (statusCode) {
        GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "Google sign in was cancelled"
        GoogleSignInStatusCodes.SIGN_IN_FAILED -> "Google sign in failed. Please try again."
        CommonStatusCodes.NETWORK_ERROR -> "Network error during Google sign in"
        CommonStatusCodes.DEVELOPER_ERROR -> {
            if (wasUsingIdToken) {
                "Google OAuth Web client mismatch. Verify google_web_client_id belongs to this Firebase project."
            } else {
                "Google sign in config mismatch. Add app SHA-1/SHA-256, keep package name com.example.complaintportal, and ensure the Android OAuth client matches this build signature."
            }
        }
        else -> "Google sign in failed: $statusCode"
    }
}

private fun buildGoogleSignInOptions(webClientId: String?): GoogleSignInOptions {
    return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestProfile()
        .apply {
            if (!webClientId.isNullOrBlank()) {
                requestIdToken(webClientId)
            }
        }
        .build()
}

enum class ForgotPasswordStep {
    ENTER_EMAIL, ENTER_OTP, ENTER_NEW_PASSWORD
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    viewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onPasswordResetSuccess: () -> Unit
) {
    val state by viewModel.authState.collectAsState()
    var currentStep by remember { mutableStateOf(ForgotPasswordStep.ENTER_EMAIL) }
    
    var email by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var resetToken by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    CleanBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF1F2937))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Reset Password",
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A3A6E)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (currentStep) {
                        ForgotPasswordStep.ENTER_EMAIL -> {
                            Text(
                                text = "Enter your email address to receive a password reset OTP.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                color = Color(0xFF6B7280),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                            )
                            CleanAuthTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = "Email Address",
                                placeholder = "name@example.com",
                                leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(20.dp)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            CleanPrimaryButton(
                                text = "Send OTP",
                                onClick = {
                                    if (email.isNotBlank()) {
                                        viewModel.sendPasswordResetOtp(email) {
                                            currentStep = ForgotPasswordStep.ENTER_OTP
                                        }
                                    } else {
                                        viewModel.setError("Please enter your email")
                                    }
                                },
                                isLoading = state.isLoading
                            )
                        }
                        ForgotPasswordStep.ENTER_OTP -> {
                            Text(
                                text = "Enter the verification code sent to $email",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                color = Color(0xFF6B7280),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                            )
                            CleanAuthTextField(
                                value = otp,
                                onValueChange = { otp = it },
                                label = "Verification Code",
                                placeholder = "6-digit code",
                                leadingIcon = { Icon(Icons.Rounded.LockOpen, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(20.dp)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            CleanPrimaryButton(
                                text = "Verify Code",
                                onClick = {
                                    if (otp.isNotBlank()) {
                                        viewModel.verifyPasswordResetOtp(email, otp) { token ->
                                            resetToken = token
                                            currentStep = ForgotPasswordStep.ENTER_NEW_PASSWORD
                                        }
                                    } else {
                                        viewModel.setError("Please enter the OTP")
                                    }
                                },
                                isLoading = state.isLoading
                            )
                        }
                        ForgotPasswordStep.ENTER_NEW_PASSWORD -> {
                            Text(
                                text = "Create a strong new password for your account.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                color = Color(0xFF6B7280),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                            )
                            CleanAuthTextField(
                                value = newPassword,
                                onValueChange = { newPassword = it },
                                label = "New Password",
                                placeholder = "Minimum 6 characters",
                                leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(20.dp)) },
                                trailingIcon = {
                                    val icon = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(icon, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(20.dp))
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            CleanAuthTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = "Confirm Password",
                                placeholder = "Repeat new password",
                                leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(20.dp)) },
                                visualTransformation = PasswordVisualTransformation()
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            CleanPrimaryButton(
                                text = "Reset Password",
                                onClick = {
                                    if (newPassword == confirmPassword && newPassword.length >= 6) {
                                        viewModel.resetPassword(
                                            com.example.complaintportal.data.model.ResetPasswordRequest(newPassword, resetToken)
                                        ) {
                                            onPasswordResetSuccess()
                                        }
                                    } else {
                                        viewModel.setError("Passwords do not match or are too short")
                                    }
                                },
                                isLoading = state.isLoading
                            )
                        }
                    }
                    CleanErrorCard(message = state.error ?: "")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpVerifyScreen(
    viewModel: AuthViewModel,
    email: String,
    onNavigateBack: () -> Unit,
    onVerifySuccess: () -> Unit
) {
    val state by viewModel.authState.collectAsState()
    var otp by remember { mutableStateOf("") }

    CleanBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF1F2937))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Verify Account",
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A3A6E)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VerifiedUser,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color(0xFF7ECFC0)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Check your email",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "We sent a 6-digit verification code to $email",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    CleanAuthTextField(
                        value = otp,
                        onValueChange = { otp = it },
                        label = "Verification Code",
                        placeholder = "000000",
                        leadingIcon = { Icon(Icons.Rounded.LockOpen, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(20.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    CleanPrimaryButton(
                        text = "Verify & Create Account",
                        onClick = {
                            if (otp.isNotBlank()) {
                                viewModel.verifyOtp(email, otp) {
                                    viewModel.pendingSignupRequest?.let { request ->
                                        viewModel.createAccount(request, onVerifySuccess)
                                    } ?: viewModel.setError("Session expired. Please try signing up again.")
                                }
                            } else {
                                viewModel.setError("Please enter the OTP")
                            }
                        },
                        isLoading = state.isLoading
                    )

                    CleanErrorCard(message = state.error ?: "")
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    TextButton(onClick = { viewModel.sendOtp(email) {} }) {
                        Text("Resend Code", color = Color(0xFF1A3A6E), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
