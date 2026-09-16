package com.civicresolve.ap.data.model

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.*
import org.junit.Test

class AuthContractTest {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    @Test fun signupProofRoundTripsAndPasswordRemainsPlaintext() {
        val verified = moshi.adapter(VerifyOtpResponse::class.java).fromJson(
            """{"success":true,"message":"OTP verified","signupToken":"test-proof"}"""
        )!!
        val request = CreateAccountRequest("Test Citizen", "test@example.test", "pass word8", "City", verified.signupToken)
        val json = moshi.adapter(CreateAccountRequest::class.java).toJson(request)
        assertTrue(json.contains("\"signupToken\":\"test-proof\""))
        assertTrue(json.contains("\"password\":\"pass word8\""))
    }

    @Test fun googleRequestHasCredentialOnly() {
        assertEquals("""{"credential":"test-id-token"}""",
            moshi.adapter(GoogleLoginRequest::class.java).toJson(GoogleLoginRequest("test-id-token")))
    }

    @Test fun passwordPolicyHonorsUtf8ByteBoundaryWithoutTrimming() {
        assertFalse(PasswordPolicy.isValid("short7"))
        assertTrue(PasswordPolicy.isValid("12345678"))
        assertTrue(PasswordPolicy.isValid("a".repeat(72)))
        assertFalse(PasswordPolicy.isValid("a".repeat(73)))
        assertTrue(PasswordPolicy.isValid("é".repeat(36)))
        assertFalse(PasswordPolicy.isValid("é".repeat(37)))
        assertTrue(PasswordPolicy.isValid(" pass 8 "))
    }
}
