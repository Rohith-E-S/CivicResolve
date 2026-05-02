package com.example.complaintportal.data.repository

import com.example.complaintportal.data.model.*
import com.example.complaintportal.data.remote.ApiService
import com.example.complaintportal.data.remote.CookieJarImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class AuthRepository(private val apiService: ApiService, private val cookieJar: CookieJarImpl) {

    private fun parseError(errorCode: Int, errorBody: String?): String {
        if (errorBody.isNullOrBlank()) return "Server error ($errorCode)"
        return try {
            val json = JSONObject(errorBody)
            json.optString("message", "Error $errorCode: ${json.optString("error", "Unknown")}")
        } catch (e: Exception) {
            "HTTP $errorCode: ${errorBody.take(100)}"
        }
    }

    suspend fun sendOtp(email: String): Result<BaseResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.sendOtp(SendOtpRequest(email))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseError(response.code(), response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyOtp(email: String, otp: String): Result<BaseResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.verifyOtp(VerifyOtpRequest(email, otp))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseError(response.code(), response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createAccount(request: CreateAccountRequest): Result<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.createAccount(request)
            if (response.isSuccessful && response.body() != null) {
                response.body()?.token?.let { cookieJar.setToken(it) }
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseError(response.code(), response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(request: LoginRequest): Result<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.login(request)
            if (response.isSuccessful && response.body() != null) {
                response.body()?.token?.let { cookieJar.setToken(it) }
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseError(response.code(), response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun googleLogin(request: GoogleLoginRequest): Result<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.googleLogin(request)
            if (response.isSuccessful && response.body() != null) {
                response.body()?.token?.let { cookieJar.setToken(it) }
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseError(response.code(), response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPasswordResetOtp(email: String): Result<BaseResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.sendPasswordResetOtp(SendPasswordResetOtpRequest(email))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseError(response.code(), response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyPasswordResetOtp(email: String, otp: String): Result<VerifyPasswordResetOtpResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.verifyPasswordResetOtp(VerifyPasswordResetOtpRequest(email, otp))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseError(response.code(), response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetPassword(request: ResetPasswordRequest): Result<BaseResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.resetPassword(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseError(response.code(), response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<BaseResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.logout()
            cookieJar.clearCookies()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseError(response.code(), response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            cookieJar.clearCookies()
            Result.failure(e)
        }
    }

    suspend fun checkAuth(): Result<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.checkAuth()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseError(response.code(), response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        fullName: okhttp3.RequestBody?,
        address: okhttp3.RequestBody?,
        profilePic: okhttp3.MultipartBody.Part?
    ): Result<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.updateProfile(fullName, address, profilePic)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseError(response.code(), response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateHomeDistrict(district: String): Result<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.updateHomeDistrict(mapOf("district" to district))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseError(response.code(), response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
