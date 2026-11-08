@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.plugin

import io.ktor.http.HttpStatusCode
import io.ktor.openapi.*

/**
 * Runtime OpenAPI schema helpers. Route summaries, tags and descriptions use
 * the official `.describe {}` DSL and KDoc comment metadata; these helpers only
 * declare request/response schemas where code inference cannot see through a
 * response wrapper.
 */
inline fun <reified T : Any> RequestBody.Builder.jsonBody(required: Boolean = true) {
    this.required = required
    content { schema = jsonSchema<T>() }
}

inline fun <reified T : Any> Responses.Builder.ok(description: String = "Success") {
    HttpStatusCode.OK {
        this.description = description
        content { schema = jsonSchema<T>() }
    }
}

inline fun <reified T : Any> Responses.Builder.created(description: String = "Created") {
    HttpStatusCode.Created {
        this.description = description
        content { schema = jsonSchema<T>() }
    }
}
