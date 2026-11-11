package com.piashcse.feature.auth

import com.piashcse.constants.UserType
import com.piashcse.database.entities.LoginResponse
import com.piashcse.database.entities.PasskeyCredentialDAO
import com.piashcse.database.entities.PasskeyCredentialTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.UserTable
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.mapper.toUserResponse
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.validator.ValidationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class CredentialDescriptor(
    val id: String,
    val type: String = "public-key",
    val transports: List<String> = emptyList(),
)

@Serializable
data class PasskeyRegisterOptionsResponse(
    val challenge: String,
    val rpId: String,
    val rpName: String,
    val userId: String,
    val userName: String,
    val userDisplayName: String,
    val timeout: Long = 60_000,
    val excludeCredentials: List<CredentialDescriptor> = emptyList(),
)

@Serializable
data class PasskeyRegisterRequest(
    val credentialId: String,
    val clientDataJson: String,
    val publicKey: String,
    val name: String? = null,
    val transports: List<String> = emptyList(),
)

@Serializable
data class PasskeyLoginOptionsRequest(
    val email: String,
    val userType: String = "customer",
)

@Serializable
data class PasskeyLoginOptionsResponse(
    val challenge: String,
    val rpId: String,
    val timeout: Long = 60_000,
    val allowCredentials: List<CredentialDescriptor> = emptyList(),
)

@Serializable
data class PasskeyLoginRequest(
    val email: String,
    val userType: String = "customer",
    val credentialId: String,
    val clientDataJson: String,
    val authenticatorData: String,
    val signature: String,
)

@Serializable
data class PasskeyCredentialResponse(
    val id: String,
    val credentialId: String,
    val name: String?,
    val transports: String?,
    val signCount: Long,
    val createdAt: String?,
    val lastUsedAt: String?,
)

/**
 * Minimal WebAuthn service. Challenges are kept in memory (5 minutes) because the API is
 * single-node; assertion verification uses the stored ES256 SPKI public key.
 */
class PasskeyService(
    private val authRepo: AuthRepository,
    private val rpId: String,
    private val rpName: String,
    private val origin: String,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private data class Challenge(
        val userId: String,
        val type: String,
        val expiresAt: Long,
    )

    private val challenges = ConcurrentHashMap<String, Challenge>()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun registerOptions(userId: String): PasskeyRegisterOptionsResponse {
        val user = query { UserDAO.findById(userId) ?: userId.throwNotFound("User") }
        val challenge = UUID.randomUUID().toString()
        challenges[challenge] = Challenge(user.id.value, "register", clock() + 5 * 60_000)
        val existing =
            query {
                PasskeyCredentialDAO.find {
                    PasskeyCredentialTable.userId eq userId.entityID(UserTable)
                }.map { it.toDescriptor() }
            }
        return PasskeyRegisterOptionsResponse(
            challenge = challenge,
            rpId = rpId,
            rpName = rpName,
            userId = base64Url(userId.toByteArray()),
            userName = user.email,
            userDisplayName = user.email,
            excludeCredentials = existing,
        )
    }

    suspend fun register(
        userId: String,
        request: PasskeyRegisterRequest,
    ): PasskeyCredentialResponse {
        verifyClientData(request.clientDataJson, "webauthn.create", userId, "register")
        val existing =
            query {
                PasskeyCredentialDAO.find { PasskeyCredentialTable.credentialId eq request.credentialId }
                    .firstOrNull()
            }
        if (existing != null) throw ValidationException("Passkey is already registered")
        validatePublicKey(request.publicKey)
        return query {
            PasskeyCredentialDAO.new {
                this.userId = userId.entityID(UserTable)
                this.credentialId = request.credentialId
                this.publicKey = request.publicKey
                this.signCount = 0
                this.transports = request.transports.joinToString(",").takeIf { it.isNotBlank() }
                this.name = request.name
            }.toResponse()
        }
    }

    suspend fun loginOptions(request: PasskeyLoginOptionsRequest): PasskeyLoginOptionsResponse {
        val userType = UserType.fromString(request.userType) ?: throw ValidationException("Invalid user type")
        val user =
            authRepo.findUserByEmailAndType(request.email.lowercase(), userType)
                ?: throw ValidationException("No passkey is registered for this account")
        val challenge = UUID.randomUUID().toString()
        challenges[challenge] = Challenge(user.id.value, "login", clock() + 5 * 60_000)
        val credentials =
            query {
                PasskeyCredentialDAO.find {
                    PasskeyCredentialTable.userId eq user.id
                }.map { it.toDescriptor() }
            }
        return PasskeyLoginOptionsResponse(
            challenge = challenge,
            rpId = rpId,
            allowCredentials = credentials,
        )
    }

    suspend fun login(request: PasskeyLoginRequest): LoginResponse {
        val userType = UserType.fromString(request.userType) ?: throw ValidationException("Invalid user type")
        val user =
            authRepo.findUserByEmailAndType(request.email.lowercase(), userType)
                ?: throw ValidationException("Invalid passkey login")
        val credential =
            query {
                PasskeyCredentialDAO.find {
                    (PasskeyCredentialTable.credentialId eq request.credentialId) and
                        (PasskeyCredentialTable.userId eq user.id)
                }.firstOrNull()
            } ?: throw ValidationException("Invalid passkey login")

        verifyClientData(request.clientDataJson, "webauthn.get", user.id.value, "login")
        if (!verifySignature(credential, request.clientDataJson, request.authenticatorData, request.signature)) {
            throw ValidationException("Invalid passkey signature")
        }
        if (!user.isActive) throw ValidationException("Account has been deactivated")
        query {
            credential.signCount = maxOf(credential.signCount, 0)
            credential.lastUsedAt = currentUtc()
        }
        val tokens = authRepo.generateTokenPair(user.id.value, user.email, user.userType.name)
        authRepo.storeRefreshToken(user.id.value, tokens.refreshToken)
        return LoginResponse(user.toUserResponse(), tokens.accessToken, tokens.refreshToken, tokens.expiresIn)
    }

    suspend fun list(userId: String): List<PasskeyCredentialResponse> =
        query {
            PasskeyCredentialDAO.find { PasskeyCredentialTable.userId eq userId.entityID(UserTable) }
                .map { it.toResponse() }
        }

    suspend fun delete(
        userId: String,
        id: String,
    ): Boolean =
        query {
            val credential =
                PasskeyCredentialDAO.find {
                    (PasskeyCredentialTable.id eq id.entityID(PasskeyCredentialTable)) and
                        (PasskeyCredentialTable.userId eq userId.entityID(UserTable))
                }.firstOrNull() ?: return@query false
            credential.delete()
            true
        }

    private fun verifyClientData(
        encoded: String,
        expectedType: String,
        userId: String,
        expectedChallengeType: String,
    ) {
        val root =
            runCatching { json.parseToJsonElement(String(decode(encoded))).jsonObject }
                .getOrElse { throw ValidationException("Invalid clientDataJSON") }
        val type = root["type"]?.jsonPrimitive?.contentOrNull
        val challenge =
            root["challenge"]?.jsonPrimitive?.contentOrNull
                ?: throw ValidationException("Missing WebAuthn challenge")
        val actualOrigin = root["origin"]?.jsonPrimitive?.contentOrNull
        if (type != expectedType) throw ValidationException("Invalid WebAuthn ceremony type")
        if (actualOrigin != origin) throw ValidationException("Invalid WebAuthn origin")
        val stored =
            challenges.remove(challenge) ?: throw ValidationException("WebAuthn challenge expired")
        if (stored.userId != userId || stored.type != expectedChallengeType || stored.expiresAt < clock()) {
            throw ValidationException("Invalid WebAuthn challenge")
        }
    }

    private fun verifySignature(
        credential: PasskeyCredentialDAO,
        clientDataJson: String,
        authenticatorData: String,
        signature: String,
    ): Boolean =
        runCatching {
            val key =
                KeyFactory.getInstance("EC").generatePublic(
                    X509EncodedKeySpec(decode(credential.publicKey)),
                )
            val verifier = Signature.getInstance("SHA256withECDSA")
            verifier.initVerify(key)
            verifier.update(decode(authenticatorData))
            verifier.update(MessageDigest.getInstance("SHA-256").digest(decode(clientDataJson)))
            verifier.verify(decode(signature))
        }.getOrDefault(false)

    private fun validatePublicKey(publicKey: String) {
        runCatching {
            KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(decode(publicKey)))
        }.getOrElse { throw ValidationException("Invalid passkey public key") }
    }

    private fun decode(value: String): ByteArray {
        val normalized = value.replace('-', '+').replace('_', '/')
        val padded = normalized + "=".repeat((4 - normalized.length % 4) % 4)
        return Base64.getDecoder().decode(padded)
    }

    private fun base64Url(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    private fun PasskeyCredentialDAO.toResponse() =
        PasskeyCredentialResponse(
            id = id.value,
            credentialId = credentialId,
            name = name,
            transports = transports,
            signCount = signCount,
            createdAt = createdAt.toUtcString(),
            lastUsedAt = lastUsedAt?.toUtcString(),
        )

    private fun PasskeyCredentialDAO.toDescriptor() =
        CredentialDescriptor(
            id = credentialId,
            transports = transports?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
        )
}
