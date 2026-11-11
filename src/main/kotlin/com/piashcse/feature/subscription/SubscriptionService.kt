package com.piashcse.feature.subscription

import com.piashcse.constants.BillingInterval
import com.piashcse.constants.InvoiceStatus
import com.piashcse.constants.SubscriptionStatus
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.SubscriptionDAO
import com.piashcse.database.entities.SubscriptionInvoiceDAO
import com.piashcse.database.entities.SubscriptionInvoiceTable
import com.piashcse.database.entities.SubscriptionPlanDAO
import com.piashcse.database.entities.SubscriptionPlanTable
import com.piashcse.database.entities.SubscriptionTable
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.Money
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.toPaginatedResponse
import com.piashcse.utils.validator.FieldValidationException
import com.piashcse.utils.validator.FieldViolation
import com.piashcse.utils.validator.ValidationException
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal
import java.time.LocalDateTime

@Serializable
data class SubscriptionPlanRequest(
    val name: String,
    val description: String,
    val productId: String,
    val interval: BillingInterval = BillingInterval.MONTH,
    @Contextual val price: BigDecimal,
    val currency: String = "USD",
    val trialDays: Int = 0,
    val isActive: Boolean = true,
) {
    init {
        if (name.isBlank()) throw FieldValidationException(listOf(FieldViolation("name", "Name is required")))
        if (price <= BigDecimal.ZERO) {
            throw FieldValidationException(listOf(FieldViolation("price", "Price must be positive")))
        }
        if (trialDays < 0) throw FieldValidationException(listOf(FieldViolation("trialDays", "Trial days cannot be negative")))
    }
}

@Serializable
data class SubscriptionPlanResponse(
    val id: String,
    val name: String,
    val description: String,
    val productId: String,
    val interval: BillingInterval,
    val price: String,
    val currency: String,
    val trialDays: Int,
    val isActive: Boolean,
)

@Serializable
data class SubscribeRequest(
    val planId: String,
    val paymentReference: String? = null,
)

@Serializable
data class SubscriptionResponse(
    val id: String,
    val userId: String,
    val planId: String,
    val planName: String,
    val productId: String,
    val status: SubscriptionStatus,
    val startedAt: String,
    val currentPeriodStart: String,
    val currentPeriodEnd: String,
    val cancelAtPeriodEnd: Boolean,
    val externalReference: String?,
)

@Serializable
data class BillingRunResponse(
    val renewed: Int,
    val canceled: Int,
)

class SubscriptionService(
    private val digitalDelivery: DigitalDeliveryService = DigitalDeliveryService(),
) {
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

    suspend fun createPlan(request: SubscriptionPlanRequest): SubscriptionPlanResponse =
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
            }.toPlanResponse()
        }

    suspend fun updatePlan(
        id: String,
        request: SubscriptionPlanRequest,
    ): SubscriptionPlanResponse =
        query {
            ProductDAO.findById(request.productId) ?: request.productId.throwNotFound("Product")
            val plan = SubscriptionPlanDAO.findById(id) ?: id.throwNotFound("Subscription plan")
            plan.apply {
                name = request.name
                description = request.description
                productId = request.productId.entityID(com.piashcse.database.entities.ProductTable)
                interval = request.interval
                price = request.price
                currency = request.currency.uppercase()
                trialDays = request.trialDays
                isActive = request.isActive
            }.toPlanResponse()
        }

    suspend fun deletePlan(id: String): Boolean =
        query {
            val plan = SubscriptionPlanDAO.findById(id) ?: return@query false
            plan.isActive = false
            true
        }

    suspend fun subscribe(
        userId: String,
        request: SubscribeRequest,
    ): SubscriptionResponse =
        query {
            val plan = SubscriptionPlanDAO.findById(request.planId) ?: request.planId.throwNotFound("Subscription plan")
            if (!plan.isActive) throw ValidationException("Subscription plan is not active")
            val existing =
                SubscriptionDAO.find {
                    (SubscriptionTable.userId eq userId.entityID(com.piashcse.database.entities.UserTable)) and
                        (SubscriptionTable.planId eq plan.id) and
                        (SubscriptionTable.status eq SubscriptionStatus.ACTIVE)
                }.firstOrNull()
            if (existing != null) throw ValidationException("You already have an active subscription for this plan")
            val start = currentUtc()
            val end = periodEnd(start.plusDays(plan.trialDays.toLong()), plan.interval)
            val subscription =
                SubscriptionDAO.new {
                    this.userId = userId.entityID(com.piashcse.database.entities.UserTable)
                    this.planId = plan.id
                    this.status = SubscriptionStatus.ACTIVE
                    this.startedAt = start
                    this.currentPeriodStart = start
                    this.currentPeriodEnd = end
                    this.cancelAtPeriodEnd = false
                    this.externalReference = request.paymentReference
                }
            SubscriptionInvoiceDAO.new {
                this.subscriptionId = subscription.id
                this.amount = plan.price
                this.currency = plan.currency
                this.status = InvoiceStatus.PAID
                this.periodStart = start
                this.periodEnd = end
                this.providerReference = request.paymentReference ?: "manual"
                this.paidAt = start
            }
            digitalDelivery.grantSubscriptionTx(userId, plan.productId.value, subscription.id.value)
            subscription.toResponse(plan)
        }

    suspend fun mySubscriptions(userId: String): List<SubscriptionResponse> =
        query {
            SubscriptionDAO.find { SubscriptionTable.userId eq userId.entityID(com.piashcse.database.entities.UserTable) }
                .map { subscription ->
                    val plan = SubscriptionPlanDAO.findById(subscription.planId.value)
                    subscription.toResponse(plan)
                }
        }

    suspend fun cancel(
        userId: String,
        subscriptionId: String,
    ): SubscriptionResponse =
        query {
            val subscription = SubscriptionDAO.findById(subscriptionId) ?: subscriptionId.throwNotFound("Subscription")
            if (subscription.userId.value != userId) throw ValidationException("Subscription not found")
            if (subscription.status !in listOf(SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE)) {
                throw ValidationException("Subscription cannot be canceled from ${subscription.status}")
            }
            subscription.cancelAtPeriodEnd = true
            subscription.toResponse(SubscriptionPlanDAO.findById(subscription.planId.value))
        }

    suspend fun adminSubscriptions(
        limit: Int,
        offset: Int,
    ): PaginatedResponse<SubscriptionResponse> =
        query {
            SubscriptionTable.selectAll()
                .orderBy(SubscriptionTable.createdAt to SortOrder.DESC)
                .toPaginatedResponse(limit, offset) {
                    val subscription = SubscriptionDAO.wrapRow(it)
                    subscription.toResponse(SubscriptionPlanDAO.findById(subscription.planId.value))
                }
        }

    /** Manual renewal run for due subscriptions. Extends periods and records paid invoices. */
    suspend fun runBilling(now: LocalDateTime = currentUtc()): BillingRunResponse =
        query {
            val due =
                SubscriptionDAO.find {
                    (SubscriptionTable.status eq SubscriptionStatus.ACTIVE) and
                        (SubscriptionTable.currentPeriodEnd lessEq now)
                }.toList()
            var renewed = 0
            var canceled = 0
            due.forEach { subscription ->
                val plan = SubscriptionPlanDAO.findById(subscription.planId.value) ?: return@forEach
                if (subscription.cancelAtPeriodEnd) {
                    subscription.status = SubscriptionStatus.CANCELED
                    canceled += 1
                    return@forEach
                }
                val start = subscription.currentPeriodEnd
                val end = periodEnd(start, plan.interval)
                SubscriptionInvoiceDAO.new {
                    this.subscriptionId = subscription.id
                    this.amount = plan.price
                    this.currency = plan.currency
                    this.status = InvoiceStatus.PAID
                    this.periodStart = start
                    this.periodEnd = end
                    this.providerReference = "manual-renewal"
                    this.paidAt = now
                }
                subscription.currentPeriodStart = start
                subscription.currentPeriodEnd = end
                digitalDelivery.grantSubscriptionTx(subscription.userId.value, plan.productId.value, subscription.id.value)
                renewed += 1
            }
            BillingRunResponse(renewed = renewed, canceled = canceled)
        }

    suspend fun invoicesForUser(
        userId: String,
        subscriptionId: String,
    ): List<SubscriptionInvoiceResponse> =
        query {
            val subscription = SubscriptionDAO.findById(subscriptionId) ?: subscriptionId.throwNotFound("Subscription")
            if (subscription.userId.value != userId) throw ValidationException("Subscription not found")
            SubscriptionInvoiceTable.selectAll()
                .where { SubscriptionInvoiceTable.subscriptionId eq subscription.id }
                .orderBy(SubscriptionInvoiceTable.createdAt to SortOrder.DESC)
                .map { SubscriptionInvoiceDAO.wrapRow(it).toInvoiceResponse() }
        }

    suspend fun invoices(subscriptionId: String): List<SubscriptionInvoiceResponse> =
        query {
            SubscriptionInvoiceTable.selectAll()
                .where { SubscriptionInvoiceTable.subscriptionId eq subscriptionId.entityID(SubscriptionTable) }
                .orderBy(SubscriptionInvoiceTable.createdAt to SortOrder.DESC)
                .map { SubscriptionInvoiceDAO.wrapRow(it).toInvoiceResponse() }
        }

    private fun periodEnd(
        start: LocalDateTime,
        interval: BillingInterval,
    ): LocalDateTime = if (interval == BillingInterval.YEAR) start.plusYears(1) else start.plusMonths(1)

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

    private fun SubscriptionDAO.toResponse(plan: SubscriptionPlanDAO?) =
        SubscriptionResponse(
            id = id.value,
            userId = userId.value,
            planId = planId.value,
            planName = plan?.name ?: planId.value,
            productId = plan?.productId?.value ?: "",
            status = status,
            startedAt = startedAt.toUtcString(),
            currentPeriodStart = currentPeriodStart.toUtcString(),
            currentPeriodEnd = currentPeriodEnd.toUtcString(),
            cancelAtPeriodEnd = cancelAtPeriodEnd,
            externalReference = externalReference,
        )

    private fun SubscriptionInvoiceDAO.toInvoiceResponse() =
        SubscriptionInvoiceResponse(
            id = id.value,
            subscriptionId = subscriptionId.value,
            amount = Money.str(amount),
            currency = currency,
            status = status,
            periodStart = periodStart.toUtcString(),
            periodEnd = periodEnd.toUtcString(),
            providerReference = providerReference,
            paidAt = paidAt?.toUtcString(),
        )
}

@Serializable
data class SubscriptionInvoiceResponse(
    val id: String,
    val subscriptionId: String,
    val amount: String,
    val currency: String,
    val status: InvoiceStatus,
    val periodStart: String,
    val periodEnd: String,
    val providerReference: String?,
    val paidAt: String?,
)
