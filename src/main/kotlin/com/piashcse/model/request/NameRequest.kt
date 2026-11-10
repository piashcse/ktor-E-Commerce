package com.piashcse.model.request

import kotlinx.serialization.Serializable
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

/**
 * Common body for catalog create/update endpoints that also accept `?name=`.
 * Validation lives here so query-string and JSON callers get identical 400s.
 */
@Serializable
data class NameRequest(val name: String) {
    init {
        validate(this) {
            validate(NameRequest::name).isNotNull().isNotEmpty()
        }
    }
}
