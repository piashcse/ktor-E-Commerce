package com.piashcse.feature.b2b

import com.piashcse.constants.B2bInvoiceStatus
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentMethod
import com.piashcse.constants.PaymentProvider
import com.piashcse.constants.PaymentStatus
import com.piashcse.database.entities.B2bInvoiceDAO
import com.piashcse.database.entities.B2bInvoiceTable
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.PaymentDAO
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.UserTable
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.event.AdminAudit
import com.piashcse.event.OutboxAggregate
import com.piashcse.event.OutboxPublisher
import com.piashcse.event.SendEmailEvent
import com.piashcse.feature.order.OrderCancellation
import com.piashcse.feature.order.OrderCompletion
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.Money
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.requireSellerByUserId
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.toPaginatedResponse
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.LocalDateTime

/** B2B invoice lifecycle: listing, settlement, voiding and dunning. */
class B2bInvoiceService {
    suspend fun voidInvoice(
        invoiceId: String,
        actorUserId: String? = null,
    ): B2bInvoiceResponse =
        query {
            val invoice =
                B2bInvoiceDAO.find { B2bInvoiceTable.id eq invoiceId.entityID(B2bInvoiceTable) }
                    .forUpdate()
                    .firstOrNull() ?: invoiceId.throwNotFound("B2B invoice")
            if (invoice.status == B2bInvoiceStatus.PAID) {
                throw ValidationException("Paid invoices cannot be voided")
            }
            if (invoice.status == B2bInvoiceStatus.VOID) return@query invoice.toResponse()
            invoice.orderId?.value?.let { orderId ->
                val order = OrderDAO.findById(orderId)
                if (order != null && order.status !in listOf(OrderStatus.PENDING, OrderStatus.CONFIRMED)) {
                    throw ValidationException(
                        "Invoice cannot be voided after the order was ${order.status}; cancel/credit the order instead",
                    )
                }
                if (order != null) {
                    OrderCancellation.cancel(
                        order,
                        "B2B invoice voided",
                        actorUserId,
                    )
                }
            }
            invoice.status = B2bInvoiceStatus.VOID
            invoice.nextReminderAt = null
            actorUserId?.let {
                AdminAudit.publish(it, "B2B_INVOICE_VOIDED", "B2B_INVOICE", invoice.id.value, "Invoice voided")
            }
            invoice.toResponse()
        }

    suspend fun myInvoices(
        buyerUserId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<B2bInvoiceResponse> =
        query {
            B2bInvoiceTable.selectAll()
                .where { B2bInvoiceTable.buyerUserId eq buyerUserId.entityID(UserTable) }
                .orderBy(B2bInvoiceTable.dueDate to SortOrder.ASC)
                .toPaginatedResponse(limit, offset) { B2bInvoiceDAO.wrapRow(it).toResponse() }
        }

    suspend fun sellerInvoices(
        sellerUserId: String,
        status: B2bInvoiceStatus?,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<B2bInvoiceResponse> =
        query {
            val seller = requireSellerByUserId(sellerUserId)
            B2bInvoiceTable.selectAll()
                .where { B2bInvoiceTable.sellerId eq seller.id }
                .also { query -> status?.let { query.andWhere { B2bInvoiceTable.status eq it } } }
                .orderBy(B2bInvoiceTable.dueDate to SortOrder.ASC)
                .toPaginatedResponse(limit, offset) { B2bInvoiceDAO.wrapRow(it).toResponse() }
        }

    suspend fun adminInvoices(
        status: B2bInvoiceStatus?,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<B2bInvoiceResponse> =
        query {
            B2bInvoiceTable.selectAll()
                .also { query -> status?.let { query.andWhere { B2bInvoiceTable.status eq it } } }
                .orderBy(B2bInvoiceTable.dueDate to SortOrder.ASC)
                .toPaginatedResponse(limit, offset) { B2bInvoiceDAO.wrapRow(it).toResponse() }
        }

    suspend fun markInvoicePaid(
        invoiceId: String,
        providerReference: String?,
        actorUserId: String? = null,
    ): B2bInvoiceResponse =
        query {
            val invoice =
                B2bInvoiceDAO.find { B2bInvoiceTable.id eq invoiceId.entityID(B2bInvoiceTable) }
                    .forUpdate()
                    .firstOrNull() ?: invoiceId.throwNotFound("B2B invoice")
            if (invoice.status == B2bInvoiceStatus.PAID) return@query invoice.toResponse()
            if (invoice.status == B2bInvoiceStatus.VOID) throw ValidationException("Void invoices cannot be paid")
            invoice.status = B2bInvoiceStatus.PAID
            invoice.paidAt = currentUtc()
            invoice.providerReference = providerReference
            invoice.nextReminderAt = null
            invoice.orderId?.let { orderId ->
                OrderDAO.findById(orderId.value)?.let { order ->
                    val payment =
                        PaymentDAO.new {
                            this.orderId = order.id
                            this.userId = order.userId
                            this.amount = invoice.amount
                            this.currency = invoice.currency
                            this.paymentMethod = PaymentMethod.BANK_TRANSFER
                            this.transactionId = "b2b:${invoice.id.value}"
                            this.provider = PaymentProvider.MANUAL
                            this.providerStatus = "b2b-invoice"
                            this.status = PaymentStatus.COMPLETED
                        }
                    if (order.paymentStatus == PaymentStatus.COMPLETED) {
                        if (order.status in listOf(OrderStatus.PENDING, OrderStatus.CONFIRMED)) {
                            order.status = OrderStatus.PAID
                        }
                    } else {
                        // Same completion effects as card/offline payments: loyalty,
                        // payout ledger, digital delivery and reservation finalization.
                        OrderCompletion.completeTx(order, payment)
                    }
                }
            }
            actorUserId?.let {
                AdminAudit.publish(
                    it,
                    "B2B_INVOICE_PAID",
                    "B2B_INVOICE",
                    invoice.id.value,
                    "Reference ${providerReference.orEmpty()}",
                )
            }
            invoice.toResponse()
        }

    suspend fun runDunning(
        now: LocalDateTime = currentUtc(),
        actorUserId: String? = null,
    ): B2bDunningResponse =
        query {
            val due =
                B2bInvoiceDAO.find {
                    (B2bInvoiceTable.status inList listOf(B2bInvoiceStatus.PENDING, B2bInvoiceStatus.OVERDUE)) and
                        (B2bInvoiceTable.dueDate lessEq now) and
                        ((B2bInvoiceTable.nextReminderAt.isNull()) or (B2bInvoiceTable.nextReminderAt lessEq now))
                }.toList()
            due.forEach { invoice ->
                invoice.status = B2bInvoiceStatus.OVERDUE
                invoice.reminderCount += 1
                invoice.lastReminderAt = now
                invoice.nextReminderAt = now.plusDays(7)
                UserDAO.findById(invoice.buyerUserId.value)?.email?.takeIf { it.isNotBlank() }?.let { email ->
                    OutboxPublisher.enqueueTx(
                        OutboxAggregate.B2B_INVOICE,
                        invoice.id.value,
                        SendEmailEvent(
                            to = email,
                            subject = "Invoice ${invoice.invoiceNumber} is overdue",
                            body =
                                "Invoice ${invoice.invoiceNumber} for ${invoice.amount.toPlainString()} " +
                                    "${invoice.currency} was due on ${invoice.dueDate}. Please arrange payment.",
                        ),
                    )
                }
            }
            actorUserId?.let {
                AdminAudit.publish(it, "B2B_DUNNING_RUN", "B2B_INVOICE", null, "Reminded ${due.size}")
            }
            B2bDunningResponse(due.size)
        }

    private fun B2bInvoiceDAO.toResponse() =
        B2bInvoiceResponse(
            id = id.value,
            purchaseOrderId = purchaseOrderId.value,
            orderId = orderId?.value,
            invoiceNumber = invoiceNumber,
            buyerUserId = buyerUserId.value,
            sellerId = sellerId.value,
            status = status,
            currency = currency,
            amount = Money.str(amount),
            dueDate = dueDate.toUtcString(),
            paidAt = paidAt?.toUtcString(),
            providerReference = providerReference,
            reminderCount = reminderCount,
            lastReminderAt = lastReminderAt?.toUtcString(),
            nextReminderAt = nextReminderAt?.toUtcString(),
        )
}
