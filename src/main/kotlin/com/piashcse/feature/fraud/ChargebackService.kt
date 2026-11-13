package com.piashcse.feature.fraud

import com.piashcse.constants.ChargebackStatus
import com.piashcse.database.entities.ChargebackDAO
import com.piashcse.database.entities.ChargebackTable
import com.piashcse.database.entities.PaymentDAO
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.database.entities.isResolved
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.Money
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.toPaginatedResponse
import com.piashcse.utils.validator.ValidationException
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.OffsetDateTime

@Serializable
data class ChargebackRequest(
    val paymentId: String,
    val providerDisputeId: String? = null,
    @Contextual val amount: BigDecimal? = null,
    val reason: String? = null,
    val evidenceDueAt: String? = null,
)

@Serializable
data class ChargebackUpdateRequest(
    val status: ChargebackStatus,
    val outcome: String? = null,
    val reason: String? = null,
)

@Serializable
data class ChargebackResponse(
    val id: String,
    val paymentId: String,
    val orderId: String,
    val providerDisputeId: String?,
    val amount: String,
    val currency: String,
    val status: ChargebackStatus,
    val reason: String?,
    val outcome: String?,
    val evidenceDueAt: String?,
    val openedAt: String,
    val resolvedAt: String?,
    val createdAt: String?,
)

class ChargebackService {
    suspend fun list(
        limit: Int,
        offset: Int,
    ): PaginatedResponse<ChargebackResponse> =
        query {
            ChargebackTable.selectAll()
                .orderBy(ChargebackTable.createdAt to SortOrder.DESC)
                .toPaginatedResponse(limit, offset) { ChargebackDAO.wrapRow(it).toResponse() }
        }

    suspend fun get(id: String): ChargebackResponse =
        query {
            val chargeback = ChargebackDAO.findById(id) ?: id.throwNotFound("Chargeback")
            chargeback.toResponse()
        }

    suspend fun create(request: ChargebackRequest): ChargebackResponse =
        query {
            val payment = PaymentDAO.findById(request.paymentId) ?: request.paymentId.throwNotFound("Payment")
            val amount = request.amount ?: payment.amount
            if (amount <= BigDecimal.ZERO) throw ValidationException("Chargeback amount must be positive")
            if (amount > payment.amount) throw ValidationException("Chargeback amount cannot exceed the payment amount")
            request.providerDisputeId?.takeIf { it.isNotBlank() }?.let { disputeId ->
                if (ChargebackDAO.find { ChargebackTable.providerDisputeId eq disputeId }.any()) {
                    throw ValidationException("Chargeback already recorded for dispute $disputeId")
                }
            }
            ChargebackDAO.new {
                this.paymentId = payment.id
                this.orderId = payment.orderId
                this.providerDisputeId = request.providerDisputeId?.takeIf { it.isNotBlank() }
                this.amount = amount
                this.currency = payment.currency
                this.status = ChargebackStatus.OPEN
                this.reason = request.reason
                this.evidenceDueAt = parseDate(request.evidenceDueAt)
            }.toResponse()
        }

    suspend fun update(
        id: String,
        request: ChargebackUpdateRequest,
    ): ChargebackResponse =
        query {
            val chargeback = ChargebackDAO.findById(id) ?: id.throwNotFound("Chargeback")
            if (chargeback.isResolved()) throw ValidationException("Chargeback is already resolved")
            if (!canTransition(chargeback.status, request.status)) {
                throw ValidationException("Invalid chargeback transition: ${chargeback.status} -> ${request.status}")
            }
            chargeback.status = request.status
            request.outcome?.let { chargeback.outcome = it }
            request.reason?.let { chargeback.reason = it }
            if (request.status in listOf(ChargebackStatus.WON, ChargebackStatus.LOST, ChargebackStatus.CLOSED)) {
                chargeback.resolvedAt = currentUtc()
            }
            chargeback.toResponse()
        }

    private fun canTransition(
        current: ChargebackStatus,
        target: ChargebackStatus,
    ): Boolean =
        when (current) {
            ChargebackStatus.OPEN ->
                target in listOf(ChargebackStatus.UNDER_REVIEW, ChargebackStatus.WON, ChargebackStatus.LOST, ChargebackStatus.CLOSED)
            ChargebackStatus.UNDER_REVIEW ->
                target in listOf(ChargebackStatus.WON, ChargebackStatus.LOST, ChargebackStatus.CLOSED)
            ChargebackStatus.WON, ChargebackStatus.LOST, ChargebackStatus.CLOSED -> false
        }

    private fun parseDate(value: String?): LocalDateTime? {
        if (value.isNullOrBlank()) return null
        return runCatching { LocalDateTime.parse(value) }
            .recoverCatching { OffsetDateTime.parse(value).atZoneSameInstant(java.time.ZoneOffset.UTC).toLocalDateTime() }
            .getOrElse { throw ValidationException("Invalid evidenceDueAt: $value") }
    }

    private fun ChargebackDAO.toResponse() =
        ChargebackResponse(
            id = id.value,
            paymentId = paymentId.value,
            orderId = orderId.value,
            providerDisputeId = providerDisputeId,
            amount = Money.str(amount),
            currency = currency,
            status = status,
            reason = reason,
            outcome = outcome,
            evidenceDueAt = evidenceDueAt?.toUtcString(),
            openedAt = openedAt.toUtcString(),
            resolvedAt = resolvedAt?.toUtcString(),
            createdAt = createdAt.toUtcString(),
        )
}
