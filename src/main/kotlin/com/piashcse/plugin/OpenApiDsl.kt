package com.piashcse.plugin

import io.ktor.http.HttpStatusCode
import io.ktor.openapi.*
import io.ktor.server.routing.Route
import io.ktor.server.routing.openapi.describe
import io.ktor.utils.io.ExperimentalKtorApi

/**
 * Concise OpenAPI annotations. Tags are applied at the route-group level in
 * [ConfigureRouting]; these helpers add per-operation metadata near the handler.
 */
@OptIn(ExperimentalKtorApi::class)
fun Route.docs(
    summary: String,
    description: String? = null,
    configure: Operation.Builder.() -> Unit = {},
): Route =
    describe {
        this.summary = summary
        if (description != null) this.description = description
        configure()
    }

/** Declares a JSON request body and infers its schema from the serializer. */
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
