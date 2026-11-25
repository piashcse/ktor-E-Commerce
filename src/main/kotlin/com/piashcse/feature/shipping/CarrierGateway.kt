package com.piashcse.feature.shipping

import com.piashcse.constants.AppConstants
import com.piashcse.constants.ShipmentStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

data class CarrierAddress(
    val name: String,
    val street1: String,
    val city: String,
    val state: String? = null,
    val zip: String,
    val country: String,
    val phone: String? = null,
    val email: String? = null,
)

data class CarrierParcel(
    val length: BigDecimal,
    val width: BigDecimal,
    val height: BigDecimal,
    val distanceUnit: String = "in",
    val weight: BigDecimal,
    val massUnit: String = "lb",
)

data class CarrierQuoteRequest(
    val orderId: String,
    val from: CarrierAddress,
    val to: CarrierAddress,
    val parcels: List<CarrierParcel>,
)

@Serializable
data class CarrierRate(
    val id: String,
    val carrier: String,
    val service: String,
    @Contextual val amount: BigDecimal,
    val currency: String = AppConstants.DEFAULT_CURRENCY,
    val estimatedDays: Int? = null,
)

data class CarrierLabelRequest(
    val rateId: String,
    val orderId: String,
)

data class CarrierLabelResult(
    val success: Boolean,
    val carrier: String? = null,
    val service: String? = null,
    val trackingNumber: String? = null,
    val labelUrl: String? = null,
    val providerReference: String? = null,
    val amount: BigDecimal? = null,
    val currency: String = AppConstants.DEFAULT_CURRENCY,
    val message: String? = null,
)

data class CarrierWebhookEvent(
    val eventId: String,
    val eventType: String,
    val trackingNumber: String? = null,
    val providerReference: String? = null,
    val status: ShipmentStatus? = null,
    val description: String? = null,
)

/**
 * Carrier abstraction. A configured provider buys labels and receives tracking
 * updates; without one, sellers can still ship manually via the order route.
 */
interface CarrierGateway {
    val provider: String
    val webhookSignatureHeaders: List<String> get() = listOf("X-Carrier-Signature")

    suspend fun quote(request: CarrierQuoteRequest): List<CarrierRate>

    suspend fun purchaseLabel(request: CarrierLabelRequest): CarrierLabelResult

    fun verifyWebhook(
        payload: String,
        signature: String?,
    ): Boolean

    fun parseWebhook(payload: String): CarrierWebhookEvent
}

interface CarrierHttpTransport {
    suspend fun postJson(
        url: String,
        headers: Map<String, String>,
        body: String,
    ): CarrierHttpResponse
}

data class CarrierHttpResponse(
    val status: Int,
    val body: String,
)

class JdkCarrierHttpTransport(
    private val timeout: Duration = Duration.ofSeconds(15),
) : CarrierHttpTransport {
    private val client = HttpClient.newBuilder().connectTimeout(timeout).build()

    override suspend fun postJson(
        url: String,
        headers: Map<String, String>,
        body: String,
    ): CarrierHttpResponse =
        withContext(Dispatchers.IO) {
            val request =
                HttpRequest.newBuilder(URI.create(url))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .apply { headers.forEach { (key, value) -> header(key, value) } }
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            CarrierHttpResponse(response.statusCode(), response.body())
        }
}
