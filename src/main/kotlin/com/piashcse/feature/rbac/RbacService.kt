package com.piashcse.feature.rbac

import com.piashcse.database.entities.UserDAO

/** Facade over role catalogue and user permission assignment. */
class RbacService {
    private val roles = RbacRoleService()
    private val users = RbacUserService()

    suspend fun permissionsForUser(userId: String): Set<String> = users.permissionsForUser(userId)

    suspend fun permissionsForUser(user: UserDAO): Set<String> = users.permissionsForUser(user)

    suspend fun canGrantRole(
        actorUserId: String,
        roleCode: String,
    ): Boolean = users.canGrantRole(actorUserId, roleCode)

    suspend fun listPermissions(): List<PermissionResponse> = roles.listPermissions()

    suspend fun listRoles(scopeType: String? = null): List<RoleResponse> = roles.listRoles(scopeType)

    suspend fun createRole(
        actorUserId: String,
        request: RoleRequest,
    ): RoleResponse = roles.createRole(actorUserId, request)

    suspend fun updateRole(
        actorUserId: String,
        roleId: String,
        request: RoleUpdateRequest,
    ): RoleResponse = roles.updateRole(actorUserId, roleId, request)

    suspend fun deleteRole(
        actorUserId: String,
        roleId: String,
    ) = roles.deleteRole(actorUserId, roleId)

    suspend fun setRolePermissions(
        actorUserId: String,
        roleId: String,
        permissionCodes: List<String>,
    ) = roles.setRolePermissions(actorUserId, roleId, permissionCodes)

    suspend fun getRole(roleId: String): RoleResponse = roles.getRole(roleId)

    suspend fun getUserRoles(userId: String): EffectivePermissionsResponse = users.getUserRoles(userId)

    suspend fun setUserRoles(
        actorUserId: String,
        userId: String,
        roleIds: List<String>,
    ): EffectivePermissionsResponse = users.setUserRoles(actorUserId, userId, roleIds)

    suspend fun effectivePermissions(userId: String): EffectivePermissionsResponse = users.effectivePermissions(userId)
}
