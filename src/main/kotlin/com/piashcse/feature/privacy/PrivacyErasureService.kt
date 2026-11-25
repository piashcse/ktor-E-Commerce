package com.piashcse.feature.privacy

import at.favre.lib.crypto.bcrypt.BCrypt
import com.piashcse.constants.AppConstants
import com.piashcse.constants.Message
import com.piashcse.database.entities.*
import com.piashcse.mapper.*
import com.piashcse.model.response.*
import com.piashcse.utils.common.AppClock
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.validator.UnauthorizedException
import org.jetbrains.exposed.v1.core.eq
import java.util.UUID

/** Account erasure: anonymize identity, delete self-service data, scrub retained records. */
class PrivacyErasureService {
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
            anonymizeAccount(user)
            deleteSelfServiceData(user, previousEmail)
            scrubRetainedRecords(user)
            true
        }

    /** Anonymizes credentials/identity fields and revokes every session. */
    private fun anonymizeAccount(user: UserDAO) {
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
        user.socialProvider = null
        user.socialSubject = null
        user.mfaSecret = null
        user.mfaEnabled = false
        user.mfaLastUsedStep = null
        RefreshTokenDAO.find { RefreshTokenTable.userId eq user.id }.forEach { it.revokedAt = AppClock.now() }

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
    }

    /** Removes self-service data the user can recreate after erasure. */
    private fun deleteSelfServiceData(
        user: UserDAO,
        previousEmail: String,
    ) {
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
        PasskeyCredentialDAO.find { PasskeyCredentialTable.userId eq user.id }.forEach { it.delete() }
        SupportMessageDAO.find { SupportMessageTable.senderId eq user.id }.forEach { it.body = "[deleted]" }
        SupportTicketDAO.find { SupportTicketTable.userId eq user.id }.forEach { it.delete() }
        DigitalEntitlementDAO.find { DigitalEntitlementTable.userId eq user.id }.forEach { it.delete() }
        AnalyticsEventDAO.find { AnalyticsEventTable.userId eq user.id }.forEach { it.delete() }
        // Seller KYC/bank/tax identifiers are the highest-risk PII in the
        // system and must not survive an erasure request.
        SellerDAO.find { SellerTable.userId eq user.id }.forEach { seller ->
            seller.businessName = null
            seller.businessRegistrationNumber = null
            seller.taxId = null
            seller.bankAccountNumber = null
            seller.bankName = null
            seller.bankRoutingNumber = null
            seller.kycDocumentUrl = null
            seller.kycStatus = com.piashcse.constants.KycStatus.NOT_SUBMITTED
            seller.kycSubmittedAt = null
            seller.kycVerifiedAt = null
            seller.kycRejectionReason = null
            KycStatusHistoryDAO.find { KycStatusHistoryTable.sellerId eq seller.id }.forEach { it.delete() }
        }
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
    }

    /** Retains financial records while stripping delivery snapshots and contact PII. */
    private fun scrubRetainedRecords(user: UserDAO) {
        OrderDAO.find { OrderTable.userId eq user.id }.forEach { order ->
            order.shippingAddress = null
            order.billingAddress = null
            order.contactEmail = null
            order.notes = null
            order.attributionReferrer = null
            order.pickupCode = null
        }
    }
}
