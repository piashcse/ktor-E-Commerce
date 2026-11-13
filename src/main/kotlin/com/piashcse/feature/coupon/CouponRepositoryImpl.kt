package com.piashcse.feature.coupon

import com.piashcse.constants.CouponDiscountType
import com.piashcse.constants.Message
import com.piashcse.database.entities.CouponDAO
import com.piashcse.database.entities.CouponTable
import com.piashcse.database.entities.CouponUsageDAO
import com.piashcse.database.entities.CouponUsageTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.event.EventBus
import com.piashcse.model.request.CouponRequest
import com.piashcse.model.response.CouponResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.toPaginatedResponse
import com.piashcse.utils.validator.NotFoundException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime

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
            val endDate = parseDate(request.endDate, "endDate", endOfDay = true)
            validateCouponRequest(request, startDate, endDate)
            CouponDAO.new {
                code = normalizeCode(request.code)
                discountType = request.discountType
                discountValue = request.discountValue
                minOrderAmount = request.minOrderAmount
                maxDiscountAmount = request.maxDiscountAmount
                this.startDate = startDate
                this.endDate = endDate
                usageLimit = request.usageLimit
                perUserLimit = request.perUserLimit
                shopId = request.shopId?.let { it.entityID(com.piashcse.database.entities.ShopTable) }
                categoryId = request.categoryId?.let { it.entityID(com.piashcse.database.entities.ProductCategoryTable) }
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
            val endDate = parseDate(request.endDate, "endDate", endOfDay = true)
            validateCouponRequest(request, startDate, endDate)
            if (request.usageLimit != null && request.usageLimit < coupon.usageCount) {
                throw ValidationException("Usage limit cannot be lower than the current usage count (${coupon.usageCount})")
            }
            coupon.apply {
                code = normalizeCode(request.code)
                discountType = request.discountType
                discountValue = request.discountValue
                minOrderAmount = request.minOrderAmount
                maxDiscountAmount = request.maxDiscountAmount
                this.startDate = startDate
                this.endDate = endDate
                usageLimit = request.usageLimit
                perUserLimit = request.perUserLimit
                shopId = request.shopId?.let { it.entityID(com.piashcse.database.entities.ShopTable) }
                categoryId = request.categoryId?.let { it.entityID(com.piashcse.database.entities.ProductCategoryTable) }
                isActive = request.isActive
            }.toResponse().also { auditCoupon(actorId, "COUPON_UPDATED", it.id) }
        }

    override suspend fun getCoupons(
        limit: Int,
        offset: Int,
        code: String?,
        active: Boolean?,
    ): PaginatedResponse<CouponResponse> =
        query {
            val query = CouponTable.selectAll()
            code?.takeIf { it.isNotBlank() }?.let { value ->
                query.andWhere { CouponTable.code eq normalizeCode(value) }
            }
            active?.let { enabled ->
                query.andWhere { CouponTable.isActive eq enabled }
            }
            query.orderBy(CouponTable.createdAt to org.jetbrains.exposed.v1.core.SortOrder.DESC)
                .toPaginatedResponse(limit, offset) {
                    CouponDAO.wrapRow(it).toResponse()
                }
        }

    override suspend fun getCouponByCode(code: String): CouponResponse? =
        query {
            val now = currentUtc()
            CouponDAO.find {
                (CouponTable.code eq normalizeCode(code)) and
                    (CouponTable.isActive eq true) and
                    (CouponTable.startDate lessEq now) and
                    (CouponTable.endDate greaterEq now)
            }.firstOrNull()?.toResponse()
        }

    override suspend fun deleteCoupon(couponId: String): Boolean =
        query {
            val coupon = CouponDAO.findById(couponId) ?: return@query false
            val used = CouponUsageDAO.find { CouponUsageTable.couponId eq coupon.id }.count()
            if (used > 0) {
                // Preserve redemption history; retire the coupon instead of hard-deleting.
                coupon.isActive = false
            } else {
                coupon.delete()
            }
            true
        }

    private fun normalizeCode(code: String): String = code.trim().uppercase()

    private fun parseDate(
        value: String,
        fieldName: String,
        endOfDay: Boolean = false,
    ): LocalDateTime =
        try {
            LocalDateTime.parse(value)
        } catch (_: Exception) {
            try {
                val date = LocalDate.parse(value)
                if (endOfDay) date.atTime(java.time.LocalTime.MAX) else date.atStartOfDay()
            } catch (_: Exception) {
                try {
                    OffsetDateTime.parse(value).atZoneSameInstant(java.time.ZoneOffset.UTC).toLocalDateTime()
                } catch (_: Exception) {
                    throw ValidationException(Message.Validation.invalidFormat("$fieldName: $value"))
                }
            }
        }

    private fun validateCouponRequest(
        request: CouponRequest,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ) {
        // Basic field constraints are enforced by CouponRequest (Valiktor). Only
        // cross-field and parsed-date rules remain here.
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
        if (startDate.isAfter(endDate)) {
            throw ValidationException("Coupon startDate must be before or equal to endDate")
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
            perUserLimit = perUserLimit,
            shopId = shopId?.value,
            categoryId = categoryId?.value,
            isActive = isActive,
        )
}
