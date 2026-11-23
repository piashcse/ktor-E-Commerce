package com.piashcse.feature.subscription

import com.piashcse.constants.AppConstants
import com.piashcse.constants.BillingInterval
import com.piashcse.constants.InvoiceStatus
import com.piashcse.constants.SubscriptionStatus
import com.piashcse.utils.validator.FieldValidationException
import com.piashcse.utils.validator.FieldViolation
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class SubscriptionPlanRequest(
    val name: String,
    val description: String,
    val productId: String,
    val interval: BillingInterval = BillingInterval.MONTH,
    @Contextual val price: BigDecimal,
    val currency: String = AppConstants.DEFAULT_CURRENCY,
    val trialDays: Int = 0,
    val isActive: Boolean = true,
) {
    init {
        if (name.isBlank()) throw FieldValidationException(listOf(FieldViolation("name", "Name is required")))
        if (price <= BigDecimal.ZERO) {
            throw FieldValidationException(listOf(FieldViolation("price", "Price must be positive")))
        }
        if (trialDays < 0) throw FieldValidationException(listOf(FieldViolation("trialDays", "Trial days cannot be negative")))
    }
}

@Serializable
data class SubscriptionPlanResponse(
    val id: String,
    val name: String,
    val description: String,
    val productId: String,
    val interval: BillingInterval,
    val price: String,
    val currency: String,
    val trialDays: Int,
    val isActive: Boolean,
)

@Serializable
data class SubscribeRequest(
    val planId: String,
    val paymentReference: String? = null,
    /** Stored provider payment-method token used for off-session renewals. */
    val paymentMethodReference: String? = null,
    /** Provider customer id that owns [paymentMethodReference]. */
    val providerCustomerReference: String? = null,
)

@Serializable
data class ActivateSubscriptionRequest(val paymentReference: String)

@Serializable
data class SubscriptionResponse(
    val id: String,
    val userId: String,
    val planId: String,
    val planName: String,
    val productId: String,
    val status: SubscriptionStatus,
    val startedAt: String,
    val currentPeriodStart: String,
    val currentPeriodEnd: String,
    val cancelAtPeriodEnd: Boolean,
    val externalReference: String?,
    val hasPaymentMethod: Boolean = false,
    val dunningAttempts: Int = 0,
    val graceUntil: String? = null,
)

@Serializable
data class BillingRunResponse(
    val invoicesCreated: Int,
    val canceled: Int,
    val paid: Int = 0,
    val failed: Int = 0,
    val expired: Int = 0,
)

@Serializable
data class SubscriptionInvoiceResponse(
    val id: String,
    val subscriptionId: String,
    val amount: String,
    val currency: String,
    val status: InvoiceStatus,
    val periodStart: String,
    val periodEnd: String,
    val providerReference: String?,
    val paidAt: String?,
)
