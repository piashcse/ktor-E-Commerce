package com.piashcse.feature.consent

import com.piashcse.constants.PolicyType
import com.piashcse.database.entities.*
import com.piashcse.mapper.toPolicyConsentResponse
import com.piashcse.model.request.PolicyConsentRequest
import com.piashcse.model.response.UserPolicyConsentResponse
import com.piashcse.utils.extension.*
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import java.time.LocalDateTime

class ConsentRepositoryImpl : ConsentRepository {
    override suspend fun recordConsent(
        userId: String,
        consentRequest: PolicyConsentRequest,
    ): UserPolicyConsentResponse =
        query {
            userId.requireNotBlank("User ID")
            consentRequest.policyId.requireNotBlank("Policy ID")

            val user = UserDAO.findById(userId) ?: userId.throwNotFound("User")
            val policy =
                PolicyDocumentDAO.findById(consentRequest.policyId)
                    ?: consentRequest.policyId.throwNotFound("Policy")

            val existingConsent =
                PolicyConsentDAO.find {
                    PolicyConsentTable.userId eq user.id and (PolicyConsentTable.policyId eq policy.id)
                }.firstOrNull()

            val consent =
                existingConsent?.apply {
                    consentDate = LocalDateTime.now()
                    ipAddress = consentRequest.ipAddress
                    userAgent = consentRequest.userAgent
                } ?: PolicyConsentDAO.new {
                    this.userId = user.id
                    policyId = policy
                    consentDate = LocalDateTime.now()
                    ipAddress = consentRequest.ipAddress
                    userAgent = consentRequest.userAgent
                }

            consent.toPolicyConsentResponse()
        }

    override suspend fun getUserConsents(userId: String): List<UserPolicyConsentResponse> =
        query {
            val user = UserDAO.findById(userId) ?: userId.throwNotFound("User")

            PolicyConsentDAO.find { PolicyConsentTable.userId eq user.id }
                .map { it.toPolicyConsentResponse() }
        }

    override suspend fun hasUserConsented(
        userId: String,
        policyType: PolicyType,
    ): Boolean =
        query {
            val user = UserDAO.findById(userId) ?: userId.throwNotFound("User")

            val activePolicy =
                PolicyDocumentDAO.find {
                    PolicyDocumentTable.type eq policyType and (PolicyDocumentTable.isActive eq true)
                }.firstOrNull() ?: return@query false

            PolicyConsentDAO.find {
                PolicyConsentTable.userId eq user.id and (PolicyConsentTable.policyId eq activePolicy.id)
            }.firstOrNull() != null
        }
}
