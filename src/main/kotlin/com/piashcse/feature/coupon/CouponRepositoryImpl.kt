package com.piashcse.feature.coupon

import com.piashcse.database.entities.CouponDAO
import com.piashcse.database.entities.CouponTable
import com.piashcse.model.request.CouponRequest
import com.piashcse.model.response.CouponResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.toPaginatedResponse
import com.piashcse.constants.Message
import com.piashcse.utils.validator.NotFoundException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

class CouponRepositoryImpl : CouponRepository {
    override suspend fun createCoupon(request: CouponRequest): CouponResponse =
        query {
            CouponDAO.new {
                code = request.code
                discountType = request.discountType
                discountValue = request.discountValue
                minOrderAmount = request.minOrderAmount
                maxDiscountAmount = request.maxDiscountAmount
                startDate = parseDate(request.startDate, "startDate")
                endDate = parseDate(request.endDate, "endDate")
                usageLimit = request.usageLimit
                isActive = request.isActive
            }.toResponse()
        }

    override suspend fun updateCoupon(
        couponId: String,
        request: CouponRequest,
    ): CouponResponse =
        query {
            val coupon = CouponDAO.findById(couponId) ?: throw NotFoundException(Message.Coupons.NOT_FOUND)
            coupon.apply {
                code = request.code
                discountType = request.discountType
                discountValue = request.discountValue
                minOrderAmount = request.minOrderAmount
                maxDiscountAmount = request.maxDiscountAmount
                startDate = parseDate(request.startDate, "startDate")
                endDate = parseDate(request.endDate, "endDate")
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

    private fun parseDate(value: String, fieldName: String): LocalDateTime =
        try {
            LocalDateTime.parse(value)
        } catch (e: DateTimeParseException) {
            throw ValidationException(Message.Validation.invalidFormat("$fieldName: $value"))
        }

    private fun CouponDAO.toResponse() =
        CouponResponse(
            id = id.value,
            code = code,
            discountType = discountType,
            discountValue = BigDecimal.valueOf(discountValue).toPlainString(),
            minOrderAmount = BigDecimal.valueOf(minOrderAmount).toPlainString(),
            maxDiscountAmount = maxDiscountAmount?.let { BigDecimal.valueOf(it).toPlainString() },
            startDate = startDate.toString(),
            endDate = endDate.toString(),
            usageLimit = usageLimit,
            usageCount = usageCount,
            isActive = isActive,
        )
}
