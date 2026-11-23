package com.piashcse.feature.dashboard

import com.piashcse.constants.*
import com.piashcse.constants.AppConstants
import com.piashcse.database.entities.*
import com.piashcse.model.response.*
import com.piashcse.utils.common.Money
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.readQuery
import org.jetbrains.exposed.v1.core.*
import java.math.BigDecimal

class DashboardRepositoryImpl : DashboardRepository {
    private val sellerDashboard = SellerDashboardService()

    override suspend fun getDashboardStats() = AdminDashboardStatsService.getDashboardStats()

    override suspend fun getRevenueStats(
        startDate: String?,
        endDate: String?,
    ) = AdminDashboardStatsService.getRevenueStats(startDate, endDate)

    override suspend fun getOrderStats(status: String?) = AdminDashboardStatsService.getOrderStats(status)

    override suspend fun getUserGrowth(days: Int?) = AdminDashboardStatsService.getUserGrowth(days)

    override suspend fun getTopProducts(limit: Int?) =
        readQuery {
            val maxResults = (limit ?: 10).coerceIn(1, 50)
            val topProducts =
                ProductDAO.find { (ProductTable.status eq ProductStatus.ACTIVE) and (ProductTable.deletedAt.isNull()) }
                    .orderBy(ProductTable.totalSales to SortOrder.DESC)
                    .limit(maxResults)
                    .toList()
            if (topProducts.isEmpty()) return@readQuery emptyList<TopProductResponse>()

            val productIds = topProducts.map { it.id }
            val revenueByProduct = DashboardRevenue.productRevenueByProduct(productIds)

            val inventoryMap =
                if (productIds.isNotEmpty()) {
                    InventoryDAO.find { InventoryTable.productId inList productIds }
                        .groupBy { it.productId.value }
                        .mapValues { (_, rows) -> rows.sumOf { it.stockQuantity } }
                } else {
                    emptyMap()
                }

            topProducts.map { p ->
                val topStock = inventoryMap[p.id.value] ?: 0
                TopProductResponse(
                    p.id.value,
                    p.name,
                    p.sku,
                    p.totalSales,
                    Money.str(revenueByProduct[p.id.value] ?: BigDecimal.ZERO),
                    topStock,
                    Money.str(p.rating),
                    p.status.name.lowercase(),
                )
            }
        }

    // Dashboard aggregates intentionally run in one read transaction; splitting the
    // query block would fragment the snapshot used by the seller console.
    override suspend fun getSellerStats(sellerUserId: String) = sellerDashboard.getSellerStats(sellerUserId)

    override suspend fun getRecentActivity(limit: Int?) = AdminDashboardService.getRecentActivity(limit)

    override suspend fun getAdminQueues() = AdminDashboardService.getAdminQueues()

    override suspend fun getSellerReadiness(sellerUserId: String) =
        readQuery {
            val seller = SellerDAO.find { SellerTable.userId eq sellerUserId.entityID(UserTable) }.firstOrNull()
            if (seller == null) {
                return@readQuery SellerReadinessResponse(
                    shopStatus = null,
                    kycStatus = KycStatus.NOT_SUBMITTED.name,
                    payoutEligible = false,
                    payoutBlockedReason = "No seller profile",
                    missingRequirements = listOf("SELLER_PROFILE"),
                    nextAction = "REGISTER_AS_SELLER",
                )
            }
            val shop = seller.shopId?.let { ShopDAO.findById(it.value) }
            val payoutRows =
                SellerPayoutDAO.find {
                    (SellerPayoutTable.sellerId eq seller.id) and
                        (SellerPayoutTable.status eq PayoutStatus.PENDING.name) and
                        SellerPayoutTable.requestedAt.isNull()
                }.toList()
            val available = payoutRows.fold(BigDecimal.ZERO) { acc, row -> acc.add(row.payoutAmount) }
            val shopApproved = shop?.status == ShopStatus.APPROVED
            val kycVerified = seller.kycStatus == KycStatus.VERIFIED
            val missing =
                buildList {
                    if (shop == null) add("SHOP")
                    if (shop?.status == ShopStatus.PENDING) add("SHOP_APPROVAL")
                    if (shop?.status == ShopStatus.REJECTED) add("SHOP_FIX_REJECTION")
                    if (seller.kycStatus == KycStatus.NOT_SUBMITTED) add("KYC_SUBMISSION")
                    if (seller.kycStatus == KycStatus.REJECTED) add("KYC_FIX_REJECTION")
                    if (seller.kycDocumentUrl.isNullOrBlank()) add("KYC_DOCUMENT")
                    if (seller.bankAccountNumber.isNullOrBlank() || seller.bankName.isNullOrBlank()) add("BANK_DETAILS")
                }
            val nextAction =
                when {
                    shop == null -> "CREATE_SHOP"
                    shop.status == ShopStatus.REJECTED -> "FIX_SHOP_REJECTION"
                    shop.status == ShopStatus.PENDING -> "AWAIT_SHOP_APPROVAL"
                    seller.kycStatus in listOf(KycStatus.NOT_SUBMITTED, KycStatus.REJECTED) -> "SUBMIT_KYC"
                    seller.kycStatus == KycStatus.PENDING -> "AWAIT_KYC_APPROVAL"
                    !shopApproved || !kycVerified -> "CONTACT_SUPPORT"
                    else -> null
                }
            SellerReadinessResponse(
                shopStatus = shop?.status?.name,
                kycStatus = seller.kycStatus.name,
                payoutEligible = shopApproved && kycVerified,
                payoutBlockedReason =
                    when {
                        !shopApproved -> "Shop is not approved"
                        !kycVerified -> "KYC is not verified"
                        else -> null
                    },
                availablePayout = Money.str(available),
                currency = payoutRows.firstOrNull()?.currency ?: AppConstants.DEFAULT_CURRENCY,
                missingRequirements = missing.distinct(),
                nextAction = nextAction,
                shopRejectionReason = shop?.rejectionReason,
                shopSuspendedReason = shop?.suspendedReason,
                kycRejectionReason = seller.kycRejectionReason,
            )
        }
}
