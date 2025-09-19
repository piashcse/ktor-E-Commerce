package com.piashcse.utils

import io.github.cdimascio.dotenv.Dotenv
import io.github.cdimascio.dotenv.dotenv

object DotEnv {
    private val dotenv: Dotenv = dotenv {
        directory = "./"
        filename = ".env"
        ignoreIfMissing = true
        ignoreIfMalformed = true
    }

    fun get(key: String): String? = dotenv[key]

    fun get(key: String, defaultValue: String): String = dotenv[key] ?: defaultValue

    fun getInt(key: String, defaultValue: Int): Int = dotenv[key]?.toIntOrNull() ?: defaultValue

    fun getBoolean(key: String, defaultValue: Boolean): Boolean = dotenv[key]?.toBoolean() ?: defaultValue
}