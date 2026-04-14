package com.piashcse.model.request

import com.piashcse.database.entities.PolicyDocumentTable
import kotlinx.serialization.Serializable
import org.valiktor.functions.isNotNull
import org.valiktor.functions.isNotEmpty
import org.valiktor.validate

/**
 * Request model for creating a policy document
 */
@Serializable
data class CreatePolicyRequest(
    val title: String,
    val type: PolicyDocumentTable.PolicyType,
    val content: String,
    val version: String,
    val effectiveDate: String
) {
    fun validation() {
        validate(this) {
            validate(CreatePolicyRequest::title).isNotNull().isNotEmpty()
            validate(CreatePolicyRequest::content).isNotNull().isNotEmpty()
            validate(CreatePolicyRequest::version).isNotNull().isNotEmpty()
            validate(CreatePolicyRequest::effectiveDate).isNotNull().isNotEmpty()
        }
    }
}

/**
 * Request model for updating a policy document
 */
@Serializable
data class UpdatePolicyRequest(
    val title: String? = null,
    val content: String? = null,
    val version: String? = null,
    val effectiveDate: String? = null,
    val isActive: Boolean? = null
) {
    fun validation() {
        validate(this) {
            title?.let {
                validate(UpdatePolicyRequest::title).isNotEmpty()
            }
            content?.let {
                validate(UpdatePolicyRequest::content).isNotEmpty()
            }
        }
    }
}

/**
 * Request model for recording user consent to a policy
 */
@Serializable
data class PolicyConsentRequest(
    val policyId: String,
    val ipAddress: String? = null,
    val userAgent: String? = null
) {
    fun validation() {
        validate(this) {
            validate(PolicyConsentRequest::policyId).isNotNull().isNotEmpty()
        }
    }
}
