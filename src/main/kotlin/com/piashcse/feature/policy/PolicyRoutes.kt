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
    route("/policy") {
        /**
         * @tag Privacy Policy
         * @description Retrieve all policy documents with optional type filter
         * @operationId getAllPolicies
         * @query type Filter policies by type (PRIVACY_POLICY, TERMS_AND_CONDITIONS, REFUND_POLICY, etc.)
         * @response 200 Policy documents retrieved successfully
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
         * @description Retrieve the latest policy document by policy type
         * @operationId getPolicyByType
         * @path type (required) Policy type (PRIVACY_POLICY, TERMS_AND_CONDITIONS, REFUND_POLICY, etc.)
         * @response 200 Policy document retrieved successfully
         * @response 400 Invalid policy type
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
         * @description Retrieve a specific policy document by its ID
         * @operationId getPolicyById
         * @path id (required) Unique identifier of the policy document
         * @response 200 Policy document retrieved successfully
         * @response 400 Invalid policy ID
         */
        get("detail/{id}") {
            val (id) = call.requiredParameters("id") ?: return@get
            call.respond(ApiResponse.success(policyController.getPolicyById(id), HttpStatusCode.OK))
        }
        authenticate(RoleManagement.ADMIN.role) {
            /**
             * @tag Privacy Policy
             * @description Admin-only: Create a new policy document
             * @operationId createPolicy
             * @body CreatePolicyRequest Policy creation request with type, title, content, and version
             * @response 201 Policy document created successfully
             * @security jwtToken
             */
            post {
                val createRequest = call.receive<CreatePolicyRequest>()
                call.respond(ApiResponse.success(policyController.createPolicy(createRequest), HttpStatusCode.Created))
            }

            /**
             * @tag Privacy Policy
             * @description Admin-only: Update an existing policy document
             * @operationId updatePolicy
             * @path id (required) Unique identifier of the policy to update
             * @body UpdatePolicyRequest Policy update request with title, content, and version
             * @response 200 Policy document updated successfully
             * @response 400 Invalid policy ID
             * @security jwtToken
             */
            put("{id}") {
                val (id) = call.requiredParameters("id") ?: return@put
                val updateRequest = call.receive<UpdatePolicyRequest>()
                call.respond(ApiResponse.success(policyController.updatePolicy(id, updateRequest), HttpStatusCode.OK))
            }

            /**
             * @tag Privacy Policy
             * @description Admin-only: Deactivate an existing policy document
             * @operationId deactivatePolicy
             * @path id (required) Unique identifier of the policy to deactivate
             * @response 200 Policy document deactivated successfully
             * @response 400 Invalid policy ID
             * @security jwtToken
             */
            post("deactivate/{id}") {
                val (id) = call.requiredParameters("id") ?: return@post
                call.respond(ApiResponse.success(policyController.deactivatePolicy(id), HttpStatusCode.OK))
            }
        }
    }
}