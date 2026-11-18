package com.piashcse.feature.rbac

import com.piashcse.database.entities.AuthRoleDAO
import com.piashcse.database.entities.AuthRoleTable
import com.piashcse.database.entities.ShopMemberDAO
import com.piashcse.database.entities.ShopMemberRoleTable
import com.piashcse.database.entities.ShopMemberTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.utils.extension.entityIDs
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.selectAll

internal fun AuthRoleDAO.toRoleRef() = RoleRef(id.value, code, name, scopeType)

internal fun AuthRoleDAO.toRoleResponse(permissions: List<String> = emptyList()) =
    RoleResponse(
        id = id.value,
        code = code,
        name = name,
        description = description,
        scopeType = scopeType,
        isSystem = isSystem,
        permissions = permissions,
    )

internal fun ShopMemberDAO.toMember(email: String = UserDAO.findById(userId.value)?.email.orEmpty()): ShopMemberResponse =
    ShopMemberResponse(
        id = id.value,
        shopId = shopId.value,
        userId = userId.value,
        email = email,
        status = status,
        isOwner = isOwner,
    )

private val ROLE_CODE_PATTERN = Regex("^[a-z][a-z0-9:_-]{2,99}$")

internal fun validateRoleCode(code: String) {
    if (!ROLE_CODE_PATTERN.matches(code)) {
        throw ValidationException("Role code must be lowercase and use letters, digits, ':', '_' or '-'")
    }
}

/** Bulk role lookup used by effective-permission and member views. */
internal fun rolesByIds(roleIds: Collection<String>): Map<String, AuthRoleDAO> =
    if (roleIds.isEmpty()) {
        emptyMap()
    } else {
        AuthRoleDAO.find { AuthRoleTable.id inList roleIds.distinct().entityIDs(AuthRoleTable) }
            .associateBy { it.id.value }
    }

/** One query for all member-role rows, then one role lookup. */
internal fun roleCodesByMemberIds(memberIds: Collection<String>): Map<String, List<String>> {
    if (memberIds.isEmpty()) return emptyMap()
    val roleIdsByMember =
        ShopMemberRoleTable.selectAll()
            .where { ShopMemberRoleTable.memberId inList memberIds.distinct().entityIDs(ShopMemberTable) }
            .groupBy({ it[ShopMemberRoleTable.memberId].value }, { it[ShopMemberRoleTable.roleId].value })
    val codes = rolesByIds(roleIdsByMember.values.flatten()).mapValues { it.value.code }
    return roleIdsByMember.mapValues { (_, roleIds) ->
        roleIds.mapNotNull { codes[it] }.filter { it.isNotBlank() }.sorted()
    }
}
