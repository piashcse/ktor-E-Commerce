package com.piashcse.plugin

import io.ktor.server.application.*
import io.ktor.server.plugins.ratelimit.*
import kotlin.time.Duration.Companion.minutes

/**
 * Rate limit names for different endpoint categories.
 */
object RateLimitNames {
    const val AUTH = "auth"
    const val GENERAL = "general"
}

/**
 * Configures rate limiting for the application.
 *
 * Defines two rate limit zones:
 * - "auth": 5 requests per 10 minutes (login, register, password reset)
 * - "general": 100 requests per minute (all other endpoints)
 */
fun Application.configureRateLimiting() {
    install(RateLimit) {
        registerRateLimitZone(RateLimitNames.AUTH, limit = 5, refillEvery = 10.minutes)
        registerRateLimitZone(RateLimitNames.GENERAL, limit = 100, refillEvery = 1.minutes)
    }
}

private fun RateLimitConfig.registerRateLimitZone(name: String, limit: Int, refillEvery: kotlin.time.Duration) {
    register(RateLimitName(name)) {
        rateLimiter(limit = limit, refillPeriod = refillEvery)
        requestKey { call -> call.request.local.remoteHost }
    }
}
