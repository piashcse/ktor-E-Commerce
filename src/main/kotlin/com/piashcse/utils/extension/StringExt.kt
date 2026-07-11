package com.piashcse.utils.extension

import com.piashcse.constants.Message
import com.piashcse.utils.validator.ConflictException
import com.piashcse.utils.validator.InvalidEnumValueException
import com.piashcse.utils.validator.NotFoundException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID

inline fun <reified T : Enum<T>> String.parseEnum(fieldName: String): T =
    runCatching { enumValueOf<T>(uppercase()) }
        .getOrElse {
            throw InvalidEnumValueException(
                message = Message.Validation.invalidEnumValue(fieldName, this),
                enumName = enumValues<T>().joinToString(", ") { it.name },
                invalidValue = this,
            )
        }

fun String.throwNotFound(resourceName: String): Nothing = throw NotFoundException("$resourceName not found")

fun String.throwNotFound(): Nothing = throw NotFoundException()

fun String.throwConflict(resourceName: String): Nothing = throw ConflictException("$resourceName already exists")

fun String.requireNotBlank(fieldName: String) {
    if (isBlank()) throw ValidationException(Message.Validation.blankField(fieldName))
}

fun String.entityID(table: IdTable<String>) = EntityID(this, table)
