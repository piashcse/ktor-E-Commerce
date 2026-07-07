package com.piashcse.feature.dashboard

import com.piashcse.model.response.*

interface DashboardRepository {
    suspend fun getDashboardStats(): DashboardStatsResponse
    suspend fun getRevenueStats(startDate: String?, endDate: String?): RevenueStatsResponse
    suspend fun getOrderStats(status: String?): OrderStatsResponse
    suspend fun getUserGrowth(days: Int?): UserGrowthResponse
    suspend fun getTopProducts(limit: Int?): List<TopProductResponse>
    suspend fun getRecentActivity(limit: Int?): List<RecentActivityResponse>
}
