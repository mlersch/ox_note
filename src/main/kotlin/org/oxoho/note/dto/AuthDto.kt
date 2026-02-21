package org.oxoho.note.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Pattern

/**
 * Request body for user registration and login
 *
 * Used by both /register and /login endpoints
 *
 * Validation rules (automatically enforced by Spring via @Valid):
 * - Email must be in valid email format (e.g., user@example.com)
 * - Password must be at least 9 characters
 * - Password must contain at least one uppercase letter
 * - Password must contain at least one lowercase letter
 * - Password must contain at least one digit
 *
 * If validation fails, Spring returns 400 BAD REQUEST with error details
 */
data class AuthRequest(
    @field:Email(message = "Invalid email format.")
    val email: String,

    @field:Pattern(
        regexp = $$"^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{9,}$",
        message = "Password must be at least 9 characters long and contain at least one digit, uppercase and lowercase character."
    )
    val password: String
)

/**
 * Request body for refreshing tokens
 *
 * Used by /refresh endpoint
 * Client sends their refresh token to get a new access token
 */
data class RefreshTokenRequest(
    val refreshToken: String  // The refresh token from previous login
)

/**
 * Response for login and refresh endpoints
 *
 * Contains two tokens that the client needs to store:
 * - accessToken: Use for API requests (expires in 15 min)
 * - refreshToken: Use to get new access tokens (expires in 30 days)
 *
 * Client usage:
 * 1. Store both tokens securely (e.g., localStorage or secure cookie)
 * 2. Include access token in requests: "Authorization: Bearer {accessToken}"
 * 3. When access token expires, use refresh token to get new pair
 */
data class TokenResponse(
    val accessToken: String,   // Short-lived token for API requests
    val refreshToken: String   // Long-lived token to get new access tokens
)
