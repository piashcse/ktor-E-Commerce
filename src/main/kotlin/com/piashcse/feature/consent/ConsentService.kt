package com.piashcse.feature.consent

import com.piashcse.database.entities.PolicyConsentDAO
import com.piashcse.database.entities.PolicyConsentTable
import com.piashcse.database.entities.PolicyDocumentDAO
import com.piashcse.database.entities.PolicyDocumentTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.UserPolicyConsentResponse
import com.piashcse.database.models.policy.PolicyConsentRequest
import com.piashcse.utils.extension.notFoundException
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.sql.and
import java.time.LocalDateTime

class ConsentService: ConsentRepository {
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
            PolicyDocumentDAO.findById(consentRequest.policyId)
                ?: throw consentRequest.policyId.notFoundException()

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