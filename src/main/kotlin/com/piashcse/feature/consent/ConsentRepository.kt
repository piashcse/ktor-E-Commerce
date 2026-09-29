package com.piashcse.feature.consent

import com.piashcse.constants.AppConstants
import com.piashcse.constants.PolicyType
import com.piashcse.constants.UserType
import com.piashcse.model.request.PolicyConsentRequest
import com.piashcse.model.response.UserPolicyConsentResponse
import com.piashcse.utils.common.PaginatedResponse

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
    suspend fun getUserConsents(
        userId: String,
        limit: Int = AppConstants.Pagination.DEFAULT_LIMIT,
        offset: Int = AppConstants.Pagination.DEFAULT_OFFSET,
    ): PaginatedResponse<UserPolicyConsentResponse>

    /**
     * Checks if a user has consented to a specific policy
     */
    suspend fun hasUserConsented(
        userId: String,
        policyType: PolicyType,
    ): Boolean

    /**
     * Revokes a consent record. The caller must own the consent or be an admin.
     */
    suspend fun revokeConsent(
        consentId: String,
        userId: String,
        userType: UserType,
    ): Boolean

    /**
     * Lists consent records, optionally filtered by user and/or policy type
     */
    suspend fun listConsentsByUser(
        userId: String?,
        policyType: PolicyType?,
    ): List<UserPolicyConsentResponse>
}
