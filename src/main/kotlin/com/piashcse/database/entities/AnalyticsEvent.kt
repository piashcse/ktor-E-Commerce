package com.piashcse.database.entities

import com.piashcse.constants.AnalyticsEventType
import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.datetime

/** Append-only storefront event stream used for funnel and attribution reports. */
object AnalyticsEventTable : BaseIdTable("analytics_event") {
    val sessionId = varchar("session_id", 100).index()
    val userId = reference("user_id", UserTable.id).nullable()
    val eventType = enumerationByName("event_type", 30, AnalyticsEventType::class).index()
    val productId = reference("product_id", ProductTable.id).nullable()
    val orderId = reference("order_id", OrderTable.id).nullable()
    val path = varchar("path", 500).nullable()
    val referrer = varchar("referrer", 500).nullable()
    val utmSource = varchar("utm_source", 100).nullable()
    val utmMedium = varchar("utm_medium", 100).nullable()
    val utmCampaign = varchar("utm_campaign", 150).nullable()
    val utmTerm = varchar("utm_term", 150).nullable()
    val utmContent = varchar("utm_content", 150).nullable()
    val locale = varchar("locale", 10).nullable()
    val value = decimal("value", 12, 2).nullable()
    val currency = varchar("currency", 3).nullable()
    val metadata = text("metadata").nullable()
    val occurredAt = datetime("occurred_at")
}

class AnalyticsEventDAO(id: EntityID<String>) : BaseEntity(id, AnalyticsEventTable) {
    companion object : BaseEntityClass<AnalyticsEventDAO>(AnalyticsEventTable, AnalyticsEventDAO::class.java)

    var sessionId by AnalyticsEventTable.sessionId
    var userId by AnalyticsEventTable.userId
    var eventType by AnalyticsEventTable.eventType
    var productId by AnalyticsEventTable.productId
    var orderId by AnalyticsEventTable.orderId
    var path by AnalyticsEventTable.path
    var referrer by AnalyticsEventTable.referrer
    var utmSource by AnalyticsEventTable.utmSource
    var utmMedium by AnalyticsEventTable.utmMedium
    var utmCampaign by AnalyticsEventTable.utmCampaign
    var utmTerm by AnalyticsEventTable.utmTerm
    var utmContent by AnalyticsEventTable.utmContent
    var locale by AnalyticsEventTable.locale
    var value by AnalyticsEventTable.value
    var currency by AnalyticsEventTable.currency
    var metadata by AnalyticsEventTable.metadata
    var occurredAt by AnalyticsEventTable.occurredAt
}
