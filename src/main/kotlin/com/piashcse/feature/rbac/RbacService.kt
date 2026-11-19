package com.piashcse.feature.rbac

import com.piashcse.constants.MembershipStatus
import com.piashcse.constants.RbacDefaults
import com.piashcse.constants.RoleScope
import com.piashcse.constants.UserType
import com.piashcse.constants.defaultPermissions
import com.piashcse.database.entities.AuthPermissionTable
import com.piashcse.database.entities.AuthRoleDAO
import com.piashcse.database.entities.AuthRolePermissionTable
import com.piashcse.database.entities.AuthRoleTable
import com.piashcse.database.entities.AuthUserRoleTable
import com.piashcse.database.entities.ShopMemberRoleTable
import com.piashcse.database.entities.ShopMemberTable
import com.piashcse.database.entities.ShopTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.UserTable
import com.piashcse.event.AdminAudit
import com.piashcse.service.CacheService
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.entityIDs
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll

/**
 * Platform authorization: permission resolution, roles and admin assignments.
 * Shop staff memberships live in [ShopMembershipService].
 *
 * Effective permissions are the union of active role assignments. Customers
 * and sellers with no assignments fall back to the legacy
 * [UserType.defaultPermissions] mapping; platform admins do not, so an
 * orphaned admin can never inherit full access.
 */
class RbacService {
    suspend fun permissionsForUser(userId: String): Set<String> {
        val user = query { UserDAO.findById(userId) } ?: return emptySet()
        return permissionsForUser(user)
    }

    suspend fun permissionsForUser(user: UserDAO): Set<String> {
        val cacheKey = "authz:user:${user.id.value}:${user.authzVersion}"
        CacheService.cache.get<Set<String>>(cacheKey)?.let { return it }

        val (hasAssignments, codes) =
            query {
                val platformRoleIds =
                    AuthUserRoleTable.selectAll()
                        .where { AuthUserRoleTable.userId eq user.id }
                        .map { it[AuthUserRoleTable.roleId].value }
                val memberIds =
                    ShopMemberTable.selectAll()
                        .where { (ShopMemberTable.userId eq user.id) and (ShopMemberTable.status eq MembershipStatus.ACTIVE) }
                        .map { it[ShopMemberTable.id].value }

                val roleIds =
                    platformRoleIds +
                        ShopMemberRoleTable.selectAll()
                            .where { ShopMemberRoleTable.memberId inList memberIds.entityIDs(ShopMemberTable) }
                            .map { it[ShopMemberRoleTable.roleId].value }

                val permissionIds =
                    if (roleIds.isEmpty()) {
                        emptyList()
                    } else {
                        AuthRolePermissionTable.selectAll()
                            .where { AuthRolePermissionTable.roleId inList roleIds.entityIDs(AuthRoleTable) }
                            .map { it[AuthRolePermissionTable.permissionId].value }
                    }

                val resolved =
                    if (permissionIds.isEmpty()) {
                        emptySet()
                    } else {
                        AuthPermissionTable.selectAll()
                            .where { AuthPermissionTable.id inList permissionIds }
                            .map { it[AuthPermissionTable.code] }
                            .toSet()
                    }
                // An active shop membership with no roles is still an assignment:
                // deleting a role must not fall back to broad legacy defaults.
                (platformRoleIds.isNotEmpty() || memberIds.isNotEmpty()) to resolved
            }

        // A user with no assignments keeps the legacy behavior, except platform
        // admins: an ADMIN without a role must never inherit full access.
        val result =
            when {
                hasAssignments -> codes
                user.userType == UserType.ADMIN || user.userType == UserType.SUPER_ADMIN -> emptySet()
                else -> user.userType.defaultPermissions().map { it.code }.toSet()
            }
        CacheService.cache.set(cacheKey, result, 60)
        return result
    }

    /** True when the actor holds every permission in [roleCode]; super admin always passes. */
    suspend fun canGrantRole(
        actorUserId: String,
        roleCode: String,
    ): Boolean {
        val permissions =
            query {
                val roleId =
                    AuthRoleTable.selectAll()
                        .where { AuthRoleTable.code eq roleCode }
                        .firstOrNull()
                        ?.get(AuthRoleTable.id)
                        ?.value
                        ?: return@query emptySet()
                rolePermissionCodes(listOf(roleId))
            }
        if (permissions.isEmpty()) return true
        if (isSuperAdmin(actorUserId)) return true
        val actor = permissionsForUser(actorUserId)
        return permissions.all { it in actor }
    }

    // ── Permissions ──────────────────────────────────────────────────────

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
    private fun replaceRolePermissionsTx(
        role: AuthRoleDAO,
        permissionCodes: List<String>,
    ) {
        if (role.code == RbacDefaults.SUPER_ADMIN_ROLE_CODE && permissionCodes.isEmpty()) {
            throw ValidationException("Super admin must retain all permissions")
        }
        val resolved =
            AuthPermissionTable.selectAll()
                .where { AuthPermissionTable.code inList permissionCodes }
                .associate { it[AuthPermissionTable.code] to it[AuthPermissionTable.id] }
        val missing = permissionCodes - resolved.keys
        if (missing.isNotEmpty()) throw ValidationException("Unknown permissions: ${missing.joinToString()}")
        AuthRolePermissionTable.deleteWhere { AuthRolePermissionTable.roleId eq role.id }
        resolved.values.forEach { permissionId ->
            AuthRolePermissionTable.insert {
                it[AuthRolePermissionTable.roleId] = role.id
                it[AuthRolePermissionTable.permissionId] = permissionId
            }
        }
        bumpUsersForRoleTx(role.id.value)
    }

    suspend fun getRole(roleId: String): RoleResponse =
        query {
            val role = AuthRoleDAO.findById(roleId) ?: roleId.throwNotFound("Role")
            role.toRoleResponse(permissions = rolePermissionCodes(listOf(roleId)).sorted())
        }

    private fun permissionCodesByRole(): Map<String, List<String>> =
        AuthPermissionTable.selectAll()
            .associate { it[AuthPermissionTable.id].value to it[AuthPermissionTable.code] }
            .let { codes ->
                AuthRolePermissionTable.selectAll()
                    .groupBy({ it[AuthRolePermissionTable.roleId].value }) { row ->
                        codes[row[AuthRolePermissionTable.permissionId].value].orEmpty()
                    }.mapValues { (_, values) -> values.filter { it.isNotBlank() }.sorted() }
            }

    // ── User role assignment ─────────────────────────────────────────────

    suspend fun getUserRoles(userId: String): EffectivePermissionsResponse = effectivePermissions(userId)

    suspend fun setUserRoles(
        actorUserId: String,
        userId: String,
        roleIds: List<String>,
    ): EffectivePermissionsResponse {
        val user = query { UserDAO.findById(userId) ?: userId.throwNotFound("User") }
        // Only a super admin may change roles on an admin account. Without this,
        // a lesser admin can strip another admin/super-admin back to no roles.
        if (user.userType in listOf(UserType.ADMIN, UserType.SUPER_ADMIN) && !isSuperAdmin(actorUserId)) {
            throw ForbiddenException("Only a super admin can modify admin role assignments")
        }
        if (roleIds.isNotEmpty() && user.userType != UserType.ADMIN && user.userType != UserType.SUPER_ADMIN) {
            throw ValidationException("Admin roles can only be assigned to ADMIN accounts")
        }
        val roles =
            if (roleIds.isEmpty()) {
                emptyList()
            } else {
                query {
                    rolesByIds(roleIds).values.map { it.toRoleRef() }
                }
            }
        val missing = roleIds.distinct().size - roles.size
        if (missing > 0) throw ValidationException("One or more roles were not found")
        if (roles.any { it.scopeType != RoleScope.GLOBAL }) throw ValidationException("Only GLOBAL roles can be assigned to platform users")

        val requestedPermissions = query { rolePermissionCodes(roles.map { it.id }) }
        ensureCanGrant(actorUserId, requestedPermissions)

        val superAdmin = isSuperAdmin(actorUserId)
        val targetIsSuper = roles.any { it.code == RbacDefaults.SUPER_ADMIN_ROLE_CODE }
        if (targetIsSuper && !superAdmin) throw ForbiddenException("Only a super admin can grant the super admin role")

        query {
            val currentlySuper = isSuperAdminTx(user.id.value)
            if (currentlySuper && !targetIsSuper) {
                if (superAdminCountTx() <= 1) {
                    throw ValidationException("Cannot remove the last super admin")
                }
            }
            AuthUserRoleTable.deleteWhere { AuthUserRoleTable.userId eq user.id }
            roles.forEach { role ->
                AuthUserRoleTable.insert {
                    it[AuthUserRoleTable.userId] = user.id
                    it[AuthUserRoleTable.roleId] = role.id.entityID(AuthRoleTable)
                    it[AuthUserRoleTable.assignedBy] = actorUserId.entityID(UserTable)
                }
            }
            user.authzVersion += 1
        }
        AdminAudit.publish(actorUserId, "RBAC_USER_ROLES_UPDATED", "USER", userId, roleIds.joinToString())
        return effectivePermissions(userId)
    }

    suspend fun effectivePermissions(userId: String): EffectivePermissionsResponse {
        val user = query { UserDAO.findById(userId) } ?: userId.throwNotFound("User")
        val (roles, memberships) =
            query {
                val platformRoleIds =
                    AuthUserRoleTable.selectAll()
                        .where { AuthUserRoleTable.userId eq user.id }
                        .map { it[AuthUserRoleTable.roleId].value }
                val roles = rolesByIds(platformRoleIds).values.map { it.toRoleRef() }.sortedBy { it.code }

                val memberRows =
                    ShopMemberTable.selectAll()
                        .where { (ShopMemberTable.userId eq user.id) and (ShopMemberTable.status neq MembershipStatus.REMOVED) }
                        .orderBy(ShopMemberTable.createdAt to SortOrder.ASC)
                        .toList()
                val memberIds = memberRows.map { it[ShopMemberTable.id].value }
                val roleCodes = roleCodesByMemberIds(memberIds)

                val shopIds = memberRows.map { it[ShopMemberTable.shopId].value }.distinct()
                val shopNames =
                    if (shopIds.isEmpty()) {
                        emptyMap()
                    } else {
                        ShopTable.selectAll()
                            .where { ShopTable.id inList shopIds.entityIDs(ShopTable) }
                            .associate { it[ShopTable.id].value to it[ShopTable.name] }
                    }
                val memberships =
                    memberRows.map { row ->
                        MembershipRef(
                            shopId = row[ShopMemberTable.shopId].value,
                            shopName = shopNames[row[ShopMemberTable.shopId].value],
                            isOwner = row[ShopMemberTable.isOwner],
                            roles = roleCodes[row[ShopMemberTable.id].value].orEmpty(),
                        )
                    }
                roles to memberships
            }
        return EffectivePermissionsResponse(
            userId = user.id.value,
            email = user.email,
            accountType = user.userType.name,
            roles = roles,
            memberships = memberships,
            permissions = permissionsForUser(user).sorted(),
        )
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private suspend fun isSuperAdmin(userId: String): Boolean = query { isSuperAdminTx(userId) }

    private fun isSuperAdminTx(userId: String): Boolean {
        val roleIds =
            AuthUserRoleTable.selectAll()
                .where { AuthUserRoleTable.userId eq userId.entityID(UserTable) }
                .map { it[AuthUserRoleTable.roleId].value }
        return rolesByIds(roleIds).values.any { it.code == RbacDefaults.SUPER_ADMIN_ROLE_CODE }
    }

    private fun superAdminCountTx(): Int {
        val roleIds =
            AuthRoleTable.selectAll()
                .where { AuthRoleTable.code eq RbacDefaults.SUPER_ADMIN_ROLE_CODE }
                .map { it[AuthRoleTable.id].value }
        if (roleIds.isEmpty()) return 0
        return AuthUserRoleTable.selectAll()
            .where { AuthUserRoleTable.roleId inList roleIds.entityIDs(AuthRoleTable) }
            .map { it[AuthUserRoleTable.userId].value }
            .distinct()
            .size
    }

    /** Prevents privilege escalation: a non-super admin can only grant what they hold. */
    private suspend fun ensureCanGrant(
        actorUserId: String,
        permissionCodes: Collection<String>,
    ) {
        if (permissionCodes.isEmpty()) return
        if (isSuperAdmin(actorUserId)) return
        val actor = permissionsForUser(actorUserId)
        val excess = permissionCodes.toSet() - actor
        if (excess.isNotEmpty()) {
            throw ForbiddenException("Cannot grant permissions you do not hold: ${excess.joinToString()}")
        }
    }

    private fun rolePermissionCodes(roleIds: List<String>): Set<String> {
        if (roleIds.isEmpty()) return emptySet()
        val permissionIds =
            AuthRolePermissionTable.selectAll()
                .where { AuthRolePermissionTable.roleId inList roleIds.entityIDs(AuthRoleTable) }
                .map { it[AuthRolePermissionTable.permissionId].value }
        if (permissionIds.isEmpty()) return emptySet()
        return AuthPermissionTable.selectAll()
            .where { AuthPermissionTable.id inList permissionIds }
            .map { it[AuthPermissionTable.code] }
            .toSet()
    }

    /** Permission changes affect every holder of the role, not only newly assigned users. */
    private fun bumpUsersForRoleTx(roleId: String) {
        val platformUserIds =
            AuthUserRoleTable.selectAll()
                .where { AuthUserRoleTable.roleId eq roleId.entityID(AuthRoleTable) }
                .map { it[AuthUserRoleTable.userId].value }
        val memberIds =
            ShopMemberRoleTable.selectAll()
                .where { ShopMemberRoleTable.roleId eq roleId.entityID(AuthRoleTable) }
                .map { it[ShopMemberRoleTable.memberId].value }
        val shopUserIds =
            if (memberIds.isEmpty()) {
                emptyList()
            } else {
                ShopMemberTable.selectAll()
                    .where { ShopMemberTable.id inList memberIds.entityIDs(ShopMemberTable) }
                    .map { it[ShopMemberTable.userId].value }
            }
        (platformUserIds + shopUserIds).distinct().forEach { userId ->
            UserDAO.findById(userId)?.let { it.authzVersion += 1 }
        }
    }
}
