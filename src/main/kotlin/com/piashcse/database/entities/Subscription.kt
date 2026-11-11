package com.piashcse.database.entities

import com.piashcse.constants.BillingInterval
import com.piashcse.constants.InvoiceStatus
import com.piashcse.constants.SubscriptionStatus
import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import com.piashcse.database.entities.base.currentUtc
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.datetime

object SubscriptionPlanTable : BaseIdTable("subscription_plan") {
    val name = varchar("name", 150)
    val description = text("description")
    val productId = reference("product_id", ProductTable.id).index()
    val interval = enumerationByName("interval", 20, BillingInterval::class).default(BillingInterval.MONTH)
    val price = decimal("price", 10, 2)
    val currency = varchar("currency", 3).default("USD")
    val trialDays = integer("trial_days").default(0)
    val isActive = bool("is_active").default(true)
}

class SubscriptionPlanDAO(id: EntityID<String>) : BaseEntity(id, SubscriptionPlanTable) {
    companion object : BaseEntityClass<SubscriptionPlanDAO>(SubscriptionPlanTable, SubscriptionPlanDAO::class.java)

    var name by SubscriptionPlanTable.name
    var description by SubscriptionPlanTable.description
    var productId by SubscriptionPlanTable.productId
    var interval by SubscriptionPlanTable.interval
    var price by SubscriptionPlanTable.price
    var currency by SubscriptionPlanTable.currency
    var trialDays by SubscriptionPlanTable.trialDays
    var isActive by SubscriptionPlanTable.isActive
}

object SubscriptionTable : BaseIdTable("subscription") {
    val userId = reference("user_id", UserTable.id).index()
    val planId = reference("plan_id", SubscriptionPlanTable.id)
    val status = enumerationByName("status", 20, SubscriptionStatus::class).default(SubscriptionStatus.ACTIVE).index()
    val startedAt = datetime("started_at").clientDefault { currentUtc() }
    val currentPeriodStart = datetime("current_period_start").clientDefault { currentUtc() }
    val currentPeriodEnd = datetime("current_period_end")
    val cancelAtPeriodEnd = bool("cancel_at_period_end").default(false)
    val externalReference = varchar("external_reference", 150).nullable()
}

class SubscriptionDAO(id: EntityID<String>) : BaseEntity(id, SubscriptionTable) {
    companion object : BaseEntityClass<SubscriptionDAO>(SubscriptionTable, SubscriptionDAO::class.java)

    var userId by SubscriptionTable.userId
    var planId by SubscriptionTable.planId
    var status by SubscriptionTable.status
    var startedAt by SubscriptionTable.startedAt
    var currentPeriodStart by SubscriptionTable.currentPeriodStart
    var currentPeriodEnd by SubscriptionTable.currentPeriodEnd
    var cancelAtPeriodEnd by SubscriptionTable.cancelAtPeriodEnd
    var externalReference by SubscriptionTable.externalReference
}

object SubscriptionInvoiceTable : BaseIdTable("subscription_invoice") {
    val subscriptionId = reference("subscription_id", SubscriptionTable.id).index()
    val amount = decimal("amount", 10, 2)
    val currency = varchar("currency", 3).default("USD")
    val status = enumerationByName("status", 20, InvoiceStatus::class).default(InvoiceStatus.PENDING)
    val periodStart = datetime("period_start")
    val periodEnd = datetime("period_end")
    val providerReference = varchar("provider_reference", 150).nullable()
    val paidAt = datetime("paid_at").nullable()
}

class SubscriptionInvoiceDAO(id: EntityID<String>) : BaseEntity(id, SubscriptionInvoiceTable) {
    companion object : BaseEntityClass<SubscriptionInvoiceDAO>(SubscriptionInvoiceTable, SubscriptionInvoiceDAO::class.java)

    var subscriptionId by SubscriptionInvoiceTable.subscriptionId
    var amount by SubscriptionInvoiceTable.amount
    var currency by SubscriptionInvoiceTable.currency
    var status by SubscriptionInvoiceTable.status
    var periodStart by SubscriptionInvoiceTable.periodStart
    var periodEnd by SubscriptionInvoiceTable.periodEnd
    var providerReference by SubscriptionInvoiceTable.providerReference
    var paidAt by SubscriptionInvoiceTable.paidAt
}

object DigitalEntitlementTable : BaseIdTable("digital_entitlement") {
    val userId = reference("user_id", UserTable.id).index()
    val productId = reference("product_id", ProductTable.id)
    val orderId = reference("order_id", OrderTable.id).nullable()
    val subscriptionId = reference("subscription_id", SubscriptionTable.id).nullable()
    val downloadToken = varchar("download_token", 100).uniqueIndex()
    val downloadUrl = text("download_url")
    val fileName = varchar("file_name", 255).nullable()
    val downloadCount = integer("download_count").default(0)
    val maxDownloads = integer("max_downloads").default(5)
    val expiresAt = datetime("expires_at").nullable()
}

class DigitalEntitlementDAO(id: EntityID<String>) : BaseEntity(id, DigitalEntitlementTable) {
    companion object : BaseEntityClass<DigitalEntitlementDAO>(DigitalEntitlementTable, DigitalEntitlementDAO::class.java)

    var userId by DigitalEntitlementTable.userId
    var productId by DigitalEntitlementTable.productId
    var orderId by DigitalEntitlementTable.orderId
    var subscriptionId by DigitalEntitlementTable.subscriptionId
    var downloadToken by DigitalEntitlementTable.downloadToken
    var downloadUrl by DigitalEntitlementTable.downloadUrl
    var fileName by DigitalEntitlementTable.fileName
    var downloadCount by DigitalEntitlementTable.downloadCount
    var maxDownloads by DigitalEntitlementTable.maxDownloads
    var expiresAt by DigitalEntitlementTable.expiresAt

    fun isActive(): Boolean = downloadCount < maxDownloads && (expiresAt == null || expiresAt!!.isAfter(currentUtc()))
}

fun ProductDAO.isDigitalProduct(): Boolean = isDigital
