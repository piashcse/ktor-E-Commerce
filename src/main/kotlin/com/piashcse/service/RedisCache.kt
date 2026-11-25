package com.piashcse.service

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.ScanArgs
import io.lettuce.core.api.sync.RedisCommands
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.time.Duration

/**
 * Optional Redis-backed [Cache] for multi-instance deployments.
 *
 * Enable with `REDIS_URL=redis://host:6379`. Values are stored as a small tagged
 * text format (booleans, ints, longs, strings, sets and lists of strings), which
 * covers the application cache without Java deserialization.
 */
class RedisCache(
    redisUrl: String,
    private val defaultTtlSeconds: Long = 300,
    private val keyPrefix: String = "ktor-ecom:",
) : Cache {
    private val client =
        RedisClient.create(
            RedisURI.create(redisUrl).apply { timeout = Duration.ofSeconds(2) },
        )
    private val commands: RedisCommands<String, String> = client.connect().sync()

    fun ping(): String = commands.ping()

    override suspend fun <T> get(key: String): T? =
        withContext(Dispatchers.IO) {
            commands.get(keyPrefix + key)?.let { decode(it) }
        }

    override suspend fun <T> set(
        key: String,
        value: T,
        ttlSeconds: Long,
    ) {
        val encoded = encode(value ?: return)
        val ttl = if (ttlSeconds > 0) ttlSeconds else defaultTtlSeconds
        withContext(Dispatchers.IO) {
            commands.setex(keyPrefix + key, ttl, encoded)
        }
    }

    override suspend fun invalidate(key: String) {
        withContext(Dispatchers.IO) {
            commands.del(keyPrefix + key)
        }
    }

    @Suppress("SpreadOperator")
    override suspend fun invalidatePattern(pattern: String) {
        withContext(Dispatchers.IO) {
            val regex = pattern.toRegex()
            val glob = pattern.replace(".*", "*")
            val keys = mutableSetOf<String>()
            var cursor = commands.scan(ScanArgs.Builder.matches(keyPrefix + glob).limit(500))
            while (true) {
                keys += cursor.keys
                if (cursor.isFinished) break
                cursor = commands.scan(cursor, ScanArgs.Builder.matches(keyPrefix + glob).limit(500))
            }
            val matching = keys.filter { it.removePrefix(keyPrefix).matches(regex) }
            if (matching.isNotEmpty()) commands.del(*matching.toTypedArray())
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> decode(raw: String): T =
        when {
            raw.startsWith("b:") -> raw.removePrefix("b:").toBoolean()
            raw.startsWith("i:") -> raw.removePrefix("i:").toInt()
            raw.startsWith("l:") -> raw.removePrefix("l:").toLong()
            raw.startsWith("s:") -> raw.removePrefix("s:")
            raw.startsWith("set:") ->
                Json.decodeFromString(ListSerializer(String.serializer()), raw.removePrefix("set:")).toSet()
            raw.startsWith("list:") ->
                Json.decodeFromString(ListSerializer(String.serializer()), raw.removePrefix("list:"))
            else -> error("Unsupported cached value for key")
        } as T

    private fun encode(value: Any): String =
        when (value) {
            is Boolean -> "b:$value"
            is Int -> "i:$value"
            is Long -> "l:$value"
            is String -> "s:$value"
            is Set<*> -> "set:" + Json.encodeToString(ListSerializer(String.serializer()), value.map { it.toString() })
            is List<*> -> "list:" + Json.encodeToString(ListSerializer(String.serializer()), value.map { it.toString() })
            else -> error("Unsupported cache value type: ${value::class.qualifiedName}")
        }
}
