package com.piashcse.repository

import com.piashcse.entities.PolicyDocumentTable
import com.piashcse.entities.UserPolicyConsentResponse
import com.piashcse.models.policy.PolicyConsentRequest

interface ConsentRepo {
    /**
     * Records user consent to a policy
     */
    suspend fun recordConsent(userId: String, consentRequest: PolicyConsentRequest): UserPolicyConsentResponse

    /**
     * Gets all consents for a user
     */
    suspend fun getUserConsents(userId: String): List<UserPolicyConsentResponse>

    /**
     * Checks if a user has consented to a specific policy
     */
    suspend fun hasUserConsented(userId: String, policyType: PolicyDocumentTable.PolicyType): Boolean
}