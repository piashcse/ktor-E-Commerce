package com.piashcse.database.entities

import com.piashcse.constants.ShopStatus
import com.piashcse.database.entities.base.BaseIntEntity
import com.piashcse.database.entities.base.BaseIntEntityClass
import com.piashcse.database.entities.base.BaseIntIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.datetime
import java.math.BigDecimal

object SellerTable : BaseIntIdTable("seller") {
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
    // createdAt and updatedAt are inherited from BaseIntIdTable
}

class SellerDAO(id: EntityID<String>) : BaseIntEntity(id, SellerTable) {
    companion object : BaseIntEntityClass<SellerDAO>(SellerTable, SellerDAO::class.java)

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

    fun response() = SellerResponse(
        id = id.value,
        userId = userId.value,
        shopId = shopId?.value,
        businessName = businessName,
        businessRegistrationNumber = businessRegistrationNumber,
        taxId = taxId,
        bankAccountNumber = bankAccountNumber,
        bankName = bankName,
        bankRoutingNumber = bankRoutingNumber,
        commissionRate = commissionRate,
        status = status,
        totalSales = totalSales,
        totalCommission = totalCommission,
        approvedAt = approvedAt,
        suspendedAt = suspendedAt,
        terminatedAt = terminatedAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

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
    val commissionRate: java.math.BigDecimal,
    val status: ShopStatus,
    val totalSales: java.math.BigDecimal,
    val totalCommission: java.math.BigDecimal,
    val approvedAt: java.time.LocalDateTime?,
    val suspendedAt: java.time.LocalDateTime?,
    val terminatedAt: java.time.LocalDateTime?,
    val createdAt: java.time.LocalDateTime?,
    val updatedAt: java.time.LocalDateTime?
)