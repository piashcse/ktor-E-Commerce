package com.piashcse.feature.coupon

import com.piashcse.model.request.CouponRequest
import com.piashcse.model.response.CouponResponse
import com.piashcse.utils.common.PaginatedResponse
import java.time.LocalDateTime

interface CouponRepository {
    suspend fun createCoupon(
        request: CouponRequest,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): CouponResponse

    suspend fun updateCoupon(
        couponId: String,
        request: CouponRequest,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): CouponResponse

    suspend fun getCoupons(
        limit: Int,
        offset: Int,
    ): PaginatedResponse<CouponResponse>

    suspend fun getCouponByCode(code: String): CouponResponse?

    suspend fun deleteCoupon(couponId: String): Boolean
}
