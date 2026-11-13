package com.piashcse.feature.kyc

import com.piashcse.constants.KycStatus
import com.piashcse.database.entities.SellerDAO
import com.piashcse.database.entities.SellerTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.UserTable
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.paginateWithPreload
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.requireSellerByUserId
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.validator.ValidationException
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

@Serializable
data class SellerKycSubmitRequest(
    val businessName: String? = null,
    val businessRegistrationNumber: String? = null,
    val taxId: String? = null,
    val bankAccountNumber: String? = null,
    val bankName: String? = null,
    val bankRoutingNumber: String? = null,
    val documentUrl: String? = null,
)

@Serializable
data class SellerKycResponse(
    val sellerId: String,
    val userId: String,
    val email: String?,
    val businessName: String?,
    val businessRegistrationNumber: String?,
    val taxId: String?,
    val bankName: String?,
    val kycStatus: KycStatus,
    val documentUrl: String?,
    val submittedAt: String?,
    val verifiedAt: String?,
    val rejectionReason: String?,
    val shopStatus: String?,
)

class KycService {
    suspend fun get(sellerUserId: String): SellerKycResponse =
        query {
            requireSellerByUserId(sellerUserId).toResponse()
        }

    suspend fun submit(
        sellerUserId: String,
        request: SellerKycSubmitRequest,
    ): SellerKycResponse =
        query {
            val seller = requireSellerByUserId(sellerUserId)
            if (seller.kycStatus == KycStatus.VERIFIED) {
                throw ValidationException("KYC is already verified")
            }
            if (request.businessName.isNullOrBlank() && request.documentUrl.isNullOrBlank()) {
                throw ValidationException("Provide business details or a KYC document")
            }
            request.businessName?.let { seller.businessName = it }
            request.businessRegistrationNumber?.let { seller.businessRegistrationNumber = it }
            request.taxId?.let { seller.taxId = it }
            request.bankAccountNumber?.let { seller.bankAccountNumber = it }
            request.bankName?.let { seller.bankName = it }
            request.bankRoutingNumber?.let { seller.bankRoutingNumber = it }
            request.documentUrl?.let { seller.kycDocumentUrl = it }
            seller.kycStatus = KycStatus.PENDING
            seller.kycSubmittedAt = currentUtc()
            seller.kycVerifiedAt = null
            seller.kycRejectionReason = null
            seller.toResponse()
        }

    suspend fun list(
        status: KycStatus?,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<SellerKycResponse> =
        query {
            val query = SellerTable.selectAll()
            status?.let { query.andWhere { SellerTable.kycStatus eq it } }
            query.orderBy(SellerTable.createdAt to SortOrder.DESC)
                .paginateWithPreload(limit, offset, rowMapper = { SellerDAO.wrapRow(it) }) { sellers ->
                    val emails =
                        UserDAO.find { UserTable.id inList sellers.map { it.userId } }
                            .associate { it.id.value to it.email }
                    sellers.map { it.toResponse(emails[it.userId.value]) }
                }
        }

    suspend fun verify(sellerId: String): SellerKycResponse =
        query {
            val seller = SellerDAO.findById(sellerId) ?: sellerId.throwNotFound("Seller")
            if (seller.kycStatus != KycStatus.PENDING) {
                throw ValidationException("Only PENDING KYC submissions can be verified")
            }
            if (seller.kycDocumentUrl.isNullOrBlank()) {
                throw ValidationException("A KYC document is required before verification")
            }
            if (seller.bankAccountNumber.isNullOrBlank() || seller.bankName.isNullOrBlank()) {
                throw ValidationException("Bank account details are required before verification")
            }
            seller.kycStatus = KycStatus.VERIFIED
            seller.kycVerifiedAt = currentUtc()
            seller.kycRejectionReason = null
            seller.toResponse()
        }

    suspend fun reject(
        sellerId: String,
        reason: String,
    ): SellerKycResponse =
        query {
            if (reason.isBlank()) throw ValidationException("Rejection reason is required")
            val seller = SellerDAO.findById(sellerId) ?: sellerId.throwNotFound("Seller")
            if (seller.kycStatus != KycStatus.PENDING) {
                throw ValidationException("Only PENDING KYC submissions can be rejected")
            }
            seller.kycStatus = KycStatus.REJECTED
            seller.kycRejectionReason = reason
            seller.kycVerifiedAt = null
            seller.toResponse()
        }

    private fun SellerDAO.toResponse(email: String? = UserDAO.findById(userId.value)?.email) =
        SellerKycResponse(
            sellerId = id.value,
            userId = userId.value,
            email = email,
            businessName = businessName,
            businessRegistrationNumber = businessRegistrationNumber,
            taxId = taxId,
            bankName = bankName,
            kycStatus = kycStatus,
            documentUrl = kycDocumentUrl,
            submittedAt = kycSubmittedAt?.toUtcString(),
            verifiedAt = kycVerifiedAt?.toUtcString(),
            rejectionReason = kycRejectionReason,
            shopStatus = status.name,
        )
}
