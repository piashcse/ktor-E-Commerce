package com.piashcse.feature.rma

import com.piashcse.constants.Message
import com.piashcse.constants.UserType
import com.piashcse.plugin.adminWriteRateLimit
import com.piashcse.plugin.customerOnlyAuth
import com.piashcse.plugin.sellerWriteRateLimit
import com.piashcse.plugin.writeRateLimit
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.getCurrentUserType
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import com.piashcse.utils.validator.UnauthorizedException
import io.ktor.server.request.receive
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.rmaRoutes() {
    val service: RmaService by inject()
    customerOnlyAuth {
        writeRateLimit {
            /**
             * @tag RMA
             * @description Create a return or exchange request for a delivered order item
             */
            post {
                call.respondCreated(service.create(call.currentUserId, call.receive<RmaCreateRequest>()))
            }

            /**
             * @tag RMA
             * @description Customer: mark an approved RMA as shipped back
             */
            post("{id}/ship") {
                val id = call.parameters["id"] ?: throw UnauthorizedException(Message.Errors.MISSING_PARAMETER)
                call.respondOk(service.ship(id, call.currentUserId, call.receive<RmaShipRequest>().trackingNumber))
            }

            /**
             * @tag RMA
             * @description Customer: cancel a pending RMA
             */
            post("{id}/cancel") {
                val id = call.parameters["id"] ?: throw UnauthorizedException(Message.Errors.MISSING_PARAMETER)
                call.respondOk(service.cancel(id, call.currentUserId))
            }
        }

        get {
            val (limit, offset) = call.paginateQueryParams()
            call.respondOk(service.getUserRmas(call.currentUserId, limit, offset))
        }
    }
}

fun Route.rmaSellerRoutes() {
    val service: RmaService by inject()
    get {
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(service.getSellerRmas(call.currentUserId, limit, offset))
    }
    sellerWriteRateLimit {
        put("{id}/status") {
            val id = call.parameters["id"] ?: throw UnauthorizedException(Message.Errors.MISSING_PARAMETER)
            call.respondOk(
                service.updateStatus(
                    id,
                    call.receive<RmaStatusUpdateRequest>(),
                    call.currentUserId,
                    call.getCurrentUserType() ?: UserType.SELLER,
                ),
            )
        }
    }
}

fun Route.rmaAdminRoutes() {
    val service: RmaService by inject()
    get {
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(service.getAdminRmas(limit, offset, call.request.queryParameters["status"]))
    }
    adminWriteRateLimit {
        put("{id}/status") {
            val id = call.parameters["id"] ?: throw UnauthorizedException(Message.Errors.MISSING_PARAMETER)
            call.respondOk(
                service.updateStatus(
                    id,
                    call.receive<RmaStatusUpdateRequest>(),
                    call.currentUserId,
                    UserType.ADMIN,
                ),
            )
        }
    }
}
