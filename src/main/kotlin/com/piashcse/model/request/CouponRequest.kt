package com.piashcse.model.request

import com.piashcse.constants.CouponDiscountType
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.valiktor.functions.isGreaterThan
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate
import java.math.BigDecimal

@Serializable
data class CouponRequest(
    val code: String,
    val discountType: CouponDiscountType,
    @Contextual val discountValue: BigDecimal,
    @Contextual val minOrderAmount: BigDecimal = BigDecimal.ZERO,
    @Contextual val maxDiscountAmount: BigDecimal? = null,
    val startDate: String, // ISO date string
    val endDate: String, // ISO date string
    val usageLimit: Int? = null,
    val isActive: Boolean = true,
) {
    init {
        validate(this) {
            validate(CouponRequest::code).isNotNull().isNotEmpty()
            validate(CouponRequest::discountType).isNotNull()
            validate(CouponRequest::discountValue).isNotNull().isGreaterThan(BigDecimal.ZERO)
            validate(CouponRequest::startDate).isNotNull().isNotEmpty()
            validate(CouponRequest::endDate).isNotNull().isNotEmpty()
        }
        require(code.isNotBlank()) { "Coupon code must not be blank" }
        require(discountType != com.piashcse.constants.CouponDiscountType.PERCENTAGE || discountValue <= BigDecimal(100)) {
            "Percentage discount cannot exceed 100"
        }
        require(minOrderAmount >= BigDecimal.ZERO) { "Minimum order amount cannot be negative" }
        maxDiscountAmount?.let { require(it > BigDecimal.ZERO) { "Max discount must be positive" } }
        usageLimit?.let { require(it > 0) { "Usage limit must be positive" } }
        require(startDate <= endDate) { "Coupon startDate must be before endDate (ISO strings)" }
    }
}
