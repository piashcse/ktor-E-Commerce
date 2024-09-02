package com.piashcse.entities.coupon

import com.piashcse.entities.base.BaseIntEntity
import com.piashcse.entities.base.BaseIntEntityClass
import com.piashcse.entities.base.BaseIntIdTable
import org.jetbrains.exposed.dao.id.EntityID

object CouponTable : BaseIntIdTable("coupon") {
    val coupon = varchar("coupon", 50)
    val discount = integer("discount")
}

class CouponsEntity(id: EntityID<String>) : BaseIntEntity(id, CouponTable) {
    companion object : BaseIntEntityClass<CouponsEntity>(CouponTable)

    var coupons by CouponTable.coupon
    var discount by CouponTable.discount
}