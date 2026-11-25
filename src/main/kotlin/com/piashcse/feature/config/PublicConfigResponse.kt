package com.piashcse.feature.config

import com.piashcse.constants.AppConstants
import com.piashcse.constants.PaymentMethod
import com.piashcse.service.payment.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class PublicConfigResponse(
    val guestCheckout: Boolean,
    val requirePolicyConsent: Boolean,
    val supportedCurrencies: List<String>,
    val baseCurrency: String = AppConstants.DEFAULT_CURRENCY,
    /** Methods the checkout may offer right now (prevents unpayable orders). */
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val paymentProvider: String = "MANUAL",
    val freeShippingThreshold: String = "75.00",
    val returnWindowDays: Int = 30,
)
