package com.piashcse.plugin

import com.piashcse.constants.AppConstants
import com.piashcse.feature.audit_log.auditLogAdminRoutes
import com.piashcse.feature.auth.authAdminRoutes
import com.piashcse.feature.auth.authRoutes
import com.piashcse.feature.brand.brandAdminRoutes
import com.piashcse.feature.brand.brandRoutes
import com.piashcse.feature.cart.cartRoutes
import com.piashcse.feature.checkout.checkoutRoutes
import com.piashcse.feature.consent.consentRoutes
import com.piashcse.feature.coupon.couponAdminRoutes
import com.piashcse.feature.coupon.couponRoutes
import com.piashcse.feature.dashboard.dashboardAdminRoutes
import com.piashcse.feature.inventory.inventorySellerRoutes
import com.piashcse.feature.order.orderAdminRoutes
import com.piashcse.feature.order.orderRoutes
import com.piashcse.feature.order.orderSellerRoutes
import com.piashcse.feature.payment.paymentRoutes
import com.piashcse.feature.policy.policyAdminRoutes
import com.piashcse.feature.policy.policyRoutes
import com.piashcse.feature.product.productAdminRoutes
import com.piashcse.feature.product.productRoutes
import com.piashcse.feature.product.productSellerRoutes
import com.piashcse.feature.product_category.productCategoryAdminRoutes
import com.piashcse.feature.product_category.productCategoryRoutes
import com.piashcse.feature.product_sub_category.productSubCategoryAdminRoutes
import com.piashcse.feature.product_sub_category.productSubCategoryRoutes
import com.piashcse.feature.profile.profileRoutes
import com.piashcse.feature.refund_request.refundAdminRoutes
import com.piashcse.feature.refund_request.refundRequestRoutes
import com.piashcse.feature.refund_request.refundSellerRoutes
import com.piashcse.feature.review_rating.reviewRatingRoutes
import com.piashcse.feature.shipping_method.shippingMethodAdminRoutes
import com.piashcse.feature.shop.shopAdminRoutes
import com.piashcse.feature.shop.shopRoutes
import com.piashcse.feature.shop.shopSellerRoutesV1
import com.piashcse.feature.shop_category.shopCategoryAdminRoutes
import com.piashcse.feature.wishlist.wishListRoutes
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import io.ktor.utils.io.*

@OptIn(ExperimentalKtorApi::class)
fun Application.configureRoute() {
    routing {
        get("/") { call.respondRedirect("/swagger") }.hide()
        get("/health") {
            call.respond(
                mapOf(
                    "status" to "UP",
                    "service" to "ktor-ecommerce",
                    "version" to AppConstants.APP_VERSION,
                    "timestamp" to java.time.Instant.now().toString(),
                ),
            )
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
    route("auth") { authRoutes() }
    route("profile") { profileRoutes() }
    route("shops") { shopRoutes() }
    route("brands") { brandRoutes() }
    route("product-categories") { productCategoryRoutes() }
    route("product-subcategories") { productSubCategoryRoutes() }
    route("products") { productRoutes() }
    route("reviews") { reviewRatingRoutes() }
    route("carts") { cartRoutes() }
    route("wishlists") { wishListRoutes() }
    route("checkout") { checkoutRoutes() }
    route("orders") { orderRoutes() }
    route("payments") { paymentRoutes() }
    route("policies") { policyRoutes() }
    route("policy-consents") { consentRoutes() }
    route("refund-requests") { refundRequestRoutes() }
    route("coupons") { couponRoutes() }
}

private fun Route.sellerRoutes() {
    route("seller") {
        sellerAuth {
            route("shops") { shopSellerRoutesV1() }
            route("products") { productSellerRoutes() }
            route("inventories") { inventorySellerRoutes() }
            route("orders") { orderSellerRoutes() }
            route("refund-requests") { refundSellerRoutes() }
        }
    }
}

private fun Route.adminRoutes() {
    route("admin") {
        adminAuth {
            route("auth") { authAdminRoutes() }
            route("brands") { brandAdminRoutes() }
            route("product-categories") { productCategoryAdminRoutes() }
            route("product-subcategories") { productSubCategoryAdminRoutes() }
            route("shop-categories") { shopCategoryAdminRoutes() }
            route("shops") { shopAdminRoutes() }
            route("products") { productAdminRoutes() }
            route("orders") { orderAdminRoutes() }
            route("refund-requests") { refundAdminRoutes() }
            route("policies") { policyAdminRoutes() }
            route("shipping-methods") { shippingMethodAdminRoutes() }
            route("coupons") { couponAdminRoutes() }
            route("dashboard") { dashboardAdminRoutes() }
            route("audit-logs") { auditLogAdminRoutes() }
        }
    }
}
