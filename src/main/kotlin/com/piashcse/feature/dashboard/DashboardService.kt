package com.piashcse.feature.dashboard

import com.piashcse.constants.*
import com.piashcse.database.entities.*
import com.piashcse.model.response.*
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.v1.core.eq
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME
private val DFMT = DateTimeFormatter.ISO_DATE

class DashboardService : DashboardRepository {

    override suspend fun getDashboardStats() = query {
        val today = LocalDateTime.now(ZoneOffset.UTC).toLocalDate().atStartOfDay()
        val allOrders = OrderDAO.all().toList()
        val active = allOrders.filter { it.status != OrderStatus.CANCELED }
        val allUsers = UserDAO.all().toList()
        val allProducts = ProductDAO.all().toList()

        DashboardStatsResponse(
            revenue = mapOf(
                "total" to active.sumOf { it.total }.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                "today" to active.filter { it.createdAt >= today }.sumOf { it.total }.setScale(2, RoundingMode.HALF_UP).toPlainString(),
            ),
            orders = mapOf(
                "total" to allOrders.size.toLong(), "today" to allOrders.count { it.createdAt >= today }.toLong(),
                "pending" to allOrders.count { it.status == OrderStatus.PENDING }.toLong(),
            ),
            users = mapOf(
                "total" to allUsers.size.toLong(), "today" to allUsers.count { it.createdAt >= today }.toLong(),
                "sellers" to allUsers.count { it.userType == UserType.SELLER }.toLong(),
            ),
            products = mapOf(
                "total" to allProducts.size.toLong(), "outOfStock" to allProducts.count { it.status == ProductStatus.OUT_OF_STOCK }.toLong(),
                "lowStock" to InventoryDAO.all().count { it.status == InventoryStatus.LOW_STOCK }.toLong(),
            ),
            shops = mapOf(
                "total" to ShopDAO.all().count(),
                "pendingApproval" to ShopDAO.all().count { it.status == ShopStatus.PENDING }.toLong(),
            ),
        )
    }

    override suspend fun getRevenueStats(startDate: String?, endDate: String?) = query {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val start = startDate?.let { LocalDate.parse(it, DFMT).atStartOfDay() } ?: now.withDayOfMonth(1).toLocalDate().atStartOfDay()
        val end = endDate?.let { LocalDate.parse(it, DFMT).atTime(LocalTime.MAX) } ?: now

        val orders = OrderDAO.all().filter { it.status != OrderStatus.CANCELED && it.createdAt in start..end }
        val totalRevenue = orders.sumOf { it.total }
        val avg = if (orders.isNotEmpty()) totalRevenue.divide(BigDecimal(orders.size), 2, RoundingMode.HALF_UP) else BigDecimal.ZERO
        val fmt = DFMT

        val daily = generateSequence(start.toLocalDate()) { it.plusDays(1) }
            .takeWhile { it <= end.toLocalDate() }
            .map { date ->
                val dayTotal = orders.filter { it.createdAt.toLocalDate() == date }.sumOf { it.total }
                mapOf("date" to date.format(fmt), "revenue" to dayTotal.setScale(2, RoundingMode.HALF_UP).toPlainString())
            }.toList()

        RevenueStatsResponse(totalRevenue.setScale(2).toPlainString(), orders.size.toLong(), avg.setScale(2).toPlainString(), daily)
    }

    override suspend fun getOrderStats(status: String?) = query {
        val orders = if (status != null) OrderDAO.find { OrderTable.status eq OrderStatus.valueOf(status.uppercase()) }.toList()
        else OrderDAO.all().toList()

        OrderStatsResponse(
            statusDistribution = OrderStatus.values().associate { it.name.lowercase() to orders.count { o -> o.status == it }.toLong() },
            recentOrders = orders.sortedByDescending { it.createdAt }.take(10).map {
                mapOf("orderNumber" to it.orderNumber, "status" to it.status.name.lowercase(),
                    "total" to it.total.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    "createdAt" to it.createdAt.format(FMT))
            },
        )
    }

    override suspend fun getUserGrowth(days: Int?) = query {
        val period = days ?: 30
        val since = LocalDateTime.now(ZoneOffset.UTC).minusDays(period.toLong())
        val allUsers = UserDAO.all().toList()

        UserGrowthResponse(
            totalUsers = allUsers.size.toLong(),
            newUsersInPeriod = allUsers.count { it.createdAt >= since }.toLong(),
            periodDays = period,
            byUserType = UserType.values().associate { it.name.lowercase() to allUsers.count { u -> u.userType == it }.toLong() },
            dailySignups = generateSequence(since.toLocalDate()) { it.plusDays(1) }
                .takeWhile { it <= LocalDate.now(ZoneOffset.UTC) }
                .map { date ->
                    val dayEnd = date.atTime(LocalTime.MAX)
                    DailySignupEntry(date.format(DFMT), allUsers.count { it.createdAt >= date.atStartOfDay() && it.createdAt <= dayEnd }.toLong())
                }.toList(),
        )
    }

    override suspend fun getTopProducts(limit: Int?) = query {
        val maxResults = (limit ?: 10).coerceAtMost(50)
        val revenueByProduct = OrderItemDAO.all().groupBy { it.productId.value }
            .mapValues { (_, items) -> items.sumOf { it.total } }

        ProductDAO.all().sortedByDescending { it.totalSales }.take(maxResults).map { p ->
            TopProductResponse(p.id.value, p.name, p.sku, p.totalSales,
                (revenueByProduct[p.id.value] ?: BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP).toPlainString(),
                p.stockQuantity, p.rating.setScale(2, RoundingMode.HALF_UP).toPlainString(), p.status.name.lowercase())
        }
    }

    override suspend fun getRecentActivity(limit: Int?) = query {
        val max = (limit ?: 20).coerceAtMost(50)
        val orders = OrderDAO.all().sortedByDescending { it.createdAt }.take(max).map {
            RecentActivityResponse(it.id.value, "order", "Order ${it.orderNumber} created - \$${it.total.setScale(2, RoundingMode.HALF_UP)}", it.status.name.lowercase(), it.createdAt.format(FMT))
        }
        val users = UserDAO.all().sortedByDescending { it.createdAt }.take(max).map {
            RecentActivityResponse(it.id.value, "user", "New ${it.userType.name.lowercase()} registered: ${it.email}", if (it.isVerified) "verified" else "unverified", it.createdAt.format(FMT))
        }
        (orders + users).sortedByDescending { it.createdAt }.take(max)
    }
}
