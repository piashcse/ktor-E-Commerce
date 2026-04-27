package com.piashcse.plugin

import com.piashcse.feature.auth.AuthService
import com.piashcse.feature.auth.authAdminRoutes
import com.piashcse.feature.auth.authRoutes
import com.piashcse.feature.brand.BrandService
import com.piashcse.feature.brand.brandAdminRoutes
import com.piashcse.feature.brand.brandRoutes
import com.piashcse.feature.cart.CartService
import com.piashcse.feature.cart.cartRoutes
import com.piashcse.feature.consent.ConsentService
import com.piashcse.feature.consent.consentRoutes
import com.piashcse.feature.inventory.InventoryService
import com.piashcse.feature.inventory.inventoryAdminRoutes
import com.piashcse.feature.inventory.inventoryRoutes
import com.piashcse.feature.order.OrderService
import com.piashcse.feature.order.orderAdminRoutes
import com.piashcse.feature.order.orderRoutes
import com.piashcse.feature.payment.PaymentService
import com.piashcse.feature.payment.paymentRoutes
import com.piashcse.feature.policy.PolicyService
import com.piashcse.feature.policy.policyAdminRoutes
import com.piashcse.feature.policy.policyRoutes
import com.piashcse.feature.product.ProductService
import com.piashcse.feature.product.productAdminRoutes
import com.piashcse.feature.product.productRoutes
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
import com.piashcse.feature.review_rating.ReviewRatingService
import com.piashcse.feature.review_rating.reviewRatingRoutes
import com.piashcse.feature.shipping.ShippingService
import com.piashcse.feature.shipping.shippingRoutes
import com.piashcse.feature.shop.ShopService
import com.piashcse.feature.shop.shopAdminRoutes
import com.piashcse.feature.shop.shopRoutes
import com.piashcse.feature.shop_category.ShopCategoryService
import com.piashcse.feature.shop_category.shopCategoryAdminRoutes
import com.piashcse.feature.wishlist.WishListService
import com.piashcse.feature.wishlist.wishListRoutes
import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

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
    val shippingService: ShippingService by inject()
    val orderService: OrderService by inject()
    val paymentService: PaymentService by inject()
    val policyService: PolicyService by inject()
    val consentService: ConsentService by inject()
    val inventoryService: InventoryService by inject()
    val returnRequestService: RefundRequestService by inject()

    routing {
        route("/api") {
            route("v1") {
                route("auth") { authRoutes(authService) }
                route("profile") { profileRoutes(userProfileService) }
                route("shop") { shopRoutes(shopService, version = 1) } 
                route("brand") { brandRoutes(brandService) }
                route("product-category") { productCategoryRoutes(productCategoryService) }
                route("product-subcategory") { productSubCategoryRoutes(productSubCategoryService) }
                route("product") { productRoutes(productService) }
                route("review-rating") { reviewRatingRoutes(reviewRatingService) }
                route("cart") { cartRoutes(cartService) }
                route("wishlist") { wishListRoutes(wishListService) }
                route("shipping") { shippingRoutes(shippingService) }
                route("order") { orderRoutes(orderService) }
                route("payment") { paymentRoutes(paymentService) }
                route("policy") { policyRoutes(policyService) }
                route("policy-consents") { consentRoutes(consentService) }
                route("inventory") { inventoryRoutes(inventoryService) }
                route("refund-requests") { refundRequestRoutes(returnRequestService) }
                route("admin") {
                    route("auth") { authAdminRoutes(authService) }
                    route("brand") { brandAdminRoutes(brandService) }
                    route("product-category") { productCategoryAdminRoutes(productCategoryService) }
                    route("product-subcategory") { productSubCategoryAdminRoutes(productSubCategoryService) }
                    route("shop-category") { shopCategoryAdminRoutes(shopCategoryService) }
                    route("shop") { shopAdminRoutes(shopService) }
                    route("product") { productAdminRoutes(productService) }
                    route("inventory") { inventoryAdminRoutes(inventoryService) }
                    route("order") { orderAdminRoutes(orderService) }
                    route("refund-requests") { refundAdminRoutes(returnRequestService) }
                    route("policy") { policyAdminRoutes(policyService) }
                }
            }

            route("v2") {
                route("shop") { shopRoutes(shopService, version = 2) } 
            }
        }
    }
}
