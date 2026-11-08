@file:Suppress("MatchingDeclarationName")

package com.piashcse.constants

import kotlinx.serialization.Serializable

/**
 * Fine-grained permissions for admin/seller operations. Role defaults are used
 * for JWT sessions; API keys may carry a narrower subset via [scopes].
 */
@Serializable
enum class Permission {
    USER_MANAGE,
    CATALOG_MANAGE,
    ORDER_MANAGE,
    PAYMENT_MANAGE,
    REFUND_MANAGE,
    PAYOUT_MANAGE,
    COUPON_MANAGE,
    SHOP_MANAGE,
    INVENTORY_MANAGE,
    POLICY_MANAGE,
    DASHBOARD_VIEW,
    AUDIT_VIEW,
    NOTIFICATION_SEND,
    TAX_MANAGE,
    FX_MANAGE,
    OUTBOX_MANAGE,
    API_KEY_MANAGE,
    FEATURE_FLAG_MANAGE,
    ;

    companion object {
        fun fromString(value: String): Permission? = values().find { it.name.equals(value, ignoreCase = true) }
    }
}

fun UserType.defaultPermissions(): Set<Permission> =
    when (this) {
        UserType.SUPER_ADMIN -> Permission.entries.toSet()
        UserType.ADMIN ->
            Permission.entries.toSet() - setOf(Permission.FEATURE_FLAG_MANAGE)
        UserType.SELLER ->
            setOf(
                Permission.ORDER_MANAGE,
                Permission.REFUND_MANAGE,
                Permission.PAYOUT_MANAGE,
                Permission.SHOP_MANAGE,
                Permission.INVENTORY_MANAGE,
                Permission.DASHBOARD_VIEW,
                Permission.NOTIFICATION_SEND,
                Permission.API_KEY_MANAGE,
            )
        UserType.CUSTOMER -> setOf(Permission.API_KEY_MANAGE)
    }
