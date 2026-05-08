package com.piashcse.model.request

import kotlinx.serialization.Serializable

@Serializable
data class CouponRequest(
    val code: String,
    val discountType: String,
    val discountValue: Double,
    val minOrderAmount: Double = 0.0,
    val maxDiscountAmount: Double? = null,
    val startDate: String, // ISO date string
    val endDate: String, // ISO date string
    val usageLimit: Int? = null,
    val isActive: Boolean = true,
)
