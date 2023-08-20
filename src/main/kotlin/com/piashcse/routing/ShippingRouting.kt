package com.piashcse.routing

import com.papsign.ktor.openapigen.route.path.auth.*
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.piashcse.controller.ShippingController
import com.piashcse.models.shipping.AddShipping
import com.piashcse.models.shipping.OrderId
import com.piashcse.models.shipping.UpdateShipping
import com.piashcse.models.user.body.JwtTokenBody
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.Response
import com.piashcse.utils.authenticateWithJwt
import io.ktor.http.*

fun NormalOpenAPIRoute.shippingRoute(shippingController: ShippingController) {
    route("shipping") {
        authenticateWithJwt(RoleManagement.USER.role) {
            post<Unit, Response, AddShipping, JwtTokenBody> { _, requestBody ->
                requestBody.validation()
                respond(
                    ApiResponse.success(
                        shippingController.addShipping(principal().userId, requestBody), HttpStatusCode.OK
                    )
                )
            }
            get<OrderId, Response, JwtTokenBody> { params ->
                respond(
                    ApiResponse.success(
                        shippingController.getShipping(principal().userId, params.orderId), HttpStatusCode.OK
                    )
                )
            }
            route("/{orderId}").put<UpdateShipping, Response, Unit, JwtTokenBody> { params, _ ->
                params.validation()
                shippingController.updateShipping(
                    principal().userId, params
                ).let {
                    respond(ApiResponse.success(it, HttpStatusCode.OK))
                }
            }
            delete<OrderId, Response, JwtTokenBody> { params ->
                params.validation()
                respond(
                    ApiResponse.success(
                        shippingController.deleteShipping(principal().userId, params.orderId), HttpStatusCode.OK
                    )
                )
            }
        }
    }
}