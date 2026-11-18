package com.piashcse.utils.common

import java.security.MessageDigest
import java.util.HexFormat

/** Stable hex SHA-256 used for API keys, invitation tokens and content hashes. */
fun sha256Hex(value: String): String = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.toByteArray()))

/** Hex encoding for random tokens. */
fun ByteArray.toHex(): String = HexFormat.of().formatHex(this)
