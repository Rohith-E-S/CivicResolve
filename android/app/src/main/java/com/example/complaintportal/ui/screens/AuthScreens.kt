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

@Composable
fun AnimatedGradientBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF00325B), // Deep Navy
                        Color(0xFF005394), // Action Blue
                        Color(0xFF00325B),
                    ),
                    tileMode = TileMode.Mirror
                )
            )
    ) {
        // Glowing Radial Orbs
        Box(
            modifier = Modifier
                .size(400.dp)
                .offset(x = (-100).dp, y = (-100).dp)
                .graphicsLayer { rotationZ = angle }
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF2b6cb0).copy(alpha = 0.3f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 50.dp, y = 50.dp)
                .graphicsLayer { rotationZ = -angle }
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF64B5F6).copy(alpha = 0.2f), Color.Transparent)
                    )
                )
        )
    }
}

@Composable
fun PremiumAuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    isValid: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val borderColor by animateColorAsState(
        targetValue = when {
            isError -> Color(0xFFD32F2F) // Crimson Red
            isValid -> Color(0xFF4CAF50) // Emerald Green
            else -> Color.White.copy(alpha = 0.3f)
        },
        label = "borderColor"
    )

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        placeholder = { 
            Text(
                placeholder, 
                color = Color.White.copy(alpha = 0.6f), 
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal // Medium to Normal for labels
            ) 
        },
        leadingIcon = leadingIcon,
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                if (isValid) {
                    Icon(
                        Icons.Rounded.CheckCircle, 
                        contentDescription = null, 
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                }
                trailingIcon?.invoke()
            }
        },
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(20.dp),
        keyboardOptions = keyboardOptions,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White.copy(alpha = 0.05f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
            focusedBorderColor = borderColor,
            unfocusedBorderColor = borderColor,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Color.White
        ),
        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium)
    )
}

@Composable
fun PremiumAuthButton(
    text: String,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    Button(
        onClick = {
            if (!isLoading && enabled) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                scope.launch {
                    scale.animateTo(0.95f, tween(100))
                    scale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy))
                    onClick()
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .shadow(
                elevation = if (enabled) 12.dp else 0.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color(0xFF005394)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.White.copy(alpha = 0.1f)
        ),
        contentPadding = PaddingValues(0.dp),
        enabled = enabled && !isLoading
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (enabled) Brush.horizontalGradient(
                        colors = listOf(Color(0xFF00796B), Color(0xFF00ACC1)) // Vibrant Teal/Blue Gradient
                    ) else Brush.linearGradient(listOf(Color.Gray.copy(alpha=0.5f), Color.Gray.copy(alpha=0.5f)))
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White, 
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = text, 
                        fontWeight = FontWeight.ExtraBold, 
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null, 
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumGoogleButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(28.dp), // Pill shape
        color = Color.White,
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp), // Added padding for the logo
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_google_logo),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "Continue with Google",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFF1F1F1F)
            )
        }
    }
}

@Composable
fun PremiumErrorCard(message: String) {
    AnimatedVisibility(
        visible = message.isNotEmpty(),
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFD32F2F).copy(alpha = 0.1f))
                .border(1.dp, Color(0xFFD32F2F).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Warning, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(message, color = Color(0xFFD32F2F), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().height(64.dp),
        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Medium, fontSize = 18.sp) },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(24.dp),
        keyboardOptions = keyboardOptions,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.onBackground,
            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedTextColor = MaterialTheme.colorScheme.onBackground,
            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
            cursorColor = MaterialTheme.colorScheme.onBackground
        ),
        textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, fontWeight = FontWeight.Medium)
    )
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

    // Google Sign-In setup
    val googleGsoWithIdToken = remember(webClientId) { buildGoogleSignInOptions(webClientId) }
    val googleGsoBasic = remember { buildGoogleSignInOptions(null) }
    val googleSignInClientWithIdToken = remember(context, googleGsoWithIdToken) {
        GoogleSignIn.getClient(context, googleGsoWithIdToken)
    }
    val googleSignInClientBasic = remember(context, googleGsoBasic) {
        GoogleSignIn.getClient(context, googleGsoBasic)
    }
    var launchedWithIdToken by remember { mutableStateOf(false) }
    var retryWithoutIdToken by remember { mutableStateOf(false) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val googleRequest = account.toGoogleLoginRequest()
            if (googleRequest != null) {
                viewModel.googleLogin(googleRequest, onLoginSuccess)
            }
            launchedWithIdToken = false
        } catch (e: ApiException) {
            if (e.statusCode == CommonStatusCodes.DEVELOPER_ERROR && launchedWithIdToken) {
                retryWithoutIdToken = true
                launchedWithIdToken = false
            } else {
                viewModel.setError(e.toGoogleAuthMessage(launchedWithIdToken))
                launchedWithIdToken = false
            }
        }
    }

    LaunchedEffect(retryWithoutIdToken) {
        if (retryWithoutIdToken) {
            retryWithoutIdToken = false
            googleSignInLauncher.launch(googleSignInClientBasic.signInIntent)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedGradientBackground()

        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Hero Identity: Civic Shield with Polish Glow
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .graphicsLayer {
                        shadowElevation = 20f
                        shape = CircleShape
                        clip = false
                    }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF64B5F6).copy(alpha = 0.6f), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = "Civic Shield",
                    modifier = Modifier.size(56.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "CivicResolve",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = (-1).sp
            )
            
            val detectedCity = state.detectedDistrict ?: "your community"
            Text(
                "Sign in to protect $detectedCity",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Glassmorphism Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(32.dp))
                    .blur(if (state.isLoading) 10.dp else 0.dp)
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PremiumAuthTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "Official Email",
                        leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null, tint = Color.White.copy(alpha = 0.6f)) },
                        isValid = isEmailValid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    PremiumAuthTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "Password",
                        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color.White.copy(alpha = 0.6f)) },
                        isValid = password.length >= 6,
                        trailingIcon = {
                            val icon = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onNavigateToForgotPassword) {
                            Text("Recover Access?", color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    PremiumAuthButton(
                        text = "Access Portal",
                        onClick = { viewModel.login(LoginRequest(email, password), onLoginSuccess) },
                        isLoading = state.isLoading,
                        enabled = isEmailValid && password.length >= 6
                    )

                    PremiumErrorCard(message = state.error ?: "")

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.1f))
                        Text(
                            "SECURE LOGIN",
                            modifier = Modifier.padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.4f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.1f))
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    PremiumGoogleButton(
                        onClick = {
                            launchedWithIdToken = webClientId.isNotBlank()
                            val client = if (launchedWithIdToken) googleSignInClientWithIdToken else googleSignInClientBasic
                            googleSignInLauncher.launch(client.signInIntent)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            TextButton(onClick = onNavigateToSignup) {
                Row {
                    Text("New here? ", color = Color.White.copy(alpha = 0.7f))
                    Text("Register as Advocate", color = Color.White, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
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

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    var launchedWithIdToken by remember { mutableStateOf(false) }
    var retryWithoutIdToken by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            isFetchingLocation = true
            fetchLocation(context, fusedLocationClient, scope) { fetchedAddress ->
                address = fetchedAddress
                isFetchingLocation = false
            }
        } else {
            viewModel.setError("Location permission denied")
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val googleRequest = account.toGoogleLoginRequest()
            if (googleRequest != null) {
                viewModel.googleLogin(googleRequest, onSignupSuccess)
            }
            launchedWithIdToken = false
        } catch (e: ApiException) {
            if (e.statusCode == CommonStatusCodes.DEVELOPER_ERROR && launchedWithIdToken) {
                retryWithoutIdToken = true
                launchedWithIdToken = false
            } else {
                viewModel.setError(e.toGoogleAuthMessage(launchedWithIdToken))
                launchedWithIdToken = false
            }
        }
    }

    LaunchedEffect(retryWithoutIdToken) {
        if (retryWithoutIdToken) {
            retryWithoutIdToken = false
            googleSignInLauncher.launch(buildGoogleSignInOptions(null).let { GoogleSignIn.getClient(context, it).signInIntent })
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedGradientBackground()

        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Hero Identity: Civic Shield with Polish Glow
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .graphicsLayer {
                        shadowElevation = 15f
                        shape = CircleShape
                        clip = false
                    }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF81C784).copy(alpha = 0.6f), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = "Civic Shield",
                    modifier = Modifier.size(44.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Join the Mission",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = (-1).sp
            )
            
            val subtitle by remember(address) {
                derivedStateOf {
                    if (address.isNotBlank()) "Registering to protect ${address.split(",").lastOrNull()?.trim() ?: "your community"}"
                    else "Register as a local advocate"
                }
            }

            AnimatedContent(
                targetState = subtitle,
                transitionSpec = {
                    fadeIn(animationSpec = tween(600)) togetherWith fadeOut(animationSpec = tween(600))
                },
                label = "subtitleFade"
            ) { targetText ->
                Text(
                    targetText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Glassmorphism Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(32.dp))
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PremiumAuthTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        placeholder = "Full Name",
                        leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null, tint = Color.White.copy(alpha = 0.6f)) },
                        isValid = fullName.length >= 3
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    PremiumAuthTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "Email Address",
                        leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null, tint = Color.White.copy(alpha = 0.6f)) },
                        isValid = isEmailValid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    PremiumAuthTextField(
                        value = address,
                        onValueChange = { address = it },
                        placeholder = "Residential Area / District",
                        leadingIcon = { Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = Color.White.copy(alpha = 0.6f)) },
                        trailingIcon = {
                            if (isFetchingLocation) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .graphicsLayer { alpha = pulseAlpha }, 
                                    strokeWidth = 2.dp, 
                                    color = Color(0xFF2196F3) // Vibrant Pulse Blue
                                )
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
                                    Icon(Icons.Rounded.GpsFixed, contentDescription = "Verify District", tint = Color.White.copy(alpha = 0.6f))
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    PremiumAuthTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "Security Password",
                        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color.White.copy(alpha = 0.6f)) },
                        isValid = isPasswordValid,
                        trailingIcon = {
                            val icon = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    PremiumAuthButton(
                        text = "Initialize Account",
                        onClick = {
                            viewModel.pendingSignupRequest = CreateAccountRequest(fullName, email, password, address)
                            viewModel.sendOtp(email) {
                                onNavigateToOtpVerify(email)
                            }
                        },
                        isLoading = state.isLoading,
                        enabled = isFormValid
                    )

                    PremiumErrorCard(message = state.error ?: "")

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.1f))
                        Text(
                            "QUICK REGISTER",
                            modifier = Modifier.padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.4f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.1f))
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    PremiumGoogleButton(
                        onClick = {
                            launchedWithIdToken = webClientId.isNotBlank()
                            val client = if (launchedWithIdToken) GoogleSignIn.getClient(context, buildGoogleSignInOptions(webClientId)) else GoogleSignIn.getClient(context, buildGoogleSignInOptions(null))
                            googleSignInLauncher.launch(client.signInIntent)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onNavigateToLogin) {
                Row {
                    Text("Already registered? ", color = Color.White.copy(alpha = 0.7f))
                    Text("Enter Portal", color = Color.White, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                text = "Forgot Password",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = (-0.5).sp
                ),
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        when (currentStep) {
            ForgotPasswordStep.ENTER_EMAIL -> {
                Text(
                    text = "Enter your email address to receive a password reset OTP.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                )
                AuthTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "Email",
                    leadingIcon = { Icon(Icons.Default.MailOutline, contentDescription = null, tint = MaterialTheme.colorScheme.outline) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = {
                        if (email.isNotBlank()) {
                            viewModel.sendPasswordResetOtp(email) {
                                currentStep = ForgotPasswordStep.ENTER_OTP
                            }
                        } else {
                            viewModel.setError("Please enter your email")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    enabled = !state.isLoading
                ) {
                    Text(if (state.isLoading) "Sending OTP..." else "Send OTP", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
            ForgotPasswordStep.ENTER_OTP -> {
                Text(
                    text = "Enter the OTP sent to $email",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                )
                AuthTextField(
                    value = otp,
                    onValueChange = { otp = it },
                    placeholder = "Enter OTP",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
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
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    enabled = !state.isLoading
                ) {
                    Text(if (state.isLoading) "Verifying..." else "Verify OTP", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
            ForgotPasswordStep.ENTER_NEW_PASSWORD -> {
                Text(
                    text = "Create a new password.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                )
                AuthTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    placeholder = "New Password",
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.outline) },
                    trailingIcon = {
                        val icon = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(icon, contentDescription = "Toggle password visibility", tint = MaterialTheme.colorScheme.outline)
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
                )
                Spacer(modifier = Modifier.height(20.dp))
                AuthTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    placeholder = "Confirm New Password",
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.outline) },
                    visualTransformation = PasswordVisualTransformation()
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = {
                        if (newPassword == confirmPassword && newPassword.isNotBlank()) {
                            viewModel.resetPassword(
                                com.example.complaintportal.data.model.ResetPasswordRequest(newPassword, resetToken)
                            ) {
                                onPasswordResetSuccess()
                            }
                        } else {
                            viewModel.setError("Passwords do not match or are empty")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    enabled = !state.isLoading
                ) {
                    Text(if (state.isLoading) "Resetting..." else "Reset Password", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }

        if (state.error != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = state.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                text = "Verify Email",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = (-0.5).sp
                ),
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        Text(
            text = "Enter the 6-digit OTP sent to $email",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp
            ),
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
        )
        
        AuthTextField(
            value = otp,
            onValueChange = { otp = it },
            placeholder = "Enter OTP",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = {
                if (otp.isNotBlank()) {
                    viewModel.verifyOtp(email, otp) {
                        val request = viewModel.pendingSignupRequest
                        if (request != null) {
                            viewModel.createAccount(request, onVerifySuccess)
                        } else {
                            viewModel.setError("Session expired. Please try signing up again.")
                        }
                    }
                } else {
                    viewModel.setError("Please enter the OTP")
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            enabled = !state.isLoading
        ) {
            Text(if (state.isLoading) "Verifying..." else "Verify Account", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        if (state.error != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = state.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}
