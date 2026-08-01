package com.piashcse.feature.consent

import com.piashcse.constants.PolicyType
import com.piashcse.model.request.PolicyConsentRequest
import com.piashcse.model.response.UserPolicyConsentResponse
import com.piashcse.utils.extension.requireNotBlank
import com.piashcse.utils.extension.suspendRetryQuery

class ConsentService(private val consentRepo: ConsentRepository) {
    /**
     * Records user consent to a policy after validating the inputs. Runs in a
     * retryable transaction.
     */
    suspend fun recordConsent(
        userId: String,
        consentRequest: PolicyConsentRequest,
    ): UserPolicyConsentResponse = suspendRetryQuery {
        userId.requireNotBlank("User ID")
        consentRequest.policyId.requireNotBlank("Policy ID")
        consentRepo.recordConsent(userId, consentRequest)
    }

    suspend fun getUserConsents(userId: String): List<UserPolicyConsentResponse> = consentRepo.getUserConsents(userId)

    suspend fun hasUserConsented(
        userId: String,
        policyType: PolicyType,
    ): Boolean = consentRepo.hasUserConsented(userId, policyType)
}
