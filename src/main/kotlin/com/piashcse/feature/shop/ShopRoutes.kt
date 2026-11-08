@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.shop

import com.piashcse.constants.Message
import com.piashcse.constants.ShopStatus
import com.piashcse.model.request.ShopRequest
import com.piashcse.model.request.UpdateShopRequest
import com.piashcse.plugin.adminWriteRateLimit
import com.piashcse.plugin.sellerWriteRateLimit
import com.piashcse.service.UploadService
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.NotFoundException
import com.piashcse.utils.validator.ValidationException
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.koin.ktor.ext.inject

/**
 * Shop Discovery Routes.
 */
fun Route.shopRoutes() {
    val shopRepo: ShopRepository by inject()
    /**
     * Retrieve detailed information about a specific shop
     */
    get("/{id}") {
        val shopId = call.requirePathParameter("id")
        val shop = shopRepo.getShopById(shopId) ?: throw NotFoundException(Message.Shops.NOT_FOUND)
        if (shop.status != ShopStatus.APPROVED) throw NotFoundException(Message.Shops.NOT_FOUND)
        call.respondOk(shop)
    }.describe { summary = "Retrieve detailed information about a specific shop" }

    /**
     * Retrieve public shops with filters (public)
     */
    get("/public") {
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(
            shopRepo.getShops(call.request.queryParameters["status"], call.request.queryParameters["category"], limit, offset),
        )
    }.describe { summary = "Retrieve public shops with filters (public)" }

    /**
     * Retrieve shops by category (public)
     */
    get("/category/{categoryId}") {
        val categoryId = call.requirePathParameter("categoryId")
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(shopRepo.getShopsByCategory(categoryId, limit, offset))
    }.describe { summary = "Retrieve shops by category (public)" }

    /**
     * Retrieve featured shops (public)
     */
    get("/featured") {
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(shopRepo.getFeaturedShops(limit, offset))
    }.describe { summary = "Retrieve featured shops (public)" }
}

/**
 * V1 Seller Shop Management Routes.
 */
fun Route.shopSellerRoutesV1() {
    val shopRepo: ShopRepository by inject()
    sellerWriteRateLimit {
        /**
         * Seller - Create a new shop
         *
         */
        post {
            call.respondCreated(shopRepo.createShop(call.currentUserId, call.receive<ShopRequest>()))
        }.describe { summary = "Seller - Create a new shop" }

        /**
         * Seller - Update shop details
         *
         */
        put("/{id}") {
            val shopId = call.requirePathParameter("id")
            call.respondOk(shopRepo.updateShop(call.currentUserId, shopId, call.receive<UpdateShopRequest>()))
        }.describe { summary = "Seller - Update shop details" }

        /**
         * Seller - Upload a shop logo
         *
         */
        post("/{id}/image") {
            val shopId = call.requirePathParameter("id")
            val multipart = call.receiveMultipart()
            var imageUrl: String? = null
            multipart.forEachPart { part ->
                if (part is PartData.FileItem) {
                    val bytes = UploadService.readAndValidateImagePart(part, "shop image")
                    val fileName = UploadService.uploadShopImage(part, bytes)
                    imageUrl = UploadService.getShopImageUrl(fileName)
                }
                part.dispose()
            }
            val url = imageUrl ?: throw ValidationException(Message.Validation.FILE_REQUIRED)
            call.respondOk(shopRepo.updateShop(call.currentUserId, shopId, UpdateShopRequest(logo = url)))
        }.describe { summary = "Seller - Upload a shop logo" }
    }

    /**
     * Seller - Retrieve owned shops
     *
     */
    get {
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(shopRepo.getShopsByUser(call.currentUserId, limit, offset))
    }.describe { summary = "Seller - Retrieve owned shops" }
}

/**
 * Admin Shop Management Routes.
 */
fun Route.shopAdminRoutes() {
    val shopRepo: ShopRepository by inject()
    adminWriteRateLimit {
        /**
         * Admin - Approve a pending shop application
         *
         */
        put("/approve/{id}") {
            val shopId = call.requirePathParameter("id")
            val (actorId, actorEmail, actorRole) = call.actor()
            call.respondOk(shopRepo.approveShop(shopId, actorId, actorEmail, actorRole))
        }.describe { summary = "Admin - Approve a pending shop application" }

        /**
         * Admin - Reject a shop application
         *
         */
        put("/reject/{id}") {
            val shopId = call.requirePathParameter("id")
            val (actorId, actorEmail, actorRole) = call.actor()
            call.respondOk(shopRepo.rejectShop(shopId, actorId, actorEmail, actorRole))
        }.describe { summary = "Admin - Reject a shop application" }

        /**
         * Admin - Suspend an active shop
         *
         */
        put("/suspend/{id}") {
            val shopId = call.requirePathParameter("id")
            val (actorId, actorEmail, actorRole) = call.actor()
            call.respondOk(shopRepo.suspendShop(shopId, actorId, actorEmail, actorRole))
        }.describe { summary = "Admin - Suspend an active shop" }

        /**
         * Admin - Activate a suspended shop
         *
         */
        put("/activate/{id}") {
            val shopId = call.requirePathParameter("id")
            val (actorId, actorEmail, actorRole) = call.actor()
            call.respondOk(shopRepo.activateShop(shopId, actorId, actorEmail, actorRole))
        }.describe { summary = "Admin - Activate a suspended shop" }
    }

    /**
     * Admin - Retrieve shops filtered by status
     *
     */
    get("/status") {
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(shopRepo.getShopsByStatus(call.requireQueryParameter("status").parseEnum<ShopStatus>("shop status"), limit, offset))
    }.describe { summary = "Admin - Retrieve shops filtered by status" }
}
