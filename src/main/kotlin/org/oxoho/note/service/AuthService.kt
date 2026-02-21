package org.oxoho.note.service

import org.oxoho.note.database.model.RefreshToken
import org.oxoho.note.database.model.User
import org.oxoho.note.database.repository.RefreshTokenRepository
import org.oxoho.note.database.repository.UserRepository
import org.oxoho.note.dto.TokenResponse
import org.oxoho.note.exception.InvalidTokenException
import org.oxoho.note.exception.ResourceAlreadyExistsException
import org.oxoho.note.security.HashEncoder
import org.oxoho.note.security.JwtService
import org.bson.types.ObjectId
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.Instant
import java.util.*

/**
 * Authentication Service
 *
 * This service handles all authentication-related business logic:
 * - User registration (creating new accounts)
 * - User login (verifying credentials and issuing tokens)
 * - Token refresh (getting new tokens when access token expires)
 *
 * Security features:
 * - Passwords are hashed with BCrypt (never stored in plain text)
 * - Refresh tokens are hashed with SHA-256 before storing in database
 * - Refresh tokens are single-use (deleted after being used)
 * - All database operations are validated
 */
@Service // Marks this as a Spring service component
class AuthService(
    private val jwtService: JwtService,                        // Creates and validates JWT tokens
    private val userRepository: UserRepository,                // Database access for users
    private val hashEncoder: HashEncoder,                      // BCrypt password hashing
    private val refreshTokenRepository: RefreshTokenRepository // Database access for refresh tokens
) {

    /**
     * Register a new user account
     *
     * What it does:
     * 1. Checks if email already exists (prevents duplicates)
     * 2. Hashes the password with BCrypt
     * 3. Saves new user to database
     *
     * Security notes:
     * - Email is trimmed to prevent whitespace tricks
     * - Password is NEVER stored in plain text
     * - Password validation happens at controller level (@Valid annotation)
     *
     * @param email User's email address
     * @param password User's plain text password (will be hashed)
     * @return The created User object
     * @throws ResourceAlreadyExistsException if email is already registered
     */
    fun register(email: String, password: String): User {
        // Check if email already exists
        val existingUser = userRepository.findByEmail(email.trim())
        if (existingUser != null) {
            throw ResourceAlreadyExistsException("A user with that email already exists.")
        }

        // Create and save new user with hashed password
        return userRepository.save(
            User(
                email = email,
                hashedPassword = hashEncoder.encode(password)  // Hash password before storing
            )
        )
    }

    /**
     * Log in a user with email and password
     *
     * What it does:
     * 1. Find user by email
     * 2. Verify password matches the stored hash
     * 3. Generate new access and refresh tokens
     * 4. Store refresh token in database (hashed)
     * 5. Return both tokens to client
     *
     * Security notes:
     * - Error message is generic ("Invalid credentials") to prevent email enumeration
     * - Password is checked using BCrypt's secure comparison
     * - Refresh token is hashed before database storage
     *
     * @param email User's email
     * @param password User's plain text password
     * @return TokenResponse with access and refresh tokens
     * @throws BadCredentialsException if email or password is wrong
     */
    fun login(email: String, password: String): TokenResponse {
        // Find user by email (or throw error if not found)
        val user = userRepository.findByEmail(email)
            ?: throw BadCredentialsException("Invalid credentials.")

        // Verify password matches stored hash
        if (!hashEncoder.matches(password, user.hashedPassword)) {
            throw BadCredentialsException("Invalid credentials.")
        }

        // Generate new token pair
        val newAccessToken = jwtService.generateAccessToken(user.id.toHexString())
        val newRefreshToken = jwtService.generateRefreshToken(user.id.toHexString())

        // Store refresh token in database (hashed for security)
        storeRefreshToken(user.id, newRefreshToken)

        return TokenResponse(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken
        )
    }

    /**
     * Refresh an access token using a refresh token
     *
     * What it does:
     * 1. Validate the refresh token (signature, expiry, type)
     * 2. Verify refresh token exists in database and hasn't been used
     * 3. Delete old refresh token (one-time use for security)
     * 4. Generate new token pair
     * 5. Store new refresh token in database
     * 6. Return new tokens
     *
     * Why one-time use?
     * - If a refresh token is stolen and used, it gets deleted
     * - Real user then tries to use it and gets error
     * - System knows something is wrong and can revoke all tokens
     * - This is called "refresh token rotation" - a security best practice
     *
     * @Transactional ensures delete + save happen together (all-or-nothing)
     *
     * @param refreshToken The refresh token from client
     * @return TokenResponse with new access and refresh tokens
     * @throws InvalidTokenException if token is invalid, expired, or already used
     */
    @Transactional  // Ensures database operations are atomic
    fun refresh(refreshToken: String): TokenResponse {
        // Validate token is properly signed, not expired, and is a refresh token
        if (!jwtService.validateRefreshToken(refreshToken)) {
            throw InvalidTokenException("Invalid refresh token.")
        }

        // Extract user ID from token
        val userId = jwtService.getUserIdFromToken(refreshToken)
        val user = userRepository.findById(ObjectId(userId))
            .orElseThrow { InvalidTokenException("Invalid refresh token.") }

        // Check if refresh token exists in database
        val hashed = hashToken(refreshToken)
        refreshTokenRepository.findByUserIdAndHashedToken(user.id, hashed)
            ?: throw InvalidTokenException("Refresh token not recognized (maybe used or expired?)")

        // DELETE old refresh token (one-time use)
        refreshTokenRepository.deleteByUserIdAndHashedToken(user.id, hashed)

        // Generate NEW token pair
        val newAccessToken = jwtService.generateAccessToken(userId)
        val newRefreshToken = jwtService.generateRefreshToken(userId)

        // Store new refresh token
        storeRefreshToken(user.id, newRefreshToken)

        return TokenResponse(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken
        )
    }

    /**
     * Store a refresh token in the database
     *
     * Security: Token is hashed with SHA-256 before storage
     * Even if database is compromised, attackers can't use the tokens
     * (they need the original unhashed token)
     *
     * @param userId The user ID this token belongs to
     * @param rawRefreshToken The plain refresh token (will be hashed)
     */
    private fun storeRefreshToken(userId: ObjectId, rawRefreshToken: String) {
        val hashed = hashToken(rawRefreshToken)
        val expiryMs = jwtService.refreshTokenValidityMs
        val expiresAt = Instant.now().plusMillis(expiryMs)

        refreshTokenRepository.save(
            RefreshToken(
                userId = userId,
                expiresAt = expiresAt,
                hashedToken = hashed  // Store HASHED token, not plain text
            )
        )
    }

    /**
     * Hash a token with SHA-256
     *
     * Why hash refresh tokens?
     * - Extra security layer in case database is compromised
     * - Attacker can't use stolen tokens from database
     * - Similar to how we hash passwords
     *
     * Note: We use SHA-256 (fast) instead of BCrypt (slow) because:
     * - Refresh tokens are already random and unpredictable
     * - No risk of dictionary attacks (unlike passwords)
     * - Fast hashing is acceptable here
     *
     * @param token The token to hash
     * @return Base64-encoded SHA-256 hash
     */
    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(token.encodeToByteArray())
        return Base64.getEncoder().encodeToString(hashBytes)
    }
}
