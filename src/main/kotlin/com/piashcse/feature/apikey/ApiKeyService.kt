package com.piashcse.feature.apikey

import com.piashcse.constants.Permission
import com.piashcse.database.entities.ApiKeyDAO
import com.piashcse.database.entities.ApiKeyTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.feature.rbac.RbacService
import com.piashcse.model.request.JwtTokenRequest
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.sha256Hex
import com.piashcse.utils.common.toHex
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.validator.ValidationException
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import java.security.SecureRandom
import java.time.LocalDateTime

@Serializable
data class ApiKeyCreateRequest(
    val name: String,
    /** Permission codes (`order:manage`) or legacy enum names (`ORDER_MANAGE`). */
    val scopes: List<String>? = null,
    /** ISO local date-time; omit for no expiry. */
    val expiresAt: String? = null,
)

@Serializable
data class ApiKeyCreatedResponse(
    val id: String,
    val name: String,
    val prefix: String,
    val scopes: List<String>,
    val key: String,
    val createdAt: String,
)

@Serializable
data class ApiKeyResponse(
    val id: String,
    val name: String,
    val prefix: String,
    val scopes: List<String>,
    val isActive: Boolean,
    val lastUsedAt: String?,
    val expiresAt: String?,
    val createdAt: String,
)

class ApiKeyService {
    private val random = SecureRandom()
    private val rbac = RbacService()

    suspend fun create(
        userId: String,
        request: ApiKeyCreateRequest,
    ): ApiKeyCreatedResponse {
        if (request.name.isBlank()) throw ValidationException("API key name is required")
        val user = query { UserDAO.findById(userId) ?: userId.throwNotFound("User") }
        val allowed = rbac.permissionsForUser(user)
        // Omit scopes to inherit the effective permission set. An explicit empty
        // list means "no scopes" and must not fail open.
        val requested =
            request.scopes
                ?.map { Permission.fromString(it)?.code ?: it.trim() }
                ?.filter { it.isNotBlank() }
                ?.toSet()
                ?: Permission.entries.filter { it.code in allowed }.map { it.code }.toSet()
        val invalid = requested.filter { it !in allowed }
        if (invalid.isNotEmpty()) {
            throw ValidationException("Requested scopes exceed the ${user.userType} role: ${invalid.joinToString()}")
        }
        val expiresAt =
            request.expiresAt?.let {
                runCatching { LocalDateTime.parse(it) }
                    .getOrElse { throw ValidationException("Invalid expiresAt: $it") }
            }
        if (expiresAt != null && !expiresAt.isAfter(currentUtc())) {
            throw ValidationException("expiresAt must be in the future")
        }

        val raw = "ek_" + ByteArray(32).also { random.nextBytes(it) }.toHex()
        val dao =
            query {
                ApiKeyDAO.new {
                    this.userId = user.id
                    this.name = request.name
                    keyHash = sha256Hex(raw)
                    prefix = raw.take(12)
                    scopes = requested.joinToString(",")
                    this.expiresAt = expiresAt
                }
            }
        return ApiKeyCreatedResponse(
            id = dao.id.value,
            name = dao.name,
            prefix = dao.prefix,
            scopes = requested.toList(),
            key = raw,
            createdAt = dao.createdAt.toUtcString(),
        )
    }

    suspend fun list(userId: String): List<ApiKeyResponse> =
        query {
            ApiKeyDAO.find { ApiKeyTable.userId eq userId }
                .orderBy(ApiKeyTable.createdAt to org.jetbrains.exposed.v1.core.SortOrder.DESC)
                .map { it.toResponse() }
        }

    suspend fun revoke(
        userId: String,
        keyId: String,
    ): Boolean =
        query {
            val key = ApiKeyDAO.findById(keyId) ?: return@query false
            if (key.userId.value != userId) return@query false
            key.isActive = false
            true
        }

    /** Resolves an `ek_...` token to the same principal shape used by JWT routes. */
    suspend fun authenticate(rawKey: String): JwtTokenRequest? {
        if (!rawKey.startsWith("ek_")) return null
        val resolved =
            query {
                val key =
                    ApiKeyDAO.find {
                        (ApiKeyTable.keyHash eq sha256Hex(rawKey)) and (ApiKeyTable.isActive eq true)
                    }.firstOrNull() ?: return@query null
                if (key.expiresAt?.isBefore(currentUtc()) == true) return@query null
                val user = UserDAO.findById(key.userId.value) ?: return@query null
                if (!user.isActiveAndVerified()) return@query null
                val now = currentUtc()
                if (key.lastUsedAt?.isBefore(now.minusMinutes(5)) ?: true) {
                    key.lastUsedAt = now
                }
                key to user
            } ?: return null

        val (key, user) = resolved
        val allowed = rbac.permissionsForUser(user)
        // A key can outlive a role change. Keep only scopes the current effective
        // permissions still allow; demotion must never preserve elevated access.
        val scopes =
            key.scopes.split(',')
                .filter { it.isNotBlank() }
                .filter { it in allowed || Permission.fromString(it)?.code in allowed }
                .toSet()
        return JwtTokenRequest(
            userId = user.id.value,
            email = user.email,
            userType = user.userType.name,
            permissions = allowed,
            authzVersion = user.authzVersion,
            scopes = scopes,
            isApiKey = true,
        )
    }

    private fun ApiKeyDAO.toResponse() =
        ApiKeyResponse(
            id = id.value,
            name = name,
            prefix = prefix,
            scopes = scopes.split(',').filter { it.isNotBlank() },
            isActive = isActive,
            lastUsedAt = lastUsedAt?.toUtcString(),
            expiresAt = expiresAt?.toUtcString(),
            createdAt = createdAt.toUtcString(),
        )
}
