package com.piashcse.feature.coupon

import com.piashcse.model.request.CouponRequest
import com.piashcse.plugin.RateLimitNames
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import io.ktor.http.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.couponRoutes() {
    val couponRepo: CouponRepository by inject()
    /**
     * @tag Coupon
     * @description Retrieve detailed information about a coupon by its code
     */
    get("{code}") {
        val code = call.requirePathParameter("code")
        val coupon = couponRepo.getCouponByCode(code)
        if (coupon != null) {
            call.respondOk(coupon)
        } else {
            call.respond(HttpStatusCode.NotFound, "Coupon not found")
        }
    }
}

fun Route.couponAdminRoutes() {
    val couponRepo: CouponRepository by inject()
    rateLimit(RateLimitName(RateLimitNames.ADMIN_WRITE)) {
        /**
         * @tag Coupon
         * @description Admin: Create a new discount coupon
         */
        post {
            call.respondCreated(couponRepo.createCoupon(call.receive<CouponRequest>()))
        }

        /**
         * @tag Coupon
         * @description Admin: Update an existing coupon
         */
        put("{id}") {
            val id = call.requirePathParameter("id")
            call.respondOk(couponRepo.updateCoupon(id, call.receive<CouponRequest>()))
        }

        /**
         * @tag Coupon
         * @description Admin: Delete a coupon
         */
        delete("{id}") {
            val id = call.requirePathParameter("id")
            call.respondOk(couponRepo.deleteCoupon(id))
        }
    }

    /**
     * @tag Coupon
     * @description Admin: Retrieve a list of all coupons
     */
    get {
        val (limit, offset) = call.paginateQueryParams(defaultPerPage = 10)
        call.respondOk(couponRepo.getCoupons(limit, offset))
    }
}
