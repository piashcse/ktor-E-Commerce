package com.piashcse.database.entities

import com.piashcse.constants.PromotionType
import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.datetime
import java.math.BigDecimal

object PromotionTable : BaseIdTable("promotion") {
    val name = varchar("name", 150)
    val type = enumerationByName("type", 30, PromotionType::class)
    val value = decimal("value", 12, 2)
    val minOrderAmount = decimal("min_order_amount", 12, 2).default(BigDecimal.ZERO)
    val maxDiscountAmount = decimal("max_discount_amount", 12, 2).nullable()
    val startsAt = datetime("starts_at")
    val endsAt = datetime("ends_at")
    val isActive = bool("is_active").default(true).index()
    val priority = integer("priority").default(0)
    val usageLimit = integer("usage_limit").nullable()
    val usageCount = integer("usage_count").default(0)
}

class PromotionDAO(id: EntityID<String>) : BaseEntity(id, PromotionTable) {
    companion object : BaseEntityClass<PromotionDAO>(PromotionTable, PromotionDAO::class.java)

    var name by PromotionTable.name
    var type by PromotionTable.type
    var value by PromotionTable.value
    var minOrderAmount by PromotionTable.minOrderAmount
    var maxDiscountAmount by PromotionTable.maxDiscountAmount
    var startsAt by PromotionTable.startsAt
    var endsAt by PromotionTable.endsAt
    var isActive by PromotionTable.isActive
    var priority by PromotionTable.priority
    var usageLimit by PromotionTable.usageLimit
    var usageCount by PromotionTable.usageCount
}
