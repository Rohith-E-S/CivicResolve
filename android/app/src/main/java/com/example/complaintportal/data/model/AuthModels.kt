package com.example.complaintportal.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class User(
    @Json(name = "_id") val id: String?,
    val email: String?,
    val fullName: String?,
    val profilePic: String?,
    val address: String?,
    val homeDistrict: String?,
    val isAdmin: Boolean? = false,
    val isVerified: Boolean? = false,
    val civicPoints: Int? = 0,
    val rank: String? = "citizen"
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    val success: Boolean,
    val message: String?,
    val user: User?,
    val token: String?
)

@JsonClass(generateAdapter = true)
data class BaseResponse(
    val success: Boolean,
    val message: String?
)

@JsonClass(generateAdapter = true)
data class SendOtpRequest(val email: String)

@JsonClass(generateAdapter = true)
data class VerifyOtpRequest(val email: String, val otp: String)

@JsonClass(generateAdapter = true)
data class CreateAccountRequest(
    val fullName: String,
    val email: String,
    val password: String,
    val address: String
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class GoogleLoginRequest(
    val email: String,
    val fullName: String,
    val profilePic: String?,
    val googleId: String
)

@JsonClass(generateAdapter = true)
data class SendPasswordResetOtpRequest(val email: String)

@JsonClass(generateAdapter = true)
data class VerifyPasswordResetOtpRequest(val email: String, val otp: String)

@JsonClass(generateAdapter = true)
data class VerifyPasswordResetOtpResponse(
    val success: Boolean,
    val message: String?,
    val resetToken: String?
)

@JsonClass(generateAdapter = true)
data class ResetPasswordRequest(val password: String, val token: String)
