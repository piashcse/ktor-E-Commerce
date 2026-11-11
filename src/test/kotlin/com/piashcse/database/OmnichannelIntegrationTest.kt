package com.piashcse.database

import com.piashcse.constants.FulfillmentType
import com.piashcse.constants.InventoryStatus
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentMethod
import com.piashcse.constants.PaymentStatus
import com.piashcse.constants.PickupStatus
import com.piashcse.constants.ProductStatus
import com.piashcse.constants.ShopStatus
import com.piashcse.constants.UserType
import com.piashcse.database.entities.InventoryDAO
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderItemDAO
import com.piashcse.database.entities.ProductCategoryDAO
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.SellerDAO
import com.piashcse.database.entities.ShopCategoryDAO
import com.piashcse.database.entities.ShopDAO
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.WarehouseDAO
import com.piashcse.feature.omnichannel.OmnichannelService
import com.piashcse.feature.omnichannel.PosSaleItem
import com.piashcse.feature.omnichannel.PosSaleRequest
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.Assume
import org.testcontainers.containers.PostgreSQLContainer
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Store locator, BOPIS and POS flows against PostgreSQL. */
class OmnichannelIntegrationTest {
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
    fun `pickup and POS sales work end to end`() {
        val external = envDb()
        if (external != null) {
            runOmnichannelFlow(external.first, external.second, external.third)
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
            runOmnichannelFlow(pg.jdbcUrl, pg.username, pg.password)
        } finally {
            pg.stop()
        }
    }

    private fun runOmnichannelFlow(
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
        lateinit var sellerUserId: String
        lateinit var customerId: String
        lateinit var shopId: String
        lateinit var warehouseId: String
        lateinit var productId: String
        lateinit var pickupOrderId: String

        transaction {
            val sellerUser =
                UserDAO.new {
                    email = "omni-seller-$suffix@example.com"
                    userType = UserType.SELLER
                    password = "hashed"
                    isVerified = true
                }
            val customer =
                UserDAO.new {
                    email = "omni-customer-$suffix@example.com"
                    userType = UserType.CUSTOMER
                    password = "hashed"
                    isVerified = true
                }
            sellerUserId = sellerUser.id.value
            customerId = customer.id.value
            val shopCategory = ShopCategoryDAO.new { name = "Omni $suffix" }
            val shop =
                ShopDAO.new {
                    userId = sellerUser.id
                    categoryId = shopCategory.id
                    name = "Omni Shop $suffix"
                    status = ShopStatus.APPROVED
                }
            shopId = shop.id.value
            SellerDAO.new {
                userId = sellerUser.id
                this.shopId = shop.id
                status = ShopStatus.APPROVED
            }
            val warehouse =
                WarehouseDAO.new {
                    this.shopId = shop.id
                    name = "Dhaka Store $suffix"
                    code = "DHAKA-$suffix"
                    address = "12 Main St"
                    city = "Dhaka"
                    country = "BD"
                    latitude = BigDecimal("23.810300")
                    longitude = BigDecimal("90.412500")
                    phone = "+8801700000000"
                    openingHours = "09:00-21:00"
                    supportsPickup = true
                    supportsPos = true
                }
            warehouseId = warehouse.id.value
            val category = ProductCategoryDAO.new { name = "Omni Cat $suffix" }
            val product =
                ProductDAO.new {
                    userId = sellerUser.id
                    this.shopId = shop.id
                    this.categoryId = category.id
                    name = "Omni Product $suffix"
                    description = "Omnichannel product"
                    sku = "OMNI-$suffix"
                    price = BigDecimal("10.00")
                    status = ProductStatus.ACTIVE
                }
            productId = product.id.value
            InventoryDAO.new {
                this.productId = product.id
                this.shopId = shop.id
                this.warehouseId = warehouse.id
                stockQuantity = 10
                status = InventoryStatus.IN_STOCK
            }
            val pickup =
                OrderDAO.new {
                    userId = customer.id
                    this.shopId = shop.id
                    orderNumber = "PICK-$suffix"
                    subTotal = BigDecimal("10.00")
                    total = BigDecimal("10.00")
                    status = OrderStatus.PAID
                    paymentStatus = PaymentStatus.COMPLETED
                    fulfillmentType = FulfillmentType.PICKUP
                    pickupWarehouseId = warehouse.id
                    pickupCode = "ABC123"
                    pickupStatus = PickupStatus.PENDING
                    shippingMethod = "PICKUP"
                }
            pickupOrderId = pickup.id.value
            OrderItemDAO.new {
                this.orderId = pickup.id
                this.productId = product.id
                this.shopId = shop.id
                quantity = 1
                price = BigDecimal("10.00")
                total = BigDecimal("10.00")
                productName = product.name
            }
        }

        val service = OmnichannelService()
        val stores = kotlinx.coroutines.runBlocking { service.stores(23.80, 90.40, 5.0, 10) }
        assertTrue(stores.any { it.id == warehouseId && it.distanceKm != null && it.distanceKm!! < 5.0 })

        val ready = kotlinx.coroutines.runBlocking { service.markReady(sellerUserId, pickupOrderId) }
        assertEquals(PickupStatus.READY, ready.pickupStatus)
        val completed = kotlinx.coroutines.runBlocking { service.completePickup(sellerUserId, pickupOrderId, "abc123") }
        assertEquals(PickupStatus.PICKED_UP, completed.pickupStatus)
        assertEquals(OrderStatus.RECEIVED, completed.status)
        assertTrue(completed.pickedUpAt != null)

        val pos =
            kotlinx.coroutines.runBlocking {
                service.posSale(
                    sellerUserId,
                    PosSaleRequest(
                        items = listOf(PosSaleItem(productId, 2)),
                        warehouseId = warehouseId,
                        paymentMethod = PaymentMethod.COD,
                    ),
                )
            }
        assertEquals(FulfillmentType.POS, pos.fulfillmentType)
        assertEquals(OrderStatus.PAID, pos.status)
        assertEquals("21.00", pos.total)
        assertEquals(1, kotlinx.coroutines.runBlocking { service.posSales(sellerUserId, 10, 0) }.data.size)
        assertEquals(8, transaction { InventoryDAO.all().first { it.productId.value == productId }.stockQuantity })
        assertEquals(1, kotlinx.coroutines.runBlocking { service.pickupOrders(sellerUserId, 10, 0) }.data.size)
    }
}
