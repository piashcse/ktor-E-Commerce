package com.piashcse.feature.coupon

import com.piashcse.constants.CouponDiscountType
import com.piashcse.constants.Message
import com.piashcse.database.entities.CouponDAO
import com.piashcse.database.entities.CouponTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.event.EventBus
import com.piashcse.model.request.CouponRequest
import com.piashcse.model.response.CouponResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.toPaginatedResponse
import com.piashcse.utils.validator.NotFoundException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

class CouponRepositoryImpl : CouponRepository {
    private fun auditCoupon(
        actorId: String?,
        action: String,
        couponId: String,
    ) {
        if (actorId.isNullOrBlank()) return
        val actor = runCatching { UserDAO.findById(actorId) }.getOrNull()
        EventBus.publishAdminAction(
            Triple(actorId, actor?.email.orEmpty(), actor?.userType?.name ?: "ADMIN"),
            action,
            "COUPON",
            couponId,
        )
    }

    override suspend fun createCoupon(
        request: CouponRequest,
        actorId: String?,
    ): CouponResponse =
        query {
            val startDate = parseDate(request.startDate, "startDate")
            val endDate = parseDate(request.endDate, "endDate")
            validateCouponRequest(request, startDate, endDate)
            CouponDAO.new {
                code = request.code
                discountType = request.discountType
                discountValue = request.discountValue
                minOrderAmount = request.minOrderAmount
                maxDiscountAmount = request.maxDiscountAmount
                this.startDate = startDate
                this.endDate = endDate
                usageLimit = request.usageLimit
                isActive = request.isActive
            }.toResponse().also { auditCoupon(actorId, "COUPON_CREATED", it.id) }
        }

    override suspend fun updateCoupon(
        couponId: String,
        request: CouponRequest,
        actorId: String?,
    ): CouponResponse =
        query {
            val coupon = CouponDAO.findById(couponId) ?: throw NotFoundException(Message.Coupons.NOT_FOUND)
            val startDate = parseDate(request.startDate, "startDate")
            val endDate = parseDate(request.endDate, "endDate")
            validateCouponRequest(request, startDate, endDate)
            coupon.apply {
                code = request.code
                discountType = request.discountType
                discountValue = request.discountValue
                minOrderAmount = request.minOrderAmount
                maxDiscountAmount = request.maxDiscountAmount
                this.startDate = startDate
                this.endDate = endDate
                usageLimit = request.usageLimit
                isActive = request.isActive
            }.toResponse().also { auditCoupon(actorId, "COUPON_UPDATED", it.id) }
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

    private fun parseDate(
        value: String,
        fieldName: String,
    ): LocalDateTime =
        try {
            LocalDateTime.parse(value)
        } catch (e: DateTimeParseException) {
            throw ValidationException(Message.Validation.invalidFormat("$fieldName: $value"))
        }

    private fun validateCouponRequest(
        request: CouponRequest,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ) {
        if (request.discountValue <= BigDecimal.ZERO) {
            throw ValidationException("Discount value must be greater than 0")
        }
        if (request.discountType == CouponDiscountType.PERCENTAGE &&
            request.discountValue > BigDecimal(100)
        ) {
            throw ValidationException("Percentage discount must be between 0 and 100")
        }
        if (request.discountType == CouponDiscountType.FIXED &&
            request.discountValue.stripTrailingZeros().scale() > 2
        ) {
            throw ValidationException("Fixed discount value must have at most 2 decimal places")
        }
        if (request.minOrderAmount < BigDecimal.ZERO) {
            throw ValidationException("Minimum order amount cannot be negative")
        }
        if (startDate.isAfter(endDate)) {
            throw ValidationException("Coupon startDate must be before or equal to endDate")
        }
        if (request.usageLimit != null && request.usageLimit < 1) {
            throw ValidationException("Usage limit must be at least 1")
        }
    }

    private fun CouponDAO.toResponse() =
        CouponResponse(
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
}
