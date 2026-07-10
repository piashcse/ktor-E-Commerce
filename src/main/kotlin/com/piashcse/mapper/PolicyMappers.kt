package com.piashcse.mapper

import com.piashcse.database.entities.PolicyConsentDAO
import com.piashcse.database.entities.PolicyDocumentDAO
import com.piashcse.model.response.PolicyDocumentResponse
import com.piashcse.model.response.UserPolicyConsentResponse
import java.time.LocalDateTime

fun PolicyDocumentDAO.toPolicyDocumentResponse() = PolicyDocumentResponse(
    id.value, title, type.name, content, version,
    effectiveDate.toString(), isActive,
)

fun PolicyConsentDAO.toPolicyConsentResponse() = UserPolicyConsentResponse(
    id.value, userId.value, policyId.id.value,
    consentDate?.toString() ?: LocalDateTime.now().toString(),
    ipAddress, userAgent,
)
