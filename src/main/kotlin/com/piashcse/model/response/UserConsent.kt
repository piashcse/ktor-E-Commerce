package com.piashcse.model.response

import kotlinx.serialization.Serializable

/**
 * Response model for user policy consents
 */
@Serializable
data class UserPolicyConsent(
    val id: String,
    val userId: String,
    val policyId: String,
    val consentDate: String,
    val ipAddress: String?,
    val userAgent: String?
)