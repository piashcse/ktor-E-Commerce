package com.example.plugins

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
    }
}
