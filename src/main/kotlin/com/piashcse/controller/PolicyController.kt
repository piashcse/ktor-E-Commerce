package com.piashcse.controller

import com.piashcse.entities.*
import com.piashcse.models.policy.CreatePolicyRequest
import com.piashcse.models.policy.PolicyConsentRequest
import com.piashcse.models.policy.UpdatePolicyRequest
import com.piashcse.repository.PolicyRepo
import com.piashcse.utils.CommonException
import com.piashcse.utils.extension.notFoundException
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.sql.and
import java.time.LocalDateTime

class PolicyController : PolicyRepo {
    /**
     * Creates a new policy document
     */
    override suspend fun createPolicy(createPolicyRequest: CreatePolicyRequest): PolicyDocumentResponse = query {
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
    override suspend fun updatePolicy(id: String, updatePolicyRequest: UpdatePolicyRequest): PolicyDocumentResponse =
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
    override suspend fun getPolicyByType(type: PolicyDocumentTable.PolicyType): PolicyDocumentResponse = query {
        val policyDocument = PolicyDocumentDAO.find {
            PolicyDocumentTable.type eq type and (PolicyDocumentTable.isActive eq true)
        }.firstOrNull() ?: throw CommonException("No active $type found")

        policyDocument.response()
    }

    /**
     * Gets a policy document by ID
     */
    override suspend fun getPolicyById(id: String): PolicyDocumentResponse = query {
        val policyDocument = PolicyDocumentDAO.findById(id) ?: throw id.notFoundException()
        policyDocument.response()
    }

    /**
     * Gets all policy documents, optionally filtered by type
     */
    override suspend fun getAllPolicies(type: PolicyDocumentTable.PolicyType?): List<PolicyDocumentResponse> = query {
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

    /**
     * Records user consent to a policy
     */
    override suspend fun recordConsent(
        currentUserId: String,
        consentRequest: PolicyConsentRequest
    ): UserPolicyConsentResponse = query {
        // Verify user and policy exist
        val user = UserDAO.findById(currentUserId) ?: throw currentUserId.notFoundException()
        val policy =
            PolicyDocumentDAO.findById(consentRequest.policyId) ?: throw consentRequest.policyId.notFoundException()

        // Check if consent already exists, if so update it
        val existingConsent = PolicyConsentDAO.find {
            PolicyConsentTable.userId eq user.id and (PolicyConsentTable.policyId eq policy.id)
        }.firstOrNull()

        val consent = existingConsent?.// Update existing consent
        apply {
            consentDate = LocalDateTime.now().toString()
            ipAddress = consentRequest.ipAddress
            userAgent = consentRequest.userAgent
        }
            ?: // Create new consent
            PolicyConsentDAO.new {
                userId = user.id
                policyId = policy
                consentDate = LocalDateTime.now().toString()
                ipAddress = consentRequest.ipAddress
                userAgent = consentRequest.userAgent
            }

        // Return the response
        consent.response()
    }

    /**
     * Gets all consents for a user
     */
    override suspend fun getUserConsents(userId: String): List<UserPolicyConsentResponse> = query {
        val user = UserDAO.findById(userId) ?: throw userId.notFoundException()

        PolicyConsentDAO.find { PolicyConsentTable.userId eq user.id }
            .map { it.response() }
    }

    /**
     * Checks if a user has consented to a specific policy
     */
    override suspend fun hasUserConsented(userId: String, policyType: PolicyDocumentTable.PolicyType): Boolean = query {
        val user = UserDAO.findById(userId) ?: throw userId.notFoundException()

        // Find the active policy of the specified type
        val activePolicy = PolicyDocumentDAO.find {
            PolicyDocumentTable.type eq policyType and (PolicyDocumentTable.isActive eq true)
        }.firstOrNull() ?: return@query false

        // Check if user has consented to this policy
        PolicyConsentDAO.find {
            PolicyConsentTable.userId eq user.id and (PolicyConsentTable.policyId eq activePolicy.id)
        }.firstOrNull() != null
    }
}