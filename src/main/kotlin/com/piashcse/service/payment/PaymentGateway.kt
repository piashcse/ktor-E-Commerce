package com.piashcse.service.payment

import com.piashcse.constants.AvsResult
import com.piashcse.constants.CvvResult
import com.piashcse.constants.PaymentMethod
import com.piashcse.constants.PaymentProvider
import com.piashcse.constants.ScaStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

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
        require3ds: Boolean = false,
    ): GatewayChargeResult

    /** Returns the amount to the original instrument. */
    suspend fun refund(
        providerReference: String,
        amount: BigDecimal,
        currency: String,
        idempotencyKey: String,
    ): GatewayRefundResult

    /**
     * Charge a stored payment method for a subscription renewal. Providers that do
     * not support off-session/recurring charges return a failed result so the invoice
     * stays pending for manual collection.
     */
    suspend fun chargeSubscription(
        subscriptionId: String,
        invoiceId: String,
        amount: BigDecimal,
        currency: String,
        providerCustomerReference: String?,
        paymentMethodReference: String,
        idempotencyKey: String,
    ): GatewayChargeResult = GatewayChargeResult(false, message = "Recurring charges are not supported by $name")

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
    val avsStatus: AvsResult = AvsResult.UNKNOWN,
    val cvvStatus: CvvResult = CvvResult.UNKNOWN,
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
    /** Provider metadata, used to route subscription renewal events. */
    val metadata: Map<String, String> = emptyMap(),
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
