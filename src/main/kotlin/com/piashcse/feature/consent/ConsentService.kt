package com.piashcse.feature.consent

import com.piashcse.constants.PolicyType
import com.piashcse.model.request.PolicyConsentRequest
import com.piashcse.model.response.UserPolicyConsentResponse

class ConsentService(private val consentRepo: ConsentRepository) {
    suspend fun recordConsent(
        userId: String,
        consentRequest: PolicyConsentRequest,
    ): UserPolicyConsentResponse = consentRepo.recordConsent(userId, consentRequest)

    suspend fun getUserConsents(userId: String): List<UserPolicyConsentResponse> = consentRepo.getUserConsents(userId)

    suspend fun hasUserConsented(
        userId: String,
        policyType: PolicyType,
    ): Boolean = consentRepo.hasUserConsented(userId, policyType)
}
