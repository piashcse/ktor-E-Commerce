package com.piashcse.config

import io.github.cdimascio.dotenv.Dotenv
import io.github.cdimascio.dotenv.dotenv

object DotEnv {
    private val dotenv: Dotenv =
        dotenv {
            directory = "./"
            filename = ".env"
            ignoreIfMissing = true
            ignoreIfMalformed = true
        }

    fun get(key: String): String? = dotenv[key] ?: System.getenv(key)

    fun get(
        key: String,
        defaultValue: String,
    ): String = dotenv[key] ?: System.getenv(key) ?: defaultValue

    fun getInt(
        key: String,
        defaultValue: Int,
    ): Int = (dotenv[key] ?: System.getenv(key))?.toIntOrNull() ?: defaultValue

    data class RateLimitConfig(val limit: Int, val refillMinutes: Long)

    fun getRateLimit(
        key: String,
        defaultLimit: Int,
        defaultPeriodMinutes: Long,
    ): RateLimitConfig {
        val value = dotenv[key] ?: System.getenv(key)
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
    ): Boolean = (dotenv[key] ?: System.getenv(key))?.toBoolean() ?: defaultValue
}
