package com.piashcse.plugin

import io.ktor.server.application.*
import io.ktor.server.plugins.ratelimit.*
import kotlin.time.Duration.Companion.minutes

/**
 * Rate limiting configuration names
 */
object RateLimitNames {
    const val AUTH = "auth"
    const val SEARCH = "search"
    const val GENERAL = "general"
}

/**
 * Configures rate limiting for the application.
 * Protects against brute-force attacks and API abuse.
 */
fun Application.configureRateLimit() {
    install(RateLimit) {
        // Global rate limit: 100 requests per minute
        register(RateLimitName(RateLimitNames.GENERAL)) {
            rateLimiter(limit = 100, refillPeriod = 1.minutes)
        }

        // Auth endpoints: 5 requests per minute (brute-force protection)
        register(RateLimitName(RateLimitNames.AUTH)) {
            rateLimiter(limit = 5, refillPeriod = 1.minutes)
        }

        // Search endpoint: 30 requests per minute (scraping protection)
        register(RateLimitName(RateLimitNames.SEARCH)) {
            rateLimiter(limit = 30, refillPeriod = 1.minutes)
        }
    }
}
