package com.piashcse.service

import com.piashcse.config.DotEnv
import com.piashcse.utils.common.AppClock
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

interface Cache {
    suspend fun <T> get(key: String): T?

    suspend fun <T> set(
        key: String,
        value: T,
        ttlSeconds: Long = -1,
    )

    suspend fun invalidate(key: String)

    suspend fun invalidatePattern(pattern: String)
}

object NoOpCache : Cache {
    override suspend fun <T> get(key: String): T? = null

    override suspend fun <T> set(
        key: String,
        value: T,
        ttlSeconds: Long,
    ) = Unit

    override suspend fun invalidate(key: String) = Unit

    override suspend fun invalidatePattern(pattern: String) = Unit
}

class MemoryCache(
    private val defaultTtlSeconds: Long = 300,
    private val maxEntries: Int = 10_000,
) : Cache {
    private data class CacheEntry(val value: Any?, val expiresAt: Long)

    private val store = ConcurrentHashMap<String, CacheEntry>()

    override suspend fun <T> get(key: String): T? {
        val entry = store[key] ?: return null
        if (AppClock.now().toEpochMilli() > entry.expiresAt) {
            store.remove(key)
            return null
        }
        @Suppress("UNCHECKED_CAST")
        return entry.value as? T
    }

    override suspend fun <T> set(
        key: String,
        value: T,
        ttlSeconds: Long,
    ) {
        val ttl = if (ttlSeconds > 0) ttlSeconds else defaultTtlSeconds
        store[key] = CacheEntry(value, AppClock.now().toEpochMilli() + ttl * 1000)
        if (store.size > maxEntries) evict()
    }

    private fun evict() {
        val now = AppClock.now().toEpochMilli()
        store.entries.removeIf { it.value.expiresAt < now }
        if (store.size <= maxEntries) return
        // Still over the cap: drop arbitrary entries until under the limit.
        val iterator = store.keys.iterator()
        while (store.size > maxEntries && iterator.hasNext()) {
            store.remove(iterator.next())
        }
    }

    override suspend fun invalidate(key: String) {
        store.remove(key)
    }

    override suspend fun invalidatePattern(pattern: String) {
        val regex = pattern.toRegex()
        store.keys.removeAll { it.matches(regex) }
    }
}

/**
 * Pluggable cache service.
 *
 * Default backend: [MemoryCache] (in-memory, single-instance).
 * Set `REDIS_URL` to use [RedisCache] for multi-instance deployments. If Redis
 * is configured but unreachable at startup, the app logs and falls back to the
 * in-memory cache so a cache outage does not take the API down.
 */
object CacheService {
    private val log = LoggerFactory.getLogger(CacheService::class.java)
    private const val DEFAULT_TTL_SECONDS = 300L

    val cache: Cache =
        DotEnv.get("REDIS_URL", "")
            .takeIf { it.isNotBlank() }
            ?.let { url ->
                runCatching { RedisCache(url).also { it.ping() } }
                    .onFailure { log.warn("Redis cache unavailable; falling back to in-memory cache", it) }
                    .getOrNull()
            } ?: MemoryCache(defaultTtlSeconds = DEFAULT_TTL_SECONDS)
}
