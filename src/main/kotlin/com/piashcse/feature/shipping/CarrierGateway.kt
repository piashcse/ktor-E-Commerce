package com.piashcse.feature.shipping

import com.piashcse.constants.ShipmentStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.math.BigDecimal
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Duration
import java.util.HexFormat
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

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
    val currency: String = "USD",
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
    val currency: String = "USD",
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
class ShippoCarrierGateway(
    private val apiToken: String,
    private val webhookSecret: String,
    private val baseUrl: String = "https://api.goshippo.com",
    private val transport: CarrierHttpTransport = JdkCarrierHttpTransport(),
) : CarrierGateway {
    override val provider: String = "SHIPPO"
    override val webhookSignatureHeaders: List<String> get() = listOf("X-Carrier-Signature")

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun quote(request: CarrierQuoteRequest): List<CarrierRate> {
        if (apiToken.isBlank() || request.parcels.isEmpty()) return emptyList()
        val body =
            buildJsonObject {
                putJsonObject("address_from") { address(request.from) }
                putJsonObject("address_to") { address(request.to) }
                putJsonArray("parcels") {
                    request.parcels.forEach { parcel ->
                        addJsonObject {
                            put("length", parcel.length.toPlainString())
                            put("width", parcel.width.toPlainString())
                            put("height", parcel.height.toPlainString())
                            put("distance_unit", parcel.distanceUnit)
                            put("weight", parcel.weight.toPlainString())
                            put("mass_unit", parcel.massUnit)
                        }
                    }
                }
                put("async", false)
            }
        val response =
            transport.postJson(
                url = "$baseUrl/shipments/",
                headers = authHeaders(),
                body = body.toString(),
            )
        val root = runCatching { json.parseToJsonElement(response.body).jsonObject }.getOrNull()
        if (response.status !in 200..299) return emptyList()
        return root?.get("rates")?.jsonArray?.mapNotNull { element ->
            val rate = element.jsonObject
            val id = rate.string("object_id") ?: return@mapNotNull null
            val amount = rate.string("amount")?.toBigDecimalOrNull() ?: return@mapNotNull null
            CarrierRate(
                id = id,
                carrier = rate.string("provider") ?: "unknown",
                service = rate.string("servicelevel_name") ?: "standard",
                amount = amount,
                currency = rate.string("currency") ?: "USD",
                estimatedDays = rate.string("estimated_days")?.toIntOrNull(),
            )
        } ?: emptyList()
    }

    override suspend fun purchaseLabel(request: CarrierLabelRequest): CarrierLabelResult {
        if (apiToken.isBlank()) {
            return CarrierLabelResult(false, message = "Shippo is not configured")
        }
        val body =
            buildJsonObject {
                put("rate", request.rateId)
                put("label_file_type", "PDF")
                put("async", false)
            }
        val response =
            transport.postJson(
                url = "$baseUrl/transactions/",
                headers = authHeaders(),
                body = body.toString(),
            )
        val root = runCatching { json.parseToJsonElement(response.body).jsonObject }.getOrNull()
        if (response.status !in 200..299) {
            return CarrierLabelResult(false, message = "Shippo label purchase failed (HTTP ${response.status})")
        }
        val status = root?.string("status") ?: "UNKNOWN"
        if (status != "SUCCESS") {
            val message =
                root?.get("messages")?.jsonArray?.joinToString("; ") {
                    it.jsonObject.string("text") ?: ""
                }?.takeIf { it.isNotBlank() } ?: "Shippo transaction returned $status"
            return CarrierLabelResult(false, message = message)
        }
        val rate = root?.get("rate")?.jsonObject
        return CarrierLabelResult(
            success = true,
            carrier = rate?.string("provider"),
            service = rate?.string("servicelevel_name"),
            trackingNumber = root?.string("tracking_number"),
            labelUrl = root?.string("label_url"),
            providerReference = root?.string("object_id"),
            amount = rate?.string("amount")?.toBigDecimalOrNull(),
            currency = rate?.string("currency") ?: "USD",
        )
    }

    override fun verifyWebhook(
        payload: String,
        signature: String?,
    ): Boolean {
        if (signature.isNullOrBlank() || webhookSecret.isBlank()) return false
        // Accept either an HMAC-SHA256 hex digest or the shared secret itself.
        val expected = hmacSha256(webhookSecret, payload)
        return MessageDigest.isEqual(
            expected.toByteArray(StandardCharsets.UTF_8),
            signature.toByteArray(StandardCharsets.UTF_8),
        ) ||
            MessageDigest.isEqual(
                webhookSecret.toByteArray(StandardCharsets.UTF_8),
                signature.toByteArray(StandardCharsets.UTF_8),
            )
    }

    override fun parseWebhook(payload: String): CarrierWebhookEvent {
        val root = runCatching { json.parseToJsonElement(payload).jsonObject }.getOrNull()
        val eventType = root?.string("event") ?: "unknown"
        val data = root?.get("data")?.jsonObject
        val tracking = data?.get("tracking_status")?.jsonObject
        val statusCode = tracking?.string("status") ?: data?.string("status")
        return CarrierWebhookEvent(
            eventId = sha256(payload),
            eventType = eventType,
            trackingNumber = data?.string("tracking_number"),
            providerReference = data?.string("object_id"),
            status =
                when (statusCode?.uppercase()) {
                    "PRE_TRANSIT", "TRANSIT", "OUT_FOR_DELIVERY" -> ShipmentStatus.IN_TRANSIT
                    "DELIVERED" -> ShipmentStatus.DELIVERED
                    "RETURNED", "RETURN_TO_SENDER" -> ShipmentStatus.RETURNED
                    "FAILURE", "FAILED", "EXPIRED" -> ShipmentStatus.FAILED
                    else -> null
                },
            description = tracking?.string("status_details"),
        )
    }

    private fun authHeaders(): Map<String, String> = mapOf("Authorization" to "ShippoToken $apiToken")

    private fun JsonObjectBuilder.address(address: CarrierAddress) {
        put("name", address.name)
        put("street1", address.street1)
        put("city", address.city)
        address.state?.let { put("state", it) }
        put("zip", address.zip)
        put("country", address.country)
        address.phone?.let { put("phone", it) }
        address.email?.let { put("email", it) }
    }

    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

    companion object {
        fun hmacSha256(
            secret: String,
            payload: String,
        ): String {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
            return HexFormat.of().formatHex(mac.doFinal(payload.toByteArray(StandardCharsets.UTF_8)))
        }

        fun sha256(payload: String): String =
            HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(StandardCharsets.UTF_8)))
    }
}
