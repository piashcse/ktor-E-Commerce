package com.piashcse.feature.shop

import com.piashcse.constants.Message
import com.piashcse.constants.ShopStatus
import com.piashcse.constants.UserType
import com.piashcse.model.request.ShopRequest
import com.piashcse.model.request.UpdateShopRequest
import com.piashcse.plugin.requireRole
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.NotFoundException

import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * Shop Discovery Routes.
 */
fun Route.shopRoutes() {
    val shopRepo: ShopRepository by inject()
    /**
     * @tag Shop
     * @description Retrieve detailed information about a specific shop
     */
    get("/{id}") {
        val shopId = call.requirePathParameter("id")
        val shop = shopRepo.getShopById(shopId) ?: throw NotFoundException(Message.Shops.NOT_FOUND)
        call.respondOk(shop)
    }

    requireRole(UserType.CUSTOMER, UserType.SELLER) {
        /**
         * @tag Shop
         * @description Retrieve public shops with filters
         */
        get("/public") {
            val (limit, offset) = call.paginateQueryParams()
            call.respondOk(shopRepo.getShops(call.request.queryParameters["status"], call.request.queryParameters["category"], limit, offset))
        }

        /**
         * @tag Shop
         * @description Retrieve shops by category
         */
        get("/category/{categoryId}") {
            val categoryId = call.requirePathParameter("categoryId")
            val (limit, offset) = call.paginateQueryParams()
            call.respondOk(shopRepo.getShopsByCategory(categoryId, limit, offset))
        }

        /**
         * @tag Shop
         * @description Retrieve featured shops
         */
        get("/featured") {
            val (limit, offset) = call.paginateQueryParams()
            call.respondOk(shopRepo.getFeaturedShops(limit, offset))
        }
    }
}

/**
 * V1 Seller Shop Management Routes.
 */
fun Route.shopSellerRoutesV1() {
    val shopRepo: ShopRepository by inject()
    /**
     * @tag Shop
     * @description Seller: Create a new shop
     */
    post {
        call.respondCreated(shopRepo.createShop(call.currentUserId, call.receive<ShopRequest>()))
    }

    /**
     * @tag Shop
     * @description Seller: Retrieve owned shops
     */
    get {
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(shopRepo.getShopsByUser(call.currentUserId, limit, offset))
    }

    /**
     * @tag Shop
     * @description Seller: Update shop details
     */
    put("/{id}") {
        val shopId = call.requirePathParameter("id")
        call.respondOk(shopRepo.updateShop(call.currentUserId, shopId, call.receive<UpdateShopRequest>()))
    }
}

/**
 * Admin Shop Management Routes.
 */
fun Route.shopAdminRoutes() {
    val shopRepo: ShopRepository by inject()
    /**
     * @tag Shop
     * @description Admin: Retrieve shops filtered by status
     */
    get("/status") {
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(shopRepo.getShopsByStatus(call.requireQueryParameter("status").parseEnum<ShopStatus>("shop status"), limit, offset))
    }

    /**
     * @tag Shop
     * @description Admin: Approve a pending shop application
     */
    put("/approve/{id}") {
        val shopId = call.requirePathParameter("id")
        call.respondOk(shopRepo.approveShop(shopId))
    }

    /**
     * @tag Shop
     * @description Admin: Reject a shop application
     */
    put("/reject/{id}") {
        val shopId = call.requirePathParameter("id")
        call.respondOk(shopRepo.rejectShop(shopId))
    }

    /**
     * @tag Shop
     * @description Admin: Suspend an active shop
     */
    put("/suspend/{id}") {
        val shopId = call.requirePathParameter("id")
        call.respondOk(shopRepo.suspendShop(shopId))
    }

    /**
     * @tag Shop
     * @description Admin: Activate a suspended shop
     */
    put("/activate/{id}") {
        val shopId = call.requirePathParameter("id")
        call.respondOk(shopRepo.activateShop(shopId))
    }
}
