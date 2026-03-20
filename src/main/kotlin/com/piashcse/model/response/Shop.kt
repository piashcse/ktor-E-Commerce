package com.piashcse.model.response

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import com.piashcse.constants.ShopStatus
import java.time.LocalDateTime

@Serializable
data class Shop(
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
    val rating: @Contextual java.math.BigDecimal = java.math.BigDecimal.ZERO,
    val totalReviews: Int = 0,
    val createdAt: @Contextual LocalDateTime?,
    val updatedAt: @Contextual LocalDateTime?
)