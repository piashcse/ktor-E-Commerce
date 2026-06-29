package com.piashcse.utils.common

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
