package org.oxoho.note.security

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

/**
 * Password Hashing Service
 *
 * This service handles password encryption and verification using BCrypt
 *
 * What is BCrypt?
 * - A password hashing algorithm designed to be SLOW on purpose
 * - Makes brute-force attacks impractical (takes time to try each password)
 * - Includes a random "salt" so same password produces different hashes
 * - Industry standard for password storage
 *
 * Why we NEVER store plain passwords:
 * - If database is compromised, attackers can't use the passwords
 * - Even we (developers) can't see user's actual passwords
 * - Users often reuse passwords, so protecting them is critical
 *
 * Example:
 * - Password: "MyPassword123"
 * - Hashed: "$2a$10$N9qo8uLOickgx2ZMRZoMye7CK4gJgNZ3qFW.u4yLJN5qjPAB4ABPC"
 */
@Component // Makes this a Spring-managed component
class HashEncoder {

    // BCrypt encoder instance - handles all the complex cryptography
    private val bcrypt = BCryptPasswordEncoder()

    /**
     * Hash a plain text password
     *
     * Takes a plain password and converts it to a cryptographic hash
     * Each time you hash the same password, you get a DIFFERENT result
     * (because BCrypt adds a random salt)
     *
     * Use this when:
     * - User registers (hash their password before saving to DB)
     * - User changes password
     *
     * @param raw The plain text password (e.g., "MyPassword123")
     * @return The hashed password (e.g., "$2a$10$N9qo8uLO...")
     */
    fun encode(raw: String): String = bcrypt.encode(raw) ?: throw IllegalStateException("Failed to encode password")

    /**
     * Check if a password matches a hash
     *
     * Compares a plain password against a stored hash to verify it matches
     * BCrypt extracts the salt from the hash and applies same algorithm
     *
     * Use this when:
     * - User logs in (check if entered password matches stored hash)
     *
     * @param raw The plain text password entered by user
     * @param hashed The stored hash from the database
     * @return true if password matches, false otherwise
     */
    fun matches(raw: String, hashed: String): Boolean = bcrypt.matches(raw, hashed)
}