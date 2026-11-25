@file:Suppress("MatchingDeclarationName")

package com.piashcse.plugin

import com.piashcse.config.DotEnv
import com.piashcse.model.request.JwtTokenRequest
import com.piashcse.service.RedisRateLimiter
import com.piashcse.utils.extension.clientIp
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.ratelimit.*
import kotlin.time.Duration.Companion.minutes

/**
 * Rate limit names for different endpoint categories.
 */
object RateLimitNames {
    const val AUTH = "auth"
    const val OTP = "otp"
    const val REFRESH_TOKEN = "refresh_token"
    const val WRITE = "write"
    const val SEARCH = "search"
    const val SELLER_WRITE = "seller_write"
    const val ADMIN_WRITE = "admin_write"
    const val GENERAL = "general"
}

/**
 * Configures rate limiting for the application.
 *
 * All zone limits are configurable via environment variables in "limit:periodMinutes" format.
 * - Per-user zones (write, seller_write, admin_write) use userId as the key when available.
 * - Global zones (auth, search, general) use the client IP address.
 */
fun Application.configureRateLimiting() {
    val authCfg = DotEnv.getRateLimit("RATE_LIMIT_AUTH", 5, 10)
    val otpCfg = DotEnv.getRateLimit("RATE_LIMIT_OTP", 10, 10)
    val refreshCfg = DotEnv.getRateLimit("RATE_LIMIT_REFRESH", 10, 10)
    val writeCfg = DotEnv.getRateLimit("RATE_LIMIT_WRITE", 30, 1)
    val searchCfg = DotEnv.getRateLimit("RATE_LIMIT_SEARCH", 60, 1)
    val sellerCfg = DotEnv.getRateLimit("RATE_LIMIT_SELLER_WRITE", 50, 1)
    val adminCfg = DotEnv.getRateLimit("RATE_LIMIT_ADMIN_WRITE", 100, 1)
    val generalCfg = DotEnv.getRateLimit("RATE_LIMIT_GENERAL", 100, 1)

    val redisUrl = DotEnv.get("REDIS_URL", "")

    install(RateLimit) {
        registerGlobalZone(RateLimitNames.AUTH, authCfg, redisUrl)
        registerGlobalZone(RateLimitNames.OTP, otpCfg, redisUrl)
        registerGlobalZone(RateLimitNames.REFRESH_TOKEN, refreshCfg, redisUrl)
        registerPerUserZone(RateLimitNames.WRITE, writeCfg, redisUrl)
        registerGlobalZone(RateLimitNames.SEARCH, searchCfg, redisUrl)
        registerPerUserZone(RateLimitNames.SELLER_WRITE, sellerCfg, redisUrl)
        registerPerUserZone(RateLimitNames.ADMIN_WRITE, adminCfg, redisUrl)
        registerGlobalZone(RateLimitNames.GENERAL, generalCfg, redisUrl)
    }
}

private fun RateLimitConfig.registerGlobalZone(
    name: String,
    cfg: DotEnv.RateLimitConfig,
    redisUrl: String,
) {
    register(RateLimitName(name)) {
        rateLimiter(name, cfg, redisUrl)
        requestKey { call -> call.clientIp }
    }
}

private fun RateLimitConfig.registerPerUserZone(
    name: String,
    cfg: DotEnv.RateLimitConfig,
    redisUrl: String,
) {
    register(RateLimitName(name)) {
        rateLimiter(name, cfg, redisUrl)
        requestKey { call ->
            call.principal<JwtTokenRequest>()?.userId?.let { "user:$it" }
                ?: call.clientIp
        }
    }
}

private fun RateLimitProviderConfig.rateLimiter(
    zone: String,
    cfg: DotEnv.RateLimitConfig,
    redisUrl: String,
) {
    if (redisUrl.isBlank()) {
        rateLimiter(limit = cfg.limit, refillPeriod = cfg.refillMinutes.minutes)
    } else {
        rateLimiter { _, key ->
            RedisRateLimiter(
                zone = zone,
                key = key.toString(),
                limit = cfg.limit,
                refillMillis = cfg.refillMinutes * 60_000,
            )
        }
    }
}
