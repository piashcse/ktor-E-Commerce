@file:Suppress("LongMethod")

package com.piashcse.database

import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentMethod
import com.piashcse.constants.ShopStatus
import com.piashcse.constants.UserType
import com.piashcse.database.entities.InventoryDAO
import com.piashcse.database.entities.InventoryTable
import com.piashcse.database.entities.OrderTable
import com.piashcse.database.entities.ProductCategoryDAO
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.SellerDAO
import com.piashcse.database.entities.ShopCategoryDAO
import com.piashcse.database.entities.ShopDAO
import com.piashcse.database.entities.StockReservationDAO
import com.piashcse.database.entities.StockReservationTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.WarehouseDAO
import com.piashcse.feature.cart.CartRepositoryImpl
import com.piashcse.feature.loyalty.LoyaltyService
import com.piashcse.feature.order.OrderPlacementService
import com.piashcse.feature.order.OrderRepositoryImpl
import com.piashcse.feature.promotion.PromotionService
import com.piashcse.feature.shipping_address.ShippingAddressRepositoryImpl
import com.piashcse.feature.shipping_method.ShippingMethodRepositoryImpl
import com.piashcse.feature.tax.TaxService
import com.piashcse.model.request.CheckoutRequest
import com.piashcse.model.request.ShippingAddressRequest
import com.piashcse.service.DigitalDeliveryService
import com.piashcse.service.FxService
import com.piashcse.utils.extension.entityID
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Cancellation must restore stock to the warehouses that actually shipped it. */
class WarehouseAllocationIntegrationTest {
    @Test
    fun `cancellation restores each warehouse allocation`() {
        PostgresTestSupport.withDatabase { jdbcUrl, user, password ->
            runFlow(jdbcUrl, user, password)
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
        lateinit var productId: String
        lateinit var firstWarehouse: String
        lateinit var secondWarehouse: String

        transaction {
            val customer =
                UserDAO.new {
                    email = "alloc-customer-$suffix@example.com"
                    userType = UserType.CUSTOMER
                    password = "hashed"
                    isVerified = true
                }
            val sellerUser =
                UserDAO.new {
                    email = "alloc-seller-$suffix@example.com"
                    userType = UserType.SELLER
                    password = "hashed"
                    isVerified = true
                }
            val shopCategory = ShopCategoryDAO.new { name = "Alloc Shops $suffix" }
            val shop =
                ShopDAO.new {
                    userId = sellerUser.id
                    categoryId = shopCategory.id
                    name = "Alloc Shop $suffix"
                    status = ShopStatus.APPROVED
                }
            SellerDAO.new {
                userId = sellerUser.id
                shopId = shop.id
                status = ShopStatus.APPROVED
            }
            val category = ProductCategoryDAO.new { name = "Alloc Cat $suffix" }
            val product =
                ProductDAO.new {
                    userId = sellerUser.id
                    shopId = shop.id
                    categoryId = category.id
                    name = "Alloc Product $suffix"
                    description = "allocation test"
                    sku = "ALLOC-$suffix"
                    price = BigDecimal("10.00")
                }
            val first =
                WarehouseDAO.new {
                    this.shopId = shop.id
                    name = "First WH $suffix"
                    code = "FIRST-$suffix"
                }
            val second =
                WarehouseDAO.new {
                    this.shopId = shop.id
                    name = "Second WH $suffix"
                    code = "SECOND-$suffix"
                }
            InventoryDAO.new {
                this.productId = product.id
                this.shopId = shop.id
                this.warehouseId = first.id
                stockQuantity = 1
            }
            InventoryDAO.new {
                this.productId = product.id
                this.shopId = shop.id
                this.warehouseId = second.id
                stockQuantity = 1
            }
            customerId = customer.id.value
            productId = product.id.value
            firstWarehouse = first.id.value
            secondWarehouse = second.id.value
        }

        val address =
            kotlinx.coroutines.runBlocking {
                ShippingAddressRepositoryImpl().createShippingAddress(
                    customerId,
                    ShippingAddressRequest("Alloc", "Customer", "alloc@example.com", "123", "1 Main St", "Dhaka", null, "BD", "1207"),
                )
            }
        val methodId =
            kotlinx.coroutines.runBlocking { ShippingMethodRepositoryImpl().getShippingMethods(10, 0) }.data.first().id
        kotlinx.coroutines.runBlocking { CartRepositoryImpl().createCart(customerId, productId, 2) }
        val checkout =
            CheckoutRequest(
                shippingAddressId = address.id,
                shippingMethodId = methodId,
                paymentMethod = PaymentMethod.COD,
                idempotencyKey = "alloc-$suffix",
            )
        val order =
            kotlinx.coroutines.runBlocking {
                OrderRepositoryImpl(
                    OrderPlacementService(FxService(), TaxService(), PromotionService(), LoyaltyService(), DigitalDeliveryService()),
                ).placeOrder(customerId, checkout)
            }.first()

        transaction {
            val reservations =
                StockReservationDAO.find {
                    StockReservationTable.orderId eq order.orderId.entityID(OrderTable)
                }.toList()
            assertEquals(2, reservations.size, "Each warehouse take produces one reservation")
            assertEquals(1, reservations.count { it.warehouseId?.value == firstWarehouse })
            assertEquals(1, reservations.count { it.warehouseId?.value == secondWarehouse })
            assertEquals(0, InventoryDAO.all().filter { it.productId.value == productId }.sumOf { it.stockQuantity })
        }

        val canceled =
            kotlinx.coroutines.runBlocking {
                OrderRepositoryImpl(
                    OrderPlacementService(FxService(), TaxService(), PromotionService(), LoyaltyService(), DigitalDeliveryService()),
                ).cancelOrder(order.orderId, customerId, "allocation test", UserType.CUSTOMER)
            }
        assertEquals(OrderStatus.CANCELED, canceled.status)
        transaction {
            val byWarehouse =
                InventoryDAO.find { InventoryTable.productId eq productId.entityID(com.piashcse.database.entities.ProductTable) }
                    .associate { it.warehouseId.value to it.stockQuantity }
            assertEquals(1, byWarehouse[firstWarehouse])
            assertEquals(1, byWarehouse[secondWarehouse])
            assertTrue(
                StockReservationDAO.find {
                    StockReservationTable.orderId eq order.orderId.entityID(OrderTable)
                }.all { it.restoredQuantity == it.quantity },
            )
        }
    }
}
