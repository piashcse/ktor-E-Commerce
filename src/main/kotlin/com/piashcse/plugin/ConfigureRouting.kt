package com.piashcse.plugin

import com.piashcse.feature.auth.*
import com.piashcse.feature.brand.*
import com.piashcse.feature.cart.*
import com.piashcse.feature.consent.*
import com.piashcse.feature.inventory.*
import com.piashcse.feature.order.*
import com.piashcse.feature.payment.*
import com.piashcse.feature.policy.*
import com.piashcse.feature.product.*
import com.piashcse.feature.product_category.*
import com.piashcse.feature.product_sub_category.*
import com.piashcse.feature.profile.*
import com.piashcse.feature.refund_request.*
import com.piashcse.feature.review_rating.*
import com.piashcse.feature.shipping.*
import com.piashcse.feature.shop.*
import com.piashcse.feature.shop_category.*
import com.piashcse.feature.wishlist.*
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
                route("admin") {
                    route("auth") { authAdminRoutes(authController) }
                    route("brand") { brandAdminRoutes(brandController) }
                    route("product-category") { productCategoryAdminRoutes(productCategoryController) }
                    route("product-subcategory") { productSubCategoryAdminRoutes(productSubCategoryController) }
                    route("shop-category") { shopCategoryAdminRoutes(shopCategoryController) }
                    route("shop") { shopAdminRoutes(shopController) }
                    route("product") { productAdminRoutes(productController) }
                    route("inventory") { inventoryAdminRoutes(inventoryController) }
                    route("order") { orderAdminRoutes(orderController) }
                    route("refund-requests") { refundAdminRoutes(returnRequestController) }
                    route("policy") { policyAdminRoutes(policyController) }
                }
            }

            route("v2") {
                route("shop") { shopRoutes(shopController, version = 2) } 
            }
        }
    }
}
