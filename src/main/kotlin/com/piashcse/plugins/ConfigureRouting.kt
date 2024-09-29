package com.piashcse.plugins

import com.piashcse.controller.*
import com.piashcse.route.*
import io.github.smiley4.ktorswaggerui.dsl.routing.route
import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Application.configureRoute() {
    val userController: UserController by inject()
    val userProfileController: UserProfileController by inject()
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
    routing {
        route {
            userRoute(userController)
            userProfileRoute(userProfileController)
            shopCategoryRoute(shopCategoryController)
            shopRoute(shopController)
            brandRoute(brandController)
            productCategoryRoute(productCategoryController)
            productSubCategoryRoute(productSubCategoryController)
            productRoute(productController)
            reviewRatingRoute(reviewRatingController)
            cartRoute(cartController)
            wishListRoute(wishListController)
            shippingRoute(shippingController)
            orderRoute(orderController)
            paymentRoute(paymentController)
        }
    }
}
