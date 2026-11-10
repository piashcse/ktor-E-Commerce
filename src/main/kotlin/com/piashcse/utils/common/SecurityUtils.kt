package com.piashcse.utils.common

import java.security.MessageDigest
import java.security.SecureRandom

private val secureRandom = SecureRandom()

fun generateOTP(length: Int = 6): String {
    require(length in 1..9) { "OTP length must be between 1 and 9" }
    var min = 1
    repeat(length - 1) { min *= 10 }
    val range = 9 * min
    return (min + secureRandom.nextInt(range)).toString()
}

fun generateToken(): String {
    val bytes = ByteArray(32)
    secureRandom.nextBytes(bytes)
    return bytes.joinToString("") { "%02x".format(it) }
}

/** Constant-time string comparison to avoid timing side-channel attacks. */
fun constantTimeEquals(
    a: String,
    b: String,
): Boolean = MessageDigest.isEqual(a.toByteArray(Charsets.UTF_8), b.toByteArray(Charsets.UTF_8))
