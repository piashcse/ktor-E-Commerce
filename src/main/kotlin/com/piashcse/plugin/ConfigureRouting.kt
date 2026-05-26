package com.piashcse.plugin

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
import com.piashcse.feature.shop.shopSellerRoutesV2
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
    val authService: AuthService by inject()
    val userProfileService: ProfileService by inject()
    val shopCategoryService: ShopCategoryService by inject()
    val shopService: ShopService by inject()
    val brandService: BrandService by inject()
    val productCategoryService: ProductCategoryService by inject()
    val productSubCategoryService: ProductSubCategoryService by inject()
    val productService: ProductService by inject()
    val reviewRatingService: ReviewRatingService by inject()
    val cartService: CartService by inject()
    val wishListService: WishListService by inject()
    val shippingAddressService: ShippingAddressService by inject()
    val shippingMethodService: ShippingMethodService by inject()
    val orderService: OrderService by inject()
    val paymentService: PaymentService by inject()
    val policyService: PolicyService by inject()
    val consentService: ConsentService by inject()
    val inventoryService: InventoryService by inject()
    val returnRequestService: RefundRequestService by inject()
    val couponService: CouponService by inject()

    routing {
        get("/") {
            call.respondRedirect("/swagger")
        }.hide()
        route("/api") {
            route("v1") {
                configureCustomerRoutes(
                    authService, userProfileService, shopService, brandService, productCategoryService,
                    productSubCategoryService, productService, reviewRatingService, cartService, wishListService,
                    shippingAddressService, shippingMethodService, orderService, paymentService, policyService,
                    consentService, returnRequestService, couponService,
                )
                configureSellerRoutes(shopService, productService, inventoryService, orderService, returnRequestService)
                configureAdminRoutes(
                    authService, brandService, productCategoryService, productSubCategoryService,
                    shopCategoryService, shopService, productService, inventoryService, orderService,
                    returnRequestService, policyService, shippingMethodService, couponService,
                )
            }

            route("v2") {
                route("seller") {
                    sellerAuth {
                        route("shops") { shopSellerRoutesV2(shopService) }
                    }
                }
            }
        }
    }
}

private fun Route.configureCustomerRoutes(
    authService: AuthService,
    profileService: ProfileService,
    shopService: ShopService,
    brandService: BrandService,
    productCategoryService: ProductCategoryService,
    productSubCategoryService: ProductSubCategoryService,
    productService: ProductService,
    reviewRatingService: ReviewRatingService,
    cartService: CartService,
    wishListService: WishListService,
    shippingAddressService: ShippingAddressService,
    shippingMethodService: ShippingMethodService,
    orderService: OrderService,
    paymentService: PaymentService,
    policyService: PolicyService,
    consentService: ConsentService,
    returnRequestService: RefundRequestService,
    couponService: CouponService,
) {
    route("auth") { authRoutes(authService) }
    route("profile") { profileRoutes(profileService) }
    route("shops") { shopRoutes(shopService) }
    route("brands") { brandRoutes(brandService) }
    route("product-categories") { productCategoryRoutes(productCategoryService) }
    route("product-subcategories") { productSubCategoryRoutes(productSubCategoryService) }
    route("products") { productRoutes(productService) }
    route("reviews") { reviewRatingRoutes(reviewRatingService) }
    route("carts") { cartRoutes(cartService) }
    route("wishlists") { wishListRoutes(wishListService) }
    route("checkout") { checkoutRoutes(shippingAddressService, shippingMethodService, orderService) }
    route("orders") { orderRoutes(orderService) }
    route("payments") { paymentRoutes(paymentService) }
    route("policies") { policyRoutes(policyService) }
    route("policy-consents") { consentRoutes(consentService) }
    route("refund-requests") { refundRequestRoutes(returnRequestService) }
    route("coupons") { couponRoutes(couponService) }
}

private fun Route.configureSellerRoutes(
    shopService: ShopService,
    productService: ProductService,
    inventoryService: InventoryService,
    orderService: OrderService,
    returnRequestService: RefundRequestService,
) {
    route("seller") {
        sellerAuth {
            route("shops") { shopSellerRoutesV1(shopService) }
            route("products") { productSellerRoutes(productService) }
            route("inventories") { inventorySellerRoutes(inventoryService) }
            route("orders") { orderSellerRoutes(orderService) }
            route("refund-requests") { refundSellerRoutes(returnRequestService) }
        }
    }
}

private fun Route.configureAdminRoutes(
    authService: AuthService,
    brandService: BrandService,
    productCategoryService: ProductCategoryService,
    productSubCategoryService: ProductSubCategoryService,
    shopCategoryService: ShopCategoryService,
    shopService: ShopService,
    productService: ProductService,
    inventoryService: InventoryService,
    orderService: OrderService,
    returnRequestService: RefundRequestService,
    policyService: PolicyService,
    shippingMethodService: ShippingMethodService,
    couponService: CouponService,
) {
    route("admin") {
        adminAuth {
            route("auth") { authAdminRoutes(authService) }
            route("brands") { brandAdminRoutes(brandService) }
            route("product-categories") { productCategoryAdminRoutes(productCategoryService) }
            route("product-subcategories") { productSubCategoryAdminRoutes(productSubCategoryService) }
            route("shop-categories") { shopCategoryAdminRoutes(shopCategoryService) }
            route("shops") { shopAdminRoutes(shopService) }
            route("products") { productAdminRoutes(productService) }
            route("inventories") { inventoryAdminRoutes(inventoryService) }
            route("orders") { orderAdminRoutes(orderService) }
            route("refund-requests") { refundAdminRoutes(returnRequestService) }
            route("policies") { policyAdminRoutes(policyService) }
            route("shipping-methods") { shippingMethodAdminRoutes(shippingMethodService) }
            route("coupons") { couponAdminRoutes(couponService) }
        }
    }
}
