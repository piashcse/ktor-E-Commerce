package com.piashcse.plugins

import com.piashcse.controller.*
import com.piashcse.routing.*
import com.papsign.ktor.openapigen.APITag
import com.papsign.ktor.openapigen.openAPIGen
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.tag
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    install(Routing) {
        // open api json loading
        get("/openapi.json") {
            call.respond(application.openAPIGen.api.serialize())
        }
        get("/") {
            call.respondRedirect("/swagger-ui/index.html?url=/openapi.json", true)
        }
        // Api routing
        apiRouting {
            tag(Tags.USER) {
                userRoute(UserController())
            }
            tag(Tags.SHOP) {
                shopRoute(ShopController())
            }
            tag(Tags.PRODUCT) {
                productRoute(ProductController())
            }
            tag(Tags.CATEGORY) {
                categoryRoute(CategoryController())
            }
            tag(Tags.SUBCATEGORY) {
                subCategoryRoute(SubCategoryController())
            }
            tag(Tags.BRAND) {
                brandRouting(BrandController())
            }
            tag(Tags.CART) {
                cartRouting(CartController())
            }
            tag(Tags.ORDER) {
                orderRouting(OrderController())
            }
        }
    }
}

enum class Tags(override val description: String) : APITag {
    USER(""), SHOP(""), PRODUCT(""), CATEGORY(""), SUBCATEGORY(""), BRAND(""), CART(""), ORDER("")
}
