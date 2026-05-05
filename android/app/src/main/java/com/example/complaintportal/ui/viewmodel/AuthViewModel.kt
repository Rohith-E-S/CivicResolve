package com.example.complaintportal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.complaintportal.data.model.CreateAccountRequest
import com.example.complaintportal.data.model.GoogleLoginRequest
import com.example.complaintportal.data.model.LoginRequest
import com.example.complaintportal.data.model.ResetPasswordRequest
import com.example.complaintportal.data.model.User
import com.example.complaintportal.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthState(
    val isChecking: Boolean = true,
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val user: User? = null,
    val error: String? = null,
    val isOnboardingComplete: Boolean = false,
    val detectedDistrict: String? = null
)

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    var pendingSignupRequest: CreateAccountRequest? = null

    init {
        checkAuth()
    }

    fun sendOtp(email: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            val result = repository.sendOtp(email)
            result.onSuccess { response ->
                _authState.value = _authState.value.copy(isLoading = false)
                if (response.success) {
                    onSuccess()
                } else {
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = response.message ?: "Failed to send OTP"
                    )
                }
            }.onFailure { e ->
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to send OTP"
                )
            }
        }
    }

    fun verifyOtp(email: String, otp: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            val result = repository.verifyOtp(email, otp)
            result.onSuccess { response ->
                _authState.value = _authState.value.copy(isLoading = false)
                if (response.success) {
                    onSuccess()
                } else {
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = response.message ?: "Invalid OTP"
                    )
                }
            }.onFailure { e ->
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Invalid OTP"
                )
            }
        }
    }

    fun checkAuth() {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isChecking = true, error = null)
            val result = repository.checkAuth()
            result.onSuccess { response ->
                if (response.success && response.user != null) {
                    _authState.value = _authState.value.copy(
                        isChecking = false,
                        isAuthenticated = true,
                        user = response.user,
                        isOnboardingComplete = !response.user.homeDistrict.isNullOrBlank(),
                        detectedDistrict = response.user.homeDistrict
                    )
                } else {
                    _authState.value = _authState.value.copy(
                        isChecking = false,
                        isAuthenticated = false
                    )
                }
            }.onFailure {
                _authState.value = _authState.value.copy(
                    isChecking = false,
                    isAuthenticated = false
                )
            }
        }
    }

    fun login(request: LoginRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            val result = repository.login(request)
            result.onSuccess { response ->
                if (response.success) {
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        user = response.user,
                        isOnboardingComplete = !response.user?.homeDistrict.isNullOrBlank(),
                        detectedDistrict = response.user?.homeDistrict
                    )
                    onSuccess()
                } else {
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = response.message ?: "Login failed"
                    )
                }
            }.onFailure { e ->
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Login failed"
                )
            }
        }
    }

    fun googleLogin(request: GoogleLoginRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            val result = repository.googleLogin(request)
            result.onSuccess { response ->
                if (response.success) {
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        user = response.user,
                        isOnboardingComplete = !response.user?.homeDistrict.isNullOrBlank(),
                        detectedDistrict = response.user?.homeDistrict
                    )
                    onSuccess()
                } else {
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = response.message ?: "Google login failed"
                    )
                }
            }.onFailure { e ->
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Google login failed"
                )
            }
        }
    }

    fun createAccount(request: CreateAccountRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            val result = repository.createAccount(request)
            result.onSuccess { response ->
                if (response.success) {
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        user = response.user,
                        isOnboardingComplete = !response.user?.homeDistrict.isNullOrBlank(),
                        detectedDistrict = response.user?.homeDistrict
                    )
                    onSuccess()
                } else {
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = response.message ?: "Signup failed"
                    )
                }
            }.onFailure { e ->
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Signup failed"
                )
            }
        }
    }

    fun sendPasswordResetOtp(email: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            val result = repository.sendPasswordResetOtp(email)
            result.onSuccess { response ->
                _authState.value = _authState.value.copy(isLoading = false)
                if (response.success) {
                    onSuccess()
                } else {
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = response.message ?: "Failed to send OTP"
                    )
                }
            }.onFailure { e ->
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to send OTP"
                )
            }
        }
    }

    fun verifyPasswordResetOtp(email: String, otp: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            val result = repository.verifyPasswordResetOtp(email, otp)
            result.onSuccess { response ->
                _authState.value = _authState.value.copy(isLoading = false)
                if (response.success) {
                    onSuccess(response.resetToken ?: "")
                } else {
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = response.message ?: "Invalid OTP"
                    )
                }
            }.onFailure { e ->
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Invalid OTP"
                )
            }
        }
    }

    fun resetPassword(request: ResetPasswordRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            val result = repository.resetPassword(request)
            result.onSuccess { response ->
                _authState.value = _authState.value.copy(isLoading = false)
                if (response.success) {
                    onSuccess()
                } else {
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = response.message ?: "Failed to reset password"
                    )
                }
            }.onFailure { e ->
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to reset password"
                )
            }
        }
    }

    fun setError(message: String) {
        _authState.value = _authState.value.copy(error = message)
    }

    fun logout(context: android.content.Context? = null) {
        val currentUserId = _authState.value.user?.id
        if (context != null && currentUserId != null) {
            val prefs = context.getSharedPreferences("dashboard_prefs", android.content.Context.MODE_PRIVATE)
            prefs.edit()
                .remove("banner_dismissed_${currentUserId}")
                .apply()
        }

        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true)
            repository.logout()
            _authState.value = AuthState(isChecking = false)
        }
    }

    fun completeOnboarding(district: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            val result = repository.updateHomeDistrict(district)
            result.onSuccess { response ->
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    isOnboardingComplete = true,
                    detectedDistrict = district,
                    user = response.user
                )
                onSuccess()
            }.onFailure { e ->
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Connection error. Please check your internet."
                )
            }
        }
    }

    fun updateProfile(
        fullName: okhttp3.RequestBody?,
        address: okhttp3.RequestBody?,
        profilePic: okhttp3.MultipartBody.Part?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            val result = repository.updateProfile(fullName, address, profilePic)
            result.onSuccess { response ->
                if (response.success && response.user != null) {
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        user = response.user
                    )
                    onSuccess()
                } else {
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = response.message ?: "Profile update failed"
                    )
                }
            }.onFailure { e ->
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Profile update failed"
                )
            }
        }
    }
}

class AuthViewModelFactory(private val repository: AuthRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
