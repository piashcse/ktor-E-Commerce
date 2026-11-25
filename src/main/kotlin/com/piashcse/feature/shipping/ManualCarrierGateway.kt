package com.piashcse.feature.shipping

class ManualCarrierGateway : CarrierGateway {
    override val provider: String = "MANUAL"

    override suspend fun quote(request: CarrierQuoteRequest): List<CarrierRate> = emptyList()

    override suspend fun purchaseLabel(request: CarrierLabelRequest): CarrierLabelResult =
        CarrierLabelResult(false, message = "No shipping carrier is configured")

    override fun verifyWebhook(
        payload: String,
        signature: String?,
    ): Boolean = false

    override fun parseWebhook(payload: String): CarrierWebhookEvent = CarrierWebhookEvent(eventId = "manual", eventType = "unknown")
}

/**
 * Shippo multi-carrier adapter (REST, no SDK).
 *
 * Quotes create a Shippo shipment; labels are purchased from the returned rate.
 * Tracking arrives through a shared-secret/HMAC webhook (`X-Carrier-Signature`).
 */
