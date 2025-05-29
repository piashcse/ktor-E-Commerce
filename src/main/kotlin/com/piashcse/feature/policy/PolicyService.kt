package com.piashcse.feature.policy

import com.piashcse.database.entities.PolicyDocumentDAO
import com.piashcse.database.entities.PolicyDocumentTable
import com.piashcse.model.request.CreatePolicyRequest
import com.piashcse.model.request.UpdatePolicyRequest
import com.piashcse.model.response.PolicyDocument
import com.piashcse.utils.CommonException
import com.piashcse.utils.extension.notFoundException
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.sql.and

class PolicyService : PolicyRepository {
    /**
     * Creates a new policy document
     */
    override suspend fun createPolicy(createPolicyRequest: CreatePolicyRequest): PolicyDocument = query {
        // Create a new policy document
        val policyDocument = PolicyDocumentDAO.new {
            title = createPolicyRequest.title
            type = createPolicyRequest.type
            content = createPolicyRequest.content
            version = createPolicyRequest.version
            effectiveDate = createPolicyRequest.effectiveDate
        }

        // If this is a new active policy of the same type, deactivate previous versions
        if (policyDocument.isActive) {
            PolicyDocumentDAO.find {
                PolicyDocumentTable.type eq createPolicyRequest.type and
                        (PolicyDocumentTable.id neq policyDocument.id) and
                        (PolicyDocumentTable.isActive eq true)
            }.forEach { it.isActive = false }
        }

        policyDocument.response()
    }

    /**
     * Updates an existing policy document
     */
    override suspend fun updatePolicy(id: String, updatePolicyRequest: UpdatePolicyRequest): PolicyDocument =
        query {
            val policyDocument = PolicyDocumentDAO.findById(id) ?: throw id.notFoundException()

            // Update only the fields that are provided
            updatePolicyRequest.title?.let { policyDocument.title = it }
            updatePolicyRequest.content?.let { policyDocument.content = it }
            updatePolicyRequest.version?.let { policyDocument.version = it }
            updatePolicyRequest.effectiveDate?.let { policyDocument.effectiveDate = it }
            updatePolicyRequest.isActive?.let {
                policyDocument.isActive = it

                // If setting this policy to active, deactivate other policies of the same type
                if (it) {
                    PolicyDocumentDAO.find {
                        PolicyDocumentTable.type eq policyDocument.type and
                                (PolicyDocumentTable.id neq policyDocument.id) and
                                (PolicyDocumentTable.isActive eq true)
                    }.forEach { otherPolicy -> otherPolicy.isActive = false }
                }
            }
            policyDocument.response()
        }

    /**
     * Gets a policy document by type, returning the latest active version
     */
    override suspend fun getPolicyByType(type: PolicyDocumentTable.PolicyType): PolicyDocument = query {
        val policyDocument = PolicyDocumentDAO.find {
            PolicyDocumentTable.type eq type and (PolicyDocumentTable.isActive eq true)
        }.firstOrNull() ?: throw CommonException("No active $type found")

        policyDocument.response()
    }

    /**
     * Gets a policy document by ID
     */
    override suspend fun getPolicyById(id: String): PolicyDocument = query {
        val policyDocument = PolicyDocumentDAO.findById(id) ?: throw id.notFoundException()
        policyDocument.response()
    }

    /**
     * Gets all policy documents, optionally filtered by type
     */
    override suspend fun getAllPolicies(type: PolicyDocumentTable.PolicyType?): List<PolicyDocument> = query {
        val query = if (type != null) {
            PolicyDocumentDAO.find { PolicyDocumentTable.type eq type }
        } else {
            PolicyDocumentDAO.all()
        }

        query.map { it.response() }
    }

    /**
     * Deactivates a policy document (doesn't delete, just marks as inactive)
     */
    override suspend fun deactivatePolicy(id: String): Boolean = query {
        val policyDocument = PolicyDocumentDAO.findById(id) ?: throw id.notFoundException()
        policyDocument.isActive = false
        true
    }
}