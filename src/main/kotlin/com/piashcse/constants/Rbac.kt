package com.piashcse.constants

/** Role scope values stored in `auth_role.scope_type`. */
object RoleScope {
    const val GLOBAL = "GLOBAL"
    const val SHOP = "SHOP"

    val all = setOf(GLOBAL, SHOP)
}

/** Shop membership lifecycle stored in `shop_member.status`. */
object MembershipStatus {
    const val ACTIVE = "ACTIVE"
    const val REMOVED = "REMOVED"
    const val SUSPENDED = "SUSPENDED"
}

/** Invitation lifecycle stored in `shop_invitation.status`. */
object InvitationStatus {
    const val PENDING = "PENDING"
    const val ACCEPTED = "ACCEPTED"
    const val EXPIRED = "EXPIRED"
}

/** Stable ids/codes seeded by the RBAC migrations. */
object RbacDefaults {
    const val SUPER_ADMIN_ROLE_ID = "role-platform-super-admin"
    const val ADMIN_ROLE_ID = "role-platform-admin"
    const val SHOP_OWNER_ROLE_ID = "role-shop-owner"

    const val SUPER_ADMIN_ROLE_CODE = "platform:super_admin"
    const val ADMIN_ROLE_CODE = "platform:admin"
    const val SHOP_OWNER_ROLE_CODE = "shop:owner"
}
