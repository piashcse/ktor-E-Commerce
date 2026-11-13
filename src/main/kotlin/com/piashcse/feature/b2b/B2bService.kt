package com.piashcse.feature.b2b

import com.piashcse.constants.B2bAccountStatus
import com.piashcse.constants.B2bInvoiceStatus
import com.piashcse.constants.B2bQuoteStatus
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentMethod
import com.piashcse.constants.PaymentProvider
import com.piashcse.constants.PaymentStatus
import com.piashcse.constants.PurchaseOrderStatus
import com.piashcse.database.entities.B2bAccountDAO
import com.piashcse.database.entities.B2bAccountTable
import com.piashcse.database.entities.B2bInvoiceDAO
import com.piashcse.database.entities.B2bInvoiceTable
import com.piashcse.database.entities.B2bPurchaseOrderDAO
import com.piashcse.database.entities.B2bPurchaseOrderTable
import com.piashcse.database.entities.B2bQuoteDAO
import com.piashcse.database.entities.B2bQuoteItemDAO
import com.piashcse.database.entities.B2bQuoteItemTable
import com.piashcse.database.entities.B2bQuoteTable
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderItemDAO
import com.piashcse.database.entities.PaymentDAO
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.ProductTable
import com.piashcse.database.entities.SellerDAO
import com.piashcse.database.entities.SellerTable
import com.piashcse.database.entities.ShopTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.addSales
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.database.entities.decrementStock
import com.piashcse.event.AdminAudit
import com.piashcse.event.OutboxPublisher
import com.piashcse.event.PaymentCompletedEvent
import com.piashcse.event.SendEmailEvent
import com.piashcse.feature.subscription.DigitalDeliveryService
import com.piashcse.service.AppMetrics
import com.piashcse.utils.common.DateTimes
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.Money
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.requireSellerByUserId
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.toPaginatedResponse
import com.piashcse.utils.validator.FieldValidationException
import com.piashcse.utils.validator.FieldViolation
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class B2bAccountRequest(
    val companyName: String,
    val taxId: String? = null,
    val contactName: String? = null,
    val phone: String? = null,
) {
    init {
        if (companyName.isBlank()) {
            throw FieldValidationException(listOf(FieldViolation("companyName", "Company name is required")))
        }
    }
}

@Serializable
data class B2bAccountUpdateRequest(
    val status: B2bAccountStatus? = null,
    @Contextual val creditLimit: BigDecimal? = null,
    val paymentTermsDays: Int? = null,
)

@Serializable
data class B2bAccountResponse(
    val id: String,
    val userId: String,
    val companyName: String,
    val taxId: String?,
    val contactName: String?,
    val phone: String?,
    val creditLimit: String,
    val paymentTermsDays: Int,
    val status: B2bAccountStatus,
    val approvedAt: String?,
    val createdAt: String?,
)

@Serializable
data class QuoteItemRequest(
    val productId: String,
    val quantity: Int,
) {
    init {
        if (quantity <= 0) throw FieldValidationException(listOf(FieldViolation("quantity", "Quantity must be positive")))
    }
}

@Serializable
data class QuoteRequest(
    val items: List<QuoteItemRequest>,
    val notes: String? = null,
    val validDays: Int = 14,
) {
    init {
        if (items.isEmpty()) throw FieldValidationException(listOf(FieldViolation("items", "At least one item is required")))
        if (validDays <= 0) throw FieldValidationException(listOf(FieldViolation("validDays", "validDays must be positive")))
    }
}

@Serializable
data class QuotePricedItem(
    val productId: String,
    val quantity: Int,
    @Contextual val unitPrice: BigDecimal,
) {
    init {
        if (quantity <= 0) throw FieldValidationException(listOf(FieldViolation("quantity", "Quantity must be positive")))
        if (unitPrice <= BigDecimal.ZERO) {
            throw FieldValidationException(listOf(FieldViolation("unitPrice", "Unit price must be positive")))
        }
    }
}

@Serializable
data class QuoteRespondRequest(
    val status: B2bQuoteStatus,
    val items: List<QuotePricedItem>? = null,
    val notes: String? = null,
    val validUntil: String? = null,
)

@Serializable
data class QuoteItemResponse(
    val productId: String,
    val productName: String?,
    val quantity: Int,
    val unitPrice: String,
    val total: String,
)

@Serializable
data class QuoteResponse(
    val id: String,
    val buyerUserId: String,
    val sellerId: String,
    val shopId: String?,
    val status: B2bQuoteStatus,
    val currency: String,
    val subtotal: String,
    val taxAmount: String,
    val total: String,
    val notes: String?,
    val validUntil: String?,
    val createdAt: String?,
    val items: List<QuoteItemResponse>,
)

@Serializable
data class PurchaseOrderResponse(
    val id: String,
    val quoteId: String,
    val orderId: String?,
    val buyerUserId: String,
    val sellerId: String,
    val shopId: String?,
    val poNumber: String,
    val status: PurchaseOrderStatus,
    val currency: String,
    val total: String,
    val netTermsDays: Int,
    val dueDate: String?,
    val fulfilledAt: String?,
    val canceledAt: String?,
    val createdAt: String?,
    val invoiceId: String? = null,
)

@Serializable
data class B2bInvoiceResponse(
    val id: String,
    val purchaseOrderId: String,
    val orderId: String?,
    val invoiceNumber: String,
    val buyerUserId: String,
    val sellerId: String,
    val status: B2bInvoiceStatus,
    val currency: String,
    val amount: String,
    val dueDate: String,
    val paidAt: String?,
    val providerReference: String?,
    val reminderCount: Int,
    val lastReminderAt: String?,
    val nextReminderAt: String?,
)

@Serializable
data class B2bDunningResponse(val reminded: Int)

class B2bService {
    suspend fun account(userId: String): B2bAccountResponse =
        query {
            val account =
                B2bAccountDAO.find { B2bAccountTable.userId eq userId.entityID(com.piashcse.database.entities.UserTable) }
                    .firstOrNull() ?: throw ValidationException("B2B account not found")
            account.toAccountResponse()
        }

    suspend fun apply(
        userId: String,
        request: B2bAccountRequest,
    ): B2bAccountResponse =
        query {
            val existing =
                B2bAccountDAO.find { B2bAccountTable.userId eq userId.entityID(com.piashcse.database.entities.UserTable) }
                    .firstOrNull()
            val account =
                existing?.apply {
                    companyName = request.companyName
                    taxId = request.taxId
                    contactName = request.contactName
                    phone = request.phone
                } ?: B2bAccountDAO.new {
                    this.userId = userId.entityID(com.piashcse.database.entities.UserTable)
                    companyName = request.companyName
                    taxId = request.taxId
                    contactName = request.contactName
                    phone = request.phone
                    creditLimit = BigDecimal.ZERO
                    paymentTermsDays = 30
                    status = B2bAccountStatus.PENDING
                }
            account.toAccountResponse()
        }

    suspend fun adminAccounts(
        status: B2bAccountStatus?,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<B2bAccountResponse> =
        query {
            B2bAccountTable.selectAll()
                .also { query ->
                    status?.let { query.andWhere { B2bAccountTable.status eq it } }
                    query.orderBy(B2bAccountTable.createdAt to SortOrder.DESC)
                }.toPaginatedResponse(limit, offset) { B2bAccountDAO.wrapRow(it).toAccountResponse() }
        }

    suspend fun updateAccount(
        userId: String,
        actorUserId: String,
        request: B2bAccountUpdateRequest,
    ): B2bAccountResponse =
        query {
            val account =
                B2bAccountDAO.find { B2bAccountTable.userId eq userId.entityID(com.piashcse.database.entities.UserTable) }
                    .firstOrNull() ?: throw ValidationException("B2B account not found")
            request.creditLimit?.let {
                if (it < BigDecimal.ZERO) throw ValidationException("Credit limit cannot be negative")
                account.creditLimit = it
            }
            request.paymentTermsDays?.let {
                if (it <= 0) throw ValidationException("Payment terms must be positive")
                account.paymentTermsDays = it
            }
            request.status?.let {
                account.status = it
                if (it == B2bAccountStatus.APPROVED) {
                    account.approvedBy = actorUserId.entityID(com.piashcse.database.entities.UserTable)
                    account.approvedAt = currentUtc()
                }
            }
            AdminAudit.publish(
                actorUserId,
                "B2B_ACCOUNT_UPDATED",
                "B2B_ACCOUNT",
                account.id.value,
                "Status ${account.status}, credit ${account.creditLimit.toPlainString()}, terms ${account.paymentTermsDays}",
            )
            account.toAccountResponse()
        }

    suspend fun createQuote(
        buyerUserId: String,
        request: QuoteRequest,
    ): QuoteResponse =
        query {
            requireApprovedAccount(buyerUserId)
            val products =
                ProductDAO.find {
                    ProductTable.id inList request.items.map { it.productId.entityID(ProductTable) }
                }.associateBy { it.id.value }
            if (products.size != request.items.map { it.productId }.distinct().size) {
                throw ValidationException("One or more products were not found")
            }
            val sellerIds =
                products.values.mapNotNull { it.userId.value }.toSet()
            if (sellerIds.size != 1) throw ValidationException("All quote items must belong to one seller")
            val seller =
                SellerDAO.find { SellerTable.userId eq products.values.first().userId }.firstOrNull()
                    ?: throw ValidationException("Seller not found")
            val shopId = products.values.first().shopId?.value
            if (products.values.any { it.shopId?.value != shopId }) {
                throw ValidationException("All quote items must belong to one shop")
            }
            val quote =
                B2bQuoteDAO.new {
                    this.buyerUserId = buyerUserId.entityID(com.piashcse.database.entities.UserTable)
                    this.sellerId = seller.id
                    this.shopId = shopId?.entityID(ShopTable)
                    this.status = B2bQuoteStatus.REQUESTED
                    this.currency = "USD"
                    this.notes = request.notes
                    this.validUntil = currentUtc().plusDays(request.validDays.toLong())
                }
            var subtotal = BigDecimal.ZERO
            request.items.forEach { item ->
                val product = products.getValue(item.productId)
                val unitPrice = product.discountPrice ?: product.price
                val total = Money.scale2(unitPrice.multiply(BigDecimal(item.quantity)))
                B2bQuoteItemDAO.new {
                    this.quoteId = quote.id
                    this.productId = product.id
                    this.quantity = item.quantity
                    this.unitPrice = unitPrice
                    this.total = total
                }
                subtotal = subtotal.add(total)
            }
            quote.subtotal = Money.scale2(subtotal)
            quote.total = Money.scale2(subtotal)
            AppMetrics.b2bQuotesRequested.increment()
            quote.toResponse()
        }

    suspend fun myQuotes(
        buyerUserId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<QuoteResponse> =
        query {
            B2bQuoteTable.selectAll()
                .where { B2bQuoteTable.buyerUserId eq buyerUserId.entityID(com.piashcse.database.entities.UserTable) }
                .orderBy(B2bQuoteTable.createdAt to SortOrder.DESC)
                .toPaginatedResponse(limit, offset) { B2bQuoteDAO.wrapRow(it).toResponse() }
        }

    suspend fun quote(
        quoteId: String,
        userId: String,
    ): QuoteResponse =
        query {
            val quote = B2bQuoteDAO.findById(quoteId) ?: quoteId.throwNotFound("Quote")
            requireQuoteAccess(quote, userId)
            quote.toResponse()
        }

    suspend fun sellerQuotes(
        sellerUserId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<QuoteResponse> =
        query {
            val seller = requireSellerByUserId(sellerUserId)
            B2bQuoteTable.selectAll()
                .where { B2bQuoteTable.sellerId eq seller.id }
                .orderBy(B2bQuoteTable.createdAt to SortOrder.DESC)
                .toPaginatedResponse(limit, offset) { B2bQuoteDAO.wrapRow(it).toResponse() }
        }

    suspend fun respond(
        sellerUserId: String,
        quoteId: String,
        request: QuoteRespondRequest,
    ): QuoteResponse =
        query {
            val seller = requireSellerByUserId(sellerUserId)
            val quote = B2bQuoteDAO.findById(quoteId) ?: quoteId.throwNotFound("Quote")
            if (quote.sellerId.value != seller.id.value) throw ForbiddenException("Quote belongs to another seller")
            if (quote.status !in listOf(B2bQuoteStatus.REQUESTED, B2bQuoteStatus.SENT)) {
                throw ValidationException("Quote cannot be updated from ${quote.status}")
            }
            if (request.status !in listOf(B2bQuoteStatus.SENT, B2bQuoteStatus.REJECTED)) {
                throw ValidationException("Sellers can only mark a quote SENT or REJECTED")
            }
            request.items?.let { items ->
                if (items.isEmpty()) throw ValidationException("At least one item is required")
                items.forEach { item ->
                    val row =
                        B2bQuoteItemDAO.find {
                            (B2bQuoteItemTable.quoteId eq quote.id) and
                                (B2bQuoteItemTable.productId eq item.productId.entityID(ProductTable))
                        }.firstOrNull() ?: throw ValidationException("Quote item not found: ${item.productId}")
                    if (item.unitPrice <= BigDecimal.ZERO) throw ValidationException("Unit price must be positive")
                    row.quantity = item.quantity
                    row.unitPrice = item.unitPrice
                    row.total = Money.scale2(item.unitPrice.multiply(BigDecimal(item.quantity)))
                }
                // Re-price every stored line, not just the lines present in this
                // request. Omitted lines must not disappear from the total while
                // fulfill() still ships them.
                val storedTotal =
                    B2bQuoteItemDAO.find { B2bQuoteItemTable.quoteId eq quote.id }
                        .fold(BigDecimal.ZERO) { acc, row -> acc.add(row.total) }
                quote.subtotal = Money.scale2(storedTotal)
                quote.total = Money.scale2(storedTotal)
            }
            request.notes?.let { quote.notes = it }
            request.validUntil?.let { quote.validUntil = DateTimes.parseFlexible(it, "validUntil") }
            quote.status = request.status
            quote.respondedAt = currentUtc()
            quote.toResponse()
        }

    suspend fun accept(
        buyerUserId: String,
        quoteId: String,
    ): PurchaseOrderResponse =
        query {
            val account = requireApprovedAccount(buyerUserId, forUpdate = true)
            val quote = B2bQuoteDAO.findById(quoteId) ?: quoteId.throwNotFound("Quote")
            if (quote.buyerUserId.value != buyerUserId) throw ForbiddenException("Quote belongs to another buyer")
            if (quote.status != B2bQuoteStatus.SENT) throw ValidationException("Only SENT quotes can be accepted")
            if (quote.validUntil?.isBefore(currentUtc()) == true) {
                quote.status = B2bQuoteStatus.EXPIRED
                throw ValidationException("Quote has expired")
            }
            val openTotal =
                B2bPurchaseOrderDAO.find {
                    (B2bPurchaseOrderTable.buyerUserId eq buyerUserId.entityID(com.piashcse.database.entities.UserTable)) and
                        (B2bPurchaseOrderTable.status eq PurchaseOrderStatus.OPEN)
                }.fold(BigDecimal.ZERO) { acc, po -> acc.add(po.total) }
            // Fulfilled POs become unpaid invoices; those still consume credit.
            val unpaidInvoices =
                B2bInvoiceDAO.find {
                    (B2bInvoiceTable.buyerUserId eq buyerUserId.entityID(com.piashcse.database.entities.UserTable)) and
                        (B2bInvoiceTable.status inList listOf(B2bInvoiceStatus.PENDING, B2bInvoiceStatus.OVERDUE))
                }.fold(BigDecimal.ZERO) { acc, invoice -> acc.add(invoice.amount) }
            val exposure = openTotal.add(unpaidInvoices)
            if (account.creditLimit <= BigDecimal.ZERO || exposure.add(quote.total) > account.creditLimit) {
                throw ValidationException(
                    "Credit limit exceeded: available ${account.creditLimit.subtract(exposure).max(BigDecimal.ZERO)}",
                )
            }
            val dueDate = currentUtc().plusDays(account.paymentTermsDays.toLong())
            val po =
                B2bPurchaseOrderDAO.new {
                    this.quoteId = quote.id
                    this.buyerUserId = quote.buyerUserId
                    this.sellerId = quote.sellerId
                    this.shopId = quote.shopId
                    this.poNumber = generatePoNumber()
                    this.status = PurchaseOrderStatus.OPEN
                    this.currency = quote.currency
                    this.total = quote.total
                    this.netTermsDays = account.paymentTermsDays
                    this.dueDate = dueDate
                }
            quote.status = B2bQuoteStatus.ACCEPTED
            po.toResponse()
        }

    suspend fun myPurchaseOrders(
        buyerUserId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<PurchaseOrderResponse> =
        query {
            B2bPurchaseOrderTable.selectAll()
                .where { B2bPurchaseOrderTable.buyerUserId eq buyerUserId.entityID(com.piashcse.database.entities.UserTable) }
                .orderBy(B2bPurchaseOrderTable.createdAt to SortOrder.DESC)
                .toPaginatedResponse(limit, offset) { B2bPurchaseOrderDAO.wrapRow(it).toResponse() }
        }

    suspend fun purchaseOrder(
        id: String,
        userId: String,
    ): PurchaseOrderResponse =
        query {
            val po = B2bPurchaseOrderDAO.findById(id) ?: id.throwNotFound("Purchase order")
            if (po.buyerUserId.value != userId) {
                val seller = runCatching { requireSellerByUserId(userId) }.getOrNull()
                if (seller == null || po.sellerId.value != seller.id.value) {
                    throw ForbiddenException("Purchase order belongs to another account")
                }
            }
            po.toResponse()
        }

    suspend fun sellerPurchaseOrders(
        sellerUserId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<PurchaseOrderResponse> =
        query {
            val seller = requireSellerByUserId(sellerUserId)
            B2bPurchaseOrderTable.selectAll()
                .where { B2bPurchaseOrderTable.sellerId eq seller.id }
                .orderBy(B2bPurchaseOrderTable.createdAt to SortOrder.DESC)
                .toPaginatedResponse(limit, offset) { B2bPurchaseOrderDAO.wrapRow(it).toResponse() }
        }

    suspend fun fulfill(
        sellerUserId: String,
        poId: String,
    ): PurchaseOrderResponse =
        query {
            val seller = requireSellerByUserId(sellerUserId)
            val po =
                B2bPurchaseOrderDAO.find { B2bPurchaseOrderTable.id eq poId.entityID(B2bPurchaseOrderTable) }
                    .forUpdate()
                    .firstOrNull() ?: poId.throwNotFound("Purchase order")
            if (po.sellerId.value != seller.id.value) throw ForbiddenException("Purchase order belongs to another seller")
            if (po.status != PurchaseOrderStatus.OPEN) throw ValidationException("Purchase order is not open")
            val shopId = po.shopId ?: throw ValidationException("Purchase order has no shop")
            val quoteItems = B2bQuoteItemDAO.find { B2bQuoteItemTable.quoteId eq po.quoteId }.toList()
            if (quoteItems.isEmpty()) throw ValidationException("Purchase order has no items")
            val products =
                ProductDAO.find {
                    ProductTable.id inList quoteItems.map { it.productId }
                }.associateBy { it.id.value }
            val buyerEmail = UserDAO.findById(po.buyerUserId.value)?.email.orEmpty()
            val order =
                OrderDAO.new {
                    this.userId = po.buyerUserId
                    this.shopId = shopId
                    this.orderNumber =
                        "B2B-${currentUtc().toLocalDate().toString().replace("-", "")}-${UUID.randomUUID().toString().take(6).uppercase()}"
                    status = OrderStatus.CONFIRMED
                    paymentStatus = PaymentStatus.PENDING
                    subTotal = po.total
                    shippingCost = BigDecimal.ZERO.setScale(2)
                    taxAmount = BigDecimal.ZERO.setScale(2)
                    total = po.total
                    currency = po.currency
                    fxRate = BigDecimal.ONE
                    paymentMethod = PaymentMethod.BANK_TRANSFER
                    shippingMethod = "B2B"
                    contactEmail = buyerEmail
                    notes = "B2B PO ${po.poNumber}; net ${po.netTermsDays} days"
                }
            // Flush before linking the PO/invoice so FK checks see the order row.
            order.flush()
            quoteItems.forEach { item ->
                val product = products[item.productId.value] ?: throw ValidationException("Product not found")
                product.decrementStock(item.quantity)
                product.addSales(item.quantity)
                OrderItemDAO.new {
                    this.orderId = order.id
                    this.productId = product.id
                    this.shopId = shopId
                    quantity = item.quantity
                    price = item.unitPrice
                    total = item.total
                    productName = product.name
                    sku = product.sku
                }
            }
            po.orderId = order.id
            po.status = PurchaseOrderStatus.FULFILLED
            po.fulfilledAt = currentUtc()
            B2bInvoiceDAO.new {
                this.purchaseOrderId = po.id
                this.orderId = order.id
                this.buyerUserId = po.buyerUserId
                this.sellerId = po.sellerId
                this.invoiceNumber = generateInvoiceNumber()
                this.status = B2bInvoiceStatus.PENDING
                this.currency = po.currency
                this.amount = po.total
                this.dueDate = po.dueDate ?: currentUtc().plusDays(po.netTermsDays.toLong())
            }
            po.toResponse()
        }

    suspend fun cancelPurchaseOrder(
        sellerUserId: String,
        poId: String,
    ): PurchaseOrderResponse =
        query {
            val seller = requireSellerByUserId(sellerUserId)
            val po =
                B2bPurchaseOrderDAO.find { B2bPurchaseOrderTable.id eq poId.entityID(B2bPurchaseOrderTable) }
                    .forUpdate()
                    .firstOrNull() ?: poId.throwNotFound("Purchase order")
            if (po.sellerId.value != seller.id.value) throw ForbiddenException("Purchase order belongs to another seller")
            if (po.status != PurchaseOrderStatus.OPEN) {
                throw ValidationException("Only OPEN purchase orders can be canceled")
            }
            po.status = PurchaseOrderStatus.CANCELED
            po.canceledAt = currentUtc()
            po.toResponse()
        }

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
                .where { B2bInvoiceTable.buyerUserId eq buyerUserId.entityID(com.piashcse.database.entities.UserTable) }
                .orderBy(B2bInvoiceTable.dueDate to SortOrder.ASC)
                .toPaginatedResponse(limit, offset) { B2bInvoiceDAO.wrapRow(it).toResponse() }
        }

    suspend fun sellerInvoices(
        sellerUserId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<B2bInvoiceResponse> =
        query {
            val seller = requireSellerByUserId(sellerUserId)
            B2bInvoiceTable.selectAll()
                .where { B2bInvoiceTable.sellerId eq seller.id }
                .orderBy(B2bInvoiceTable.dueDate to SortOrder.ASC)
                .toPaginatedResponse(limit, offset) { B2bInvoiceDAO.wrapRow(it).toResponse() }
        }

    suspend fun adminInvoices(
        limit: Int,
        offset: Int,
    ): PaginatedResponse<B2bInvoiceResponse> =
        query {
            B2bInvoiceTable.selectAll()
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
                    order.paymentStatus = PaymentStatus.COMPLETED
                    if (order.status in listOf(OrderStatus.PENDING, OrderStatus.CONFIRMED)) {
                        order.status = OrderStatus.PAID
                    }
                    DigitalDeliveryService().grantForOrderTx(order)
                    OutboxPublisher.enqueueTx(
                        "PAYMENT",
                        payment.id.value,
                        PaymentCompletedEvent(
                            paymentId = payment.id.value,
                            orderId = order.id.value,
                            userId = order.userId.value,
                            email = UserDAO.findById(order.userId.value)?.email.orEmpty(),
                            amount = payment.amount,
                        ),
                    )
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
                        "B2B_INVOICE",
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

    suspend fun adminPurchaseOrders(
        limit: Int,
        offset: Int,
    ): PaginatedResponse<PurchaseOrderResponse> =
        query {
            B2bPurchaseOrderTable.selectAll()
                .orderBy(B2bPurchaseOrderTable.createdAt to SortOrder.DESC)
                .toPaginatedResponse(limit, offset) { B2bPurchaseOrderDAO.wrapRow(it).toResponse() }
        }

    private fun requireApprovedAccount(
        userId: String,
        forUpdate: Boolean = false,
    ): B2bAccountDAO {
        val query =
            B2bAccountDAO.find { B2bAccountTable.userId eq userId.entityID(com.piashcse.database.entities.UserTable) }
        val account =
            (if (forUpdate) query.forUpdate() else query).firstOrNull()
                ?: throw ValidationException("Apply for a B2B account first")
        if (account.status != B2bAccountStatus.APPROVED) {
            throw ValidationException("B2B account is not approved")
        }
        return account
    }

    private fun requireQuoteAccess(
        quote: B2bQuoteDAO,
        userId: String,
    ) {
        if (quote.buyerUserId.value == userId) return
        val seller = runCatching { requireSellerByUserId(userId) }.getOrNull()
        if (seller != null && quote.sellerId.value == seller.id.value) return
        throw ForbiddenException("Quote belongs to another account")
    }

    private fun generatePoNumber(): String =
        "PO-${currentUtc().toLocalDate().toString().replace("-", "")}-${UUID.randomUUID().toString().take(6).uppercase()}"

    private fun generateInvoiceNumber(): String =
        "INV-${currentUtc().toLocalDate().toString().replace("-", "")}-${UUID.randomUUID().toString().take(6).uppercase()}"

    private fun B2bAccountDAO.toAccountResponse() =
        B2bAccountResponse(
            id = id.value,
            userId = userId.value,
            companyName = companyName,
            taxId = taxId,
            contactName = contactName,
            phone = phone,
            creditLimit = Money.str(creditLimit),
            paymentTermsDays = paymentTermsDays,
            status = status,
            approvedAt = approvedAt?.toUtcString(),
            createdAt = createdAt.toUtcString(),
        )

    private fun B2bQuoteDAO.toResponse() =
        QuoteResponse(
            id = id.value,
            buyerUserId = buyerUserId.value,
            sellerId = sellerId.value,
            shopId = shopId?.value,
            status = status,
            currency = currency,
            subtotal = Money.str(subtotal),
            taxAmount = Money.str(taxAmount),
            total = Money.str(total),
            notes = notes,
            validUntil = validUntil?.toUtcString(),
            createdAt = createdAt.toUtcString(),
            items =
                B2bQuoteItemDAO.find { B2bQuoteItemTable.quoteId eq id }.map { item ->
                    QuoteItemResponse(
                        productId = item.productId.value,
                        productName = ProductDAO.findById(item.productId.value)?.name,
                        quantity = item.quantity,
                        unitPrice = Money.str(item.unitPrice),
                        total = Money.str(item.total),
                    )
                },
        )

    private fun B2bPurchaseOrderDAO.toResponse() =
        PurchaseOrderResponse(
            id = id.value,
            quoteId = quoteId.value,
            orderId = orderId?.value,
            buyerUserId = buyerUserId.value,
            sellerId = sellerId.value,
            shopId = shopId?.value,
            poNumber = poNumber,
            status = status,
            currency = currency,
            total = Money.str(total),
            netTermsDays = netTermsDays,
            dueDate = dueDate?.toUtcString(),
            fulfilledAt = fulfilledAt?.toUtcString(),
            canceledAt = canceledAt?.toUtcString(),
            createdAt = createdAt.toUtcString(),
            invoiceId =
                B2bInvoiceDAO.find { B2bInvoiceTable.purchaseOrderId eq id }
                    .firstOrNull()
                    ?.id
                    ?.value,
        )

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
