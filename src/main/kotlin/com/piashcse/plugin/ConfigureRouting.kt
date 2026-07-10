package com.piashcse.plugin

import com.piashcse.constants.AppConstants
import com.piashcse.feature.audit_log.AuditLogService
import com.piashcse.feature.audit_log.auditLogAdminRoutes
import com.piashcse.feature.auth.PasswordManagementService
import com.piashcse.feature.auth.TokenManagementService
import com.piashcse.feature.auth.UserAuthenticationService
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
import com.piashcse.feature.order.CheckoutOrchestrator
import com.piashcse.feature.order.OrderManagementService
import com.piashcse.feature.order.OrderQueryService
import com.piashcse.feature.order.orderAdminRoutes
import com.piashcse.feature.order.orderRoutes
import com.piashcse.feature.order.orderSellerRoutes
import com.piashcse.feature.payment.PaymentService
import com.piashcse.feature.payment.paymentRoutes
import com.piashcse.feature.policy.PolicyService
import com.piashcse.feature.policy.policyAdminRoutes
import com.piashcse.feature.policy.policyRoutes
import com.piashcse.feature.product.ProductCatalogService
import com.piashcse.feature.product.ProductCrudService
import com.piashcse.feature.product.ProductQueryService
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
    // Split auth services
    val userAuthService: UserAuthenticationService by inject()
    val passwordService: PasswordManagementService by inject()
    val tokenService: TokenManagementService by inject()

    // Split product services
    val productCatalogService: ProductCatalogService by inject()
    val productQueryService: ProductQueryService by inject()
    val productCrudService: ProductCrudService by inject()

    // Split order services
    val checkoutOrchestrator: CheckoutOrchestrator by inject()
    val orderManagementService: OrderManagementService by inject()
    val orderQueryService: OrderQueryService by inject()

    val auditLogService: AuditLogService by inject()
    val brandService: BrandService by inject()
    val cartService: CartService by inject()
    val consentService: ConsentService by inject()
    val couponService: CouponService by inject()
    val dashboardService: DashboardService by inject()
    val inventoryService: InventoryService by inject()
    val paymentService: PaymentService by inject()
    val policyService: PolicyService by inject()
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
                    userAuthService = userAuthService,
                    passwordService = passwordService,
                    tokenService = tokenService,
                    profileService = profileService,
                    shopService = shopService,
                    brandService = brandService,
                    productCategoryService = productCategoryService,
                    productSubCategoryService = productSubCategoryService,
                    productCatalogService = productCatalogService,
                    productQueryService = productQueryService,
                    reviewRatingService = reviewRatingService,
                    cartService = cartService,
                    wishListService = wishListService,
                    shippingAddressService = shippingAddressService,
                    shippingMethodService = shippingMethodService,
                    checkoutOrchestrator = checkoutOrchestrator,
                    orderQueryService = orderQueryService,
                    orderManagementService = orderManagementService,
                    paymentService = paymentService,
                    policyService = policyService,
                    consentService = consentService,
                    refundRequestService = refundRequestService,
                    couponService = couponService,
                )
                sellerRoutes(
                    shopService = shopService,
                    productQueryService = productQueryService,
                    productCrudService = productCrudService,
                    inventoryService = inventoryService,
                    orderQueryService = orderQueryService,
                    refundRequestService = refundRequestService,
                )
                adminRoutes(
                    userAuthService = userAuthService,
                    brandService = brandService,
                    productCategoryService = productCategoryService,
                    productSubCategoryService = productSubCategoryService,
                    shopCategoryService = shopCategoryService,
                    shopService = shopService,
                    productCrudService = productCrudService,
                    inventoryService = inventoryService,
                    orderQueryService = orderQueryService,
                    orderManagementService = orderManagementService,
                    dashboardService = dashboardService,
                    auditLogService = auditLogService,
                    refundRequestService = refundRequestService,
                    policyService = policyService,
                    shippingMethodService = shippingMethodService,
                    couponService = couponService,
                )
            }
        }
    }
}

private fun Route.customerRoutes(
    userAuthService: UserAuthenticationService,
    passwordService: PasswordManagementService,
    tokenService: TokenManagementService,
    profileService: ProfileService,
    shopService: ShopService,
    brandService: BrandService,
    productCategoryService: ProductCategoryService,
    productSubCategoryService: ProductSubCategoryService,
    productCatalogService: ProductCatalogService,
    productQueryService: ProductQueryService,
    reviewRatingService: ReviewRatingService,
    cartService: CartService,
    wishListService: WishListService,
    shippingAddressService: ShippingAddressService,
    shippingMethodService: ShippingMethodService,
    checkoutOrchestrator: CheckoutOrchestrator,
    orderQueryService: OrderQueryService,
    orderManagementService: OrderManagementService,
    paymentService: PaymentService,
    policyService: PolicyService,
    consentService: ConsentService,
    refundRequestService: RefundRequestService,
    couponService: CouponService,
) {
    route("auth") { authRoutes(userAuthService, passwordService, tokenService) }
    route("profile") { profileRoutes(profileService) }
    route("shops") { shopRoutes(shopService) }
    route("brands") { brandRoutes(brandService) }
    route("product-categories") { productCategoryRoutes(productCategoryService) }
    route("product-subcategories") { productSubCategoryRoutes(productSubCategoryService) }
    route("products") { productRoutes(productCatalogService, productQueryService) }
    route("reviews") { reviewRatingRoutes(reviewRatingService) }
    route("carts") { cartRoutes(cartService) }
    route("wishlists") { wishListRoutes(wishListService) }
    route("checkout") { checkoutRoutes(shippingAddressService, shippingMethodService, checkoutOrchestrator) }
    route("orders") { orderRoutes(orderQueryService, orderManagementService) }
    route("payments") { paymentRoutes(paymentService) }
    route("policies") { policyRoutes(policyService) }
    route("policy-consents") { consentRoutes(consentService) }
    route("refund-requests") { refundRequestRoutes(refundRequestService) }
    route("coupons") { couponRoutes(couponService) }
}

private fun Route.sellerRoutes(
    shopService: ShopService,
    productQueryService: ProductQueryService,
    productCrudService: ProductCrudService,
    inventoryService: InventoryService,
    orderQueryService: OrderQueryService,
    refundRequestService: RefundRequestService,
) {
    route("seller") {
        sellerAuth {
            route("shops") { shopSellerRoutesV1(shopService) }
            route("products") { productSellerRoutes(productQueryService, productCrudService) }
            route("inventories") { inventorySellerRoutes(inventoryService) }
            route("orders") { orderSellerRoutes(orderQueryService) }
            route("refund-requests") { refundSellerRoutes(refundRequestService) }
        }
    }
}

private fun Route.adminRoutes(
    userAuthService: UserAuthenticationService,
    brandService: BrandService,
    productCategoryService: ProductCategoryService,
    productSubCategoryService: ProductSubCategoryService,
    shopCategoryService: ShopCategoryService,
    shopService: ShopService,
    productCrudService: ProductCrudService,
    inventoryService: InventoryService,
    orderQueryService: OrderQueryService,
    orderManagementService: OrderManagementService,
    dashboardService: DashboardService,
    auditLogService: AuditLogService,
    refundRequestService: RefundRequestService,
    policyService: PolicyService,
    shippingMethodService: ShippingMethodService,
    couponService: CouponService,
) {
    route("admin") {
        adminAuth {
            route("auth") { authAdminRoutes(userAuthService) }
            route("brands") { brandAdminRoutes(brandService) }
            route("product-categories") { productCategoryAdminRoutes(productCategoryService) }
            route("product-subcategories") { productSubCategoryAdminRoutes(productSubCategoryService) }
            route("shop-categories") { shopCategoryAdminRoutes(shopCategoryService) }
            route("shops") { shopAdminRoutes(shopService) }
            route("products") { productAdminRoutes(productCrudService) }
            route("inventories") { inventoryAdminRoutes(inventoryService) }
            route("orders") { orderAdminRoutes(orderQueryService, orderManagementService) }
            route("refund-requests") { refundAdminRoutes(refundRequestService) }
            route("policies") { policyAdminRoutes(policyService) }
            route("shipping-methods") { shippingMethodAdminRoutes(shippingMethodService) }
            route("coupons") { couponAdminRoutes(couponService) }
            route("dashboard") { dashboardAdminRoutes(dashboardService) }
            route("audit-logs") { auditLogAdminRoutes(auditLogService) }
        }
    }
}
