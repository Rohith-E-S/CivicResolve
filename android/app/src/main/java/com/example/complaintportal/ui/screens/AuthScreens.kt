package com.example.complaintportal.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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


private val Slate900 = Color(0xFF0F172A)
private val Slate700 = Color(0xFF334155)
private val Slate500 = Color(0xFF64748B)
private val Slate300 = Color(0xFFCBD5E1)
private val Slate200 = Color(0xFFE2E8F0)
private val BgColor = Color(0xFFF7F7F7)

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
        placeholder = { Text(placeholder, color = Slate500, fontWeight = FontWeight.Medium, fontSize = 18.sp) },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(24.dp),
        keyboardOptions = keyboardOptions,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = Slate900,
            unfocusedBorderColor = Slate200,
            focusedTextColor = Slate900,
            unfocusedTextColor = Slate900,
            cursorColor = Slate900
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
            if (googleRequest == null) {
                viewModel.setError("Google sign in failed: missing account details")
                return@rememberLauncherForActivityResult
            }
            viewModel.googleLogin(googleRequest, onLoginSuccess)
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
            viewModel.setError("Retrying Google sign in with basic account flow...")
            googleSignInLauncher.launch(googleSignInClientBasic.signInIntent)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(38.dp))
        
        // Help Icon
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Slate200),
                contentAlignment = Alignment.Center
            ) {
//                Icon(Icons.Default.HelpOutline, contentDescription = "Help", tint = Slate500)
            }
        }
        
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp)) {
            Text(
                text = "Welcome back",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Slate900,
                    letterSpacing = (-0.5).sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Sign in to continue your civic journey.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate700,
                    letterSpacing = 0.5.sp
                )
            )
        }

        // Email Field
        AuthTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = "Email",
            leadingIcon = { Icon(Icons.Default.MailOutline, contentDescription = null, tint = Slate500) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Password Field
        AuthTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = "Password",
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Slate500) },
            trailingIcon = {
                val icon = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(icon, contentDescription = "Toggle password visibility", tint = Slate500)
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
        )

        // Forgot Password
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = onNavigateToForgotPassword,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = Slate200,
                    contentColor = Slate900
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Forgot Password?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.login(LoginRequest(email, password), onLoginSuccess) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Slate900,
                contentColor = Color.White
            ),
            enabled = !state.isLoading
        ) {
            Text(if (state.isLoading) "Loading..." else "Sign In", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNavigateToSignup,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Slate200,
                contentColor = Slate900
            )
        ) {
            Text("Create Account", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Digital ID Divider
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = Slate200)
            Text(
                text = "DIGITAL ID",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.labelSmall,
                color = Slate500,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = Slate200)
        }

        // Social Logins
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            Surface(
                onClick = {
                    val useIdToken = webClientId.isNotBlank()
                    launchedWithIdToken = useIdToken
                    val client = if (useIdToken) googleSignInClientWithIdToken else googleSignInClientBasic
                    googleSignInLauncher.launch(client.signInIntent)
                },
                shape = CircleShape,
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFF1F5F9)), // slate-100
                shadowElevation = 2.dp,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_google_logo),
                        contentDescription = "Google",
                        modifier = Modifier.size(24.dp)
                    )
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

    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var isFetchingLocation by remember { mutableStateOf(false) }

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
            if (googleRequest == null) {
                viewModel.setError("Google sign in failed: missing account details")
                return@rememberLauncherForActivityResult
            }
            viewModel.googleLogin(googleRequest, onSignupSuccess)
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
            viewModel.setError("Retrying Google sign in with basic account flow...")
            googleSignInLauncher.launch(googleSignInClientBasic.signInIntent)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp)) {
            Text(
                text = "Complete Profile",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Slate900,
                    letterSpacing = (-0.5).sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Last step to join your community.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate700,
                    letterSpacing = 0.5.sp
                )
            )
        }

        AuthTextField(
            value = fullName,
            onValueChange = { fullName = it },
            placeholder = "Full Name",
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Slate500) }
        )
        Spacer(modifier = Modifier.height(20.dp))
        
        AuthTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = "Email",
            leadingIcon = { Icon(Icons.Default.MailOutline, contentDescription = null, tint = Slate500) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        Spacer(modifier = Modifier.height(20.dp))
        
        AuthTextField(
            value = address,
            onValueChange = { address = it },
            placeholder = "Residential Address",
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Slate500) },
            trailingIcon = {
                if (isFetchingLocation) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Slate900)
                } else {
                    IconButton(onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            isFetchingLocation = true
                            fetchLocation(context, fusedLocationClient, scope) { fetchedAddress ->
                                address = fetchedAddress
                                isFetchingLocation = false
                            }
                        } else {
                            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                        }
                    }) {
                        Icon(Icons.Default.GpsFixed, contentDescription = "Get Location", tint = Slate500)
                    }
                }
            }
        )
        Spacer(modifier = Modifier.height(20.dp))

        AuthTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = "Password",
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Slate500) },
            trailingIcon = {
                val icon = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(icon, contentDescription = "Toggle password visibility", tint = Slate500)
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (fullName.isNotBlank() && email.isNotBlank() && password.isNotBlank() && address.isNotBlank()) {
                    viewModel.pendingSignupRequest = CreateAccountRequest(fullName, email, password, address)
                    viewModel.sendOtp(email) {
                        onNavigateToOtpVerify(email)
                    }
                } else {
                    viewModel.setError("Please fill all fields")
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Slate900,
                contentColor = Color.White
            ),
            enabled = !state.isLoading
        ) {
            Text(if (state.isLoading) "Sending OTP..." else "Continue with email", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // OR Divider
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = Slate200)
            Text(
                text = "OR",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.labelSmall,
                color = Slate500,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = Slate200)
        }

        // Social Login
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            Surface(
                onClick = {
                    val useIdToken = webClientId.isNotBlank()
                    launchedWithIdToken = useIdToken
                    val client = if (useIdToken) googleSignInClientWithIdToken else googleSignInClientBasic
                    googleSignInLauncher.launch(client.signInIntent)
                },
                shape = CircleShape,
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                shadowElevation = 2.dp,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_google_logo),
                        contentDescription = "Google",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(
            onClick = onNavigateToLogin,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Already have an account? Login", color = Slate900, fontWeight = FontWeight.Bold)
        }

        if (state.error != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = state.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
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
            .background(BgColor)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack, modifier = Modifier.background(Slate200, CircleShape)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Slate900)
            }
            Text(
                text = "Forgot Password",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Slate900,
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
                        color = Slate700,
                        letterSpacing = 0.5.sp
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                )
                AuthTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "Email",
                    leadingIcon = { Icon(Icons.Default.MailOutline, contentDescription = null, tint = Slate500) },
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
                        containerColor = Slate900,
                        contentColor = Color.White
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
                        color = Slate700,
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
                        containerColor = Slate900,
                        contentColor = Color.White
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
                        color = Slate700,
                        letterSpacing = 0.5.sp
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                )
                AuthTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    placeholder = "New Password",
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Slate500) },
                    trailingIcon = {
                        val icon = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(icon, contentDescription = "Toggle password visibility", tint = Slate500)
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
                )
                Spacer(modifier = Modifier.height(20.dp))
                AuthTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    placeholder = "Confirm New Password",
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Slate500) },
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
                        containerColor = Slate900,
                        contentColor = Color.White
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
            .background(BgColor)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack, modifier = Modifier.background(Slate200, CircleShape)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Slate900)
            }
            Text(
                text = "Verify Email",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Slate900,
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
                color = Slate700,
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
                containerColor = Slate900,
                contentColor = Color.White
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
