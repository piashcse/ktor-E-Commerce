package com.piashcse.utils

import io.github.cdimascio.dotenv.dotenv

object DotEnv {
    private val dotenv = dotenv {
        directory = "./"
        filename = ".env"
        ignoreIfMissing = true
    }

    fun get(key: String): String? {
        return dotenv[key]
    }

    fun get(key: String, defaultValue: String): String {
        return dotenv[key] ?: defaultValue
    }

    fun getInt(key: String, defaultValue: Int): Int {
        return dotenv[key]?.toIntOrNull() ?: defaultValue
    }

    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return dotenv[key]?.toBoolean() ?: defaultValue
    }
}