package com.piashcse.feature.notification

import com.piashcse.event.OutboxPublisher
import com.piashcse.event.RmaStatusChangedEvent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.modules.contextual
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class NotificationTemplatesTest {
    @Test
    fun `order template includes number and total`() {
        val content = NotificationTemplates.orderPlaced("ORD-1", "10.00")
        assertEquals("Order ORD-1 placed", content.title)
        assertTrue(content.body.contains("10.00"))
    }

    @Test
    fun `refund template describes the transition`() {
        val content = NotificationTemplates.refundStatusChanged("r-1", "PENDING", "APPROVED")
        assertTrue(content.title.contains("APPROVED"))
        assertTrue(content.body.contains("from PENDING to APPROVED"))
    }

    @Test
    fun `rma template describes the transition`() {
        val content = NotificationTemplates.rmaStatusChanged("rma-1", "REQUESTED", "APPROVED")
        assertEquals("Return/exchange APPROVED", content.title)
        assertTrue(content.body.contains("rma-1"))
    }

    @Test
    fun `rma event round-trips through the outbox`() {
        val event =
            RmaStatusChangedEvent(
                rmaId = "rma-1",
                orderId = "o-1",
                userId = "u-1",
                email = "buyer@example.com",
                fromStatus = "REQUESTED",
                toStatus = "APPROVED",
                occurredAt = java.time.LocalDateTime.of(2026, 1, 1, 0, 0),
            )
        val encoded = encode(event)
        val decoded = OutboxPublisher.decode("RMA_STATUS_CHANGED", encoded)
        assertIs<RmaStatusChangedEvent>(decoded)
        assertEquals("APPROVED", decoded.toStatus)
    }

    private fun encode(event: RmaStatusChangedEvent): String {
        val json =
            kotlinx.serialization.json.Json {
                serializersModule =
                    kotlinx.serialization.modules.SerializersModule {
                        contextual(com.piashcse.plugin.BigDecimalSerializer)
                        contextual(com.piashcse.plugin.LocalDateTimeSerializer)
                    }
            }
        return json.encodeToString(RmaStatusChangedEvent.serializer(), event)
    }
}
