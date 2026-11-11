package com.piashcse.feature.subscription

import com.piashcse.constants.InvoiceStatus
import com.piashcse.constants.PaymentProvider
import com.piashcse.constants.SubscriptionStatus
import com.piashcse.database.entities.SubscriptionDAO
import com.piashcse.database.entities.SubscriptionInvoiceDAO
import com.piashcse.database.entities.SubscriptionInvoiceTable
import com.piashcse.database.entities.SubscriptionPlanDAO
import com.piashcse.database.entities.SubscriptionTable
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.feature.payment.ManualPaymentGateway
import com.piashcse.feature.payment.PaymentGateway
import com.piashcse.service.AppMetrics
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.or
import java.math.BigDecimal
import java.time.LocalDateTime

data class SubscriptionBillingResult(
    val invoicesCreated: Int,
    val invoicesPaid: Int,
    val paymentFailures: Int,
    val expired: Int,
    val canceled: Int,
)

/**
 * Recurring subscription billing and dunning.
 *
 * The first charge is completed by the client (paymentReference) or admin activation.
 * Renewals use the stored provider customer/payment-method references for an off-session
 * charge. Failures move the subscription to PAST_DUE, retry with backoff, and eventually
 * expire access after the grace period. Paid provider webhooks are applied idempotently
 * through [applyPaidInvoiceTx].
 */
class SubscriptionBillingService(
    private val digitalDelivery: DigitalDeliveryService = DigitalDeliveryService(),
    private val gateway: PaymentGateway = ManualPaymentGateway(),
    private val graceDays: Long = 3,
    private val maxAttempts: Int = 3,
) {
    private data class RenewalSnapshot(
        val subscriptionId: String,
        val amount: BigDecimal,
        val currency: String,
        val invoiceId: String,
        val attemptCount: Int,
        val paymentMethodReference: String?,
        val providerCustomerReference: String?,
        val chargeProvider: PaymentProvider,
    )

    private sealed interface Loaded {
        data class Ready(val snapshot: RenewalSnapshot) : Loaded

        data object Expired : Loaded

        data object Canceled : Loaded

        data object Skipped : Loaded
    }

    suspend fun run(now: LocalDateTime = currentUtc()): SubscriptionBillingResult {
        val dueIds =
            query {
                val active =
                    SubscriptionDAO.find {
                        (SubscriptionTable.status eq SubscriptionStatus.ACTIVE) and
                            (SubscriptionTable.currentPeriodEnd lessEq now)
                    }.map { it.id.value }
                val pastDue =
                    SubscriptionDAO.find {
                        SubscriptionTable.status eq SubscriptionStatus.PAST_DUE
                    }.map { it.id.value }
                (active + pastDue).distinct()
            }
        var created = 0
        var paid = 0
        var failed = 0
        var expired = 0
        var canceled = 0
        dueIds.forEach { id ->
            when (val result = bill(id, now)) {
                is RenewalOutcome.Created -> created += 1
                is RenewalOutcome.Paid -> {
                    created += 1
                    paid += 1
                }
                is RenewalOutcome.Failed -> {
                    created += 1
                    failed += 1
                }
                RenewalOutcome.Expired -> expired += 1
                RenewalOutcome.Canceled -> canceled += 1
                RenewalOutcome.Skipped -> Unit
            }
        }
        return SubscriptionBillingResult(created, paid, failed, expired, canceled)
    }

    private sealed interface RenewalOutcome {
        data class Created(val invoiceId: String) : RenewalOutcome

        data class Paid(val invoiceId: String) : RenewalOutcome

        data class Failed(val invoiceId: String) : RenewalOutcome

        data object Expired : RenewalOutcome

        data object Canceled : RenewalOutcome

        data object Skipped : RenewalOutcome
    }

    private suspend fun bill(
        subscriptionId: String,
        now: LocalDateTime,
    ): RenewalOutcome {
        val snapshot =
            query {
                val subscription =
                    SubscriptionDAO.find { SubscriptionTable.id eq subscriptionId.entityID(SubscriptionTable) }
                        .forUpdate()
                        .firstOrNull() ?: return@query Loaded.Skipped
                val plan = SubscriptionPlanDAO.findById(subscription.planId.value) ?: return@query Loaded.Skipped
                if (subscription.cancelAtPeriodEnd && !subscription.currentPeriodEnd.isAfter(now)) {
                    subscription.status = SubscriptionStatus.CANCELED
                    digitalDelivery.revokeSubscriptionTx(subscription.id.value)
                    return@query Loaded.Canceled
                }
                val dueInvoice =
                    SubscriptionInvoiceDAO.find {
                        (SubscriptionInvoiceTable.subscriptionId eq subscription.id) and
                            (
                                SubscriptionInvoiceTable.status inList
                                    listOf(InvoiceStatus.PENDING, InvoiceStatus.FAILED)
                            ) and
                            (
                                SubscriptionInvoiceTable.nextRetryAt.isNull() or
                                    (SubscriptionInvoiceTable.nextRetryAt lessEq now)
                            )
                    }.orderBy(SubscriptionInvoiceTable.createdAt to SortOrder.DESC)
                        .forUpdate()
                        .firstOrNull()
                val invoice =
                    dueInvoice ?: if (subscription.status == SubscriptionStatus.ACTIVE) {
                        SubscriptionInvoiceDAO.new {
                            this.subscriptionId = subscription.id
                            amount = plan.price
                            currency = plan.currency
                            status = InvoiceStatus.PENDING
                            periodStart = subscription.currentPeriodEnd
                            periodEnd = periodEnd(subscription.currentPeriodEnd, plan)
                        }
                    } else {
                        null
                    }
                if (invoice == null) {
                    if (subscription.graceUntil != null && !subscription.graceUntil!!.isAfter(now)) {
                        subscription.status = SubscriptionStatus.EXPIRED
                        digitalDelivery.revokeSubscriptionTx(subscription.id.value)
                        return@query Loaded.Expired
                    }
                    return@query Loaded.Skipped
                }
                val graceExpired = subscription.graceUntil != null && !subscription.graceUntil!!.isAfter(now)
                if (invoice.status == InvoiceStatus.FAILED && (invoice.attemptCount >= maxAttempts || graceExpired)) {
                    subscription.status = SubscriptionStatus.EXPIRED
                    digitalDelivery.revokeSubscriptionTx(subscription.id.value)
                    return@query Loaded.Expired
                }
                if (subscription.status == SubscriptionStatus.ACTIVE) {
                    subscription.status = SubscriptionStatus.PAST_DUE
                    subscription.graceUntil = now.plusDays(graceDays)
                }
                Loaded.Ready(
                    RenewalSnapshot(
                        subscriptionId = subscription.id.value,
                        amount = invoice.amount,
                        currency = invoice.currency,
                        invoiceId = invoice.id.value,
                        attemptCount = invoice.attemptCount,
                        paymentMethodReference = subscription.paymentMethodReference,
                        providerCustomerReference = subscription.providerCustomerReference,
                        chargeProvider = gateway.provider,
                    ),
                )
            }
        val ready =
            when (snapshot) {
                Loaded.Canceled -> return RenewalOutcome.Canceled
                Loaded.Expired -> return RenewalOutcome.Expired
                Loaded.Skipped -> return RenewalOutcome.Skipped
                is Loaded.Ready -> snapshot.snapshot
            }

        if (ready.paymentMethodReference.isNullOrBlank() || ready.chargeProvider == PaymentProvider.MANUAL) {
            return RenewalOutcome.Created(ready.invoiceId)
        }

        val charge =
            runCatching {
                gateway.chargeSubscription(
                    subscriptionId = ready.subscriptionId,
                    invoiceId = ready.invoiceId,
                    amount = ready.amount,
                    currency = ready.currency,
                    providerCustomerReference = ready.providerCustomerReference,
                    paymentMethodReference = ready.paymentMethodReference,
                    idempotencyKey = "renew:${ready.invoiceId}:${ready.attemptCount + 1}",
                )
            }.getOrElse { error ->
                return recordFailure(ready, now, error.message ?: "Subscription charge failed")
            }
        if (!charge.success) {
            return recordFailure(ready, now, charge.message ?: "Subscription charge failed")
        }
        if (!charge.captured) {
            query {
                val invoice = SubscriptionInvoiceDAO.findById(ready.invoiceId) ?: return@query
                invoice.attemptCount += 1
                invoice.lastAttemptAt = now
                invoice.nextRetryAt = now.plusDays(1)
                invoice.failureReason = charge.providerStatus ?: charge.message ?: "Payment requires action"
                SubscriptionDAO.findById(ready.subscriptionId)?.let {
                    it.status = SubscriptionStatus.PAST_DUE
                    if (it.graceUntil == null) it.graceUntil = now.plusDays(graceDays)
                }
            }
            AppMetrics.subscriptionBillingFailed.increment()
            return RenewalOutcome.Failed(ready.invoiceId)
        }

        query {
            applyPaidInvoiceTx(ready.invoiceId, charge.gatewayTransactionId)
        }
        AppMetrics.subscriptionBillingSucceeded.increment()
        return RenewalOutcome.Paid(ready.invoiceId)
    }

    private suspend fun recordFailure(
        ready: RenewalSnapshot,
        now: LocalDateTime,
        reason: String,
    ): RenewalOutcome {
        val expired =
            query {
                val invoice = SubscriptionInvoiceDAO.findById(ready.invoiceId) ?: return@query false
                invoice.status = InvoiceStatus.FAILED
                invoice.attemptCount += 1
                invoice.lastAttemptAt = now
                invoice.failureReason = reason
                invoice.nextRetryAt = now.plusDays(invoice.attemptCount.toLong())
                val subscription = SubscriptionDAO.findById(ready.subscriptionId) ?: return@query false
                subscription.dunningAttempts += 1
                subscription.status = SubscriptionStatus.PAST_DUE
                if (subscription.graceUntil == null) subscription.graceUntil = now.plusDays(graceDays)
                if (invoice.attemptCount >= maxAttempts) {
                    subscription.status = SubscriptionStatus.EXPIRED
                    digitalDelivery.revokeSubscriptionTx(subscription.id.value)
                    true
                } else {
                    false
                }
            }
        AppMetrics.subscriptionBillingFailed.increment()
        return if (expired) RenewalOutcome.Expired else RenewalOutcome.Failed(ready.invoiceId)
    }

    /** Applies a provider/admin-confirmed payment to an invoice. Safe to call twice. */
    suspend fun applyInvoicePayment(
        invoiceId: String,
        providerReference: String?,
    ): Boolean = query { applyPaidInvoiceTx(invoiceId, providerReference) }

    /** Must run inside the invoice transaction. */
    fun applyPaidInvoiceTx(
        invoiceId: String,
        providerReference: String?,
    ): Boolean {
        val invoice =
            SubscriptionInvoiceDAO.find {
                SubscriptionInvoiceTable.id eq invoiceId.entityID(SubscriptionInvoiceTable)
            }.forUpdate().firstOrNull() ?: return false
        if (invoice.status == InvoiceStatus.PAID) return false
        val subscription =
            SubscriptionDAO.find { SubscriptionTable.id eq invoice.subscriptionId }
                .forUpdate()
                .firstOrNull() ?: return false
        val plan = SubscriptionPlanDAO.findById(subscription.planId.value)
        invoice.status = InvoiceStatus.PAID
        invoice.providerReference = providerReference
        invoice.paidAt = currentUtc()
        invoice.failureReason = null
        invoice.nextRetryAt = null
        subscription.currentPeriodStart = invoice.periodStart
        subscription.currentPeriodEnd = invoice.periodEnd
        subscription.status = SubscriptionStatus.ACTIVE
        subscription.dunningAttempts = 0
        subscription.graceUntil = null
        subscription.externalReference = providerReference
        plan?.let {
            digitalDelivery.grantSubscriptionTx(subscription.userId.value, it.productId.value, subscription.id.value)
        }
        return true
    }

    /** Applies a provider failure event. Safe to call twice. */
    fun applyFailedInvoiceTx(
        invoiceId: String,
        reason: String,
    ): Boolean {
        val invoice =
            SubscriptionInvoiceDAO.find {
                SubscriptionInvoiceTable.id eq invoiceId.entityID(SubscriptionInvoiceTable)
            }.forUpdate().firstOrNull() ?: return false
        if (invoice.status == InvoiceStatus.PAID) return false
        val subscription = SubscriptionDAO.findById(invoice.subscriptionId.value) ?: return false
        invoice.status = InvoiceStatus.FAILED
        invoice.failureReason = reason
        invoice.attemptCount += 1
        invoice.lastAttemptAt = currentUtc()
        invoice.nextRetryAt = currentUtc().plusDays(invoice.attemptCount.toLong())
        subscription.status = SubscriptionStatus.PAST_DUE
        if (subscription.graceUntil == null) subscription.graceUntil = currentUtc().plusDays(graceDays)
        if (invoice.attemptCount >= maxAttempts) {
            subscription.status = SubscriptionStatus.EXPIRED
            digitalDelivery.revokeSubscriptionTx(subscription.id.value)
        }
        return true
    }

    private fun periodEnd(
        start: LocalDateTime,
        plan: SubscriptionPlanDAO,
    ): LocalDateTime = if (plan.interval == com.piashcse.constants.BillingInterval.YEAR) start.plusYears(1) else start.plusMonths(1)
}
