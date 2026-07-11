package com.piashcse.plugin

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private const val SUNSET_HEADER = "Sunset"
private const val DEPRECATION_HEADER = "Deprecation"

/**
 * Helper to mark a route handler as deprecated with a Sunset header.
 */
fun Route.deprecated(
    version: String,
    sunsetDate: String? = null,
    migrationGuide: String? = null,
    build: Route.() -> Unit,
) {
    route("") {
        build()
    }.apply {
        val sunsetValue = sunsetDate ?: "unknown"
        val deprecationValue = "version=\"$version\""
        val linkValue = migrationGuide?.let { "<$it>; rel=\"migration\"" }

        children.forEach { child ->
            child.intercept(ApplicationCallPipeline.Call) {
                call.response.header(SUNSET_HEADER, sunsetValue)
                call.response.header(DEPRECATION_HEADER, deprecationValue)
                linkValue?.let { call.response.header(HttpHeaders.Link, it) }
            }
        }
    }
}

/**
 * Extension to add the Sunset header to a single route.
 */
fun Route.withSunset(
    sunsetDate: String,
    deprecationVersion: String = "v1",
    migrationGuide: String? = null,
): Route {
    intercept(ApplicationCallPipeline.Call) {
        call.response.header(SUNSET_HEADER, sunsetDate)
        call.response.header(DEPRECATION_HEADER, "version=\"$deprecationVersion\"")
        migrationGuide?.let { call.response.header(HttpHeaders.Link, "<$it>; rel=\"migration\"") }
    }
    return this
}
