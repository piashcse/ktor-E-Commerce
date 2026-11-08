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
        }.docs("Service health", "Version and overall status")
        get("/health/live") {
            call.respond(
                MessageResponse("UP"),
            )
        }.docs("Liveness probe")
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
        }.docs("Readiness probe", "Checks database connectivity")

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
    route("auth") { authRoutes() }.describe { tag("Auth") }
    route("profile") { profileRoutes() }.describe { tag("Profile") }
    route("shops") { shopRoutes() }.describe { tag("Shop") }
    route("brands") { brandRoutes() }.describe { tag("Brand") }
    route("product-categories") { productCategoryRoutes() }.describe { tag("Product Category") }
    route("product-subcategories") { productSubCategoryRoutes() }.describe { tag("Product Sub Category") }
    route("shop-categories") { shopCategoryRoutes() }.describe { tag("Shop Category") }
    route("products") { productRoutes() }.describe { tag("Product") }
    route("reviews") { reviewRatingRoutes() }.describe { tag("Review") }
    route("carts") { cartRoutes() }.describe { tag("Cart") }
    route("wishlists") { wishListRoutes() }.describe { tag("Wishlist") }
    route("checkout") { checkoutRoutes() }.describe { tag("Checkout") }
    route("orders") { orderRoutes() }.describe { tag("Order") }
    route("payments") { paymentRoutes() }.describe { tag("Payment") }
    route("policies") { policyRoutes() }.describe { tag("Policy") }
    route("policy-consents") { consentRoutes() }.describe { tag("Consent") }
    route("refund-requests") { refundRequestRoutes() }.describe { tag("Refund") }
    route("rma-requests") { rmaRoutes() }.describe { tag("RMA") }
    route("recommendations") { recommendationRoutes() }.describe { tag("Recommendation") }
    route("coupons") { couponRoutes() }.describe { tag("Coupon") }
    route("notifications") { notificationRoutes() }.describe { tag("Notification") }
    route("privacy") { privacyRoutes() }.describe { tag("Privacy") }
    route("api-keys") { apiKeyRoutes() }.describe { tag("API Keys") }
}

private fun Route.sellerRoutes() {
    route("seller") {
        sellerAuth {
            route("shops") { shopSellerRoutesV1() }.describe { tag("Shop") }
            route("products") { productSellerRoutes() }.describe { tag("Product") }
            route("inventories") { inventorySellerRoutes() }.describe { tag("Inventory") }
            route("warehouses") { warehouseSellerRoutes() }.describe { tag("Warehouse") }
            route("kyc") { sellerKycRoutes() }.describe { tag("KYC") }
            route("orders") { orderSellerRoutes() }.describe { tag("Order") }
            route("refund-requests") { refundSellerRoutes() }.describe { tag("Refund") }
            route("rma-requests") { rmaSellerRoutes() }.describe { tag("RMA") }
            route("dashboard") { dashboardSellerRoutes() }.describe { tag("Dashboard") }
            route("payouts") { payoutSellerRoutes() }.describe { tag("Payout") }
        }
    }
}

private fun Route.adminRoutes() {
    route("admin") {
        adminAuth {
            route("auth") { authAdminRoutes() }.describe { tag("Auth") }
            route("brands") { brandAdminRoutes() }.describe { tag("Brand") }
            route("product-categories") { productCategoryAdminRoutes() }.describe { tag("Product Category") }
            route("product-subcategories") { productSubCategoryAdminRoutes() }.describe { tag("Product Sub Category") }
            route("shop-categories") { shopCategoryAdminRoutes() }.describe { tag("Shop Category") }
            route("shops") { shopAdminRoutes() }.describe { tag("Shop") }
            route("sellers") { adminKycRoutes() }.describe { tag("KYC") }
            route("products") { productAdminRoutes() }.describe { tag("Product") }
            route("inventories") { inventoryAdminRoutes() }.describe { tag("Inventory") }
            route("orders") { orderAdminRoutes() }.describe { tag("Order") }
            route("payments") { paymentAdminRoutes() }.describe { tag("Payment") }
            route("refund-requests") { refundAdminRoutes() }.describe { tag("Refund") }
            route("rma-requests") { rmaAdminRoutes() }.describe { tag("RMA") }
            route("policies") { policyAdminRoutes() }.describe { tag("Policy") }
            route("policy-consents") { consentAdminRoutes() }.describe { tag("Consent") }
            route("shipping-methods") { shippingMethodAdminRoutes() }.describe { tag("Shipping Method") }
            route("coupons") { couponAdminRoutes() }.describe { tag("Coupon") }
            route("dashboard") { dashboardAdminRoutes() }.describe { tag("Dashboard") }
            route("audit-logs") { auditLogAdminRoutes() }.describe { tag("Audit Log") }
            route("payouts") { payoutAdminRoutes() }.describe { tag("Payout") }
            route("fx-rates") { fxAdminRoutes() }.describe { tag("FX") }
            route("tax-rules") { taxAdminRoutes() }.describe { tag("Tax") }
            route("feature-flags") { featureFlagRoutes() }.describe { tag("Feature Flags") }
            route("outbox") { outboxAdminRoutes() }.describe { tag("Outbox") }
        }
    }
}
