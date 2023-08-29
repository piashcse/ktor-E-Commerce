package com.piashcse.plugins

import com.piashcse.controller.*
import com.piashcse.route.*
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
            tag(Tags.PROFILE) {
                profileRouting(ProfileController())
            }
            tag(Tags.SHOP) {
                shopRoute(ShopController())
            }
            tag(Tags.PRODUCT) {
                productRoute(ProductController())
            }
            tag(Tags.PRODUCT_CATEGORY) {
                productCategoryRoute(CategoryController())
            }
            tag(Tags.PRODUCT_SUB_CATEGORY) {
                productSubCategoryRoute(SubCategoryController())
            }
            tag(Tags.BRAND) {
                brandRoute(BrandController())
            }
            tag(Tags.CART) {
                cartRoute(CartController())
            }
            tag(Tags.ORDER) {
                orderRoute(OrderController())
            }
            tag(Tags.WISHLIST) {
                wishListRoute(WishListController())
            }
            tag(Tags.SHIPPING) {
                shippingRoute(ShippingController())
            }
        }
    }
}

enum class Tags(override val description: String) : APITag {
    USER(""), PROFILE(""), SHOP(""), PRODUCT(""), PRODUCT_CATEGORY(""), PRODUCT_SUB_CATEGORY(""), BRAND(""), CART(""), ORDER(""), WISHLIST(
        ""
    ),
    SHIPPING(
        ""
    )
}
