package com.piashcse.feature.subscription

import com.piashcse.database.entities.base.currentUtc
import com.piashcse.utils.common.PaginatedResponse
import java.time.LocalDateTime

class SubscriptionService(
    private val digitalDelivery: DigitalDeliveryService,
    private val billing: SubscriptionBillingService,
) {
    private val plans = SubscriptionPlanService()
    private val lifecycle = SubscriptionLifecycleService(billing)

    suspend fun plans(productId: String?): List<SubscriptionPlanResponse> = plans.plans(productId)

    suspend fun listPlans(
        limit: Int,
        offset: Int,
    ): PaginatedResponse<SubscriptionPlanResponse> = plans.listPlans(limit, offset)

    suspend fun createPlan(
        request: SubscriptionPlanRequest,
        actorUserId: String? = null,
    ): SubscriptionPlanResponse = plans.createPlan(request, actorUserId)

    suspend fun updatePlan(
        id: String,
        request: SubscriptionPlanRequest,
        actorUserId: String? = null,
    ): SubscriptionPlanResponse = plans.updatePlan(id, request, actorUserId)

    suspend fun deletePlan(
        id: String,
        actorUserId: String? = null,
    ): Boolean = plans.deletePlan(id, actorUserId)

    suspend fun subscribe(
        userId: String,
        request: SubscribeRequest,
    ): SubscriptionResponse = lifecycle.subscribe(userId, request)

    suspend fun mySubscriptions(userId: String): List<SubscriptionResponse> = lifecycle.mySubscriptions(userId)

    suspend fun activate(
        subscriptionId: String,
        paymentReference: String,
        actorUserId: String? = null,
    ): SubscriptionResponse = lifecycle.activate(subscriptionId, paymentReference, actorUserId)

    suspend fun cancel(
        userId: String,
        subscriptionId: String,
    ): SubscriptionResponse = lifecycle.cancel(userId, subscriptionId)

    suspend fun adminSubscriptions(
        limit: Int,
        offset: Int,
    ): PaginatedResponse<SubscriptionResponse> = lifecycle.adminSubscriptions(limit, offset)

    suspend fun runBilling(
        now: LocalDateTime = currentUtc(),
        actorUserId: String? = null,
    ): BillingRunResponse = lifecycle.runBilling(now, actorUserId)

    suspend fun invoicesForUser(
        userId: String,
        subscriptionId: String,
    ): List<SubscriptionInvoiceResponse> = lifecycle.invoicesForUser(userId, subscriptionId)

    suspend fun invoices(subscriptionId: String): List<SubscriptionInvoiceResponse> = lifecycle.invoices(subscriptionId)
}
