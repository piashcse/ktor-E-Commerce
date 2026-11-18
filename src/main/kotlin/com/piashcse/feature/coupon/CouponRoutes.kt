@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.coupon

import com.piashcse.constants.Message
import com.piashcse.model.request.CouponRequest
import com.piashcse.plugin.adminWriteRateLimit
import com.piashcse.plugin.jsonBody
import com.piashcse.plugin.sellerWriteRateLimit
import com.piashcse.utils.common.ApiError
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.pageRequest
import com.piashcse.utils.extension.queryBooleanOrNull
import com.piashcse.utils.extension.requireSellerShopId
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondDeleted
import com.piashcse.utils.extension.respondOk
import com.piashcse.utils.validator.NotFoundException
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.koin.ktor.ext.inject

fun Route.couponRoutes() {
    val couponRepo: CouponRepository by inject()
    /**
     * Retrieve detailed information about a coupon by its code
     */
    get("{code}") {
        val code = call.requirePathParameter("code")
        val coupon = couponRepo.getCouponByCode(code)
        if (coupon != null) {
            call.respondOk(coupon)
        } else {
            call.respond(HttpStatusCode.NotFound, ApiError(message = Message.Coupons.NOT_FOUND, code = "NOT_FOUND"))
        }
    }.describe { summary = "Retrieve detailed information about a coupon by its code" }
}

fun Route.couponAdminRoutes() {
    val couponRepo: CouponRepository by inject()
    adminWriteRateLimit {
        /**
         * Admin - Create a new discount coupon
         *
         */
        post {
            call.respondCreated(couponRepo.createCoupon(call.receive<CouponRequest>(), call.currentUserId))
        }.describe { summary = "Admin - Create a new discount coupon" }

        /**
         * Admin - Update an existing coupon
         *
         */
        put("{id}") {
            val id = call.requirePathParameter("id")
            call.respondOk(couponRepo.updateCoupon(id, call.receive<CouponRequest>(), call.currentUserId))
        }.describe { summary = "Admin - Update an existing coupon" }

        /**
         * Admin - Delete a coupon
         *
         */
        delete("{id}") {
            val id = call.requirePathParameter("id")
            if (!couponRepo.deleteCoupon(id)) {
                throw NotFoundException(Message.Coupons.NOT_FOUND)
            }
            call.respondDeleted("Coupon deleted successfully")
        }.describe { summary = "Admin - Delete a coupon" }
    }

    /**
     * Admin - Retrieve a list of all coupons
     *
     */
    get {
        val (limit, offset) = call.pageRequest(defaultPerPage = 10)
        val active = call.queryBooleanOrNull("active")
        call.respondOk(
            couponRepo.getCoupons(
                limit = limit,
                offset = offset,
                code = call.request.queryParameters["code"],
                active = active,
            ),
        )
    }.describe { summary = "Admin - Retrieve a list of all coupons" }
}

/** Seller coupons scoped to the seller's shop. */
fun Route.sellerCouponRoutes() {
    val couponRepo: CouponRepository by inject()
    get {
        val (limit, offset) = call.pageRequest(defaultPerPage = 10)
        val shopId = requireSellerShopId(call.currentUserId)
        call.respondOk(couponRepo.getSellerCoupons(shopId, limit, offset, call.queryBooleanOrNull("active")))
    }.describe { summary = "Seller - List my shop coupons" }

    sellerWriteRateLimit {
        post {
            val shopId = requireSellerShopId(call.currentUserId)
            call.respondCreated(couponRepo.createSellerCoupon(shopId, call.receive<CouponRequest>(), call.currentUserId))
        }.describe {
            summary = "Seller - Create a shop coupon"
            requestBody { jsonBody<CouponRequest>() }
        }
        put("{id}") {
            val shopId = requireSellerShopId(call.currentUserId)
            call.respondOk(
                couponRepo.updateSellerCoupon(shopId, call.requirePathParameter("id"), call.receive<CouponRequest>(), call.currentUserId),
            )
        }.describe {
            summary = "Seller - Update a shop coupon"
            requestBody { jsonBody<CouponRequest>() }
        }
        delete("{id}") {
            val shopId = requireSellerShopId(call.currentUserId)
            if (!couponRepo.deleteSellerCoupon(shopId, call.requirePathParameter("id"))) {
                throw NotFoundException(Message.Coupons.NOT_FOUND)
            }
            call.respondDeleted("Coupon deleted successfully")
        }.describe { summary = "Seller - Delete a shop coupon" }
    }
}
