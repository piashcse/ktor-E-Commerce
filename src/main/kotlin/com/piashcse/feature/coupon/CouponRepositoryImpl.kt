package com.piashcse.feature.coupon

import com.piashcse.database.entities.CouponDAO
import com.piashcse.database.entities.CouponTable
import com.piashcse.mapper.toCouponResponse
import com.piashcse.model.request.CouponRequest
import com.piashcse.model.response.CouponResponse
import com.piashcse.repository.base.BaseCrudRepository
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.v1.core.eq
import java.time.LocalDateTime

class CouponRepositoryImpl : CouponRepository,
    BaseCrudRepository<CouponDAO, CouponResponse>(CouponDAO, CouponTable, "Coupon") {

    override fun CouponDAO.toResponse() = toCouponResponse()

    override suspend fun createCoupon(
        request: CouponRequest,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): CouponResponse =
        create {
            code = request.code
            discountType = request.discountType
            discountValue = request.discountValue
            minOrderAmount = request.minOrderAmount
            maxDiscountAmount = request.maxDiscountAmount
            this.startDate = startDate
            this.endDate = endDate
            usageLimit = request.usageLimit
            isActive = request.isActive
        }

    override suspend fun updateCoupon(
        couponId: String,
        request: CouponRequest,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): CouponResponse = update(couponId) {
        code = request.code
        discountType = request.discountType
        discountValue = request.discountValue
        minOrderAmount = request.minOrderAmount
        maxDiscountAmount = request.maxDiscountAmount
        this.startDate = startDate
        this.endDate = endDate
        usageLimit = request.usageLimit
        isActive = request.isActive
    }

    override suspend fun getCoupons(
        limit: Int,
        offset: Int,
    ): PaginatedResponse<CouponResponse> = getAll(limit, offset)

    override suspend fun getCouponByCode(code: String): CouponResponse? = query {
        CouponDAO.find { CouponTable.code eq code }.firstOrNull()?.toResponse()
    }

    override suspend fun deleteCoupon(couponId: String): Boolean = query {
        val coupon = CouponDAO.findById(couponId) ?: return@query false
        coupon.delete()
        true
    }
}
