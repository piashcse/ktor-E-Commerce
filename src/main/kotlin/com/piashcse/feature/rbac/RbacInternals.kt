package com.piashcse.feature.rbac

import com.piashcse.constants.MembershipStatus
import com.piashcse.constants.RbacDefaults
import com.piashcse.constants.UserType
import com.piashcse.constants.defaultPermissions
import com.piashcse.database.entities.AuthPermissionTable
import com.piashcse.database.entities.AuthRoleDAO
import com.piashcse.database.entities.AuthRolePermissionTable
import com.piashcse.database.entities.AuthRoleTable
import com.piashcse.database.entities.AuthUserRoleTable
import com.piashcse.database.entities.ShopMemberRoleTable
import com.piashcse.database.entities.ShopMemberTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.UserTable
import com.piashcse.service.CacheService
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.entityIDs
import com.piashcse.utils.extension.query
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll

internal fun replaceRolePermissionsTx(
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

internal fun permissionCodesByRole(): Map<String, List<String>> =
    AuthPermissionTable.selectAll()
        .associate { it[AuthPermissionTable.id].value to it[AuthPermissionTable.code] }
        .let { codes ->
            AuthRolePermissionTable.selectAll()
                .groupBy({ it[AuthRolePermissionTable.roleId].value }) { row ->
                    codes[row[AuthRolePermissionTable.permissionId].value].orEmpty()
                }.mapValues { (_, values) -> values.filter { it.isNotBlank() }.sorted() }
        }

// ── User role assignment ─────────────────────────────────────────────
internal suspend fun isSuperAdmin(userId: String): Boolean = query { isSuperAdminTx(userId) }

internal fun isSuperAdminTx(userId: String): Boolean {
    val roleIds =
        AuthUserRoleTable.selectAll()
            .where { AuthUserRoleTable.userId eq userId.entityID(UserTable) }
            .map { it[AuthUserRoleTable.roleId].value }
    return rolesByIds(roleIds).values.any { it.code == RbacDefaults.SUPER_ADMIN_ROLE_CODE }
}

internal fun superAdminCountTx(): Int {
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
internal suspend fun ensureCanGrant(
    actorUserId: String,
    permissionCodes: Collection<String>,
) {
    if (permissionCodes.isEmpty()) return
    if (isSuperAdmin(actorUserId)) return
    val actor = resolvePermissions(actorUserId)
    val excess = permissionCodes.toSet() - actor
    if (excess.isNotEmpty()) {
        throw ForbiddenException("Cannot grant permissions you do not hold: ${excess.joinToString()}")
    }
}

internal suspend fun resolvePermissions(userId: String): Set<String> {
    val user = query { UserDAO.findById(userId) } ?: return emptySet()
    return resolvePermissions(user)
}

internal suspend fun resolvePermissions(user: UserDAO): Set<String> {
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

internal fun rolePermissionCodes(roleIds: List<String>): Set<String> {
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
internal fun bumpUsersForRoleTx(roleId: String) {
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
