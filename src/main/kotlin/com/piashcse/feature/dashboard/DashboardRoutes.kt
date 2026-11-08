@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.dashboard

import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.respondOk
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.koin.ktor.ext.inject

fun Route.dashboardAdminRoutes() {
    val repo: DashboardRepository by inject()
    /**
     * Get summary dashboard stats (revenue, orders, users, products, shops)
     */
    get {
        call.respondOk(repo.getDashboardStats())
    }

    /**
     * Get detailed revenue stats with optional date range and daily breakdown
     */
    get("revenue") {
        call.respondOk(repo.getRevenueStats(call.queryParameters["startDate"], call.queryParameters["endDate"]))
    }

    /**
     * Get order statistics with status distribution
     */
    get("orders") {
        call.respondOk(repo.getOrderStats(call.queryParameters["status"]))
    }

    /**
     * Get user growth analytics over a period
     */
    get("users") {
        call.respondOk(repo.getUserGrowth(call.queryParameters["days"]?.toIntOrNull()))
    }

    /**
     * Get top-selling products sorted by sales volume
     */
    get("top-products") {
        call.respondOk(repo.getTopProducts(call.queryParameters["limit"]?.toIntOrNull()))
    }

    /**
     * Get recent activity feed (orders + user registrations)
     */
    get("activity") {
        call.respondOk(repo.getRecentActivity(call.queryParameters["limit"]?.toIntOrNull()))
    }
}

fun Route.dashboardSellerRoutes() {
    val repo: DashboardRepository by inject()
    get {
        call.respondOk(repo.getSellerStats(call.currentUserId))
    }.describe {
        summary = "Seller dashboard"
        description = "Sales, orders, stock and top products"
    }
}
