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

// ── Service bundles ──────────────────────────────────────────────────────────

data class CustomerServices(
    val auth: AuthService,
    val profile: ProfileService,
    val shop: ShopService,
    val brand: BrandService,
    val productCategory: ProductCategoryService,
    val productSubCategory: ProductSubCategoryService,
    val product: ProductService,
    val reviewRating: ReviewRatingService,
    val cart: CartService,
    val wishList: WishListService,
    val shippingAddress: ShippingAddressService,
    val shippingMethod: ShippingMethodService,
    val order: OrderService,
    val payment: PaymentService,
    val policy: PolicyService,
    val consent: ConsentService,
    val refundRequest: RefundRequestService,
    val coupon: CouponService,
)

data class SellerServices(
    val shop: ShopService,
    val product: ProductService,
    val inventory: InventoryService,
    val order: OrderService,
    val refundRequest: RefundRequestService,
)

data class AdminServices(
    val auth: AuthService,
    val brand: BrandService,
    val productCategory: ProductCategoryService,
    val productSubCategory: ProductSubCategoryService,
    val shopCategory: ShopCategoryService,
    val shop: ShopService,
    val product: ProductService,
    val inventory: InventoryService,
    val order: OrderService,
    val refundRequest: RefundRequestService,
    val policy: PolicyService,
    val shippingMethod: ShippingMethodService,
    val coupon: CouponService,
)

// ── Entry point ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalKtorApi::class)
fun Application.configureRoute() {
    val authService: AuthService by inject()
    val profileService: ProfileService by inject()
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
    val refundRequestService: RefundRequestService by inject()
    val couponService: CouponService by inject()

    val customer = CustomerServices(
        auth = authService, profile = profileService, shop = shopService,
        brand = brandService, productCategory = productCategoryService,
        productSubCategory = productSubCategoryService, product = productService,
        reviewRating = reviewRatingService, cart = cartService, wishList = wishListService,
        shippingAddress = shippingAddressService, shippingMethod = shippingMethodService,
        order = orderService, payment = paymentService, policy = policyService,
        consent = consentService, refundRequest = refundRequestService, coupon = couponService,
    )
    val seller = SellerServices(
        shop = shopService, product = productService, inventory = inventoryService,
        order = orderService, refundRequest = refundRequestService,
    )
    val admin = AdminServices(
        auth = authService, brand = brandService, productCategory = productCategoryService,
        productSubCategory = productSubCategoryService, shopCategory = shopCategoryService,
        shop = shopService, product = productService, inventory = inventoryService,
        order = orderService, refundRequest = refundRequestService, policy = policyService,
        shippingMethod = shippingMethodService, coupon = couponService,
    )

    routing {
        get("/") { call.respondRedirect("/swagger") }.hide()
        route("/api") {
            route("v1") {
                customerRoutes(customer)
                sellerRoutes(seller)
                adminRoutes(admin)
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

// ── Route groups ─────────────────────────────────────────────────────────────

private fun Route.customerRoutes(s: CustomerServices) {
    route("auth") { authRoutes(s.auth) }
    route("profile") { profileRoutes(s.profile) }
    route("shops") { shopRoutes(s.shop) }
    route("brands") { brandRoutes(s.brand) }
    route("product-categories") { productCategoryRoutes(s.productCategory) }
    route("product-subcategories") { productSubCategoryRoutes(s.productSubCategory) }
    route("products") { productRoutes(s.product) }
    route("reviews") { reviewRatingRoutes(s.reviewRating) }
    route("carts") { cartRoutes(s.cart) }
    route("wishlists") { wishListRoutes(s.wishList) }
    route("checkout") { checkoutRoutes(s.shippingAddress, s.shippingMethod, s.order) }
    route("orders") { orderRoutes(s.order) }
    route("payments") { paymentRoutes(s.payment) }
    route("policies") { policyRoutes(s.policy) }
    route("policy-consents") { consentRoutes(s.consent) }
    route("refund-requests") { refundRequestRoutes(s.refundRequest) }
    route("coupons") { couponRoutes(s.coupon) }
}

private fun Route.sellerRoutes(s: SellerServices) {
    route("seller") {
        sellerAuth {
            route("shops") { shopSellerRoutesV1(s.shop) }
            route("products") { productSellerRoutes(s.product) }
            route("inventories") { inventorySellerRoutes(s.inventory) }
            route("orders") { orderSellerRoutes(s.order) }
            route("refund-requests") { refundSellerRoutes(s.refundRequest) }
        }
    }
}

private fun Route.adminRoutes(s: AdminServices) {
    route("admin") {
        adminAuth {
            route("auth") { authAdminRoutes(s.auth) }
            route("brands") { brandAdminRoutes(s.brand) }
            route("product-categories") { productCategoryAdminRoutes(s.productCategory) }
            route("product-subcategories") { productSubCategoryAdminRoutes(s.productSubCategory) }
            route("shop-categories") { shopCategoryAdminRoutes(s.shopCategory) }
            route("shops") { shopAdminRoutes(s.shop) }
            route("products") { productAdminRoutes(s.product) }
            route("inventories") { inventoryAdminRoutes(s.inventory) }
            route("orders") { orderAdminRoutes(s.order) }
            route("refund-requests") { refundAdminRoutes(s.refundRequest) }
            route("policies") { policyAdminRoutes(s.policy) }
            route("shipping-methods") { shippingMethodAdminRoutes(s.shippingMethod) }
            route("coupons") { couponAdminRoutes(s.coupon) }
        }
    }
}
