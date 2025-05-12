package com.piashcse.plugins

import com.piashcse.modules.auth.authRoutes
import com.piashcse.modules.auth.AuthService
import com.piashcse.modules.brand.brandRoutes
import com.piashcse.modules.brand.BrandService
import com.piashcse.modules.cart.cartRoutes
import com.piashcse.modules.cart.CartService
import com.piashcse.modules.consent.consentRoutes
import com.piashcse.modules.consent.ConsentService
import com.piashcse.modules.order.orderRoutes
import com.piashcse.modules.order.OrderService
import com.piashcse.modules.payment.paymentRoutes
import com.piashcse.modules.payment.PaymentService
import com.piashcse.modules.policy.policyRoutes
import com.piashcse.modules.policy.PolicyService
import com.piashcse.modules.product.productRoutes
import com.piashcse.modules.product.ProductService
import com.piashcse.modules.productcategory.productCategoryRoutes
import com.piashcse.modules.productcategory.ProductCategoryService
import com.piashcse.modules.productsubcategory.productSubCategoryRoutes
import com.piashcse.modules.productsubcategory.ProductSubCategoryService
import com.piashcse.modules.profile.profileRoutes
import com.piashcse.modules.profile.ProfileService
import com.piashcse.modules.review_rating.reviewRatingRoutes
import com.piashcse.modules.review_rating.ReviewRatingService
import com.piashcse.modules.shipping.shippingRoutes
import com.piashcse.modules.shipping.ShippingService
import com.piashcse.modules.shop.ShopService
import com.piashcse.modules.shop.shopRoutes
import com.piashcse.modules.shopcategory.shopCategoryRoutes
import com.piashcse.modules.shopcategory.ShopCategoryService
import com.piashcse.modules.wishlist.wishListRoutes
import com.piashcse.modules.wishlist.WishListService
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
