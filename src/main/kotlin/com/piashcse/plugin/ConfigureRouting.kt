package com.piashcse.plugin

import com.piashcse.constants.AppConstants
import com.piashcse.feature.audit_log.AuditLogService
import com.piashcse.feature.audit_log.auditLogAdminRoutes
import com.piashcse.feature.auth.AuthService
import com.piashcse.feature.auth.authAdminRoutes
import com.piashcse.feature.auth.authRoutes
import com.piashcse.feature.brand.BrandService
import com.piashcse.feature.brand.brandAdminRoutes
import com.piashcse.feature.brand.brandRoutes
import com.piashcse.feature.cart.CartService
import com.piashcse.feature.cart.cartRoutes
import com.piashcse.feature.checkout.checkoutRoutes
import com.piashcse.feature.consent.ConsentService
import com.piashcse.feature.consent.consentRoutes
import com.piashcse.feature.coupon.CouponService
import com.piashcse.feature.coupon.couponAdminRoutes
import com.piashcse.feature.coupon.couponRoutes
import com.piashcse.feature.dashboard.DashboardService
import com.piashcse.feature.dashboard.dashboardAdminRoutes
import com.piashcse.feature.inventory.InventoryService
import com.piashcse.feature.inventory.inventoryAdminRoutes
import com.piashcse.feature.inventory.inventorySellerRoutes
import com.piashcse.feature.order.OrderService
import com.piashcse.feature.order.orderAdminRoutes
import com.piashcse.feature.order.orderRoutes
import com.piashcse.feature.order.orderSellerRoutes
import com.piashcse.feature.payment.PaymentService
import com.piashcse.feature.payment.paymentRoutes
import com.piashcse.feature.policy.PolicyService
import com.piashcse.feature.policy.policyAdminRoutes
import com.piashcse.feature.policy.policyRoutes
import com.piashcse.feature.product.ProductService
import com.piashcse.feature.product.productAdminRoutes
import com.piashcse.feature.product.productRoutes
import com.piashcse.feature.product.productSellerRoutes
import com.piashcse.feature.product_category.ProductCategoryService
import com.piashcse.feature.product_category.productCategoryAdminRoutes
import com.piashcse.feature.product_category.productCategoryRoutes
import com.piashcse.feature.product_sub_category.ProductSubCategoryService
import com.piashcse.feature.product_sub_category.productSubCategoryAdminRoutes
import com.piashcse.feature.product_sub_category.productSubCategoryRoutes
import com.piashcse.feature.profile.ProfileService
import com.piashcse.feature.profile.profileRoutes
import com.piashcse.feature.refund_request.RefundRequestService
import com.piashcse.feature.refund_request.refundAdminRoutes
import com.piashcse.feature.refund_request.refundRequestRoutes
import com.piashcse.feature.refund_request.refundSellerRoutes
import com.piashcse.feature.review_rating.ReviewRatingService
import com.piashcse.feature.review_rating.reviewRatingRoutes
import com.piashcse.feature.shipping_address.ShippingAddressService
import com.piashcse.feature.shipping_method.ShippingMethodService
import com.piashcse.feature.shipping_method.shippingMethodAdminRoutes
import com.piashcse.feature.shop.ShopService
import com.piashcse.feature.shop.shopAdminRoutes
import com.piashcse.feature.shop.shopRoutes
import com.piashcse.feature.shop.shopSellerRoutesV1
import com.piashcse.feature.shop_category.ShopCategoryService
import com.piashcse.feature.shop_category.shopCategoryAdminRoutes
import com.piashcse.feature.wishlist.WishListService
import com.piashcse.feature.wishlist.wishListRoutes
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.hide
import io.ktor.utils.io.ExperimentalKtorApi
import org.koin.ktor.ext.inject

@OptIn(ExperimentalKtorApi::class)
fun Application.configureRoute() {
    val auditLogService: AuditLogService by inject()
    val authService: AuthService by inject()
    val brandService: BrandService by inject()
    val cartService: CartService by inject()
    val consentService: ConsentService by inject()
    val couponService: CouponService by inject()
    val dashboardService: DashboardService by inject()
    val inventoryService: InventoryService by inject()
    val orderService: OrderService by inject()
    val paymentService: PaymentService by inject()
    val policyService: PolicyService by inject()
    val productService: ProductService by inject()
    val productCategoryService: ProductCategoryService by inject()
    val productSubCategoryService: ProductSubCategoryService by inject()
    val profileService: ProfileService by inject()
    val refundRequestService: RefundRequestService by inject()
    val reviewRatingService: ReviewRatingService by inject()
    val shippingAddressService: ShippingAddressService by inject()
    val shippingMethodService: ShippingMethodService by inject()
    val shopService: ShopService by inject()
    val shopCategoryService: ShopCategoryService by inject()
    val wishListService: WishListService by inject()

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
                customerRoutes(
                    authService, profileService, shopService, brandService,
                    productCategoryService, productSubCategoryService, productService,
                    reviewRatingService, cartService, wishListService,
                    shippingAddressService, shippingMethodService, orderService,
                    paymentService, policyService, consentService,
                    refundRequestService, couponService,
                )
                sellerRoutes(shopService, productService, inventoryService, orderService, refundRequestService)
                adminRoutes(
                    authService, brandService, productCategoryService,
                    productSubCategoryService, shopCategoryService, shopService,
                    productService, inventoryService, orderService, dashboardService,
                    auditLogService,
                    refundRequestService, policyService, shippingMethodService, couponService,
                )
            }
        }
    }
}

private fun Route.customerRoutes(
    auth: AuthService, profile: ProfileService, shop: ShopService,
    brand: BrandService, productCategory: ProductCategoryService,
    productSubCategory: ProductSubCategoryService, product: ProductService,
    reviewRating: ReviewRatingService, cart: CartService, wishList: WishListService,
    shippingAddress: ShippingAddressService, shippingMethod: ShippingMethodService,
    order: OrderService, payment: PaymentService, policy: PolicyService,
    consent: ConsentService, refundRequest: RefundRequestService, coupon: CouponService,
) {
    route("auth") { authRoutes(auth) }
    route("profile") { profileRoutes(profile) }
    route("shops") { shopRoutes(shop) }
    route("brands") { brandRoutes(brand) }
    route("product-categories") { productCategoryRoutes(productCategory) }
    route("product-subcategories") { productSubCategoryRoutes(productSubCategory) }
    route("products") { productRoutes(product) }
    route("reviews") { reviewRatingRoutes(reviewRating) }
    route("carts") { cartRoutes(cart) }
    route("wishlists") { wishListRoutes(wishList) }
    route("checkout") { checkoutRoutes(shippingAddress, shippingMethod, order) }
    route("orders") { orderRoutes(order) }
    route("payments") { paymentRoutes(payment) }
    route("policies") { policyRoutes(policy) }
    route("policy-consents") { consentRoutes(consent) }
    route("refund-requests") { refundRequestRoutes(refundRequest) }
    route("coupons") { couponRoutes(coupon) }
}

private fun Route.sellerRoutes(
    shop: ShopService, product: ProductService, inventory: InventoryService,
    order: OrderService, refundRequest: RefundRequestService,
) {
    route("seller") {
        sellerAuth {
            route("shops") { shopSellerRoutesV1(shop) }
            route("products") { productSellerRoutes(product) }
            route("inventories") { inventorySellerRoutes(inventory) }
            route("orders") { orderSellerRoutes(order) }
            route("refund-requests") { refundSellerRoutes(refundRequest) }
        }
    }
}

private fun Route.adminRoutes(
    auth: AuthService, brand: BrandService,
    productCategory: ProductCategoryService, productSubCategory: ProductSubCategoryService,
    shopCategory: ShopCategoryService, shop: ShopService, product: ProductService,
    inventory: InventoryService, order: OrderService, dashboard: DashboardService,
    auditLog: AuditLogService,
    refundRequest: RefundRequestService,
    policy: PolicyService, shippingMethod: ShippingMethodService, coupon: CouponService,
) {
    route("admin") {
        adminAuth {
            route("auth") { authAdminRoutes(auth) }
            route("brands") { brandAdminRoutes(brand) }
            route("product-categories") { productCategoryAdminRoutes(productCategory) }
            route("product-subcategories") { productSubCategoryAdminRoutes(productSubCategory) }
            route("shop-categories") { shopCategoryAdminRoutes(shopCategory) }
            route("shops") { shopAdminRoutes(shop) }
            route("products") { productAdminRoutes(product) }
            route("inventories") { inventoryAdminRoutes(inventory) }
            route("orders") { orderAdminRoutes(order) }
            route("refund-requests") { refundAdminRoutes(refundRequest) }
            route("policies") { policyAdminRoutes(policy) }
            route("shipping-methods") { shippingMethodAdminRoutes(shippingMethod) }
            route("coupons") { couponAdminRoutes(coupon) }
            route("dashboard") { dashboardAdminRoutes(dashboard) }
            route("audit-logs") { auditLogAdminRoutes(auditLog) }
        }
    }
}
