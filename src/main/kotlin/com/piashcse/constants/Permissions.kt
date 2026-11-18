@file:Suppress("MatchingDeclarationName")

package com.piashcse.constants

import kotlinx.serialization.Serializable

/**
 * Coarse route-level permissions. Each value carries the stable `resource:action`
 * code stored in the RBAC catalog; custom roles may grant codes that do not have
 * an enum constant yet.
 *
 * Role defaults are only a migration fallback. Runtime authorization resolves
 * the effective permission set from the database (see `RbacService`).
 */
@Serializable
enum class Permission(val code: String) {
    USER_MANAGE("user:manage"),
    CATALOG_MANAGE("catalog:manage"),
    ORDER_MANAGE("order:manage"),
    PAYMENT_MANAGE("payment:manage"),
    REFUND_MANAGE("refund:manage"),
    PAYOUT_MANAGE("payout:manage"),
    COUPON_MANAGE("coupon:manage"),
    SHOP_MANAGE("shop:manage"),
    INVENTORY_MANAGE("inventory:manage"),
    POLICY_MANAGE("policy:manage"),
    DASHBOARD_VIEW("dashboard:read"),
    AUDIT_VIEW("audit:read"),
    NOTIFICATION_SEND("notification:send"),
    TAX_MANAGE("tax:manage"),
    FX_MANAGE("fx:manage"),
    OUTBOX_MANAGE("outbox:manage"),
    API_KEY_MANAGE("api_key:manage"),
    FEATURE_FLAG_MANAGE("feature_flag:manage"),
    ROLE_MANAGE("role:manage"),
    SUPPORT_MANAGE("support:manage"),
    FRAUD_MANAGE("fraud:manage"),
    CMS_MANAGE("cms:manage"),
    SUBSCRIPTION_MANAGE("subscription:manage"),
    REVIEW_MANAGE("review:manage"),
    SHIPPING_MANAGE("shipping:manage"),
    B2B_MANAGE("b2b:manage"),
    ;

    companion object {
        /** Accepts either the enum name (legacy API keys) or the stable code. */
        fun fromString(value: String): Permission? =
            entries.find { it.name.equals(value, ignoreCase = true) || it.code.equals(value, ignoreCase = true) }
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
