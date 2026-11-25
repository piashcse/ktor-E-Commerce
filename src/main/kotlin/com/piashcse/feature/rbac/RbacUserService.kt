package com.piashcse.feature.rbac

import com.piashcse.constants.MembershipStatus
import com.piashcse.constants.RbacDefaults
import com.piashcse.constants.RoleScope
import com.piashcse.constants.UserType
import com.piashcse.constants.defaultPermissions
import com.piashcse.database.entities.AuthPermissionTable
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
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll

/** Effective permissions and platform role assignment. */
class RbacUserService {
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
}
