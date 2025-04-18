package com.piashcse.route

import com.piashcse.controller.PolicyController
import com.piashcse.entities.PolicyDocumentTable
import com.piashcse.models.policy.CreatePolicyRequest
import com.piashcse.models.policy.PolicyConsentRequest
import com.piashcse.models.policy.UpdatePolicyRequest
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.apiResponse
import com.piashcse.utils.extension.currentUser
import com.piashcse.utils.extension.requiredParameters
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktoropenapi.put
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Defines routes for managing policy documents including privacy policy, terms and conditions, refund policy, etc.
 * Different routes are available based on user roles (CUSTOMER, ADMIN).
 *
 * @param policyController The controller handling policy-related operations.
 */
fun Route.policyRoute(policyController: PolicyController) {
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

        /**
         * POST request to record user consent to a policy.
         *
         * Accessible by authenticated users (CUSTOMER role).
         */
        authenticate(RoleManagement.CUSTOMER.role) {
            post("consent", {
                tags("Privacy Policy")
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
                        policyController.recordConsent(userId, updatedRequest),
                        HttpStatusCode.Created
                    )
                )
            }
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

    // User consent management routes
    route("user-consents") {
        authenticate(RoleManagement.CUSTOMER.role, RoleManagement.ADMIN.role) {
            /**
             * GET request to retrieve all consents for a specific user.
             *
             * Accessible by the user themselves or admins.
             */
            get({
                tags("Privacy Policy")
                summary = "auth[admin, customer]"
                apiResponse()
            }) {
                val userId = call.currentUser().userId
                call.respond(ApiResponse.success(policyController.getUserConsents(userId), HttpStatusCode.OK))
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
                tags("Privacy Policy")
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

                val hasConsented = policyController.hasUserConsented(
                    userId,
                    PolicyDocumentTable.PolicyType.valueOf(policyType)
                )
                call.respond(ApiResponse.success(mapOf("hasConsented" to hasConsented), HttpStatusCode.OK))
            }
        }
    }
}