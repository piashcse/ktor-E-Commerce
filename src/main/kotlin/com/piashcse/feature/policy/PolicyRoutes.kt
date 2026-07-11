package com.piashcse.feature.policy

import com.piashcse.constants.PolicyType
import com.piashcse.model.request.CreatePolicyRequest
import com.piashcse.plugin.RateLimitNames
import com.piashcse.utils.extension.parseEnum
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * Public policy routes.
 */
fun Route.policyRoutes() {
    val policyRepo: PolicyRepository by inject()
    /**
     * @tag Privacy-Policy
     * @description Retrieve the latest active version of a policy by type
     */
    get("{policyType}") {
        val policyType = call.requirePathParameter("policyType").parseEnum<PolicyType>("policy type")
        call.respondOk(policyRepo.getPolicyByType(policyType))
    }
}

/**
 * Admin policy management routes.
 */
fun Route.policyAdminRoutes() {
    val policyRepo: PolicyRepository by inject()
    rateLimit(RateLimitName(RateLimitNames.ADMIN_WRITE)) {
        /**
         * @tag Privacy-Policy
         * @description Admin: Create a new policy document or new version
         */
        post {
            call.respondCreated(policyRepo.createPolicy(call.receive<CreatePolicyRequest>()))
        }
    }

    /**
     * @tag Privacy-Policy
     * @description Admin: Retrieve all versions of a specific policy type
     */
    get("{policyType}/history") {
        val policyType = call.requirePathParameter("policyType").parseEnum<PolicyType>("policy type")
        call.respondOk(policyRepo.getAllPolicies(policyType))
    }
}
