package com.piashcse.plugin

import com.piashcse.feature.auth.AuthService
import com.piashcse.feature.auth.authRoutes
import com.piashcse.feature.brand.BrandService
import com.piashcse.feature.brand.brandRoutes
import com.piashcse.feature.cart.CartService
import com.piashcse.feature.cart.cartRoutes
import com.piashcse.feature.consent.ConsentService
import com.piashcse.feature.consent.consentRoutes
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
    routing {
        authRoutes(authController)
        profileRoutes(userProfileController)
        shopCategoryRoutes(shopCategoryController)
        shopRoutes(shopController)
        brandRoutes(brandController)
        productCategoryRoutes(productCategoryController)
        productSubCategoryRoutes(productSubCategoryController)
        productRoutes(productController)
        reviewRatingRoutes(reviewRatingController)
        cartRoutes(cartController)
        wishListRoutes(wishListController)
        shippingRoutes(shippingController)
        orderRoutes(orderController)
        paymentRoutes(paymentController)
        policyRoutes(policyController)
        consentRoutes(consentController)
    }
}
