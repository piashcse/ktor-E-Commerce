package com.piashcse.feature.loyalty

import java.security.SecureRandom

/** Human-friendly referral/gift-card code generator. */
internal object LoyaltyCodes {
    private val random = SecureRandom()
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    fun random(length: Int): String = (1..length).map { ALPHABET[random.nextInt(ALPHABET.length)] }.joinToString("")
}
