package com.piashcse.utils.extension

import com.piashcse.utils.validator.ConflictException
import com.piashcse.utils.validator.NotFoundException

// ============================================================================
//  STRING EXTENSIONS — concise throw syntax for common patterns
// ============================================================================

/** Throw NotFoundException with specific entity type */
fun String.throwNotFound(resourceName: String): Nothing =
    throw NotFoundException("$resourceName not found")

/** Throw NotFoundException with default message */
fun String.throwNotFound(): Nothing =
    throw NotFoundException()

/** Throw ConflictException */
fun String.throwConflict(resourceName: String): Nothing =
    throw ConflictException("$resourceName already exists")
