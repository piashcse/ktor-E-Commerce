package com.piashcse.feature.shipping

import com.piashcse.constants.AppConstants
import com.piashcse.constants.ShipmentStatus
import com.piashcse.utils.common.sha256Hex
import kotlinx.serialization.json.*
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.HexFormat
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

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
                currency = rate.string("currency") ?: AppConstants.DEFAULT_CURRENCY,
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
            currency = rate?.string("currency") ?: AppConstants.DEFAULT_CURRENCY,
        )
    }

    override fun verifyWebhook(
        payload: String,
        signature: String?,
    ): Boolean {
        if (signature.isNullOrBlank() || webhookSecret.isBlank()) return false
        // HMAC-SHA256 only. Accepting the shared secret itself would put a
        // long-lived credential on the wire and turn signatures into bearer tokens.
        val expected = hmacSha256(webhookSecret, payload)
        return MessageDigest.isEqual(
            expected.toByteArray(StandardCharsets.UTF_8),
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

        fun sha256(payload: String): String = sha256Hex(payload)
    }
}
