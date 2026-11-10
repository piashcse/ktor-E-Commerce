package com.piashcse.feature.consent

import com.piashcse.constants.PolicyType
import com.piashcse.constants.UserType
import com.piashcse.database.entities.*
import com.piashcse.database.entities.base.currentUtc
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
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.security.MessageDigest

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
            if (!policy.isActive || policy.effectiveDate.isAfter(currentUtc())) {
                throw ValidationException("Policy is not active yet")
            }

            // Supersede any active consent for the same policy instead of overwriting
            // the audit row. History is preserved for compliance evidence.
            PolicyConsentDAO.find {
                (PolicyConsentTable.userId eq user.id) and
                    (PolicyConsentTable.policyId eq policy.id) and
                    (PolicyConsentTable.revokedAt.isNull())
            }.forEach { it.revokedAt = currentUtc() }

            val consent =
                PolicyConsentDAO.new {
                    this.userId = user.id
                    policyId = policy
                    consentDate = currentUtc()
                    ipAddress = hashIp(consentRequest.ipAddress)
                    userAgent = truncateUserAgent(consentRequest.userAgent)
                }

            EventBus.publishAdminAction(
                Triple(userId, user.email, user.userType.name),
                "CONSENT_GRANTED",
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
                    (PolicyDocumentTable.type eq policyType) and
                        (PolicyDocumentTable.isActive eq true) and
                        (PolicyDocumentTable.effectiveDate lessEq currentUtc())
                }.orderBy(PolicyDocumentTable.effectiveDate to org.jetbrains.exposed.v1.core.SortOrder.DESC)
                    .firstOrNull() ?: return@query false

            PolicyConsentDAO.find {
                (PolicyConsentTable.userId eq user.id) and
                    (PolicyConsentTable.policyId eq activePolicy.id) and
                    (PolicyConsentTable.revokedAt.isNull())
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
            if (consent.revokedAt == null) consent.revokedAt = currentUtc()
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
        limit: Int,
        offset: Int,
    ): PaginatedResponse<UserPolicyConsentResponse> =
        query {
            val policyIds =
                policyType?.let {
                    PolicyDocumentDAO.find { PolicyDocumentTable.type eq it }.map { policy -> policy.id }
                }
            val base = PolicyConsentTable.selectAll()
            userId?.let { base.andWhere { PolicyConsentTable.userId eq it.entityID(UserTable) } }
            policyIds?.let { base.andWhere { PolicyConsentTable.policyId inList it } }
            base.orderBy(PolicyConsentTable.createdAt to org.jetbrains.exposed.v1.core.SortOrder.DESC)
                .toPaginatedResponse(limit, offset) {
                    PolicyConsentDAO.wrapRow(it).toPolicyConsentResponse()
                }
        }
}
