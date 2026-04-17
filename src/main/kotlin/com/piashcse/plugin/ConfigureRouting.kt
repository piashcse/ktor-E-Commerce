package com.piashcse.plugin

import com.piashcse.feature.auth.AuthService
import com.piashcse.feature.auth.authRoutes
import com.piashcse.feature.brand.BrandService
import com.piashcse.feature.brand.brandRoutes
import com.piashcse.feature.cart.CartService
import com.piashcse.feature.cart.cartRoutes
import com.piashcse.feature.consent.ConsentService
import com.piashcse.feature.consent.consentRoutes
import com.piashcse.feature.inventory.InventoryService
import com.piashcse.feature.inventory.inventoryRoutes
import com.piashcse.feature.order.OrderService
import com.piashcse.feature.order.orderRoutes
import com.piashcse.feature.payment.PaymentService
import com.piashcse.feature.payment.paymentRoutes
import com.piashcse.feature.policy.PolicyService
import com.piashcse.feature.policy.policyRoutes
import com.piashcse.feature.product.ProductService
import com.piashcse.feature.product.productRoutes
import com.piashcse.feature.product_category.ProductCategoryService
import com.piashcse.feature.product_category.productCategoryRoutes
import com.piashcse.feature.product_sub_category.ProductSubCategoryService
import com.piashcse.feature.product_sub_category.productSubCategoryRoutes
import com.piashcse.feature.profile.ProfileService
import com.piashcse.feature.profile.profileRoutes
import com.piashcse.feature.refund_request.RefundRequestService
import com.piashcse.feature.refund_request.refundRequestRoutes
import com.piashcse.feature.review_rating.ReviewRatingService
import com.piashcse.feature.review_rating.reviewRatingRoutes
import com.piashcse.feature.shipping.ShippingService
import com.piashcse.feature.shipping.shippingRoutes
import com.piashcse.feature.shop.ShopService
import com.piashcse.feature.shop.shopRoutes
import com.piashcse.feature.shop_category.ShopCategoryService
import com.piashcse.feature.shop_category.shopCategoryRoutes
import com.piashcse.feature.wishlist.WishListService
import com.piashcse.feature.wishlist.wishListRoutes
import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Application.configureRoute() {
    val authController: AuthService by inject()
    val userProfileController: ProfileService by inject()
    val shopCategoryController: ShopCategoryService by inject()
    val shopController: ShopService by inject()
    val brandController: BrandService by inject()
    val productCategoryController: ProductCategoryService by inject()
    val productSubCategoryController: ProductSubCategoryService by inject()
    val productController: ProductService by inject()
    val reviewRatingController: ReviewRatingService by inject()
    val cartController: CartService by inject()
    val wishListController: WishListService by inject()
    val shippingController: ShippingService by inject()
    val orderController: OrderService by inject()
    val paymentController: PaymentService by inject()
    val policyController: PolicyService by inject()
    val consentController: ConsentService by inject()
    val inventoryController: InventoryService by inject()
    val returnRequestController: RefundRequestService by inject()

    routing {

        route("/api") {
            route("v1") {
                route("auth") { authRoutes(authController) }
                route("profile") { profileRoutes(userProfileController) }
                route("shop-category") { shopCategoryRoutes(shopCategoryController) }
                route("shop") { shopRoutes(shopController, version = 1) } 
                route("brand") { brandRoutes(brandController) }
                route("product-category") { productCategoryRoutes(productCategoryController) }
                route("product-subcategory") { productSubCategoryRoutes(productSubCategoryController) }
                route("product") { productRoutes(productController) }
                route("review-rating") { reviewRatingRoutes(reviewRatingController) }
                route("cart") { cartRoutes(cartController) }
                route("wishlist") { wishListRoutes(wishListController) }
                route("shipping") { shippingRoutes(shippingController) }
                route("order") { orderRoutes(orderController) }
                route("payment") { paymentRoutes(paymentController) }
                route("policy") { policyRoutes(policyController) }
                route("policy-consents") { consentRoutes(consentController) }
                route("inventory") { inventoryRoutes(inventoryController) }
                route("refund-requests") { refundRequestRoutes(returnRequestController) }
            }

            route("v2") {
                route("shop") { shopRoutes(shopController, version = 2) } 
            }
        }
    }
}
