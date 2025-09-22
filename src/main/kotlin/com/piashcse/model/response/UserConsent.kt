package com.piashcse.model.response

/**
 * Response model for user policy consents
 */
data class UserPolicyConsent(
    val id: String,
    val userId: String,
    val policyId: String,
    val consentDate: String,
    val ipAddress: String?,
    val userAgent: String?
)