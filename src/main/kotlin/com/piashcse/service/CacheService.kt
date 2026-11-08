package com.piashcse.service

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
        if (System.currentTimeMillis() > entry.expiresAt) {
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
        store[key] = CacheEntry(value, System.currentTimeMillis() + ttl * 1000)
        if (store.size > maxEntries) evict()
    }

    private fun evict() {
        val now = System.currentTimeMillis()
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
 * To use Redis, implement [Cache] with a Redis client (e.g., Lettuce/Jedis)
 * and change [cache] to return a Redis-backed instance.
 *
 * Redis was intentionally omitted from the base project to keep dependencies
 * minimal. For multi-instance deployments, add the Redis dependency and
 * implement a Redis-backed [Cache].
 */
object CacheService {
    val cache: Cache = MemoryCache(defaultTtlSeconds = 300)
}
