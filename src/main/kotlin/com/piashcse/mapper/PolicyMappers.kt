package com.piashcse.mapper

import com.piashcse.database.entities.PolicyConsentDAO
import com.piashcse.database.entities.PolicyDocumentDAO
import com.piashcse.model.response.PolicyDocumentResponse
import com.piashcse.model.response.UserPolicyConsentResponse
import com.piashcse.utils.common.DateTimes.toUtcString

fun PolicyDocumentDAO.toPolicyDocumentResponse() =
    PolicyDocumentResponse(
        id.value,
        title,
        type.name,
        content,
        version,
        effectiveDate.toUtcString(),
        isActive,
        createdAt.toUtcString(),
        updatedAt?.toUtcString(),
    )

fun PolicyConsentDAO.toPolicyConsentResponse() =
    UserPolicyConsentResponse(
        id.value,
        userId.value,
        policyId.id.value,
        consentDate?.toUtcString(),
        ipAddress,
        userAgent,
        revokedAt?.toUtcString(),
        policyVersion,
        policyContentHash,
    )
