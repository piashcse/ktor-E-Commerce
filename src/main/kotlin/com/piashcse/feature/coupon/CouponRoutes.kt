package com.piashcse.feature.coupon

import com.piashcse.model.request.CouponRequest
import com.piashcse.utils.extension.paginationParameters
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.couponRoutes(couponService: CouponService) {
    /**
     * @tag Coupon
     * @description Retrieve detailed information about a coupon by its code
     */
    get("{code}") {
        val code = call.requirePathParameter("code")
        val coupon = couponService.getCouponByCode(code)
        if (coupon != null) {
            call.respond(HttpStatusCode.OK, coupon)
        } else {
            call.respond(HttpStatusCode.NotFound, "Coupon not found")
        }
    }
}

fun Route.couponAdminRoutes(couponService: CouponService) {
    /**
     * @tag Coupon
     * @description Admin: Create a new discount coupon
     */
    post {
        val request = call.receive<CouponRequest>()
        call.respond(HttpStatusCode.Created, couponService.createCoupon(request))
    }

    /**
     * @tag Coupon
     * @description Admin: Retrieve a list of all coupons
     */
    get {
        val (limit, offset) = call.paginationParameters(defaultLimit = 10)
        call.respond(HttpStatusCode.OK, couponService.getCoupons(limit, offset))
    }

    /**
     * @tag Coupon
     * @description Admin: Update an existing coupon
     */
    put("{id}") {
        val id = call.requirePathParameter("id")
        val request = call.receive<CouponRequest>()
        call.respond(HttpStatusCode.OK, couponService.updateCoupon(id, request))
    }

    /**
     * @tag Coupon
     * @description Admin: Delete a coupon
     */
    delete("{id}") {
        val id = call.requirePathParameter("id")
        call.respond(HttpStatusCode.OK, couponService.deleteCoupon(id))
    }
}
