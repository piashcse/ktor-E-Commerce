package com.piashcse.feature.checkout

import com.piashcse.model.request.GuestCheckoutItem
import com.piashcse.model.request.GuestCheckoutRequest
import org.valiktor.ConstraintViolationException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class GuestCheckoutRequestTest {
    private fun valid() =
        GuestCheckoutRequest(
            email = "guest@example.com",
            firstName = "Guest",
            lastName = "User",
            phoneNumber = "123456",
            streetAddress = "1 Main St",
            city = "Dhaka",
            country = "BD",
            postCode = "1000",
            shippingMethodId = "m-1",
            items = listOf(GuestCheckoutItem("p-1", 1)),
        )

    @Test
    fun `valid guest checkout request`() {
        valid()
    }

    @Test
    fun `guest checkout requires at least one item`() {
        assertFailsWith<ConstraintViolationException> { valid().copy(items = emptyList()) }
    }

    @Test
    fun `guest checkout rejects invalid email`() {
        assertFailsWith<ConstraintViolationException> { valid().copy(email = "not-an-email") }
    }

    @Test
    fun `guest checkout rejects non-positive quantity`() {
        assertFailsWith<ConstraintViolationException> { GuestCheckoutItem("p-1", 0) }
    }
}
