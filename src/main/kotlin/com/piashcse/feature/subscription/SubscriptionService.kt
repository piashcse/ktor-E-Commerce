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
import com.piashcse.event.AdminAudit
import com.piashcse.service.AppMetrics
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
import org.jetbrains.exposed.v1.core.inList
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
    /** Stored provider payment-method token used for off-session renewals. */
    val paymentMethodReference: String? = null,
    /** Provider customer id that owns [paymentMethodReference]. */
    val providerCustomerReference: String? = null,
)

@Serializable
data class ActivateSubscriptionRequest(val paymentReference: String)

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
    val hasPaymentMethod: Boolean = false,
    val dunningAttempts: Int = 0,
    val graceUntil: String? = null,
)

@Serializable
data class BillingRunResponse(
    val invoicesCreated: Int,
    val canceled: Int,
    val paid: Int = 0,
    val failed: Int = 0,
    val expired: Int = 0,
)

class SubscriptionService(
    private val digitalDelivery: DigitalDeliveryService = DigitalDeliveryService(),
    private val billing: SubscriptionBillingService = SubscriptionBillingService(digitalDelivery),
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
            // Never activate from a client-supplied reference. Admin activation
            // (or a verified provider webhook) is the only path to PAID.
            val subscription =
                SubscriptionDAO.new {
                    this.userId = userId.entityID(com.piashcse.database.entities.UserTable)
                    this.planId = plan.id
                    this.status = SubscriptionStatus.PENDING
                    this.startedAt = start
                    this.currentPeriodStart = start
                    this.currentPeriodEnd = end
                    this.cancelAtPeriodEnd = false
                    this.externalReference = request.paymentReference
                    this.paymentMethodReference = request.paymentMethodReference
                    this.providerCustomerReference = request.providerCustomerReference
                }
            SubscriptionInvoiceDAO.new {
                this.subscriptionId = subscription.id
                this.amount = plan.price
                this.currency = plan.currency
                this.status = InvoiceStatus.PENDING
                this.periodStart = start
                this.periodEnd = end
                this.providerReference = request.paymentReference
                this.paidAt = null
            }
            AppMetrics.subscriptionsCreated.increment()
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

    suspend fun activate(
        subscriptionId: String,
        paymentReference: String,
        actorUserId: String? = null,
    ): SubscriptionResponse =
        query {
            if (paymentReference.isBlank()) throw ValidationException("paymentReference is required")
            val subscription =
                SubscriptionDAO.findById(subscriptionId)
                    ?: subscriptionId.throwNotFound("Subscription")
            val invoice =
                SubscriptionInvoiceDAO.find {
                    (SubscriptionInvoiceTable.subscriptionId eq subscription.id) and
                        (SubscriptionInvoiceTable.status inList listOf(InvoiceStatus.PENDING, InvoiceStatus.FAILED))
                }.orderBy(SubscriptionInvoiceTable.createdAt to SortOrder.DESC)
                    .firstOrNull() ?: throw ValidationException("No pending invoice for this subscription")
            if (!billing.applyPaidInvoiceTx(invoice.id.value, paymentReference)) {
                throw ValidationException("Invoice is already paid")
            }
            actorUserId?.let {
                AdminAudit.publish(
                    it,
                    "SUBSCRIPTION_ACTIVATED",
                    "SUBSCRIPTION",
                    subscription.id.value,
                    "Reference $paymentReference",
                )
            }
            val plan = SubscriptionPlanDAO.findById(subscription.planId.value)
            subscription.toResponse(plan)
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

    /**
     * Renewal run for due subscriptions. Off-session charges paid via the configured
     * provider extend the period; failures move to PAST_DUE with retries and a grace
     * period. Without a stored payment method the invoice remains pending for manual
     * collection.
     */
    suspend fun runBilling(
        now: LocalDateTime = currentUtc(),
        actorUserId: String? = null,
    ): BillingRunResponse {
        val result = billing.run(now)
        actorUserId?.let {
            AdminAudit.publish(
                it,
                "SUBSCRIPTION_BILLING_RUN",
                "SUBSCRIPTION",
                null,
                "Created ${result.invoicesCreated}, paid ${result.invoicesPaid}, " +
                    "failed ${result.paymentFailures}, expired ${result.expired}",
            )
        }
        return BillingRunResponse(
            invoicesCreated = result.invoicesCreated,
            canceled = result.canceled,
            paid = result.invoicesPaid,
            failed = result.paymentFailures,
            expired = result.expired,
        )
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
            hasPaymentMethod = !paymentMethodReference.isNullOrBlank(),
            dunningAttempts = dunningAttempts,
            graceUntil = graceUntil?.toUtcString(),
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
