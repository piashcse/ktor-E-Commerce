package com.piashcse.database

import com.piashcse.constants.KycStatus
import com.piashcse.constants.ShopStatus
import com.piashcse.constants.UserType
import com.piashcse.database.entities.InventoryDAO
import com.piashcse.database.entities.ProductCategoryDAO
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.SellerDAO
import com.piashcse.database.entities.ShopCategoryDAO
import com.piashcse.database.entities.ShopDAO
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.WarehouseDAO
import com.piashcse.feature.apikey.ApiKeyCreateRequest
import com.piashcse.feature.apikey.ApiKeyService
import com.piashcse.feature.audit_log.AuditLogRepositoryImpl
import com.piashcse.feature.featureflag.FeatureFlagUpdateRequest
import com.piashcse.feature.featureflag.FeatureFlags
import com.piashcse.feature.kyc.KycService
import com.piashcse.feature.kyc.SellerKycSubmitRequest
import com.piashcse.feature.notification.NotificationPreferencesService
import com.piashcse.feature.outbox.OutboxService
import com.piashcse.feature.shipping_method.ShippingMethodRepositoryImpl
import com.piashcse.feature.tax.TaxRuleRequest
import com.piashcse.feature.tax.TaxService
import com.piashcse.feature.warehouse.WarehouseRepository
import com.piashcse.feature.warehouse.WarehouseRequest
import com.piashcse.model.request.ShippingMethodRequest
import com.piashcse.service.FxService
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Service/repository behaviour against a real PostgreSQL database.
 *
 * Prefers the CI `DB_*` service and falls back to Testcontainers locally.
 */
class ServiceIntegrationTest {
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
    fun `services persist and read back`() {
        val external = envDb()
        if (external != null) {
            runServiceChecks(external.first, external.second, external.third)
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
            runServiceChecks(pg.jdbcUrl, pg.username, pg.password)
        } finally {
            pg.stop()
        }
    }

    private fun runServiceChecks(
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
        lateinit var sellerId: String
        lateinit var sellerUserId: String
        lateinit var productId: String

        transaction {
            val customer =
                UserDAO.new {
                    email = "services-customer-$suffix@example.com"
                    userType = UserType.CUSTOMER
                    password = "hashed"
                    isVerified = true
                }
            val sellerUser =
                UserDAO.new {
                    email = "services-seller-$suffix@example.com"
                    userType = UserType.SELLER
                    password = "hashed"
                    isVerified = true
                }
            val shopCategory = ShopCategoryDAO.new { name = "Services $suffix" }
            val shop =
                ShopDAO.new {
                    userId = sellerUser.id
                    categoryId = shopCategory.id
                    name = "Services Shop $suffix"
                    status = ShopStatus.APPROVED
                }
            val seller =
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
                    name = "Service Product $suffix"
                    description = "Integration service product"
                    sku = "SVC-$suffix"
                    price = BigDecimal("19.99")
                }
            val warehouse =
                WarehouseDAO.new {
                    this.shopId = shop.id
                    name = "Service WH $suffix"
                    code = "SVC-$suffix"
                }
            InventoryDAO.new {
                this.productId = product.id
                this.shopId = shop.id
                this.warehouseId = warehouse.id
                stockQuantity = 10
            }
            customerId = customer.id.value
            sellerId = seller.id.value
            sellerUserId = sellerUser.id.value
            productId = product.id.value
        }

        // Tax rules
        val tax = TaxService()
        val taxRule = kotlinx.coroutines.runBlocking { tax.upsert(TaxRuleRequest("BD", null, BigDecimal("0.05"), true)) }
        assertEquals("0.05", taxRule.rate)
        assertEquals(0, transaction { tax.resolve("BD", null) }.compareTo(BigDecimal("0.05")))
        assertTrue(kotlinx.coroutines.runBlocking { tax.list() }.any { it.id == taxRule.id })
        assertTrue(kotlinx.coroutines.runBlocking { tax.delete(taxRule.id) })

        // FX rates
        val fx = FxService()
        kotlinx.coroutines.runBlocking { fx.upsertRate("EUR", BigDecimal("0.92")) }
        assertEquals(0, kotlinx.coroutines.runBlocking { fx.rateTo("EUR") }.compareTo(BigDecimal("0.92")))
        assertEquals(
            0,
            kotlinx.coroutines.runBlocking { fx.convertUsd(BigDecimal("10.00"), "EUR") }
                .compareTo(BigDecimal("9.20")),
        )
        assertTrue(kotlinx.coroutines.runBlocking { fx.listRates() }.any { it.targetCurrency == "EUR" })

        // Feature flags
        val flags = FeatureFlags()
        val flagKey = "service_flag_$suffix"
        kotlinx.coroutines.runBlocking { flags.set(flagKey, FeatureFlagUpdateRequest(true, "integration")) }
        assertTrue(kotlinx.coroutines.runBlocking { flags.isEnabled(flagKey) })
        assertTrue(kotlinx.coroutines.runBlocking { flags.list() }.any { it.key == flagKey })

        // Outbox summary/list (tolerates rows produced by other tests)
        val outbox = OutboxService()
        assertTrue(kotlinx.coroutines.runBlocking { outbox.summary() }.pending >= 0)
        assertTrue(kotlinx.coroutines.runBlocking { outbox.listDeadLetters(5, 0) }.data.size <= 5)

        // Shipping methods
        val shipping = ShippingMethodRepositoryImpl()
        val method =
            kotlinx.coroutines.runBlocking {
                shipping.createShippingMethod(ShippingMethodRequest("Service Express $suffix", "EXPRESS", BigDecimal("9.99"), "2 days"))
            }
        assertEquals("Service Express $suffix", method.name)
        val updated =
            kotlinx.coroutines.runBlocking {
                shipping.updateShippingMethod(
                    method.id,
                    ShippingMethodRequest("Service Express 2 $suffix", "EXPRESS", BigDecimal("12.99"), "3 days"),
                )
            }
        assertEquals("12.99", updated.price)
        assertTrue(kotlinx.coroutines.runBlocking { shipping.getShippingMethods(20, 0) }.data.any { it.id == method.id })
        assertTrue(kotlinx.coroutines.runBlocking { shipping.deleteShippingMethod(method.id) })

        // Notification preferences
        val prefs = NotificationPreferencesService()
        val saved = kotlinx.coroutines.runBlocking { prefs.set(customerId, "ORDER_PLACED", "EMAIL") }
        assertEquals("EMAIL", saved.channel)
        assertEquals("EMAIL", kotlinx.coroutines.runBlocking { prefs.get(customerId, "ORDER_PLACED") }.channel)
        assertTrue(kotlinx.coroutines.runBlocking { prefs.list(customerId) }.overrides.isNotEmpty())

        // Audit log
        val audit = AuditLogRepositoryImpl()
        kotlinx.coroutines.runBlocking {
            audit.log(customerId, "services@example.com", "ADMIN", "SERVICE_TEST", "USER", customerId, "integration")
        }
        val logs =
            kotlinx.coroutines.runBlocking {
                audit.getAuditLogs(10, 0, null, "SERVICE_TEST", null, null, null)
            }
        assertTrue(logs.data.isNotEmpty())
        assertNotNull(kotlinx.coroutines.runBlocking { audit.getAuditLogById(logs.data.first().id) })

        // Warehouses
        val warehouses = WarehouseRepository()
        assertTrue(kotlinx.coroutines.runBlocking { warehouses.list(sellerUserId) }.isNotEmpty())
        val warehouse =
            kotlinx.coroutines.runBlocking {
                warehouses.create(
                    sellerUserId,
                    WarehouseRequest("Service DC $suffix", "DC-$suffix"),
                )
            }
        assertEquals("Service DC $suffix", warehouse.name)
        val renamed =
            kotlinx.coroutines.runBlocking {
                warehouses.update(sellerUserId, warehouse.id, WarehouseRequest("Service DC 2 $suffix", "DC2-$suffix"))
            }
        assertEquals("Service DC 2 $suffix", renamed.name)
        assertTrue(kotlinx.coroutines.runBlocking { warehouses.deactivate(sellerUserId, warehouse.id) })

        // KYC
        val kyc = KycService()
        kotlinx.coroutines.runBlocking { kyc.submit(sellerUserId, SellerKycSubmitRequest(businessName = "Service Business")) }
        assertEquals(KycStatus.PENDING, kotlinx.coroutines.runBlocking { kyc.get(sellerUserId) }.kycStatus)
        assertEquals(KycStatus.VERIFIED, kotlinx.coroutines.runBlocking { kyc.verify(sellerId) }.kycStatus)

        // API keys
        val apiKeys = ApiKeyService()
        val created = kotlinx.coroutines.runBlocking { apiKeys.create(customerId, ApiKeyCreateRequest("service-key-$suffix")) }
        assertNotNull(kotlinx.coroutines.runBlocking { apiKeys.authenticate(created.key) })
        assertTrue(kotlinx.coroutines.runBlocking { apiKeys.list(customerId) }.any { it.id == created.id })
        assertTrue(kotlinx.coroutines.runBlocking { apiKeys.revoke(customerId, created.id) })
        assertNull(kotlinx.coroutines.runBlocking { apiKeys.authenticate(created.key) })

        // Seed sanity: the product created for this test still exists.
        transaction { assertNotNull(ProductDAO.findById(productId)) }
    }
}
