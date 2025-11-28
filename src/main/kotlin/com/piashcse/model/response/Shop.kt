package com.piashcse.model.response

import com.piashcse.constants.ShopStatus
import java.time.LocalDateTime

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
    val rating: java.math.BigDecimal = java.math.BigDecimal.ZERO,
    val totalReviews: Int = 0,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
)