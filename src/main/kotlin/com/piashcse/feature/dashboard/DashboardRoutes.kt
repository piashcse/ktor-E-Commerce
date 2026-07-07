package com.piashcse.feature.dashboard

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.dashboardAdminRoutes(service: DashboardService) {
    /**
     * @tag Dashboard
     * @description Get summary dashboard stats (revenue, orders, users, products, shops)
     */
    get {
        call.respond(HttpStatusCode.OK, service.getDashboardStats())
    }

    /**
     * @tag Dashboard
     * @description Get detailed revenue stats with optional date range and daily breakdown
     */
    get("revenue") {
        call.respond(
            HttpStatusCode.OK,
            service.getRevenueStats(call.queryParameters["startDate"], call.queryParameters["endDate"]),
        )
    }

    /**
     * @tag Dashboard
     * @description Get order statistics with status distribution
     */
    get("orders") {
        call.respond(HttpStatusCode.OK, service.getOrderStats(call.queryParameters["status"]))
    }

    /**
     * @tag Dashboard
     * @description Get user growth analytics over a period
     */
    get("users") {
        call.respond(HttpStatusCode.OK, service.getUserGrowth(call.queryParameters["days"]?.toIntOrNull()))
    }

    /**
     * @tag Dashboard
     * @description Get top-selling products sorted by sales volume
     */
    get("top-products") {
        call.respond(HttpStatusCode.OK, service.getTopProducts(call.queryParameters["limit"]?.toIntOrNull()))
    }

    /**
     * @tag Dashboard
     * @description Get recent activity feed (orders + user registrations)
     */
    get("activity") {
        call.respond(HttpStatusCode.OK, service.getRecentActivity(call.queryParameters["limit"]?.toIntOrNull()))
    }
}
