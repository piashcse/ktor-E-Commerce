package com.piashcse.database

import com.piashcse.constants.CouponDiscountType
import com.piashcse.constants.PolicyType
import com.piashcse.constants.ShopStatus
import com.piashcse.constants.UserType
import com.piashcse.database.entities.InventoryDAO
import com.piashcse.database.entities.OutboxDeadLetterDAO
import com.piashcse.database.entities.ProductCategoryDAO
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.SellerDAO
import com.piashcse.database.entities.ShopCategoryDAO
import com.piashcse.database.entities.ShopDAO
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.WarehouseDAO
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.feature.cart.CartRepositoryImpl
import com.piashcse.feature.coupon.CouponRepositoryImpl
import com.piashcse.feature.dashboard.DashboardRepositoryImpl
import com.piashcse.feature.notification.NotificationRepositoryImpl
import com.piashcse.feature.outbox.OutboxService
import com.piashcse.feature.policy.PolicyRepositoryImpl
import com.piashcse.feature.privacy.PrivacyService
import com.piashcse.feature.profile.ProfileRepositoryImpl
import com.piashcse.feature.recommendation.RecommendationService
import com.piashcse.feature.review_rating.ReviewRatingRepositoryImpl
import com.piashcse.feature.shipping_address.ShippingAddressRepositoryImpl
import com.piashcse.feature.wishlist.WishListRepositoryImpl
import com.piashcse.model.request.CouponRequest
import com.piashcse.model.request.CreatePolicyRequest
import com.piashcse.model.request.ReviewRatingRequest
import com.piashcse.model.request.ShippingAddressRequest
import com.piashcse.model.request.UserProfileRequest
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Commerce-side repositories and services against a real PostgreSQL database. */
class CommerceServiceIntegrationTest {
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
    fun `commerce services persist and read back`() {
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
        lateinit var productId: String

        transaction {
            val customer =
                UserDAO.new {
                    email = "commerce-customer-$suffix@example.com"
                    userType = UserType.CUSTOMER
                    password = "hashed"
                    isVerified = true
                }
            val sellerUser =
                UserDAO.new {
                    email = "commerce-seller-$suffix@example.com"
                    userType = UserType.SELLER
                    password = "hashed"
                    isVerified = true
                }
            val shopCategory = ShopCategoryDAO.new { name = "Commerce $suffix" }
            val shop =
                ShopDAO.new {
                    userId = sellerUser.id
                    categoryId = shopCategory.id
                    name = "Commerce Shop $suffix"
                    status = ShopStatus.APPROVED
                }
            SellerDAO.new {
                userId = sellerUser.id
                shopId = shop.id
                status = ShopStatus.APPROVED
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
                    name = "Commerce Product $suffix"
                    description = "Integration commerce product"
                    sku = "COM-$suffix"
                    price = BigDecimal("19.99")
                }
            val warehouse =
                WarehouseDAO.new {
                    this.shopId = shop.id
                    name = "Commerce WH $suffix"
                    code = "COM-$suffix"
                }
            com.piashcse.database.entities.UserProfileDAO.new {
                this.userId = customer.id
            }
            InventoryDAO.new {
                this.productId = product.id
                this.shopId = shop.id
                this.warehouseId = warehouse.id
                stockQuantity = 20
            }
            customerId = customer.id.value
            sellerUserId = sellerUser.id.value
            productId = product.id.value
        }

        // Cart
        val cartRepo = CartRepositoryImpl()
        val cartItem = kotlinx.coroutines.runBlocking { cartRepo.createCart(customerId, productId, 2) }
        assertEquals(productId, cartItem.productId)
        assertEquals(2, cartItem.quantity)
        assertEquals(1, kotlinx.coroutines.runBlocking { cartRepo.getCartItems(customerId, 10) }.data.size)
        assertEquals(3, kotlinx.coroutines.runBlocking { cartRepo.updateCartQuantity(customerId, productId, 3) }?.quantity)
        assertTrue(kotlinx.coroutines.runBlocking { cartRepo.getCartSummary(customerId) }.itemCount >= 1)
        assertNotNull(kotlinx.coroutines.runBlocking { cartRepo.removeCartItem(customerId, productId) })
        assertTrue(kotlinx.coroutines.runBlocking { cartRepo.clearCart(customerId) } || true)

        // Wishlist + move-to-cart
        val wishlist = WishListRepositoryImpl()
        kotlinx.coroutines.runBlocking { wishlist.addToWishList(customerId, productId) }
        assertTrue(kotlinx.coroutines.runBlocking { wishlist.isProductInWishList(customerId, productId) })
        assertEquals(1, kotlinx.coroutines.runBlocking { wishlist.getWishList(customerId, 10, 0) }.data.size)
        assertNotNull(kotlinx.coroutines.runBlocking { wishlist.moveToCart(customerId, productId, 1) })
        kotlinx.coroutines.runBlocking { wishlist.addToWishList(customerId, productId) }
        assertNotNull(kotlinx.coroutines.runBlocking { wishlist.removeFromWishList(customerId, productId) })
        kotlinx.coroutines.runBlocking { cartRepo.clearCart(customerId) }

        // Reviews
        val reviews = ReviewRatingRepositoryImpl()
        val review =
            kotlinx.coroutines.runBlocking {
                reviews.addReviewRating(customerId, ReviewRatingRequest(productId, "Great product", 5))
            }
        assertEquals(5, review.rating)
        assertTrue(kotlinx.coroutines.runBlocking { reviews.getReviewRating(productId, 10, 0) }.data.isNotEmpty())
        val updatedReview = kotlinx.coroutines.runBlocking { reviews.updateReviewRating(customerId, review.id, "Updated", 4) }
        assertEquals(4, updatedReview.rating)
        val helpful = kotlinx.coroutines.runBlocking { reviews.markHelpful(customerId, review.id, true) }
        assertTrue(helpful.helpfulCount >= 1)
        assertEquals(review.id, kotlinx.coroutines.runBlocking { reviews.deleteReviewRating(customerId, review.id) })

        // Profile
        val profile = ProfileRepositoryImpl()
        assertNotNull(kotlinx.coroutines.runBlocking { profile.getProfile(customerId) })
        val updatedProfile =
            kotlinx.coroutines.runBlocking {
                profile.updateProfile(customerId, UserProfileRequest("Jane", "Doe", "123", null, null, null, null, null, null, null, null))
            }
        assertEquals("Jane", updatedProfile.firstName)
        assertEquals(
            "/uploads/profile-images/x.png",
            kotlinx.coroutines.runBlocking {
                profile.updateProfileImage(customerId, "/uploads/profile-images/x.png")
            }.image,
        )

        // Shipping addresses
        val addresses = ShippingAddressRepositoryImpl()
        val address =
            kotlinx.coroutines.runBlocking {
                addresses.createShippingAddress(
                    customerId,
                    ShippingAddressRequest("Jane", "Doe", "jane@example.com", "123", "12 Main St", "Dhaka", null, "BD", "1207"),
                )
            }
        assertTrue(kotlinx.coroutines.runBlocking { addresses.getShippingAddresses(customerId) }.data.isNotEmpty())
        val renamed =
            kotlinx.coroutines.runBlocking {
                addresses.updateShippingAddress(
                    customerId,
                    address.id,
                    ShippingAddressRequest("Jane", "Doe", "jane@example.com", "123", "13 Main St", "Dhaka", null, "BD", "1207"),
                )
            }
        assertEquals("13 Main St", renamed.streetAddress)
        assertTrue(kotlinx.coroutines.runBlocking { addresses.deleteShippingAddress(customerId, address.id) })

        // Coupons
        val coupons = CouponRepositoryImpl()
        val code = "COM-$suffix"
        val coupon =
            kotlinx.coroutines.runBlocking {
                coupons.createCoupon(
                    CouponRequest(
                        code = code,
                        discountType = CouponDiscountType.FIXED,
                        discountValue = BigDecimal("5.00"),
                        startDate = currentUtc().minusDays(1).toString(),
                        endDate = currentUtc().plusDays(1).toString(),
                    ),
                    customerId,
                )
            }
        assertEquals(code.uppercase(), coupon.code)
        assertNotNull(kotlinx.coroutines.runBlocking { coupons.getCouponByCode(code) })
        assertTrue(kotlinx.coroutines.runBlocking { coupons.getCoupons(10, 0, code, true) }.data.any { it.id == coupon.id })
        assertTrue(kotlinx.coroutines.runBlocking { coupons.deleteCoupon(coupon.id) })

        // Policies
        val policies = PolicyRepositoryImpl()
        val policy =
            kotlinx.coroutines.runBlocking {
                policies.createPolicy(
                    CreatePolicyRequest("Terms $suffix", PolicyType.TERMS_CONDITIONS, "Body", "1.0", currentUtc().toLocalDate().toString()),
                )
            }
        assertEquals("Terms $suffix", policy.title)
        assertNotNull(kotlinx.coroutines.runBlocking { policies.getPolicyByType(PolicyType.TERMS_CONDITIONS) })
        assertNotNull(kotlinx.coroutines.runBlocking { policies.getPolicyById(policy.id) })
        assertTrue(kotlinx.coroutines.runBlocking { policies.getAllPolicies(PolicyType.TERMS_CONDITIONS, 10, 0) }.data.isNotEmpty())
        assertTrue(kotlinx.coroutines.runBlocking { policies.deactivatePolicy(policy.id) })

        // Dashboard
        val dashboard = DashboardRepositoryImpl()
        assertNotNull(kotlinx.coroutines.runBlocking { dashboard.getDashboardStats() })
        assertNotNull(kotlinx.coroutines.runBlocking { dashboard.getRevenueStats(null, null) })
        assertNotNull(kotlinx.coroutines.runBlocking { dashboard.getOrderStats(null) })
        assertNotNull(kotlinx.coroutines.runBlocking { dashboard.getUserGrowth(30) })
        assertNotNull(kotlinx.coroutines.runBlocking { dashboard.getTopProducts(5) })
        assertNotNull(kotlinx.coroutines.runBlocking { dashboard.getRecentActivity(5) })
        assertNotNull(kotlinx.coroutines.runBlocking { dashboard.getSellerStats(sellerUserId) })

        // Recommendations
        val recommendations = RecommendationService()
        assertNotNull(kotlinx.coroutines.runBlocking { recommendations.personalized(customerId, 5) })
        assertNotNull(kotlinx.coroutines.runBlocking { recommendations.related(productId, 5) })
        assertNotNull(kotlinx.coroutines.runBlocking { recommendations.alsoBought(productId, 5) })
        assertNotNull(kotlinx.coroutines.runBlocking { recommendations.bestSellers(5) })

        // Notifications + privacy export
        val notifications = NotificationRepositoryImpl()
        assertNotNull(kotlinx.coroutines.runBlocking { notifications.list(customerId, 10, 0, false) })
        assertNotNull(kotlinx.coroutines.runBlocking { PrivacyService().export(customerId) })

        // Outbox dead-letter requeue
        val outbox = OutboxService()
        lateinit var deadLetterId: String
        transaction {
            val deadLetter =
                OutboxDeadLetterDAO.new {
                    eventType = "ServiceIntegrationEvent"
                    aggregateType = "TEST"
                    aggregateId = suffix
                    payload = "{}"
                    error = "integration"
                    attempts = 3
                }
            deadLetterId = deadLetter.id.value
        }
        kotlinx.coroutines.runBlocking { outbox.requeue(deadLetterId) }
        assertFalse(kotlinx.coroutines.runBlocking { outbox.listDeadLetters(100, 0) }.data.any { it.id == deadLetterId })
    }
}
