package com.piashcse.feature.payment

import com.piashcse.constants.PaymentMethod
import com.piashcse.constants.PaymentProvider

/**
 * The single source of truth for which payment methods the storefront may
 * offer. Offline methods are valid manual flows even when the configured
 * gateway cannot settle them directly (COD, bank transfer).
 */
fun PaymentGateway.availablePaymentMethods(): List<PaymentMethod> =
    when (provider) {
        PaymentProvider.MANUAL -> listOf(PaymentMethod.COD, PaymentMethod.BANK_TRANSFER)
        PaymentProvider.STRIPE -> PaymentMethod.values().filter { supports(it) }
    }

fun PaymentGateway.isMethodAllowed(method: PaymentMethod?): Boolean = method != null && method in availablePaymentMethods()

/** Methods that need the provider to capture money before the order is paid. */
fun PaymentMethod.isOnlinePayment(): Boolean = this == PaymentMethod.CREDIT_CARD || this == PaymentMethod.PAYPAL
