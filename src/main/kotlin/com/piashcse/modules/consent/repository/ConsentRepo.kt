package com.piashcse.modules.consent.repository

import com.piashcse.database.entities.PolicyDocumentTable
import com.piashcse.database.entities.UserPolicyConsentResponse
import com.piashcse.database.models.policy.PolicyConsentRequest

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