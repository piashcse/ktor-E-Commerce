package com.piashcse.plugins

import com.piashcse.controller.*
import com.piashcse.modules.auth.routes.authRoutes
import com.piashcse.modules.auth.service.AuthController
import com.piashcse.modules.brand.routes.brandRoutes
import com.piashcse.modules.brand.service.BrandController
import com.piashcse.modules.cart.routes.cartRoutes
import com.piashcse.modules.cart.service.CartController
import com.piashcse.modules.consent.routes.consentRoutes
import com.piashcse.modules.consent.service.ConsentController
import com.piashcse.modules.order.routes.orderRoutes
import com.piashcse.modules.order.service.OrderController
import com.piashcse.modules.payment.routes.paymentRoutes
import com.piashcse.modules.payment.service.PaymentController
import com.piashcse.modules.policy.routes.policyRoutes
import com.piashcse.modules.policy.service.PolicyController
import com.piashcse.modules.product.routes.productRoutes
import com.piashcse.modules.product.service.ProductController
import com.piashcse.modules.productcategory.routes.productCategoryRoutes
import com.piashcse.modules.productcategory.service.ProductCategoryController
import com.piashcse.modules.productsubcategory.routes.productSubCategoryRoutes
import com.piashcse.modules.productsubcategory.service.ProductSubCategoryController
import com.piashcse.modules.profile.routes.profileRoutes
import com.piashcse.modules.profile.service.ProfileController
import com.piashcse.modules.review_rating.routes.reviewRatingRoutes
import com.piashcse.modules.review_rating.service.ReviewRatingController
import com.piashcse.modules.shipping.routes.shippingRoutes
import com.piashcse.modules.shipping.service.ShippingController
import com.piashcse.modules.shop.service.ShopController
import com.piashcse.modules.shop.routes.shopRoutes
import com.piashcse.modules.shopcategory.routes.shopCategoryRoutes
import com.piashcse.modules.shopcategory.service.ShopCategoryController
import com.piashcse.modules.wishlist.routes.wishListRoutes
import com.piashcse.modules.wishlist.service.WishListController
import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Application.configureRoute() {
    val authController: AuthController by inject()
    val userProfileController: ProfileController by inject()
    val shopCategoryController: ShopCategoryController by inject()
    val shopController: ShopController by inject()
    val brandController: BrandController by inject()
    val productCategoryController: ProductCategoryController by inject()
    val productSubCategoryController: ProductSubCategoryController by inject()
    val productController: ProductController by inject()
    val reviewRatingController: ReviewRatingController by inject()
    val cartController: CartController by inject()
    val wishListController: WishListController by inject()
    val shippingController: ShippingController by inject()
    val orderController: OrderController by inject()
    val paymentController: PaymentController by inject()
    val policyController: PolicyController by inject()
    val consentController: ConsentController by inject()
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
