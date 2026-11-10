@file:Suppress("MatchingDeclarationName")

package com.piashcse.feature.kyc
import com.piashcse.constants.KycStatus
import com.piashcse.plugin.adminWriteRateLimit
import com.piashcse.plugin.sellerWriteRateLimit
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.parseEnum
import com.piashcse.utils.extension.respondOk
import com.piashcse.utils.validator.ValidationException
import io.ktor.server.request.receive
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

@Serializable
data class KycRejectRequest(val reason: String)

fun Route.sellerKycRoutes() {
    val service: KycService by inject()
    get {
        call.respondOk(service.get(call.currentUserId))
    }
    sellerWriteRateLimit {
        post {
            call.respondOk(service.submit(call.currentUserId, call.receive<SellerKycSubmitRequest>()))
        }
    }
}

fun Route.adminKycRoutes() {
    val service: KycService by inject()
    get {
        val (limit, offset) = call.paginateQueryParams()
        val status = call.request.queryParameters["status"]?.parseEnum<KycStatus>("kyc status")
        call.respondOk(service.list(status, limit, offset))
    }
    adminWriteRateLimit {
        put("{sellerId}/verify") {
            val sellerId =
                call.parameters["sellerId"]
                    ?: throw ValidationException("sellerId is required")
            call.respondOk(service.verify(sellerId))
        }
        put("{sellerId}/reject") {
            val sellerId =
                call.parameters["sellerId"]
                    ?: throw ValidationException("sellerId is required")
            call.respondOk(service.reject(sellerId, call.receive<KycRejectRequest>().reason))
        }
    }
}
