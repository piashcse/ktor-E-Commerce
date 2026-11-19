package com.piashcse.utils.common

import java.security.MessageDigest
import java.util.HexFormat
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Stable hex SHA-256 used for API keys, invitation tokens and content hashes. */
fun sha256Hex(value: String): String = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.toByteArray()))

/** Hex-encoded HMAC-SHA256, also used to derive purpose-specific subkeys. */
fun hmacSha256Hex(
    key: String,
    value: String,
): String {
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(SecretKeySpec(key.toByteArray(), "HmacSHA256"))
    return HexFormat.of().formatHex(mac.doFinal(value.toByteArray()))
}

/** Hex encoding for random tokens. */
fun ByteArray.toHex(): String = HexFormat.of().formatHex(this)
