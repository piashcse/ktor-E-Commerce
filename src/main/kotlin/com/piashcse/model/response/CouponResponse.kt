package com.piashcse.model.response

import com.piashcse.constants.CouponDiscountType
import kotlinx.serialization.Serializable

@Serializable
data class CouponResponse(
    val id: String,
    val code: String,
    val discountType: CouponDiscountType,
    val discountValue: Double,
    val minOrderAmount: Double,
    val maxDiscountAmount: Double?,
    val startDate: String,
    val endDate: String,
    val usageLimit: Int?,
    val usageCount: Int,
    val isActive: Boolean,
)
