package org.oxoho.note.security

import org.oxoho.note.config.JwtProperties
import org.oxoho.note.exception.InvalidTokenException
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Service
import java.util.Base64
import java.util.Date

/**
 * JWT (JSON Web Token) Service
 *
 * This service handles everything related to JWT tokens:
 * - Generating new access and refresh tokens
 * - Validating tokens
 * - Extracting user information from tokens
 *
 * What is JWT?
 * - A JWT is a digitally signed string that contains user information
 * - It has three parts: header.payload.signature
 * - The signature proves the token wasn't tampered with
 * - We use it as a "digital passport" for authenticated users
 *
 * Two types of tokens:
 * - ACCESS TOKEN: Short-lived (15 min), used for API requests
 * - REFRESH TOKEN: Long-lived (30 days), used to get new access tokens
 */
@Service // Marks this as a Spring service component
class JwtService(
    // Configuration loaded from application.properties
    private val jwtProperties: JwtProperties
) {

    // The secret key used to sign and verify tokens
    // Decoded from base64 string and converted to cryptographic key
    private val secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtProperties.secret))

    // Expose refresh token validity so AuthService can use it
    val refreshTokenValidityMs = jwtProperties.refreshTokenValidityMs

    /**
     * Internal helper to generate a JWT token
     *
     * This is the core token generation logic used by both access and refresh token methods
     *
     * Token structure:
     * - subject: User ID (who this token belongs to)
     * - type: "access" or "refresh" (what kind of token it is)
     * - issuedAt: When the token was created
     * - expiration: When the token becomes invalid
     * - signature: Cryptographic signature using our secret key
     *
     * @param userId The ID of the user this token is for
     * @param type "access" or "refresh"
     * @param expiry How long until the token expires (in milliseconds)
     * @return A JWT token string (looks like: eyJhbGc.eyJzdWI.SflKxwRJ)
     */
    private fun generateToken(
        userId: String,
        type: String,
        expiry: Long
    ): String {
        val now = Date()
        val expiryDate = Date(now.time + expiry)
        return Jwts.builder()
            .subject(userId)                          // Who the token is for
            .claim("type", type)                      // Custom claim: token type
            .issuedAt(now)                           // When it was created
            .expiration(expiryDate)                  // When it expires
            .signWith(secretKey, Jwts.SIG.HS256)    // Sign with secret key using HS256 algorithm
            .compact()                               // Build the final string
    }

    /**
     * Generate a new access token
     *
     * Access tokens are short-lived (15 minutes) and used for API requests
     * The client includes this in the Authorization header: "Bearer {token}"
     *
     * @param userId The user ID to encode in the token
     * @return A JWT access token
     */
    fun generateAccessToken(userId: String): String {
        return generateToken(userId, "access", jwtProperties.accessTokenValidityMs)
    }

    /**
     * Generate a new refresh token
     *
     * Refresh tokens are long-lived (30 days) and used to get new access tokens
     * When an access token expires, client sends refresh token to get a new pair
     *
     * @param userId The user ID to encode in the token
     * @return A JWT refresh token
     */
    fun generateRefreshToken(userId: String): String {
        return generateToken(userId, "refresh", jwtProperties.refreshTokenValidityMs)
    }

    /**
     * Validate an access token
     *
     * Checks:
     * 1. Token is properly signed and not expired (via parseAllClaims)
     * 2. Token type is "access" (not a refresh token)
     *
     * @param token The JWT token to validate
     * @return true if valid access token, false otherwise
     */
    fun validateAccessToken(token: String): Boolean {
        val claims = parseAllClaims(token) ?: return false  // Invalid/expired token
        val tokenType = claims["type"] as? String ?: return false
        return tokenType == "access"  // Must be an access token, not refresh
    }

    /**
     * Validate a refresh token
     *
     * Checks:
     * 1. Token is properly signed and not expired (via parseAllClaims)
     * 2. Token type is "refresh" (not an access token)
     *
     * @param token The JWT token to validate
     * @return true if valid refresh token, false otherwise
     */
    fun validateRefreshToken(token: String): Boolean {
        val claims = parseAllClaims(token) ?: return false
        val tokenType = claims["type"] as? String ?: return false
        return tokenType == "refresh"  // Must be a refresh token, not access
    }

    /**
     * Extract the user ID from a token
     *
     * Reads the "subject" field from the token's payload
     * The subject contains the user ID that was encoded when the token was created
     *
     * @param token The JWT token
     * @return The user ID as a string
     * @throws InvalidTokenException if token is invalid or expired
     */
    fun getUserIdFromToken(token: String): String {
        val claims = parseAllClaims(token) ?: throw InvalidTokenException("Invalid token.")
        return claims.subject  // The user ID
    }

    /**
     * Parse and validate a JWT token
     *
     * This is the core validation method that:
     * 1. Strips the "Bearer " prefix if present
     * 2. Verifies the signature using our secret key
     * 3. Checks that the token hasn't expired
     * 4. Returns the claims (payload data) from the token
     *
     * Why it can return null:
     * - Token signature is invalid (token was tampered with)
     * - Token is expired
     * - Token format is malformed
     * - Any other parsing error
     *
     * @param token The JWT token string
     * @return Claims object containing token data, or null if invalid
     */
    private fun parseAllClaims(token: String): Claims? {
        // Remove "Bearer " prefix if present (common in Authorization headers)
        val rawToken = if(token.startsWith("Bearer ")) {
            token.removePrefix("Bearer ")
        } else token

        return try {
            Jwts.parser()
                .verifyWith(secretKey)           // Use our secret key to verify signature
                .build()
                .parseSignedClaims(rawToken)     // Parse and validate the token
                .payload                         // Extract the payload (claims)
        } catch(e: Exception) {
            // Any exception (expired, invalid signature, etc.) returns null
            null
        }
    }
}