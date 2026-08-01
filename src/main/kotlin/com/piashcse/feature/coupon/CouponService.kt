package com.piashcse.feature.coupon

import com.piashcse.model.request.CouponRequest
import com.piashcse.model.response.CouponResponse
import com.piashcse.utils.common.PaginatedResponse

class CouponService(private val couponRepo: CouponRepository) {
    suspend fun createCoupon(request: CouponRequest): CouponResponse = couponRepo.createCoupon(request)

    suspend fun updateCoupon(
        couponId: String,
        request: CouponRequest,
    ): CouponResponse = couponRepo.updateCoupon(couponId, request)

    suspend fun getCoupons(
        limit: Int,
        offset: Int,
    ): PaginatedResponse<CouponResponse> = couponRepo.getCoupons(limit, offset)

    suspend fun getCouponByCode(code: String): CouponResponse? = couponRepo.getCouponByCode(code)

    suspend fun deleteCoupon(couponId: String): Boolean = couponRepo.deleteCoupon(couponId)
}
