package com.piashcse.feature.webhook

import com.piashcse.constants.WebhookEventStatus
import com.piashcse.database.entities.CarrierWebhookEventTable
import com.piashcse.database.entities.PaymentWebhookEventTable
import com.piashcse.feature.payment.PaymentRepository
import com.piashcse.feature.payment.WebhookResult
import com.piashcse.feature.shipping.CarrierWebhookResult
import com.piashcse.feature.shipping.ShipmentService
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.common.PaginationMetadata
import com.piashcse.utils.extension.query
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

@Serializable
enum class WebhookSource {
    PAYMENT,
    CARRIER,
}

@Serializable
data class WebhookEventResponse(
    val id: String,
    val source: WebhookSource,
    val provider: String,
    val eventId: String,
    val eventType: String,
    val status: WebhookEventStatus,
    val error: String?,
    val receivedAt: String?,
    val processedAt: String?,
)

/** Admin inspection and replay of the durable payment/carrier webhook inboxes. */
class WebhookAdminService(
    private val payments: PaymentRepository,
    private val shipments: ShipmentService,
) {
    suspend fun list(
        source: WebhookSource?,
        status: WebhookEventStatus?,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<WebhookEventResponse> =
        query {
            val fetch = (offset + limit).coerceAtLeast(limit)
            val includePayment = source == null || source == WebhookSource.PAYMENT
            val includeCarrier = source == null || source == WebhookSource.CARRIER

            val paymentQuery =
                PaymentWebhookEventTable.selectAll().apply {
                    status?.let { value -> andWhere { PaymentWebhookEventTable.status eq value } }
                }
            val carrierQuery =
                CarrierWebhookEventTable.selectAll().apply {
                    status?.let { value -> andWhere { CarrierWebhookEventTable.status eq value } }
                }

            val paymentTotal = if (includePayment) paymentQuery.count() else 0L
            val carrierTotal = if (includeCarrier) carrierQuery.count() else 0L

            val rows = mutableListOf<WebhookEventResponse>()
            if (includePayment) {
                paymentQuery
                    .orderBy(PaymentWebhookEventTable.receivedAt to SortOrder.DESC)
                    .limit(fetch)
                    .mapTo(rows) { row ->
                        WebhookEventResponse(
                            id = row[PaymentWebhookEventTable.id].value,
                            source = WebhookSource.PAYMENT,
                            provider = row[PaymentWebhookEventTable.provider].name,
                            eventId = row[PaymentWebhookEventTable.eventId],
                            eventType = row[PaymentWebhookEventTable.eventType],
                            status = row[PaymentWebhookEventTable.status],
                            error = row[PaymentWebhookEventTable.error],
                            receivedAt = row[PaymentWebhookEventTable.receivedAt].toUtcString(),
                            processedAt = row[PaymentWebhookEventTable.processedAt]?.toUtcString(),
                        )
                    }
            }
            if (includeCarrier) {
                carrierQuery
                    .orderBy(CarrierWebhookEventTable.receivedAt to SortOrder.DESC)
                    .limit(fetch)
                    .mapTo(rows) { row ->
                        WebhookEventResponse(
                            id = row[CarrierWebhookEventTable.id].value,
                            source = WebhookSource.CARRIER,
                            provider = row[CarrierWebhookEventTable.provider],
                            eventId = row[CarrierWebhookEventTable.eventId],
                            eventType = row[CarrierWebhookEventTable.eventType],
                            status = row[CarrierWebhookEventTable.status],
                            error = row[CarrierWebhookEventTable.error],
                            receivedAt = row[CarrierWebhookEventTable.receivedAt].toUtcString(),
                            processedAt = row[CarrierWebhookEventTable.processedAt]?.toUtcString(),
                        )
                    }
            }
            val sorted = rows.sortedByDescending { it.receivedAt.orEmpty() }
            PaginatedResponse(
                data = sorted.drop(offset).take(limit),
                metadata =
                    PaginationMetadata(
                        totalCount = paymentTotal + carrierTotal,
                        limit = limit,
                        offset = offset,
                    ),
            )
        }

    suspend fun replayPayment(
        eventId: String,
        actorUserId: String,
    ): WebhookResult = payments.replayWebhook(eventId, actorUserId)

    suspend fun replayCarrier(
        eventId: String,
        actorUserId: String,
    ): CarrierWebhookResult = shipments.replayWebhook(eventId, actorUserId)
}
