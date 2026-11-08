package com.piashcse.config

import io.github.cdimascio.dotenv.Dotenv
import io.github.cdimascio.dotenv.dotenv

object DotEnv {
    private val source: Dotenv =
        dotenv {
            directory = "./"
            filename = ".env"
            ignoreIfMissing = true
            ignoreIfMalformed = true
        }

    // 12-factor: process environment wins over a local .env file so container
    // deployments can always override a stale developer .env.
    fun get(key: String): String? = System.getenv(key) ?: source[key]

    fun get(
        key: String,
        defaultValue: String,
    ): String = System.getenv(key) ?: source[key] ?: defaultValue

    fun getInt(
        key: String,
        defaultValue: Int,
    ): Int = (System.getenv(key) ?: source[key])?.toIntOrNull() ?: defaultValue

    data class RateLimitConfig(val limit: Int, val refillMinutes: Long)

    fun getRateLimit(
        key: String,
        defaultLimit: Int,
        defaultPeriodMinutes: Long,
    ): RateLimitConfig {
        val value = System.getenv(key) ?: source[key]
        if (value != null) {
            val parts = value.split(":")
            if (parts.size == 2) {
                val limit = parts[0].toIntOrNull()
                val minutes = parts[1].toLongOrNull()
                if (limit != null && minutes != null) {
                    return RateLimitConfig(limit, minutes)
                }
            }
        }
        return RateLimitConfig(defaultLimit, defaultPeriodMinutes)
    }

    fun getBoolean(
        key: String,
        defaultValue: Boolean,
    ): Boolean = (System.getenv(key) ?: source[key])?.toBoolean() ?: defaultValue
}
