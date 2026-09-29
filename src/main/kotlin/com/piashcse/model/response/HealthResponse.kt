package com.piashcse.model.response

import kotlinx.serialization.Serializable

/** Typed health payload — replaces the ad-hoc mapOf for OpenAPI schema. */
@Serializable
data class HealthResponse(
    val status: String,
    val service: String = "ktor-ecommerce",
    val version: String,
    val timestamp: String,
)
