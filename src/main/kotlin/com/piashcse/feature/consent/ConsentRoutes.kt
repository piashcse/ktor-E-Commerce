package com.piashcse.feature.consent

import com.piashcse.database.entities.PolicyDocumentTable
import com.piashcse.model.request.PolicyConsentRequest
import com.piashcse.plugin.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.currentUser
import com.piashcse.utils.extension.requiredParameters
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.consentRoutes(consentController: ConsentService) {
    // User consent management routes
    route("/policy-consents") {
        authenticate(RoleManagement.CUSTOMER.role) {
            /**
             * @tag Privacy Policy Consent
             * @body [PolicyConsentRequest]
             * @response 201 [Response]
             */
            post("consent") {
                val consentRequest = call.receive<PolicyConsentRequest>()

                // Automatically collect all necessary information
                val userId = call.currentUser().userId
                val policyId = consentRequest.policyId
                val userAgent = call.request.headers["User-Agent"]
                val ipAddress = call.request.origin.remoteHost

                // Set the current user ID as the consenting user
                val updatedRequest = consentRequest.copy(policyId, ipAddress, userAgent)
                call.respond(
                    ApiResponse.success(
                        consentController.recordConsent(userId, updatedRequest),
                        HttpStatusCode.Created
                    )
                )
            }
        }

        authenticate(RoleManagement.CUSTOMER.role, RoleManagement.ADMIN.role) {
            /**
             * @tag Privacy Policy Consent
             * @response 200 [Response]
             */
            get {
                val userId = call.currentUser().userId
                call.respond(ApiResponse.success(consentController.getUserConsents(userId), HttpStatusCode.OK))
            }

            /**
             * @tag Privacy Policy Consent
             * @path policyType (required)
             * @response 200 [Response]
             * @response 400
             */
            get("{policyType}") {
                val (policyType) = call.requiredParameters("policyType") ?: return@get
                val userId = call.currentUser().userId

                val hasConsented = consentController.hasUserConsented(
                    userId,
                    PolicyDocumentTable.PolicyType.valueOf(policyType)
                )
                call.respond(ApiResponse.success(mapOf("hasConsented" to hasConsented), HttpStatusCode.OK))
            }
        }
    }
}