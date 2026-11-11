package com.piashcse.database

import com.piashcse.constants.B2bAccountStatus
import com.piashcse.constants.B2bQuoteStatus
import com.piashcse.constants.InventoryStatus
import com.piashcse.constants.KycStatus
import com.piashcse.constants.PurchaseOrderStatus
import com.piashcse.constants.ShopStatus
import com.piashcse.constants.UserType
import com.piashcse.database.entities.InventoryDAO
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.ProductCategoryDAO
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.SellerDAO
import com.piashcse.database.entities.ShopCategoryDAO
import com.piashcse.database.entities.ShopDAO
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.WarehouseDAO
import com.piashcse.feature.b2b.B2bAccountRequest
import com.piashcse.feature.b2b.B2bAccountUpdateRequest
import com.piashcse.feature.b2b.B2bService
import com.piashcse.feature.b2b.QuoteItemRequest
import com.piashcse.feature.b2b.QuotePricedItem
import com.piashcse.feature.b2b.QuoteRequest
import com.piashcse.feature.b2b.QuoteRespondRequest
import com.piashcse.utils.validator.ValidationException
import org.flywaydb.core.Flyway
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

/** B2B accounts, quotes, purchase orders and credit terms against PostgreSQL. */
class B2bIntegrationTest {
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
    fun `quotes convert to purchase orders within the credit limit`() {
        val external = envDb()
        if (external != null) {
            runB2bFlow(external.first, external.second, external.third)
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
            runB2bFlow(pg.jdbcUrl, pg.username, pg.password)
        } finally {
            pg.stop()
        }
    }

    private fun runB2bFlow(
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
        lateinit var buyerId: String
        lateinit var sellerUserId: String
        lateinit var productA: String
        lateinit var productB: String

        transaction {
            val buyer =
                UserDAO.new {
                    email = "b2b-buyer-$suffix@example.com"
                    userType = UserType.CUSTOMER
                    password = "hashed"
                    isVerified = true
                }
            val sellerUser =
                UserDAO.new {
                    email = "b2b-seller-$suffix@example.com"
                    userType = UserType.SELLER
                    password = "hashed"
                    isVerified = true
                }
            buyerId = buyer.id.value
            sellerUserId = sellerUser.id.value
            val shopCategory = ShopCategoryDAO.new { name = "B2B $suffix" }
            val shop =
                ShopDAO.new {
                    userId = sellerUser.id
                    categoryId = shopCategory.id
                    name = "B2B Shop $suffix"
                    status = ShopStatus.APPROVED
                }
            SellerDAO.new {
                userId = sellerUser.id
                shopId = shop.id
                status = ShopStatus.APPROVED
                kycStatus = KycStatus.NOT_SUBMITTED
            }
            val warehouse =
                WarehouseDAO.new {
                    this.shopId = shop.id
                    name = "B2B Warehouse $suffix"
                    code = "B2B-$suffix"
                }
            val category = ProductCategoryDAO.new { name = "B2B Cat $suffix" }
            val productARow =
                ProductDAO.new {
                    userId = sellerUser.id
                    shopId = shop.id
                    categoryId = category.id
                    name = "Wholesale A $suffix"
                    description = "Wholesale product A"
                    sku = "B2B-A-$suffix"
                    price = BigDecimal("50.00")
                }
            productA = productARow.id.value
            val productBRow =
                ProductDAO.new {
                    userId = sellerUser.id
                    shopId = shop.id
                    categoryId = category.id
                    name = "Wholesale B $suffix"
                    description = "Wholesale product B"
                    sku = "B2B-B-$suffix"
                    price = BigDecimal("50.00")
                }
            productB = productBRow.id.value
            listOf(productARow, productBRow).forEach { product ->
                InventoryDAO.new {
                    productId = product.id
                    this.shopId = shop.id
                    this.warehouseId = warehouse.id
                    stockQuantity = 100
                    status = InventoryStatus.IN_STOCK
                }
            }
        }

        val b2b = B2bService()
        val applied =
            kotlinx.coroutines.runBlocking {
                b2b.apply(buyerId, B2bAccountRequest("Acme Wholesale $suffix", "TAX-$suffix"))
            }
        assertEquals(B2bAccountStatus.PENDING, applied.status)
        val approved =
            kotlinx.coroutines.runBlocking {
                b2b.updateAccount(
                    buyerId,
                    sellerUserId,
                    B2bAccountUpdateRequest(
                        status = B2bAccountStatus.APPROVED,
                        creditLimit = BigDecimal("500.00"),
                        paymentTermsDays = 30,
                    ),
                )
            }
        assertEquals(B2bAccountStatus.APPROVED, approved.status)

        val quote =
            kotlinx.coroutines.runBlocking {
                b2b.createQuote(
                    buyerId,
                    QuoteRequest(
                        items = listOf(QuoteItemRequest(productA, 2), QuoteItemRequest(productB, 3)),
                        notes = "Please quote wholesale pricing",
                    ),
                )
            }
        assertEquals(B2bQuoteStatus.REQUESTED, quote.status)
        assertEquals("250.00", quote.total)

        val sent =
            kotlinx.coroutines.runBlocking {
                b2b.respond(
                    sellerUserId,
                    quote.id,
                    QuoteRespondRequest(
                        status = B2bQuoteStatus.SENT,
                        items =
                            listOf(
                                QuotePricedItem(productA, 2, BigDecimal("45.00")),
                                QuotePricedItem(productB, 3, BigDecimal("40.00")),
                            ),
                    ),
                )
            }
        assertEquals(B2bQuoteStatus.SENT, sent.status)
        assertEquals("210.00", sent.total)

        val po = kotlinx.coroutines.runBlocking { b2b.accept(buyerId, quote.id) }
        assertEquals(PurchaseOrderStatus.OPEN, po.status)
        assertEquals("210.00", po.total)
        assertEquals(30, po.netTermsDays)
        assertTrue(po.dueDate != null)

        // A second quote that would exceed the remaining credit must be rejected.
        val bigQuote =
            kotlinx.coroutines.runBlocking {
                b2b.createQuote(buyerId, QuoteRequest(items = listOf(QuoteItemRequest(productA, 10))))
            }
        val bigSent =
            kotlinx.coroutines.runBlocking {
                b2b.respond(
                    sellerUserId,
                    bigQuote.id,
                    QuoteRespondRequest(status = B2bQuoteStatus.SENT),
                )
            }
        assertEquals("500.00", bigSent.total)
        assertFailsWith<ValidationException> {
            kotlinx.coroutines.runBlocking { b2b.accept(buyerId, bigQuote.id) }
        }

        val fulfilled = kotlinx.coroutines.runBlocking { b2b.fulfill(sellerUserId, po.id) }
        assertEquals(PurchaseOrderStatus.FULFILLED, fulfilled.status)
        assertTrue(fulfilled.orderId != null)
        transaction {
            val order = OrderDAO.findById(fulfilled.orderId!!)!!
            assertEquals(buyerId, order.userId.value)
            assertEquals("210.00", order.total.toPlainString())
        }
        assertEquals(1, kotlinx.coroutines.runBlocking { b2b.myPurchaseOrders(buyerId, 10, 0) }.data.size)
        assertEquals(1, kotlinx.coroutines.runBlocking { b2b.sellerPurchaseOrders(sellerUserId, 10, 0) }.data.size)
        assertEquals(1, kotlinx.coroutines.runBlocking { b2b.adminAccounts(null, 10, 0) }.data.size)
    }
}
