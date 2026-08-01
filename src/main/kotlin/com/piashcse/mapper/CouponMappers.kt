package com.piashcse.mapper

import com.piashcse.database.entities.CouponDAO
import com.piashcse.model.response.CouponResponse

fun CouponDAO.toCouponResponse() = CouponResponse(
    id = id.value,
    code = code,
    discountType = discountType,
    discountValue = discountValue.toPlainString(),
    minOrderAmount = minOrderAmount.toPlainString(),
    maxDiscountAmount = maxDiscountAmount?.toPlainString(),
    startDate = startDate.toString(),
    endDate = endDate.toString(),
    usageLimit = usageLimit,
    usageCount = usageCount,
    isActive = isActive,
)
