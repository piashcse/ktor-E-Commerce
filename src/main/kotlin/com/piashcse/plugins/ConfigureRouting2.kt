package com.piashcse.plugins

import com.piashcse.controller.*
import com.piashcse.route.*
import io.github.smiley4.ktorswaggerui.dsl.routing.route
import io.github.smiley4.ktorswaggerui.routing.openApiSpec
import io.github.smiley4.ktorswaggerui.routing.swaggerUI
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting2() {
    routing {
        // Create a route for the openapi-spec file.
        route("api.json") {
            openApiSpec()
        }
        // Create a route for the swagger-ui using the openapi-spec at "/api.json".
        route("swagger") {
            swaggerUI("/api.json")
        }
        route {
            userRoute(UserController())
            profileRoute(ProfileController())
            shopRoute(ShopController())
            brandRoute(BrandController())
            cartRoute(CartController())
            wishListRoute(WishListController())
            shippingRoute(ShippingController())
            productCategoryRoute(ProductCategoryController())
            productSubCategoryRoute(ProductSubCategoryController())
        }
    }
}
