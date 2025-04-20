package com.piashcse.modules.policy.repository

import com.piashcse.database.entities.PolicyDocumentResponse
import com.piashcse.database.entities.PolicyDocumentTable
import com.piashcse.database.models.policy.CreatePolicyRequest
import com.piashcse.database.models.policy.UpdatePolicyRequest

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