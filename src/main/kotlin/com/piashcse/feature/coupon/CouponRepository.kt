package com.piashcse.feature.coupon

import com.piashcse.model.request.CouponRequest
import com.piashcse.model.response.CouponResponse
import com.piashcse.utils.common.PaginatedResponse

interface CouponRepository {
    suspend fun createCoupon(
        request: CouponRequest,
        actorId: String? = null,
    ): CouponResponse

    suspend fun updateCoupon(
        couponId: String,
        request: CouponRequest,
        actorId: String? = null,
    ): CouponResponse

    suspend fun getCoupons(
        limit: Int,
        offset: Int,
        code: String? = null,
        active: Boolean? = null,
    ): PaginatedResponse<CouponResponse>

    suspend fun getCouponByCode(code: String): CouponResponse?

    suspend fun deleteCoupon(couponId: String): Boolean

    suspend fun getSellerCoupons(
        shopId: String,
        limit: Int,
        offset: Int,
        active: Boolean? = null,
    ): PaginatedResponse<CouponResponse>

    suspend fun createSellerCoupon(
        shopId: String,
        request: CouponRequest,
        actorId: String? = null,
    ): CouponResponse

    suspend fun updateSellerCoupon(
        shopId: String,
        couponId: String,
        request: CouponRequest,
        actorId: String? = null,
    ): CouponResponse

    suspend fun deleteSellerCoupon(
        shopId: String,
        couponId: String,
    ): Boolean
}
