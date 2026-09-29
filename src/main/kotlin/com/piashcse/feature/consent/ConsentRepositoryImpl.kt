package com.piashcse.feature.consent

import com.piashcse.constants.PolicyType
import com.piashcse.constants.UserType
import com.piashcse.database.entities.*
import com.piashcse.event.EventBus
import com.piashcse.mapper.toPolicyConsentResponse
import com.piashcse.model.request.PolicyConsentRequest
import com.piashcse.model.response.UserPolicyConsentResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.requireNotBlank
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.toPaginatedResponse
import com.piashcse.utils.extension.verifyOwnership
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.security.MessageDigest
import java.time.LocalDateTime

class ConsentRepositoryImpl : ConsentRepository {
    // PII minimization: store only a sha256 prefix of the IP and a truncated UA.
    private fun hashIp(ipAddress: String?): String? =
        ipAddress?.takeIf { it.isNotBlank() }?.let {
            MessageDigest.getInstance("SHA-256").digest(it.toByteArray())
                .joinToString("") { byte -> "%02x".format(byte) }.take(16)
        }

    private fun truncateUserAgent(userAgent: String?): String? = userAgent?.take(200)

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
                    ipAddress = hashIp(consentRequest.ipAddress)
                    userAgent = truncateUserAgent(consentRequest.userAgent)
                } ?: PolicyConsentDAO.new {
                    this.userId = user.id
                    policyId = policy
                    consentDate = LocalDateTime.now()
                    ipAddress = hashIp(consentRequest.ipAddress)
                    userAgent = truncateUserAgent(consentRequest.userAgent)
                }

            EventBus.publishAdminAction(
                Triple(userId, user.email, user.userType.name),
                if (existingConsent == null) "CONSENT_GRANTED" else "CONSENT_UPDATED",
                "CONSENT",
                consent.id.value,
            )
            consent.toPolicyConsentResponse()
        }

    override suspend fun getUserConsents(
        userId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<UserPolicyConsentResponse> =
        query {
            val user = UserDAO.findById(userId) ?: userId.throwNotFound("User")

            PolicyConsentTable.selectAll().andWhere { PolicyConsentTable.userId eq user.id }
                .toPaginatedResponse(limit, offset) { PolicyConsentDAO.wrapRow(it).toPolicyConsentResponse() }
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

    override suspend fun revokeConsent(
        consentId: String,
        userId: String,
        userType: UserType,
    ): Boolean =
        query {
            val consent = PolicyConsentDAO.findById(consentId) ?: consentId.throwNotFound("Consent")
            if (userType != UserType.ADMIN && userType != UserType.SUPER_ADMIN) {
                consent.verifyOwnership(userId, "Consent") { it.userId.value }
            }
            consent.delete()
            val actor = runCatching { UserDAO.findById(userId) }.getOrNull()
            EventBus.publishAdminAction(
                Triple(userId, actor?.email.orEmpty(), actor?.userType?.name ?: userType.name),
                "CONSENT_REVOKED",
                "CONSENT",
                consentId,
            )
            true
        }

    override suspend fun listConsentsByUser(
        userId: String?,
        policyType: PolicyType?,
    ): List<UserPolicyConsentResponse> =
        query {
            val policyIds =
                policyType?.let {
                    PolicyDocumentDAO.find { PolicyDocumentTable.type eq it }.map { policy -> policy.id }
                }
            val consents =
                when {
                    userId != null && policyIds != null ->
                        PolicyConsentDAO.find {
                            (PolicyConsentTable.userId eq userId.entityID(UserTable)) and
                                (PolicyConsentTable.policyId inList policyIds)
                        }
                    userId != null ->
                        PolicyConsentDAO.find { PolicyConsentTable.userId eq userId.entityID(UserTable) }
                    policyIds != null ->
                        PolicyConsentDAO.find { PolicyConsentTable.policyId inList policyIds }
                    else -> PolicyConsentDAO.all()
                }
            consents.map { it.toPolicyConsentResponse() }
        }
}
