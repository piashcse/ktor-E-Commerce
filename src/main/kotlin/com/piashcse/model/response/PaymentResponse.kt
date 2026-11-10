package com.piashcse.model.response

import com.piashcse.constants.AvsResult
import com.piashcse.constants.CvvResult
import com.piashcse.constants.PaymentMethod
import com.piashcse.constants.PaymentProvider
import com.piashcse.constants.PaymentStatus
import com.piashcse.constants.RiskDecision
import com.piashcse.constants.ScaStatus
import kotlinx.serialization.Serializable

@Serializable
data class PaymentResponse(
    val id: String,
    val orderId: String,
    val amount: String,
    val currency: String = "USD",
    val status: PaymentStatus,
    val paymentMethod: PaymentMethod,
    val transactionId: String?,
    val createdAt: String? = null,
    val provider: PaymentProvider = PaymentProvider.MANUAL,
    val providerReference: String? = null,
    /** Present for client-side confirmation (e.g. Stripe.js). */
    val clientSecret: String? = null,
    /** Browser redirect for 3DS/SCA when [scaStatus] is REQUIRED. */
    val nextActionUrl: String? = null,
    val scaStatus: ScaStatus = ScaStatus.NOT_REQUIRED,
    val providerStatus: String? = null,
    val riskScore: Int = 0,
    val riskDecision: RiskDecision = RiskDecision.ALLOW,
    val avsStatus: AvsResult = AvsResult.UNKNOWN,
    val cvvStatus: CvvResult = CvvResult.UNKNOWN,
    val threeDsEnforced: Boolean = false,
)
