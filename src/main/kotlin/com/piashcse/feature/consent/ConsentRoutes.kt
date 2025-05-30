package com.piashcse.feature.consent

import com.piashcse.database.entities.PolicyDocumentTable
import com.piashcse.model.request.PolicyConsentRequest
import com.piashcse.plugin.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.apiResponse
import com.piashcse.utils.extension.currentUser
import com.piashcse.utils.extension.requiredParameters
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.consentRoutes(consentController: ConsentService) {
    // User consent management routes
    route("policy-consents") {
        /**
         * POST request to record user consent to a policy.
         *
         * Accessible by authenticated users (CUSTOMER role).
         */
        authenticate(RoleManagement.CUSTOMER.role) {
            post("consent", {
                tags("Privacy Policy Consent")
                summary = "auth[customer]"
                request {
                    body<PolicyConsentRequest>()
                }
                apiResponse()
            }) {
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
             * GET request to retrieve all consents for a specific user.
             *
             * Accessible by the user themselves or admins.
             */
            get({
                tags("Privacy Policy Consent")
                summary = "auth[admin, customer]"
                apiResponse()
            }) {
                val userId = call.currentUser().userId
                call.respond(ApiResponse.success(consentController.getUserConsents(userId), HttpStatusCode.OK))
            }

            /**
             * GET request to check if a user has consented to a specific policy type.
             *
             * Accessible by the user themselves or admins.
             *
             * @param userId The ID of the user.
             * @param policyType The type of policy to check.
             */
            get("{policyType}", {
                tags("Privacy Policy Consent")
                summary = "auth[admin, customer]"
                request {
                    pathParameter<String>("policyType") {
                        description = "Policy type like PRIVACY_POLICY, TERMS_CONDITIONS, etc."
                        required = true
                    }
                }
                apiResponse()
            }) {
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