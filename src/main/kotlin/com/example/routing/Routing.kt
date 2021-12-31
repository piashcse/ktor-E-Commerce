package com.example.routing

import com.example.controller.ProductController
import com.example.controller.ShopController
import com.example.controller.UserController
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    install(Routing) {
        routing {
            get("/") {
                call.respondText("Hello World!")
            }
        }
        userRoute(UserController())
        productCategoryRoute(ProductController())
        shopRoute(ShopController())
    }
}
