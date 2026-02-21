package org.oxoho.note.controllers

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Simple health check controller
 *
 * This controller provides a basic endpoint to check if the application is running
 * Useful for:
 * - Monitoring systems to verify the app is alive
 * - Load balancers to check instance health
 * - Quick manual testing
 *
 * This endpoint is public (no authentication required) - see SecurityConfig
 */
@RestController
@RequestMapping("/api/v1")
class StatusController {

    /**
     * Health check endpoint
     *
     * Endpoint: GET /api/v1/health
     *
     * What it does:
     * - Returns a simple status object indicating the application is running
     * - No database check or complex logic
     *
     * Example usage:
     * - curl http://localhost:8085/api/v1/health
     * - Should return: {"status":"UP","message":"Application is running"}
     *
     * @return Map with status and message fields
     */
    @GetMapping("/health")
    fun healthCheck(): Map<String, String> {
        return mapOf(
            "status" to "UP",
            "message" to "Application is running"
        )
    }
}