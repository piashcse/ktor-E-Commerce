package com.piashcse.feature.dashboard

import com.piashcse.model.response.*

class DashboardService(private val dashboardRepo: DashboardRepository) {
    suspend fun getDashboardStats(): DashboardStatsResponse = dashboardRepo.getDashboardStats()

    suspend fun getRevenueStats(startDate: String?, endDate: String?): RevenueStatsResponse =
        dashboardRepo.getRevenueStats(startDate, endDate)

    suspend fun getOrderStats(status: String?): OrderStatsResponse = dashboardRepo.getOrderStats(status)

    suspend fun getUserGrowth(days: Int?): UserGrowthResponse = dashboardRepo.getUserGrowth(days)

    suspend fun getTopProducts(limit: Int?): List<TopProductResponse> = dashboardRepo.getTopProducts(limit)

    suspend fun getRecentActivity(limit: Int?): List<RecentActivityResponse> = dashboardRepo.getRecentActivity(limit)
}
