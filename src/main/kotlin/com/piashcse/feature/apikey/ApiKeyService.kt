package com.piashcse.feature.apikey

import com.piashcse.constants.Permission
import com.piashcse.constants.defaultPermissions
import com.piashcse.database.entities.ApiKeyDAO
import com.piashcse.database.entities.ApiKeyTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.model.request.JwtTokenRequest
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.validator.ValidationException
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.LocalDateTime

@Serializable
data class ApiKeyCreateRequest(
    val name: String,
    val scopes: List<Permission>? = null,
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

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }

    suspend fun create(
        userId: String,
        request: ApiKeyCreateRequest,
    ): ApiKeyCreatedResponse =
        query {
            if (request.name.isBlank()) throw ValidationException("API key name is required")
            val user = UserDAO.findById(userId) ?: userId.throwNotFound("User")
            val allowed = user.userType.defaultPermissions()
            val requested =
                request.scopes?.takeIf { it.isNotEmpty() }?.toSet() ?: allowed
            val invalid = requested - allowed
            if (invalid.isNotEmpty()) {
                throw ValidationException(
                    "Requested scopes exceed the ${user.userType} role: ${invalid.joinToString { it.name }}",
                )
            }
            val expiresAt =
                request.expiresAt?.let {
                    runCatching { LocalDateTime.parse(it) }
                        .getOrElse { throw ValidationException("Invalid expiresAt: $it") }
                }
            if (expiresAt != null && !expiresAt.isAfter(currentUtc())) {
                throw ValidationException("expiresAt must be in the future")
            }

            val raw = "ek_" + ByteArray(32).also { random.nextBytes(it) }.joinToString("") { "%02x".format(it) }
            val dao =
                ApiKeyDAO.new {
                    this.userId = user.id
                    this.name = request.name
                    keyHash = sha256(raw)
                    prefix = raw.take(12)
                    scopes = requested.joinToString(",") { it.name }
                    this.expiresAt = expiresAt
                }
            ApiKeyCreatedResponse(
                id = dao.id.value,
                name = dao.name,
                prefix = dao.prefix,
                scopes = requested.map { it.name },
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
    suspend fun authenticate(rawKey: String): JwtTokenRequest? =
        query {
            if (!rawKey.startsWith("ek_")) return@query null
            val key =
                ApiKeyDAO.find {
                    (ApiKeyTable.keyHash eq sha256(rawKey)) and (ApiKeyTable.isActive eq true)
                }.firstOrNull() ?: return@query null
            if (key.expiresAt?.isBefore(currentUtc()) == true) return@query null
            val user = UserDAO.findById(key.userId.value) ?: return@query null
            if (!user.isActiveAndVerified()) return@query null
            key.lastUsedAt = currentUtc()
            JwtTokenRequest(
                userId = user.id.value,
                email = user.email,
                userType = user.userType.name,
                scopes = key.scopes.split(',').filter { it.isNotBlank() }.toSet(),
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
