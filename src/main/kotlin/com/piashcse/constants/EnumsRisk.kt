package com.piashcse.constants

import kotlinx.serialization.Serializable

@Serializable
enum class KycStatus {
    NOT_SUBMITTED,
    PENDING,
    VERIFIED,
    REJECTED,
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
