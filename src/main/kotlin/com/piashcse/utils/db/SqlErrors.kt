package com.piashcse.utils.db

import java.sql.SQLException

/**
 * SQLSTATE helpers for translating database constraint failures at the HTTP
 * boundary. Matching on SQLSTATE is stable; matching on driver messages is not.
 */
private fun Throwable.sqlStateOrNull(): String? =
    generateSequence(this) { it.cause }
        .filterIsInstance<SQLException>()
        .firstNotNullOfOrNull { it.sqlState }

fun Throwable.isUniqueViolation(): Boolean = sqlStateOrNull() == UNIQUE_VIOLATION

fun Throwable.isForeignKeyViolation(): Boolean = sqlStateOrNull() == FOREIGN_KEY_VIOLATION

fun Throwable.isNotNullViolation(): Boolean = sqlStateOrNull() == NOT_NULL_VIOLATION

fun Throwable.isCheckViolation(): Boolean = sqlStateOrNull() == CHECK_VIOLATION

/** SQLSTATE class 22: invalid/truncated client data (bad date, overlong varchar, etc.). */
fun Throwable.isDataException(): Boolean = sqlStateOrNull()?.startsWith("22") == true

const val UNIQUE_VIOLATION = "23505"
const val FOREIGN_KEY_VIOLATION = "23503"
const val NOT_NULL_VIOLATION = "23502"
const val CHECK_VIOLATION = "23514"
