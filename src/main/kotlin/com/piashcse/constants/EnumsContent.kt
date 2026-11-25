package com.piashcse.constants

import kotlinx.serialization.Serializable

@Serializable
enum class CmsPageStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED,
}

@Serializable
enum class SeoEntityType {
    HOME,
    PAGE,
    PRODUCT,
    CATEGORY,
    SHOP,
}

@Serializable
enum class TicketCategory {
    ORDER,
    PAYMENT,
    ACCOUNT,
    PRODUCT,
    OTHER,
}

@Serializable
enum class TicketPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT,
}

@Serializable
enum class TicketStatus {
    OPEN,
    PENDING,
    RESOLVED,
    CLOSED,
}

@Serializable
enum class SupportSenderRole {
    CUSTOMER,
    SELLER,
    ADMIN,
    SYSTEM,
}

@Serializable
enum class PolicyType {
    PRIVACY_POLICY,
    TERMS_CONDITIONS,
    REFUND_POLICY,
    COOKIE_POLICY,
    DISCLAIMER,
    EULA,
    SHIPPING_POLICY,
    ;

    val isLegalPolicy get() = this in listOf(PRIVACY_POLICY, TERMS_CONDITIONS, DISCLAIMER, EULA)
    val requiresConsent get() = this != SHIPPING_POLICY
}

/**
 * User role hierarchy and permissions
 */
@Serializable
enum class UserType {
    SUPER_ADMIN,
    ADMIN,
    SELLER,
    CUSTOMER,
    ;

    val isSuperAdmin get() = this == SUPER_ADMIN
    val isAdminOrHigher get() = this in listOf(SUPER_ADMIN, ADMIN)
    val isSellerOrHigher get() = this in listOf(SUPER_ADMIN, ADMIN, SELLER)
    val isCustomerOrHigher get() = true // All roles can act as customers

    fun canManage(targetUserType: UserType): Boolean =
        when (this) {
            SUPER_ADMIN -> true
            ADMIN -> targetUserType != SUPER_ADMIN && targetUserType != ADMIN
            SELLER -> targetUserType == CUSTOMER
            CUSTOMER -> targetUserType == CUSTOMER
        }

    fun hasAccessTo(role: UserType): Boolean =
        when (role) {
            SUPER_ADMIN -> this == SUPER_ADMIN
            ADMIN -> this.isAdminOrHigher
            SELLER -> this.isSellerOrHigher
            CUSTOMER -> this.isCustomerOrHigher
        }

    /** Strict least-privilege check: exact role match, no hierarchy. Used by customerOnlyAuth. */
    fun hasExactRole(role: UserType): Boolean = this == role

    companion object {
        fun fromString(role: String): UserType? = values().find { it.name.equals(role.trim(), ignoreCase = true) }
    }
}
