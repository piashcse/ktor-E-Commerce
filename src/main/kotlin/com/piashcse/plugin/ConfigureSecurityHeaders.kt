package com.piashcse.plugin

import io.ktor.server.application.*

/**
 * Configures security headers for all HTTP responses.
 * 
 * Note: For Ktor 3.x, the recommended approach is to add security headers 
 * at the reverse proxy level (nginx, Cloudflare, etc.) for better performance.
 * 
 * Recommended headers:
 * - X-Content-Type-Options: nosniff
 * - X-Frame-Options: DENY
 * - X-XSS-Protection: 1; mode=block
 * - Referrer-Policy: strict-origin-when-cross-origin
 * - Permissions-Policy: camera=(), microphone=(), geolocation=()
 * - Strict-Transport-Security: max-age=31536000 (production only)
 */
fun Application.configureSecurityHeaders() {
    // Security headers implementation pending Ktor 3.x pipeline API compatibility fix.
    // Add headers via reverse proxy (nginx/Cloudflare) in production.
}

/**
 * Check if the application is running in development mode.
 */
fun isDevelopment(): Boolean {
    val ktorEnv = System.getenv("KTOR_ENV")
    val ktorDev = System.getProperty("io.ktor.development")
    return ktorEnv == "development" || ktorDev == "true"
}
