package com.piashcse.plugins

import com.piashcse.controller.*
import com.piashcse.route.*
import io.github.smiley4.ktorswaggerui.dsl.routing.route
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting2() {
    routing {
        route {
            userRoute(UserController())
            profileRoute(ProfileController())
            shopRoute(ShopController())
            brandRoute(BrandController())
            cartRoute(CartController())
            wishListRoute(WishListController())
            shippingRoute(ShippingController())
            productRoute(ProductController())
            productCategoryRoute(ProductCategoryController())
            productSubCategoryRoute(ProductSubCategoryController())
        }
    }
}
