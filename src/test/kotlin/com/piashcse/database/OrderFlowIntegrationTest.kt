package com.piashcse.database

import com.piashcse.constants.KycStatus
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentMethod
import com.piashcse.constants.PaymentStatus
import com.piashcse.constants.ShopStatus
import com.piashcse.constants.UserType
import com.piashcse.database.entities.InventoryDAO
import com.piashcse.database.entities.OutboxDAO
import com.piashcse.database.entities.OutboxTable
import com.piashcse.database.entities.ProductCategoryDAO
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.SellerDAO
import com.piashcse.database.entities.ShopCategoryDAO
import com.piashcse.database.entities.ShopDAO
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.WarehouseDAO
import com.piashcse.feature.cart.CartRepositoryImpl
import com.piashcse.feature.order.OrderRepositoryImpl
import com.piashcse.feature.payment.ManualPaymentGateway
import com.piashcse.feature.payment.PaymentRepositoryImpl
import com.piashcse.feature.shipping_address.ShippingAddressRepositoryImpl
import com.piashcse.feature.shipping_method.ShippingMethodRepositoryImpl
import com.piashcse.model.request.CheckoutRequest
import com.piashcse.model.request.PaymentRequest
import com.piashcse.model.request.ShippingAddressRequest
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** End-to-end order + payment state machine against a real PostgreSQL database. */
class OrderFlowIntegrationTest {
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
    fun `order payment shipping and cancellation flow`() {
        val external = envDb()
        if (external != null) {
            runFlow(external.first, external.second, external.third)
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
            runFlow(pg.jdbcUrl, pg.username, pg.password)
        } finally {
            pg.stop()
        }
    }

    private fun runFlow(
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
        lateinit var productId: String
        lateinit var addressId: String
        lateinit var methodId: String

        transaction {
            val customer =
                UserDAO.new {
                    email = "order-customer-$suffix@example.com"
                    userType = UserType.CUSTOMER
                    password = "hashed"
                    isVerified = true
                }
            val sellerUser =
                UserDAO.new {
                    email = "order-seller-$suffix@example.com"
                    userType = UserType.SELLER
                    password = "hashed"
                    isVerified = true
                }
            val shopCategory = ShopCategoryDAO.new { name = "Order Flow $suffix" }
            val shop =
                ShopDAO.new {
                    userId = sellerUser.id
                    categoryId = shopCategory.id
                    name = "Order Flow Shop $suffix"
                    status = ShopStatus.APPROVED
                }
            SellerDAO.new {
                userId = sellerUser.id
                shopId = shop.id
                status = ShopStatus.APPROVED
                kycStatus = KycStatus.VERIFIED
                commissionRate = BigDecimal("10.00")
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
                    name = "Order Flow Product $suffix"
                    description = "Integration order product"
                    sku = "ORD-$suffix"
                    price = BigDecimal("25.00")
                }
            val warehouse =
                WarehouseDAO.new {
                    this.shopId = shop.id
                    name = "Order WH $suffix"
                    code = "ORD-$suffix"
                }
            InventoryDAO.new {
                this.productId = product.id
                this.shopId = shop.id
                this.warehouseId = warehouse.id
                stockQuantity = 10
            }
            customerId = customer.id.value
            sellerUserId = sellerUser.id.value
            productId = product.id.value
        }

        val addresses = ShippingAddressRepositoryImpl()
        addressId =
            kotlinx.coroutines.runBlocking {
                addresses.createShippingAddress(
                    customerId,
                    ShippingAddressRequest("Order", "Customer", "order@example.com", "123", "12 Main St", "Dhaka", null, "BD", "1207"),
                ).id
            }
        methodId = kotlinx.coroutines.runBlocking { ShippingMethodRepositoryImpl().getShippingMethods(10, 0) }.data.first().id

        val cart = CartRepositoryImpl()
        val orders = OrderRepositoryImpl()
        val payments = PaymentRepositoryImpl(ManualPaymentGateway())

        kotlinx.coroutines.runBlocking { cart.createCart(customerId, productId, 1) }
        val checkout =
            CheckoutRequest(
                shippingAddressId = addressId,
                shippingMethodId = methodId,
                paymentMethod = PaymentMethod.COD,
                idempotencyKey = "order-flow-$suffix",
            )
        assertNotNull(kotlinx.coroutines.runBlocking { orders.getCheckoutSummary(customerId, checkout) })
        val order = kotlinx.coroutines.runBlocking { orders.placeOrder(customerId, checkout) }.first()
        assertEquals(OrderStatus.PENDING, order.status)
        assertTrue(kotlinx.coroutines.runBlocking { orders.getOrders(customerId, 10, 0) }.data.isNotEmpty())
        assertTrue(kotlinx.coroutines.runBlocking { orders.getSellerOrders(sellerUserId, 10, 0, null) }.data.isNotEmpty())
        assertTrue(kotlinx.coroutines.runBlocking { orders.getAdminOrders(10, 0, null, null, null) }.data.isNotEmpty())

        val confirmed = kotlinx.coroutines.runBlocking { orders.updateOrderStatus(sellerUserId, order.orderId, OrderStatus.CONFIRMED) }
        assertEquals(OrderStatus.CONFIRMED, confirmed.status)

        val payment =
            kotlinx.coroutines.runBlocking {
                payments.createPayment(
                    PaymentRequest(order.orderId, BigDecimal(order.total), PaymentMethod.COD, idempotencyKey = "pay-$suffix"),
                    customerId,
                )
            }
        assertEquals(PaymentStatus.PENDING, payment.status)
        assertEquals(PaymentStatus.COMPLETED, kotlinx.coroutines.runBlocking { payments.confirmPayment(payment.id, sellerUserId) }.status)
        assertNotNull(kotlinx.coroutines.runBlocking { payments.getPaymentById(payment.id, customerId) })
        assertTrue(kotlinx.coroutines.runBlocking { payments.getPaymentsByOrderId(order.orderId, customerId) }.data.isNotEmpty())

        val shipped = kotlinx.coroutines.runBlocking { orders.shipOrder(order.orderId, sellerUserId, "TRACK-$suffix") }
        assertEquals(OrderStatus.SHIPPED, shipped.status)
        assertEquals("TRACK-$suffix", shipped.trackingNumber)
        val delivered = kotlinx.coroutines.runBlocking { orders.updateOrderStatus(sellerUserId, order.orderId, OrderStatus.DELIVERED) }
        assertEquals(OrderStatus.DELIVERED, delivered.status)

        assertTrue(
            transaction { OutboxDAO.find { OutboxTable.eventType eq "ORDER_STATUS_CHANGED" }.count() } >= 2,
            "Shipping and delivery must enqueue order-status notification events",
        )

        // Second order is canceled and must not keep stock.
        kotlinx.coroutines.runBlocking { cart.createCart(customerId, productId, 1) }
        val second =
            kotlinx.coroutines.runBlocking {
                orders.placeOrder(customerId, checkout.copy(idempotencyKey = "order-flow-cancel-$suffix"))
            }.first()
        val canceled = kotlinx.coroutines.runBlocking { orders.cancelOrder(second.orderId, customerId, "changed mind", UserType.CUSTOMER) }
        assertEquals(OrderStatus.CANCELED, canceled.status)
        assertTrue(
            transaction { OutboxDAO.find { OutboxTable.eventType eq "ORDER_STATUS_CHANGED" }.count() } >= 3,
            "Cancellation must enqueue an order-status notification event",
        )

        val stock = transaction { InventoryDAO.all().first { it.productId.value == productId }.stockQuantity }
        assertEquals(9, stock, "Only the delivered order should consume stock")
    }
}
