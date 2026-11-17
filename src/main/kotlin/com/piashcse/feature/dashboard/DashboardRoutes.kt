@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.dashboard

import com.piashcse.model.response.DashboardStatsResponse
import com.piashcse.model.response.SellerReadinessResponse
import com.piashcse.model.response.WorkQueuesResponse
import com.piashcse.plugin.ok
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.queryLimit
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
    }.describe {
        summary = "Get summary dashboard stats (revenue, orders, users, products, shops)"
        responses { ok<DashboardStatsResponse>("Dashboard summary") }
    }

    /**
     * Get detailed revenue stats with optional date range and daily breakdown
     */
    get("revenue") {
        call.respondOk(repo.getRevenueStats(call.queryParameters["startDate"], call.queryParameters["endDate"]))
    }.describe { summary = "Get detailed revenue stats with optional date range and daily breakdown" }

    /**
     * Get order statistics with status distribution
     */
    get("orders") {
        call.respondOk(repo.getOrderStats(call.queryParameters["status"]))
    }.describe { summary = "Get order statistics with status distribution" }

    /**
     * Get user growth analytics over a period
     */
    get("users") {
        call.respondOk(repo.getUserGrowth(call.queryParameters["days"]?.toIntOrNull()))
    }.describe { summary = "Get user growth analytics over a period" }

    /**
     * Get top-selling products sorted by sales volume
     */
    get("top-products") {
        call.respondOk(repo.getTopProducts(call.queryLimit(default = 10, max = 50)))
    }.describe { summary = "Get top-selling products sorted by sales volume" }

    /**
     * Get recent activity feed (orders + user registrations)
     */
    get("activity") {
        call.respondOk(repo.getRecentActivity(call.queryLimit(default = 20, max = 50)))
    }.describe { summary = "Get recent activity feed (orders + user registrations)" }
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

/** Seller onboarding/payout readiness in one call. */
fun Route.sellerReadinessRoutes() {
    val repo: DashboardRepository by inject()
    get {
        call.respondOk(repo.getSellerReadiness(call.currentUserId))
    }.describe {
        summary = "Seller readiness"
        description = "Shop approval, KYC, payout eligibility and the next required action"
        responses { ok<SellerReadinessResponse>("Seller readiness") }
    }
}

/** Aggregated actionable admin queues. */
fun Route.adminQueueRoutes() {
    val repo: DashboardRepository by inject()
    get {
        call.respondOk(repo.getAdminQueues())
    }.describe {
        summary = "Admin work queues"
        description = "Counts and oldest item for every actionable admin queue"
        responses { ok<WorkQueuesResponse>("Admin queues") }
    }
}
