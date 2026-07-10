package com.piashcse.feature.dashboard


import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.piashcse.utils.extension.*
import org.koin.ktor.ext.inject

fun Route.dashboardAdminRoutes() {
    val repo: DashboardRepository by inject()
    /**
     * @tag Dashboard
     * @description Get summary dashboard stats (revenue, orders, users, products, shops)
     */
    get {
        call.respondOk(repo.getDashboardStats())
    }

    /**
     * @tag Dashboard
     * @description Get detailed revenue stats with optional date range and daily breakdown
     */
    get("revenue") {
        call.respondOk(repo.getRevenueStats(call.queryParameters["startDate"], call.queryParameters["endDate"]))
    }

    /**
     * @tag Dashboard
     * @description Get order statistics with status distribution
     */
    get("orders") {
        call.respondOk(repo.getOrderStats(call.queryParameters["status"]))
    }

    /**
     * @tag Dashboard
     * @description Get user growth analytics over a period
     */
    get("users") {
        call.respondOk(repo.getUserGrowth(call.queryParameters["days"]?.toIntOrNull()))
    }

    /**
     * @tag Dashboard
     * @description Get top-selling products sorted by sales volume
     */
    get("top-products") {
        call.respondOk(repo.getTopProducts(call.queryParameters["limit"]?.toIntOrNull()))
    }

    /**
     * @tag Dashboard
     * @description Get recent activity feed (orders + user registrations)
     */
    get("activity") {
        call.respondOk(repo.getRecentActivity(call.queryParameters["limit"]?.toIntOrNull()))
    }
}
