package com.piashcse.feature.consent

import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class ConsentCheckResponse(val hasConsented: Boolean)

@Serializable
data class RequiredPolicyResponse(
    val type: String,
    val policyId: String,
    val title: String,
    val version: String,
    val effectiveDate: String,
    val hasConsented: Boolean,
)

@Serializable
data class ConsentRequirementsResponse(
    val required: List<RequiredPolicyResponse>,
    val complete: Boolean,
)
