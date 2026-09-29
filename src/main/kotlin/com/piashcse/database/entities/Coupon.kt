package com.piashcse.database.entities

import com.piashcse.constants.CouponDiscountType
import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.datetime
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset

object CouponTable : BaseIdTable("coupon") {
    val code = varchar("code", 50).uniqueIndex()
    val discountType = enumerationByName("discount_type", 20, CouponDiscountType::class)
    val discountValue = decimal("discount_value", 10, 2)
    val minOrderAmount = decimal("min_order_amount", 10, 2).default(BigDecimal.ZERO)
    val maxDiscountAmount = decimal("max_discount_amount", 10, 2).nullable()
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

object CouponUsageTable : BaseIdTable("coupon_usage") {
    val couponId = reference("coupon_id", CouponTable.id).index()
    val userId = reference("user_id", UserTable.id).index()
    val orderId = reference("order_id", OrderTable.id).nullable()
    val usedAt = datetime("used_at").clientDefault { LocalDateTime.now(ZoneOffset.UTC) }

    init {
        uniqueIndex(customIndexName = "coupon_usage_coupon_user_order_unique", couponId, userId, orderId)
    }
}

class CouponUsageDAO(id: EntityID<String>) : BaseEntity(id, CouponUsageTable) {
    companion object : BaseEntityClass<CouponUsageDAO>(CouponUsageTable, CouponUsageDAO::class.java)

    var couponId by CouponUsageTable.couponId
    var userId by CouponUsageTable.userId
    var orderId by CouponUsageTable.orderId
    var usedAt by CouponUsageTable.usedAt
}
