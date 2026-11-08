package com.piashcse.feature.coupon

import com.piashcse.constants.Message
import com.piashcse.model.request.CouponRequest
import com.piashcse.plugin.adminWriteRateLimit
import com.piashcse.utils.common.ApiError
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import com.piashcse.utils.validator.NotFoundException
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.couponRoutes() {
    val couponRepo: CouponRepository by inject()
    /**
     * Retrieve detailed information about a coupon by its code
     *
     * @tag Coupon
     * @description Retrieve detailed information about a coupon by its code
     */
    get("{code}") {
        val code = call.requirePathParameter("code")
        val coupon = couponRepo.getCouponByCode(code)
        if (coupon != null) {
            call.respondOk(coupon)
        } else {
            call.respond(HttpStatusCode.NotFound, ApiError(message = Message.Coupons.NOT_FOUND, code = "NOT_FOUND"))
        }
    }
}

fun Route.couponAdminRoutes() {
    val couponRepo: CouponRepository by inject()
    adminWriteRateLimit {
        /**
         * Admin: Create a new discount coupon
         *
         * @tag Coupon
         * @description Admin: Create a new discount coupon
         */
        post {
            call.respondCreated(couponRepo.createCoupon(call.receive<CouponRequest>(), call.currentUserId))
        }

        /**
         * Admin: Update an existing coupon
         *
         * @tag Coupon
         * @description Admin: Update an existing coupon
         */
        put("{id}") {
            val id = call.requirePathParameter("id")
            call.respondOk(couponRepo.updateCoupon(id, call.receive<CouponRequest>(), call.currentUserId))
        }

        /**
         * Admin: Delete a coupon
         *
         * @tag Coupon
         * @description Admin: Delete a coupon
         */
        delete("{id}") {
            val id = call.requirePathParameter("id")
            if (!couponRepo.deleteCoupon(id)) {
                throw NotFoundException(Message.Coupons.NOT_FOUND)
            }
            call.respondOk(com.piashcse.utils.common.MessageResponse("Coupon deleted successfully"))
        }
    }

    /**
     * Admin: Retrieve a list of all coupons
     *
     * @tag Coupon
     * @description Admin: Retrieve a list of all coupons
     */
    get {
        val (limit, offset) = call.paginateQueryParams(defaultPerPage = 10)
        val active = call.request.queryParameters["active"]?.toBooleanStrictOrNull()
        call.respondOk(
            couponRepo.getCoupons(
                limit = limit,
                offset = offset,
                code = call.request.queryParameters["code"],
                active = active,
            ),
        )
    }
}
