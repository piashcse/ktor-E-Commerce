package com.piashcse.feature.policy

import com.piashcse.constants.Message
import com.piashcse.constants.PolicyType
import com.piashcse.database.entities.PolicyDocumentDAO
import com.piashcse.database.entities.PolicyDocumentTable
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.mapper.toPolicyDocumentResponse
import com.piashcse.model.request.CreatePolicyRequest
import com.piashcse.model.request.UpdatePolicyRequest
import com.piashcse.model.response.PolicyDocumentResponse
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.neq
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime

class PolicyRepositoryImpl : PolicyRepository {
    private fun parseDate(value: String): LocalDateTime =
        try {
            LocalDateTime.parse(value)
        } catch (_: Exception) {
            try {
                LocalDate.parse(value).atStartOfDay()
            } catch (_: Exception) {
                try {
                    OffsetDateTime.parse(value).toLocalDateTime()
                } catch (_: Exception) {
                    throw ValidationException("Invalid effectiveDate: $value")
                }
            }
        }

    override suspend fun createPolicy(createPolicyRequest: CreatePolicyRequest): PolicyDocumentResponse =
        query {
            val effectiveDate = parseDate(createPolicyRequest.effectiveDate)
            val shouldBeActive = !effectiveDate.isAfter(currentUtc())

            // Deactivate before inserting when this policy becomes live, otherwise the
            // partial unique index on (type) WHERE is_active would reject the insert.
            if (shouldBeActive) {
                PolicyDocumentDAO.find {
                    (PolicyDocumentTable.type eq createPolicyRequest.type) and
                        (PolicyDocumentTable.isActive eq true)
                }.forEach { it.isActive = false }
            }

            val policyDocument =
                PolicyDocumentDAO.new {
                    title = createPolicyRequest.title
                    type = createPolicyRequest.type
                    content = createPolicyRequest.content
                    version = createPolicyRequest.version
                    this.effectiveDate = effectiveDate
                    isActive = shouldBeActive
                }

            policyDocument.toPolicyDocumentResponse()
        }

    override suspend fun updatePolicy(
        id: String,
        updatePolicyRequest: UpdatePolicyRequest,
    ): PolicyDocumentResponse =
        query {
            val policyDocument = PolicyDocumentDAO.findById(id) ?: id.throwNotFound("Policy")

            updatePolicyRequest.title?.let { policyDocument.title = it }
            updatePolicyRequest.content?.let { policyDocument.content = it }
            updatePolicyRequest.version?.let { policyDocument.version = it }
            updatePolicyRequest.effectiveDate?.let {
                policyDocument.effectiveDate = parseDate(it)
            }
            updatePolicyRequest.isActive?.let {
                if (it && policyDocument.effectiveDate.isAfter(currentUtc())) {
                    throw ValidationException("Cannot activate a policy before its effective date")
                }
                if (it) {
                    // Deactivate siblings before flipping this row active to satisfy
                    // the one-active-policy-per-type partial unique index.
                    PolicyDocumentDAO.find {
                        (PolicyDocumentTable.type eq policyDocument.type) and
                            (PolicyDocumentTable.id neq policyDocument.id) and
                            (PolicyDocumentTable.isActive eq true)
                    }.forEach { otherPolicy -> otherPolicy.isActive = false }
                }
                policyDocument.isActive = it
            }
            policyDocument.toPolicyDocumentResponse()
        }

    override suspend fun getPolicyByType(type: PolicyType): PolicyDocumentResponse =
        query {
            val policyDocument =
                PolicyDocumentDAO.find {
                    (PolicyDocumentTable.type eq type) and
                        (PolicyDocumentTable.isActive eq true) and
                        (PolicyDocumentTable.effectiveDate lessEq currentUtc())
                }.orderBy(PolicyDocumentTable.effectiveDate to SortOrder.DESC)
                    .firstOrNull() ?: throw ValidationException(Message.Policy.noActivePolicy(type.name))

            policyDocument.toPolicyDocumentResponse()
        }

    override suspend fun getPolicyById(id: String): PolicyDocumentResponse =
        query {
            val policyDocument = PolicyDocumentDAO.findById(id) ?: id.throwNotFound("Policy")
            policyDocument.toPolicyDocumentResponse()
        }

    override suspend fun getAllPolicies(type: PolicyType?): List<PolicyDocumentResponse> =
        query {
            val query =
                if (type != null) {
                    PolicyDocumentDAO.find { PolicyDocumentTable.type eq type }
                } else {
                    PolicyDocumentDAO.all()
                }

            query.orderBy(PolicyDocumentTable.effectiveDate to SortOrder.DESC)
                .map { it.toPolicyDocumentResponse() }
        }

    override suspend fun deactivatePolicy(id: String): Boolean =
        query {
            val policyDocument = PolicyDocumentDAO.findById(id) ?: id.throwNotFound("Policy")
            policyDocument.isActive = false
            true
        }
}
