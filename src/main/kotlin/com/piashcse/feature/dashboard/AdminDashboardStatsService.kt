package com.piashcse.feature.dashboard

import com.piashcse.constants.*
import com.piashcse.database.entities.*
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.model.response.*
import com.piashcse.utils.common.DateTimes
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.Money
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

/** Platform revenue, order and user growth aggregates. */
internal object AdminDashboardStatsService {
    private val DFMT = DateTimeFormatter.ISO_DATE

    suspend fun getDashboardStats() =
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

    suspend fun getRevenueStats(
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

    suspend fun getOrderStats(status: String?) =
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

    suspend fun getUserGrowth(days: Int?) =
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
}
