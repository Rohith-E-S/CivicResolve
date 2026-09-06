package com.civicresolve.ap.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.civicresolve.ap.ui.components.SimpleOtpRow
import com.civicresolve.ap.ui.components.UiCard
import com.civicresolve.ap.ui.theme.DisplayFontFamily
import com.civicresolve.ap.ui.theme.MonoFontFamily

@Composable
fun LoginScreen(
    onLogin: (String, String) -> Unit,
    onGoogleLogin: () -> Unit,
    onSignup: () -> Unit,
    onForgot: () -> Unit,
    isLoading: Boolean,
    error: String?,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.Center) {
        UiCard(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.width(4.dp).height(60.dp))
            Text("Citizen access • Step 1 of 1", fontFamily = MonoFontFamily, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
            Text("Sign in", style = MaterialTheme.typography.displaySmall, fontFamily = DisplayFontFamily)
            Text("Access your ledger, chats, and verification duties.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!error.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, placeholder = { Text("name@example.com") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(10.dp))
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Password", fontFamily = MonoFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = onForgot) { Text("Forgot password?", fontSize = 12.sp) }
            }
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, placeholder = { Text("Enter your password") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(10.dp))
            Spacer(Modifier.height(14.dp))
            Button(onClick = { onLogin(email, password) }, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) { Text(if (isLoading) "Signing in..." else "Sign in") }
            Spacer(Modifier.height(12.dp))
            Divider()
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onGoogleLogin, modifier = Modifier.fillMaxWidth()) { Text("Continue with Google") }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text("New here? ", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onSignup) { Text("Create an account") }
            }
        }
    }
}

@Composable
fun SignupScreen(
    onSubmit: (String, String, String, String) -> Unit,
    onGoogleLogin: () -> Unit,
    onLogin: () -> Unit,
    isLoading: Boolean,
    error: String?,
    modifier: Modifier = Modifier
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        UiCard(modifier = Modifier.fillMaxWidth()) {
            Text("New ledger entry • Step 1 of 2", fontFamily = MonoFontFamily, fontSize = 10.sp, color = Color(0xFFFF6B2B))
            Text("Create account", style = MaterialTheme.typography.displaySmall, fontFamily = DisplayFontFamily)
            Text("Takes 45 seconds. You'll verify your email next.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!error.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full name") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(10.dp))
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(10.dp))
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(10.dp))
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth(), minLines = 3, shape = RoundedCornerShape(10.dp))
            Spacer(Modifier.height(12.dp))
            Button(onClick = { onSubmit(fullName, email, password, address) }, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) { Text(if (isLoading) "Sending OTP..." else "Continue with email") }
            Spacer(Modifier.height(12.dp))
            Divider()
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onGoogleLogin, modifier = Modifier.fillMaxWidth()) { Text("Continue with Google") }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text("Already have an account? ", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onLogin) { Text("Sign in") }
            }
        }
    }
}

@Composable
fun OtpVerifyScreen(
    email: String,
    onVerify: (String) -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean,
    error: String?,
    modifier: Modifier = Modifier
) {
    var otpValues by remember { mutableStateOf(List(6) { "" }) }
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.Center) {
        UiCard(modifier = Modifier.fillMaxWidth()) {
            Text("Step 2 of 2 • Check your inbox", fontFamily = MonoFontFamily, fontSize = 10.sp, color = Color(0xFF0E9F6E))
            Text("Verify your email", style = MaterialTheme.typography.displaySmall, fontFamily = DisplayFontFamily)
            Text("Enter the 6-digit code sent to $email. Valid for 5 minutes.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!error.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
            Spacer(Modifier.height(16.dp))
            SimpleOtpRow(values = otpValues, onChange = { idx, v -> otpValues = otpValues.toMutableList().also { it[idx] = v } })
            Spacer(Modifier.height(16.dp))
            Button(onClick = { onVerify(otpValues.joinToString("")) }, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) { Text(if (isLoading) "Verifying..." else "Verify account") }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Use a different email") }
        }
    }
}

@Composable
fun ForgotPasswordScreen(
    onSendOtp: (String) -> Unit,
    onVerifyOtp: (String, String) -> Unit,
    isLoading: Boolean,
    error: String?,
    message: String?,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var isOtpSent by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.Center) {
        UiCard {
            Text("Reset password", style = MaterialTheme.typography.displaySmall, fontFamily = DisplayFontFamily)
            Text(if (isOtpSent) "Enter the OTP from your email to continue." else "Enter your email and we will send a one-time password.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!error.isNullOrBlank()) { Spacer(Modifier.height(8.dp)); Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
            if (!message.isNullOrBlank()) { Spacer(Modifier.height(8.dp)); Text(message, color = Color(0xFF0E9F6E), fontSize = 12.sp) }
            Spacer(Modifier.height(16.dp))
            if (!isOtpSent) {
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                Spacer(Modifier.height(12.dp))
                Button(onClick = { onSendOtp(email); isOtpSent = true }, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) { Text(if (isLoading) "Sending..." else "Send OTP") }
            } else {
                OutlinedTextField(value = otp, onValueChange = { otp = it }, label = { Text("One-time password") }, placeholder = { Text("Enter 6-digit OTP") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                Spacer(Modifier.height(12.dp))
                Button(onClick = { onVerifyOtp(email, otp) }, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) { Text(if (isLoading) "Verifying..." else "Verify OTP") }
            }
        }
    }
}

@Composable
fun ResetPasswordScreen(
    onReset: (String) -> Unit,
    isLoading: Boolean,
    error: String?,
    message: String?,
    modifier: Modifier = Modifier
) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.Center) {
        UiCard {
            Text("Set a new password", style = MaterialTheme.typography.displaySmall, fontFamily = DisplayFontFamily)
            Text("Use a strong password that you can remember.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!message.isNullOrBlank()) { Spacer(Modifier.height(8.dp)); Text(message, color = Color(0xFF0E9F6E), fontSize = 12.sp) }
            if (!error.isNullOrBlank()) { Spacer(Modifier.height(8.dp)); Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("New password") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(10.dp))
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = confirm, onValueChange = { confirm = it }, label = { Text("Confirm password") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(10.dp))
            Spacer(Modifier.height(12.dp))
            Button(onClick = { if (password == confirm) onReset(password) }, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) { Text(if (isLoading) "Resetting..." else "Reset password") }
        }
    }
}
