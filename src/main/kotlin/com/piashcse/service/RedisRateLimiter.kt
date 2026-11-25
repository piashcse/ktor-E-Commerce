package com.piashcse.service

import com.piashcse.config.DotEnv
import com.piashcse.utils.common.AppClock
import io.ktor.server.plugins.ratelimit.RateLimiter
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.sync.RedisCommands
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

/**
 * Fixed-window rate limiter shared through Redis, used when `REDIS_URL` is set.
 * Falls back to an in-process limiter per key if Redis is unavailable so a cache
 * outage degrades to single-instance limits instead of failing requests.
 */
class RedisRateLimiter(
    private val zone: String,
    private val key: String,
    private val limit: Int,
    private val refillMillis: Long,
) : RateLimiter {
    private val log = LoggerFactory.getLogger(RedisRateLimiter::class.java)
    private val fallback = ConcurrentHashMap<String, RateLimiter>()

    override suspend fun tryConsume(tokens: Int): RateLimiter.State {
        val commands = RedisRateLimitClient.commands
        if (commands == null) return fallbackLimiter().tryConsume(tokens)

        val now = AppClock.now().toEpochMilli()
        val window = now / refillMillis
        val redisKey = "ratelimit:$zone:$key:$window"
        return runCatching {
            val count =
                withContext(Dispatchers.IO) {
                    val used = commands.incrby(redisKey, tokens.toLong())
                    if (used == tokens.toLong()) commands.pexpire(redisKey, refillMillis)
                    used
                }
            val refillAt = (window + 1) * refillMillis
            if (count <= limit) {
                RateLimiter.State.Available(
                    remainingTokens = (limit - count).toInt().coerceAtLeast(0),
                    limit = limit,
                    refillAtTimeMillis = refillAt,
                )
            } else {
                RateLimiter.State.Exhausted((refillAt - now).coerceAtLeast(1).milliseconds)
            }
        }.getOrElse {
            log.warn("Redis rate limit failed for zone '$zone'; using in-process fallback", it)
            fallbackLimiter().tryConsume(tokens)
        }
    }

    private fun fallbackLimiter(): RateLimiter =
        fallback.getOrPut(key) {
            RateLimiter.default(
                limit = limit,
                refillPeriod = refillMillis.milliseconds,
                clock = { AppClock.now().toEpochMilli() },
            )
        }
}

/** One shared Lettuce connection for all rate-limit zones. */
internal object RedisRateLimitClient {
    private val log = LoggerFactory.getLogger(RedisRateLimitClient::class.java)

    val commands: RedisCommands<String, String>? by lazy {
        DotEnv.get("REDIS_URL", "")
            .takeIf { it.isNotBlank() }
            ?.let { url ->
                runCatching {
                    RedisClient.create(
                        RedisURI.create(url).apply { timeout = Duration.ofSeconds(2) },
                    ).connect().sync()
                }.onFailure {
                    log.warn("Redis rate limiting unavailable; falling back to in-process limits", it)
                }.getOrNull()
            }
    }
}
