@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.plugin

import com.piashcse.utils.common.ApiError
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

/**
 * Declares the standard error envelope for a route. Call alongside the success
 * response so generated clients get typed 4xx/5xx handling.
 */
fun Responses.Builder.standardErrors() {
    HttpStatusCode.BadRequest {
        description = "Validation or malformed request"
        content { schema = jsonSchema<ApiError>() }
    }
    HttpStatusCode.Unauthorized {
        description = "Missing or invalid credentials"
        content { schema = jsonSchema<ApiError>() }
    }
    HttpStatusCode.Forbidden {
        description = "Insufficient privileges"
        content { schema = jsonSchema<ApiError>() }
    }
    HttpStatusCode.NotFound {
        description = "Resource not found"
        content { schema = jsonSchema<ApiError>() }
    }
    HttpStatusCode.Conflict {
        description = "State conflict or duplicate resource"
        content { schema = jsonSchema<ApiError>() }
    }
    HttpStatusCode.TooManyRequests {
        description = "Rate limit exceeded"
        content { schema = jsonSchema<ApiError>() }
    }
    HttpStatusCode.InternalServerError {
        description = "Unexpected server error"
        content { schema = jsonSchema<ApiError>() }
    }
}
