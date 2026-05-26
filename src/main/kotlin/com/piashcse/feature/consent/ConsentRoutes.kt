package com.piashcse.feature.consent

import com.piashcse.constants.UserType
import com.piashcse.database.entities.PolicyDocumentTable
import com.piashcse.model.request.PolicyConsentRequest
import com.piashcse.plugin.customerAuth
import com.piashcse.plugin.requireRole
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.validator.InvalidEnumValueException
import io.ktor.http.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Routes for managing user policy consents.
 */
fun Route.consentRoutes(consentService: ConsentService) {
    customerAuth {
        /**
         * @tag Privacy-Policy-Consent
         * @description Record user consent for a specific policy document
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
                consentService.recordConsent(userId, updatedRequest),
            )
        }
    }

    requireRole(UserType.CUSTOMER, UserType.ADMIN) {
        /**
         * @tag Privacy-Policy-Consent
         * @description Retrieve all consent records for the authenticated user
         */
        get {
            val userId = call.currentUserId
            call.respond(HttpStatusCode.OK, consentService.getUserConsents(userId))
        }

        /**
         * @tag Privacy-Policy-Consent
         * @description Check if the user has consented to a specific policy type
         */
        get("{policyType}") {
            val policyType = call.requirePathParameter("policyType")
            val userId = call.currentUserId

            val policyTypeValue =
                runCatching {
                    PolicyDocumentTable.PolicyType.valueOf(policyType)
                }.getOrElse {
                    throw InvalidEnumValueException(
                        "Invalid policy type: $policyType",
                        enumName = "PolicyType",
                        invalidValue = policyType,
                    )
                }

            val hasConsented = consentService.hasUserConsented(userId, policyTypeValue)
            call.respond(HttpStatusCode.OK, mapOf("hasConsented" to hasConsented))
        }
    }
}
