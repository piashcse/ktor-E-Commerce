package com.piashcse.feature.consent

import com.piashcse.database.entities.PolicyDocumentTable
import com.piashcse.model.request.PolicyConsentRequest
import com.piashcse.plugin.RoleManagement
import com.piashcse.utils.InvalidEnumValueException
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.requireParameters
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.consentRoutes(consentController: ConsentService) {
    authenticate(RoleManagement.CUSTOMER.role) {
            /**
             * @tag Privacy Policy Consent
             * @description Record user consent for a specific policy document
             * @operationId recordConsent
             * @body PolicyConsentRequest Consent request with policy ID
             * @response 201 User consent recorded successfully
             * @security jwtToken
             */
            post("consent") {
                val consentRequest = call.receive<PolicyConsentRequest>()

                val userId = call.currentUserId
                val policyId = consentRequest.policyId
                val userAgent = call.request.headers["User-Agent"]
                val ipAddress = call.request.origin.remoteHost

                val updatedRequest = consentRequest.copy(policyId, ipAddress, userAgent)
                call.respond(
                    HttpStatusCode.OK,
                    consentController.recordConsent(userId, updatedRequest)
                )
            }
        }

        authenticate(RoleManagement.CUSTOMER.role, RoleManagement.ADMIN.role) {
            /**
             * @tag Privacy Policy Consent
             * @description Retrieve all consent records for the authenticated user
             * @operationId getUserConsents
             * @response 200 User consents retrieved successfully
             * @security jwtToken
             */
            get {
                val userId = call.currentUserId
                call.respond(HttpStatusCode.OK, consentController.getUserConsents(userId))
            }

            /**
             * @tag Privacy Policy Consent
             * @description Check if the user has consented to a specific policy type
             * @operationId hasUserConsented
             * @path policyType (required) Type of policy to check (PRIVACY_POLICY, TERMS_AND_CONDITIONS, etc.)
             * @response 200 Consent status retrieved successfully
             * @response 400 Invalid policy type
             * @security jwtToken
             */
            get("{policyType}") {
                val policyType = call.requireParameters("policyType")
                val userId = call.currentUserId

                val policyTypeValue = runCatching {
                    PolicyDocumentTable.PolicyType.valueOf(policyType.first())
                }.getOrElse {
                    throw InvalidEnumValueException(
                        "Invalid policy type: ${policyType.first()}",
                        enumName = "PolicyType",
                        invalidValue = policyType.first()
                    )
                }

                val hasConsented = consentController.hasUserConsented(userId, policyTypeValue)
                call.respond(HttpStatusCode.OK, mapOf("hasConsented" to hasConsented))
            }
        }
    }
