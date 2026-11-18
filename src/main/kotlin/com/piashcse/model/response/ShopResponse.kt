package com.piashcse.model.response

import com.piashcse.constants.KycStatus
import com.piashcse.constants.ShopStatus
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDateTime

@Serializable
data class ShopResponse(
    val id: String,
    val name: String,
    val categoryId: String,
    val description: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val logo: String? = null,
    val coverImage: String? = null,
    val status: ShopStatus = ShopStatus.PENDING,
    val rejectionReason: String? = null,
    val suspendedReason: String? = null,
    val ownerUserId: String? = null,
    val ownerEmail: String? = null,
    val kycStatus: KycStatus? = null,
    val rating: @Contextual BigDecimal = BigDecimal.ZERO,
    val totalReviews: Int = 0,
    val createdAt: @Contextual LocalDateTime?,
    val updatedAt: @Contextual LocalDateTime?,
)
