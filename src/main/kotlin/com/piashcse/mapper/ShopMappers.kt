package com.piashcse.mapper

import com.piashcse.constants.KycStatus
import com.piashcse.database.entities.ShopDAO
import com.piashcse.model.response.ShopResponse

fun ShopDAO.toShopResponse(
    ownerEmail: String? = null,
    kycStatus: KycStatus? = null,
    includeInternal: Boolean = false,
) = ShopResponse(
    id = id.value,
    name = name,
    categoryId = categoryId.value,
    description = description,
    address = address,
    phone = phone,
    email = email,
    logo = logo,
    coverImage = coverImage,
    status = status,
    rejectionReason = rejectionReason.takeIf { includeInternal },
    suspendedReason = suspendedReason.takeIf { includeInternal },
    ownerUserId = userId.value.takeIf { includeInternal },
    ownerEmail = ownerEmail.takeIf { includeInternal },
    kycStatus = kycStatus.takeIf { includeInternal },
    rating = rating,
    totalReviews = totalReviews,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
