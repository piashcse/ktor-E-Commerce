package com.piashcse.feature.privacy

import at.favre.lib.crypto.bcrypt.BCrypt
import com.piashcse.constants.AppConstants
import com.piashcse.constants.Message
import com.piashcse.database.entities.*
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.feature.notification.NotificationResponse
import com.piashcse.mapper.*
import com.piashcse.model.response.*
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.validator.UnauthorizedException
import org.jetbrains.exposed.v1.core.eq
import java.time.Instant
import java.util.UUID

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
    val exportedAt: String,
)

@kotlinx.serialization.Serializable
data class EraseAccountRequest(val password: String)

/**
 * GDPR data-subject requests. Orders/payments are retained in pseudonymous form
 * for financial/legal obligations; direct identifiers and self-service data are
 * removed or anonymized.
 */
class PrivacyService {
    suspend fun export(userId: String): PrivacyExportResponse =
        query {
            val user = UserDAO.findById(userId) ?: userId.throwNotFound("User")
            val orders = OrderDAO.find { OrderTable.userId eq user.id }.limit(1_000).toList()
            PrivacyExportResponse(
                user = user.toUserResponse(),
                profile = UserProfileDAO.find { UserProfileTable.userId eq user.id }.firstOrNull()?.toUserProfileResponse(),
                shippingAddresses =
                    ShippingAddressDAO.find { ShippingAddressTable.userId eq user.id }
                        .map { it.toShippingAddressResponse() },
                orders =
                    orders.map {
                        it.toOrderResponse(
                            OrderItemDAO.itemsForOrder(it.id).map {
                                    item ->
                                item.toOrderItemResponse()
                            },
                        )
                    },
                payments = PaymentDAO.find { PaymentTable.userId eq user.id }.map { it.toPaymentResponse() },
                refunds = RefundRequestDAO.find { RefundRequestTable.userId eq user.id }.map { it.toRefundRequestResponse() },
                consents = PolicyConsentDAO.find { PolicyConsentTable.userId eq user.id }.map { it.toPolicyConsentResponse() },
                reviews = ReviewRatingDAO.find { ReviewRatingTable.userId eq user.id }.map { it.toReviewRatingResponse() },
                notifications = NotificationDAO.find { NotificationTable.userId eq user.id }.map { it.toPrivacyNotification() },
                exportedAt = currentUtc().toUtcString(),
            )
        }

    suspend fun erase(
        userId: String,
        password: String,
    ): Boolean =
        query {
            val user = UserDAO.findById(userId) ?: userId.throwNotFound("User")
            if (!BCrypt.verifyer().verify(password.toCharArray(), user.password).verified) {
                throw UnauthorizedException(Message.Auth.INVALID_CREDENTIALS)
            }

            // Anonymize the account and invalidate every session.
            user.email = "deleted+${user.id.value}@invalid.local"
            user.password =
                BCrypt.withDefaults().hashToString(
                    AppConstants.BCRYPT_COST,
                    UUID.randomUUID().toString().toCharArray(),
                )
            user.isActive = false
            user.isVerified = false
            user.otpCode = null
            user.otpExpiry = null
            user.resetOtpCode = null
            user.resetOtpExpiry = null
            RefreshTokenDAO.find { RefreshTokenTable.userId eq user.id }.forEach { it.revokedAt = Instant.now() }

            // Remove self-service data.
            CartItemDAO.find { CartItemTable.userId eq user.id }.forEach { it.delete() }
            WishListDAO.find { WishListTable.userId eq user.id }.forEach { it.delete() }
            ShippingAddressDAO.find { ShippingAddressTable.userId eq user.id }.forEach { it.delete() }
            PolicyConsentDAO.find { PolicyConsentTable.userId eq user.id }.forEach { it.delete() }
            ReviewHelpfulVoteDAO.find { ReviewHelpfulVoteTable.userId eq user.id }.forEach { it.delete() }
            ReviewRatingDAO.find { ReviewRatingTable.userId eq user.id }.forEach { it.delete() }
            NotificationDAO.find { NotificationTable.userId eq user.id }.forEach { it.delete() }

            UserProfileDAO.find { UserProfileTable.userId eq user.id }.forEach { profile ->
                profile.image = null
                profile.firstName = null
                profile.lastName = null
                profile.mobile = null
                profile.faxNumber = null
                profile.streetAddress = null
                profile.city = null
                profile.state = null
                profile.country = null
                profile.identificationType = null
                profile.identificationNo = null
                profile.occupation = null
                profile.postCode = null
                profile.gender = null
                profile.dateOfBirth = null
                profile.bio = null
                profile.isActive = false
                profile.verified = false
            }

            // Retain financial records but strip the shipping/notes snapshot.
            OrderDAO.find { OrderTable.userId eq user.id }.forEach { order ->
                order.shippingAddress = null
                order.notes = null
            }
            true
        }

    private fun NotificationDAO.toPrivacyNotification() =
        NotificationResponse(
            id = id.value,
            channel = channel,
            type = type,
            title = title,
            body = body,
            resourceType = resourceType,
            resourceId = resourceId,
            isRead = isRead,
            createdAt = createdAt.toUtcString(),
        )
}
