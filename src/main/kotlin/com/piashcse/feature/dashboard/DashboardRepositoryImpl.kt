package com.piashcse.feature.dashboard

import com.piashcse.constants.*
import com.piashcse.database.entities.*
import com.piashcse.model.response.*
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.sum
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME
private val DFMT = DateTimeFormatter.ISO_DATE

class DashboardRepositoryImpl : DashboardRepository {

    override suspend fun getDashboardStats() = query {
        val today = LocalDateTime.now(ZoneOffset.UTC).toLocalDate().atStartOfDay()

        val totalRevenue = OrderTable.select(OrderTable.total.sum())
            .where { OrderTable.status neq OrderStatus.CANCELED }
            .firstOrNull()?.get(OrderTable.total.sum()) ?: BigDecimal.ZERO
        val todayRevenue = OrderTable.select(OrderTable.total.sum())
            .where { (OrderTable.status neq OrderStatus.CANCELED) and (OrderTable.createdAt greaterEq today) }
            .firstOrNull()?.get(OrderTable.total.sum()) ?: BigDecimal.ZERO

        DashboardStatsResponse(
            revenue = mapOf(
                "total" to totalRevenue.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                "today" to todayRevenue.setScale(2, RoundingMode.HALF_UP).toPlainString(),
            ),
            orders = mapOf(
                "total" to OrderTable.selectAll().count(), "today" to OrderTable.selectAll().where { OrderTable.createdAt greaterEq today }.count(),
                "pending" to OrderTable.selectAll().where { OrderTable.status eq OrderStatus.PENDING }.count(),
            ),
            users = mapOf(
                "total" to UserTable.selectAll().count(), "today" to UserTable.selectAll().where { UserTable.createdAt greaterEq today }.count(),
                "sellers" to UserTable.selectAll().where { UserTable.userType eq UserType.SELLER }.count(),
            ),
            products = mapOf(
                "total" to ProductTable.selectAll().count(), "outOfStock" to ProductTable.selectAll().where { ProductTable.status eq ProductStatus.OUT_OF_STOCK }.count(),
                "lowStock" to InventoryTable.selectAll().where { InventoryTable.status eq InventoryStatus.LOW_STOCK }.count(),
            ),
            shops = mapOf(
                "total" to ShopTable.selectAll().count(), "pendingApproval" to ShopTable.selectAll().where { ShopTable.status eq ShopStatus.PENDING }.count(),
            ),
        )
    }

    override suspend fun getRevenueStats(startDate: String?, endDate: String?) = query {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val start = startDate?.let { LocalDate.parse(it, DFMT).atStartOfDay() }
            ?: now.withDayOfMonth(1).toLocalDate().atStartOfDay()
        val end = endDate?.let { LocalDate.parse(it, DFMT).atTime(LocalTime.MAX) } ?: now

        val totalRevenue = OrderTable.select(OrderTable.total.sum())
            .where { (OrderTable.status neq OrderStatus.CANCELED) and (OrderTable.createdAt greaterEq start) and (OrderTable.createdAt lessEq end) }
            .firstOrNull()?.get(OrderTable.total.sum()) ?: BigDecimal.ZERO
        val orderCount = OrderTable.selectAll().where {
            (OrderTable.status neq OrderStatus.CANCELED) and (OrderTable.createdAt greaterEq start) and (OrderTable.createdAt lessEq end)
        }.count()
        val avg = if (orderCount > 0) totalRevenue.divide(BigDecimal(orderCount), 2, RoundingMode.HALF_UP) else BigDecimal.ZERO

        val daily = generateSequence(start.toLocalDate()) { it.plusDays(1) }
            .takeWhile { it <= end.toLocalDate() }
            .map { date ->
                val dayStart = date.atStartOfDay()
                val dayEnd = date.atTime(LocalTime.MAX)
                val dayTotal = OrderTable.select(OrderTable.total.sum())
                    .where { (OrderTable.status neq OrderStatus.CANCELED) and (OrderTable.createdAt greaterEq dayStart) and (OrderTable.createdAt lessEq dayEnd) }
                    .firstOrNull()?.get(OrderTable.total.sum()) ?: BigDecimal.ZERO
                mapOf("date" to date.format(DFMT), "revenue" to dayTotal.setScale(2, RoundingMode.HALF_UP).toPlainString())
            }.toList()

        RevenueStatsResponse(totalRevenue.setScale(2).toPlainString(), orderCount, avg.setScale(2).toPlainString(), daily)
    }

    override suspend fun getOrderStats(status: String?) = query {
        val ordersQuery = if (status != null) {
            OrderTable.selectAll().where { OrderTable.status eq OrderStatus.valueOf(status.uppercase()) }
        } else {
            OrderTable.selectAll()
        }

        val statusDistribution = OrderStatus.values().associate { ot ->
            ot.name.lowercase() to OrderTable.selectAll().where { OrderTable.status eq ot }.count()
        }
        val recentOrders = ordersQuery.orderBy(OrderTable.createdAt to SortOrder.DESC).limit(10).map {
            val dao = OrderDAO.wrapRow(it)
            mapOf(
                "orderNumber" to dao.orderNumber, "status" to dao.status.name.lowercase(),
                "total" to dao.total.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                "createdAt" to dao.createdAt.format(FMT),
            )
        }
        OrderStatsResponse(statusDistribution, recentOrders)
    }

    override suspend fun getUserGrowth(days: Int?) = query {
        val period = days ?: 30
        val since = LocalDateTime.now(ZoneOffset.UTC).minusDays(period.toLong())

        val byUserType = UserType.values().associate { it.name.lowercase() to UserTable.selectAll().where { UserTable.userType eq it }.count() }
        val dailySignups = generateSequence(since.toLocalDate()) { it.plusDays(1) }
            .takeWhile { it <= LocalDate.now(ZoneOffset.UTC) }
            .map { date ->
                val dayStart = date.atStartOfDay()
                val dayEnd = date.atTime(LocalTime.MAX)
                DailySignupEntry(date.format(DFMT), UserTable.selectAll().where {
                    (UserTable.createdAt greaterEq dayStart) and (UserTable.createdAt lessEq dayEnd)
                }.count())
            }.toList()

        UserGrowthResponse(
            totalUsers = UserTable.selectAll().count(),
            newUsersInPeriod = UserTable.selectAll().where { UserTable.createdAt greaterEq since }.count(),
            periodDays = period,
            byUserType = byUserType,
            dailySignups = dailySignups,
        )
    }

    override suspend fun getTopProducts(limit: Int?) = query {
        val maxResults = (limit ?: 10).coerceAtMost(50)
        val topProducts = ProductDAO.find { ProductTable.status eq ProductStatus.ACTIVE }
            .orderBy(ProductTable.totalSales to SortOrder.DESC)
            .limit(maxResults)
            .toList()
        if (topProducts.isEmpty()) return@query emptyList<TopProductResponse>()

        val productIds = topProducts.map { it.id }
        val revenueByProduct = OrderItemTable.select(OrderItemTable.productId, OrderItemTable.total.sum())
            .where { OrderItemTable.productId inList productIds }
            .groupBy(OrderItemTable.productId)
            .toList()
            .associate { row -> row[OrderItemTable.productId].value to (row[OrderItemTable.total.sum()] ?: BigDecimal.ZERO) }

        topProducts.map { p ->
            TopProductResponse(p.id.value, p.name, p.sku, p.totalSales,
                (revenueByProduct[p.id.value] ?: BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP).toPlainString(),
                p.stockQuantity, p.rating.setScale(2, RoundingMode.HALF_UP).toPlainString(), p.status.name.lowercase())
        }
    }

    override suspend fun getRecentActivity(limit: Int?) = query {
        val max = (limit ?: 20).coerceAtMost(50)
        val orders = OrderTable.selectAll().orderBy(OrderTable.createdAt to SortOrder.DESC).limit(max).map {
            val dao = OrderDAO.wrapRow(it)
            RecentActivityResponse(dao.id.value, "order",
                "Order ${dao.orderNumber} created - \$${dao.total.setScale(2, RoundingMode.HALF_UP)}",
                dao.status.name.lowercase(), dao.createdAt.format(FMT))
        }
        val users = UserTable.selectAll().orderBy(UserTable.createdAt to SortOrder.DESC).limit(max).map {
            val dao = UserDAO.wrapRow(it)
            RecentActivityResponse(dao.id.value, "user",
                "New ${dao.userType.name.lowercase()} registered: ${dao.email}",
                if (dao.isVerified) "verified" else "unverified", dao.createdAt.format(FMT))
        }
        (orders + users).sortedByDescending { it.createdAt }.take(max)
    }
}
