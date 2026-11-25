package com.piashcse.feature.b2b

import com.piashcse.constants.B2bAccountStatus
import com.piashcse.database.entities.B2bAccountDAO
import com.piashcse.database.entities.B2bAccountTable
import com.piashcse.database.entities.B2bInvoiceDAO
import com.piashcse.database.entities.B2bInvoiceTable
import com.piashcse.database.entities.B2bPurchaseOrderDAO
import com.piashcse.database.entities.B2bQuoteDAO
import com.piashcse.database.entities.B2bQuoteItemDAO
import com.piashcse.database.entities.B2bQuoteItemTable
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.UserTable
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.Money
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.requireSellerByUserId
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.eq
import java.util.UUID

internal fun requireApprovedAccount(
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

internal fun requireQuoteAccess(
    quote: B2bQuoteDAO,
    userId: String,
) {
    if (quote.buyerUserId.value == userId) return
    val seller = runCatching { requireSellerByUserId(userId) }.getOrNull()
    if (seller != null && quote.sellerId.value == seller.id.value) return
    throw ForbiddenException("Quote belongs to another account")
}

internal fun generatePoNumber(): String =
    "PO-${currentUtc().toLocalDate().toString().replace("-", "")}-${UUID.randomUUID().toString().take(6).uppercase()}"

internal fun generateInvoiceNumber(): String =
    "INV-${currentUtc().toLocalDate().toString().replace("-", "")}-${UUID.randomUUID().toString().take(6).uppercase()}"

internal fun B2bQuoteDAO.toResponse() =
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

internal fun B2bPurchaseOrderDAO.toResponse() =
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
