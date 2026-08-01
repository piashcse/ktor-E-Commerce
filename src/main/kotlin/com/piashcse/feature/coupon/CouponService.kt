package com.piashcse.feature.coupon

import com.piashcse.constants.Message
import com.piashcse.model.request.CouponRequest
import com.piashcse.model.response.CouponResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.suspendRetryQuery
import com.piashcse.utils.validator.ValidationException
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

class CouponService(private val couponRepo: CouponRepository) {
    /**
     * Creates a coupon after validating its date range. Runs in a retryable
     * transaction.
     */
    suspend fun createCoupon(request: CouponRequest): CouponResponse = suspendRetryQuery {
        couponRepo.createCoupon(request, parseDate(request.startDate, "startDate"), parseDate(request.endDate, "endDate"))
    }

    /**
     * Updates a coupon after validating its date range. Runs in a retryable
     * transaction.
     */
    suspend fun updateCoupon(
        couponId: String,
        request: CouponRequest,
    ): CouponResponse = suspendRetryQuery {
        couponRepo.updateCoupon(couponId, request, parseDate(request.startDate, "startDate"), parseDate(request.endDate, "endDate"))
    }

    suspend fun getCoupons(
        limit: Int,
        offset: Int,
    ): PaginatedResponse<CouponResponse> = couponRepo.getCoupons(limit, offset)

    suspend fun getCouponByCode(code: String): CouponResponse? = couponRepo.getCouponByCode(code)

    /**
     * Deletes a coupon. Runs in a retryable transaction.
     */
    suspend fun deleteCoupon(couponId: String): Boolean = suspendRetryQuery { couponRepo.deleteCoupon(couponId) }

    private fun parseDate(value: String, fieldName: String): LocalDateTime =
        try {
            LocalDateTime.parse(value)
        } catch (e: DateTimeParseException) {
            throw ValidationException(Message.Validation.invalidFormat("$fieldName: $value"))
        }
}
