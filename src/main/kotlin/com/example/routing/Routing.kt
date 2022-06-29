package com.example.routing

import com.example.controller.ProductController
import com.example.controller.ShopController
import com.example.controller.UserController
import com.papsign.ktor.openapigen.APITag
import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.openAPIGen
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.tag
import com.papsign.ktor.openapigen.schema.namer.DefaultSchemaNamer
import com.papsign.ktor.openapigen.schema.namer.SchemaNamer
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlin.reflect.KType

fun Application.configureRouting() {

    install(OpenAPIGen) {
        // basic info
        info {
            version = "0.0.1"
            title = "Ktor Ecommerce"
            description = "Api Documentation for Ktor Ecommerce App"
            contact {
                name = "PLabs Corporation"
                email = "piash599@gmail.com"
            }
        }
        // describe the server, add as many as you want
        server("http://localhost:8080/") {
            description = "Ktor for local server"
        }
        //optional custom schema object namer
        replaceModule(DefaultSchemaNamer, object : SchemaNamer {
            val regex = Regex("[A-Za-z0-9_.]+")
            override fun get(type: KType): String {
                return type.toString().replace(regex) { it.value.split(".").last() }.replace(Regex(">|<|, "), "_")
            }
        })
    }
    install(Routing) {
        get("/openapi.json") {
            call.respond(application.openAPIGen.api.serialize())
        }
        get("/") {
            call.respondRedirect("/swagger-ui/index.html?url=/openapi.json", true)
        }


      /*  apiRouting {
            tag(Tags.USER) {
                userRoute(UserController())
            }
            tag(Tags.SHOP) {
                shopRoute(ShopController())
            }
            tag(Tags.PRODUCT) {
                productRoute(ProductController())
            }
        }*/
    }
}

enum class Tags(override val description: String) : APITag {
    USER(""), SHOP(""), PRODUCT("")
}
