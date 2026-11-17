package com.piashcse.feature.dashboard

import com.piashcse.constants.*
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
    override suspend fun getDashboardStats() =
        readQuery {
            val today = currentUtc().toLocalDate().atStartOfDay()

            val paidOrders =
                OrderTable.select(OrderTable.total, OrderTable.fxRate, OrderTable.createdAt)
                    .where {
                        (OrderTable.status neq OrderStatus.CANCELED) and
                            (OrderTable.paymentStatus eq PaymentStatus.COMPLETED)
                    }.toList()
            val totalRevenue =
                paidOrders.fold(BigDecimal.ZERO) { acc, row ->
                    acc.add(Money.toUsd(row[OrderTable.total], row[OrderTable.fxRate]))
                }
            val todayRevenue =
                paidOrders.filter { it[OrderTable.createdAt] >= today }
                    .fold(BigDecimal.ZERO) { acc, row ->
                        acc.add(Money.toUsd(row[OrderTable.total], row[OrderTable.fxRate]))
                    }

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

    override suspend fun getTopProducts(limit: Int?) =
        query {
            val maxResults = (limit ?: 10).coerceIn(1, 50)
            val topProducts =
                ProductDAO.find { (ProductTable.status eq ProductStatus.ACTIVE) and (ProductTable.deletedAt.isNull()) }
                    .orderBy(ProductTable.totalSales to SortOrder.DESC)
                    .limit(maxResults)
                    .toList()
            if (topProducts.isEmpty()) return@query emptyList<TopProductResponse>()

            val productIds = topProducts.map { it.id }
            val revenueByProduct =
                (OrderItemTable innerJoin OrderTable)
                    .select(
                        OrderItemTable.productId,
                        OrderItemTable.total,
                        OrderItemTable.discountAmount,
                        OrderTable.fxRate,
                    )
                    .where {
                        (OrderItemTable.productId inList productIds) and
                            (OrderTable.status neq OrderStatus.CANCELED) and
                            (OrderTable.paymentStatus eq PaymentStatus.COMPLETED)
                    }
                    .toList()
                    .groupBy { it[OrderItemTable.productId].value }
                    .mapValues { (_, rows) ->
                        rows.fold(BigDecimal.ZERO) { acc, row ->
                            val net = row[OrderItemTable.total].subtract(row[OrderItemTable.discountAmount])
                            acc.add(Money.toUsd(net.max(BigDecimal.ZERO), row[OrderTable.fxRate]))
                        }
                    }

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
    @Suppress("LongMethod")
    override suspend fun getSellerStats(sellerUserId: String) =
        readQuery {
            val shopIds =
                SellerDAO.find { SellerTable.userId eq sellerUserId }
                    .mapNotNull { it.shopId?.value }
                    .distinct()
            if (shopIds.isEmpty()) {
                return@readQuery DashboardStatsResponse(
                    revenue = mapOf("total" to "0.00", "today" to "0.00"),
                    orders = mapOf("total" to 0L, "today" to 0L, "pending" to 0L, "canceled" to 0L),
                    users = mapOf("total" to 0L, "today" to 0L, "sellers" to 0L),
                    products = mapOf("total" to 0L, "outOfStock" to 0L, "lowStock" to 0L),
                    shops = mapOf("total" to 0L, "pendingApproval" to 0L),
                )
            }
            val today = currentUtc().toLocalDate().atStartOfDay()
            val sellerShopIds = shopIds.map { it.entityID(ShopTable) }

            val sellerPaidOrders =
                OrderTable.selectAll().where {
                    (OrderTable.shopId inList sellerShopIds) and
                        (OrderTable.status neq OrderStatus.CANCELED) and
                        (OrderTable.paymentStatus eq PaymentStatus.COMPLETED)
                }.toList()
            val totalRevenue =
                sellerPaidOrders.fold(BigDecimal.ZERO) { acc, row ->
                    acc.add(Money.toUsd(row[OrderTable.total], row[OrderTable.fxRate]))
                }
            val todayRevenue =
                sellerPaidOrders.filter { it[OrderTable.createdAt] >= today }
                    .fold(BigDecimal.ZERO) { acc, row ->
                        acc.add(Money.toUsd(row[OrderTable.total], row[OrderTable.fxRate]))
                    }

            val customers =
                OrderTable.select(OrderTable.userId)
                    .where { OrderTable.shopId inList sellerShopIds }
                    .map { it[OrderTable.userId].value }
                    .distinct()
            val todayCustomers =
                OrderTable.select(OrderTable.userId)
                    .where { (OrderTable.shopId inList sellerShopIds) and (OrderTable.createdAt greaterEq today) }
                    .map { it[OrderTable.userId].value }
                    .distinct()

            val sellerTopProducts =
                ProductDAO.find {
                    (ProductTable.shopId inList sellerShopIds) and
                        (ProductTable.status eq ProductStatus.ACTIVE) and
                        ProductTable.deletedAt.isNull()
                }.orderBy(ProductTable.totalSales to SortOrder.DESC).limit(5).toList()
            val sellerProductIds = sellerTopProducts.map { it.id }
            val sellerRevenueByProduct =
                if (sellerProductIds.isEmpty()) {
                    emptyMap()
                } else {
                    (OrderItemTable innerJoin OrderTable)
                        .select(
                            OrderItemTable.productId,
                            OrderItemTable.total,
                            OrderItemTable.discountAmount,
                            OrderTable.fxRate,
                        )
                        .where {
                            (OrderItemTable.productId inList sellerProductIds) and
                                (OrderTable.shopId inList sellerShopIds) and
                                (OrderTable.status neq OrderStatus.CANCELED) and
                                (OrderTable.paymentStatus eq PaymentStatus.COMPLETED)
                        }
                        .toList()
                        .groupBy { it[OrderItemTable.productId].value }
                        .mapValues { (_, rows) ->
                            rows.fold(BigDecimal.ZERO) { acc, row ->
                                val net = row[OrderItemTable.total].subtract(row[OrderItemTable.discountAmount])
                                acc.add(Money.toUsd(net.max(BigDecimal.ZERO), row[OrderTable.fxRate]))
                            }
                        }
                }
            val sellerStock =
                if (sellerProductIds.isEmpty()) {
                    emptyMap()
                } else {
                    InventoryDAO.find { InventoryTable.productId inList sellerProductIds }
                        .groupBy { it.productId.value }
                        .mapValues { (_, rows) -> rows.sumOf { it.stockQuantity } }
                }
            val sellerTopResponses =
                sellerTopProducts.map { p ->
                    TopProductResponse(
                        p.id.value,
                        p.name,
                        p.sku,
                        p.totalSales,
                        Money.str(sellerRevenueByProduct[p.id.value] ?: BigDecimal.ZERO),
                        sellerStock[p.id.value] ?: 0,
                        Money.str(p.rating),
                        p.status.name.lowercase(),
                    )
                }

            val sellerOrderTotal = OrderTable.selectAll().where { OrderTable.shopId inList sellerShopIds }.count()
            val sellerOrderToday =
                OrderTable.selectAll().where {
                    (OrderTable.shopId inList sellerShopIds) and (OrderTable.createdAt greaterEq today)
                }.count()
            val sellerOrderPending =
                OrderTable.selectAll().where {
                    (OrderTable.shopId inList sellerShopIds) and (OrderTable.status eq OrderStatus.PENDING)
                }.count()
            val sellerOrderCanceled =
                OrderTable.selectAll().where {
                    (OrderTable.shopId inList sellerShopIds) and (OrderTable.status eq OrderStatus.CANCELED)
                }.count()
            val sellerUnfulfilled =
                OrderTable.selectAll().where {
                    (OrderTable.shopId inList sellerShopIds) and (OrderTable.status eq OrderStatus.PAID)
                }.count()
            val sellerProductTotal =
                ProductTable.selectAll().where {
                    (ProductTable.shopId inList sellerShopIds) and ProductTable.deletedAt.isNull()
                }.count()
            val sellerOutOfStock =
                ProductTable.selectAll().where {
                    (ProductTable.shopId inList sellerShopIds) and
                        (ProductTable.status eq ProductStatus.OUT_OF_STOCK) and ProductTable.deletedAt.isNull()
                }.count()
            val sellerLowStock =
                InventoryTable.selectAll().where {
                    (InventoryTable.shopId inList sellerShopIds) and (InventoryTable.status eq InventoryStatus.LOW_STOCK)
                }.count()
            val sellerIdList = SellerDAO.find { SellerTable.shopId inList sellerShopIds }.map { it.id }
            val pendingRefunds =
                if (sellerIdList.isEmpty()) {
                    0L
                } else {
                    val sellerOrderIds = OrderDAO.find { OrderTable.shopId inList sellerShopIds }.map { it.id }
                    RefundRequestDAO.find {
                        (RefundRequestTable.orderId inList sellerOrderIds) and (RefundRequestTable.status eq RefundStatus.PENDING)
                    }.count()
                }
            val pendingRmas =
                RmaDAO.find {
                    (RmaTable.shopId inList sellerShopIds) and (RmaTable.status eq RmaStatus.REQUESTED)
                }.count()
            val unreadReviews =
                SellerReviewDAO.find {
                    (SellerReviewTable.shopId inList sellerShopIds) and (SellerReviewTable.status eq ReviewStatus.ACTIVE)
                }.count()
            val payoutRows =
                if (sellerIdList.isEmpty()) {
                    emptyList()
                } else {
                    SellerPayoutDAO.find {
                        (SellerPayoutTable.sellerId inList sellerIdList) and
                            (SellerPayoutTable.status eq PayoutStatus.PENDING.name) and
                            SellerPayoutTable.requestedAt.isNull()
                    }.toList()
                }
            val availablePayout = payoutRows.fold(BigDecimal.ZERO) { acc, row -> acc.add(row.payoutAmount) }
            val sellerRecord = SellerDAO.find { SellerTable.userId eq sellerUserId.entityID(UserTable) }.firstOrNull()
            val shopRecord = shopIds.firstOrNull()?.let { ShopDAO.findById(it) }
            val pendingSellerShops =
                ShopTable.selectAll().where {
                    (ShopTable.id inList sellerShopIds) and (ShopTable.status eq ShopStatus.PENDING)
                }.count()
            DashboardStatsResponse(
                revenue = mapOf("total" to Money.str(totalRevenue), "today" to Money.str(todayRevenue)),
                orders =
                    mapOf(
                        "total" to OrderTable.selectAll().where { OrderTable.shopId inList sellerShopIds }.count(),
                        "today" to
                            OrderTable.selectAll().where {
                                (OrderTable.shopId inList sellerShopIds) and (OrderTable.createdAt greaterEq today)
                            }.count(),
                        "pending" to
                            OrderTable.selectAll().where {
                                (OrderTable.shopId inList sellerShopIds) and (OrderTable.status eq OrderStatus.PENDING)
                            }.count(),
                        "canceled" to
                            OrderTable.selectAll().where {
                                (OrderTable.shopId inList sellerShopIds) and (OrderTable.status eq OrderStatus.CANCELED)
                            }.count(),
                    ),
                users =
                    mapOf(
                        "total" to customers.size.toLong(),
                        "today" to todayCustomers.size.toLong(),
                        "sellers" to SellerDAO.find { SellerTable.shopId inList sellerShopIds }.count(),
                    ),
                products =
                    mapOf(
                        "total" to
                            ProductTable.selectAll().where {
                                (ProductTable.shopId inList sellerShopIds) and ProductTable.deletedAt.isNull()
                            }.count(),
                        "outOfStock" to
                            ProductTable.selectAll().where {
                                (ProductTable.shopId inList sellerShopIds) and
                                    (ProductTable.status eq ProductStatus.OUT_OF_STOCK) and
                                    ProductTable.deletedAt.isNull()
                            }.count(),
                        "lowStock" to
                            InventoryTable.selectAll().where {
                                (InventoryTable.shopId inList sellerShopIds) and
                                    (InventoryTable.status eq InventoryStatus.LOW_STOCK)
                            }.count(),
                    ),
                shops =
                    mapOf(
                        "total" to shopIds.size.toLong(),
                        "pendingApproval" to
                            ShopTable.selectAll().where {
                                (ShopTable.id inList sellerShopIds) and (ShopTable.status eq ShopStatus.PENDING)
                            }.count(),
                    ),
                topProducts = sellerTopResponses,
                revenueStats = DashboardRevenueStats(Money.str(totalRevenue), Money.str(todayRevenue)),
                orderStats =
                    DashboardOrderStats(
                        sellerOrderTotal,
                        sellerOrderToday,
                        sellerOrderPending,
                        sellerOrderCanceled,
                        sellerUnfulfilled,
                    ),
                userStats = DashboardUserStats(customers.size.toLong(), todayCustomers.size.toLong(), sellerIdList.size.toLong()),
                productStats = DashboardProductStats(sellerProductTotal, sellerOutOfStock, sellerLowStock),
                shopStats = DashboardShopStats(shopIds.size.toLong(), pendingSellerShops),
                sellerExtras =
                    SellerDashboardExtras(
                        kycStatus = sellerRecord?.kycStatus?.name,
                        shopStatus = shopRecord?.status?.name,
                        availablePayout = Money.str(availablePayout),
                        payoutCurrency = payoutRows.firstOrNull()?.currency ?: "USD",
                        pendingRefunds = pendingRefunds,
                        pendingRmas = pendingRmas,
                        activeReviews = unreadReviews,
                        unfulfilledOrders = sellerUnfulfilled,
                    ),
            )
        }

    override suspend fun getRecentActivity(limit: Int?) =
        query {
            val max = (limit ?: 20).coerceIn(1, 50)
            val orders =
                OrderTable.selectAll().orderBy(OrderTable.createdAt to SortOrder.DESC).limit(max).map {
                    val dao = OrderDAO.wrapRow(it)
                    RecentActivityResponse(
                        dao.id.value,
                        "order",
                        "Order ${dao.orderNumber} created - \$${Money.str(dao.total)}",
                        dao.status.name.lowercase(),
                        dao.createdAt.toUtcString(),
                    )
                }
            val users =
                UserTable.selectAll().orderBy(UserTable.createdAt to SortOrder.DESC).limit(max).map {
                    val dao = UserDAO.wrapRow(it)
                    RecentActivityResponse(
                        dao.id.value,
                        "user",
                        "New ${dao.userType.name.lowercase()} registered: ${dao.email}",
                        if (dao.isVerified) "verified" else "unverified",
                        dao.createdAt.toUtcString(),
                    )
                }
            (orders + users).sortedByDescending { it.createdAt }.take(max)
        }

    override suspend fun getAdminQueues() =
        readQuery {
            fun oldest(
                column: org.jetbrains.exposed.v1.core.Column<*>,
                query: org.jetbrains.exposed.v1.jdbc.Query,
            ): String? =
                query.orderBy(column to SortOrder.ASC)
                    .limit(1)
                    .firstOrNull()
                    ?.let { row -> (row[column] as? java.time.LocalDateTime)?.toUtcString() }

            val pendingShops = ShopTable.selectAll().where { ShopTable.status eq ShopStatus.PENDING }
            val pendingKyc = SellerTable.selectAll().where { SellerTable.kycStatus eq KycStatus.PENDING }
            val requestedPayouts = PayoutRequestTable.selectAll().where { PayoutRequestTable.status eq PayoutStatus.REQUESTED.name }
            val pendingRefunds = RefundRequestTable.selectAll().where { RefundRequestTable.status eq RefundStatus.PENDING }
            val requestedRmas = RmaTable.selectAll().where { RmaTable.status eq RmaStatus.REQUESTED }
            val openDisputes =
                SellerDisputeTable.selectAll().where {
                    SellerDisputeTable.status inList listOf(DisputeStatus.OPEN, DisputeStatus.UNDER_REVIEW)
                }
            val chargebacks =
                ChargebackTable.selectAll().where {
                    ChargebackTable.status inList listOf(ChargebackStatus.OPEN, ChargebackStatus.UNDER_REVIEW)
                }
            val failedWebhooks =
                PaymentWebhookEventTable.selectAll().where { PaymentWebhookEventTable.status eq WebhookEventStatus.FAILED }
            val deadLetters = OutboxDeadLetterTable.selectAll()
            val lowStock = InventoryTable.selectAll().where { InventoryTable.status eq InventoryStatus.LOW_STOCK }

            WorkQueuesResponse(
                listOf(
                    WorkQueueItem(
                        "shops",
                        "Pending shop approvals",
                        pendingShops.count(),
                        oldest(ShopTable.createdAt, pendingShops),
                        "/admin/shops/status?status=PENDING",
                    ),
                    WorkQueueItem(
                        "kyc",
                        "Pending KYC reviews",
                        pendingKyc.count(),
                        oldest(SellerTable.kycSubmittedAt, pendingKyc),
                        "/admin/sellers?status=PENDING",
                    ),
                    WorkQueueItem(
                        "payouts",
                        "Requested payouts",
                        requestedPayouts.count(),
                        oldest(PayoutRequestTable.createdAt, requestedPayouts),
                        "/admin/payouts/requests?status=REQUESTED",
                    ),
                    WorkQueueItem(
                        "refunds",
                        "Pending refunds",
                        pendingRefunds.count(),
                        oldest(RefundRequestTable.createdAt, pendingRefunds),
                        "/admin/refund-requests?status=PENDING",
                    ),
                    WorkQueueItem(
                        "rmas",
                        "Requested returns",
                        requestedRmas.count(),
                        oldest(RmaTable.createdAt, requestedRmas),
                        "/admin/rma-requests?status=REQUESTED",
                    ),
                    WorkQueueItem(
                        "disputes",
                        "Open disputes",
                        openDisputes.count(),
                        oldest(SellerDisputeTable.createdAt, openDisputes),
                        "/admin/disputes?status=OPEN",
                    ),
                    WorkQueueItem(
                        "chargebacks",
                        "Open chargebacks",
                        chargebacks.count(),
                        oldest(ChargebackTable.evidenceDueAt, chargebacks),
                        "/admin/chargebacks?status=OPEN",
                    ),
                    WorkQueueItem(
                        "webhooks",
                        "Failed webhooks",
                        failedWebhooks.count(),
                        oldest(PaymentWebhookEventTable.receivedAt, failedWebhooks),
                        "/admin/webhooks?status=FAILED",
                    ),
                    WorkQueueItem(
                        "outbox",
                        "Dead letters",
                        deadLetters.count(),
                        oldest(OutboxDeadLetterTable.createdAt, deadLetters),
                        "/admin/outbox/dead-letters",
                    ),
                    WorkQueueItem(
                        "inventory",
                        "Low-stock items",
                        lowStock.count(),
                        oldest(InventoryTable.updatedAt, lowStock),
                        "/admin/inventories?lowStock=true",
                    ),
                ),
            )
        }

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
                currency = payoutRows.firstOrNull()?.currency ?: "USD",
                missingRequirements = missing.distinct(),
                nextAction = nextAction,
                shopRejectionReason = shop?.rejectionReason,
                shopSuspendedReason = shop?.suspendedReason,
                kycRejectionReason = seller.kycRejectionReason,
            )
        }
}
