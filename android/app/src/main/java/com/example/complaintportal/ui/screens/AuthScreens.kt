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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

// --- Styled Components ---

@Composable
fun AuthBackground(imageModel: Any, content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = rememberAsyncImagePainter(imageModel),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent, Color.Black.copy(alpha = 0.2f)),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )
        content()
    }
}

@Composable
fun AuthContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 32.dp, vertical = 40.dp)
                .fillMaxWidth(),
            content = content
        )
    }
}

@Composable
fun UnderlinedAuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .height(44.dp)
        ) {
            if (leadingIcon != null) {
                Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                    leadingIcon()
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                ),
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                singleLine = true,
                cursorBrush = Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary)),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        innerTextField()
                    }
                }
            )
            if (trailingIcon != null) {
                trailingIcon()
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(top = 4.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
    }
}

@Composable
fun OrangeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(4.dp, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.5f)
        ),
        enabled = enabled && !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = contentColor, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        } else {
            Text(text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SocialLoginButtons(modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().padding(top = 20.dp)
    ) {
        SocialCircleButton(
            icon = {
                Image(
                    painter = painterResource(id = R.drawable.ic_google_logo),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            },
            onClick = {}
        )
    }
}

@Composable
fun SocialCircleButton(
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    backgroundColor: Color = MaterialTheme.colorScheme.surface
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = backgroundColor,
        shadowElevation = 2.dp,
        modifier = Modifier.size(50.dp),
        border = if (backgroundColor == MaterialTheme.colorScheme.surface) BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            icon()
        }
    }
}

@Composable
fun AppLogoIcon() {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.size(56.dp),
        shadowElevation = 4.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Rounded.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp)
            )
        }
    }
}

@Composable
fun FixedHeaderContent(
    modifier: Modifier = Modifier,
    title: String = "For a Better\nCommunity"
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp)
    ) {
//        Spacer(modifier = Modifier.height(60.dp))
//        AppLogoIcon()
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineLarge.copy(
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 38.sp,
                lineHeight = 46.sp
            )
        )
    }
}

// --- Screens ---



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
    val scrollState = rememberScrollState()

    AuthBackground(imageModel = R.drawable.login_bg) {
        // FIXED HEADER: This stays behind the scrollable column
        FixedHeaderContent()

        // SCROLLABLE CONTENT: This will move up and overlap the header
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Spacer to initially push the AuthContainer below the fixed header
            Spacer(modifier = Modifier.height(320.dp))

            AuthContainer {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Welcome back",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 32.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                UnderlinedAuthTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
                    leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) }
                )

                UnderlinedAuthTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    TextButton(onClick = onNavigateToForgotPassword) {
                        Text(
                            "Forgot Password?", 
                            color = MaterialTheme.colorScheme.primary, 
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                OrangeButton(
                    text = "Sign in",
                    onClick = { viewModel.login(LoginRequest(email, password), onLoginSuccess) },
                    isLoading = state.isLoading
                )

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    "or sign in with",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )

                SocialLoginButtons(modifier = Modifier.align(Alignment.CenterHorizontally))
                
                if (state.error != null) {
                    Text(
                        state.error!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("New here? ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Text(
                        "Create Account",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { onNavigateToSignup() }
                    )
                }
            }
        }
    }
}

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
    val scrollState = rememberScrollState()

    val context = LocalContext.current
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
        }
    }

    AuthBackground(imageModel = "https://images.unsplash.com/photo-1441974231531-c6227db76b6e") {
        // FIXED HEADER
        FixedHeaderContent()

        // SCROLLABLE CONTENT
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(280.dp))

            AuthContainer {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "New\nAccount",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 28.sp,
                            lineHeight = 34.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                UnderlinedAuthTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
                    leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) }
                )

                UnderlinedAuthTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = "Username",
                    leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) }
                )

                UnderlinedAuthTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = "Address / Location",
                    leadingIcon = { Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                    trailingIcon = {
                        if (isFetchingLocation) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
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
                                Icon(Icons.Rounded.MyLocation, contentDescription = "Get Location", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                )

                UnderlinedAuthTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(36.dp))

                OrangeButton(
                    text = "Sign up",
                    onClick = {
                        if (fullName.isNotBlank() && email.isNotBlank() && password.isNotBlank() && address.isNotBlank()) {
                            viewModel.pendingSignupRequest = CreateAccountRequest(fullName, email, password, address)
                            viewModel.sendOtp(email) { onNavigateToOtpVerify(email) }
                        } else {
                            viewModel.setError("Please fill all fields")
                        }
                    },
                    isLoading = state.isLoading
                )

                if (state.error != null) {
                    Text(
                        state.error!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Already have an account? ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Text(
                        "Sign In",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { onNavigateToLogin() }
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
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
    val scrollState = rememberScrollState()

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    AuthBackground(imageModel = "https://images.unsplash.com/photo-1501785888041-af3ef285b470") {
        // FIXED HEADER (Simulated with title change)
        FixedHeaderContent(title = "Reset Password")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .imePadding()
        ) {
            Spacer(modifier = Modifier.height(200.dp))

            AuthContainer(modifier = Modifier.heightIn(min = screenHeight)) {
                when (currentStep) {
                    ForgotPasswordStep.ENTER_EMAIL -> {
                        Text(
                            text = "Enter your email address to receive a password reset OTP.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                        UnderlinedAuthTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = "Email Address",
                            leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        OrangeButton(
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
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                        UnderlinedAuthTextField(
                            value = otp,
                            onValueChange = { otp = it },
                            label = "Verification Code",
                            leadingIcon = { Icon(Icons.Rounded.LockOpen, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        OrangeButton(
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
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                        UnderlinedAuthTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = "New Password",
                            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
                        )
                        UnderlinedAuthTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = "Confirm Password",
                            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            visualTransformation = PasswordVisualTransformation()
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        OrangeButton(
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
                if (state.error != null) {
                    Text(
                        state.error!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 16.dp)
                    )
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
    val scrollState = rememberScrollState()

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    AuthBackground(imageModel = "https://images.unsplash.com/photo-1501785888041-af3ef285b470") {
        // FIXED HEADER
        FixedHeaderContent(title = "Verify Account")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(200.dp))

            AuthContainer(modifier = Modifier.heightIn(min = screenHeight)) {
                Icon(
                    imageVector = Icons.Rounded.VerifiedUser,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp).align(Alignment.CenterHorizontally),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Check your email",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Text(
                    text = "We sent a 6-digit verification code to $email",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                UnderlinedAuthTextField(
                    value = otp,
                    onValueChange = { otp = it },
                    label = "Verification Code",
                    leadingIcon = { Icon(Icons.Rounded.LockOpen, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(32.dp))

                OrangeButton(
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

                if (state.error != null) {
                    Text(
                        state.error!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(
                    onClick = { viewModel.sendOtp(email) {} },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Resend Code", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --- Utils ---

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

enum class ForgotPasswordStep {
    ENTER_EMAIL, ENTER_OTP, ENTER_NEW_PASSWORD
}
