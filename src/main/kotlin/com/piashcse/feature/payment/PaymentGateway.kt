package com.piashcse.feature.payment

import com.piashcse.constants.PaymentMethod
import com.piashcse.constants.PaymentProvider
import com.piashcse.constants.ScaStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Duration
import java.util.HexFormat
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Payment gateway abstraction. Real providers (Stripe/SSLCommerz/bKash) implement
 * this interface. Until a provider is configured only CASH_ON_DELIVERY is allowed:
 * the charge is recorded as PENDING and must be confirmed by an admin when cash is
 * collected. This prevents a client from self-declaring a successful payment.
 */
interface PaymentGateway {
    val name: String
    val provider: PaymentProvider
    val webhookSignatureHeaders: List<String> get() = listOf("X-Webhook-Signature")

    fun supports(paymentMethod: PaymentMethod): Boolean

    suspend fun charge(
        orderId: String,
        amount: BigDecimal,
        currency: String,
        paymentMethod: PaymentMethod,
        transactionId: String?,
        returnUrl: String? = null,
        idempotencyKey: String? = null,
    ): GatewayChargeResult

    /** Returns the amount to the original instrument. */
    suspend fun refund(
        providerReference: String,
        amount: BigDecimal,
        currency: String,
        idempotencyKey: String,
    ): GatewayRefundResult

    fun verifyWebhook(
        payload: String,
        signature: String?,
    ): Boolean

    fun parseWebhook(payload: String): GatewayWebhookEvent
}

data class GatewayChargeResult(
    val success: Boolean,
    /** True when the provider captured money immediately (card/wallet), false for COD/manual/3DS-pending. */
    val captured: Boolean = false,
    val gatewayTransactionId: String? = null,
    val clientSecret: String? = null,
    val nextActionUrl: String? = null,
    val scaStatus: ScaStatus = ScaStatus.NOT_REQUIRED,
    val providerStatus: String? = null,
    val message: String? = null,
)

data class GatewayRefundResult(
    val success: Boolean,
    val providerRefundId: String? = null,
    val status: String? = null,
    val message: String? = null,
)

enum class GatewayEventType {
    PAYMENT_SUCCEEDED,
    PAYMENT_FAILED,
    PAYMENT_REQUIRES_ACTION,
    PAYMENT_PROCESSING,
    REFUND_UPDATED,
    UNKNOWN,
}

data class GatewayRefundUpdate(
    val providerRefundId: String,
    val status: String,
    val amount: BigDecimal? = null,
)

data class GatewayWebhookEvent(
    val eventId: String,
    val provider: PaymentProvider,
    val type: GatewayEventType,
    val providerReference: String? = null,
    val amount: BigDecimal? = null,
    val currency: String? = null,
    val scaStatus: ScaStatus? = null,
    val providerStatus: String? = null,
    val refund: GatewayRefundUpdate? = null,
)

/** Minimal transport seam so provider adapters can be tested without network access. */
interface HttpTransport {
    suspend fun postForm(
        url: String,
        headers: Map<String, String>,
        form: Map<String, String>,
    ): GatewayHttpResponse
}

data class GatewayHttpResponse(
    val status: Int,
    val body: String,
)

class JdkHttpTransport(
    private val timeout: Duration = Duration.ofSeconds(15),
) : HttpTransport {
    private val client = HttpClient.newBuilder().connectTimeout(timeout).build()

    override suspend fun postForm(
        url: String,
        headers: Map<String, String>,
        form: Map<String, String>,
    ): GatewayHttpResponse =
        withContext(Dispatchers.IO) {
            val body =
                form.entries.joinToString("&") { (key, value) ->
                    "${encode(key)}=${encode(value)}"
                }
            val request =
                HttpRequest.newBuilder(URI.create(url))
                    .timeout(timeout)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .apply { headers.forEach { (key, value) -> header(key, value) } }
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            GatewayHttpResponse(response.statusCode(), response.body())
        }

    private fun encode(value: String) = URLEncoder.encode(value, StandardCharsets.UTF_8)
}

class ManualPaymentGateway : PaymentGateway {
    override val name: String = "CASH_ON_DELIVERY"
    override val provider: PaymentProvider = PaymentProvider.MANUAL

    override fun supports(paymentMethod: PaymentMethod): Boolean = paymentMethod == PaymentMethod.COD

    override suspend fun charge(
        orderId: String,
        amount: BigDecimal,
        currency: String,
        paymentMethod: PaymentMethod,
        transactionId: String?,
        returnUrl: String?,
        idempotencyKey: String?,
    ): GatewayChargeResult =
        if (paymentMethod == PaymentMethod.COD) {
            GatewayChargeResult(
                success = true,
                captured = false,
                gatewayTransactionId = transactionId,
                providerStatus = "manual",
                message = "Cash on delivery recorded; payment will be confirmed on collection",
            )
        } else {
            GatewayChargeResult(
                success = false,
                message = "No payment provider is configured for $paymentMethod. Use CASH_ON_DELIVERY or configure a gateway.",
            )
        }

    override suspend fun refund(
        providerReference: String,
        amount: BigDecimal,
        currency: String,
        idempotencyKey: String,
    ): GatewayRefundResult = GatewayRefundResult(false, message = "Manual payments must be refunded out-of-band")

    override fun verifyWebhook(
        payload: String,
        signature: String?,
    ): Boolean = false

    override fun parseWebhook(payload: String): GatewayWebhookEvent =
        GatewayWebhookEvent("manual", PaymentProvider.MANUAL, GatewayEventType.UNKNOWN)
}

/**
 * Stripe adapter using only the REST API (no SDK).
 *
 * Flow: create a PaymentIntent, return `clientSecret`/`nextActionUrl`, then let the
 * client confirm. 3DS/SCA outcomes and captures arrive through signed webhooks.
 */
class StripePaymentGateway(
    private val secretKey: String,
    private val webhookSecret: String,
    private val baseUrl: String = "https://api.stripe.com",
    private val transport: HttpTransport = JdkHttpTransport(),
    private val webhookToleranceSeconds: Long = 300,
    private val clock: () -> Long = System::currentTimeMillis,
) : PaymentGateway {
    override val name: String = "STRIPE"
    override val provider: PaymentProvider = PaymentProvider.STRIPE
    override val webhookSignatureHeaders: List<String> get() = listOf("Stripe-Signature")

    private val json = Json { ignoreUnknownKeys = true }

    override fun supports(paymentMethod: PaymentMethod): Boolean = paymentMethod == PaymentMethod.CREDIT_CARD

    override suspend fun charge(
        orderId: String,
        amount: BigDecimal,
        currency: String,
        paymentMethod: PaymentMethod,
        transactionId: String?,
        returnUrl: String?,
        idempotencyKey: String?,
    ): GatewayChargeResult {
        if (!supports(paymentMethod)) {
            return GatewayChargeResult(false, message = "Stripe does not support $paymentMethod")
        }
        if (secretKey.isBlank()) {
            return GatewayChargeResult(false, message = "Stripe is not configured")
        }

        val response =
            transport.postForm(
                url = "$baseUrl/v1/payment_intents",
                headers =
                    buildMap {
                        put("Authorization", "Bearer $secretKey")
                        idempotencyKey?.let { put("Idempotency-Key", it) }
                    },
                form =
                    buildMap {
                        put("amount", amountToMinorUnits(amount))
                        put("currency", currency.lowercase())
                        put("automatic_payment_methods[enabled]", "true")
                        put("metadata[order_id]", orderId)
                        put("description", "Order $orderId")
                        transactionId?.let { put("metadata[transaction_id]", it) }
                        put("automatic_payment_methods[allow_redirects]", "always")
                        // Return URL is metadata only: confirmation (and 3DS) is client-side.
                        returnUrl?.let { put("metadata[return_url]", it) }
                    },
            )

        val body = runCatching { json.parseToJsonElement(response.body).jsonObject }.getOrNull()
        if (response.status !in 200..299) {
            return GatewayChargeResult(false, message = stripeError(body, response.status))
        }
        val intent = body ?: return GatewayChargeResult(false, message = "Invalid Stripe response")
        val status = intent.string("status") ?: "unknown"
        val intentId = intent.string("id")
        val clientSecret = intent.string("client_secret")
        val nextActionUrl =
            intent["next_action"]
                ?.jsonObject
                ?.get("redirect_to_url")
                ?.jsonObject
                ?.get("url")
                ?.jsonPrimitive
                ?.contentOrNull

        return when (status) {
            "succeeded" ->
                GatewayChargeResult(
                    success = true,
                    captured = true,
                    gatewayTransactionId = intentId,
                    clientSecret = clientSecret,
                    scaStatus = ScaStatus.AUTHENTICATED,
                    providerStatus = status,
                )
            "requires_action" ->
                GatewayChargeResult(
                    success = true,
                    gatewayTransactionId = intentId,
                    clientSecret = clientSecret,
                    nextActionUrl = nextActionUrl,
                    scaStatus = ScaStatus.REQUIRED,
                    providerStatus = status,
                )
            "requires_confirmation" ->
                GatewayChargeResult(
                    success = true,
                    gatewayTransactionId = intentId,
                    clientSecret = clientSecret,
                    scaStatus = ScaStatus.REQUIRED,
                    providerStatus = status,
                )
            "processing" ->
                GatewayChargeResult(
                    success = true,
                    gatewayTransactionId = intentId,
                    clientSecret = clientSecret,
                    scaStatus = ScaStatus.PENDING,
                    providerStatus = status,
                )
            "requires_payment_method" ->
                GatewayChargeResult(
                    success = true,
                    gatewayTransactionId = intentId,
                    clientSecret = clientSecret,
                    scaStatus = ScaStatus.NOT_REQUIRED,
                    providerStatus = status,
                )
            "canceled" -> GatewayChargeResult(false, message = "Payment intent was canceled")
            else ->
                GatewayChargeResult(
                    success = true,
                    gatewayTransactionId = intentId,
                    clientSecret = clientSecret,
                    scaStatus = ScaStatus.PENDING,
                    providerStatus = status,
                )
        }
    }

    override suspend fun refund(
        providerReference: String,
        amount: BigDecimal,
        currency: String,
        idempotencyKey: String,
    ): GatewayRefundResult {
        if (secretKey.isBlank()) return GatewayRefundResult(false, message = "Stripe is not configured")
        val response =
            transport.postForm(
                url = "$baseUrl/v1/refunds",
                headers =
                    mapOf(
                        "Authorization" to "Bearer $secretKey",
                        "Idempotency-Key" to idempotencyKey,
                    ),
                form =
                    mapOf(
                        "payment_intent" to providerReference,
                        "amount" to amountToMinorUnits(amount),
                    ),
            )
        val body = runCatching { json.parseToJsonElement(response.body).jsonObject }.getOrNull()
        if (response.status !in 200..299) {
            return GatewayRefundResult(false, message = stripeError(body, response.status))
        }
        val refund = body ?: return GatewayRefundResult(false, message = "Invalid Stripe response")
        val status = refund.string("status") ?: "unknown"
        return GatewayRefundResult(
            success = status in setOf("succeeded", "pending", "requires_action"),
            providerRefundId = refund.string("id"),
            status = status,
            message = if (status in setOf("succeeded", "pending", "requires_action")) null else "Refund failed: $status",
        )
    }

    override fun verifyWebhook(
        payload: String,
        signature: String?,
    ): Boolean {
        if (signature.isNullOrBlank() || webhookSecret.isBlank()) return false
        val parts =
            signature.split(',')
                .mapNotNull { part ->
                    val index = part.indexOf('=')
                    if (index <= 0) null else part.substring(0, index) to part.substring(index + 1)
                }
        val timestamp = parts.firstOrNull { it.first == "t" }?.second?.toLongOrNull() ?: return false
        val signatures = parts.filter { it.first == "v1" }.map { it.second }
        if (signatures.isEmpty()) return false
        if (kotlin.math.abs(clock() / 1000 - timestamp) > webhookToleranceSeconds) return false

        val expected = hmacSha256(webhookSecret, "$timestamp.$payload")
        return signatures.any { candidate ->
            MessageDigest.isEqual(expected.toByteArray(StandardCharsets.UTF_8), candidate.toByteArray(StandardCharsets.UTF_8))
        }
    }

    override fun parseWebhook(payload: String): GatewayWebhookEvent {
        val root = json.parseToJsonElement(payload).jsonObject
        val eventId = root.string("id") ?: "unknown"
        val type = root.string("type") ?: "unknown"
        val obj = root["data"]?.jsonObject?.get("object")?.jsonObject

        if (type.startsWith("refund.") || type == "charge.refunded") {
            val refundObject =
                if (type == "charge.refunded") {
                    obj?.get("refunds")?.jsonObject?.get("data")?.let { data ->
                        runCatching { data.jsonArray.first().jsonObject }.getOrNull()
                    }
                } else {
                    obj
                }
            val refundId = refundObject?.string("id")
            return GatewayWebhookEvent(
                eventId = eventId,
                provider = provider,
                type = GatewayEventType.REFUND_UPDATED,
                providerReference = refundObject?.string("payment_intent"),
                amount = refundObject?.minorUnits("amount"),
                currency = refundObject?.string("currency"),
                refund =
                    refundId?.let {
                        GatewayRefundUpdate(
                            providerRefundId = it,
                            status = refundObject.string("status") ?: "unknown",
                            amount = refundObject.minorUnits("amount"),
                        )
                    },
            )
        }

        val intentId = obj?.string("id")
        val intentStatus = obj?.string("status")
        val amount = obj?.minorUnits("amount_received")?.takeIf { it > BigDecimal.ZERO } ?: obj?.minorUnits("amount")
        val currency = obj?.string("currency")
        val eventType =
            when (type) {
                "payment_intent.succeeded" -> GatewayEventType.PAYMENT_SUCCEEDED
                "payment_intent.payment_failed", "payment_intent.canceled" -> GatewayEventType.PAYMENT_FAILED
                "payment_intent.requires_action", "payment_intent.requires_confirmation" -> GatewayEventType.PAYMENT_REQUIRES_ACTION
                "payment_intent.processing",
                "payment_intent.created",
                "payment_intent.requires_capture",
                -> GatewayEventType.PAYMENT_PROCESSING
                else -> GatewayEventType.UNKNOWN
            }
        return GatewayWebhookEvent(
            eventId = eventId,
            provider = provider,
            type = eventType,
            providerReference = intentId.takeIf { eventType != GatewayEventType.UNKNOWN },
            amount = amount,
            currency = currency,
            scaStatus =
                when (eventType) {
                    GatewayEventType.PAYMENT_SUCCEEDED -> ScaStatus.AUTHENTICATED
                    GatewayEventType.PAYMENT_FAILED -> ScaStatus.FAILED
                    GatewayEventType.PAYMENT_REQUIRES_ACTION -> ScaStatus.REQUIRED
                    GatewayEventType.PAYMENT_PROCESSING -> ScaStatus.PENDING
                    else -> null
                },
            providerStatus = intentStatus,
        )
    }

    private fun stripeError(
        body: JsonObject?,
        statusCode: Int,
    ): String =
        body?.get("error")?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
            ?: "Stripe request failed with HTTP $statusCode"

    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.minorUnits(key: String): BigDecimal? =
        this[key]?.jsonPrimitive?.contentOrNull?.toLongOrNull()?.let {
            BigDecimal(it).movePointLeft(2).setScale(2, RoundingMode.HALF_UP)
        }

    private fun amountToMinorUnits(amount: BigDecimal): String = amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).toPlainString()

    companion object {
        fun hmacSha256(
            secret: String,
            payload: String,
        ): String {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
            return HexFormat.of().formatHex(mac.doFinal(payload.toByteArray(StandardCharsets.UTF_8)))
        }
    }
}
