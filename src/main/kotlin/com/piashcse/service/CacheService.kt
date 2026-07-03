package com.piashcse.service

import com.piashcse.config.DotEnvConfig
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

interface Cache {
    suspend fun <T> get(key: String): T?
    suspend fun <T> set(key: String, value: T, ttlSeconds: Long = 300)
    suspend fun invalidate(key: String)
    suspend fun invalidatePattern(pattern: String)
}

object NoOpCache : Cache {
    override suspend fun <T> get(key: String): T? = null
    override suspend fun <T> set(key: String, value: T, ttlSeconds: Long) = Unit
    override suspend fun invalidate(key: String) = Unit
    override suspend fun invalidatePattern(pattern: String) = Unit
}

class MemoryCache(private val defaultTtlSeconds: Long = 300) : Cache {
    private data class CacheEntry(val value: Any?, val expiresAt: Long)
    private val store = ConcurrentHashMap<String, CacheEntry>()

    override suspend fun <T> get(key: String): T? {
        val entry = store[key] ?: return null
        if (System.currentTimeMillis() > entry.expiresAt) {
            store.remove(key)
            return null
        }
        @Suppress("UNCHECKED_CAST")
        return entry.value as? T
    }

    override suspend fun <T> set(key: String, value: T, ttlSeconds: Long) {
        store[key] = CacheEntry(value, System.currentTimeMillis() + ttlSeconds * 1000)
    }

    override suspend fun invalidate(key: String) { store.remove(key) }

    override suspend fun invalidatePattern(pattern: String) {
        val regex = pattern.toRegex()
        store.keys.removeAll { it.matches(regex) }
    }
}

object CacheService {
    private val log = LoggerFactory.getLogger(CacheService::class.java)
    val cache: Cache = createCache()

    private fun createCache(): Cache {
        val redisUrl = runCatching { DotEnvConfig.redisUrl }.getOrNull()
        return if (!redisUrl.isNullOrBlank()) {
            log.warn("Redis support not yet implemented, falling back to in-memory cache")
            MemoryCache(defaultTtlSeconds = 300)
        } else {
            log.info("Using in-memory cache (set REDIS_URL in .env for Redis)")
            MemoryCache(defaultTtlSeconds = 300)
        }
    }
}
