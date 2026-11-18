package com.piashcse.feature.rbac

import com.piashcse.constants.RoleScope
import kotlinx.serialization.Serializable

@Serializable
data class PermissionResponse(
    val id: String,
    val code: String,
    val resource: String,
    val action: String,
    val description: String?,
)

@Serializable
data class RoleResponse(
    val id: String,
    val code: String,
    val name: String,
    val description: String?,
    val scopeType: String,
    val isSystem: Boolean,
    val permissions: List<String> = emptyList(),
)

@Serializable
data class RoleRequest(
    val code: String,
    val name: String,
    val description: String? = null,
    val scopeType: String = RoleScope.GLOBAL,
    val permissions: List<String> = emptyList(),
)

@Serializable
data class RoleUpdateRequest(
    val name: String? = null,
    val description: String? = null,
)

@Serializable
data class RolePermissionsRequest(val permissions: List<String>)

@Serializable
data class RoleRef(
    val id: String,
    val code: String,
    val name: String,
    val scopeType: String,
)

@Serializable
data class MembershipRef(
    val shopId: String,
    val shopName: String?,
    val isOwner: Boolean,
    val roles: List<String> = emptyList(),
)

@Serializable
data class EffectivePermissionsResponse(
    val userId: String,
    val email: String,
    val accountType: String,
    val roles: List<RoleRef> = emptyList(),
    val memberships: List<MembershipRef> = emptyList(),
    val permissions: List<String> = emptyList(),
)

@Serializable
data class AssignUserRolesRequest(val roleIds: List<String>)

@Serializable
data class ShopMemberResponse(
    val id: String,
    val shopId: String,
    val userId: String,
    val email: String,
    val status: String,
    val isOwner: Boolean,
    val roles: List<String> = emptyList(),
)

@Serializable
data class InviteShopMemberRequest(
    val email: String,
    val roleIds: List<String>,
)

@Serializable
data class UpdateShopMemberRolesRequest(val roleIds: List<String>)

@Serializable
data class InvitationCreatedResponse(
    val id: String,
    val shopId: String,
    val email: String,
    val expiresAt: String,
    /** Returned once. Deliver by email in production. */
    val token: String,
)

@Serializable
data class AcceptInvitationRequest(val token: String)
