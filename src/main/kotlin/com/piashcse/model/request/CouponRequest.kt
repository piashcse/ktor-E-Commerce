package com.piashcse.model.request

import com.piashcse.constants.CouponDiscountType
import com.piashcse.utils.validator.FieldValidationException
import com.piashcse.utils.validator.FieldViolation
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.valiktor.functions.isGreaterThan
import org.valiktor.functions.isGreaterThanOrEqualTo
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
    // ISO date strings
    val startDate: String,
    val endDate: String,
    val usageLimit: Int? = null,
    val perUserLimit: Int = 1,
    val shopId: String? = null,
    val categoryId: String? = null,
    val isActive: Boolean = true,
) {
    init {
        validate(this) {
            validate(CouponRequest::code).isNotNull().isNotEmpty()
            validate(CouponRequest::discountType).isNotNull()
            validate(CouponRequest::discountValue).isNotNull().isGreaterThan(BigDecimal.ZERO)
            validate(CouponRequest::minOrderAmount).isGreaterThanOrEqualTo(BigDecimal.ZERO)
            validate(CouponRequest::startDate).isNotNull().isNotEmpty()
            validate(CouponRequest::endDate).isNotNull().isNotEmpty()
            validate(CouponRequest::perUserLimit).isGreaterThan(0)
            maxDiscountAmount?.let { validate(CouponRequest::maxDiscountAmount).isGreaterThan(BigDecimal.ZERO) }
            usageLimit?.let { validate(CouponRequest::usageLimit).isGreaterThan(0) }
        }

        val violations =
            buildList {
                if (discountType == CouponDiscountType.PERCENTAGE && discountValue > BigDecimal(100)) {
                    add(FieldViolation("discountValue", "Percentage discount cannot exceed 100"))
                }
                if (startDate > endDate) {
                    add(FieldViolation("startDate", "Coupon startDate must be before endDate"))
                }
            }
        if (violations.isNotEmpty()) throw FieldValidationException(violations)
    }
}
