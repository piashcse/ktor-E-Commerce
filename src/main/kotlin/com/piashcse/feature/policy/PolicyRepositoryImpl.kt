package com.piashcse.feature.policy

import com.piashcse.constants.Message
import com.piashcse.constants.PolicyType
import com.piashcse.database.entities.PolicyDocumentDAO
import com.piashcse.database.entities.PolicyDocumentTable
import com.piashcse.mapper.toPolicyDocumentResponse
import com.piashcse.model.request.CreatePolicyRequest
import com.piashcse.model.request.UpdatePolicyRequest
import com.piashcse.model.response.PolicyDocumentResponse
import com.piashcse.repository.base.BaseCrudRepository
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class PolicyRepositoryImpl : PolicyRepository,
    BaseCrudRepository<PolicyDocumentDAO, PolicyDocumentResponse>(
        PolicyDocumentDAO,
        PolicyDocumentTable,
        "Policy",
    ) {

    override fun PolicyDocumentDAO.toResponse(): PolicyDocumentResponse = toPolicyDocumentResponse()

    override suspend fun createPolicy(createPolicyRequest: CreatePolicyRequest): PolicyDocumentResponse =
        query {
            val policyDocument =
                PolicyDocumentDAO.new {
                    title = createPolicyRequest.title
                    type = createPolicyRequest.type
                    content = createPolicyRequest.content
                    version = createPolicyRequest.version
                    effectiveDate = LocalDateTime.parse(createPolicyRequest.effectiveDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                }
            if (policyDocument.isActive) {
                deactivateSiblingsTx(policyDocument.id.value, policyDocument.type, policyDocument.isActive)
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
            updatePolicyRequest.effectiveDate?.let { policyDocument.effectiveDate = LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME) }
            updatePolicyRequest.isActive?.let { policyDocument.isActive = it }
            deactivateSiblingsTx(id, policyDocument.type, policyDocument.isActive)
            policyDocument.toPolicyDocumentResponse()
        }

    override suspend fun getPolicyByType(type: PolicyType): PolicyDocumentResponse =
        query {
            val policyDocument =
                PolicyDocumentDAO.find {
                    PolicyDocumentTable.type eq type and (PolicyDocumentTable.isActive eq true)
                }.firstOrNull() ?: throw ValidationException(Message.Policy.noActivePolicy(type.name))

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

            query.map { it.toPolicyDocumentResponse() }
        }

    override suspend fun deactivatePolicy(id: String): Boolean =
        query {
            val policyDocument = PolicyDocumentDAO.findById(id) ?: id.throwNotFound("Policy")
            policyDocument.isActive = false
            true
        }

    private fun deactivateSiblingsTx(id: String, type: PolicyType, isActive: Boolean) {
        if (!isActive) return
        PolicyDocumentDAO.find {
            PolicyDocumentTable.type eq type and
                (PolicyDocumentTable.id neq id.entityID(PolicyDocumentTable)) and
                (PolicyDocumentTable.isActive eq true)
        }.forEach { it.isActive = false }
    }
}
