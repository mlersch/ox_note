package org.oxoho.note.security

import jakarta.servlet.DispatcherType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

/**
 * Spring Security Configuration
 *
 * This class configures the security settings for the entire application
 * It defines:
 * - Which URLs require authentication and which are public
 * - How to handle authentication (JWT tokens instead of sessions)
 * - What to do when authentication fails
 *
 * Key concepts:
 * - STATELESS: No server-side sessions (uses JWT tokens instead)
 * - PUBLIC ENDPOINTS: Login, register, health check, API docs
 * - PROTECTED ENDPOINTS: Everything else (like /api/v1/notes)
 */
@Configuration // Tells Spring this is a configuration class
class SecurityConfig(
    // Custom filter that validates JWT tokens on each request
    private val jwtAuthFilter: JwtAuthFilter
) {

    /**
     * Defines the security filter chain
     *
     * This is the "heart" of Spring Security configuration
     * It sets up all the security rules for incoming HTTP requests
     *
     * @param httpSecurity Spring Security's HTTP security builder
     * @return Configured SecurityFilterChain that Spring will use
     */
    @Bean // Creates a Spring bean that Spring Security will use
    fun filterChain(httpSecurity: HttpSecurity): SecurityFilterChain {
        return httpSecurity
            // 1. DISABLE CSRF (Cross-Site Request Forgery protection)
            //    Why? Because we're using JWT tokens (stateless), not cookies
            //    CSRF protection is mainly needed for cookie-based authentication
            .csrf { csrf -> csrf.disable() }

            // 2. SESSION MANAGEMENT: Set to STATELESS
            //    Tells Spring to NOT create HTTP sessions
            //    Each request must provide its own authentication (JWT token)
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }

            // 3. AUTHORIZATION RULES: Define which URLs need authentication
            .authorizeHttpRequests { auth ->
                auth
                    // PUBLIC ENDPOINTS (no login required)
                    .requestMatchers(
                        "/api/v1/auth/**",      // Login, register, refresh token
                        "/api/v1/health",       // Health check
                        "/api-docs/**",         // OpenAPI documentation
                        "/swagger-ui/**",       // Swagger UI static files
                        "/swagger-ui.html"      // Swagger UI main page
                    )
                    .permitAll() // Anyone can access these URLs

                    // Allow error and forward dispatchers (Spring internal routing)
                    .dispatcherTypeMatchers(
                        DispatcherType.ERROR,
                        DispatcherType.FORWARD
                    )
                    .permitAll()

                    // PROTECTED ENDPOINTS: Everything else requires authentication
                    .anyRequest()
                    .authenticated() // Must have valid JWT token
            }

            // 4. EXCEPTION HANDLING: What to do when authentication fails
            .exceptionHandling { configurer ->
                configurer
                    // If user tries to access protected endpoint without token
                    // Return 401 UNAUTHORIZED status instead of redirecting to login page
                    .authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            }

            // 5. ADD JWT FILTER: Insert our custom JWT validation filter
            //    This filter runs BEFORE Spring's default username/password filter
            //    It extracts and validates the JWT token from each request
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)

            .build() // Build the final SecurityFilterChain
    }
}