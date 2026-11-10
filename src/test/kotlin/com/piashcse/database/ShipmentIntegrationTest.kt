package com.piashcse.database

import com.piashcse.constants.KycStatus
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentStatus
import com.piashcse.constants.ShipmentStatus
import com.piashcse.constants.ShopStatus
import com.piashcse.constants.UserType
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderItemDAO
import com.piashcse.database.entities.ProductCategoryDAO
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.SellerDAO
import com.piashcse.database.entities.ShipmentDAO
import com.piashcse.database.entities.ShopCategoryDAO
import com.piashcse.database.entities.ShopDAO
import com.piashcse.database.entities.UserDAO
import com.piashcse.feature.shipping.CarrierGateway
import com.piashcse.feature.shipping.CarrierLabelRequest
import com.piashcse.feature.shipping.CarrierLabelResult
import com.piashcse.feature.shipping.CarrierQuoteRequest
import com.piashcse.feature.shipping.CarrierRate
import com.piashcse.feature.shipping.CarrierWebhookEvent
import com.piashcse.feature.shipping.PurchaseShipmentRequest
import com.piashcse.feature.shipping.ShipmentAddressRequest
import com.piashcse.feature.shipping.ShipmentItemRequest
import com.piashcse.feature.shipping.ShipmentParcelRequest
import com.piashcse.feature.shipping.ShipmentQuoteRequest
import com.piashcse.feature.shipping.ShipmentService
import com.piashcse.feature.shipping.ShippoCarrierGateway
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.Assume
import org.testcontainers.containers.PostgreSQLContainer
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Partial fulfillment, label purchase and tracking webhooks against PostgreSQL. */
class ShipmentIntegrationTest {
    private fun envDb(): Triple<String, String, String>? {
        val host = System.getenv("DB_HOST")?.takeIf { it.isNotBlank() } ?: return null
        val port = System.getenv("DB_PORT") ?: "5432"
        val name = System.getenv("DB_NAME") ?: "ktor-ecommerce"
        val user = System.getenv("DB_USER") ?: "postgres"
        val password = System.getenv("DB_PASSWORD") ?: "postgres"
        return Triple("jdbc:postgresql://$host:$port/$name", user, password)
    }

    private fun dockerAvailable(): Boolean =
        runCatching {
            ProcessBuilder("docker", "info").redirectErrorStream(true).start().waitFor() == 0
        }.getOrDefault(false)

    @Test
    fun `partial shipments and tracking webhooks update the order`() {
        val external = envDb()
        if (external != null) {
            runShipmentFlow(external.first, external.second, external.third)
            return
        }
        if (System.getenv("CI") != null) {
            assertTrue(dockerAvailable(), "DB_* environment or Docker is required in CI")
        } else {
            Assume.assumeTrue("Docker daemon or DB_* environment required", dockerAvailable())
        }
        val pg = PostgreSQLContainer("postgres:16-alpine")
        pg.start()
        try {
            runShipmentFlow(pg.jdbcUrl, pg.username, pg.password)
        } finally {
            pg.stop()
        }
    }

    private class FakeCarrier : CarrierGateway {
        override val provider: String = "SHIPPO"

        override suspend fun quote(request: CarrierQuoteRequest): List<CarrierRate> =
            listOf(
                CarrierRate(
                    id = "rate-1",
                    carrier = "USPS",
                    service = "Priority",
                    amount = BigDecimal("7.50"),
                ),
            )

        override suspend fun purchaseLabel(request: CarrierLabelRequest): CarrierLabelResult =
            CarrierLabelResult(
                success = true,
                carrier = "USPS",
                service = "Priority",
                trackingNumber = "TRACK-${request.rateId}",
                labelUrl = "https://labels.example/${request.rateId}.pdf",
                providerReference = "txn-${request.rateId}",
                amount = BigDecimal("7.50"),
            )

        override fun verifyWebhook(
            payload: String,
            signature: String?,
        ): Boolean = signature == "sig"

        override fun parseWebhook(payload: String): CarrierWebhookEvent {
            val root = Json.parseToJsonElement(payload).jsonObject
            val tracking = root["tracking"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val status = root["status"]?.jsonPrimitive?.contentOrNull.orEmpty()
            return CarrierWebhookEvent(
                eventId = ShippoCarrierGateway.sha256(payload),
                eventType = "track_updated",
                trackingNumber = tracking,
                status = runCatching { ShipmentStatus.valueOf(status) }.getOrNull(),
            )
        }
    }

    private fun runShipmentFlow(
        jdbcUrl: String,
        user: String,
        dbPassword: String,
    ) {
        Flyway.configure()
            .dataSource(jdbcUrl, user, dbPassword)
            .locations("classpath:db/migration")
            .load()
            .migrate()
        Database.connect(jdbcUrl, user = user, password = dbPassword)

        val suffix = UUID.randomUUID().toString().take(8)
        lateinit var customerId: String
        lateinit var sellerUserId: String
        lateinit var orderId: String
        lateinit var itemA: String
        lateinit var itemB: String

        transaction {
            val customer =
                UserDAO.new {
                    email = "ship-customer-$suffix@example.com"
                    userType = UserType.CUSTOMER
                    password = "hashed"
                    isVerified = true
                }
            val sellerUser =
                UserDAO.new {
                    email = "ship-seller-$suffix@example.com"
                    userType = UserType.SELLER
                    password = "hashed"
                    isVerified = true
                }
            customerId = customer.id.value
            sellerUserId = sellerUser.id.value
            val shopCategory = ShopCategoryDAO.new { name = "Ship $suffix" }
            val shop =
                ShopDAO.new {
                    userId = sellerUser.id
                    categoryId = shopCategory.id
                    name = "Ship Shop $suffix"
                    status = ShopStatus.APPROVED
                }
            SellerDAO.new {
                userId = sellerUser.id
                shopId = shop.id
                status = ShopStatus.APPROVED
                kycStatus = KycStatus.NOT_SUBMITTED
            }
            val category =
                ProductCategoryDAO.find { com.piashcse.database.entities.ProductCategoryTable.name eq "Electronics" }
                    .firstOrNull()
                    ?: ProductCategoryDAO.new { name = "Electronics" }
            val product =
                ProductDAO.new {
                    userId = sellerUser.id
                    shopId = shop.id
                    categoryId = category.id
                    name = "Ship Product $suffix"
                    description = "Shipment integration product"
                    sku = "SHIP-$suffix"
                    price = BigDecimal("10.00")
                }
            val order =
                OrderDAO.new {
                    userId = customer.id
                    shopId = shop.id
                    orderNumber = "SHIP-$suffix"
                    subTotal = BigDecimal("30.00")
                    total = BigDecimal("30.00")
                    status = OrderStatus.PAID
                    paymentStatus = PaymentStatus.COMPLETED
                }
            orderId = order.id.value
            itemA =
                OrderItemDAO.new {
                    this.orderId = order.id
                    productId = product.id
                    shopId = shop.id
                    quantity = 2
                    price = BigDecimal("10.00")
                    total = BigDecimal("20.00")
                    productName = product.name
                }.id.value
            itemB =
                OrderItemDAO.new {
                    this.orderId = order.id
                    productId = product.id
                    shopId = shop.id
                    quantity = 1
                    price = BigDecimal("10.00")
                    total = BigDecimal("10.00")
                    productName = product.name
                }.id.value
        }

        val service = ShipmentService(FakeCarrier())

        val quotes =
            kotlinx.coroutines.runBlocking {
                service.quote(
                    orderId,
                    customerId,
                    ShipmentQuoteRequest(
                        origin =
                            ShipmentAddressRequest("Shop", "1 Main", "Dhaka", zipCode = "1207", country = "BD"),
                        destination =
                            ShipmentAddressRequest("Customer", "2 Side", "Chattogram", zipCode = "4000", country = "BD"),
                        parcels =
                            listOf(
                                ShipmentParcelRequest(BigDecimal("10"), BigDecimal("8"), BigDecimal("4"), weight = BigDecimal("2")),
                            ),
                    ),
                )
            }
        assertEquals("rate-1", quotes.single().id)

        assertFailsWith<ForbiddenException> {
            kotlinx.coroutines.runBlocking {
                service.purchaseLabel(orderId, customerId, PurchaseShipmentRequest("rate-1"))
            }
        }

        // First parcel: one of the two units of item A.
        val first =
            kotlinx.coroutines.runBlocking {
                service.purchaseLabel(
                    orderId,
                    sellerUserId,
                    PurchaseShipmentRequest("rate-1", listOf(ShipmentItemRequest(itemA, 1))),
                )
            }
        assertEquals(ShipmentStatus.LABEL_PURCHASED, first.status)
        assertEquals("TRACK-rate-1", first.trackingNumber)
        transaction { assertEquals(OrderStatus.PAID, OrderDAO.findById(orderId)!!.status) }

        // Second parcel completes the allocation and ships the order.
        val second =
            kotlinx.coroutines.runBlocking {
                service.purchaseLabel(
                    orderId,
                    sellerUserId,
                    PurchaseShipmentRequest(
                        "rate-2",
                        listOf(ShipmentItemRequest(itemA, 1), ShipmentItemRequest(itemB, 1)),
                    ),
                )
            }
        assertEquals("TRACK-rate-2", second.trackingNumber)
        transaction {
            val order = OrderDAO.findById(orderId)!!
            assertEquals(OrderStatus.SHIPPED, order.status)
            assertEquals("TRACK-rate-2", order.trackingNumber)
            assertEquals(2, ShipmentDAO.find { com.piashcse.database.entities.ShipmentTable.orderId eq order.id }.count().toInt())
        }

        // Overshipping is rejected.
        assertFailsWith<ValidationException> {
            kotlinx.coroutines.runBlocking {
                service.purchaseLabel(orderId, sellerUserId, PurchaseShipmentRequest("rate-3"))
            }
        }

        // First parcel delivered: order remains SHIPPED.
        val deliveredFirst =
            kotlinx.coroutines.runBlocking {
                service.handleWebhook("shippo", """{"tracking":"TRACK-rate-1","status":"DELIVERED"}""", "sig")
            }
        assertTrue(deliveredFirst.accepted)
        transaction { assertEquals(OrderStatus.SHIPPED, OrderDAO.findById(orderId)!!.status) }

        // Second parcel delivered: order becomes DELIVERED.
        kotlinx.coroutines.runBlocking {
            service.handleWebhook("shippo", """{"tracking":"TRACK-rate-2","status":"DELIVERED"}""", "sig")
        }
        transaction {
            val order = OrderDAO.findById(orderId)!!
            assertEquals(OrderStatus.DELIVERED, order.status)
        }

        val duplicate =
            kotlinx.coroutines.runBlocking {
                service.handleWebhook("shippo", """{"tracking":"TRACK-rate-2","status":"DELIVERED"}""", "sig")
            }
        assertTrue(duplicate.duplicate)

        assertFailsWith<ValidationException> {
            kotlinx.coroutines.runBlocking {
                service.handleWebhook("shippo", """{"tracking":"TRACK-rate-2","status":"DELIVERED"}""", "bad")
            }
        }

        assertEquals(2, kotlinx.coroutines.runBlocking { service.list(orderId, customerId) }.size)
    }
}
