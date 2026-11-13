package com.piashcse.database.entities

import com.piashcse.constants.B2bAccountStatus
import com.piashcse.constants.B2bQuoteStatus
import com.piashcse.constants.PurchaseOrderStatus
import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.datetime
import java.math.BigDecimal

object B2bAccountTable : BaseIdTable("b2b_account") {
    val userId = reference("user_id", UserTable.id).uniqueIndex()
    val companyName = varchar("company_name", 200)
    val taxId = varchar("tax_id", 50).nullable()
    val contactName = varchar("contact_name", 150).nullable()
    val phone = varchar("phone", 30).nullable()
    val creditLimit = decimal("credit_limit", 12, 2).default(BigDecimal.ZERO)
    val paymentTermsDays = integer("payment_terms_days").default(30)
    val status = enumerationByName("status", 20, B2bAccountStatus::class).default(B2bAccountStatus.PENDING).index()
    val approvedBy = reference("approved_by", UserTable.id).nullable()
    val approvedAt = datetime("approved_at").nullable()
}

class B2bAccountDAO(id: EntityID<String>) : BaseEntity(id, B2bAccountTable) {
    companion object : BaseEntityClass<B2bAccountDAO>(B2bAccountTable, B2bAccountDAO::class.java)

    var userId by B2bAccountTable.userId
    var companyName by B2bAccountTable.companyName
    var taxId by B2bAccountTable.taxId
    var contactName by B2bAccountTable.contactName
    var phone by B2bAccountTable.phone
    var creditLimit by B2bAccountTable.creditLimit
    var paymentTermsDays by B2bAccountTable.paymentTermsDays
    var status by B2bAccountTable.status
    var approvedBy by B2bAccountTable.approvedBy
    var approvedAt by B2bAccountTable.approvedAt
}

object B2bQuoteTable : BaseIdTable("b2b_quote") {
    val buyerUserId = reference("buyer_user_id", UserTable.id).index()
    val sellerId = reference("seller_id", SellerTable.id).index()
    val shopId = reference("shop_id", ShopTable.id).nullable()
    val status = enumerationByName("status", 20, B2bQuoteStatus::class).default(B2bQuoteStatus.REQUESTED)
    val currency = varchar("currency", 3).default("USD")
    val subtotal = decimal("subtotal", 12, 2).default(BigDecimal.ZERO)
    val taxAmount = decimal("tax_amount", 12, 2).default(BigDecimal.ZERO)
    val total = decimal("total", 12, 2).default(BigDecimal.ZERO)
    val notes = text("notes").nullable()
    val validUntil = datetime("valid_until").nullable()
    val respondedAt = datetime("responded_at").nullable()
}

class B2bQuoteDAO(id: EntityID<String>) : BaseEntity(id, B2bQuoteTable) {
    companion object : BaseEntityClass<B2bQuoteDAO>(B2bQuoteTable, B2bQuoteDAO::class.java)

    var buyerUserId by B2bQuoteTable.buyerUserId
    var sellerId by B2bQuoteTable.sellerId
    var shopId by B2bQuoteTable.shopId
    var status by B2bQuoteTable.status
    var currency by B2bQuoteTable.currency
    var subtotal by B2bQuoteTable.subtotal
    var taxAmount by B2bQuoteTable.taxAmount
    var total by B2bQuoteTable.total
    var notes by B2bQuoteTable.notes
    var validUntil by B2bQuoteTable.validUntil
    var respondedAt by B2bQuoteTable.respondedAt
}

object B2bQuoteItemTable : BaseIdTable("b2b_quote_item") {
    val quoteId = reference("quote_id", B2bQuoteTable.id).index()
    val productId = reference("product_id", ProductTable.id)
    val quantity = integer("quantity")
    val unitPrice = decimal("unit_price", 10, 2)
    val total = decimal("total", 12, 2)
}

class B2bQuoteItemDAO(id: EntityID<String>) : BaseEntity(id, B2bQuoteItemTable) {
    companion object : BaseEntityClass<B2bQuoteItemDAO>(B2bQuoteItemTable, B2bQuoteItemDAO::class.java)

    var quoteId by B2bQuoteItemTable.quoteId
    var productId by B2bQuoteItemTable.productId
    var quantity by B2bQuoteItemTable.quantity
    var unitPrice by B2bQuoteItemTable.unitPrice
    var total by B2bQuoteItemTable.total
}

object B2bPurchaseOrderTable : BaseIdTable("b2b_purchase_order") {
    val quoteId = reference("quote_id", B2bQuoteTable.id).uniqueIndex()
    val orderId = reference("order_id", OrderTable.id).nullable()
    val buyerUserId = reference("buyer_user_id", UserTable.id).index()
    val sellerId = reference("seller_id", SellerTable.id).index()
    val shopId = reference("shop_id", ShopTable.id).nullable()
    val poNumber = varchar("po_number", 50).uniqueIndex()
    val status = enumerationByName("status", 20, PurchaseOrderStatus::class).default(PurchaseOrderStatus.OPEN)
    val currency = varchar("currency", 3).default("USD")
    val total = decimal("total", 12, 2)
    val netTermsDays = integer("net_terms_days").default(30)
    val dueDate = datetime("due_date").nullable()
    val fulfilledAt = datetime("fulfilled_at").nullable()
    val canceledAt = datetime("canceled_at").nullable()
}

class B2bPurchaseOrderDAO(id: EntityID<String>) : BaseEntity(id, B2bPurchaseOrderTable) {
    companion object : BaseEntityClass<B2bPurchaseOrderDAO>(B2bPurchaseOrderTable, B2bPurchaseOrderDAO::class.java)

    var quoteId by B2bPurchaseOrderTable.quoteId
    var orderId by B2bPurchaseOrderTable.orderId
    var buyerUserId by B2bPurchaseOrderTable.buyerUserId
    var sellerId by B2bPurchaseOrderTable.sellerId
    var shopId by B2bPurchaseOrderTable.shopId
    var poNumber by B2bPurchaseOrderTable.poNumber
    var status by B2bPurchaseOrderTable.status
    var currency by B2bPurchaseOrderTable.currency
    var total by B2bPurchaseOrderTable.total
    var netTermsDays by B2bPurchaseOrderTable.netTermsDays
    var dueDate by B2bPurchaseOrderTable.dueDate
    var fulfilledAt by B2bPurchaseOrderTable.fulfilledAt
    var canceledAt by B2bPurchaseOrderTable.canceledAt
}
