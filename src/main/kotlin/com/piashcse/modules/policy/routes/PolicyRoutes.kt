package com.piashcse.modules.policy.routes

import com.piashcse.modules.policy.controller.PolicyController
import com.piashcse.database.entities.PolicyDocumentTable
import com.piashcse.database.models.policy.CreatePolicyRequest
import com.piashcse.database.models.policy.UpdatePolicyRequest
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.apiResponse
import com.piashcse.utils.extension.requiredParameters
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktoropenapi.put
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
fun Route.policyRoutes(policyController: PolicyController) {
    // Main route for policy management
    route("policy") {
        // Public routes for accessing policies - no authentication required

        /**
         * GET request to retrieve all policies, optionally filtered by type.
         *
         * Accessible by anyone.
         *
         * @param type Optional filter by policy type (PRIVACY_POLICY, TERMS_CONDITIONS, etc.)
         */
        get({
            tags("Privacy Policy")
            request {
                queryParameter<String>("type") {
                    description = "Filter policies by type"
                    required = false
                }
            }
            apiResponse()
        }) {
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
         * GET request to retrieve the active policy of a specific type.
         *
         * Accessible by anyone.
         *
         * @param type The policy type (PRIVACY_POLICY, TERMS_CONDITIONS, etc.)
         */
        get("{type}", {
            tags("Privacy Policy")
            request {
                pathParameter<String>("type") {
                    description = "Policy type like PRIVACY_POLICY, TERMS_CONDITIONS, etc."
                    required = true
                }
            }
            apiResponse()
        }) {
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
         * GET request to retrieve a specific policy by ID.
         *
         * Accessible by anyone.
         *
         * @param id The unique identifier of the policy.
         */
        get("detail/{id}", {
            tags("Privacy Policy")
            request {
                pathParameter<String>("id") {
                    description = "Policy ID"
                    required = true
                }
            }
            apiResponse()
        }) {
            val (id) = call.requiredParameters("id") ?: return@get
            call.respond(ApiResponse.success(policyController.getPolicyById(id), HttpStatusCode.OK))
        }

        // Admin routes for managing policies
        authenticate(RoleManagement.ADMIN.role) {
            /**
             * POST request to create a new policy document.
             *
             * Accessible by admins only.
             *
             * @param createPolicyRequest The details of the policy to create.
             */
            post({
                tags("Privacy Policy")
                summary = "auth[admin]"
                request {
                    body<CreatePolicyRequest>()
                }
                apiResponse()
            }) {
                val createRequest = call.receive<CreatePolicyRequest>()
                call.respond(ApiResponse.success(policyController.createPolicy(createRequest), HttpStatusCode.Created))
            }

            /**
             * PUT request to update an existing policy document.
             *
             * Accessible by admins only.
             *
             * @param id The ID of the policy to update.
             * @param updatePolicyRequest The parameters to update.
             */
            put("{id}", {
                tags("Privacy Policy")
                summary = "auth[admin]"
                request {
                    pathParameter<String>("id") {
                        description = "Policy ID"
                        required = true
                    }
                    body<UpdatePolicyRequest>()
                }
                apiResponse()
            }) {
                val (id) = call.requiredParameters("id") ?: return@put
                val updateRequest = call.receive<UpdatePolicyRequest>()
                call.respond(ApiResponse.success(policyController.updatePolicy(id, updateRequest), HttpStatusCode.OK))
            }

            /**
             * POST request to deactivate a policy document.
             *
             * Accessible by admins only.
             *
             * @param id The ID of the policy to deactivate.
             */
            post("deactivate/{id}", {
                tags("Privacy Policy")
                summary = "auth[admin]"
                request {
                    pathParameter<String>("id") {
                        description = "Policy ID"
                        required = true
                    }
                }
                apiResponse()
            }) {
                val (id) = call.requiredParameters("id") ?: return@post
                call.respond(ApiResponse.success(policyController.deactivatePolicy(id), HttpStatusCode.OK))
            }
        }
    }
}