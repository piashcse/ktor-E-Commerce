package com.piashcse.feature.auth

import com.piashcse.config.DotEnvConfig
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** AES-GCM encryption for TOTP secrets at rest. */
class MfaSecretCipher(
    keyMaterial: String,
) {
    private val key =
        SecretKeySpec(
            MessageDigest.getInstance("SHA-256").digest(keyMaterial.toByteArray()),
            "AES",
        )
    private val random = SecureRandom()

    fun encrypt(secret: String): String {
        val iv = ByteArray(12).also { random.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(secret.toByteArray())
        return PREFIX + Base64.getEncoder().encodeToString(iv + encrypted)
    }

    fun decrypt(value: String): String {
        if (!value.startsWith(PREFIX)) return value // legacy plaintext rows
        val bytes = Base64.getDecoder().decode(value.removePrefix(PREFIX))
        val iv = bytes.copyOfRange(0, 12)
        val encrypted = bytes.copyOfRange(12, bytes.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return String(cipher.doFinal(encrypted))
    }

    companion object {
        private const val PREFIX = "enc:v1:"

        fun fromConfig(): MfaSecretCipher {
            val material = DotEnvConfig.mfaEncryptionKey.ifBlank { DotEnvConfig.jwtSecret }
            return MfaSecretCipher(material)
        }
    }
}
