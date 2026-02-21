package org.oxoho.note.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * JWT Authentication Filter
 *
 * This filter runs on EVERY incoming HTTP request to check for JWT tokens
 * It's the gatekeeper that decides if a user is authenticated or not
 *
 * How it works:
 * 1. Runs before any controller methods
 * 2. Looks for "Authorization: Bearer {token}" header
 * 3. If valid token found, marks the user as authenticated
 * 4. If no token or invalid, request proceeds but user is NOT authenticated
 *    (SecurityConfig will then block protected endpoints)
 *
 * Why extend OncePerRequestFilter?
 * - Ensures filter runs exactly once per request (even with forwards/redirects)
 */
@Component // Makes this a Spring-managed component
class JwtAuthFilter(
    private val jwtService: JwtService  // Used to validate tokens
): OncePerRequestFilter() {  // Guarantees filter runs once per request

    /**
     * The main filter method that runs on every HTTP request
     *
     * Flow:
     * 1. Extract Authorization header
     * 2. If header exists and starts with "Bearer "
     *    a. Validate the token
     *    b. Extract user ID from token
     *    c. Mark user as authenticated in Spring Security context
     * 3. Pass request to next filter/controller
     *
     * Important:
     * - If token is missing or invalid, we DON'T return error here
     * - We just don't set authentication
     * - SecurityConfig will later reject unauthenticated requests to protected endpoints
     *
     * @param request The incoming HTTP request
     * @param response The HTTP response (not modified here)
     * @param filterChain Chain of filters to continue processing
     */
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // Try to get Authorization header (e.g., "Bearer eyJhbGc...")
        val authHeader = request.getHeader("Authorization")

        // Check if header exists and has correct format
        if(authHeader != null && authHeader.startsWith("Bearer ")) {
            // Validate that it's a valid, unexpired access token
            if(jwtService.validateAccessToken(authHeader)) {
                // Extract user ID from the token
                val userId = jwtService.getUserIdFromToken(authHeader)

                // Create authentication object
                // - userId as principal (the authenticated user)
                // - null credentials (no password needed for JWT)
                // - emptyList() for authorities/roles (we don't use roles in this app)
                val auth = UsernamePasswordAuthenticationToken(userId, null, emptyList())

                // Store authentication in Spring Security context
                // This makes the user "logged in" for this request
                // Controllers can access userId via @AuthenticationPrincipal
                SecurityContextHolder.getContext().authentication = auth
            }
            // If token is invalid, we simply don't set authentication
            // The request continues but user is NOT authenticated
        }

        // Continue to next filter or controller
        // This MUST be called or the request will hang
        filterChain.doFilter(request, response)
    }
}