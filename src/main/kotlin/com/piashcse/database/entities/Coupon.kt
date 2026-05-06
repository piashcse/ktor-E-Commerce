package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.datetime

object CouponTable : BaseIdTable("coupon") {
    val code = varchar("code", 50).uniqueIndex()
    val discountType = varchar("discount_type", 20) // FIXED, PERCENTAGE
    val discountValue = double("discount_value")
    val minOrderAmount = double("min_order_amount").default(0.0)
    val maxDiscountAmount = double("max_discount_amount").nullable()
    val startDate = datetime("start_date")
    val endDate = datetime("end_date")
    val usageLimit = integer("usage_limit").nullable()
    val usageCount = integer("usage_count").default(0)
    val isActive = bool("is_active").default(true)
}

class CouponDAO(id: EntityID<String>) : BaseEntity(id, CouponTable) {
    companion object : BaseEntityClass<CouponDAO>(CouponTable, CouponDAO::class.java)

    var code by CouponTable.code
    var discountType by CouponTable.discountType
    var discountValue by CouponTable.discountValue
    var minOrderAmount by CouponTable.minOrderAmount
    var maxDiscountAmount by CouponTable.maxDiscountAmount
    var startDate by CouponTable.startDate
    var endDate by CouponTable.endDate
    var usageLimit by CouponTable.usageLimit
    var usageCount by CouponTable.usageCount
    var isActive by CouponTable.isActive
}
