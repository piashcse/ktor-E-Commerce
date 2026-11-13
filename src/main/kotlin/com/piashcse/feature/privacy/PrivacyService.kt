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
    val additionalData: Map<String, List<Map<String, String>>> = emptyMap(),
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
            val orders = OrderDAO.find { OrderTable.userId eq user.id }.toList()
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
                additionalData =
                    buildMap {
                        put("walletLedger", WalletLedgerDAO.find { WalletLedgerTable.userId eq user.id }.map { it.toPrivacyMap() })
                        put(
                            "giftCardRedemptions",
                            GiftCardRedemptionDAO.find { GiftCardRedemptionTable.userId eq user.id }
                                .map { it.toPrivacyMap() },
                        )
                        val subscriptions = SubscriptionDAO.find { SubscriptionTable.userId eq user.id }.toList()
                        put("subscriptions", subscriptions.map { it.toPrivacyMap() })
                        put(
                            "subscriptionInvoices",
                            subscriptions.flatMap { subscription ->
                                SubscriptionInvoiceDAO.find { SubscriptionInvoiceTable.subscriptionId eq subscription.id }
                                    .map { it.toPrivacyMap() }
                            },
                        )
                        put("supportTickets", SupportTicketDAO.find { SupportTicketTable.userId eq user.id }.map { it.toPrivacyMap() })
                        put(
                            "supportMessages",
                            SupportMessageDAO.find { SupportMessageTable.senderId eq user.id }.map { it.toPrivacyMap() },
                        )
                        put(
                            "digitalEntitlements",
                            DigitalEntitlementDAO.find { DigitalEntitlementTable.userId eq user.id }.map { it.toPrivacyMap() },
                        )
                        put("passkeys", PasskeyCredentialDAO.find { PasskeyCredentialTable.userId eq user.id }.map { it.toPrivacyMap() })
                        put(
                            "b2bQuotes",
                            B2bQuoteDAO.find { B2bQuoteTable.buyerUserId eq user.id }.map { it.toPrivacyMap() },
                        )
                        put(
                            "b2bPurchaseOrders",
                            B2bPurchaseOrderDAO.find { B2bPurchaseOrderTable.buyerUserId eq user.id }.map { it.toPrivacyMap() },
                        )
                        put(
                            "sellerReviews",
                            SellerReviewDAO.find { SellerReviewTable.userId eq user.id }.map { it.toPrivacyMap() },
                        )
                        put(
                            "sellerDisputes",
                            SellerDisputeDAO.find { SellerDisputeTable.userId eq user.id }.map { it.toPrivacyMap() },
                        )
                        put(
                            "analyticsEvents",
                            AnalyticsEventDAO.find { AnalyticsEventTable.userId eq user.id }.map { it.toPrivacyMap() },
                        )
                        put(
                            "cartItems",
                            CartItemDAO.find { CartItemTable.userId eq user.id }.map {
                                mapOf(
                                    "productId" to it.productId.value,
                                    "variantId" to it.variantId?.value.orEmpty(),
                                    "quantity" to it.quantity.toString(),
                                )
                            },
                        )
                        put(
                            "wishlist",
                            WishlistDAO.find { WishlistTable.userId eq user.id }.map {
                                mapOf("productId" to it.productId.value, "createdAt" to it.createdAt.toUtcString())
                            },
                        )
                        put(
                            "stockAlerts",
                            StockAlertDAO.find { StockAlertTable.userId eq user.id }.map {
                                mapOf(
                                    "productId" to it.productId.value,
                                    "variantId" to it.variantId?.value.orEmpty(),
                                    "notifiedAt" to (it.notifiedAt?.toUtcString() ?: ""),
                                )
                            },
                        )
                        put(
                            "notificationPreferences",
                            NotificationPreferenceDAO.find { NotificationPreferenceTable.userId eq user.id }.map {
                                mapOf("eventType" to it.eventType, "channel" to it.channel)
                            },
                        )
                        put(
                            "apiKeys",
                            ApiKeyDAO.find { ApiKeyTable.userId eq user.id }.map {
                                mapOf(
                                    "id" to it.id.value,
                                    "name" to it.name,
                                    "prefix" to it.prefix,
                                    "scopes" to it.scopes,
                                    "isActive" to it.isActive.toString(),
                                    "createdAt" to it.createdAt.toUtcString(),
                                )
                            },
                        )
                        put(
                            "couponUsage",
                            CouponUsageDAO.find { CouponUsageTable.userId eq user.id }.map {
                                mapOf(
                                    "couponId" to it.couponId.value,
                                    "orderId" to it.orderId?.value.orEmpty(),
                                    "usedAt" to it.usedAt.toUtcString(),
                                )
                            },
                        )
                        put(
                            "taxExemptions",
                            TaxExemptionDAO.find { TaxExemptionTable.userId eq user.id }.map {
                                mapOf(
                                    "id" to it.id.value,
                                    "businessTaxId" to it.businessTaxId.orEmpty(),
                                    "country" to it.country.orEmpty(),
                                    "expiresAt" to (it.expiresAt?.toUtcString() ?: ""),
                                )
                            },
                        )
                        put(
                            "walletAccount",
                            WalletAccountDAO.find { WalletAccountTable.userId eq user.id }.map {
                                mapOf(
                                    "creditBalance" to it.creditBalance.toPlainString(),
                                    "pointsBalance" to it.pointsBalance.toString(),
                                    "lifetimePoints" to it.lifetimePoints.toString(),
                                    "referralCode" to it.referralCode,
                                )
                            },
                        )
                    },
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
            val previousEmail = user.email

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
            WishlistDAO.find { WishlistTable.userId eq user.id }.forEach { it.delete() }
            ShippingAddressDAO.find { ShippingAddressTable.userId eq user.id }.forEach { it.delete() }
            PolicyConsentDAO.find { PolicyConsentTable.userId eq user.id }.forEach { it.delete() }
            ReviewHelpfulVoteDAO.find { ReviewHelpfulVoteTable.userId eq user.id }.forEach { it.delete() }
            ReviewRatingDAO.find { ReviewRatingTable.userId eq user.id }.forEach { it.delete() }
            NotificationDAO.find { NotificationTable.userId eq user.id }.forEach { it.delete() }
            NotificationPreferenceDAO.find { NotificationPreferenceTable.userId eq user.id }.forEach { it.delete() }
            StockAlertDAO.find { StockAlertTable.userId eq user.id }.forEach { it.delete() }
            ApiKeyDAO.find { ApiKeyTable.userId eq user.id }.forEach { it.delete() }
            CouponUsageDAO.find { CouponUsageTable.userId eq user.id }.forEach { it.delete() }
            TaxExemptionDAO.find { TaxExemptionTable.userId eq user.id }.forEach { it.delete() }
            OtpAttemptDAO.find { OtpAttemptTable.userId eq user.id }.forEach { it.delete() }
            PasskeyChallengeDAO.find { PasskeyChallengeTable.userId eq user.id }.forEach { it.delete() }
            LoginAttemptDAO.find { LoginAttemptTable.email eq previousEmail }.forEach { it.delete() }

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

            // Remove credentials, support content, analytics and self-service entitlements.
            PasskeyCredentialDAO.find { PasskeyCredentialTable.userId eq user.id }.forEach { it.delete() }
            SupportMessageDAO.find { SupportMessageTable.senderId eq user.id }.forEach { it.body = "[deleted]" }
            SupportTicketDAO.find { SupportTicketTable.userId eq user.id }.forEach { it.delete() }
            DigitalEntitlementDAO.find { DigitalEntitlementTable.userId eq user.id }.forEach { it.delete() }
            AnalyticsEventDAO.find { AnalyticsEventTable.userId eq user.id }.forEach { it.delete() }
            WalletLedgerDAO.find { WalletLedgerTable.userId eq user.id }.forEach { it.delete() }
            WalletAccountDAO.find { WalletAccountTable.userId eq user.id }.forEach { it.delete() }
            SellerReviewDAO.find { SellerReviewTable.userId eq user.id }.forEach { it.delete() }
            SellerDisputeDAO.find { SellerDisputeTable.userId eq user.id }.forEach { it.delete() }
            SubscriptionDAO.find { SubscriptionTable.userId eq user.id }.forEach {
                it.status = com.piashcse.constants.SubscriptionStatus.CANCELED
                it.cancelAtPeriodEnd = false
            }
            B2bAccountDAO.find { B2bAccountTable.userId eq user.id }.forEach {
                it.companyName = "Deleted account"
                it.taxId = null
                it.contactName = null
                it.phone = null
                it.status = com.piashcse.constants.B2bAccountStatus.SUSPENDED
            }
            user.socialProvider = null
            user.socialSubject = null
            user.mfaSecret = null
            user.mfaEnabled = false
            user.mfaLastUsedStep = null

            // Retain financial records but strip direct PII and delivery snapshots.
            OrderDAO.find { OrderTable.userId eq user.id }.forEach { order ->
                order.shippingAddress = null
                order.billingAddress = null
                order.contactEmail = null
                order.notes = null
                order.attributionReferrer = null
                order.pickupCode = null
            }
            true
        }

    private fun WalletLedgerDAO.toPrivacyMap() =
        mapOf(
            "id" to id.value,
            "type" to type.name,
            "pointsDelta" to pointsDelta.toString(),
            "creditDelta" to creditDelta.toPlainString(),
            "referenceType" to referenceType.orEmpty(),
            "referenceId" to referenceId.orEmpty(),
            "createdAt" to createdAt.toUtcString(),
        )

    private fun GiftCardRedemptionDAO.toPrivacyMap() =
        mapOf("id" to id.value, "giftCardId" to giftCardId.value, "orderId" to orderId.value, "amount" to amount.toPlainString())

    private fun SubscriptionDAO.toPrivacyMap() =
        mapOf(
            "id" to id.value,
            "planId" to planId.value,
            "status" to status.name,
            "currentPeriodEnd" to currentPeriodEnd.toUtcString(),
            "cancelAtPeriodEnd" to cancelAtPeriodEnd.toString(),
        )

    private fun SubscriptionInvoiceDAO.toPrivacyMap() =
        mapOf(
            "id" to id.value,
            "subscriptionId" to subscriptionId.value,
            "amount" to amount.toPlainString(),
            "currency" to currency,
            "status" to status.name,
        )

    private fun SupportTicketDAO.toPrivacyMap() =
        mapOf("id" to id.value, "subject" to subject, "status" to status.name, "createdAt" to createdAt.toUtcString())

    private fun SupportMessageDAO.toPrivacyMap() =
        mapOf("id" to id.value, "ticketId" to ticketId.value, "body" to body, "createdAt" to createdAt.toUtcString())

    private fun DigitalEntitlementDAO.toPrivacyMap() =
        mapOf(
            "id" to id.value,
            "productId" to productId.value,
            "fileName" to fileName.orEmpty(),
            "expiresAt" to (expiresAt?.toUtcString() ?: ""),
        )

    private fun PasskeyCredentialDAO.toPrivacyMap() =
        mapOf("id" to id.value, "credentialId" to credentialId, "name" to name.orEmpty(), "createdAt" to createdAt.toUtcString())

    private fun B2bQuoteDAO.toPrivacyMap() =
        mapOf("id" to id.value, "status" to status.name, "total" to total.toPlainString(), "createdAt" to createdAt.toUtcString())

    private fun B2bPurchaseOrderDAO.toPrivacyMap() =
        mapOf("id" to id.value, "poNumber" to poNumber, "status" to status.name, "total" to total.toPlainString())

    private fun SellerReviewDAO.toPrivacyMap() =
        mapOf("id" to id.value, "sellerId" to sellerId.value, "rating" to rating.toString(), "comment" to comment.orEmpty())

    private fun SellerDisputeDAO.toPrivacyMap() =
        mapOf("id" to id.value, "subject" to subject, "status" to status.name, "createdAt" to createdAt.toUtcString())

    private fun AnalyticsEventDAO.toPrivacyMap() =
        mapOf("id" to id.value, "eventType" to eventType.name, "path" to path.orEmpty(), "occurredAt" to occurredAt.toUtcString())

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
