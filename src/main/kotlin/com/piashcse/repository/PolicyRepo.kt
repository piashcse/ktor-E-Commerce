package com.piashcse.repository

import com.piashcse.entities.PolicyDocumentResponse
import com.piashcse.entities.PolicyDocumentTable
import com.piashcse.entities.UserPolicyConsentResponse
import com.piashcse.models.policy.CreatePolicyRequest
import com.piashcse.models.policy.PolicyConsentRequest
import com.piashcse.models.policy.UpdatePolicyRequest

interface PolicyRepo {
    /**
     * Creates a new policy document
     */
    suspend fun createPolicy(createPolicyRequest: CreatePolicyRequest): PolicyDocumentResponse

    /**
     * Updates an existing policy document
     */
    suspend fun updatePolicy(id: String, updatePolicyRequest: UpdatePolicyRequest): PolicyDocumentResponse

    /**
     * Gets a policy document by type, returning the latest active version
     */
    suspend fun getPolicyByType(type: PolicyDocumentTable.PolicyType): PolicyDocumentResponse

    /**
     * Gets a policy document by ID
     */
    suspend fun getPolicyById(id: String): PolicyDocumentResponse

    /**
     * Gets all policy documents, optionally filtered by type
     */
    suspend fun getAllPolicies(type: PolicyDocumentTable.PolicyType? = null): List<PolicyDocumentResponse>

    /**
     * Deactivates a policy document (doesn't delete, just marks as inactive)
     */
    suspend fun deactivatePolicy(id: String): Boolean
}