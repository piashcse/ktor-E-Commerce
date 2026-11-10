@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.plugin

import com.piashcse.constants.AppConstants
import com.piashcse.feature.apikey.apiKeyRoutes
import com.piashcse.feature.audit_log.auditLogAdminRoutes
import com.piashcse.feature.auth.authAdminRoutes
import com.piashcse.feature.auth.authRoutes
import com.piashcse.feature.brand.brandAdminRoutes
import com.piashcse.feature.brand.brandRoutes
import com.piashcse.feature.cart.cartRoutes
import com.piashcse.feature.checkout.checkoutRoutes
import com.piashcse.feature.consent.consentAdminRoutes
import com.piashcse.feature.consent.consentRoutes
import com.piashcse.feature.coupon.couponAdminRoutes
import com.piashcse.feature.coupon.couponRoutes
import com.piashcse.feature.dashboard.dashboardAdminRoutes
import com.piashcse.feature.dashboard.dashboardSellerRoutes
import com.piashcse.feature.featureflag.featureFlagRoutes
import com.piashcse.feature.fx.fxAdminRoutes
import com.piashcse.feature.inventory.inventoryAdminRoutes
import com.piashcse.feature.inventory.inventorySellerRoutes
import com.piashcse.feature.kyc.adminKycRoutes
import com.piashcse.feature.kyc.sellerKycRoutes
import com.piashcse.feature.notification.notificationRoutes
import com.piashcse.feature.order.orderAdminRoutes
import com.piashcse.feature.order.orderRoutes
import com.piashcse.feature.order.orderSellerRoutes
import com.piashcse.feature.outbox.outboxAdminRoutes
import com.piashcse.feature.payment.paymentAdminRoutes
import com.piashcse.feature.payment.paymentRoutes
import com.piashcse.feature.payout.payoutAdminRoutes
import com.piashcse.feature.payout.payoutSellerRoutes
import com.piashcse.feature.policy.policyAdminRoutes
import com.piashcse.feature.policy.policyRoutes
import com.piashcse.feature.privacy.privacyRoutes
import com.piashcse.feature.product.productAdminRoutes
import com.piashcse.feature.product.productRoutes
import com.piashcse.feature.product.productSellerRoutes
import com.piashcse.feature.product_category.productCategoryAdminRoutes
import com.piashcse.feature.product_category.productCategoryRoutes
import com.piashcse.feature.product_sub_category.productSubCategoryAdminRoutes
import com.piashcse.feature.product_sub_category.productSubCategoryRoutes
import com.piashcse.feature.profile.profileRoutes
import com.piashcse.feature.recommendation.recommendationRoutes
import com.piashcse.feature.refund_request.refundAdminRoutes
import com.piashcse.feature.refund_request.refundRequestRoutes
import com.piashcse.feature.refund_request.refundSellerRoutes
import com.piashcse.feature.review_rating.reviewRatingRoutes
import com.piashcse.feature.rma.rmaAdminRoutes
import com.piashcse.feature.rma.rmaRoutes
import com.piashcse.feature.rma.rmaSellerRoutes
import com.piashcse.feature.shipping_method.shippingMethodAdminRoutes
import com.piashcse.feature.shop.shopAdminRoutes
import com.piashcse.feature.shop.shopRoutes
import com.piashcse.feature.shop.shopSellerRoutesV1
import com.piashcse.feature.shop_category.shopCategoryAdminRoutes
import com.piashcse.feature.shop_category.shopCategoryRoutes
import com.piashcse.feature.tax.taxAdminRoutes
import com.piashcse.feature.warehouse.warehouseSellerRoutes
import com.piashcse.feature.wishlist.wishListRoutes
import com.piashcse.utils.common.ApiError
import com.piashcse.utils.common.MessageResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import io.ktor.server.routing.openapi.describe
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/** Minimal liveness payload — service/infra detail stays out of the public contract. */
@Serializable
private data class HealthStatusResponse(val status: String, val version: String)

@OptIn(ExperimentalKtorApi::class)
fun Application.configureRoute() {
    routing {
        get("/") { call.respondRedirect("/swagger") }.hide()
        get("/health") {
            call.respond(
                HealthStatusResponse(status = "UP", version = AppConstants.APP_VERSION),
            )
        }.describe {
            summary = "Service health"
            description = "Version and overall status"
            tag(ApiTags.HEALTH)
        }
        get("/health/live") {
            call.respond(
                MessageResponse("UP"),
            )
        }.describe {
            summary = "Liveness probe"
            tag(ApiTags.HEALTH)
        }
        get("/health/ready") {
            val dbOk =
                runCatching {
                    withTimeout(3_000) {
                        withContext(Dispatchers.IO) {
                            transaction {
                                exec("SELECT 1")
                            }
                        }
                    }
                }.isSuccess
            if (dbOk) {
                call.respond(MessageResponse("READY"))
            } else {
                call.respond(
                    HttpStatusCode.ServiceUnavailable,
                    ApiError(
                        message = "Database unreachable",
                        code = "SERVICE_UNAVAILABLE",
                        requestId = runCatching { call.requestId() }.getOrNull(),
                    ),
                )
            }
        }.describe {
            summary = "Readiness probe"
            description = "Checks database connectivity"
            tag(ApiTags.HEALTH)
        }

        route("/api") {
            route("v1") {
                customerRoutes()
                sellerRoutes()
                adminRoutes()
            }
        }
    }
}

private fun Route.customerRoutes() {
    route("auth") { authRoutes() }.describe { tag(ApiTags.AUTH) }
    route("profile") { profileRoutes() }.describe { tag(ApiTags.PROFILE) }
    route("shops") { shopRoutes() }.describe { tag(ApiTags.SHOP) }
    route("brands") { brandRoutes() }.describe { tag(ApiTags.BRAND) }
    route("product-categories") { productCategoryRoutes() }.describe { tag(ApiTags.PRODUCT_CATEGORY) }
    route("product-subcategories") { productSubCategoryRoutes() }.describe { tag(ApiTags.PRODUCT_SUB_CATEGORY) }
    route("shop-categories") { shopCategoryRoutes() }.describe { tag(ApiTags.SHOP_CATEGORY) }
    route("products") { productRoutes() }.describe { tag(ApiTags.PRODUCT) }
    route("reviews") { reviewRatingRoutes() }.describe { tag(ApiTags.REVIEW) }
    route("carts") { cartRoutes() }.describe { tag(ApiTags.CART) }
    route("wishlists") { wishListRoutes() }.describe { tag(ApiTags.WISHLIST) }
    route("checkout") { checkoutRoutes() }.describe { tag(ApiTags.CHECKOUT) }
    route("orders") { orderRoutes() }.describe { tag(ApiTags.ORDER) }
    route("payments") { paymentRoutes() }.describe { tag(ApiTags.PAYMENT) }
    route("policies") { policyRoutes() }.describe { tag(ApiTags.POLICY) }
    route("policy-consents") { consentRoutes() }.describe { tag(ApiTags.CONSENT) }
    route("refund-requests") { refundRequestRoutes() }.describe { tag(ApiTags.REFUND) }
    route("rma-requests") { rmaRoutes() }.describe { tag(ApiTags.RMA) }
    route("recommendations") { recommendationRoutes() }.describe { tag(ApiTags.RECOMMENDATION) }
    route("coupons") { couponRoutes() }.describe { tag(ApiTags.COUPON) }
    route("notifications") { notificationRoutes() }.describe { tag(ApiTags.NOTIFICATION) }
    route("privacy") { privacyRoutes() }.describe { tag(ApiTags.PRIVACY) }
    route("api-keys") { apiKeyRoutes() }.describe { tag(ApiTags.API_KEYS) }
}

private fun Route.sellerRoutes() {
    route("seller") {
        sellerAuth {
            route("shops") { shopSellerRoutesV1() }.describe { tag(ApiTags.SHOP) }
            route("products") { productSellerRoutes() }.describe { tag(ApiTags.PRODUCT) }
            route("inventories") { inventorySellerRoutes() }.describe { tag(ApiTags.INVENTORY) }
            route("warehouses") { warehouseSellerRoutes() }.describe { tag(ApiTags.WAREHOUSE) }
            route("kyc") { sellerKycRoutes() }.describe { tag(ApiTags.KYC) }
            route("orders") { orderSellerRoutes() }.describe { tag(ApiTags.ORDER) }
            route("refund-requests") { refundSellerRoutes() }.describe { tag(ApiTags.REFUND) }
            route("rma-requests") { rmaSellerRoutes() }.describe { tag(ApiTags.RMA) }
            route("dashboard") { dashboardSellerRoutes() }.describe { tag(ApiTags.DASHBOARD) }
            route("payouts") { payoutSellerRoutes() }.describe { tag(ApiTags.PAYOUT) }
        }
    }
}

private fun Route.adminRoutes() {
    route("admin") {
        adminAuth {
            route("auth") { authAdminRoutes() }.describe { tag(ApiTags.AUTH) }
            route("brands") { brandAdminRoutes() }.describe { tag(ApiTags.BRAND) }
            route("product-categories") { productCategoryAdminRoutes() }.describe { tag(ApiTags.PRODUCT_CATEGORY) }
            route("product-subcategories") { productSubCategoryAdminRoutes() }.describe { tag(ApiTags.PRODUCT_SUB_CATEGORY) }
            route("shop-categories") { shopCategoryAdminRoutes() }.describe { tag(ApiTags.SHOP_CATEGORY) }
            route("shops") { shopAdminRoutes() }.describe { tag(ApiTags.SHOP) }
            route("sellers") { adminKycRoutes() }.describe { tag(ApiTags.KYC) }
            route("products") { productAdminRoutes() }.describe { tag(ApiTags.PRODUCT) }
            route("inventories") { inventoryAdminRoutes() }.describe { tag(ApiTags.INVENTORY) }
            route("orders") { orderAdminRoutes() }.describe { tag(ApiTags.ORDER) }
            route("payments") { paymentAdminRoutes() }.describe { tag(ApiTags.PAYMENT) }
            route("refund-requests") { refundAdminRoutes() }.describe { tag(ApiTags.REFUND) }
            route("rma-requests") { rmaAdminRoutes() }.describe { tag(ApiTags.RMA) }
            route("policies") { policyAdminRoutes() }.describe { tag(ApiTags.POLICY) }
            route("policy-consents") { consentAdminRoutes() }.describe { tag(ApiTags.CONSENT) }
            route("shipping-methods") { shippingMethodAdminRoutes() }.describe { tag(ApiTags.SHIPPING_METHOD) }
            route("coupons") { couponAdminRoutes() }.describe { tag(ApiTags.COUPON) }
            route("dashboard") { dashboardAdminRoutes() }.describe { tag(ApiTags.DASHBOARD) }
            route("audit-logs") { auditLogAdminRoutes() }.describe { tag(ApiTags.AUDIT_LOG) }
            route("payouts") { payoutAdminRoutes() }.describe { tag(ApiTags.PAYOUT) }
            route("fx-rates") { fxAdminRoutes() }.describe { tag(ApiTags.FX) }
            route("tax-rules") { taxAdminRoutes() }.describe { tag(ApiTags.TAX) }
            route("feature-flags") { featureFlagRoutes() }.describe { tag(ApiTags.FEATURE_FLAGS) }
            route("outbox") { outboxAdminRoutes() }.describe { tag(ApiTags.OUTBOX) }
        }
    }
}
