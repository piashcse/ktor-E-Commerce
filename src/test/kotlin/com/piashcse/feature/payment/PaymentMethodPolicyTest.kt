package com.piashcse.feature.payment

import com.piashcse.constants.PaymentMethod
import com.piashcse.constants.PaymentProvider
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PaymentMethodPolicyTest {
    @Test
    fun `manual provider offers offline methods only`() {
        val gateway = mockk<PaymentGateway>()
        every { gateway.provider } returns PaymentProvider.MANUAL

        assertEquals(
            listOf(PaymentMethod.COD, PaymentMethod.BANK_TRANSFER),
            gateway.availablePaymentMethods(),
        )
        assertTrue(gateway.isMethodAllowed(PaymentMethod.COD))
        assertFalse(gateway.isMethodAllowed(PaymentMethod.CREDIT_CARD))
        assertFalse(gateway.isMethodAllowed(null))
    }

    @Test
    fun `stripe provider exposes only gateway supported methods`() {
        val gateway = mockk<PaymentGateway>()
        every { gateway.provider } returns PaymentProvider.STRIPE
        every { gateway.supports(any()) } answers {
            firstArg<PaymentMethod>() == PaymentMethod.CREDIT_CARD
        }

        assertEquals(listOf(PaymentMethod.CREDIT_CARD), gateway.availablePaymentMethods())
        assertTrue(gateway.isMethodAllowed(PaymentMethod.CREDIT_CARD))
        assertFalse(gateway.isMethodAllowed(PaymentMethod.COD))
    }

    @Test
    fun `online methods are limited to card and paypal`() {
        assertTrue(PaymentMethod.CREDIT_CARD.isOnlinePayment())
        assertTrue(PaymentMethod.PAYPAL.isOnlinePayment())
        assertFalse(PaymentMethod.COD.isOnlinePayment())
        assertFalse(PaymentMethod.BANK_TRANSFER.isOnlinePayment())
    }
}
