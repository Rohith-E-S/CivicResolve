package com.civicresolve.ap.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.civicresolve.ap.di.AppContainer
import com.civicresolve.ap.ui.screens.*
import com.civicresolve.ap.ui.viewmodel.AuthViewModel
import com.civicresolve.ap.ui.viewmodel.AuthViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Landing : Screen("landing")
    object Login : Screen("login")
    object Signup : Screen("signup")
    object OtpVerify : Screen("otp_verify/{email}") { fun createRoute(email: String) = "otp_verify/$email" }
    object ForgotPassword : Screen("forgot_password")
    object ResetPassword : Screen("reset_password/{token}") { fun createRoute(token: String) = "reset_password/$token" }
    object Dashboard : Screen("dashboard?tab={tab}") {
        fun createRoute(tab: String? = null): String =
            if (tab.isNullOrBlank()) "dashboard" else "dashboard?tab=$tab"
    }
    object Explore : Screen("explore")
    object MapView : Screen("map_view")
    object ComplaintOverview : Screen("complaint_overview/{id}") { fun createRoute(id: String) = "complaint_overview/$id" }
    object AdminComplaintOverview : Screen("admin_complaint_overview/{id}") { fun createRoute(id: String) = "admin_complaint_overview/$id" }
    object Chat : Screen("chat/{complaintId}") { fun createRoute(id: String) = "chat/$id" }
    object AdminDashboard : Screen("admin_dashboard")
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    appContainer: AppContainer
) {
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(appContainer.authRepository))
    val authState by authViewModel.authState.collectAsState()
    var publicStats by remember { mutableStateOf<com.civicresolve.ap.data.model.PublicStats?>(null) }

    LaunchedEffect(Unit) {
        val r = appContainer.complaintRepository.getPublicStats("all")
        r.onSuccess { res -> publicStats = res.stats }
    }

    // Keep the notification socket in sync with the session
    LaunchedEffect(authState.isAuthenticated, authState.user?.id) {
        val userId = authState.user?.id
        if (authState.isAuthenticated && userId != null) {
            appContainer.notificationSocket.connect(userId)
        } else {
            appContainer.notificationSocket.disconnect()
        }
    }

    // Redirect when auth changes
    LaunchedEffect(authState.isAuthenticated, authState.isChecking) {
        if (!authState.isChecking) {
            val current = navController.currentDestination?.route
            if (authState.isAuthenticated) {
                if (current == Screen.Login.route || current == Screen.Signup.route || current == Screen.Landing.route) {
                    val dest = if (authState.user?.isAdmin == true) Screen.AdminDashboard.route else Screen.Dashboard.route
                    navController.navigate(dest) { popUpTo(Screen.Landing.route) { inclusive = true } }
                }
            }
        }
    }

    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = Screen.Landing.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn(tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) + fadeOut(tween(300)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut(tween(300)) }
        ) {
            composable(Screen.Landing.route) {
                val isLoggedIn = authState.isAuthenticated
                LandingScreen(
                    isLoggedIn = isLoggedIn,
                    stats = publicStats,
                    onLogin = { navController.navigate(Screen.Login.route) },
                    onSignup = { navController.navigate(Screen.Signup.route) },
                    onDashboard = {
                        val dest = if (authState.user?.isAdmin == true) Screen.AdminDashboard.route else Screen.Dashboard.route
                        navController.navigate(dest)
                    },
                    onExplore = { navController.navigate(Screen.Explore.route) },
                    onLogout = { authViewModel.logout(); navController.navigate(Screen.Landing.route) { popUpTo(0) { inclusive = true } } }
                )
            }
            composable(Screen.Login.route) {
                var error by remember { mutableStateOf<String?>(null) }
                LaunchedEffect(authState.error) { error = authState.error }
                if (authState.isChecking) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else {
                    LoginScreen(
                        onLogin = { email, pass ->
                            error = null
                            authViewModel.login(com.civicresolve.ap.data.model.LoginRequest(email, pass)) {
                                val dest = if (authViewModel.authState.value.user?.isAdmin == true) Screen.AdminDashboard.route else Screen.Dashboard.route
                                navController.navigate(dest) { popUpTo(Screen.Login.route) { inclusive = true } }
                            }
                        },
                        onGoogleLogin = { error = "Google login not configured on this build" },
                        onSignup = { navController.navigate(Screen.Signup.route) },
                        onForgot = { navController.navigate(Screen.ForgotPassword.route) },
                        isLoading = authState.isLoading,
                        error = error ?: authState.error
                    )
                    LaunchedEffect(authState.error) { error = authState.error }
                }
            }
            composable(Screen.Signup.route) {
                var error by remember { mutableStateOf<String?>(null) }
                SignupScreen(
                    onSubmit = { fullName, email, password, address ->
                        error = null
                        if (fullName.isBlank() || email.isBlank() || password.isBlank() || address.isBlank()) { error = "All fields required"; return@SignupScreen }
                        authViewModel.sendOtp(email) {
                            authViewModel.pendingSignupRequest = com.civicresolve.ap.data.model.CreateAccountRequest(fullName, email, password, address)
                            navController.navigate(Screen.OtpVerify.createRoute(email))
                        }
                    },
                    onGoogleLogin = { error = "Google login not configured" },
                    onLogin = { navController.navigate(Screen.Login.route) },
                    isLoading = authState.isLoading,
                    error = error ?: authState.error
                )
            }
            composable(Screen.OtpVerify.route, arguments = listOf(navArgument("email") { type = NavType.StringType })) { entry ->
                val email = entry.arguments?.getString("email") ?: ""
                OtpVerifyScreen(
                    email = email,
                    onVerify = { otp ->
                        authViewModel.verifyOtp(email, otp) {
                            val pending = authViewModel.pendingSignupRequest
                            if (pending != null) {
                                authViewModel.createAccount(pending) {
                                    navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Signup.route) { inclusive = true } }
                                }
                            } else navController.navigate(Screen.Login.route)
                        }
                    },
                    onBack = { navController.popBackStack() },
                    isLoading = authState.isLoading,
                    error = authState.error
                )
            }
            composable(Screen.ForgotPassword.route) {
                var error by remember { mutableStateOf<String?>(null) }
                var msg by remember { mutableStateOf<String?>(null) }
                var pendingToken by remember { mutableStateOf<String?>(null) }
                val scope = rememberCoroutineScope()
                if (pendingToken != null) {
                    ResetPasswordScreen(
                        onReset = { pass ->
                            scope.launch {
                                val r = appContainer.authRepository.resetPassword(com.civicresolve.ap.data.model.ResetPasswordRequest(pass, pendingToken!!))
                                r.onSuccess { msg = "Password reset. Redirecting..."; delay(1500); navController.navigate(Screen.Login.route) { popUpTo(Screen.ForgotPassword.route) { inclusive = true } } }
                                    .onFailure { error = it.message }
                            }
                        },
                        isLoading = false,
                        error = error,
                        message = msg
                    )
                } else {
                    ForgotPasswordScreen(
                        onSendOtp = { email ->
                            scope.launch {
                                val r = appContainer.authRepository.sendPasswordResetOtp(email)
                                r.onSuccess { msg = "OTP sent via email. Please check your inbox."; error = null }
                                    .onFailure { error = it.message }
                            }
                        },
                        onVerifyOtp = { email, otp ->
                            scope.launch {
                                val r = appContainer.authRepository.verifyPasswordResetOtp(email, otp)
                                r.onSuccess { res -> pendingToken = res.resetToken; error = null }
                                    .onFailure { error = it.message }
                            }
                        },
                        isLoading = authState.isLoading,
                        error = error ?: authState.error,
                        message = msg
                    )
                }
            }
            composable(
                Screen.Dashboard.route,
                arguments = listOf(navArgument("tab") { nullable = true; defaultValue = null })
            ) {
                if (authState.isChecking) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                else if (!authState.isAuthenticated) {
                    LaunchedEffect(Unit) { navController.navigate(Screen.Login.route) { popUpTo(Screen.Dashboard.route) { inclusive = true } } }
                } else if (authState.user?.isAdmin == true) {
                    LaunchedEffect(Unit) { navController.navigate(Screen.AdminDashboard.route) { popUpTo(Screen.Dashboard.route) { inclusive = true } } }
                } else {
                    DashboardScreen(
                        appContainer = appContainer,
                        authViewModel = authViewModel,
                        initialTab = it.arguments?.getString("tab"),
                        onNavigateExplore = { navController.navigate(Screen.Explore.route) },
                        onNavigateMap = { navController.navigate(Screen.MapView.route) },
                        onOpenComplaint = { id -> navController.navigate(Screen.ComplaintOverview.createRoute(id)) },
                        onOpenChat = { id -> navController.navigate(Screen.Chat.createRoute(id)) },
                        onLogout = { authViewModel.logout(); navController.navigate(Screen.Landing.route) { popUpTo(0) { inclusive = true } } }
                    )
                }
            }
            composable(Screen.Explore.route) {
                ExploreScreen(
                    appContainer = appContainer,
                    authViewModel = authViewModel,
                    onOpenComplaint = { id -> navController.navigate(Screen.ComplaintOverview.createRoute(id)) },
                    onDashboardTab = { tab -> navController.navigate(Screen.Dashboard.createRoute(tab)) },
                    onDashboard = {
                        val dest = if (authState.user?.isAdmin == true) Screen.AdminDashboard.route else Screen.Dashboard.route
                        navController.navigate(dest) { popUpTo(Screen.Explore.route) { inclusive = true } }
                    },
                    onMap = { navController.navigate(Screen.MapView.route) }
                )
            }
            composable(Screen.MapView.route) {
                MapViewScreen(
                    appContainer = appContainer,
                    authViewModel = authViewModel,
                    onDashboard = {
                        val dest = if (authState.user?.isAdmin == true) Screen.AdminDashboard.route else Screen.Dashboard.route
                        navController.navigate(dest) { popUpTo(Screen.MapView.route) { inclusive = true } }
                    },
                    onOpenComplaint = { id -> navController.navigate(Screen.ComplaintOverview.createRoute(id)) }
                )
            }
            composable(Screen.ComplaintOverview.route, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                val id = entry.arguments?.getString("id") ?: ""
                ComplaintOverviewScreen(
                    complaintId = id,
                    appContainer = appContainer,
                    onBack = { navController.popBackStack() },
                    onChat = { navController.navigate(Screen.Chat.createRoute(id)) }
                )
            }
            composable(Screen.AdminComplaintOverview.route, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                val id = entry.arguments?.getString("id") ?: ""
                AdminComplaintOverviewScreen(
                    complaintId = id,
                    appContainer = appContainer,
                    onBack = { navController.popBackStack() },
                    onChat = { navController.navigate(Screen.Chat.createRoute(id)) }
                )
            }
            composable(Screen.Chat.route, arguments = listOf(navArgument("complaintId") { type = NavType.StringType })) { entry ->
                val cid = entry.arguments?.getString("complaintId") ?: ""
                ComplaintChatScreen(
                    complaintId = cid,
                    appContainer = appContainer,
                    authViewModel = authViewModel,
                    onBack = { navController.popBackStack() },
                    onDetails = {
                        val dest = if (authState.user?.isAdmin == true) Screen.AdminComplaintOverview.createRoute(cid) else Screen.ComplaintOverview.createRoute(cid)
                        navController.navigate(dest)
                    }
                )
            }
            composable(Screen.AdminDashboard.route) {
                if (authState.user?.isAdmin != true) {
                    LaunchedEffect(Unit) { navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.AdminDashboard.route) { inclusive = true } } }
                } else {
                    AdminDashboardScreen(
                        appContainer = appContainer,
                        onOpenComplaint = { id -> navController.navigate(Screen.AdminComplaintOverview.createRoute(id)) },
                        onOpenChat = { id -> navController.navigate(Screen.Chat.createRoute(id)) },
                        onLogout = { authViewModel.logout(); navController.navigate(Screen.Landing.route) { popUpTo(0) { inclusive = true } } }
                    )
                }
            }
        }
    }
}
