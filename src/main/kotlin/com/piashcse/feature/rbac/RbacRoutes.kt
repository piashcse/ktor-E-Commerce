@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.rbac

import com.piashcse.constants.Permission
import com.piashcse.plugin.adminWriteRateLimit
import com.piashcse.plugin.created
import com.piashcse.plugin.generalRateLimit
import com.piashcse.plugin.jsonBody
import com.piashcse.plugin.ok
import com.piashcse.plugin.requireJwtSession
import com.piashcse.plugin.requirePermission
import com.piashcse.plugin.requireRole
import com.piashcse.plugin.standardErrors
import com.piashcse.plugin.writeRateLimit
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondDeleted
import com.piashcse.utils.extension.respondOk
import io.ktor.server.request.receive
import io.ktor.server.request.requirePathParameter
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.koin.ktor.ext.inject

/** Platform role/permission administration. */
fun Route.adminRbacRoutes() {
    val service: RbacService by inject()

    requirePermission(Permission.ROLE_MANAGE) {
        generalRateLimit {
            get("permissions") {
                call.respondOk(service.listPermissions())
            }.describe {
                summary = "Admin: list permission catalog"
                responses { ok<List<PermissionResponse>>("Permissions") }
            }

            get("roles") {
                call.respondOk(service.listRoles(call.request.queryParameters["scope"]))
            }.describe {
                summary = "Admin: list roles"
                description = "Optional scope filter: GLOBAL or SHOP"
            }

            get("roles/{id}") {
                call.respondOk(service.getRole(call.requirePathParameter("id")))
            }.describe { summary = "Admin: get a role" }

            get("users/{id}/roles") {
                call.respondOk(service.getUserRoles(call.requirePathParameter("id")))
            }.describe { summary = "Admin: get a user's roles and effective permissions" }

            get("users/{id}/effective-permissions") {
                call.respondOk(service.effectivePermissions(call.requirePathParameter("id")))
            }.describe { summary = "Admin: inspect effective permissions" }
        }

        adminWriteRateLimit {
            post("roles") {
                call.respondCreated(service.createRole(call.currentUserId, call.receive<RoleRequest>()))
            }.describe {
                summary = "Admin: create a custom role"
                requestBody { jsonBody<RoleRequest>() }
                responses {
                    created<RoleResponse>("Role created")
                    standardErrors()
                }
            }

            put("roles/{id}") {
                call.respondOk(
                    service.updateRole(
                        call.currentUserId,
                        call.requirePathParameter("id"),
                        call.receive<RoleUpdateRequest>(),
                    ),
                )
            }.describe { summary = "Admin: update a role" }

            put("roles/{id}/permissions") {
                val id = call.requirePathParameter("id")
                service.setRolePermissions(call.currentUserId, id, call.receive<RolePermissionsRequest>().permissions)
                call.respondOk(service.getRole(id))
            }.describe { summary = "Admin: replace a role's permissions" }

            delete("roles/{id}") {
                service.deleteRole(call.currentUserId, call.requirePathParameter("id"))
                call.respondDeleted("Role deleted")
            }.describe { summary = "Admin: delete a custom role" }

            put("users/{id}/roles") {
                call.respondOk(
                    service.setUserRoles(
                        call.currentUserId,
                        call.requirePathParameter("id"),
                        call.receive<AssignUserRolesRequest>().roleIds,
                    ),
                )
            }.describe {
                summary = "Admin: replace a user's platform roles"
                description = "Admin role assignment only; bumps the user's authz version"
            }
        }
    }
}

/** Seller owner staff management. Authorization is enforced in the service. */
fun Route.sellerMemberRoutes() {
    val service: ShopMembershipService by inject()

    get {
        call.respondOk(service.listShopMembers(call.currentUserId))
    }.describe {
        summary = "Seller: list shop members"
        responses { ok<List<ShopMemberResponse>>("Members") }
    }

    writeRateLimit {
        post("invite") {
            call.respondCreated(service.inviteShopMember(call.currentUserId, call.receive<InviteShopMemberRequest>()))
        }.describe {
            summary = "Seller: invite a staff member"
            description = "Returns the invitation token once; deliver it by email"
            requestBody { jsonBody<InviteShopMemberRequest>() }
            responses { created<InvitationCreatedResponse>("Invitation created") }
        }

        put("{id}/roles") {
            call.respondOk(
                service.updateShopMemberRoles(
                    call.currentUserId,
                    call.requirePathParameter("id"),
                    call.receive<UpdateShopMemberRolesRequest>().roleIds,
                ),
            )
        }.describe { summary = "Seller: replace a member's roles" }

        delete("{id}") {
            service.removeShopMember(call.currentUserId, call.requirePathParameter("id"))
            call.respondDeleted("Shop member removed")
        }.describe { summary = "Seller: remove a shop member" }
    }
}

/** Any authenticated user: effective permissions for the current principal. */
fun Route.meRoutes() {
    val service: RbacService by inject()
    requireRole {
        get("permissions") {
            call.respondOk(service.effectivePermissions(call.currentUserId))
        }.describe {
            summary = "My roles and effective permissions"
            description = "Used by clients to render menus and action buttons"
        }
    }
}

/** Any authenticated user: accept a shop staff invitation. */
fun Route.invitationRoutes() {
    val service: ShopMembershipService by inject()
    requireRole {
        post("accept") {
            call.requireJwtSession()
            val body = call.receive<AcceptInvitationRequest>()
            call.respondOk(service.acceptInvitation(call.currentUserId, body.token))
        }.describe {
            summary = "Accept a shop invitation"
            requestBody { jsonBody<AcceptInvitationRequest>() }
            responses { ok<ShopMemberResponse>("Membership created") }
        }
    }
}
