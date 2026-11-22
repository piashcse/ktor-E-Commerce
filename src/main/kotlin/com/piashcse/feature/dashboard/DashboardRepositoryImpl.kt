package com.piashcse.feature.dashboard

import com.piashcse.constants.*
import com.piashcse.constants.AppConstants
import com.piashcse.database.entities.*
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.model.response.*
import com.piashcse.utils.common.DateTimes
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.Money
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.readQuery
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val DFMT = DateTimeFormatter.ISO_DATE

class DashboardRepositoryImpl : DashboardRepository {
    private val sellerDashboard = SellerDashboardService()

    override suspend fun getDashboardStats() =
        readQuery {
            val today = currentUtc().toLocalDate().atStartOfDay()

            val paidOrders =
                OrderTable.select(OrderTable.id, OrderTable.total, OrderTable.fxRate, OrderTable.createdAt)
                    .where {
                        (OrderTable.status neq OrderStatus.CANCELED) and
                            (OrderTable.paymentStatus eq PaymentStatus.COMPLETED)
                    }.toList()
            val orderFx = paidOrders.associate { it[OrderTable.id].value to it[OrderTable.fxRate] }
            val refunds =
                RefundRequestDAO.find { RefundRequestTable.status eq RefundStatus.REFUNDED }.toList()

            fun refundUsd(refund: RefundRequestDAO): BigDecimal {
                val amount = refund.refundAmount ?: BigDecimal.ZERO
                val total = amount.add(refund.shippingRefund).add(refund.taxRefund)
                return Money.toUsd(total, orderFx[refund.orderId.value] ?: BigDecimal.ONE)
            }
            val totalRevenue =
                paidOrders.fold(BigDecimal.ZERO) { acc, row ->
                    acc.add(Money.toUsd(row[OrderTable.total], row[OrderTable.fxRate]))
                }.subtract(refunds.fold(BigDecimal.ZERO) { acc, refund -> acc.add(refundUsd(refund)) })
            val todayRevenue =
                paidOrders.filter { it[OrderTable.createdAt] >= today }
                    .fold(BigDecimal.ZERO) { acc, row ->
                        acc.add(Money.toUsd(row[OrderTable.total], row[OrderTable.fxRate]))
                    }
                    .subtract(
                        refunds.filter { (it.resolvedAt ?: it.updatedAt ?: it.createdAt) >= today }
                            .fold(BigDecimal.ZERO) { acc, refund -> acc.add(refundUsd(refund)) },
                    )

            val orderTotal = OrderTable.selectAll().count()
            val orderToday = OrderTable.selectAll().where { OrderTable.createdAt greaterEq today }.count()
            val orderPending = OrderTable.selectAll().where { OrderTable.status eq OrderStatus.PENDING }.count()
            val orderCanceled = OrderTable.selectAll().where { OrderTable.status eq OrderStatus.CANCELED }.count()
            val unfulfilled = OrderTable.selectAll().where { OrderTable.status eq OrderStatus.PAID }.count()
            val userTotal = UserTable.selectAll().where { UserTable.isActive eq true }.count()
            val userToday =
                UserTable.selectAll().where {
                    (UserTable.isActive eq true) and (UserTable.createdAt greaterEq today)
                }.count()
            val sellerTotal =
                UserTable.selectAll().where {
                    (UserTable.isActive eq true) and (UserTable.userType eq UserType.SELLER)
                }.count()
            val productTotal = ProductTable.selectAll().where { ProductTable.deletedAt.isNull() }.count()
            val outOfStock =
                ProductTable.selectAll().where {
                    (ProductTable.deletedAt.isNull()) and (ProductTable.status eq ProductStatus.OUT_OF_STOCK)
                }.count()
            val lowStock = InventoryTable.selectAll().where { InventoryTable.status eq InventoryStatus.LOW_STOCK }.count()
            val shopTotal = ShopTable.selectAll().count()
            val pendingShops = ShopTable.selectAll().where { ShopTable.status eq ShopStatus.PENDING }.count()
            DashboardStatsResponse(
                revenue = mapOf("total" to Money.str(totalRevenue), "today" to Money.str(todayRevenue)),
                orders =
                    mapOf(
                        "total" to orderTotal,
                        "today" to orderToday,
                        "pending" to orderPending,
                        "canceled" to orderCanceled,
                    ),
                users = mapOf("total" to userTotal, "today" to userToday, "sellers" to sellerTotal),
                products = mapOf("total" to productTotal, "outOfStock" to outOfStock, "lowStock" to lowStock),
                shops = mapOf("total" to shopTotal, "pendingApproval" to pendingShops),
                revenueStats = DashboardRevenueStats(Money.str(totalRevenue), Money.str(todayRevenue)),
                orderStats = DashboardOrderStats(orderTotal, orderToday, orderPending, orderCanceled, unfulfilled),
                userStats = DashboardUserStats(userTotal, userToday, sellerTotal),
                productStats = DashboardProductStats(productTotal, outOfStock, lowStock),
                shopStats = DashboardShopStats(shopTotal, pendingShops),
            )
        }

    override suspend fun getRevenueStats(
        startDate: String?,
        endDate: String?,
    ) = readQuery {
        val now = currentUtc()
        val start =
            startDate?.let { DateTimes.parseLocalDate(it, "startDate").atStartOfDay() }
                ?: now.withDayOfMonth(1).toLocalDate().atStartOfDay()
        val end = endDate?.let { DateTimes.parseLocalDate(it, "endDate").atTime(LocalTime.MAX) } ?: now

        val rangeRows =
            OrderTable.selectAll().where {
                (OrderTable.status neq OrderStatus.CANCELED) and
                    (OrderTable.paymentStatus eq PaymentStatus.COMPLETED) and
                    (OrderTable.createdAt greaterEq start) and
                    (OrderTable.createdAt lessEq end)
            }.toList()
        val totalRevenue =
            rangeRows.fold(BigDecimal.ZERO) { acc, row ->
                acc.add(Money.toUsd(row[OrderTable.total], row[OrderTable.fxRate]))
            }
        val orderCount = rangeRows.size.toLong()
        val avg = Money.average(totalRevenue, orderCount)

        // Single range query grouped in Kotlin (was 1 query/day = N+1).
        val byDay = rangeRows.groupBy { it[OrderTable.createdAt].toLocalDate() }
        val daily =
            generateSequence(start.toLocalDate()) { it.plusDays(1) }
                .takeWhile { it <= end.toLocalDate() }
                .map { date ->
                    val dayTotal =
                        (byDay[date] ?: emptyList()).fold(BigDecimal.ZERO) { acc, row ->
                            acc.add(Money.toUsd(row[OrderTable.total], row[OrderTable.fxRate]))
                        }
                    mapOf("date" to date.format(DFMT), "revenue" to Money.str(dayTotal))
                }.toList()

        RevenueStatsResponse(Money.str(totalRevenue), orderCount, Money.str(avg), daily)
    }

    override suspend fun getOrderStats(status: String?) =
        query {
            val ordersQuery =
                if (status != null) {
                    val parsed =
                        runCatching { OrderStatus.valueOf(status.trim().uppercase()) }.getOrElse {
                            throw ValidationException("Invalid status: $status")
                        }
                    OrderTable.selectAll().where { OrderTable.status eq parsed }
                } else {
                    OrderTable.selectAll()
                }

            val statusDistribution =
                OrderStatus.values().associate { ot ->
                    ot.name.lowercase() to OrderTable.selectAll().where { OrderTable.status eq ot }.count()
                }
            val recentOrders =
                ordersQuery.orderBy(OrderTable.createdAt to SortOrder.DESC).limit(10).map {
                    val dao = OrderDAO.wrapRow(it)
                    mapOf(
                        "orderNumber" to dao.orderNumber,
                        "status" to dao.status.name.lowercase(),
                        "total" to Money.str(dao.total),
                        "createdAt" to dao.createdAt.toUtcString(),
                    )
                }
            OrderStatsResponse(statusDistribution, recentOrders)
        }

    override suspend fun getUserGrowth(days: Int?) =
        query {
            val period = (days ?: 30).coerceIn(1, 365)
            val since = currentUtc().minusDays(period.toLong())

            val byUserType =
                UserType.values().associate { type ->
                    type.name.lowercase() to UserTable.selectAll().where { UserTable.userType eq type }.count()
                }
            // Single range query grouped in Kotlin (was 1 query/day).
            val rangeUsers = UserTable.selectAll().where { UserTable.createdAt greaterEq since }.toList()
            val usersByDay = rangeUsers.groupBy { it[UserTable.createdAt].toLocalDate() }
            val dailySignups =
                generateSequence(since.toLocalDate()) { it.plusDays(1) }
                    .takeWhile { it <= LocalDate.now(ZoneOffset.UTC) }
                    .map { date ->
                        DailySignupEntry(date.format(DFMT), (usersByDay[date]?.size ?: 0).toLong())
                    }.toList()

            UserGrowthResponse(
                totalUsers = UserTable.selectAll().count(),
                newUsersInPeriod = UserTable.selectAll().where { UserTable.createdAt greaterEq since }.count(),
                periodDays = period,
                byUserType = byUserType,
                dailySignups = dailySignups,
            )
        }

    /**
     * Net product revenue (line total − seller discount) converted to USD.
     * One implementation for platform-wide and seller-scoped top products.
     */
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
