package com.piashcse.feature.webhook

import com.piashcse.constants.WebhookEventStatus
import com.piashcse.database.entities.CarrierWebhookEventDAO
import com.piashcse.database.entities.PaymentWebhookEventDAO
import com.piashcse.feature.payment.PaymentRepository
import com.piashcse.feature.payment.WebhookResult
import com.piashcse.feature.shipping.CarrierWebhookResult
import com.piashcse.feature.shipping.ShipmentService
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.common.PaginationMetadata
import com.piashcse.utils.extension.query
import kotlinx.serialization.Serializable

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
            val rows = mutableListOf<WebhookEventResponse>()
            if (source == null || source == WebhookSource.PAYMENT) {
                PaymentWebhookEventDAO.all()
                    .filter { status == null || it.status == status }
                    .mapTo(rows) {
                        WebhookEventResponse(
                            id = it.id.value,
                            source = WebhookSource.PAYMENT,
                            provider = it.provider.name,
                            eventId = it.eventId,
                            eventType = it.eventType,
                            status = it.status,
                            error = it.error,
                            receivedAt = it.receivedAt.toUtcString(),
                            processedAt = it.processedAt?.toUtcString(),
                        )
                    }
            }
            if (source == null || source == WebhookSource.CARRIER) {
                CarrierWebhookEventDAO.all()
                    .filter { status == null || it.status == status }
                    .mapTo(rows) {
                        WebhookEventResponse(
                            id = it.id.value,
                            source = WebhookSource.CARRIER,
                            provider = it.provider,
                            eventId = it.eventId,
                            eventType = it.eventType,
                            status = it.status,
                            error = it.error,
                            receivedAt = it.receivedAt.toUtcString(),
                            processedAt = it.processedAt?.toUtcString(),
                        )
                    }
            }
            val sorted = rows.sortedByDescending { it.receivedAt.orEmpty() }
            PaginatedResponse(
                data = sorted.drop(offset).take(limit),
                metadata = PaginationMetadata(totalCount = sorted.size.toLong(), limit = limit, offset = offset),
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
