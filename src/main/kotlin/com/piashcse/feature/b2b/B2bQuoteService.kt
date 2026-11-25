package com.piashcse.feature.b2b

import com.piashcse.constants.AppConstants
import com.piashcse.constants.B2bInvoiceStatus
import com.piashcse.constants.B2bQuoteStatus
import com.piashcse.constants.ProductStatus
import com.piashcse.constants.PurchaseOrderStatus
import com.piashcse.database.entities.B2bInvoiceDAO
import com.piashcse.database.entities.B2bInvoiceTable
import com.piashcse.database.entities.B2bPurchaseOrderDAO
import com.piashcse.database.entities.B2bPurchaseOrderTable
import com.piashcse.database.entities.B2bQuoteDAO
import com.piashcse.database.entities.B2bQuoteItemDAO
import com.piashcse.database.entities.B2bQuoteItemTable
import com.piashcse.database.entities.B2bQuoteTable
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.ProductTable
import com.piashcse.database.entities.SellerDAO
import com.piashcse.database.entities.SellerTable
import com.piashcse.database.entities.ShopTable
import com.piashcse.database.entities.UserTable
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.service.AppMetrics
import com.piashcse.utils.common.DateTimes
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
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal

/** B2B quotes: request, respond, accept. */
class B2bQuoteService {
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
}
