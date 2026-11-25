package com.piashcse.feature.rbac

import com.piashcse.constants.RoleScope
import com.piashcse.database.entities.AuthPermissionTable
import com.piashcse.database.entities.AuthRoleDAO
import com.piashcse.database.entities.AuthRoleTable
import com.piashcse.event.AdminAudit
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

/** Role and permission catalogue CRUD. */
class RbacRoleService {
    suspend fun listPermissions(): List<PermissionResponse> =
        query {
            AuthPermissionTable.selectAll()
                .orderBy(AuthPermissionTable.resource to SortOrder.ASC)
                .map {
                    PermissionResponse(
                        id = it[AuthPermissionTable.id].value,
                        code = it[AuthPermissionTable.code],
                        resource = it[AuthPermissionTable.resource],
                        action = it[AuthPermissionTable.action],
                        description = it[AuthPermissionTable.description],
                    )
                }
        }

    // ── Roles ────────────────────────────────────────────────────────────
    suspend fun listRoles(scopeType: String? = null): List<RoleResponse> =
        query {
            val roles =
                AuthRoleTable.selectAll()
                    .also { q -> scopeType?.let { q.andWhere { AuthRoleTable.scopeType eq it.uppercase() } } }
                    .orderBy(AuthRoleTable.scopeType to SortOrder.ASC, AuthRoleTable.name to SortOrder.ASC)
                    .map { AuthRoleDAO.wrapRow(it).toRoleResponse() }
            val permissionCodes = permissionCodesByRole()
            roles.map { it.copy(permissions = permissionCodes[it.id].orEmpty()) }
        }

    suspend fun createRole(
        actorUserId: String,
        request: RoleRequest,
    ): RoleResponse {
        validateRoleCode(request.code)
        val scope = request.scopeType.uppercase()
        if (scope !in RoleScope.all) throw ValidationException("scopeType must be GLOBAL or SHOP")
        val requested = request.permissions.distinct()
        ensureCanGrant(actorUserId, requested)

        val role =
            query {
                if (AuthRoleTable.selectAll().where { AuthRoleTable.code eq request.code.lowercase() }.any()) {
                    throw ValidationException("Role code already exists")
                }
                AuthRoleDAO.new {
                    code = request.code.lowercase()
                    name = request.name.trim().ifBlank { throw ValidationException("Role name is required") }
                    description = request.description
                    scopeType = scope
                    isSystem = false
                }.also { replaceRolePermissionsTx(it, requested) }
            }
        AdminAudit.publish(actorUserId, "RBAC_ROLE_CREATED", "ROLE", role.id.value, role.code)
        return getRole(role.id.value)
    }

    suspend fun updateRole(
        actorUserId: String,
        roleId: String,
        request: RoleUpdateRequest,
    ): RoleResponse {
        query {
            val role = AuthRoleDAO.findById(roleId) ?: roleId.throwNotFound("Role")
            if (role.isSystem && !isSuperAdminTx(actorUserId)) {
                throw ForbiddenException("System roles can only be changed by a super admin")
            }
            request.name?.trim()?.takeIf { it.isNotBlank() }?.let { role.name = it }
            request.description?.let { role.description = it }
        }
        AdminAudit.publish(actorUserId, "RBAC_ROLE_UPDATED", "ROLE", roleId)
        return getRole(roleId)
    }

    suspend fun deleteRole(
        actorUserId: String,
        roleId: String,
    ) {
        query {
            val role = AuthRoleDAO.findById(roleId) ?: roleId.throwNotFound("Role")
            if (role.isSystem) throw ValidationException("System roles cannot be deleted")
            bumpUsersForRoleTx(role.id.value)
            role.delete()
        }
        AdminAudit.publish(actorUserId, "RBAC_ROLE_DELETED", "ROLE", roleId)
    }

    suspend fun setRolePermissions(
        actorUserId: String,
        roleId: String,
        permissionCodes: List<String>,
    ) {
        val requested = permissionCodes.distinct()
        ensureCanGrant(actorUserId, requested)
        query {
            val role = AuthRoleDAO.findById(roleId) ?: roleId.throwNotFound("Role")
            if (role.isSystem && !isSuperAdminTx(actorUserId)) {
                throw ForbiddenException("System roles can only be changed by a super admin")
            }
            replaceRolePermissionsTx(role, requested)
        }
    }

    /** Must be called inside a transaction. */
    suspend fun getRole(roleId: String): RoleResponse =
        query {
            val role = AuthRoleDAO.findById(roleId) ?: roleId.throwNotFound("Role")
            role.toRoleResponse(permissions = rolePermissionCodes(listOf(roleId)).sorted())
        }
}
