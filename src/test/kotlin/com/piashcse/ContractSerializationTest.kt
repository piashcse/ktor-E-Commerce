package com.piashcse

import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentMethod
import com.piashcse.model.request.PaymentRequest
import com.piashcse.model.request.ShopRequest
import com.piashcse.model.request.UpdateProductRequest
import com.piashcse.model.response.UserProfileResponse
import com.piashcse.plugin.BigDecimalSerializer
import com.piashcse.plugin.LocalDateSerializer
import com.piashcse.plugin.LocalDateTimeSerializer
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.common.PaginationMetadata
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ContractSerializationTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            serializersModule =
                SerializersModule {
                    contextual(LocalDateTimeSerializer)
                    contextual(BigDecimalSerializer)
                    contextual(LocalDateSerializer)
                }
        }

    @Test
    fun `money accepts both string and numeric json`() {
        val numeric = json.decodeFromString<PaymentRequest>("""{"orderId":"o-1","amount":19.99,"paymentMethod":"COD"}""")
        val string = json.decodeFromString<PaymentRequest>("""{"orderId":"o-1","amount":"19.99","paymentMethod":"COD"}""")
        assertEquals(numeric.amount, string.amount)
        assertEquals(PaymentMethod.COD, numeric.paymentMethod)
    }

    @Test
    fun `optional shop fields can be omitted`() {
        val shop = json.decodeFromString<ShopRequest>("""{"name":"Shop","categoryId":"c-1"}""")
        assertEquals("Shop", shop.name)
    }

    @Test
    fun `partial product update can be empty`() {
        val update = json.decodeFromString<UpdateProductRequest>("""{}""")
        assertEquals(null, update.name)
    }

    @Test
    fun `profile birth date serializes`() {
        val body =
            """{"userId":"u-1","image":null,"firstName":null,"lastName":null,"mobile":null,""" +
                """"faxNumber":null,"streetAddress":null,"city":null,"identificationType":null,""" +
                """"identificationNo":null,"occupation":null,"postCode":null,"gender":null,""" +
                """"dateOfBirth":"2000-01-02"}"""
        val profile = json.decodeFromString<UserProfileResponse>(body)
        assertNotNull(profile.dateOfBirth)
        assertTrue(json.encodeToString(profile).contains("2000-01-02"))
    }

    @Test
    fun `pagination metadata always encodes computed fields`() {
        val response =
            PaginatedResponse(
                data = listOf("a", "b"),
                metadata = PaginationMetadata(totalCount = 12, limit = 2, offset = 2),
            )
        val body = json.encodeToString(response)
        assertContains(body, "\"page\":2")
        assertContains(body, "\"perPage\":2")
        assertContains(body, "\"totalPages\":6")
        assertContains(body, "\"hasNext\":true")
        assertContains(body, "\"hasPrev\":true")
    }

    @Test
    fun `shipped is a legal transition from paid`() {
        assertTrue(OrderStatus.canTransitionTo(OrderStatus.PAID, OrderStatus.SHIPPED))
        assertTrue(OrderStatus.canTransitionTo(OrderStatus.SHIPPED, OrderStatus.DELIVERED))
        assertFalse(OrderStatus.canTransitionTo(OrderStatus.CONFIRMED, OrderStatus.DELIVERED))
    }
}
