package com.piashcse.model.response

import com.piashcse.constants.CouponDiscountType
import kotlinx.serialization.Serializable

@Serializable
data class CouponResponse(
    val id: String,
    val code: String,
    val discountType: CouponDiscountType,
    val discountValue: String,
    val minOrderAmount: String,
    val maxDiscountAmount: String?,
    val startDate: String,
    val endDate: String,
    val usageLimit: Int?,
    val usageCount: Int,
    val isActive: Boolean,
)
