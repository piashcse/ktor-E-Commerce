package com.piashcse.feature.policy

import com.piashcse.database.entities.PolicyDocumentTable
import com.piashcse.model.request.CreatePolicyRequest
import com.piashcse.model.request.UpdatePolicyRequest
import com.piashcse.plugin.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.requiredParameters
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Defines routes for managing policy documents including privacy policy, terms and conditions, refund policy, etc.
 * Different routes are available based on user roles (CUSTOMER, ADMIN).
 *
 * @param policyController The controller handling policy-related operations.
 */
fun Route.policyRoutes(policyController: PolicyService) {
    // Main route for policy management
    route("/policy") {
        /**
         * @tag Privacy Policy
         * @query type
         * @response 200 [Response]
         */
        get {
            val type = call.request.queryParameters["type"]
            val policyType = type?.let {
                PolicyDocumentTable.PolicyType.valueOf(type)
            }
            call.respond(
                ApiResponse.success(
                    policyController.getAllPolicies(policyType),
                    HttpStatusCode.OK
                )
            )
        }

        /**
         * @tag Privacy Policy
         * @path type (required)
         * @response 200 [Response]
         * @response 400
         */
        get("{type}") {
            val (type) = call.requiredParameters("type") ?: return@get
            call.respond(
                ApiResponse.success(
                    policyController.getPolicyByType(
                        PolicyDocumentTable.PolicyType.valueOf(
                            type
                        )
                    ), HttpStatusCode.OK
                )
            )
        }

        /**
         * @tag Privacy Policy
         * @path id (required)
         * @response 200 [Response]
         * @response 400
         */
        get("detail/{id}") {
            val (id) = call.requiredParameters("id") ?: return@get
            call.respond(ApiResponse.success(policyController.getPolicyById(id), HttpStatusCode.OK))
        }

        // Admin routes for managing policies
        authenticate(RoleManagement.ADMIN.role) {
            /**
             * @tag Privacy Policy
             * @body [CreatePolicyRequest]
             * @response 201 [Response]
             */
            post {
                val createRequest = call.receive<CreatePolicyRequest>()
                call.respond(ApiResponse.success(policyController.createPolicy(createRequest), HttpStatusCode.Created))
            }

            /**
             * @tag Privacy Policy
             * @path id
             * @body [UpdatePolicyRequest]
             * @response 200 [Response]
             * @response 400
             */
            put("{id}") {
                val (id) = call.requiredParameters("id") ?: return@put
                val updateRequest = call.receive<UpdatePolicyRequest>()
                call.respond(ApiResponse.success(policyController.updatePolicy(id, updateRequest), HttpStatusCode.OK))
            }

            /**
             * @tag Privacy Policy
             * @path id
             * @response 200 [Response]
             * @response 400
             */
            post("deactivate/{id}") {
                val (id) = call.requiredParameters("id") ?: return@post
                call.respond(ApiResponse.success(policyController.deactivatePolicy(id), HttpStatusCode.OK))
            }
        }
    }
}