package com.piashcse.route

import com.papsign.ktor.openapigen.route.path.auth.*
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.piashcse.controller.OrderController
import com.piashcse.models.PagingData
import com.piashcse.models.order.AddOrder
import com.piashcse.models.order.OrderId
import com.piashcse.models.orderitem.OrderItem
import com.piashcse.models.user.body.JwtTokenBody
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.Response
import com.piashcse.utils.authenticateWithJwt
import com.piashcse.utils.extension.OrderStatus
import io.ktor.http.*

fun NormalOpenAPIRoute.orderRoute(orderController: OrderController) {
    route("order") {
        authenticateWithJwt(RoleManagement.CUSTOMER.role) {
            post<Unit, Response, AddOrder, JwtTokenBody>(
                exampleRequest = AddOrder(
                    1,
                    100f,
                    100f,
                    2f,
                    orderStatus = "pending",
                    mutableListOf(OrderItem("productId", 1)),
                ),
            ) { _, orderBody ->
                orderBody.validation()
                respond(
                    ApiResponse.success(
                        orderController.createOrder(principal().userId, orderBody), HttpStatusCode.OK
                    )
                )
            }
            get<PagingData, Response, JwtTokenBody> { params ->
                params.validation()
                respond(ApiResponse.success(orderController.getOrders(principal().userId, params), HttpStatusCode.OK))
            }
            route("/payment").put<OrderId, Response, Unit, JwtTokenBody> { params,_  ->
                params.validation()
                respond(
                    ApiResponse.success(
                        orderController.updateOrder(principal().userId, params, OrderStatus.PAID),
                        HttpStatusCode.OK
                    )
                )
            }
            route("/cancel").put<OrderId, Response, Unit, JwtTokenBody> { params, _ ->
                params.validation()
                respond(
                    ApiResponse.success(
                        orderController.updateOrder(principal().userId, params, OrderStatus.CANCELED), HttpStatusCode.OK
                    )
                )
            }
            route("/receive").put<OrderId, Response, Unit, JwtTokenBody> { params, _ ->
                params.validation()
                respond(
                    ApiResponse.success(
                        orderController.updateOrder(principal().userId, params, OrderStatus.RECEIVED), HttpStatusCode.OK
                    )
                )
            }
        }
        authenticateWithJwt(RoleManagement.SELLER.role) {
            route("/confirm").put<OrderId, Response, Unit, JwtTokenBody> { params, _ ->
                params.validation()
                respond(
                    ApiResponse.success(
                        orderController.updateOrder(principal().userId, params, OrderStatus.CONFIRMED),
                        HttpStatusCode.OK
                    )
                )
            }
            route("/deliver").put<OrderId, Response, Unit, JwtTokenBody> { params, _ ->
                params.validation()
                respond(
                    ApiResponse.success(
                        orderController.updateOrder(principal().userId, params, OrderStatus.DELIVERED),
                        HttpStatusCode.OK
                    )
                )
            }
        }
    }
}