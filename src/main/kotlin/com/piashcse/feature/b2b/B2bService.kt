package com.piashcse.feature.b2b

import com.piashcse.constants.B2bAccountStatus
import com.piashcse.constants.B2bInvoiceStatus
import com.piashcse.constants.PurchaseOrderStatus
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.feature.loyalty.LoyaltyService
import com.piashcse.service.DigitalDeliveryService
import com.piashcse.utils.common.PaginatedResponse
import java.time.LocalDateTime

/** Facade over B2B accounts, quotes, purchase orders and invoices. */
class B2bService(
    private val loyaltyService: LoyaltyService,
    private val digitalDelivery: DigitalDeliveryService,
) {
    private val accounts = B2bAccountService()
    private val invoices = B2bInvoiceService(loyaltyService, digitalDelivery)
    private val quotes = B2bQuoteService()
    private val purchaseOrders = B2bPurchaseOrderService()

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

    suspend fun createQuote(
        buyerUserId: String,
        request: QuoteRequest,
    ): QuoteResponse = quotes.createQuote(buyerUserId, request)

    suspend fun myQuotes(
        buyerUserId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<QuoteResponse> = quotes.myQuotes(buyerUserId, limit, offset)

    suspend fun quote(
        quoteId: String,
        userId: String,
    ): QuoteResponse = quotes.quote(quoteId, userId)

    suspend fun sellerQuotes(
        sellerUserId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<QuoteResponse> = quotes.sellerQuotes(sellerUserId, limit, offset)

    suspend fun respond(
        sellerUserId: String,
        quoteId: String,
        request: QuoteRespondRequest,
    ): QuoteResponse = quotes.respond(sellerUserId, quoteId, request)

    suspend fun accept(
        buyerUserId: String,
        quoteId: String,
    ): PurchaseOrderResponse = quotes.accept(buyerUserId, quoteId)

    suspend fun myPurchaseOrders(
        buyerUserId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<PurchaseOrderResponse> = purchaseOrders.myPurchaseOrders(buyerUserId, limit, offset)

    suspend fun purchaseOrder(
        id: String,
        userId: String,
    ): PurchaseOrderResponse = purchaseOrders.purchaseOrder(id, userId)

    suspend fun sellerPurchaseOrders(
        sellerUserId: String,
        status: PurchaseOrderStatus?,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<PurchaseOrderResponse> = purchaseOrders.sellerPurchaseOrders(sellerUserId, status, limit, offset)

    suspend fun fulfill(
        sellerUserId: String,
        poId: String,
    ): PurchaseOrderResponse = purchaseOrders.fulfill(sellerUserId, poId)

    suspend fun cancelPurchaseOrder(
        sellerUserId: String,
        poId: String,
    ): PurchaseOrderResponse = purchaseOrders.cancelPurchaseOrder(sellerUserId, poId)

    suspend fun adminPurchaseOrders(
        status: PurchaseOrderStatus?,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<PurchaseOrderResponse> = purchaseOrders.adminPurchaseOrders(status, limit, offset)
}
