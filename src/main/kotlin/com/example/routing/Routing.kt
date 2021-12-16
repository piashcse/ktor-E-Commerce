package com.example.routing

import com.example.controller.CategoryController
import com.example.controller.ShopController
import com.example.controller.UserController
import io.ktor.routing.*
import io.ktor.application.*
import io.ktor.response.*

fun Application.configureRouting() {
    install(Routing) {
        routing {
            get("/") {
                call.respondText("Hello World!")
            }
        }
        userRoute(UserController())
        categoryRouter(CategoryController())
        shopRoute(ShopController())
    }
}
