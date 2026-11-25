package com.piashcse.feature.b2b

import com.piashcse.constants.B2bInvoiceStatus
import com.piashcse.constants.B2bQuoteStatus
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentMethod
import com.piashcse.constants.PaymentStatus
import com.piashcse.constants.ProductStatus
import com.piashcse.constants.PurchaseOrderStatus
import com.piashcse.database.entities.B2bInvoiceDAO
import com.piashcse.database.entities.B2bPurchaseOrderDAO
import com.piashcse.database.entities.B2bPurchaseOrderTable
import com.piashcse.database.entities.B2bQuoteDAO
import com.piashcse.database.entities.B2bQuoteItemDAO
import com.piashcse.database.entities.B2bQuoteItemTable
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderItemDAO
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.ProductTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.UserTable
import com.piashcse.database.entities.addSales
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.database.entities.decrementStock
import com.piashcse.feature.seller.SellerLedger
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.requireSellerByUserId
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.toPaginatedResponse
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal
import java.util.UUID

/** B2B purchase orders: fulfillment, cancellation and admin views. */
class B2bPurchaseOrderService {
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
}
