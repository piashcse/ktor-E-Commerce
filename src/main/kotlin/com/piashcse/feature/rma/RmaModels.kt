package com.piashcse.feature.rma

import com.piashcse.constants.RmaResolution
import com.piashcse.constants.RmaStatus
import com.piashcse.constants.RmaType
import com.piashcse.database.entities.*
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.ValidationException
import kotlinx.serialization.Serializable

@Serializable
data class RmaCreateRequest(
    val orderId: String,
    val orderItemId: String,
    val quantity: Int = 1,
    val type: RmaType = RmaType.RETURN,
    val reason: String,
) {
    init {
        if (quantity <= 0) throw ValidationException("Quantity must be positive")
        if (reason.isBlank()) throw ValidationException("Reason is required")
    }
}

@Serializable
data class RmaShipRequest(val trackingNumber: String)

@Serializable
data class RmaStatusUpdateRequest(
    val status: RmaStatus,
    val resolution: RmaResolution? = null,
    val replacementOrderId: String? = null,
    val note: String? = null,
)

@Serializable
data class RmaResponse(
    val id: String,
    val orderId: String,
    val orderItemId: String,
    val userId: String,
    val shopId: String?,
    val quantity: Int,
    val type: RmaType,
    val status: RmaStatus,
    val reason: String,
    val resolution: RmaResolution?,
    val note: String? = null,
    val orderNumber: String? = null,
    val customerEmail: String? = null,
    val productName: String? = null,
    val allowedActions: List<String> = emptyList(),
    val trackingNumber: String?,
    val replacementOrderId: String?,
    val refundRequestId: String?,
    val createdAt: String,
    val resolvedAt: String?,
)
