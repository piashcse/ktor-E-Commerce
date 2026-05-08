package com.piashcse.feature.consent

import com.piashcse.database.entities.PolicyDocumentTable
import com.piashcse.model.request.PolicyConsentRequest
import com.piashcse.model.response.UserPolicyConsentResponse

interface ConsentRepository {
    /**
     * Records user consent to a policy
     */
    suspend fun recordConsent(
        userId: String,
        consentRequest: PolicyConsentRequest,
    ): UserPolicyConsentResponse

    /**
     * Gets all consents for a user
     */
    suspend fun getUserConsents(userId: String): List<UserPolicyConsentResponse>

    /**
     * Checks if a user has consented to a specific policy
     */
    suspend fun hasUserConsented(
        userId: String,
        policyType: PolicyDocumentTable.PolicyType,
    ): Boolean
}
