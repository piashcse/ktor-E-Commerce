package com.piashcse.utils.common

import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.math.pow

private val secureRandom = SecureRandom()

fun generateOTP(length: Int = 6): String {
    val min = 10.0.pow(length - 1).toInt()
    val max = (10.0.pow(length) - 1).toInt()
    return (secureRandom.nextInt(max - min) + min).toString()
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
