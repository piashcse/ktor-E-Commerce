package com.piashcse.model.response

import kotlinx.serialization.Serializable

@Serializable data class DashboardStatsResponse(val revenue: Map<String, String>, val orders: Map<String, Long>,
    val users: Map<String, Long>, val products: Map<String, Long>, val shops: Map<String, Long>)
@Serializable data class RevenueStatsResponse(val totalRevenue: String, val totalOrders: Long,
    val averageOrderValue: String, val dailyRevenue: List<Map<String, String>>, val currency: String = "USD")
@Serializable data class OrderStatsResponse(val statusDistribution: Map<String, Long>, val recentOrders: List<Map<String, String>>)
@Serializable data class DailySignupEntry(val date: String, val count: Long)
@Serializable data class UserGrowthResponse(val totalUsers: Long, val newUsersInPeriod: Long, val periodDays: Int,
    val byUserType: Map<String, Long>, val dailySignups: List<DailySignupEntry>)
@Serializable data class TopProductResponse(val productId: String, val name: String, val sku: String, val totalSales: Int,
    val totalRevenue: String, val stockQuantity: Int, val rating: String, val status: String)
@Serializable data class RecentActivityResponse(val id: String, val type: String, val summary: String,
    val status: String, val createdAt: String)
