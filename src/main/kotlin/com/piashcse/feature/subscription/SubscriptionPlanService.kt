package com.piashcse.feature.subscription

import com.piashcse.constants.SubscriptionStatus
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.SubscriptionDAO
import com.piashcse.database.entities.SubscriptionPlanDAO
import com.piashcse.database.entities.SubscriptionPlanTable
import com.piashcse.database.entities.SubscriptionTable
import com.piashcse.event.AdminAudit
import com.piashcse.utils.common.Money
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.toPaginatedResponse
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

/** Subscription plan catalog and admin mutations. */
internal class SubscriptionPlanService {
    private fun SubscriptionPlanDAO.toPlanResponse() =
        SubscriptionPlanResponse(
            id = id.value,
            name = name,
            description = description,
            productId = productId.value,
            interval = interval,
            price = Money.str(price),
            currency = currency,
            trialDays = trialDays,
            isActive = isActive,
        )

    suspend fun plans(productId: String?): List<SubscriptionPlanResponse> =
        query {
            SubscriptionPlanTable.selectAll()
                .where { SubscriptionPlanTable.isActive eq true }
                .also { query ->
                    productId?.let {
                        query.andWhere {
                            SubscriptionPlanTable.productId eq it.entityID(com.piashcse.database.entities.ProductTable)
                        }
                    }
                }
                .map { SubscriptionPlanDAO.wrapRow(it).toPlanResponse() }
        }

    suspend fun listPlans(
        limit: Int,
        offset: Int,
    ): PaginatedResponse<SubscriptionPlanResponse> =
        query {
            SubscriptionPlanTable.selectAll()
                .toPaginatedResponse(limit, offset) { SubscriptionPlanDAO.wrapRow(it).toPlanResponse() }
        }

    suspend fun createPlan(
        request: SubscriptionPlanRequest,
        actorUserId: String? = null,
    ): SubscriptionPlanResponse =
        query {
            ProductDAO.findById(request.productId) ?: request.productId.throwNotFound("Product")
            SubscriptionPlanDAO.new {
                name = request.name
                description = request.description
                productId = request.productId.entityID(com.piashcse.database.entities.ProductTable)
                interval = request.interval
                price = request.price
                currency = request.currency.uppercase()
                trialDays = request.trialDays
                isActive = request.isActive
            }.also {
                actorUserId?.let { actor ->
                    AdminAudit.publish(actor, "SUBSCRIPTION_PLAN_CREATED", "SUBSCRIPTION_PLAN", it.id.value, it.name)
                }
            }.toPlanResponse()
        }

    suspend fun updatePlan(
        id: String,
        request: SubscriptionPlanRequest,
        actorUserId: String? = null,
    ): SubscriptionPlanResponse =
        query {
            ProductDAO.findById(request.productId) ?: request.productId.throwNotFound("Product")
            val plan = SubscriptionPlanDAO.findById(id) ?: id.throwNotFound("Subscription plan")
            val hasLiveSubscribers =
                SubscriptionDAO.find {
                    (SubscriptionTable.planId eq plan.id) and
                        (
                            SubscriptionTable.status inList
                                listOf(
                                    SubscriptionStatus.PENDING,
                                    SubscriptionStatus.ACTIVE,
                                    SubscriptionStatus.PAST_DUE,
                                )
                        )
                }.any()
            if (hasLiveSubscribers) {
                throw ValidationException(
                    "Plan terms cannot be changed while it has pending/active subscribers",
                )
            }
            plan.apply {
                name = request.name
                description = request.description
                productId = request.productId.entityID(com.piashcse.database.entities.ProductTable)
                interval = request.interval
                price = request.price
                currency = request.currency.uppercase()
                trialDays = request.trialDays
                isActive = request.isActive
            }.also {
                actorUserId?.let { actor ->
                    AdminAudit.publish(actor, "SUBSCRIPTION_PLAN_UPDATED", "SUBSCRIPTION_PLAN", it.id.value, it.name)
                }
            }.toPlanResponse()
        }

    suspend fun deletePlan(
        id: String,
        actorUserId: String? = null,
    ): Boolean =
        query {
            val plan = SubscriptionPlanDAO.findById(id) ?: return@query false
            plan.isActive = false
            actorUserId?.let { AdminAudit.publish(it, "SUBSCRIPTION_PLAN_DEACTIVATED", "SUBSCRIPTION_PLAN", id, plan.name) }
            true
        }
}
