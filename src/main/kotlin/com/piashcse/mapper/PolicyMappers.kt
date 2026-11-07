package com.piashcse.mapper

import com.piashcse.database.entities.PolicyConsentDAO
import com.piashcse.database.entities.PolicyDocumentDAO
import com.piashcse.model.response.PolicyDocumentResponse
import com.piashcse.model.response.UserPolicyConsentResponse

fun PolicyDocumentDAO.toPolicyDocumentResponse() =
    PolicyDocumentResponse(
        id.value,
        title,
        type.name,
        content,
        version,
        effectiveDate.toString(),
        isActive,
        createdAt.toString(),
        updatedAt?.toString(),
    )

fun PolicyConsentDAO.toPolicyConsentResponse() =
    UserPolicyConsentResponse(
        id.value,
        userId.value,
        policyId.id.value,
        consentDate?.toString(),
        ipAddress,
        userAgent,
        revokedAt?.toString(),
    )
