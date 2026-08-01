package com.piashcse.feature.policy

import com.piashcse.constants.PolicyType
import com.piashcse.model.request.CreatePolicyRequest
import com.piashcse.model.request.UpdatePolicyRequest
import com.piashcse.model.response.PolicyDocumentResponse
import com.piashcse.utils.extension.suspendRetryQuery

class PolicyService(private val policyRepo: PolicyRepository) {
    /**
     * Creates a policy document. Runs in a retryable transaction.
     */
    suspend fun createPolicy(createPolicyRequest: CreatePolicyRequest): PolicyDocumentResponse =
        suspendRetryQuery { policyRepo.createPolicy(createPolicyRequest) }

    /**
     * Updates a policy document. Runs in a retryable transaction.
     */
    suspend fun updatePolicy(
        id: String,
        updatePolicyRequest: UpdatePolicyRequest,
    ): PolicyDocumentResponse = suspendRetryQuery { policyRepo.updatePolicy(id, updatePolicyRequest) }

    suspend fun getPolicyByType(type: PolicyType): PolicyDocumentResponse = policyRepo.getPolicyByType(type)

    suspend fun getPolicyById(id: String): PolicyDocumentResponse = policyRepo.getPolicyById(id)

    suspend fun getAllPolicies(type: PolicyType? = null): List<PolicyDocumentResponse> = policyRepo.getAllPolicies(type)

    /**
     * Deactivates a policy document. Runs in a retryable transaction.
     */
    suspend fun deactivatePolicy(id: String): Boolean = suspendRetryQuery { policyRepo.deactivatePolicy(id) }
}
