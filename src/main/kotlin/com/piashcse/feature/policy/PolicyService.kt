package com.piashcse.feature.policy

import com.piashcse.constants.PolicyType
import com.piashcse.model.request.CreatePolicyRequest
import com.piashcse.model.request.UpdatePolicyRequest
import com.piashcse.model.response.PolicyDocumentResponse

class PolicyService(private val policyRepo: PolicyRepository) {
    suspend fun createPolicy(createPolicyRequest: CreatePolicyRequest): PolicyDocumentResponse =
        policyRepo.createPolicy(createPolicyRequest)

    suspend fun updatePolicy(
        id: String,
        updatePolicyRequest: UpdatePolicyRequest,
    ): PolicyDocumentResponse = policyRepo.updatePolicy(id, updatePolicyRequest)

    suspend fun getPolicyByType(type: PolicyType): PolicyDocumentResponse = policyRepo.getPolicyByType(type)

    suspend fun getPolicyById(id: String): PolicyDocumentResponse = policyRepo.getPolicyById(id)

    suspend fun getAllPolicies(type: PolicyType? = null): List<PolicyDocumentResponse> = policyRepo.getAllPolicies(type)

    suspend fun deactivatePolicy(id: String): Boolean = policyRepo.deactivatePolicy(id)
}
