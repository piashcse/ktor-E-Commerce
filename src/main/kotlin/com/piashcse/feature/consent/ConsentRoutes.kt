package com.piashcse.feature.consent

import com.piashcse.constants.PolicyType
import com.piashcse.constants.UserType
import com.piashcse.model.request.PolicyConsentRequest
import com.piashcse.plugin.RateLimitNames
import com.piashcse.plugin.customerAuth
import com.piashcse.plugin.requireRole
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.parseEnum
import com.piashcse.utils.extension.respondOk
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * Routes for managing user policy consents.
 */
fun Route.consentRoutes() {
    val consentRepo: ConsentRepository by inject()
    customerAuth {
        rateLimit(RateLimitName(RateLimitNames.WRITE)) {
            /**
             * @tag Privacy-Policy-Consent
             * @description Record user consent for a specific policy document
             */
            post("consent") {
                call.respondOk(call.receive<PolicyConsentRequest>().let {
                    consentRepo.recordConsent(call.currentUserId, it.copy(it.policyId, call.request.origin.remoteHost, call.request.headers["User-Agent"]))
                })
            }
        }
    }

    requireRole(UserType.CUSTOMER, UserType.ADMIN) {
        /**
         * @tag Privacy-Policy-Consent
         * @description Retrieve all consent records for the authenticated user
         */
        get {
            call.respondOk(consentRepo.getUserConsents(call.currentUserId))
        }

        /**
         * @tag Privacy-Policy-Consent
         * @description Check if the user has consented to a specific policy type
         */
        get("{policyType}") {
            call.respondOk(mapOf("hasConsented" to consentRepo.hasUserConsented(call.currentUserId, call.requirePathParameter("policyType").parseEnum<PolicyType>("policy type"))))
        }
    }
}
