package com.piashcse.feature.promotion

import com.piashcse.constants.PromotionType
import com.piashcse.database.entities.PromotionDAO
import com.piashcse.database.entities.PromotionTable
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.Money
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.toPaginatedResponse
import com.piashcse.utils.validator.FieldValidationException
import com.piashcse.utils.validator.FieldViolation
import com.piashcse.utils.validator.ValidationException
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.OffsetDateTime

@Serializable
data class PromotionRequest(
    val name: String,
    val type: PromotionType,
    @Contextual val value: BigDecimal,
    @Contextual val minOrderAmount: BigDecimal = BigDecimal.ZERO,
    @Contextual val maxDiscountAmount: BigDecimal? = null,
    val startsAt: String,
    val endsAt: String,
    val isActive: Boolean = true,
    val priority: Int = 0,
    val usageLimit: Int? = null,
) {
    init {
        val violations =
            buildList {
                if (name.isBlank()) add(FieldViolation("name", "Promotion name is required"))
                if (value <= BigDecimal.ZERO) add(FieldViolation("value", "Promotion value must be positive"))
                if (type == PromotionType.PERCENTAGE && value > BigDecimal(100)) {
                    add(FieldViolation("value", "Percentage promotion cannot exceed 100"))
                }
                if (minOrderAmount < BigDecimal.ZERO) add(FieldViolation("minOrderAmount", "Minimum order amount cannot be negative"))
                maxDiscountAmount?.let {
                    if (it <= BigDecimal.ZERO) add(FieldViolation("maxDiscountAmount", "Max discount must be positive"))
                }
                usageLimit?.let {
                    if (it < 1) add(FieldViolation("usageLimit", "Usage limit must be at least 1"))
                }
            }
        if (violations.isNotEmpty()) throw FieldValidationException(violations)
    }
}

@Serializable
data class PromotionResponse(
    val id: String,
    val name: String,
    val type: PromotionType,
    val value: String,
    val minOrderAmount: String,
    val maxDiscountAmount: String?,
    val startsAt: String,
    val endsAt: String,
    val isActive: Boolean,
    val priority: Int,
    val usageLimit: Int?,
    val usageCount: Int,
    val createdAt: String?,
)

/** Result of applying the single best automatic promotion. */
data class PromotionMatch(
    val id: String,
    val name: String,
    val type: PromotionType,
    val discount: BigDecimal,
    val freeShipping: Boolean,
)

class PromotionService {
    /** Must be called inside a DB transaction (checkout pricing path). */
    fun evaluateTx(amountUsd: BigDecimal): PromotionMatch? {
        if (amountUsd <= BigDecimal.ZERO) return null
        val now = currentUtc()
        val promotion =
            PromotionDAO.find {
                (PromotionTable.isActive eq true) and
                    (PromotionTable.startsAt lessEq now) and
                    (PromotionTable.endsAt greaterEq now) and
                    (PromotionTable.minOrderAmount lessEq amountUsd)
            }.orderBy(PromotionTable.priority to org.jetbrains.exposed.v1.core.SortOrder.DESC)
                .orderBy(PromotionTable.createdAt to org.jetbrains.exposed.v1.core.SortOrder.ASC)
                .firstOrNull { it.usageLimit == null || it.usageCount < it.usageLimit!! }
                ?: return null

        val discount =
            when (promotion.type) {
                PromotionType.PERCENTAGE ->
                    Money.percentOf(amountUsd, promotion.value)
                        .let { it.min(promotion.maxDiscountAmount ?: it) }
                PromotionType.FIXED -> promotion.value.min(amountUsd)
                PromotionType.FREE_SHIPPING -> BigDecimal.ZERO
            }
        return PromotionMatch(
            id = promotion.id.value,
            name = promotion.name,
            type = promotion.type,
            discount = Money.scale2(discount),
            freeShipping = promotion.type == PromotionType.FREE_SHIPPING,
        )
    }

    suspend fun evaluate(amountUsd: BigDecimal): PromotionMatch? = query { evaluateTx(amountUsd) }

    /** Must be called inside the same DB transaction as the order write. */
    fun markUsedTx(promotionId: String) {
        val promotion =
            PromotionDAO.find { PromotionTable.id eq promotionId }
                .forUpdate()
                .firstOrNull() ?: return
        if (promotion.usageLimit == null || promotion.usageCount < promotion.usageLimit!!) {
            promotion.usageCount++
        }
    }

    suspend fun markUsed(promotionId: String) = query { markUsedTx(promotionId) }

    suspend fun list(
        active: Boolean?,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<PromotionResponse> =
        query {
            val query = PromotionTable.selectAll()
            active?.let { query.andWhere { PromotionTable.isActive eq it } }
            query.orderBy(PromotionTable.priority to org.jetbrains.exposed.v1.core.SortOrder.DESC)
                .toPaginatedResponse(limit, offset) { PromotionDAO.wrapRow(it).toResponse() }
        }

    suspend fun create(request: PromotionRequest): PromotionResponse =
        query {
            val startsAt = parseDate(request.startsAt, "startsAt")
            val endsAt = parseDate(request.endsAt, "endsAt")
            if (startsAt.isAfter(endsAt)) throw ValidationException("startsAt must be before endsAt")
            PromotionDAO.new {
                name = request.name
                type = request.type
                value = request.value
                minOrderAmount = request.minOrderAmount
                maxDiscountAmount = request.maxDiscountAmount
                this.startsAt = startsAt
                this.endsAt = endsAt
                isActive = request.isActive
                priority = request.priority
                usageLimit = request.usageLimit
            }.toResponse()
        }

    suspend fun update(
        id: String,
        request: PromotionRequest,
    ): PromotionResponse =
        query {
            val promotion = PromotionDAO.findById(id) ?: id.throwNotFound("Promotion")
            val startsAt = parseDate(request.startsAt, "startsAt")
            val endsAt = parseDate(request.endsAt, "endsAt")
            if (startsAt.isAfter(endsAt)) throw ValidationException("startsAt must be before endsAt")
            promotion.apply {
                name = request.name
                type = request.type
                value = request.value
                minOrderAmount = request.minOrderAmount
                maxDiscountAmount = request.maxDiscountAmount
                this.startsAt = startsAt
                this.endsAt = endsAt
                isActive = request.isActive
                priority = request.priority
                usageLimit = request.usageLimit
            }.toResponse()
        }

    suspend fun delete(id: String): Boolean =
        query {
            val promotion = PromotionDAO.findById(id) ?: return@query false
            // Preserve usage history; retiring is safer than hard delete.
            promotion.isActive = false
            true
        }

    private fun parseDate(
        value: String,
        field: String,
    ): LocalDateTime =
        runCatching { LocalDateTime.parse(value) }
            .recoverCatching { OffsetDateTime.parse(value).toLocalDateTime() }
            .getOrElse { throw ValidationException("Invalid $field: $value") }

    private fun PromotionDAO.toResponse() =
        PromotionResponse(
            id = id.value,
            name = name,
            type = type,
            value = Money.str(value),
            minOrderAmount = Money.str(minOrderAmount),
            maxDiscountAmount = maxDiscountAmount?.let { Money.str(it) },
            startsAt = startsAt.toUtcString(),
            endsAt = endsAt.toUtcString(),
            isActive = isActive,
            priority = priority,
            usageLimit = usageLimit,
            usageCount = usageCount,
            createdAt = createdAt.toUtcString(),
        )
}
