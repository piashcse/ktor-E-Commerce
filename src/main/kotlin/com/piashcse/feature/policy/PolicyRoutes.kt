package com.piashcse.feature.policy

import com.piashcse.constants.PolicyType
import com.piashcse.model.request.CreatePolicyRequest
import com.piashcse.model.request.UpdatePolicyRequest
import com.piashcse.plugin.adminWriteRateLimit
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.parseEnum
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * Public policy routes.
 */
fun Route.policyRoutes() {
    val policyRepo: PolicyRepository by inject()
    /**
     * Retrieve the latest active version of a policy by type
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
    adminWriteRateLimit {
        /**
         * Admin - Create a new policy document or new version
         *
         */
        post {
            call.respondCreated(policyRepo.createPolicy(call.receive<CreatePolicyRequest>()))
        }

        /**
         * Admin - Update an existing policy document
         *
         */
        put("{id}") {
            val id = call.requirePathParameter("id")
            call.respondOk(policyRepo.updatePolicy(id, call.receive<UpdatePolicyRequest>()))
        }

        /**
         * Admin - Deactivate a policy document
         *
         */
        post("deactivate/{id}") {
            val id = call.requirePathParameter("id")
            call.respondOk(policyRepo.deactivatePolicy(id))
        }
    }

    /**
     * Admin - Retrieve a policy document by ID
     *
     */
    get("by-id/{id}") {
        val id = call.requirePathParameter("id")
        call.respondOk(policyRepo.getPolicyById(id))
    }

    /**
     * Admin - Retrieve all versions of a specific policy type
     *
     */
    get("{policyType}/history") {
        val policyType = call.requirePathParameter("policyType").parseEnum<PolicyType>("policy type")
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(policyRepo.getAllPolicies(policyType, limit, offset))
    }
}
