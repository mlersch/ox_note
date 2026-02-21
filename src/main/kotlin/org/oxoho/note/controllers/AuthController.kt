package org.oxoho.note.controllers

import org.oxoho.note.dto.AuthRequest
import org.oxoho.note.dto.RefreshTokenRequest
import org.oxoho.note.dto.TokenResponse
import org.oxoho.note.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

/**
 * REST Controller for handling user authentication
 *
 * This controller manages three main authentication operations:
 * - User registration (creating new accounts)
 * - User login (getting access tokens)
 * - Token refresh (getting new tokens when access token expires)
 *
 * All endpoints are public (no login required) - see SecurityConfig for details
 */
@RestController // Tells Spring this class handles HTTP requests and returns JSON
@RequestMapping("/api/v1/auth") // All endpoints in this controller start with /api/v1/auth
class AuthController(
    // Spring automatically provides an instance of AuthService (dependency injection)
    private val authService: AuthService
) {

    /**
     * Register a new user account
     *
     * Endpoint: POST /api/v1/auth/register
     *
     * What it does:
     * 1. Validates the email format and password strength (via @Valid)
     * 2. Creates a new user in the database with hashed password
     * 3. Returns 201 CREATED status on success
     *
     * @param request Contains email and password (validated automatically)
     * @throws ResourceAlreadyExistsException if email is already taken
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED) // Returns 201 instead of default 200
    fun register(@Valid @RequestBody request: AuthRequest) {
        // @Valid triggers validation rules defined in AuthRequest (email format, password strength)
        // @RequestBody tells Spring to read the JSON from the request body
        authService.register(request.email, request.password)
    }

    /**
     * Log in with existing credentials
     *
     * Endpoint: POST /api/v1/auth/login
     *
     * What it does:
     * 1. Validates email and password against database
     * 2. Generates two tokens:
     *    - Access Token: Short-lived (15 min), used to access protected endpoints
     *    - Refresh Token: Long-lived (30 days), used to get new access tokens
     * 3. Returns both tokens to the client
     *
     * @param request Contains email and password
     * @return TokenResponse with both access and refresh tokens
     * @throws BadCredentialsException if email or password is wrong
     */
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: AuthRequest): TokenResponse {
        return authService.login(request.email, request.password)
    }

    /**
     * Get a new access token using a refresh token
     *
     * Endpoint: POST /api/v1/auth/refresh
     *
     * Why this exists:
     * - Access tokens expire after 15 minutes for security
     * - Instead of logging in again, use this endpoint with your refresh token
     * - Gets you a brand new pair of tokens
     *
     * What it does:
     * 1. Validates the refresh token
     * 2. Verifies it exists in the database and hasn't been used before
     * 3. Deletes the old refresh token (one-time use for security)
     * 4. Generates and returns new access + refresh tokens
     *
     * @param request Contains the refresh token
     * @return TokenResponse with new access and refresh tokens
     * @throws InvalidTokenException if refresh token is invalid, expired, or already used
     */
    @PostMapping("/refresh")
    fun refresh(@RequestBody request: RefreshTokenRequest): TokenResponse {
        return authService.refresh(request.refreshToken)
    }
}