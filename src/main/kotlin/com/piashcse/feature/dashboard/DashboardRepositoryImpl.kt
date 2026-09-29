package com.piashcse.feature.dashboard

import com.piashcse.constants.*
import com.piashcse.database.entities.*
import com.piashcse.model.response.*
import com.piashcse.utils.common.Money
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME
private val DFMT = DateTimeFormatter.ISO_DATE

class DashboardRepositoryImpl : DashboardRepository {
    override suspend fun getDashboardStats() =
        query {
            val today = LocalDateTime.now(ZoneOffset.UTC).toLocalDate().atStartOfDay()

            val totalRevenue =
                OrderTable.select(OrderTable.total.sum())
                    .where { OrderTable.status neq OrderStatus.CANCELED }
                    .firstOrNull()?.get(OrderTable.total.sum()) ?: BigDecimal.ZERO
            val todayRevenue =
                OrderTable.select(OrderTable.total.sum())
                    .where { (OrderTable.status neq OrderStatus.CANCELED) and (OrderTable.createdAt greaterEq today) }
                    .firstOrNull()?.get(OrderTable.total.sum()) ?: BigDecimal.ZERO

            DashboardStatsResponse(
                revenue =
                    mapOf(
                        "total" to Money.str(totalRevenue),
                        "today" to Money.str(todayRevenue),
                    ),
                orders =
                    mapOf(
                        "total" to OrderTable.selectAll().count(),
                        "today" to OrderTable.selectAll().where { OrderTable.createdAt greaterEq today }.count(),
                        "pending" to OrderTable.selectAll().where { OrderTable.status eq OrderStatus.PENDING }.count(),
                        "canceled" to OrderTable.selectAll().where { OrderTable.status eq OrderStatus.CANCELED }.count(),
                    ),
                users =
                    mapOf(
                        "total" to UserTable.selectAll().count(),
                        "today" to UserTable.selectAll().where { UserTable.createdAt greaterEq today }.count(),
                        "sellers" to UserTable.selectAll().where { UserTable.userType eq UserType.SELLER }.count(),
                    ),
                products =
                    mapOf(
                        "total" to ProductTable.selectAll().count(),
                        "outOfStock" to
                            ProductTable.selectAll().where {
                                ProductTable.status eq ProductStatus.OUT_OF_STOCK
                            }.count(),
                        "lowStock" to InventoryTable.selectAll().where { InventoryTable.status eq InventoryStatus.LOW_STOCK }.count(),
                    ),
                shops =
                    mapOf(
                        "total" to ShopTable.selectAll().count(),
                        "pendingApproval" to
                            ShopTable.selectAll().where {
                                ShopTable.status eq ShopStatus.PENDING
                            }.count(),
                    ),
            )
        }

    override suspend fun getRevenueStats(
        startDate: String?,
        endDate: String?,
    ) = query {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val start =
            startDate?.let { LocalDate.parse(it, DFMT).atStartOfDay() }
                ?: now.withDayOfMonth(1).toLocalDate().atStartOfDay()
        val end = endDate?.let { LocalDate.parse(it, DFMT).atTime(LocalTime.MAX) } ?: now

        val totalRevenue =
            OrderTable.select(OrderTable.total.sum())
                .where {
                    (OrderTable.status neq OrderStatus.CANCELED) and
                        (OrderTable.createdAt greaterEq start) and
                        (OrderTable.createdAt lessEq end)
                }
                .firstOrNull()?.get(OrderTable.total.sum()) ?: BigDecimal.ZERO
        val orderCount =
            OrderTable.selectAll().where {
                (OrderTable.status neq OrderStatus.CANCELED) and
                    (OrderTable.createdAt greaterEq start) and
                    (OrderTable.createdAt lessEq end)
            }.count()
        val avg = Money.average(totalRevenue, orderCount)

        // Single range query grouped in Kotlin (was 1 query/day = N+1).
        val rangeRows =
            OrderTable.selectAll().where {
                (OrderTable.status neq OrderStatus.CANCELED) and
                    (OrderTable.createdAt greaterEq start) and
                    (OrderTable.createdAt lessEq end)
            }.toList()
        val byDay = rangeRows.groupBy { it[OrderTable.createdAt].toLocalDate() }
        val daily =
            generateSequence(start.toLocalDate()) { it.plusDays(1) }
                .takeWhile { it <= end.toLocalDate() }
                .map { date ->
                    val dayTotal = (byDay[date] ?: emptyList()).fold(BigDecimal.ZERO) { acc, row -> acc.add(row[OrderTable.total]) }
                    mapOf("date" to date.format(DFMT), "revenue" to Money.str(dayTotal))
                }.toList()

        RevenueStatsResponse(Money.str(totalRevenue), orderCount, Money.str(avg), daily)
    }

    override suspend fun getOrderStats(status: String?) =
        query {
            val ordersQuery =
                if (status != null) {
                    OrderTable.selectAll().where { OrderTable.status eq OrderStatus.valueOf(status.uppercase()) }
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
                        "createdAt" to dao.createdAt.format(FMT),
                    )
                }
            OrderStatsResponse(statusDistribution, recentOrders)
        }

    override suspend fun getUserGrowth(days: Int?) =
        query {
            val period = days ?: 30
            val since = LocalDateTime.now(ZoneOffset.UTC).minusDays(period.toLong())

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
            val maxResults = (limit ?: 10).coerceAtMost(50)
            val topProducts =
                ProductDAO.find { ProductTable.status eq ProductStatus.ACTIVE }
                    .orderBy(ProductTable.totalSales to SortOrder.DESC)
                    .limit(maxResults)
                    .toList()
            if (topProducts.isEmpty()) return@query emptyList<TopProductResponse>()

            val productIds = topProducts.map { it.id }
            val revenueByProduct =
                OrderItemTable.select(OrderItemTable.productId, OrderItemTable.total.sum())
                    .where { OrderItemTable.productId inList productIds }
                    .groupBy(OrderItemTable.productId)
                    .toList()
                    .associate { row -> row[OrderItemTable.productId].value to (row[OrderItemTable.total.sum()] ?: BigDecimal.ZERO) }

            val inventoryMap =
                if (productIds.isNotEmpty()) {
                    InventoryDAO.find { InventoryTable.productId inList productIds }
                        .associate { it.productId.value to it.stockQuantity }
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

    override suspend fun getSellerStats(sellerUserId: String) =
        query {
            val shopId =
                SellerDAO.find { SellerTable.userId eq sellerUserId }.firstOrNull()?.shopId?.value
                    ?: return@query DashboardStatsResponse(
                        revenue = mapOf("total" to "0.00", "today" to "0.00"),
                        orders = mapOf("total" to 0, "today" to 0, "pending" to 0, "canceled" to 0),
                        users = mapOf("total" to 0, "today" to 0, "sellers" to 0),
                        products = mapOf("total" to 0, "outOfStock" to 0, "lowStock" to 0),
                        shops = mapOf("total" to 0, "pendingApproval" to 0),
                    )
            val today = LocalDateTime.now(ZoneOffset.UTC).toLocalDate().atStartOfDay()
            val shopOrders = { OrderTable.selectAll().where { OrderTable.shopId eq shopId } }
            val revenue =
                OrderTable.select(OrderTable.total.sum())
                    .where { (OrderTable.shopId eq shopId) and (OrderTable.status neq OrderStatus.CANCELED) }
                    .firstOrNull()?.get(OrderTable.total.sum()) ?: BigDecimal.ZERO
            DashboardStatsResponse(
                revenue = mapOf("total" to Money.str(revenue), "today" to "0.00"),
                orders =
                    mapOf(
                        "total" to shopOrders().count(),
                        "today" to
                            shopOrders().where {
                                OrderTable.createdAt greaterEq today
                            }.count(),
                        "pending" to
                            shopOrders().where {
                                OrderTable.status eq OrderStatus.PENDING
                            }.count(),
                        "canceled" to shopOrders().where { OrderTable.status eq OrderStatus.CANCELED }.count(),
                    ),
                users = mapOf("total" to 0, "today" to 0, "sellers" to 0),
                products =
                    mapOf(
                        "total" to ProductTable.selectAll().where { ProductTable.shopId eq shopId }.count(),
                        "outOfStock" to 0,
                        "lowStock" to 0,
                    ),
                shops = mapOf("total" to 1, "pendingApproval" to 0),
            )
        }

    override suspend fun getRecentActivity(limit: Int?) =
        query {
            val max = (limit ?: 20).coerceAtMost(50)
            val orders =
                OrderTable.selectAll().orderBy(OrderTable.createdAt to SortOrder.DESC).limit(max).map {
                    val dao = OrderDAO.wrapRow(it)
                    RecentActivityResponse(
                        dao.id.value,
                        "order",
                        "Order ${dao.orderNumber} created - \$${Money.str(dao.total)}",
                        dao.status.name.lowercase(),
                        dao.createdAt.format(FMT),
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
                        dao.createdAt.format(FMT),
                    )
                }
            (orders + users).sortedByDescending { it.createdAt }.take(max)
        }
}
