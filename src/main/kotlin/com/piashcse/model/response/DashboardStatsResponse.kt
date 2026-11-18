package com.piashcse.model.response

import kotlinx.serialization.Serializable

@Serializable data class DashboardStatsResponse(
    val revenue: Map<String, String>,
    val orders: Map<String, Long>,
    val users: Map<String, Long>,
    val products: Map<String, Long>,
    val shops: Map<String, Long>,
    val topProducts: List<TopProductResponse> = emptyList(),
    /** Typed mirrors of the legacy maps; clients should prefer these. */
    val revenueStats: DashboardRevenueStats? = null,
    val orderStats: DashboardOrderStats? = null,
    val userStats: DashboardUserStats? = null,
    val productStats: DashboardProductStats? = null,
    val shopStats: DashboardShopStats? = null,
    val sellerExtras: SellerDashboardExtras? = null,
)

@Serializable data class DashboardRevenueStats(val total: String, val today: String = "0.00")

@Serializable
data class DashboardOrderStats(
    val total: Long,
    val today: Long = 0,
    val pending: Long = 0,
    val canceled: Long = 0,
    val unfulfilled: Long = 0,
)

@Serializable data class DashboardUserStats(val total: Long, val today: Long = 0, val sellers: Long = 0)

@Serializable data class DashboardProductStats(val total: Long, val outOfStock: Long = 0, val lowStock: Long = 0)

@Serializable data class DashboardShopStats(val total: Long, val pendingApproval: Long = 0)

@Serializable
data class SellerDashboardExtras(
    val kycStatus: String? = null,
    val shopStatus: String? = null,
    val availablePayout: String = "0.00",
    val payoutCurrency: String = "USD",
    val pendingRefunds: Long = 0,
    val pendingRmas: Long = 0,
    val activeReviews: Long = 0,
    val unfulfilledOrders: Long = 0,
)

@Serializable data class RevenueStatsResponse(
    val totalRevenue: String,
    val totalOrders: Long,
    val averageOrderValue: String,
    val dailyRevenue: List<Map<String, String>>,
    val currency: String = "USD",
)

@Serializable data class OrderStatsResponse(val statusDistribution: Map<String, Long>, val recentOrders: List<Map<String, String>>)

@Serializable data class DailySignupEntry(val date: String, val count: Long)

@Serializable data class UserGrowthResponse(
    val totalUsers: Long,
    val newUsersInPeriod: Long,
    val periodDays: Int,
    val byUserType: Map<String, Long>,
    val dailySignups: List<DailySignupEntry>,
)

@Serializable data class TopProductResponse(
    val productId: String,
    val name: String,
    val sku: String,
    val totalSales: Int,
    val totalRevenue: String,
    val stockQuantity: Int,
    val rating: String,
    val status: String,
)

@Serializable data class RecentActivityResponse(
    val id: String,
    val type: String,
    val summary: String,
    val status: String,
    val createdAt: String,
)

@Serializable
data class WorkQueueItem(
    val key: String,
    val label: String,
    val count: Long,
    val oldestAt: String? = null,
    val path: String,
)

@Serializable data class WorkQueuesResponse(val queues: List<WorkQueueItem>)

@Serializable
data class SellerReadinessResponse(
    val shopStatus: String?,
    val kycStatus: String,
    val payoutEligible: Boolean,
    val payoutBlockedReason: String? = null,
    val availablePayout: String = "0.00",
    val currency: String = "USD",
    val missingRequirements: List<String> = emptyList(),
    val nextAction: String? = null,
    val shopRejectionReason: String? = null,
    val shopSuspendedReason: String? = null,
    val kycRejectionReason: String? = null,
)
