package com.piashcse.feature.b2b

import com.piashcse.constants.B2bAccountStatus
import com.piashcse.constants.B2bInvoiceStatus
import com.piashcse.constants.B2bQuoteStatus
import com.piashcse.constants.PurchaseOrderStatus
import com.piashcse.utils.validator.FieldValidationException
import com.piashcse.utils.validator.FieldViolation
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

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
) {
    init {
        val violations =
            buildList {
                creditLimit?.let {
                    if (it < BigDecimal.ZERO) add(FieldViolation("creditLimit", "Credit limit cannot be negative"))
                }
                paymentTermsDays?.let {
                    if (it !in 0..365) add(FieldViolation("paymentTermsDays", "Payment terms must be between 0 and 365 days"))
                }
            }
        if (violations.isNotEmpty()) throw FieldValidationException(violations)
    }
}

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
