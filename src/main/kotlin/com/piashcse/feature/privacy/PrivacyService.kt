package com.piashcse.feature.privacy

import com.piashcse.database.entities.*
import com.piashcse.feature.notification.NotificationResponse
import com.piashcse.mapper.*
import com.piashcse.model.response.*

@kotlinx.serialization.Serializable
data class PrivacyExportResponse(
    val user: UserResponse,
    val profile: UserProfileResponse?,
    val shippingAddresses: List<ShippingAddressResponse>,
    val orders: List<OrderResponse>,
    val payments: List<PaymentResponse>,
    val refunds: List<RefundRequestResponse>,
    val consents: List<UserPolicyConsentResponse>,
    val reviews: List<ReviewRatingResponse>,
    val notifications: List<NotificationResponse>,
    val additionalData: Map<String, List<Map<String, String>>> = emptyMap(),
    val exportedAt: String,
)

@kotlinx.serialization.Serializable
data class EraseAccountRequest(val password: String)

/** Facade over privacy export and erasure. */
class PrivacyService {
    private val exports = PrivacyExportService()
    private val erasure = PrivacyErasureService()

    suspend fun export(userId: String): PrivacyExportResponse = exports.export(userId)

    suspend fun erase(
        userId: String,
        password: String,
    ): Boolean = erasure.erase(userId, password)
}
