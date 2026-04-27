package com.piashcse.feature.policy

import com.piashcse.database.entities.PolicyDocumentTable
import com.piashcse.model.request.CreatePolicyRequest
import com.piashcse.plugin.adminAuth
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Public policy routes.
 */
fun Route.policyRoutes(policyController: PolicyService) {
    /**
     * @tag Privacy Policy
     * @description Retrieve the latest active version of a policy by type
     */
    get("{policyType}") {
        val policyTypeParam = call.parameters["policyType"] ?: return@get call.respond(HttpStatusCode.BadRequest, "policyType is required")
        val policyType = try {
            PolicyDocumentTable.PolicyType.valueOf(policyTypeParam.uppercase())
        } catch (e: IllegalArgumentException) {
            return@get call.respond(HttpStatusCode.BadRequest, "Invalid policy type")
        }
        
        val policy = policyController.getPolicyByType(policyType)
        call.respond(HttpStatusCode.OK, policy)
    }
}

/**
 * Admin policy management routes.
 */
fun Route.policyAdminRoutes(policyController: PolicyService) {
    adminAuth {
        /**
         * @tag Privacy Policy
         * @description Admin: Create a new policy document or new version
         */
        post {
            val requestBody = call.receive<CreatePolicyRequest>()
            call.respond(HttpStatusCode.Created, policyController.createPolicy(requestBody))
        }

        /**
         * @tag Privacy Policy
         * @description Admin: Retrieve all versions of a specific policy type
         */
        get("{policyType}/history") {
            val policyTypeParam = call.parameters["policyType"] ?: return@get call.respond(HttpStatusCode.BadRequest, "policyType is required")
            val policyType = try {
                PolicyDocumentTable.PolicyType.valueOf(policyTypeParam.uppercase())
            } catch (e: IllegalArgumentException) {
                return@get call.respond(HttpStatusCode.BadRequest, "Invalid policy type")
            }
            call.respond(HttpStatusCode.OK, policyController.getAllPolicies(policyType))
        }
    }
}
