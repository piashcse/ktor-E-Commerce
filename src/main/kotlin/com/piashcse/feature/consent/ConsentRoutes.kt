@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

@file:Suppress("MatchingDeclarationName")

package com.piashcse.feature.consent

import com.piashcse.constants.Message
import com.piashcse.constants.PolicyType
import com.piashcse.constants.UserType
import com.piashcse.model.request.PolicyConsentRequest
import com.piashcse.plugin.customerAuth
import com.piashcse.plugin.requireRole
import com.piashcse.plugin.writeRateLimit
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.getCurrentUserType
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.parseEnum
import com.piashcse.utils.extension.respondOk
import com.piashcse.utils.validator.UnauthorizedException
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

@Serializable
data class ConsentCheckResponse(val hasConsented: Boolean)

/**
 * Routes for managing user policy consents.
 */
fun Route.consentRoutes() {
    val consentRepo: ConsentRepository by inject()
    customerAuth {
        writeRateLimit {
            /**
             * Record user consent for a specific policy document
             */
            post("consent") {
                call.respondOk(
                    call.receive<PolicyConsentRequest>().let {
                        consentRepo.recordConsent(
                            call.currentUserId,
                            it.copy(it.policyId, call.request.origin.remoteHost, call.request.headers["User-Agent"]),
                        )
                    },
                )
            }.describe { summary = "Record user consent for a specific policy document" }
        }
    }

    requireRole(UserType.CUSTOMER, UserType.ADMIN) {
        /**
         * Retrieve all consent records for the authenticated user
         */
        get {
            val (limit, offset) = call.paginateQueryParams()
            call.respondOk(consentRepo.getUserConsents(call.currentUserId, limit, offset))
        }.describe { summary = "Retrieve all consent records for the authenticated user" }

        /**
         * Check if the user has consented to a specific policy type
         */
        get("{policyType}") {
            call.respondOk(
                ConsentCheckResponse(
                    consentRepo.hasUserConsented(
                        call.currentUserId,
                        call.requirePathParameter("policyType").parseEnum<PolicyType>("policy type"),
                    ),
                ),
            )
        }.describe { summary = "Check if the user has consented to a specific policy type" }

        /**
         * Revoke a consent record (owner or admin)
         */
        delete("{id}") {
            consentRepo.revokeConsent(
                call.requirePathParameter("id"),
                call.currentUserId,
                call.getCurrentUserType() ?: throw UnauthorizedException(Message.Errors.UNAUTHORIZED),
            )
            call.respondOk(com.piashcse.utils.common.MessageResponse("Consent revoked successfully"))
        }.describe { summary = "Revoke a consent record (owner or admin)" }
    }
}

/**
 * Admin policy consent routes.
 */
fun Route.consentAdminRoutes() {
    val consentRepo: ConsentRepository by inject()
    /**
     * Admin - Retrieve consent records filtered by user and/or policy type
     *
     */
    get {
        val userId = call.request.queryParameters["userId"]
        val policyType = call.request.queryParameters["policyType"]?.parseEnum<PolicyType>("policy type")
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(consentRepo.listConsentsByUser(userId, policyType, limit, offset))
    }.describe { summary = "Admin - Retrieve consent records filtered by user and/or policy type" }
}
