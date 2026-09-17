package com.civicresolve.ap.data.model

/** Send plaintext over TLS; bcrypt byte limits are enforced by the server too. */
object PasswordPolicy {
    const val message = "Use at least 8 characters and no more than 72 UTF-8 bytes for your password."
    fun isValid(password: String): Boolean = password.length >= 8 &&
        password.toByteArray(Charsets.UTF_8).size <= 72
}
