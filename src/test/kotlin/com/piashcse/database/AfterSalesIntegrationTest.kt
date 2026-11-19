@file:Suppress("LongMethod")

package com.piashcse.database

import com.piashcse.constants.KycStatus
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentMethod
import com.piashcse.constants.PayoutStatus
import com.piashcse.constants.RefundMethod
import com.piashcse.constants.RefundStatus
import com.piashcse.constants.ShopStatus
import com.piashcse.constants.UserType
import com.piashcse.database.entities.InventoryDAO
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderItemDAO
import com.piashcse.database.entities.ProductCategoryDAO
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.ProductTable
import com.piashcse.database.entities.RefundRequestDAO
import com.piashcse.database.entities.SellerDAO
import com.piashcse.database.entities.SellerPayoutDAO
import com.piashcse.database.entities.ShopCategoryDAO
import com.piashcse.database.entities.ShopDAO
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.WarehouseDAO
import com.piashcse.feature.cart.CartRepositoryImpl
import com.piashcse.feature.order.OrderRepositoryImpl
import com.piashcse.feature.payment.ManualPaymentGateway
import com.piashcse.feature.payment.PaymentRepositoryImpl
import com.piashcse.feature.payout.PayoutRepositoryImpl
import com.piashcse.feature.refund_request.RefundRequestRepositoryImpl
import com.piashcse.feature.shipping_address.ShippingAddressRepositoryImpl
import com.piashcse.feature.shipping_method.ShippingMethodRepositoryImpl
import com.piashcse.model.request.CheckoutRequest
import com.piashcse.model.request.PaymentRequest
import com.piashcse.model.request.RefundRequestRequest
import com.piashcse.model.request.ShipRefundRequest
import com.piashcse.model.request.ShippingAddressRequest
import com.piashcse.model.request.UpdateRefundStatusRequest
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.validator.ConflictException
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Refund and payout lifecycle against a real PostgreSQL database. */
class AfterSalesIntegrationTest {
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
    fun `refund and payout lifecycle`() {
        val external = envDb()
        if (external != null) {
            runChecks(external.first, external.second, external.third)
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
            runChecks(pg.jdbcUrl, pg.username, pg.password)
        } finally {
            pg.stop()
        }
    }

    private fun runChecks(
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
        lateinit var sellerId: String
        lateinit var shopId: String
        lateinit var productId: String
        lateinit var adminId: String

        transaction {
            val customer =
                UserDAO.new {
                    email = "after-customer-$suffix@example.com"
                    userType = UserType.CUSTOMER
                    password = "hashed"
                    isVerified = true
                }
            val sellerUser =
                UserDAO.new {
                    email = "after-seller-$suffix@example.com"
                    userType = UserType.SELLER
                    password = "hashed"
                    isVerified = true
                }
            val admin =
                UserDAO.new {
                    email = "after-admin-$suffix@example.com"
                    userType = UserType.ADMIN
                    password = "hashed"
                    isVerified = true
                }
            val shopCategory = ShopCategoryDAO.new { name = "After Sales $suffix" }
            val shop =
                ShopDAO.new {
                    userId = sellerUser.id
                    categoryId = shopCategory.id
                    name = "After Sales Shop $suffix"
                    status = ShopStatus.APPROVED
                }
            val seller =
                SellerDAO.new {
                    userId = sellerUser.id
                    this.shopId = shop.id
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
                    this.shopId = shop.id
                    categoryId = category.id
                    name = "After Sales Product $suffix"
                    description = "After-sales integration product"
                    sku = "AFT-$suffix"
                    price = BigDecimal("50.00")
                }
            val warehouse =
                WarehouseDAO.new {
                    this.shopId = shop.id
                    name = "After WH $suffix"
                    code = "AFT-$suffix"
                }
            InventoryDAO.new {
                this.productId = product.id
                this.shopId = shop.id
                this.warehouseId = warehouse.id
                stockQuantity = 10
            }
            customerId = customer.id.value
            sellerUserId = sellerUser.id.value
            sellerId = seller.id.value
            shopId = shop.id.value
            productId = product.id.value
            adminId = admin.id.value
        }

        val addressId =
            kotlinx.coroutines.runBlocking {
                ShippingAddressRepositoryImpl().createShippingAddress(
                    customerId,
                    ShippingAddressRequest("After", "Customer", "after@example.com", "123", "12 Main St", "Dhaka", null, "BD", "1207"),
                ).id
            }
        val methodId = kotlinx.coroutines.runBlocking { ShippingMethodRepositoryImpl().getShippingMethods(10, 0) }.data.first().id
        val cart = CartRepositoryImpl()
        val orders = OrderRepositoryImpl()
        val payments = PaymentRepositoryImpl(ManualPaymentGateway())

        kotlinx.coroutines.runBlocking { cart.createCart(customerId, productId, 1) }
        val checkout =
            CheckoutRequest(
                shippingAddressId = addressId,
                shippingMethodId = methodId,
                paymentMethod = PaymentMethod.COD,
                idempotencyKey = "after-$suffix",
            )
        val order = kotlinx.coroutines.runBlocking { orders.placeOrder(customerId, checkout) }.first()
        kotlinx.coroutines.runBlocking { orders.updateOrderStatus(sellerUserId, order.orderId, OrderStatus.CONFIRMED) }
        val payment =
            kotlinx.coroutines.runBlocking {
                payments.createPayment(
                    PaymentRequest(order.orderId, BigDecimal(order.total), PaymentMethod.COD, idempotencyKey = "after-pay-$suffix"),
                    customerId,
                )
            }
        kotlinx.coroutines.runBlocking { payments.confirmPayment(payment.id, adminId) }
        kotlinx.coroutines.runBlocking { orders.shipOrder(order.orderId, sellerUserId, "AFT-TRACK-$suffix") }
        kotlinx.coroutines.runBlocking { orders.updateOrderStatus(sellerUserId, order.orderId, OrderStatus.DELIVERED) }

        val itemId = assertNotNull(order.items?.firstOrNull()).id
        val refunds = RefundRequestRepositoryImpl()
        val refund =
            kotlinx.coroutines.runBlocking {
                refunds.createRefundRequest(customerId, order.orderId, RefundRequestRequest(itemId, "damaged", null, 1))
            }
        assertEquals(RefundStatus.PENDING, refund.status)
        assertEquals(
            1,
            kotlinx.coroutines.runBlocking { refunds.getRefundsByOrderId(order.orderId, customerId, UserType.CUSTOMER) }.data.size,
        )
        assertTrue(
            kotlinx.coroutines.runBlocking { refunds.getRefundsByOrderId(order.orderId, sellerUserId, UserType.SELLER) }
                .data.isNotEmpty(),
        )
        assertNotNull(kotlinx.coroutines.runBlocking { refunds.getRefundById(refund.id, customerId, UserType.CUSTOMER) })
        val evidenced =
            kotlinx.coroutines.runBlocking {
                refunds.addRefundEvidence(refund.id, customerId, "/uploads/refund-images/evidence-$suffix.png")
            }
        assertTrue(evidenced.images.orEmpty().contains("evidence-$suffix.png"))
        assertTrue(
            kotlinx.coroutines.runBlocking { refunds.listForSeller(sellerUserId, RefundStatus.PENDING) }
                .data.any { it.id == refund.id },
        )
        assertTrue(
            kotlinx.coroutines.runBlocking { refunds.listAll(RefundStatus.PENDING) }
                .data.any { it.id == refund.id },
        )

        val amount = BigDecimal(refund.refundAmount ?: order.items!!.first().total)
        assertFailsWith<ForbiddenException> {
            kotlinx.coroutines.runBlocking {
                refunds.updateRefundStatus(
                    refund.id,
                    UpdateRefundStatusRequest(RefundStatus.REFUNDED, refundAmount = amount, refundMethod = RefundMethod.ORIGINAL),
                    sellerUserId,
                )
            }
        }
        assertFailsWith<ConflictException> {
            kotlinx.coroutines.runBlocking {
                refunds.updateRefundStatus(
                    refund.id,
                    UpdateRefundStatusRequest(RefundStatus.REFUNDED, refundAmount = amount, refundMethod = RefundMethod.BANK_TRANSFER),
                    adminId,
                )
            }
        }
        assertFailsWith<ValidationException> {
            kotlinx.coroutines.runBlocking {
                refunds.updateRefundStatus(
                    refund.id,
                    UpdateRefundStatusRequest(
                        RefundStatus.APPROVED,
                        refundAmount = amount.add(BigDecimal("100.00")),
                        refundMethod = RefundMethod.ORIGINAL,
                    ),
                    sellerUserId,
                )
            }
        }
        val approved =
            kotlinx.coroutines.runBlocking {
                refunds.updateRefundStatus(
                    refund.id,
                    UpdateRefundStatusRequest(RefundStatus.APPROVED, refundAmount = amount, refundMethod = RefundMethod.ORIGINAL),
                    sellerUserId,
                )
            }
        assertEquals(RefundStatus.APPROVED, approved.status)
        assertEquals(
            RefundStatus.SHIPPED,
            kotlinx.coroutines.runBlocking { refunds.shipRefund(refund.id, ShipRefundRequest("RETURN-$suffix"), customerId) }.status,
        )
        val refunded =
            kotlinx.coroutines.runBlocking {
                refunds.updateRefundStatus(
                    refund.id,
                    UpdateRefundStatusRequest(RefundStatus.REFUNDED, refundAmount = amount, refundMethod = RefundMethod.BANK_TRANSFER),
                    adminId,
                )
            }
        assertEquals(RefundStatus.REFUNDED, refunded.status)

        // Provider-initiated refund reconciliation applies local effects idempotently.
        val providerRefundId = "prov-$suffix"
        transaction {
            val source = OrderDAO.findById(order.orderId)!!
            val reconcileOrder =
                OrderDAO.new {
                    userId = source.userId
                    this.shopId = source.shopId
                    orderNumber = "RECON-$suffix"
                    subTotal = BigDecimal("10.00")
                    total = BigDecimal("10.00")
                    status = OrderStatus.DELIVERED
                    paymentStatus = com.piashcse.constants.PaymentStatus.COMPLETED
                }
            val item =
                OrderItemDAO.new {
                    orderId = reconcileOrder.id
                    this.productId = productId.entityID(ProductTable)
                    this.shopId = source.shopId!!
                    quantity = 1
                    price = BigDecimal("10.00")
                    total = BigDecimal("10.00")
                    productName = "Provider refund item"
                }
            RefundRequestDAO.new {
                orderItemId = item.id
                userId = source.userId
                orderId = reconcileOrder.id
                reason = "Provider refund"
                currency = "USD"
                quantity = 1
                status = RefundStatus.APPROVED
                refundAmount = BigDecimal("10.00")
                refundMethod = RefundMethod.ORIGINAL
                this.providerRefundId = providerRefundId
            }
        }
        assertTrue(kotlinx.coroutines.runBlocking { refunds.reconcileProviderRefund(providerRefundId, "succeeded", BigDecimal("10.00")) })
        assertEquals(
            RefundStatus.REFUNDED,
            transaction {
                RefundRequestDAO.find { com.piashcse.database.entities.RefundRequestTable.providerRefundId eq providerRefundId }
                    .single().status
            },
        )
        assertFalse(kotlinx.coroutines.runBlocking { refunds.reconcileProviderRefund("missing-$suffix", "succeeded", null) })

        // Payout ledger row is created atomically by payment completion now, and
        // the refund above must have reversed it.
        lateinit var payoutId: String
        transaction {
            val original = SellerPayoutDAO.all().first { it.orderId.value == order.orderId }
            assertEquals("REFUNDED", original.status, "A refunded order must reverse the seller payout")

            // Create a separate paid order/payout for the payout lifecycle checks.
            val payoutOrder =
                OrderDAO.new {
                    userId = customerId.entityID(com.piashcse.database.entities.UserTable)
                    orderNumber = "PAYOUT-$suffix"
                    subTotal = BigDecimal("100.00")
                    total = BigDecimal("100.00")
                    status = com.piashcse.constants.OrderStatus.PAID
                    paymentStatus = com.piashcse.constants.PaymentStatus.COMPLETED
                }
            val payout =
                SellerPayoutDAO.new {
                    this.sellerId = sellerId.entityID(com.piashcse.database.entities.SellerTable)
                    this.orderId = payoutOrder.id
                    subTotal = BigDecimal("100.00")
                    commissionAmount = BigDecimal("10.00")
                    payoutAmount = BigDecimal("90.00")
                    currency = "USD"
                    status = "PENDING"
                }
            payoutId = payout.id.value
        }

        val payouts = PayoutRepositoryImpl()
        val balance = kotlinx.coroutines.runBlocking { payouts.availableBalance(sellerUserId) }
        assertTrue(BigDecimal(balance.available) >= BigDecimal("90.00"))
        val requested =
            kotlinx.coroutines.runBlocking {
                payouts.requestPayout(sellerUserId, BigDecimal(balance.available), "payout-$suffix")
            }
        assertTrue(requested.isNotEmpty())
        val requestId = requested.first().payoutRequestId
        assertNotNull(requestId)
        assertTrue(
            kotlinx.coroutines.runBlocking {
                payouts.sellerPayoutRequests(sellerUserId, PayoutStatus.REQUESTED, 10, 0)
            }.data.any { it.id == requestId },
        )
        assertNotNull(kotlinx.coroutines.runBlocking { payouts.sellerPayoutRequestDetail(sellerUserId, requestId) })
        assertNotNull(kotlinx.coroutines.runBlocking { payouts.adminPayoutRequestDetail(requestId) })
        assertTrue(
            kotlinx.coroutines.runBlocking { payouts.allPayoutRequests(null, null, 10, 0) }
                .data.any { it.id == requestId },
        )
        val approvedPayout = kotlinx.coroutines.runBlocking { payouts.approvePayout(requested.first().id) }
        assertEquals("APPROVED", approvedPayout.status)
        val paid = kotlinx.coroutines.runBlocking { payouts.markPaid(approvedPayout.id, adminId) }
        assertEquals("PAID", paid.status)
        assertNotNull(kotlinx.coroutines.runBlocking { payouts.statement(sellerUserId, null, null, null) })
        assertTrue(
            kotlinx.coroutines.runBlocking { payouts.sellerPayouts(sellerUserId, 10, 0, PayoutStatus.PAID) }
                .data.isNotEmpty(),
        )
        assertTrue(
            kotlinx.coroutines.runBlocking { payouts.allPayouts(10, 0, PayoutStatus.PAID) }
                .data.isNotEmpty(),
        )

        // A second ledger row exercises the rejection path without disturbing
        // the paid request above.
        lateinit var rejectPayoutId: String
        transaction {
            val rejectOrder =
                OrderDAO.new {
                    userId = customerId.entityID(com.piashcse.database.entities.UserTable)
                    orderNumber = "PAYOUT-REJECT-$suffix"
                    subTotal = BigDecimal("20.00")
                    total = BigDecimal("20.00")
                    status = OrderStatus.PAID
                    paymentStatus = com.piashcse.constants.PaymentStatus.COMPLETED
                }
            rejectPayoutId =
                SellerPayoutDAO.new {
                    this.sellerId = sellerId.entityID(com.piashcse.database.entities.SellerTable)
                    this.orderId = rejectOrder.id
                    subTotal = BigDecimal("20.00")
                    commissionAmount = BigDecimal("2.00")
                    payoutAmount = BigDecimal("18.00")
                    currency = "USD"
                    status = "PENDING"
                }.id.value
        }
        val rejectRequest =
            kotlinx.coroutines.runBlocking {
                payouts.requestPayout(sellerUserId, BigDecimal("18.00"), "payout-reject-$suffix")
            }
        assertTrue(rejectRequest.isNotEmpty())
        val rejected = kotlinx.coroutines.runBlocking { payouts.rejectPayout(rejectPayoutId, "Compliance hold") }
        assertEquals("PENDING", rejected.status)
        assertEquals("Compliance hold", rejected.rejectionReason)

        // Multi-currency balances/statements must not mix currencies.
        transaction {
            val eurOrder =
                OrderDAO.new {
                    userId = customerId.entityID(com.piashcse.database.entities.UserTable)
                    orderNumber = "PAYOUT-EUR-$suffix"
                    subTotal = BigDecimal("30.00")
                    total = BigDecimal("30.00")
                    currency = "EUR"
                    fxRate = BigDecimal("0.92")
                    status = OrderStatus.PAID
                    paymentStatus = com.piashcse.constants.PaymentStatus.COMPLETED
                }
            SellerPayoutDAO.new {
                this.sellerId = sellerId.entityID(com.piashcse.database.entities.SellerTable)
                this.orderId = eurOrder.id
                subTotal = BigDecimal("30.00")
                commissionAmount = BigDecimal("3.00")
                payoutAmount = BigDecimal("27.00")
                this.currency = "EUR"
                status = "PENDING"
            }
        }
        val balances = kotlinx.coroutines.runBlocking { payouts.availableBalance(sellerUserId) }
        assertEquals("18.00", balances.balances["USD"])
        assertEquals("27.00", balances.balances["EUR"])
        val eurStatement = kotlinx.coroutines.runBlocking { payouts.statement(sellerUserId, null, null, "EUR") }
        assertEquals("EUR", eurStatement.currency)
        assertEquals("27.00", eurStatement.netPayout)
        assertTrue(payoutId.isNotBlank())
    }
}
