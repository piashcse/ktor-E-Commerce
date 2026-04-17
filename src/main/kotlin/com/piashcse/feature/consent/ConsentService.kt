package com.piashcse.feature.consent

import com.piashcse.constants.Message
import com.piashcse.database.entities.*
import com.piashcse.model.request.PolicyConsentRequest
import com.piashcse.model.response.UserPolicyConsent
import com.piashcse.utils.ValidationException
import com.piashcse.utils.extension.query
import com.piashcse.utils.throwNotFound
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import java.time.LocalDateTime

class ConsentService: ConsentRepository {
    /**
     * Records user consent to a policy
     */
    override suspend fun recordConsent(
        currentUserId: String,
        consentRequest: PolicyConsentRequest
    ): UserPolicyConsent = query {
        if (currentUserId.isBlank()) {
            throw ValidationException(Message.Validation.blankField("User ID"))
        }
        if (consentRequest.policyId.isBlank()) {
            throw ValidationException(Message.Validation.blankField("Policy ID"))
        }

        // Verify user and policy exist
        val user = UserDAO.findById(currentUserId) ?: currentUserId.throwNotFound("User")
        val policy = PolicyDocumentDAO.findById(consentRequest.policyId)
            ?: consentRequest.policyId.throwNotFound("Policy")

        // Check if consent already exists, if so update it
        val existingConsent = PolicyConsentDAO.find {
            PolicyConsentTable.userId eq user.id and (PolicyConsentTable.policyId eq policy.id)
        }.firstOrNull()

        val consent = existingConsent?.apply {
            // Update existing consent
            consentDate = LocalDateTime.now()
            ipAddress = consentRequest.ipAddress
            userAgent = consentRequest.userAgent
        } ?: PolicyConsentDAO.new {
            // Create new consent
            userId = user.id
            policyId = policy
            consentDate = LocalDateTime.now()
            ipAddress = consentRequest.ipAddress
            userAgent = consentRequest.userAgent
        }

        // Return the response
        consent.response()
    }

    /**
     * Gets all consents for a user
     */
    override suspend fun getUserConsents(userId: String): List<UserPolicyConsent> = query {
        val user = UserDAO.findById(userId) ?: userId.throwNotFound("User")

        PolicyConsentDAO.find { PolicyConsentTable.userId eq user.id }
            .map { it.response() }
    }

    /**
     * Checks if a user has consented to a specific policy
     */
    override suspend fun hasUserConsented(userId: String, policyType: PolicyDocumentTable.PolicyType): Boolean = query {
        val user = UserDAO.findById(userId) ?: userId.throwNotFound("User")

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