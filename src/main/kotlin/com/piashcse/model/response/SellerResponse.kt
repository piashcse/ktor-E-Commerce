package com.piashcse.model.response

import com.piashcse.constants.ShopStatus
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDateTime

@Serializable
data class SellerResponse(
    val id: String,
    val userId: String,
    val shopId: String?,
    val businessName: String?,
    val businessRegistrationNumber: String?,
    val taxId: String?,
    val bankAccountNumber: String?,
    val bankName: String?,
    val bankRoutingNumber: String?,
    val commissionRate: @Contextual BigDecimal,
    val status: ShopStatus,
    val totalSales: @Contextual BigDecimal,
    val totalCommission: @Contextual BigDecimal,
    val approvedAt: @Contextual LocalDateTime?,
    val suspendedAt: @Contextual LocalDateTime?,
    val terminatedAt: @Contextual LocalDateTime?,
    val createdAt: @Contextual LocalDateTime?,
    val updatedAt: @Contextual LocalDateTime?,
)
