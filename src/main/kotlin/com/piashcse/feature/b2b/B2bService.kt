package com.piashcse.feature.b2b

import com.piashcse.constants.AppConstants
import com.piashcse.constants.B2bAccountStatus
import com.piashcse.constants.B2bInvoiceStatus
import com.piashcse.constants.B2bQuoteStatus
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentMethod
import com.piashcse.constants.PaymentStatus
import com.piashcse.constants.ProductStatus
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
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.ProductTable
import com.piashcse.database.entities.SellerDAO
import com.piashcse.database.entities.SellerTable
import com.piashcse.database.entities.ShopTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.UserTable
import com.piashcse.database.entities.addSales
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.database.entities.decrementStock
import com.piashcse.feature.seller.SellerLedger
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
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

class B2bService {
    private val accounts = B2bAccountService()
    private val invoices = B2bInvoiceService()

    suspend fun account(userId: String): B2bAccountResponse = accounts.account(userId)

    suspend fun apply(
        userId: String,
        request: B2bAccountRequest,
    ): B2bAccountResponse = accounts.apply(userId, request)

    suspend fun adminAccounts(
        status: B2bAccountStatus?,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<B2bAccountResponse> = accounts.adminAccounts(status, limit, offset)

    suspend fun updateAccount(
        userId: String,
        actorUserId: String,
        request: B2bAccountUpdateRequest,
    ): B2bAccountResponse = accounts.updateAccount(userId, actorUserId, request)

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
            if (products.values.any {
                    it.deletedAt != null || it.status != ProductStatus.ACTIVE
                }
            ) {
                throw ValidationException("One or more products are not active")
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
                    this.buyerUserId = buyerUserId.entityID(UserTable)
                    this.sellerId = seller.id
                    this.shopId = shopId?.entityID(ShopTable)
                    this.status = B2bQuoteStatus.REQUESTED
                    this.currency = AppConstants.DEFAULT_CURRENCY
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
                .where { B2bQuoteTable.buyerUserId eq buyerUserId.entityID(UserTable) }
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
            val quote =
                B2bQuoteDAO.find { B2bQuoteTable.id eq quoteId.entityID(B2bQuoteTable) }
                    .forUpdate()
                    .firstOrNull()
                    ?: quoteId.throwNotFound("Quote")
            if (quote.buyerUserId.value != buyerUserId) throw ForbiddenException("Quote belongs to another buyer")
            if (quote.status == B2bQuoteStatus.ACCEPTED) {
                B2bPurchaseOrderDAO.find { B2bPurchaseOrderTable.quoteId eq quote.id }
                    .firstOrNull()
                    ?.let { return@query it.toResponse() }
            }
            if (quote.status != B2bQuoteStatus.SENT) throw ValidationException("Only SENT quotes can be accepted")
            if (quote.validUntil?.isBefore(currentUtc()) == true) {
                quote.status = B2bQuoteStatus.EXPIRED
                throw ValidationException("Quote has expired")
            }
            val openTotal =
                B2bPurchaseOrderDAO.find {
                    (B2bPurchaseOrderTable.buyerUserId eq buyerUserId.entityID(UserTable)) and
                        (B2bPurchaseOrderTable.status eq PurchaseOrderStatus.OPEN)
                }.fold(BigDecimal.ZERO) { acc, po -> acc.add(po.total) }
            // Fulfilled POs become unpaid invoices; those still consume credit.
            val unpaidInvoices =
                B2bInvoiceDAO.find {
                    (B2bInvoiceTable.buyerUserId eq buyerUserId.entityID(UserTable)) and
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
                .where { B2bPurchaseOrderTable.buyerUserId eq buyerUserId.entityID(UserTable) }
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
        status: PurchaseOrderStatus?,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<PurchaseOrderResponse> =
        query {
            val seller = requireSellerByUserId(sellerUserId)
            B2bPurchaseOrderTable.selectAll()
                .where { B2bPurchaseOrderTable.sellerId eq seller.id }
                .also { query -> status?.let { query.andWhere { B2bPurchaseOrderTable.status eq it } } }
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
                if (product.deletedAt != null || product.status != ProductStatus.ACTIVE) {
                    throw ValidationException("Product ${product.name} is not active")
                }
                if (!product.isDigital) product.decrementStock(item.quantity)
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
            SellerLedger.applyOrder(order)
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
            // Allow the buyer to accept the same quote again after a seller
            // cancellation instead of returning the dead PO forever.
            B2bQuoteDAO.findById(po.quoteId.value)?.let { quote ->
                if (quote.status == B2bQuoteStatus.ACCEPTED) quote.status = B2bQuoteStatus.SENT
            }
            po.toResponse()
        }

    suspend fun voidInvoice(
        invoiceId: String,
        actorUserId: String? = null,
    ): B2bInvoiceResponse = invoices.voidInvoice(invoiceId, actorUserId)

    suspend fun myInvoices(
        buyerUserId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<B2bInvoiceResponse> = invoices.myInvoices(buyerUserId, limit, offset)

    suspend fun sellerInvoices(
        sellerUserId: String,
        status: B2bInvoiceStatus?,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<B2bInvoiceResponse> = invoices.sellerInvoices(sellerUserId, status, limit, offset)

    suspend fun adminInvoices(
        status: B2bInvoiceStatus?,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<B2bInvoiceResponse> = invoices.adminInvoices(status, limit, offset)

    suspend fun markInvoicePaid(
        invoiceId: String,
        providerReference: String?,
        actorUserId: String? = null,
    ): B2bInvoiceResponse = invoices.markInvoicePaid(invoiceId, providerReference, actorUserId)

    suspend fun runDunning(
        now: LocalDateTime = currentUtc(),
        actorUserId: String? = null,
    ): B2bDunningResponse = invoices.runDunning(now, actorUserId)

    suspend fun adminPurchaseOrders(
        status: PurchaseOrderStatus?,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<PurchaseOrderResponse> =
        query {
            B2bPurchaseOrderTable.selectAll()
                .also { query -> status?.let { query.andWhere { B2bPurchaseOrderTable.status eq it } } }
                .orderBy(B2bPurchaseOrderTable.createdAt to SortOrder.DESC)
                .toPaginatedResponse(limit, offset) { B2bPurchaseOrderDAO.wrapRow(it).toResponse() }
        }

    private fun requireApprovedAccount(
        userId: String,
        forUpdate: Boolean = false,
    ): B2bAccountDAO {
        val query =
            B2bAccountDAO.find { B2bAccountTable.userId eq userId.entityID(UserTable) }
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
}
