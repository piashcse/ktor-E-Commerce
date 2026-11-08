package com.piashcse.database.entities

import com.piashcse.constants.KycStatus
import com.piashcse.constants.ShopStatus
import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.datetime
import java.math.BigDecimal
import java.time.LocalDateTime

object SellerTable : BaseIdTable("seller") {
    val userId = reference("user_id", UserTable.id)
    val shopId = reference("shop_id", ShopTable.id).nullable()
    val businessName = varchar("business_name", 255).nullable()
    val businessRegistrationNumber = varchar("business_registration_number", 100).nullable()
    val taxId = varchar("tax_id", 100).nullable()
    val bankAccountNumber = varchar("bank_account_number", 50).nullable()
    val bankName = varchar("bank_name", 100).nullable()
    val bankRoutingNumber = varchar("bank_routing_number", 50).nullable()
    val commissionRate = decimal("commission_rate", 5, 2).default(BigDecimal("10.00")) // Default 10% commission
    val status = enumerationByName<ShopStatus>("status", 50).default(ShopStatus.PENDING) // Seller status
    val totalSales = decimal("total_sales", 12, 2).default(BigDecimal("0.00"))
    val totalCommission = decimal("total_commission", 12, 2).default(BigDecimal("0.00"))
    val approvedAt = datetime("approved_at").nullable()
    val suspendedAt = datetime("suspended_at").nullable()
    val terminatedAt = datetime("terminated_at").nullable()
    val kycStatus = enumerationByName("kyc_status", 20, KycStatus::class).default(KycStatus.NOT_SUBMITTED)
    val kycDocumentUrl = varchar("kyc_document_url", 500).nullable()
    val kycSubmittedAt = datetime("kyc_submitted_at").nullable()
    val kycVerifiedAt = datetime("kyc_verified_at").nullable()
    val kycRejectionReason = text("kyc_rejection_reason").nullable()
    // createdAt and updatedAt are inherited from BaseIdTable
}

class SellerDAO(id: EntityID<String>) : BaseEntity(id, SellerTable) {
    companion object : BaseEntityClass<SellerDAO>(SellerTable, SellerDAO::class.java)

    var userId by SellerTable.userId
    var shopId by SellerTable.shopId
    var businessName by SellerTable.businessName
    var businessRegistrationNumber by SellerTable.businessRegistrationNumber
    var taxId by SellerTable.taxId
    var bankAccountNumber by SellerTable.bankAccountNumber
    var bankName by SellerTable.bankName
    var bankRoutingNumber by SellerTable.bankRoutingNumber
    var commissionRate by SellerTable.commissionRate
    var status by SellerTable.status
    var totalSales by SellerTable.totalSales
    var totalCommission by SellerTable.totalCommission
    var approvedAt by SellerTable.approvedAt
    var suspendedAt by SellerTable.suspendedAt
    var terminatedAt by SellerTable.terminatedAt
    var kycStatus by SellerTable.kycStatus
    var kycDocumentUrl by SellerTable.kycDocumentUrl
    var kycSubmittedAt by SellerTable.kycSubmittedAt
    var kycVerifiedAt by SellerTable.kycVerifiedAt
    var kycRejectionReason by SellerTable.kycRejectionReason

    /**
     * Get the associated user information
     */
    fun getUser(): UserDAO? = UserDAO.findById(userId)

    /**
     * Check if the seller is approved
     */
    fun isApproved(): Boolean = status == ShopStatus.APPROVED

    /**
     * Check if the seller is pending approval
     */
    fun isPending(): Boolean = status == ShopStatus.PENDING

    /**
     * Check if the seller is rejected
     */
    fun isRejected(): Boolean = status == ShopStatus.REJECTED

    /**
     * Check if the seller is suspended
     */
    fun isSuspended(): Boolean = status == ShopStatus.SUSPENDED
}

@Serializable
data class SellerResponse(
    val id: String,
    val userId: String,
    val shopId: String?,
    val businessName: String?,
    val businessRegistrationNumber: String?,
    val taxId: String?,
    val bankAccountNumber: String?,
    val bankName: String?,
    val bankRoutingNumber: String?,
    val commissionRate: @Contextual BigDecimal,
    val status: ShopStatus,
    val totalSales: @Contextual BigDecimal,
    val totalCommission: @Contextual BigDecimal,
    val approvedAt: @Contextual LocalDateTime?,
    val suspendedAt: @Contextual LocalDateTime?,
    val terminatedAt: @Contextual LocalDateTime?,
    val createdAt: @Contextual LocalDateTime?,
    val updatedAt: @Contextual LocalDateTime?,
    val kycStatus: KycStatus = KycStatus.NOT_SUBMITTED,
    val kycDocumentUrl: String? = null,
    val kycSubmittedAt: @Contextual LocalDateTime? = null,
    val kycVerifiedAt: @Contextual LocalDateTime? = null,
    val kycRejectionReason: String? = null,
)
