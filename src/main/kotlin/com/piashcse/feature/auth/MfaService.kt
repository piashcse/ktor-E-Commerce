package com.piashcse.feature.auth

import com.piashcse.database.entities.UserDAO
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.validator.ValidationException
import kotlinx.serialization.Serializable
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.and

@Serializable
data class MfaSetupResponse(
    val secret: String,
    val otpauthUrl: String,
)

@Serializable
data class MfaCodeRequest(val code: String)

/**
 * RFC 6238 TOTP (SHA-1, 6 digits, 30-second period, ±1 step window).
 * Secrets are stored per user and MFA is enabled only after a valid code.
 */
class MfaService(
    private val cipher: MfaSecretCipher = MfaSecretCipher.fromConfig(),
) {
    private val random = SecureRandom()

    suspend fun setup(userId: String): MfaSetupResponse =
        query {
            val user = UserDAO.findById(userId) ?: userId.throwNotFound("User")
            if (user.mfaEnabled) throw ValidationException("MFA is already enabled")
            val secret = Base32.encode(ByteArray(20).also { random.nextBytes(it) })
            user.mfaSecret = cipher.encrypt(secret)
            user.mfaEnabled = false
            MfaSetupResponse(secret, otpauthUrl(user.email, secret))
        }

    suspend fun enable(
        userId: String,
        code: String,
    ): Boolean =
        query {
            val user = UserDAO.findById(userId) ?: userId.throwNotFound("User")
            if (user.mfaEnabled) return@query true
            val secret = user.mfaSecret ?: throw ValidationException("Start MFA setup first")
            if (!verify(cipher.decrypt(secret), code)) throw ValidationException("Invalid MFA code")
            user.mfaEnabled = true
            true
        }

    suspend fun disable(
        userId: String,
        code: String,
    ): Boolean =
        query {
            val user = UserDAO.findById(userId) ?: userId.throwNotFound("User")
            if (!user.mfaEnabled) return@query true
            val secret = user.mfaSecret ?: throw ValidationException("MFA is not configured")
            if (!verify(cipher.decrypt(secret), code)) throw ValidationException("Invalid MFA code")
            user.mfaEnabled = false
            user.mfaSecret = null
            true
        }

    /** Returns true when MFA is disabled or the supplied code is valid. */
    suspend fun verifyLogin(
        userId: String,
        code: String?,
    ): Boolean =
        query {
            val user = UserDAO.findById(userId) ?: userId.throwNotFound("User")
            if (!user.mfaEnabled) return@query true
            val secret = user.mfaSecret ?: return@query false
            code != null && verify(cipher.decrypt(secret), code)
        }

    fun verifyStoredSecret(
        storedSecret: String,
        code: String,
        nowMillis: Long = System.currentTimeMillis(),
    ): Boolean = verifyStoredStep(storedSecret, code, nowMillis) != null

    fun verifyStoredStep(
        storedSecret: String,
        code: String,
        nowMillis: Long = System.currentTimeMillis(),
    ): Long? = verifyStep(cipher.decrypt(storedSecret), code, nowMillis)

    fun verify(
        secret: String,
        code: String,
        nowMillis: Long = System.currentTimeMillis(),
    ): Boolean = verifyStep(secret, code, nowMillis) != null

    /** Returns the matched TOTP step so callers can reject replay of the same step. */
    fun verifyStep(
        secret: String,
        code: String,
        nowMillis: Long = System.currentTimeMillis(),
    ): Long? {
        if (!code.matches(Regex("\\d{6}"))) return null
        val current = nowMillis / 1000 / 30
        return (-1L..1L).firstOrNull { offset ->
            generateCode(secret, current + offset) == code
        }?.let { current + it }
    }

    fun generateCode(
        secret: String,
        timeStep: Long,
    ): String {
        val key = Base32.decode(secret)
        val data = ByteBuffer.allocate(8).putLong(timeStep).array()
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key, "HmacSHA1"))
        val hash = mac.doFinal(data)
        val offset = (hash[hash.size - 1] and 0x0f).toInt()
        val binary =
            ((hash[offset].toInt() and 0x7f) shl 24) or
                ((hash[offset + 1].toInt() and 0xff) shl 16) or
                ((hash[offset + 2].toInt() and 0xff) shl 8) or
                (hash[offset + 3].toInt() and 0xff)
        return (binary % 1_000_000).toString().padStart(6, '0')
    }

    private fun otpauthUrl(
        account: String,
        secret: String,
        issuer: String = "Ktor E-Commerce",
    ): String {
        val label = "$issuer:$account"
        return "otpauth://totp/${label.replace(" ", "%20")}?secret=$secret&issuer=${issuer.replace(" ", "%20")}&digits=6&period=30"
    }
}

/** Minimal RFC 4648 Base32 (no padding) for authenticator-app secrets. */
object Base32 {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    fun encode(bytes: ByteArray): String {
        val out = StringBuilder((bytes.size * 8 + 4) / 5)
        var buffer = 0
        var bits = 0
        for (byte in bytes) {
            buffer = (buffer shl 8) or (byte.toInt() and 0xff)
            bits += 8
            while (bits >= 5) {
                out.append(ALPHABET[(buffer shr (bits - 5)) and 0x1f])
                bits -= 5
            }
        }
        if (bits > 0) out.append(ALPHABET[(buffer shl (5 - bits)) and 0x1f])
        return out.toString()
    }

    fun decode(value: String): ByteArray {
        val clean = value.uppercase().replace("=", "")
        val out = java.io.ByteArrayOutputStream()
        var buffer = 0
        var bits = 0
        for (char in clean) {
            val index = ALPHABET.indexOf(char)
            require(index >= 0) { "Invalid Base32 character: $char" }
            buffer = (buffer shl 5) or index
            bits += 5
            if (bits >= 8) {
                out.write((buffer shr (bits - 8)) and 0xff)
                bits -= 8
            }
        }
        return out.toByteArray()
    }
}
