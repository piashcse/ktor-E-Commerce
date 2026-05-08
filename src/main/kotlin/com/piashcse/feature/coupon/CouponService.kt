package com.piashcse.feature.coupon

import com.piashcse.database.entities.CouponDAO
import com.piashcse.database.entities.CouponTable
import com.piashcse.model.request.CouponRequest
import com.piashcse.model.response.CouponResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.toPaginatedResponse
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.LocalDateTime

class CouponService : CouponRepository {
    override suspend fun createCoupon(request: CouponRequest): CouponResponse =
        query {
            CouponDAO.new {
                code = request.code
                discountType = request.discountType
                discountValue = request.discountValue
                minOrderAmount = request.minOrderAmount
                maxDiscountAmount = request.maxDiscountAmount
                startDate = LocalDateTime.parse(request.startDate)
                endDate = LocalDateTime.parse(request.endDate)
                usageLimit = request.usageLimit
                isActive = request.isActive
            }.toResponse()
        }

    override suspend fun updateCoupon(
        couponId: String,
        request: CouponRequest,
    ): CouponResponse =
        query {
            val coupon = CouponDAO.findById(couponId) ?: throw Exception("Coupon not found")
            coupon.apply {
                code = request.code
                discountType = request.discountType
                discountValue = request.discountValue
                minOrderAmount = request.minOrderAmount
                maxDiscountAmount = request.maxDiscountAmount
                startDate = LocalDateTime.parse(request.startDate)
                endDate = LocalDateTime.parse(request.endDate)
                usageLimit = request.usageLimit
                isActive = request.isActive
            }.toResponse()
        }

    override suspend fun getCoupons(
        limit: Int,
        offset: Int,
    ): PaginatedResponse<CouponResponse> =
        query {
            CouponTable.selectAll().toPaginatedResponse(limit, offset) {
                CouponDAO.wrapRow(it).toResponse()
            }
        }

    override suspend fun getCouponByCode(code: String): CouponResponse? =
        query {
            CouponDAO.find { CouponTable.code eq code }.firstOrNull()?.toResponse()
        }

    override suspend fun deleteCoupon(couponId: String): Boolean =
        query {
            val coupon = CouponDAO.findById(couponId) ?: return@query false
            coupon.delete()
            true
        }

    private fun CouponDAO.toResponse() =
        CouponResponse(
            id = id.value,
            code = code,
            discountType = discountType,
            discountValue = discountValue,
            minOrderAmount = minOrderAmount,
            maxDiscountAmount = maxDiscountAmount,
            startDate = startDate.toString(),
            endDate = endDate.toString(),
            usageLimit = usageLimit,
            usageCount = usageCount,
            isActive = isActive,
        )
}
