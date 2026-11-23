package com.piashcse.feature.dashboard

import com.piashcse.constants.AppConstants
import com.piashcse.constants.InventoryStatus
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentStatus
import com.piashcse.constants.PayoutStatus
import com.piashcse.constants.ProductStatus
import com.piashcse.constants.RefundStatus
import com.piashcse.constants.ReviewStatus
import com.piashcse.constants.RmaStatus
import com.piashcse.constants.ShopStatus
import com.piashcse.database.entities.InventoryDAO
import com.piashcse.database.entities.InventoryTable
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderTable
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.ProductTable
import com.piashcse.database.entities.RefundRequestDAO
import com.piashcse.database.entities.RefundRequestTable
import com.piashcse.database.entities.RmaDAO
import com.piashcse.database.entities.RmaTable
import com.piashcse.database.entities.SellerDAO
import com.piashcse.database.entities.SellerPayoutDAO
import com.piashcse.database.entities.SellerPayoutTable
import com.piashcse.database.entities.SellerReviewDAO
import com.piashcse.database.entities.SellerReviewTable
import com.piashcse.database.entities.SellerTable
import com.piashcse.database.entities.ShopDAO
import com.piashcse.database.entities.ShopTable
import com.piashcse.database.entities.UserTable
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.model.response.DashboardOrderStats
import com.piashcse.model.response.DashboardProductStats
import com.piashcse.model.response.DashboardRevenueStats
import com.piashcse.model.response.DashboardShopStats
import com.piashcse.model.response.DashboardStatsResponse
import com.piashcse.model.response.DashboardUserStats
import com.piashcse.model.response.SellerDashboardExtras
import com.piashcse.model.response.TopProductResponse
import com.piashcse.utils.common.Money
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.readQuery
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal
import java.time.LocalDateTime

/** Seller-scoped dashboard aggregates. */
class SellerDashboardService {
    suspend fun getSellerStats(sellerUserId: String) =
        readQuery {
            val shopIds =
                SellerDAO.find { SellerTable.userId eq sellerUserId }
                    .mapNotNull { it.shopId?.value }
                    .distinct()
            if (shopIds.isEmpty()) return@readQuery emptySellerStats()

            val today = currentUtc().toLocalDate().atStartOfDay()
            val sellerShopIds = shopIds.map { it.entityID(ShopTable) }
            val sellerIdList = SellerDAO.find { SellerTable.shopId inList sellerShopIds }.map { it.id }
            val (totalRevenue, todayRevenue) = revenue(sellerShopIds, today)
            val (customerCount, todayCustomerCount) = customers(sellerShopIds, today)
            val orders = orderCounts(sellerShopIds, today)
            val catalog = catalogCounts(sellerShopIds)
            val support = supportCounts(sellerShopIds, sellerIdList)
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
                        "total" to orders.total,
                        "today" to orders.today,
                        "pending" to orders.pending,
                        "canceled" to orders.canceled,
                    ),
                users =
                    mapOf(
                        "total" to customerCount,
                        "today" to todayCustomerCount,
                        "sellers" to sellerIdList.size.toLong(),
                    ),
                products =
                    mapOf(
                        "total" to catalog.total,
                        "outOfStock" to catalog.outOfStock,
                        "lowStock" to catalog.lowStock,
                    ),
                shops =
                    mapOf(
                        "total" to shopIds.size.toLong(),
                        "pendingApproval" to pendingSellerShops,
                    ),
                topProducts = topProducts(sellerShopIds),
                revenueStats = DashboardRevenueStats(Money.str(totalRevenue), Money.str(todayRevenue)),
                orderStats =
                    DashboardOrderStats(
                        orders.total,
                        orders.today,
                        orders.pending,
                        orders.canceled,
                        orders.unfulfilled,
                    ),
                userStats = DashboardUserStats(customerCount, todayCustomerCount, sellerIdList.size.toLong()),
                productStats = DashboardProductStats(catalog.total, catalog.outOfStock, catalog.lowStock),
                shopStats = DashboardShopStats(shopIds.size.toLong(), pendingSellerShops),
                sellerExtras =
                    SellerDashboardExtras(
                        kycStatus = sellerRecord?.kycStatus?.name,
                        shopStatus = shopRecord?.status?.name,
                        availablePayout = Money.str(support.availablePayout),
                        payoutCurrency = support.payoutCurrency,
                        pendingRefunds = support.pendingRefunds,
                        pendingRmas = support.pendingRmas,
                        activeReviews = support.activeReviews,
                        unfulfilledOrders = orders.unfulfilled,
                    ),
            )
        }

    private fun emptySellerStats() =
        DashboardStatsResponse(
            revenue = mapOf("total" to "0.00", "today" to "0.00"),
            orders = mapOf("total" to 0L, "today" to 0L, "pending" to 0L, "canceled" to 0L),
            users = mapOf("total" to 0L, "today" to 0L, "sellers" to 0L),
            products = mapOf("total" to 0L, "outOfStock" to 0L, "lowStock" to 0L),
            shops = mapOf("total" to 0L, "pendingApproval" to 0L),
        )

    private fun revenue(
        sellerShopIds: List<EntityID<String>>,
        today: LocalDateTime,
    ): Pair<BigDecimal, BigDecimal> {
        val paidOrders =
            OrderTable.selectAll().where {
                (OrderTable.shopId inList sellerShopIds) and
                    (OrderTable.status neq OrderStatus.CANCELED) and
                    (OrderTable.paymentStatus eq PaymentStatus.COMPLETED)
            }.toList()
        val total =
            paidOrders.fold(BigDecimal.ZERO) { acc, row ->
                acc.add(Money.toUsd(row[OrderTable.total], row[OrderTable.fxRate]))
            }
        val todayTotal =
            paidOrders.filter { it[OrderTable.createdAt] >= today }
                .fold(BigDecimal.ZERO) { acc, row ->
                    acc.add(Money.toUsd(row[OrderTable.total], row[OrderTable.fxRate]))
                }
        return total to todayTotal
    }

    private fun customers(
        sellerShopIds: List<EntityID<String>>,
        today: LocalDateTime,
    ): Pair<Long, Long> {
        val all =
            OrderTable.select(OrderTable.userId)
                .where { OrderTable.shopId inList sellerShopIds }
                .map { it[OrderTable.userId].value }
                .distinct()
        val todayCount =
            OrderTable.select(OrderTable.userId)
                .where { (OrderTable.shopId inList sellerShopIds) and (OrderTable.createdAt greaterEq today) }
                .map { it[OrderTable.userId].value }
                .distinct()
        return all.size.toLong() to todayCount.size.toLong()
    }

    private fun topProducts(sellerShopIds: List<EntityID<String>>): List<TopProductResponse> {
        val rows =
            ProductDAO.find {
                (ProductTable.shopId inList sellerShopIds) and
                    (ProductTable.status eq ProductStatus.ACTIVE) and
                    ProductTable.deletedAt.isNull()
            }.orderBy(ProductTable.totalSales to SortOrder.DESC).limit(5).toList()
        val productIds = rows.map { it.id }
        val revenue = DashboardRevenue.productRevenueByProduct(productIds, sellerShopIds)
        val stock =
            if (productIds.isEmpty()) {
                emptyMap()
            } else {
                InventoryDAO.find { InventoryTable.productId inList productIds }
                    .groupBy { it.productId.value }
                    .mapValues { (_, inventory) -> inventory.sumOf { it.stockQuantity } }
            }
        return rows.map { product ->
            TopProductResponse(
                product.id.value,
                product.name,
                product.sku,
                product.totalSales,
                Money.str(revenue[product.id.value] ?: BigDecimal.ZERO),
                stock[product.id.value] ?: 0,
                Money.str(product.rating),
                product.status.name.lowercase(),
            )
        }
    }

    private fun orderCounts(
        sellerShopIds: List<EntityID<String>>,
        today: LocalDateTime,
    ): SellerOrderCounts =
        SellerOrderCounts(
            total = OrderTable.selectAll().where { OrderTable.shopId inList sellerShopIds }.count(),
            today =
                OrderTable.selectAll().where {
                    (OrderTable.shopId inList sellerShopIds) and (OrderTable.createdAt greaterEq today)
                }.count(),
            pending =
                OrderTable.selectAll().where {
                    (OrderTable.shopId inList sellerShopIds) and (OrderTable.status eq OrderStatus.PENDING)
                }.count(),
            canceled =
                OrderTable.selectAll().where {
                    (OrderTable.shopId inList sellerShopIds) and (OrderTable.status eq OrderStatus.CANCELED)
                }.count(),
            unfulfilled =
                OrderTable.selectAll().where {
                    (OrderTable.shopId inList sellerShopIds) and (OrderTable.status eq OrderStatus.PAID)
                }.count(),
        )

    private fun catalogCounts(sellerShopIds: List<EntityID<String>>): SellerCatalogCounts =
        SellerCatalogCounts(
            total =
                ProductTable.selectAll().where {
                    (ProductTable.shopId inList sellerShopIds) and ProductTable.deletedAt.isNull()
                }.count(),
            outOfStock =
                ProductTable.selectAll().where {
                    (ProductTable.shopId inList sellerShopIds) and
                        (ProductTable.status eq ProductStatus.OUT_OF_STOCK) and ProductTable.deletedAt.isNull()
                }.count(),
            lowStock =
                InventoryTable.selectAll().where {
                    (InventoryTable.shopId inList sellerShopIds) and (InventoryTable.status eq InventoryStatus.LOW_STOCK)
                }.count(),
        )

    private fun supportCounts(
        sellerShopIds: List<EntityID<String>>,
        sellerIdList: List<EntityID<String>>,
    ): SellerSupportCounts {
        val orderIds = OrderDAO.find { OrderTable.shopId inList sellerShopIds }.map { it.id }
        val pendingRefunds =
            if (sellerIdList.isEmpty()) {
                0L
            } else {
                RefundRequestDAO.find {
                    (RefundRequestTable.orderId inList orderIds) and (RefundRequestTable.status eq RefundStatus.PENDING)
                }.count()
            }
        val pendingRmas =
            RmaDAO.find {
                (RmaTable.shopId inList sellerShopIds) and (RmaTable.status eq RmaStatus.REQUESTED)
            }.count()
        val activeReviews =
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
        return SellerSupportCounts(
            pendingRefunds = pendingRefunds,
            pendingRmas = pendingRmas,
            activeReviews = activeReviews,
            availablePayout = payoutRows.fold(BigDecimal.ZERO) { acc, row -> acc.add(row.payoutAmount) },
            payoutCurrency = payoutRows.firstOrNull()?.currency ?: AppConstants.DEFAULT_CURRENCY,
        )
    }

    private data class SellerOrderCounts(
        val total: Long,
        val today: Long,
        val pending: Long,
        val canceled: Long,
        val unfulfilled: Long,
    )

    private data class SellerCatalogCounts(
        val total: Long,
        val outOfStock: Long,
        val lowStock: Long,
    )

    private data class SellerSupportCounts(
        val pendingRefunds: Long,
        val pendingRmas: Long,
        val activeReviews: Long,
        val availablePayout: BigDecimal,
        val payoutCurrency: String,
    )
}
