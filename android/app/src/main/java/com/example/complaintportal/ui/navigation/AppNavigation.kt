package com.example.complaintportal.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import com.example.complaintportal.di.AppContainer
import com.example.complaintportal.ui.screens.*
import com.example.complaintportal.ui.screens.admin.*
import com.example.complaintportal.ui.screens.user.*
import com.example.complaintportal.ui.viewmodel.*

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalNavAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Signup : Screen("signup")
    object OtpVerify : Screen("otp_verify/{email}") {
        fun createRoute(email: String) = "otp_verify/$email"
    }
    object ForgotPassword : Screen("forgot_password")
    object LocationOnboarding : Screen("location_onboarding")
    object Dashboard : Screen("dashboard")
    object Profile : Screen("profile")
    object CreateComplaint : Screen("create_complaint")
    object ComplaintDetail : Screen("complaint_detail/{complaintId}") {
        fun createRoute(complaintId: String) = "complaint_detail/$complaintId"
    }
    object Chat : Screen("chat/{complaintId}") {
        fun createRoute(complaintId: String) = "chat/$complaintId"
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    appContainer: AppContainer
) {
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(appContainer.authRepository)
    )
    val complaintViewModel: ComplaintViewModel = viewModel(
        factory = ComplaintViewModelFactory(appContainer.complaintRepository)
    )
    val messageViewModel: MessageViewModel = viewModel(
        factory = MessageViewModelFactory(
            appContainer.messageRepository, 
            appContainer.cookieJar, 
            appContainer.moshi,
            appContainer.socketUrl
        )
    )

    val authState by authViewModel.authState.collectAsState()

    SharedTransitionLayout {
            CompositionLocalProvider(
                LocalSharedTransitionScope provides this
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Splash.route,
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300)) },
                    popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)) },
                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300)) }
                ) {
                    composable(Screen.Splash.route) {
                        SplashScreen(onFinished = {
                            val destination = when {
                                !authState.isAuthenticated -> Screen.Login.route
                                authState.detectedDistrict.isNullOrBlank() -> Screen.LocationOnboarding.route
                                else -> Screen.Dashboard.route
                            }
                            navController.navigate(destination) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        })
                    }
            composable(Screen.Login.route) {
                LoginScreen(
                    viewModel = authViewModel,
                    onNavigateToSignup = { navController.navigate(Screen.Signup.route) },
                    onNavigateToForgotPassword = { navController.navigate(Screen.ForgotPassword.route) },
                    onLoginSuccess = {
                        val dest = if (authState.detectedDistrict.isNullOrBlank())
                            Screen.LocationOnboarding.route
                        else
                            Screen.Dashboard.route
                        navController.navigate(dest) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.ForgotPassword.route) {
                ForgotPasswordScreen(
                    viewModel = authViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onPasswordResetSuccess = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.ForgotPassword.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Signup.route) {
                SignupScreen(
                    viewModel = authViewModel,
                    onNavigateToLogin = { navController.navigate(Screen.Login.route) },
                    onNavigateToOtpVerify = { email -> 
                        navController.navigate(Screen.OtpVerify.createRoute(email))
                    },
                    onSignupSuccess = {
                        // New accounts never have a district — always go to onboarding
                        navController.navigate(Screen.LocationOnboarding.route) {
                            popUpTo(Screen.Signup.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Screen.OtpVerify.route,
                arguments = listOf(navArgument("email") { type = NavType.StringType })
            ) { backStackEntry ->
                val email = backStackEntry.arguments?.getString("email") ?: ""
                OtpVerifyScreen(
                    viewModel = authViewModel,
                    email = email,
                    onNavigateBack = { navController.popBackStack() },
                    onVerifySuccess = {
                        navController.navigate(Screen.LocationOnboarding.route) {
                            popUpTo(Screen.Signup.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.LocationOnboarding.route) {
                LocationOnboardingScreen(
                    viewModel = authViewModel,
                    onOnboardingComplete = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.LocationOnboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Dashboard.route) {
                CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                    if (authState.isAuthenticated) {
                        if (authState.user?.isAdmin == true) {
                            AdminDashboardScreen(
                                viewModel = complaintViewModel,
                                onNavigateToDetail = { complaintId -> 
                                    if (complaintId == "profile") {
                                        navController.navigate(Screen.Profile.route)
                                    } else {
                                        navController.navigate(Screen.ComplaintDetail.createRoute(complaintId)) 
                                    }
                                }
                            )
                        } else {
                            UserDashboardScreen(
                                viewModel = complaintViewModel,
                                userName = authState.user?.fullName ?: "Citizen",
                                district = authState.detectedDistrict,
                                onNavigateToCreate = { navController.navigate(Screen.CreateComplaint.route) },
                                onNavigateToDetail = { complaintId -> 
                                    if (complaintId == "profile") {
                                        navController.navigate(Screen.Profile.route)
                                    } else {
                                        navController.navigate(Screen.ComplaintDetail.createRoute(complaintId)) 
                                    }
                                }
                            )
                        }
                    }
                }
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    viewModel = authViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.CreateComplaint.route) {
                CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                    CreateComplaintScreen(
                        viewModel = complaintViewModel,
                        authViewModel = authViewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onSuccess = {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Dashboard.route) { inclusive = true }
                            }
                        }
                    )
                }
            }

            composable(
                route = Screen.ComplaintDetail.route,
                arguments = listOf(navArgument("complaintId") { type = NavType.StringType })
            ) { backStackEntry ->
                CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                    val complaintId = backStackEntry.arguments?.getString("complaintId") ?: ""
                    if (authState.user?.isAdmin == true) {
                        AdminComplaintDetailScreen(
                            viewModel = complaintViewModel,
                            complaintId = complaintId,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToChat = { id -> navController.navigate(Screen.Chat.createRoute(id)) }
                        )
                    } else {
                        UserComplaintDetailScreen(
                            viewModel = complaintViewModel,
                            complaintId = complaintId,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToChat = { id -> navController.navigate(Screen.Chat.createRoute(id)) }
                        )
                    }
                }
            }

            composable(
                route = Screen.Chat.route,
                arguments = listOf(navArgument("complaintId") { type = NavType.StringType })
            ) { backStackEntry ->
                val complaintId = backStackEntry.arguments?.getString("complaintId") ?: ""
                ChatScreen(
                    messageViewModel = messageViewModel,
                    complaintViewModel = complaintViewModel,
                    complaintId = complaintId,
                    currentUserId = authState.user?.id ?: "",
                    isAdmin = authState.user?.isAdmin == true,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
                }
            }
        }
    }
