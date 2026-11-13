package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import com.piashcse.database.entities.base.currentUtc
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.datetime
import java.math.BigDecimal

/** Country/category/zone/class tax rate. A NULL category/class is the fallback. */
object TaxRuleTable : BaseIdTable("tax_rule") {
    val country = varchar("country", 3).index()
    val categoryId = reference("category_id", ProductCategoryTable.id).nullable()
    val zoneId = reference("zone_id", TaxZoneTable.id).nullable()
    val taxClassId = reference("tax_class_id", TaxClassTable.id).nullable()
    val rate = decimal("rate", 6, 4)
    val isInclusive = bool("is_inclusive").default(false)
    val priority = integer("priority").default(0)
    val isActive = bool("is_active").default(true)
}

class TaxRuleDAO(id: EntityID<String>) : BaseEntity(id, TaxRuleTable) {
    companion object : BaseEntityClass<TaxRuleDAO>(TaxRuleTable, TaxRuleDAO::class.java)

    var country by TaxRuleTable.country
    var categoryId by TaxRuleTable.categoryId
    var zoneId by TaxRuleTable.zoneId
    var taxClassId by TaxRuleTable.taxClassId
    var rate by TaxRuleTable.rate
    var isInclusive by TaxRuleTable.isInclusive
    var priority by TaxRuleTable.priority
    var isActive by TaxRuleTable.isActive
}

/** Tax class assigned to products (e.g. standard, reduced, zero-rated). */
object TaxClassTable : BaseIdTable("tax_class") {
    val name = varchar("name", 100)
    val code = varchar("code", 50)
    val rate = decimal("rate", 6, 4).default(BigDecimal.ZERO)
    val isInclusive = bool("is_inclusive").default(false)
    val isActive = bool("is_active").default(true)
}

class TaxClassDAO(id: EntityID<String>) : BaseEntity(id, TaxClassTable) {
    companion object : BaseEntityClass<TaxClassDAO>(TaxClassTable, TaxClassDAO::class.java)

    var name by TaxClassTable.name
    var code by TaxClassTable.code
    var rate by TaxClassTable.rate
    var isInclusive by TaxClassTable.isInclusive
    var isActive by TaxClassTable.isActive
}

/** Jurisdiction where the merchant has nexus. Region is optional (state/province). */
object TaxZoneTable : BaseIdTable("tax_zone") {
    val name = varchar("name", 100)
    val country = varchar("country", 3).index()
    val region = varchar("region", 100).nullable()
    val isNexus = bool("is_nexus").default(true)
    val priority = integer("priority").default(0)
    val isActive = bool("is_active").default(true)
}

class TaxZoneDAO(id: EntityID<String>) : BaseEntity(id, TaxZoneTable) {
    companion object : BaseEntityClass<TaxZoneDAO>(TaxZoneTable, TaxZoneDAO::class.java)

    var name by TaxZoneTable.name
    var country by TaxZoneTable.country
    var region by TaxZoneTable.region
    var isNexus by TaxZoneTable.isNexus
    var priority by TaxZoneTable.priority
    var isActive by TaxZoneTable.isActive
}

/** Customer/business tax exemption certificate. A null country applies everywhere. */
object TaxExemptionTable : BaseIdTable("tax_exemption") {
    val userId = reference("user_id", UserTable.id).nullable()
    val businessTaxId = varchar("business_tax_id", 50).nullable()
    val country = varchar("country", 3).nullable()
    val reason = text("reason")
    val certificateUrl = varchar("certificate_url", 500).nullable()
    val isActive = bool("is_active").default(true)
    val expiresAt = datetime("expires_at").nullable()
}

class TaxExemptionDAO(id: EntityID<String>) : BaseEntity(id, TaxExemptionTable) {
    companion object : BaseEntityClass<TaxExemptionDAO>(TaxExemptionTable, TaxExemptionDAO::class.java)

    var userId by TaxExemptionTable.userId
    var businessTaxId by TaxExemptionTable.businessTaxId
    var country by TaxExemptionTable.country
    var reason by TaxExemptionTable.reason
    var certificateUrl by TaxExemptionTable.certificateUrl
    var isActive by TaxExemptionTable.isActive
    var expiresAt by TaxExemptionTable.expiresAt
}

fun TaxExemptionDAO.isLive(): Boolean = isActive && (expiresAt?.isAfter(currentUtc()) ?: true)
