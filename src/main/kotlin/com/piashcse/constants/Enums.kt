package com.piashcse.constants

import kotlinx.serialization.Serializable

@Serializable
enum class CouponDiscountType {
    FIXED,
    PERCENTAGE,
}

@Serializable
enum class PromotionType {
    PERCENTAGE,
    FIXED,
    FREE_SHIPPING,
}

@Serializable
enum class ReviewStatus {
    ACTIVE,
    HIDDEN,
    FLAGGED,
}

@Serializable
enum class RefundStatus {
    PENDING,
    APPROVED,
    REJECTED,
    REFUNDED,
    SHIPPED,
}

@Serializable
enum class RefundMethod {
    ORIGINAL,
    BANK_TRANSFER,
}

@Serializable
enum class KycStatus {
    NOT_SUBMITTED,
    PENDING,
    VERIFIED,
    REJECTED,
}

@Serializable
enum class RmaType {
    RETURN,
    EXCHANGE,
}

@Serializable
enum class RmaStatus {
    REQUESTED,
    APPROVED,
    REJECTED,
    IN_TRANSIT,
    RECEIVED,
    COMPLETED,
    CANCELLED,
    ;

    companion object {
        fun canTransitionTo(
            current: RmaStatus,
            target: RmaStatus,
        ): Boolean =
            when (current) {
                REQUESTED -> target in listOf(APPROVED, REJECTED, CANCELLED)
                APPROVED -> target in listOf(IN_TRANSIT, CANCELLED)
                IN_TRANSIT -> target in listOf(RECEIVED)
                RECEIVED -> target in listOf(COMPLETED)
                REJECTED, COMPLETED, CANCELLED -> false
            }
    }
}

@Serializable
enum class RmaResolution {
    REFUND,
    REPLACEMENT,
}

@Serializable
enum class PaymentMethod {
    CREDIT_CARD,
    PAYPAL,
    BANK_TRANSFER,
    COD,
}

/** Payment processor that owns the charge. */
@Serializable
enum class PaymentProvider {
    MANUAL,
    STRIPE,
}

/** Strong Customer Authentication / 3-D Secure state reported by the provider. */
@Serializable
enum class ScaStatus {
    NOT_REQUIRED,
    REQUIRED,
    PENDING,
    AUTHENTICATED,
    FAILED,
}

/** Processing state of a provider webhook delivery. */
@Serializable
enum class WebhookEventStatus {
    RECEIVED,
    PROCESSED,
    IGNORED,
    FAILED,
}

/** Lifecycle of a carrier shipment/label. */
@Serializable
enum class ShipmentStatus {
    PENDING,
    LABEL_PURCHASED,
    IN_TRANSIT,
    DELIVERED,
    RETURNED,
    CANCELED,
    FAILED,
}

/** Fraud metric evaluated by a velocity rule. */
@Serializable
enum class FraudMetric {
    ORDER_COUNT,
    ORDER_AMOUNT,
    PAYMENT_ATTEMPTS,
}

/** Rule action when a fraud threshold is met. */
@Serializable
enum class RiskAction {
    ALLOW,
    REVIEW,
    REQUIRE_3DS,
    BLOCK,
}

/** Final risk decision stored on the payment. */
@Serializable
enum class RiskDecision {
    ALLOW,
    REVIEW,
    BLOCK,
}

/** Address Verification System result supplied by the processor. */
@Serializable
enum class AvsResult {
    MATCH,
    PARTIAL,
    MISMATCH,
    UNKNOWN,
}

/** Card Verification Value result supplied by the processor. */
@Serializable
enum class CvvResult {
    MATCH,
    MISMATCH,
    UNKNOWN,
}

@Serializable
enum class BlocklistType {
    USER,
    EMAIL,
    IP,
}

@Serializable
enum class ChargebackStatus {
    OPEN,
    UNDER_REVIEW,
    WON,
    LOST,
    CLOSED,
}

@Serializable
enum class WalletEntryType {
    POINTS_EARN,
    POINTS_REDEEM,
    CREDIT_EARN,
    CREDIT_REDEEM,
    REFERRAL_BONUS,
    ADMIN_ADJUSTMENT,
}

@Serializable
enum class ReferralStatus {
    PENDING,
    COMPLETED,
    EXPIRED,
}

@Serializable
enum class GiftCardStatus {
    ACTIVE,
    REDEEMED,
    EXPIRED,
    VOID,
}

/** Storefront funnel event names. */
@Serializable
enum class AnalyticsEventType {
    PAGE_VIEW,
    PRODUCT_VIEW,
    SEARCH,
    ADD_TO_CART,
    CHECKOUT_START,
    PURCHASE,
    SIGNUP,
}

/** Buyer/seller dispute categories. */
@Serializable
enum class DisputeType {
    ORDER_ISSUE,
    PAYOUT,
    REVIEW,
    OTHER,
}

@Serializable
enum class DisputeStatus {
    OPEN,
    UNDER_REVIEW,
    RESOLVED,
    REJECTED,
}

@Serializable
enum class BillingInterval {
    MONTH,
    YEAR,
}

@Serializable
enum class SubscriptionStatus {
    PENDING,
    ACTIVE,
    PAST_DUE,
    CANCELED,
    EXPIRED,
}

@Serializable
enum class InvoiceStatus {
    PENDING,
    PAID,
    FAILED,
}

@Serializable
enum class B2bAccountStatus {
    PENDING,
    APPROVED,
    SUSPENDED,
}

@Serializable
enum class B2bQuoteStatus {
    REQUESTED,
    SENT,
    ACCEPTED,
    REJECTED,
    EXPIRED,
}

@Serializable
enum class PurchaseOrderStatus {
    OPEN,
    FULFILLED,
    CANCELED,
}

@Serializable
enum class FulfillmentType {
    DELIVERY,
    PICKUP,
    POS,
}

@Serializable
enum class PickupStatus {
    PENDING,
    READY,
    PICKED_UP,
    CANCELED,
}

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
 * Status of orders throughout their lifecycle
 */
@Serializable
enum class OrderStatus {
    PENDING,
    CONFIRMED,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELED,
    RECEIVED,
    ;

    companion object {
        fun canTransitionTo(
            current: OrderStatus,
            target: OrderStatus,
        ): Boolean =
            when (current) {
                PENDING -> target in listOf(CONFIRMED, CANCELED)
                CONFIRMED -> target in listOf(PAID, CANCELED)
                // PAID orders cannot be canceled outright: cancel-after-paid must go through
                // a refund record (see RefundRequest flow), so CANCELED is not a legal target here.
                PAID -> target == SHIPPED
                SHIPPED -> target == DELIVERED
                DELIVERED -> target == RECEIVED // Delivered order can be marked as received
                CANCELED -> false // Canceled orders cannot transition to other statuses
                RECEIVED -> false // Completed orders remain in received status
            }

        // Only unpaid orders may be canceled directly; PAID/DELIVERED must use the refund flow.
        fun canBeCanceled(current: OrderStatus): Boolean = current in listOf(PENDING, CONFIRMED)
    }
}

/**
 * Payment status throughout transaction lifecycle
 */
@Serializable
enum class PaymentStatus {
    PENDING,
    COMPLETED,
    FAILED,
    REFUNDED,
    ;

    val isFinal get() = this in listOf(COMPLETED, FAILED, REFUNDED)
    val isSuccessful get() = this == COMPLETED
}

/**
 * Product availability status
 */
@Serializable
enum class ProductStatus {
    ACTIVE, // Product is available for purchase
    OUT_OF_STOCK, // Product is not available
    ;

    val isActive get() = this == ACTIVE
}

/**
 * Shop approval and operational status
 */
@Serializable
enum class ShopStatus {
    PENDING, // Shop application pending approval
    APPROVED, // Shop approved and active
    REJECTED, // Shop application rejected
    SUSPENDED, // Shop suspended by admin
    ;

    val isOperational get() = this == APPROVED
    val isActive get() = this == APPROVED
}

/**
 * Inventory level status indicators
 */
@Serializable
enum class InventoryStatus {
    IN_STOCK,
    LOW_STOCK,
    OUT_OF_STOCK,
    ;

    val isAvailable get() = this == IN_STOCK
    val needsAttention get() = this in listOf(LOW_STOCK, OUT_OF_STOCK)

    companion object {
        fun fromStockLevel(
            stock: Int,
            minLevel: Int,
        ): InventoryStatus =
            when {
                stock <= 0 -> OUT_OF_STOCK
                stock <= minLevel -> LOW_STOCK
                else -> IN_STOCK
            }
    }
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
        fun fromString(role: String): UserType? = values().find { it.name.equals(role, ignoreCase = true) }
    }
}
