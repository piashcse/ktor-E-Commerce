package com.piashcse.model.request

import kotlinx.serialization.Serializable
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

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
) {
    init {
        validate(this) {
            validate(CouponRequest::code).isNotNull().isNotEmpty()
            validate(CouponRequest::discountType).isNotNull().isNotEmpty()
            validate(CouponRequest::discountValue).isNotNull()
            validate(CouponRequest::startDate).isNotNull().isNotEmpty()
            validate(CouponRequest::endDate).isNotNull().isNotEmpty()
        }
    }
}
